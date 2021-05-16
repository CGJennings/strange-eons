package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.platform.PlatformSupport;
import java.io.IOException;
import javax.swing.KeyStroke;
import resources.Settings;

/**
 * An {@code InstalledPlugin} bridges the gap between a plug-in bundle and
 * the plug-ins that it contains. It is not the plug-in itself, but it creates
 * and manages instances of the plug-in on demand. When a plug-in bundle is
 * installed, an instance is created for each plug-in listed in the bundle's
 * root file.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see BundleInstaller#getInstalledPlugins()
 */
public class InstalledPlugin extends AbstractInstalledPlugin {

    private String defAccel;

    InstalledPlugin(PluginBundle bundle, String className) throws IOException {
        super(bundle, className);
    }

    @Override
    protected void collectPluginInfo(Plugin p) {
        defAccel = p.getDefaultAcceleratorKey();
    }

    /**
     * Returns {@code true} if this plug-in is disabled. This state is
     * tracked in user {@link Settings}.
     *
     * @return {@code true} if the plug-in is enabled (which it is by
     * default)
     */
    public boolean isEnabled() {
        return !Settings.getUser().getYesNo(settingKey("disable"));
    }

    /**
     * Sets whether this plug-in is enabled. Note that this state is not
     * enforced by {@code InstalledPlugin}. Rather, higher level code is
     * expected to check whether a plug-in is enabled before starting it.
     *
     * @param enable {@code true} to enable the plug-in
     */
    public void setEnabled(boolean enable) {
        Settings.getUser().set(settingKey("disable"), enable ? "no" : "yes");
    }

    /**
     * Returns the accelerator key assigned to the plug-in. Types other than
     * {@code ACTIVATED} will always return {@code null}.
     *
     * @return the accelerator key for the plug-in, or {@code null} if none
     */
    public KeyStroke getAcceleratorKey() {
        if (getPluginType() != Plugin.ACTIVATED) {
            return null;
        }

        String value = Settings.getUser().get(settingKey("accelerator"));
        if (value == null) {
            value = defAccel;
        }
        if (value != null) {
            value = value.trim();
            return value.isEmpty() ? null : PlatformSupport.getKeyStroke(value);
        }
        return null;
    }

    /**
     * Sets the accelerator key for the plug-in, or assigns no accelerator if
     * {@code null}.
     *
     * @param ks the accelerator to assign, or {@code null}
     */
    public void setAcceleratorKey(KeyStroke ks) {
        final String key = settingKey("accelerator");
        if (ks != null) {
            Settings.getUser().set(key, ks.toString());
        } else {
            Settings.getUser().set(key, "");
        }
    }

    /**
     * Reset the plug-in's accelerator key to the default value.
     */
    public void resetAcceleratorKey() {
        Settings.getUser().reset(settingKey("accelerator"));
    }

    private String settingKey(String baseKeyName) {
        if (baseKeyName == null) {
            throw new NullPointerException("setting");
        }
        return getSettingPrefix() + baseKeyName;
    }
}
