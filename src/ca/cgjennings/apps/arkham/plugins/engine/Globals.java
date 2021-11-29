package ca.cgjennings.apps.arkham.plugins.engine;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
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

    /**
     * Adds the methods in this class as functions in the specified scope.
     */
    static void defineIn(ScriptableObject scope) {
        scope.defineFunctionProperties(NAMES, Globals.class, ScriptableObject.DONTENUM);
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
}
