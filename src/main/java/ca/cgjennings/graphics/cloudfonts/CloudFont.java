package ca.cgjennings.graphics.cloudfonts;

import java.awt.Font;
import java.io.File;
import java.io.IOException;

/**
 * Represents a font that can be downloaded from a {@link CloudFontCollection}.
 * 
 * <p>While some methods are included to obtain basic information about the font,
 * these may be provided without access to the actual font, and therefore 
 * are not definitive since a remote font could change at any time. 
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public interface CloudFont {
    /**
     * Returns the family that includes the font.
     *
     * @return the font family
     */
    CloudFontFamily getFamily();

    /**
     * Returns the name of the font.
     *
     * @return the font name
     */
    String getName();

    /**
     * Returns a description of the style of the font,
     * if any is available, such as "Regular" or "BoldItalic".
     *
     * @return the font style
     */
    String getStyle();

    /**
     * Returns an array of variable axes associated with the font.
     *
     * @return an array of font axes
     */
    Axis[] getAxes();
    
    /**
     * Returns an AWT font for this cloud font, downloading it
     * from the cloud if necessary. If the entire font family
     * should be downloaded, use
     * {@link CloudFontFamily#download()} instead
     * as it may download needed files more efficiently.
     * 
     * @return a font instance for this cloud font
     * @throws IOException if an error occurs while downloading, loading, or
     * decoding the font
     */
    Font getFont() throws IOException;

    /**
     * Returns the file of the downloaded font, downloading it
     * from the cloud if necessary.
     * 
     * @return the local cache file for the font
     * @throws IOException if an error occurs while downloading the font
     */
    File getFontFile() throws IOException;

    /**
     * Returns true if the font is downloaded locally.
     * In some cases, requesting the font may initiate a download
     * even if this method returns true (for example, if the local
     * copy is out of date.)
     * 
     * @return true if the font is in the local download cache
     */
    boolean isDownloaded();
    
    /**
     * Returns true if this font was successfully registered during a call
     * to {@link CloudFontFamily#register()}.
     * 
     * @return true if the font is known to be registereds
     */
    boolean isRegistered();

    /**
     * Returns a string descriptor of the font.
     *
     * @return a string descriptor of the font
     */
    @Override
    String toString();
}
