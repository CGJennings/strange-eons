package ca.cgjennings.apps.arkham.plugins.engine;

import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.NativeJavaPackage;
import org.mozilla.javascript.NativeJavaTopPackage;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Synchronizer;

/**
 * Functions to be placed in the global scope of script engines.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.3
 */
final class Globals {

    private Globals() {
    }

    private static final String[] NAMES;

    static {
        LinkedList<String> names = new LinkedList<>();
        for (Method m : Globals.class.getDeclaredMethods()) {
            final int mod = m.getModifiers();
            if (Modifier.isStatic(mod) && Modifier.isPublic(mod)) {
                names.add(m.getName());
            }
        }
        NAMES = names.toArray(new String[names.size()]);
    }

    private static void defineConst(ScriptableObject global, String name, Scriptable value) {
        global.defineConst(name, global);
        global.putConst(name, global, value);
    }

    private static void define(ScriptableObject global, String name, Scriptable value) {
        global.put(name, global, value);
    }

    private static void defineAlias(ScriptableObject global, String name, String alias) {
        define(global, alias, (Scriptable) global.get(name, global));
    }

    private static void defineClasses(ScriptableObject global, Class<?>... classes) {
        for (Class c : classes) {
            final NativeJavaClass njc = new NativeJavaClass(global, c, false);
            define(global, c.getSimpleName(), njc);
        }
    }

    /**
     * Adds the methods in this class as functions in the specified global.
     */
    static void defineIn(ImporterTopLevel global) {
        // add static public methods in this class as functions
        global.defineFunctionProperties(NAMES, Globals.class, ScriptableObject.DONTENUM);
        // add some standard variables, except for values like "globalThis",
        // which can only be defined once a particular global global exists;
        // see SEScriptEngine.createScriptableForContext

    }

    /**
     * Wraps a script function in a synchronizer that calls the function as if
     * wrapped in {@code synchronzied(thisObj) { ... }}.
     */
    public static Function sync(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        if (args.length == 1 && args[0] instanceof Function) {
            return new Synchronizer((Function) args[0]);
        }
        throw Context.reportRuntimeError("not a function");
    }

    /**
     * Stops the running script by throwing a special exception recognized by
     * {@link ScriptMonkey}.
     */
    public static void exit() {
        throw new BreakException();
    }

    @SuppressWarnings("serial")
    private static class BreakException extends ThreadDeath {
    }
}
