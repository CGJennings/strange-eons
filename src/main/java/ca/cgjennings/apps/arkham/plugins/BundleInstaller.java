package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.algo.ProgressListener;
import ca.cgjennings.apps.arkham.BusyDialog;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.plugins.catalog.CatalogID;
import ca.cgjennings.apps.arkham.plugins.catalog.PluginBundlePublisher;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import ca.cgjennings.jvm.JarLoader;
import ca.cgjennings.ui.JUtilities;
import ca.cgjennings.ui.theme.Theme;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import resources.*;
import static resources.Language.string;

/**
 * The bundle installer is responsible for discovering, installing, and
 * uninstalling plug-in bundles, as well as linking them to the application at
 * runtime. Plug-in bundles are JAR-like archives that contain the code and
 * resources required by a plug-in. The following kinds of bundles are
 * recognized; each type is identified by a different file extension:
 *
 * <table border=0>
 * <caption>Bundle types</caption>
 * <tr><th>Type     <th>Extension          <th>Description</tr>
 * <tr><td>Library  <td><tt>.selibrary</tt><td>Resources and code used by other
 * plug-ins</tr>
 * <tr><td>Theme    <td><tt>.setheme</tt>  <td>A user interface theme (compiled code
 * only)</tr>
 * <tr><td>Extension<td><tt>.seext</tt>    <td>Extension plug-ins that are loaded
 * during startup</tr>
 * <tr><td>Plug-in  <td><tt>.seplugin</tt> <td>Regular (activated or injected)
 * plug-ins</tr>
 * </table>
 *
 * <p>
 * <b>Note:</b> Some plug-in files may have an additional extension,
 * <tt>.pbz</tt>. These are plug-in bundles that have been <i>published</i> to
 * prepare them for inclusion in the plug-in catalog. They cannot be installed
 * as-is, but must first be unpacked into the standard format. See
 * {@link PluginBundlePublisher}.
 *
 * <p>
 * Plug-in loading happens primarily in three stages: discovery, linking, and
 * instantiation. During discovery, the plug-in folder is scanned for plug-in
 * bundle files of a particular type. After checking each bundle for validity,
 * and converting the bundle to {@code FORMAT_PLAIN} if required (see
 * {@link PluginBundle}), the bundle's {@link PluginRoot} is obtained and the
 * bundle is linked to the application. For library bundles, information about
 * the bundle is obtained from the root file and an {@link InstalledLibrary} is
 * created. For other bundles, information is obtained both from the root file
 * and from the theme or plug-in that the bundle contains. This is done by
 * instantiating the plug-in in information probe mode (see
 * {@link PluginContext#isInformationProbe}). Using this information, an
 * {@link InstalledPlugin} or {@link InstalledTheme} is created. (An
 * {@code InstalledPlugin} is not the plug-in itself, but it can be used to
 * create an instance of the plug-in.) Extensions are discovered, linked, and
 * instantiated all at once during application startup. Other plug-ins are
 * discovered and linked during start-up, but are started later (see
 * {@link ca.cgjennings.apps.arkham.StrangeEons#loadPlugins()}).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class BundleInstaller {

    private BundleInstaller() {
    }

    /**
     * The folder where plug-ins are installed.
     */
    public static final File PLUGIN_FOLDER = StrangeEons.getUserStorageFile("plug-ins");

    /**
     * A user settings key that points to an additional folder to search for
     * plug-ins.
     */
    public static final String USER_PLUGIN_FOLDER_KEY = "plugin-folder";

    /**
     * The file extension used by extension plug-in bundles (.seext)
     */
    public static final String EXTENSION_FILE_EXT = ".seext";
    /**
     * The file extension used by plug-in bundles (.seplugin)
     */
    public static final String PLUGIN_FILE_EXT = ".seplugin";
    /**
     * The file extension used by theme plug-in bundles (.setheme)
     */
    public static final String THEME_FILE_EXT = ".setheme";
    /**
     * The file extension used by library bundles (.selibrary)
     */
    public static final String LIBRARY_FILE_EXT = ".selibrary";
    /**
     * The file extension used to by a bundle that will update an existing
     * bundle at next application start (.autoupdate)
     */
    public static final String UPDATE_FILE_EXT = ".autoupdate";

    private static File[] testBundles = new File[0];

    /**
     * Sets one or more special bundles to be loaded as if they were in a
     * plug-in folder. This is normally set by starting the application with the
     * <tt>--plugintest</tt> option.
     *
     * @param bundles the non-null files containing the bundle to test
     */
    public static void setTestBundles(File[] bundles) {
        if (bundles == null) {
            throw new NullPointerException();
        }
        for (File bundle : bundles) {
            if (!bundle.exists() || bundle.isDirectory()) {
                throw new IllegalArgumentException(bundle.toString());
            }
        }
        testBundles = bundles;
    }

    /**
     * Returns a new array containing all of the registered test bundles, or an
     * empty array if there are no test bundles.
     *
     * @return files containing bundles to test
     */
    public static File[] getTestBundles() {
        return testBundles.clone();
    }

    /**
     * Returns true if and only if the app is running in plug-in test mode.
     */
    public static boolean hasTestBundles() {
        return testBundles.length > 0;
    }

    /**
     * Returns a file representing the location of the main application's
     * <tt>.selibrary</tt> file or base class directory. The location can be
     * decoded if the application is unpacked to the local file system or if it
     * is stored in a plain plug-in bundle. If not, then an
     * {@code AssertionError} will be thrown.
     *
     * @return the main application archive, or the root folder for the class
     * path
     */
    public static File getApplicationLibrary() {
        URL classUrl = BundleInstaller.class.getResource("BundleInstaller.class");
        String urlPath = classUrl.toString();
        File libFile = null;
        if (urlPath.startsWith("jar:")) {
            try {
                // this will have a URL to the file starting after the
                // jar: and going up to the ! (after which the path within the
                // jar is concatenated
                int excl = urlPath.indexOf('!');
                if (excl != -1) {
                    urlPath = urlPath.substring(4, excl);
                    libFile = new File(new URL(urlPath).toURI());
                }
            } catch (Exception e) {
            }
        } else {
            // if not a jar, then assume its a local file
            // this is certainly true, but on the chance it is not there will
            // be an exception anyway
            try {
                libFile = new File(classUrl.toURI());
                // the path to the class is .../ca/cgjennings/apps/arkham/plugins/impl/ThisClass.class
                for (int i = 0; i < 7; ++i) {
                    libFile = libFile.getParentFile();
                }
            } catch (Exception e) {
            }
        }

        if (libFile == null) {
            throw new AssertionError("Unable to decode app location from " + urlPath);
        }

        return libFile;
    }

    /**
     * Applies pending bundle updates by copying .autoupdate bundles over their
     * old versions. This is called once during application start, before any
     * bundles are installed.
     */
    public static void applyPendingBundleUpdates() {
        StrangeEons.log.info("applying plug-in bundle updates from .autoupdate files");
        applyBundleUpdatesImpl(PLUGIN_FOLDER);
    }

    private static void applyBundleUpdatesImpl(File folder) {
        if (!folder.exists()) {
            return;
        }
        for (File child : folder.listFiles()) {
            if (child.isDirectory()) {
                applyBundleUpdatesImpl(child);
            } else if (child.getName().endsWith(UPDATE_FILE_EXT)) {
                final String oldPath = child.getAbsolutePath();
                File oldVersion = new File(oldPath.substring(0, oldPath.length() - UPDATE_FILE_EXT.length()));
                if (oldVersion.exists()) {
                    oldVersion.delete();
                }

                try {
                    // rename or copy the .autoupdate file over the existing version
                    if (oldVersion.exists() || !child.renameTo(oldVersion)) {
                        ProjectUtilities.copyFile(child, oldVersion);
                        if (!child.delete()) {
                            child.deleteOnExit();
                            StrangeEons.log.log(Level.WARNING, "failed to delete .autoupdate file; will try again at exit: {0}", child);
                        }
                    }
                    PluginBundle pb = new PluginBundle(oldVersion);
                    if (pb.getPluginRoot() != null) {
                        // we can't run the installers yet because the bundle
                        // hasn't been loaded; instead we add any installers
                        // to a list and load them from loadPluginBundles()
                        if (pb.getPluginRoot().getInstallerIdentifier() != null) {
                            if (updatedBundlesWaitingToRunInstaller == null) {
                                updatedBundlesWaitingToRunInstaller = new HashSet<>();
                            }
                            updatedBundlesWaitingToRunInstaller.add(pb);
                            StrangeEons.log.log(Level.INFO, "deferring running installer for updated plug-in until bundle is loaded: {0}", child.getName());
                        }
                    }
                } catch (Throwable e) {
                    StrangeEons.log.log(Level.WARNING, "failed to install update {0}", child.getName());
                    ErrorDialog.displayError(string("plug-err-autoupdate", oldVersion.getName()), e);
                }
            }
        }
    }
    private static HashSet<PluginBundle> updatedBundlesWaitingToRunInstaller = null;

    /**
     * When we applied bundle updates, the bundles were not loaded yet so we
     * could not run their installers (if any). After each type of plug-in
     * bundle is loaded, we call this to run pending installers for just-loaded
     * bundles.
     */
    private static void runPendingInstallersFromBundleUpdates() {
        // if this is non-null, then we copied some .autoupdate files that
        // had installers, and we couldn't run the installers because the
        // bundles were not loaded yet---
        if (updatedBundlesWaitingToRunInstaller != null) {
            Iterator<PluginBundle> it = updatedBundlesWaitingToRunInstaller.iterator();
            while (it.hasNext()) {
                PluginBundle pending = it.next();
                PluginBundle loaded = discoveredBundles.get(pending.getFile());
                if (loaded != null) {
                    try {
                        it.remove(); // remove even if we fail, so we don't keep trying
                        PluginRoot root = pending.getPluginRoot();
                        if (root != null) {
                            runInstallerScript(root, false);
                            StrangeEons.log.log(Level.INFO, "ran deferred installer for updated bundle: {0}", pending.getFile().getName());
                        }
                    } catch (IOException e) {
                        StrangeEons.log.log(Level.WARNING, "exception while running deferred installer: {0}", pending.getFile().getName());
                    }
                }
            }
            if (updatedBundlesWaitingToRunInstaller.isEmpty()) {
                updatedBundlesWaitingToRunInstaller = null;
            }
        }
    }

    /**
     * Searches for plug-in bundles ({@code .seplugin} files) in the plug-in
     * folders. Newly discovered plug-ins are linked to the application and
     * added to a
     * {@linkplain #getInstalledPlugins() list of installed plug-ins}, but they
     * are not started immediately. This is because regular plug-ins can be
     * reloaded while the application is running, a process which is
     * {@linkplain StrangeEons#loadPlugins() coordinated by the application}.
     *
     * @see #getInstalledPlugins()
     * @see StrangeEons#loadPlugins()
     * @see StrangeEons#unloadPlugins()
     */
    public synchronized static void loadPluginBundles() {
        JUtilities.threadAssert();

        // the first time this is called, we need to add the system plug-ins
        if (installedPlugins.isEmpty()) {
            try {
                installedPlugins.add(new InstalledPlugin(null, QuickscriptPlugin.class.getName()));
            } catch (IOException ex) {
                throw new AssertionError("unable to create system plug-in");
            }
        }

        Set<InstalledPlugin> newPlugins = new TreeSet<>();
        scanForPlugins(newPlugins, PluginBundle.TYPE_PLUGIN);
        runPendingInstallersFromBundleUpdates();

        for (InstalledPlugin p : newPlugins) {
            installedPlugins.add(p);
        }
    }

    /**
     * Keeps track of all of the .seplugin plug-ins we have discovered so far.
     */
    private static final Set<InstalledPlugin> installedPlugins = new HashSet<>();

    /**
     * Returns an array of the installed plug-ins (from .seplugin bundles and
     * system plug-ins).
     *
     * @return a copy of the installed (not necessarily running)
     * {@link Plugin#ACTIVATED} and {@link Plugin#INJECTED} plug-ins
     */
    public synchronized static InstalledPlugin[] getInstalledPlugins() {
        InstalledPlugin[] plugins = installedPlugins.toArray(InstalledPlugin[]::new);
        Arrays.sort(plugins);
        return plugins;
    }

    /**
     * Searches for library bundles ({@code .selibrary} files) in the plug-in
     * folders, and attempts to link the application to any libraries that it
     * finds. This method is not normally called by user code. To install a
     * library bundle, call {@link #installPluginBundle(java.io.File)}.
     *
     * @see #getInstalledLibraries()
     */
    public synchronized static void loadLibraryBundles() {
        // Note: since no code gets run in the libraries, the priority
        // has no practical effect other than the order that the objects
        // are listed for the user.
        init();
        scanForPlugins(null, PluginBundle.TYPE_LIBRARY);
        runPendingInstallersFromBundleUpdates();
    }

    /**
     * Returns an array of the available {@link InstalledLibrary} objects
     * representing libraries loaded by the plug-in system.
     *
     * @return an array of discovered libraries, sorted by name
     */
    public synchronized static InstalledLibrary[] getInstalledLibraries() {
        InstalledLibrary[] libs = installedLibraries.toArray(InstalledLibrary[]::new);
        Arrays.sort(libs);
        return libs;
    }
    private static final TreeSet<InstalledLibrary> installedLibraries = new TreeSet<>();

    /**
     * Searches for theme bundles ({@code .setheme} files) in the plug-in
     * folders, installing any themes that it finds.
     *
     * @see #getInstalledThemes()
     */
    public synchronized static void loadThemeBundles() {
        StrangeEons.log.info("loading themes");

        if (installedThemes == null) {
            installedThemes = new TreeSet<>();

            // Only add built-in themes on the first scan
            try {
                installedThemes.add(new InstalledTheme(null, ca.cgjennings.ui.theme.HydraTheme.class.getName()));
                installedThemes.add(new InstalledTheme(null, ca.cgjennings.ui.theme.DagonTheme.class.getName()));
                installedThemes.add(new InstalledTheme(null, ca.cgjennings.ui.theme.UltharTheme.class.getName()));
                installedThemes.add(new InstalledTheme(null, ca.cgjennings.ui.theme.DreamlandsTheme.class.getName()));
            } catch (Exception e) {
                StrangeEons.log.log(Level.SEVERE, "standard themes not available", e);
            }
            //  - add system L&F theme
            try {
                installedThemes.add(new InstalledTheme(null, ca.cgjennings.ui.theme.TchoTchoTheme.class.getName()));
            } catch (Exception e) {
                StrangeEons.log.log(Level.SEVERE, "native theme not available", e);
            }
        }

        // Discover any theme bundles added since the last call, and add them
        //   to the collection of themes we know about.
        TreeSet<InstalledTheme> themes = new TreeSet<>();
        scanForPlugins(themes, PluginBundle.TYPE_THEME);
        runPendingInstallersFromBundleUpdates();
        for (InstalledTheme themePlugin : themes) {
            installedThemes.add(themePlugin);
        }
    }

    /**
     * Returns an array of the available {@link InstalledTheme}s.
     *
     * @return an array of discovered themes, sorted by
     * {@linkplain Theme#getThemeName() theme name}
     */
    public synchronized static InstalledTheme[] getInstalledThemes() {
        if (installedThemes == null) {
            throw new IllegalStateException("themes have not been installed yet");
        }
        InstalledTheme[] themes = installedThemes.toArray(InstalledTheme[]::new);
        Arrays.sort(themes);
        return themes;
    }

    /**
     * Returns the {@link InstalledTheme} whose {@link Theme} class has the name
     * className, or {@code null} if no such theme is available.
     *
     * @param className the class name of the theme to search for
     * @return the {@link InstalledTheme} with the class name, or {@code null}
     */
    public synchronized static InstalledTheme getInstalledThemeForClassName(String className) {
        if (className == null) {
            throw new NullPointerException("className");
        }
        if (installedThemes == null) {
            throw new IllegalStateException("themes have not been installed yet");
        }
        for (InstalledTheme t : installedThemes) {
            if (className.equals(t.getThemeClass())) {
                return t;
            }
        }
        return null;
    }

    // the collection of available themes
    private static Set<InstalledTheme> installedThemes;

    /**
     * Searches for extension bundles ({@code .seext} files) in the plug-in
     * folders and attempts to start extension plug-ins that it finds. This is
     * called during application startup. It should never be called by user
     * code.
     *
     * @param pl if non-{@code null}, this listener will be called periodically
     * with updates on installation progress
     * @see #unloadExtensions()
     * @see #getInstalledExtensions()
     */
    public synchronized static void loadExtensionBundles(ProgressListener pl) {
        JUtilities.threadAssert();
        StrangeEons.log.info("loading extensions");

        if (pl != null) {
            pl.progressUpdate(string("init-plugins"), 0f);
        }
        TreeSet<InstalledExtension> extensions = new TreeSet<>();
        scanForPlugins(extensions, PluginBundle.TYPE_EXTENSION);
        runPendingInstallersFromBundleUpdates();

        int extNum = 0;
        for (final InstalledExtension ext : extensions) {
            ++extNum;
            if (installedExtensions.contains(ext)) {
                continue;
            }

            if (pl != null) {
                String name;
                name = ext.getPluginRoot().getLocalizedClientProperty(PluginRoot.CLIENT_KEY_NAME);
                if (name == null) {
                    name = ext.getPluginRoot().getLocalizedClientProperty(PluginRoot.CLIENT_KEY_CATALOG_NAME);
                    if (name == null) {
                        name = ext.getBundle().getFile().getName();
                    }
                }
                int dot = name.lastIndexOf('.');
                if (dot >= 0) {
                    name = name.substring(0, dot);
                }
                pl.progressUpdate(name + "...", extNum / (float) extensions.size());
            }

            try {
                StrangeEons.log.log(Level.INFO, "starting extension {0} [priority={1}]", new Object[]{ext.getPluginClass(), ext.getPriority()});
                Plugin p = ext.startPlugin();
                if (p.getPluginType() != Plugin.EXTENSION) {
                    ErrorDialog.displayError(string("rk-err-ext-notanext", ext.getPluginClass()), null);
                } else {
                    installedExtensions.add(ext);
                    continue; // so not marked as failed
                }
            } catch (Throwable e) {
                if (e instanceof InvocationTargetException) {
                    e = e.getCause();
                }
                ErrorDialog.displayError(string("rk-err-plugin-init", ext.getPluginClass()), e);
            }

            // if the continue clause wasn't hit, there was an exception or
            // this plug-in isn't an extension
            failUUID(ext.getCatalogID());
        }
    }

    /**
     * Returns an immutable set containing the UUIDs of bundles that failed to
     * start.
     *
     * @return a (possibly empty) set of UUIDs for plug-ins that failed to start
     */
    public static synchronized Set<UUID> getFailedUUIDs() {
        if (failedUUIDs == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(failedUUIDs);
    }

    private static HashSet<UUID> failedUUIDs;

    /**
     * Adds the id to the list of failed UUIDs (does nothing if the id is
     * {@code null}).
     *
     * @param id the id to add
     * @see #getFailedUUIDs()
     */
    static void failUUID(CatalogID id) {
        if (id != null) {
            synchronized (BundleInstaller.class) {
                if (failedUUIDs == null) {
                    failedUUIDs = new HashSet<>(4);
                }
                failedUUIDs.add(id.getUUID());
            }
        }
    }

    /**
     * Returns an array of the extension plug-ins that were loaded by
     * {@link #loadExtensionBundles}. This should only be used for informational
     * purposes, e.g., querying for name, description, and version.
     *
     * @return an array of the installed extension plug-ins
     */
    public synchronized static InstalledExtension[] getInstalledExtensions() {
        InstalledExtension[] exts = installedExtensions.toArray(InstalledExtension[]::new);
        Arrays.sort(exts);
        return exts;
    }

    /**
     * Shuts down installed extensions. This method is called during application
     * shutdown. It should never be called by user code.
     */
    public synchronized static void unloadExtensions() {
        for (InstalledExtension p : installedExtensions) {
            StrangeEons.log.log(Level.INFO, "stopping extension {0}", p.getName());
            try {
                p.stopPlugin();
            } catch (Throwable t) {
                StrangeEons.log.log(Level.WARNING, "uncaught exception while stopping extension", t);
            }
        }
    }

    private static final List<InstalledExtension> installedExtensions = new LinkedList<>();

    /**
     * Returns the installed libraries, themes, plug-ins, etc. with the given
     * UUID (possibly empty). Typically, there is at most one installed bundle
     * object with a given UUID, but technically a plug-in bundle can contain
     * multiple plug-ins.
     *
     * @param uuid the UUID to search for
     * @return an array of the installed objects that were loaded from the
     * bundle with the specified URL (possibly empty)
     * @see CatalogID#getUUID()
     */
    public synchronized static InstalledBundleObject[] getInstalledBundleObjectsForUUID(UUID uuid) {
        File file = getBundleFileForUUID(uuid);
        if (file == null) {
            return EMPTY_BUNDLE_OBJECT_ARRAY;
        }

        LinkedList<InstalledBundleObject> objs = new LinkedList<>();
        switch (PluginBundle.getBundleType(file)) {
            case PluginBundle.TYPE_LIBRARY:
                for (InstalledLibrary x : installedLibraries) {
                    CatalogID xid = x.getCatalogID();
                    if (xid != null && xid.getUUID().equals(uuid)) {
                        objs.add(x);
                        break;
                    }
                }
                break;
            case PluginBundle.TYPE_THEME:
                for (InstalledTheme x : installedThemes) {
                    CatalogID xid = x.getCatalogID();
                    if (xid != null && xid.getUUID().equals(uuid)) {
                        objs.add(x);
                    }
                }
                break;
            case PluginBundle.TYPE_EXTENSION:
                for (InstalledExtension x : installedExtensions) {
                    CatalogID xid = x.getCatalogID();
                    if (xid != null && xid.getUUID().equals(uuid)) {
                        objs.add(x);
                    }
                }
                break;
            case PluginBundle.TYPE_PLUGIN:
                for (InstalledPlugin x : installedPlugins) {
                    CatalogID xid = x.getCatalogID();
                    if (xid != null && xid.getUUID().equals(uuid)) {
                        objs.add(x);
                    }
                }
                break;
            default:
                throw new AssertionError("unknown bundle type " + file);
        }
        InstalledBundleObject[] retval = objs.toArray(InstalledBundleObject[]::new);
        Arrays.sort(retval);
        return retval;
    }
    private static final InstalledBundleObject[] EMPTY_BUNDLE_OBJECT_ARRAY = new InstalledBundleObject[0];

    /**
     * Search the plug-in folders for files with names that end in
     * {@code extension}. For each such file, any plug-ins it contains are
     * enumerated and added to {@code pluginSet}. If it contains at least one
     * plug-in, or if {@code pluginSet} is {@code null}, then the file is added
     * to the class path.
     *
     * @param pluginSet a set that discovered plug-ins will be added to (may be
     * {@code null} for libraries)
     * @param pluginType the type of plug-in to scan for (see
     * {@link PluginBundle})
     */
    private static void scanForPlugins(Set<? extends InstalledBundleObject> pluginSet, int pluginType) {
        // try adding the test plug-in bundle, if it exists
        for (File tb : testBundles) {
            scanBundle(pluginSet, tb, pluginType);
        }

        if (disablePluginLoading) {
            return;
        }

        // scan the folder pointed to by the extra folder setting
        String folder = RawSettings.getUserSetting(USER_PLUGIN_FOLDER_KEY);
        if (folder != null) {
            scanFolder(pluginSet, new File(folder), pluginType);
        }

        // scan the user's plug-in folder
        File homeDirFolder = PLUGIN_FOLDER;
        if (homeDirFolder.isDirectory()) {
            scanFolder(pluginSet, homeDirFolder, pluginType);
        }

        // scan the /plug-ins folder in the same directory as the jar file
        File pluginFolder = getApplicationLibrary();
        // if not run from a jar, will already be a base classpath directory
        while (!pluginFolder.isDirectory()) {
            pluginFolder = pluginFolder.getParentFile();
        }
        pluginFolder = new File(pluginFolder, "plug-ins");
        if (pluginFolder.isDirectory()) {
            scanFolder(pluginSet, pluginFolder, pluginType);
        }
    }

    private static boolean disablePluginLoading = false;

    /**
     * Calling this method will prevent plug-ins bundles from being loaded,
     * except for any test bundles specified on the command line.
     */
    public static void disablePluginLoading() {
        disablePluginLoading = true;
    }

    /**
     * Called by {@link #scanForPlugins}. Search a directory tree for bundles
     * that have not been previously discovered. When an undiscovered bundle is
     * found:
     * <ol>
     * <li>It will be converted to the "plain" bundle format by decompressing
     * the bundle, if required.
     * <li>Its root file will be parsed, and if a bundle with its UUID is not
     * already installed then:
     * <li>{@link InstalledPlugin}s will be created for each plug-in listed in
     * the bundle, and added to pluginSet if it is not {@code null}. (No actual
     * plug-ins are instantiated, however.)
     * <li>The bundle will be linked to the application (added to the class
     * path).
     * </ol>
     *
     * @param pluginSet the set to add new plug-ins to; this should be a sorted
     * set so that plug-ins can be started in priority order
     * @param folder the parent directory to search
     * @param pluginType the type of plug-in to scan for (see
     * {@link PluginBundle})
     * @throws IllegalArgumentException if folder exists but is not a directory
     */
    private static void scanFolder(Set<? extends InstalledBundleObject> pluginSet, File folder, int pluginType) {
        if (folder == null) {
            throw new NullPointerException("folder");
        }
        if (pluginType <= PluginBundle.TYPE_UNKNOWN || pluginType > PluginBundle.TYPE_PLUGIN) {
            throw new IllegalArgumentException("invalid plug-in type: " + pluginType);
        }

        StrangeEons.log.log(Level.FINE, "discovering bundles in folder: {0}", folder);
        if (!folder.exists()) {
            return;
        }

        if (folder.isDirectory()) {
            for (File file : folder.listFiles()) {
                if (file.isDirectory()) {
                    scanFolder(pluginSet, file, pluginType);
                } else {
                    scanBundle(pluginSet, file, pluginType);
                }
            }
        } else {
            throw new IllegalArgumentException("not a folder: " + folder);
        }
    }

    /**
     * Called by {@link #scanFolder} (and by {@link #scanForPlugins} to load the
     * test bundle). Analyzes a plug-in bundle file. If the file's extension
     * matches the extension we are searching for, it exists, it is not a
     * directory, and it is not on the list of deprecated bundles, then
     * {@link InstalledPlugin}s will be created and bundle linking will occur
     * (see {@link #scanFolder}).
     *
     * @param pluginSet the set to add discovered plug-ins to
     * @param bundleFile the bundle file to scan
     * @param pluginType the type of bundle being scanned for
     */
    @SuppressWarnings("unchecked")
    private static void scanBundle(Set<? extends InstalledBundleObject> pluginSet, File bundleFile, int pluginType) {
        // stop if the bundle is not a file with the correct extension
        if (bundleFile == null || !bundleFile.exists() || bundleFile.isDirectory()) {
            return;
        }
        if (PluginBundle.getBundleType(bundleFile) != pluginType) {
            return;
        }

        // stop if we already scanned this on a previous pass
        if (discoveredBundles.containsKey(bundleFile)) {
            return;
        }

        // stop if the bundle has been marked deprecated
        final String name = bundleFile.getName();
        for (int i = 0; i < deprecatedBundles.length; ++i) {
            if (deprecatedBundles[i].equalsIgnoreCase(name)) {
                StrangeEons.log.log(Level.INFO, "skipping deprecated bundle: {0}", name);
                return;
            }
        }

        // at this point we will try to load the bundle
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            dot = name.length();
        }
        if (BusyDialog.getCurrentDialog() != null) {
            String message = string("init-plugins2", name.substring(0, dot));
            BusyDialog.statusText(message);
        }

        try {
            // create a plugin bundle for the file and ensure it is FORMAT_PLAIN
            PluginBundle pluginBundle = new PluginBundle(bundleFile).copy(bundleFile);
            PluginRoot root = pluginBundle.getPluginRoot();

            // capture the catalog ID info for versioning purposes
            CatalogID id = root.getCatalogID();
            if (id == null) {
                missingCatalogID.add(bundleFile.getName());
                StrangeEons.log.warning("bundle has no catalog ID: " + bundleFile.getName());
            } else {
                // check if clashes with existing UUID and if so skip
                if (catalogIDMap.containsKey(id.getUUID()) && !bundleFile.equals(uuidToFileMap.get(id.getUUID()))) {
                    File alreadyLoaded = uuidToFileMap.get(id.getUUID());
                    StrangeEons.log.log(Level.WARNING,
                            "ignoring {0} because a bundle with the same UUID was already loaded: {1}",
                            new Object[]{bundleFile.getName(), alreadyLoaded.getAbsolutePath()}
                    );
                    return;
                }
                catalogIDMap.put(id.getUUID(), id);
                missingCatalogID.remove(bundleFile.getName());
                uuidToFileMap.put(id.getUUID(), bundleFile);
            }

            // register any languages reported by the root file
            registerRootFileLocales(root);
            // apply any special property definitions requested by the root file
            applyRootFileSpecialDefinitionProperties(root);

            final String[] rootErrors = root.getErrors();
            if (rootErrors != null && rootErrors.length > 0) {
                if (StrangeEons.log.isLoggable(Level.WARNING)) {
                    StringBuilder b = new StringBuilder("error(s) found while parsing plug-in root file ").append(bundleFile).append(':');
                    for (String e : rootErrors) {
                        b.append("\n    ").append(e);
                    }
                    StrangeEons.log.warning(b.toString());
                }
            }

            // Merge the bundle with the application class path
            linkPluginBundle(pluginBundle);

            // pluginSet == null ==> we are scanning libraries (so we are just
            // linking, not collecting plug-in lists). Note: if this is a theme
            // scan, loadThemeBundles() will convert the InstalledPlugin objects
            // to InstalledTheme objects later on.
            if (pluginSet != null) {
                for (String pluginClass : root.getPluginIdentifiers()) {
                    InstalledBundleObject ibo;
                    switch (pluginType) {
                        case PluginBundle.TYPE_THEME:
                            if (pluginClass.startsWith("script:") || pluginClass.endsWith(".js")) {
                                StrangeEons.log.log(Level.WARNING, "themes must be compiled classes: ignoring {0}", pluginClass);
                                continue;
                            }
                            try {
                                ibo = new InstalledTheme(pluginBundle, pluginClass);
                            } catch (CoreComponents.MissingCoreComponentException e) {
                                StrangeEons.log.log(Level.INFO, "theme not installed becasue required L&F is missing: {0}", pluginClass);
                                continue;
                            }
                            break;
                        case PluginBundle.TYPE_EXTENSION:
                            ibo = new InstalledExtension(pluginBundle, pluginClass);
                            break;
                        case PluginBundle.TYPE_PLUGIN:
                            ibo = new InstalledPlugin(pluginBundle, pluginClass);
                            break;
                        default:
                            // for libs, pluginSet should be null
                            throw new IllegalArgumentException();
                    }
                    ((Set<InstalledBundleObject>) pluginSet).add(ibo); // correct type always passed in
                }
            } else {
                installedLibraries.add(new InstalledLibrary(pluginBundle));
            }
        } catch (Exception e) {
            // add the file with a null bundle; this prevents us from
            // trying to add the bundle again
            discoveredBundles.put(bundleFile, null);
            ErrorDialog.displayError(string("rk-err-plugin-init", name), e);
        }
    }

    /**
     * Adds locales listed in the bundle root file to the list of known locales.
     */
    private static void registerRootFileLocales(PluginRoot root) {
        String langList = root.getClientProperty(PluginRoot.CLIENT_KEY_GAME_LANGUAGES);
        for (int i = 0; i < 2; ++i) {
            if (langList != null) {
                for (String token : langList.split(",")) {
                    try {
                        Locale loc = Language.parseLocaleDescription(token.trim());
                        if (i == 0) {
                            Language.addGameLocale(loc);
                        } else {
                            Language.addInterfaceLocale(loc);
                        }
                    } catch (IllegalArgumentException e) {
                        // not using {0} so we don't get two copies of the string constant
                        StrangeEons.log.log(Level.WARNING, "ignoring invalid locale " + token);
                    } catch (Exception e) {
                        // only expecting IllegalArgumentException, but best to
                        // catch broadly here rather than killing the app
                        StrangeEons.log.log(Level.SEVERE, "ignoring invalid locale " + token, e);
                    }
                }
            }
            langList = root.getClientProperty(PluginRoot.CLIENT_KEY_UI_LANGUAGES);
        }
    }

    /**
     * Applies special -D and -S properties defined in the root file.
     */
    private static void applyRootFileSpecialDefinitionProperties(PluginRoot root) {
        List<String> keys = root.getSpecialDefinitionKeys();
        if (keys != null) {
            for (String fullKey : keys) {
                String property = fullKey.substring(2);
                String value = root.getClientProperty(fullKey);
                switch (fullKey.charAt(1)) {
                    case 'D':
                        System.setProperty(property, value);
                        break;
                    case 'S':
                        RawSettings.setGlobalSetting(property, value);
                        break;
                    default:
                        StrangeEons.log.severe((String) null);
                }
            }
        }
    }

    /**
     * Adds the plug-in bundle to the class path. The bundle must be in plain
     * format.
     *
     * @param pluginBundle the bundle to link
     * @throws PluginException if linking fails
     */
    private static void linkPluginBundle(PluginBundle pluginBundle) throws PluginException {
        if (pluginBundle == null) {
            throw new NullPointerException("pluginBundle");
        }
        if (pluginBundle.getFormat() != PluginBundle.FORMAT_PLAIN) {
            throw new IllegalArgumentException("bundle must be FORMAT_PLAIN");
        }

        File bundleFile = pluginBundle.getFile();
        try {
            JarLoader.addToClassPath(bundleFile);
            discoveredBundles.put(bundleFile, pluginBundle);
            StrangeEons.log.log(Level.FINE, "dynamically linked to plug-in bundle {0}", bundleFile);
        } catch (IOException ioe) {
            throw new PluginException("failed to load bundle", ioe);
        } catch (Throwable t) {
            throw new PluginException("bundle link error", t);
        }
    }

    /**
     * Returns the {@link CatalogID} extracted from a discovered bundle whose
     * UUID matches {@code uuid}, or {@code null}.
     *
     * @param uuid the UUID to match against installed catalog IDs
     * @return the ID associated with the UUID, or {@code null}
     */
    public static CatalogID getInstalledCatalogID(UUID uuid) {
        return catalogIDMap.get(uuid);
    }

    /**
     * Returns {@code true} if a bundle with this file name but no catalog
     * information has been installed. It will be assumed by the catalog system
     * then when a catalog is opened that contains a bundle whose name is the
     * same as a bundle with no ID, the new bundle (now with catalog info)
     * updates the old bundle.
     *
     * @param name the name to check against installed bundles
     * @return {@code true} if a bundle with this name and no catalog
     * information is installed
     */
    public static boolean isUncatalogedBundleName(String name) {
        return missingCatalogID.contains(name);
    }

    /**
     * Returns all of the {@link CatalogID}s from discovered bundles that
     * include catalog information.
     *
     * @return installed catalog IDs
     */
    public static CatalogID[] getInstalledCatalogIDs() {
        // (this method provided for debugging purposes)
        return catalogIDMap.values().toArray(new CatalogID[catalogIDMap.size()]);
    }

    /**
     * Returns the names (without path information) of all installed bundles
     * that do not include catalog information.
     *
     * @return uncatalogued bundle file names
     */
    public static String[] getUncataloguedBundleNames() {
        // (this method provided for debugging purposes)
        return missingCatalogID.toArray(String[]::new);
    }

    /**
     * Returns the file that a plug-in is stored in, or {@code null} if the
     * plug-in is not stored in a bundle.
     *
     * @param identifier the normalized plug-in identifier, such as a class name
     * @return the bundle file for the plug-in
     * @see #getPluginBundle(java.io.File)
     */
    public static File getBundleFileForPlugin(String identifier) {
        return pluginToFileMap.get(identifier);
    }

    /**
     * Returns the plug-in bundle file whose catalog ID uses the specified UUID.
     *
     * @param uuid the unique ID of the bundle's catalog ID
     * @return the file that contains this UUID, or {@code null}
     * @see #getPluginBundle(java.io.File)
     */
    public static File getBundleFileForUUID(UUID uuid) {
        return uuidToFileMap.get(uuid);
    }

    /**
     * Returns the {@link PluginBundle PluginBundle} associated with a loaded
     * plug-in bundle, or {@code null} if the file is not a loaded bundle.
     *
     * @param f the bundle file to match
     * @return the bundle file's plug-in bundle, or {@code null}
     */
    public static PluginBundle getPluginBundle(File f) {
        synchronized (discoveredBundles) {
            return discoveredBundles.get(f);
        }
    }

    /**
     * Returns the {@link PluginBundle PluginBundle} associated with the loaded
     * plug-in that has the specified UUID, or {@code null} if there is no such
     * loaded bundle.
     *
     * @param uuid the UUID of the target bundle, as specified in its catalog ID
     * @return the loaded bundle with the target UUID, or {@code null}
     */
    public static PluginBundle getPluginBundle(UUID uuid) {
        return getPluginBundle(getBundleFileForUUID(uuid));
    }

    /**
     * Checks if a plug-in with the given UUID is installed.
     *
     * @param uuid the UUID of the plug-in
     * @return whether the plug-in is installed or not
     */
    public static boolean isPluginBundleInstalled(UUID uuid) {
        return getInstalledCatalogID(uuid) != null;
    }

    /**
     * Checks if a plug-in with the given {@link CatalogID}, or a newer version,
     * is installed.
     *
     * @param catId the {@code CatalogID} of the plug-in
     * @return whether the plug-in, or a newer version, is installed or not
     */
    public static boolean isPluginBundleInstalled(CatalogID catId) {
        CatalogID installedId = getInstalledCatalogID(catId.getUUID());
        return installedId != null && !installedId.isOlderThan(catId);
    }

    /**
     * Checks if a plug-in with the given identifier is installed. The
     * identifier should be a string which either contains a UUID or the string
     * representation of a {@link CatalogID}. If it is a {@code CatalogID}, then
     * the version of the installed plug-in must match or be newer.
     *
     * @param rawId the identifier of the plug-in
     * @return whether the plug-in, or a newer version, is installed or not
     * @throws IllegalArgumentException if the identifier is invalid
     */
    public static boolean isPluginBundleInstalled(String rawId) {
        CatalogID catId = CatalogID.extractCatalogID(rawId);
        if (catId != null) {
            return isPluginBundleInstalled(catId);
        }
        try {
            return isPluginBundleInstalled(UUID.fromString(rawId));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("must be a CatalogID or UUID: " + rawId);
        }
    }

    /**
     * Returns an array of the bundle files that have been dynamically added to
     * the class path. This set may include bundles that were discovered, but
     * not loaded (for example, if the bundle file was corrupt). In these cases,
     * calling {@link #getPluginBundle(java.io.File) getPluginBundle} for the
     * unloadable bundle will return {@code null}.
     *
     * @return an array of discovered bundle files
     * @see #getPluginBundle(java.io.File)
     */
    public static File[] getDiscoveredBundleFiles() {
        synchronized (discoveredBundles) {
            Set<File> keys = discoveredBundles.keySet();
            return keys.toArray(File[]::new);
        }
    }
    private static final HashMap<File, PluginBundle> discoveredBundles = new HashMap<>();

    private static final Map<UUID, CatalogID> catalogIDMap = new HashMap<>();
    private static final Map<UUID, File> uuidToFileMap = new HashMap<>();
    private static final Map<String, File> pluginToFileMap = new HashMap<>();
    private static final Set<String> missingCatalogID = new HashSet<>();

    /**
     * Returns the class loader for the plug-in system.
     *
     * @return the default parent class loader for the plug-in system
     */
    public static ClassLoader getPluginClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }

    private static void init() {
        if (!JarLoader.isSupported()) {
            ErrorDialog.displayFatalError(
                    "Unable to link to plug-in bundles; add <tt>-javaagent:strange-eons.jar</tt> to the command line",
                    null
            );
        }

        StrangeEons.log.log(Level.INFO, "loading dynamic bundles via {0}", JarLoader.getStrategy());
        deprecatedBundles = null;
        String ignoreValue = Settings.getShared().get("deprecated-bundles");
        if (ignoreValue != null) {
            deprecatedBundles = ignoreValue.trim().split("\\s*,\\s*");
        }
        for (int i = 0; i < deprecatedBundles.length; ++i) {
            deprecatedBundles[i] = deprecatedBundles[i].toLowerCase(Locale.CANADA);
        }
    }

    private static String[] deprecatedBundles;

    /**
     * If the plug-in bundle specified by bundleFile contains installation
     * notes, these are shown in a pop-up window.
     *
     * @param bundleFile the plain bundle that may contain installation notes
     */
    public static void showBundleInstallationNotes(final File bundleFile) {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(() -> {
                showBundleInstallationNotes(bundleFile);
            });
            return;
        }

        JarFile jar = null;
        try {
            PluginBundle pb = new PluginBundle(bundleFile);
            jar = new JarFile(pb.getPlainFile(), false);

            int names = 0;
            Locale loc = Language.getInterfaceLocale();
            String[] matches = new String[10];
            for (int j = 0; j < 2; ++j) {
                if (!loc.getLanguage().isEmpty()) {
                    if (!loc.getCountry().isEmpty()) {
                        matches[names++] = "install_" + loc.getLanguage() + '_' + loc.getCountry() + ".md";
                        matches[names++] = "install_" + loc.getLanguage() + '_' + loc.getCountry() + ".html";
                    }
                    matches[names++] = "install_" + loc.getLanguage() + ".md";
                    matches[names++] = "install_" + loc.getLanguage() + ".html";
                }
                loc = Language.getGameLocale();
            }
            matches[names++] = "install.md";
            matches[names++] = "install.html";

            int bestMatchIndex = names;
            JarEntry bestMatch = null;
            Enumeration<JarEntry> files = jar.entries();
            while (files.hasMoreElements() && bestMatchIndex > 0) {
                JarEntry entry = files.nextElement();
                String name = entry.getName();
                for (int i = 0; i < bestMatchIndex; ++i) {
                    if (name.equals(matches[i])) {
                        bestMatch = entry;
                        bestMatchIndex = i;
                        break;
                    }
                }
            }

            if (bestMatch != null) {
                URL url = new URL("jar:" + bundleFile.toURI().toURL().toString() + "!/" + bestMatch);
                new InstallationNotesViewer(StrangeEons.getWindow(), url).setVisible(true);
            }
        } catch (IOException e) {
            StrangeEons.log.log(Level.WARNING, "exception while trying to read installation notes " + bundleFile, e);
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Returns a list of the plug-in identifiers in a bundle. A plug-in
     * identifier is either a class name (for a compiled plug-in class) or a
     * script identifier that consists of the string <tt>script:</tt> followed
     * by a URL that points to the script.
     *
     * @param bundle the bundle file to list plug-ins from
     * @return an array, possibly empty, of plug-ins in the script
     */
    public static String[] listPluginsInBundle(File bundle) {
        try {
            PluginBundle pb = new PluginBundle(bundle);
            PluginRoot root = pb.getPluginRoot();
            String[] found = root.getPluginIdentifiers();
            for (String s : found) {
                pluginToFileMap.put(s, bundle);
            }
            return found;
        } catch (Exception e) {
            StrangeEons.log.log(Level.SEVERE, "unable to read plug-in list from root file for " + bundle, e);
        }
        return new String[0];
    }

    /**
     * Installs a plug-in file by copying it to the user's plug-in folder. If
     * the source bundle is in a published form (the format typically used for
     * network transport by the catalogue system), it must be unpacked
     * <i>prior</i>
     * to installation with this method. If the bundle is a regular bundle file
     * in Web-safe format, it will be converted to plain format by this method.
     * If the bundle's catalogue ID is the same as an already installed bundle,
     * then this bundle will be copied to the folder as an <tt>.autoupdate</tt>
     * file and the installation will complete the next time the application
     * starts.
     *
     * <p>
     * If the bundle file is in the plug-in folder and has already been
     * discovered and loaded by the plug-in system, then if it is currently
     * marked for uninstallation, the uninstall request will be cleared.
     *
     * <p>
     * If the bundle is installed successfully, its installer (if any) will then
     * be run. If it is copied to an <tt>.autoupdate</tt> file, the installer
     * will run when the plug-in is installed after restart. this method will
     * clear the uninstall request.
     *
     * <p>
     * This method returns a bitmask of flags that indicate the type of bundle
     * that was installed, the installation type, and whether a restart is
     * required. If the installer takes no action or simply clears the
     * uninstallation flag, it returns 0. If several bundles are being installed
     * together, successive return values can be binary or'd together to obtain
     * a summary of all actions that were taken.
     *
     * <p>
     * This method does not re-scan the plug-in folders to link or start the
     * newly installed bundle. If a plug-in (as opposed to a theme or extension)
     * was installed, and if it was not an update, then the new plug-in can be
     * installed by {@linkplain #loadPluginBundles() loading the bundle}, then
     * {@linkplain StrangeEons#loadPlugins() reloading all plug-ins}.
     *
     * @param bundleFile the file to install
     * @return a bitmask summarizing the result, or 0
     * @throws NullPointerException if the bundle file is {@code null}
     * @throws IOException if an I/O error occurs while installing the bundle
     * @see #INSTALL_FLAG_RESTART_REQUIRED
     * @see #INSTALL_FLAG_UPDATED
     * @see #INSTALL_FLAG_REGRESSED
     * @see #INSTALL_FLAG_LIBRARY
     * @see #INSTALL_FLAG_THEME
     * @see #INSTALL_FLAG_EXTENSION
     * @see #INSTALL_FLAG_PLUGIN
     */
    public static synchronized int installPluginBundle(File bundleFile) throws IOException {
        if (bundleFile == null) {
            throw new NullPointerException("bundleFile");
        }

        // if the target is already installed, make sure it will not uninstall
        // but otherwise do nothing
        if (getPluginBundle(bundleFile) != null) {
            if (StrangeEons.log.isLoggable(Level.INFO)) {
                if (StrangeEons.getApplication().willDeleteOnStartup(bundleFile)) {
                    StrangeEons.log.log(Level.INFO, "clearing bundle uninstall flag: " + bundleFile.getName());
                }
            }
            StrangeEons.getApplication().deleteOnStartup(bundleFile, false);
            return 0;
        }

        int flags = 0;
        File dest;

        // obtain the CatID, if any
        PluginBundle pb = new PluginBundle(bundleFile);
        PluginRoot root = pb.getPluginRoot();
        CatalogID id = root != null ? root.getCatalogID() : null;

        // if there is a bundle with this ID installed, we will use its file name
        if (id != null && getBundleFileForUUID(id.getUUID()) != null) {
            StrangeEons.log.log(Level.INFO, "updating plug-in bundle: {0}", bundleFile.getName());
            CatalogID installedID = getInstalledCatalogID(id.getUUID());
            if (id.isOlderThan(installedID)) {
                StrangeEons.log.log(Level.WARNING, "installing old version of plug-in over new version: {0}", bundleFile.getName());
                flags |= INSTALL_FLAG_REGRESSED;
            }
            File existing = getBundleFileForUUID(id.getUUID());
            dest = new File(existing.getParentFile(), existing.getName() + UPDATE_FILE_EXT);
            flags |= INSTALL_FLAG_UPDATED;
        } // otherwise, try to use the same name as the incoming bundle
        //   - if a bundle with this name exists:
        //     - if it has no CatID, assume we are updating that bundle with the new bundle
        //     - otherwise, create a new dummy name for the destination
        else {
            dest = new File(PLUGIN_FOLDER, bundleFile.getName());
            if (dest.exists()) {
                PluginBundle existing = getPluginBundle(dest);
                if (existing != null) {
                    CatalogID existingID = existing.getPluginRoot() != null ? existing.getPluginRoot().getCatalogID() : null;
                    if (existingID == null) {
                        StrangeEons.log.log(Level.WARNING, "assuming that a bundle with no CatalogID is being updated: {0}", bundleFile.getName());
                        dest = new File(PLUGIN_FOLDER, bundleFile.getName() + UPDATE_FILE_EXT);
                        flags |= INSTALL_FLAG_UPDATED;
                    } else {
                        StrangeEons.log.log(Level.INFO, "two bundles have the same file name, one will be renamed: {0}", bundleFile.getName());
                        dest = ProjectUtilities.getAvailableFile(dest);
                    }
                }
            }
        }

        // copy the source bundle file to the install location,
        // unwrapping it in the process if required
        pb = pb.copy(dest);

        // show the installation notes, if any
        showBundleInstallationNotes(pb.getFile());

        // determine install flags to return
        flags |= PluginBundle.getBundleType(pb.getFile());
        if (flags == 0) {
            StrangeEons.log.severe("not expecting flags == 0 at this point");
        }

        if ((flags & (BundleInstaller.INSTALL_FLAG_THEME | BundleInstaller.INSTALL_FLAG_EXTENSION)) != 0) {
            flags |= INSTALL_FLAG_RESTART_REQUIRED;
        }
        if ((flags & INSTALL_FLAG_UPDATED) != 0) {
            flags |= INSTALL_FLAG_RESTART_REQUIRED;
        } else {
            // if not installed as an update, run the installer (if any) now
            runInstallerScript(root, false);
        }
        return flags;
    }

    /**
     * An installation flag that indicates that a restart is required.
     */
    public static final int INSTALL_FLAG_RESTART_REQUIRED = 1;
    /**
     * An installation flag that indicates that a library bundle was installed.
     */
    public static final int INSTALL_FLAG_LIBRARY = 2;
    /**
     * An installation flag that indicates that a theme bundle was installed.
     */
    public static final int INSTALL_FLAG_THEME = 4;
    /**
     * An installation flag that indicates that an extension plug-in bundle was
     * installed.
     */
    public static final int INSTALL_FLAG_EXTENSION = 8;
    /**
     * An installation flag that indicates that a regular plug-in bundle was
     * installed.
     */
    public static final int INSTALL_FLAG_PLUGIN = 16;
    /**
     * An installation flag that indicates that a plug-in bundle was updated (or
     * will be when the application restarts).
     */
    public static final int INSTALL_FLAG_UPDATED = 32;
    /**
     * An installation flag that indicates that a plug-in bundle was "updated"
     * to a previous version of the plug-in. (The updated flag bit will also be
     * set.)
     */
    public static final int INSTALL_FLAG_REGRESSED = 64;

    /**
     * Completes the installation of one or more bundles by rescanning the
     * plug-in folders and loading new plug-ins. If a restart is required to
     * complete installation,
     * {@link ca.cgjennings.apps.arkham.StrangeEonsAppWindow#suggestRestart}
     * will be called to inform the user.
     *
     * @param flags the binary-or of all installation flags returned by the
     * plug-ins that were installed
     */
    public static synchronized void finishBundleInstallation(int flags) {
        JUtilities.threadAssert();

        if ((flags & INSTALL_FLAG_LIBRARY) != 0) {
            loadLibraryBundles();
        }
        if ((flags & INSTALL_FLAG_THEME) != 0) {
            loadThemeBundles();
        }
        if ((flags & INSTALL_FLAG_EXTENSION) != 0) {
            // nothing to do since it can't be started until next run
        }
        if ((flags & INSTALL_FLAG_PLUGIN) != 0) {
            loadPluginBundles();
        }
        if ((flags & INSTALL_FLAG_RESTART_REQUIRED) != 0) {
            StrangeEons.getWindow().suggestRestart(null);
        }
    }

    /**
     * Marks a plug-in for uninstallation. The bundle will not be uninstalled
     * immediately. If the bundle is still marked for uninstallation at
     * application shutdown, then the bundle's uninstaller will run (if any).
     * When the application is next started, the bundle file will be deleted.
     *
     * @param bundle the bundle to uninstall
     */
    public static synchronized void uninstallPluginBundle(PluginBundle bundle) {
        if (bundle == null) {
            throw new NullPointerException("bundle");
        }

        if (!StrangeEons.getApplication().willDeleteOnStartup(bundle.getFile())) {
            StrangeEons.getApplication().deleteOnStartup(bundle.getFile(), true);

            // check for an uninstaller, and if found prepare to run it at shutdown
            PluginRoot root;
            try {
                root = bundle.getPluginRoot();
                if (root.getInstallerIdentifier() == null) {
                    return;
                }
            } catch (IOException e) {
                StrangeEons.log.log(Level.SEVERE, "exception looking for uninstall script in root", e);
                return;
            }

            // the bundle has an installer script
            if (pendingUninstallScripts == null) {
                pendingUninstallScripts = new HashSet<>();
                StrangeEons.getApplication().addExitTask(new Runnable() {
                    @Override
                    public void run() {
                        for (PluginRoot root : pendingUninstallScripts) {
                            if (StrangeEons.getApplication().willDeleteOnStartup(root.getBundle().getFile())) {
                                StrangeEons.log.log(Level.INFO, "uninstalling {0}", root.getBundle().getFile().getName());
                                runInstallerScript(root, true);
                            }
                        }
                    }

                    @Override
                    public String toString() {
                        return "running plug-in bundle uninstallers";
                    }
                });
            }
            pendingUninstallScripts.add(root);
        }
    }
    private static HashSet<PluginRoot> pendingUninstallScripts;

    private static void runInstallerScript(PluginRoot root, boolean uninstall) {
        if (root == null) {
            return;
        }
        String script = root.getInstallerIdentifier();
        if (script == null) {
            return;
        }

        InstallationActions installer = null;

        if (script.startsWith(PluginRoot.SCRIPT_PREFIX)) {
            ScriptMonkey sm = new ScriptMonkey(script.substring("script:".length()));
            try {
                script = script.substring("script:".length());
                sm.eval(new URL(script));
            } catch (MalformedURLException e) {
                StrangeEons.log.log(Level.WARNING, "bad install script URL: {0}", script);
            }
            installer = sm.implement(InstallationActions.class);
        } else {
            try {
                installer = (InstallationActions) Class.forName(PluginRoot.normalizePluginIdentifier(script)).getConstructor().newInstance();
            } catch (ClassNotFoundException ex) {
                StrangeEons.log.log(Level.SEVERE, "installer class not found: {0}", script);
            } catch (Exception ex) {
                StrangeEons.log.log(Level.WARNING, "could not instantiate installer: " + script, ex);
            }
        }

        // If we got an installer after all of that, use it...
        if (installer != null) {
            try {
                if (uninstall) {
                    StrangeEons.log.log(Level.INFO, "running uninstall script for {0}", root.getBundle().getFile());
                    installer.uninstall(root.getBundle());
                } else {
                    StrangeEons.log.log(Level.INFO, "running install script for {0}", root.getBundle().getFile());
                    installer.install(root.getBundle());
                }
            } catch (Exception e) {
                StrangeEons.log.log(Level.WARNING, string("rk-err-plugin-install"), e);
            }
        }
    }

    /**
     * Returns {@code true} if the JavaFX runtime is available. This method will
     * attempt to locate, dynamically load, and start the runtime before
     * returning {@code false}.
     *
     * @return {@code true} if the JavaFX runtime is available
     * @deprecated This method returns {@code false}.
     */
    @Deprecated
    public static boolean isFXRuntimeAvailable() {
        return false;
    }
}
