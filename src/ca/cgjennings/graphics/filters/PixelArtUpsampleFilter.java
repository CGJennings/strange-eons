package ca.cgjennings.graphics.filters;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.Arrays;

/**
 * An image filter that upscales images to twice their original size. As it uses
 * an algorithm optimized for small or low-resolution images, it often produces
 * better results than standard sampling algorithms on icons and pixel art.
 *
 * <p>
 * <b>In-place filtering:</b> This class <b>does not</b> support in-place
 * filtering (the source and destination images must be different). Moreover the
 * destination must be of exactly double the width and height of the source.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.3
 */
public class PixelArtUpsampleFilter extends AbstractImageFilter {

    /**
     * Max alpha difference before pixels considered different.
     */
    private static final int THRESH_ALPHA = 4;
    /**
     * Max luminance difference before pixels considered different.
     */
    private static final int THRESH_LUM = 18;
    /**
     * Max chrominance difference before pixels considered different.
     */
    private static final int THRESH_CHROMA = 26;

    public PixelArtUpsampleFilter() {
    }

    @Override
    public Rectangle2D getBounds2D(BufferedImage source) {
        return new Rectangle(0, 0, source.getWidth() * 2, source.getHeight() * 2);
    }

    @Override
    public Point2D getPoint2D(Point2D sourcePoint, Point2D destPoint) {
        if (destPoint == null) {
            destPoint = new Point2D.Double();
        }
        destPoint.setLocation(sourcePoint.getX() * 2d, sourcePoint.getY() * 2d);
        return destPoint;
    }

    @Override
    public BufferedImage createCompatibleDestImage(BufferedImage source, ColorModel destinationColorModel) {
        if (destinationColorModel == null) {
            destinationColorModel = source.getColorModel();
        }
        return new BufferedImage(
                destinationColorModel,
                destinationColorModel.createCompatibleWritableRaster(source.getWidth() * 2, source.getHeight() * 2),
                destinationColorModel.isAlphaPremultiplied(),
                null);
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        final int w = src.getWidth();
        final int h = src.getHeight();
        final int w2 = src.getWidth() * 2;
        final int h2 = src.getHeight() * 2;

        if (dest == null) {
            dest = createCompatibleDestImage(src, null);
        } else {
            if (dest.getWidth() != w2 || dest.getHeight() != h2) {
                throw new IllegalArgumentException("dest has bad dimensions");
            }
        }

        // each pass reads 3 source rows and produces 2 dest rows;
        // at the end of each row we will rotate the source rows
        // to avoid reading the same line multiple times
        int[] src0 = new int[w];
        int[] src1 = new int[w];
        int[] src2;
        int[] dst0 = new int[w2];
        int[] dst1 = new int[w2];

        // see https://www.scale2x.it/
        int A, B, C, D, E, F, G, H, I;

        // preload source rows to rotate
        getARGB(src, 0, 0, w, 1, src1);
        src2 = Arrays.copyOf(src1, w);

        for (int y = 0, dy = 0; y < h; ++y, dy += 2) {
            // shift source data up one row
            int[] temp = src0;
            src0 = src1;
            src1 = src2;
            final int yn = y + 1;
            if (yn >= h) {
                src2 = src1;
            } else {
                src2 = temp;
                getARGB(src, 0, yn, w, 1, src2);
            }

            for (int x = 0, d = 0; x < w; ++x, d += 2) {
                // A B C  load source pixels from around target pixel E
                // D E F
                // G H I
                B = src0[x];
                E = src1[x];
                H = src2[x];
                if (x > 0) {
                    final int xp = x - 1;
                    A = src0[xp];
                    D = src1[xp];
                    G = src2[xp];
                } else {
                    A = B;
                    D = E;
                    G = H;
                }
                final int xn = x + 1;
                if (xn < w) {
                    C = src0[xn];
                    F = src1[xn];
                    I = src2[xn];
                } else {
                    C = B;
                    F = E;
                    I = H;
                }

                // E0 E1  choose four pixel values to represent E'
                // E2 E3
                if (!eq(B, H) && !eq(D, F)) {
                    // E0
                    dst0[d] = eq(D, B) ? blend(D, B) : E;
                    // E1
                    dst0[d + 1] = eq(B, F) ? blend(B, F) : E;
                    // E2
                    dst1[d] = eq(D, H) ? blend(D, H) : E;
                    // E3
                    dst1[d + 1] = eq(H, F) ? blend(H, F) : E;
                } else {
                    // E0 = E1 = E2 = E3 = E
                    dst0[d] = E;
                    dst0[d + 1] = E;
                    dst1[d] = E;
                    dst1[d + 1] = E;
                }
            }

            setARGB(dest, 0, y * 2, w2, 1, dst0);
            setARGB(dest, 0, y * 2 + 1, w2, 1, dst1);
        }
        return dest;
    }

    /**
     * Compare two RGB values in the YCbCr colour space.
     *
     * @return true if the colours are "about the same"
     */
    private boolean eq(final int rgb1, final int rgb2) {
        if (rgb1 == rgb2) {
            return true;
        }

        // compare alpha
        if (Math.abs(((rgb1 >> 24) - (rgb2 >> 24))) > THRESH_ALPHA) {
            return false;
        }

        final int r1 = (rgb1 & 0xff0000) >> 16, r2 = (rgb2 & 0xff0000) >> 16;
        final int g1 = (rgb1 & 0xff00) >> 8, g2 = (rgb2 & 0xff00) >> 8;
        final int b1 = rgb1 & 0xff, b2 = rgb2 & 0xff;

        // compare Y
        final float y1 = 0.299f * r1 + 0.587f * g1 + 0.114f * b1;
        final float y2 = 0.299f * r2 + 0.587f * g2 + 0.114f * b2;
        if (Math.abs(y1 - y2) > THRESH_LUM) {
            return false;
        }

        // compare Cb
        final float cb1 = -0.168736f * r1 - 0.331264f * g1 + 0.5f * b1;
        final float cb2 = -0.168736f * r2 - 0.331264f * g2 + 0.5f * b2;
        if (Math.abs(cb1 - cb2) > THRESH_CHROMA) {
            return false;
        }

        // compare Cr
        final float cr1 = 0.5f * r1 - 0.418688f * g1 - 0.081312f * b1;
        final float cr2 = 0.5f * r2 - 0.418688f * g2 - 0.081312f * b2;
        return Math.abs(cr1 - cr2) <= THRESH_CHROMA;
    }

    /**
     * Blend two pixels (in sRGB not linear gamma).
     */
    private static int blend(final int rgb1, final int rgb2) {
        if (rgb1 == rgb2) {
            return rgb1;
        }

        // returns a pixel containing the average of the each channel
        return ((((rgb1 & MASK_A) >> 1) + ((rgb2 & MASK_A) >> 1)) & MASK_A)
                | ((((rgb1 & MASK_RB) + (rgb2 & MASK_RB)) >> 1) & MASK_RB)
                | ((((rgb1 & MASK_G) + (rgb2 & MASK_G)) >> 1) & MASK_G);
    }
    private static final int MASK_A = 0xff000000;
    private static final int MASK_RB = 0x00ff00ff;
    private static final int MASK_G = 0x0000ff00;
}
