package ca.cgjennings.graphics.filters;

import java.awt.*;

/**
 * This filter changes the colour of every pixel to an HSB colour value, but
 * leaves the alpha value untouched.
 */
public class TintOverlayFilter extends AbstractTintingFilter {

    private int newrgb;

    public TintOverlayFilter() {
        super(0f, 1f, 0.5f);
    }

    public TintOverlayFilter(float h, float s, float b) {
        super(h, s, b);
    }

    @Override
    public void setFactors(float h, float s, float b) {
        if (s > 1f) {
            s = 1f;
        }
        if (b > 1f) {
            b = 1f;
        }
        super.setFactors(h, s, b);
        newrgb = Color.HSBtoRGB(hFactor, sFactor, bFactor) & 0xffffff;
    }

    @Override
    public void filterPixels(int[] argb, int start, int end) {
        for (int p = start; p < end; ++p) {
            argb[p] = newrgb | (argb[p] & 0xff000000);
        }
    }

    @Override
    public boolean isIdentity() {
        return false;
    }
}
