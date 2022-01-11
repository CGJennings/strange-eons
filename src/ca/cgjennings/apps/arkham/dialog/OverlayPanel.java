package ca.cgjennings.apps.arkham.dialog;

import ca.cgjennings.ui.ArcBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import javax.swing.JPanel;

/**
 * A panel for adding a set of utility buttons to a corner of a dialog window.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class OverlayPanel extends JPanel {

    public OverlayPanel() {
        super(new FlowLayout(FlowLayout.TRAILING, 3, 5));
        setBorder(new ArcBorder(ArcBorder.ARC_TOP_RIGHT, ARC_COLOR, 16, 1, 0.4f));
    }

    private Color getParentBackground() {
        Component c = getParent();
        if (c != null) {
            Color b = c.getBackground();
            if (b != null) {
                return b;
            }
        }
        return Color.GRAY;
    }

    protected Color deriveBackground(Color parentBackground) {
        if (parentBackground == null) {
            return Color.GRAY;
        }
        return new Color(
                Math.min(255, parentBackground.getRed() * FACTOR / 100),
                Math.min(255, parentBackground.getGreen() * FACTOR / 100),
                Math.min(255, parentBackground.getBlue() * FACTOR / 100),
                parentBackground.getAlpha()
        );
    }

    private static final int FACTOR = 108;

    @Override
    protected void paintComponent(Graphics g) {
        Color bg = getParentBackground();
        if (!bg.equals(lastParentBackground)) {
            lastParentBackground = bg;
            super.setBackground(deriveBackground(bg));
        }
        super.paintComponent(g);
    }

    private Color lastParentBackground = null;
    private static final Color ARC_COLOR = new Color(0x55000000, true);
}
