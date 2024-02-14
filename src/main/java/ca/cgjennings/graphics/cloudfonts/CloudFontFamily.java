package ca.cgjennings.graphics.cloudfonts;

import java.awt.Font;
import java.io.IOException;

import ca.cgjennings.ui.IconProvider;
import resources.ResourceKit;

/**
 * A family of related cloud fonts that come from a single collection
 * and share a family name.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public interface CloudFontFamily extends IconProvider {
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
     * Returns the type of font license for this family, or an
     * empty string if the license is unknown.
     * 
     * @return the license type, such as OFL, or an empty string
     */
    String getLicenseType();

    /**
     * Returns the set of categories that describe this family,
     * such as whether it is a serif or sans-serif font,
     * and whether or not it is intended for display or text.
     * 
     * @return the set of applicable categories
     */
    CategorySet getCategories();

    /**
     * Returns the subsets for which this font provides coverage.
     * 
     * @return an array of subset names
     */
    String[] getSubsets();

    /**
     * Returns the variable axes associated with fonts in this family.
     * 
     * @return an array of font axes
     */
    Axis[] getAxes();

    /**
     * Returns the fonts that are part of this family.
     */
    CloudFont[] getCloudFonts();
    
    /**
     * Returns an array of AWT fonts, one for each font in this family,
     * downloading them as necessary.
     * 
     * @return an array of fonts, in the same order as returned by
     * {@link #getCloudFonts()}
     * @throws IOException if an error occurs while downloading, loading, or
     * decoding any font
     */
    Font[] getFonts() throws IOException;
    
    /**
     * Registers this font family. Registered font families can be located
     * by their family name when creating a font.
     * 
     * @return an array of registration results, one for each font in the family
     * @throws IOException if an error occurs while downloading, loading, or
     * decoding any font
     * @see ResourceKit#registerFont(java.awt.Font)
     */
     ResourceKit.FontRegistrationResult[] register() throws IOException;

     /**
      * Returns true if all fonts in the family have been downloaded
      * to the local cache. Individual fonts can be checked using
      * {@link CloudFont#isDownloaded()}.
      *
      * @return true if all fonts are downloaded
      */
     default boolean isDownloaded() {
        for (CloudFont f : getCloudFonts()) {
            if (!f.isDownloaded()) {
                return false;
            }
        }
        return true;
     }

     /**
      * Returns true if the family has been registered.

      * @return true if all fonts are registered
      */
     default boolean isRegistered() {
        for (CloudFont f : getCloudFonts()) {
            if (!f.isRegistered()) {
                return false;
            }
        }
        return true;
     }
}