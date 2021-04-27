package ca.cgjennings.graphics.filters;

import ca.cgjennings.algo.SplitJoin;
import java.awt.image.*;

/**
 * An abstract superclass for filters that work on a row-by-row basis. It is
 * similar to {@link AbstractPixelwiseFilter} but it guarantees that an entire
 * row is filtered at a time and it provides the current y-index.
 */
public abstract class AbstractRowwiseFilter extends AbstractImageFilter {

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
                filterPixels(y, pixelRow);
                destRaster.setDataElements(0, y, width, 1, pixelRow);
            } else if (type == BufferedImage.TYPE_INT_ARGB) {
                sourceRaster.getDataElements(0, y, width, 1, pixelRow);
                filterPixels(y, pixelRow);
                destRaster.setDataElements(0, y, width, 1, pixelRow);
            } else {
                source.getRGB(0, y, width, 1, pixelRow, 0, width);
                filterPixels(y, pixelRow);
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
                    filterPixels(y, argb);
                }
            };
            y += rows;
        }
        sj.runUnchecked(units);
        return dest;
    }

    /**
     * This method is called with a block of ARGB values to be filtered.
     * Subclasses should override this method to implement the actual filtering
     * algorithm, replacing each pixel in the supplied array with its filtered
     * value.
     */
    public abstract void filterPixels(int y, int[] argb);

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
