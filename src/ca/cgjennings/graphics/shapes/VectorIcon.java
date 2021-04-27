package ca.cgjennings.graphics.shapes;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.net.URL;
import java.util.logging.Level;
import javax.swing.Icon;

/**
 * An icon that is drawn from a {@link VectorImage}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class VectorIcon implements Icon {

    private int w, h;
    private VectorImage im;
    private boolean fit;
//	private BufferedImage imCache;
//	private boolean cache;

    public VectorIcon(URL imageURL) {
        this(imageURL, 48, 48, true);
    }

    public VectorIcon(URL imageURL, int iconWidth, int iconHeight) {
        this(imageURL, iconWidth, iconHeight, true);
    }

    public VectorIcon(URL imageURL, int iconWidth, int iconHeight, boolean fitToSize) {
        VectorImage vi = null;
        try {
            vi = new SVGVectorImage(imageURL);
        } catch (Exception e) {
            StrangeEons.log.log(Level.WARNING, "error creating vector image for icon", e);
        }
        init(vi, iconWidth, iconHeight, fitToSize);
    }

    public VectorIcon(VectorImage vectorImage, int iconWidth, int iconHeight, boolean fitToSize) {
        init(vectorImage, iconWidth, iconHeight, fitToSize);
    }

    private void init(VectorImage vectorImage, int iconWidth, int iconHeight, boolean fitToSize) {
        if (iconWidth < 0) {
            throw new IllegalArgumentException("iconWidth < 0");
        }
        if (iconHeight < 0) {
            throw new IllegalArgumentException("iconHeight < 0");
        }
        im = vectorImage;
        w = iconWidth;
        h = iconHeight;
        fit = fitToSize;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (im == null || w == 0 || h == 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        Object aa = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        im.paint(g2, x, y, w, h, fit);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
    }

    /**
     * Returns the width of this icon. This is the width occupied by the icon in
     * an interface layout.
     *
     * @return the width of the icon
     * @see #setIconWidth(int)
     */
    @Override
    public final int getIconWidth() {
        return w;
    }

    /**
     * Sets the width of this icon. This is the width occupied by the icon in an
     * interface layout.
     *
     * @param width the new icon width
     * @see #getIconWidth()
     */
    public final void setIconWidth(int width) {
        if (width < 0) {
            throw new IllegalArgumentException("width < 0");
        }
//		if( w != width ) {
        w = width;
//			cache = null;
//		}
    }

    /**
     * Returns the height of this icon. This is the height occupied by the icon
     * in an interface layout.
     *
     * @return the height of the icon
     * @see #setIconHeight(int)
     */
    @Override
    public final int getIconHeight() {
        return h;
    }

    /**
     * Sets the height of this icon. This is the height occupied by the icon in
     * an interface layout.
     *
     * @param height the new icon width
     * @see #getIconHeight()
     */
    public final void setIconHeight(int height) {
        if (height < 0) {
            throw new IllegalArgumentException("height < 0");
        }
//		if( h != height ) {
        h = height;
//			cache = null;
//		}
    }

    /**
     * Returns <code>true</code> if the image's aspect ratio will be maintained
     * by fitting it within the icon width and height.
     *
     * @return <code>true</code> if the painted area will pad one dimension, if
     * necessary, to maintain the original image's aspect ratio
     * @see #setImageFittingEnabled(boolean)
     */
    public final boolean isImageFittingEnabled() {
        return fit;
    }

    /**
     * Sets whether the image's aspect ratio will be maintained by fitting it
     * within the icon width and height.
     *
     * @param fit if <code>true</code> the painted area will be padded in one
     * dimension, if necessary, to maintain the original image's aspect ratio
     * @see #setImageFittingEnabled(boolean)
     */
    public final void setImageFittingEnabled(boolean fit) {
        this.fit = fit;
    }

    /**
     * Returns the vector image painted by this icon.
     *
     * @return the vector image used to create the icon
     */
    public final VectorImage getImage() {
        return im;
    }

    /**
     * Returns a new vector icon for the same image as this icon, but with a
     * different icon size. A new icon is always returned, even if the new size
     * is equal to this icon's size.
     *
     * @param newWidth the new icon width
     * @param newHeight the new icon height
     * @return an icon identical to this icon, except for having the newly
     * specified icon dimensions
     */
    public final VectorIcon derive(int newWidth, int newHeight) {
        return new VectorIcon(im, newWidth, newHeight, fit);
    }

    @Override
    public String toString() {
        return "VectorIcon{" + w + " x " + h + '}';
    }
}
