package ca.cgjennings.layout;

import java.awt.Color;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.text.AttributedString;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * A combination of {@code TextAttribute}s that represents a particular
 * style of text (a heading, for example).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class TextStyle {

    private HashMap<TextAttribute, Object> styles;

    /**
     * Create a new, empty style.
     */
    public TextStyle() {
        styles = new HashMap<>(8);
    }

    /**
     * Create a new style which initially consists of the specified styles.
     */
    public TextStyle(Object... listOfAttributesAndStyles) {
        this();
        add(listOfAttributesAndStyles);
    }

    /**
     * Create a new style which initially consists of a single style. This may
     * optimize internal storage for a style with only a single member, but does
     * not preclude adding additional members later.
     */
    public TextStyle(TextAttribute attr, Object styleValue) {
        styles = new HashMap<>(1, 1f);
        styles.put(attr, styleValue);
    }

    /**
     * Add a new {@code TextAttribute} to this style.
     */
    public void add(TextAttribute attr, Object styleValue) {
        styles.put(attr, styleValue);
    }

    /**
     * Add an arbitrarily long sequence of styles and values. The sequence of
     * arguments must come in pairs of {@code TextAttribute}s followed by
     * {@code Object} values.
     */
    public void add(Object... listOfAttributesAndStyles) {
        for (int i = 0; i < listOfAttributesAndStyles.length; i += 2) {
            if (!(listOfAttributesAndStyles[i] instanceof TextAttribute)) {
                throw new IllegalArgumentException("Argument " + i + " must be a TextAttribute instance.");
            }
            styles.put((TextAttribute) listOfAttributesAndStyles[i], listOfAttributesAndStyles[i + 1]);
        }
    }

    /**
     * Add all the attributes of the {@code sourceStyle} to this style.
     */
    public void add(TextStyle sourceStyle) {
        styles.putAll(sourceStyle.styles);
    }

    /**
     * Get the value for a {@code TextAttribute} in this style. Returns
     * {@code null} if the attribute has not been specified.
     */
    public Object get(TextAttribute attr) {
        return styles.get(attr);
    }

    /**
     * Remove a {@code TextAttribute} from the style.
     */
    public void remove(TextAttribute attr) {
        styles.remove(attr);
    }

    /**
     * Returns whether the specified attribute is a member of this style.
     */
    public boolean contains(TextAttribute attr) {
        return styles.containsKey(attr);
    }

    /**
     * Apply this style to a range of characters in an
     * {@code AttributedString}.
     *
     * @param s the string to the apply the attributes of the style to
     * @param beginPos the index of the first character in the range
     * @param endPos the index one past the last character in the range
     */
    public void applyStyle(AttributedString s, int beginPos, int endPos) {
        if (beginPos == endPos) {
            return;
        }
        for (Entry<TextAttribute, Object> entry : styles.entrySet()) {
            s.addAttribute(entry.getKey(), entry.getValue(), beginPos, endPos);
        }
    }

    /**
     * Apply a style to all characters in an {@code AttributedString}.
     */
    public void applyStyle(AttributedString s) {
        for (Entry<TextAttribute, Object> entry : styles.entrySet()) {
            s.addAttribute(entry.getKey(), entry.getValue());
        }
    }
    /**
     * A shared instance of a style that will render serif text. If this
     * instance is modified, everyone using it is also affected.
     */
    public static final TextStyle SERIF_STYLE = new TextStyle(TextAttribute.FAMILY, Font.SERIF);
    /**
     * A shared instance of a style that will render sans serif text. If this
     * instance is modified, everyone using it is also affected.
     */
    public static final TextStyle SANSSERIF_STYLE = new TextStyle(TextAttribute.FAMILY, Font.SANS_SERIF);
    /**
     * A shared instance of a style that will render sans serif text. If this
     * instance is modified, everyone using it is also affected.
     */
    public static final TextStyle TYPEWRITER_STYLE = new TextStyle(TextAttribute.FAMILY, Font.MONOSPACED);
    /**
     * A shared instance of a style that will render bold text. If this instance
     * is modified, everyone using it is also affected.
     */
    public static final TextStyle BOLD_STYLE = new TextStyle(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
    /**
     * A shared instance of a style that will render italic text. If this
     * instance is modified, everyone using it is also affected.
     */
    public static final TextStyle ITALIC_STYLE = new TextStyle(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
    public static final TextStyle UPRIGHT_STYLE = new TextStyle(TextAttribute.POSTURE, TextAttribute.POSTURE_REGULAR);
    /**
     * A shared instance of a style that will render superscript text. If this
     * instance is modified, everyone using it is also affected.
     */
    public static final TextStyle SUPERSCRIPT_STYLE = new TextStyle(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUPER);
    /**
     * A shared instance of a style that will render subscript text. If this
     * instance is modified, everyone using it is also affected.
     */
    public static final TextStyle SUBSCRIPT_STYLE = new TextStyle(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB);
    /**
     * A shared instance of a style that will render struck out text. If this
     * instance is modified, everyone using it is also affected.
     */
    public static final TextStyle STRIKETHROUGH_STYLE = new TextStyle(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
    /**
     * A shared instance of a style that will render underlined text. If this
     * instance is modified, everyone using it is also affected.
     */
    public static final TextStyle UNDERLINE_STYLE = new TextStyle(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
    /**
     * A shared instance of a style that will enable kerning of text. If this
     * instance is modified, everyone using it is also affected.
     */
    public static final TextStyle KERNING_ON = new TextStyle(TextAttribute.KERNING, TextAttribute.KERNING_ON);
    /**
     * A shared instance of a style that will disable kerning of text. If this
     * instance is modified, everyone using it is also affected.
     */
    public static final TextStyle KERNING_OFF = new TextStyle(TextAttribute.KERNING, 0);
    /**
     * A shared instance of a style that will enable optional ligature
     * replacement. If this instance is modified, everyone using it is also affected.
     */
    public static final TextStyle LIGATURES_ON = new TextStyle(TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON);
    /**
     * A shared instance of a style that will disable optional ligature
     * replacement. If this instance is modified, everyone using it is also affected.
     */
    public static final TextStyle LIGATURES_OFF = new TextStyle(TextAttribute.LIGATURES, 0);
    /**
     * A shared instance of a style that will enable justification. If this
     * instance is modified, everyone using it is also affected.
     */
    public static final TextStyle JUSTIFY = new TextStyle(TextAttribute.JUSTIFICATION, TextAttribute.JUSTIFICATION_FULL);
    /**
     * A shared instance of a style that will render "plain" text, removing any
     * italic and bold effects.
     */
    public static final TextStyle PLAIN_STYLE = new TextStyle(
            TextAttribute.POSTURE, TextAttribute.POSTURE_REGULAR,
            TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR
    );

    /**
     * Standard colours.
     */
    public static final TextStyle COLOR_BLACK = new TextStyle(TextAttribute.FOREGROUND, Color.BLACK);
    public static final TextStyle COLOR_WHITE = new TextStyle(TextAttribute.FOREGROUND, Color.WHITE);
    public static final TextStyle COLOR_LTGREY = new TextStyle(TextAttribute.FOREGROUND, Color.LIGHT_GRAY);
    public static final TextStyle COLOR_GREY = new TextStyle(TextAttribute.FOREGROUND, Color.GRAY);
    public static final TextStyle COLOR_DKGREY = new TextStyle(TextAttribute.FOREGROUND, Color.DARK_GRAY);
    public static final TextStyle COLOR_RED = new TextStyle(TextAttribute.FOREGROUND, new Color(0xa21b1b));
    public static final TextStyle COLOR_ORANGE = new TextStyle(TextAttribute.FOREGROUND, new Color(0xe65100));
    public static final TextStyle COLOR_YELLOW = new TextStyle(TextAttribute.FOREGROUND, new Color(0xfbc02d));
    public static final TextStyle COLOR_GREEN = new TextStyle(TextAttribute.FOREGROUND, new Color(0x1b5e20));
    public static final TextStyle COLOR_BLUE = new TextStyle(TextAttribute.FOREGROUND, new Color(0x0d47a1));
    public static final TextStyle COLOR_PURPLE = new TextStyle(TextAttribute.FOREGROUND, new Color(0x4a148c));
    public static final TextStyle COLOR_BROWN = new TextStyle(TextAttribute.FOREGROUND, new Color(0x3e2723));

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (TextAttribute ta : styles.keySet()) {
            if (b.length() == 0) {
                b.append("TextStyle{");
            } else {
                b.append(", ");
            }
            String attr = ta.toString();
            int open = attr.indexOf('(');
            int close = attr.lastIndexOf(')');
            if (open >= 0 && close >= 0 && open < close) {
                attr = attr.substring(open + 1, close);
            }

            b.append(attr).append('=').append(styles.get(ta));
        }
        return b.append('}').toString();
    }
}
