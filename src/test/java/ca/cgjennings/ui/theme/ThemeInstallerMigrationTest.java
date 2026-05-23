package ca.cgjennings.ui.theme;

import org.junit.jupiter.api.Test;
import resources.Settings;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThemeInstallerMigrationTest {

    private static final String DAGON = DagonTheme.class.getName();
    private static final String HYDRA = HydraTheme.class.getName();

    @Test
    void darkThemeYuggothOverrideIsRewrittenToDagon() {
        Settings s = new Settings();
        s.set("dark-theme", ThemeInstaller.LEGACY_YUGGOTH_THEME_CLASS);

        ThemeInstaller.migrateLegacyThemePreferences(s);

        assertEquals(DAGON, s.getOverride("dark-theme"));
    }

    @Test
    void lightThemeYuggothOverrideIsRewrittenToDagon() {
        Settings s = new Settings();
        s.set("theme", ThemeInstaller.LEGACY_YUGGOTH_THEME_CLASS);

        ThemeInstaller.migrateLegacyThemePreferences(s);

        assertEquals(DAGON, s.getOverride("theme"));
    }

    @Test
    void bothKeysMigrateIndependentlyInOnePass() {
        Settings s = new Settings();
        s.set("theme", ThemeInstaller.LEGACY_YUGGOTH_THEME_CLASS);
        s.set("dark-theme", ThemeInstaller.LEGACY_YUGGOTH_THEME_CLASS);

        ThemeInstaller.migrateLegacyThemePreferences(s);

        assertEquals(DAGON, s.getOverride("theme"));
        assertEquals(DAGON, s.getOverride("dark-theme"));
    }

    @Test
    void nonLegacyOverrideIsLeftAlone() {
        Settings s = new Settings();
        s.set("theme", HYDRA);
        s.set("dark-theme", DAGON);

        ThemeInstaller.migrateLegacyThemePreferences(s);

        assertEquals(HYDRA, s.getOverride("theme"));
        assertEquals(DAGON, s.getOverride("dark-theme"));
    }
}
