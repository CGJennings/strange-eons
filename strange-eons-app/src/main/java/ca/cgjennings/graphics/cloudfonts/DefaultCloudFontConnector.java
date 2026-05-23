package ca.cgjennings.graphics.cloudfonts;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import resources.Settings;

/**
 * The connector used to implement {@link CloudFonts#getDefaultCollection()}.
 */
public class DefaultCloudFontConnector implements CloudFontConnector {
    private final String identifier;
    private final URL fontBaseUrl;
    private final URL metadataUrl;
    private final URL versionUrl;
    private final File cacheRoot;

    public DefaultCloudFontConnector() {
        this(
            "default",
            "cloud-font-cache",
            Settings.getShared().get("cloudfont-fontbase-url", "https://github.com/google/fonts/raw/main/"),
            Settings.getShared().get("cloudfont-metadata-url", "https://github.com/CGJennings/gf-metadata/raw/main/metadata.gz")
        );
    }

    public DefaultCloudFontConnector(String identifier, String cacheName, String fontBaseUrl, String metadataUrl) {
        try {
            this.identifier = Objects.requireNonNull(identifier);
            this.cacheRoot = StrangeEons.getUserStorageFile(cacheName + "/");
            this.fontBaseUrl = new URL(fontBaseUrl);
            this.metadataUrl = new URL(metadataUrl);
            versionUrl = new URL(this.metadataUrl, "version");
        } catch (MalformedURLException e) {
            throw new AssertionError("invalid font cache URL", e);
        }
    }

    @Override
    public String getIdentifier() {
        return identifier;
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
    public URL getUrlForMetadataVersion() {
        return versionUrl;
    }    

    @Override
    public File getLocalCacheRoot() {
        return cacheRoot;
    }

    @Override
    public final CloudFontCollection createFontCollection() {
        return new GFCloudFontCollection(this);
    }
}
