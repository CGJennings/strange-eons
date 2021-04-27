package ca.cgjennings.ui.anim;

/**
 * This is an abstract base class for composition filters: compositions which
 * modify the behaviour of other compositions.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public abstract class AbstractFilteredComposer implements FrameComposer {

    private FrameComposer composer;

    /**
     * Creates a filter for an existing composition.
     *
     * @param composer the composition to filter
     * @throws NullPointerException if the composer is <code>null</code>
     */
    public AbstractFilteredComposer(FrameComposer composer) {
        if (composer == null) {
            throw new NullPointerException("filteredComposition");
        }
        this.composer = composer;
    }

    /**
     * Creates a filter for an animation. This will create a filter for the
     * composition set on the animation and then replace the animation's
     * composition with this filter.
     *
     * @param animation the animation whose composer should be filtered
     * @throws NullPointerException if the animation is <code>null</code>
     */
    public AbstractFilteredComposer(Animation animation) {
        if (animation == null) {
            throw new NullPointerException("animation");
        }
        composer = animation.getComposer();
        animation.setComposer(this);
    }

    /**
     * Returns the filtered composer.
     *
     * @return the composer being immediately filtered by this composer
     */
    public FrameComposer getComposer() {
        return composer;
    }

    /**
     * Sets the composer to be filtered.
     *
     * @param composer the composer to which filtering will be applied
     * @throws NullPointerException if the composer is <code>null</code>
     */
    public void setComposer(FrameComposer composer) {
        if (composer == null) {
            throw new NullPointerException("composer");
        }
        this.composer = composer;
    }
}
