package ca.cgjennings.apps.arkham.component;

import ca.cgjennings.graphics.ImageUtilities;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.EnumSet;
import resources.ResourceKit;
import resources.Settings;
import resources.StrangeImage;

/**
 * An abstract base class for {@link Portrait} implementations. This base class
 * implements the <code>Point2D</code>-based pan methods in terms of
 * <code>setPanX</code>, <code>setPanY</code>, <code>getPanX</code>, and
 * <code>getPanY</code>. It also returns a default set of portrait features
 * supporting source-changing, panning, and scaling, but not rotation.
 * Accordingly, it provides dummy implementations of the rotation getter/setter
 * methods that enforce an immutable angle of 0 degrees. It provides a no-op
 * implementation of {@link #installDefault()}. It implements
 * {@link #getClipStencil()} to return <code>null</code>, but it provides some
 * static helper methods for subclasses that wish to provide
 * non-<code>null</code> stencils.
 *
 * @see DefaultPortrait
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public abstract class AbstractPortrait implements Portrait {

    /**
     * {@inheritDoc}
     *
     * <p>
     * The base class calls {@link #setPanX} and {@link #setPanY} using the x
     * and y values stored in <code>pan</code>.
     */
    @Override
    public void setPan(Point2D pan) {
        setPanX(pan.getX());
        setPanY(pan.getY());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The base class calls {@link #getPanX} and {@link #getPanY} to obtain x
     * and y values to store in the destination point.
     */
    @Override
    public Point2D getPan(Point2D dest) {
        if (dest == null) {
            dest = new Point2D.Double(getPanX(), getPanY());
        } else {
            dest.setLocation(getPanX(), getPanY());
        }
        return dest;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The base class always returns 0 since the abstract portrait does not
     * support rotation by default.
     */
    @Override
    public double getRotation() {
        return 0.0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The base class does nothing since the abstract portrait does not support
     * rotation by default.
     */
    @Override
    public void setRotation(double angleInDegrees) {
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The base class returns {@link Portrait#STANDARD_PORTRAIT_FEATURES},
     * indicating that it supports all features except rotation.
     */
    @Override
    public EnumSet<Feature> getFeatures() {
        return Portrait.STANDARD_PORTRAIT_FEATURES;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The base class returns <code>null</code>, indicating that the portrait
     * area is unobscured.
     */
    @Override
    public BufferedImage getClipStencil() {
        return null;
    }

    /**
     * Returns an image suitable for use with {@link #getClipStencil()}. This
     * method is best used when a sheet's template image has a translucent
     * "hole" that defines the shape of the portrait area. (The portrait is
     * drawn, and then the template is drawn overtop.) It returns the subimage
     * of the template image covered by the given region.
     *
     * <p>
     * If an image other than the sheet's template image is used, and that image
     * is not the same size as the template image, then the coordinates of the
     * supplied rectangle must be adjusted accordingly.
     *
     * <p>
     * If the resulting image would have no transparent or translucent pixels
     * (that is, if every pixel would have an alpha value of 255), then this
     * method returns <code>null</code>. This is consistent with how
     * <code>null</code> is interpreted when it is returned from
     * {@link #getClipStencil()}, because if the subimage is completely opaque
     * then the portrait is presumably being drawn over the template rather than
     * under it.
     *
     * @param template the sheet's template image
     * @param portraitRegion the rectangular area covered by the portrait on the
     * template image
     * @return the subimage of the template covered by the portrait rectangle,
     * or <code>null</code> if the template image is <code>null</code> or
     * completely opaque
     * @throws NullPointerException if the portrait region is <code>null</code>
     */
    public static BufferedImage createStencil(BufferedImage template, Rectangle portraitRegion) {
        if (portraitRegion == null) {
            throw new NullPointerException("portraitRegion");
        }

        if (template == null) {
            return null;
        }
        if (template.getTransparency() == BufferedImage.OPAQUE) {
            return null;
        }

        BufferedImage s = new BufferedImage(portraitRegion.width, portraitRegion.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = s.createGraphics();
        try {
            g.drawImage(template, -portraitRegion.x, -portraitRegion.y, null);
        } finally {
            g.dispose();
        }

        if (ImageUtilities.isOpaque(s)) {
            s = null;
        }
        return s;
    }

    /**
     * Creates an image suitable for use with {@link #getClipStencil()}. This
     * method obtains an image and rectangle from settings values and then calls
     * {@link #createStencil(java.awt.image.BufferedImage, java.awt.Rectangle)}
     * to create the stencil image.
     *
     * @param s the settings to use to look up the keys
     * @param templateKey a setting key that names an image resource to load
     * @param portraitRegionKey a setting key (without "-region") that describes
     * the clip region
     * @return the subimage of the template image referenced by
     * <code>templateKey</code> covered by the portrait rectangle described by
     * <code>portraitRegionKey</code>, or <code>null</code> if the resulting
     * image would be completely opaque
     * @see #createStencil(java.awt.image.BufferedImage, java.awt.Rectangle)
     */
    public static BufferedImage createStencil(Settings s, String templateKey, String portraitRegionKey) {
        return createStencil(ResourceKit.getImageQuietly(s.get(templateKey)), s.getRegion(portraitRegionKey));
    }

    /**
     * Loads a user-supplied image. This method is similar to
     * {@link StrangeImage#get}, but it will automatically convert vector images
     * to bitmaps. The size of the bitmap is chosen to be suitable for the
     * specified portrait region.
     *
     * @param resource the image identifier to load
     * @param portraitClipSize the portrait region size to target for vector
     * images
     * @return an image to use as the portrait source
     */
    public static BufferedImage getImageFromIdentifier(String resource, Dimension portraitClipSize) {
        StrangeImage si = StrangeImage.get(resource);
        if (portraitClipSize == null || !si.isVectorFormat() || si == StrangeImage.getMissingImage()) {
            return si.asBufferedImage();
        }

        double iw = si.getWidth2D();
        double ih = si.getHeight2D();
        double pw = portraitClipSize.width;
        double ph = portraitClipSize.height;
        double scale = 2d * ImageUtilities.idealBoundingScaleForImage(pw, ph, iw, ih);

        if (iw * scale < 512 && ih * scale < 512) {
            scale = ImageUtilities.idealBoundingScaleForImage(512, 512, iw, ih);
        }

//		System.out.printf("%.0f,%.0f\n",iw*scale,ih*scale);
        return si.toBufferedImage((int) (iw * scale + 0.5), (int) (ih * scale + 0.5), false);
    }
}
