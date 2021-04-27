package ca.cgjennings.graphics.filters;

import ca.cgjennings.graphics.ImageUtilities;

/**
 * A filter that converts images to greyscale, completely desaturating the
 * colours.
 *
 * <p>
 * <b>In-place filtering:</b> This class supports in-place filtering (the source
 * and destination image may be the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @see ImageUtilities#desaturate(java.awt.image.BufferedImage)
 */
public class GreyscaleFilter extends AbstractPixelwiseFilter {

    @Override
    public void filterPixels(int[] argb, int start, int end) {
        for (int i = start; i < end; ++i) {
            int rgb = argb[i];
            int gray = ((77 * ((rgb >> 16) & 0xff))
                    + (150 * ((rgb >> 8) & 0xff))
                    + (28 * (rgb & 0xff))) / 255;
            argb[i] = (rgb & 0xff000000) | (gray << 16) | (gray << 8) | (gray);
        }
    }
}
