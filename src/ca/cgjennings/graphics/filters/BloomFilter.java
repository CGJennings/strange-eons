package ca.cgjennings.graphics.filters;

import ca.cgjennings.algo.SplitJoin;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

/**
 * An image filter that applies a bloom effect to images. Bloom adds bright,
 * blurry fringes around the bright areas of an image.
 *
 * <p>
 * <b>In-place filtering:</b> This class supports in-place filtering (the source
 * and destination images can be the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class BloomFilter extends BlurFilter {

    private int threshold = 96;
    private float fThreshold = 0.375f;
    private float strength = 0.3333f;

    public BloomFilter() {
        super(3, 3);
    }

    /**
     * Returns the strength of the bloom effect, between 0 and 1.
     *
     * @return the strength of the bloom glow
     */
    public float getStrength() {
        return strength;
    }

    /**
     * Sets the strength of the bloom effect. The bloom strength must be between
     * 0 and 1 inclusive. The higher the value, the brighter the glow caused by
     * the bloom.
     *
     * @param strength the bloom strength, between 0 to 1
     */
    public void setStrength(float strength) {
        if (strength < 0f || strength > 1f) {
            throw new IllegalArgumentException("bloom strength not in range [0..1]: " + strength);
        }
        if (this.strength != strength) {
            this.strength = strength;
            alphaTable = null;
        }
    }

    /**
     * Returns the brightness threshold for applying bloom effect. This is a
     * value between 0 and 1 that defines the minimum brightness of a pixel
     * before the bloom effect will show through.
     *
     * @return the brightness threshold, from 0 to 1
     */
    public float getThreshold() {
        return fThreshold;
    }

    /**
     * Sets the brightness threshold for applying bloom effect. This is a value
     * between 0 and 1 that defines the minimum brightness of a pixel before the
     * bloom effect will show through. For pixels above the threshold, the
     * strength of the bloom effect is proportional to the amount that the
     * brightness exceeds the threshold.
     *
     * @param threshold the brightness threshold, from 0 to 1
     */
    public void setThreshold(float threshold) {
        if (threshold < 0f || threshold > 1f) {
            throw new IllegalArgumentException("threshold not in [0..1]: " + threshold);
        }
        if (fThreshold != threshold) {
            fThreshold = threshold;
            this.threshold = (int) (threshold * 255f + 0.5f);
            alphaTable = null;
        }
    }

    /**
     * Applies the bloom filter to an image, storing the result in the specified
     * destination.
     */
    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        final boolean premultiply = isPremultiplied();
        final int iterations = getIterations();
        final int hRad = getHorizontalRadius();
        final int vRad = getVerticalRadius();

        final int width = src.getWidth();
        final int height = src.getHeight();
        final int[] blurred = new int[width * height];
        final int[] out = new int[width * height];

        if (dst == null) {
            dst = createCompatibleDestImage(src, null);
        }

        getARGB(src, blurred);

        // Blur a copy of the source image
        if (premultiply && src.getTransparency() != Transparency.OPAQUE) {
            premultiply(blurred);
        }
        blur(blurred, out, width, height, hRad, vRad, iterations, false, 0);
        if (premultiply && src.getTransparency() != Transparency.OPAQUE) {
            unpremultiply(blurred);
        }

        // Get an unblurred copy of the source image
        getARGB(src, out);

        makeAlphaTable();

        if ((width * height) > Tuning.PER_ROW) {
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
                    addBlock(blurred, out, width, y0, y0 + rowCount, alphaTable);
                };
                y += rows;
            }
            sj.runUnchecked(units);
        } else {
            addBlock(blurred, out, width, 0, height, alphaTable);
        }

        // Copy final pixels to dest image
        setARGB(dst, out);

        return dst;
    }

    private void makeAlphaTable() {
        if (alphaTable == null) {
            alphaTable = new float[256];
            final float baseAlpha = strength * MAX_ALPHA * 2f;
            final float range = 255 - threshold;
            final float thresh = threshold;
            for (int i = 0; i < 256; ++i) {
                if (i <= threshold) {
                    alphaTable[i] = 0f;
                } else {
                    alphaTable[i] = Math.min(MAX_ALPHA, baseAlpha * (i - thresh) / range);
                }
            }
        }
    }
    private float[] alphaTable;
    private static final float MAX_ALPHA = 4f;

    private void addBlock(int[] blurred, int[] out, int width, int y0, int y1, float[] aFactor) {
        int end = y1 * width;

        for (int i = y0 * width; i < end; ++i) {
            final int dRGB = out[i];
            int dR = (dRGB >> 16) & 0xff;
            int dG = (dRGB >> 8) & 0xff;
            int dB = dRGB & 0xff;

            final int sRGB = blurred[i];
            int sR = (sRGB >> 16) & 0xff;
            int sG = (sRGB >> 8) & 0xff;
            int sB = sRGB & 0xff;

            int bri = (sR + sG + sB) / 3;
            float alpha = aFactor[bri];

            dR = clamp((int) (dR + alpha * sR));
            dG = clamp((int) (dG + alpha * sG));
            dB = clamp((int) (dB + alpha * sB));

            out[i] = (dRGB & 0xff00_0000) | (dR << 16) | (dG << 8) | dB;
        }
    }

//	public static void main( String[] args ) {
//		try {
//			BufferedImage im = ImageIO.read( new File( "d:\\test.png" ) );
//			im = ImageUtilities.ensureIntRGBFormat( im );
//
//			im = new BloomFilter().filter( im, null );
//
//			ImageIO.write( im, "png", new File( "d:\\test-out.png" ) );
//		} catch( Exception e ) {
//			e.printStackTrace();
//		}
//	}
}
