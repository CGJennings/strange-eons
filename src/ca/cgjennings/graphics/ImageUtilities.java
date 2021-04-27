package ca.cgjennings.graphics;

import ca.cgjennings.graphics.filters.AbstractImageFilter;
import ca.cgjennings.graphics.filters.AbstractPixelwiseFilter;
import ca.cgjennings.graphics.filters.CheckeredScreenFilter;
import ca.cgjennings.graphics.filters.GreyscaleFilter;
import ca.cgjennings.graphics.filters.InversionFilter;
import ca.cgjennings.graphics.filters.TrimFilter;
import ca.cgjennings.graphics.filters.TurnAndFlipFilter;
import ca.cgjennings.ui.theme.ThemedIcon;
import java.awt.AlphaComposite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Utility methods for creating and converting images, icons, and other
 * graphics. The functionality includes: creating and converting images in
 * certain common formats (particularly the integer (A)RGB formats that the
 * filters in the graphics filters package are optimized for), converting icons
 * to images, adjusting the size of icons, and applying common filter effects to
 * icon images, creating custom {@link Cursor}s, applying common filters to
 * images, copying images, and adjusting the margins of images (padding,
 * trimming, centreing), computing the ideal scale for an image to fit it within
 * or over a given area, and resampling (resizing) images at high quality.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public final class ImageUtilities {

    private ImageUtilities() {
    }

    /**
     * Creates a new image the same size as <code>im</code> which is in either
     * <tt>TYPE_INT_RGB</tt> or <tt>TYPE_INT_ARGB</tt> format depending on
     * whether
     * <tt>im</tt> is opaque.
     *
     * @param im the image to create a new image for
     * @return a new image compatible with <tt>im</tt> but guaranteed to use an
     * 8-bit RGB format
     */
    public static BufferedImage createCompatibleIntRGBFormat(BufferedImage im) {
        return createCompatibleIntRGBFormat(im, im.getWidth(), im.getHeight());
    }

    /**
     * Creates a new image with the specified size which is in either
     * <tt>TYPE_INT_RGB</tt> or <tt>TYPE_INT_ARGB</tt> format depending on
     * whether
     * <tt>im</tt> is opaque.
     *
     * @param im the image to create a new image for
     * @return a new image compatible with <tt>im</tt> but guaranteed to use an
     * 8-bit RGB format
     */
    public static BufferedImage createCompatibleIntRGBFormat(BufferedImage im, int width, int height) {
        final int type = (im.getTransparency() == BufferedImage.OPAQUE)
                ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        return new BufferedImage(width, height, type);
    }

    /**
     * Creates a new image the same size as <code>im</code> which is in
     * <tt>TYPE_INT_ARGB</tt> format.
     *
     * @param im the image to create a new image for
     * @return a new image compatible with <tt>im</tt> but guaranteed to use
     * 8-bit ARGB format
     */
    public static BufferedImage createCompatibleIntARGBFormat(BufferedImage im) {
        return new BufferedImage(
                im.getWidth(), im.getHeight(), BufferedImage.TYPE_INT_ARGB
        );
    }

    /**
     * If <tt>im</tt> is not in an integer (A)RGB format, returns a new,
     * equivalent image in an integer RGB format. If <tt>im</tt> is already in
     * an integer RGB format, returns the original image.
     *
     * @param im the image to be provided in integer RGB format
     * @return the original image, or a copy converted to a suitable format
     */
    public static BufferedImage ensureIntRGBFormat(BufferedImage im) {
        final int type = im.getType();
        if (type != BufferedImage.TYPE_INT_RGB && type != BufferedImage.TYPE_INT_ARGB && type != BufferedImage.TYPE_INT_ARGB_PRE) {
            BufferedImage dest = createCompatibleIntRGBFormat(im);
            Graphics2D g = dest.createGraphics();
            try {
                g.drawImage(im, 0, 0, null);
            } finally {
                g.dispose();
            }
            im = dest;
        }
        return im;
    }

    /**
     * Returns a version of an image that includes an alpha channel. If the
     * source image has an alpha channel, it is returned. Otherwise, a copy is
     * returned.
     *
     * @param im the image to check
     * @return the original image, or a copy that includes an alpha channel
     */
    public static BufferedImage ensureImageHasAlphaChannel(BufferedImage im) {
        int type = im.getType();
        if (type != BufferedImage.TYPE_INT_ARGB && type != BufferedImage.TYPE_INT_ARGB_PRE) {
            BufferedImage dest = new BufferedImage(im.getWidth(), im.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = dest.createGraphics();
            try {
                g.drawImage(im, 0, 0, null);
            } finally {
                g.dispose();
            }
            im = dest;
        }
        return im;
    }

    /**
     * If <tt>im</tt> does not use the specified pixel format, an equivalent
     * image in that format is returned. If <tt>im</tt> is already in the
     * specified format, it is returned unchanged.
     *
     * @param im the image to be provided in the specified format
     * @param type the <code>BufferedImage.TYPE_*</code> value for the desired
     * format
     * @return the original image, or a copy converted to the requested format
     */
    public static BufferedImage ensureImageHasType(BufferedImage im, int type) {
        if (im.getType() != type) {
            BufferedImage dest = new BufferedImage(im.getWidth(), im.getHeight(), type);
            Graphics2D g = dest.createGraphics();
            try {
                g.drawImage(im, 0, 0, null);
            } finally {
                g.dispose();
            }
            im = dest;
        }
        return im;
    }

    /**
     * Returns a version of an image that is in an integer (A)RGB format. If the
     * source includes an alpha channel, the returned image will use a
     * premultiplied image type.
     *
     * @param im the image to be provided in the specified format
     * @return the original image, or a copy in an integer (A)RGB format, with
     * premultiplied alpha if appropriate
     */
    public static BufferedImage ensurePremultipliedFormat(BufferedImage im) {
        if (!im.isAlphaPremultiplied() && im.getTransparency() != Transparency.OPAQUE) {
            BufferedImage dest = new BufferedImage(im.getWidth(), im.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = dest.createGraphics();
            try {
                g.drawImage(im, 0, 0, null);
            } finally {
                g.dispose();
            }
            im = dest;
        } else {
            im = ensureIntRGBFormat(im);
        }
        return im;
    }

    /**
     * Defines a new custom cursor for use with the default {@link Toolkit},
     * based on the specified image.
     *
     * @param image the image to use for the cursor
     * @param hotspotX the x-coordinate of the cursor's hotspot (the image
     * offset where clicks occur)
     * @param hotspotY the y-coordinate of the cursor's hotspot
     * @return a custom cursor as close to the requested form as allowed by the
     * toolkit; if the toolkit does not support custom cursors, the default
     * cursor is returned
     */
    public static Cursor createCustomCursor(BufferedImage image, int hotspotX, int hotspotY) {
        return createCustomCursor(null, image, hotspotX, hotspotY, null);
    }
    private static int customIndex;

    /**
     * Defines a new custom cursor for use with a {@link Toolkit}, based on the
     * specified image.
     *
     * @param toolkit the toolkit on which to define the cursor
     * (<code>null</code> for default)
     * @param image the image to use for the cursor
     * @param hotspotX the x-coordinate of the cursor's hotspot (the image
     * offset where clicks occur)
     * @param hotspotY the y-coordinate of the cursor's hotspot
     * @param name the custom cursor name, for accessibility, or
     * <code>null</code> to generate a name
     * @return a custom cursor as close to the requested form as allowed by the
     * toolkit; if the toolkit does not support custom cursors, the default
     * cursor is returned
     */
    public static Cursor createCustomCursor(Toolkit toolkit, BufferedImage image, int hotspotX, int hotspotY, String name) {
        if (toolkit == null) {
            toolkit = Toolkit.getDefaultToolkit();
        }
        synchronized (toolkit) {
            if (name == null) {
                name = "Custom Cursor Number " + customIndex++;
            }
        }
        Dimension d = toolkit.getBestCursorSize(image.getWidth(), image.getHeight());

        // toolkit doesn't support custom cursors
        if (d.width == 0 || d.height == 0) {
            return Cursor.getDefaultCursor();
        }

        // image is larger than ideal size; scale down
        if (d.width < image.getWidth() || d.height < image.getHeight()) {
            float scale = idealCoveringScaleForImage(d.width, d.height, image.getWidth(), image.getHeight());
            image = resample(image, scale);
            hotspotX = Math.round(hotspotX * scale);
            hotspotY = Math.round(hotspotY * scale);
        }

        // image is not equal to ideal size; pad to ideal size
        if (d.width != image.getWidth() || d.height != image.getHeight()) {
            BufferedImage temp = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = temp.createGraphics();
            try {
                g.drawImage(image, 0, 0, null);
            } finally {
                g.dispose();
            }
            image = temp;
        }

        return toolkit.createCustomCursor(image, new Point(hotspotX, hotspotY), name);
    }

    /**
     * Returns a {@link BufferedImage} that is equivalent to a specified
     * {@link Image}. If the image is already a <code>BufferedImage</code>, it
     * is returned unmodified. Otherwise, a new <code>BufferedImage</code> will
     * be created and the contents of the image copied into it. If necessary,
     * the method will wait until the image has finished downloading.
     *
     * @param image the image to convert
     * @return the original image, converted into a <code>BufferedImage</code>
     * if it is not already of that type
     */
    public static BufferedImage imageToBufferedImage(Image image) {
        if (image == null) {
            throw new NullPointerException("image");
        }

        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }

        // wait for image to load
        int w, h;
        while (((w = image.getWidth(null)) == -1) || ((h = image.getHeight(null)) == -1)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }

        // ensure that all pixels are available
        PixelGrabber grabber = new PixelGrabber(image, 0, 0, w, h, false);
        try {
            grabber.grabPixels();
        } catch (InterruptedException e) {
            throw new AssertionError();
        }

        // draw to a buffered image
        BufferedImage bi = new BufferedImage(w, h, grabber.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB
        );
        Graphics2D g = bi.createGraphics();
        try {
            g.drawImage(image, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }

        return bi;
    }

    /**
     * Creates an icon from <code>image</code> that is constrained to
     * <code>size</code> by <code>size</code> pixels.
     *
     * @param image the image to create an icon from
     * @param size the maximum width and height of the icon
     * @return an icon created from <code>image</code>
     */
    public static Icon createIconForSize(BufferedImage image, int size) {
        if (image.getWidth() > size || image.getHeight() > size) {
            double scale = ImageUtilities.idealCoveringScaleForImage(size, size, image.getWidth(), image.getHeight());
            image = ImageUtilities.resample(image, (float) scale);
        }
        if (image.getWidth() < size || image.getHeight() < size) {
            image = center(image, size, size);
        }
        return new ImageIcon(image);
    }

    /**
     * Ensures that the supplied icon has the specified size. If the source icon
     * is <code>null</code>, <code>null</code> is returned. Otherwise, if the
     * icon has the correct dimensions it is returned. If not, a new icon is
     * created as if by calling {@link #createIconForSize} on an image of the
     * icon.
     *
     * @param icon the image to create an icon from
     * @param size the desired width and height of the icon
     * @return an icon of the requested size, or <code>null</code>
     */
    public static Icon ensureIconHasSize(Icon icon, int size) {
        if (icon == null) {
            return null;
        }
        if (icon.getIconWidth() != size || icon.getIconHeight() != size) {
            icon = createIconForSize(iconToImage(icon), size);
        }
        return icon;
    }

    /**
     * Returns the visual content of an icon as an image. If possible, the image
     * will be obtained directly from the icon instance without creating a new
     * image.
     *
     * @param i the icon
     * @return the content of the icon, as an image
     */
    public static BufferedImage iconToImage(Icon i) {
        if (i instanceof ThemedIcon) {
            return ((ThemedIcon) i).getImage();
        }
        if (i instanceof ImageIcon) {
            Image ii = ((ImageIcon) i).getImage();
            if (ii instanceof BufferedImage) {
                return (BufferedImage) ii;
            }
        }

        BufferedImage img = new BufferedImage(i.getIconWidth(), i.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            i.paintIcon(null, g, 0, 0);
        } finally {
            g.dispose();
        }
        return img;
    }

    /**
     * Returns a disabled version of an icon.
     *
     * @param src the icon to convert
     * @return a version of the icon with a suitable "disabled" effect applied
     */
    public static ImageIcon createDisabledIcon(Icon src) {
        synchronized (ImageUtilities.class) {
            if (disFilter == null) {
                disFilter = new AbstractPixelwiseFilter() {
                    @Override
                    public void filterPixels(int[] pixels, int start, int end) {
                        for (int i = 0; i < pixels.length; ++i) {
                            int argb = pixels[i];
                            int gray = ((77 * ((argb >> 16) & 0xff))
                                    + (150 * ((argb >> 8) & 0xff))
                                    + (28 * (argb & 0xff))) / 255;

                            gray = (255 - ((255 - gray) / 2));
                            if (gray < 0) {
                                gray = 0;
                            }
                            if (gray > 255) {
                                gray = 255;
                            }
                            pixels[i] = (argb & 0xff000000) | (gray << 16) | (gray << 8) | (gray);
                        }
                    }
                };
            }
        }
        return new ImageIcon(disFilter.filter(iconToImage(src), null));
    }
    private static AbstractPixelwiseFilter disFilter;

    /**
     * Returns a ghosted version of an icon. Ghosted icons are equivalent to the
     * original icon, but with every other pixel made transparent in a
     * checkerboard pattern.
     *
     * @param src the icon to convert
     * @return a version of the icon with a ghosting effect applied
     */
    public static ImageIcon createGhostedIcon(Icon src) {
        synchronized (ImageUtilities.class) {
            if (csFilter == null) {
                csFilter = new CheckeredScreenFilter();
            }
        }
        return new ImageIcon(csFilter.filter(desaturate(iconToImage(src)), null));
    }
    private static CheckeredScreenFilter csFilter;

    /**
     * Returns a greyscale version of an icon.
     *
     * @param src the icon to convert
     * @return a greyscale version of the icon
     */
    public static ImageIcon createDesaturatedIcon(Icon src) {
        return new ImageIcon(desaturate(iconToImage(src)));
    }

    /**
     * Returns a greyscale version of an image.
     *
     * @param src the image to convert
     * @return a greyscale version of the image
     * @see GreyscaleFilter
     */
    public static BufferedImage desaturate(BufferedImage src) {
        synchronized (ImageUtilities.class) {
            if (gsFilter == null) {
                gsFilter = new GreyscaleFilter();
            }
        }
        return gsFilter.filter(src, null);
    }
    private static GreyscaleFilter gsFilter;

    /**
     * Returns a copy of the source image with the colour values inverted.
     * (Black pixels become white, red pixels become cyan, and so on.)
     *
     * @param source the image to invert
     * @return the inverted image
     */
    public static final BufferedImage invert(BufferedImage source) {
        synchronized (ImageUtilities.class) {
            if (invFilter == null) {
                invFilter = new InversionFilter();
            }
        }
        return invFilter.filter(source, null);
    }
    private static InversionFilter invFilter;

    /**
     * Resample an image by a scaling factor, at a high level of quality.
     *
     * @param src the source image
     * @param factor a relative size factor applied to both dimensions
     * @return a new image at the requested size
     */
    public static BufferedImage resample(BufferedImage src, float factor) {
        int w = Math.round(src.getWidth() * factor);
        int h = Math.round(src.getHeight() * factor);
        if (w < 1) {
            w = 1;
        }
        if (h < 1) {
            h = 1;
        }
        return resample(src, w, h, true, RenderingHints.VALUE_INTERPOLATION_BICUBIC, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    public static BufferedImage resample(BufferedImage src, float factor, boolean multipass, Object finalPassHint, Object intermediateHint) {
        int w = Math.round(src.getWidth() * factor);
        int h = Math.round(src.getHeight() * factor);
        if (w < 1) {
            w = 1;
        }
        if (h < 1) {
            h = 1;
        }
        return resample(src, w, h, multipass, finalPassHint, intermediateHint);
    }

    /**
     * Resample an image to a specified size, at a high level of quality.
     *
     * @param src the source image
     * @param width the new image width
     * @param height the new image height
     * @return a new image at the requested size
     */
    public static BufferedImage resample(BufferedImage src, int width, int height) {
        return resample(src, width, height, true,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );
    }

    /**
     * Resample an image. If <code>multipass</code> is <code>true</code>, then
     * the method <i>may</i> split the resampling into multiple passes in order
     * to increase the quality of the result. The quality of the interpolation
     * is also controlled by the hint values, which must be one of the
     * <code>RenderingHints.VALUE_INTERPOLATION_*</code> values, or else
     * <code>null</code>. (If a hint is <code>null</code>, a slow area-averaging
     * algorithm will be used.) The hints determine the type of interpolation
     * used at each stage; the final pass hint is always used exactly once
     * (unless the image is already the requested size). The intermediate hint
     * is used for all other passes, if any. The following table shows some
     * suggested combinations of values, in order from fastest/lowest quality to
     * slowest/highest quality:
     * <table>
     * <caption>Suggested resampling combinations</caption>
     * <tr><th>multipass<th>finalPassHint<th>intermediateHint</tr>
     * <tr><td><code>false</code><td>VALUE_INTERPOLATION_NEAREST_NEIGHBOUR<td><code>null</code>
     * (ignored)</tr>
     * <tr><td><code>true</code>
     * <td>VALUE_INTERPOLATION_BILINEAR<td>VALUE_INTERPOLATION_BILINEAR</tr>
     * <tr><td><code>true</code>
     * <td>VALUE_INTERPOLATION_BILINEAR<td>VALUE_INTERPOLATION_BICUBIC</tr>
     * <tr><td><code>true</code>
     * <td>VALUE_INTERPOLATION_BICUBIC<td>VALUE_INTERPOLATION_BICUBIC</tr>
     * </table>
     *
     * @param src the source image
     * @param width the new image width
     * @param height the new image height
     * @param multipass if multiple passes are allowed
     * @param finalPassHint a rendering hint specifying the type of
     * interpolation to use for the final pass
     * @param intermediateHint a rendering hint specifying the type of
     * interpolation to use for intermediate passes, if any
     * @return a new image at the requested size, or the original image if the
     * new size matches the source image
     */
    public static BufferedImage resample(BufferedImage src, int width, int height, boolean multipass, Object finalPassHint, Object intermediateHint) {
        int swidth = src.getWidth();
        int sheight = src.getHeight();
        if (width == swidth && height == sheight) {
            return src;
        }

        int w, h;
        if (multipass) {
            w = swidth;
            h = sheight;
        } else {
            w = width;
            h = height;
        }

        do {
            if (multipass) {
                if (w > width) {
                    w /= 2;
                }
                if (w < width) {
                    w = width;
                }
                if (h > height) {
                    h /= 2;
                }
                if (h < height) {
                    h = height;
                }
            }

            Object hint;
            if (w == width && h == height) {
                hint = finalPassHint;
            } else {
                hint = intermediateHint;
            }
            if (hint == null) {
                src = ensureIntRGBFormat(imageToBufferedImage(src.getScaledInstance(w, h, BufferedImage.SCALE_AREA_AVERAGING)));
            } else {
                BufferedImage temp = createCompatibleIntRGBFormat(src, w, h);
                Graphics2D g = temp.createGraphics();
                try {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
                    g.drawImage(src, 0, 0, w, h, null);
                } finally {
                    g.dispose();
                }
                src = temp;
            }

        } while (w != width || h != height);

        return src;
    }

    /**
     * Returns the largest scaling factor that, when multiplied by the given
     * image size, ensures that the image will match the ideal size in at least
     * one dimension. The image will either match the ideal size in the other
     * dimension, or else be larger than the other ideal dimension. If the image
     * is opaque, the result is the scaling factor needed to obtain the smallest
     * image with the same aspect ratio that would completely cover the ideal
     * image area.
     *
     * @param idealWidth the width of the area the image must cover
     * @param idealHeight the height of the area the image must cover
     * @param imageWidth the current width of the image to be fitted
     * @param imageHeight the current height of the image to be fitted
     * @return the scale that would ensure that the image would just cover the
     * specified area
     */
    public static double idealCoveringScaleForImage(double idealWidth, double idealHeight, double imageWidth, double imageHeight) {
        double hScale = idealWidth / imageWidth;
        double vScale = idealHeight / imageHeight;

        if (hScale < vScale) {
            return vScale;
        } else {
            return hScale;
        }
    }

    /**
     * Returns the largest scaling factor that, when multiplied by the given
     * image size, ensures that the image will match the ideal size in at least
     * one dimension. The image will either match the ideal size in the other
     * dimension, or else be larger than the other ideal dimension. If the image
     * is opaque, the result is the scaling factor to obtain the smallest image
     * with the same aspect ratio that would completely cover the ideal image
     * area.
     *
     * @param idealWidth the width of the area the image must cover
     * @param idealHeight the height of the area the image must cover
     * @param imageWidth the current width of the image to be fitted
     * @param imageHeight the current height of the image to be fitted
     * @return the scale that would ensure that the image would just cover the
     * specified area
     */
    public static float idealCoveringScaleForImage(float idealWidth, float idealHeight, float imageWidth, float imageHeight) {
        float hScale = idealWidth / imageWidth;
        float vScale = idealHeight / imageHeight;

        if (hScale < vScale) {
            return vScale;
        } else {
            return hScale;
        }
    }

    /**
     * Returns the largest scaling factor that, when multiplied by the given
     * image size, ensures that the image will match the ideal size in at least
     * one dimension. The image will either match the ideal size in the other
     * dimension, or else be larger than the other ideal dimension. If the image
     * is opaque, the result is the scaling factor to obtain the smallest image
     * with the same aspect ratio that would completely cover the ideal image
     * area.
     *
     * @param idealWidth the width of the area the image must cover
     * @param idealHeight the height of the area the image must cover
     * @param imageWidth the current width of the image to be fitted
     * @param imageHeight the current height of the image to be fitted
     * @return the scale that would ensure that the image would just cover the
     * specified area
     */
    public static float idealCoveringScaleForImage(int idealWidth, int idealHeight, int imageWidth, int imageHeight) {
        float hScale = idealWidth / (float) imageWidth;
        float vScale = idealHeight / (float) imageHeight;
        if (hScale < vScale) {
            return vScale;
        } else {
            return hScale;
        }
    }

    /**
     * Returns the smallest scaling factor that, when multiplied by the given
     * image size, ensures that the image will match the ideal size in at least
     * one dimension and will not be larger than the ideal size in the other
     * dimension.
     *
     * @param idealWidth the width of the area the image must just fit within
     * @param idealHeight the height of the area the image must just fit within
     * @param imageWidth the current width of the image to be fitted
     * @param imageHeight the current height of the image to be fitted
     * @return the scale that just fits the image within the bounds, without any
     * of the image lying outside of the ideal area
     */
    public static double idealBoundingScaleForImage(double idealWidth, double idealHeight, double imageWidth, double imageHeight) {
        double hScale = idealWidth / imageWidth;
        double vScale = idealHeight / imageHeight;
        if (hScale > vScale) {
            return vScale;
        } else {
            return hScale;
        }
    }

    /**
     * Returns the smallest scaling factor that, when multiplied by the given
     * image size, ensures that the image will match the ideal size in at least
     * one dimension and will not be larger than the ideal size in the other
     * dimension.
     *
     * @param idealWidth the width of the area the image must just fit within
     * @param idealHeight the height of the area the image must just fit within
     * @param imageWidth the current width of the image to be fitted
     * @param imageHeight the current height of the image to be fitted
     * @return the scale that just fits the image within the bounds, without any
     * of the image lying outside of the ideal area
     */
    public static float idealBoundingScaleForImage(float idealWidth, float idealHeight, float imageWidth, float imageHeight) {
        float hScale = idealWidth / imageWidth;
        float vScale = idealHeight / imageHeight;

        if (hScale > vScale) {
            return vScale;
        } else {
            return hScale;
        }
    }

    /**
     * Returns the smallest scaling factor that, when multiplied by the given
     * image size, ensures that the image will match the ideal size in at least
     * one dimension and will not be larger than the ideal size in the other
     * dimension. The result is the largest image that fits within the ideal
     * image dimensions without exceeding them, even though there may be extra
     * space around the image on one dimension.
     *
     * @param idealWidth the width of the area the image must just fit within
     * @param idealHeight the height of the area the image must just fit within
     * @param imageWidth the current width of the image to be fitted
     * @param imageHeight the current height of the image to be fitted
     * @return the scale that just fits the image within the bounds, without any
     * of the image lying outside of the ideal area
     */
    public static float idealBoundingScaleForImage(int idealWidth, int idealHeight, int imageWidth, int imageHeight) {
        float hScale = idealWidth / (float) imageWidth;
        float vScale = idealHeight / (float) imageHeight;

        if (hScale > vScale) {
            return vScale;
        } else {
            return hScale;
        }
    }

    /**
     * Returns a copy of the image.
     *
     * @param source the image to copy
     * @return a copy of the image in an integer (A)RGB format
     */
    public static BufferedImage copy(BufferedImage source) {
        BufferedImage dest = createCompatibleIntRGBFormat(source);
        Graphics2D g = dest.createGraphics();
        try {
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return dest;
    }

    /**
     * Returns an image that centres a source image over a new image of a given
     * size.
     *
     * @param source the source image
     * @param width the width of the new image
     * @param height the height of the new image
     * @return an integer (A)RGB image containing the source image such that the
     * centre pixels of the two images are aligned
     */
    public static BufferedImage center(BufferedImage source, int width, int height) {
        BufferedImage dst = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        try {
            g.drawImage(
                    source, (width - source.getWidth()) / 2, (height - source.getHeight()) / 2, null
            );
        } finally {
            g.dispose();
        }
        return dst;
    }

    /**
     * Returns an image that is identical to a source image except that it is
     * padded by blank pixels around the outside. The returned image is always
     * of type <code>TYPE_INT_ARGB</code>. Margins can be negative, in which
     * case rows or columns are removed from the outside of the image. If the
     * margins are such that the width or height becomes less than 1, a blank 1
     * by 1 pixel image is returned. If the margin is 0 on all sides and the
     * image type is suitable, the source image is returned.
     *
     * @param source the source image
     * @param top the number of pixels to add to the top edge
     * @param left the number of pixels to add to the left edge
     * @param bottom the number of pixels to add to the bottom edge
     * @param right the number of pixels to add to the right edge
     * @return a padded copy of the image
     */
    public static final BufferedImage pad(BufferedImage source, int top, int left, int bottom, int right) {
        if (top == 0 && bottom == 0 && left == 0 && right == 0 && source.getType() == BufferedImage.TYPE_INT_ARGB) {
            return source;
        }
        int newW = source.getWidth() + left + right;
        int newH = source.getHeight() + top + bottom;
        if (newW < 1 || newH < 1) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }

        BufferedImage dest = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dest.createGraphics();
        try {
            g.drawImage(source, left, top, null);
        } finally {
            g.dispose();
        }
        return dest;
    }

    /**
     * Create a trimmed version of the source image that excludes fully
     * transparent (alpha=0) rows and columns around the image's edges. If there
     * are no such rows or columns, returns the original image. If the entire
     * image is fully transparent, returns a 1x1 transparent image.
     *
     * @param sourceImage the image to trim
     * @return a trimmed version of the image, or the original image if it does
     * not need trimming
     * @see TrimFilter
     */
    public static BufferedImage trim(BufferedImage sourceImage) {
        BufferedImage dest = TRIMMER.filter(sourceImage, null);
        if (dest == null) {
            dest = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }
        return dest;
    }
    private static final TrimFilter TRIMMER = new TrimFilter();

    /**
     * Returns a mirrored version of the source image. If neither parameter is
     * <code>true</code>, the original image is returned. Otherwise, a copy is
     * returned that is flipped horizontally and/or vertically.
     *
     * @param source the image to create a mirrored version of
     * @param horiz if <code>true</code>, the image is mirrored horizontally
     * (i.e., on the y axis)
     * @param vert if <code>true</code>, the image is mirrored vertically (i.e.,
     * on the x axis)
     * @return the mirrored version of the image
     * @see TurnAndFlipFilter
     */
    public static BufferedImage flip(BufferedImage source, boolean horiz, boolean vert) {
        if (!horiz && !vert) {
            return source;
        }

        BufferedImage dest = createCompatibleIntRGBFormat(source);

        int w = source.getWidth(), h = source.getHeight();
        int dx1, dx2, dy1, dy2;
        if (horiz) {
            dx1 = w;
            dx2 = 0;
        } else {
            dx1 = 0;
            dx2 = w;
        }
        if (vert) {
            dy1 = h;
            dy2 = 0;
        } else {
            dy1 = 0;
            dy2 = h;
        }

        Graphics2D g = dest.createGraphics();
        try {
            g.drawImage(source, dx1, dy1, dx2, dy2, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return dest;
    }

    /**
     * Draws a series of images overtop of each other. The images are drawn in
     * the reverse order of the parameters; that is, the first parameter will be
     * drawn overtop of the second, the second over the third and so forth. The
     * images will be drawn directly onto the last image in the list. If this is
     * <code>null</code>, a new image will be created with a width and height
     * equal to the maximum width and maximum height of all of the other images.
     * The last image in the last (possibly newly created if it was
     * <code>null</code>) is returned from the method.
     *
     * @param images a series of images to paint over each other
     * @return the bottom image, or a new image if the bottom (last) image was
     * <code>null</code>
     * @throws NullPointerException if a <code>null</code> array is passed in
     * @throws IllegalArgumentException if the image array has length 0
     */
    public static BufferedImage merge(BufferedImage... images) {
        if (images == null) {
            throw new NullPointerException("images");
        }
        if (images.length == 0) {
            throw new IllegalArgumentException("images.length == 0");
        }
        if (images.length == 1) {
            return images[0];
        }

        final int len_1 = images.length - 1;

        if (images[len_1] == null) {
            int maxW = 0, maxH = 0;
            for (int i = 0; i < len_1; ++i) {
                if (images[i] != null) {
                    maxW = Math.max(maxW, images[i].getWidth());
                    maxH = Math.max(maxH, images[i].getHeight());
                }
                images[len_1] = new BufferedImage(maxW, maxH, BufferedImage.TYPE_INT_ARGB);
            }
        }

        Graphics2D g = images[len_1].createGraphics();
        try {
            for (int i = 0; i < len_1; ++i) {
                g.drawImage(images[i], 0, 0, null);
            }
        } finally {
            g.dispose();
        }

        return images[len_1];
    }

    /**
     * Return a version of this image with an overall transparency change.
     * Pixels with an opacity of 1 (that is, 100%) in the source image will have
     * the specified opacity in the destination image; other pixels with have a
     * new opacity equal to their current opacity times the specified value. For
     * example, if the specified opacity is 0.5, and the source pixel has
     * opacity 0.5, its opacity in the output image will be 0.25.
     *
     * @param source the original image
     * @param opacity the alpha level to draw the image at, as a floating point
     * value between 0 and 1
     * @return a version of the image at the specified transparency (possibly
     * the original image)
     */
    public static BufferedImage alphaComposite(BufferedImage source, float opacity) {
        if (opacity >= 1f) {
            return ensureImageHasAlphaChannel(source);
        }
        BufferedImage filtered = createCompatibleIntARGBFormat(source);
        if (opacity <= 0f) {
            return filtered;
        }

        Graphics2D g = filtered.createGraphics();
        try {
            g.setComposite(AlphaComposite.SrcOver.derive(opacity));
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return filtered;
    }

    /**
     * Returns <code>true</code> if every pixel in this image is fully opaque.
     * If the image has no alpha channel, then <code>true</code> is returned
     * immediately. Otherwise, the image is checked pixel-by-pixel.
     *
     * @param bi the image to check
     * @return <code>true</code> if and only if every pixel is opaque
     * @throws NullPointerException if the image is <code>null</code>
     */
    public static boolean isOpaque(BufferedImage bi) {
        if (bi == null) {
            throw new NullPointerException("bi");
        }
        if (bi.getTransparency() == BufferedImage.OPAQUE) {
            return true;
        }

        final int width = bi.getWidth(), height = bi.getHeight();
        final int[] row = new int[width];
        for (int y = 0; y < height; ++y) {
            AbstractImageFilter.getARGB(bi, 0, y, width, 1, row);
            for (int x = 0; x < width; ++x) {
                if ((row[x] & 0xff00_0000) != 0xff00_0000) {
                    return false;
                }
            }
        }
        return true;
    }
}
