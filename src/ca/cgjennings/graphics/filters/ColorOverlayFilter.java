package ca.cgjennings.graphics.filters;

import ca.cgjennings.algo.SplitJoin;
import java.awt.Color;

/**
 * Sets every pixel to a common RGB value without modifying the alpha channel.
 * Where the alpha channel uses transparency to define a shape, the effect of
 * this filter is to overlay the shape with a solid colour. To simply set every
 * pixel to a single value, overwriting the alpha value, use a
 * {@link ClearFilter} instead. For more complex channel mixing and swapping of
 * channels, see {@link ChannelSwapFilter}.
 *
 * <p>
 * <b>In-place filtering:</b> This class supports in-place filtering (the source
 * and destination images may be the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class ColorOverlayFilter extends AbstractPixelwiseFilter {

    private int rgb = 0;
    private boolean invert = false;

    public ColorOverlayFilter() {
    }

    public ColorOverlayFilter(int rgb) {
        this.rgb = rgb;
    }

    public ColorOverlayFilter(int rgb, boolean invert) {
        this.rgb = rgb;
        this.invert = invert;
    }

    /**
     * Sets the colour to overlay as a packed integer RGB value.
     *
     * @param rgb the colour value to apply
     */
    public void setColorRGB(int rgb) {
        this.rgb = rgb;
    }

    /**
     * Returns the colour to overlay as a packed RGB value.
     *
     * @return the colour value to be applied
     */
    public int getColorRGB() {
        return rgb | 0xff00_0000; // set alpha to 255
    }

    /**
     * Sets the colour to overlay from a {@code Color} object.
     *
     * @param c the colour value to apply
     */
    public void setColor(Color c) {
        rgb = c.getRGB();
    }

    /**
     * Returns the colour to overlay as a {@code Color} object. If the
     * colour was set from a colour object, this is not guaranteed to be the
     * same instance.
     *
     * @return the colour value to be applied
     */
    public Color getColor() {
        return new Color(rgb);
    }

    /**
     * Sets whether the alpha channel will be inverted. The default is
     * {@code false}, so that the overlay is applied to the interior of the
     * shape(s) defined by the alpha channel. If set to {@code true}, the
     * effect is to apply the overlay to the exterior of that shape.
     *
     * @param invert if	{@code true}, alpha values are inverted
     */
    public void setAlphaInverted(boolean invert) {
        this.invert = invert;
    }

    /**
     * Returns {@code true} if the alpha channel values will be inverted,
     * changing the shape from the exterior to the interior of the original
     * shape.
     *
     * @return {@code true} if alpha values are inverted
     */
    public boolean isAlphaInverted() {
        return invert;
    }

    @Override
    public void filterPixels(int[] argb, int start, int end) {
        final int rgb = this.rgb & 0xff_ffff;
        if (invert) {
            for (int i = start; i < end; ++i) {
                final int p = argb[i];
                argb[i] = ((0xff00_0000 - (p & 0xff00_0000)) & 0xff00_0000) | rgb;
            }
        } else {
            for (int i = start; i < end; ++i) {
                final int p = argb[i];
                argb[i] = (p & 0xff00_0000) | rgb;
            }
        }
    }

    @Override
    public int filterPixel(int argb) {
        final int rgb = this.rgb & 0xff_ffff;
        final int a = (invert ? (0xff00_0000 - (argb & 0xff00_0000)) : argb) & 0xff00_0000;
        return a | rgb;
    }

    /**
     * Applies a colour overlay to an array of image data in place without the
     * need to create a new filter instance.
     *
     * @param pixels the array of pixel data
     * @param rgb the colour to apply
     * @param invert if {@code true}, the alpha value is inverted
     */
    static void overlay(final int[] pixels, final int width, int height, final int rgb, final boolean invert) {
        if (width * height >= Tuning.PER_ROW) {
            SplitJoin sj = SplitJoin.getInstance();
            final int n = Math.min(height, sj.getIdealSplitCount());
            final int rowsPerTask = height / n;
            final int remainder = height - n * rowsPerTask;
            Runnable[] units = new Runnable[n];
            int y = 0;
            for (int i = 0; i < n; ++i) {
                int rows = rowsPerTask;
                if (i < remainder) {
                    ++rows;
                }
                final int y0 = y;
                final int rowCount = rows;
                units[i] = () -> {
                    overlay(pixels, width, y0, rowCount, rgb, invert);
                };
                y += rows;
            }
            sj.runUnchecked(units);
        } else {
            overlay(pixels, width, 0, height, rgb, invert);
        }
    }

    private static void overlay(int[] in, int width, int y0, int rows, int rgb, boolean invert) {
        int i = y0 * width;
        final int limit = i + width * rows;
        rgb &= 0xff_ffff;

        if (invert) {
            for (; i < limit; ++i) {
                final int p = in[i];
                in[i] = ((0xff00_0000 - (p & 0xff00_0000)) & 0xff00_0000) | rgb;
            }
        } else {
            for (i = 0; i < limit; ++i) {
                final int p = in[i];
                in[i] = (p & 0xff00_0000) | rgb;
            }
        }
    }
}
