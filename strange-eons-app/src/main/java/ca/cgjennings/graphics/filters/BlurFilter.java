package ca.cgjennings.graphics.filters;

import ca.cgjennings.algo.SplitJoin;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

/**
 * A filter that performs a fast box blur operation. The operation is equivalent
 * to convolving with a kernel where each element has a value equal to the
 * inverse of the kernel size (a box blur).
 *
 * <p>
 * An option is provided to repeat the blurring process in-place. Successive box
 * blurs approximate the effect of a {@link GaussianBlurFilter}, but require
 * significantly less time as long as the number of iterations is small.
 *
 * <p>
 * The filter allows setting separate vertical and horizontal radii. The radii
 * define the width and height of a rectangle (box) that determines the extent
 * of the blur effect. The value of each pixel after filtering is determined by
 * centering the pixel within the box and then computing the average value of
 * all of the pixels within the box. By setting different values for the
 * horizontal and vertical radius, it is possible to achieve various special
 * effects, such as simulated motion blur.
 *
 * <p>
 * <b>In-place filtering:</b> This class supports in-place filtering (the source
 * and destination images can be the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see GaussianBlurFilter
 */
public class BlurFilter extends AbstractImageFilter {

    private int iterations = 1;
    private int hRad = 3;
    private int vRad = 3;
    private int[] hLut;
    private int[] vLut;
    private boolean premultiply = true;

    /**
     * Creates a blur filter with a radius of 3.
     */
    public BlurFilter() {
    }

    /**
     * Creates a blur filter with equal horizontal and vertical radii.
     *
     * @param radius the radius of the blur
     * @param iterations the number of times to repeat the blur
     *
     * @throws IllegalArgumentException if {@code radius}or {@code iterations}
     * is negative
     */
    public BlurFilter(int radius, int iterations) {
        setHorizontalRadius(radius);
        setVerticalRadius(radius);
        setIterations(iterations);
    }

    /**
     * Creates a blur filter with the specified radii.
     *
     * @param horzRadius the radius of the blur
     * @param vertRadius the radius of the blur
     * @param iterations the number of times to repeat the blur
     *
     * @throws IllegalArgumentException if either radius or {@code iterations}
     * is negative
     */
    public BlurFilter(int horzRadius, int vertRadius, int iterations) {
        setHorizontalRadius(horzRadius);
        setVerticalRadius(vertRadius);
        setIterations(iterations);
    }

    /**
     * Sets the number of times that the blurring operation should be repeated.
     * (The number of iterations may be 0, in which case no blurring will
     * actually occur.) Repeated blurring will produce successively closer
     * approximations of a Gaussian blur (but typically requires much less
     * time).
     *
     * @param iterations the number of times to apply the blur filter
     * @throws IllegalArgumentException if the number of iterations is negative
     */
    public final void setIterations(int iterations) {
        if (iterations < 0) {
            throw new IllegalArgumentException("iterations < 0: " + iterations);
        }
        this.iterations = iterations;
    }

    /**
     * Returns the number of times that the blurring operation will be repeated
     * during filtering. This value may be 0, but cannot be negative.
     *
     * @return the number of times the blur effect is applied
     */
    public final int getIterations() {
        return iterations;
    }

    /**
     * Sets the horizontal radius of the blur effect. The filter box will have a
     * width of twice this radius, plus one.
     *
     * @param hRadius the non-negative horizontal blur radius
     * @throws IllegalArgumentException if {@code hRadius} is negative
     */
    public final void setHorizontalRadius(int hRadius) {
        if (hRadius < 0) {
            throw new IllegalArgumentException("hRadius < 0: " + hRadius);
        }
        if (hRad != hRadius) {
            hRad = hRadius;
            hLut = null;
        }
    }

    /**
     * Returns the current horizontal blur radius.
     *
     * @return the non-negative horizontal blur radius
     */
    public final int getHorizontalRadius() {
        return hRad;
    }

    /**
     * Sets the vertical radius of the blur effect. The filter box will have a
     * height of twice this radius, plus one.
     *
     * @param vRadius the non-negative vertical blur radius
     * @throws IllegalArgumentException if {@code vRadius} is negative
     */
    public final void setVerticalRadius(int vRadius) {
        if (vRadius < 0) {
            throw new IllegalArgumentException("vRadius < 0: " + vRadius);
        }
        if (vRad != vRadius) {
            vRad = vRadius;
            vLut = null;
        }
    }

    /**
     * Returns the current vertical blur radius.
     *
     * @return the non-negative vertical blur radius
     */
    public final int getVerticalRadius() {
        return vRad;
    }

    /**
     * Sets the blur radius of the filter. This is a convenience method that
     * sets both the horizontal and vertical radii to the same value.
     *
     * @param radius the non-negative blur radius
     * @throws IllegalArgumentException if {@code radius} is negative
     */
    public final void setRadius(int radius) {
        setHorizontalRadius(radius);
        setVerticalRadius(radius);
    }

    /**
     * Sets whether pixel data will be processed with a premultiplied alpha
     * channel. Setting this to {@code true} (the default) can avoid a common
     * artifact that appears when transparent pixels are a very different colour
     * than surrounding translucent or opaque pixels. The artifact manifests as
     * a fringe of the non-matching colour(s) around the edges of the
     * non-transparent parts of the image. Although premultiplication avoids
     * this artifact, it also increases processing time and decreases colour
     * accuracy.
     *
     * @param enable if {@code true}, pixel values will be premultiplied by
     * their alpha value before processing, and unpremultiplied afterward
     */
    public final void setPremultiplied(boolean enable) {
        premultiply = enable;
    }

    /**
     * Returns {@code true} if automatic premultiplication is enabled (the
     * default).
     *
     * @return {@code true} if pixel values will be premultiplied by their alpha
     * value before processing, and unpremultiplied afterward
     * @see #setPremultiplied
     */
    public final boolean isPremultiplied() {
        return premultiply;
    }

    /**
     * Blurs the source image and places the result in a destination image. The
     * destination image may be {@code null}, in which case a compatible image
     * is created automatically. It may also be the source image, in which case
     * the original image data is replaced by the result.
     *
     * @param src the source image to blur
     * @param dst the destination image to copy the result to, or {@code null}
     * @return the destination image
     */
    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        final int width = src.getWidth();
        final int height = src.getHeight();
        final int[] in = new int[width * height];
        final int[] out = new int[width * height];

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        // update the lookup tables if they are out of date
        updateTables();

        getARGB(src, in);

        if (premultiply && src.getTransparency() != Transparency.OPAQUE) {
            premultiply(in);
        }

        for (int i = 0; i < iterations; ++i) {
            blurTransp(in, out, width, height, hRad, hLut, false, 0);
            blurTransp(out, in, height, width, vRad, vLut, false, 0);
        }

        if (premultiply && src.getTransparency() != Transparency.OPAQUE) {
            unpremultiply(in);
        }

        setARGB(dst, in);
        return dst;
    }

    private void updateTables() {
        if (hLut == null) {
            if (hRad == vRad && vLut != null) {
                hLut = vLut;
            } else {
                hLut = createLookupTable(hRad);
            }
        }
        if (vLut == null) {
            if (hRad == vRad && hLut != null) {
                vLut = hLut;
            } else {
                vLut = createLookupTable(vRad);
            }
        }
    }

    static int[] createLookupTable(final int radius) {
        final int size = 2 * radius + 1;
        final int[] table = new int[size * 256];
        for (int i = 0; i < table.length; ++i) {
            table[i] = i / size;
        }
        return table;
    }

    /**
     * Blurs a block of image data. This method is provided for use by other
     * filters as part of their internal processing. <b>Note:</b> the contents
     * of {@code in} will contain the final blurred data.
     *
     * @param in input ARGB pixel buffer
     * @param temp temporary ARGB pixel buffer (may be {@code null})
     * @param width image width, in pixels
     * @param height image height, in pixels
     * @param horzRadius the horizontal blur radius
     * @param vertRadius the vertical blur radius
     * @param iterations the number of blur iterations
     * @param alphaOnly if {@code true}, the blur only affects the alpha channel
     * @param alphaOnlyRGB if <tt>alphaOnly</tt> is {@code true}, this is the
     * RGB value to fill into the other channels
     */
    static void blur(int[] in, int[] temp, int width, int height, int horzRadius, int vertRadius, int iterations, boolean alphaOnly, int alphaOnlyRGB) {
        if (temp == null) {
            temp = new int[in.length];
        }
        int[] hLut = createLookupTable(horzRadius);
        int[] vLut;
        if (horzRadius == vertRadius) {
            vLut = hLut;
        } else {
            vLut = createLookupTable(vertRadius);
        }

        for (int i = 0; i < iterations; ++i) {
            blurTransp(in, temp, width, height, horzRadius, hLut, alphaOnly, alphaOnlyRGB);
            blurTransp(temp, in, height, width, vertRadius, vLut, alphaOnly, alphaOnlyRGB);
        }
    }

    /**
     * Blur and transpose pixels, possibly in parallel. This performs one pass
     * of a box blur operation; the output pixels are transposed such that they
     * are suitable to be used as the input for a second pass. A complete blur
     * is performed with two consecutive calls.
     *
     * @param in input ARGB pixel buffer
     * @param out output ARGB pixel buffer
     * @param width image width, in pixels (or height on second pass)
     * @param height image height, in pixels (or width on second pass)
     * @param radius the blur radius, horizontal or vertical depending on the
     * pass
     * @param lut the horizontal or vertical lookup table
     */
    private static void blurTransp(final int[] in, final int[] out, final int width, final int height, final int radius, final int[] lut, final boolean alphaOnly, final int alphaOnlyRGB) {
        if ((width * height) > (Tuning.PER_IMAGE / radius)) {
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
                    if (alphaOnly) {
                        blurBlockAlpha(in, out, width, height, y0, y0 + rowCount - 1, radius, lut, alphaOnlyRGB & 0xffffff);
                    } else {
                        blurBlock(in, out, width, height, y0, y0 + rowCount - 1, radius, lut);
                    }
                };
                y += rows;
            }
            sj.runUnchecked(units);
        } else {
            if (alphaOnly) {
                blurBlockAlpha(in, out, width, height, 0, height - 1, radius, lut, alphaOnlyRGB & 0xffffff);
            } else {
                blurBlock(in, out, width, height, 0, height - 1, radius, lut);
            }
        }
    }

    /**
     * Blur and transpose a contiguous block of rows. This is called by
     * {@link #blurTransp} to complete a blur pass in parallel.
     *
     * @param in input ARGB pixel buffer
     * @param out output ARGB pixel buffer
     * @param width image width, in pixels (or height on second pass)
     * @param height image height, in pixels (or width on second pass)
     * @param y0 the index of the first row to process
     * @param y1 the index of the last row to process
     * @param radius the blur radius, horizontal or vertical depending on the
     * pass
     * @param lut the horizontal or vertical lookup table
     */
    private static void blurBlock(int[] in, int[] out, int width, int height, int y0, int y1, int radius, int[] lut) {
        final int width1 = width - 1;

        int inOff = y0 * width;

        for (int y = y0; y <= y1; ++y) {
            int a = 0, r = 0, g = 0, b = 0;

            // compute the initial convolution sum
            for (int i = -radius; i <= radius; ++i) {
                final int pixel = in[inOff + clamp(i, 0, width1)];
                a += (pixel >>> 24);
                r += (pixel >> 16) & 0xff;
                g += (pixel >> 8) & 0xff;
                b += pixel & 0xff;
            }

            for (int x = 0, outOff = y; x < width; ++x, outOff += height) {
                out[outOff] = (lut[a] << 24) | (lut[r] << 16) | (lut[g] << 8) | lut[b];
                // move the sliding window of convolution sums:
                // shift in the next pixel (rPixel); shift out the old pixel (lPixel)
                final int lPixel = in[inOff + Math.max(x - radius, 0)];
                final int rPixel = in[inOff + Math.min(x + radius + 1, width1)];
                a += (rPixel >>> 24) - (lPixel >>> 24);
                r += ((rPixel & 0xff0000) - (lPixel & 0xff0000)) >> 16;
                g += ((rPixel & 0xff00) - (lPixel & 0xff00)) >> 8;
                b += (rPixel & 0xff) - (lPixel & 0xff);
            }

            inOff += width;
        }
    }

    /**
     * Blur and transpose the alpha channel of a contiguous block of rows. This
     * is the same as {@link #blurBlock}, but it only affects the alpha channel.
     *
     * @param in input ARGB pixel buffer
     * @param out output ARGB pixel buffer
     * @param width image width, in pixels (or height on second pass)
     * @param height image height, in pixels (or width on second pass)
     * @param y0 the index of the first row to process
     * @param y1 the index of the last row to process
     * @param radius the blur radius, horizontal or vertical depending on the
     * pass
     * @param lut the horizontal or vertical lookup table
     */
    private static void blurBlockAlpha(int[] in, int[] out, int width, int height, int y0, int y1, int radius, int[] lut, int rgb) {
        final int width1 = width - 1;

        int inOff = y0 * width;

        for (int y = y0; y <= y1; ++y) {
            int a = 0;

            // compute the initial convolution sum
            for (int i = -radius; i <= radius; ++i) {
                final int pixel = in[inOff + clamp(i, 0, width1)];
                a += (pixel >>> 24);
            }

            for (int x = 0, outOff = y; x < width; ++x, outOff += height) {
                out[outOff] = (lut[a] << 24) | rgb;
                // move the sliding window of convolution sums:
                // shift in the next pixel (rPixel); shift out the old pixel (lPixel)
                final int lPixel = in[inOff + Math.max(x - radius, 0)];
                final int rPixel = in[inOff + Math.min(x + radius + 1, width1)];
                a += (rPixel >>> 24) - (lPixel >>> 24);
            }

            inOff += width;
        }
    }
}
