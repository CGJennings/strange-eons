package ca.cgjennings.graphics.cloudfonts;

import java.io.IOException;

/**
 * A collection of cloud fonts. Calling any method may incur
 * an intial network request to retrieve metadata.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public interface CloudFontCollection {
    /**
     * Returns an array of available families in this cloud collection.
     * 
     * @return an array of available families
     * @throws IOException if an exception occurs while getting family metadata
     */
    CloudFontFamily[] getFamilies() throws IOException;
    
    /**
     * Returns a family by name.
     * 
     * @param name the name of the family to locate
     * @return the matching family, or null
     * @throws IOException if an exception occurs while getting family metadata
     */
    CloudFontFamily getFamily(String name) throws IOException;    
    
    /**
     * Attempts to locate a font family in the collection that matches the
     * description, which should be a family name or part of a family name.
     * 
     * @param description the description to match, case insensitive
     * @return an array of all matching families, possibly empty
     * @throws IOException if an exception occurs while getting family metadata
     */
    CloudFontFamily[] match(String description) throws IOException;

    /**
     * Forces a refresh of the collection by downloading the most recent
     * font metadata. Some collections may ignore this request.
     */
    void refresh() throws IOException;
}
