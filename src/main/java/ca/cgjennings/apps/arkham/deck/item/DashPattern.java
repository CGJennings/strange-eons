package ca.cgjennings.apps.arkham.deck.item;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JList;

/**
 * The standard dash patterns available to draw paths and outlines.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public enum DashPattern {
    SOLID {
        @Override
        public float[] createDashArray(float size, float capSpread) {
            return null;
        }
    },
    DASHED_LARGE {
        @Override
        public float[] createDashArray(float size, float capSpread) {
            return new float[]{size * LGAP - capSpread, size * MGAP + capSpread};
        }
    },
    DASHED_MEDIUM {
        @Override
        public float[] createDashArray(float size, float capSpread) {
            return new float[]{size * MGAP - capSpread, size * MGAP + capSpread};
        }
    },
    DASHED {
        @Override
        public float[] createDashArray(float size, float capSpread) {
            return new float[]{size * MGAP - capSpread, size + capSpread};
        }
    },
    DOTTED {
        @Override
        public float[] createDashArray(float size, float capSpread) {
            return new float[]{size - capSpread, size + capSpread};
        }
    },
    DASH_DOT {
        @Override
        public float[] createDashArray(float size, float capSpread) {
            return new float[]{size * MGAP - capSpread, size + capSpread, size - capSpread, size + capSpread};
        }
    },
    DASH_DOT_DOT {
        @Override
        public float[] createDashArray(float size, float capSpread) {
            return new float[]{size * MGAP - capSpread, size + capSpread, size - capSpread, size + capSpread, size - capSpread, size + capSpread};
        }
    },
    DASH_DASH_DOT {
        @Override
        public float[] createDashArray(float size, float capSpread) {
            return new float[]{size * MGAP - capSpread, size + capSpread, size * MGAP - capSpread, size + capSpread, size - capSpread, size + capSpread};
        }
    },
    LDASH_DASH {
        @Override
        public float[] createDashArray(float size, float capSpread) {
            return new float[]{size * LGAP - capSpread, size + capSpread, size * MGAP - capSpread, size + capSpread};
        }
    },
    DASH_DASH_DOT_DOT {
        @Override
        public float[] createDashArray(float size, float capSpread) {
            return new float[]{size * MGAP - capSpread, size + capSpread, size * MGAP - capSpread, size + capSpread,
                size - capSpread, size + capSpread, size - capSpread, size + capSpread};
        }
    },
    LDASH_DOT_DASH_DOT {
        @Override
        public float[] createDashArray(float size, float capSpread) {
            return new float[]{size * LGAP - capSpread, size + capSpread,
                size - capSpread, size + capSpread, size * MGAP - capSpread,
                size + capSpread, size - capSpread, size + capSpread};
        }
    },
    LDASH_DOT_DOT_DOT {
        @Override
        public float[] createDashArray(float size, float capSpread) {
            return new float[]{size * LGAP - capSpread, size + capSpread,
                size - capSpread, size + capSpread, size - capSpread,
                size + capSpread, size - capSpread, size + capSpread};
        }
    },;

    private static final float LGAP = 6f;
    private static final float MGAP = 3f;

    /**
     * Returns an array of dash segment lengths suitable for representing this
     * dash pattern at the specified stroke width. The lengths of the dash
     * segments will be padded to account for a line end cap length of half the
     * stroke width (on each end of a stroke).
     *
     * @param penWidth the stroke width
     * @return an array of dash segment lengths for the dash pattern
     */
    public final float[] createDashArray(float penWidth) {
        return createDashArray(penWidth, penWidth);
    }

    /**
     * Returns an array of dash segment lengths suitable for representing this
     * dash pattern at the specified stroke width.
     *
     * @param penWidth the stroke width
     * @param capSpread the amount to pad dash segment lengths by in order to
     * account for the line's end caps
     * @return an array of dash segment lengths for the dash pattern
     */
    public abstract float[] createDashArray(float penWidth, float capSpread);

    /**
     * Applies a model and renderer to a combo box so that the combo box can be
     * used to select a dash pattern.
     *
     * @return the modified combo box
     */
    @SuppressWarnings("unchecked")
    public static JComboBox<DashPattern> createSelector(JComboBox box) {
        DefaultComboBoxModel<DashPattern> model = new DefaultComboBoxModel<>(DashPattern.values());
        box.setModel(model);
        box.setEditable(false);
        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, final Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, "", index, isSelected, cellHasFocus);
                setIcon(
                        new Icon() {
                    @Override
                    public void paintIcon(Component c, Graphics g1, int x, int y) {
                        Graphics2D g = (Graphics2D) g1;
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        BasicStroke s = new BasicStroke(
                                3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                                ((DashPattern) value).createDashArray(3f), 0f
                        );
                        g.setColor(getForeground());
                        Stroke old = g.getStroke();
                        g.setStroke(s);
                        g.drawLine(x + 1, y + IHEIGHT / 2, x + IWIDTH - 2, y + IHEIGHT / 2);
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
                );
                return this;
            }
        });
        return box;
    }
    private static final int IWIDTH = 96;
    private static final int IHEIGHT = 16;
}
