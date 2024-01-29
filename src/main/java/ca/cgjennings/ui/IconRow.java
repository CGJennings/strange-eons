package ca.cgjennings.ui;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Graphics;
import java.util.Locale;
import javax.swing.Icon;

/**
 * An icon row combines two or more icons into a single icon by painting them in
 * a horizontal row.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class IconRow implements Icon {

    private Icon[] icons;
    private int w, h, gap;

    /**
     * Creates a new icon row with a default gap size of four pixels.
     *
     * @param icons the icons to include in the row
     */
    public IconRow(Icon... icons) {
        this(4, icons);
    }

    /**
     * Creates a new icon row with the specified gap size.
     *
     * @param iconGap the gap between icons, in pixels
     * @param icons the icons to include in the row
     */
    public IconRow(int iconGap, Icon... icons) {
        if (icons == null) {
            throw new NullPointerException("icons");
        }
        if (icons.length == 0) {
            throw new IllegalArgumentException("icons.length == 0");
        }
        gap = iconGap;
        for (int i = 0; i < icons.length; ++i) {
            if (icons[i] == null) {
                throw new NullPointerException("icons[" + i + ']');
            }
            w += icons[i].getIconWidth() + iconGap;
            h = Math.max(h, icons[i].getIconHeight());
        }

        if (!ComponentOrientation.getOrientation(Locale.getDefault()).isLeftToRight()) {
            int last = icons.length - 1;
            int middle = icons.length / 2;
            for (int i = 0; i < middle; ++i) {
                Icon temp = icons[i];
                icons[i] = icons[last - i];
                icons[last - i] = temp;
            }
        }

        this.icons = icons;
        w -= iconGap;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        for (int i = 0; i < icons.length; ++i) {
            Icon icon = icons[i];
            icon.paintIcon(c, g, x, y + (h - icon.getIconHeight()) / 2);
            x += icon.getIconWidth() + gap;
        }
    }

    @Override
    public int getIconWidth() {
        return w;
    }

    @Override
    public int getIconHeight() {
        return h;
    }
}
