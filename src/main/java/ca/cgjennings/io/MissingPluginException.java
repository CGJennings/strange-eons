package ca.cgjennings.io;

import java.io.IOException;
import resources.Language;

/**
 * An exception that is thrown while reading a file when the file requires a
 * plug-in that is not installed.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class MissingPluginException extends IOException {

    /**
     * The name of the plug-in that isn't available.
     *
     * @param pluginName a string that identifies the missing plug-in
     */
    public MissingPluginException(String pluginName) {
        super(Language.string("rk-err-missing-plugin", pluginName));
    }

    /**
     * This constructor is not intended for public use; the method is public to
     * cross a package barrier.
     *
     * @param c code
     * @param n if {@code null}, the value of {@code c} will be used as the
     * message for the exception
     */
    public MissingPluginException(String c, String n) {
        super((n == null) ? c : Language.string("rk-err-missing-game", c, n));
    }
}
