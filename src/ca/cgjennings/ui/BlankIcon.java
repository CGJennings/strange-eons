package ca.cgjennings.ui;

import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

/**
 * An icon that takes up space but paints nothing.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class BlankIcon implements Icon {

    private int width, height;

    /**
     * Creates a 16x16 blank icon.
     */
    public BlankIcon() {
        this(16, 16);
    }

    /**
     * Creates a blank icon with equal width and height.
     *
     * @param size the width and height of the icon
     */
    public BlankIcon(int size) {
        this(size, size);
    }

    /**
     * Creates a blank icon with the given width and height.
     *
     * @param width the width of the icon
     * @param height the height of the icon
     */
    public BlankIcon(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public int getIconHeight() {
        return height;
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
    }
}
