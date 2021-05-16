package resources;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.graphics.shapes.AbstractVectorImage;
import ca.cgjennings.graphics.shapes.SVGVectorImage;
import ca.cgjennings.graphics.shapes.VectorImage;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Level;
import javax.imageio.ImageIO;

/**
 * A high-level representation of a drawable graphic that can be used by
 * <i>Strange Eons</i> in various contexts. This class is often the starting
 * point when working with images {@linkplain #get selected by the user}. The
 * class provides a common interface that can be used to work with both bitmap
 * and vector images transparently.
 *
 * <p>
 * For drawing purposes, a {@code StrangeImage} can be treated as either a
 * {@linkplain #asBufferedImage() bitmap} or
 * {@linkplain #asVectorImage() vector} image regardless of the original format.
 * It can also be
 * {@linkplain #paint(java.awt.Graphics2D, int, int, int, int, boolean) painted directly}.
 *
 * <p>
 * A {@code StrangeImage} must have a positive width and height. Breaking
 * this condition will lead to bugs that may be hard to detect. If you must
 * return a representation of an image with a zero width or height, return an
 * {@linkplain #getInvisibleImage() invisible image} instead.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0.3695
 */
public abstract class StrangeImage {
    // the timestamp is used by the built-in cache for images loaded from a file

    private long timestamp;

    /**
     * Constructs a new {@code StrangeImage}.
     */
    public StrangeImage() {
    }

    /**
     * Paints the image to a graphics context at the specified location and
     * dimensions.
     *
     * @param g the graphics context to use for painting
     * @param x the x-coordinate of the upper-left corner of the image
     * @param y the y-coordinate of the upper-left corner of the image
     * @param width the width of the image
     * @param height the height of the image
     * @param fitToSize if {@code true}, the image's original aspect ratio
     * will be maintained
     */
    public abstract void paint(Graphics2D g, int x, int y, int width, int height, boolean fitToSize);

    /**
     * Paints the image to a graphics context at the specified location and
     * dimensions. The location and size are specified using double precision
     * floating point values; depending on the underlying implementation, these
     * coordinates may be rounded to a less accurate form before painting.
     *
     * @param g the graphics context to use for painting
     * @param x the x-coordinate of the upper-left corner of the image
     * @param y the y-coordinate of the upper-left corner of the image
     * @param width the width of the image
     * @param height the height of the image
     * @param fitToSize if {@code true}, the image's original aspect ratio
     * will be maintained
     */
    public void paint(Graphics2D g, double x, double y, double width, double height, boolean fitToSize) {
        paint(g, (int) (x + 0.5d), (int) (y + 0.5d), (int) (width + 0.5d), (int) (height + 0.5d), fitToSize);
    }

    /**
     * Paints the image to a graphics context within the bounds of the specified
     * rectangle.
     *
     * @param g the graphics context
     * @param rect the rectangle defining the upper-left corner and dimensions
     * of the image
     * @param fitToSize if {@code true}, the image's original aspect ratio
     * will be maintained
     */
    public final void paint(Graphics2D g, Rectangle2D rect, boolean fitToSize) {
        if (rect == null) {
            throw new NullPointerException("rect");
        }
        paint(g, rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), fitToSize);
    }

    /**
     * Returns a version of the image as a {@link BufferedImage} image. The
     * returned value may be shared, so the image must not be modified. If the
     * underlying image type is a bitmap image, then the returned image will be
     * the same size as the original image. The returned image is guaranteed to
     * use an RGB or ARGB pixel format.
     *
     * @return a bitmap version of the image
     */
    public abstract BufferedImage asBufferedImage();

    /**
     * Returns a version of the image as a {@link VectorImage} image. Note that
     * if the underlying image is a bitmap image, this will <b>not</b>
     * convert the image into a true vector format (for example, by applying a
     * tracing algorithm). It simply provides a {@link VectorImage} interface to
     * the underlying bitmap data.
     *
     * @return a vector version of the image
     */
    public abstract VectorImage asVectorImage();

    /**
     * Returns a new buffered image representing the image content. The image
     * will be resized, if necessary, to the specified size. If
     * {@code fitToSize} is {@code true}, then the original aspect
     * ratio of the image will be maintained. The returned image is guaranteed
     * to use an RGB or ARGB pixel format.
     *
     * @param width the image width
     * @param height the image height
     * @param fitToSize if {@code true}, the original aspect ratio will be
     * maintained
     * @return a version of the image as a bitmap
     */
    public BufferedImage toBufferedImage(int width, int height, boolean fitToSize) {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            paint(g, 0, 0, width, height, fitToSize);
        } finally {
            g.dispose();
        }
        return bi;
    }

    /**
     * Returns a typical pixel width for the image. If the underlying image is a
     * bitmap image, this will return the true pixel width of that image.
     *
     * @return a width for the image, in pixels
     */
    public abstract int getWidth();

    /**
     * Returns a typical pixel height for the image. If the underlying image is
     * a bitmap image, this will return the true pixel height of that image.
     *
     * @return a height for the image, in pixels
     */
    public abstract int getHeight();

    /**
     * Returns the image width as a {@code double} value. Depending on the
     * underlying image, this may return a more accurate measure of the true
     * image width than {@link #getWidth()}.
     *
     * @return a width for the image, in pixels
     */
    public abstract double getWidth2D();

    /**
     * Returns the image height as a {@code double} value. Depending on the
     * underlying image, this may return a more accurate measure of the true
     * image height than {@link #getHeight()}.
     *
     * @return a height for the image, in pixels
     */
    public abstract double getHeight2D();

    /**
     * Returns the aspect ratio of the image, that is, the width divided by the
     * height. When painting the image at a particular width or height, one can
     * calculate the value for other dimension that will maintain the same image
     * shape using the aspect ratio:
     * <table border=0>
     * <caption>Compute size from aspect ratio</caption>
     * <tr><th>Known Dimension:&nbsp;<th>Other Dimension Calculated Using:</tr>
     * <tr><td>width<td>height = width / aspectRatio</tr>
     * <tr><td>height<td>width = height * aspectRatio</tr>
     * </table>
     *
     * @return the image aspect ratio
     */
    public double getAspectRatio() {
        return getWidth2D() / getHeight2D();
    }

    /**
     * Returns {@code true} if some parts of the image may not be opaque.
     * If this returns {@code false}, then it is guaranteed that the image
     * completely covers a rectangular area described by the specified width and
     * height when
     * {@linkplain #paint(java.awt.Graphics2D, int, int, int, int, boolean) painted}.
     * If this method returns {@code true}, then some parts of the painted
     * area <i>may</i> not be completely covered.
     *
     * @return {@code true} if the image may have transparent areas;
     * {@code false} if the image is known with certainly to be completely
     * opaque
     */
    public boolean isTransparent() {
        return true;
    }

    /**
     * Returns {@code true} if the underlying image is a vector image. If
     * you wish to work with the original image in its underlying form, you can
     * use code like the following:
     * <pre>
     * if( si.isVectorFormat() ) {
     *     VectorImage vi = si.asVectorImage();
     *     // ...
     * } else {
     *     BufferedImage bi = si.asBufferedImage();
     *     // ...
     * }
     * </pre>
     *
     * @return {@code true} if the underlying image is a vector image;
     * {@code false} if it is a bitmap image
     */
    public boolean isVectorFormat() {
        return true;
    }

    private static final class Bitmap extends StrangeImage {

        private final BufferedImage bi;
        private VectorImage vi;
        private final int w;
        private final int h;

        public Bitmap(BufferedImage bi) {
            this.bi = ResourceKit.prepareNewImage(bi);
            w = bi.getWidth();
            h = bi.getHeight();
        }

        @Override
        public void paint(Graphics2D g, int x, int y, int width, int height, boolean fitToSize) {
            if (width == w && height == h) {
                g.drawImage(bi, x, y, null);
            } else if (fitToSize) {
                float scale = ImageUtilities.idealCoveringScaleForImage(width, height, w, h);
                int fw = Math.round(scale * w);
                int fh = Math.round(scale * h);
                g.drawImage(bi, x + (width - fw) / 2, y + (height - fh) / 2, null);
            } else {
                g.drawImage(bi, x, y, width, height, null);
            }
        }

        @Override
        public BufferedImage asBufferedImage() {
            return bi;
        }

        @Override
        public synchronized VectorImage asVectorImage() {
            if (vi == null) {
                vi = new AbstractVectorImage() {
                    {
                        tx = ty = 0d;
                        iw = w;
                        ih = h;
                    }

                    @Override
                    protected void render(Graphics2D g) {
                        g.drawImage(bi, 0, 0, w, h, null);
                    }
                };
            }
            return vi;
        }

        @Override
        public int getWidth() {
            return w;
        }

        @Override
        public int getHeight() {
            return h;
        }

        @Override
        public double getWidth2D() {
            return w;
        }

        @Override
        public double getHeight2D() {
            return h;
        }

        @Override
        public boolean isTransparent() {
            return bi.getTransparency() != BufferedImage.OPAQUE;
        }

        @Override
        public boolean isVectorFormat() {
            return false;
        }
    }

    private static final class Vector extends StrangeImage {

        private final VectorImage vi;
        private final int w;
        private final int h;
        private final double dw;
        private final double dh;

        private SoftReference<BufferedImage> bitmapCache;

        public Vector(VectorImage vi) {
            this.vi = vi;
            dw = vi.getWidth();
            dh = vi.getHeight();
            w = (int) (dw + 0.5d);
            h = (int) (dh + 0.5d);
        }

        @Override
        public void paint(Graphics2D g, int x, int y, int width, int height, boolean fitToSize) {
            vi.paint(g, x, y, width, height, fitToSize);
        }

        @Override
        public void paint(Graphics2D g, double x, double y, double width, double height, boolean fitToSize) {
            vi.paint(g, x, y, width, height, fitToSize);
        }

        @Override
        public synchronized BufferedImage asBufferedImage() {
            BufferedImage bi = null;
            if (bitmapCache != null) {
                bi = bitmapCache.get();
            }
            if (bi == null) {
                int iw = w, ih = h;
                if (w < MIN_BITMAP_SIZE && h < MIN_BITMAP_SIZE) {
                    if (w > h) {
                        iw = MIN_BITMAP_SIZE;
                        ih = (int) (MIN_BITMAP_SIZE * (vi.getHeight() / vi.getWidth()) + 0.5d);
                    } else {
                        iw = (int) (MIN_BITMAP_SIZE * (vi.getWidth() / vi.getHeight()) + 0.5d);
                        ih = MIN_BITMAP_SIZE;
                    }
                }
                bi = vi.createRasterImage(iw, ih, true);
                bitmapCache = new SoftReference<>(bi);
            }
            return bi;
        }

        @Override
        public VectorImage asVectorImage() {
            return vi;
        }

        @Override
        public int getWidth() {
            return w;
        }

        @Override
        public int getHeight() {
            return h;
        }

        @Override
        public double getWidth2D() {
            return dw;
        }

        @Override
        public double getHeight2D() {
            return dh;
        }

        private static final int MIN_BITMAP_SIZE = 600;
    }

    /**
     * Creates a new {@code StrangeImage} from the specified bitmap image
     * source.
     *
     * @param source the source image
     * @return a view of the source image as a {@code StrangeImage}
     * instance
     */
    public static StrangeImage create(Image source) {
        if (source == null) {
            throw new NullPointerException("source");
        }

        return new Bitmap(ImageUtilities.imageToBufferedImage(source));
    }

    /**
     * Creates a new {@code StrangeImage} from the specified vector image
     * source.
     *
     * @param source the source image
     * @return a view of the source image as a {@code StrangeImage}
     * instance
     */
    public static StrangeImage create(VectorImage source) {
        if (source == null) {
            throw new NullPointerException("source");
        }

        if (source.getWidth() <= 0d || source.getHeight() <= 0d) {
            return getInvisibleImage();
        }
        return new Vector(source);
    }

    /**
     * Returns a {@code StrangeImage} for the given identifier. The
     * identifier can be a local file path or a {@code file:},
     * {@code http:}, {@code res:}, or {@code project:} URL. Any
     * string that is valid in a portrait panel will produce the same image
     * here, except for the empty string that produce's the component-specific
     * default portrait.
     *
     * @param identifier the identifier to use to locate the file
     * @return the identified image, or a "missing image" stand-in
     */
    public static StrangeImage get(String identifier) {
        StrangeImage si = null;
        URL url = identifierToURL(identifier);

        if (url != null) {
            si = cacheGet(identifier);
            if (si == null) {
                // check if vector image and render it as a bitmap if so
                int dot = identifier.lastIndexOf('.');
                if (dot >= 0 && url.toString().indexOf('/') < dot) {
                    String suffix = identifier.substring(dot + 1).toLowerCase(Locale.CANADA);
                    if (suffix.equals("svg") || suffix.equals("svgz")) {
                        try {
                            si = create(new SVGVectorImage(url));
                        } catch (CoreComponents.MissingCoreComponentException mcc) {
                            // core not installed; will get missing image
                        } catch( FileNotFoundException fnf ) {
                            si = getMissingImage();
                        } catch (IOException e) {
                            StrangeEons.log.log(Level.WARNING, "failed to load SVG: " + identifier, e);
                        }
                        if (si == null) {
                            si = getMissingImage();
                        }
                    }
                }

                // if not a vector image, try to load as a bitmap
                if (si == null) {
                    try {
                        BufferedImage bi = ImageIO.read(url);
                        if (bi != null) {
                            bi = ResourceKit.prepareNewImage(bi);
                            si = create(bi);
                        }
                    } catch ( FileNotFoundException fnf) {
                        si = getMissingImage();
                    } catch (IOException e) {
                        StrangeEons.log.log(Level.WARNING, "failed to load bitmap: " + identifier, e);
                    }
                }

                if (si != null && si != getMissingImage()) {
                    cachePut(identifier, si);
                }
            }
        }
        if (si == null) {
            si = getMissingImage();
        }
        return si;
    }

    private static StrangeImage cacheGet(String id) {
        StrangeImage si = null;
        synchronized (imageCache) {
            SoftReference<StrangeImage> ref = imageCache.get(id);
            if (ref != null) {
                si = ref.get();
            }

            // this must be a valid file, check if the timestamp changed
            // since this indicates the file was modified
            if (si != null && si.timestamp != 0L) {
                if (si.timestamp != new File(id).lastModified()) {
                    si = null;
                }
            }
        }
        return si;
    }

    private static void cachePut(String id, StrangeImage si) {
        synchronized (imageCache) {
            imageCache.put(id, new SoftReference<>(si));

            // if putting an image from a file into the cache, set the timestamp
            if (isFileIdentifier(id)) {
                si.timestamp = new File(id).lastModified();
            }
        }
    }


    /**
     * Returns a {@code BufferedImage} for the given identifier. The
     * identifier can be a local file path or a {@code file:},
     * {@code http:}, {@code res:}, or {@code project:} URL. Any
     * string that is valid in a portrait panel will produce the same image
     * here, except for the empty string that produce's the component-specific
     * default portrait.
     *
     * <p>This method is similar to {@link #get}, but ensures that the returned
     * image is a {@code BufferedImage} bitmap. Vector images will be converted
     * to bitmaps automatically. Where possible, prefer code that works
     * with both bitmaps and vectors transparently by using {@link #get} instead.
     *
     * @param identifier the identifier to use to locate the file
     * @return the identified image, or a "missing image" stand-in
     * @since 3.2
     */
    public static BufferedImage getAsBufferedImage(String identifier) {
        BufferedImage bi;
        StrangeImage si = StrangeImage.get(identifier);
        if (si == StrangeImage.getMissingImage()) {
            bi = ResourceKit.getMissingImage();
        } else {
            bi = si.asBufferedImage();
        }
        return bi;
    }

    /**
     * Returns whether or not an identifier refers to a valid image.
     * For example, passing a file identifier for a file that does not exist
     * would return false. Note that this will load (and cache) the image
     * if it is not already loaded.
     *
     * @param identifier the identifier to use to locate the file
     * @return true if the identifier points to a valid image, false otherwise
     * @since 3.2
     */
    public static boolean exists(String identifier) {
        return get(identifier) != getMissingImage();
    }

    /**
     * Returns a URL for a user image identifier, or {@code null}. If the
     * identifier is {@code null} or empty, then {@code null} is
     * returned. (An empty string is typically used to indicate that a default
     * image should be used.)
     *
     * @param identifier an identifier string containing a local file path or
     * URL
     * @return a URL that can be used to read the identified content
     */
    public static URL identifierToURL(String identifier) {
        URL url = null;
        if (identifier != null && !identifier.isEmpty()) {
            try {
                if (identifier.startsWith("./")) {
                    url = new URL(identifier);
                } else if (isFileIdentifier(identifier)) {
                    url = new File(identifier).toURI().toURL();
                } else {
                    url = new URL(identifier);
                }
            } catch (MalformedURLException e) {
                StrangeEons.log.log(Level.WARNING, null, e);
            }
        }
        return url;
    }

    private static final HashMap<String, SoftReference<StrangeImage>> imageCache = new HashMap<>();

    /**
     * Returns {@code true} if the image identifier names a local file, as
     * opposed to a special URL.
     *
     * @param identifier the identifier to locate
     * @return {@code true} if the identifier is for a local file
     * @see #get
     */
    public static boolean isFileIdentifier(String identifier) {
        if (identifier != null) {
            return !(identifier.startsWith("jar:") || identifier.startsWith("file:")
                    || identifier.startsWith("project:") || identifier.startsWith("res:")
                    || identifier.startsWith("http:") || identifier.contains("://"));
        }
        return false;
    }

    /**
     * Returns a {@code StrangeImage} that is completely invisible. This
     * can be used as a stand-in image for an image with a zero width or height,
     * since {@code StrangeImage}s must have a non-zero area.
     *
     * @return a completely transparent image
     */
    public static synchronized StrangeImage getInvisibleImage() {
        if (ZDS == null) {
            ZDS = new StrangeImage() {
                private final BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

                {
                    bi.setRGB(0, 0, 0x00ff0000);
                }

                private final VectorImage vi = new AbstractVectorImage() {
                    {
                        tx = ty = 0d;
                        iw = ih = 1d;
                    }

                    @Override
                    protected void render(Graphics2D g) {
                    }
                };

                @Override
                public void paint(Graphics2D g, int x, int y, int width, int height, boolean fitToSize) {
                }

                @Override
                public BufferedImage asBufferedImage() {
                    return bi;
                }

                @Override
                public VectorImage asVectorImage() {
                    return vi;
                }

                @Override
                public int getWidth() {
                    return 1;
                }

                @Override
                public int getHeight() {
                    return 1;
                }

                @Override
                public double getWidth2D() {
                    return 1d;
                }

                @Override
                public double getHeight2D() {
                    return 1d;
                }
            };
        }
        return ZDS;
    }
    private static StrangeImage ZDS;

    /**
     * Returns a standard image returned from {@link #get} when an image cannot
     * be obtained from the specified identifier.
     *
     * @return the shared missing image
     */
    public static synchronized StrangeImage getMissingImage() {
        if (missing == null) {
            missing = create(ResourceKit.getMissingVectorImage());
        }
        return missing;
    }
    private static StrangeImage missing;
}
