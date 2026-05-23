package ca.cgjennings.graphics.filters;

import java.awt.Color;

/**
 * A filter which inverts the brightness each pixel without affecting the
 * colour. One effective use of this filter is to convert an image that was
 * designed to look good against a light background into one that looks
 * good against a dark background, or vice-versa.
 * 
 * <p>
 * <b>In-place filtering:</b> This class supports in-place filtering (the source
 * and destination image may be the same).
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class DarkMagicFilter extends AbstractPixelwiseFilter {
    public DarkMagicFilter() {
    }

    @Override
    public void filterPixels(int[] argb, int start, int end) {
        float[] newHsb = new float[3];

        for (int p = start; p < end; ++p) {
            int newrgb = argb[p];
            int alpha = newrgb & 0xff000000;

            if (alpha != 0) {
                Color.RGBtoHSB(
                        255 - ((newrgb & 0xff0000) >> 16),
                        255 - ((newrgb & 0xff00) >> 8),
                        255 - (newrgb & 0xff),
                        newHsb
                );
                newHsb[0] += 0.5f;
                newrgb = Color.HSBtoRGB(newHsb[0], newHsb[1], newHsb[2]);
                argb[p] = (newrgb & 0xffffff) | alpha;
            }
        }
    }  
}
