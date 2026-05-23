package ca.cgjennings.layout;

import java.awt.font.TextAttribute;

/**
 * Create a text style that colours the text background according to the value
 * of the first parameter. It is parsed similarly to colours in HTML, using a
 * parameter of the form #rrggbb or #aarrggbb.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class BackgroundColorStyleFactory extends ForegroundColorStyleFactory {

    @Override
    public TextStyle createStyle(MarkupRenderer renderer, String[] parameters) {
        return new TextStyle(TextAttribute.BACKGROUND, parseColor(parameters));
    }

    public static final BackgroundColorStyleFactory getShared() {
        return shared;
    }

    private static final BackgroundColorStyleFactory shared = new BackgroundColorStyleFactory();
}
