package ca.cgjennings.graphics.cloudfonts;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import ca.cgjennings.io.EscapedLineReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
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
final class GFCloudCollection implements CloudFontCollection {
    /** How often to check if the font collection has been updated. */
    private static final long MAX_CACHE_AGE = 7L * 24 * 60 * 60 * 1000;
    private static final String METADATA = "metadata";

    /** Creates a collection using the specified settings. */
    GFCloudCollection(CloudFontConnector connector) {
        this.connector = connector;
        cacheRoot = connector.getLocalCacheRoot();
    }

    private final CloudFontConnector connector;
    private final File cacheRoot;
    private GFFamily[] families;

    static final Axis[] EMPTY_AXIS_ARRAY = new Axis[0];
    static final String[] EMPTY_STRING_ARRAY = new String[0];

    /** Convert a family to a sort key  */
    static String toSortKey(String familyName) {
        return familyName.toLowerCase(Locale.ROOT).replaceAll("[^-_a-z0-9]", "");
    }

    /** Family collection is sorted on this to make looking up a family by name faster. */
    static final Comparator<GFFamily> sortKeyComparator = (GFFamily a, GFFamily b) -> {
        return a.sortKey.compareTo(b.sortKey);
    };


    @Override
    public CloudFontFamily[] getFamilies() throws IOException {
        synchronized (this) {
            if (families == null) {
                loadFamilies();
            }
            return families.clone();
        }
    }

    @Override
    public void refresh() throws IOException {
        synchronized (this) {
            final String localVersion = Settings.getUser().get(metadataVersionSettingKey());
            final String remoteVersion = getMetadataVersion();
            if (!Objects.equals(localVersion, remoteVersion)) {
                families = null;
                validateLocalMetadata(true);
                loadFamilies();
            }
        }
    }

    /**
     * Returns the version of the font repository on the server
     * by downloading the {@code version} file there, which contains only
     * this information.
     * A change in version indicates that the metadata file which describes
     * the available font families and their versions has been updated.
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
     * Returns a key used to store the version of the local repository.
     * Connectors with different identifiers will use different keys.
     */
    private String metadataVersionSettingKey() {
        return "cloudfont-metadata-version_id_" + connector.getIdentifier();
    }


    /**
     * If the local metadata does not exist or is old,
     * this method checks the server for a new version and updates it if necessary.
     * As long as a local cache exists, it is only compared to the server
     * if it is older than {@link #MAX_CACHE_AGE}, unless the force parameter is true.
     * 
     * @param forceServerCheck if true, then the version on the server is
     *   always checked against the local version
     * @returns the (possibly changed) version string for the local version
     * @throws IOException
     */
    private synchronized String validateLocalMetadata(boolean forceServerCheck) throws IOException {
        File localCache = new File(cacheRoot, METADATA);
        String localVersion = Settings.getUser().get(metadataVersionSettingKey());
        final boolean cacheIsStale = forceServerCheck || (localCache.lastModified() < System.currentTimeMillis() - MAX_CACHE_AGE);
        final boolean cacheExists = localCache.exists();
        if (localVersion == null || cacheIsStale || !cacheExists) {
            final String remoteVersion = getMetadataVersion();
            if (!remoteVersion.equals(localVersion) || !cacheExists) {
                updateLocalMetadataFromServer(remoteVersion, localCache);
                localVersion = remoteVersion;
            }
        }
        return localVersion;
    }
    
    private synchronized void updateLocalMetadataFromServer(String remoteVersion, File localCache) throws IOException {
        log.log(Level.INFO, "getting new font metadata version \"{0}\"", remoteVersion);
        // download to a temporary file first, in case of failure
        File temp = new File(cacheRoot, METADATA + ".tmp");
        download(connector.getUrlForMetadata(), temp);
        ProjectUtilities.moveFile(temp, localCache);
        Settings.getUser().set(metadataVersionSettingKey(), remoteVersion);
    }


    /**
     * Initializes the collection by parsing the metadata file that describes
     * the available font families. A local cache of the metadata may be used,
     * or if it is stale or unavailable, a copy will be downloaded from the server.
     * 
     * @throws IOException
     */
    private synchronized void loadFamilies() throws IOException {
        if (families != null) return;

        // in the event we get no results or the version doesn't match what we expect,
        // we'll try to refresh the cache and try one more time
        boolean retry = false;
        try {
            loadFamiliesImpl(true);
            if (families.length == 0) retry = true;
        } catch (IllegalStateException ex) {
            retry = true;
        }
        if (retry) {
            validateLocalMetadata(true);
            loadFamiliesImpl(false);
        }
    }    
    private synchronized void loadFamiliesImpl(boolean throwOnVersionMismatch) throws IOException {
        final String metadataVersion = validateLocalMetadata(false);
        final File metadataFile = new File(cacheRoot, METADATA);
        final WeakIntern intern = new WeakIntern();
        java.util.LinkedList<ca.cgjennings.graphics.cloudfonts.GFFamily> familyList = new LinkedList<GFFamily>();
        try (EscapedLineReader props = new EscapedLineReader(metadataFile)) {
            String[] prop;
            String name = null, path = null, fileList = null, catList = null,
                    axesList = null, subsetList = null, versionHash = null;
            while ((prop = props.readProperty()) != null) {
                try {
                    switch(prop[0]) {
                        case "path": path = prop[1]; break;
                        case "list": fileList = prop[1]; break;
                        case "name": name = prop[1]; break;
                        case "cats": catList = prop[1]; break;
                        case "sets": subsetList = prop[1]; break;
                        case "axes": axesList = prop[1]; break;
                        case "hash":
                            // the end of a family must always be marked by a hash
                            versionHash = prop[1];
                            GFFamily family = new GFFamily(this, name, path, fileList, catList, axesList, subsetList, versionHash, intern);
                            familyList.add(family);
                            name = path = fileList = catList = axesList = subsetList = versionHash = null;
                            break;
                        case "$version": 
                            if (!metadataVersion.equals(prop[1])) {
                                if (throwOnVersionMismatch) {
                                    throw new IllegalStateException();
                                }
                                log.log(Level.WARNING, "expected version {0}, but metadata is version {1}",
                                        new Object[] { metadataVersion, prop[1] });
                            }
                        default:
                            // ignore unknown properties, assume they are for a compatible future version
                            log.log(Level.INFO, "ignoring unknown metadata property: {0}", prop[0]);
                    }
                } catch (IllegalStateException ex) {
                    // indicates version mismatch
                    throw ex;
                } catch (Exception ex) {
                    log.log(
                        Level.WARNING,
                        "error parsing metadata near " + (familyList.isEmpty() ? "start" : familyList.getLast().getName()),
                        ex
                    );
                    if (!"hash".equals(prop[0])) {
                        // try to skip to the next family before resuming
                        String skipLine;
                        while((skipLine = props.readLine()) != null && !skipLine.trim().startsWith("hash"));
                    }
                }
            }
        }

        families = familyList.toArray(GFFamily[]::new);
        Arrays.sort(families, sortKeyComparator);
    }

    @Override
    public CloudFontFamily getFamily(String name) throws IOException {
        GFFamily[] searchFamilies;
        synchronized (this) {
            loadFamilies();
            searchFamilies = families;
        }

        int found = Arrays.binarySearch(searchFamilies, new GFFamily(toSortKey(name)), sortKeyComparator);
        if (found >= 0) {
            return searchFamilies[found];
        }

        // if not found, try returning the first tag-based match
        CloudFontFamily[] matches = match(name);
        if (matches.length > 0) {
            return matches[0];
        }

        return null;
    }

    @Override
    public CloudFontFamily[] match(String keywords) throws IOException {
        GFFamily[] searchFamilies;
        synchronized (this) {
            loadFamilies();
            searchFamilies = families;
        }

        String[] tokens = keywords.trim().split("\\s+");
        for (int i=0; i<tokens.length; ++i) {
            tokens[i] = toSortKey(tokens[i]);
        }

        List<CloudFontFamily> matches = new LinkedList<>();
        for (int f=0; f<searchFamilies.length; ++f) {            
            for (int t=0; t<tokens.length; ++t) {
                if (searchFamilies[f].sortKey.contains(tokens[t])) {
                    matches.add(searchFamilies[f]);
                    break;
                }
            }
        }

        return matches.toArray(CloudFontFamily[]::new);
    }

    /** Convenience utility to download a single font. */
    File downloadFont(GFFont font) throws IOException {
        return downloadFonts(new GFFont[] { font })[0];
    }

    /**
     * Downloads the given fonts as a group, returning the local files.
     * 
     * @param fonts the fonts to make available locally
     * @return the files containing the fonts
     * @throws IOException if an error occurs while downloading or writing the fonts
     */
    synchronized File[] downloadFonts(GFFont[] fonts) throws IOException {
        File[] localFiles = new File[fonts.length];
        List<Runnable> tasks = null;
        for (int i=0; i<fonts.length; ++i) {
            final GFFont font = fonts[i];
            final String cloudPath = normalizeFontPath(font.getCloudPath());
            final File dest = fontPathToLocalCacheFile(cloudPath, font.family.getVersionHash());
            localFiles[i] = dest;

            // is already downloaded?
            if (dest.exists()) continue;

            // nope, get it from the cloud
            if (tasks == null) tasks = new LinkedList<>();
            tasks.add(() -> {
                try {
                    dest.getParentFile().mkdirs();
                    download(connector.getUrlForFontPath(cloudPath), dest);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        if (tasks != null) {
            if (tasks.size() == 1) {
                try {
                    tasks.get(0).run();
                } catch (RuntimeException ex) {
                    throw new IOException("download failed", ex.getCause());
                }
            } else {
                final int numThreads = Settings.getUser().getInt("cloudfont-concurrent-downloads", 3);
                var executor = Executors.newFixedThreadPool(numThreads);
                try {
                    List<Future<?>> futures = new LinkedList<>();
                    for (Runnable task : tasks) {
                        futures.add(executor.submit(task));
                    }
                    for (Future<?> f : futures) {
                        f.get();
                    }
                } catch (CancellationException|InterruptedException ex) {
                    throw new AssertionError();
                } catch (ExecutionException ex) {
                    // expect an ExecutionException wrapping a RuntimeException wrapping an IOException
                    Throwable nested = ex.getCause();
                    if (nested instanceof RuntimeException && nested.getCause() instanceof IOException) {
                        throw (IOException) nested.getCause();
                    }
                    throw new IOException("download failed", nested);
                } finally {
                    executor.shutdown();
                    try {
                        executor.awaitTermination(12L, java.util.concurrent.TimeUnit.HOURS);
                    } catch (InterruptedException ex) {
                        // main thread interrupted
                        throw new AssertionError();
                    }
                }
            }
        }
        return localFiles;
    }


    /**
     * Normalizes a font path by adding a leading "./" if necessary.
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
     * Given a font path, returns the location where the font's cached
     * version will be found, if it exists.
     * 
     * @param fontPath the normalized font path
     * @return the local cache file
     */
    File fontPathToLocalCacheFile(String fontPath, String hash) {
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
    
    /**
     * Low-level utility to download a URL to a local file.
     * 
     * @param source the remote URL
     * @param dest the local file
     * @throws IOException
     */
    private static void download(URL source, File dest) throws IOException {
        try (InputStream in = source.openStream()) {
            InputStream decorated = in;
            if (source.getPath().endsWith(".gz")) {
                decorated = new java.util.zip.GZIPInputStream(in);
            }
            log.log(Level.INFO, "downloading {0}", dest.getName());
            // copy to a temporary file first, in case of failure
            File temp = new File(dest.getParentFile(), dest.getName() + ".tmp");            
            ProjectUtilities.copyStream(decorated, temp);
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
                } catch (IOException e) {
                    log.log(Level.WARNING, "failed to open font cache", e);
                }
            }
        }
        log.log(Level.WARNING, "not supported on platform");
    }

    public static void main(String[] args) {
        try {
            GFCloudCollection gfc = (GFCloudCollection) CloudFonts.getDefaultCollection();
            var f = gfc.getFamily("ubuntu");
            f.register();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static final Logger log = StrangeEons.log;
}
