package ca.cgjennings.apps.arkham.plugins;

import resources.Settings;

/**
 * The standard implementation of {@link PluginContext}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
final class PluginContextImpl implements PluginContext {

    private final AbstractInstalledPlugin installedPlugin;
    private final Plugin plugin;
    private final String settingPrefix;
    private final int modifiers;
    // set in constructor
    private final boolean informationProbe;
    // do not use directly: access through getSettings() */
    private Settings nameSpace;

    /**
     * Creates a new plug-in context for an installed plug-in. The installed
     * plug-in can be <code>null</code> if a dummy context is required; this is
     * used when scripts are run during development.
     *
     * @param installedPlugin the installed plug-in that holds the plug-in
     * instance that the context applied to
     * @param plugin the plug-in to create the context for; if
     * <code>null</code>, will be requested from the installedPlugin
     * @param modifiers if the plug-in is being activated, the mask for the
     * modifier keys that are held down; otherwise 0
     * @param isProbe <code>true</code> to set the context's information probe
     * flag, which indicates to the plug-in that only basic information will be
     * collected
     * @throws PluginException if the plug-in must be started and an exception
     * occurs when this is attempted
     */
    PluginContextImpl(AbstractInstalledPlugin installedPlugin, Plugin plugin, int modifiers, boolean isProbe) throws PluginException {
        this.installedPlugin = installedPlugin;
        if (installedPlugin != null) {
            if (plugin == null) {
                this.plugin = installedPlugin.startPlugin();
            } else {
                this.plugin = plugin;
            }
            settingPrefix = installedPlugin.getSettingPrefix();
        } else {
            this.plugin = null;
            settingPrefix = "IN_DEVELOPMENT:";
        }
        this.modifiers = modifiers;
        informationProbe = isProbe;
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public AbstractInstalledPlugin getInstalledPlugin() {
        return installedPlugin;
    }

    @Override
    public boolean isInformationProbe() {
        return informationProbe;
    }

    @Override
    public synchronized Settings getSettings() {
        if (nameSpace == null) {
            nameSpace = Settings.getUser().createNamespace(settingPrefix);
        }
        return nameSpace;
    }

    @Override
    public int getModifiers() {
        return modifiers;
    }

    @Override
    public String toString() {
        return "{3.x Plug-in Context}";
    }
}
