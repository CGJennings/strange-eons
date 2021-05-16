package ca.cgjennings.graphics.composites;

import ca.cgjennings.algo.SplitJoin;
import ca.cgjennings.graphics.filters.Tuning;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * This abstract implementation of {@code CompositeContext} is designed to
 * handle RGB/ARGB data. Subclasses must implement the abstract general case
 * method, and may optionally override other methods to provide special case
 * code for use when the {@code VALUE_ALPHA_INTERPOLATION_SPEED} hint is
 * set and/or the source and destination both have 4 bands (ARGB). The context
 * will transparently make use of multiple CPUs to increase compositing
 * performance, so subclasses must ensure that their implementations of
 * {@code compose} methods can be called concurrently from multiple
 * threads.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public abstract class AbstractCompositeContext implements CompositeContext {

    protected final float alpha;
    protected final ColorModel srcColorModel;
    protected final ColorModel dstColorModel;
    protected final RenderingHints renderingHints;
    protected final boolean fast;

    public AbstractCompositeContext(float alpha, ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
        this.alpha = alpha;
        this.srcColorModel = srcColorModel;
        this.dstColorModel = dstColorModel;
        renderingHints = hints;
        fast = hints.get(RenderingHints.KEY_ALPHA_INTERPOLATION) == RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED;
    }

    @Override
    public void compose(Raster top, Raster bottom, WritableRaster dest) {
        final int x = dest.getMinX();
        final int w = dest.getWidth();

        final int y1 = dest.getMinY();
        final int h = dest.getHeight();
        final int y2 = y1 + h - 1;

        final int sBands = top.getNumBands();
        final int dBands = bottom.getNumBands();
        final int aFactor = clamp(Math.round(alpha * 255));

        if (sBands != 3 && sBands != 4) {
            throw new UnsupportedOperationException("unsupported number of bands in source raster: " + sBands);
        }
        if (dBands != 3 && dBands != 4) {
            throw new UnsupportedOperationException("unsupported number of bands in destination raster: " + dBands);
        }

        boolean parallel = w > Tuning.PER_ROW / h;

        if (parallel) {
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
                units[i] = new CompositingUnit(dest, top, bottom, y, y + rows - 1, x, w, aFactor, sBands, dBands);
                y += rows;
            }
            sj.runUnchecked(units);
        } // serial processing
        else if (sBands == 4 && dBands == 4) {
            if (fast) {
                int[] src = null;
                int[] dst = null;
                for (int y = y1; y <= y2; y++) {
                    src = top.getPixels(x, y, w, 1, src);
                    dst = bottom.getPixels(x, y, w, 1, dst);
                    composeF44(src, dst, aFactor);
                    dest.setPixels(x, y, w, 1, dst);
                }

            } else {
                int[] src = null;
                int[] dst = null;
                for (int y = y1; y <= y2; y++) {
                    src = top.getPixels(x, y, w, 1, src);
                    dst = bottom.getPixels(x, y, w, 1, dst);
                    compose44(src, dst, aFactor);
                    dest.setPixels(x, y, w, 1, dst);
                }
            }
        } else {
            if (fast) {
                int[] src = null;
                int[] dst = null;
                for (int y = y1; y <= y2; y++) {
                    src = top.getPixels(x, y, w, 1, src);
                    dst = bottom.getPixels(x, y, w, 1, dst);
                    composeFxx(src, dst, sBands, dBands, aFactor);
                    dest.setPixels(x, y, w, 1, dst);
                }
            } else {
                int[] src = null;
                int[] dst = null;
                for (int y = y1; y <= y2; y++) {
                    src = top.getPixels(x, y, w, 1, src);
                    dst = bottom.getPixels(x, y, w, 1, dst);
                    compose(src, dst, sBands, dBands, aFactor);
                    dest.setPixels(x, y, w, 1, dst);
                }
            }
        }
    }

    private class CompositingUnit implements Runnable {

        private final WritableRaster dest;
        private final Raster top;
        private Raster bottom;
        private final int y1;
        private int y2, x, w, aFactor;
        private final int sBands;
        private int dBands;

        public CompositingUnit(WritableRaster dest, Raster top, Raster bottom, int y1, int y2, int x, int w, int aFactor, int sBands, int dBands) {
            this.dest = dest;
            this.top = top;
            this.bottom = bottom;
            this.y1 = y1;
            this.y2 = y2;
            this.x = x;
            this.w = w;
            this.aFactor = aFactor;
            this.sBands = sBands;
            this.dBands = dBands;
        }

        @Override
        public void run() {
            if (sBands == 4 && dBands == 4) {
                if (fast) {
                    int[] src = null;
                    int[] dst = null;
                    for (int y = y1; y <= y2; y++) {
                        src = top.getPixels(x, y, w, 1, src);
                        dst = bottom.getPixels(x, y, w, 1, dst);
                        composeF44(src, dst, aFactor);
                        dest.setPixels(x, y, w, 1, dst);
                    }
                } else {
                    int[] src = null;
                    int[] dst = null;
                    for (int y = y1; y <= y2; y++) {
                        src = top.getPixels(x, y, w, 1, src);
                        dst = bottom.getPixels(x, y, w, 1, dst);
                        compose44(src, dst, aFactor);
                        dest.setPixels(x, y, w, 1, dst);
                    }
                }
            } else {
                if (fast) {
                    int[] src = null;
                    int[] dst = null;
                    for (int y = y1; y <= y2; y++) {
                        src = top.getPixels(x, y, w, 1, src);
                        dst = bottom.getPixels(x, y, w, 1, dst);
                        composeFxx(src, dst, sBands, dBands, aFactor);
                        dest.setPixels(x, y, w, 1, dst);
                    }
                } else {
                    int[] src = null;
                    int[] dst = null;
                    for (int y = y1; y <= y2; y++) {
                        src = top.getPixels(x, y, w, 1, src);
                        dst = bottom.getPixels(x, y, w, 1, dst);
                        compose(src, dst, sBands, dBands, aFactor);
                        dest.setPixels(x, y, w, 1, dst);
                    }
                }
            }
        }
    }

    /**
     * Subclasses must override this to perform general case compositing.
     *
     * @param src the source data
     * @param dst the destination data
     * @param sBands the number of source bands
     * @param dBands the number of destination bands
     */
    protected abstract void compose(int[] src, int[] dst, int sBands, int dBands, int aFactor);

    /**
     * This method can be overidden to provide fast special case code for images
     * with variable bands in both source and destination when the alpha
     * interpolation rendering hint is set for speed. The default implementation
     * forwards to {@code compose( src, dst, sBands, dBands, aFactor )}
     *
     * @param src the source data to mix
     * @param dst the destination to be mixed into
     */
    protected void composeFxx(int[] src, int[] dst, int sBands, int dBands, int aFactor) {
        compose(src, dst, sBands, dBands, aFactor);
    }

    /**
     * This method can be overidden to provide fast special case code for images
     * with 4 bands (A+RGB) in both source and destination. The default
     * implementation forwards to {@code composeF44( src, dst, aFactor )}
     *
     * @param src the source data to mix
     * @param dst the destination to be mixed into
     */
    protected void compose44(int[] src, int[] dst, int aFactor) {
        composeF44(src, dst, aFactor);
    }

    /**
     * This method can be overidden to provide fast special case code for images
     * with 4 bands (A+RGB) in both source and destination when the alpha
     * interpolation rendering hint is set for speed. The default implementation
     * forwards to {@code compose( src, dst, 4, 4, aFactor )}
     *
     * @param src the source data to mix
     * @param dst the destination to be mixed into
     */
    protected void composeF44(int[] src, int[] dst, int aFactor) {
        compose(src, dst, 4, 4, aFactor);
    }

    @Override
    public void dispose() {
    }

    protected static int clamp(int a) {
        return a < 0 ? 0 : (a > 255 ? 255 : a);
    }

    protected static int mul(int a, int b) {
        int i = a * b + 0x80;
        return ((i >> 8) + i) >> 8;
    }

    protected static int div(int a, int b) {
        return b == 0 ? a : a / b;
    }

    protected static int mix(int aFactor, int b) {
        return aFactor * b / 255;
    }

    protected static int fmix(int aFactor, int b) {
        return aFactor * b >>> 8;
    }
}
