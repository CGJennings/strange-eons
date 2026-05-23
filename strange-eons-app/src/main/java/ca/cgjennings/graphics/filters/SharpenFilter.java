package ca.cgjennings.graphics.filters;

import java.awt.image.Kernel;

/**
 * A filter that sharpens the input image.
 *
 * <p>
 * <b>In-place filtering:</b> This class allows in-place filtering (the source
 * and destination images can be the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class SharpenFilter extends AbstractConvolver {

    private Kernel[] kernels = new Kernel[1];
    private float sharpeningFactor = 1f;

    /**
     * Creates a new sharpening filter with a sharpening factor of 1.
     */
    public SharpenFilter() {
    }

    /**
     * Creates a new sharpening filter with the specified sharpening factor.
     *
     * @param sharpeningFactor
     */
    public SharpenFilter(float sharpeningFactor) {
        setSharpeningFactor(sharpeningFactor);
    }

    /**
     * Returns a factor representing the amount of sharpening that will be
     * applied by this filter.
     *
     * @return the sharpening factor
     */
    public float getSharpeningFactor() {
        return sharpeningFactor;
    }

    /**
     * Sets the sharpening factor. Higher values increase the sharpening effect.
     * Lower values produce a less pronounced sharpening effect. A value of zero
     * will reproduce the original image (some pixel values may change slightly
     * due to rounding and conversion errors). The default is factor is one.
     *
     * @param sharpeningFactor a factor which determines how pronounced the
     * sharpening effect is
     * @throws IllegalArgumentException if the sharpening factor is negative
     */
    public void setSharpeningFactor(float sharpeningFactor) {
        if (sharpeningFactor < 0) {
            throw new IllegalArgumentException("sharpeningFactor < 0: " + sharpeningFactor);
        }
        if (this.sharpeningFactor != sharpeningFactor) {
            this.sharpeningFactor = sharpeningFactor;
            kernels[0] = null;
        }
    }

    @Override
    protected Kernel[] getKernels() {
        if (kernels[0] == null) {
            final float base = 4f * sharpeningFactor + 1f;
            final float arm = -sharpeningFactor;
            kernels[0] = new Kernel(3, 3, new float[]{
                0f, arm, 0f,
                arm, base, arm,
                0f, arm, 0f
            });
        }
        return kernels;
    }
}
