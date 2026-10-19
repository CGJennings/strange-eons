package ca.cgjennings.ui.filedialog;

import java.awt.Component;
import java.io.File;
import java.util.Objects;
import javax.swing.JFileChooser;

/**
 * Shows file dialogs using a {@link JFileChooser}. The chooser is reused
 * between requests, so accessories such as previews remain attached.
 */
public final class SwingFileDialogProvider implements FileDialogProvider {

    private final JFileChooser chooser;

    public SwingFileDialogProvider(JFileChooser chooser) {
        this.chooser = Objects.requireNonNull(chooser, "chooser");
    }

    public JFileChooser getChooser() {
        return chooser;
    }

    @Override
    public FileDialogResult show(Component parent, FileDialogRequest request) {
        configure(request);
        int option = request.getMode() == FileDialogRequest.Mode.SAVE
                ? chooser.showSaveDialog(parent)
                : chooser.showOpenDialog(parent);
        if (option != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return result();
    }

    @Override
    public boolean confirmsReplace() {
        return false;
    }

    void configure(FileDialogRequest request) {
        chooser.setDialogType(request.getMode() == FileDialogRequest.Mode.SAVE
                ? JFileChooser.SAVE_DIALOG
                : JFileChooser.OPEN_DIALOG);
        chooser.setDialogTitle(request.getTitle());
        chooser.setApproveButtonText(request.getApproveText());
        chooser.setFileSelectionMode(request.isDirectoriesOnly()
                ? JFileChooser.DIRECTORIES_ONLY
                : JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(request.isMultiSelection());
        if (request.getFilter() != null) {
            chooser.setFileFilter(request.getFilter());
        }
        if (request.getDirectory() != null) {
            chooser.setCurrentDirectory(request.getDirectory());
        }
        if (request.getSelectedFile() != null) {
            chooser.setSelectedFile(request.getSelectedFile());
        }
    }

    FileDialogResult result() {
        File[] files = chooser.isMultiSelectionEnabled()
                ? chooser.getSelectedFiles()
                : new File[]{chooser.getSelectedFile()};
        if (files.length == 0 || files[0] == null) {
            return null;
        }
        File dir = chooser.getCurrentDirectory();
        if (chooser.isDirectorySelectionEnabled() && files[0].isDirectory()) {
            dir = files[0];
        }
        return new FileDialogResult(files, dir);
    }
}
