package ca.cgjennings.apps.arkham.plugins.engine;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.script.ScriptEngine;
import org.mozilla.javascript.ContextFactory;

/**
 * A script engine factory that provides access to the Strange Rhino scripting
 * engine via the JSR 223 scripting API. This is a lower-level API than using
 * {@link ScriptMonkey}, but a higher-level API than using Strange Rhino
 * directly.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class SEScriptEngineFactory extends ScriptEngineFactoryBase {

    private static final String ENGINE_VERSION = "1.7.14rc1p1-SE";

    private static final SEScriptEngineFactory shared = new SEScriptEngineFactory();
    private static ContextFactoryImpl globalContextFactory;
    private static final ThreadLocal<Boolean> isUtilityThread = new ThreadLocal<>();

    volatile static boolean warningsEnabled = true;
    volatile static boolean warningsAreErrors = false;
    volatile static int optimizationLevel = 0;
    volatile static boolean debugInfoEnabled = false;

    static {
        synchronized (SEScriptEngineFactory.class) {
            if (ContextFactory.hasExplicitGlobal()) {
                throw new AssertionError("global ContextFactory already set");
            }
            globalContextFactory = new ContextFactoryImpl();
            ContextFactory.initGlobal(globalContextFactory);
        }
    }

    /**
     * Creates a script engine factory for Strange Eons scripts.
     */
    public SEScriptEngineFactory() {
    }

    /**
     * Returns a standard shared instance of this factory.
     *
     * @return the standard script engine factory
     * @see #getDefaultScriptEngine()
     */
    public static SEScriptEngineFactory getDefaultFactory() {
        return shared;
    }

    /**
     * Returns a standard script engine using the shared script engine factory.
     *
     * @return a new script engine
     * @see #getDefaultFactory()
     */
    public static SEScriptEngine getDefaultScriptEngine() {
        return shared.getScriptEngine();
    }

    /**
     * Returns a new script engine created by this factory instance.
     *
     * @return a new script engine
     * @see #getDefaultScriptEngine()
     */
    @Override
    public SEScriptEngine getScriptEngine() {
        return new SEScriptEngine(this);
    }

    /**
     * Makes the current thread a "utility thread". A utility thread is a
     * separate thread used to run an extensive JavaScript-based tool. Calling
     * this alters the behaviour of all script engines created in the thread.
     * For example, engines in the thread will ignore warnings regardless of the
     * global warning preference.
     */
    public static void makeCurrentThreadAUtilityThread() {
        isUtilityThread.set(true);
    }

    /**
     * Returns whether the current thread is a standard thread or a utility
     * thread.
     *
     * @return true if the current thread is a standard thread
     */
    public static boolean isStandardThread() {
        return !isUtilityThread.get().equals(Boolean.TRUE);
    }

    /**
     * Returns a short string that describes the version of script engines
     * produced by this factory.
     *
     * @return the engine version string
     */
    public static String getVersion() {
        return ENGINE_VERSION;
    }

    /**
     * Returns {@code true} the scripting system will report warnings in
     * addition to errors.
     *
     * @return {@code true} if warnings are reported
     * @see #setWarningsEnabled(boolean)
     */
    public static boolean getWarningsEnabled() {
        return warningsEnabled;
    }

    /**
     * Sets whether the scripting system will report warnings in addition to
     * errors.
     *
     * @param warningsEnabled {@code true} if warnings are reported
     * @see #getWarningsEnabled()
     */
    public static void setWarningsEnabled(boolean warningsEnabled) {
        SEScriptEngineFactory.warningsEnabled = warningsEnabled;
    }

    /**
     * Sets whether warnings will be promoted to errors.
     *
     * @return if true, compilation warnings will prevent scripts from running
     */
    public static boolean getWarningsAreTreatedAsErrors() {
        return warningsAreErrors;
    }

    /**
     * Gets whether warnings will be promoted to errors.
     *
     * @param treatAsErrors if true, compilation warnings will prevent scripts
     * from running
     */
    public static void setWarningsAreTreatedAsErrors(boolean treatAsErrors) {
        warningsAreErrors = treatAsErrors;
    }

    /**
     * Returns the optimization level that will be applied to scripts.
     *
     * @return the current optimization level
     * @see #setOptimizationLevel(int)
     */
    public static int getOptimizationLevel() {
        return optimizationLevel;
    }

    /**
     * Sets the optimization level that will be applied to scripts. Valid values
     * include -1 (interpret only, no compilation), 0 (compile without
     * optimization), or 1 through 9 (enable successive degrees of optimization;
     * in practice not all levels may have distinct effects).
     *
     * @param optimizationLevel the new optimization level
     * @throws IllegalArgumentException if the new level is outside of the legal
     * range
     * @see #getOptimizationLevel()
     */
    public static void setOptimizationLevel(int optimizationLevel) {
        if (optimizationLevel < -1 || optimizationLevel > 9) {
            throw new IllegalArgumentException("invalid optimization level: " + optimizationLevel);
        }
        SEScriptEngineFactory.optimizationLevel = optimizationLevel;
    }

    /**
     * Returns whether debugging information will be generated for parsed
     * scripts.
     *
     * @return {@code true} if generation of debugging info is enabled
     */
    public static boolean isDebugInfoEnabled() {
        return debugInfoEnabled;
    }

    /**
     * Sets whether debugging information will be generated for parsed scripts.
     * If this is enabled, it may limit the optimization level that is
     * effectively applied to executed scripts.
     *
     * @param debugInfoEnabled {@code true} to enable generation of debugging
     * info
     */
    public static void setDebugInfoEnabled(boolean debugInfoEnabled) {
        SEScriptEngineFactory.debugInfoEnabled = debugInfoEnabled;
    }

    private static final List<String> NAMES = Collections.unmodifiableList(Arrays.asList(
            "strange-rhino", "js", "JavaScript", "javascript", "ECMAScript", "ecmascript"
    ));
    private static final List<String> MIME_TYPES = Collections.unmodifiableList(Arrays.asList(
            "application/javascript", "application/ecmascript", "text/javascript", "text/ecmascript"
    ));
    private static final List<String> FILE_EXTENSIONS = Collections.unmodifiableList(Arrays.asList(
            "js", "ajs"
    ));

    @Override
    public List<String> getNames() {
        return NAMES;
    }

    @Override
    public List<String> getExtensions() {
        return FILE_EXTENSIONS;
    }

    @Override
    public List<String> getMimeTypes() {
        return MIME_TYPES;
    }

    @Override
    public Object getParameter(String key) {
        switch (key) {
            case ScriptEngine.NAME:
                return "javascript";
            case ScriptEngine.ENGINE:
                return "Strange Rhino";
            case ScriptEngine.ENGINE_VERSION:
                return ENGINE_VERSION;
            case ScriptEngine.LANGUAGE_VERSION:
                return "ES6-like";
            case ScriptEngine.LANGUAGE:
                return "ECMAScript";
            case "THREADING":
                return "MULTITHREADED";
            default:
                throw new IllegalArgumentException("unknown key: " + key);
        }
    }

    @Override
    public String getMethodCallSyntax(String obj, String method, String... args) {
        StringBuilder b = new StringBuilder((args.length + 1) * 32);
        b.append(obj).append('.').append(method).append('(');
        for (int i = 0; i < args.length; ++i) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(args[i]);
        }
        b.append(");");
        return b.toString();
    }

    @Override
    public String getOutputStatement(String toDisplay) {
        return "print(" + toDisplay + ")";
    }

    @Override
    public String getProgram(String... statements) {
        StringBuilder b = new StringBuilder(statements.length * 32);
        for (int i = 0; i < statements.length; i++) {
            b.append(statements[i]).append(';');
        }
        return b.toString();
    }
}
