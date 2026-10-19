package ca.cgjennings.ui.filedialog;

import ca.cgjennings.platform.PlatformSupport;
import java.awt.Window;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.jawt.JAWT;
import org.lwjgl.system.jawt.JAWTDrawingSurface;
import org.lwjgl.system.jawt.JAWTDrawingSurfaceInfo;
import org.lwjgl.system.jawt.JAWTWin32DrawingSurfaceInfo;
import org.lwjgl.system.jawt.JAWTX11DrawingSurfaceInfo;

import static org.lwjgl.system.jawt.JAWTFunctions.*;

/**
 * Looks up the native handle of an AWT window: an HWND on Windows or an X11
 * window ID elsewhere.
 */
final class JawtWindowHandles {

    private JawtWindowHandles() {
    }

    /**
     * Returns the window's native handle, or 0 if it has none or it cannot be
     * determined.
     */
    static long handleOf(Window w) {
        if (w == null || !w.isDisplayable() || PlatformSupport.PLATFORM_IS_MAC) {
            return 0L;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            JAWT awt = JAWT.calloc(stack).version(JAWT_VERSION_9);
            if (!JAWT_GetAWT(awt)) {
                return 0L;
            }
            JAWTDrawingSurface ds = JAWT_GetDrawingSurface(w, awt.GetDrawingSurface());
            if (ds == null) {
                return 0L;
            }
            try {
                if ((JAWT_DrawingSurface_Lock(ds, ds.Lock()) & JAWT_LOCK_ERROR) != 0) {
                    return 0L;
                }
                try {
                    JAWTDrawingSurfaceInfo dsi = JAWT_DrawingSurface_GetDrawingSurfaceInfo(ds, ds.GetDrawingSurfaceInfo());
                    if (dsi == null) {
                        return 0L;
                    }
                    try {
                        return PlatformSupport.PLATFORM_IS_WINDOWS
                                ? JAWTWin32DrawingSurfaceInfo.create(dsi.platformInfo()).hwnd()
                                : JAWTX11DrawingSurfaceInfo.create(dsi.platformInfo()).drawable();
                    } finally {
                        JAWT_DrawingSurface_FreeDrawingSurfaceInfo(dsi, ds.FreeDrawingSurfaceInfo());
                    }
                } finally {
                    JAWT_DrawingSurface_Unlock(ds, ds.Unlock());
                }
            } finally {
                JAWT_FreeDrawingSurface(ds, awt.FreeDrawingSurface());
            }
        } catch (Throwable t) {
            return 0L;
        }
    }
}
