package ca.cgjennings.ui;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Locale;
import javax.swing.filechooser.FileFilter;

/**
 * Creates a file filter for a specified file type. The filter takes a
 * description of the file type and a list of matching extensions and creates a
 * suitable filter for the file type and filters out files that do not match the
 * extension (folders are not filtered out, to allow navigation).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class FileNameExtensionFilter extends FileFilter {

    private String desc;
    private String[] exts;

    /**
     * Creates a file name filter for files that match the specified extensions.
     * The provided description will be used as part of a description of the
     * file type, but the exact description may vary by platform.
     *
     * @param fileTypeDescription the file type description
     * @param fileNameExtensions file extensions to accept; may not be
     * {@code null}, empty, or contain {@code null} elements
     */
    public FileNameExtensionFilter(String fileTypeDescription, String... fileNameExtensions) {
        if (fileTypeDescription == null) {
            throw new NullPointerException("description");
        }
        if (fileNameExtensions == null) {
            throw new NullPointerException("extensions");
        }
        if (fileNameExtensions.length == 0) {
            throw new IllegalArgumentException("extensions.length == 0");
        }

        exts = new String[fileNameExtensions.length];
        for (int i = 0; i < exts.length; ++i) {
            if (fileNameExtensions[i] == null) {
                throw new NullPointerException("extensions[" + i + ']');
            }
            if (!fileNameExtensions[i].isEmpty() && fileNameExtensions[i].charAt(0) == '.') {
                throw new IllegalArgumentException("leave off '.'");
            }
            exts[i] = fileNameExtensions[i].toLowerCase(Locale.CANADA);
        }

        desc = makeDescription(fileTypeDescription);
    }

    /**
     * Build the human-friendly description from the description text and
     * extensions.
     */
    private String makeDescription(String fileTypeDescription) {
        String[] fileNameExtensions = reduce();

        StringBuilder b = new StringBuilder(fileTypeDescription.length() + fileNameExtensions.length * 10 + 3);
        b.append(fileTypeDescription).append(" (");
        for (int i = 0; i < fileNameExtensions.length; ++i) {
            if (i > 0) {
                b.append('|');
            }
            if (fileNameExtensions[i].isEmpty()) {
                b.append('*');
            } else {
                b.append("*.").append(fileNameExtensions[i]);
            }
        }
        b.append(')');
        return b.toString();
    }

    /**
     * Return a copy of the extensions after filtering out extraneous entries.
     */
    private String[] reduce() {
        LinkedHashSet<String> reducedExtensionSet = new LinkedHashSet<>();
        for (String ext : exts) {
            switch (ext) {
                case "jpeg":
                    if (!matches("jpg")) {
                        reducedExtensionSet.add(ext);
                    }
                    break;
                case "svgz":
                    if (!matches("svg")) {
                        reducedExtensionSet.add(ext);
                    }
                    break;
                default:
                    reducedExtensionSet.add(ext);
            }
        }
        if (reducedExtensionSet.size() == exts.length) {
            return exts;
        }
        return reducedExtensionSet.toArray(new String[reducedExtensionSet.size()]);
    }

    /**
     * Returns {@code true} if the filter accepts the file; that is, if a
     * file chooser using the filter would display the file.
     *
     * @param file the file to test
     * @return {@code true} if the file represents a folder or matches one
     * of the filter's extensions
     */
    @Override
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        }

        String name = file.getName();
        String extension;
        int dot = name.indexOf('.');
        if (dot >= 0) {
            extension = name.substring(dot + 1);
        } else {
            extension = "";
        }

        return matches(extension);
    }

    /**
     * Returns a human-friendly description of the filter.
     *
     * @return a description of the file filter
     */
    @Override
    public String getDescription() {
        return desc;
    }

    /**
     * Returns {@code true} if the specified extension matches one of the
     * extensions in the filter.
     *
     * @param extension the extension (not including the '.' character)
     * @return {@code true} if the extension matches one of the extensions
     * accepted by this filter
     */
    public boolean matches(String extension) {
        if (extension == null) {
            throw new NullPointerException("extension");
        }
        extension = extension.toLowerCase(Locale.CANADA);

        for (int i = 0; i < exts.length; ++i) {
            if (exts[i].equals(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a string description of the filter for debugging purposes.
     *
     * @return a string describing the filter
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + desc + '}';
    }
}
