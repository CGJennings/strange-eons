package ca.cgjennings.apps.arkham;

import java.io.File;

/**
 * Describes the script runner mode state. Script runner mode is activated by
 * passing a command line argument {@code -run} (or {@code --run}) and a file
 * path for a script file. When activated, the app will start without making its
 * windows visible, then attempt to run the script file specified on the command
 * line in the event dispatch thread. Once the script returns, the app will exit
 * unless the {@link #setKeepAlive keep alive} flag is set. If the script cannot
 * be read, or the script throws an uncaught exception, an error message is
 * printed and the app exits (whether the keep alive flag is set or not).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.2
 */
public interface ScriptRunnerState {

    /**
     * Returns the script file to be run.
     *
     * @return the file that was passed as the
     */
    File getFile();

    /**
     * Returns whether Strange Eons will continue running after the main script
     * completes.
     *
     * @return true if Strange Eons will continue running after the end of the
     * script file is reached
     * @see #setKeepAlive
     */
    boolean getKeepAlive();

    /**
     * Returns whether the main script file has finished running. Note that if
     * the script installs event handlers or starts new threads, those parts of
     * the script can continue to run after the script finishes. If you think of
     * the script as a function, then this method returns true as soon as the
     * script has "returned."
     *
     * @return true if the script file
     */
    boolean isFinished();

    /**
     * Returns whether the process of running the script has started. Once true,
     * it will remain true until the application has terminated. For example,
     * this would return false while an extension plug-in was being initialized
     * because the script is not run until the app is ready to start, that is,
     * after the main app window would normally become visible. If called from
     * the script file itself, it would always return true.
     *
     * @return true if the script has started to load and run, even if it since
     * finished; false otherwise
     */
    boolean isStarted();

    /**
     * Sets whether Strange Eons will exit after the main script completes. By
     * default, as soon as the specified script runs the app will exit. The
     * script itself can prevent this by calling this method (for example, using
     * {@code Eons.scriptRunningMode.keepAlive = true}). Scripts might wish to
     * do this if they start other threads or install event handlers.
     *
     * @param keepAlive if true, the app will not exit when the end of the
     * script file is reached
     */
    void setKeepAlive(boolean keepAlive);
}
