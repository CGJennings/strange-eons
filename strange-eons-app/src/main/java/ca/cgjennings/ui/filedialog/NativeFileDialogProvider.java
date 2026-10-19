package ca.cgjennings.ui.filedialog;

import ca.cgjennings.platform.PlatformSupport;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Window;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Objects;
import java.util.function.Function;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

/**
 * Shows file dialogs using the operating system's own dialogs through
 * {@link FileDialog}. Folder selection is only available natively on macOS;
 * elsewhere, folder requests are passed to a fallback provider.
 */
public final class NativeFileDialogProvider implements FileDialogProvider {

    /**
     * Operating system families that differ in {@link FileDialog} support.
     */
    public enum Platform {
        MAC, WINDOWS, OTHER;

        public static Platform current() {
            return of(PlatformSupport.PLATFORM_IS_MAC, PlatformSupport.PLATFORM_IS_WINDOWS);
        }

        static Platform of(boolean mac, boolean windows) {
            if (mac) {
                return MAC;
            }
            return windows ? WINDOWS : OTHER;
        }
    }

    static final String MAC_DIRECTORIES_PROPERTY = "apple.awt.fileDialogForDirectories";

    /**
     * The subset of {@link FileDialog} used by this provider.
     */
    interface NativeDialog {

        void setTitle(String title);

        void setMode(int mode);

        void setDirectory(String dir);

        void setFile(String file);

        void setFilenameFilter(FilenameFilter filter);

        void setMultipleMode(boolean enable);

        /**
         * Shows the dialog modally.
         */
        void show();

        File[] getFiles();

        String getDirectory();
    }

    private final Platform platform;
    private final FileDialogProvider directoryFallback;
    private final Function<Window, NativeDialog> dialogFactory;

    /**
     * Creates a provider for the current platform.
     *
     * @param directoryFallback shows folder requests where the platform
     * dialog cannot select folders
     */
    public NativeFileDialogProvider(FileDialogProvider directoryFallback) {
        this(Platform.current(), directoryFallback, NativeFileDialogProvider::createDialog);
    }

    NativeFileDialogProvider(Platform platform, FileDialogProvider directoryFallback, Function<Window, NativeDialog> dialogFactory) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.directoryFallback = Objects.requireNonNull(directoryFallback, "directoryFallback");
        this.dialogFactory = Objects.requireNonNull(dialogFactory, "dialogFactory");
    }

    @Override
    public FileDialogResult show(Component parent, FileDialogRequest request) {
        if (request.isDirectoriesOnly() && platform != Platform.MAC) {
            return directoryFallback.show(parent, request);
        }

        NativeDialog d = dialogFactory.apply(windowFor(parent));
        configure(d, request);

        if (!request.isDirectoriesOnly()) {
            d.show();
        } else {
            String old = System.getProperty(MAC_DIRECTORIES_PROPERTY);
            System.setProperty(MAC_DIRECTORIES_PROPERTY, "true");
            try {
                d.show();
            } finally {
                if (old == null) {
                    System.clearProperty(MAC_DIRECTORIES_PROPERTY);
                } else {
                    System.setProperty(MAC_DIRECTORIES_PROPERTY, old);
                }
            }
        }
        return result(d, request);
    }

    /**
     * Returns {@code true}: the native save dialogs of all supported
     * platforms ask before replacing a file.
     */
    @Override
    public boolean confirmsReplace() {
        return true;
    }

    void configure(NativeDialog d, FileDialogRequest request) {
        boolean save = request.getMode() == FileDialogRequest.Mode.SAVE;
        d.setMode(save ? FileDialog.SAVE : FileDialog.LOAD);
        if (request.getTitle() != null) {
            d.setTitle(request.getTitle());
        }
        d.setMultipleMode(request.isMultiSelection() && !save);

        File dir = request.getDirectory();
        File sel = request.getSelectedFile();
        if (sel != null && sel.getParentFile() != null) {
            dir = sel.getParentFile();
        }
        if (dir != null) {
            d.setDirectory(dir.getAbsolutePath());
        }

        // in folder mode a filter would also hide folders the user must
        // navigate through, so the selection is validated afterwards instead
        FileFilter filter = request.isDirectoriesOnly() ? null : request.getFilter();
        if (filter != null) {
            d.setFilenameFilter(toFilenameFilter(filter));
        }
        if (sel != null) {
            d.setFile(sel.getName());
        } else if (platform == Platform.WINDOWS && !save) {
            // Windows ignores FilenameFilter but accepts a wildcard pattern
            String pattern = windowsPattern(filter);
            if (pattern != null) {
                d.setFile(pattern);
            }
        }
    }

    FileDialogResult result(NativeDialog d, FileDialogRequest request) {
        File[] files = d.getFiles();
        if (files == null || files.length == 0) {
            return null;
        }
        if (!request.isMultiSelection() && files.length > 1) {
            files = new File[]{files[0]};
        }
        File dir;
        if (request.isDirectoriesOnly()) {
            dir = files[0];
        } else if (d.getDirectory() != null) {
            dir = new File(d.getDirectory());
        } else {
            dir = files[0].getParentFile();
        }
        return new FileDialogResult(files, dir);
    }

    static FilenameFilter toFilenameFilter(FileFilter filter) {
        return (dir, name) -> filter.accept(new File(dir, name));
    }

    /**
     * Returns a Windows wildcard pattern matching the filter's extensions, or
     * {@code null} if the filter is not extension based.
     */
    static String windowsPattern(FileFilter filter) {
        String[] exts = FileFilters.extensions(filter);
        if (exts == null) {
            return null;
        }
        StringBuilder b = new StringBuilder();
        for (String ext : exts) {
            if (b.length() > 0) {
                b.append(';');
            }
            b.append("*.").append(ext);
        }
        return b.toString();
    }

    static Window windowFor(Component parent) {
        if (parent == null) {
            return null;
        }
        if (parent instanceof Window) {
            return (Window) parent;
        }
        return SwingUtilities.getWindowAncestor(parent);
    }

    static NativeDialog createDialog(Window owner) {
        FileDialog fd;
        if (owner instanceof Frame) {
            fd = new FileDialog((Frame) owner);
        } else if (owner instanceof Dialog) {
            fd = new FileDialog((Dialog) owner);
        } else {
            fd = new FileDialog((Frame) null);
        }
        return new AwtDialog(fd);
    }

    static final class AwtDialog implements NativeDialog {

        final FileDialog fd;

        AwtDialog(FileDialog fd) {
            this.fd = fd;
        }

        @Override
        public void setTitle(String title) {
            fd.setTitle(title);
        }

        @Override
        public void setMode(int mode) {
            fd.setMode(mode);
        }

        @Override
        public void setDirectory(String dir) {
            fd.setDirectory(dir);
        }

        @Override
        public void setFile(String file) {
            fd.setFile(file);
        }

        @Override
        public void setFilenameFilter(FilenameFilter filter) {
            fd.setFilenameFilter(filter);
        }

        @Override
        public void setMultipleMode(boolean enable) {
            fd.setMultipleMode(enable);
        }

        @Override
        public void show() {
            fd.setVisible(true);
            fd.dispose();
        }

        @Override
        public File[] getFiles() {
            return fd.getFiles();
        }

        @Override
        public String getDirectory() {
            return fd.getDirectory();
        }
    }
}
