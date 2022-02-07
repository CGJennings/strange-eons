package ca.cgjennings.ui.theme;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.util.Objects;

/**
 * Icon that shows what a paint looks like by using it to fill a shape.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class PaintSampleIcon extends AbstractThemedIcon {
    private final Paint paint;
    
    public PaintSampleIcon(Paint paint) {
        this.paint = Objects.requireNonNull(paint, "paint");
    }

    @Override
    public PaintSampleIcon derive(int newWidth, int newHeight) {
        if (newWidth < 1 || newHeight < 1) {
            throw new IllegalArgumentException("icon size " + newWidth + 'x' + newHeight);
        }
        if (width == newWidth && height == newHeight) {
            return this;
        }
        
        PaintSampleIcon icon = new PaintSampleIcon(paint);
        icon.width = newWidth;
        icon.height = newHeight;
        return icon;
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g, int x, int y) {
        Paint oldPaint = g.getPaint();
        g.setPaint(paint);
        g.fillOval(x, y, width, height);
        g.setPaint(oldPaint);
    }
}
