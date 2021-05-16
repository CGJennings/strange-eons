package ca.cgjennings.graphics.filters;

import java.awt.Color;

/**
 * A basic, general purpose card tinting filter. It shifts hue, and scales
 * saturation and brightness of the pixels in the source image.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class TintFilter extends TintOverlayFilter {

    private boolean identity = false;

    public TintFilter() {
    }

    public TintFilter(float h, float s, float b) {
        super(h, s, b);
    }

    @Override
    public void setFactors(float h, float s, float b) {
        if (s < 0) {
            s = 0;
        }
        if (b < 0) {
            b = 0;
        }

        hFactor = h;
        sFactor = s;
        bFactor = b;

        if (Math.abs(h) < 0.5f / 255f && Math.abs(s - 1) < 0.5f / 255f && Math.abs(b - 1) < 0.5f / 255f) {
            identity = true;
        } else {
            identity = false;
        }
    }

    public int adjustColor(int argb) {
        int newrgb = argb;
        int alpha = newrgb & 0xff00_0000;
        float[] newHsb = new float[3];

        if (alpha != 0) {
            Color.RGBtoHSB(
                    (newrgb & 0xff_0000) >> 16,
                    (newrgb & 0xff00) >> 8,
                    (newrgb & 0xff),
                    newHsb);
            newHsb[0] += hFactor;
            newHsb[1] *= sFactor;
            if (newHsb[1] > 1f) {
                newHsb[1] = 1f;
            }
            newHsb[2] *= bFactor;
            if (newHsb[2] > 1f) {
                newHsb[2] = 1f;
            }

            newrgb = Color.HSBtoRGB(newHsb[0], newHsb[1], newHsb[2]);
            argb = (newrgb & 0xff_ffff) | alpha;
        }
        return argb;
    }

    @Override
    public void filterPixels(int[] argb, int start, int end) {
        float[] newHsb = new float[3];

        for (int p = start; p < end; ++p) {
            int newrgb = argb[p];
            int alpha = newrgb & 0xff00_0000;

            if (alpha != 0) {
                Color.RGBtoHSB(
                        (newrgb & 0xff_0000) >> 16,
                        (newrgb & 0xff00) >> 8,
                        (newrgb & 0xff),
                        newHsb);
                newHsb[0] += hFactor;
                newHsb[1] *= sFactor;
                if (newHsb[1] > 1f) {
                    newHsb[1] = 1f;
                }
                newHsb[2] *= bFactor;
                if (newHsb[2] > 1f) {
                    newHsb[2] = 1f;
                }

                newrgb = Color.HSBtoRGB(newHsb[0], newHsb[1], newHsb[2]);
                argb[p] = (newrgb & 0xff_ffff) | alpha;
            }
        }
    }

    @Override
    public boolean isIdentity() {
        return identity;
    }

    /**
     * A {@code TintFilter} that scales its saturation and brightness. For
     * example, if the scale is 2, then a factor of 0.5 is scaled to 1. This is
     * useful when reading values from a {@code TintPanel}.
     */
    public static class ScaledTintFilter extends TintFilter {

        protected float sScale = 2f;
        protected float bScale = 2f;

        public ScaledTintFilter() {
        }

        public ScaledTintFilter(float h, float s, float b) {
            super(h, s, b);
        }

        public ScaledTintFilter(float h, float s, float b, float sScale, float bScale) {
            super(h, s, b);
            setSScale(sScale);
            setBScale(bScale);
        }

        public final float getSScale() {
            return sScale;
        }

        public final void setSScale(float sScale) {
            this.sScale = sScale;
        }

        public final float getBScale() {
            return bScale;
        }

        public final void setBScale(float bScale) {
            this.bScale = bScale;
        }

        public void filterPixels(int[] argb) {
            float[] newHsb = new float[3];
            float sFactor = this.sFactor * sScale;
            float bFactor = this.bFactor * bScale;

            for (int p = 0; p < argb.length; ++p) {
                int newrgb = argb[p];
                int alpha = newrgb & 0xff00_0000;

                if (alpha != 0) {
                    Color.RGBtoHSB(
                            (newrgb & 0xff_0000) >> 16,
                            (newrgb & 0xff00) >> 8,
                            (newrgb & 0xff),
                            newHsb);
                    newHsb[0] += hFactor;
                    newHsb[1] *= sFactor;
                    if (newHsb[1] > 1f) {
                        newHsb[1] = 1f;
                    }
                    newHsb[2] *= bFactor;
                    if (newHsb[2] > 1f) {
                        newHsb[2] = 1f;
                    }

                    newrgb = Color.HSBtoRGB(newHsb[0], newHsb[1], newHsb[2]);
                    argb[p] = (newrgb & 0xff_ffff) | alpha;
                }
            }
        }
    }
}
