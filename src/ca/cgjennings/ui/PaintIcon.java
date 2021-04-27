package ca.cgjennings.ui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import javax.swing.Icon;

/**
 * An icon that draws its content using a <code>Paint</code> value.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class PaintIcon implements Icon {

    private Paint paint;
    private int width, height;

    public PaintIcon(Paint paint) {
        this(paint, 16, 16);
    }

    public PaintIcon(Paint paint, int size) {
        this(paint, size, size);
    }

    public PaintIcon(Paint paint, int width, int height) {
        if (paint == null) {
            throw new NullPointerException("paint");
        }
        if (width < 1) {
            throw new IllegalArgumentException("width < 1: " + width);
        }
        if (height < 1) {
            throw new IllegalArgumentException("height < 1: " + height);
        }
        this.paint = paint;
        this.width = width;
        this.height = height;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g;
        Paint old = g2.getPaint();
        g2.setPaint(paint);
        g2.fillRect(x, y, width, height);
        g2.setPaint(old);
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }
}
