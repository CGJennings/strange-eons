package ca.cgjennings.apps.arkham.plugins;

import java.io.IOException;

/**
 * An {@code InstalledExtension} bridges the gap between an extension
 * plug-in bundle and the plug-ins that it contains. It is not the plug-in
 * itself, but it creates and manages instances of the plug-in on demand. When a
 * plug-in bundle is installed, an instance is created for each plug-in listed
 * in the bundle's root file.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see BundleInstaller#getInstalledExtensions()
 */
public final class InstalledExtension extends AbstractInstalledPlugin {

    private boolean hasBeenStarted = false;

    InstalledExtension(PluginBundle bundle, String className) throws IOException {
        super(bundle, className);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the extension was started previously
     */
    @Override
    public synchronized Plugin startPlugin() throws PluginException {
        Plugin p;
        if (hasBeenStarted) {
            p = getPlugin();
            if (p != null) {
                return p;
            }
            throw new IllegalStateException("extensions can only be started once");
        }
        p = super.startPlugin();
        if (p != null) {
            hasBeenStarted = true;
        }
        return p;
    }

    @Override
    protected void collectPluginInfo(Plugin p) {
        hasBeenStarted = true;
    }

    @Override
    protected boolean isReloadable() {
        return false;
    }
}
