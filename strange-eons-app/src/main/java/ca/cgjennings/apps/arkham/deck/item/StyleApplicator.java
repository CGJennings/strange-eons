package ca.cgjennings.apps.arkham.deck.item;

/**
 * The minimal interface implemented by objects that can apply a {@link Style}
 * to other objects.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface StyleApplicator {

    /**
     * Applies style properties to the specified object by calling appropriate
     * setters defined in a {@link Style} subinterface. If the object does not
     * implement any {@link Style} interface, then this will have no effect. An
     * applicator is not required to set every style property supported by the
     * object, or to affect all objects that implement the same styles equally.
     *
     * @param o the target object to apply style information to
     */
    void apply(Object o);
}
