package ca.cgjennings.graphics.cloudfonts;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * The connector used to implement {@link CloudFonts#getDefaultCollection()}.
 */
public class DefaultCloudFontConnector implements CloudFontConnector {

    private final URL fontBaseUrl;
    private final URL metadataUrl;

    public DefaultCloudFontConnector() {
        try {
            fontBaseUrl = new URL("https://github.com/google/fonts/raw/main/");
            metadataUrl = new URL("https://github.com/CGJennings/gf-metadata/raw/main/metadata.properties");
        } catch (MalformedURLException e) {
            throw new AssertionError("invalid font cache URL", e);
        }
    }

    @Override
    public URL getUrlForFontPath(String fontPath) {
        try {
            return new URL(fontBaseUrl, fontPath);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid font path: " + fontPath);
        }
    }

    @Override
    public URL getUrlForMetadata() {
        return metadataUrl;
    }

    @Override
    public File getLocalCacheRoot() {
        File root = StrangeEons.getUserStorageFile("google-font-cache");
        root.mkdirs();
        return root;
    }

    @Override
    public final CloudFontCollection create() {
        return new GoogleFonts(this);
    }
}
