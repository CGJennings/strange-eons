package ca.cgjennings.ui.theme;

import ca.cgjennings.algo.SplitJoin;
import ca.cgjennings.graphics.ImageUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import javax.swing.Icon;
import javax.swing.UIManager;
import resources.ResourceKit;

/**
 * An icon that whose image can change according to the installed {@link Theme}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class ThemedIcon implements Icon {

    private String resource;

    /**
     * Creates a new themed icon. The icon's image will normally be obtained as
     * if loading an image with the {@link ResourceKit}, but if a theme is
     * installed then the theme will be given a chance to switch the image for a
     * themed version. If {@code deferLoading} is {@code true}, then
     * it will not be loaded until the first time it is needed. Otherwise, the
     * image will be loaded immediately.
     *
     * @param resource the resource identifier for the icon
     * @see Theme#applyThemeToImage(java.lang.String)
     */
    public ThemedIcon(String resource) {
        this(resource, false);
    }

    /**
     * Creates a new themed icon. The icon's image will normally be obtained as
     * if loading an image with the {@link ResourceKit}, but if a theme is
     * installed then the theme will be given a chance to switch the image for a
     * themed version. If {@code deferLoading} is {@code true}, then
     * it will not be loaded until the first time it is needed. Otherwise, the
     * image will start loading immediately.
     *
     * @param resource the resource identifier for the icon
     * @param deferLoading if {@code true}, the image is loaded lazily
     * @see Theme#applyThemeToImage(java.lang.String)
     */
    public ThemedIcon(String resource, boolean deferLoading) {
        if (resource == null) {
            throw new NullPointerException("resource");
        }
        this.resource = resource;

        if (!deferLoading) {
            if (Runtime.getRuntime().availableProcessors() > 1) {
                SplitJoin.getInstance().execute(this::getImage);
            } else {
                getImage();
            }
        }
    }

    /**
     * Returns the resource identifier for this icon.
     *
     * @return the image resource
     */
    public String getResource() {
        return resource;
    }

    /**
     * Returns the (possibly themed) image that will be used by the icon.
     *
     * @return the image drawn by the icon
     */
    public final BufferedImage getImage() {
        BufferedImage im = this.im;
        if (im == null) {
            synchronized (this) {
                im = this.im;
                if (im == null) {
                    Theme th = ThemeInstaller.getInstalledTheme();
                    if (th == null) {
                        this.im = im = ResourceKit.getImageQuietly(getResource());
                    } else {
                        this.im = im = th.applyThemeToImage(getResource());
                    }
                }
            }
        }
        return im;
    }
    private volatile BufferedImage im;

    private final BufferedImage getDisabledImage() {
        if (dim != null) return dim;

        BufferedImage im = getImage();
        BufferedImageOp op = (BufferedImageOp) UIManager.get(Theme.DISABLED_ICON_FILTER);
        if (op == null) {
            dim = ImageUtilities.createDisabledImage(im);
        } else {
            dim = op.filter(im, null);
        }
        return dim;
    }
    private BufferedImage dim;

    @Override
    public int getIconWidth() {
        BufferedImage bi = getImage();
        if (bi == null) {
            return 16;
        }
        return bi.getWidth();
    }

    @Override
    public int getIconHeight() {
        BufferedImage bi = getImage();
        if (bi == null) {
            return 16;
        }
        return bi.getHeight();
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        BufferedImage bi = getImage();

        if (c != null && !c.isEnabled()) {
            final BufferedImage dbi = getDisabledImage();
            if (dbi != null) {
                g.drawImage(dbi, x, y, null);
                return;
            }
        }

        if (bi != null) {
            g.drawImage(bi, x, y, null);
        } else {
            // draw placeholder icon
            Color p = g.getColor();
            g.setColor(Color.GRAY);
            int w = getIconWidth(), h = getIconHeight();
            int x2 = w * 2;
            for (int x1 = -w; x1 < x2; x1 += 2) {
                g.drawLine(x1, 0, x1 + w, h);
            }
            g.setColor(p);
        }
    }
}
