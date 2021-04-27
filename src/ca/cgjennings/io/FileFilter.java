package ca.cgjennings.io;

import java.io.File;

/**
 * An abstract file filter that can be used with both file chooser APIs and
 * <code>File.listFiles</code>.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public abstract class FileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {

    /**
     * Converts an I/O file filter into a dual filter that can be used for I/O
     * or for file choosers.
     *
     * @param ioFilter the I/O filter
     * @param description a description used when the filter is used in a file
     * chooser
     * @return a dual filter
     */
    public static FileFilter adapt(final java.io.FileFilter ioFilter, final String description) {
        return new FileFilter() {
            @Override
            public boolean accept(File f) {
                return ioFilter.accept(f);
            }

            @Override
            public String getDescription() {
                return description;
            }
        };
    }

    /**
     * Converts an file chooser filter into a dual filter that can be used for
     * I/O or for file choosers.
     *
     * @param chooserFilter a file chooser filter
     * @return a dual filter
     */
    public static FileFilter adapt(final javax.swing.filechooser.FileFilter chooserFilter) {
        return new FileFilter() {
            @Override
            public boolean accept(File f) {
                return chooserFilter.accept(f);
            }

            @Override
            public String getDescription() {
                return chooserFilter.getDescription();
            }
        };
    }
}
