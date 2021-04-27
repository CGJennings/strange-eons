package ca.cgjennings.ui;

import ca.cgjennings.ui.theme.Theme;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 * A title heading label with large font and grey underline.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.00
 */
@SuppressWarnings("serial")
public class JHeading extends JLabel {

    public JHeading() {
        init();
    }

    public JHeading(Icon image) {
        super(image);
        init();
    }

    public JHeading(Icon image, int horizontalAlignment) {
        super(image, horizontalAlignment);
        init();
    }

    public JHeading(String text) {
        super(text);
        init();
    }

    public JHeading(String text, int horizontalAlignment) {
        super(text, horizontalAlignment);
        init();
    }

    public JHeading(String text, Icon icon, int horizontalAlignment) {
        super(text, icon, horizontalAlignment);
        init();
    }

    private void init() {
        setFont(HEADING_FONT);
        setBorder(HEADING_BORDER);
    }

    /**
     * Sets whether this heading is a main heading (<code>false</code>, the
     * default) or a subheading (<code>true</code>).
     *
     * @param isSubhead <code>true</code> to make this a subheading
     */
    public void setSubheading(boolean isSubhead) {
        setFont(isSubhead ? SUBHEADING_FONT : HEADING_FONT);
    }

    /**
     * Returns <code>true</code> if this is a subheading.
     *
     * @return <code>true</code> if the font matches the subheading font
     */
    public boolean isSubheading() {
        return getFont().equals(SUBHEADING_FONT);
    }

    /**
     * The theme-sensitive color of the underline appearing below a heading.
     */
    public static final Color HEADING_LINE_COLOR;
    /**
     * border used to apply an underline effect to a heading.
     */
    public static final Border HEADING_BORDER;
    /**
     * The theme-sensitive font used for headings.
     */
    public static final Font HEADING_FONT;
    /**
     * The theme-sensitive font used for subheadings.
     */
    public static final Font SUBHEADING_FONT;

    static {
        UIDefaults uid = UIManager.getDefaults();

        Color c = uid.getColor("nimbusBorder");
//c = uid.getColor( "nimbusOrange" );	
        if (c == null) {
            c = Color.GRAY;
        }
        HEADING_LINE_COLOR = c;

        Border b = uid.getBorder(Theme.HEADING_BORDER);
        if (b == null) {
            b = BorderFactory.createMatteBorder(0, 0, 1, 0, c);
        }
        HEADING_BORDER = b;
        Font f = uid.getFont("TitledBorder.font");
        if (f == null) {
            f = new JLabel().getFont();
        }
        f = f.deriveFont(Font.BOLD);
        HEADING_FONT = f.deriveFont(f.getSize2D() + 4f);
        SUBHEADING_FONT = f.deriveFont(f.getSize2D() + 2f);
    }
}
