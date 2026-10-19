package ca.cgjennings.ui.filedialog;

import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.filedialog.NativeFileDialogProvider.Platform;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class LwjglNfdBackendTest {

    @Test
    void notUsedOnMac() {
        assertEquals("not used on macOS", new LwjglNfdBackend(Platform.MAC).init());
    }

    @Test
    void reportsMissingNativesOnMac() {
        // no macOS natives are bundled, so loading the library fails
        assumeTrue(PlatformSupport.PLATFORM_IS_MAC);
        assertNotNull(new LwjglNfdBackend(Platform.WINDOWS).init());
        assertNotNull(new LwjglNfdBackend(Platform.OTHER).init());
    }

    @Test
    void windowWithoutHandle() {
        assertEquals(0L, JawtWindowHandles.handleOf(null));
        assumeFalse(GraphicsEnvironment.isHeadless());
        Frame f = new Frame();
        try {
            assertEquals(0L, JawtWindowHandles.handleOf(f));
        } finally {
            f.dispose();
        }
    }
}
