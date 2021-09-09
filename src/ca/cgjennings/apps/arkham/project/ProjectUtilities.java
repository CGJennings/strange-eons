package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.BusyDialog;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.Subprocess;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.apps.arkham.plugins.PluginContextFactory;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.io.StreamPump;
import ca.cgjennings.platform.PlatformSupport;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import resources.ResourceKit;
import resources.Settings;

/**
 * Helper methods that are useful when writing {@link TaskAction}s and other
 * project-related classes and scripts. The file and stream copying methods are
 * threadsafe.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class ProjectUtilities {

    private ProjectUtilities() {
    }

    /**
     * Return the current project view, if any, in the application. This is
     * shorthand for {@code StrangeEons.getWindow().getOpenProjectView()}.
     *
     * @return the current project view, or {@code null}
     */
    private static ProjectView getView() {
        return StrangeEons.getWindow().getOpenProjectView();
    }

    /**
     * Simplifies a description of the target of an action. Actions are asked
     * whether they apply to a member, along with its task and project. When a
     * task is selected, the member will be {@code null}. Likewise, then a
     * project is selected, the task will also be {@code null}. If a
     * {@code TaskAction} does not wish to alter its behaviour when applied
     * to these different member types, it can call this method to collapse
     * these three parameters to a single value, the selected member (no matter
     * the type).
     *
     * @param project the project
     * @param task the task within the project, or {@code null}
     * @param member the member within the task, or {@code null}
     * @return the rightmost non-{@code null} parameter, or
     * {@code null} if all parameters are {@code null}
     */
    public static Member simplify(Project project, Task task, Member member) {
        Member m = member;
        if (m == null) {
            m = task;
        }
        if (m == null) {
            m = project;
        }
        return m;
    }

    /**
     * Simplify an array of non-{@code null} members, if possible, by
     * eliminating duplicates and keeping only the ancestor if an ancestor and a
     * descendent both occur. Given the following members:
     * <pre>
     * Fred
     * Martha
     * Martha/Sarah
     * Martha/Sarah/Abby
     * Fred
     * Martha/Jane
     * George
     * Jim/Ralph
     * </pre> a merged version would consist of:
     * <pre>
     * Fred
     * Martha
     * George
     * Jim/Ralph
     * </pre> The order of elements in {@code members} is maintained in the
     * merged result.
     *
     * @param members an array of members to merge
     * @return the merged members, or the original array if merging does not
     * reduce the number of elements
     */
    public static Member[] merge(Member[] members) {
        Member[] clone = members.clone();
        int kept = clone.length;
        for (int m = 0; m < clone.length; ++m) {
            boolean keep = true;
            for (int i = 0; i < clone.length; ++i) {
                if (i == m || clone[i] == null || clone[m] == null) {
                    continue;
                }
                if (clone[i].equals(clone[m]) || clone[i].isAncestorOf(clone[m])) {
                    keep = false;
                    break;
                }
            }
            if (!keep) {
                clone[m] = null;
                --kept;
            }
        }
        if (kept == members.length) {
            return members;
        }

        int m = 0;
        Member[] merged = new Member[kept];
        for (int i = 0; i < clone.length; ++i) {
            if (clone[i] != null) {
                merged[m++] = clone[i];
            }
        }
        return merged;
    }

    /**
     * Deletes a file or folder and all of its contents, if possible. Note that
     * if the deletion fails, and the file is a folder, some of the files or
     * folders it contains may have been deleted before the failure occurred.
     *
     * <p>
     * This method can be used to safely delete both regular files and project
     * files. It will detect whether any file it should delete is a member of
     * the open project, and hand such files off to {@link Member#deleteFile()}
     * for safe deletion.
     *
     * <p>
     * Since this method deletes files recursively, the caller should be doubly
     * sure that the specified file is correct. As a precaution, the method will
     * throw an exception if asked to delete the root folder of a file system.
     *
     * @param target the file to delete
     * @return {@code true} if all of the files were successfully deleted
     * @throws NullPointerException if the target is {@code null}
     * @throws IllegalArgumentException if the specified file is a file system
     * root (such as C: on windows or / on UNIX).
     */
    public static boolean deleteAll(File target) {
        if (target == null) {
            throw new NullPointerException("target");
        }
        Path targetPath = target.toPath();
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            if (targetPath.equals(root)) {
                throw new IllegalArgumentException("cannot delete file system root " + target);
            }
        }

        if (target.isDirectory()) {
            for (File kid : target.listFiles()) {
                deleteAll(kid);
            }
        }

        Project proj = StrangeEons.getOpenProject();
        if (proj != null) {
            Member member = proj.findMember(target);
            if (member != null) {
                try {
                    member.deleteFile();
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }
        }

        return target.delete();
    }

    /**
     * Recursively copy a file or folder into a folder.
     *
     * @param original a file or folder to copy
     * @param parent the parent directory where that should contain the copy
     * @throws IllegalArgumentException if {@code parent} is not a folder
     * @throws IOException if an exception occurs while copying
     */
    public static void copyFolder(File original, File parent) throws IOException {
        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("parent is not a folder");
        }
        if (parent.isHidden() || sameFile(original, parent)) {
            return;
        }

        File dest = new File(parent, original.getName());
        if (original.isDirectory()) {
            dest.mkdirs();
            for (File kid : original.listFiles()) {
                copyFolder(kid, dest);
            }
        } else {
            dest = getAvailableFile(dest);
            copyFile(original, dest);
        }
    }

    /**
     * Copy a file. This is not recursive, and should not be used to copy
     * folders. The file attributes of the source file will also be copied, if
     * possible.
     *
     * @param original the file to copy
     * @param copy the destination to copy to
     * @throws IOException if an I/O error occurs during the copy
     */
    public static void copyFile(File original, File copy) throws IOException {
        if (original.isHidden() || sameFile(original, copy)) {
            return;
        }
        Files.copy(original.toPath(), copy.toPath(), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Moves a file. This is not recursive and should not be used to move
     * folders.
     *
     * @param source the file to move
     * @param dest the destination (including the file name part)
     * @throws IOException if an I/O error occurs during the move
     */
    public static void moveFile(File source, File dest) throws IOException {
        if (source.isHidden() || sameFile(source, dest)) {
            return;
        }
        try {
            Files.move(source.toPath(), dest.toPath(), StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Check carefully whether two file objects refer to the same file in the
     * file system.
     *
     * @param a the first file to consider
     * @param b the second file to consider
     * @return {@code true} if {@code a} and {@code b} are
     * determined to be the same file
     */
    public static boolean sameFile(File a, File b) {
        try {
            if (a.getCanonicalFile().equals(b.getCanonicalFile())) {
                return true;
            }
        } catch (IOException e) {
            if (a.getAbsoluteFile().equals(b.getAbsoluteFile())) {
                return true;
            }
        }
        return a.equals(b);
    }

    /**
     * Copy the content of a file to an output stream.
     *
     * @param original
     * @param out
     * @throws IOException
     */
    public static void copyFile(File original, OutputStream out) throws IOException {
        try (FileInputStream in = new FileInputStream(original)) {
            copyStream(in, out);
        }
    }

    /**
     * Copy the content from a file stored in the application resources to a
     * file.
     *
     * @param resource the path to the resource file
     * @param copy the file to copy the resource content to
     * @throws IOException if the file cannot be copied
     */
    public static void copyResourceToFile(String resource, File copy) throws IOException {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            if (resource.startsWith("res://")) {
                resource = resource.substring("res://".length());
            }
            URL url = ResourceKit.composeResourceURL(resource);
            if (url == null) {
                throw new FileNotFoundException("res://" + resource);
            }
            in = url.openStream();
            out = new FileOutputStream(copy);
            copyStream(in, out);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    /**
     * Copy the content of an input stream to an output stream.
     *
     * @param in
     * @param out
     * @throws IOException
     */
    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        StreamPump.copy(in, out);
    }

    /**
     * Copy the content of an input stream to a file.
     *
     * @param in
     * @param out
     * @throws IOException
     */
    public static void copyStream(InputStream in, File out) throws IOException {
        FileOutputStream outs = new FileOutputStream(out);
        try {
            copyStream(in, outs);
        } finally {
            try {
                outs.close();
            } catch (IOException e) {
                throw e;
            }
        }
    }

    /**
     * Return the complete text of a file as a string.
     *
     * @param f
     * @param enc the encoding, e.g., {@link #ENC_UTF8}
     * @return the text of the file
     * @throws IOException
     */
    public static String getFileText(File f, String enc) throws IOException {
        StringWriter s = new StringWriter();
        try (Reader r = new InputStreamReader(new FileInputStream(f), enc)) {
            copyReader(r, s);
        }
        return s.toString();
    }

    /**
     * Copy the content of a reader to a writer.
     *
     * @param in
     * @param out
     * @throws IOException
     */
    public static void copyReader(Reader in, Writer out) throws IOException {
        StreamPump.copy(in, out);
    }

    /**
     * Copy the content of a reader to a file using the ISO-8859-15 encoding.
     * {@linkplain #copyReader(java.io.Reader, java.io.File, java.lang.String) Specifying an encoding explicitly is recommended.}
     *
     * @param in
     * @param out
     * @throws IOException
     */
    public static void copyReader(Reader in, File out) throws IOException {
        copyReader(in, out, ENC_SETTINGS);
    }

    /**
     * Copy the content of a reader to a file using a specified encoding.
     *
     * @param in
     * @param out
     * @param enc the encoding method used to write the text
     * @throws IOException
     */
    public static void copyReader(Reader in, File out, String enc) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out), enc))) {
            copyReader(in, bw);
        }
    }

    /**
     * The encoding used to store settings files.
     */
    public static final String ENC_SETTINGS = "ISO-8859-15";
    /**
     * The encoding used to store script files (UTF-8).
     */
    public static final String ENC_SCRIPT = "UTF-8";
    /**
     * The encoding used to store properties (UI text).
     */
    public static final String ENC_UI_PROPERTIES = "ISO-8859-1";
    /**
     * The UTF-8 encoding.
     */
    public static final String ENC_UTF8 = "UTF-8";

    /**
     * Create a ZIP output stream for writing a plug-in bundle or ZIP archive.
     * In order to be a valid plug-in bundle, a valid <tt>eons-plugin</tt> file
     * must be written into the archive's root folder. If {@code webSafe}
     * is {@code false} and the archive contains no
     * {@code eons-plugin} file, then the result will be a plain ZIP file
     * that can be used to create a portable archive for other purposes.
     *
     * @param out the stream to write the archive to
     * @param webSafe if {@code true}, a Web-safe bundle archive is created
     * (do not use if creating a plain ZIP archive)
     * @return the archive stream to write file data to
     * @throws IOException if an I/O error occurs while creating the archive
     * stream
     */
    public static ZipOutputStream createPluginBundleArchive(OutputStream out, boolean webSafe) throws IOException {
        if (webSafe) {
            out.write(new byte[]{(byte) 0x07, (byte) 0x88, (byte) 0x53, (byte) 0x45});
        }
        return new JarOutputStream(out);
    }

    /**
     * This is a convenience method that creates a plug-in bundle archive that
     * will be written to a file.
     *
     * @param archive the path to the file that the archive will be written to
     * @param webSafe if {@code true}, a Web-safe bundle archive is created
     * (do not use if creating a plain ZIP archive)
     * @return the archive stream to write file data to
     * @throws IOException if an I/O error occurs while creating the archive
     * stream
     * @see #createPluginBundleArchive(java.io.OutputStream, boolean)
     */
    public static ZipOutputStream createPluginBundleArchive(File archive, boolean webSafe) throws IOException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(archive);
            return createPluginBundleArchive(out, webSafe);
        } catch (IOException e) {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception inner) {
                }
            }
            throw e;
        }
    }

    /**
     * Copy one or more files into an archive.
     *
     * @param out the archive stream to copy to
     * @param baseFile the source file to be copied
     * @param baseZIPEntry the folder name to use as the file's parent in the
     * archive
     * @param recurse if {@code true}, recursively copy all children of
     * {@code baseFile} (if any) into the archive
     * @param filterFiles if files that match the excluded project file list
     * should be ignored
     * @throws IOException if an I/O error occurs while reading the source file
     * or writing to the archive
     */
    public static void copyToArchive(ZipOutputStream out, File baseFile, String baseZIPEntry, boolean recurse, boolean filterFiles, boolean compress) throws IOException {
        if (baseFile.isHidden() && !baseFile.getName().equals(Task.TASK_SETTINGS)) {
            return;
        }
        if (filterFiles && Member.isFileExcluded(baseFile)) {
            return;
        }

        BusyDialog bd = BusyDialog.getCurrentDialog();

        String name;
        if (baseZIPEntry.length() > 0 && !baseZIPEntry.endsWith("/")) {
            name = baseZIPEntry + "/" + baseFile.getName();
        } else {
            name = baseZIPEntry + baseFile.getName();
        }
        if (bd != null) {
            bd.setStatusText(name);
        }
        ZipEntry ze;
        if (baseFile.isDirectory()) {
            ze = new ZipEntry(name + "/");
        } else {
            ze = new ZipEntry(name);
        }
        ze.setTime(baseFile.lastModified());

        if (!compress) {
            ze.setMethod(ZipOutputStream.STORED);
            long size = baseFile.isDirectory() ? 0L : baseFile.length();
            ze.setSize(size);
            ze.setCompressedSize(size);
            ze.setCrc(size == 0 ? 0 : crc32(baseFile));
        }

        out.putNextEntry(ze);
        if (baseFile.isDirectory()) {
            out.closeEntry();
            if (recurse) {
                for (File kid : baseFile.listFiles()) {
                    copyToArchive(out, kid, ze.getName(), true, filterFiles, compress);
                }
            }
            // if not recursing, this simply creates a directory entry with
            // no file data
        } else {
            copyFile(baseFile, out);
            out.closeEntry();
        }
    }

    private static long crc32(File f) throws IOException {
        CRC32 crc = new CRC32();
        byte[] buff = new byte[32_768];
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(f), 32_768);
            int bytes = 0;
            while ((bytes = in.read(buff)) != -1) {
                crc.update(buff, 0, bytes);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        return crc.getValue();
    }

    static void mixOptions(Object[] options) {
        if (PlatformSupport.PLATFORM_IS_MAC) {
            Object t = options[0];
            options[0] = options[1];
            options[1] = t;
        }
    }

    static int unmixChoice(Object[] options, int choice) {
        if (PlatformSupport.PLATFORM_IS_MAC) {
            if (choice == 0) {
                choice = 1;
            } else {
                choice = 0;
            }
        }
        return choice;
    }

    /**
     * Returns {@code true} if the {@code possibleChild} has
     * {@code container} as an ancestor. Returns {@code true} if the
     * two files are equal. If either file is {@code null}, returns
     * {@code false}.
     *
     * @param container the parent file that may contain
     * {@code possibleChild}
     * @param possibleChild the file to test
     * @return {@code true} if and only if both files are
     * non-{@code null} and {@code container} is a parent of
     * {@code possibleChild}
     */
    public static boolean contains(File container, File possibleChild) {
        while (possibleChild != null) {
            if (possibleChild.equals(container)) {
                return true;
            }
            possibleChild = possibleChild.getParentFile();
        }
        return false;
    }

    /**
     * Returns the {@code preferred} file name if it is not in use,
     * otherwise generates a numbered variant of the name that does not already
     * exist at the time the method is called. A typical use is to create an
     * alternative target file if the target of a copy operation already exists.
     *
     * @param preferred the ideal file that should be used if it is available
     * (does not exist)
     * @return either the preferred file or an available variant
     */
    public static File getAvailableFile(File preferred) {
        if (!preferred.exists()) {
            return preferred;
        }

        String name = preferred.getName();
        String ext;
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            ext = name.substring(dot + 1);
            name = name.substring(0, dot);
        } else {
            ext = "";
        }
        name = name.replaceFirst("\\ \\(\\d+\\)$", "");

        int i = 2;
        File parent = preferred.getParentFile();
        if (parent == null) {
            throw new IllegalArgumentException("preferred target has no parent");
        }

        while (preferred.exists()) {
            preferred = new File(parent, name + " (" + (i++) + ")." + ext);
        }
        return preferred;
    }

    /**
     * Returns a file's extension as a lowercase string. If the file has no
     * extension, an empty string is returned. A file's extension consists of
     * the suffix starting after its final period (<tt>.</tt>) character. Given
     * a file with the name
     * <tt>myPony.pony.Zip</tt>, this method returns <tt>zip</tt> as the file
     * extension. Extensions are a fairly reliable cross-platform mechanism for
     * identifying the content type of a file.
     *
     * @param f the file whose file name extension is desired
     * @return the lowercase file extension of the file's name
     */
    public static String getFileExtension(File f) {
        String name = f.getName();
        int p = name.lastIndexOf('.');
        if (p < 0) {
            return "";
        } else {
            return name.substring(p + 1).toLowerCase(Locale.CANADA);
        }
    }

    /**
     * Matches a file against a list of file extensions. If the file is not a
     * folder and it's file name matches any of the extensions, then this method
     * returns {@code true}, otherwise {@code false}. The list of
     * extensions should be one or more lowercase strings, each of which is a
     * file extension without a '<tt>.</tt>' (e.g., <tt>png</tt>, <tt>eon</tt>,
     * <tt>xml</tt>, etc.). Note that the file does not need to exist in order
     * to be matched by this function, but if it does exist, it must not be a
     * folder.
     *
     * @param f the file to be tested
     * @param extensions the candidate file extensions to be tested against
     * @return {@code true} if {@code f} is not a folder and its name
     * matches any of the given extensions
     */
    public static boolean matchExtension(File f, String... extensions) {
        if (f.isDirectory()) {
            return false;
        }
        String ext = getFileExtension(f);
        for (int i = 0; i < extensions.length; ++i) {
            if (ext.equals(extensions[i])) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchExtension(File f, String extension) {
        if (f.isDirectory()) {
            return false;
        }
        String ext = getFileExtension(f);
        return ext.equals(extension);
    }

    public static boolean matchExtension(Member m, String... extensions) {
        if (m.isFolder()) {
            return false;
        }
        String ext = m.getExtension();
        for (int i = 0; i < extensions.length; ++i) {
            if (ext.equals(extensions[i])) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchExtension(Member m, String extension) {
        if (m.isFolder()) {
            return false;
        }
        String ext = m.getExtension();
        return ext.equals(extension);
    }

    /**
     * Returns a new file with the same name and path as {@code f} except
     * that it will have the extension {@code newExtension}. If the name
     * did not have an extension previously, a period and the new extension are
     * appended. Otherwise, the last extension in the name is removed and the
     * new extension appended to replace it. If the original file has a parent
     * path, the new file will have the same parent.
     *
     * @param f the original file
     * @param newExtension the extension to use, {@code null} to remove the
     * extension
     * @return a file with the same base name but a different extension
     */
    public static File changeExtension(File f, String newExtension) {
        String name = f.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot < 0) {
            if (newExtension != null) {
                name = name + '.' + newExtension;
            } // else do nothing since already has no extension
        } else {
            if (newExtension != null) {
                name = name.substring(0, lastDot + 1) + newExtension;
            } else {
                name = name.substring(0, lastDot);
            }
        }

        File parent = f.getParentFile();
        if (parent == null) {
            f = new File(name);
        } else {
            f = new File(parent, name);
        }

        return f;
    }

    /**
     * Tries to make a file relative to another base file. If this is possible,
     * the relative file is returned. Otherwise, the original file that was to
     * be made relative is returned. If either file is relative, it will be made
     * absolute first (using the current directory).
     *
     * @param baseFile the file that the returned file is relative to
     * @param fileToMakeRelative the file that the returned file will point to
     * when resolved against the base file
     * @return the location of the second file, relative to the first
     * @throws IllegalArgumentException if the paths in either of the parameters
     * are not valid local paths
     */
    public static File makeFileRelativeTo(File baseFile, File fileToMakeRelative) {
        // Java 7 implementation
        Path base, rel;
        try {
            base = baseFile.getAbsoluteFile().toPath();
        } catch (InvalidPathException ipe) {
            throw new IllegalArgumentException("baseFile");
        }
        try {
            rel = fileToMakeRelative.getAbsoluteFile().toPath();
        } catch (InvalidPathException ipe) {
            throw new IllegalArgumentException("fileToMakeRelative");
        }
        try {
            return base.relativize(rel).toFile();
        } catch (IllegalArgumentException | UnsupportedOperationException iae) {
            // can't relativize the path; e.g., file is on another drive letter
            // or, can't convert to file because path is invalid (should not happen)
        }
        return fileToMakeRelative;
    }

    /**
     * Returns a localized string that describes a file size (or other
     * measurement in bytes).
     */
    public static String formatByteSize(long size) {
        if (size < 1_500) {
            return formatter.format(size) + " B";
        }

        int i = -1;
        double dsize = size;
        do {
            dsize /= 1024.0;
            ++i;
            // MAX_LONG prevents i from exceeding array length			
        } while (dsize > 1024.0);

        return formatter.format(dsize) + units[i];
    }

    private static final String[] units = new String[]{
        // NB: SI prefix is k, but IEC prefix is K
        " KiB", " MiB", " GiB", " TiB", " PiB", " EiB" //, " ZiB", " YiB"
    };

    private static final NumberFormat formatter;

    static {
        formatter = NumberFormat.getNumberInstance();
        formatter.setMaximumFractionDigits(2);
        formatter.setMinimumFractionDigits(0);
    }

    static String getFragment(String resource) {
        try {
            return getResourceText("projects/" + resource, ENC_SETTINGS);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the contents of a text file stored in resources as a string, or
     * {@code null} if there is no such resource.
     *
     * @param resource the path to the resource file to read
     * @param encoding the text encoding, or {@code null} to use the
     * default (ISO-8859-15)
     * @return the text content of the file
     * @throws IOException if an I/O error occurs while reading the resource
     */
    public static String getResourceText(String resource, String encoding) throws IOException {
        if (resource == null) {
            throw new NullPointerException("resource");
        }
        if (encoding == null) {
            encoding = ENC_SETTINGS;
        }
        URL url = null;
        url = ResourceKit.composeResourceURL(resource);
        if (url == null) {
            return null;
        }
        try (InputStream in = url.openStream()) {
            StringWriter sw = new StringWriter(512);
            copyReader(new InputStreamReader(in, encoding), sw);
            return sw.toString();
        }
    }

    /**
     * Returns the contents of a text file stored in resources as a string, or
     * {@code null} if there is no such resource. The text must be encoded
     * in UTF-8.
     *
     * @param resource the path to the resource file to read
     * @return the text content of the file
     * @throws IOException if an I/O error occurs while reading the resource
     */
    public static String getResourceText(String resource) throws IOException {
        return getResourceText(resource, ENC_UTF8);
    }

    /**
     * Creates a file that contains the given text in the UTF-8 encoding.
     *
     * @param file the file to write to
     * @param text the text to write to the file
     * @throws IOException if an error occurs while writing the text
     */
    public static void writeTextFile(File file, String text) throws IOException {
        writeTextFile(file, text, ENC_UTF8);
    }

    /**
     * Creates a file that contains the given text.
     *
     * @param file the file to write to
     * @param text the text to write to the file
     * @param enc the encoding to use
     * @throws IOException if an error occurs while writing the text
     */
    public static void writeTextFile(File file, String text, String enc) throws IOException {
        FileOutputStream fout = new FileOutputStream(file);
        try (Writer out = new BufferedWriter(new OutputStreamWriter(fout, enc))) {
            out.write(text);
        }
    }

    /**
     * Returns the contents of a file as a string.
     *
     * @param f the file to read
     * @param enc the character encoding of the file
     * @return the contents of the file as a string
     * @throws IOException if an I/O errors while reading the file
     */
    public static String getFileAsString(File f, String enc) throws IOException {
        StringWriter sw = new StringWriter(2_048);
        try (Reader in = new InputStreamReader(new FileInputStream(f), enc)) {
            copyReader(in, sw);
        }
        return sw.toString();
    }

    /**
     * Runs a script whose source text is stored in a file. This is a cover for
     * {@code runScript( script, null, null, null, false )}.
     *
     * @param script the file that contains the source text to execute
     * @return the script's return value; typically the value of the last
     * executed statement
     * @throws IOException if an I/O error occurs while reading the script
     */
    public static Object runScript(File script) throws IOException {
        return runScript(script, null, null, null, false);
    }

    /**
     * Runs a script file. This is a cover for
     * {@code runScript( scriptName, script, null, null, null, false )}.
     *
     * @param scriptName the source file name to be used to identify the script
     * @param script the source code of the script
     * @return the script's return value; typically the value of the last
     * executed statement
     */
    public static Object runScript(String scriptName, String script) {
        return runScript(scriptName, script, null, null, null, false);
    }

    /**
     * Runs a script whose source text is stored in a file. The script's return
     * value, if any, is returned by this method. In the script, the variables
     * {@code project}, {@code task}, and {@code member} will be
     * bound to the values passed to this method. The script will be provided
     * with a default {@code PluginContext} that is not associated with any
     * plug-in.
     *
     * @param script the file that contains the source text to execute
     * @param project the project associated with the script, or
     * {@code null}
     * @param task the task associated with the script, or {@code null}
     * @param member the project member associated with the script, or
     * {@code null}
     * @param debug if {@code true}, the script will be executed in debug
     * mode if possible
     * @return the script's return value; typically the value of the last
     * executed statement
     * @throws IOException if an I/O error occurs while reading the script
     */
    public static Object runScript(File script, Project project, Task task, Member member, boolean debug) throws IOException {
        if (script == null) {
            throw new NullPointerException("script");
        }
        String code = getFileAsString(script, ProjectUtilities.ENC_SCRIPT);
        return runScriptImpl("Quickscript", script, code, project, task, member, debug);
    }

    /**
     * Runs a script whose source text is stored in a string. The script's
     * return value, if any, is returned by this method. In the script, the
     * variables {@code project}, {@code task}, and
     * {@code member} will be bound to the values passed to this method.
     * The script will be provided with a default {@code PluginContext}
     * that is not associated with any plug-in.
     *
     * @param scriptName the source file name to be used to identify the script
     * @param script the source code of the script
     * @param project the project associated with the script, or
     * {@code null}
     * @param task the task associated with the script, or {@code null}
     * @param member the project member associated with the script, or
     * {@code null}
     * @param debug if {@code true}, the script will be executed in debug
     * mode if possible
     * @return the script's return value; typically the value of the last
     * executed statement
     */
    public static Object runScript(String scriptName, String script, Project project, Task task, Member member, boolean debug) {
        return runScriptImpl(scriptName, null, script, project, task, member, debug);
    }

    private static Object runScriptImpl(String scriptName, File sourceFile, String script, Project project, Task task, Member member, boolean debug) {
        if (script == null) {
            throw new NullPointerException("script");
        }
        if (scriptName == null) {
            throw new NullPointerException("scriptName");
        }

        if (Settings.getUser().getYesNo(ScriptMonkey.CLEAR_CONSOLE_ON_RUN_KEY)) {
            ScriptMonkey.getSharedConsole().clear();
        }

        ScriptMonkey monkey = new ScriptMonkey(scriptName);

        if (sourceFile != null) {
            Project p = getView() == null ? null : getView().getProject();
            String internalName = null;
            if (p != null) {
                Member m = p.findMember(sourceFile);
                if (m != null) {
                    StringBuilder b = new StringBuilder(128);
                    while (m != null && !(m instanceof Project)) {
                        b.insert(0, m.getName());
                        b.insert(0, '/');
                        m = m.getParent();
                    }
                    b.insert(0, "project:/");
                    internalName = b.toString();
                }
            }
            if (internalName == null) {
                internalName = sourceFile.toURI().toString();
            }
            monkey.setInternalFileName(internalName);
        }

        monkey.bind(PluginContextFactory.createDummyContext());
        monkey.bind("project", project);
        monkey.bind("task", task);
        monkey.bind("member", member);
        monkey.setBreakpoint(debug);
        Object result = monkey.eval(script);
        return result;
    }

    /**
     * Executes a shell command on the local system. The first string passed in
     * is the command name, and each subsequent string is an argument to pass to
     * the command. This method will not return until the command completes, and
     * any output from the command will appear in the script console.
     *
     * @param commandArray the command and arguments to use
     * @return the exit code from the command, or -1 if this thread was
     * interrupted while waiting for the command to finish
     * @throws NullPointerException if the command array is {@code null}
     * @throws IllegalArgumentException if the command array is empty
     */
    public static int exec(String... commandArray) {
        if (commandArray == null) {
            throw new NullPointerException("command");
        }
        if (commandArray.length == 0) {
            throw new IllegalArgumentException("empty command");
        }
        Subprocess p = new Subprocess(commandArray);
        // not really, since we will wait for it, but this avoids the overhead
        // of adding and then removing an exit task to kill the process
        p.setSurvivor(true);
        p.setExitCodeShown(false);
        p.start();
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            StrangeEons.log.warning("this thread interrupted while waiting for exec thread");
        }
        return p.getExitCode();
    }

    /**
     * Executes a shell command on the local system. The first string passed in
     * is the command name, and each subsequent string is an argument to pass to
     * the command. Any output from the command will appear in the script
     * console. This method will return immediately; the command will run in
     * the background.
     *
     * @param commandArray the command and arguments to use
     * @throws NullPointerException if the command array is {@code null}
     * @throws IllegalArgumentException if the command array is empty
     * @since 3.2
     */
    public static void execAsync(String... commandArray) {
        if (commandArray == null) {
            throw new NullPointerException("command");
        }
        if (commandArray.length == 0) {
            throw new IllegalArgumentException("empty command");
        }
        Subprocess p = new Subprocess(commandArray);
        p.setExitCodeShown(false);
        p.start();
    }

    /**
     * If there is a support editor that is being used to edit this file, and
     * the editor supports the <b>Save</b> command, and the editor has unsaved
     * changes, the editor will be asked to save the file.
     *
     * @param f the file that, if open in an editor, will be saved
     */
    public static void saveIfBeingEdited(File f) {
        for (StrangeEonsEditor ed : StrangeEons.getWindow().getEditorsShowingFile(f)) {
            if (ed.hasUnsavedChanges() && (ed.canPerformCommand(Commands.SAVE))) {
                ed.save();
            }
        }
    }

    /**
     * Tries to save every open editor that is a member of the current project.
     *
     * @param closeAfterSave if {@code true}, each file is closed after
     * being saved
     */
    public static void saveAllOpenProjectFiles(boolean closeAfterSave) {
        if (getView() == null) {
            return;
        }
        Project p = getView().getProject();
        if (p == null) {
            return;
        }

        for (StrangeEonsEditor editor : StrangeEons.getWindow().getEditors()) {
            if (!editor.hasUnsavedChanges()) {
                continue;
            }
            File f = editor.getFile();
            if (f == null) {
                continue;
            }
            Member m = p.findMember(f);
            if (m == null) {
                continue;
            }

            if (editor.canPerformCommand(Commands.SAVE)) {
                editor.save();
            }

            if (closeAfterSave) {
                editor.close();
            }
        }
    }

    public static List<Member> listMatchingMembers(Member parent, boolean recurse, String... extensions) {
        if (parent == null) {
            throw new NullPointerException("parent");
        }
        List<Member> list = new LinkedList<>();
        if (!parent.hasChildren()) {
            return list;
        }
        listMatchingMembersImpl(parent, recurse, list, extensions);
        return list;
    }

    private static void listMatchingMembersImpl(Member parent, boolean recurse, List<Member> list, String... extensions) {
        for (Member kid : parent.getChildren()) {
            if (kid.isFolder()) {
                if (recurse) {
                    listMatchingMembersImpl(kid, true, list, extensions);
                }
            } else if (matchExtension(kid, extensions)) {
                list.add(kid);
            }
        }
    }

    /**
     * A array of extensions that can be passed to {@link #matchExtension} to
     * match plug-in bundle files.
     */
    public static final String[] BUNDLE_EXTENSIONS = new String[]{
        "seplugin", "seext", "selibrary", "setheme"
    };
    /**
     * A array of extensions that can be passed to {@link #matchExtension} to
     * match image files.
     */
    public static final String[] IMAGE_EXTENSIONS = new String[]{
        "png", "jpg", "jp2"
    };
}
