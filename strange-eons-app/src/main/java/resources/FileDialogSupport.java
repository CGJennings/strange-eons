package resources;

import ca.cgjennings.apps.arkham.project.Project;
import ca.cgjennings.ui.filedialog.FileDialogProvider;
import ca.cgjennings.ui.filedialog.FileDialogRequest;
import ca.cgjennings.ui.filedialog.FileDialogResult;
import ca.cgjennings.ui.filedialog.FileFilters;
import ca.cgjennings.ui.filedialog.NativeFileDialogProvider;
import ca.cgjennings.ui.filedialog.NativeFileDialogProvider.Platform;
import ca.cgjennings.ui.filedialog.NfdFileDialogProvider;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

/**
 * Shows the resource kit's file dialogs: remembers the folder used for each
 * kind of dialog and confirms replacing files when the dialog does not.
 */
final class FileDialogSupport {

    /**
     * Stores the last folder used for each kind of dialog.
     */
    interface FolderStore {

        String get(String key);

        void set(String key, String path);
    }

    /**
     * Asks whether to replace an existing file.
     */
    interface ReplaceConfirmer {

        /**
         * @return {@link JOptionPane#YES_OPTION}, {@link JOptionPane#NO_OPTION}
         * to choose another file, or {@link JOptionPane#CANCEL_OPTION}
         */
        int confirm(File f);
    }

    private final FolderStore folders;
    private final ReplaceConfirmer confirmer;

    FileDialogSupport(FolderStore folders, ReplaceConfirmer confirmer) {
        this.folders = Objects.requireNonNull(folders, "folders");
        this.confirmer = Objects.requireNonNull(confirmer, "confirmer");
    }

    /**
     * Chooses the provider for a dialog.
     *
     * @param swing the built-in dialog for this kind of request
     * @param useNative whether native dialogs are enabled
     * @param platform the current platform
     * @param nfd supplies the shared nativefiledialog provider
     */
    static FileDialogProvider select(FileDialogProvider swing, boolean useNative, Platform platform, Supplier<NfdFileDialogProvider> nfd) {
        if (!useNative) {
            return swing;
        }
        FileDialogProvider awt = new NativeFileDialogProvider(swing);
        if (platform == Platform.MAC) {
            return awt;
        }
        return nfd.get().withFallback(awt);
    }

    /**
     * Shows an open dialog starting in the folder stored under the key.
     *
     * @param remember whether to store the folder of the selection
     * @return the selection, or {@code null} if cancelled
     */
    FileDialogResult open(Component parent, FileDialogProvider provider, String key, boolean remember, FileDialogRequest request) {
        startIn(key, request);
        FileDialogResult r = provider.show(parent, request);
        if (r != null && remember) {
            remember(key, r.getDirectory());
        }
        return r;
    }

    /**
     * Shows a save dialog starting in the folder stored under the key. If the
     * name lacks one of the filter's extensions, the first is added. If the
     * file exists and the dialog did not already ask, the user confirms
     * replacing it.
     *
     * @param unconfirmed a file that may be replaced without asking, or
     * {@code null}
     * @return the file to write, or {@code null} if cancelled
     */
    File save(Component parent, FileDialogProvider provider, String key, File unconfirmed, FileDialogRequest request) {
        startIn(key, request);
        for (;;) {
            FileDialogResult r = provider.show(parent, request);
            if (r == null) {
                return null;
            }
            File chosen = r.getFile();
            File f = withExtension(chosen, request.getFilter());
            boolean asked = provider.confirmsReplace() && f.equals(chosen);
            if (f.exists() && !asked && !f.equals(unconfirmed)) {
                int answer = confirmer.confirm(f);
                if (answer == JOptionPane.CANCEL_OPTION) {
                    return null;
                }
                if (answer == JOptionPane.NO_OPTION) {
                    request.selectedFile(f);
                    continue;
                }
            }
            remember(key, r.getDirectory());
            return f;
        }
    }

    /**
     * Stores a folder under the key.
     */
    void remember(String key, File dir) {
        if (dir != null) {
            folders.set(key, dir.getAbsolutePath());
        }
    }

    private void startIn(String key, FileDialogRequest request) {
        if (request.getDirectory() == null) {
            String dir = folders.get(key);
            if (dir != null) {
                request.directory(new File(dir));
            }
        }
    }

    /**
     * The name of the settings file that marks a project folder.
     */
    static final String PROJECT_MARKER = "seproject";

    /**
     * Accepts folders, project settings files, and packaged projects, for
     * dialogs that open a project by choosing a file.
     */
    static final FileFilter PROJECT_FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(File f) {
            return f.isDirectory()
                    || f.getName().equals(PROJECT_MARKER)
                    || f.getName().endsWith('.' + PROJECT_MARKER);
        }

        @Override
        public String getDescription() {
            return PROJECT_MARKER;
        }
    };

    /**
     * Returns the project to open for a file chosen in a project dialog: the
     * folder containing a project settings file, or a packaged project.
     *
     * @param chosen the chosen file, which may also be a project folder
     * @return the project folder or package, or {@code null} if the file does
     * not identify a project
     * @throws IOException if the project settings cannot be read
     */
    static File projectFor(File chosen) throws IOException {
        if (Project.isProjectPackage(chosen)) {
            return chosen;
        }
        File folder = chosen.isDirectory() ? chosen : chosen.getParentFile();
        if (folder != null && Project.isProjectFolder(folder)) {
            return folder;
        }
        return null;
    }

    /**
     * Returns the file with the filter's first extension added, unless its
     * name already ends with one of the filter's extensions.
     */
    static File withExtension(File f, FileFilter filter) {
        String[] exts = FileFilters.extensions(filter);
        if (exts == null) {
            return f;
        }
        String name = f.getName().toLowerCase(Locale.ROOT);
        for (String ext : exts) {
            if (name.endsWith('.' + ext.toLowerCase(Locale.ROOT))) {
                return f;
            }
        }
        return new File(f.getParentFile(), f.getName() + '.' + exts[0]);
    }
}
