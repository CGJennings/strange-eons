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
 * @since 3.4
 */
public class PixelArtUpscalingFilter extends AbstractImageFilter {

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
    
    /** For future use. Current implementation only performs 2x scaling. */
    final int scale = 2;

    public PixelArtUpscalingFilter() {
    }

    @Override
    public Rectangle2D getBounds2D(BufferedImage source) {
        return new Rectangle(0, 0, source.getWidth() * scale, source.getHeight() * scale);
    }

    @Override
    public Point2D getPoint2D(Point2D sourcePoint, Point2D destPoint) {
        if (destPoint == null) {
            destPoint = new Point2D.Double();
        }
        destPoint.setLocation(sourcePoint.getX()* scale, sourcePoint.getY() * scale);
        return destPoint;
    }

    @Override
    public BufferedImage createCompatibleDestImage(BufferedImage source, ColorModel destinationColorModel) {
        if (destinationColorModel == null) {
            destinationColorModel = source.getColorModel();
        }
        return new BufferedImage(
                destinationColorModel,
                destinationColorModel.createCompatibleWritableRaster(source.getWidth() * scale, source.getHeight() * scale),
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
        
        // This implements the EPX/Scale2x algorithm, except that it does not
        // require exact matches of colour (that algorithm was designed for
        // images using an indexed colour model). Instead, colours are tested
        // for similarity by converting them to the YCbCr colour space. Then,
        // if considered similar, an equal mix is used as the output colour.
        //
        // In Scale 2x each pixel is scaled up by 2x,         E0 E1
        // from one source pixel, E, to four pixels E0-E3.    E2 E3
        //
        // The values of these pixels are determined by       A  B  C
        // comparing the pixels around the source pixel E     D  E  F
        // in the source image, called A-I.                   G  H  I
        //
        // The scaling rules are fast, simple and deterministic.
        // See https://www.scale2x.it/
        //
        // Note that in practice, A, C, G, and I are only used in the Scale3x
        // version. For completeness, they are included below but commented out.
        
        int /*A,*/ B, /*C,*/ D, E, F, /*G,*/ H /*, I*/;

        // each pass reads 3 source rows and produces 2 dest rows;
        // at the end of each row we will rotate the source rows
        // to avoid reading the same line multiple times
        int[] src0 = new int[w];
        int[] src1 = new int[w];
        int[] src2;
        int[] dst0 = new int[w2];
        int[] dst1 = new int[w2];


        // preload source rows to rotate
        getARGB(src, 0, 0, w, 1, src1);
        src2 = Arrays.copyOf(src1, w);

        for (int y = 0; y < h; ++y) {
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
                // load source pixels from around target pixel E
                B = src0[x];
                E = src1[x];
                H = src2[x];
                if (x > 0) {
                    final int xp = x - 1;
                    /*A = src0[xp];*/
                    D = src1[xp];
                    /*G = src2[xp];*/
                } else {
                    /*A = B;*/
                    D = E;
                    /*G = H;*/
                }
                final int xn = x + 1;
                if (xn < w) {
                    /*C = src0[xn];*/
                    F = src1[xn];
                    /*I = src2[xn];*/
                } else {
                    /*C = B;*/
                    F = E;
                    /*I = H;*/
                }

                // choose four pixel values to represent E'
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
