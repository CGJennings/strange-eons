package ca.cgjennings.graphics.filters;

import static ca.cgjennings.graphics.filters.AbstractImageFilter.clampBoth;
import static ca.cgjennings.math.Interpolation.lerp;

/**
 * An image filter that replaces one ARGB value with another. The substitution
 * only occurs when the input value is an exact match. Typically, this is used
 * to replace a "magic" or "key" value, for example, when a source image uses a
 * special value to indicate pixels that should be transparent.
 *
 * <p>
 * <b>In-place filtering:</b> This filter supports in-place filtering (the
 * source and destination may be the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class SubstitutionFilter extends AbstractPixelwiseFilter {

    private int target = 0xffff00ff;
    private int replacement = 0;
    private float error = 0f;
    private boolean blend = false;

    /**
     * Creates a new filter that substitutes pixels with value 0xffff00ff with
     * the value 0x00000000.
     */
    public SubstitutionFilter() {
    }

    /**
     * Creates a new filter that substitutes pixels as specified.
     *
     * @param target the value to search for
     * @param replacement the value to replace the target with
     */
    public SubstitutionFilter(int target, int replacement) {
        this.target = target;
        this.replacement = replacement;
    }

    /**
     * Returns the target value.
     *
     * @return the pixel value to match
     */
    public int getTarget() {
        return target;
    }

    /**
     * Sets the target pixel value to match.
     *
     * @param target the pixel value to match
     */
    public void setTarget(int target) {
        this.target = target;
    }

    /**
     * Returns the replacement value.
     *
     * @return the pixel value to replace the target with
     */
    public int getReplacement() {
        return replacement;
    }

    /**
     * Sets the replacement value.
     *
     * @param replacement the pixel value to replace target pixels with
     */
    public void setReplacement(int replacement) {
        this.replacement = replacement;
    }

    /**
     * Sets the amount of error tolerance allowed when matching pixels. The
     * error value of any pixel is the distance between the tuples formed by the
     * channel values of the target value and the pixel being considered. The
     * default tolerance is 0, meaning that the pixel values must match exactly.
     *
     * @param tolerance the error tolerance to allow when matching the target
     * pixel
     * @see #getErrorTolerance()
     */
    public void setErrorTolerance(float tolerance) {
        if (tolerance < 0f) {
            throw new IllegalArgumentException("error tolerance < 0: " + tolerance);
        }
        error = tolerance;
    }

    /**
     * Returns the amount of error tolerance allowed when matching pixels.
     *
     * @return the error tolerance to allow when matching the target pixel
     * @see #setErrorTolerance(float)
     */
    public float getErrorTolerance() {
        return error;
    }

    /**
     * Returns <code>true</code> if the substitute colour is blended with the
     * original colour according to the distance from the target colour. This
     * has no effect if the error tolerance is 0.
     *
     * @return <code>true</code> if the substitute colour is blended with the
     * target colour in proportion to the target's error
     */
    public boolean isBlendedWithOriginal() {
        return blend;
    }

    /**
     * Sets whether the substitute colour is blended with the target colour in
     * proportion to a pixel's distance from the colour to match. This has no
     * effect if the error tolerance is 0.
     *
     * @param blend if <code>true</code>, blending is performed relative to the
     * amount of error
     */
    public void setBlendedWithOriginal(boolean blend) {
        this.blend = blend;
    }

    @Override
    public void filterPixels(int[] argb, int start, int end) {
        final int target = this.target;
        final int replacement = this.replacement;
        final int len = argb.length;

        if (error == 0f) {
            for (int i = 0; i < len; ++i) {
                if (argb[i] == target) {
                    argb[i] = replacement;
                }
            }
        } else {
            final int rA = (replacement >>> 24);
            final int rR = (replacement >> 16) & 0xff;
            final int rG = (replacement >> 8) & 0xff;
            final int rB = (replacement & 0xff);
            final int tA = (target >>> 24);
            final int tR = (target >> 16) & 0xff;
            final int tG = (target >> 8) & 0xff;
            final int tB = target & 0xff;
            float errorSq = error * error;
            for (int i = 0; i < len; ++i) {
                final int rgb = argb[i];
                if (rgb == target) {
                    argb[i] = replacement;
                } else {
                    final int sA = (rgb >>> 24);
                    final int sR = ((rgb >> 16) & 0xff);
                    final int sG = ((rgb >> 8) & 0xff);
                    final int sB = (rgb & 0xff);
                    final int dA = sA - tA;
                    final int dR = sR - tR;
                    final int dG = sG - tG;
                    final int dB = sB - tB;
                    final float dSq = (dA * dA + dR * dR + dG * dG + dB * dB);
                    if (dSq <= errorSq) {
                        if (blend) {
                            float a = (float) Math.sqrt(dSq) / error;
                            final int oA = clampBoth(lerp(a, rA, tA));
                            final int oR = clampBoth(lerp(a, rR, tR));
                            final int oG = clampBoth(lerp(a, rG, tG));
                            final int oB = clampBoth(lerp(a, rB, tB));
                            argb[i] = (oA << 24) | (oR << 16) | (oG << 8) | oB;
                        } else {
                            argb[i] = replacement;
                        }
                    }
                }
            }
        }
    }
}
