package ca.cgjennings.graphics.filters;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

/**
 * A filter that applies a checkerboard pattern to the source image by
 * multiplying the alpha channel of alternating pixels by different values.
 * pattern. This filter is intended for use with translucent images; if the
 * destination image has no alpha channel, then the filter will have no effect.
 *
 * <p>
 * <b>In-place filtering:</b> This class supports in-place filtering (the source
 * and destination images may be the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class CheckeredScreenFilter extends AbstractRowwiseFilter {

    private int[] even = new int[256];
    private int[] odd = new int[256];
    private float evenAlpha = 0f;
    private float oddAlpha = 0f;
    private int size = 1;

    private static void updateLookup(float multiplier, int[] table) {
        int imult = (int) (255f * multiplier + 0.5f);
        for (int i = 0; i < 256; ++i) {
            table[i] = Math.min(255, i * imult / 255);
        }
    }

    /**
     * Creates a new filter with an even alpha multiplier of 0 and an odd
     * multiplier of 1. This will cause even pixels to become completely
     * transparent while odd pixels retain the same alpha value.
     */
    public CheckeredScreenFilter() {
        this(0f, 1f);
    }

    /**
     * Creates a new filter with a size of 1 and the specified even and odd
     * alpha multipliers.
     *
     * @param evenAlpha the alpha value for even squares
     * @param oddAlpha the alpha value for odd squares
     */
    public CheckeredScreenFilter(float evenAlpha, float oddAlpha) {
        setEvenAlpha(evenAlpha);
        setOddAlpha(oddAlpha);
    }

    /**
     * Creates a new filter with the specified size of 1 and the specified even
     * and odd alpha multipliers.
     *
     * @param checkSize the size of the squares in the check pattern, in pixels
     * @param evenAlpha the alpha value for even squares
     * @param oddAlpha the alpha value for odd squares
     */
    public CheckeredScreenFilter(int checkSize, float evenAlpha, float oddAlpha) {
        setCheckSize(checkSize);
        setEvenAlpha(evenAlpha);
        setOddAlpha(oddAlpha);
    }

    /**
     * Returns the alpha multiplier for even squares.
     *
     * @return the non-negative even multiplier
     */
    public float getEvenAlpha() {
        return evenAlpha;
    }

    /**
     * Sets the alpha multiplier for even squares. The existing alpha value in
     * even squares will be multiplied by this amount. Typical values are
     * between 0 and 1, but values above 1 are allowed.
     *
     * @param evenAlpha the even multiplier
     */
    public void setEvenAlpha(float evenAlpha) {
        if (evenAlpha < 0f) {
            throw new IllegalArgumentException("evenAlpha < 0");
        }
        if (this.evenAlpha != evenAlpha) {
            this.evenAlpha = evenAlpha;
            updateLookup(evenAlpha, even);
        }
    }

    /**
     * Returns the alpha multiplier for odd squares.
     *
     * @return the non-negative odd multiplier
     */
    public float getOddAlpha() {
        return oddAlpha;
    }

    /**
     * Sets the alpha multiplier for odd squares. The existing alpha value in
     * odd squares will be multiplied by this amount. Typical values are between
     * 0 and 1, but values above 1 are allowed.
     *
     * @param oddAlpha the even multiplier
     */
    public void setOddAlpha(float oddAlpha) {
        if (oddAlpha < 0f) {
            throw new IllegalArgumentException("oddAlpha < 0");
        }
        if (this.oddAlpha != oddAlpha) {
            this.oddAlpha = oddAlpha;
            updateLookup(oddAlpha, odd);
        }
    }

    /**
     * Returns the size of the checkered squares, in pixels.
     *
     * @return the square size, a positive integer
     */
    public int getCheckSize() {
        return size;
    }

    /**
     * Sets the size of the checkered squares, in pixels.
     *
     * @param size the size of check pattern squares
     */
    public void setCheckSize(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("size < 1");
        }
        this.size = size;
    }

    @Override
    public BufferedImage createCompatibleDestImage(BufferedImage source, ColorModel destinationColorModel) {
        if (destinationColorModel == null) {
            return new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        }
        return new BufferedImage(
                destinationColorModel,
                destinationColorModel.createCompatibleWritableRaster(source.getWidth(), source.getHeight()),
                destinationColorModel.isAlphaPremultiplied(),
                null
        );
    }

    @Override
    public void filterPixels(int y, int[] argb) {
        if (size == 1) {
            final int parity = y & 1;
            for (int i = 0; i < argb.length; ++i) {
                int alpha = (argb[i] >>> 24);
                alpha = ((i & 1) == parity) ? even[alpha] : odd[alpha];
                argb[i] = (argb[i] & 0x00ff_ffff) | (alpha << 24);
            }
        } else {
            int parity = (y / size) & 1;
            int c = 0;
            for (int i = 0; i < argb.length; ++i) {
                int alpha = (argb[i] >>> 24);
                alpha = (parity == 0) ? even[alpha] : odd[alpha];
                argb[i] = (argb[i] & 0x00ff_ffff) | (alpha << 24);
                if (++c == size) {
                    c = 0;
                    parity = 1 - parity;
                }
            }
        }
    }
}
