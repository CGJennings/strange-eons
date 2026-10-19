package ca.cgjennings.ui.filedialog;

import java.util.Arrays;
import javax.swing.filechooser.FileFilter;

/**
 * Helpers for translating Swing file filters into native dialog filters.
 */
public final class FileFilters {

    private FileFilters() {
    }

    /**
     * Returns the non-empty extensions of an extension-based filter, or
     * {@code null} if the filter is not extension based or lists no non-empty
     * extensions.
     */
    public static String[] extensions(FileFilter filter) {
        String[] exts;
        if (filter instanceof javax.swing.filechooser.FileNameExtensionFilter) {
            exts = ((javax.swing.filechooser.FileNameExtensionFilter) filter).getExtensions();
        } else if (filter instanceof ca.cgjennings.ui.FileNameExtensionFilter) {
            exts = ((ca.cgjennings.ui.FileNameExtensionFilter) filter).getExtensions();
        } else {
            return null;
        }
        exts = Arrays.stream(exts).filter(e -> !e.isEmpty()).toArray(String[]::new);
        return exts.length == 0 ? null : exts;
    }
}
