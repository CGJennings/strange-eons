package ca.cgjennings.graphics.cloudfonts;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.io.IOException;
import resources.ResourceKit;

/**
 * A family of related cloud fonts that come from a single collection
 * and share a family name.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface CloudFontFamily {
    /**
     * Returns the fonts that are part of this family.
     */
    CloudFont[] getCloudFonts();

    /**
     * Returns the type of font license for this family, or an
     * empty string if the license is unknown.
     * 
     * @return the license type, such as OFL, or an empty string
     */
    String getLicense();

    /**
     * Returns the family name. Note that this is based on the file
     * name, as the actual fonts might not be downloaded. Therefore
     * this name may not exactly match the family name in the actual
     * fonts.
     * 
     * @return the family name
     */
    String getName();

    /**
     * Returns whether the font family name matches the specified tag,
     * which must be a single lower-case word.
     */
    boolean matchesTag(String tag);
    
    /**
     * Returns an array of AWT fonts, one for each font in this family,
     * downloading them from the cloud as necessary.
     * 
     * @return an array of fonts, in the same order as returned by
     * {@link #getCloudFonts()}
     * @throws IOException if an error occurs while downloading, loading, or
     * decoding any font
     */
    Font[] getFonts() throws IOException;
    
    /**
     * Registers this font family. Registered font families can be located
     * by their family name. (For example, when setting {@link TextAttribute#FAMILY}
     * on a markup box.
     * 
     * @return an array of registration results, one for each font in the family
     * @throws IOException if an error occurs while downloading, loading, or
     * decoding any font
     */
     ResourceKit.FontRegistrationResult[] register() throws IOException;
}
