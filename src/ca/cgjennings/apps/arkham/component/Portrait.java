package ca.cgjennings.apps.arkham.component;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.EnumSet;

/**
 * A portrait is a user-supplied image that is part of a game component, such as
 * a character portrait on a character card. Portraits are obtained using the
 * {@link PortraitProvider} interface implemented by components that support
 * them. Note that a component may implement that interface yet not actually
 * have any portraits (by returning 0 from
 * {@link PortraitProvider#getPortraitCount()}).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface Portrait {

    /**
     * Sets the image used by the portrait.The value of {@code resource}
 is an identifier such as a local path to an image file or a URL. Either
 {@code null} or an empty string is a special identifier that
     * requests a default image that depends on the component and portrait
     * number.
     * <p>
     * When a new portrait is set, the scale, pan, and rotation values will be
     * changed to suit the new image. If you wish to keep these fixed, you must
     * store these values in temporary variables before setting them image, and
     * then restore them once this method returns.
     * <p>
     * If an image cannot be obtained from {@code resource} (for example,
     * if the file it names does not exist or is invalid), then a special error
     * image is substituted.
     *
     * @param resource an identifier user to locate the image (a file or URL)
     * @throws NullPointerException if {@code resource} is
     * {@code null}
     */
    public void setSource(String resource);

    /**
     * Returns the identifier for the image being used by this portrait. See the
     * description of {@link #setSource(java.lang.String)} for details.
     *
     * @return a resource identifier, such as a file path or URL
     */
    public String getSource();

    /**
     * Returns the image being used by this portrait. The original image is
     * returned, rather than a copy, in order to conserve resources. However, it
     * is important that this image not be modified in any way as it may be
     * shared between several objects.
     *
     * @return a shared copy of the image used by the component
     */
    public BufferedImage getImage();

    /**
     * Explicitly sets the image used by this portrait. The source for the
     * portrait will be set to reported source, but no attempt will be made to
     * load the portrait from this source. Instead, the given image will be used
     * as if it was the image loaded from the reported source. This is sometimes
     * useful when you want to copy an image to a new component but you do not
     * know if the source location is actually available on this system.
     *
     * @param reportedSource the apparent source of the image
     * @param image the image to use
     */
    public void setImage(String reportedSource, BufferedImage image);

    /**
     * Returns the scale of the portrait. A scale of 1 means that the portrait
     * is being used at the default size chosen by the component.
     *
     * @return the scale multiplier for the portrait
     */
    public double getScale();

    /**
     * Sets the scale factor applied to the default size chosen by the
     * component. Note that for many component types, the default size just fits
     * in the available space, so that a value less than 1 will show blank space
     * at one or more edges of the portrait area.
     *
     * @param scale the scale multiplier to apply to the image's default size
     */
    public void setScale(double scale);

    /**
     * Returns the horizontal pan value. A value of 0 places the image at its
     * default location within the portrait area. The units are pixels in the
     * component's template image.
     *
     * @return the shift from the default X position of the portrait
     */
    public double getPanX();

    /**
     * Sets the horizontal pan value.
     *
     * @param x the shift from the default X position of the portrait
     */
    public void setPanX(double x);

    /**
     * Returns the vertical pan value. A value of 0 places the image at its
     * default location within the portrait area. The units are pixels in the
     * component's template image.
     *
     * @return the shift from the default Y position of the portrait
     */
    public double getPanY();

    /**
     * Sets the vertical pan value.
     *
     * @param y the shift from the default Y position of the portrait
     */
    public void setPanY(double y);

    /**
     * Sets the pan value from a reference point.
     *
     * @param pan a point set to the new X and Y pan values
     */
    public void setPan(Point2D pan);

    /**
     * Returns the pan value as a {@code Point2D}. The pan values will be
     * stored in {@code dest}; if {@code null}, a new point object
     * will be allocated and returned.
     *
     * @param dest a point object to store the pan value in
     * @return the point object that contains the pan value
     */
    public Point2D getPan(Point2D dest);

    /**
     * Returns the rotation angle for the portrait image. The default rotation
     * is usually 0, but it differs by component. Positive angles turn in the
     * anti-clockwise direction, while negative angles turn in the clockwise
     * direction.
     *
     * @return the rotation angle, in degrees
     */
    public double getRotation();

    /**
     * Sets the rotation angle for the portrait image.
     *
     * @param angleInDegrees the rotation angle, in degrees
     */
    public void setRotation(double angleInDegrees);

    /**
     * Requests that this portrait be set to use a default portrait image.
     */
    public void installDefault();

    /**
     * Returns the size of the bounding rectangle of the area that the portrait
     * is drawn within on the component sheet, in the coordinate system of the
     * sheet's template image.
     *
     * <p>
     * <b>Note:</b> The value returned by this method is intended for use in
     * creating sample composites for the user to manipulate when adjusting the
     * portrait. It usually, but may not, reflect the dimensions used internally
     * by the portrait when drawing or calculating a default portrait scale.
     *
     * @return the dimensions of the portrait's clipping rectangle
     */
    public Dimension getClipDimensions();

    /**
     * Optionally returns an image that describes how the portrait will "show
     * through" surrounding card features. The image's alpha channel is taken to
     * represent a mask for the features that appear over the portrait area. For
     * example, pixels with an alpha of 0 will show the portrait image only, and
     * pixels with an alpha of 255 will show part of a sheet feature that
     * obscures the underlying portrait. In other words, the alpha channel
     * describes the shape of the portrait. If the portrait is simply an
     * unobscured rectangle (that is, nothing is drawn overtop of it), this
     * method can simply return {@code null}.
     *
     * <p>
     * <b>Note:</b> The value returned by this method is not guaranteed to be
     * accurate and should not be relied on.
     *
     * @return an image whose alpha channel describes the obscured areas of the
     * portrait, or {@code null}
     */
    public BufferedImage getClipStencil();

    /**
     * Identifies features that this portrait supports. Note that even
     * unsupported features can be used safely. Getting an unsupported feature
     * will return a dummy value, while setting an unsupported feature will have
     * no effect.
     */
    public enum Feature {
        /**
         * Indicates that the portrait can be changed by setting the source.
         */
        SOURCE,
        /**
         * Indicates that the portrait scale can be changed.
         */
        SCALE,
        /**
         * Indicates that the portrait panning can be changed.
         */
        PAN,
        /**
         * Indicates that the portrait rotation can be changed.
         */
        ROTATE
    };

    /**
     * Returns a set of the features supported by this portrait.
     */
    public EnumSet<Feature> getFeatures();

    /**
     * A predefined feature set that includes all of the features except
     * rotation.
     */
    public static final EnumSet<Feature> STANDARD_PORTRAIT_FEATURES = EnumSet.range(Feature.SOURCE, Feature.PAN);
    /**
     * A predefined feature set that includes all of the features except
     * rotation.
     */
    public static final EnumSet<Feature> ROTATABLE_PORTRAIT_FEATURES = EnumSet.allOf(Feature.class);
}
