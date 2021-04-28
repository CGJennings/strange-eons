package ca.cgjennings.ui.dnd;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 * A temporary border that can be installed on drop targets during a drag
 * operation to provide visual feedback. It draws itself over the existing
 * border (or does nothing if the existing border is <code>null</code>).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class DropBorder implements Border {

    private final Border b;
    private final JComponent t;
    private final Insets i;
    private int depth;
    private static final Color DROP_COLOR;

    static {
        Color c = UIManager.getDefaults().getColor("FileDrop.borderColor");
        if (c == null) {
            c = Color.BLUE;
        }
        DROP_COLOR = c;
    }

    public DropBorder(JComponent dropTarget) {
        t = dropTarget;
        b = dropTarget.getBorder();
        if (b == null) {
            i = new Insets(0, 0, 0, 0);
        } else {
            i = b.getBorderInsets(t);
            depth = Math.min(3, Math.min(i.top, Math.min(i.left, Math.min(i.bottom, i.right))));
        }
    }

    @Override
    public void paintBorder(Component c, Graphics g1, int x, int y, int width, int height) {
        if (b != null) {
            b.paintBorder(c, g1, x, y, width, height);
        }
        if (depth <= 0) {
            return;
        }

        Graphics2D g = (Graphics2D) g1;
        Paint oldPaint = g.getPaint();
        Composite oldComp = g.getComposite();

        try {
            g.setPaint(DROP_COLOR);

            int bx = x + i.left - 1;
            int by = y + i.top - 1;
            int bw = width - (i.left + i.right - 2);
            int bh = height - (i.top + i.bottom - 2);

            g.setComposite(AlphaComposite.SrcOver.derive(depth == 1 ? 0.667f : 0.443f));
            g.drawRect(bx, by, bw, bh);
            if (depth > 2) {
                // draw third line while still at 44.3%
                g.drawRect(bx - 2, by - 2, bw + 4, bh + 4);
            }
            if (depth > 1) {
                g.setComposite(AlphaComposite.SrcOver.derive(0.667f));
                g.drawRect(bx - 1, by - 1, bw + 2, bh + 2);
            }
        } finally {
            g.setComposite(oldComp);
            g.setPaint(oldPaint);
        }
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return i;
    }

    @Override
    public boolean isBorderOpaque() {
        return b == null ? true : b.isBorderOpaque();
    }

    /**
     * Restores the original border on the drop target.
     */
    public void restore() {
        t.setBorder(b);
    }
}
