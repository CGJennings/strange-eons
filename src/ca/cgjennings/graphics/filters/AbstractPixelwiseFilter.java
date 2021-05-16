package ca.cgjennings.graphics.filters;

import ca.cgjennings.algo.SplitJoin;
import java.awt.image.*;

/**
 * An abstract superclass for filters that work on a pixel-by-pixel basis, where
 * the new value of a given pixel is independent of every other pixel and the
 * location of the pixel in the image. For efficiency, the filtering method is
 * called with arrays of ARGB values to filter rather than being called once for
 * each pixel.
 *
 * <p>
 * <b>In-place filtering:</b> Unless otherwise noted, filters based on this
 * class support in-place filtering (the source and destination images can be
 * the same).
 *
 * <p>
 * <b>Note:</b> Concrete subclasses may override {@link #filter} in order to
 * perform setup or cleanup steps before or after filtering begins, but should
 * call the superclass to execute the filter. The superclass will automatically
 * perform filtering in parallel on systems with multiple CPUs, and may also use
 * other techniques to accelerate filtering.
 */
public abstract class AbstractPixelwiseFilter extends AbstractImageFilter {

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        if ((src.getWidth() * src.getHeight()) * workFactor() >= Tuning.PER_ROW) {
            return filterParallel(src, dest);
        }
        return filterSerial(src, dest);
    }

    private BufferedImage filterSerial(BufferedImage source, BufferedImage dest) {
        int width = source.getWidth();
        int height = source.getHeight();
        int type = source.getType();
        WritableRaster sourceRaster = source.getRaster();

        if (dest == null) {
            dest = createCompatibleDestImage(source, null);
        }
        WritableRaster destRaster = dest.getRaster();

        int[] pixelRow = new int[width];
        for (int y = 0; y < height; ++y) {
            if (type == BufferedImage.TYPE_INT_RGB) {
                sourceRaster.getDataElements(0, y, width, 1, pixelRow);
                for (int i = 0; i < width; ++i) {
                    pixelRow[i] |= 0xff00_0000;
                }
                filterPixels(pixelRow, 0, pixelRow.length);
                destRaster.setDataElements(0, y, width, 1, pixelRow);
            } else if (type == BufferedImage.TYPE_INT_ARGB) {
                sourceRaster.getDataElements(0, y, width, 1, pixelRow);
                filterPixels(pixelRow, 0, pixelRow.length);
                destRaster.setDataElements(0, y, width, 1, pixelRow);
            } else {
                source.getRGB(0, y, width, 1, pixelRow, 0, width);
                filterPixels(pixelRow, 0, pixelRow.length);
                dest.setRGB(0, y, width, 1, pixelRow, 0, width);
            }
        }

        return dest;
    }

    private BufferedImage filterParallel(BufferedImage source, BufferedImage dest) {
        if (dest == null) {
            dest = createCompatibleDestImage(source, null);
        }
        final int h = source.getHeight();
        SplitJoin sj = SplitJoin.getInstance();
        final int n = Math.min(h, sj.getIdealSplitCount());
        final int rowsPerTask = h / n;
        final int remainder = h - n * rowsPerTask;
        Runnable[] units = new Runnable[n];
        int y = 0;
        for (int i = 0; i < n; ++i) {
            int rows = rowsPerTask;
            if (i < remainder) {
                ++rows;
            }
            units[i] = new RowwiseWorkUnit(source, dest, y, rows) {
                @Override
                protected void process(int y, int[] argb) {
                    filterPixels(argb, 0, argb.length);
                }
            };
            y += rows;
        }
        sj.runUnchecked(units);
        return dest;
    }

    /**
     * Filters an array of raw pixel data in place. This allows other filters
     * that operate on arrays of pixel data to make use of the filter as part of
     * their internal processing without the need to write data into a temporary
     * image. Like the standard {@link #filter} method, this method will
     * automatically perform filtering in parallel when appropriate.
     *
     * @param argb the ARGB pixel values to filter; the filtered results will
     * overwrite these values
     */
    public void filter(int[] argb) {
        if (argb.length * workFactor() >= Tuning.PER_ROW) {
            SplitJoin sj = SplitJoin.getInstance();
            final int len = argb.length;
            final int n = Math.min(len, sj.getIdealSplitCount());
            final int elsPerTask = len / n;
            final int remainder = len - n * elsPerTask;
            Runnable[] units = new Runnable[n];
            int start = 0;
            for (int i = 0; i < n; ++i) {
                int els = elsPerTask;
                if (i < remainder) {
                    ++els;
                }
                units[i] = new InlineWorkUnit(argb, start, start + els);
                start += els;
            }
            sj.runUnchecked(units);
        } else {
            filterPixels(argb, 0, argb.length);
        }
    }

    /**
     * Helper class for {@link #filterPixels}.
     */
    private class InlineWorkUnit implements Runnable {

        private final int[] data;
        private final int start;
        private int end;

        public InlineWorkUnit(int[] data, int start, int end) {
            this.data = data;
            this.start = start;
            this.end = end;
        }

        @Override
        public void run() {
            filterPixels(data, start, end);
        }
    }

    /**
     * This method is called with a block of ARGB values to be filtered.
     * Subclasses must override this method to implement the actual filtering
     * algorithm by replacing each pixel value in the range
     * {@code argb[start] ... argb[end-1]} with the filtered value.
     *
     * @param argb an array of pixel data to filter
     * @param start the index of the first pixel to filter
     * @param end the index of the last pixel to filter, plus one
     */
    public abstract void filterPixels(int[] argb, int start, int end);

    /**
     * Returns the result of applying the filter to a single ARGB pixel value.
     *
     * <p>
     * The base class implementation creates a singleton array containing the
     * pixel value and passes this to {@link #filterPixels}, returning the
     * result. Subclasses may wish override this to provide a more efficient
     * implementation.
     *
     * @param argb the pixel value to filter
     * @return the filtered pixel value
     */
    public int filterPixel(int argb) {
        int[] pixel = new int[]{argb};
        filterPixels(pixel, 0, 1);
        return pixel[0];
    }

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
