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
public class FlatTheme extends Theme {
    public FlatTheme() {
    }

    protected boolean dark;

    @Override
    public String getLookAndFeelClassName() {
        return "com.formdev.flatlaf.Flat"
                + (dark ? "Dark" : "Light")
                + "Laf";
    }

    @Override
    public void modifyManagerDefaults(UIDefaults defaults) {
        UIManager.put( "Button.arc", 999 );
        UIManager.put( "ScrollBar.width", 12 );
        UIManager.put( "ScrollBar.thumbArc", 999 );
        UIManager.put( "ScrollBar.thumbInsets", new Insets( 2, 2, 2, 2 ) );
        UIManager.put( "TabbedPane.showTabSeparators", true );
        UIManager.put( "TabbedPane.tabSeparatorsFullHeight", true );
        UIManager.put( "TabbedPane.selectedBackground", dark ? Color.BLACK : Color.WHITE);
        UIManager.put(EDITOR_TAB_BACKGROUND, null);
        UIManager.put(NOTES_BACKGROUND, null);
        UIManager.put(PROJECT_FIND_BACKGROUND, null);
        UIManager.put(PROJECT_FIND_FOREGROUND, null);
    }

    @Override
    public void modifyLookAndFeelDefaults(UIDefaults defaults) {
    }

//    @Override
//    public String getThemeDescription() {
//        return super.getThemeDescription(); //To change body of generated methods, choose Tools | Templates.
//    }
    @Override
    public boolean isDarkOnLight() {
        return dark;
    }
}
