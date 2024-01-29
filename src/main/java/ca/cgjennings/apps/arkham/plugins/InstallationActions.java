package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;

/**
 * The interface implemented by installer scripts and classes for plug-in
 * bundles. An installer is listed in a plug-in bundle's root file by adding an
 * {@linkplain PluginRoot#KEY_INSTALL_SCRIPT installer key}. The value of the
 * key must identify a script or class that implements this interface.
 *
 * <p>
 * <b>Do not assume that the user interface is available when the methods in
 * this interface are invoked.</b> For example, you should not use
 * {@link ErrorDialog} to display error messages. You may, however, write
 * messages to the {@linkplain StrangeEons#log application log}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface InstallationActions {

    /**
     * Called when a plug-in bundle is being installed before any plug-ins in
     * the bundle are instantiated.
     *
     * @param bundle the bundle being installed
     */
    void install(PluginBundle bundle);

    /**
     * Called when a plug-in bundle is being uninstalled before the bundle file
     * is deleted.
     *
     * @param bundle the bundle being uninstalled
     */
    void uninstall(PluginBundle bundle);
}
