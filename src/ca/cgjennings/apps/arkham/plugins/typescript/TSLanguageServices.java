package ca.cgjennings.apps.arkham.plugins.typescript;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.TextEncoding;
import ca.cgjennings.apps.arkham.plugins.engine.SEScriptEngine;
import ca.cgjennings.apps.arkham.plugins.engine.SEScriptEngineFactory;
import java.awt.EventQueue;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.logging.Level;
import javax.script.ScriptException;
import javax.swing.Icon;
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
 * @author chris
 */
public final class TSLanguageServices {
    private static TSLanguageServices shared = new TSLanguageServices();
    private final boolean debuggable = StrangeEons.getReleaseType() == StrangeEons.ReleaseType.DEVELOPMENT;

    private volatile boolean hasLoaded;
    private List<Runnable> tasksToRunWhenLoaded;

    /**
     * Returns the shared default instance.
     * @return a shared instance
     */
    public static TSLanguageServices getShared() {
        return shared;
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
                cx.setGeneratingDebug(debuggable);
                cx.setMaximumInterpreterStackDepth(Integer.MAX_VALUE);
                // This file is stored in lib/typescript-services.jar to
                // reduce build times and prevent IDEs from trying to
                // process it for errors, code completions, etc.
                // It can be updated via a script in build-tools.
                String lib = "typescriptServices.js";
                for (int i=0; i<2; ++i) {
                    try (Reader r = new InputStreamReader(
                            TSLanguageServices.class.getResourceAsStream(lib),
                            TextEncoding.SOURCE_CODE
                    )) {
                        engine.eval(r);
                    } catch (IOException ex) {
                        throw new AssertionError("unable to load " + lib, ex);
                    }
                    lib = "java-bridge.js";
                }
                services = engine.getInterface(ServiceInterface.class);
            } catch (ScriptException ex) {
                throw new AssertionError("failed to parse library", ex);
            } finally {
                if (cx != null) {
                    Context.exit();
                }
            }        
            StrangeEons.log.log(Level.INFO, "services ready, started in {0}ms", System.currentTimeMillis() - start);

            synchronized (TSLanguageServices.this) {
                hasLoaded = true;
                if (tasksToRunWhenLoaded != null) {
                    for (Runnable task : tasksToRunWhenLoaded) {
                        EventQueue.invokeLater(task);
                    }
                }
            }
            
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
     * Queues a task to run once the services have finished loading. If the
     * services have already loaded, the task runs immediately. This method
     * expects to be called from, and will run tasks on, the event dispatch thread.
     * 
     * @param task the task to run
     */
    public void runWhenLoaded(Runnable task) {
        synchronized (this) {
            if (hasLoaded) {
                task.run();
            } else {
                if (tasksToRunWhenLoaded == null) {
                    tasksToRunWhenLoaded = new ArrayList<>();
                }
                tasksToRunWhenLoaded.add(task);
            }
        }
    }
    
    /**
     * Releases resources consumed by the services when no longer needed.
     * The result of using this instance after calling this method is
     * undefined.
     */
    public void dispose() {
        StrangeEons.log.info("shutting down");
        go(DISPOSE, null);
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
        go(VERSION, cb);
    }

    /**
     * Gets the services library version as a string.
     * @return the version
     */    
    public String getVersion() {
        return goSync(VERSION, (Object[]) null);
    }
    
    private static final int VERSION = 1;

    /**
     * Returns the raw TypeScript service library JS object
     * for development and debugging.
     * @param cb the callback that will receive the result
     */
    public void getServicesLib(Consumer<Object> cb) {
        go(GET_LIB, cb);
    }

    /**
     * Returns the raw TypeScript service library JS object
     * for development and debugging.
     * 
     * @return the TS services library
     */    
    public Object getServicesLib() {
        return goSync(GET_LIB);
    }    

    private static final int GET_LIB = 2;

    /**
     * Performs a simple transpilation of a single file to JavaScript code.
     * Performs no type checking and reports no errors.
     * 
     * @param fileName an optional fileName for the script
     * @param script the text of the script
     * @param cb the callback that will receive the result 
     */
    public void transpile(String fileName, String script, Consumer<String> cb) {
        go(TRANSPILE_SIMPLE, cb, fileName, script);
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
        return goSync(TRANSPILE_SIMPLE, fileName, script);
    }    
    
    private static final int TRANSPILE_SIMPLE = 3;
    
    /**
     * Creates a script snapshot from the text of a script.
     * 
     * @param script the script to snapshot
     * @return the snapshot object
     */
    public Object createSnapshot(String script) {
        return goSync(CREATE_SNAPSHOT, script);
    }
    
    private static final int CREATE_SNAPSHOT = 4;

    /**
     * Creates a language service host from the given root.
     * @param root the compilation root to delegate to
     * @return a language service host
     */
    public Object createLanguageServiceHost(CompilationRoot root) {
        return goSync(CREATE_HOST, root);
    }
    
    private static final int CREATE_HOST = 5;

    /**
     * Creates a language service instance from the given root.
     * @param root the compilation root to delegate to
     * @return a language service
     */
    public Object createLanguageService(CompilationRoot root) {
        return goSync(CREATE_SERVICE, root);
    }
    
    /**
     * Creates a language service instance from the given host.
     * @param languageServiceHost the host instance
     * @return a language service
     */
    public Object createLanguageService(Object languageServiceHost) {
        return goSync(CREATE_SERVICE, languageServiceHost);
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
        go(COMPILE, cb, languageService, fileName);
    }  
    
    /**
     * Compiles a source file.
     * @param languageService the language service
     * @param fileName the name of the file to compile
     * @return the compilation output
     */
    public CompiledSource compile(Object languageService, String fileName) {
        return goSync(COMPILE, languageService, fileName);
    }
    
    private static final int COMPILE = 7;
    
    /**
     * Returns a list of diagnostic messages for a file.
     * 
     * @param service the language service that manages the file
     * @param fileName the file name to get diagnostics for
     * @param includeSyntactic if true, includes syntax-related diagnostics
     * @param includeSemantic if true, includes semantic diagnostics
     * @return a list of diagnostics, or null
     */    
    public void getDiagnostics(Object languageService, String fileName, boolean includeSyntactic, boolean includeSemantic, Consumer<List<Diagnostic>> cb) {
        go(GET_DIAGNOSTICS, cb, languageService, fileName, includeSyntactic, includeSemantic);
    }
    
    /**
     * Returns a list of diagnostic messages for a file.
     * 
     * @param service the language service that manages the file
     * @param fileName the file name to get diagnostics for
     * @param includeSyntactic if true, includes syntax-related diagnostics
     * @param includeSemantic if true, includes semantic diagnostics
     * @return a list of diagnostics, or null
     */    
    public List<Diagnostic> getDiagnostics(Object languageService, String fileName, boolean includeSyntactic, boolean includeSemantic) {
        return goSync(GET_DIAGNOSTICS, languageService, fileName, includeSyntactic, includeSemantic);
    }
    
    private static final int GET_DIAGNOSTICS = 8;
    
        
    /** Returns a collection of code completions or null. */
    public CompletionInfo getCodeCompletions(Object languageService, String fileName, int position) {
        return goSync(GET_COMPLETIONS, languageService, fileName, position);
    }
    
    private static final int GET_COMPLETIONS = 9;

    public CompletionInfo.EntryDetails getCodeCompletionDetails(Object languageService, String fileName, int position, CompletionInfo.Entry completion) {
        return goSync(GET_COMPLETION_DETAILS, languageService, fileName, position, completion);
    }
    
    private static final int GET_COMPLETION_DETAILS = 10;
    
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
                    ((Request<Object>)r).send(services.getLib());
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
                default:
                    throw new AssertionError("unknown request type");
            }
        } catch (Throwable t) {
            StrangeEons.log.log(Level.SEVERE, "exception while handling request" + r, t);
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
    @SuppressWarnings("unchecked")
    private <T> void go(int type, Consumer<T> callback, Object... args) {
        queue.add(new Request(type, callback, args));
    }

    /**
     * Adds a request to be handled synchronously.
     * 
     * @param <T> the return value type
     * @param type the action to perform
     * @param args the arguments needed to complete the request
     * @return the return value of the request, or null
     */    
    private <T> T goSync(int type, Object... args) {
        @SuppressWarnings("unchecked")
        Request<T> r = new Request(-type, null, args);
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
    
    private static Thread workerThread;
    private static BlockingQueue<Request> queue = new LinkedBlockingQueue<>();
    private SEScriptEngine engine;
    private ServiceInterface services;
    
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
            String s = "Request{type=" + Math.abs(type)
                    + ", synch=" + (type < 0)
                    + ", cb=" + callback
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
    
    /**
     * Interfaces that bridges access from the Java wrapper for TypeScript
     * services to the JavaScript implementation.
     */
    static interface ServiceInterface {
        /** Returns the raw TS lib object for development/debugging. */
        Object getLib();
        
        /** Returns the version of TypeScript services. */
        String getVersion();

        /** Returns a simple transpilation of a single file with no checking. */
        String transpile(String fileName, String text);

        /** Creates a script snapshot from the text of a script. */
        Object createSnapshot(String text);
        
        /** Creates a language service host that delegates to the specified root. */
        Object createLanguageServiceHost(CompilationRoot root);
        
        /** Creates a language service from the specified language service host. */
        Object createLanguageService(Object host);
        
        /** Compiles a source file. */
        CompiledSource compile(Object languageService, String fileName);
        
        /** Returns a list of diagnostic messages for a file. */
        List<Diagnostic> getDiagnostics(Object languageService, String fileName, boolean includeSyntactic, boolean includeSemantic);
        
        /** Returns a collection of code completions or null. */
        CompletionInfo getCodeCompletions(Object languageService, String fileName, int position);
        
        /** Returns additional information about a code completion. */
        CompletionInfo.EntryDetails getCodeCompletionDetails(Object languageService, String fileName, int position, CompletionInfo.Entry completion);
    }
}
