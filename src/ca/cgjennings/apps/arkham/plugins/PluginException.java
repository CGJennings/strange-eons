package ca.cgjennings.apps.arkham.plugins;

/**
 * A PluginException is thrown when a plug-in cannot be instantiated or there is
 * an unexpected error in the plug-in system.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class PluginException extends Exception {

    private static final long serialVersionUID = 6_465_456_564_565L;

    public PluginException(Throwable cause) {
        super(cause);
    }

    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginException(String message) {
        super(message);
    }

    public PluginException() {
    }
}
