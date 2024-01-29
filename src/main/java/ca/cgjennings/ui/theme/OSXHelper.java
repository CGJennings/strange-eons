package ca.cgjennings.ui.theme;

import ca.cgjennings.apps.arkham.CommandLineArguments;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.ui.MnemonicInstaller;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
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
    // "MenuBarUI", "MenuUI", "MenuItemUI", "CheckBoxMenuItemUI",
    // "RadioButtonMenuItemUI", "PopupMenuUI"

    private static List<Object> menuDelegateKeys;
    private static List<Object> menuDelegateUIs;

    public OSXHelper() {
        JFrame.setDefaultLookAndFeelDecorated(false);
        JDialog.setDefaultLookAndFeelDecorated(false);

        Settings settings = Settings.getUser();
        CommandLineArguments arguments = StrangeEons.getApplication().getCommandLineArguments();

        if (settings.getYesNo("use-osx-menu-delegates") && !arguments.xDisableSystemMenu) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            try {
                Class<?> aquaLnFClass = Class.forName(UIManager.getSystemLookAndFeelClassName());
                LookAndFeel aquaLnF = (LookAndFeel) aquaLnFClass.getConstructor().newInstance();
                UIDefaults aquaDefaults = aquaLnF.getDefaults();
                menuDelegateKeys = new LinkedList<>();
                menuDelegateUIs = new LinkedList<>();

                // suffixes of keys to copy:
                //     "ground" covers all fg/bg colour combos
                //     "ont" covers (F|f)ont
                //     etc.
                String[] suffixes = {
                    "UI", "Painter", "Painted", "ground", "ont",
                    "con", "order", "argin"
                };

                for (Map.Entry<Object, Object> e : aquaDefaults.entrySet()) {
                    if (e.getKey() == null) {
                        continue;
                    }
                    String name = e.getKey().toString();
                    if (name.contains("Menu")) {
                        boolean copy = false;
                        for (String suffix : suffixes) {
                            if (name.endsWith(suffix)) {
                                copy = true;
                                break;
                            }
                        }

                        // Used to help figure out which keys to copy:
                        // changes when Aqua L&F updated (rare).
//						if( !copy ) {
//							System.err.print( name );
//							System.err.print( copy ? '*' : '-' );
//							System.err.println(" : " + e.getValue() );
//						}
                        if (copy) {
                            menuDelegateKeys.add(e.getKey());
                            menuDelegateUIs.add(e.getValue());
                        }
                    }
                }
            } catch (Throwable t) {
                // if for some reason this fails, prevent restoring the keys later
                menuDelegateKeys = null;
                menuDelegateUIs = null;
                StrangeEons.log.log(Level.WARNING, "unable to apply Aqua L&F while trying to obtain UI delegates", t);
            }

            // this MUST come AFTER the system menu property has been set;
            // see USE_POPUP_BORDER_HACK in MnemonicInstaller
            MnemonicInstaller.setMnemonicHidden(true);
        }
    }

    public void finish() {
        if (menuDelegateKeys != null) {
            Iterator<Object> keys = menuDelegateKeys.iterator();
            Iterator<Object> vals = menuDelegateUIs.iterator();
            while (keys.hasNext()) {
                UIManager.put(keys.next(), vals.next());
            }
        }
    }
}
