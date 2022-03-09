package ca.cgjennings.apps.arkham;

import java.awt.Color;
import java.awt.Insets;
import javax.swing.BorderFactory;
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
        setUI(new BasicButtonUI());
        super.setOpaque(true);
        setContentAreaFilled(false);
        setDefaultCapable(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setBorder(BorderFactory.createMatteBorder(0, 4, 0, 4, Color.BLACK));
        setMargin(MARGIN);
        setRolloverEnabled(true);
        setBackground(Color.BLACK);
        
        setIcon(ResourceKit.getIcon("tab-close"));
        setRolloverIcon(ResourceKit.getIcon("tab-close-hi"));
        setRolloverSelectedIcon(ResourceKit.getIcon("tab-close-press"));
        setSelectedIcon(ResourceKit.getIcon("tab-close-press"));
    }

    @Override
    public final void setOpaque(boolean isOpaque) {
        // this keeps getting reset/ignored
        // this'll fix 'em
    }

    private static final Insets MARGIN = new Insets(0, 4, 0, 4);
}
