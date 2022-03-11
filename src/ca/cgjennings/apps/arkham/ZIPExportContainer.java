package ca.cgjennings.apps.arkham;

import ca.cgjennings.platform.DesktopIntegration;
import java.awt.Component;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import resources.Language;
import resources.ResourceKit;

/**
 * An export container that creates ZIP archives.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class ZIPExportContainer implements ExportContainer {

    private File location;
    private JarOutputStream zip;
    private ByteArrayOutputStream out;

    private void checkClosed() {
        if (zip != null) {
            throw new IllegalStateException("last container not closed");
        }
    }

    private void checkOpen() {
        if (zip == null) {
            throw new IllegalStateException("no container created");
        }
    }

    @Override
    public String getIdentifier() {
        return "zip-archive";
    }

    @Override
    public boolean selectLocation(String baseName, Component locationHint) {
        checkClosed();
        location = ResourceKit.showZipFileDialog(locationHint, ResourceKit.makeStringFileSafe(baseName));
        return location != null;
    }

    @Override
    public void setLocation(File location) {
        checkClosed();
        if (location == null) {
            throw new NullPointerException("location");
        }
        this.location = location;
    }

    @Override
    public void createContainer() throws IOException {
        checkClosed();
        zip = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(location)));
        zip.setLevel(9);
    }

    @Override
    public OutputStream addEntry(String name) throws IOException {
        checkOpen();
        finishDanglingEntry(true);
        ZipEntry e = new ZipEntry(name);
        zip.putNextEntry(e);
        return out;
    }

    @Override
    public void closeContainer(boolean display) throws IOException {
        checkOpen();
        try {
            finishDanglingEntry(false);
        } finally {
            File loctemp = location;
            JarOutputStream ztemp = zip;
            zip = null;
            out = null;
            location = null;
            if (ztemp != null) {
                ztemp.close();
            }
            if (display) {
                DesktopIntegration.showInShell(loctemp);
            }
        }
    }

    @Override
    public void destroyContainer() {
        checkOpen();
        try {
            if (zip != null) {
                zip.close();
            }
        } catch (Throwable t) {
        } finally {
            if (location != null) {
                location.delete();
            }
            zip = null;
            out = null;
            location = null;
        }
    }

    private void finishDanglingEntry(boolean isStartingNewEntry) throws IOException {
        if (out != null) {
            zip.write(out.toByteArray());
            out.reset();
        } else if (isStartingNewEntry) {
            out = new ByteArrayOutputStream(1024 * 128);
        }
    }

    @Override
    public boolean isFileFormatSupported(String extension) {
        return true;
    }

    @Override
    public String toString() {
        return Language.string("exf-destination-zip");
    }

    @Override
    public void configure(Component locationHint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }
}
