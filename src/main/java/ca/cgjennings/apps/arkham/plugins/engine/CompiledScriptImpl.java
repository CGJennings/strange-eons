package ca.cgjennings.apps.arkham.plugins.engine;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

final class CompiledScriptImpl extends CompiledScript {

    private final SEScriptEngine engine;
    private final Script script;

    CompiledScriptImpl(SEScriptEngine engine, Script script) {
        this.engine = engine;
        this.script = script;
    }

    @Override
    public ScriptEngine getEngine() {
        return engine;
    }

    @Override
    public Object eval(ScriptContext context) throws ScriptException {

        Object retval = null;
        Context cx = Context.enter();
        try {
            Scriptable global = engine.createScriptableForContext(context);
            retval = script.exec(cx, global);
            retval = EngineUtilities.unwrapJsObject(retval);
        } catch (RhinoException rex) {
            throw EngineUtilities.convertException(rex);
        } finally {
            Context.exit();
        }

        return retval;
    }
}
