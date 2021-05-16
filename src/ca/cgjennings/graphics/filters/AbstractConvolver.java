package ca.cgjennings.graphics.filters;

import static ca.cgjennings.graphics.filters.AbstractImageFilter.clampBoth;
import static ca.cgjennings.graphics.filters.AbstractImageFilter.premultiply;
import static ca.cgjennings.graphics.filters.AbstractImageFilter.unpremultiply;
import static ca.cgjennings.graphics.filters.EdgeHandling.REPEAT;
import static ca.cgjennings.graphics.filters.EdgeHandling.WRAP;
import static ca.cgjennings.graphics.filters.EdgeHandling.ZERO;
import java.awt.image.Kernel;

/**
 * An abstract base class for filters that perform convolutions. Concrete
 * subclasses are responsible for supplying the convolution kernel(s) by
 * implementing {@link #getKernels()}. (Separable convolutions can be
 * implemented by returning a pair of kernels.)
 *
 * <p>
 * Additional methods modify the behaviour of the convolution. They have default
 * implementations that return the following values:
 *
 * <p>
 * {@link #getEdgeHandling()} determines how input pixels that fall off the edge
 * of the source image are handled. The base class returns
 * {@link EdgeHandling#REPEAT}.
 *
 * <p>
 * {@link #isAlphaPremultiplied()} determines whether the alpha channel is
 * premultiplied. The default is {@code true}. Subclasses may choose to
 * override this if they are calling the
 * {@linkplain #filter(int[], int[], int, int) integer array} filtering method
 * with pixel data that is already premultiplied.
 *
 * <p>
 * {@link #isAlphaFiltered()} determines whether the alpha channel is retained
 * after filtering. The default is {@code true}. If {@code false}, the
 * alpha channel will be completely opaque after filtering.
 *
 * <p>
 * <b>In-place filtering:</b> Unless otherwise noted, filters based on this
 * class support in-place filtering (the source and destination images can be
 * the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public abstract class AbstractConvolver extends AbstractImagewiseFilter {

    public AbstractConvolver() {
    }

    /**
     * Returns the edge handling mode used by the convolution.
     *
     * @return how the filter handles pixels that fall of the image edge
     */
    public EdgeHandling getEdgeHandling() {
        return EdgeHandling.REPEAT;
    }

    /**
     * Returns {@code true} if images with an alpha channel will
     * automatically be converted to a premultiplied format during the
     * convolution, and converted back afterward.
     *
     * @return {@code true} if images are premultiplied automatically
     */
    public boolean isAlphaPremultiplied() {
        return true;
    }

    /**
     * Returns {@code true} if the alpha channel is filtered. If this
     * returns {@code false}, the alpha channel is set to 255. For more
     * complex channel value manipulation, see {@link ChannelSwapFilter}.
     *
     * @return {@code true} if the alpha channel is processed by the filter
     */
    public boolean isAlphaFiltered() {
        return true;
    }

    /**
     * Returns the convolution kernel(s) that should be applied to execute this
     * filter. Typically, array is either length one (non-separable) or length
     * two (separable).
     *
     * @return an array of non-{@code null} convolution kernels
     */
    protected abstract Kernel[] getKernels();

    @Override
    public int[] filter(int[] source, int[] destination, final int width, final int height) {
        if (destination == null) {
            destination = new int[width * height];
        }

        Kernel[] kernels = getKernels();

        // if there are no kernels, return the original image (possibly made opaque)
        if (kernels.length == 0) {
            if (destination != source) {
                System.arraycopy(source, 0, destination, 0, destination.length);
                if (!isAlphaFiltered()) {
                    for (int i = 0; i < destination.length; ++i) {
                        destination[i] = 0xff000000 | (destination[i] & 0xffffff);
                    }
                }
            }
            return destination;
        }

        final boolean premul = isAlphaPremultiplied();
        if (premul) {
            premultiply(source);
        }

        prepareKernel(kernels[0]);
        super.filter(source, destination, width, height);
        // if there are multiple kernels, apply each in turn, reusing the
        // source buffer from the previous iteration as the new dest buffer
        if (kernels.length > 1) {
            for (int k = 1; k < kernels.length; ++k) {
                int[] bufferSwap = source;
                source = destination;
                destination = bufferSwap;
                prepareKernel(kernels[k]);
                super.filter(source, destination, width, height);
            }
        }

        if (premul) {
            unpremultiply(destination);
        }

        return destination;
    }

    private void prepareKernel(Kernel k) {
        rows = k.getHeight();
        cols = k.getWidth();
        if (matrix != null && matrix.length < (rows * cols)) {
            matrix = null;
        }
        matrix = k.getKernelData(matrix);
    }

    // the kernel for the convolution currently being executed
    private float[] matrix;
    private int rows, cols;

    @Override
    protected void filterPixels(int[] srcPixels, int[] dstPixels, int width, int height, int y0, int rowsToFilter) {
        final int rows = this.rows;
        final int cols = this.cols;

//		if( rows == 1 ) {
//			filterColumn( srcPixels, dstPixels, width, height, y0, rowsToFilter );
//			return;
//		} else if( cols == 1 ) {
//			filterRow( srcPixels, dstPixels, width, height, y0, rowsToFilter );
//			return;
//		}
        final float[] matrix = this.matrix;
        final int rows2 = rows / 2;
        final int cols2 = cols / 2;
        final int rowLimit = rows2 + (rows & 1);
        final int colLimit = cols2 + (cols & 1);

        final boolean filterAlpha = isAlphaFiltered();
        final EdgeHandling edgeHandling = getEdgeHandling();
        final int y1 = y0 + rowsToFilter;

        int dstIndex = y0 * width;

        for (int y = y0; y < y1; ++y) {
            for (int x = 0; x < width; ++x) {
                float r = 0f, g = 0f, b = 0f, a = 0f;

                for (int row = -rows2; row < rowLimit; row++) {
                    int iy = y + row;
                    int ioffset;
                    if (iy >= 0 && iy < height) {
                        ioffset = iy * width;
                    } else {
                        switch (edgeHandling) {
                            case ZERO:
                                continue;
                            case REPEAT:
                                ioffset = y * width;
                                break;
                            case WRAP:
                                ioffset = ((iy + height) % height) * width;
                                break;
                            default:
                                throw new AssertionError();
                        }
                    }

                    int moffset = cols * (row + rows2) + cols2;

                    // For the inner loop, check if any input pixel will be
                    // off the image; if not, use an optimized loop
                    if ((x - cols2) > 0 && (x + colLimit) <= width) {
                        // Optimized loop: matrix row is never off the image edge
                        for (int col = -cols2; col < colLimit; col++) {
                            float f = matrix[moffset + col];
                            if (f != 0) {
                                final int ix = x + col;
                                final int argb = srcPixels[ioffset + ix];
                                a += f * (argb >>> 24);
                                r += f * ((argb >> 16) & 0xff);
                                g += f * ((argb >> 8) & 0xff);
                                b += f * (argb & 0xff);
                            }
                        }
                    } else {
                        // Edge loop: matrix row is sometimes off the image edge
                        for (int col = -cols2; col < colLimit; col++) {
                            float f = matrix[moffset + col];

                            if (f != 0) {
                                int ix = x + col;
                                if (ix < 0 || ix >= width) {
                                    switch (edgeHandling) {
                                        case ZERO:
                                            continue;
                                        case REPEAT:
                                            ix = x;
                                            break;
                                        case WRAP:
                                            ix = (x + width) % width;
                                            break;
                                        default:
                                            throw new AssertionError();
                                    }
                                }

                                final int argb = srcPixels[ioffset + ix];
                                a += f * (argb >>> 24);
                                r += f * ((argb >> 16) & 0xff);
                                g += f * ((argb >> 8) & 0xff);
                                b += f * (argb & 0xff);
                            }
                        }
                    }
                }

                final int aInt = filterAlpha ? clampBoth((int) (a + 0.5f)) : 0xff;
                final int rInt = clampBoth((int) (r + 0.5f));
                final int gInt = clampBoth((int) (g + 0.5f));
                final int bInt = clampBoth((int) (b + 0.5f));

                dstPixels[dstIndex++] = (aInt << 24) | (rInt << 16) | (gInt << 8) | bInt;
            }
        }
    }

    @Override
    protected float workFactor() {
        if (matrix == null) {
            prepareKernel(getKernels()[0]);
        }
        return rows * cols;
    }

//	public static void main(String[] args) {
//		try {
//			BufferedImage bi = ImageIO.read( new File("d:\\in.png") );
//			bi = ImageUtilities.ensureIntRGBFormat( bi );
//			
//			Kernel k = new Kernel( 3, 3, new float[] {
//				0f,    -1f,  0f,
//				0f,    3f,  0f,
//				0f,    -1f,  0f
//			});
//
////			bi = new GaussianBlurFilter(1).filter( bi, null );
//			bi = new ConvolveFilter(k).filter( bi, null );
////			bi = new ConvolveOp(k).filter( bi, null );
//
//			ImageIO.write( bi, "png", new File("d:\\out.png") );
//		} catch( Throwable t ) {
//			t.printStackTrace();
//		}
//	}
}
