package ca.cgjennings.graphics.filters;

import ca.cgjennings.algo.SplitJoin;
import ca.cgjennings.graphics.ImageUtilities;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * An abstract base class for filtering images. While {@code BufferedImageOp}s
 * are generally designed to operate on images that use any storage format or
 * colour model, {@code AbstractImageFilter} is optimized to work with
 * 32-bit ARGB image data ({@code BufferedImage.TYPE_INT_ARGB}). Images
 * stored in other formats will typically be converted to and from ARGB data as
 * necessary. This greatly simplifies the task of writing an image filter,
 * though at the cost of conversion (when required).
 *
 * <p>
 * This base class provides default implementations for many methods of
 * {@code BufferedImageOp}. These default implementations are suitable for any
 * operation that does not change the shape or size of the image. Most concrete
 * filters will not subclass this base class directly, but will instead subclass
 * one of {@link AbstractPixelwiseFilter},
 * {@link AbstractRowwiseFilter}, or {@link AbstractImagewiseFilter}.
 *
 * <p>
 * <b>Performance note:</b> As a side effect of automatic conversion, image
 * types other than {@code TYPE_INT_ARGB} and {@code TYPE_INT_RGB}
 * will become <i>unmanaged images</i> as a result of the filtering process.
 * Unmanaged images cannot be hardware accelerated, and thus are typically much
 * slower to draw. The best way to maximize performance is to ensure that all
 * images are in one of the optimal formats. If you are unsure about a
 * particular image, use
 * {@link ImageUtilities#ensureIntRGBFormat(java.awt.image.BufferedImage)}.
 */
public abstract class AbstractImageFilter implements BufferedImageOp {

    public AbstractImageFilter() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedImage createCompatibleDestImage(BufferedImage source, ColorModel destinationColorModel) {
        if (destinationColorModel == null) {
            destinationColorModel = source.getColorModel();
        }
        return new BufferedImage(
                destinationColorModel,
                destinationColorModel.createCompatibleWritableRaster(source.getWidth(), source.getHeight()),
                destinationColorModel.isAlphaPremultiplied(),
                null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The base implementation returns the bounds of the image in image space;
     * that is, the rectangle defined by
     * {@code 0, 0, source.getWidth(), source.getHeight()}.
     */
    @Override
    public Rectangle2D getBounds2D(BufferedImage source) {
        return new Rectangle(0, 0, source.getWidth(), source.getHeight());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The base implementation returns a copy of the original point.
     */
    @Override
    public Point2D getPoint2D(Point2D sourcePoint, Point2D destPoint) {
        if (destPoint == null) {
            destPoint = new Point2D.Double();
        }
        destPoint.setLocation(sourcePoint.getX(), sourcePoint.getY());
        return destPoint;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The base implementation returns {@code null}.
     */
    @Override
    public RenderingHints getRenderingHints() {
        return null;
    }

    /**
     * Clamps an input pixel value to the range 0..255.
     *
     * @param pixelByte the pixel value
     * @return max(0,min(pixelByte,255))
     */
    public static int clampBoth(int pixelByte) {
        if (pixelByte > 255) {
            return 255;
        }
        if (pixelByte < 0) {
            return 0;
        }
        return pixelByte;
    }

    /**
     * Clamps an input pixel value to a maximum of 255.
     *
     * @param pixelByte the pixel value
     * @return min(pixelByte,255)
     */
    public static int clamp(int pixelByte) {
        if (pixelByte > 255) {
            return 255;
        }
        return pixelByte;
    }

    /**
     * Clamps an integer value to the range low..high inclusive.
     *
     * @param value the value to clamp
     * @param low the minimum value
     * @param high the maximum value
     * @return the clamped value
     */
    public static int clamp(int value, int low, int high) {
        return (value < low) ? low : (value > high) ? high : value;
    }

    /**
     * Clamps an input pixel value to a maximum of 255, converting it from float
     * to int in the process.
     *
     * @param pixelByte the pixel value
     * @return min( (int) (pixelByte+0.5f),255)
     */
    public static int clamp(float pixelByte) {
        final int p = (int) (pixelByte + 0.5f);
        if (p > 255) {
            return 255;
        }
        return p;
    }

    private static final float[] fTable = new float[256];

    static {
        for (int i = 0; i < fTable.length; ++i) {
            fTable[i] = i / 255f;
        }
    }

    /**
     * Returns the alpha component of a packed ARGB pixel as a float value
     * between 0 and 1. This value is equivalent to the following calculation,
     * (though possibly more efficient):
     * <pre>(float) ((argb &gt;&gt; 24) &amp; 0xff) / 255f</pre>
     *
     * @param argb the 32-bit pixel value
     * @return the alpha component as a unit value
     */
    public static float fA(int argb) {
        return fTable[argb >>> 24];
    }

    /**
     * Returns the red component of a packed ARGB pixel as a float value between
     * 0 and 1. This value is equivalent to the following calculation, (though
     * possibly more efficient):
     * <pre>(float) ((argb &gt;&gt; 16) &amp; 0xff) / 255f</pre>
     *
     * @param argb the 32-bit pixel value
     * @return the red component as a unit value
     */
    public static float fR(int argb) {
        return fTable[(argb >> 16) & 0xff];
    }

    /**
     * Returns the green component of a packed ARGB pixel as a float value
     * between 0 and 1. This value is equivalent to the following calculation,
     * (though possibly more efficient):
     * <pre>(float) ((argb &gt;&gt; 8) &amp; 0xff) / 255f</pre>
     *
     * @param argb the 32-bit pixel value
     * @return the green component as a unit value
     */
    public static float fG(int argb) {
        return fTable[(argb >> 8) & 0xff];
    }

    /**
     * Returns the blue component of a packed ARGB pixel as a float value
     * between 0 and 1. This value is equivalent to the following calculation,
     * (though possibly more efficient):
     * <pre>(float) (argb &amp; 0xff) / 255f</pre>
     *
     * @param argb the 32-bit pixel value
     * @return the blue component as a unit value
     */
    public static float fB(int argb) {
        return fTable[argb & 0xff];
    }

    /**
     * Converts a floating point alpha value between 0 and 1 to a packed ARGB
     * alpha value. (The value is clamped to the range 0..255 and shifted left
     * 24 bits.)
     *
     * @param a the alpha component from 0 to 1
     * @return a packed ARGB alpha value
     */
    public static int iA(float a) {
        if (a <= 0f) {
            return 0;
        }
        int ia = (int) (a + 0.5f);
        if (ia > 255) {
            ia = 255;
        }
        return ia << 24;
    }

    /**
     * Converts a floating point red value between 0 and 1 to a packed ARGB red
     * value. (The value is clamped to the range 0..255 and shifted left 16
     * bits.)
     *
     * @param r the red component from 0 to 1
     * @return a packed ARGB red value
     */
    public static int iR(float r) {
        if (r <= 0f) {
            return 0;
        }
        int ir = (int) (r + 0.5f);
        if (ir > 255) {
            ir = 255;
        }
        return ir << 16;
    }

    /**
     * Converts a floating point green value between 0 and 1 to a packed ARGB
     * green value. (The value is clamped to the range 0..255 and shifted left 8
     * bits.)
     *
     * @param g the green component from 0 to 1
     * @return a packed ARGB green value
     */
    public static int iG(float g) {
        if (g <= 0f) {
            return 0;
        }
        int ig = (int) (g + 0.5f);
        if (ig > 255) {
            ig = 255;
        }
        return ig << 8;
    }

    /**
     * Converts a floating point blue value between 0 and 1 to a packed ARGB
     * blue value. (The value is clamped to the range 0..255.)
     *
     * @param b the blue component from 0 to 1
     * @return a packed ARGB blue value
     */
    public static int iB(float b) {
        if (b <= 0f) {
            return 0;
        }
        int ib = (int) (b + 0.5f);
        if (ib > 255) {
            ib = 255;
        }
        return ib;
    }

    /**
     * Fetches pixel data from an image in ARGB format. The data are converted
     * from the source format, if required.
     *
     * @param image the image to get pixels from
     * @param x the x-offset of the subimage
     * @param y the y-offset of the subimage
     * @param width the width of the subimage
     * @param height the height of the subimage
     * @param pixels an array to use; may be {@code null}
     * @return an array of pixel data for the subimage
     */
    public static int[] getARGB(BufferedImage image, int x, int y, int width, int height, int[] pixels) {
        final int type = image.getType();
        float threshold;
        if (type == BufferedImage.TYPE_INT_ARGB) {
            threshold = Tuning.GET_INT_ARGB;
        } else if (type == BufferedImage.TYPE_INT_RGB) {
            threshold = Tuning.GET_INT_RGB;
        } else {
            threshold = Tuning.GET_INT_OTHER;
        }

        if (width * height >= threshold) {
            SplitJoin sj = SplitJoin.getInstance();
            if (pixels == null) {
                pixels = new int[width * height];
            }
            final int n = Math.min(height, sj.getIdealSplitCount());
            final int rowsPerTask = height / n;
            final int remainder = height - n * rowsPerTask;
            Runnable[] units = new Runnable[n];
            int y0 = y;
            for (int i = 0; i < n; ++i) {
                int rows = rowsPerTask;
                if (i < remainder) {
                    ++rows;
                }
                units[i] = new RowGetterUnit(image, pixels, y0, rows);
                y0 += rows;
            }
            sj.runUnchecked(units);
            return pixels;
        } else {
            return getARGBSynch(image, x, y, width, height, pixels);
        }
    }

    /**
     * Fetches pixel data from an image in ARGB format. The data are converted
     * from the source format, if required. This method does not perform
     * parallel processing.
     *
     * @param image the image to get pixels from
     * @param x the x-offset of the subimage
     * @param y the y-offset of the subimage
     * @param width the width of the subimage
     * @param height the height of the subimage
     * @param pixels an array to use; may be {@code null}
     * @return an array of pixel data for the subimage
     */
    protected static int[] getARGBSynch(BufferedImage image, int x, int y, int width, int height, int[] pixels) {
        final int type = image.getType();
        if (type == BufferedImage.TYPE_INT_ARGB) {
            return (int[]) image.getRaster().getDataElements(x, y, width, height, pixels);
        } else if (type == BufferedImage.TYPE_INT_ARGB) {
            pixels = (int[]) image.getRaster().getDataElements(x, y, width, height, pixels);
            int count = width * height;
            for (int i = 0; i < count; ++i) {
                pixels[i] |= 0xff00_0000;
            }
            return pixels;
        }
        return image.getRGB(x, y, width, height, pixels, 0, width);
    }

    /**
     * Stores ARGB pixel data in an image. The data are converted into the
     * destination format, if required. This method may perform parallel
     * processing.
     *
     * @param image the image to set pixels on
     * @param x the x-offset of the subimage
     * @param y the y-offset of the subimage
     * @param width the width of the subimage
     * @param height the height of the subimage
     * @param pixels an array to use
     */
    public static void setARGB(BufferedImage image, int x, int y, int width, int height, int[] pixels) {
        final int type = image.getType();
        float threshold;
        if (type == BufferedImage.TYPE_INT_ARGB) {
            threshold = Tuning.SET_INT_ARGB;
        } else if (type == BufferedImage.TYPE_INT_RGB) {
            threshold = Tuning.SET_INT_RGB;
        } else {
            threshold = Tuning.SET_INT_OTHER;
        }

        if (width * height >= threshold) {
            SplitJoin sj = SplitJoin.getInstance();
            final int n = Math.min(height, sj.getIdealSplitCount());
            final int rowsPerTask = height / n;
            final int remainder = height - n * rowsPerTask;
            Runnable[] units = new Runnable[n];
            int y0 = y;
            for (int i = 0; i < n; ++i) {
                int rows = rowsPerTask;
                if (i < remainder) {
                    ++rows;
                }
                units[i] = new RowSetterUnit(pixels, image, y0, rows);
                y0 += rows;
            }
            sj.runUnchecked(units);
        } else {
            setARGBSynch(image, x, y, width, height, pixels);
        }
    }

    /**
     * Stores ARGB pixel data in an image. The data are converted into the
     * destination format, if required. This method does not perform parallel
     * processing.
     *
     * @param image the image to set pixels on
     * @param x the x-offset of the subimage
     * @param y the y-offset of the subimage
     * @param width the width of the subimage
     * @param height the height of the subimage
     * @param pixels an array to use
     */
    protected static void setARGBSynch(BufferedImage image, int x, int y, int width, int height, int[] pixels) {
        final int type = image.getType();
        if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB) {
            image.getRaster().setDataElements(x, y, width, height, pixels);
        } else {
            image.setRGB(x, y, width, height, pixels, 0, width);
        }
    }

    /**
     * Stores the ARGB pixel data for an entire image in an array. The data are
     * converted from the source format, if required. This method may perform
     * the copying in parallel depending on image size.
     *
     * @param image the image to obtain a pixel array for
     * @param pixels the array to store the data in (may be null)
     */
    public static int[] getARGB(BufferedImage image, int[] pixels) {
        return getARGB(image, 0, 0, image.getWidth(), image.getHeight(), pixels);
    }

    /**
     * Replaces the data in an image with pixel values from an array. The data
     * are converted into the destination format, if required. This method may
     * perform the copying in parallel depending on image size.
     *
     * @param image the image to set pixels on
     * @param pixels an array of source pixels
     */
    public static void setARGB(BufferedImage image, int[] pixels) {
        setARGB(image, 0, 0, image.getWidth(), image.getHeight(), pixels);
    }

    /**
     * Work unit that copies rows into an array.
     */
    private static class RowGetterUnit implements Runnable {

        public RowGetterUnit(BufferedImage source, int[] destination, int y0, int rowCount) {
            this.y0 = y0;
            this.y1 = y0 + rowCount;
            this.source = source;
            this.dest = destination;
        }
        private final BufferedImage source;
        private final int[] dest;
        private final int y0;
        private int y1;

        @Override
        public void run() {
            final int y0 = this.y0;
            final int y1 = this.y1;
            final BufferedImage src = source;
            final int[] dst = dest;
            final int w = src.getWidth();
            final int[] pixelRow = new int[w];
            final int type = src.getType();

            // destination index
            int dx = y0 * w;
            if (type == BufferedImage.TYPE_INT_ARGB) {
                WritableRaster sourceRaster = src.getRaster();
                for (int y = y0; y < y1; ++y) {
                    sourceRaster.getDataElements(0, y, w, 1, pixelRow);
                    for (int x = 0; x < w; ++x) {
                        dest[dx++] = pixelRow[x];
                    }
                }
            } else if (type == BufferedImage.TYPE_INT_RGB) {
                WritableRaster sourceRaster = src.getRaster();
                for (int y = y0; y < y1; ++y) {
                    sourceRaster.getDataElements(0, y, w, 1, pixelRow);
                    for (int x = 0; x < w; ++x) {
                        dest[dx++] = pixelRow[x] | 0xff00_0000;
                    }
                }
            } else {
                for (int y = y0; y < y1; ++y) {
                    src.getRGB(0, y, w, 1, pixelRow, 0, w);
                    for (int x = 0; x < w; ++x) {
                        dest[dx++] = pixelRow[x];
                    }
                }
            }
        }
    }

    /**
     * Work unit that copies rows from an array to an image.
     */
    private static class RowSetterUnit implements Runnable {

        public RowSetterUnit(int[] source, BufferedImage destination, int y0, int rowCount) {
            this.y0 = y0;
            this.y1 = y0 + rowCount;
            this.source = source;
            this.dest = destination;
        }
        private final BufferedImage dest;
        private final int[] source;
        private final int y0;
        private int y1;

        @Override
        public void run() {
            final int y0 = this.y0;
            final int y1 = this.y1;
            final BufferedImage dest = this.dest;
            final int w = dest.getWidth();
            final int[] pixelRow = new int[w];
            final int type = dest.getType();
            int si = y0 * w;
            try {
                if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB) {
                    WritableRaster destRaster = dest.getRaster();
                    for (int y = y0; y < y1; ++y) {
                        for (int x = 0; x < w; ++x) {
                            pixelRow[x] = source[si++];
                        }
                        destRaster.setDataElements(0, y, w, 1, pixelRow);
                    }
                } else {
                    for (int y = y0; y < y1; ++y) {
                        for (int x = 0; x < w; ++x) {
                            pixelRow[x] = source[si++];
                        }
                        dest.setRGB(0, y, w, 1, pixelRow, 0, w);
                    }
                }
            } catch (Throwable t) {
                System.err.println("!!" + t);
                System.err.println("!!");
            }
        }
    }

    /**
     * A processing work unit that processes a block of rows but has access to
     * the full source image data. The run method should be overridden to
     * process the rows from y0 to y0+rows-1 inclusive, reading pixels from the
     * source and writing them to the destination.
     */
    static abstract class ImagewiseWorkUnit implements Runnable {

        protected int[] source;
        protected int[] dest;
        protected int width, height;
        protected int y0, rows;

        public ImagewiseWorkUnit(int[] source, int[] dest, int width, int height, int y0, int rows) {
            this.source = source;
            this.dest = dest;
            this.width = width;
            this.height = height;
            this.y0 = y0;
            this.rows = rows;
        }
    }

    /**
     * A processing work unit that extracts a range of rows from a source image,
     * processes them a row at a time, and writes the result to a destination
     * image.
     */
    static abstract class RowwiseWorkUnit implements Runnable {

        /**
         * Creates a work unit that processes rowCount rows starting at y0.
         *
         * @param source the source image
         * @param destination the destination image
         * @param y0 the start row
         * @param rows the number of rows to process
         */
        public RowwiseWorkUnit(BufferedImage source, BufferedImage destination, int y0, int rowCount) {
            this.y0 = y0;
            this.y1 = y0 + rowCount;
            this.source = source;
            this.dest = destination;
        }
        private final BufferedImage source;
        private BufferedImage dest;
        private final int y0;
        private int y1;

        @Override
        public void run() {
            final int y0 = this.y0;
            final int y1 = this.y1;
            final BufferedImage src = source;
            final BufferedImage dst = dest;
            final int w = source.getWidth();
            final int[] pixelRow = new int[w];
            final int type = source.getType();

            if (type == BufferedImage.TYPE_INT_ARGB) {
                WritableRaster sourceRaster = source.getRaster();
                WritableRaster destRaster = dest.getRaster();
                for (int y = y0; y < y1; ++y) {
                    sourceRaster.getDataElements(0, y, w, 1, pixelRow);
                    process(y, pixelRow);
                    destRaster.setDataElements(0, y, w, 1, pixelRow);
                }
            } else if (type == BufferedImage.TYPE_INT_RGB) {
                WritableRaster sourceRaster = source.getRaster();
                WritableRaster destRaster = dest.getRaster();
                for (int y = y0; y < y1; ++y) {
                    sourceRaster.getDataElements(0, y, w, 1, pixelRow);
                    for (int x = 0; x < w; ++x) {
                        pixelRow[x] |= 0xff00_0000;
                    }
                    process(y, pixelRow);
                    destRaster.setDataElements(0, y, w, 1, pixelRow);
                }
            } else {
                for (int y = y0; y < y1; ++y) {
                    source.getRGB(0, y, w, 1, pixelRow, 0, w);
                    process(y, pixelRow);
                    dest.setRGB(0, y, w, 1, pixelRow, 0, w);
                }
            }
        }

        protected abstract void process(int y, int[] argb);
    }

    /**
     * A work unit that performs pre- or post-processing on pixel data in an
     * array. Override the run method and process {@code this.pixels} from
     * index {@code this.start} to index {@code this.end-1}.
     */
    static abstract class ArrayWorkUnit implements Runnable {

        protected int[] pixels;
        protected int start;
        protected int end;

        public ArrayWorkUnit(int[] pixels, int start, int len) {
            this.pixels = pixels;
            this.start = start;
            end = start + len;
        }
    };

    private static final class PremulUnit extends ArrayWorkUnit {

        public PremulUnit(int[] pixels, int start, int len) {
            super(pixels, start, len);
        }

        @Override
        public void run() {
            int end = this.end;
            for (int i = start; i < end; ++i) {
                final int argb = pixels[i];
                final int a = argb >>> 24;

                if (a != 255) {
                    final int r = ((argb >> 16) & 0xff) * a / 255;
                    final int g = ((argb >> 8) & 0xff) * a / 255;
                    final int b = (argb & 0xff) * a / 255;

                    pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
                }
            }
        }
    }

    private static final class UnpremulUnit extends ArrayWorkUnit {

        public UnpremulUnit(int[] pixels, int start, int len) {
            super(pixels, start, len);
        }

        @Override
        public void run() {
            int end = this.end;
            for (int i = start; i < end; ++i) {
                final int argb = pixels[i];
                final int a = argb >>> 24;

                if (a != 0 && a != 255) {
                    int r = ((argb >> 16) & 0xff) * 255 / a;
                    int g = ((argb >> 8) & 0xff) * 255 / a;
                    int b = (argb & 0xff) * 255 / a;

                    if (r > 255) {
                        r = 255;
                    }
                    if (g > 255) {
                        g = 255;
                    }
                    if (b > 255) {
                        b = 255;
                    }

                    pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
                }
            }
        }
    }

    /**
     * Premultiply the alpha of a single pixel value.
     *
     * @param argb the value to premultiply
     * @return the premultiplied pixel
     */
    static int premultiply(int argb) {
        final int a = (argb >>> 24);
        if (a != 255) {
            final int r = ((argb >> 16) & 0xff) * a / 255;
            final int g = ((argb >> 8) & 0xff) * a / 255;
            final int b = (argb & 0xff) * a / 255;

            argb = (a << 24) | (r << 16) | (g << 8) | b;
        }
        return argb;
    }

    /**
     * Premultiply the alpha of the pixels in the array.
     *
     * @param pixels the pixel values to premultiply
     */
    static void premultiply(int[] pixels) {
        if (pixels.length < Tuning.PREMUL) {
            new PremulUnit(pixels, 0, pixels.length).run();
        } else {
            int len = pixels.length;
            SplitJoin sj = SplitJoin.getInstance();
            final int n = Math.min(len, sj.getIdealSplitCount());
            final int pixelsPerTask = len / n;
            final int remainder = len - n * pixelsPerTask;
            Runnable[] units = new Runnable[n];
            int off = 0;
            for (int i = 0; i < n; ++i) {
                int count = pixelsPerTask;
                if (i < remainder) {
                    ++count;
                }
                units[i] = new PremulUnit(pixels, off, count);
                off += count;
            }
            sj.runUnchecked(units);
        }
    }

    /**
     * Unpremultiply the alpha of the pixels in the array.
     *
     * @param pixels the premultiplied pixel values to restore
     */
    static void unpremultiply(int[] pixels) {
        if (pixels.length * 2 < Tuning.PREMUL) {
            new UnpremulUnit(pixels, 0, pixels.length).run();
        } else {
            int len = pixels.length;
            SplitJoin sj = SplitJoin.getInstance();
            final int n = Math.min(len, sj.getIdealSplitCount());
            final int pixelsPerTask = len / n;
            final int remainder = len - n * pixelsPerTask;
            Runnable[] units = new Runnable[n];
            int off = 0;
            for (int i = 0; i < n; ++i) {
                int count = pixelsPerTask;
                if (i < remainder) {
                    ++count;
                }
                units[i] = new UnpremulUnit(pixels, off, count);
                off += count;
            }
            sj.runUnchecked(units);
        }
    }

    /**
     * Writes an image to a PNG file, printing and returning the file name. Used
     * for testing during filter development.
     *
     * @param bi the image to dump
     * @return the file that was written
     */
    static synchronized File testDump(BufferedImage bi) {
        try {
            int index = Integer.valueOf(System.getProperty("cgjennings.image.testdump", "1"));
            System.setProperty("cgjennings.image.testdump", Integer.toString(index + 1));
            File out = File.createTempFile("test-num" + index + '-', ".png", new File(System.getProperty("user.home")));
            ImageIO.write(bi, "png", out);
            System.err.println("wrote test image to:");
            System.err.println(out);
            return out;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new Error("failed to dump test image", e);
        }
    }

    /**
     * Writes an image to a PNG file, printing and returning the file name. Used
     * for testing during filter development.
     *
     * @param bi the image to dump
     * @return the file that was written
     */
    File testDump(int width, int height, int[] pixels) {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        setARGB(bi, pixels);
        return testDump(bi);
    }
}
