package ca.cgjennings.ui.theme;

import ca.cgjennings.platform.PlatformSupport;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import resources.Language;

/**
 * A fallback theme that uses the system look and feel. (This theme was
 * available in previous versions, but it was implemented virtually rather than
 * using a theme class.)
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class TchoTchoTheme extends Theme {

    public TchoTchoTheme() {
    }

    @Override
    public String getThemeName() {
        return Language.string("sd-theme-tcho-tcho-name");
    }

    /**
     * Called from {@link #modifyManagerDefaults} when the host operating system
     * is Windows. Allows the installation of platform-specific changes to the
     * system look and feel.
     */
    protected void patchWindows() {
        replacementClass = "net.java.plaf.windows.WindowsLookAndFeel";
        try {
            Class.forName(replacementClass);
        } catch (ClassNotFoundException e) {
            replacementClass = null;
        }
    }

    /**
     * Called from {@link #modifyManagerDefaults} when the host operating system
     * is OS X. Allows the installation of platform-specific changes to the
     * system look and feel.
     */
    protected void patchOSX() {
        UIManager.put("ComboBox.harmonizePreferredHeight", Boolean.TRUE);
    }

    /**
     * Called from {@link #modifyManagerDefaults} when the host operating system
     * is neither Windows nor OS X. Allows the installation of platform-specific
     * changes to the system look and feel.
     */
    protected void patchOther() {
    }

    @Override
    public void modifyManagerDefaults(UIDefaults defaults) {
        if (PlatformSupport.PLATFORM_IS_MAC) {
            patchOSX();
        } else if (PlatformSupport.PLATFORM_IS_WINDOWS) {
            patchWindows();
        } else {
            patchOther();
        }
    }

    @Override
    public void modifyLookAndFeelDefaults(UIDefaults defaults) {
    }

    @Override
    public String getLookAndFeelClassName() {
        if (replacementClass != null) {
            return replacementClass;
        }
        return UIManager.getSystemLookAndFeelClassName();
    }

    private String replacementClass;
    
    @Override
    public String getThemeGroup() {
        return "\udbff\udfff_zz";
    }        
}
