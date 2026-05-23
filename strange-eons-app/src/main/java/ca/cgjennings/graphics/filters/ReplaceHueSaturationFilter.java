package ca.cgjennings.graphics.filters;

import java.awt.*;

/**
 * A tinting filter that replaces the hue and saturation of every pixel with
 * fixed values, and scales the brightness. This tinting filter is useful when
 * the image to be tinted is a template drawn in a single colour, possibly with
 * variations in brightness. Typically the colour used is pure red; the
 * following filter shows one way to convert an existing image to this format:
 * <pre>new ChannelSwapFilter( ChannelSwapFilter.ALPHA, ChannelSwapFilter.GREY, 0, 0 );</pre>
 *
 * <p>
 * <b>In-place filtering:</b> This class supports in-place filtering (the source
 * and destination image may be the same).
 */
public class ReplaceHueSaturationFilter extends AbstractTintingFilter {

    public ReplaceHueSaturationFilter() {
        super();
    }

    public ReplaceHueSaturationFilter(float h, float s, float b) {
        super(h, s, b);
    }

    @Override
    public void setFactors(float h, float s, float b) {
        if (s > 1) {
            s = 1;
        }
        super.setFactors(h, s, b);
    }

    @Override
    public void filterPixels(int[] argb, int start, int end) {
        for (int p = start; p < end; ++p) {
            int newrgb = argb[p];
            int alpha = newrgb & 0xff000000;

            if (alpha != 0) {
                int bInt = (((newrgb & 0xff0000) >> 16) + ((newrgb & 0xff00) >> 8) + (newrgb & 0xff)) / 3;

                float b = (bInt / 255f) * bFactor;
                if (b > 1f) {
                    b = 1f;
                }
                newrgb = Color.HSBtoRGB(hFactor, sFactor, b);
                argb[p] = (newrgb & 0xffffff) | alpha;
            }
        }
    }
}
