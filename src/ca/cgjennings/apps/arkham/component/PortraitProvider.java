package ca.cgjennings.apps.arkham.component;

/**
 * This interface is implemented by game components that include (or may
 * include) {@link Portrait}s: images that can be configured by the user and
 * which appear on the component's sheets.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1a13
 */
public interface PortraitProvider {

    /**
     * Returns a {@code Portrait} that can be used to manipulate one of the
     * portrait images used by a component. A component may use more than one
     * portrait. The value of {@code index} indicates which portrait is desired,
     * and must be be between 0 and {@link #getPortraitCount()}-1 (inclusive).
     *
     * @param index the index of the desired portrait (0 for the primary
     * portrait)
     * @return a portrait instance that controls the requested portrait
     * @throws IndexOutOfBoundsException if the portrait index is negative or
     * greater or equal to the portrait count
     */
    public Portrait getPortrait(int index);

    /**
     * Returns the number of portraits available from this provider. The number
     * of portraits should remain fixed over the lifetime of a provider.
     *
     * @return the number of portraits that can be obtained with
     * {@link #getPortrait(int)} (may be 0)
     */
    public int getPortraitCount();
}
