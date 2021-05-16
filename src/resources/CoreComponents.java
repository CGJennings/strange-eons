package resources;

import ca.cgjennings.apps.arkham.NewEditorDialog;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.deck.PDFPrintSupport;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import ca.cgjennings.apps.arkham.plugins.catalog.Catalog;
import ca.cgjennings.apps.arkham.plugins.catalog.Catalog.VersioningState;
import ca.cgjennings.apps.arkham.plugins.catalog.CatalogID;
import ca.cgjennings.apps.arkham.plugins.catalog.CoreUpdateDialog;
import ca.cgjennings.graphics.shapes.SVGVectorImage;
import java.awt.EventQueue;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import static resources.Language.string;

/**
 * The set of named core components that can be loaded and installed on demand.
 * This class also provides static methods that can be used to install other
 * bundles on demand.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public enum CoreComponents {
    /**
     * Core typefaces for body text and last resort glyphs.
     */
    CORE_TYPEFACES("CATALOGUEID{7c3c4f03-84a2-48cd-bb6d-2c227d95ed04:2012-5-3-9-47-7-275}"),
    /**
     * Resources required to perform live spelling checking.
     */
    SPELLING_COMPONENTS("CATALOGUEID{38698844-2654-46a8-b791-36e5d454a7ec:2010-0-24-10-42-7-24}"),
    /**
     * Library needed to enable PDF output via {@link PDFPrintSupport}.
     */
    PDF_OUTPUT_SUPPORT("CATALOGUEID{c7c96b28-2c70-4632-b364-e060fd3a25b3:2013-0-14-22-27-6-54}"),
    /**
     * Library needed to load SVG image files using {@link SVGVectorImage}.
     */
    SVG_IMAGE_SUPPORT("CATALOGUEID{e69d2bd2-f155-4182-a38c-a3a356cfa0fb:2013-1-3-0-0-0-0}");

    CoreComponents(String id) {
        this.id = CatalogID.extractCatalogID(id);
    }

    /**
     * Returns the catalog ID that identifies this core component.
     *
     * @return the ID of the core component
     */
    public CatalogID getCatalogID() {
        return id;
    }

    /**
     * Returns an installation state for the core component. The return value
     * will be one of:
     * <pre>
     * VersioningState.NOT_INSTALLED
     * VersioningState.OUT_OF_DATE
     * VersioningState.UP_TO_DATE
     * </pre>
     *
     * @return the current installation state of the core component
     */
    public VersioningState getInstallationState() {
        if (knownUpToDate) {
            return VersioningState.UP_TO_DATE;
        }

        VersioningState vs = getInstallationState(id);

        if (vs == VersioningState.UP_TO_DATE) {
            knownUpToDate = true;
        }
        return vs;
    }

    /**
     * Returns an installation state for an arbitrary catalog ID. The return
     * value will be one of:
     * <pre>
     * VersioningState.NOT_INSTALLED
     * VersioningState.OUT_OF_DATE   (installed version is older than id)
     * VersioningState.UP_TO_DATE    (installed version is newer/same date)
     * </pre>
     *
     * @return the installation state of the requested component
     */
    public static VersioningState getInstallationState(CatalogID id) {
        CatalogID installed = BundleInstaller.getInstalledCatalogID(id.getUUID());

        if (installed == null) {
            return VersioningState.NOT_INSTALLED;
        }

        // the installed version is older than the current version
        // (i.e. the version this was compiled against)
        if (installed.isOlderThan(id)) {
            return VersioningState.OUT_OF_DATE;
        }

        return VersioningState.UP_TO_DATE;
    }

    /**
     * Ensures that this core component is installed and up to date. If not, the
     * user is prompted to download the missing component. If the user refuses,
     * a {@link MissingCoreComponentException} is thrown.
     */
    public void validate() {
        StrangeEons.log.log(Level.INFO, "checking for core component {0}", toString());

        VersioningState currentState = getInstallationState();

        if (currentState != VersioningState.UP_TO_DATE) {
            validateCoreComponents(id.toString());
        }
    }

    /**
     * Check for the presence of core or other required plug-ins, and install
     * them if required. If {@code descriptor} is a
     * {@code CoreComponents} identifier, that component is checked for.
     * Otherwise the value is converted to a string and parsed as follows:
     * <ol>
     * <li> The string is split into tokens by breaking at newlines; each token
     * represents one required plug-in bundle.
     * <li> Each token must contain a catalog ID. This ID identifies the
     * plug-in.
     * <li> The ID may optionally follow a URL ending in a '/'. This URL is
     * taken to be the location of the catalog that lists the bundle. If no URL
     * is present, the standard SE catalog is implied. Example:
     * <tt>http://somewhere.tld/CATALOGUEID{...}</tt>.
     * </ol>
     *
     * @param descriptor a {@code CoreComponent} or an object describing
     * the component to install (see above)
     * @throws NullPointerException if {@code descriptor} is
     * {@code null}
     * @throws MissingCoreComponentException if any component is missing or out
     * of date and the user refuses to install it, or if installation fails
     */
    public static void validateCoreComponents(Object descriptor) {
        if (descriptor == null) {
            throw new NullPointerException("descriptor");
        }

        if (descriptor instanceof CoreComponents) {
            ((CoreComponents) descriptor).validate();
            return;
        }

        final ComponentSet set = parseDescriptor(descriptor.toString());
        if (set.missingIDs) {
            Runnable r = () -> {
                boolean install = true;
                if (!Settings.getShared().getYesNo("core-dialog-shown")
                        || !Settings.getShared().getYesNo("core-dialog-autoinstall")) {
                    CoreUpdateDialog cud = new CoreUpdateDialog(set.restartRequired);
                    install = cud.showDialog();
                }
                if (!install) {
                    throw new MissingCoreComponentException(string("core-info"));
                }
                String current = "";
                try {
                    for (String url : set.catalogs.toArray(new String[set.catalogs.size()])) {
                        current = url;
                        Catalog c = new Catalog(new URL(url));
                        for (CatalogID id1 : set.idToCatUrlMap.keySet()) {
                            String targetCatalog = set.idToCatUrlMap.get(id1);
                            if (!targetCatalog.equals(url)) {
                                continue;
                            }
                            int index = c.findListingByUUID(id1.getUUID());
                            if (index < 0) {
                                StrangeEons.log.log(Level.WARNING, "required component is not in specified catalog: {0}", id1);
                            } else {
                                c.setInstallFlag(index, install);
                            }
                        }
                        c.installFlaggedPlugins();
                    }
                    if (set.restartRequired) {
                        StrangeEons.getWindow().suggestRestart(null);
                        throw new MissingCoreComponentException(string("core-restart-required"));
                    }
                    BundleInstaller.loadLibraryBundles();
                }catch (IOException e) {
                    ErrorDialog.displayError(string("cat-err-dl", current), e);
                }
            };
            if (EventQueue.isDispatchThread()) {
                try {
                    r.run();
                } catch (Throwable t) {
                    if (t instanceof MissingCoreComponentException) {
                        throw (MissingCoreComponentException) t;
                    }
                    t.printStackTrace();
                }
            } else {
                try {
                    EventQueue.invokeAndWait(r);
                } catch (Throwable t) {
                    if (t instanceof InvocationTargetException) {
                        t = t.getCause();
                    }
                    if (t instanceof MissingCoreComponentException) {
                        throw (MissingCoreComponentException) t;
                    }
                    t.printStackTrace();
                }
            }
        }
    }

    private static ComponentSet parseDescriptor(String desc) {
        ComponentSet set = new ComponentSet();
        String[] tokens = desc.split("\n");
        for (int i = 0; i < tokens.length; ++i) {
            String url;
            CatalogID id = CatalogID.extractCatalogID(tokens[i]);
            int slash = tokens[i].lastIndexOf('/');
            if (slash < 0) {
                url = Settings.getShared().get("catalog-url-1");
            } else {
                url = tokens[i].substring(0, slash + 1);
            }
            if(id == null) {
                StrangeEons.log.warning("unexpeced null UUID: " + desc);
                continue;
            }
            CatalogID installedId = BundleInstaller.getInstalledCatalogID(id.getUUID());
            if (installedId == null || installedId.isOlderThan(id)) {
                set.catalogs.add(url);
                set.idToCatUrlMap.put(id, url);
                set.missingIDs = true;
                if (installedId != null) {
                    set.restartRequired = true;
                }
            }
        }
        return set;
    }

    private static class ComponentSet {

        private HashSet<String> catalogs = new HashSet<>();
        private HashMap<CatalogID, String> idToCatUrlMap = new HashMap<>();
        private boolean missingIDs;
        private boolean restartRequired;
    }

    private final CatalogID id;
    private boolean knownUpToDate = false;

    /**
     * The exception that is thrown when a requested core component is missing
     * and cannot be installed due to an error or the user cancelling the
     * installation. This is an unchecked exception. It will generally only be
     * thrown during one of two processes: while creating a new component
     * through {@link NewEditorDialog#createEditorFromClassMapKey} or when
     * loading a component from disk using
     * {@link ResourceKit#getGameComponentFromFile}, both of which already catch
     * and handle this exception. If you write plug-in code that checks for
     * required components outside of new component creation/component loading,
     * then make sure that you handle this exception.
     */
    @SuppressWarnings("serial")
    public static class MissingCoreComponentException extends RuntimeException {

        public MissingCoreComponentException(String s) {
            super(s);
        }
    }
}
