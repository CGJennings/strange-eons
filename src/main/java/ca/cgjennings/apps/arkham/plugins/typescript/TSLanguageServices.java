package ca.cgjennings.apps.arkham.plugins.typescript;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.TextEncoding;
import ca.cgjennings.apps.arkham.plugins.engine.SEScriptEngine;
import ca.cgjennings.apps.arkham.plugins.engine.SEScriptEngineFactory;
import java.awt.EventQueue;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.logging.Level;
import javax.script.ScriptException;
import org.mozilla.javascript.Context;

/**
 * Provides low-level access to TypeScript language services from Java.
 * Methods of this class delegate to a service provider that runs in
 * another thread. Each method generally comes in two flavours:
 * a synchronous version that blocks until the operation completes, or a 
 * version which accepts a callback. When callbacks are used, the
 * callback is always invoked on the event dispatch thread. Synchronous
 * methods can be called from any thread.
 * 
 * <p>For most purposes, it is easier and more convenient to use a
 * {@link CompilationRoot} to access these services.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class TSLanguageServices {
    private static volatile TSLanguageServices shared = new TSLanguageServices();
    private final boolean debuggable = StrangeEons.getReleaseType() == StrangeEons.ReleaseType.DEVELOPMENT;
    private volatile boolean hasLoaded;
    private Thread workerThread;
    private BlockingQueue<Request<?>> queue = new LinkedBlockingQueue<>();
    private SEScriptEngine engine;
    private ServiceInterface services;    

    /**
     * Returns the shared default instance.
     * @return a shared instance
     */
    public static TSLanguageServices getShared() {
        return shared;
    }

    private static void loadServiceLib(SEScriptEngine engine, String libName) throws ScriptException {
        try (Reader r = new InputStreamReader(
                TSLanguageServices.class.getResourceAsStream(libName),
                TextEncoding.SOURCE_CODE
        )) {
            engine.eval(r);
        } catch (Exception ex) {
            throw new AssertionError("unable to load " + libName, ex);
        }
    }
    
    /**
     * Creates a new language service. The new service will immediately start
     * loading in another thread. Since language service instances are expensive
     * to create and designed to be shared, use the {@linkplain #getShared() shared}
     * instance unless you know what you are doing.
     */
    public TSLanguageServices() {
        workerThread = new Thread(() -> {
            long start = System.currentTimeMillis();
            StrangeEons.log.info("loading TypeScript language services");
            Context cx = Context.enter();
            try {
                SEScriptEngineFactory.makeCurrentThreadAUtilityThread();
                engine = SEScriptEngineFactory.getDefaultScriptEngine();
                cx.setOptimizationLevel(-1);
                cx.setGeneratingSource(false);
                cx.setMaximumInterpreterStackDepth(Integer.MAX_VALUE);
                cx.setGeneratingDebug(debuggable);
                engine.put("DEBUG", debuggable);
                engine.put("javax.script.filename", "ts-services");

                // stored in lib/typescript-services.jar; a script in build-tools updates it
                loadServiceLib(engine, "typescriptServices.js");
                // fake some CommonJS globals so the transpiled bridge code runs unchanged
                engine.eval("var exports = {}; var require = function (p) { return ts; }");
                loadServiceLib(engine, "java-bridge.js");
                // delete the faked module globals, but only if we are not debugging
                // since otherwise this script fragment will represent the lib in the debugger
                if (!debuggable) engine.eval("delete exports; delete require");
                services = engine.getInterface(ServiceInterface.class);
            } catch (ScriptException ex) {
                throw new AssertionError("failed to parse library", ex);
            } finally {
                if (cx != null) {
                    Context.exit();
                }
            }
            hasLoaded = true;
            StrangeEons.log.log(Level.INFO, "services ready, started in {0}ms", System.currentTimeMillis() - start);
            
            for (;;) {
                try {
                    Request<?> request = queue.take();
                    serviceRequest(request);
                } catch (InterruptedException ex) {
                    // ... continue waiting
                }
            }
        }, "TypeScript service thread");
        workerThread.setDaemon(true);
        workerThread.start();
    }
    
    /**
     * Return whether the services have finished loading and begun accepting
     * commands.
     * 
     * @return true if the services have finished initializing
     */
    public boolean isLoaded() {
        return hasLoaded;
    }
    
    /**
     * Releases resources consumed by the services when no longer needed.
     * The result of using this instance after calling this method is
     * undefined.
     */
    public void dispose() {
        StrangeEons.log.info("shutting down");
        invoke(DISPOSE, null);
    }
    
    private void disposeImpl() {
        EventQueue.invokeLater(() -> {
            workerThread = null;
            queue = null;
            engine = null;
            if (shared == this) {
                shared = null;
            }
            StrangeEons.log.info("stopped");
        });
    }
    
    private static final int DISPOSE = 0;
    
    /**
     * Gets the services library version as a string.
     * @param cb the callback that will receive the result
     */
    public void getVersion(Consumer<String> cb) {
        invoke(VERSION, cb);
    }

    /**
     * Gets the services library version as a string.
     * @return the version
     */    
    public String getVersion() {
        return invokeSync(VERSION, (Object[]) null);
    }
    
    private static final int VERSION = 1;

    /**
     * Returns the raw TypeScript service library JS object
     * for development and debugging.
     * @param cb the callback that will receive the result
     */
    public void getServicesLib(Consumer<Object> cb) {
        invoke(GET_LIB, cb);
    }

    /**
     * Returns the raw TypeScript service library JS object
     * for development and debugging.
     * 
     * @return the TS services library
     */    
    public Object getServicesLib() {
        return invokeSync(GET_LIB);
    }

    private static final int GET_LIB = 2;
    
    public static void debugRestart() {
        var openTsFiles = new ArrayList<File>();
        for (var ed : StrangeEons.getWindow().getEditors()) {
            if (ed.getFile() != null && "ts".equals(ed.getFileNameExtension())) {
                openTsFiles.add(ed.getFile());
                if (ed.hasUnsavedChanges()) {
                    ed.save();
                }
                ed.close();
            }
        }
        if (shared != null) shared.dispose();
        var replace = new TSLanguageServices();
        // wait for replacement to load then re-open .ts files with new 
        replace.getVersion((s) -> {
            shared = replace;
            CompilationFactory.debugClearRoots();
            for (var f : openTsFiles) {
                StrangeEons.getWindow().openFile(f);
            }            
        });
    }

    /**
     * Performs a simple transpilation of a single file to JavaScript code.
     * Performs no type checking and reports no errors.
     * 
     * @param fileName an optional fileName for the script
     * @param script the text of the script
     * @param cb the callback that will receive the result 
     */
    public void transpile(String fileName, String script, Consumer<String> cb) {
        invoke(TRANSPILE_SIMPLE, cb, fileName, script);
    }
    
    /**
     * Performs a simple transpilation of a single file to JavaScript code.
     * Performs no type checking and reports no errors.
     * 
     * @param fileName an optional fileName for the script
     * @param script the text of the script
     * @return the JavaScript equivalent of the specified script text
     */    
    public String transpile(String fileName, String script) {
        return invokeSync(TRANSPILE_SIMPLE, fileName, script);
    }    
    
    private static final int TRANSPILE_SIMPLE = 3;
    
    /**
     * Creates a script snapshot from the text of a script.
     * 
     * @param script the script to snapshot
     * @return the snapshot object
     */
    public Object createSnapshot(String script) {
        return invokeSync(CREATE_SNAPSHOT, script);
    }
    
    private static final int CREATE_SNAPSHOT = 4;

    /**
     * Creates a language service host from the given root.
     * @param root the compilation root to delegate to
     * @return a language service host
     */
    public Object createLanguageServiceHost(CompilationRoot root) {
        return invokeSync(CREATE_HOST, root);
    }
    
    private static final int CREATE_HOST = 5;

    /**
     * Creates a language service instance from the given root.
     * @param root the compilation root to delegate to
     * @return a language service
     */
    public Object createLanguageService(CompilationRoot root) {
        return invokeSync(CREATE_SERVICE, root);
    }
    
    /**
     * Creates a language service instance from the given host.
     * @param languageServiceHost the host instance
     * @return a language service
     */
    public Object createLanguageService(Object languageServiceHost) {
        return invokeSync(CREATE_SERVICE, languageServiceHost);
    }    

    private static final int CREATE_SERVICE = 6;
    
    /**
     * Compiles a source file.
     * @param languageService the language service
     * @param fileName the name of the file to compile
     * @param cb the callback that will receive the result 
     * @return the compilation output
     */
    public void compile(Object languageService, String fileName, Consumer<CompiledSource> cb) {
        invoke(COMPILE, cb, languageService, fileName);
    }  
    
    /**
     * Compiles a source file.
     * @param languageService the language service
     * @param fileName the name of the file to compile
     * @return the compilation output
     */
    public CompiledSource compile(Object languageService, String fileName) {
        return invokeSync(COMPILE, languageService, fileName);
    }
    
    private static final int COMPILE = 7;
    
    /**
     * Returns a list of diagnostic messages for a file.
     * 
     * @param languageService the language service that manages the file
     * @param fileName the file name to get diagnostics for
     * @param includeSyntactic if true, includes syntax-related diagnostics
     * @param includeSemantic if true, includes semantic diagnostics
     * @return a list of diagnostics, or null
     */    
    public void getDiagnostics(Object languageService, String fileName, boolean includeSyntactic, boolean includeSemantic, Consumer<List<Diagnostic>> cb) {
        invoke(GET_DIAGNOSTICS, cb, languageService, fileName, includeSyntactic, includeSemantic);
    }
    
    /**
     * Returns a list of diagnostic messages for a file.
     * 
     * @param languageService the language service that manages the file
     * @param fileName the file name to get diagnostics for
     * @param includeSyntactic if true, includes syntax-related diagnostics
     * @param includeSemantic if true, includes semantic diagnostics
     * @return a list of diagnostics, or null
     */    
    public List<Diagnostic> getDiagnostics(Object languageService, String fileName, boolean includeSyntactic, boolean includeSemantic) {
        return invokeSync(GET_DIAGNOSTICS, languageService, fileName, includeSyntactic, includeSemantic);
    }
    
    private static final int GET_DIAGNOSTICS = 8;
    
        
    /**
     * Returns a collection of code completions or null.
     * 
     * @param languageService the language service that manages the file
     * @param fileName the file name to get diagnostics for
     * @param position the offset into the source file
     */
    public CompletionInfo getCodeCompletions(Object languageService, String fileName, int position) {
        return invokeSync(GET_COMPLETIONS, languageService, fileName, position);
    }
    
    private static final int GET_COMPLETIONS = 9;

    /**
     * Returns further details about a code completion returned from
     * {@link #getCodeCompletions(java.lang.Object, java.lang.String, int)}.
     * 
     * @param languageService the language service that manages the file
     * @param fileName the file name to get diagnostics for
     * @param position the offset into the source file
     * @param completion the completion that details are requested for
     * @return the additional details, such as signature and doc comments
     */
    public CompletionInfo.EntryDetails getCodeCompletionDetails(Object languageService, String fileName, int position, CompletionInfo.Entry completion) {
        return invokeSync(GET_COMPLETION_DETAILS, languageService, fileName, position, completion);
    }
    
    private static final int GET_COMPLETION_DETAILS = 10;
    
    /**
     * Returns a file's current navigation tree.
     * 
     * @param languageService the language service that manages the file
     * @param fileName the file name to get diagnostics for
     */
    public void getNavigationTree(Object languageService, String fileName, Consumer<NavigationTree> callback) {
        invoke(GET_NAVIGATION_TREE, callback, languageService, fileName);
    }    
    
    /**
     * Returns a file's current navigation tree.
     * 
     * @param languageService the language service that manages the file
     * @param fileName the file name to get diagnostics for
     * @return the file's current navigation tree
     */
    public NavigationTree getNavigationTree(Object languageService, String fileName) {
        return invokeSync(GET_NAVIGATION_TREE, languageService, fileName);
    }

    private static final int GET_NAVIGATION_TREE = 11;
    
    /**
     * Returns a tool tip of information about the specified position.
     * 
     * @param languageService the language service that manages the file
     * @param fileName the file name to get diagnostics for
     * @param position the offset into the source file
     */
    public Overview getOverview(Object languageService, String fileName, int position) {
        return invokeSync(GET_OVERVIEW, languageService, fileName, position);
    }
    
    private static final int GET_OVERVIEW = 12;
            
            
    /**
     * Handles an individual request by calling into the TS lib, returning
     * any result.
     * 
     * @param r the request to process
     */
    @SuppressWarnings("unchecked")
    private void serviceRequest(Request<?> r) {
        try {
            switch (Math.abs(r.type)) {
                case DISPOSE:
                    disposeImpl();
                    return;
                case VERSION:
                    ((Request<String>)r).send(services.getVersion());
                    break;
                case GET_LIB:
                    ((Request<Object>)r).send(services.getServicesLib());
                    break;
                case TRANSPILE_SIMPLE:
                    ((Request<String>)r).send(services.transpile((String)r.args[0], (String)r.args[1]));
                    break;
                case CREATE_SNAPSHOT:
                    ((Request<Object>)r).send(services.createSnapshot((String)r.args[0]));
                    break;
                case CREATE_HOST:
                    ((Request<Object>)r).send(services.createLanguageServiceHost((CompilationRoot)r.args[0]));
                    break;
                case CREATE_SERVICE: {
                    Object host = r.args[0];
                    if (host instanceof CompilationRoot) {
                        host = services.createLanguageServiceHost((CompilationRoot)host);
                    }
                    ((Request<Object>)r).send(services.createLanguageService(host));
                    break;
                }
                case COMPILE:
                    ((Request<CompiledSource>)r).send(services.compile(r.args[0], (String)r.args[1]));
                    break;
                case GET_DIAGNOSTICS:
                    ((Request<List<Diagnostic>>)r).send(services.getDiagnostics(r.args[0], (String) r.args[1], (Boolean) r.args[2], (Boolean) r.args[3]));
                    break;
                case GET_COMPLETIONS:
                    ((Request<CompletionInfo>)r).send(services.getCodeCompletions(r.args[0], (String) r.args[1], (Integer) r.args[2]));
                    break;
                case GET_COMPLETION_DETAILS:
                    ((Request<CompletionInfo.EntryDetails>)r).send(services.getCodeCompletionDetails(r.args[0], (String) r.args[1], (Integer) r.args[2], (CompletionInfo.Entry) r.args[3]));
                    break;
                case GET_NAVIGATION_TREE:
                    ((Request<NavigationTree>)r).send(services.getNavigationTree(r.args[0], (String) r.args[1]));
                    break;
                case GET_OVERVIEW:
                    ((Request<Overview>) r).send(services.getOverview(r.args[0], (String) r.args[1], (Integer) r.args[2]));
                    break;
                default:
                    throw new AssertionError("unknown request type");
            }
        } catch (Throwable t) {
            StrangeEons.log.log(Level.SEVERE, "exception while handling " + r, t);
            if (r.type < 0 || r.callback != null) {
                r.send(null);
            }
        }
    }    
    
    /**
     * Adds a request to be handled asynchronously.
     * 
     * @param <T> the return value type
     * @param type the action to perform
     * @param callback the callback to invoke with a return value
     * @param args the arguments needed to complete the request
     */
    private <T> void invoke(int type, Consumer<T> callback, Object... args) {
        queue.add(new Request<T>(type, callback, args));
    }

    /**
     * Adds a request to be handled synchronously.
     * 
     * @param <T> the return value type
     * @param type the action to perform
     * @param args the arguments needed to complete the request
     * @return the return value of the request, or null
     */    
    private <T> T invokeSync(int type, Object... args) {
        Request<T> r = new Request<>(-type, null, args);
        // if we are called from the worker thread, handle the request
        // directly without putting it on the queue; this allows
        // the TS library to invoke methods on passed-in Java objects
        // that in turn make calls back into the TS library
        if (Thread.currentThread() == workerThread) {
            serviceRequest(r);
        } else {
            // otherwise, the usual case is to add the request to
            // the queue and then wait for the worker thread
            synchronized (r) {
                queue.add(r);
                try {
                    r.wait();
                } catch (InterruptedException iex) {
                    StrangeEons.log.warning("interrupted");
                    return null;
                }
            }
        }
        if (r.exception != null) {
            throw new RuntimeException("Exception in language services", r.exception);
        }
        return r.retval;
    }
    
    /**
     * Encapsulates a request to be sent to the worker thread.
     * @param <T> the return type of the request
     */
    private class Request<T> {
        private final int type;
        private final Object[] args;
        private final Consumer<T> callback;
        private T retval;
        private Throwable exception;
        
        /**
         * Creates a new request to be added to the queue.
         * 
         * @param type the type constant, if negative, then the actual type
         * is the absolute value of the type constant but the request is
         * synchronous
         * @param callback the callback, can be null if the type returns
         * no value or is synchronous
         * @param args arguments required to complete the request,
         * may be null if the type takes no arguments
         */
        public Request(int type, Consumer<T> callback, Object... args) {
            this.type = type;
            this.callback = callback;
            this.args = args;
        }
        
        public void send(T retval) {
            // a positive type value means to use the callback,
            // a negative type value means to send back a synchronous result
            if (type >= 0) {            
                EventQueue.invokeLater(() -> {
                    try {
                        callback.accept(retval);
                    } catch (Throwable t) {
                        StrangeEons.log.log(Level.SEVERE, "exception in callback", t);
                    }
                });
            } else {
                synchronized (this) {
                    this.retval = retval;
                    notify();
                }
            }
        }
        
        @Override
        public String toString() {
            String s = "Request{type=" + nameOfRequestType(type)
                    + ", synch=" + (type < 0)
            ;
            if (args != null) {
                s += ", args=[";
                for (int i=0; i<args.length; ++i) {
                    if (i > 0) s += ", ";
                    s += args[i];
                }
                s += ']';
            }
            return s + '}';
        }
    }
    
    private static String nameOfRequestType(int type) {
        try {
            type = Math.abs(type);
            for (var f : TSLanguageServices.class.getDeclaredFields()) {
                final int mod = f.getModifiers();
                if (f.getType() == int.class && Modifier.isStatic(mod) && Modifier.isFinal(mod)) {
                    if (type == f.getInt(null)) return f.getName();
                }                
            }
        } catch (Exception rex) {}
        return "<Unknown>";
    }    
    
    /**
     * Interfaces that bridges access from the Java wrapper for TypeScript
     * services to the JavaScript implementation.
     */
    static interface ServiceInterface {
        /** Returns the raw TS lib object for development/debugging. */
        Object getServicesLib();
        
        /** Returns the version of TypeScript services. */
        String getVersion();

        /** Returns a simple transpilation of a single file with no checking. */
        String transpile(String fileName, String text);

        /** Creates a script snapshot from the text of a script. */
        Object createSnapshot(String text);
        
        /** Creates a language service from the specified language service host. */
        Object createLanguageService(Object host);
        
        /** Creates a language service host that delegates to the specified root. */
        Object createLanguageServiceHost(CompilationRoot root);
        
        /** Compiles a source file. */
        CompiledSource compile(Object languageService, String fileName);
        
        /** Returns a list of diagnostic messages for a file. */
        List<Diagnostic> getDiagnostics(Object languageService, String fileName, boolean includeSyntactic, boolean includeSemantic);
        
        /** Returns a collection of code completions or null. */
        CompletionInfo getCodeCompletions(Object languageService, String fileName, int position);
        
        /** Returns additional information about a code completion. */
        CompletionInfo.EntryDetails getCodeCompletionDetails(Object languageService, String fileName, int position, CompletionInfo.Entry completion);
        
        /** Returns an outline of the file. */
        NavigationTree getNavigationTree(Object languageService, String fileName);
        
        /** Returns a quick overview of the node at the specified position. */
        Overview getOverview(Object languageService, String fileName, int position);
    }
}
