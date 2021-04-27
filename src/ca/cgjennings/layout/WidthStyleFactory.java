package ca.cgjennings.layout;

import java.awt.font.TextAttribute;

/**
 * Produce different text widths based on the parameter to the width tag.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class WidthStyleFactory implements ParametricStyleFactory {

    private static String[] standardWidthNames = {
        "regular", "semicondensed", "semiextended", "condensed", "extended"
    };
    private static Float[] standardWidthValues = {
        TextAttribute.WIDTH_REGULAR,
        TextAttribute.WIDTH_SEMI_CONDENSED,
        TextAttribute.WIDTH_SEMI_EXTENDED,
        TextAttribute.WIDTH_CONDENSED,
        TextAttribute.WIDTH_EXTENDED
    };

    @Override
    public TextStyle createStyle(MarkupRenderer renderer, String[] parameters) {
        Float value = TextAttribute.WIDTH_REGULAR;
        if (parameters.length >= 1 && parameters[0].length() > 0) {
            String p = parameters[0];
            if (Character.isLetter(p.charAt(0))) {
                for (int i = 0; i < standardWidthNames.length; ++i) {
                    if (standardWidthNames[i].equalsIgnoreCase(p)) {
                        value = standardWidthValues[i];
                    }
                }
            } else {
                try {
                    float v = Float.parseFloat(p);
                    if (v > 0f && v <= 100f) {
                        value = v;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        return new TextStyle(TextAttribute.WIDTH, value);
    }

    public static final WidthStyleFactory getShared() {
        return shared;
    }

    private static final WidthStyleFactory shared = new WidthStyleFactory();
}
