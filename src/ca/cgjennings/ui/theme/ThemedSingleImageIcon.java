package ca.cgjennings.ui.theme;

import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.graphics.filters.PixelArtUpscalingFilter;
import resources.ResourceKit;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.Objects;

/**
 * An image icon that scales the source image appropriately for high DPI
 * displays. This is a simpler alternative to {@link ThemedImageIcon} that is
 * preferable when it is known that only one source image is available. Where a
 * {@link ThemedImageIcon} chooses an image from one or more possible
 * representations, this class will cache a high-quality pre-scaled image
 * adjusted for the desktop scale.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class ThemedSingleImageIcon extends AbstractThemedIcon {

    private String resource;
    private BufferedImage source;
    private BufferedImage scaled;
    private static final int HIGH_FREQ_LIMIT = 32;

    /**
     * Creates a new, small icon from the specified image resource identifier.
     *
     * @param imageResource the non-null image resource, e.g.,
     * {@code "res://path/image.png"}
     */
    public ThemedSingleImageIcon(String imageResource) {
        this(imageResource, SMALL, SMALL);
    }

    /**
     * Creates a new icon from the specified image resource identifier. This is
     * a cover for creating an icon of equal width and height.
     *
     * @param imageResource the non-null image resource, e.g.,
     * {@code "res://path/image.png"}
     * @param size the desired icon width and height
     */
    public ThemedSingleImageIcon(String imageResource, int size) {
        this(imageResource, size, size);
    }

    /**
     * Creates a new icon from the specified image resource identifier. The
     * width and height should be given as if the desktop is not being scaled.
     *
     * @param imageResource the non-null image resource, e.g.,
     * {@code "res://path/image.png"}
     * @param iconWidth the desired icon width
     * @param iconHeight the desired icon height
     */
    public ThemedSingleImageIcon(String imageResource, int iconWidth, int iconHeight) {
        this.resource = Objects.requireNonNull(imageResource, "imageResource");

        if (iconWidth < 1 || iconHeight < 1) {
            throw new IllegalArgumentException("bad size: " + iconWidth + 'x' + iconHeight);
        }
    }

    /**
     * Creates a new, small icon from the specified image.
     *
     * @param sourceImage the non-null image
     */
    public ThemedSingleImageIcon(BufferedImage sourceImage) {
        this(sourceImage, SMALL, SMALL);
    }

    /**
     * Creates a new icon from the specified image. This is a cover for creating
     * an icon of equal width and height.
     *
     * @param sourceImage the non-null image
     * @param size the desired icon width and height
     */
    public ThemedSingleImageIcon(BufferedImage sourceImage, int size) {
        this(sourceImage, size, size);
    }

    /**
     * Creates a new icon from the specified image. The width and height should
     * be given as if the desktop is not being scaled.
     *
     * @param sourceImage the non-null image
     * @param iconWidth the desired icon width
     * @param iconHeight the desired icon height
     */
    public ThemedSingleImageIcon(BufferedImage sourceImage, int iconWidth, int iconHeight) {
        if (iconWidth < 1 || iconHeight < 1) {
            throw new IllegalArgumentException("bad size: " + iconWidth + 'x' + iconHeight);
        }
        source = Objects.requireNonNull(sourceImage, "sourceImage");
        width = iconWidth;
        height = iconHeight;
    }

    /**
     * Returns the resource identifier for this icon. This will be null if the
     * icon was not created from a resource identifier.
     *
     * @return the image resource
     */
    public String getResource() {
        return resource;
    }

    /**
     * Returns the source image that this icon will use as a basis for drawing.
     *
     * @return the non-null source image
     */
    public BufferedImage getImage() {
        // this can only be null if the icon was created with a resource string,
        // therefore resource must not be null
        if (source == null) {
            source = ResourceKit.getImageQuietly(resource);
        }
        return source;
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g, int x, int y) {
        if (scaled == null) {
            if (source == null) {
                getImage();
            }

            // scale the icon dimensions (in desktop units) by the desktop
            // scaling factor to get the actual pixel dimensions we need
            double desktopScale = resources.ResourceKit.estimateDesktopScalingFactor();
            int targetWidth = Math.max(1, (int) (width * desktopScale + 0.5d));
            int targetHeight = Math.max(1, (int) (height * desktopScale + 0.5d));

            scaled = source;
            if (source.getWidth() != targetWidth || source.getHeight() != targetHeight) {
                // in cases where the source image is small, first apply the
                // high-frequency scaling filter to get a larger starting image
                if ((source.getWidth() < HIGH_FREQ_LIMIT || source.getHeight() < HIGH_FREQ_LIMIT) && targetWidth > source.getWidth()) {
                    final BufferedImageOp scaler = new PixelArtUpscalingFilter();
                    while (targetWidth > scaled.getWidth()) {
                        scaled = scaler.filter(source, null);
                    }
                }
                scaled = ImageUtilities.resample(scaled, targetWidth, targetHeight);
            }
            scaled = ThemeInstaller.getInstalledTheme().applyThemeToImage(scaled);
        }

        g.drawImage(scaled, x, y, width, height, null);
    }

    @Override
    public ThemedIcon derive(int newWidth, int newHeight) {
        if (width == newWidth && height == newHeight) {
            return this;
        }
        if (source == null) {
            getImage();
        }
        return new ThemedSingleImageIcon(source, newWidth, newHeight);
    }
}
