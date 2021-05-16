package ca.cgjennings.apps.arkham;

import java.awt.image.BufferedImage;

/**
 * A ZUI-style previewer for bitmapped images.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class ImageViewer extends AbstractViewer {

    /**
     * Creates a new viewer for bitmapped images.
     */
    public ImageViewer() {
        super();
    }

    private BufferedImage image;

    @Override
    protected BufferedImage getCurrentImage() {
        return getImage();
    }

    /**
     * Returns the vector image displayed by this viewer.
     *
     * @return the displayed image, or {@code null}
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * Sets the image displayed by this viewer.
     *
     * @param image the image to display, or {@code null}
     */
    public void setImage(BufferedImage image) {
        this.image = image;
        if (autoReset) {
            userScaleMultiplier = 1d;
            setTranslation(0d, 0d);
        }
        repaint();
    }

    private boolean autoReset = true;

    /**
     * Returns {@code true} if the zoom and pan will be reset when the
     * image is changed.
     *
     * @return {@code true} if the image transform is reset when the image
     * changes
     */
    public final boolean isScaleResetAutomatically() {
        return autoReset;
    }

    /**
     * Sets whether the zoom and pan will be reset when the image is changed.
     * This is enabled by default.
     *
     * @param autoReset if {@code true}, calls to {@link #setImage} will
     * reset the pan and zoom and settings
     */
    public final void setScaleResetAutomatically(boolean autoReset) {
        this.autoReset = autoReset;
    }
}
