package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.plugins.PluginContextFactory;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import javax.swing.SwingUtilities;

/**
 * A support class that helps the app implement script running mode. Script
 * running mode is activated by passing a command line argument {@code -run} (or
 * {@code --run}) and a file path for a script file. When activated, the app
 * will start without making its windows visible, then attempt to run the script
 * file specified on the command line in the event dispatch thread. Once the
 * script returns, the app will exit unless the {@link #setKeepAlive keep alive}
 * flag is set. If the script cannot be read, or the script throws an uncaught
 * exception, an error message is printed and the app exits (whether the keep
 * alive flag is set or not).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.2
 */
public final class ScriptRunningModeHelper {
    private static final int STATE_NOT_STARTED = -1;
    private static final int STATE_STARTED = 0;
    private static final int STATE_FINISHED = 1;

    private volatile int runState = STATE_NOT_STARTED;
    private volatile boolean keepAlive;
    private final File script;

    ScriptRunningModeHelper(File scriptFile) {
        this.script = Objects.requireNonNull(scriptFile);
    }

    /**
     * Runs the script. Called from the app once it has finished starting up.
     * When the script file completes, it will exit Strange Eons unless keep
     * alive mode was enabled.
     */
    void run() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalThreadStateException();
        }
        if (runState != STATE_NOT_STARTED) {
            throw new IllegalStateException();
        }

        runState = STATE_STARTED;

        String toRun = null;
        try {
            toRun = ProjectUtilities.getFileAsString(script, ProjectUtilities.ENC_SCRIPT);
        } catch (IOException loadFail) {
            keepAlive = false;
            System.err.println("Could not read script \"" + script + '"');
            System.err.println(loadFail.getLocalizedMessage());
        }

        if (toRun != null) {
            try {
                ScriptMonkey monkey = new ScriptMonkey(script.getName());
                monkey.bind(PluginContextFactory.createDummyContext());
                monkey.eval(toRun);
            } catch (Throwable t) {
                // uncaught exceptions will be printed to the console
                // for debugging purposes, then the app will exit
                keepAlive = false;
                System.err.println("Uncaught exception thrown by script \"" + script + '\"');
                t.printStackTrace();
            } finally {
                runState = STATE_FINISHED;
            }
        }

        if (!keepAlive) {
            StrangeEons.getWindow().exitApplication(false);
        }
    }

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
    public boolean isStarted() {
        return runState >= STATE_STARTED;
    }

    /**
     * Returns whether the main script file has finished running. Note that if
     * the script installs event handlers or starts new threads, those parts of
     * the script can continue to run after the script finishes. If you think of
     * the script as a function, then this method returns true as soon as the
     * script has "returned."
     *
     * @return true if the script file
     */
    public boolean isFinished() {
        return runState >= STATE_FINISHED;
    }

    /**
     * Returns the script file to be run.
     *
     * @return the file that was passed as the
     */
    public File getFile() {
        return script;
    }

    /**
     * Sets whether Strange Eons will exit after the main script completes. By
     * default, as soon as the specified script runs the app will exit. The
     * script itself can prevent this by calling this method (for example, using
     * {@code Eons.scriptRunningMode.keepAlive = true}). Scripts might wish to
     * do this if they start other threads or install event handlers.
     *
     * @param keepAlive if true, the app will not exit when the end of the
     *     script file is reached
     */
    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    /**
     * Returns whether Strange Eons will continue running after the main script
     * completes.
     *
     * @return true if Strange Eons will continue running after the end of the
     *     script file is reached
     * @see #setKeepAlive
     */
    public boolean getKeepAlive() {
        return keepAlive;
    }
}
