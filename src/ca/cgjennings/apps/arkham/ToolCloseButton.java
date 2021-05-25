package ca.cgjennings.apps.arkham;

import java.awt.Color;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicButtonUI;
import resources.ResourceKit;

/**
 * The small black close button that is used on tool panels and windows.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ToolCloseButton extends JButton {

    public ToolCloseButton() {
        super("");
        if (NORMAL == null) {
            NORMAL = ResourceKit.getIcon("ui/controls/close-inv0.png");
            ROLLOVER = ResourceKit.getIcon("ui/controls/close-inv1.png");
            SELECTED = ResourceKit.getIcon("ui/controls/close-inv2.png");
        }
        super.setOpaque(true);
        setContentAreaFilled(false);
        setDefaultCapable(false);
        setUI(new BasicButtonUI());
        setBackground(Color.BLACK);
        setBorderPainted(false);
        setFocusPainted(false);
        setBorder(BorderFactory.createMatteBorder(0, 4, 0, 4, Color.BLACK));
        setMargin(MARGIN);
        setIcon(NORMAL);
        setSelectedIcon(SELECTED);
        setRolloverEnabled(true);
        setRolloverIcon(ROLLOVER);
        setRolloverSelectedIcon(ROLLOVER);

    }

    @Override
    public final void setOpaque(boolean isOpaque) {
        // this keeps getting reset/ignored
        // this'll fix 'em
    }

    private static Icon NORMAL, ROLLOVER, SELECTED;
    private static final Insets MARGIN = new Insets(0, 4, 0, 4);
}
