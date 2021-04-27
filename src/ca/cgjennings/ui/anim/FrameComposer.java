package ca.cgjennings.ui.anim;

/**
 * Implemented by objects that compose animations.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface FrameComposer {

    /**
     * This method is called with a value between 0 (start) and 1 (end). It
     * should set up the animation state proportionally between these two
     * conditions.
     *
     * @param position the interpolated position at which to compose the frame
     * between the start and end key frames
     */
    void composeFrame(float position);
}
