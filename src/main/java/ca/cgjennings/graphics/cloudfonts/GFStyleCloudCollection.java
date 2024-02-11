package ca.cgjennings.graphics.cloudfonts;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import ca.cgjennings.io.EscapedLineReader;
import ca.cgjennings.io.EscapedLineWriter;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.FileNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import resources.ResourceKit;

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
    private static final long MAX_CACHE_AGE = 30L * 24 * 60 * 60 * 1000;
    private static final String HASHES = "hashes";
    private static final String METADATA = "metadata";

    GFStyleCloudCollection(CloudFontConnector provider) {
        this.connector = provider;
        cacheRoot = provider.getLocalCacheRoot();

        try(var reader = new EscapedLineReader(new File(cacheRoot, HASHES))) {
            String[] hashkv;
            while ((hashkv = reader.readProperty()) != null) {
                hashes.put(hashkv[0], hashkv[1]);
            }
        } catch (FileNotFoundException fnf) {
            // never created, or intentionally deleted
        } catch (IOException ex) {
            StrangeEons.log.log(Level.WARNING, "could not read hashes", ex);
        }
    }

    /**
     * Write the list of hashes of locally cached files.
     */
    private void writeFileHashes() {
        synchronized (this) {
            try (var writer = new EscapedLineWriter(new File(cacheRoot, HASHES))) {
                StrangeEons.log.info("writing local cache file hashes");
                writer.writeProperties(hashes);
            } catch (IOException ex ) {
                StrangeEons.log.log(Level.WARNING, "could not write hashes", ex);
            }
        }
    }

    private final CloudFontConnector connector;
    private final File cacheRoot;
    /** Map from cloud paths to the hash of a locally downloaded file. */
    private final HashMap<String,String> hashes = new HashMap<>();
    private CloudFontFamily[] families;


    static class GoogleFont implements CloudFont, Comparable<GoogleFont> {
        protected GoogleFont(GoogleFontFamily family, String file) {
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

        private final GoogleFontFamily family;
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
                    StrangeEons.log.log(Level.INFO, "loading font {0}", fontFile);
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
                File dlFile = family.coll.fontPathToLocalCacheFile(getCloudPath(), family.getHash());
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
        public int compareTo(GoogleFont other) {
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

    static class GoogleFontFamily implements CloudFontFamily {
        protected GoogleFontFamily(GFStyleCloudCollection collection, String key, String fileList) {
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
        private GoogleFont[] fonts;

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
                fonts = new GoogleFont[files.length - 1];
                int ok = 0;
                for (int i = 0; i < files.length - 1; ++i) {
                    try {
                        fonts[ok] = new GoogleFont(this, files[i]);
                        ++ok;
                    } catch (Exception ex) {
                        StrangeEons.log.log(Level.WARNING, "could not parse " + key, ex);
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
            File[] files = coll.downloadFonts((GoogleFont[]) cf);
            Font[] awtFonts = new Font[cf.length];            
            for (int i=0; i<cf.length; ++i) {
                awtFonts[i] = ((GoogleFont) cf[i]).localFileToFont(files[i]);
            }
            return awtFonts;
        }
        
        @Override
        public ResourceKit.FontRegistrationResult[] register() throws IOException {
            Font[] awtFonts = getFonts();
            ResourceKit.FontRegistrationResult[] results = new ResourceKit.FontRegistrationResult[awtFonts.length];
            for (int i=0; i<awtFonts.length; ++i) {
                if (fonts[i].alreadyRegistered) {
                    results[i] = new ResourceKit.FontRegistrationResult(
                            awtFonts[i], this.fonts[i].alreadyRegistered
                    );
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

        public String getHash() {
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

    private synchronized CloudFontFamily[] getFamilies(boolean useCache) throws IOException {
        if (useCache && families != null) {
            return families.clone();
        }
        // if the metadata file doesn't exist or is old, download it
        File cache = new File(connector.getLocalCacheRoot(), METADATA);
        if (!useCache || !cache.exists() || cache.lastModified() < System.currentTimeMillis() - MAX_CACHE_AGE) {
            download(connector.getUrlForMetadata(), new File(cacheRoot, METADATA));
        }
        var familyList = new LinkedList<GoogleFontFamily>();
        try (EscapedLineReader props = new EscapedLineReader(cache)) {
            String[] prop;
            while ((prop = props.readProperty()) != null) {
                GoogleFontFamily family = new GoogleFontFamily(this, prop[0], prop[1]);
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
        name = name.replaceAll("[^a-z0-9]+", "");
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
    private File downloadFont(GoogleFont font) throws IOException {
        return downloadFonts(new GoogleFont[] { font })[0];
    }

    /**
     * Get files for a an array of cloud fonts, downloading as necessary.
     * 
     * @param fonts the fonts to make available locally
     * @return the files containing the fonts
     * @throws IOException if an error occurs while downloading or writing the fonts
     */
    private synchronized File[] downloadFonts(GoogleFont[] fonts) throws IOException {
        boolean updatedHashes = false;
        File[] localFiles = new File[fonts.length];
        for (int i=0; i<fonts.length; ++i) {
            final GoogleFont font = fonts[i];
            final String cloudPath = normalizeFontPath(font.getCloudPath());
            final File localFile = fontPathToLocalCacheFile(cloudPath, font.family.getHash());
            final String hash = font.family.getHash();

            localFiles[i] = localFile;

            // is already downloaded and up to date?
            String localHash = hashes.get(cloudPath);
            if (hash.equals(localHash) && localFile.exists()) {
                continue;
            }
    
            // nope, get it from the cloud
            download(connector.getUrlForFontPath(cloudPath), localFile);
            hashes.put(cloudPath, hash);
            updatedHashes = true;
        }

        if (updatedHashes) {
            writeFileHashes();
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
        String platformPath = fontPath.replace('/', File.separatorChar);

        // insert hash for versioning
        int lastDot = platformPath.lastIndexOf('.');
        if (lastDot >= 0) {
            platformPath = platformPath.substring(0, lastDot) + '-' + hash + platformPath.substring(lastDot);
        } else {
            platformPath += '-' + hash;
        }

        File fontFile = new File(cacheRoot, platformPath);
        File parent = fontFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        return fontFile;
    }
    
    private void download(URL source, File dest) throws IOException {
        try (InputStream in = source.openStream()) {
            StrangeEons.log.log(Level.INFO, "downloading {0}", dest.getName());
            ProjectUtilities.copyStream(in, dest);
        } catch (IOException e) {
            try {
                dest.delete();
            } catch (Throwable t) {
                StrangeEons.log.log(Level.WARNING, "unable to delete failed font download", t);
            }
            throw new IOException("Failed to download file: " + source, e);
        }
    }

    public void showCacheFolder() {
        if (Desktop.isDesktopSupported()) {
            Desktop d = Desktop.getDesktop();
            if (d.isSupported(Desktop.Action.OPEN)) {
                try {
                    d.open(cacheRoot);
                    return;
                } catch (Exception e) {
                    StrangeEons.log.log(Level.WARNING, "failed to open font cache", e);
                }
            }
        }
        StrangeEons.log.log(Level.WARNING, "not supported on platform");
    }

    public static void main(String[] args) {
        try {
            GFStyleCloudCollection gfc = (GFStyleCloudCollection) CloudFonts.getDefaultCollection();
            CloudFontFamily cff = gfc.getFamily("raleway");
            cff.register();
            System.out.println(cff);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
