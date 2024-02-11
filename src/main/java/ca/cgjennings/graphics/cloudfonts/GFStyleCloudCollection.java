package ca.cgjennings.graphics.cloudfonts;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import ca.cgjennings.io.EscapedLineReader;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import resources.ResourceKit;
import resources.Settings;

/**
 * A cloud font collection backed by an online repository of fonts
 * that stores its fonts using the same folder structure and
 * file name conventions as the Google Fonts repository.
 * 
 * <p>In addition to the fonts themselves, the collection needs to
 * be able to access:
 * <ol>
 *   <li> a metadata file describes the location and files that make
 *        up each font family, along with a version tag for each family;
 *   <li> a version file that contains a version tag for the metadata
 *        file, to detect when it has changed.
 * </ol>
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
final class GFStyleCloudCollection implements CloudFontCollection {
    /** How often to check if the font collection has been updated. */
    private static final long MAX_CACHE_AGE = 30L * 24 * 60 * 60 * 1000;
    private static final String METADATA = "metadata";

    /** Creates a collection using the specified settings. */
    GFStyleCloudCollection(CloudFontConnector connector) {
        this.connector = connector;
        cacheRoot = connector.getLocalCacheRoot();
    }

    private final CloudFontConnector connector;
    private final File cacheRoot;
    private CloudFontFamily[] families;

    static class GFFont implements CloudFont, Comparable<GFFont> {
        protected GFFont(GFFamily family, String file) {
            this.family = family;
            this.file = file;
            
            String parsedStyle = "";
            String[] parsedAxes = null;
            
            int oSquare = file.indexOf('[');
            if (oSquare >= 0) {
                int cSquare = file.indexOf(']', oSquare + 1);
                if (cSquare < 0) {
                    throw new AssertionError("] before [");
                }
                parsedAxes = file.substring(oSquare + 1, cSquare).split(",");
            }
            int styleEnd = oSquare < 0 ? file.lastIndexOf('.'): oSquare;
            if (styleEnd < 0) throw new AssertionError("no extension");
            
            int lastHyphen = file.lastIndexOf('-', styleEnd);
            if (lastHyphen >= 0) {
                parsedStyle = file.substring(lastHyphen + 1, styleEnd);
                if (parsedStyle.matches("(Black|Bold|Extra|Italic|Light|Medium|Regular|Semi|Thin)*")) {
                    name = file.substring(0, lastHyphen);
                } else {
                    parsedStyle = "";
                    name = file.substring(0, styleEnd);
                }
            } else {
                name = file.substring(0, styleEnd);
            }
            
            style = parsedStyle;
            axes = parsedAxes == null ? EMPTY_AXES : parsedAxes;
            
            sortKey = 2;
            if (parsedStyle.isEmpty()) {
                sortKey = 0;
            } else if (parsedStyle.equals("Regular")) {
                sortKey = 1;
            }
        }

        private final GFFamily family;
        private final String file;
        private final String name;
        private final String style;
        private final String[] axes;        
        private int sortKey;
        // file at time font was created, since it will be locked
        private File fontFile;
        private Font font;
        private boolean alreadyRegistered;

        @Override
        public CloudFontFamily getFamily() {
            return family;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getStyle() {
            return style;
        }

        @Override
        public String[] getAxes() {
            return axes.length == 0 ? axes : axes.clone();
        }

        String getCloudPath() {
            return family.key + '/' + file;
        }
        
        @Override
        public Font getFont() throws IOException {
            synchronized (family.coll) {
                if (font != null) return font;
                fontFile = family.coll.downloadFont(this);
                return localFileToFont(fontFile);
            }
        }

        private Font localFileToFont(File fontFile) throws IOException {
            synchronized (family.coll) {
                if (font != null) return font;
                try {
                    log.log(Level.INFO, "loading font {0}", fontFile);
                    font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                    return font;
                } catch (FontFormatException ffe) {
                    throw new IOException("invalid font file " + fontFile, ffe);
                }
            }
        }

        @Override
        public boolean isDownloaded() {
            try {
                File dlFile = family.coll.fontPathToLocalCacheFile(getCloudPath(), family.getVersionHash());
                return dlFile.exists();
            } catch (Exception ex) {
                return false;
            }
        }

        @Override
        public boolean isRegistered() {
            return alreadyRegistered;
        }        

        @Override
        public int compareTo(GFFont other) {
            if (this == other) {
                return 0;
            }
            int d = name.compareTo(other.name);
            if (d != 0) {
                return d;
            }            
            d = sortKey - other.sortKey;
            if (d != 0) {
                return d;
            }
            d = style.compareTo(other.style);
            if (d != 0) {
                return d;
            }
            return file.compareTo(other.file);
        }

        @Override
        public String toString() {
            String s = '"' + name + "\""
                    + (!style.isEmpty() ? ' ' + style : "");
            for (int i=0; i<axes.length; ++i) {
                s += " [" + axes[i] + ']';                
            }
            return s;
        }
    }
    
    private static final String[] EMPTY_AXES = new String[0];

    static class GFFamily implements CloudFontFamily {
        protected GFFamily(GFStyleCloudCollection collection, String key, String fileList) {
            this.coll = collection;
            this.key = Objects.requireNonNull(key);
            nameStart = key.indexOf('/') + 1;
            this.fileList = Objects.requireNonNull(fileList);
        }

        private final GFStyleCloudCollection coll;
        private String key;
        private final int nameStart;
        private String fileList;
        private String hash;
        private GFFont[] fonts;

        @Override
        public boolean matchesTag(String tag) {
            return key.indexOf(tag, nameStart) >= 0;
        }

        @Override
        public String getName() {
            if (fonts == null) getCloudFonts();
            return fonts[0].getName();
        }

        @Override
        public CloudFont[] getCloudFonts() {
            if (fonts == null) {
                String[] files = fileList.split(":");
                if (files.length < 2) {
                    throw new AssertionError("invalid font list: " + fileList);
                }
                hash = files[files.length - 1];
                fonts = new GFFont[files.length - 1];
                int ok = 0;
                for (int i = 0; i < files.length - 1; ++i) {
                    try {
                        fonts[ok] = new GFFont(this, files[i]);
                        ++ok;
                    } catch (Exception ex) {
                        log.log(Level.WARNING, "could not parse " + key, ex);
                    }
                }
                if (ok < fonts.length) fonts = Arrays.copyOf(fonts, ok);
                Arrays.sort(fonts);
                fileList = null;
            }
            return fonts.clone();
        }
        
        @Override
        public Font[] getFonts() throws IOException {
            CloudFont[] cf = getCloudFonts();
            File[] files = coll.downloadFonts((GFFont[]) cf);
            Font[] awtFonts = new Font[cf.length];            
            for (int i=0; i<cf.length; ++i) {
                awtFonts[i] = ((GFFont) cf[i]).localFileToFont(files[i]);
            }
            return awtFonts;
        }
        
        @Override
        public ResourceKit.FontRegistrationResult[] register() throws IOException {
            Font[] awtFonts = getFonts();
            ResourceKit.FontRegistrationResult[] results = new ResourceKit.FontRegistrationResult[awtFonts.length];
            for (int i=0; i<awtFonts.length; ++i) {
                if (fonts[i].alreadyRegistered) {
                    results[i] = new ResourceKit.FontRegistrationResult(awtFonts[i], true);
                } else {
                    results[i] = new ResourceKit.FontRegistrationResult(
                            awtFonts[i],
                            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(awtFonts[i])
                    );
                    fonts[i].alreadyRegistered = results[i].isRegistrationSuccessful();
                }
            }
            return results;
        }

        public String getVersionHash() {
            if (hash == null) {
                getCloudFonts();
            }
            return hash;
        }

        @Override
        public String getLicense() {
            switch (key.substring(0, nameStart - 1)) {
                case "apache":
                    return "Apache";
                case "ofl":
                    return "OFL";
                case "ufl":
                    return "UFL";
                default:
                    return "";
            }
        }
        
        @Override
        public String toString() {
            return getName().replaceAll("(\\p{Ll})(\\p{Lu})","$1 $2")
                        .replaceAll("(\\p{L})(\\p{Nd})","$1 $2");
        }
    }

    @Override
    public synchronized CloudFontFamily[] getFamilies() throws IOException {
        return getFamilies(true);
    }

    @Override
    public void refresh() throws IOException {
        getFamilies(false);
    }

    /**
     * Returns the version of the font repository on the server.
     * A change in version indicates that the metadata file has been updated.
     * (The metadata contains version hashes for each font family.)
     * 
     * @returns the version string, which is never null
     */
    private String getMetadataVersion() throws IOException {
        try (InputStream in = connector.getUrlForMetadataVersion().openStream()) {
            log.info("checking server for updated font metadata");
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), 16);
            final String version = reader.readLine();
            return version == null ? "" : version;
        }
    }

    /**
     * If the local metadata does not exist, or if the local cache is old,
     * this method checks the server for a new version and downloads it.
     * 
     * @returns the file that contains the metadata, whether or not it was updated
     * @throws IOException
     */
    private String updateLocalMetadata(boolean force) throws IOException {
        File cache = new File(cacheRoot, METADATA);
        final String versionKey = "cloudfont-metadata-version_id_" + connector.getIdentifier();
        String localVersion = Settings.getUser().get(versionKey);
        boolean cacheIsStale = force || (cache.lastModified() < System.currentTimeMillis() - MAX_CACHE_AGE);
        if (localVersion == null || cacheIsStale || !cache.exists()) {
            final String remoteVersion = getMetadataVersion();
            if (!remoteVersion.equals(localVersion)) {
                log.log(Level.INFO, "new metadata version \"{0}\"", remoteVersion);
                // download to a temporary file first, in case of failure
                File temp = new File(cacheRoot, METADATA + ".tmp");
                download(connector.getUrlForMetadata(), temp);
                ProjectUtilities.moveFile(temp, cache);
                Settings.getUser().set(versionKey, remoteVersion);
                localVersion = remoteVersion;
            }
        }
        return localVersion;
    }

    private synchronized CloudFontFamily[] getFamilies(boolean useCache) throws IOException {
        if (useCache && families != null) {
            return families.clone();
        }

        final String metadataVersion = updateLocalMetadata(!useCache);
        final File metadataFile = new File(cacheRoot, METADATA);
        var familyList = new LinkedList<GFFamily>();
        try (EscapedLineReader props = new EscapedLineReader(metadataFile)) {
            String[] prop;
            while ((prop = props.readProperty()) != null) {
                // metakeys start with $, currently only $version
                if (prop[0].startsWith("$")) {
                    switch (prop[0]) {
                        case "$version":
                            log.log(Level.INFO, "using font metadata version \"{0}\"", metadataVersion);
                            break;
                        default:
                            log.log(Level.WARNING, "ignoring unknown metadata property {0}", prop[0]);
                    }
                    continue;
                }
                GFFamily family = new GFFamily(this, prop[0], prop[1]);
                familyList.add(family);
            }
        }
        families = familyList.toArray(CloudFontFamily[]::new);
        return families.clone();
    }

    @Override
    public CloudFontFamily getFamily(String name) throws IOException {
        // filter out all chars but letters and numbers since we are matching against a path
        name = name.trim().toLowerCase(Locale.ROOT);
        name = name.replaceAll("[^-a-z0-9_ ]+", "");
        for (CloudFontFamily family : getFamilies(true)) {
            if (family.getName().equalsIgnoreCase(name)) {
                return family;
            }
        }
        return null;
    }

    @Override
    public CloudFontFamily matchFirst(String description) throws IOException {
        String[] tokens = description.trim().toLowerCase(Locale.ROOT).split("\\s+");
        for (int f=0; f<families.length; ++f) {
            int t=0;
            for (; t<tokens.length; ++t) {
                if (!families[f].matchesTag(tokens[t])) {
                    break;
                }
            }
            if (t == tokens.length) {
                return families[f];
            }
        }
        return null;
    }

    @Override
    public CloudFontFamily[] match(String description) throws IOException {
        List<CloudFontFamily> matches = new LinkedList<>();
        String[] tokens = description.trim().toLowerCase(Locale.ROOT).split("\\s+");
        CloudFontFamily[] families = getFamilies(true);
        for (int f=0; f<families.length; ++f) {
            int t=0;
            for (; t<tokens.length; ++t) {
                if (!families[f].matchesTag(tokens[t])) {
                    break;
                }
            }
            if (t == tokens.length) {
                matches.add(families[f]);
            }
        }
        return matches.toArray(CloudFontFamily[]::new);
    }

    /** Convenience method to download a single font. */
    private File downloadFont(GFFont font) throws IOException {
        return downloadFonts(new GFFont[] { font })[0];
    }

    /**
     * Downloads the given fonts as a group, returning the local files.
     * 
     * @param fonts the fonts to make available locally
     * @return the files containing the fonts
     * @throws IOException if an error occurs while downloading or writing the fonts
     */
    private synchronized File[] downloadFonts(GFFont[] fonts) throws IOException {
        File[] localFiles = new File[fonts.length];
        for (int i=0; i<fonts.length; ++i) {
            final GFFont font = fonts[i];
            final String cloudPath = normalizeFontPath(font.getCloudPath());
            final File dest = fontPathToLocalCacheFile(cloudPath, font.family.getVersionHash());
            localFiles[i] = dest;

            // is already downloaded?
            if (dest.exists()) continue;

            // nope, get it from the cloud
            dest.getParentFile().mkdirs();
            download(connector.getUrlForFontPath(cloudPath), dest);
        }

        return localFiles;
    }

    /**
     * Normalizes the font path by adding a leading "./" if necessary.
     * 
     * @param fontPath the font path
     * @return the normalized font path
     */
    private static String normalizeFontPath(String fontPath) {
        if (!fontPath.startsWith("./")) {
            if (fontPath.startsWith("/")) {
                fontPath = '.' + fontPath;
            } else {
                fontPath = "./" + fontPath;
            }
        }
        return fontPath;
    }

    /**
     * Given a font path, returns the location where the font would be cached.
     * Ensures that intermediate directories are created if necessary.
     * 
     * @param fontPath the *normalized* font path
     * @return the local file where the font would be cached
     */
    private File fontPathToLocalCacheFile(String fontPath, String hash) {
        // insert version hash in local file name
        String platformPath = fontPath.replace('/', File.separatorChar);
        int lastDot = platformPath.lastIndexOf('.');
        if (lastDot >= 0) {
            platformPath = platformPath.substring(0, lastDot) + '-' + hash + platformPath.substring(lastDot);
        } else {
            platformPath += '-' + hash;
        }

        File fontFile = new File(cacheRoot, ".cache" + File.separatorChar + platformPath);
        return fontFile;
    }
    
    private void download(URL source, File dest) throws IOException {
        try (InputStream in = source.openStream()) {
            log.log(Level.INFO, "downloading {0}", dest.getName());
            // copy to a temporary file first, in case of failure
            File temp = new File(dest.getParentFile(), dest.getName() + ".tmp");            
            ProjectUtilities.copyStream(in, temp);
            if (!temp.renameTo(dest)) {
                throw new IOException("failed to rename temporary file to " + dest);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "download \"{0}\" -> \"{1}\" failed", new Object[] { source, dest });
            throw e;
        }
    }

    /** Open the cache folder in explorer. */
    public void showCacheFolder() {
        if (Desktop.isDesktopSupported()) {
            Desktop d = Desktop.getDesktop();
            if (d.isSupported(Desktop.Action.OPEN)) {
                try {
                    d.open(cacheRoot);
                    return;
                } catch (Exception e) {
                    log.log(Level.WARNING, "failed to open font cache", e);
                }
            }
        }
        log.log(Level.WARNING, "not supported on platform");
    }

    public static void main(String[] args) {
        try {
            GFStyleCloudCollection gfc = (GFStyleCloudCollection) CloudFonts.getDefaultCollection();
            var f = gfc.getFamily("raleway");
            f.register();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static final Logger log = StrangeEons.log;
}
