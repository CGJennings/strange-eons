package ca.cgjennings.ui.theme;

import java.awt.Graphics2D;
import java.awt.Paint;
import javax.swing.Painter;

/**
 * A painter that simply paints the entire surface with a {@code Color} or other
 * {@code Paint}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class SolidPainter implements Painter {

    private Paint p;

    public SolidPainter(Paint paint) {
        if (paint == null) {
            throw new NullPointerException("paint");
        }
        p = paint;
    }

    @Override
    public void paint(Graphics2D g, Object object, int width, int height) {
        g.setPaint(p);
        g.fillRect(0, 0, width, height);
    }
}
