package ca.cgjennings.ui.filedialog;

import java.io.File;
import java.util.Objects;
import javax.swing.filechooser.FileFilter;

/**
 * Describes a file dialog to be shown by a {@link FileDialogProvider}.
 */
public final class FileDialogRequest {

    /**
     * Whether the dialog selects existing files or a save destination.
     */
    public enum Mode {
        OPEN, SAVE
    }

    private final Mode mode;
    private String title;
    private String approveText;
    private FileFilter filter;
    private boolean multiSelection;
    private boolean directoriesOnly;
    private File directory;
    private File selectedFile;

    public FileDialogRequest(Mode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    public Mode getMode() {
        return mode;
    }

    public String getTitle() {
        return title;
    }

    public FileDialogRequest title(String title) {
        this.title = title;
        return this;
    }

    public String getApproveText() {
        return approveText;
    }

    public FileDialogRequest approveText(String approveText) {
        this.approveText = approveText;
        return this;
    }

    public FileFilter getFilter() {
        return filter;
    }

    public FileDialogRequest filter(FileFilter filter) {
        this.filter = filter;
        return this;
    }

    public boolean isMultiSelection() {
        return multiSelection;
    }

    public FileDialogRequest multiSelection(boolean multiSelection) {
        this.multiSelection = multiSelection;
        return this;
    }

    public boolean isDirectoriesOnly() {
        return directoriesOnly;
    }

    public FileDialogRequest directoriesOnly(boolean directoriesOnly) {
        this.directoriesOnly = directoriesOnly;
        return this;
    }

    /**
     * Returns the folder the dialog starts in, or {@code null} to let the
     * provider decide.
     */
    public File getDirectory() {
        return directory;
    }

    public FileDialogRequest directory(File directory) {
        this.directory = directory;
        return this;
    }

    /**
     * Returns the suggested file; a bare name is resolved against the starting
     * folder.
     */
    public File getSelectedFile() {
        return selectedFile;
    }

    public FileDialogRequest selectedFile(File selectedFile) {
        this.selectedFile = selectedFile;
        return this;
    }
}
