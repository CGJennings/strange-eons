package ca.cgjennings.apps.arkham.plugins.engine;

import java.util.*;
import javax.script.*;
import org.mozilla.javascript.ConstProperties;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Symbol;
import org.mozilla.javascript.SymbolScriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

/**
 * A JS engine scriptable object that consults the bindings of a
 * {@link ScriptContext}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.3
 */
final class BindingsScriptable implements Scriptable, ConstProperties, SymbolScriptable {

    private final ScriptContext context;
    private final Map<Object, Object> otherProps = new HashMap<>();
    private final Set<String> constProps = new HashSet<>();
    private Scriptable parentScope;
    private Scriptable prototype;

    /**
     * Creates a new script object that wraps the specified context.
     *
     * @param context the context whose bindings will supply named properties
     */
    BindingsScriptable(ScriptContext context) {
        if (context == null) {
            throw new NullPointerException("context is null");
        }
        this.context = context;
    }

    /**
     * Helper to add a const to the scope.
     */
    void addConst(String name, Object value) {
        defineConst(name, this);
        putConst(name, this, value);
    }

    @Override
    public String getClassName() {
        return "Global";
    }

    ScriptContext getContext() {
        return context;
    }

    @Override
    public synchronized Scriptable getParentScope() {
        return parentScope;
    }

    @Override
    public synchronized void setParentScope(Scriptable parent) {
        this.parentScope = parent;
    }

    @Override
    public synchronized Scriptable getPrototype() {
        return prototype;
    }

    @Override
    public synchronized void setPrototype(Scriptable prototype) {
        this.prototype = prototype;
    }

    @Override
    public boolean hasInstance(Scriptable lhs) {
        Scriptable proto = lhs;
        while (proto != null) {
            if (proto.equals(this)) {
                return true;
            }
            proto = proto.getPrototype();
        }
        return false;
    }

    @Override
    public synchronized Object get(String name, Scriptable start) {
        if (name.isEmpty()) {
            return otherProps.getOrDefault(name, NOT_FOUND);
        }
        synchronized (context) {
            Object val = context.getAttribute(name);
            if (val == null && context.getAttributesScope(name) < 0) {
                return NOT_FOUND;
            }
            return Context.javaToJS(val, this);
        }
    }

    @Override
    public synchronized boolean has(String name, Scriptable start) {
        if (name.isEmpty()) {
            return otherProps.containsKey(name);
        }
        synchronized (context) {
            return context.getAttributesScope(name) != -1;
        }
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        if (start != this) {
            start.put(name, start, value);
            return;
        }

        synchronized (this) {
            if (name.isEmpty()) {
                otherProps.put(name, value);
                return;
            }
            synchronized (context) {
                if (!isConst(name) || get(name, start) == Undefined.instance) {
                    int scope = context.getAttributesScope(name);
                    if (scope == -1) {
                        scope = ScriptContext.ENGINE_SCOPE;
                    }
                    context.setAttribute(name, jsToJava(value), scope);
                }
            }
        }
    }

    @Override
    public void putConst(String name, Scriptable start, Object value) {
        put(name, start, value);
    }

    @Override
    public void defineConst(String name, Scriptable start) {
        synchronized (this) {
            put(name, start, Undefined.instance);
            constProps.add(name);
        }

        if (start != this) {
            if (start instanceof ConstProperties) {
                ((ConstProperties) start).defineConst(name, start);
            }
        }
    }

    @Override
    public synchronized boolean isConst(String name) {
        return constProps.contains(name);
    }

    @Override
    public synchronized void delete(String name) {
        if (name.isEmpty()) {
            otherProps.remove(name);
            return;
        }

        synchronized (context) {
            int scope = context.getAttributesScope(name);
            if (scope != -1) {
                context.removeAttribute(name, scope);
                constProps.remove(name);
            }
        }
    }

    @Override
    public synchronized Object[] getIds() {
        synchronized (context) {
            int size = otherProps.size();
            for (int scope : context.getScopes()) {
                Bindings b = context.getBindings(scope);
                size += b == null ? 0 : b.size();
            }

            Object[] ids = new Object[size];
            int i = 0;
            for (int scope : context.getScopes()) {
                Bindings b = context.getBindings(scope);
                if (b == null) {
                    continue;
                }
                for (String key : b.keySet()) {
                    ids[i++] = key;
                }
            }
            for (Object key : otherProps.keySet()) {
                ids[i++] = key;
            }

            return ids;
        }
    }

    @Override
    public Object getDefaultValue(Class<?> typeHint) {
        return ScriptableObject.getDefaultValue(this, typeHint);
    }

    /**
     * Converts JS values to Java values so values in the underlying Bindings
     * make sense when read from Java side.
     */
    private static Object jsToJava(Object jsObj) {
        // Wrapped Java objects are stored in bindings as the real Java object,
        // with some exceptions:
        //   - NativeJavaClass must stay wrapped, or importClass won't work
        //   - Wrappers around Java primitive classes must stay wrapped so
        //     so scripts can create create these explicitly for Java interop
        if (jsObj instanceof Wrapper) {
            if (jsObj instanceof NativeJavaClass) {
                return jsObj;
            }
            Object javaObj = ((Wrapper) jsObj).unwrap();
            if (javaObj instanceof Number || javaObj instanceof String
                    || javaObj instanceof Boolean || javaObj instanceof Character) {
                return jsObj;
            } else {
                return javaObj;
            }
        } else { // not-a-Java-wrapper
            return jsObj;
        }
    }

    @Override
    public synchronized Object get(int index, Scriptable start) {
        return otherProps.getOrDefault(index, NOT_FOUND);
    }

    @Override
    public synchronized boolean has(int index, Scriptable start) {
        return otherProps.containsKey(index);
    }

    @Override
    public synchronized void put(int index, Scriptable start, Object value) {
        if (start == this) {
            synchronized (this) {
                otherProps.put(index, value);
            }
        } else {
            start.put(index, start, value);
        }
    }

    @Override
    public synchronized void delete(int index) {
        otherProps.remove(index);
    }

    @Override
    public synchronized Object get(Symbol symbol, Scriptable start) {
        return otherProps.getOrDefault(symbol, NOT_FOUND);
    }

    @Override
    public synchronized boolean has(Symbol symbol, Scriptable start) {
        return otherProps.containsKey(symbol);
    }

    @Override
    public void put(Symbol symbol, Scriptable start, Object value) {
        if (start == this) {
            synchronized (this) {
                otherProps.put(symbol, value);
            }
        } else if (start instanceof SymbolScriptable) {
            ((SymbolScriptable) start).put(symbol, start, value);
        }
    }

    @Override
    public synchronized void delete(Symbol symbol) {
        otherProps.remove(symbol);
    }
}
