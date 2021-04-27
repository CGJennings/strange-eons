package ca.cgjennings.graphics.filters;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Automatically crops an image by trimming off edges that fail to satisfy a
 * test. The test is specified by a <i>mask</i> and a <i>condition</i>. Edges
 * are tested one row or column at a time, starting at the image edges and
 * working towards the image center. Each row or column is checked for any pixel
 * whose ARGB value fails to satisfy the statement
 * {@code pixelARGB & mask == condition}. Once any pixel in a row or column
 * fails to satisfy the test, trimming along that edge stops.
 *
 * <p>
 * <table border=0>
 * <caption>Some example <i>mask</i> and <i>condition</i> values</caption>
 * <tr><th>Mask</th>       <th>Condition</th>  <th>Effect</th></tr>
 * <tr><td><code>0xff000000 </code></td> <td><code>0x00000000 </code></td>
 * <td>Trim transparent (alpha=0) edges</td></tr>
 * <tr><td><code>0xffffffff </code></td> <td><code>0xffffffff </code></td>
 * <td>Trim solid white edges</td></tr>
 * <tr><td><code>0xffffffff </code></td> <td><code>0xff000000 </code></td>
 * <td>Trim solid black edges</td></tr>
 * <tr><td><code>0x00ffffff </code></td> <td><code>0x00RRGGBB </code></td>
 * <td>Trim edges with colour RRGGBB, ignoring transparency</td></tr>
 * </table>
 *
 * <p>
 * Since the purpose of this filter is to resize an image in a way that cannot
 * be known before the filter is applied, it behaves differently from a typical
 * filter. The destination image parameter is always ignored. If the trimmed
 * image is smaller than the source image, a new image is created at the trimmed
 * size and returned. If no trimming is required, the source image is returned
 * unchanged.
 *
 * <p>
 * <b>In-place filtering:</b> This filter may or may not be applied in place;
 * see previous paragraph.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class TrimFilter extends AbstractImageFilter {

    private int mask = 0xff00_0000;
    private int condition;

    /**
     * Creates a new filter that trims fully transparent edges off of images.
     * That is, with a mask of <code>0xff000000</code> and a condition of
     * <code>0xff000000</code>.
     */
    public TrimFilter() {
    }

    /**
     * Creates a new trim filter with the specified mask and condition.
     *
     * @param mask the mask value for trim testing
     * @param condition the value that masked edges must satisfy to be trimmed
     */
    public TrimFilter(int mask, int condition) {
        this.mask = mask;
        this.condition = condition;
    }

    /**
     * Filters the image, returning an image with trimmable edges cropped off.
     * The destination image is ignored, as described in the
     * {@linkplain TrimFilter class description}. If the trim operation would
     * result in an empty image, the filter returns <code>null</code>.
     *
     * @param sourceImage the image to trim
     * @param ignoredDestination this parameter is ignored
     * @return a new, trimmed image; or the source image if no edges are
     * trimmed; or <code>null</code> if the entire image is trimmed
     */
    @Override
    @SuppressWarnings("empty-statement")
    public BufferedImage filter(BufferedImage sourceImage, BufferedImage ignoredDestination) {
        // Special case: if the test requires an image with transparency,
        // check if the image is opaque, and if so return it immediately.
        if ((mask & 0xff00_0000) != (condition & 0xff00_0000)) {
            if (sourceImage.getTransparency() == BufferedImage.OPAQUE) {
                return sourceImage;
            }
        }

        int w = sourceImage.getWidth();
        int h = sourceImage.getHeight();
        int tl, tr, tb, tt;

        int[] pixels = new int[Math.max(sourceImage.getWidth(), sourceImage.getHeight())];

        // find trim bounds: could easily be optimized if needed
        for (tt = 0; tt < h && isTrimmableRow(sourceImage, tt, pixels); ++tt);
        if (tt >= h) {
            return null;
        }
        for (tb = h - 1; tb >= 0 && isTrimmableRow(sourceImage, tb, pixels); --tb);

        for (tl = 0; tl < w && isTrimmableColumn(sourceImage, tl, pixels, tt, tb); ++tl);
        for (tr = w - 1; tr >= 0 && isTrimmableColumn(sourceImage, tr, pixels, tt, tb); --tr);

        BufferedImage trimmedImage;
        if (w == tr - tl + 1 && h == tb - tt + 1) {
            // nothing to trim
            trimmedImage = sourceImage;
        } else {
            w = tr - tl + 1;
            h = tb - tt + 1;

            int type = BufferedImage.TYPE_INT_ARGB;
            if (sourceImage.getTransparency() == BufferedImage.OPAQUE) {
                type = BufferedImage.TYPE_INT_RGB;
            }

            trimmedImage = new BufferedImage(w, h, type);
            Graphics2D g = trimmedImage.createGraphics();
            try {
                // Graphics implementation seems to interpret the sx2/sy2/dx2/dy2
                // parameters as if they were widths and heights (w=sx2-sx1+1) instead
                // of corners; hence we define rectangles one larger in both dimensions
                // for both source and dest as otherwise the copied area is too small
                g.drawImage(sourceImage, 0, 0, w, h, tl, tt, tr + 1, tb + 1, null);
            } finally {
                g.dispose();
            }
        }
        return trimmedImage;
    }

    private boolean isTrimmableRow(BufferedImage img, int row, int[] pixels) {
        final int mask = this.mask;
        final int condition = this.condition;
        final int w = img.getWidth();
        getARGB(img, 0, row, w, 1, pixels);
        for (int x = 0; x < w; ++x) {
            if ((pixels[x] & mask) != condition) {
                return false;
            }
        }
        return true;
    }

    private boolean isTrimmableColumn(BufferedImage img, int col, int[] pixels, int y0, int y1) {
        final int mask = this.mask;
        final int condition = this.condition;
        final int h = y1 - y0 + 1; // only check part of column between trimmed top and bottom
        getARGB(img, col, y0, 1, h, pixels);
        for (int i = 0; i < h; ++i) { // y = y0 + i
            if ((pixels[i] & mask) != condition) {
                return false;
            }
        }
        return true;
    }
}
