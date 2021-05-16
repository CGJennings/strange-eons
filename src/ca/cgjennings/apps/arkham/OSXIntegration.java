package ca.cgjennings.apps.arkham;

import ca.cgjennings.platform.OSXAdapter;
import ca.cgjennings.platform.PlatformSupport;
import java.awt.Component;
import java.io.File;
import java.util.logging.Level;

/**
 * Integrates the main application window more closely with the operating system
 * on the OS X platform. This class must be public for technical reasons. It is
 * not meant for use for by plug-in developers.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class OSXIntegration {

    private OSXIntegration(Component exitSeparator, Component exitItem, Component aboutSeparator, Component aboutItem, Component preferencesSeparator, Component preferencesItem) {
        if (!PlatformSupport.PLATFORM_IS_MAC) {
            throw new IllegalStateException();
        }

        // Use OSX adapter to try to register handlers; as each succeeds, the
        // corresponding menu item (of any) can be hidden or removed. Each
        // handler type is treated differently although they *should* either
        // all succeed or all fail.
        try {
            OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("exit", (Class[]) null));
            exitSeparator.setVisible(false);
            exitItem.setVisible(false);
        } catch (Exception e) {
            StrangeEons.log.log(Level.WARNING, "OSXIntegration failure", e);
        }

        try {
            OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("about", (Class[]) null));
            aboutSeparator.setVisible(false);
            aboutItem.setVisible(false);
        } catch (Exception e) {
            StrangeEons.log.log(Level.WARNING, "OSXIntegration failure", e);
        }

        try {
            OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod("preferences", (Class[]) null));
            preferencesSeparator.setVisible(false);
            preferencesItem.setVisible(false);
        } catch (Exception e) {
            StrangeEons.log.log(Level.WARNING, "OSXIntegration failure", e);
        }

        try {
            OSXAdapter.setFileHandler(this, getClass().getDeclaredMethod("open", new Class[]{String.class}));
        } catch (Exception e) {
            StrangeEons.log.log(Level.WARNING, "OSXIntegration failure", e);
        }
    }

    static void install(Component exitSeparator, Component exitItem, Component aboutSeparator, Component aboutItem, Component preferencesSeparator, Component preferencesItem) {
        new OSXIntegration(exitSeparator, exitItem, aboutSeparator, aboutItem, preferencesSeparator, preferencesItem);
    }

    /**
     * Proxy method for opening a document.
     *
     * @param filespec a string the specifies the full path to the document to
     * open
     * @see StrangeEonsAppWindow#openFile(java.io.File)
     */
    public void open(String filespec) {
        AppFrame.getApp().openFile(new File(filespec));
    }

    /**
     * Proxy method for displaying the <b>About</b> dialog.
     *
     * @see StrangeEonsAppWindow#showAboutDialog()
     */
    public void about() {
        AppFrame.getApp().showAboutDialog();
    }

    /**
     * Proxy method for displaying the <b>Preferences</b> dialog.
     *
     * @see StrangeEonsAppWindow#showPreferencesDialog(java.awt.Component,
     * ca.cgjennings.apps.arkham.dialog.prefs.PreferenceCategory)
     */
    public void preferences() {
        AppFrame.getApp().showPreferencesDialog(AppFrame.getApp(), null);
    }

    /**
     * Proxy method for exiting the application.
     *
     * @see StrangeEonsAppWindow#exitApplication(boolean)
     * @return {@code false} if the exit was cancelled
     */
    public boolean exit() {
        AppFrame.getApp().exitApplication(false);
        return false;
    }
}
