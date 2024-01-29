package ca.cgjennings.layout;

import java.awt.font.TextAttribute;

/**
 * Produce different text widths based on the parameter to the width tag.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class WeightStyleFactory implements ParametricStyleFactory {

    private static final String[] standardWidthNames = {
        "regular", "bold", "light",
        "extralight", "demilight", "semibold",
        "medium", "demibold", "heavy", "extrabold", "ultrabold"

    };
    private static final Float[] standardWidthValues = {
        TextAttribute.WEIGHT_REGULAR,
        TextAttribute.WEIGHT_BOLD,
        TextAttribute.WEIGHT_LIGHT,
        TextAttribute.WEIGHT_EXTRA_LIGHT,
        TextAttribute.WEIGHT_DEMILIGHT,
        TextAttribute.WEIGHT_SEMIBOLD,
        TextAttribute.WEIGHT_MEDIUM,
        TextAttribute.WEIGHT_DEMIBOLD,
        TextAttribute.WEIGHT_HEAVY,
        TextAttribute.WEIGHT_EXTRABOLD,
        TextAttribute.WEIGHT_ULTRABOLD
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
        return new TextStyle(TextAttribute.WEIGHT, value);
    }

    public static final WeightStyleFactory getShared() {
        return shared;
    }

    private static final WeightStyleFactory shared = new WeightStyleFactory();
}
