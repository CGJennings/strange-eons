package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.plugins.engine.SettingBindings;
import ca.cgjennings.apps.arkham.plugins.engine.SEScriptEngineFactory;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsAppWindow;
import ca.cgjennings.apps.arkham.TextEncoding;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.dialog.prefs.Preferences;
import ca.cgjennings.apps.arkham.plugins.debugging.ScriptDebugging;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.RhinoException;
import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.logging.Level;
import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import resources.Language;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * A {@code ScriptMonkey} manages the execution of a script from Strange Eons.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class ScriptMonkey {

    private static ScriptConsole console;

    private final ScriptEngine engine;

    /**
     * If a component's private settings set this key, then the script it
     * contains will be executed when the component is installed in an editor.
     */
    public static final String ON_INSTALL_EVENT_KEY = "on-install";
    /**
     * If a component's private settings set this key, then {@code Sheet}s
     * evaluate the script it contains and call the function
     * {@code onPaint( Graphics2D, GameComponent, Sheet )}.
     */
    public static final String ON_PAINT_EVENT_KEY = "on-paint";

    /**
     * A boolean setting key that stores whether the script output console
     * should be cleared before running a script. This applies to user-initiated
     * actions, like right-clicking on a script editor and choosing <b>Run</b>.
     * It has no effect in other circumstances, such as when plug-ins are being
     * loaded.
     */
    public static final String CLEAR_CONSOLE_ON_RUN_KEY = "clear-script-console";

    /**
     * Creates a new {@code ScriptMonkey} that can be used to execute script
     * code.
     *
     * @param scriptFileName the identifier to associate with the code that will
     * be executed; this will be reported as the script's file name in error
     * messages
     * @see #setInternalFileName(java.lang.String)
     */
    public ScriptMonkey(String scriptFileName) {
        engine = createScriptEngine();
        engine.put(VAR_FILE, scriptFileName);
        setInternalFileName(scriptFileName);

        ScriptContext context = engine.getContext();
        context.setWriter(console.getWriter());
        context.setErrorWriter(console.getErrorWriter());
    }

    /**
     * The script engine tracks two identifiers for the source of the script.
     * One is the "sourcefile" variable available to the script. When a plug-in
     * is running, this will normally be the same name as the other, true,
     * identifier. But when running a script "on demand", as from the project
     * <b>Run</b> command or the <b>Quickscript</b> window, then the
     * {@code ScriptMonkey} will typically be constructed using the file name
     * "Quickscript", because many scripts check for this and change their
     * behaviour accordingly. (For example, the source code for a plug-in might
     * run a test of the plug-in functionality.) However, in this case the
     * script will also report the identifier "Quickscript" in error messages,
     * which may not be ideal. This method can be used to set the internal name
     * to a different value than the one available to the script, so that error
     * messages display a more appropriate name.
     *
     * @param name the internal identifier to use when the script is evaluated
     */
    public void setInternalFileName(String name) {
        engine.put(ScriptEngine.FILENAME, name);
        
        // for CommonJS compatibility, define __dirname and __filename;
        // we don't know if this is a system file path or URL-style path,
        // so to determine the directory we first assume '/' and if that
        // doesn't work, try the file system separator instead
        String __dirname, __filename;
        final int slash = name.lastIndexOf('/');        
        if (slash < 0) {
            __dirname = "";
            __filename = name;
        } else {
            __dirname = name.substring(0, slash);
            __filename = name.substring(slash + 1);
        }        
        engine.put("__dirname", __dirname);
        engine.put("__filename", __filename);
    }

    private static void updateScriptEngineOpimizationSettings() {
        Settings s = Settings.getShared();

        int optLevel = s.getInt("script-optimization-level");
        if (optLevel < -1) {
            optLevel = -1;
        }
        if (optLevel > 9) {
            optLevel = 9;
        }
        SEScriptEngineFactory.setOptimizationLevel(optLevel);
        SEScriptEngineFactory.setWarningsEnabled(s.getYesNo("script-warnings"));
    }

    static {
        // initialize the script system
        if (!EventQueue.isDispatchThread()) {
            throw new AssertionError("script system must be initialized in EDT");
        }

        // load the initial optimization settings
        updateScriptEngineOpimizationSettings();

        // listen for changes to the optimization settings
        Preferences.addPreferenceUpdateListener(ScriptMonkey::updateScriptEngineOpimizationSettings);

        // create the shared console
        console = new ScriptConsole(StrangeEons.getWindow());
        console.setIconImages(StrangeEonsAppWindow.getApplicationFrameIcons());
    }

    /**
     * Returns the shared script console.
     *
     * @return the script output window
     */
    public static ScriptConsole getSharedConsole() {
        return console;
    }

    /**
     * The reserved variable name for the
     * {@linkplain StrangeEons application instance} ({@code Eons})
     */
    public static final String VAR_APPLICATION = "Eons";
    /**
     * The reserved variable name for the script's
     * {@link PluginContext PluginContext} ({@code PluginContext}).
     */
    public static final String VAR_CONTEXT = "PluginContext";
    /**
     * The reserved variable name for the active (edited) game component
     * ({@code Component}).
     */
    public static final String VAR_COMPONENT = "Component";
    /**
     * The reserved variable name for the active (selected) editor
     * ({@code Editor}).
     */
    public static final String VAR_EDITOR = "Editor";
    /**
     * The reserved variable name for the source file name ({@code sourcefile}).
     */
    public static final String VAR_FILE = "sourcefile";

    /**
     * The standard encoding for script code stored in a file (UTF-8).
     */
    public static final Charset SCRIPT_FILE_ENCODING = TextEncoding.SCRIPT_CODE_CS;

    /**
     * Returns an object that implements a Java interface by calling script
     * functions. Functions that are not defined in the evaluated script code
     * will do nothing. If such missing functions return a value in the
     * specified interface, they will return zero if the return type is a
     * primitive numeric type, or {@code null} otherwise.
     *
     * @param <T> the type of the implemented interface
     * @param klass the class value of the interface to implement
     * @return an instance of the requested class that is implemented by script
     * code
     */
    public <T> T implement(Class<T> klass) {
        return ((Invocable) engine).getInterface(klass);
    }

    /**
     * Evaluates a script from a URL. Any errors will be displayed on the script
     * console.
     *
     * @param url the URL that contains script code to execute
     * @return the return value of the script code, if any
     */
    public Object eval(URL url) {
        if (url == null) {
            throw new NullPointerException("url");
        }
        Object result = null;
        InputStream in = null;
        try {
            in = url.openStream();
            result = eval(new BufferedReader(new InputStreamReader(in, SCRIPT_FILE_ENCODING)));
        } catch (Exception e) {
            StrangeEons.log.log(Level.WARNING, "evaluation failed: " + engine.get(ScriptEngine.FILENAME), e);
            scriptError(e);
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    StrangeEons.log.log(Level.WARNING, "close", e);
                }
            }
        }
        return result;
    }

    /**
     * Evaluates a script from a reader. Any errors will be displayed on the
     * script console.
     *
     * @param r the reader that contains script code to execute
     * @return the return value of the script code, if any
     */
    public Object eval(Reader r) {
        try {
            return evaluate(r);
        } catch (ScriptException se) {
            StrangeEons.log.log(Level.WARNING, "evaluation failed: " + engine.get(ScriptEngine.FILENAME), se);
            scriptError(se);
            return null;
        }
    }

    /**
     * Evaluates a script stored in a string. Any errors will be displayed on
     * the script console.
     *
     * @param s the script code to execute
     * @return the return value of the script code, if any
     */
    public Object eval(String s) {
        try {
            return evaluate(s);
        } catch (ScriptException se) {
            StrangeEons.log.log(Level.WARNING, "evaluation failed: " + engine.get(ScriptEngine.FILENAME), se);
            scriptError(se);
            return null;
        }
    }

    /**
     * Call a script function, returning {@code null} without displaying an
     * error if the method does not exist. If a script error occurs, an error
     * message is displayed and {@code null} is returned.
     *
     * @param method the name of a script function
     * @param args the arguments to pass to the function
     * @return the return value of the function
     */
    public Object ambivalentCall(String method, Object... args) {
        Invocable inv = (Invocable) engine;
        try {
            return inv.invokeFunction(method, args);
        } catch (ScriptException se) {
            scriptError(se);
            return null;
        } catch (NoSuchMethodException nsm) {
            return null;
        }
    }

    /**
     * Call a script function. If the function does not exist, an error message
     * is displayed and a {@code NoSuchMethodException} exception is returned.
     * If a script error occurs, the error is printed on the console and
     * returned. Otherwise, the return value of the function (or {@code null})
     * is returned.
     *
     * @param method the name of a script function
     * @param args the arguments to pass to the function
     * @return the return value of the function
     */
    public Object call(String method, Object... args) {
        Invocable inv = (Invocable) engine;
        try {
            return inv.invokeFunction(method, args);
        } catch (ScriptException se) {
            scriptError(se);
            return se;
        } catch (NoSuchMethodException nsm) {
            StrangeEons.log.log(Level.WARNING, "no such method in script {0}", method);
            ErrorDialog.displayError(string("rk-err-script-method", method), null);
            return nsm;
        }
    }

    /**
     * Bind a value to a variable name in the global scope of the script.
     *
     * @param name the variable name to bind
     * @param object the value to assign to the variable name
     * @throws NullPointerException if the variable name is {@code null}
     * @throws IllegalArgumentException if the variable name starts with a
     * reserved character ($, @, #)
     */
    public void bind(String name, Object object) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (!name.isEmpty()) {
            char ch = name.charAt(0);
            if (ch == '$' || ch == '@' || ch == '#') {
                throw new IllegalArgumentException("Variable names that start with " + ch + " are reserved: " + name);
            }
        }
        engine.put(name, object);
    }

    /**
     * Binds the specified {@link PluginContext} to the global scope.
     *
     * @param pluginContext the plug-in context to bind (may be {@code null})
     * @see PluginContextFactory
     */
    public void bind(PluginContext pluginContext) {
        engine.put(VAR_CONTEXT, pluginContext);
    }

    /**
     * Handles an error as if it was an uncaught exception thrown from a script.
     *
     * @param se the script exception (or pseudo-script exception) to handle
     */
    public static void scriptError(Throwable se) {
        if (!EventQueue.isDispatchThread()) {
            final Throwable finalEx = se;
            EventQueue.invokeLater(() -> {
                ScriptMonkey.scriptError(finalEx);
            });
            return;
        }

        boolean fullStackTrace = Settings.getShared().getYesNo("script-full-exception-trace");

        ScriptConsole.ConsolePrintWriter out = getSharedConsole().getWriter();
        ScriptConsole.ConsolePrintWriter err = getSharedConsole().getErrorWriter();
        out.flush();
        err.flush();

        // check for exit() command
        String message = se.getMessage();
        if (message != null && message.contains("BreakException") && message.contains(ScriptMonkey.class.getPackage().getName())) {
            return;
        }

        // the real exception created by the script is the cause
        Throwable cause = se.getCause();
        while (cause != null) {
            if (se instanceof ScriptException || se instanceof UndeclaredThrowableException) {
                se = cause;
                cause = cause.getCause();
            } else {
                break;
            }
        }

        err.print("Uncaught ");

        if (se instanceof RhinoException) {
            RhinoException rex = (RhinoException) se;

            if (se instanceof JavaScriptException) {
                JavaScriptException jse = (JavaScriptException) se;
                err.println(jse.getValue());
            } else {
                message = rex.details();
                if (message == null) {
                    message = rex.getMessage();
                }
                if (message == null) {
                    message = rex.toString();
                }
                if (message.startsWith("Wrapped ")) {
                    message = message.substring("Wrapped ".length());
                }
                err.println(message);
                if (rex.getCause() != null) {
                    String wrappedMessage = rex.getCause().getLocalizedMessage();
                    if (wrappedMessage == null) {
                        wrappedMessage = rex.getCause().toString();
                    }
                    if (!message.equals(wrappedMessage)) {
                        err.println(wrappedMessage);
                    }
                }
            }

            // print stack trace if available, or source file and line
            String trace = rex.getScriptStackTrace();
            if (trace.length() == 0 || trace.trim().length() == 0) {
                if (rex.sourceName() != null) {
                    if (rex.lineNumber() > 0) {
                        err.println("\tat " + rex.sourceName() + ":" + rex.lineNumber());
                    } else {
                        err.println("\tat " + rex.sourceName());
                    }
                }
            } else {
                err.println(trace);
            }

            // if a column is available, print the line and pos of error
            int col = rex.columnNumber() - 1;
            if (rex.columnNumber() >= 0) {
                String source = rex.lineSource();
                if (source != null) {
                    String[] lines = source.split("$(?m)");
                    int ln = rex.lineNumber() - 1;
                    if (ln >= 0 && ln < lines.length) {
                        if (col >= lines[ln].length()) {
                            col = lines[ln].length() - 1;
                        }
                        err.println();
                        err.print(lines[ln].substring(0, col));
                        err.flush();
                        out.print(lines[ln].charAt(col));
                        out.flush();
                        err.println(lines[ln].substring(col + 1));
                        err.flush();
                    }
                }
            }

            // dump the full trace to stderr
            rex.printStackTrace(System.err);
            if (fullStackTrace) {
                rex.printStackTrace(console.getErrorWriter());
            }
            return;
        }

        // exception not intentionally made by script system?
        se.printStackTrace(err);
        if (fullStackTrace) {
            se.printStackTrace(console.getErrorWriter());
        }
    }

    /**
     * Runs a script file stored in a resource file. The script is simply
     * evaluated; no specific function is called or result captured. Returns
     * {@code true} if the script was loaded and run without error,
     * {@code false} otherwise.
     *
     * @param resource the resource file containing the script
     * @return {@code true} if a breakpoint should be set at the start of the
     * script
     */
    public static boolean runResourceScript(String resource, boolean debug) {
        InputStream in = null;
        if (resource.startsWith("script:")) {
            resource = resource.substring("script:".length());
        }

        try {
            ScriptMonkey monkey = new ScriptMonkey(resource);
            monkey.bind(PluginContextFactory.createDummyContext());

            URL url = ResourceKit.composeResourceURL(resource);
            if (url == null) {
                ErrorDialog.displayError(string("rk-err-missing-script", resource), null);
                return false;
            }

            try {
                in = url.openStream();
                StrangeEons.setWaitCursor(true); // *** must be right after prev line so we only remove it if in != null
                monkey.setBreakpoint(debug);
                monkey.evaluate(new BufferedReader(new InputStreamReader(in, SCRIPT_FILE_ENCODING)));
            } catch (IOException ioe) {
                StrangeEons.log.log(Level.WARNING, "exception reading resource", ioe);
                return false;
            }
        } catch (ScriptException se) {
            scriptError(se);
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                }
                StrangeEons.setWaitCursor(false);
            }
        }
        return true;
    }

    /**
     * Runs a resource creation script stored in a resource file. The script is
     * evaluated and then its {@code createResource()} method is invoked. The
     * result of that function is then returned. If there is an error loading or
     * running the script, a {@code null} value is returned. If the script is
     * missing, an error message is shown. If there is an error in the script,
     * it is displayed in the output console.
     *
     * @param resource the resource file containing the script
     * @return the object returned from {@code createResource()}, or
     * {@code null}
     */
    public static Object runResourceCreationScript(String resource) {
        Object createdObject = null;
        InputStream in = null;
        try {
            ScriptMonkey monkey = new ScriptMonkey(resource);
            monkey.bind(PluginContextFactory.createDummyContext());
            in = ResourceKit.class.getResourceAsStream(resource);
            if (in != null) {
                StrangeEons.setWaitCursor(true);
                monkey.evaluate(new BufferedReader(new InputStreamReader(in, SCRIPT_FILE_ENCODING)));
                createdObject = monkey.call("createResource");
            } else {
                ErrorDialog.displayError(string("rk-err-missing-script", resource), null);
            }
        } catch (ScriptException se) {
            scriptError(se);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                }
                StrangeEons.setWaitCursor(false);
            }
        }
        return createdObject;
    }

    private Object evaluate(Reader in) throws ScriptException {
        try {
            StringBuilder b = new StringBuilder();
            final int BUFFSIZE = 8192;
            char[] buff = new char[BUFFSIZE];
            int read;
            while ((read = in.read(buff)) != -1) {
                b.append(buff, 0, read);
            }
            return evaluate(b.toString());
        } catch (IOException e) {
            throw new ScriptException(e.getLocalizedMessage());
        }
    }

    private Object evaluate(String script) throws ScriptException {
        Object o = null;
        if (breakpoint) {
            breakpoint = false;
            ScriptDebugging.setBreak();
        }
        o = engine.eval(script);
        return o;
    }

    /**
     * If the debugger is enabled, sets whether a breakpoint should be set at
     * the start of the next script evaluation. This setting reverts to false
     * when the next script is evaluated. If the debugger is not enabled, this
     * method has no effect.
     *
     * @param breakpoint
     */
    public void setBreakpoint(boolean breakpoint) {
        this.breakpoint = breakpoint;
    }
    private boolean breakpoint;

    /**
     * Sets the {@link Settings} object that is used to look up settings with
     * <tt>$<i>setting_name</i></tt> syntax.
     *
     * @param settings the settings that will be visible in the global scope
     */
    public void setSettingProvider(Settings settings) {
        ((SettingBindings) engine.getBindings(ScriptContext.ENGINE_SCOPE)).setSettings(settings);
    }

    /**
     * Returns the {@link Settings} object that is used to look up settings with
     * <tt>$<i>setting_name</i></tt> syntax.
     *
     * @return the settings that are visible in the global scope
     */
    public Settings getSettingProvider() {
        return ((SettingBindings) engine.getBindings(ScriptContext.ENGINE_SCOPE)).getSettings();
    }

    /**
     * Sets the {@link Language} object that is used to look up interface
     * strings with <tt>@<i>string_key</i></tt> syntax. Passing {@code null}
     * will reset the language to {@code Language.getInterface()}.
     *
     * @param language the UI language whose strings will be visible in the
     * global scope
     */
    public void setUiLangProvider(Language language) {
        ((SettingBindings) engine.getBindings(ScriptContext.ENGINE_SCOPE)).setUILanguage(language);
    }

    /**
     * Returns the {@link Language} object that is used to look up interface
     * strings with <tt>@<i>string_key</i></tt> syntax.
     *
     * @return the interface language that is visible in the global scope
     */
    public Language getUiLangProvider() {
        return ((SettingBindings) engine.getBindings(ScriptContext.ENGINE_SCOPE)).getUILanguage();
    }

    /**
     * Sets the {@link Language} object that is used to look up game strings
     * with <tt>#<i>string_key</i></tt> syntax. Passing {@code null} will reset
     * the language to {@code Language.getGame()}.
     *
     * @param language the game language whose strings will be visible in the
     * global scope
     */
    public void setGameLangProvider(Language language) {
        ((SettingBindings) engine.getBindings(ScriptContext.ENGINE_SCOPE)).setGameLanguage(language);
    }

    /**
     * Returns the {@link Language} object that is used to look up game strings
     * with <tt>#<i>string_key</i></tt> syntax.
     *
     * @return the game language that is visible in the global scope
     */
    public Language getGameLangProvider() {
        return ((SettingBindings) engine.getBindings(ScriptContext.ENGINE_SCOPE)).getGameLanguage();
    }

    /**
     * Creates a script engine, initializes its global scope, and runs the
     * bootstrap library.
     *
     * @return an initialized script engine
     */
    private ScriptEngine createScriptEngine() {
        updateScriptEngineOpimizationSettings();
        ScriptEngine engine = SEScriptEngineFactory.getDefaultScriptEngine();

        // INSTALL SUPPORT FOR $-NOTATION
//        Bindings parentBindings = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
//        if (parentBindings == null) {
//            engine.setBindings(engine.createBindings(), ScriptContext.GLOBAL_SCOPE);
//        } else if (!(parentBindings instanceof SettingBindings)) {
//            engine.setBindings(new SettingBindings(parentBindings), ScriptContext.GLOBAL_SCOPE);
//        }
//
//        Bindings parentBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
//        if (parentBindings == null) {
//            engine.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
//        } else if (!(parentBindings instanceof SettingBindings)) {
//            engine.setBindings(new SettingBindings(parentBindings), ScriptContext.ENGINE_SCOPE);
//        }
        // EVALUATE THE BOOTSTRAP LIBRARY (WHICH DEFINES useLibrary AND IMPORTS common.js)
        if (BOOTSTRAP_LIBRARY == null) {
            final String libLocation = "/resources/libraries/bootstrap";
            try {
                BOOTSTRAP_LIBRARY = streamToString(ScriptMonkey.class.getResourceAsStream(libLocation));
            } catch (Exception e) {
                StrangeEons.log.log(Level.SEVERE, "the bootstrap library was not found at " + libLocation, e);
                throw new AssertionError(e);
            }
        }

        Bindings engineBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        engine.put("ENGINE_OBJECT", engine);
        engine.put("SCRIPT_MONKEY_OBJECT", this);

        Object oldName = engine.get(ScriptEngine.FILENAME);
        try {
            engine.put(ScriptEngine.FILENAME, "<library bootstrap>");
            engine.eval(BOOTSTRAP_LIBRARY);
        } catch (ScriptException e) {
            StrangeEons.log.log(Level.SEVERE, "bootstrap code failed", e);
            scriptError(e);
        } finally {
            engine.put(ScriptEngine.FILENAME, oldName);
        }

        engineBindings.remove("SCRIPT_MONKEY_OBJECT");
        engineBindings.remove("ENGINE_OBJECT");

        return engine;
    }
    private static String BOOTSTRAP_LIBRARY;

    /**
     * Evaluate a library file within the current script context. This method is
     * not intended for external use, although nothing prevents it. It is
     * invoked from within scripts in order to process {@code useLibrary()}
     * calls.
     *
     * @param engine the engine to add the library to
     * @param name the library's identifier, or a URL that points to it
     * @throws javax.script.ScriptException if the library evaluation results in
     * an error
     * @throws java.io.IOException if there is an error loading the library
     * @throws java.net.MalformedURLException if the URL is invalid
     */
    public static void includeLibrary(ScriptEngine engine, String name) throws ScriptException, IOException, MalformedURLException {
        String oldFile = (String) engine.get(ScriptEngine.FILENAME);
        String library = getLibrary(name);
        try {
            engine.put(ScriptEngine.FILENAME, name);
            engine.eval(library);
        } finally {
            engine.put(ScriptEngine.FILENAME, oldFile);
        }
    }

    /**
     * Returns a library as a string. If the string contains <tt>:</tt>, it is
     * treated as a URL and the library is fetched from that location.
     * Otherwise, it is assumed to be a standard library that lives in
     * <tt>resources/libraries</tt>. In this case, the ".js" extension is added
     * automatically.
     *
     * @param name the library name (or a URL)
     * @return the requested library
     * @throws java.io.IOException
     * @throws java.net.MalformedURLException
     */
    public static String getLibrary(String name) throws IOException, MalformedURLException {
        String lib;

        lib = libraryCache.get(name);
        if (lib == null) {
            if (!isLibraryNameAURL(name)) {
                URL url = ResourceKit.composeResourceURL("libraries/" + name + ".js");
                if (url == null) {
                    url = ResourceKit.composeResourceURL("libraries/" + name + ".doc");
                }
                if (url == null) {
                    throw new FileNotFoundException("missing standard library: " + name);
                }
                lib = getLibraryImpl(url, name);
                libraryCache.put(name, lib);
                // now that the library is cached, it is safe to auto-register it
                // (registration calls getLibrary to make sure the lib is reachable)
                LibraryRegistry.register(name);
            } else {
                URL url;
                // the name contains a protocol, so it is already a URL
                if (name.indexOf(':') >= 0 && !name.startsWith("res:")) {
                    url = new URL(name);
                } else {
                    url = ResourceKit.composeResourceURL(name);
                }
                lib = getLibraryImpl(url, name);
                if (ResourceKit.isResourceStatic(url)) {
                    libraryCache.put(name, lib);
                }
            }
        }

        return lib;
    }

    /**
     * Returns {@code true} if a library identifier is actually a URL. For
     * example, it would return {@code false} for "imageutils", but {@code true}
     * for "res://my/library.js".
     *
     * @param s the identifier to check
     * @return {@code true} if the identifier is not a simple library name
     */
    public static boolean isLibraryNameAURL(String s) {
        return s.indexOf(':') >= 0;
    }

    /**
     * Returns a library file in string form by reading it from a URL. The file
     * is assumed to be in UTF-8 format.
     *
     * @param source the URL of the library
     * @return a string obtained from the URL
     * @throws java.io.IOException
     */
    private static String getLibraryImpl(URL source, String name) throws IOException {
        try {
            return streamToString(source.openStream());
        } catch (NullPointerException e) {
            throw new FileNotFoundException("missing library: " + name);
        }
    }
    private static final HashMap<String, String> libraryCache = new HashMap<>();

    /**
     * Returns a string composed of the characters in an input stream. The
     * stream is closed on completion.
     */
    private static String streamToString(InputStream in) throws IOException {
        StringBuilder b = new StringBuilder();
        Reader r = null;
        try {
            r = new InputStreamReader(in, SCRIPT_FILE_ENCODING);
            int charsRead;
            char[] buffer = new char[8192];
            while ((charsRead = r.read(buffer)) > 0) {
                b.append(buffer, 0, charsRead);
            }
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (Exception e) {
                    StrangeEons.log.log(Level.WARNING, null, e);
                }
            }
        }
        return b.toString();
    }
    
    /**
     * Returns a string describing the current file and line of the script
     * running on the current thread.
     * 
     * @return the location of the currently running script, or null
     */
    public static String getCurrentScriptLocation() {
        Context cx = Context.getCurrentContext();
        if (cx == null) {
            return null;
        }
        
        ErrorReporter oldReporter = cx.getErrorReporter();
        cx.setErrorReporter(locationReporter);
        Context.reportWarning("");
        cx.setErrorReporter(oldReporter);
        String location = reportedLocation.get();
        reportedLocation.set(null);
        return location;
    }
    
    private static ThreadLocal<String> reportedLocation = new ThreadLocal<>();
    
    /** Captures current script location for {@link #getScriptLocation()} */
    private static final ErrorReporter locationReporter = new ErrorReporter() {
        @Override
        public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
            reportedLocation.set(sourceName + ':' + line);
        }

        @Override
        public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
        }

        @Override
        public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
            return null;
        }
    };
}
