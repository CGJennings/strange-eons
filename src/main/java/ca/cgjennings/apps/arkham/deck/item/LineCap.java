package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.ui.IconProvider;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import javax.swing.Icon;
import static resources.Language.string;

/**
 * An enumeration of the line end cap styles.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public enum LineCap implements IconProvider {
    /**
     * Ends unclosed subpaths and dash segments with no added decoration.
     */
    BUTT(BasicStroke.CAP_BUTT, "style-li-butt"),
    /**
     * Ends unclosed subpaths and dash segments with a round decoration that has
     * a radius equal to half of the width of the pen.
     */
    ROUND(BasicStroke.CAP_ROUND, "style-li-round"),
    /**
     * Ends unclosed subpaths and dash segments with a square projection that
     * extends beyond the end of the segment to a distance equal to half of the
     * line width.
     */
    SQUARE(BasicStroke.CAP_SQUARE, "style-li-square");

    private LineCap(int strokeCap, String key) {
        cap = strokeCap;
        name = string(key);
    }

    private final int cap;
    private Icon icon;
    private final String name;

    static LineCap fromInt(int icap) {
        LineCap ocap;
        if (icap == SQUARE.cap) {
            ocap = SQUARE;
        } else if (icap == ROUND.cap) {
            ocap = ROUND;
        } else {
            ocap = BUTT;
        }
        return ocap;
    }

    int toInt() {
        return cap;
    }

    @Override
    public Icon getIcon() {
        synchronized (this) {
            if (icon == null) {
                icon = new LineCapIcon(this);
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

    static class LineCapIcon implements Icon {

        private final int cap;

        public LineCapIcon(LineCap cap) {
            this.cap = cap.toInt();
        }

        @Override
        public void paintIcon(Component c, Graphics g1, int x, int y) {
            Graphics2D g = (Graphics2D) g1;

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Stroke old = g.getStroke();

            if (cap != BasicStroke.CAP_BUTT) {
                g.setColor(Color.GRAY);
                g.setStroke(new BasicStroke(
                        7f, cap, BasicStroke.JOIN_BEVEL
                ));
                g.drawLine(x + 4, y + IHEIGHT / 2, x + IWIDTH - 4, y + IHEIGHT / 2);
            }

            g.setColor(c == null ? Color.BLACK : c.getForeground());
            g.setStroke(new BasicStroke(
                    7f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL
            ));
            g.drawLine(x + 4, y + IHEIGHT / 2, x + IWIDTH - 4, y + IHEIGHT / 2);
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
