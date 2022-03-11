package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.project.ProjectFolderDialog;
import ca.cgjennings.platform.DesktopIntegration;
import java.awt.Component;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import resources.Language;

/**
 * An export container that writes files to a folder.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class FolderExportContainer implements ExportContainer {

    private File parent;
    private LinkedList<File> written;

    private void checkClosed() {
        if (written != null) {
            throw new IllegalStateException("last container not closed");
        }
    }

    private void checkOpen() {
        if (written == null) {
            throw new IllegalStateException("no container created");
        }
    }

    @Override
    public String getIdentifier() {
        return "folder";
    }

    @Override
    public boolean selectLocation(String baseName, Component locationHint) {
        checkClosed();
        ProjectFolderDialog fd = new ProjectFolderDialog(locationHint, ProjectFolderDialog.Mode.SELECT_FOLDER);
        fd.useSettingKey("default-export-folder");
        fd.setSuggestedFolderName(baseName);
        File folder = fd.showDialog();

        if (folder == null) {
            return false;
        }
        if (!folder.exists()) {
            folder.mkdirs();
        }
        while (!folder.isDirectory()) {
            folder = folder.getParentFile();
        }
        parent = folder;
        return true;
    }

    @Override
    public void setLocation(File location) {
        checkClosed();
        if (location == null) {
            throw new NullPointerException("location");
        }
        if (!location.exists()) {
            location.mkdirs();
        }
        if (!location.isDirectory()) {
            throw new IllegalArgumentException("not a directory");
        }
        parent = location;
    }

    @Override
    public void createContainer() throws IOException {
        checkClosed();
        written = new LinkedList<>();
    }

    @Override
    public OutputStream addEntry(String name) throws IOException {
        checkOpen();
        FileOutputStream fout = null;
        File f = new File(parent, name);
        fout = new FileOutputStream(f);
        written.add(f);
        return new BufferedOutputStream(fout, 64 * 1024);
    }

    @Override
    public void closeContainer(boolean display) throws IOException {
        checkOpen();
        if (display) {
            File f = parent;
            if (!written.isEmpty()) {
                f = written.getFirst();
            }
            DesktopIntegration.showInShell(f);
        }
        parent = null;
        written = null;
    }

    @Override
    public void destroyContainer() {
        checkOpen();
        for (File f : written) {
            f.delete();
        }
        parent = null;
        written = null;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public void configure(Component locationHint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFileFormatSupported(String extension) {
        return true;
    }

    @Override
    public String toString() {
        return Language.string("exf-destination-folder");
    }
}
