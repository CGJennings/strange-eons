package ca.cgjennings.apps.arkham.dialog;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.dialog.GameComponentExplorer.GameObject;
import ca.cgjennings.apps.arkham.dialog.GameComponentExplorer.GameObjectCollection;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;

/**
 * An implementation of {@link GameObject} that handles standard game
 * components.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
class GameObjectImpl implements GameObject {

    protected AbstractGameComponentEditor parent;
    protected Object wrapped;
    protected String[] methodNames;
    protected Map<String, Method> methodMap;
    protected String[] constantNames;
    protected Map<String, Object> constantMap;

    protected GameObjectImpl() {
    }

    private GameObjectImpl(AbstractGameComponentEditor editor, Object objectToWrap) {
        parent = editor;
        wrapped = objectToWrap;
        methodMap = new HashMap<>();
        constantMap = new HashMap<>();
        filterMethods();
        filterConstants();
    }

    public static GameObject create(AbstractGameComponentEditor editor) {
        if (editor == null) {
            throw new NullPointerException("editor");
        }
        GameComponent gc = editor.getGameComponent();
        if (gc == null) {
            throw new IllegalArgumentException("editor has no game component");
        }
        return new GameObjectImpl(editor, gc);
    }

    public static GameObject create(GameObject owner, Object objectToWrap) {
        if (owner == null) {
            throw new NullPointerException("owner");
        }
        if (objectToWrap.getClass().isArray()) {
            return new ArrayGameComponent((GameObjectImpl) owner, (Object[]) objectToWrap);
        }
        return new GameObjectImpl(((GameObjectImpl) owner).parent, objectToWrap);
    }

    private void filterConstants() {
        Field[] fields = wrapped.getClass().getFields();
        for (int i = 0; i < fields.length; ++i) {
            if ((fields[i].getModifiers() & Modifier.FINAL) != 0) {
                try {
                    constantMap.put(fields[i].getName(), fields[i].get(null));
                } catch (IllegalAccessException e) {
                    throw new AssertionError("unexpected access exception reading fields");
                }
            }
        }
        Set<String> names = constantMap.keySet();
        constantNames = names.toArray(new String[names.size()]);
        Arrays.sort(constantNames);
    }

    @Override
    public String[] getConstantNames() {
        return constantNames.clone();
    }

    @Override
    public Class getConstantType(String name) {
        if (!constantMap.containsKey(name)) {
            throw new IllegalArgumentException("not a constant: " + name);
        }
        Object o = constantMap.get(name);
        if (o == null) {
            return void.class;
        }
        return wrapType(o.getClass());
    }

    @Override
    public Object getConstant(String name) {
        if (!constantMap.containsKey(name)) {
            throw new IllegalArgumentException("not a constant: " + name);
        }
        Object o = constantMap.get(name);
        if (o == null) {
            return null;
        }
        if (isWrappedType(o.getClass())) {
            return wrap(o);
        } else {
            return o;
        }
    }

    /**
     * Analyze the abstracted component and create a list of "allowed" methods,
     * filtering out any that callers should not be permitted to use.
     */
    private void filterMethods() {
        Method[] list = wrapped.getClass().getMethods();

        // start by adding all of the public, non-static methods;
        // note that only one method with a given name is allowed
        for (Method m : list) {
            int mods = m.getModifiers();
            if (!Modifier.isStatic(mods) && m.getAnnotation(Deprecated.class) == null) {
                methodMap.put(m.getName(), m);
            }
        }

        // subtract out all of the methods that are not permitted
        for (String s : FORBIDDEN) {
            methodMap.remove(s);
        }

        Set<String> keys = methodMap.keySet();
        methodNames = keys.toArray(new String[keys.size()]);
        Arrays.sort(methodNames);
    }
    /**
     * A list of method names that the user should never be allowed to call.
     */
    private static String[] FORBIDDEN = new String[]{
        "createDefaultSheets",
        "createDefaultEditor",
        "equals",
        "getClass",
        "getColumnTotalInSkillCategory",
        "getNextEventID",
        "getSheets",
        "hasChanged",
        "hasUnsavedChanges",
        "hashCode",
        "markSaved",
        "notify",
        "notifyAll",
        "setFocusAdjustmentTrustworthiness",
        "setSheets",
        "toString",
        "validate",
        "wait",
        "clone"
    };

    @Override
    public synchronized Object call(String methodName, Object... arguments) throws InvocationTargetException {
        exception = null;
        retval = null;

        Method m = methodMap.get(methodName);
        if (m != null) {
            arguments = unwrapArguments(arguments);
            try {
                if (SwingUtilities.isEventDispatchThread()) {
                    invokeDirectly(methodName, wrapped, m, arguments);
                } else {
                    invokeAndWait(methodName, wrapped, m, arguments);
                }

                if (exception != null) {
                    while (exception instanceof InvocationTargetException) {
                        if (!(exception.getCause() instanceof InvocationTargetException)) {
                            throw (InvocationTargetException) exception;
                        }
                        exception = exception.getCause();
                    }
                    if (exception instanceof IllegalAccessException) {
                        throw (IllegalAccessException) exception;
                    }
                    if (exception instanceof IllegalArgumentException) {
                        throw (IllegalArgumentException) exception;
                    }

                    exception.printStackTrace();
                    throw new AssertionError("unexpection exception: " + exception);
                }

                if (retval != null && isWrappedType(retval.getClass())) {
                    retval = wrap(retval);
                }
                return retval;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new AssertionError("unexpected security restriction: " + e);
            }
        }
        throw new IllegalArgumentException("method not available: " + methodName);
    }

    private void invokeDirectly(String methodName, Object caller, Method method, Object[] arguments) {
        try {
            retval = method.invoke(caller, arguments);
            synchronizeEditor(methodName);
        } catch (Throwable t) {
            exception = t;
        }
    }

    private void invokeAndWait(final String methodName, final Object caller, final Method method, final Object[] arguments) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                invokeDirectly(methodName, caller, method, arguments);
            });
        } catch (InvocationTargetException ite) {
            exception = ite;
        } catch (InterruptedException ie) {
            throw new AssertionError("calling thread was interrupted");
        }
    }
    /**
     * The object to be returned from {@code call}.
     */
    private Object retval;
    /**
     * An exception that occurred while calling a method.
     */
    private Throwable exception;

    private void synchronizeEditor(String methodName) {
        if (parent != null && !methodName.startsWith("get")) {
            parent.populateFieldsFromComponent();
        }
    }

    @Override
    public Class[] getArgumentTypes(String methodName) {
        Class[] types = getActualArgumentTypes(methodName);
        for (int i = 0; i < types.length; ++i) {
            types[i] = wrapType(types[i]);
        }
        return types;
    }

    @Override
    public Class[] getActualArgumentTypes(String methodName) {
        Method m = methodMap.get(methodName);
        if (m != null) {
            return m.getParameterTypes();
        }
        throw new IllegalArgumentException("method not available: " + methodName);
    }

    @Override
    public Class getReturnType(String methodName) {
        return wrapType(getActualReturnType(methodName));
    }

    @Override
    public Class getActualReturnType(String methodName) {
        Method m = methodMap.get(methodName);
        if (m != null) {
            return m.getReturnType();
        }
        throw new IllegalArgumentException("method not available: " + methodName);
    }

    protected GameObject wrap(Object o) {
        return create(this, o);
    }

    public static Object unwrap(Object o) {
        return o instanceof GameObjectImpl ? ((GameObjectImpl) o).wrapped : o;
    }

    @Override
    public Object unwrap() {
        return wrapped;
    }

    /**
     * If any element in arguments is a wrapped object, create a copy of
     * arguments that contains unwrapped versions of all wrapped objects.
     * Otherwise, return the original array.
     *
     * @param arguments the arguments to unwrap
     * @return an unwrapped verison of arguments (possibly the original array)
     */
    protected Object[] unwrapArguments(Object[] arguments) {
        boolean anyWrapped = false;
        for (int i = 0; i < arguments.length && !anyWrapped; ++i) {
            if (arguments[i] instanceof GameObjectImpl) {
                anyWrapped = true;
            }
        }

        if (anyWrapped) {
            arguments = arguments.clone();
            for (int i = 0; i < arguments.length; ++i) {
                if (arguments[i] instanceof GameObjectImpl) {
                    arguments[i] = unwrap(arguments[i]);
                }
            }
        }
        return arguments;
    }

    private Class wrapType(Class t) {
        if (isWrappedType(t)) {
            return GameObject.class;
        }
        return t;
    }

    /**
     * Returns true if a type must be wrapped in a {@code GameComponent}.
     *
     * @param type
     * @return whether to wrap the
     */
    private boolean isWrappedType(Class type) {
        if (type.isPrimitive() || type == String.class) {
            return false;
        }
        if (type.isArray()) {
            return isWrappedType(type.getComponentType());
        }
        if (type.getPackage().getName().startsWith("ca.cgjennings")) {
            return true;
        }
        return false;
    }

    @Override
    public String[] getMethodNames() {
        return methodNames.clone();
    }

    @Override
    public boolean hasMethod(String methodName) {
        return methodMap.containsKey(methodName);
    }

    private static class ArrayGameComponent extends GameObjectImpl implements GameObjectCollection {

        private final Object[] wrappedArray;

        public ArrayGameComponent(GameObjectImpl parent, Object[] array) {
            methodNames = METHOD_NAMES;
            this.parent = parent.parent;
            wrapped = array;
            wrappedArray = array;
        }

        @Override
        public boolean hasMethod(String methodName) {
            return lookup(methodName) >= 0;
        }

        @Override
        public Class[] getArgumentTypes(String methodName) {
            int i = lookup(methodName);
            if (i < 0) {
                throw new IllegalArgumentException("method not available: " + methodName);
            }
            return ARGUMENT_TYPES[i].clone();
        }

        @Override
        public Class getReturnType(String methodName) {
            int i = lookup(methodName);
            if (i < 0) {
                throw new IllegalArgumentException("method not available: " + methodName);
            }
            return RETURN_TYPES[i];
        }

        @Override
        public Object call(String methodName, Object... arguments) throws InvocationTargetException {
            // NOTE: the array implementation does not check the calling thread
            //       components should always return copies of arrays, so
            //       setting a value from another thread is considered safe.
            try {
                int i = lookup(methodName);
                if (arguments.length != ARGUMENT_TYPES[i].length) {
                    throw new IllegalArgumentException("wrong number of arguments: " + arguments.length);
                }
                switch (i) {
                    case GET:
                        return wrap(wrappedArray[intArg(arguments[0])]);
                    case SET:
                        int index = intArg(arguments[0]);
                        Object v = arguments[1];
                        if (!(v instanceof GameObjectImpl)) {
                            throw new IllegalArgumentException("argument not a GameComponent: " + v);
                        }
                        wrappedArray[index] = unwrap(v);
                        return null;
                    case LENGTH:
                        return wrappedArray.length;
                    default:
                        throw new IllegalArgumentException("method not available: " + methodName);
                }
            } catch (Throwable t) {
                throw new InvocationTargetException(t);
            }
        }

        private int intArg(Object a) {
            if (!(a instanceof Number)) {
                invalid(a);
            }
            return ((Number) a).intValue();
        }

        private void invalid(Object arg) {
            throw new IllegalArgumentException("invalid argument: " + arg);
        }

        private int lookup(String name) {
            for (int i = 0; i < methodNames.length; ++i) {
                if (methodNames[i].equals(name)) {
                    return i;
                }
            }
            return -1;
        }
        /**
         * The order is important.
         */
        private static final String[] METHOD_NAMES = new String[]{
            "get", "length", "set"
        };
        private static final int GET = 0, LENGTH = 1, SET = 2;
        private static final Class[][] ARGUMENT_TYPES = new Class[][]{
            new Class[]{int.class},
            new Class[]{},
            new Class[]{int.class, GameObject.class},};
        private static final Class[] RETURN_TYPES = new Class[]{
            GameObject.class, int.class, void.class
        };

        @Override
        public GameObject get(int index) {
            return wrap(wrappedArray[index]);
        }

        @Override
        public void set(int index, GameObject value) {
            wrappedArray[index] = unwrap(value);
        }

        @Override
        public int length() {
            return wrappedArray.length;
        }
    }
}
