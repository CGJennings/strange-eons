package ca.cgjennings.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import resources.ResourceKit;

/**
 * A banner is a tall icon placed along the left side of a dialog. It is
 * commonly used for staged disclosure dialogs, for complex dialogs that are a
 * step in a larger operation, or when a dialog's importance should be
 * emphasized.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class Banner extends JLabel {

    public Banner() {
        init(ResourceKit.getImage("icons/application/banner.jpg"));
    }

    public Banner(BufferedImage image) {
        init(image);
    }

    private void init(BufferedImage bi) {
        setBackground(Color.DARK_GRAY);
        setOpaque(true);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.DARK_GRAY));
        setBannerImage(bi);
        setVerticalAlignment(TOP);
    }

    /**
     * Composes the current banner image with a new image, replacing the current
     * image. This meant to be used with the default constructor to add a badge
     * on top of the default banner image.
     *
     * @param imageResource the image to draw over the current banner image
     *
     */
    public void compose(String imageResource, int dx, int dy) {
        BufferedImage ci = ResourceKit.getImage(imageResource);
        BufferedImage bi = (BufferedImage) ((ImageIcon) getIcon()).getImage();
        Graphics2D g = bi.createGraphics();
        try {
            int x = (bi.getWidth() - ci.getWidth()) / 2;
            int y = 32;
            g.drawImage(ci, x + dx, y + dy, null);
        } finally {
            g.dispose();
        }
        if (isShowing()) {
            super.setIcon(new ImageIcon(bi));
        } else {
            ((ImageIcon) getIcon()).setImage(bi);
        }
    }

    /**
     * Calls to this method are ignored.
     */
    @Override
    public void setText(String text) {
    }

    /**
     * Calls to this method are ignored.
     */
    @Override
    public void setIcon(Icon icon) {
    }

    private String banner = null;

    public void setBannerImage(BufferedImage image) {
        setBannerImageImpl(image);
        banner = null;
    }

    private void setBannerImageImpl(BufferedImage image) {
        image = ResourceKit.createBleedBanner(image);
        super.setIcon(new ImageIcon(image));
    }

    public void setImageResource(String image) {
        setBannerImage(ResourceKit.getImage(image));
        banner = image;
    }

    public String getImageResource() {
        return banner;
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension d = super.getMinimumSize();
        return new Dimension(d.width, 0);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, 0);
    }
}
