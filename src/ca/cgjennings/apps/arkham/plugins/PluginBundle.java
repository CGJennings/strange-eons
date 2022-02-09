package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.plugins.catalog.Catalog;
import ca.cgjennings.io.InvalidFileFormatException;
import ca.cgjennings.io.StreamPump;
import ca.cgjennings.ui.theme.ThemedIcon;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.Icon;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * A plug-in bundle is a file that stores the code and resources required by a
 * plug-in. The {@code PluginBundle} class encapsulates operations that can be
 * performed on such a bundle <i>without actually linking it to the
 * application</i>. (The distinction is important because linking a plug-in
 * bundle to the application also typically locks the bundle file until the
 * application terminates.)
 *
 * <p>
 * A plug-in bundle is essentially a ZIP archive, with the following
 * restrictions and exceptions:
 * <ol>
 * <li>All file names in the archive are encoded in UTF-8.
 * <li>The following byte sequence may occur before the normal start of the
 * file: 0x07, 0x88, 0x53, 0x45; in this case the bundle is called
 * <i>Web-safe</i>.
 * <li>The archive must contain a file named <tt>eons-plugin</tt>
 * in its root folder. This file must follow the format for a plug-in root file.
 * (Strictly speaking, a root file is optional for library bundles, but highly
 * recommended.) See {@link PluginRoot}.
 * </ol>
 *
 * <p>
 * <b>Web-safe bundles:</b> Unless a server is properly configured to recognize
 * the standard plug-in file extensions and MIME types, the user's browser
 * (Internet Explorer in particular) may detect its type on the fly and convert
 * the file extension to <tt>.zip</tt> without informing the user. This makes it
 * difficult for users to install the plug-in, since they must both recognize
 * the problem and know the correct extension to use. A Web-safe bundle gets
 * around this by adding a few bytes to the start of the file, which prevents it
 * from being detected as a ZIP archive. However, a Web-safe bundle cannot be
 * linked to the application without first converting it back to the standard
 * format. Most of the methods in this class will transparently convert a
 * Web-safe bundle to a standard bundle; see the specific method descriptions
 * for details.
 *
 * <p>
 * <b>Obsolete Formats:</b> Some alpha and beta releases of Strange Eons
 * supported bundles that used other archive formats. These bundles are no
 * longer supported, and attempting to create a {@code PluginBundle} instance
 * for such files, or to determine their format, will throw an exception.
 * Bundles stored in these obsolete formats would now typically be converted
 * into published bundles (see below).
 *
 * <p>
 * <b>Published Bundles:</b> This class does not handle bundles that have been
 * <i>published</i>. Published bundles are the network transport format used by
 * the {@link Catalog} system. They have been reorganized and compressed using a
 * special multistep process that greatly reduces download size. Published
 * bundle files can be identified by an additional file extension. This
 * extension always starts with "p"; typically ".plzm" or ".pbz". For example,
 * <tt>myplugin.seplugin.pbz</tt> is the name of a published version of
 * <tt>myplugin.seplugin</tt>. Published bundles must be converted back to the
 * regular bundle format before they can be used with this class.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 * @see PluginRoot
 * @see BundleInstaller
 */
public class PluginBundle {

    /**
     * The bundle format is a plain ZIP-compatible archive bundle.
     */
    public static final int FORMAT_PLAIN = 1;
    /**
     * The bundle format is a "Web-safe" wrapped bundle.
     */
    public static final int FORMAT_WRAPPED = 2;
    /**
     * The file is not a valid plug-in bundle.
     */
    public static final int FORMAT_INVALID = 0;

    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    // N.B. These values are intentionally cross-defined with INSTALL_FLAG_* //
    //      in BundleInstaller; BundleInstaller depends on this being true.  //
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /**
     * The file appears to be a library bundle (based on the file extension).
     */
    public static final int TYPE_LIBRARY = BundleInstaller.INSTALL_FLAG_LIBRARY;
    /**
     * The file appears to be a theme (based on the file extension).
     */
    public static final int TYPE_THEME = BundleInstaller.INSTALL_FLAG_THEME;
    /**
     * The file appears to be an extension plug-in bundle (based on the file
     * extension).
     */
    public static final int TYPE_EXTENSION = BundleInstaller.INSTALL_FLAG_EXTENSION;
    /**
     * The file appears to be a plug-in bundle (based on the file extension).
     */
    public static final int TYPE_PLUGIN = BundleInstaller.INSTALL_FLAG_PLUGIN;
    /**
     * The file type is unknown (based on the file extension).
     */
    public static final int TYPE_UNKNOWN = 0;

    /**
     * The format of the original file.
     */
    private int format;
    /**
     * The type of bundle.
     */
    private int type;
    /**
     * The original file.
     */
    private File file;
    /**
     * The unwrapped temp file, or {@code null} if none exists. If {@code null},
     * either: the original bundle is not wrapped; the user has not called any
     * methods that would generate a temp file; or dispose() was called.
     */
    private File temp;

    /**
     * Creates a new plug-in bundle from a file path stored in a string. This
     * convenience constructor is equivalent to
     * {@code PluginBundle( new java.io.File( file ) )}.
     *
     * @param file the path and name of the bundle file
     * @throws IOException if an error occurs while accessing the bundle or it
     * is not a plug-in bundle
     */
    public PluginBundle(String file) throws IOException {
        this(new File(file));
    }

    /**
     * Creates a new plug-in bundle instance for a file.
     *
     * @param file the file that represents a plug-in bundle
     * @throws IOException if an error occurs while accessing the bundle or it
     * is not a plug-in bundle
     */
    public PluginBundle(File file) throws IOException {
        this.file = file;
        format = getBundleFormat(file);
        if (format == FORMAT_INVALID) {
            throw new InvalidFileFormatException(string("plug-err-empty-file"));
        }
        type = getBundleType(file);
    }

    /**
     * Returns a {@link PluginRoot} for the root file in this bundle, or
     * {@code null} if the plug-in does not have a root file. (A plug-in bundle
     * with no root file is invalid unless it is a library bundle.) If the
     * bundle format is not {@code FORMAT_PLAIN}, then it will first be
     * converted into a plain bundle as if by calling
     * {@code this.copy( this.getFile() )}.
     *
     * @return the content of the bundle's root file, or {@code null}
     * @throws IOException if an I/O exception occurs while reading the root
     * file
     */
    public PluginRoot getPluginRoot() throws IOException {
        if (cachedRoot == null) {
            cachedRoot = new PluginRoot(this);
        }
        return cachedRoot;
    }
    private PluginRoot cachedRoot;

    /**
     * Returns the file represented by this plug-in bundle. This is always the
     * original file that was used in the constructor.
     *
     * @return the original plug-in bundle file
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns a file that contains the contents of this bundle in
     * {@code FORMAT_PLAIN}. If the original bundle was already plain, then this
     * is equivalent to calling {@link #getFile()}. Otherwise, a temporary file
     * will be created in {@code FORMAT_PLAIN} and returned. (Repeated calls
     * will return the same file unless {@link #dispose()} is called in the
     * meantime.)
     *
     * @return an plain version of the bundle file
     * @throws IOException if the file is wrapped and an exception occurs while
     * unwrapping to a temporary file
     */
    public File getPlainFile() throws IOException {
        return getUnwrappedFile();
    }

    /**
     * Returns a {@code ZipFile} that represents the archived contents of this
     * bundle.
     *
     * @return a {@code ZipFile} that can be used to access the plug-in bundle's
     * content
     * @throws java.io.IOException
     */
    public ZipFile getZipFile() throws IOException {
        // note: using JarFile ensures file name encoding is UTF-8
        return new JarFile(getUnwrappedFile());
    }

    /**
     * Returns a {@code URL} that can be used as a base URL to access the
     * archived contents of this bundle without linking it against the
     * application.
     *
     * @return a {@code JarFile} URL for the plug-in bundle's content
     * @throws java.io.IOException if an I/O error occurs
     */
    public URL getBaseURL() throws IOException {
        return new URL("jar:" + getUnwrappedFile().toURI().toURL() + "!/");
    }

    /**
     * If the file is wrapped, make an unwrapped copy in a temporary file and
     * return that file. Otherwise, return the original file.
     */
    private File getUnwrappedFile() throws IOException {
        if (format == FORMAT_WRAPPED) {
            if (temp == null) {
                String ext = file.getName();
                int dot = ext.lastIndexOf('.');
                if (dot >= 0) {
                    ext = ext.substring(dot);
                } else {
                    ext = "";
                }
                File dest = File.createTempFile("se-unwrapped-", ext);
                dest.deleteOnExit();
                copy(dest);
                temp = dest;
            }
            return temp;
        }
        return file;
    }

    /**
     * Writes a copy of the plug-in bundle to {@code destination}. The copy of
     * the bundle will always be in {@code FORMAT_PLAIN}, but is otherwise
     * identical to the original bundle. The newly created bundle is returned as
     * a {@code PluginBundle}.
     * <p>
     * It is valid to use the file containing this bundle as the destination. In
     * this case, {@code this} is returned if the bundle is already unwrapped.
     * If the bundle is wrapped, it is unwrapped in place, replacing the
     * original file. In this case, this bundle's state is updated to reflect
     * the new status of the file.
     *
     * @param destination the {@code File} to write the copy to
     * @return a {@code PluginBundle} representing the destination
     * @throws IOException if an I/O error occurs during the copy
     */
    public PluginBundle copy(File destination) throws IOException {
        // special case of copying the bundle over itself as a
        // way of unwrapping it in place:
        //   - if it is not wrapped, then we are done
        //   - if it is wrapped, we make a temporary unwrapped copy and
        //     then copy the temporary over ourselves
        if (file.equals(destination)) {
            if (format != FORMAT_PLAIN) {
                // copy from this file to a temporary file, skipping the header;
                // then copy from the temporary file to this file
                copyImpl(getUnwrappedFile(), file, false, false);
                format = getBundleFormat(file);
                if (format != FORMAT_PLAIN) {
                    throw new IOException("Error while replacing source; plug-in bundle corrupted: backup available as " + temp.getAbsolutePath());
                }
                // we don't need the temp file anymore, since this is now unwrapped
                // (if it is in use by someone else the delete will fail silently)
                temp.delete();
                temp = null;
            }
            return this;
        }

        // standard case: source != destination
        copyImpl(file, destination, format == FORMAT_WRAPPED, false/*,   compression, 0*/);
        return new PluginBundle(destination);
    }

    /**
     * Convert the plug-in bundle file into a wrapped version, replacing the
     * original file. If the file is already wrapped, this method does nothing.
     *
     * @throws IOException if an I/O error occurs while wapping
     */
    public void wrap() throws IOException {
        if (format == FORMAT_WRAPPED /*&& this.compression == ARCHIVE_ZIP*/) {
            return;
        }

        File unwrapped = getUnwrappedFile();
        File wrapped = File.createTempFile("se-wrapped-", ".tmp");
        wrapped.deleteOnExit();
        copyImpl(unwrapped, wrapped, false, true);
        copyImpl(wrapped, file, false, false);

        PluginBundle check = new PluginBundle(file);
        if (check.getFormat() != FORMAT_WRAPPED /*|| check.getArchiveType() != ARCHIVE_ZIP*/) {
            throw new IOException("Error while wrapping file; plug-in bundle may be corrupted: backup available as " + temp.getAbsolutePath());
        }
        format = FORMAT_WRAPPED;
        wrapped.delete();
    }

    /**
     * Copies the bundle content into a ZIP archive with no file compression
     * (all files in the archive are {@code STORE}d). There are two primary
     * reasons for wanting an uncompressed archive:
     * <ol>
     * <li>When the plug-in will actually be run from the bundle in question
     * (i.e., when installing the bundle): after an initial access, the file
     * content will generally be found in the file system cache, so the
     * decompression overhead is not compensating for slow I/O operations.
     * <li>When the bundle file is going to be compressed again, such as when
     * creating an archive of plug-in bundles for distribution. Compressing
     * already compressed data generally achieves worse compression ratios than
     * only compressing once.
     * </ol>
     *
     * @return a new temporary file containing the uncompressed archive file
     * @throws IOException if an error occurs while creating the flat file
     */
    public File createUncompressedArchive() throws IOException {
        // unwrap to zip, if required
        File unwrapped = getUnwrappedFile();

        // unwrapped, but zip is probably compressed
        File uncompressed = File.createTempFile("se-flat-", ".tmp");
        uncompressed.deleteOnExit();
        byte[] bbuff = new byte[1_024 * 32];
        JarOutputStream zipOut = null;
        JarFile zipIn = null;
        try {
            zipOut = new JarOutputStream(new FileOutputStream(uncompressed));
            zipIn = new JarFile(unwrapped);
            Enumeration<? extends ZipEntry> entries = zipIn.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                InputStream ein = zipIn.getInputStream(entry);

                ZipEntry outEntry = new ZipEntry(entry);
                outEntry.setMethod(ZipEntry.STORED);
                outEntry.setCompressedSize(entry.getSize());
                zipOut.putNextEntry(outEntry);

                int read = -1;
                while ((read = ein.read(bbuff)) > 0) {
                    zipOut.write(bbuff, 0, read);
                }
                zipOut.closeEntry();
            }
        } finally {
            if (zipOut != null) {
                try {
                    zipOut.close();
                } catch (IOException e) {
                    StrangeEons.log.log(Level.WARNING, "close() exception", e);
                }
            }
            if (zipIn != null) {
                zipIn.close();
            }
        }

        return uncompressed;
    }

    /**
     * Returns the storage format of this bundle, either {@link #FORMAT_PLAIN}
     * or {@link #FORMAT_WRAPPED}.
     * <p>
     * Note that if the file has been modified outside of this class between the
     * time this object was constructed and the the time this method is called,
     * this value may be out of date.
     *
     * @return the storage format of this bundle
     */
    public int getFormat() {
        return format;
    }

    /**
     * Returns the apparent type of this bundle file, based on the file name's
     * extension. This will be one of {@code TYPE_LIBRARY}, {@code TYPE_THEME},
     * {@code TYPE_EXTENSION}, {@code TYPE_PLUGIN}, or {@code TYPE_UNKNOWN}.
     *
     * @return a type ID that matches the apparent type of this bundle
     * @throws NullPointerException if the file is {@code null}
     */
    public int getType() {
        return type;
    }

    /**
     * Make a copy of a file, possibly inserting or removing the 4-byte wrapping
     * header from the start of the copy. (If {@code removeHeader} is
     * {@code true}, 4 bytes are skipped from the start of the source. If
     * {@code insertHeader} is {@code true}, the wrapping header is inserted at
     * the start of the destination file.)
     */
    private static void copyImpl(File source, File destination, boolean removeHeader, boolean insertHeader) throws IOException {
        if (source.equals(destination)) {
            throw new IllegalArgumentException("source == destination");
        }

        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(source);
            out = new FileOutputStream(destination);

            if (removeHeader) {
                skipWrapper(in);
            }

            if (insertHeader) {
                out.write(WRAPPED_MAGIC);
            }

            StreamPump.copy(in, out);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static void skipWrapper(InputStream in) throws IOException {
        for (int i = 0; i < WRAPPED_MAGIC.length; ++i) {
            in.read();
        }
    }

    /**
     * Returns the apparent type of an arbitrary bundle file, based on the file
     * name's extension. The passed-in value can be any object; the object's
     * string value is used. This will be one of {@code TYPE_LIBRARY},
     * {@code TYPE_THEME}, {@code TYPE_EXTENSION}, {@code TYPE_PLUGIN}, or
     * {@code TYPE_UNKNOWN}.
     *
     * @param file this object's string value will be treated as a file name
     * that will be used to return a more specific icon
     * @return a type ID that matches the apparent type of the file
     * @throws NullPointerException if the file is {@code null}
     */
    public static int getBundleType(Object file) {
        if (file == null) {
            throw new NullPointerException("file");
        }
        if ((file instanceof File) && ((File) file).isDirectory()) {
            return TYPE_UNKNOWN;
        }

        String name = file.toString();
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            name = name.substring(dot + 1);
        }
        if (name.equalsIgnoreCase("selibrary")) {
            return TYPE_LIBRARY;
        }
        if (name.equalsIgnoreCase("setheme")) {
            return TYPE_THEME;
        }
        if (name.equalsIgnoreCase("seext")) {
            return TYPE_EXTENSION;
        }
        if (name.equalsIgnoreCase("seplugin")) {
            return TYPE_PLUGIN;
        }
        return TYPE_UNKNOWN;
    }

    /**
     * Returns a large or small icon that can be used to represent a plug-in
     * bundle in message dialogs, etc. A valid icon is returned even if file is
     * {@code null}. However, if a valid plug-in file is named, the returned
     * icon may more accurately reflect the file's contents.
     *
     * @param file this object's string value will be treated as a file name
     * that will be used to return a more specific icon
     * @param smallIcon if true, returns a small icon
     * @return an icon that represents the type of the bundle implied by
     * {@code file}
     */
    public static ThemedIcon getIcon(Object file, boolean smallIcon) {
        String image = "plugin";
        if (file != null) {
            switch (getBundleType(file)) {
                case TYPE_LIBRARY:
                    image = "library";
                    break;
                case TYPE_THEME:
                    image = "theme";
                    break;
                case TYPE_EXTENSION:
                    image = "extension";
                    break;
            }
        }
        ThemedIcon icon = ResourceKit.getIcon(image);
        if (smallIcon) {
            icon = icon.small();
        } else {
            icon = icon.medium();
        }
        return icon;
    }

    /**
     * Returns the storage format of an arbitrary bundle file, which will be one
     * of {@code FORMAT_PLAIN}, {@code FORMAT_WRAPPED}, or
     * {@code FORMAT_INVALID}.
     *
     * @param file the file to determine the format of
     * @return a format ID that matches the bundle format of the file
     * @throws java.io.IOException if an I/O error occurs while determining the
     * format
     * @throws NullPointerException if the file is {@code null}
     */
    public static int getBundleFormat(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("file");
        }
        FileInputStream in = null;
        try {
            boolean foundWrapper = false;
            in = new FileInputStream(file);
            byte[] buffer = new byte[ZIP_MAGIC.length];
            for (;;) {
                // read next four bytes from file
                int read = 0, total = 0;
                while ((read = in.read(buffer, total, buffer.length - total)) > 0 && total < buffer.length) {
                    total += read;
                }

                if (java.util.Arrays.equals(buffer, ZIP_MAGIC)) {
                    return foundWrapper ? FORMAT_WRAPPED : FORMAT_PLAIN;
                }

                if (java.util.Arrays.equals(buffer, BZIP2_MAGIC)) {
                    throw new InvalidFileFormatException("obsolete bundle format: BZip2 wrapped");
                }
                if (java.util.Arrays.equals(buffer, LZMA_MAGIC)) {
                    throw new InvalidFileFormatException("obsolete bundle format: LZMA wrapped");
                }

                if (java.util.Arrays.equals(buffer, WRAPPED_MAGIC)) {
                    // two wrappers in a row
                    if (foundWrapper) {
                        return FORMAT_INVALID;
                    }
                    foundWrapper = true;
                    // next look for ZIP magic
                    continue;
                }

                return FORMAT_INVALID;
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private static final byte[] ZIP_MAGIC = new byte[]{(byte) 0x50, (byte) 0x4b, (byte) 0x03, (byte) 0x04};
    private static final byte[] WRAPPED_MAGIC = new byte[]{(byte) 0x07, (byte) 0x88, (byte) 0x53, (byte) 0x45};
    private static final byte[] BZIP2_MAGIC = new byte[]{(byte) 0x53, (byte) 0x45, (byte) 0x42, (byte) 0x5a};
    private static final byte[] LZMA_MAGIC = new byte[]{(byte) 0x53, (byte) 0x45, (byte) 0x4c, (byte) 0x4d};

    /**
     * Performs cleanup when a plug-in bundle instance is no longer required. If
     * a temporary copy of the bundle file was created in {@code FORMAT_PLAIN},
     * calling this method will delete the temporary file. If this method is not
     * called, the temporary file will be deleted when the application
     * terminates. It is safe to continue using this instance after calling
     * {@code dispose()}, though potentially inefficient.
     *
     * <p>
     * <b>Note:</b> In no event will calling this method delete the original
     * file. Only temporary files created during processing will be affected.
     */
    public void dispose() {
        if (temp != null) {
            File t = temp;
            temp = null;
            t.delete();
        }
    }

    /**
     * Returns a string representation of this bundle instance, including the
     * class name, file name, and bundle format.
     *
     * @return a string representation of this bundle
     */
    @Override
    public String toString() {
        return getClass().getCanonicalName() + ": FILE=" + file.getAbsoluteFile() + "; FORMAT=" + format;
    }
}
