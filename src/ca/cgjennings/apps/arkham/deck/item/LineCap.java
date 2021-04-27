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

    private int cap;
    private Icon icon;
    private String name;

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

//	/**
//	 * Applies a model and renderer to a combo box so that the combo
//	 * box can be used to select a a line join pattern.
//	 * @return the modified combo box
//	 */
//	public static JComboBox createSelector( JComboBox box ) {
//		LineCap[] values = LineCap.values();
//		DefaultComboBoxModel model = new DefaultComboBoxModel( values );
//		box.setModel( model );
//		box.setEditable( false );
//		box.setRenderer( new DefaultListCellRenderer() {
//			@Override
//			public Component getListCellRendererComponent( JList list, final Object value, int index, boolean isSelected, boolean cellHasFocus ) {
//				String label;
//				switch( (LineCap) value ) {
//					case BUTT: label = string("style-li-butt"); break;
//					case ROUND: label = string("style-li-round"); break;
//					case SQUARE: label = string("style-li-square"); break;
//					default: throw new AssertionError();
//				}
//				super.getListCellRendererComponent( list, label, index, isSelected, cellHasFocus );
//				setIcon( new LineCapIcon( (LineCap) value ) );
//				return this;
//			}
//		});
//		return box;
//	}
    private static final int IWIDTH = 16;
    private static final int IHEIGHT = 16;

    static class LineCapIcon implements Icon {

        private int cap;

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
