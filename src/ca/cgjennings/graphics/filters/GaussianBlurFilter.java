package ca.cgjennings.graphics.filters;

import java.awt.image.Kernel;

/**
 * A filter that applies a Gaussian blur to the source image.
 *
 * <p>
 * <b>In-place filtering:</b> Unless otherwise noted, filters based on this
 * class support in-place filtering (the source and destination images can be
 * the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class GaussianBlurFilter extends AbstractConvolver {

    private float radius;
    private Kernel[] kernels = new Kernel[2];

    /**
     * Creates a filter with a 1-pixel blur radius.
     */
    public GaussianBlurFilter() {
        this(1f);
    }

    /**
     * Creates a filter with the specified blur radius.
     *
     * @param radius the blur radius, in pixels
     */
    public GaussianBlurFilter(float radius) {
        setRadius(radius);
    }

    /**
     * Sets the radius of the blur effect.
     *
     * @param radius the blur radius
     */
    public void setRadius(float radius) {
        if (radius <= 0f) {
            throw new IllegalArgumentException("radius must be > 0");
        }
        if (this.radius == radius) {
            return;
        }

        this.radius = radius;
        kernels[0] = null;
    }

    /**
     * Returns the current blur radius.
     *
     * @return the radius of the blur effect
     */
    public float getRadius() {
        return radius;
    }

    @Override
    protected Kernel[] getKernels() {
        if (kernels[0] != null) {
            return kernels;
        }

        final float radius = this.radius;

        final int r = (int) Math.ceil(radius);
        final int rows = 2 * r + 1;
        final float[] kernel = new float[rows];

        final float radius2 = radius * radius;
        final float sigma = radius / 3f;
        final float sigma2Sq = 2f * sigma * sigma;
        final float sqrtSigma2Pi = (float) Math.sqrt(sigma * 2d * Math.PI);

        int i = 0;
        float sum = 0f;

        for (int row = -r; row <= r; ++row) {
            float distance = row * row;
            if (distance > radius2) {
                kernel[i] = 0f;
            } else {
                kernel[i] = (float) Math.exp(-distance / sigma2Sq) / sqrtSigma2Pi;
            }
            sum += kernel[i++];
        }
        for (i = 0; i < rows; ++i) {
            kernel[i] /= sum;
        }

        kernels[0] = new Kernel(1, rows, kernel);
        kernels[1] = new Kernel(rows, 1, kernel);
        return kernels;
    }
}
