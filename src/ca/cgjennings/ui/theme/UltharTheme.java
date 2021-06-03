package ca.cgjennings.ui.theme;

import java.awt.Color;
import java.awt.Insets;
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
        return "com.formdev.flatlaf.Flat"
                + (isDark() ? "Dark" : "Light")
                + "Laf";
    }

    @Override
    public void modifyManagerDefaults(UIDefaults defaults) {
        final boolean dark = isDark();
        final Color DEEP_GREY = new Color(0x333333);
        UIManager.put( "Button.arc", 999 );
        UIManager.put( "ScrollBar.width", 12 );
        UIManager.put( "ScrollBar.thumbArc", 999 );
        UIManager.put( "ScrollBar.thumbInsets", new Insets( 2, 2, 2, 2 ) );
        UIManager.put( "TabbedPane.showTabSeparators", true );
        UIManager.put( "TabbedPane.tabSeparatorsFullHeight", true );
        UIManager.put(ALTERNATE_DOCUMENT_TAB_ORIENTATION, true);
        UIManager.put(NOTES_BACKGROUND, null);
        UIManager.put(EDITOR_TAB_BACKGROUND, null);
        if(dark) {
            UIManager.put(PROJECT_HEADER_BACKGROUND, DEEP_GREY);
            UIManager.put(PROJECT_FIND_BACKGROUND, null);
        } else {
            final Color DIVIDER = new Color(0xe7e7e7);
            UIManager.put("SplitPane.background", DIVIDER);
            UIManager.put(PROJECT_HEADER_BACKGROUND, DIVIDER);
            UIManager.put(PROJECT_HEADER_FOREGROUND, DEEP_GREY);
            UIManager.put(PROJECT_FIND_BACKGROUND, null);
            UIManager.put(PROJECT_FIND_FOREGROUND, null);
        }
    }

    @Override
    public void modifyLookAndFeelDefaults(UIDefaults defaults) {
    }

    @Override
    public boolean isDark() {
        return false;
    }
}
