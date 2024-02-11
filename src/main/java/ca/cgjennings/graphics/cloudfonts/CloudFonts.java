package ca.cgjennings.graphics.cloudfonts;

/**
 * Provides a default cloud font collection.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class CloudFonts {
    private CloudFonts() {
    }

    private static CloudFontCollection defaultCollection;

    public static CloudFontCollection getDefaultCollection() {
        synchronized (CloudFonts.class) {
            if (defaultCollection == null) {
                defaultCollection = new DefaultCloudFontConnector().createFontCollection();
            }
            return defaultCollection;
        }
    }
}
