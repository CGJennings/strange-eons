package ca.cgjennings.graphics.cloudfonts;

import java.io.File;
import java.net.URL;

/**
 * A connector provides the connection details needed
 * to connect to a cloud font collection, and a location
 * where the collection can be cached locally.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public interface CloudFontConnector {
    /**
     * Returns a unique identifier for the connector.
     * This can be used to distinguish data from different connectors,
     * for example, when storing settings.
     * 
     * @return a unique identifier for the connector
     */
    public String getIdentifier();

    /**
     * Given a path to a font resource relative to the cloud file system
     * root, returns a URL that can be used to download the resource.
     *
     * @param fontPath a path to a font resource, such as {@code "ofl/lobster/Lobster-Regular.ttf"}
     * @return a URL that can be used to download the resource
     * @throws IllegalArgumentException if the path is invalid
     */
    public URL getUrlForFontPath(String fontPath);

    /**
     * Returns a URL for metadata that describes the available fonts.
     * 
     * @return a URL for the font metadata
     */
    public URL getUrlForMetadata();

    /**
     * Returns a URL that returns a version code for the font metadata.
     * When this version code changes, any cached metadata should be considered stale.
     * 
     * @return a URL for the font metadata version
     */
    public URL getUrlForMetadataVersion();    

    /**
     * Returns the root directory of the local cache. This directory
     * is used to store downloaded font files and metadata. Two collection
     * instances should not share the same cache root.
     *
     * @return the local cache root
     */
    public File getLocalCacheRoot();

    /**
     * Creates a collection that will use this connector's
     * connection details.
     */
    public CloudFontCollection createFontCollection();
}
