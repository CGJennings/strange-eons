package ca.cgjennings.ui;

import ca.cgjennings.ui.theme.Theme;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * A label control that is styled for use as a panel title (the default style is
 * a black background and bold white text).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class TitleLabel extends JLabel {

    static final Border TITLE_SPACING_BORDER = new EmptyBorder(2, 4, 2, 4);

    public TitleLabel() {
        init();
    }

    public TitleLabel(String text) {
        super(text);
        init();
    }

    private void init() {
        setBorder(TITLE_SPACING_BORDER);
        setBackground(getColor(Theme.SIDEPANEL_TITLE_BACKGROUND, Color.BLACK));
        setForeground(getColor(Theme.SIDEPANEL_TITLE_FOREGROUND, Color.WHITE));
        setOpaque(true);
        setFont(getFont().deriveFont(Font.BOLD));
    }

    private Color getColor(String name, Color fallback) {
        UIDefaults ui = UIManager.getDefaults();
        return ui.containsKey(name) ? ui.getColor(name) : fallback;
    }
}
