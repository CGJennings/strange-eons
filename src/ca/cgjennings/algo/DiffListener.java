package ca.cgjennings.algo;

/**
 * A listener that receives results from the {@link Diff} algorithm.
 *
 * @param <E> the type of the elements that may be inserted, deleted, or
 * changed; for example, in a text file each element might be a
 * {@code String} representing a single line
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public interface DiffListener<E> {

    /**
     * Called to indicate that an element was copied without change from the
     * original to the changed sequence.
     *
     * @param original the array or list that was passed to
     * {@link Diff#findChanges} as the parameter named {@code original}
     * @param changed the array or list that was passed to
     * {@link Diff#findChanges} as the parameter named {@code changed}
     * @param originalIndex the index in the original sequence at which this
     * event occurs
     * @param element the original element
     */
    public void unchanged(Object original, Object changed, int originalIndex, E element);

    /**
     * Called to indicate that an element has been inserted to create the
     * changed sequence.
     *
     * @param original the array or list that was passed to
     * {@link Diff#findChanges} as the parameter named {@code original}
     * @param changed the array or list that was passed to
     * {@link Diff#findChanges} as the parameter named {@code changed}
     * @param originalIndex the index in the original sequence at which this
     * event occurs
     * @param insertedelement the element from the changed sequence that must be
     * inserted
     */
    public void inserted(Object original, Object changed, int originalIndex, E insertedelement);

    /**
     * Called to indicate that an element has been removed to create the changed
     * sequence.
     *
     * @param original the array or list that was passed to
     * {@link Diff#findChanges} as the parameter named {@code original}
     * @param changed the array or list that was passed to
     * {@link Diff#findChanges} as the parameter named {@code changed}
     * @param originalIndex the index in the original sequence at which this
     * event occurs
     * @param removedelement the element from the changed sequence that must be
     * inserted
     */
    public void removed(Object original, Object changed, int originalIndex, E removedelement);
}
