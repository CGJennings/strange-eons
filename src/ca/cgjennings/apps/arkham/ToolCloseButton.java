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
        if (DK_NORMAL == null) {
            DK_NORMAL = ResourceKit.getIcon("ui/controls/close-inv0.png");
            DK_ROLLOVER = ResourceKit.getIcon("ui/controls/close-inv1.png");
            DK_SELECTED = ResourceKit.getIcon("ui/controls/close-inv2.png");
            LI_NORMAL = ResourceKit.getIcon("ui/controls/close0.png");
            LI_ROLLOVER = ResourceKit.getIcon("ui/controls/close1.png");
            LI_SELECTED = ResourceKit.getIcon("ui/controls/close2.png");
        }
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
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        if (bg == null) {
            bg = getBackground();
        }
        final int greyness = bg == null ? 255 : (bg.getRed() + bg.getGreen() + bg.getBlue()) / 3;
        if (greyness < 128) {
            setIcon(DK_NORMAL);
            setSelectedIcon(DK_SELECTED);
            setRolloverIcon(DK_ROLLOVER);
            setRolloverSelectedIcon(DK_ROLLOVER);
        } else {
            setIcon(LI_NORMAL);
            setSelectedIcon(LI_SELECTED);
            setRolloverIcon(LI_ROLLOVER);
            setRolloverSelectedIcon(LI_ROLLOVER);
        }
    }

    @Override
    public final void setOpaque(boolean isOpaque) {
        // this keeps getting reset/ignored
        // this'll fix 'em
    }

    private static Icon DK_NORMAL, DK_ROLLOVER, DK_SELECTED, LI_NORMAL, LI_ROLLOVER, LI_SELECTED;
    private static final Insets MARGIN = new Insets(0, 4, 0, 4);
}
