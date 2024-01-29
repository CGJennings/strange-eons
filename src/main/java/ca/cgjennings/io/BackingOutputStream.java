package ca.cgjennings.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An output stream that writes to a file, but it writes initially to a file
 * other than the one requested. If the process succeeds and the file is closed
 * without any exceptions, then the temporary file is overwritten with the new
 * content. If an exception is thrown while the file is written or the is never
 * closed, then the original file is not modified.
 * <p>
 * <b>WARNING: NOT YET IMPLEMENTED</b>
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class BackingOutputStream extends OutputStream {

    private File trueFile;
    private File tempFile;
    private FileOutputStream out;
    private boolean errors = false;

    public BackingOutputStream(File file) throws IOException {
        if (file.isDirectory()) {
            throw new FileNotFoundException("file exists and is a directory: " + file);
        }
        trueFile = file;
        tempFile = createTemp(file);
        out = new FileOutputStream(tempFile);
    }

    public BackingOutputStream(String file) throws IOException {
        this(new File(file));
    }

    /**
     * Close the temporary file and copy it over the real file. If any
     * exceptions have been thrown from this instance while this file was
     * written to, then an exception will be thrown. If the temporary file
     * throws an exception when closed, an exception will be thrown. Otherwise,
     * the following actions occur: (1) If the target file exists, it is renamed
     * to a temporary file name. (2) The temporary file is renamed to the target
     * file. (3) The original, now renamed, file is deleted. If the deletion
     * fails, another attempt is made when the VM exits.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        try {
            out.close();
        } catch (IOException e) {
            errors = true;
            throw e;
        }
        if (errors) {
            throw new IOException("cannot overwrite original because an exception was thrown");
        }

        if (trueFile.exists()) {
            File temp = createTemp(trueFile);
            if (!trueFile.renameTo(temp)) {
                errors = true;
                throw new IOException("unable to rename original file to temporary name");
            }
            if (!tempFile.renameTo(trueFile)) {
                // try to undo the original renaming to restore the original file
                temp.renameTo(trueFile);
                errors = true;
                throw new IOException("unable to rename temporary file to true file");
            }
            if (!temp.delete()) {
                temp.deleteOnExit();
            }
        }
    }

    private File createTemp(File file) throws IOException {
        return File.createTempFile("~" + file.getName(), null, file.getParentFile());
    }

    @Override
    public void write(int b) throws IOException {
        try {
            out.write(b);
        } catch (IOException e) {
            errors = true;
            throw e;
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            out.flush();
        } catch (IOException e) {
            errors = true;
            throw e;
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        try {
            out.write(b);
        } catch (IOException e) {
            errors = true;
            throw e;
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            out.write(b, off, len);
        } catch (IOException e) {
            errors = true;
            throw e;
        }
    }
}
