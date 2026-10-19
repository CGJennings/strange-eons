package ca.cgjennings.ui.filedialog;

import ca.cgjennings.ui.filedialog.NativeFileDialogProvider.Platform;
import ca.cgjennings.ui.filedialog.NfdFileDialogProvider.Filter;
import ca.cgjennings.ui.filedialog.NfdFileDialogProvider.Outcome;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.nfd.NFDFilterItem;
import org.lwjgl.util.nfd.NFDOpenDialogArgs;
import org.lwjgl.util.nfd.NFDPathSetEnum;
import org.lwjgl.util.nfd.NFDPickFolderArgs;
import org.lwjgl.util.nfd.NFDSaveDialogArgs;
import org.lwjgl.util.nfd.NFDWindowHandle;

import static org.lwjgl.util.nfd.NativeFileDialog.*;

/**
 * Calls nativefiledialog-extended through LWJGL. On Linux the desktop portal
 * build is used: the GTK build would share GTK with AWT across threads.
 */
final class LwjglNfdBackend implements NfdFileDialogProvider.Backend {

    private final Platform platform;

    LwjglNfdBackend() {
        this(Platform.current());
    }

    LwjglNfdBackend(Platform platform) {
        this.platform = platform;
    }

    @Override
    public String init() {
        if (platform == Platform.MAC) {
            return "not used on macOS";
        }
        try {
            if (platform == Platform.OTHER) {
                Configuration.NFD_LINUX_PORTAL.set(true);
            }
            return NFD_Init() == NFD_OKAY ? null : NFD_GetError();
        } catch (Throwable t) {
            return t.toString();
        }
    }

    @Override
    public Outcome open(long parent, List<Filter> filters, String defaultPath, boolean multiple) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NFDOpenDialogArgs args = NFDOpenDialogArgs.calloc(stack)
                    .filterList(filterList(stack, filters))
                    .parentWindow(windowHandle(stack, parent));
            if (defaultPath != null) {
                args.defaultPath(stack.UTF8(defaultPath));
            }
            PointerBuffer out = stack.mallocPointer(1);
            if (!multiple) {
                return single(NFD_OpenDialog_With(out, args), out);
            }
            int status = NFD_OpenDialogMultiple_With(out, args);
            if (status != NFD_OKAY) {
                return failure(status);
            }
            return Outcome.ok(pathSet(stack, out.get(0)));
        }
    }

    @Override
    public Outcome save(long parent, List<Filter> filters, String defaultPath, String defaultName) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NFDSaveDialogArgs args = NFDSaveDialogArgs.calloc(stack)
                    .filterList(filterList(stack, filters))
                    .parentWindow(windowHandle(stack, parent));
            if (defaultPath != null) {
                args.defaultPath(stack.UTF8(defaultPath));
            }
            if (defaultName != null) {
                args.defaultName(stack.UTF8(defaultName));
            }
            PointerBuffer out = stack.mallocPointer(1);
            return single(NFD_SaveDialog_With(out, args), out);
        }
    }

    @Override
    public Outcome pickFolder(long parent, String defaultPath) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NFDPickFolderArgs args = NFDPickFolderArgs.calloc(stack)
                    .parentWindow(windowHandle(stack, parent));
            if (defaultPath != null) {
                args.defaultPath(stack.UTF8(defaultPath));
            }
            PointerBuffer out = stack.mallocPointer(1);
            return single(NFD_PickFolder_With(out, args), out);
        }
    }

    private NFDWindowHandle windowHandle(MemoryStack stack, long parent) {
        int type;
        if (parent == 0L) {
            type = NFD_WINDOW_HANDLE_TYPE_UNSET;
        } else {
            type = platform == Platform.WINDOWS ? NFD_WINDOW_HANDLE_TYPE_WINDOWS : NFD_WINDOW_HANDLE_TYPE_X11;
        }
        return NFDWindowHandle.malloc(stack).type(type).handle(parent);
    }

    private static NFDFilterItem.Buffer filterList(MemoryStack stack, List<Filter> filters) {
        if (filters.isEmpty()) {
            return null;
        }
        NFDFilterItem.Buffer list = NFDFilterItem.malloc(filters.size(), stack);
        for (int i = 0; i < filters.size(); ++i) {
            list.get(i)
                    .name(stack.UTF8(filters.get(i).name()))
                    .spec(stack.UTF8(filters.get(i).spec()));
        }
        return list;
    }

    private static Outcome single(int status, PointerBuffer out) {
        if (status != NFD_OKAY) {
            return failure(status);
        }
        String path = out.getStringUTF8(0);
        NFD_FreePath(out.get(0));
        return Outcome.ok(List.of(path));
    }

    private static Outcome failure(int status) {
        return status == NFD_CANCEL ? Outcome.cancel() : Outcome.error(NFD_GetError());
    }

    private static List<String> pathSet(MemoryStack stack, long set) {
        List<String> paths = new ArrayList<>();
        NFDPathSetEnum e = NFDPathSetEnum.calloc(stack);
        NFD_PathSet_GetEnum(set, e);
        PointerBuffer p = stack.mallocPointer(1);
        while (NFD_PathSet_EnumNext(e, p) == NFD_OKAY && p.get(0) != 0L) {
            paths.add(p.getStringUTF8(0));
            NFD_PathSet_FreePath(p.get(0));
        }
        NFD_PathSet_FreeEnum(e);
        NFD_PathSet_Free(set);
        return paths;
    }
}
