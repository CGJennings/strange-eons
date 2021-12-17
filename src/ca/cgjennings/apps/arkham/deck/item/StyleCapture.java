package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

/**
 * A style capture can collect a composite of the styles of one or more items,
 * and can optionally apply those styles to another set of items.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class StyleCapture implements Iterable<StyleCapture.Property>, StyleApplicator {

    /**
     * Encapsulates a captured style property.
     */
    public static final class Property {

        private Property(String name, Object value, Class<?> type, Class<? extends Style> style, boolean conflicted) {
            this.name = name;
            this.value = value;
            this.type = type;
            this.style = style;
            this.conflicted = conflicted;
        }

        String name;
        Object value;
        Class<?> type;
        Class<? extends Style> style;
        boolean conflicted;

        /**
         * Returns the name of this property. The property's name will be the
         * name of the getter method use to read the property from its
         * {@link Style} interface, without the "get" or "is" prefix.
         *
         * @return the name of the property
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the value of the property. This is the value of the most
         * recently captured property with this name.
         *
         * @return the property value
         */
        public Object getValue() {
            return value;
        }

        /**
         * Sets the value of the property. The class of the value object must be
         * compatible with the type of the property.
         *
         * @param value the new value to assign to the property
         */
        public void setValue(Object value) {
            try {
                getType().cast(value);
            } catch (ClassCastException cce) {
                throw new IllegalArgumentException("value has wrong type: expected " + getType().getName() + " but got " + (value != null ? value.getClass().getName() : null));
            }
            this.value = value;
        }

        /**
         * Returns the {@code Class} instance that represents the original type
         * of the property. For example, if the property being read has type
         * {@code float} then the object returned by {#link getValue()} will be
         * a {@code Float} instance and the type returned by this method will be
         * {@code float.class}.
         *
         * @return the actual type of the captured property value, as declared
         * in its getter method
         */
        public Class<?> getType() {
            return type;
        }

        /**
         * Returns the {@code Class} object for the {@link Style} interface that
         * this property belongs to.
         *
         * @return the style interface that the property is declared in
         */
        public Class<? extends Style> getStyle() {
            return style;
        }

        /**
         * Returns {@code true} if the property has had different values during
         * the life of this capture.
         *
         * @return {@code true} if different values have been captured for this
         * property
         */
        public boolean isConflicted() {
            return conflicted;
        }

        /**
         * Returns a debugging string that describes the property and its value.
         *
         * @return a string that describes this property
         */
        @Override
        public String toString() {
            return '(' + type.getSimpleName() + ") " + name + " => " + value;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 17 * hash + (this.name != null ? this.name.hashCode() : 0);
            hash = 17 * hash + (this.value != null ? this.value.hashCode() : 0);
            return hash;
        }

        /**
         * Returns {@code true} if the specified object is a {@code Property}
         * with the same name and value. (If this returns {@code true} the type
         * and style must match unless the contract for {@link Style} has been
         * violated.)
         *
         * @param obj the object to compare this property with
         * @return {@code true} if the object represents the same property and
         * has an equal value
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Property other = (Property) obj;
            if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
                return false;
            }
            return !(this.value != other.value && (this.value == null || !this.value.equals(other.value)));
        }
    }

    private Set<Class<? extends Style>> captures = new HashSet<>();
    private Set<Class<? extends Style>> conflicts = new HashSet<>();
    private Map<String, Property> props = new HashMap<>();

    /**
     * Creates a new, empty style capture.
     */
    public StyleCapture() {
    }

    /**
     * Creates a new style capture whose initial state is a capture of the
     * specified items.
     *
     * @param item the initial capture
     */
    public StyleCapture(PageItem item) {
        capture(item);
    }

    /**
     * Creates a new style capture whose initial state is a capture of the
     * specified items.
     *
     * @param items the initial capture
     */
    public StyleCapture(PageItem... items) {
        for (PageItem i : items) {
            capture(i);
        }
    }

    /**
     * Creates a new style capture whose initial state is a capture of the
     * specified items.
     *
     * @param items the initial capture
     */
    public StyleCapture(Collection<? extends PageItem> items) {
        for (PageItem i : items) {
            capture(i);
        }
    }

    /**
     * Captures style information from the specified object (typically a
     * {@link PageItem}) and merges it with the existing capture.
     *
     * @param pi the item to capture
     * @return the number of subinterfaces of the {@link Style} interface whose
     * properties were captured from the page item
     */
    @SuppressWarnings("unchecked")
    public int capture(Object pi) {
        // look for all the Style interfaces implemented by the item and
        // capture each interface in turn
        int classes = 0;
        Class<?> superclass = pi.getClass();
        while (superclass != null) {
            for (Class<?> iface : superclass.getInterfaces()) {
                if (Style.class != iface && Style.class.isAssignableFrom(iface)) {
                    capture(pi, (Class<? extends Style>) iface);
                    ++classes;
                }
            }
            superclass = superclass.getSuperclass();
        }
        return classes;
    }

    /**
     * Capture the properties of the specified Style interface by reading them
     * from this item's getters and adding them to our property map.
     *
     * @param pi the item being captured
     * @param klass the Style interface to capture
     */
    private void capture(Object pi, Class<? extends Style> iface) {
        for (Method getter : iface.getDeclaredMethods()) {
            String property = null;
            if (getter.getName().startsWith("get")) {
                property = getter.getName().substring(3);
            } else if (getter.getName().startsWith("is")) {
                property = getter.getName().substring(2);
            }

            // getters have no args and start with "is" or "get"
            if (property == null || property.isEmpty() || getter.getParameterTypes().length != 0) {
                continue;
            }

            try {
                Object value = getter.invoke(pi, (Object[]) null);
                Property oldProp = props.get(property);
                boolean conflicted = detectConflict(oldProp, value);
                if (conflicted) {
                    conflicts.add(iface);
                }
                sanityCheck(oldProp, iface, getter.getReturnType());
                props.put(property, new Property(property, value, getter.getReturnType(), iface, conflicted));
            } catch (IllegalAccessException | InvocationTargetException ex) {
                StrangeEons.log.log(Level.SEVERE, null, ex);
            }
        }
        captures.add(iface);
    }

    private static boolean detectConflict(Property oldProp, Object newVal) {
        if (oldProp == null) {
            return false;
        }

        if (oldProp.conflicted) {
            return true;
        }

        if (oldProp.value == null) {
            return newVal != null;
        }

        return !oldProp.value.equals(newVal);
    }

    private static void sanityCheck(Property oldProp, Class<? extends Style> newStyle, Class<?> valueType) {
        if (oldProp == null) {
            return;
        }
        if (oldProp.style != newStyle) {
            throw new AssertionError("styles " + oldProp.style.getSimpleName() + " and " + newStyle.getSimpleName()
                    + " cannot both define property " + oldProp.name
            );
        }
        if (oldProp.type != valueType) {
            throw new AssertionError("property " + oldProp.name + " has multiple signatures: " + oldProp.type.getSimpleName() + ", " + valueType.getSimpleName());
        }
    }

    /**
     * Applies the properties in this capture to the given object. This is done
     * by calling the setters on the page item with names and signatures that
     * match the captured properties.
     *
     * @param target the item to modify
     */
    @Override
    public void apply(Object target) {
        Class<?> klass = target.getClass();

        // does this implement any of the captured interfaces?
        boolean isInstance = false;
        for (Class<? extends Style> iface : captures) {
            if (iface.isInstance(target)) {
                isInstance = true;
                break;
            }
        }
        if (!isInstance) {
            return;
        }

        // at least one captured style is present, iterate over the properties
        for (Entry<String, Property> en : props.entrySet()) {
            Property prop = en.getValue();
            if (prop.style.isInstance(target)) {
                try {
                    Method setter = klass.getMethod("set" + prop.name, prop.type);
                    try {
                        setter.invoke(target, prop.value);
                    } catch (IllegalAccessException | InvocationTargetException ex) {
                        StrangeEons.log.log(Level.SEVERE, "setting " + prop, ex);
                    }
                } catch (NoSuchMethodException ex) {
                    throw new AssertionError(prop.style.getSimpleName() + ": interface has no setter for " + prop.name);
                }
            }
        }
    }

    /**
     * Returns a set of the style classes that have been captured. For example,
     * to test if the set contains a opacity, you could use the following code:
     * <pre>
     * getCapturedStyles().contains( OpacityStyle.class );
     * </pre>
     *
     *
     * @return an immutable set of the captured styles
     */
    public Set<Class<? extends Style>> getCapturedStyles() {
        return Collections.unmodifiableSet(captures);
    }

    /**
     * Returns {@code true} if the specified style has been captured.
     *
     * @param style the style to test for
     * @return {@code true} if any of the captured items provide this style
     */
    public boolean isStyleInCapture(Class<? extends Style> style) {
        return captures.contains(style);
    }

    /**
     * As styles are captured from more than one item, if two items share a
     * common style but the properties of that style are different, then only
     * the most recently added item's style will be reflected. The capture is
     * said to have a conflict for that style.
     *
     * @param style the style to check
     * @return {@code true} if the style is in conflict
     */
    public boolean isStyleInConflict(Class<? extends Style> style) {
        return conflicts.contains(style);
    }

    /**
     * Returns a {@link Property} instance representing the captured property
     * with the given name, or {@code null} if the property has not been
     * captured.
     *
     * @param name the property name, which must be non-{@code null}
     * @return the value of the property, or {@code null} if it has not been
     * captured
     */
    public Property getProperty(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (name.isEmpty()) {
            return null;
        }

        Property value = props.get(name);
        if (value == null && Character.isLowerCase(name.charAt(0))) {
            value = props.get(Character.toUpperCase(name.charAt(0)) + name.substring(1));
        }
        return value;
    }

    /**
     * Sets the value of a property. If the property is already defined, its
     * value will be replaced (possibly creating a conflict for the style). This
     * method is used primarily to define a capture that represents a set of
     * style defaults.
     *
     * @param style the {@code Style} interface that the property belongs to
     * @param name the name of the property
     * @param value the property's new value (the type of this object must be
     * appropriate for the an object with the correct type for the property that
     * will become the propits new value
     * @throws IllegalArgumentException if the specified style class has no
     * property with the specified name
     */
    public void setProperty(Class<? extends Style> style, String name, Object value) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("empty name");
        }
        if (Character.isLowerCase(name.charAt(0))) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }

        // see if the property is already defined, in which case we can skip
        // looking up the formal type
        final Property oldProp = props.get(name);
        if (oldProp != null) {

            boolean conflicted = detectConflict(oldProp, value);
            if (conflicted) {
                conflicts.add(style);
            }
            sanityCheck(oldProp, style, oldProp.type);
            props.put(name, new Property(name, value, oldProp.type, style, conflicted));
            return;
        }

        // Property is not defined, so we need to look up the formal type of the
        // value by finding the setter method
        Class<?> valueType = null;
        final String methodName = "set" + name;
        for (Method m : style.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                Class<?>[] paramTypes = m.getParameterTypes();
                if (paramTypes.length == 1) {
                    valueType = paramTypes[0];
                    break;
                }
            }
        }
        if (valueType == null) {
            throw new IllegalArgumentException(
                    "no property " + name + " in style " + style.getSimpleName()
            );
        }

        captures.add(style);
        props.put(name, new Property(name, value, valueType, style, false));
    }

    /**
     * Returns the number of style properties in this capture.
     *
     * @return the number of captured properties
     */
    public int size() {
        return props.size();
    }

    /**
     * Returns an iterator over the captured properties.
     *
     * @return an iterator over the properties in this capture
     */
    @Override
    public Iterator<Property> iterator() {
        return props.values().iterator();
    }

    /**
     * Clears the captured information, resetting this capture to an empty
     * state.
     */
    public void clear() {
        props.clear();
        captures.clear();
        conflicts.clear();
    }

    /**
     * Returns a string containing debugging information about this capture. The
     * returned string will include all of the captured interfaces and all of
     * the captured properties and their values.
     *
     * @return a string describing what has been captured by this capture
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("Interfaces captured:\n");
        for (Class<? extends Style> iface : captures) {
            b.append(iface.getSimpleName()).append('\n');
        }

        b.append("\nStyle properties:\n");
        for (Entry<String, Property> en : props.entrySet()) {
            b.append(en.getValue()).append('\n');
        }
        return b.toString();
    }

    /**
     * Copies this style capture to the global style clipboard. The properties
     * of the captured style classes in this capture will replace the properties
     * of the same classes in the capture.
     */
    public void copy() {
        if (props.isEmpty()) {
            return;
        }
        synchronized (StyleCapture.class) {
            if (styleClip == null) {
                styleClip = new StyleCapture();
            }
            // we don't care about maintaining accurate conflict
            // info in the clip buffer
            for (Property p : props.values()) {
                styleClip.props.put(p.name, p);
            }
            for (Class<? extends Style> c : captures) {
                styleClip.captures.add(c);
            }
        }
    }

    /**
     * Applies the styles in the global style clipboard to the specified
     * objects. If no styles have been copied into the global style clipboard,
     * this method does nothing.
     *
     * @param destination the objects to apply the style clipboard styles to
     */
    public synchronized static void paste(Object... destination) {
        if (styleClip != null) {
            for (Object o : destination) {
                styleClip.apply(o);
            }
        }
    }

    /**
     * Returns {@code true} if the global style clipboard contains style
     * information.
     *
     * @return {@code true} if non-empty captures have previously been copied.
     */
    public static synchronized boolean canPaste() {
        return styleClip != null;
    }

    private static StyleCapture styleClip;
}
