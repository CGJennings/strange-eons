package ca.cgjennings.apps.arkham.plugins.engine;

import ca.cgjennings.apps.arkham.plugins.debugging.ScriptDebugging;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Objects;
import javax.script.*;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Implementation of {@code ScriptEngine} for Strange Rhino, the modified
 * Mozilla Rhino build used for Strange Eons scripts.
 */
public final class SEScriptEngine extends AbstractScriptEngine implements Invocable, Compilable {

    /**
     * Factory that created this engine.
     */
    private final SEScriptEngineFactory factory;
    /**
     * Top-level scope where standard JavaScript objects are defined, above even
     * the scopes defined by a given {@link ScriptConext}.
     */
    private final ScriptableObject topLevel;

    /**
     * Creates a new script engine.
     */
    SEScriptEngine(SEScriptEngineFactory factory) {
        final long start = ScriptEngineMetrics.ENABLED ? System.nanoTime() : 0L;
        this.factory = Objects.requireNonNull(factory);
        // wrap bindings created by AbstractScriptEngine
        Bindings raw = context.getBindings(ScriptContext.ENGINE_SCOPE);
        context.setBindings(new SettingBindings(raw), ScriptContext.ENGINE_SCOPE);
        // Per-engine ImporterTopLevel sits between the per-eval BindingsScriptable
        // and SEScriptEngineFactory.SHARED_TOP. It receives importClass / importPackage
        // mutations (so per-engine name imports don't leak across engines), while
        // standard objects, Globals, and the ClassCache are inherited from SHARED_TOP
        // via the prototype chain — paying the per-engine setup cost only for an
        // empty ImporterTopLevel rather than a full Context.initStandardObjects.
        ImporterTopLevel engineTop = new ImporterTopLevel();
        engineTop.setPrototype(SEScriptEngineFactory.SHARED_TOP);
        this.topLevel = engineTop;
        if (ScriptEngineMetrics.ENABLED) {
            ScriptEngineMetrics.engineCreated(System.nanoTime() - start);
        }
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return factory;
    }

    @Override
    public Object eval(Reader reader, ScriptContext scriptContext) throws ScriptException {
        final String fileName = EngineUtilities.fileNameFrom(scriptContext);
        final long start = ScriptEngineMetrics.ENABLED ? System.nanoTime() : 0L;

        final String source;
        try {
            source = readFully(reader);
        } catch (IOException ioex) {
            throw EngineUtilities.convertException(ioex, fileName);
        }

        Object retval;
        ScriptDebugging.prepareToEnterContext();
        final Context cx = Context.enter();
        try {
            Script script = CompiledScriptCache.getOrCompile(cx, source, fileName);
            Scriptable scope = createScriptableForContext(scriptContext);
            retval = script.exec(cx, scope);
        } catch (RhinoException rex) {
            throw EngineUtilities.convertException(rex);
        } finally {
            Context.exit();
            if (ScriptEngineMetrics.ENABLED) {
                ScriptEngineMetrics.evalRecorded(fileName, System.nanoTime() - start);
            }
        }
        return EngineUtilities.unwrapJsObject(retval);
    }

    private static String readFully(Reader r) throws IOException {
        StringBuilder sb = new StringBuilder(8192);
        char[] buf = new char[8192];
        int n;
        while ((n = r.read(buf)) >= 0) {
            sb.append(buf, 0, n);
        }
        return sb.toString();
    }

    @Override
    public Object eval(String script, ScriptContext scriptContext) throws ScriptException {
        Objects.requireNonNull(script, "script");
        final String fileName = EngineUtilities.fileNameFrom(scriptContext);
        return eval(preprocessScript(fileName, new StringReader(script)), scriptContext);
    }

    /**
     * Create a new bindings instance, used to define script values in a
     * {@link ScriptContext} to make them available to a running script. This
     * will return bindings that can correctly handle keys that link to setting
     * values and translated strings.
     *
     * @return a map of values that can be placed in a script's scope
     */
    @Override
    public Bindings createBindings() {
        return new SettingBindings(new SimpleBindings());
    }

    @Override
    public Object invokeFunction(String name, Object... args)
            throws ScriptException, NoSuchMethodException {
        return invokeMethod(null, name, args);
    }

    @Override
    public Object invokeMethod(Object thiz, String name, Object... args)
            throws ScriptException, NoSuchMethodException {

        final Context cx = Context.enter();
        try {
            if (name == null) {
                throw new NullPointerException("method name is null");
            }

            if (thiz != null && !(thiz instanceof Scriptable)) {
                thiz = Context.toObject(thiz, topLevel);
            }

            Scriptable engineScope = createScriptableForContext(context);
            Scriptable localScope = (thiz != null) ? (Scriptable) thiz
                    : engineScope;
            Object obj = ScriptableObject.getProperty(localScope, name);
            if (!(obj instanceof Function)) {
                throw new NoSuchMethodException(name);
            }

            Function func = (Function) obj;
            Scriptable scope = func.getParentScope();
            if (scope == null) {
                scope = engineScope;
            }

            Object[] jsArgs = args == null ? Context.emptyArgs : new Object[args.length];
            for (int a = 0; a < jsArgs.length; ++a) {
                jsArgs[a] = Context.javaToJS(args[a], topLevel);
            }

            Object result = func.call(cx, scope, localScope, jsArgs);
            return EngineUtilities.unwrapJsObject(result);
        } catch (RhinoException rex) {
            throw EngineUtilities.convertException(rex);
        } finally {
            Context.exit();
        }
    }

    @Override
    public <T> T getInterface(Class<T> interfaceType) {
        return EngineUtilities.implement(this, null, interfaceType);
    }

    @Override
    public <T> T getInterface(Object scriptObject, Class<T> interfaceType) {
        Objects.requireNonNull("scriptObject");
        return EngineUtilities.implement(this, scriptObject, interfaceType);
    }

    /**
     * Creates a JS engine scriptable that wraps the given script context and
     * hooks it into the engine's top-level scope.
     *
     * @param scriptContext a non-null JSR 223 script context
     * @return an engine scriptable for a global scope that links to the
     * bindings in the script context
     */
    Scriptable createScriptableForContext(ScriptContext scriptContext) {
        Objects.requireNonNull(scriptContext, "scriptContext");

        BindingsScriptable newScope = new BindingsScriptable(scriptContext);
        newScope.setPrototype(topLevel);
        // Setting the parent scope is required so that ImporterTopLevel.realScope
        // can walk up to find a top-level ScriptableObject (the per-engine topLevel).
        // Without this, getTopLevelScope returns the BindingsScriptable, which is
        // not a ScriptableObject, and importClass falls back to writing on the
        // shared SHARED_TOP — leaking import names across engines.
        newScope.setParentScope(topLevel);
        // predefine some common names for the global scope:
        // Node.js "global", Web Worker "self", ECMAScript 2020 "globalThis"
        newScope.addConst("global", newScope);
        newScope.addConst("globalThis", newScope);
        newScope.addConst("self", newScope);

        return newScope;
    }

    @Override
    public CompiledScript compile(String script) throws ScriptException {
        return compile(new StringReader(script));
    }

    @Override
    public CompiledScript compile(java.io.Reader script) throws ScriptException {
        final String fileName = EngineUtilities.fileNameFrom(context);
        script = preprocessScript(fileName, script);

        final Context cx = Context.enter();
        try {
            Script executable = cx.compileReader(script, fileName, 1, null);
            return new SECompiledScript(this, executable);
        } catch (Exception ex) {
            throw EngineUtilities.convertException(ex);
        } finally {
            Context.exit();
        }
    }

    /**
     * Subclasses may override this to construct a pipeline that can access and
     * modify the script source before it is evaluated.
     *
     * @param fileName the script file name; will be a fallback name if unknown
     * @param reader a non-null reader that will produce the source text
     * @return a reader that will produce the modified text, or the original
     * reader
     * @throws ScriptException if an exception occurs while modifying the text
     */
    protected Reader preprocessScript(String fileName, Reader reader) throws ScriptException {
        return reader;
    }
}
