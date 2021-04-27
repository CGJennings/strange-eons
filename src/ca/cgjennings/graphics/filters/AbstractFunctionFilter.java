package ca.cgjennings.graphics.filters;

import ca.cgjennings.math.Fn;

/**
 * An abstract base class for filters that apply a transfer function to the red,
 * green, and blue channels to produce a result. The alpha channel is not
 * affected.
 *
 * <p>
 * Subclasses must implement {@link #f} to compute the specific function to be
 * applied to the channels, and must call {@link #functionChanged()} whenever
 * the filter settings change in a way that affects the output of
 * <code>f</code>.
 *
 * <p>
 * The transfer function <code>f</code> is passed values between 0 and 1
 * inclusive, representing the brightness of the red, green, or blue channels
 * for a given sample (pixel). The transfer function then returns new values
 * representing how the input value should change as a result of applying the
 * filter. As a simple example, the following transfer function would brighten
 * images by 10 percent:
 * <pre>
 * public float f( float x ) {
 *     return x * 1.10f;
 * }
 * </pre>
 *
 * <p>
 * <b>In-place filtering:</b> This class supports in-place filtering (the source
 * and destination images may be the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public abstract class AbstractFunctionFilter extends AbstractPixelwiseFilter implements Fn {

    private final int[] table = new int[256];
    private boolean tableIsValid = false;

    public AbstractFunctionFilter() {
    }

    /**
     * Subclasses must implement this method to define the transfer function
     * used by the filter. The function must return a valid result for any input
     * between 0 and 1, inclusive. The value returned is normally also in this
     * range, but this is not required. (Out of range values will be clamped to
     * the nearest in-range value.)
     *
     * @param x the input value to the function, between 0 and 1 inclusive
     * @return the output value of the function
     */
    @Override
    public abstract double f(double x);

    /**
     * Subclasses must call this function if a change to the filter instance
     * would cause the transfer function to return different values. For
     * example, if the filter function defines parameters used by the transfer
     * function, this function must be called if one of those parameters
     * changes.
     */
    protected void functionChanged() {
        tableIsValid = false;
    }

    private void validateTable() {
        for (int i = 0; i < 256; i++) {
            int v = (int) (255 * f(i / 255.0f) + 0.5f);
            v = Math.max(0, Math.min(255, v));
            table[i] = v;
        }
    }

    @Override
    public void filterPixels(int[] pixels, int start, int end) {
        // must be called here so that if pixels are filtered directly
        // by the user (or, say, CompoundPixelwiseFilter), the table is valid
        if (!tableIsValid) {
            validateTable();
        }
        for (int i = start; i < end; ++i) {
            int argb = pixels[i];
            pixels[i] = (argb & 0xff00_0000)
                    | (table[(argb & 0xff_0000) >>> 16] << 16)
                    | (table[(argb & 0xff00) >>> 8] << 8)
                    | (table[argb & 0xff]);
        }
    }
}
