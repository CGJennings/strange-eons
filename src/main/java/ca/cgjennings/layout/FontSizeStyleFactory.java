package ca.cgjennings.layout;

import java.awt.font.TextAttribute;

/**
 * A parametric style that changes text point size.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class FontSizeStyleFactory implements ParametricStyleFactory {

    @Override
    public TextStyle createStyle(MarkupRenderer renderer, String[] parameters) {
        float size = 10f;
        if (parameters.length >= 1) {
            Object relative = null;
            if (parameters[0].endsWith("%")) {
                relative = renderer.getStyleInCurrentContext(TextAttribute.SIZE);
                parameters[0] = parameters[0].substring(0, parameters[0].length() - 1);
                size = 100f;
            }
            try {
                size = Float.parseFloat(parameters[0]);
            } catch (NumberFormatException e) {
            }
            if (relative != null) {
                size = (size / 100f) * ((Number) relative).floatValue();
            }
        }
        return new TextStyle(TextAttribute.SIZE, size);
    }

    public static final FontSizeStyleFactory getShared() {
        return shared;
    }
    private static final FontSizeStyleFactory shared = new FontSizeStyleFactory();
}
