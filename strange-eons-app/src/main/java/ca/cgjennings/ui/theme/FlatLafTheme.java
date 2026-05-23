package ca.cgjennings.ui.theme;

import javax.swing.UIDefaults;

/**
 * Base class for themes that adopt one of the look-and-feels bundled with
 * FlatLaf without further customization. Concrete subclasses supply the
 * display name, group, FlatLaf L&amp;F class, and dark/light flag through
 * the constructor.
 *
 * @since 3.5
 */
abstract class FlatLafTheme extends Theme {

    private final String name;
    private final String group;
    private final String lafClassName;
    private final boolean dark;

    protected FlatLafTheme(String name, String group, String lafClassName, boolean dark) {
        this.name = name;
        this.group = group;
        this.lafClassName = lafClassName;
        this.dark = dark;
    }

    @Override
    public final String getThemeName() {
        return name;
    }

    @Override
    public final String getThemeGroup() {
        return group;
    }

    @Override
    public final String getLookAndFeelClassName() {
        return lafClassName;
    }

    @Override
    public final boolean isDark() {
        return dark;
    }

    @Override
    public void modifyManagerDefaults(UIDefaults defaults) {
        // ThemeInstaller seeds these SE-specific keys with hard-coded
        // colors (black project header, blue editor-tab background, etc.).
        // Clear them so the project tree header and the Properties/Notes
        // tab pane fall through to FlatLaf's natural chrome.
        defaults.put(Theme.PROJECT_HEADER_BACKGROUND, null);
        defaults.put(Theme.PROJECT_HEADER_FOREGROUND, null);
        defaults.put(Theme.SIDEPANEL_TITLE_BACKGROUND, null);
        defaults.put(Theme.SIDEPANEL_TITLE_FOREGROUND, null);
        defaults.put(Theme.EDITOR_TAB_BACKGROUND, null);
    }

    @Override
    public void modifyLookAndFeelDefaults(UIDefaults defaults) {
    }
}
