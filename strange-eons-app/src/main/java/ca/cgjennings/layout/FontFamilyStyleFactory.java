package ca.cgjennings.layout;

import java.awt.font.TextAttribute;

import resources.ResourceKit;

/**
 * A parametric style that changes the font family.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class FontFamilyStyleFactory implements ParametricStyleFactory {

    @Override
    public TextStyle createStyle(MarkupRenderer renderer, String[] parameters) {
        String family = "";
        if (parameters.length >= 1) {
            family = parameters[0];
        }
        family = ResourceKit.normalizeFontFamilyName(family);
        return new TextStyle(TextAttribute.FAMILY, family);
    }

    public static final FontFamilyStyleFactory getShared() {
        return shared;
    }

    private static final FontFamilyStyleFactory shared = new FontFamilyStyleFactory();
}
