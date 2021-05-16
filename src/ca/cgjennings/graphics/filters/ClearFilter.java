package ca.cgjennings.graphics.filters;

import java.awt.Color;

/**
 * Sets every pixel in an image to a single ARGB value. The new value is not
 * mixed or painted onto the destination image, but set explicitly. To apply a
 * solid color without disturbing the alpha channel, use a
 * {@link ColorOverlayFilter}.
 *
 * <p>
 * <b>In-place filtering:</b> This class supports in-place filtering (the source
 * and destination images may be the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class ClearFilter extends AbstractPixelwiseFilter {

    private int argb;

    /**
     * Creates a filter that fills in 0x00000000.
     */
    public ClearFilter() {
    }

    /**
     * Creates a filter that fills in the specified ARGB value.
     *
     * @param argb
     */
    public ClearFilter(int argb) {
        this.argb = argb;
    }

    /**
     * Creates a filter that uses the ARGB value specified by a
     * {@code Color} instance.
     *
     * @param c the color to obtain an ARGB value from
     */
    public ClearFilter(Color c) {
        argb = c.getRGB();
    }

    /**
     * Returns the current fill colour as an ARGB value. This can be converted
     * to a {@code Color}, if required, using code like the following:
     * {@code new Color( this.getColorRGB(), true )}.
     *
     * @return the ARGB (sRGB) value of the fill colour
     */
    public int getColorRGB() {
        return argb;
    }

    /**
     * Sets the current fill colour from an ARGB value.
     *
     * @param argb the new colour value in 0xAARRGGBB format
     */
    public void setColorRGB(int argb) {
        this.argb = argb;
    }

    @Override
    public void filterPixels(int[] argb, int start, int end) {
        final int c = this.argb;
        for (int i = start; i < end; ++i) {
            argb[i] = c;
        }
    }
}
