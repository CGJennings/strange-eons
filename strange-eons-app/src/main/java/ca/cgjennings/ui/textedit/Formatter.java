package ca.cgjennings.ui.textedit;

/**
 * Interface implemented by objects that can format source code.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public interface Formatter {

    /**
     * Format and return the given source code string.
     *
     * @param code the source code to format
     * @return the formatted code
     */
    String format(String code);
}
