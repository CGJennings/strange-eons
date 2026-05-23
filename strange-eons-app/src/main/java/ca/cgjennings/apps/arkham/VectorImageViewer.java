package ca.cgjennings.apps.arkham;

import ca.cgjennings.graphics.shapes.VectorImage;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;

/**
 * A ZUI-style previewer for vector images.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class VectorImageViewer extends AbstractViewer {
//	private BufferedImage cache;

    private VectorImage image;

    /**
     * Creates a new viewer for vector images.
     */
    public VectorImageViewer() {
        super();
    }

    @Override
    protected BufferedImage getCurrentImage() {
//		if( image == null ) return null;
//		int idealWidth = (int) (getWidth() * userScaleMultiplier + 0.5d);
//		if( cache == null || cache.getWidth() != idealWidth ) {
//			int idealHeight = (int) (getHeight() * userScaleMultiplier + 0.5d);
//			cache = image.createRasterImage( idealWidth, idealHeight, true );
//		}
//		return cache;
        return null;
    }

    /**
     * Returns the vector image displayed by this viewer.
     *
     * @return the displayed image, or {@code null}
     */
    public VectorImage getImage() {
        return image;
    }

    /**
     * Sets the vector image displayed by this viewer.
     *
     * @param image the image to display, or {@code null}
     */
    public void setImage(VectorImage image) {
        this.image = image;
//		cache = null;
        if (autoReset) {
            userScaleMultiplier = 1d;
            setTranslation(0d, 0d);
        }
        repaint();
    }

    private boolean autoReset = true;

    /**
     * Returns {@code true} if the zoom and pan will be reset when the image is
     * changed.
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
     * @param autoReset if {@code true}, calls to {@link #setImage} will reset
     * the pan and zoom and settings
     */
    public final void setScaleResetAutomatically(boolean autoReset) {
        this.autoReset = autoReset;
    }

    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        g.setPaint(getBackgroundPaint());
        g.fillRect(0, 0, getWidth(), getHeight());
        Shape oldClip = g.getClip();

        // adjust for border insets
        double compWidth, compHeight;
        {
            borderInsets = getInsets(borderInsets);
            final int cw = getWidth() - (borderInsets.left + borderInsets.right);
            final int ch = getHeight() - (borderInsets.top + borderInsets.bottom);
            g.translate(borderInsets.left, borderInsets.top);
            g.clipRect(0, 0, cw, ch);
            compWidth = cw - 4d;
            compHeight = ch - 4d;
        }

        if (image == null) {
            return;
        }

        double hscale = compWidth / image.getWidth();
        double vscale = compHeight / image.getHeight();
        double scale = hscale < vscale ? hscale : vscale;
        if (scale > 1d) {
            scale = 1d;
        }
        if (autoFitToWindow) {
            scale *= userScaleMultiplier;
        } else {
            scale = userScaleMultiplier;
        }

        double newWidth = image.getWidth() * scale;
        double newHeight = image.getHeight() * scale;
        double x = 2d + (compWidth - newWidth) / 2;
        double y = 2d + (compHeight - newHeight) / 2;

        {
            Graphics2D clone = (Graphics2D) g.create();
            clone.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            clone.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            clone.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            clone.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            image.paint(clone, tx + x, ty + y, newWidth, newHeight, false);
        }

        paintZoomLabel(g, scale);

//		if( scale < 1.0 ) {
//			paintLoupe( g, currentImage, ix, iy, newWidth, newHeight );
//		}
        g.setClip(oldClip);
    }

}
