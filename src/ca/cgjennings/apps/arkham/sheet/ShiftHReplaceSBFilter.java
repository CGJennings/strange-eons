package ca.cgjennings.apps.arkham.sheet;

import ca.cgjennings.graphics.filters.TintFilter;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * This filter shifts the hue of each pixel by a fixed amount, sets the
 * saturation to a fixed value, and scales the brightness. An optional
 * brightness adjustment factor allows compensation for darker images to allow a
 * greater dynamic range.
 */
public class ShiftHReplaceSBFilter extends TintFilter {

    private float bAdjust;

    public ShiftHReplaceSBFilter() {
        super();
    }

    public ShiftHReplaceSBFilter(float h, float s, float b) {
        super(h, s, b);
    }

    @Override
    public void setFactors(float h, float s, float b) {
        hFactor = h;
        if (s < 0) {
            s = 0;
        }
        if (s > 1) {
            s = 1;
        }
        sFactor = s;
        if (b < 0) {
            b = 0;
        }
        bFactor = b;
    }

    @Override
    public BufferedImage filter(BufferedImage source, BufferedImage dest) {
        return super.filter(source, dest);
    }

    @Override
    public void filterPixels(int[] argb, int start, int end) {
        float[] newHsb = new float[3];
        for (int p = start; p < end; ++p) {
            int newrgb = argb[p];
            int alpha = newrgb >>> 24;

            if (alpha != 0) {
                Color.RGBtoHSB(
                        (newrgb & 0xff0000) >> 16,
                        (newrgb & 0xff00) >> 8,
                        (newrgb & 0xff),
                        newHsb);
                newHsb[0] += hFactor;
                newHsb[1] = sFactor;
                newHsb[2] *= (bFactor * bAdjust);
                if (newHsb[2] > 1f) {
                    newHsb[2] = 1f;
                }
                newrgb = Color.HSBtoRGB(newHsb[0], newHsb[1], newHsb[2]);
                argb[p] = (newrgb & 0xff_ffff) | (alpha << 24);
            }
        }
    }

    public float getBrightAdjust() {
        return bAdjust;
    }

    public void setBrightAdjust(float bAdjust) {
        this.bAdjust = bAdjust;
    }
}
