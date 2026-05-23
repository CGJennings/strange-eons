package ca.cgjennings.ui.theme;

import java.awt.Color;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

/**
 * A Theme implementation that wraps the
 * <a href="https://github.com/JFormDesigner/FlatLaf">FlatLaf</a>.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.2
 */
public class UltharTheme extends Theme {

    public UltharTheme() {
    }

    @Override
    public String getLookAndFeelClassName() {
        return null;
    }

    @Override
    public LookAndFeel createLookAndFeelInstance() {
        if (isDark()) {
            return new com.formdev.flatlaf.FlatDarkLaf() {
                @Override
                public Icon getDisabledIcon(JComponent component, Icon icon) {
                    if (icon != null) {
                        icon = Theme.getDisabledIcon(component, icon);
                        if (icon == null) {
                            icon = super.getDisabledIcon(component, icon);
                        }
                    }
                    return icon;
                }
            };
        }
        return new com.formdev.flatlaf.FlatLightLaf() {
            @Override
            public Icon getDisabledIcon(JComponent component, Icon icon) {
                if (icon != null) {
                    icon = Theme.getDisabledIcon(component, icon);
                    if (icon == null) {
                        icon = super.getDisabledIcon(component, icon);
                    }
                }
                return icon;
            }
        };
    }
    
    

    @Override
    public void modifyManagerDefaults(UIDefaults defaults) {
        final boolean dark = isDark();
        UIManager.put("Button.arc", 999);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("TabbedPane.tabSeparatorsFullHeight", true);
        UIManager.put(ALTERNATE_DOCUMENT_TAB_ORIENTATION, true);
        UIManager.put(NOTES_BACKGROUND, null);
        UIManager.put(EDITOR_TAB_BACKGROUND, null);
        if (dark) {
            final Color DEEPER_GREY = new Color(0x27292a);
            final Color BRIGHT_GREY = new Color(0xa0aba6);
            UIManager.put(PROJECT_HEADER_BACKGROUND, DEEPER_GREY);
            UIManager.put(PROJECT_FIND_BACKGROUND, DEEPER_GREY);
            UIManager.put(PLUGIN_README_BACKGROUND, DEEPER_GREY);
            UIManager.put(PLUGIN_README_FOREGROUND, BRIGHT_GREY);
            
            UIManager.put(CONSOLE_BACKROUND, new Color(0x1e1e1e));
            UIManager.put(CONSOLE_OUTPUT, new Color(0xd4d4d4));
            UIManager.put(CONSOLE_ERROR, new Color(0xdf44747));
            UIManager.put(CONSOLE_SELECTION_BACKGROUND, new Color(0x264f78));
            UIManager.put(CONSOLE_SELECTION_FOREGROUND, new Color(0xd4d4d4));
        } else {
            final Color DEEP_GREY = new Color(0x3c3f41);
            final Color DIVIDER = new Color(0xe7e7e7);
            UIManager.put("SplitPane.background", DIVIDER);
            UIManager.put(PROJECT_HEADER_BACKGROUND, DIVIDER);
            UIManager.put(PROJECT_HEADER_FOREGROUND, DEEP_GREY);
            UIManager.put(PROJECT_FIND_BACKGROUND, null);
            UIManager.put(PROJECT_FIND_FOREGROUND, null);
            
            UIManager.put(CONSOLE_BACKROUND, new Color(0xf7f7f7));
            UIManager.put(CONSOLE_OUTPUT, new Color(0x000000));
            UIManager.put(CONSOLE_ERROR, new Color(0x830404));
            UIManager.put(CONSOLE_SELECTION_BACKGROUND, new Color(0xb0c5e3));
            UIManager.put(CONSOLE_SELECTION_FOREGROUND, new Color(0x000000));
        }
    }

    @Override
    public void modifyLookAndFeelDefaults(UIDefaults defaults) {
    }

    @Override
    public String getThemeGroup() {
        return "\udbff\udfff_00";
    }
    
    @Override
    public boolean isDark() {
        return false;
    }
}
