package ca.cgjennings.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import resources.ResourceKit;

/**
 * A label that is styled as a warning box. The label will use a special warning
 * icon. If the label includes text, then the label will be opaque and provide a
 * special border and background. Otherwise, it will simply show the warning
 * icon with no other decoration.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class JWarningLabel extends JLabel {

    /**
     * Create a warning label with empty text.
     */
    public JWarningLabel() {
        super();
        setIcon(ResourceKit.getIcon("ui/warning.png"));
        final Font f = getFont();
        setFont(f.deriveFont(f.getSize2D() - 1f));
        setIconTextGap(6);
        setBackground(Color.WHITE);
        setForeground(Color.BLACK);
    }

    /**
     * Create a warning label with the specified text.
     *
     * @param warning the warning text to set on the label
     */
    public JWarningLabel(String warning) {
        this();
        setText(warning);
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        if (text == null || text.isEmpty()) {
            setBorder(null);
            setOpaque(false);
        } else {
            setBorder(textBorder);
            setOpaque(true);
        }
    }

    private Border textBorder = new CompoundBorder(
            new EtchedBorder(new Color(255, 204, 51), new Color(204, 102, 0)),
            new EmptyBorder(2, 4, 2, 4)
    );
}
