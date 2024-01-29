package ca.cgjennings.ui;

import ca.cgjennings.ui.theme.Theme;
import java.awt.Color;
import java.awt.Insets;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.MatteBorder;

/**
 * A matte border whose default style matches the current {@link Theme}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class ThemedMatteBorder extends MatteBorder {

    private static final Color borderColor;

    static {
        UIDefaults uid = UIManager.getLookAndFeelDefaults();
        Color c = uid.getColor("nimbusBorder");
        if (c == null) {
            c = uid.getColor("Button.shadow");
            if (c == null) {
                c = Color.GRAY;
            }
        }
        borderColor = c;
    }

    /**
     * Creates a themed border with one-pixel thick border on all sides.
     */
    public ThemedMatteBorder() {
        super(1, 1, 1, 1, borderColor);
    }

    /**
     * Creates a themed border with the specified border thicknesses.
     */
    public ThemedMatteBorder(int top, int left, int bottom, int right) {
        super(top, left, bottom, right, borderColor);
    }

    /**
     * Creates a themed border with a bottom border of the specified thickness.
     */
    public ThemedMatteBorder(int bottom) {
        super(0, 0, bottom, 0, borderColor);
    }

    /**
     * Creates a themed border with the specified border thicknesses.
     */
    public ThemedMatteBorder(Insets insets) {
        super(insets, borderColor);
    }
}
