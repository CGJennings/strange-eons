package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.plugins.catalog.CatalogID;
import ca.cgjennings.ui.IconProvider;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import javax.swing.Icon;
import resources.Language;

/**
 * Implemented by objects that encapsulate a plug-in bundle that has been
 * discovered by the bundle installer. This interface defines a minimal level of
 * functionality; different kinds of plug-in bundles provide more specific
 * concrete implementations.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public abstract class InstalledBundleObject implements IconProvider, Comparable<InstalledBundleObject> {

    private PluginBundle bundle;
    private PluginRoot root;

    /**
     * Creates a new bundle object for the specified bundle. The bundle's root
     * file will be loaded, so it will be available to the constructors of
     * concrete subclasses via {@link #getPluginRoot()}.
     *
     * @param bundle the bundle file
     * @throws IOException if an I/O error occurs while loading the plug-in root
     */
    protected InstalledBundleObject(PluginBundle bundle) throws IOException {
        this.bundle = bundle;
        if (bundle != null) {
            try {
                root = bundle.getPluginRoot();
            } catch (IOException e) {
                StrangeEons.log.log(Level.SEVERE, "unable to read root file: " + bundle.getFile(), e);
                throw e;
            }
        }
    }

    /**
     * Returns the bundle that this was loaded from, or {@code null} if this
     * represents a built-in plug-in.
     *
     * @return the source bundle, or {@code null}
     */
    public PluginBundle getBundle() {
        return bundle;
    }

    /**
     * Returns the bundle's root file, or {@code null} if it does not have a
     * root file.
     *
     * @return the root file of the source bundle, or {@code null}
     */
    public PluginRoot getPluginRoot() {
        return root;
    }

    /**
     * Returns the {@link CatalogID} for the source bundle, or {@code null} if
     * the bundle's root file does not specify an id (or there is no bundle).
     *
     * @return the bundle's ID, or {@code null}
     */
    public CatalogID getCatalogID() {
        PluginRoot r = getPluginRoot();
        if (r != null) {
            return r.getCatalogID();
        }
        return null;
    }

    /**
     * Returns the startup priority in the bundle's root file, or
     * {@link PluginRoot#PRIORITY_DEFAULT} if none is specified.
     *
     * @return the plug-in's start priority
     */
    public int getPriority() {
        return getPluginRoot() == null ? PluginRoot.PRIORITY_DEFAULT : getPluginRoot().getPriority();
    }

    /**
     * Returns the name of the plug-in (or other bundle object). If necessary, a
     * new instance of the plug-in will be created temporarily in order to get
     * this information.
     *
     * @return the name reported by the plug-in
     */
    public abstract String getName();

    /**
     * Returns a description of the plug-in (or other bundle object). If
     * necessary, a new instance of the plug-in will be created temporarily in
     * order to get this information.
     *
     * @return the description reported by the plug-in
     */
    public abstract String getDescription();

    /**
     * Returns an icon for the plug-in (or other bundle object). If the plug-in
     * has a representative image, an icon is returned based on that image.
     * Otherwise, a generic icon is returned based on the type of plug-in
     * bundle.
     *
     * @return an icon for the plug-in
     */
    @Override
    public abstract Icon getIcon();

    /**
     * Returns the representative image of the plug-in. If necessary, a new
     * instance of the plug-in will be created temporarily in order to get this
     * information.
     *
     * @return the representative name reported by the plug-in (may be
     * {@code null})
     */
    public abstract BufferedImage getRepresentativeImage();

    /**
     * Returns {@code true} if there is an update pending for this plug-in. A
     * pending update is an updated version of the plug-in that will be
     * installed automatically the next time the application starts.
     *
     * @return {@code true} if an update is pending
     */
    public boolean isUpdatePending() {
        PluginBundle pb = getBundle();
        if (pb != null) {
            return new File(pb.getFile().getPath() + BundleInstaller.UPDATE_FILE_EXT).exists();
        }
        return false;
    }

    /**
     * Returns {@code true} if this plug-in's bundle has been marked for
     * uninstallation. (The plug-in's bundle will be deleted the next time the
     * application starts.)
     *
     * @return {@code true} if the plug-in will be uninstalled
     */
    public boolean isUninstallPending() {
        PluginBundle pb = getBundle();
        if (pb != null) {
            return StrangeEons.getApplication().willDeleteOnStartup(pb.getFile());
        }
        return false;
    }

    /**
     * Returns {@code true} is the bundle has been loaded. When a bundle has
     * been loaded, you can call {@link #getName}, {@link #getIcon}, and
     * {@link #getDescription} without triggering plug-in instantiation. When
     * sorting bundles, if this returns {@code false}, then only the bundle type
     * and priority will be used to determine bundle order.
     *
     * @return {@code true} is plug-in information has been loaded
     */
    abstract boolean isLoaded();

    /**
     * Returns the name of the object.
     *
     * @return the value of {@link #getName}
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * A comparator that sorts plug-ins by load order, then by priority, then by
     * name, then by catalog ID date.
     *
     * @param o the bundle object to compare to
     * @return a value that is negative, zero, or positive as the compared
     * bundle object is greater, equal, or less than this bundle object
     * @throws NullPointerException if the compared object is {@code null}
     * @see #getPriority()
     * @see #getName()
     * @see #getCatalogID()
     * @see
     * CatalogID#compareDates(ca.cgjennings.apps.arkham.plugins.catalog.CatalogID)
     */
    @Override
    public int compareTo(InstalledBundleObject o) {
        int cmp = loadOrder(this) - loadOrder(o);
        if (cmp == 0) {
            cmp = getPriority() - o.getPriority();
            if (cmp == 0 && isLoaded() && o.isLoaded()) {
                cmp = Language.getInterface().getCollator().compare(getName(), o.getName());
            }
            if (cmp == 0) {
                CatalogID tid = getCatalogID(), oid = o.getCatalogID();
                if (tid == null) {
                    if (oid != null) {
                        cmp = -1; // otherwise cmp==0
                    }
                } else if (oid == null) {
                    return 1;
                } else {
                    cmp = -tid.compareDates(oid);
                }
            }
        }
        return cmp;
    }

    private static int loadOrder(InstalledBundleObject o) {
        // generally not good OO practice, but adding an overridable method
        // for this seems just as inelegant
        if (o instanceof InstalledLibrary) {
            return 0;
        }
        if (o instanceof InstalledTheme) {
            return 1;
        }
        if (o instanceof InstalledExtension) {
            return 2;
        }
        if (o instanceof InstalledPlugin) {
            return 3;
        }
        StrangeEons.log.severe("unknown plug-in type");
        return 5;
    }

    /**
     * Marks the bundle for this object as having failed. Subclasses call this
     * when the specific bundle object fails to load or start correctly. If this
     * object has a catalog ID, then the UUID associated with that ID is marked
     * as having failed to start correctly. The {@link BundleInstaller}
     * maintains a set of all of the failed bundles.
     *
     * @see BundleInstaller#getFailedUUIDs()
     */
    protected final void markFailed() {
        try {
            BundleInstaller.failUUID(getCatalogID());
        } catch (Throwable t) {
            StrangeEons.log.log(Level.SEVERE, null, t);
        }
    }
}
