package ca.cgjennings.ui.filedialog;

import java.io.File;
import java.util.Objects;

/**
 * The outcome of an approved file dialog.
 */
public final class FileDialogResult {

    private final File[] files;
    private final File directory;

    public FileDialogResult(File[] files, File directory) {
        Objects.requireNonNull(files, "files");
        if (files.length == 0) {
            throw new IllegalArgumentException("no files selected");
        }
        this.files = files.clone();
        this.directory = directory;
    }

    /**
     * Returns the first selected file.
     */
    public File getFile() {
        return files[0];
    }

    public File[] getFiles() {
        return files.clone();
    }

    /**
     * Returns the folder that should be offered the next time a similar dialog
     * is shown.
     */
    public File getDirectory() {
        return directory;
    }
}
