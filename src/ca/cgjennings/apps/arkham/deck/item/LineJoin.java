package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.ui.IconProvider;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import javax.swing.Icon;
import static resources.Language.string;

/**
 * An enumeration of the available line join styles used when drawing a path,
 * such as when outlining a shape.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public enum LineJoin implements IconProvider {
    /**
     * Joins path segments by connecting the outer corners of their wide
     * outlines with a straight segment.
     */
    BEVEL(BasicStroke.JOIN_BEVEL, "style-li-bevel"),
    /**
     * Joins path segments by extending their outside edges until they meet.
     */
    MITER(BasicStroke.JOIN_MITER, "style-li-mitered"),
    /**
     * Joins path segments by rounding off the corner at a radius of half the
     * line width.
     */
    ROUND(BasicStroke.JOIN_ROUND, "style-li-join-rounded");

    private LineJoin(int strokeJoin, String key) {
        join = strokeJoin;
        name = string(key);
    }

    private Icon icon;
    private int join;
    private String name;

    static LineJoin fromInt(int icap) {
        LineJoin ojoin;
        if (icap == MITER.join) {
            ojoin = MITER;
        } else if (icap == ROUND.join) {
            ojoin = ROUND;
        } else {
            ojoin = BEVEL;
        }
        return ojoin;
    }

    int toInt() {
        return join;
    }

    @Override
    public Icon getIcon() {
        synchronized (this) {
            if (icon == null) {
                icon = new LineJoinIcon(this);
            }
            return icon;
        }
    }

    @Override
    public String toString() {
        return name;
    }

    private static final int IWIDTH = 16;
    private static final int IHEIGHT = 16;

    public static class LineJoinIcon implements Icon {

        private int join;

        public LineJoinIcon(LineJoin join) {
            this.join = join.toInt();
        }

        @Override
        public void paintIcon(Component c, Graphics g1, int x, int y) {
            Graphics2D g = (Graphics2D) g1;

//			g.setColor( Color.RED );
//			g.drawRect( x, y, IWIDTH, IHEIGHT );
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            BasicStroke s = new BasicStroke(
                    7f, BasicStroke.CAP_BUTT, join
            );
            g.setColor(c == null ? Color.BLACK : c.getForeground());
            Stroke old = g.getStroke();
            g.setStroke(s);
            Path2D.Float path = new Path2D.Float();
            path.moveTo(x + 5, y + IHEIGHT - 1);
            path.lineTo(x + 5, y + 5);
            path.lineTo(x + IWIDTH - 2, y + 5);
            g.draw(path);
            g.setStroke(old);
        }

        @Override
        public int getIconWidth() {
            return IWIDTH;
        }

        @Override
        public int getIconHeight() {
            return IHEIGHT;
        }
    }
}
