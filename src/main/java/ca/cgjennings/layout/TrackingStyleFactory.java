package ca.cgjennings.layout;

import java.awt.font.TextAttribute;

/**
 * A parametric style that changes text tracking (spacing between letters).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class TrackingStyleFactory implements ParametricStyleFactory {

    @Override
    public TextStyle createStyle(MarkupRenderer renderer, String[] parameters) {
        float tracking = 0f;
        if (parameters.length >= 1) {
            try {
                tracking = Float.parseFloat(parameters[0]);
            } catch (NumberFormatException e) {
            }
        }
        return new TextStyle(TextAttribute.TRACKING, tracking);
    }

    public static final TrackingStyleFactory getShared() {
        return shared;
    }

    private static final TrackingStyleFactory shared = new TrackingStyleFactory();
}
