package ca.cgjennings.ui.theme;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import ca.cgjennings.apps.arkham.plugins.PluginBundle;
import ca.cgjennings.platform.DarkModeDetector;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.JUtilities;
import ca.cgjennings.ui.MnemonicInstaller;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.ColorUIResource;
import resources.Language;
import resources.ResourceKit;
import resources.Settings;

/**
 * Installs the {@link Theme} specified by the user's settings. If the theme
 * cannot be installed, a series of fallback mechanisms will be tried,
 * eventually ending with installing the system look and feel.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class ThemeInstaller {

    /**
     * Class name of the built-in "Dagon" theme.
     */
    public static final String THEME_DAGON_CLASS = "ca.cgjennings.ui.theme.DagonTheme";
    /**
     * Class name of the built-in "Hydra" theme.
     */
    public static final String THEME_HYDRA_CLASS = "ca.cgjennings.ui.theme.HydraTheme";
    /**
     * Class name of the built-in "Yuggoth" theme.
     */
    public static final String THEME_YUGGOTH_CLASS = "ca.cgjennings.ui.theme.YuggothTheme";

    /**
     * Class name of the built-in "Tcho Tcho" theme, which is based on the
     * system look and feel.
     */
    public static final String THEME_TCHO_TCHO_CLASS = "ca.cgjennings.ui.theme.TchoTchoTheme";
    /**
     * Class name of the theme applied during plug-in testing.
     */
    public static final String THEME_PLUGIN_TEST_THEME = "ca.cgjennings.ui.theme.PluginTestTheme";

    private static final String FALLBACK_THEME_CLASS = THEME_DAGON_CLASS;
    private static final String KEY_THEME_CLASS = "theme";
    private static final String KEY_DARK_THEME_CLASS = "dark-theme";
    private static final String KEY_AUTO_DARK = "auto-select-dark-theme";
    private static final String KEY_USE_TEST_THEME = "test-bundle-use-testing-theme";

    private static Object platformHelper;

    /* Instantiates a platform-specific helper, if one exists, and stores it
	 * in platformHelper so that its finish() method can be called later. */
    private static void loadPlatformHelper() {
        try {
            String name;
            if (PlatformSupport.PLATFORM_IS_MAC) {
                name = "OSX";
            } else if (PlatformSupport.PLATFORM_IS_WINDOWS) {
                name = "Windows";
            } else {
                name = "Other";
            }
            platformHelper = Class.forName("ca.cgjennings.ui.theme." + name + "Helper").getConstructor().newInstance();
        } catch (ClassNotFoundException cnf) {
            // no helper for this platform
        } catch (Throwable t) {
            StrangeEons.log.log(Level.SEVERE, null, t);
        }
    }

    private static Theme installed;

    /**
     * Returns the installed theme, or {@code null} if no theme is
     * installed. This will only be {@code null} if themes have not been
     * installed yet, or if the user theme could not be installed and the
     * built-in fallback theme could not be instantiated.
     *
     * @return the installed theme
     */
    public static Theme getInstalledTheme() {
        return installed;
    }

    /**
     * Implementation of {@link #install()}, called from the EDT.
     */
    private static void installOnEDT() {
        loadPlatformHelper();

        MnemonicInstaller.setMask(MnemonicInstaller.ALL);
        StrangeEons.log.fine("installed mnemonic handler");

        BundleInstaller.loadThemeBundles();
        installImpl();

        UIManager.getLookAndFeelDefaults().put("OptionPane.sameSizeButtons", Boolean.TRUE);

        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);

        if (platformHelper != null) {
            try {
                platformHelper.getClass().getMethod("finish").invoke(platformHelper);
            } catch (Throwable t) {
                StrangeEons.log.log(Level.SEVERE, null, t);
            }
            platformHelper = null;
        }
    }

    /**
     * Installs a look and feel based on the user's preference setting.
     */
    public static void install() {
        try {
            if (EventQueue.isDispatchThread()) {
                installOnEDT();
            } else {
                EventQueue.invokeAndWait(() -> {
                    try {
                        installOnEDT();
                    } catch (Throwable t) {
                        StrangeEons.log.log(Level.SEVERE, "installer threw uncaught exception", t);
                    }
                });
            }
        } catch (Throwable t) {
            StrangeEons.log.log(Level.SEVERE, "installer threw uncaught exception", t);
        }
    }

    private static Theme instantiateTheme() {
        Theme theme;
        String themeClass = Settings.getShared().get(KEY_THEME_CLASS);;
        if (Settings.getShared().getYesNo(KEY_AUTO_DARK)) {
            if (new DarkModeDetector().detect()) {
                themeClass = Settings.getShared().get(KEY_DARK_THEME_CLASS);
                StrangeEons.log.info("detected dark mode system setting");
            } else {
                StrangeEons.log.info("did not detect a dark mode system setting");
            }
        }
        if (themeClass == null || themeClass.length() == 0) {
            themeClass = FALLBACK_THEME_CLASS;
        }

        // if testing a plug-in, override default with special testing theme
        if (BundleInstaller.hasTestBundles()) {
            boolean foundTheme = false;
            for(File bundle : BundleInstaller.getTestBundles()) {
                if (bundle.getName().endsWith(BundleInstaller.THEME_FILE_EXT)) {
                    PluginBundle pb = BundleInstaller.getPluginBundle(bundle);
                    try {
                        String[] themes = pb.getPluginRoot().getPluginIdentifiers();
                        if (themes.length == 1 && !themes[0].startsWith("script:")) {
                            themeClass = themes[0];
                            foundTheme = true;
                        } else {
                            StrangeEons.log.warning("invalid root file for theme: must list exactly one compiled class name");
                        }
                    } catch (IOException e) {
                        StrangeEons.log.log(Level.SEVERE, "I/O error while reading test bundle", e);
                    }
                }
            }
            // no test bundle was a theme, use the special test theme
            if(!foundTheme && Settings.getShared().getYesNo(KEY_USE_TEST_THEME)) {
                themeClass = THEME_PLUGIN_TEST_THEME;
            }
        }

        try {
            theme = (Theme) Class.forName(themeClass).getConstructor().newInstance();
        } catch (Throwable t) {
            StrangeEons.log.log(Level.WARNING, "failed to instantiate theme " + themeClass, t);
            theme = new DagonTheme();
        }

        // typically, this means that the theme uses Nimbus
        // but this system is pre-Java 6u10
        if (theme.getLookAndFeelClassName() == null) {
            theme = new TchoTchoTheme();
        }

        return theme;
    }

    private static void installImpl() {
        Theme theme = instantiateTheme();

        try {
            installStrangeEonsUIDefaults();
            theme.modifyManagerDefaults(UIManager.getDefaults());
            LookAndFeel laf = (LookAndFeel) Class.forName(theme.getLookAndFeelClassName()).getConstructor().newInstance();

            UIDefaults lafDefs = laf.getDefaults();

            lafDefs.put("OptionPane.errorIcon", new ImageIcon(ResourceKit.class.getResource("icons/application/error.png")));
            lafDefs.put("OptionPane.warningIcon", new ImageIcon(ResourceKit.class.getResource("icons/application/warning.png")));
            lafDefs.put("OptionPane.questionIcon", null);
            lafDefs.put("OptionPane.informationIcon", null);

            // put in the theme's L&F defaults
            theme.modifyLookAndFeelDefaults(lafDefs);

            theme.modifyLookAndFeel(laf);
            UIManager.setLookAndFeel(laf);
            installStrangeEonsUIFallbackDefaults();
            theme.themeInstalled();

            installed = theme;
            StrangeEons.log.log(Level.INFO, "installed theme \"{0}\"", theme.getThemeName());
        } catch (Throwable t) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Throwable it) {
                StrangeEons.log.log(Level.SEVERE, null, it);
            }
            installed = null;
            ErrorDialog.displayError(Language.string("rk-err-theme"), t);
        }
    }

    private static void installStrangeEonsUIDefaults() {
        UIDefaults ui = UIManager.getDefaults();
        ui.put(Theme.EDITOR_TAB_BACKGROUND, new Color(0x73_96ab));
        ui.put(Theme.SIDEPANEL_TITLE_BACKGROUND, Color.BLACK);
        ui.put(Theme.SIDEPANEL_TITLE_FOREGROUND, Color.WHITE);

        ui.put(Theme.CONTEXT_BAR_BACKGROUND, Color.WHITE);
        ui.put(Theme.CONTEXT_BAR_FOREGROUND, Color.BLACK);
        ui.put(Theme.CONTEXT_BAR_BUTTON_BACKGROUND, Color.WHITE);
        Color rollover = new Color(0xff_cb41);
        ui.put(Theme.CONTEXT_BAR_BUTTON_ROLLOVER_BACKGROUND, rollover);
        ui.put(Theme.CONTEXT_BAR_BUTTON_ROLLOVER_OUTLINE_FOREGROUND, rollover.darker());
        ui.put(Theme.CONTEXT_BAR_BUTTON_ARMED_OUTLINE_FOREGROUND, new Color(0x40_b3ff).darker());

        ui.put(Theme.CONSOLE_FONT, new Font(Font.MONOSPACED, Font.PLAIN, 13));
    }

    private static void installStrangeEonsUIFallbackDefaults() {
        UIDefaults ui = UIManager.getDefaults();
        installFallbackColour(ui, Theme.MESSAGE_BORDER_EXTERIOR, "text", 0x202f66);
        installFallbackColour(ui, Theme.MESSAGE_BORDER_EDGE, "controlHighlight", 0xf7f8fa);
        installFallbackColour(ui, Theme.MESSAGE_BORDER_MAIN, "nimbusFocus", 0xb5caff);
        installFallbackColour(ui, Theme.MESSAGE_BACKGROUND, "nimbusLightBackground", 0xffffff);
        installFallbackColour(ui, Theme.MESSAGE_FOREGROUND, "text", 0x000000);

        if (ui.get(Theme.MESSAGE_BORDER_DIALOG) == null) {
            Border darkBorder = new LineBorder(ui.getColor(Theme.MESSAGE_BORDER_EXTERIOR), 1);
            Border lightBorder = new LineBorder(ui.getColor(Theme.MESSAGE_BORDER_EDGE), 1);
            Border midBorder = new LineBorder(ui.getColor(Theme.MESSAGE_BORDER_MAIN), 4);
            ui.put(
                    Theme.MESSAGE_BORDER_DIALOG,
                    JUtilities.createCompoundBorder(darkBorder, lightBorder, midBorder, lightBorder, darkBorder)
            );
        }
        if (ui.get(Theme.MESSAGE_BORDER_INFORMATION) == null) {
            Border darkBorder = new MatteBorder(0, 1, 1, 1, ui.getColor(Theme.MESSAGE_BORDER_EXTERIOR));
            Border lightBorder = new MatteBorder(0, 1, 1, 1, ui.getColor(Theme.MESSAGE_BORDER_EDGE));
            Border midBorder = new MatteBorder(0, 4, 4, 4, ui.getColor(Theme.MESSAGE_BORDER_MAIN));
            ui.put(
                    Theme.MESSAGE_BORDER_INFORMATION,
                    JUtilities.createCompoundBorder(darkBorder, lightBorder, midBorder, lightBorder, darkBorder)
            );
        }
    }

    private static void installFallbackColour(UIDefaults ui, String key, String sourceKey, int defaultColor) {
        if (ui.getColor(key) == null) {
            Color c = ui.getColor(sourceKey);
            if (c == null) {
                c = new Color(defaultColor);
            }
            if (c instanceof ColorUIResource) {
                c = new Color(c.getRGB(), true);
            }
            ui.put(key, c);
        }
    }

    /**
     * This method will ensure that some kind of basic, familiar look and feel
     * is installed. If the theme was successfully installed, it does nothing.
     * Otherwise it will try to install one or more fallback look and feels.
     */
    public static void ensureBaselineLookAndFeelInstalled() {
        if (installed == null && !installedBaseline) {
            for (UIManager.LookAndFeelInfo lafi : UIManager.getInstalledLookAndFeels()) {
                if (lafi.getName().equals("Nimbus")) {
                    try {
                        UIManager.setLookAndFeel(lafi.getClassName());
                        installStrangeEonsUIDefaults();
                        installStrangeEonsUIFallbackDefaults();
                        return;
                    } catch (Throwable t) {
                        // do nothing, will eventually fall back on system LaF
                        StrangeEons.log.log(Level.WARNING, null, t);
                    }
                }
            }
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Throwable t) {
                StrangeEons.log.log(Level.WARNING, null, t);
            }
            // We tried our best... use whatever default LaF is already installed
            installedBaseline = true;
        }
    }
    private static boolean installedBaseline;

    private ThemeInstaller() {
    }
}
