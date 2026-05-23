package ca.cgjennings.ui.theme;

import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;

/**
 * A theme that performs no customization. The look and feel is left at
 * whatever Swing initialized — typically the JDK's cross-platform Metal
 * L&amp;F — and {@link #applyThemeToImage} / {@link #applyThemeToColor}
 * pass their inputs through unchanged.
 *
 * <p>Selectable via the {@code theme} setting (set to this class's name)
 * or the {@code --xDisableTheme} command-line option.
 *
 * @since 3.5
 */
public class NoTheme extends Theme {

    public NoTheme() {
    }

    @Override
    public String getThemeName() {
        return "None";
    }

    @Override
    public String getLookAndFeelClassName() {
        return null;
    }

    @Override
    public LookAndFeel createLookAndFeelInstance() {
        return null;
    }

    @Override
    public void modifyManagerDefaults(UIDefaults defaults) {
        // Drop the hard-coded SE chrome overrides so the L&F's own colors
        // come through (project tree header, sidepanel titles, editor tab
        // background). Legacy themes that rely on them are untouched.
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
