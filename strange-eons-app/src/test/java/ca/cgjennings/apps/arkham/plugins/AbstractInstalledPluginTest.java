package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.ui.theme.ThemedIcon;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractInstalledPluginTest {

    /**
     * Regression: opening Plugin Manager threw NullPointerException after
     * the user disabled a tool plug-in. collectPluginInfo() probes the
     * plug-in to cache its metadata, then stops it again because the
     * plug-in is reloadable, leaving the plugin field null. getIcon() must
     * fall through to the bundle/default icon instead of dereferencing it.
     */
    @Test
    void getIconDoesNotNpeWhenPluginInstanceIsStopped() throws Exception {
        InstalledPlugin ip = new InstalledPlugin(null, NoopPlugin.class.getName());
        ip.getName();

        ThemedIcon icon = assertDoesNotThrow(ip::getIcon);
        assertNotNull(icon);
    }

    /**
     * After the info probe stops a reloadable plug-in, getIcon() should
     * still return the icon the plug-in supplied during the probe rather
     * than the generic bundle fallback. This is what allows Plugin Manager
     * to display a meaningful icon next to a disabled plug-in.
     */
    @Test
    void getIconReturnsCapturedIconAfterProbe() throws Exception {
        ThemedIcon expected = mock(ThemedIcon.class);
        when(expected.small()).thenReturn(expected);
        IconPlugin.iconForNextInstance = expected;

        InstalledPlugin ip = new InstalledPlugin(null, IconPlugin.class.getName());
        ip.getName();

        assertSame(expected, ip.getIcon());
    }

    @AfterEach
    void clearIconHandoff() {
        IconPlugin.iconForNextInstance = null;
    }

    public static class NoopPlugin implements Plugin {

        @Override
        public boolean initializePlugin(PluginContext context) {
            return true;
        }

        @Override
        public void unloadPlugin() {
        }

        @Override
        public String getPluginName() {
            return "Test";
        }

        @Override
        public String getPluginDescription() {
            return "Test";
        }

        @Override
        public float getPluginVersion() {
            return 1.0f;
        }

        @Override
        public int getPluginType() {
            return ACTIVATED;
        }

        @Override
        public void showPlugin(PluginContext context, boolean show) {
        }

        @Override
        public boolean isPluginShowing() {
            return false;
        }

        @Override
        public boolean isPluginUsable() {
            return true;
        }

        @Override
        public ThemedIcon getPluginIcon() {
            return null;
        }

        @Override
        public String getDefaultAcceleratorKey() {
            return null;
        }
    }

    public static final class IconPlugin extends NoopPlugin {

        static ThemedIcon iconForNextInstance;
        private final ThemedIcon icon = iconForNextInstance;

        @Override
        public ThemedIcon getPluginIcon() {
            return icon;
        }
    }
}
