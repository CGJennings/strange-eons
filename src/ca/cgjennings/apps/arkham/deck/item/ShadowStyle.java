package ca.cgjennings.apps.arkham.deck.item;

/**
 * Interface implemented by items that support a drop shadow.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface ShadowStyle extends Style {

    /**
     * Returns {@code true} if a drop shadow is enabled.
     *
     * @return {@code true} if a shadow is enabled
     */
    boolean isShadowed();

    /**
     * Sets whether the drop shadow are enabled.
     *
     * @param enable if {@code true}, the object will have a drop shadow
     */
    void setShadowed(boolean enable);
}
