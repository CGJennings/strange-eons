package ca.cgjennings.apps.arkham.dialog;

import ca.cgjennings.ui.ArcBorder;
import java.awt.Color;
import java.awt.FlowLayout;
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
        setBorder(new ArcBorder(ArcBorder.ARC_TOP_RIGHT, DEFAULT_LINE, 16, 2, 0.4f));
        setBackground(DEFAULT_BACKGROUND);
    }

    public static Color DEFAULT_LINE = Color.GRAY;
    public static Color DEFAULT_BACKGROUND = new Color(0xaa_aaaa);
}
