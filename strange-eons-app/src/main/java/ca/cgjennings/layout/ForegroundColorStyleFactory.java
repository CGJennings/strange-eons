package ca.cgjennings.layout;

import java.awt.Color;
import java.awt.font.TextAttribute;

/**
 * Create a text style that colours text according to the value of the
 * parameters. It is parsed similarly to colours in HTML, using a parameter of
 * the form #rrggbb or #aarrggbb (the # is optional).
 * <p>
 * Alternatively, the color may consist of floating point numbers between 0 and
 * 1 that describe hue, saturation, brightness, and (optionally) alpha
 * components as an angle (hue) and ratios, e.g. "180 0.5 0.5 0.5".
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class ForegroundColorStyleFactory implements ParametricStyleFactory {

    private static float parseComponent(String s, boolean clip) throws NumberFormatException {
        float f = Float.parseFloat(s);
        if (clip) {
            if (f < 0f) {
                f = 0f;
            }
            if (f > 1f) {
                f = 1f;
            }
        }
        return f;
    }

    /**
     * Parse a color value from the tag's parameters. Returns {@code null} if
     * the parameters cannot be effectively parsed, in which case a style is
     * returned that has no effect.
     */
    public static Color parseColor(String[] parameters) {
        Color c = null;
        try {
            if (parameters.length == 1) {
                String p = parameters[0];
                if (p.startsWith("#")) {
                    p = p.substring(1);
                }
                if (p.length() > 8) {
                    p = p.substring(0, 8);
                }

                long RGB = Long.parseLong(p, 16);
                int rgb = (int) RGB;

                if (p.length() > 6) {
                    c = new Color(rgb, true);
                } else {
                    c = new Color(rgb);
                }
            } else if (parameters.length == 3 || parameters.length == 4) {
                int rgb = Color.HSBtoRGB(parseComponent(parameters[0], false) / 360f,
                        parseComponent(parameters[1], true),
                        parseComponent(parameters[2], true));
                c = new Color(rgb);
                if (parameters.length == 4) {
                    c = new Color(c.getRed(), c.getGreen(), c.getBlue(),
                            Math.round(parseComponent(parameters[3], true) * 255f));
                }
            }
        } catch (NumberFormatException e) {
        }

        return c;
    }

    @Override
    public TextStyle createStyle(MarkupRenderer renderer, String[] parameters) {
        return new TextStyle(TextAttribute.FOREGROUND, parseColor(parameters));
    }

    public static ForegroundColorStyleFactory getShared() {
        return shared;
    }

    private static final ForegroundColorStyleFactory shared = new ForegroundColorStyleFactory();
}
