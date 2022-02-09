package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.plugins.catalog.CatalogID;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.ui.theme.ThemedIcon;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import javax.swing.Icon;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * An installed plug-in bridges the gap between a plug-in bundle and the
 * plug-ins that it contains. It is not the plug-in itself, but it creates and
 * manages instances of the plug-in on demand. This is an abstract base class
 * for the two basic types of plug-in in Strange Eons: extensions and "regular"
 * plug-ins (which encompasses both the {@code INJECTED} and {@code ACTIVATED}
 * plug-in types). These two types are similar except that extensions can only
 * be started once (during startup), while regular plug-ins are stopped and
 * started on demand.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public abstract class AbstractInstalledPlugin extends InstalledBundleObject {

    private String id;
    private Plugin plugin;
    private boolean collectedInfo;
    private String name;
    private String desc;
    private float version;
    private Icon icon;
    private int type;
    private String prefix;

    AbstractInstalledPlugin(PluginBundle bundle, String className) throws IOException {
        super(bundle);
        if (className == null) {
            throw new NullPointerException("className");
        }
        id = className;
    }

    /**
     * Returns the plug-in description. If necessary, a new instance of the
     * plug-in will be created temporarily in order to get this information.
     *
     * @return the description reported by the plug-in
     */
    @Override
    public String getDescription() {
        collectPluginInfo();
        return desc;
    }

    /**
     * Returns an icon for the plug-in. If the plug-in has a representative
     * image, an icon is returned based on that image. Otherwise, a generic icon
     * is returned based on the type of plug-in bundle.
     *
     * @return an icon for the plug-in
     */
    @Override
    public ThemedIcon getIcon() {
        collectPluginInfo();
        ThemedIcon icon = plugin.getPluginIcon();
        if (icon == null) {
            PluginBundle bundle = getBundle();
            if (bundle != null) {
                icon = PluginBundle.getIcon(bundle.getFile(), true);                    
            }
        }
        if (icon == null) {
            icon = ResourceKit.getIcon("plugin").small();
        }
        return icon;
    }

    /**
     * Returns the name of the plug-in. If necessary, a new instance of the
     * plug-in will be created temporarily in order to get this information.
     *
     * @return the name reported by the plug-in
     */
    @Override
    public String getName() {
        collectPluginInfo();
        return name;
    }

    /**
     * Returns the plug-in instance managed by this InstalledPlugin, or
     * {@code null} if no instance currently exists. To create an instance, call
     * {@link #startPlugin()}.
     *
     * @return the current plug-in instance, or {@code null}
     */
    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * Returns the class or script identifier, in normalized form, that this
     * plug-in represents.
     *
     * @return the plug-in class or script controlled by this instance
     */
    public String getPluginClass() {
        return id;
    }

    /**
     * Returns the type of the plug-in. If necessary, a new instance of the
     * plug-in will be created temporarily in order to get this information.
     *
     * @return the type reported by the plug-in
     */
    public int getPluginType() {
        collectPluginInfo();
        return type;
    }

    /**
     * Returns the representative image of the plug-in. If necessary, a new
     * instance of the plug-in will be created temporarily in order to get this
     * information.
     *
     * @return the representative name reported by the plug-in (may be
     * {@code null})
     */
    @Override
    public BufferedImage getRepresentativeImage() {
        return ImageUtilities.iconToImage(getIcon().mediumSmall());
    }

    /**
     * Returns a prefix that can be used to define a private namespace for the
     * plug-in when accessing settings. The same prefix will be used by the
     * plug-in context.
     *
     * @return a setting namespace prefix
     * @see Settings#createNamespace(java.lang.String)
     * @see PluginContext#getSettings()
     */
    public String getSettingPrefix() {
        synchronized (this) {
            if (prefix == null) {
                String infix;
                CatalogID id = getCatalogID();
                if (id != null) {
                    infix = id.toUUIDString();
                } else {
                    infix = getPluginClass();
                }
                prefix = "PLUGIN@" + infix + '-';
            }
        }
        return prefix;
    }

    /**
     * Returns the plug-in's internal version. This is separate from the version
     * information in the catalog ID. If necessary, a new instance of the
     * plug-in will be created temporarily in order to get this information.
     *
     * @return the version reported by the plug-in
     */
    public float getVersion() {
        collectPluginInfo();
        return version;
    }

    /**
     * Returns {@code true} if the plug-in represented by this object is
     * currently started.
     *
     * @return {@code true} if {@link #getPlugin} would return a
     * non-{@code null} value
     */
    public synchronized boolean isStarted() {
        return plugin != null;
    }

    /**
     * Instantiates and initializes the plug-in represented by this
     * InstalledPlugin. If an instance already exists, that instance is returned
     * as if by {@link #getPlugin()}.
     *
     * @return the started plug-in instance
     * @throws PluginException if the plug-in cannot be started
     */
    public synchronized Plugin startPlugin() throws PluginException {
        if (plugin != null) {
            return plugin;
        }
        plugin = startPluginImpl(false);
        collectPluginInfo();
        return plugin;
    }

    /**
     * Shuts down the plug-in instance, if any. After this call returns,
     * {@link #getPlugin()} will return {@code null} unless the plug-in is
     * started again.
     *
     * @throws PluginException if an exception occurs while stopping the plug-in
     */
    public synchronized void stopPlugin() throws PluginException {
        if (plugin != null) {
            try {
                stopPluginImpl(plugin);
            } finally {
                plugin = null;
            }
        }
    }

    private Plugin startPluginImpl(boolean infoProbe) throws PluginException {
        Plugin p;
        try {
            if (id.startsWith("script:")) {
                p = new DefaultScriptedPlugin(id);
            } else {
                p = (Plugin) Class.forName(id).getConstructor().newInstance();
            }
            PluginContext context;

            if (infoProbe && isReloadable()) {
                context = PluginContextFactory.createProbeContext(this, p);
            } else {
                context = PluginContextFactory.createDualContext(this, p);
            }

            if (!p.initializePlugin(context)) {
                throw new PluginException("plug-in reports that it failed to initialize: " + id);
            }
        } catch (PluginException e) {
            markFailed();
            throw e;
        } catch (Throwable t) {
            markFailed();
            throw new PluginException("uncaught exception while starting plug-in: " + id, t);
        }
        return p;
    }

    private void stopPluginImpl(Plugin p) throws PluginException {
        try {
            if (p.getPluginType() == Plugin.ACTIVATED && p.isPluginShowing()) {
                p.showPlugin(PluginContextFactory.createContext(this, 0), false);
            }
            p.unloadPlugin();
        } catch (Throwable t) {
            throw new PluginException("uncaught exception while stopping plug-in", t);
        }
    }

    /**
     * Called by the bundle installer to collect basic plug-in information. A
     * plug-in instance is started, if necessary, but not stopped; subclasses
     * should stop the instance if applicable.
     */
    private synchronized void collectPluginInfo() {
        if (collectedInfo) {
            return;
        }

        Plugin p = plugin;
        boolean startedOnDemand = false;
        try {
            if (p == null) {
                p = startPluginImpl(true);
                startedOnDemand = true;
            }
            name = p.getPluginName();
            desc = p.getPluginDescription();
            version = p.getPluginVersion();
            type = p.getPluginType();

            collectPluginInfo(p);

            if (startedOnDemand) {
                if (isReloadable()) {
                    // if reloadable, stop and start again later
                    stopPluginImpl(p);
                } else {
                    // if not reloadable, keep this instance
                    plugin = p;
                }
            }
        } catch (PluginException e) {
            StrangeEons.log.log(Level.SEVERE, "exception while collecting plug-in info", e);
            name = "<html><font color='#ff0000'>&lt;?&gt;";
            desc = string("plug-err-instantiate");
        }

        if (name == null) {
            name = "<?>";
        }

        if (desc == null) {
            desc = "";
        }

        collectedInfo = true;
    }

    /**
     * Called to allow subclasses to collect additional information about a
     * plug-in while an information probe is being conducted. The base class
     * does nothing.
     */
    protected void collectPluginInfo(Plugin p) {
    }

    /**
     * Returns {@code true} if the two installed plug-ins are equal. Two
     * installed plug-ins are equal if they have the same class identifier.
     *
     * @param obj object to compare with
     * @return true if {@code obj} is an installed plug-in with the same plug-in
     * class
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AbstractInstalledPlugin)) {
            return false;
        }
        return getPluginClass().equals(((AbstractInstalledPlugin) obj).getPluginClass());
    }

    /**
     * Returns a hash code based on the plug-in's class identifier, for
     * consistency with {@link #equals(java.lang.Object)}.
     *
     * @return a hash code for the plug-in instance
     */
    @Override
    public int hashCode() {
        return getPluginClass().hashCode();
    }

    /**
     * Returns {@code true} if this plug-in is a reloadable type, or false if it
     * is only run once (and thus should be run at the same time as the plug-in
     * info is collected).
     *
     * @return {@code false} if the plug-in is an extension, {@code true}
     * otherwise
     */
    protected boolean isReloadable() {
        return true;
    }

    @Override
    boolean isLoaded() {
        return collectedInfo;
    }
}
