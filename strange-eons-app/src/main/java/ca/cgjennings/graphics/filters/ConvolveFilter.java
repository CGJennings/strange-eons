package ca.cgjennings.graphics.filters;

import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/**
 * Performs an image convolution on an image. This is similar to
 * {@link ConvolveOp}, with two key differences:
 * <ol>
 * <li> This class offers more complex options for handling image edges.
 * <li> This class makes use of the acceleration framework provided by the
 * {@link AbstractImageFilter} framework. Note that {@code ConvolveOp} may also
 * provide acceleration; typically this is GPU-based acceleration for small
 * kernel sizes. (Which implementation is faster will depend on a number of
 * platform-specific factors.)
 * </ol>
 *
 * <p>
 * <b>In-place filtering:</b> Unless otherwise noted, filters based on this
 * class support in-place filtering (the source and destination images can be
 * the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class ConvolveFilter extends AbstractConvolver {

    private Kernel[] kernels = new Kernel[1];
    private EdgeHandling edgeHandling = EdgeHandling.REPEAT;

    /**
     * Creates a new convolve filter set with a simple blur filter.
     */
    public ConvolveFilter() {
    }

    /**
     * Creates a new convolve filter. This is a convenience that creates a new
     * {@link Kernel} with the specified width, height, and matrix values. The
     * kernel matrix will not be normalized; if the elements of the kernel do
     * not sum to 1, the brightness of the destination image will be changed by
     * the filter.
     *
     * @param kernelWidth the width of the kernel matrix
     * @param kernelHeight the height of the kernel matrix
     * @param kernelData the matrix data values
     */
    public ConvolveFilter(int kernelWidth, int kernelHeight, float... kernelData) {
        setKernel(false, kernelWidth, kernelHeight, kernelData);
    }

    /**
     * Creates a new convolve filter. This is a convenience that creates a new
     * {@link Kernel} with the specified width, height, and matrix values.
     *
     * @param normalize whether the m
     * @param kernelWidth the width of the kernel matrix
     * @param kernelHeight the height of the kernel matrix
     * @param kernelData the matrix data values
     */
    public ConvolveFilter(boolean normalize, int kernelWidth, int kernelHeight, float... kernelData) {
        setKernel(false, kernelWidth, kernelHeight, kernelData);
    }

    /**
     * Creates a new convolve filter with the specified kernel.
     *
     * @param kernel the convolution kernel
     */
    public ConvolveFilter(Kernel kernel) {
        setKernel(kernel);
    }

    /**
     * Returns the convolution kernel.
     *
     * @return the current kernel
     */
    public Kernel getKernel() {
        return getKernels()[0];
    }

    /**
     * Sets the convolution kernel.
     *
     * @param kernel the kernel to use
     */
    public void setKernel(Kernel kernel) {
        if (kernel == null) {
            throw new NullPointerException("kernel");
        }
        kernels[0] = kernel;
    }

    /**
     * Sets the convolution kernel. This is a convenience that creates a new
     * {@link Kernel} with the specified width, height, and matrix values.
     *
     * @param normalize if {@code true}, the matrix will be normalized so that
     * its elements sum to 1; this prevents the overall image brightness from
     * changing
     * @param width the width of the kernel
     * @param height the height of the kernel
     * @param kernelData an array of kernel values, in
     * <a href='http://en.wikipedia.org/wiki/Row-major_order'>row-major
     * order</a>
     * @throws IllegalArgumentException if the length of the array is less than
     * with product of the width and height or if {@code normalize} is
     * {@code true} and the kernel elements sum to zero
     */
    public void setKernel(boolean normalize, int width, int height, float... kernelData) {
        if (kernelData == null) {
            throw new NullPointerException("kernelData");
        }
        if (width * height != kernelData.length) {
            throw new IllegalArgumentException("kernelData.length != width * height");
        }
        if (normalize) {
            float sum = 0f;
            for (int i = 0; i < kernelData.length; ++i) {
                sum += kernelData[i];
            }
            if (Math.abs(sum) < 1e-10f) {
                throw new IllegalArgumentException("can't normalize kernel whose elements sum to 0");
            }
            if (Math.abs(sum - 1f) > 1e-10f) {
                for (int i = 0; i < kernelData.length; ++i) {
                    kernelData[i] /= sum;
                }
            }
        }
        setKernel(new Kernel(width, height, kernelData));
    }

    /**
     * Sets the edge handling mode. The edge handling mode determines the
     * stand-in pixel values used for pixels at the edge of the image, where
     * part of the kernel would lie outside of the source image.
     *
     * @param edgeHandling the edge handling mode
     * @throws NullPointerException if the edge handling mode is {@code null}
     */
    public void setEdgeHandling(EdgeHandling edgeHandling) {
        if (edgeHandling == null) {
            throw new NullPointerException("edgeHandling");
        }
        this.edgeHandling = edgeHandling;
    }

    /**
     * Returns the current edge handling mode.
     *
     * @return the edge handling mode
     */
    @Override
    public EdgeHandling getEdgeHandling() {
        return edgeHandling;
    }

    @Override
    protected Kernel[] getKernels() {
        if (kernels[0] == null) {
            kernels[0] = new Kernel(3, 3, new float[]{
                1 / 14f, 2 / 14f, 1 / 14f,
                2 / 14f, 2 / 14f, 2 / 14f,
                1 / 14f, 2 / 14f, 1 / 14f
            });
        }
        return kernels;
    }
}
