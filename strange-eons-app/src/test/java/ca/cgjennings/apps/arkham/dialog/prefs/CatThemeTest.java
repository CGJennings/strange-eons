package ca.cgjennings.apps.arkham.dialog.prefs;

import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import ca.cgjennings.apps.arkham.plugins.InstalledTheme;
import ca.cgjennings.ui.theme.Theme;
import ca.cgjennings.ui.theme.ThemeInstaller;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import resources.Settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class CatThemeTest {

    private static final String KEY = "use-native-file-dialogs";
    private static final String RECENT_KEY = "recent-file-menu-length";

    @Test
    void nativeFileDialogsOffByDefault() throws Exception {
        Properties p = new Properties();
        try (InputStream in = CatThemeTest.class.getResourceAsStream("/resources/default.settings")) {
            assertNotNull(in);
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        assertEquals("no", p.getProperty(KEY));
    }

    @Test
    void nativeFileDialogsCheckRoundTrips() {
        assumeFalse(GraphicsEnvironment.isHeadless());
        Settings user = Settings.getUser();
        String old = user.get(KEY);
        String oldRecent = user.get(RECENT_KEY);
        try (MockedStatic<BundleInstaller> bi = mockStatic(BundleInstaller.class);
                MockedStatic<ThemeInstaller> ti = mockStatic(ThemeInstaller.class)) {
            bi.when(BundleInstaller::getInstalledThemes).thenReturn(new InstalledTheme[0]);
            ti.when(ThemeInstaller::getInstalledTheme).thenReturn(mock(Theme.class));
            CatTheme cat = new CatTheme();

            user.set(RECENT_KEY, "5");
            user.set(KEY, "yes");
            cat.loadSettings();
            assertTrue(cat.nativeFileDialogsCheck.isSelected());
            cat.nativeFileDialogsCheck.setSelected(false);
            cat.storeSettings();
            assertEquals("no", user.get(KEY));

            cat.loadSettings();
            assertFalse(cat.nativeFileDialogsCheck.isSelected());
            cat.nativeFileDialogsCheck.setSelected(true);
            cat.storeSettings();
            assertEquals("yes", user.get(KEY));
        } finally {
            restore(user, KEY, old);
            restore(user, RECENT_KEY, oldRecent);
        }
    }

    private static void restore(Settings s, String key, String value) {
        if (value == null) {
            s.reset(key);
        } else {
            s.set(key, value);
        }
    }
}
