package ca.cgjennings.ui.textedit.completion;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.lang.ref.SoftReference;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;

/**
 * A node representing a Java package in a {@link PackageRoot}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class ClassNode extends APINode {

    /**
     * Internal representation of the class information; can be GC'd if memory
     * is low.
     */
    private static class ClassInfo {

        public ClassInfo(String fqn) throws ClassNotFoundException {
            klass = Class.forName(fqn, false, getClass().getClassLoader());
        }

        public synchronized Constructor[] getConstructors() {
            if (ctors == null) {
                ctors = klass.getConstructors();
            }
            return ctors;
        }

        private void initMethods() {
            LinkedList<Method> nonstaticm = new LinkedList<>();
            LinkedList<Method> staticm = new LinkedList<>();
            for (Method m : klass.getMethods()) {
                if (Modifier.isStatic(m.getModifiers())) {
                    staticm.add(m);
                } else {
                    nonstaticm.add(m);
                }
            }
            methods = nonstaticm.toArray(new Method[nonstaticm.size()]);
            staticMethods = staticm.toArray(new Method[staticm.size()]);
        }

        public synchronized Method[] getMethods() {
            if (methods == null) {
                initMethods();
            }
            return methods;
        }

        public synchronized Method[] getStaticMethods() {
            if (methods == null) {
                initMethods();
            }
            return staticMethods;
        }

        public synchronized String[] getParameterNames(AccessibleObject m) {
            String[] names = null;
            if (nameMap == null) {
                nameMap = new HashMap<>();
            }
            names = nameMap.get(m);
            if (names == null) {
                Class[] types = m instanceof Method ? (((Method) m).getParameterTypes()) : (((Constructor) m).getParameterTypes());
                if (types.length == 0) {
                    names = EMPTY_STRING_ARRAY;
                } else {
                    NameReader reader = null;
                    if (nameReaderRef != null) {
                        reader = nameReaderRef.get();
                    }
                    if (reader == null) {
                        reader = new NameReader();
                        nameReaderRef = new SoftReference<>(reader);
                    }
                    names = reader.lookupParameterNames(m);
                    if (names == null) {
                        names = new String[types.length];
                        HashMap<Class, Integer> counts = new HashMap<>(types.length);
                        for (int i = 0; i < types.length; ++i) {
                            Class c = types[i];
                            int n = 0;
                            if (counts.containsKey(c)) {
                                n = counts.get(c) + 1;
                                counts.put(c, n);
                            }
                            String baseName = types[i].getSimpleName().replace("[]", "Array");
                            StringBuilder b = new StringBuilder(24);
                            if (baseName.length() > 1 && Character.isUpperCase(baseName.charAt(0)) && Character.isLowerCase(baseName.charAt(1))) {
                                b.append(Character.toLowerCase(baseName.charAt(0)))
                                        .append(baseName, 1, baseName.length());
                            } else {
                                b.append(baseName);
                            }
                            b.append(n);
                            names[i] = b.toString();
                        }
                    }
                }
                nameMap.put(m, names);
            }
            return names;
        }

        public synchronized String[] getEnumNames() {
            if (enums == null) {
                Object[] vals = klass.getEnumConstants();
                if (vals == null) {
                    enums = EMPTY_STRING_ARRAY;
                } else {
                    enums = new String[vals.length];
                    try {
                        for (int i = 0; i < vals.length; ++i) {
                            Method name = vals[i].getClass().getMethod("name");
                            enums[i] = (String) name.invoke(vals[i]);
                        }
                    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        enums = EMPTY_STRING_ARRAY;
                        StrangeEons.log.log(Level.WARNING, this.toString(), e);
                    }
                }
            }
            return enums;
        }

        private void initFields() {
            LinkedList<Field> fields = new LinkedList<>();
            LinkedList<Field> sfields = new LinkedList<>();
            for (Field f : klass.getFields()) {
                if (Modifier.isStatic(f.getModifiers()) && !f.isEnumConstant()) {
                    sfields.add(f);
                } else {
                    fields.add(f);
                }
            }
            if (sfields.isEmpty()) {
                staticFields = EMPTY_FIELD_ARRAY;
            } else {
                staticFields = sfields.toArray(new Field[sfields.size()]);
            }
            if (fields.isEmpty()) {
                this.fields = EMPTY_FIELD_ARRAY;
            } else {
                this.fields = fields.toArray(new Field[fields.size()]);
            }
        }

        public synchronized Field[] getFields() {
            if (fields == null) {
                initFields();
            }
            return fields;
        }

        public synchronized Field[] getStaticFields() {
            if (fields == null) {
                initFields();
            }
            return staticFields;
        }

        private Class klass;
        private Constructor[] ctors;
        private Method[] methods;
        private Method[] staticMethods;
        private String[] enums;
        private Field[] fields;
        private Field[] staticFields;
        private HashMap<AccessibleObject, String[]> nameMap;
    }
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final Field[] EMPTY_FIELD_ARRAY = new Field[0];
    private static SoftReference<NameReader> nameReaderRef;

    private SoftReference<ClassInfo> classInfoRef;

    /**
     * Creates a new class node.
     *
     * @param name the class name
     */
    ClassNode(String name) {
        super(name);
    }

//	@Override
//	public Set<APINode> children() {
//
//		// generate children dynamically from the class
//
//		return super.children();
//	}
    private synchronized ClassInfo getInfo() {
        ClassInfo ci = null;
        if (classInfoRef != null) {
            ci = classInfoRef.get();
        }
        if (ci == null) {
            try {
                ci = new ClassInfo(getFullyQualifiedNameInternal());
            } catch (ClassNotFoundException e) {
                StrangeEons.log.log(Level.WARNING, getFullyQualifiedNameInternal(), e);
                try {
                    ci = new ClassInfo("java.lang.Object");
                } catch (ClassNotFoundException e2) {
                    throw new AssertionError();
                }
            }
            classInfoRef = new SoftReference<>(ci);
        }
        return ci;
    }

    /**
     * Returns the Java {@link Class} object for the class represented by this
     * class node.
     *
     * <p>
     * Do not confuse this with <code>getClass()</code>, which will return the
     * class object for the <code>ClassNode</code> class itself.
     *
     * @return the class object for the class represented by this node
     */
    public Class getJavaClass() {
        return getInfo().klass;
    }

    /**
     * Returns an array of the public constructors of the class represented by
     * this node. The returned array may be shared with other callers. It must
     * not be modified.
     *
     * @return an array of the class constructors
     */
    public Constructor[] getConstructors() {
        return getInfo().getConstructors();
    }

    /**
     * Returns an array of the public, non-static methods of the class
     * represented by this node. The returned array may be shared with other
     * callers. It must not be modified.
     *
     * @return an array of the class constructors
     */
    public Method[] getMethods() {
        return getInfo().getMethods();
    }

    /**
     * Returns an array of the public, static methods of the class represented
     * by this node. The returned array may be shared with other callers. It
     * must not be modified.
     *
     * @return an array of the class constructors
     */
    public Method[] getStaticMethods() {
        return getInfo().getStaticMethods();
    }

    /**
     * Returns an array of the public fields of the class represented by this
     * node. The returned array may be shared with other callers. It must not be
     * modified.
     *
     * @return an array of static fields
     */
    public Field[] getFields() {
        return getInfo().getFields();
    }

    /**
     * Returns an array of the public, static fields of the class represented by
     * this node. The returned array may be shared with other callers. It must
     * not be modified.
     *
     * @return an array of static fields
     */
    public Field[] getStaticFields() {
        return getInfo().getStaticFields();
    }

    /**
     * This method returns parameter names for the parameters of one of the
     * methods or constructors of the class represented by this node. If the
     * compiled class is available, and was compiled with debugging information,
     * the actual parameter names will be returned. Otherwise, parameter names
     * will be generated based on the parameter types. The returned array may be
     * shared with other callers. It must not be modified.
     *
     * @param m the method or constructor to return parameter names for
     * @return an array of parameter names
     */
    public String[] getParameterNames(AccessibleObject m) {
        if (m == null) {
            throw new NullPointerException("m");
        }
        if (!(m instanceof Method) && !(m instanceof Constructor)) {
            throw new IllegalArgumentException("not a method or constructor: " + m);
        }
        return getInfo().getParameterNames(m);
    }

    /**
     * If the class represented by this node is an enumeration, returns the
     * names of the enumeration values. If the class is not an enumeration,
     * returns an empty array.
     *
     * @return the members of this enumeration, or an empty array
     */
    public String[] getEnumNames() {
        return getInfo().getEnumNames();
    }
}
