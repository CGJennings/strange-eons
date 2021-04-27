package ca.cgjennings.apps.arkham.plugins;

/**
 * This is an unchecked exception that can be thrown from within a plug-in when
 * an unexpected error occurs from which the plug-in cannot recover. If it is
 * thrown while the plug-in is being initialized, it will prevent the plug-in
 * form being loaded.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class PluginError extends RuntimeException {

    /**
     * Creates a plug-in error with no specific error message.
     */
    public PluginError() {
    }

    /**
     * Creates a plug-in error with the given error message.
     *
     * @param message an error message
     */
    public PluginError(String message) {
        super(message);
    }

    /**
     * Creates a plug-in error with the specified exception set as the root
     * cause.
     *
     * @param cause the exception that caused the plug-in error to be thrown
     */
    public PluginError(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a plug-in error with the given error message that indicates the
     * specified exception as the root cause.
     *
     * @param message an error message
     * @param cause the exception that caused the plug-in error to be thrown
     */
    public PluginError(String message, Throwable cause) {
        super(message, cause);
    }
}
