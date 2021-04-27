package ca.cgjennings.graphics.filters;

import ca.cgjennings.algo.SplitJoin;
import java.awt.image.BufferedImage;

/**
 * An abstract base class for image filters that require access to the entire
 * image at once when processing. Filters of this type produce images of the
 * same dimensions as the original.
 *
 * <p>
 * It is assumed that pixels will only be read from the source image and written
 * to the destination image. That is, that pixel values depend only on
 * combinations of the source pixels and not on previously computed destination
 * pixels. This allows filtering to be accelerated automatically by generating
 * blocks of destination pixels in parallel. A filter that does not meet these
 * criteria may return a value of 0 from {@link #workFactor()} to disable the
 * automatic acceleration mechanism.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public abstract class AbstractImagewiseFilter extends AbstractImageFilter {

    public AbstractImagewiseFilter() {
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation will call {@link #filterPixels} to perform the actual
     * filtering.
     *
     * @param source the source image
     * @param destination the destination image (may be <code>null</code>)
     * @return the destination image
     */
    @Override
    public BufferedImage filter(BufferedImage source, BufferedImage destination) {
        final int width = source.getWidth();
        final int height = source.getHeight();

        if (destination == null) {
            destination = createCompatibleDestImage(source, null);
        }

        final int[] srcPixels = getARGB(source, null);
        final int[] dstPixels = filter(srcPixels, null, width, height);
        setARGB(destination, dstPixels);

        return destination;
    }

    /**
     * Applies the filter to ARGB pixel data stored in an array. This method can
     * be used during the internal processing of other filters.
     *
     * @param source the source pixel data in ARGB format
     * @param destination the destination in which the filtered should be
     * stored, may be <code>null</code>
     * @param width the width of the source image
     * @param height the height of the source image
     * @return the array that holds the destination pixels
     */
    public int[] filter(final int[] source, int[] destination, final int width, final int height) {
        final int[] dest = destination == null ? new int[width * height] : destination;

        if ((width * height) * workFactor() > Tuning.PER_IMAGE) {
            SplitJoin sj = SplitJoin.getInstance();
            final int n = Math.min(height, sj.getIdealSplitCount());
            final int rowsPerTask = height / n;
            final int remainder = height - n * rowsPerTask;
            Runnable[] units = new Runnable[n];
            int y = 0;
            for (int i = 0; i < n; ++i) {
                int rows = rowsPerTask;
                if (i < remainder) {
                    ++rows;
                }

                final int y0 = y;
                final int rowCount = rows;
                units[i] = () -> {
                    filterPixels(source, dest, width, height, y0, rowCount);
                };

                y += rows;
            }
            sj.runUnchecked(units);
        } else {
            filterPixels(source, dest, width, height, 0, height);
        }
        return dest;
    }

    /**
     * Filters a block of rows in the source image, placing the result in the
     * corresponding rows in the destination image. The block of rows to be
     * filtered runs from y0 to y0 + rows-1 (inclusive).
     *
     * @param srcPixels the pixel data for the source image
     * @param dstPixels the destination for output pixels
     * @param width the width of the image
     * @param height the height of the image
     * @param y0 the index of the first row to filter
     * @param rows the number of rows to filter
     */
    protected abstract void filterPixels(int[] srcPixels, int[] dstPixels, int width, int height, int y0, int rows);

    /**
     * Returns a factor representing the amount of work performed by this filter
     * relative to a filter that simply copies the source image by reading and
     * writing each pixel. For example, a filter that implemented a 3x3
     * convolution might return the value 9. The work factor value helps
     * determine when an image should be processed in parallel. (There is
     * significant overhead involved in running a filter in parallel, so it is
     * only worth doing if the image is relatively large or the amount of
     * processing per pixel is relatively high.)
     *
     * <p>
     * <b>Note:</b> The work factor may vary depending on the current filter
     * settings.
     *
     * @return the approximate amount of work per pixel, relative to simply
     * copying the pixel values
     */
    protected float workFactor() {
        return 1f;
    }
}
