package ca.cgjennings.ui.theme;

import ca.cgjennings.apps.arkham.CommandLineArguments;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.ui.MnemonicInstaller;
import javax.swing.JDialog;
import javax.swing.JFrame;
import resources.Settings;

/**
 * Encapsulates platform-specific theme bootstrapping details for OS X. This
 * moves platform-specific logic out of the main theme installer class. Helpers
 * conform (informally) to the following interface: {@link ThemeInstaller} will
 * instantiate (with a no-arg public constructor) a platform-specific helper if
 * one exists for the platform. Then, after the theme is installed, the theme
 * installer will call the helper's {@link #finish()} method.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
class OSXHelper {

    public OSXHelper() {
        JFrame.setDefaultLookAndFeelDecorated(false);
        JDialog.setDefaultLookAndFeelDecorated(false);

        Settings settings = Settings.getUser();
        CommandLineArguments arguments = StrangeEons.getApplication().getCommandLineArguments();

        if (settings.getYesNo("use-osx-menu-delegates") && !arguments.xDisableSystemMenu) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            // this MUST come AFTER the system menu property has been set;
            // see USE_POPUP_BORDER_HACK in MnemonicInstaller
            MnemonicInstaller.setMnemonicHidden(true);
        }
    }

    public void finish() {
    }
}
