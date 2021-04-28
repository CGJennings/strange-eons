package ca.cgjennings.apps.arkham.plugins.catalog;

import ca.cgjennings.apps.arkham.BusyDialog;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import ca.cgjennings.apps.arkham.plugins.PluginBundle;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import ca.cgjennings.io.ConnectionSupport;
import ca.cgjennings.io.CountingInputStream;
import ca.cgjennings.io.EscapedTextCodec;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import static resources.Language.string;
import resources.Settings;

/**
 * A collection of {@link Listing}s that describe plug-ins from a download
 * source. The source may be local or, more typically, located over a network.
 * The catalog can determine whether its listings match installed plug-ins, and
 * if so compare their versions to determine if the listing is newer. Listings
 * in the catalog can be "flagged" for installation, and flagged listings can be
 * downloaded and installed automatically.
 * <p>
 * Catalogs can be described using plain text files, and a catalog in this
 * format can be instantiated from a remote URL. On a server, a catalog and its
 * plug-ins would be located together in a single folder. The catalog would be
 * stored in this folder as
 * <tt>catalog.txt</tt>, and the listings in the catalog would point to the file
 * names of plug-in bundles stored in the folder. To create the catalog locally,
 * it would be constructed with the URL of the base folder. The constructor will
 * download the catalog, and the base URL will be used to download any installed
 * plug-ins.
 * <p>
 * Catalog listings may be marked as "hidden". In this case, they are placed at
 * the end of a catalog when it is loaded, after all regular listings. When
 * iterating over the catalog, if the catalog size is determined using
 * {@link #size()}, then you will not see hidden listings. To iterate over all
 * listings, including hidden listings, use {@link #trueSize()}. Hidden listings
 * are usually added to a catalog when it contains components that are required
 * by other components but not useful by themselves. They can also be used to
 * link to required components that are in other catalogs. For example, this
 * could be used to create a private catalog that contains a component that
 * requires a plug-in from another site:
 * <pre>
 * url = ./myplugin.seplugin
 * name = My Plug-in
 * requires = CATALOGUEID{thatid}
 * id = CATALOGUEID{thisid}
 *
 * hidden
 * url = http://anothersite.org/se/plugins/RequiredPlugin.seplugin
 * name = Required Plugin
 * id = CATALOGUEID{thatid}
 * </pre>
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class Catalog {

    private URL base;
    private String openingComments;
    private BitSet installFlags = new BitSet();
    private int hiddenListings = 0;

    /**
     * Creates a new, empty catalog with a <code>null</code> base URL. This can
     * be useful as a temporary placeholder in a UI while downloading a catalog,
     * or it can be used by subclasses.
     */
    public Catalog() {
    }

    /**
     * Creates a catalog from a collection of {@link Listing}s. This is normally
     * used to create a temporary local catalog in order to write a catalog
     * description file. The base URL is not required unless installation is
     * attempted, and may be <code>null</code>.
     */
    public Catalog(URL base, Collection<Listing> listings) {
        this.base = base;
        for (Listing li : listings) {
            add(li);
        }
    }

    /**
     * Loads a catalog from a base URL. The URL of the catalog will be the base
     * URL with "catalog.txt" appended.
     *
     * @param base the URL to load the catalog from
     * @throws IOException if an error occurs while reading the catalog
     * @throws NullPointerException if <code>base</code> is <code>null</code>
     */
    public Catalog(URL base) throws IOException {
        this(base, true, (JProgressBar) null);
    }

    /**
     * Loads a catalog from a base URL. The URL of the catalog will be the base
     * URL with "catalog.txt" appended. If <code>feedback</code> if
     * non-<code>null</code>, then it will be updated with progress information.
     *
     * @param base the URL to load the catalog from
     * @param allowCache if <code>true</code>, and if the URL identifies the
     * default catalog URL, then a cached version of the catalog may be returned
     * @param feedback an optional progress bar to send feedback to while
     * reading
     * @throws IOException if an error occurs while reading the catalog
     * @throws NullPointerException if <code>base</code> is <code>null</code>
     */
    public Catalog(URL base, boolean allowCache, JProgressBar feedback) throws IOException {
        if (base == null) {
            throw new NullPointerException("base");
        }

        // If the requested URL matches the primary URL, then we can potentially
        // use the cached local copy. If we don't use the cached copy, we can
        // create or update the cached copy.
        boolean isPrimaryURL = false;
        try {
            URL primaryURL = new URL(Settings.getShared().get("catalog-url-1"));
            if (primaryURL.equals(base)) {
                isPrimaryURL = true;
            }
        } catch (MalformedURLException e) {
        }

        // if cached version is allowed, and the base URL is the primary one
        // then the cache file is checked to see if it exists and is recent;
        // if so then we try using the cached version and return early;
        // if anything goes wrong, instead of returning early we'll fall
        // back on downloading the online version.
        if (isPrimaryURL && allowCache) {
            StrangeEons.log.fine("loading primary catalog from local cache");
            synchronized (CACHE_FILE) {
                if (CACHE_FILE.exists() && (new Date().getTime() - CACHE_FILE.lastModified()) < CACHE_EXPIRY_TIME) {
                    try {
                        this.base = base;
                        int size = (int) CACHE_FILE.length();
                        try (InputStream in = new FileInputStream(CACHE_FILE)) {
                            readCatalog(in, feedback, size);
                        }
                        return;
                    } catch (Throwable t) {
                        // if anything goes wrong, download the real thing
                        StrangeEons.log.log(Level.WARNING, "exception reading cached catalog", t);
                    }
                }
            }
        }

        // download the catalog
        this.base = base;
        StrangeEons.log.fine("downloading catalog from " + base);
        readCatalog(feedback);

        // if we downloaded the primary catalog, try to create the cache file
        if (isPrimaryURL) {
            synchronized (CACHE_FILE) {
                OutputStream out = null;
                try {
                    out = new FileOutputStream(CACHE_FILE);
                    write(out);
                } catch (Throwable t) {
                    // the cache file may be garbled, try very hard to delete it
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException ie) {
                        }
                    }
                    if (CACHE_FILE.exists() && !CACHE_FILE.delete()) {
                        CACHE_FILE.deleteOnExit();
                    }
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            StrangeEons.log.log(Level.WARNING, "close() exception", e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the locally cached catalog, if any. If there is no locally cached
     * catalog, returns <code>null</code>.
     *
     * @return the locally cached copy of the primary catalog, or
     * <code>null</code>
     */
    public static Catalog getLocalCopy() {
        synchronized (CACHE_FILE) {
            if (CACHE_FILE.exists()) {
                try {
                    Catalog c = new Catalog();
                    try (InputStream in = new FileInputStream(CACHE_FILE)) {
                        c.readCatalog(in, null, (int) CACHE_FILE.length());
                        c.base = new URL(Settings.getShared().get("catalog-url-1"));
                        return c;
                    }
                } catch (Throwable e) {
                    StrangeEons.log.log(Level.WARNING, null, e);
                }
            }
        }
        return null;
    }

    /**
     * File used to store a cached copy of the primary catalog.
     */
    private static final File CACHE_FILE = StrangeEons.getUserStorageFile("cat-cache");
    /**
     * How long a cached copy of the catalog is considered fresh, in
     * milliseconds.
     */
    private static final long CACHE_EXPIRY_TIME = 23L * 60L * 60L * 1_000L;

    /**
     * Returns the base URL used to resolve the location of plug-in bundles
     * listed in the catalog.
     *
     * @return the base URL to download plug-ins listed in the catalog from
     */
    public final URL getBaseURL() {
        return base;
    }

    /**
     * Subclasses may use this to set a custom base URL.
     *
     * @param url the new base URL
     */
    protected final void setBaseURL(URL url) {
        base = url;
    }

    /**
     * Returns the number of listings in the catalog, not counting any hidden
     * listings. The first hidden listing, if any, begins at the index equal to
     * this value.
     *
     * @return the number of non-hidden listings
     */
    public final int size() {
        return listings.size() - hiddenListings;
    }

    /**
     * Returns the number of listings in the catalog, including any hidden
     * listings. The number of hidden listings is always equal to
     * <code>trueSize() - size()</code>.
     *
     * @return the total number of listings, including hidden ones
     */
    public final int trueSize() {
        return listings.size();
    }

    /**
     * Returns the listing at index <code>n</code> in the catalog.
     *
     * @param n the index of the desired listing
     * @return the listing at the specified index
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public final Listing get(int n) {
        if (n < 0 || n >= listings.size()) {
            throw new IndexOutOfBoundsException("listing index: " + n);
        }
        return listings.get(n);
    }

    /**
     * An enumeration of the possible versioning states for a listing.
     */
    public enum VersioningState {
        /**
         * This plug-in bundle is not currently installed.
         */
        NOT_INSTALLED,
        /**
         * This plug-in bundle is installed and has the same or a newer date
         * than the listing.
         */
        UP_TO_DATE,
        /**
         * This plug-in bundle is installed, but the listing is for a newer
         * version. (The installed version is out of date.)
         */
        OUT_OF_DATE,
        /**
         * There is a plug-in bundle with the same file name installed, but the
         * installed bundle does not have catalogue information embedded. The
         * listing is probably an updated version of a legacy version of the
         * plug-in, but it may also simply use the same file name.
         */
        OUT_OF_DATE_LEGACY,
        /**
         * This plug-in bundle is installed, but its version is newer than the
         * one in the listing. This may indicate that the catalog was not
         * updated to match the plug-in.
         */
        INSTALLED_IS_NEWER,
        /**
         * This listing requires a newer version of the application. This takes
         * precedence over other states; if the listing is installed but
         * requires a newer version, then this is returned.
         */
        REQUIRES_APP_UPDATE
    }

    /**
     * Compares the listing at index <code>n</code> with the set of installed
     * plug-ins and returns a {@link VersioningState} value that describes the
     * relative version of the installed plug-in, if any.
     *
     * @param n the listing index to compare to installed plug-ins
     * @return a comparison of the version of the plug-in in the listing and the
     * installed version
     */
    public VersioningState getVersioningState(int n) {
        if (n < 0 || n >= listings.size()) {
            throw new IndexOutOfBoundsException("listing index: " + n);
        }

        Listing li = listings.get(n);

        // check the version information
        if (li.getApplicationCompatibility() == 1) {
            return VersioningState.REQUIRES_APP_UPDATE;
        }

        CatalogID listID = li.getCatalogID();
        // NOTE: listID should only be null when testing, as ID is removed
        //       from the list of required fields
        CatalogID installedID = listID == null ? null : BundleInstaller.getInstalledCatalogID(listID.getUUID());
        if (installedID != null && listID != null) {
            if (installedID.isOlderThan(listID)) {
                return VersioningState.OUT_OF_DATE;
            } else if (listID.isOlderThan(installedID)) {
                return VersioningState.INSTALLED_IS_NEWER;
            } else {
                return VersioningState.UP_TO_DATE;
            }
        } else {
            String name = li.get(Listing.URL);
            int slash = name.lastIndexOf('/');
            if (slash >= 0) {
                name = name.substring(slash + 1);
            }
            if (BundleInstaller.isUncatalogedBundleName(name)) {
                return VersioningState.OUT_OF_DATE_LEGACY;
            }
        }

        return VersioningState.NOT_INSTALLED;
    }

    /**
     * Mark the listing at index <code>n</code> for installation. All flagged
     * plug-ins are installed together when {@link #installFlaggedPlugins()} is
     * called.
     *
     * @param n the index of the listing to mark
     * @param install the new value for the listing's install flag
     */
    public final void setInstallFlag(int n, boolean install) {
        synchronized (Catalog.class) {
            if (n < 0 || n >= listings.size()) {
                throw new IndexOutOfBoundsException("listing index: " + n);
            }
            installFlags.set(n, install);
        }
    }

    /**
     * Returns <code>true</code> if the listing at the indicated index is
     * flagged for installation.
     *
     * @param n the index of the listing to test
     * @return <code>true</code> if the listing is marked for installation
     */
    public final boolean getInstallFlag(int n) {
        if (n < 0 || n >= listings.size()) {
            throw new IndexOutOfBoundsException("listing index: " + n);
        }
        return installFlags.get(n);
    }

    /**
     * Returns a count of the number of plug-ins that are currently flagged for
     * installation from this catalog.
     *
     * @return the number of plug-ins that are flagged for installation
     */
    public final int getInstallFlagCount() {
        return installFlags.cardinality();
    }

    /**
     * Determines the installation requirements set for the plug-ins that are
     * currently flagged for installation. The installation requirements set
     * includes not just the plug-ins that are flagged for download, but any
     * plug-ins that those plug-ins require (recursively). The requirements set
     * is returned as a bit set in which the indexes of each possible listing
     * are set to 0 if that listing would not be installed and 1 if it would.
     *
     * @return a set of the plug-ins that would be
     */
    public BitSet determineInstallationRequirements() {
        BitSet reqdFlags = (BitSet) installFlags.clone();
        int oldSize;
        HashSet<String> warned = new HashSet<>();
        do {
            oldSize = reqdFlags.cardinality();
            for (int i = 0; i < listings.size(); ++i) {
                if (!reqdFlags.get(i)) {
                    continue;
                }
                Listing li = listings.get(i);
                String requiredPlugins = li.get(Listing.REQUIRES);
                if (requiredPlugins == null) {
                    continue;
                }
                for (String token : requiredPlugins.split("\\s*,\\s*")) {
                    token = token.trim();
                    try {
                        UUID uuid;
                        CatalogID dummyID = CatalogID.extractCatalogID(token);
                        if (dummyID != null) {
                            uuid = dummyID.getUUID();
                        } else {
                            uuid = UUID.fromString(token);
                        }
                        int index = findListingByUUID(uuid);
                        if (index >= 0) {
                            CatalogID listID = get(index).getCatalogID();
                            CatalogID installedID = BundleInstaller.getInstalledCatalogID(uuid);
                            if (listID == null || installedID == null || installedID.isOlderThan(listID)) {
                                reqdFlags.set(index);
                            }
                        } else if (!warned.contains(token)) {
                            StrangeEons.log.log(Level.WARNING, "required plug-in not found in catalog: {0}", token);
                            warned.add(token);
                        }
                    } catch (IllegalArgumentException e) {
                        if (!warned.contains(token)) {
                            StrangeEons.log.log(Level.WARNING, "ignoring bad required plug-in entry: {0}", token);
                            warned.add(token);
                        }
                    }
                }
            }
            // as long as this is larger, we added at least one new plug-in:
            // it may also require additional plug-ins, so we repeat the loop again
        } while (reqdFlags.cardinality() > oldSize);
        return reqdFlags;
    }

    /**
     * Attempt to download and install all plug-ins that are currently flagged
     * for installation. If a listing includes a <tt>requires</tt> entry, other
     * plug-ins in the catalog that are named in the requires entry will be
     * automatically flagged if they are not already flagged and they are not
     * installed and up to date. This is recursive: if a required plug-in has
     * required plugins, they are also added (and so on). When this method
     * returns, all installation flags in this catalog will be cleared.
     *
     * @return <code>true</code> if a restart (of the application) is required
     * to complete installation
     */
    public boolean installFlaggedPlugins() {
        synchronized (Catalog.class) {
            if (installFlags.cardinality() == 0) {
                return false;
            }
            requiresRestart = 0;
            StrangeEons app = StrangeEons.getApplication();
            try {
                app.unloadPlugins();
                flagRequiredPlugins();

                cancelled = false;
                new BusyDialog(string("cat-busy2", 1, installFlags.cardinality()), this::installFlaggedPluginsImpl, (ActionEvent e) -> {
                    cancelled = true;
                });

                return (requiresRestart & BundleInstaller.INSTALL_FLAG_RESTART_REQUIRED) != 0;
            } finally {
                installFlags.clear();
                BundleInstaller.finishBundleInstallation(requiresRestart);
                app.loadPlugins();
                if (hooks != null) {
                    // by using a copy, hooks can safely uninstall themselves
                    LinkedList<Runnable> hooksCopy = new LinkedList<>(hooks);
                    for (Runnable r : hooksCopy) {
                        try {
                            r.run();
                        } catch (Throwable t) {
                            StrangeEons.log.log(Level.WARNING, "uncaught install hook exception", t);
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns <code>true</code> if a restart (of the application) would be
     * required after the currently selected plug-ins are installed. The answer
     * is based on the types of plug-ins flagged for installation, and the types
     * of any plug-ins they require.
     * <p>
     * If this method returns <code>false</code>, a restart may still be
     * required if one of the plug-in bundles could not be written and an
     * autoupdate file was generated.
     *
     * @return <code>true</code> if a restart should not be required
     */
    public boolean isRestartRequiredAfterInstall() {
        synchronized (Catalog.class) {
            BitSet original = (BitSet) installFlags.clone();
            try {
                flagRequiredPlugins();
                BitSet extendedFlags = installFlags;
                installFlags = original;

                for (int i = 0; i < listings.size(); ++i) {
                    if (!extendedFlags.get(i)) {
                        continue;
                    }

                    String url = listings.get(i).get(Listing.URL).trim();
                    if (url != null && !url.endsWith(BundleInstaller.PLUGIN_FILE_EXT) && !url.endsWith(BundleInstaller.LIBRARY_FILE_EXT)) {
                        return true;
                    }

                    if (getVersioningState(i) != VersioningState.NOT_INSTALLED) {
                        return true;
                    }
                }
                return false;
            } finally {
                installFlags = original;
            }
        }
    }

    private void installFlaggedPluginsImpl() {
        int current = 0;
        for (int i = 0; i < listings.size(); ++i) {
            if (cancelled) {
                break;
            }
            if (!installFlags.get(i)) {
                continue;
            }

            final MD5Checksum md5 = new MD5Checksum();

            BusyDialog.titleText(string("cat-busy2", ++current, installFlags.cardinality()));
            Listing li = listings.get(i);

            File dest = null;
            try {
                BusyDialog.statusText(string("cat-dl1", li.getName()));
                boolean redownload;
                do {
                    // download to a temp folder
                    md5.reset();
                    redownload = false;
                    dest = download(li, md5);

                    // if catalog gives an MD5, check it now
                    PluginBundle pb = new PluginBundle(dest);
                    if (!md5.matches(li.getChecksum())) {
                        ChecksumInterrogator ci = new ChecksumInterrogator();
                        try {
                            EventQueue.invokeAndWait(ci);
                        } catch (Exception e) {
                            StrangeEons.log.log(Level.SEVERE, null, e);
                        }
                        switch (ci.choice) {
                            case 0:
                                redownload = true;
                                break; // download again
                            case 1:
                                dest = null;
                                break;         // skip; no error message since no exception, but null pb means no install
                            case 2:
                                break;                    // force install; pb is untouched so it is treated like a normal download
                            default:
                                throw new AssertionError();
                        }
                    }
                    if (redownload || dest == null) {
                        // clean up the bad download
                        deleteTempFolder(dest);
                    }
                } while (redownload);
            } catch (IOException e) {
                ErrorDialog.displayError(string("cat-err-dl", li.getName()), e);
                deleteTempFolder(dest);
            } catch (Cancelled e) {
                deleteTempFolder(dest);
                break;
            }

            BusyDialog.maximumProgress(-1);

            if (dest != null) {
                try {
                    BusyDialog.statusText(string("cat-install", li.getName()));

                    // set restart flag if .autoupdate was created
                    requiresRestart |= BundleInstaller.installPluginBundle(dest);

                    // install was OK; if the listing names bundles that this bundle
                    // replaces, then uninstall those bundles
                    String replacedBundleList = li.get(Listing.REPLACES);
                    if (replacedBundleList != null) {
                        for (String replace : replacedBundleList.split("\\s*,\\s*")) {
                            CatalogID replacedID = CatalogID.extractCatalogID(replace);
                            if (replacedID != null) {
                                File bundleToDelete = BundleInstaller.getBundleFileForUUID(replacedID.getUUID());
                                if (bundleToDelete != null) {
                                    StrangeEons.log.log(Level.INFO, "bundle is replaced by new plug-in and will be deleted: {0}", bundleToDelete.getName());
                                    if (!bundleToDelete.delete()) {
                                        StrangeEons.getApplication().deleteOnStartup(bundleToDelete, true);
                                        requiresRestart |= BundleInstaller.INSTALL_FLAG_RESTART_REQUIRED;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    ErrorDialog.displayError(string("cat-err-install", dest.getName()), e);
                } finally {
                    deleteTempFolder(dest);
                }
            }
        }
    }

    /**
     * Returns just the final path segment from a URL (everything after the
     * final slash).
     *
     * @param value the URL value to extract a name from
     * @return the file name part of the URL
     */
    private static String extractFileNameFromCatalogURL(String value) {
        int slash = value.lastIndexOf('/');
        if (slash >= 0) {
            value = value.substring(slash + 1);
        }
        return value;
    }

    private volatile boolean cancelled;
    private int requiresRestart;

    private static class ChecksumInterrogator implements Runnable {

        private int choice = 1;

        @Override
        public void run() {
            String[] options = new String[]{
                string("cat-err-checksum-dl"),
                string("cat-err-checksum-skip"),
                string("cat-err-checksum-force")
            };
            choice = JOptionPane.showOptionDialog(
                    StrangeEons.getWindow(),
                    string("cat-err-checksum"),
                    string("cat-title"),
                    0, JOptionPane.WARNING_MESSAGE, null,
                    options, options[0]
            );
            if (choice < 0) {
                choice = 1;
            }
        }
    };

    private static File createTempFolder() {
        File t;
        int trials = 0;
        do {
            t = new File(System.getProperty("java.io.tmpdir"), TEMP_FOLDER_BASE_NAME + Long.toString(System.nanoTime(), Character.MAX_RADIX));
            if (t.exists()) {
                continue;
            }
            t.mkdir();
            if (t.isDirectory()) {
                return t;
            }
            Thread.yield();
        } while (trials++ < 200);
        return null;
    }

    /**
     * Deletes a folder created with {@link #createTempFolder()}. The parameter
     * can either be the folder itself or a file within the folder.
     *
     * @param tempFolder the temp folder to delete (or a direct child of that
     * folder)
     */
    private static void deleteTempFolder(File tempFolder) {
        // checking for the temp folder name prefix is a sanity check to prevent
        // accidental deletion of a non-temporary directory tree
        while (tempFolder != null && (!tempFolder.isDirectory() || !tempFolder.getName().startsWith(TEMP_FOLDER_BASE_NAME))) {
            tempFolder = tempFolder.getParentFile();
        }
        if (tempFolder == null) {
            return;
        }

        if (tempFolder.isDirectory()) {
            ProjectUtilities.deleteAll(tempFolder);
        }
    }

    private static final String TEMP_FOLDER_BASE_NAME = "SE-install-";

    /**
     * Download the file referred to by a catalog listing to a file in a
     * temporary folder.
     *
     * @param li the listing whose bundle should be downloaded
     * @return the file that the listing was downloaded to
     * @throws IOException
     */
    private File download(Listing li, MD5Checksum md5) throws IOException {
        String name = li.get(Listing.URL);
        name = extractFileNameFromCatalogURL(name);

        File parent = createTempFolder();
        parent.deleteOnExit();
        File tempFile = new File(parent, name);
        tempFile.deleteOnExit();

        boolean allOK = false;
        OutputStream out = null;
        InputStream in = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(tempFile));
            URLConnection conn = new URL(getBaseURL(), li.get(Listing.URL)).openConnection();
            long size = conn.getContentLength();

            String sizeStr;
            if (size > 0) {
                sizeStr = ProjectUtilities.formatByteSize(size);
                BusyDialog.maximumProgress(500);
            } else {
                sizeStr = "";
                BusyDialog.maximumProgress(-1);
            }

            in = new BufferedInputStream(conn.getInputStream(), 128 * 1_024);

            byte[] buff = new byte[64 * 1_024];

            long total = 0;
            int read;
            while ((read = in.read(buff)) >= 0) {
                if (cancelled) {
                    throw new Cancelled();
                }

                out.write(buff, 0, read);
                md5.update(buff, 0, read);

                total += read;
                String text;
                if (size > 0) {
                    text = string("cat-dl2", name, ProjectUtilities.formatByteSize(total), sizeStr);
                } else {
                    text = string("cat-dl1", name);
                }
                BusyDialog.statusText(text, PROGRESS_UPDATE_RATE_MS);
                if (size > 0) {
                    BusyDialog.currentProgress((int) Math.round((total / (double) size) * 500), PROGRESS_UPDATE_RATE_MS);
                }
            }
            allOK = true;
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (!allOK) {
                tempFile.delete();
            }
        }

        // unpack published bundles
        if (PluginBundlePublisher.CompressionMethod.forExtension(name) != null) {
            BusyDialog.statusText(string("cat-unpack"));

            File unpacked = PluginBundlePublisher.publishedBundleToPluginBundle(tempFile, null);
            unpacked.deleteOnExit();
            tempFile.delete();
            tempFile = unpacked;
        }

        return tempFile;
    }

    private static final int PROGRESS_UPDATE_RATE_MS = 250;

    /**
     * Scan the install list, adding any required plug-ins in this catalog to
     * the install list. This is recursive: plug-ins required by required
     * plug-ins (and so on) will all be added. That is, if the set of flagged
     * listings is {A} but A requires B and C while C requires D, then after
     * calling this the flagged listings will be {A,B,C,D}.
     */
    private void flagRequiredPlugins() {
        installFlags = determineInstallationRequirements();
    }

    /**
     * Returns the index of the listing in this catalog that has the requested
     * UUID. If no listing matches the UUID, returns -1.
     *
     * @param uuid the unique ID to match
     * @return the index of the plug-in with the requested UUID, or -1
     */
    public int findListingByUUID(UUID uuid) {
        if (uuid == null) {
            throw new NullPointerException("uuid");
        }
        for (int i = 0; i < listings.size(); ++i) {
            CatalogID id = listings.get(i).getCatalogID();
            if (id != null && id.getUUID().equals(uuid)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Reload the catalog from the source. This will clear all download flags.
     * If an exception occurs, the original catalog and settings are retained.
     *
     * @throws IOException if an I/O exception occurs while reading the catalog
     */
    public void reload() throws IOException {
        List<Listing> backup = listings;
        int hiddenBackup = hiddenListings;
        try {
            listings = new ArrayList<>(50);
            hiddenListings = 0;
            readCatalog(null);
            installFlags.clear();
        } catch (IOException e) {
            listings = backup;
            hiddenListings = hiddenBackup;
            throw e;
        }
    }

    private void readCatalog(JProgressBar feedback) throws IOException {
        URL catalogURL = new URL(base, "./catalog.txt");
        InputStream in = null;
        try {
            URLConnection conn = catalogURL.openConnection();
            // we handle catalog caching ourselves
            conn.setUseCaches(false);
            ConnectionSupport.enableCompression(conn);
            int size = conn.getContentLength();
            in = ConnectionSupport.openStream(conn, -1);
            readCatalog(in, feedback, size);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private void readCatalog(InputStream in, final JProgressBar feedback, final int size) throws IOException {
        CountingInputStream cis = new CountingInputStream(in);
        BufferedReader r = new BufferedReader(new InputStreamReader(cis, ProjectUtilities.ENC_UTF8));
        StringBuilder comments = new StringBuilder();
        boolean commentsEnded = false;

        if (size >= 0 && feedback != null) {
            Runnable setMax = () -> {
                feedback.setIndeterminate(false);
                feedback.setMaximum(size);
            };
            if (EventQueue.isDispatchThread()) {
                setMax.run();
            } else {
                EventQueue.invokeLater(setMax);
            }
        }

        StringBuilder block = new StringBuilder(256);
        String line;
        while ((line = r.readLine()) != null) {
            if (size >= 0) {
                readCatalog_feedback(feedback, (int) cis.getBytesRead());
            }
            line = line.trim();
            if (line.startsWith("#") || line.startsWith("!")) {
                if (!commentsEnded) {
                    if (line.equalsIgnoreCase("#lock")) {
                        throw new CatalogIsLockedException();
                    }
                    comments.append(line).append('\n');
                }
                continue;
            }
            commentsEnded = true;
            if (line.isEmpty()) {
                createBufferListing(block);
            } else {
                if (block.length() > 0) {
                    block.append('\n');
                }
                block.append(line);
            }
        }
        createBufferListing(block);

        if (comments.length() > 0) {
            openingComments = comments.toString();
        } else {
            openingComments = null;
        }
    }

    private void readCatalog_feedback(final JProgressBar feedback, final int current) {
        if (feedback == null) {
            return;
        }
        if (EventQueue.isDispatchThread()) {
            int val = Math.min(current, feedback.getMaximum());
            feedback.setValue(val);
        } else {
            EventQueue.invokeLater(() -> {
                int val = Math.min(current, feedback.getMaximum());
                feedback.setValue(val);
            });
        }
    }

    /* Called from readCatalog to build a listing from a buffer containing a
	 * the lines that describe a single entry. */
    private void createBufferListing(StringBuilder block) throws IOException {
        if (block.length() > 0) {
            Properties p = new Properties();
            p.load(new StringReader(block.toString()));
            Listing listing = new Listing(base, p);
            block.delete(0, block.length());
            add(listing);
        }
    }

    /**
     * Adds a listing to the catalog. If the version information for the listing
     * indicates that the plug-in is too old for this version of the
     * application, then the listing is not added.
     *
     * @param li the listing to add
     * @throws IllegalArgumentException if the listing has a catalog ID and
     * another listing in the catalog uses the same UUID
     */
    public void add(Listing li) {
        if (li.getApplicationCompatibility() == -1) {
            return;
        }

        CatalogID id = li.getCatalogID();
        if (id != null && findListingByUUID(id.getUUID()) >= 0) {
            throw new IllegalArgumentException("Two or more catalog entries use the same ID: " + id.getUUID().toString().toUpperCase(Locale.CANADA));
        }

        if (li.isHidden()) {
            listings.add(li);
            ++hiddenListings;
        } else {
            listings.add(listings.size() - hiddenListings, li);
        }

        // this lets the updater track the newest date it has seen
        if (id != null) {
            AutomaticUpdater.isNew(id);
        }
    }

    /**
     * Removes the listing whose catalog ID matches the provided UUID.
     *
     * @param uuid the UUID of the listing to remove
     * @return <code>true</code> if an entry was removed, <code>false</code> if
     * no entry matches the UUID
     */
    public boolean remove(UUID uuid) {
        int index = findListingByUUID(uuid);
        if (index < 0) {
            return false;
        }
        Listing li = listings.get(index);
        listings.remove(index);
        if (li.isHidden()) {
            --hiddenListings;
        }
        return true;
    }

    private List<Listing> listings = new ArrayList<>(50);

    @SuppressWarnings("serial")
    private static class Cancelled extends RuntimeException {
        public Cancelled() {
        }
    }

    /**
     * Write the catalog to a stream as a catalog description file.
     *
     * @param out the stream to write to
     * @throws IOException if an error occurs while writing the catalog
     */
    public void write(OutputStream out) throws IOException {
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out, ProjectUtilities.ENC_UTF8));

        if (openingComments != null) {
            w.write(EscapedTextCodec.escapeUnicode(openingComments));
            w.write('\n');
        }

        for (int i = 0; i < listings.size(); ++i) {
            if (i > 0) {
                w.write('\n');
            }
            listings.get(i).write(w);
        }

        w.flush();
    }

    /*
	public static void main( String[] args )  {
			try{
		File p = new File("C:/Users/Chris/Documents/Web Sites/personal/eons/plugins");
		Catalog c = new Catalog( p.toURL() );
		int count = 0;
		for( File f : p.listFiles() ) {
			String n = f.getName();
			if( n.endsWith( ".setheme") || n.endsWith( ".seplugin") || n.endsWith(".seext") || n.endsWith( ".selibrary") ) {
				++count;
				CatalogID id = CatalogID.getCatalogID( f );
				if( id == null ) {
					System.out.println( "Creating ID: " + n );
					id = new CatalogID();
					CatalogID.setCatalogID( f, id );
				} else {
					System.out.println( "Already has ID: " + n );
				}
				int i;
				for( i=0; i<c.size(); ++i ) {
					Listing li = c.getLocalized( i );
					String liName = li.getLocalized( Listing.URL );
					int slash = liName.lastIndexOf( '/' );
					if( slash >= 0 ) liName = liName.substring( slash+1 );
					if( liName.equals( n ) ) {
						System.out.println( "Found listing, setting ID: " + n );
						li.setCatalogID( id );
						li.set( Listing.SIZE, String.valueOf( f.length() ) );

						PluginBundle pb = new PluginBundle( f );
						li.setMessageDigest( pb.getMD5() );

						break;
					}
				}
				if( i == c.size() ) {
					System.out.println( "Warning: no listing for " + n );
				}
			} else {
				System.out.println( "Skipping " + n );
			}
		}
		System.out.println( "Writing temporary catalog" );
		OutputStream out = new FileOutputStream( new File( p, "_catalog.txt" ) );
		c.write( out );
		out.close();
		System.out.println( "Processed " + count );
			} catch( IOException e ) { e.printStackTrace(); }
  }
     */

//	/**
//	 * Creates a compressed version of a catalog file. Compressed catalogs
//	 * require less bandwidth to download than uncompressed catalogs,
//	 * but they cannot be edited as easily. They are a good choice for
//	 * sites that host large catalogues. Note that the default name
//	 * when downloading a catalogue is still "catalog.txt", even if it
//	 * is compressed.
//	 * <p>
//	 * A compressed catalog begins with the sequence of bytes matching
//	 * the ASCII characters "<tt>#+BZ</tt>". After this signature comes the
//	 * original catalog content, but as a compressed BZip2 stream.
//	 * The catalog is written in a normalized form, as with {@link #write(java.io.OutputStream)},
//	 * but with all comment lines removed.
//	 *
//	 * @param source the uncompressed catalog
//	 * @param dest the compressed catalog
//	 * @throws IOException
//	 * @throws IllegalArgumentException if the catalogue is already compressed
//	 */
//	public static void compress( File source, File dest ) throws IOException {
//		if( isCompressed( source ) ) throw new IllegalArgumentException( "catalog already compressed" );
//
//		InputStream in = null;
//		OutputStream out = null;
//		try {
//			in = new FileInputStream( source );
//			out = new FileOutputStream( dest );
//			Catalog c = new Catalog();
//			c.readCatalog( in, null, -1 );
//			c.openingComments = null;
//
//			for( int i=0; i<COMPRESSED_MAGIC.length; ++i ) {
//				out.write( COMPRESSED_MAGIC[i] );
//			}
//
//			out = new CBZip2OutputStream( out, 2 );
//			c.write( out );
//			out.flush();
//		} finally {
//			if( in != null ) in.close();
//			if( out != null ) out.close();
//		}
//	}
//	/**
//	 * Returns <code>true</code> if a file appears to contain a compressed
//	 * catalog.
//	 * @param f the potential catalog to test
//	 * @return <code>true</code> if the file has compressed catalog magic
//	 * @throws IOException
//	 */
//	public static boolean isCompressed( File f ) throws IOException {
//		InputStream in = null;
//		try {
//			in = new FileInputStream( f );
//			for( int i=0; i<COMPRESSED_MAGIC.length; ++i ) {
//				if( in.read() != COMPRESSED_MAGIC[i] ) {
//					return false;
//				}
//			}
//			return true;
//		} finally {
//			if( in != null ) in.close();
//		}
//	}
//
//	private static final int[] COMPRESSED_MAGIC = new int[] { '#', '+', 'B', 'Z' };
    /**
     * Adds a new post-installation hook. All current post-installation hooks
     * are called after one or more new bundles are installed by calling
     * {@link #installFlaggedPlugins()}. This can be used to detect a previously
     * missing plug-in bundle and change the behaviour of the application or
     * plug-in accordingly. For example, when the core spelling bundle is
     * missing, the application installs a hook that will activate spelling
     * checking if the bundle is later installed.
     *
     * @param h the hook function to call following plug-in bundle installation
     */
    public static void addPostInstallationHook(Runnable h) {
        synchronized (Catalog.class) {
            if (hooks == null) {
                hooks = new LinkedList<>();
            }
            StrangeEons.log.log(Level.INFO, "added post-installation hook \"{0}\"", h);
            hooks.remove(h);
            hooks.add(h);
        }
    }

    /**
     * Removes a previously installed post-installation hook. It is safe to call
     * this from <i>within</i> a hook (hooks can uninstall themselves).
     *
     * @param h the hook function to stop calling following plug-in bundle
     * installation
     */
    public static void removePostInstallationHook(Runnable h) {
        synchronized (Catalog.class) {
            if (hooks == null) {
                return;
            }
            hooks.remove(h);
            if (hooks.isEmpty()) {
                hooks = null;
            }
        }
    }

    private static List<Runnable> hooks = null;

    /**
     * This exception is thrown when you attempt to download a catalog that is
     * currently being updated. If the comment "#lock" is found at the top of a
     * catalog file as it is downloaded, the download stops and this exception
     * will be thrown.
     */
    public static class CatalogIsLockedException extends IOException {

        private static final long serialVersionUID = 0x1fa_11ca_ba51L;

        public CatalogIsLockedException() {
            super("The catalogue is being updated; try again later.");
        }
    }
}
