package ca.cgjennings.ui.anim;

import ca.cgjennings.math.Fn;
import ca.cgjennings.math.Interpolation;

/**
 * A filter for composers that adjusts the timing of the filtered composer
 * according to a function. Given an unshifted position value, the function must
 * return the position value after shifting. The shifted values are not clamped
 * to to range 0..1 as this simplifies the writing of some animations. Likewise,
 * it is allowed to repeat the same value at different times in the shifted
 * animation. When the shift function exhibits either behaviour, it is up to the
 * user to ensure that the underlying composer handles them correctly.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class TimeShiftedComposer extends AbstractFilteredComposer {

    private Fn timeFunction;

    /**
     * Creates a new time shifted composer that wraps the specified animation. A
     * default timing function that applies an ease-in/ease-out to the animation
     * will be used.
     *
     * @param animation the animation to modify the timing of
     */
    public TimeShiftedComposer(Animation animation) {
        this(createDefaultFunction(), animation);
    }

    /**
     * Creates a new time shifted composer that wraps the specified animation. A
     * default timing function that applies an ease-in/ease-out to the animation
     * will be used.
     *
     * @param composer the composer to modify the timing of
     */
    public TimeShiftedComposer(FrameComposer composer) {
        this(createDefaultFunction(), composer);
    }

    private static Fn createDefaultFunction() {
        return new Interpolation.CubicSpline(
                new double[]{0.0d, 0.15d, 0.85d, 1d},
                new double[]{0.0d, 0.05d, 0.95d, 1d}
        );
    }

    /**
     * Creates a new time shifted composer that wraps the specified animation.
     *
     * @param timeFunction the function used to modify the timing of the
     * animation
     * @param animation the animation to modify the timing of
     */
    public TimeShiftedComposer(Fn timeFunction, Animation animation) {
        super(animation);
        setShiftFunction(timeFunction);
    }

    /**
     * Creates a new time shifted composer that wraps the specified animation.
     *
     * @param timeFunction the function used to modify the timing of the
     * animation
     * @param composer the composer to modify the timing of
     */
    public TimeShiftedComposer(Fn timeFunction, FrameComposer composer) {
        super(composer);
        setShiftFunction(timeFunction);
    }

    /**
     * Returns the time shifting function to be applied to the animation.
     *
     * @return the time shift function
     * @see #setShiftFunction(ca.cgjennings.math.Fn)
     */
    public final Fn getShiftFunction() {
        return timeFunction;
    }

    /**
     * Sets the time shifting function to be applied to the animation. This
     * function will be passed the original timing position as a value between 0
     * and 1. It should return a new value, also between 0 and 1, that the
     * original value maps to. For example, the function
     * {@code f(x)&nbsp;=&nbsp;1-x} would play the original animation in
     * reverse.
     *
     * @see #getShiftFunction()
     */
    public final void setShiftFunction(Fn f) {
        if (f == null) {
            throw new NullPointerException("f");
        }
        timeFunction = f;
    }

    /**
     * Creates a time shift function that uses cubic spline interpolation to
     * create an interpolator that maps the unshifted times in
     * {@code inputTimes} to the elements with the same index in
     * {@code outputTimes}. The elements of {@code inputTimes} must be monotone
     * increasing values (sorted into increasing order and with no repeats).
     *
     * @param inputTimes positions of key frames in unshifted time
     * @param outputTimes the positions to map those key frames to
     */
    public void setShiftFunction(float[] inputTimes, float[] outputTimes) {
        if (inputTimes == null) {
            throw new NullPointerException("inputTimes");
        }
        if (outputTimes == null) {
            throw new NullPointerException("outputTimes");
        }
        if (inputTimes.length != outputTimes.length) {
            throw new IllegalArgumentException("arguments must have equal length");
        }
        double[] x = new double[inputTimes.length];
        for (int i = 0; i < inputTimes.length; ++i) {
            x[i] = inputTimes[i];
        }
        double[] y = new double[outputTimes.length];
        for (int i = 0; i < outputTimes.length; ++i) {
            y[i] = outputTimes[i];
        }
        setShiftFunction(new Interpolation.CubicSpline(x, y));
    }

    @Override
    public void composeFrame(float position) {
        getComposer().composeFrame((float) timeFunction.f(position));
    }
}
