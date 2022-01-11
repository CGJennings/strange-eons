package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.plugins.PluginContextFactory;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import javax.swing.SwingUtilities;

/**
 * A support class that helps the app implement script runner mode. Script
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
final class ScriptRunnerModeHelper implements ScriptRunnerState {
    private static final int STATE_NOT_STARTED = -1;
    private static final int STATE_STARTED = 0;
    private static final int STATE_FINISHED = 1;

    private volatile int runState = STATE_NOT_STARTED;
    private volatile boolean keepAlive;
    private final File script;

    /**
     * Creates a helper for the specified script file.
     * @param scriptFile the script file to run; typically that passed as the
     *    script runner argument
     */
    ScriptRunnerModeHelper(File scriptFile) {
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
                StrangeEons.log.log(Level.SEVERE, "uncaught exception thrown by \"" + script + '\"', t);
            } finally {
                runState = STATE_FINISHED;
            }
        }

        if (!keepAlive) {
            StrangeEons.getWindow().exitApplication(false);
        }
    }

    @Override
    public boolean isStarted() {
        return runState >= STATE_STARTED;
    }

    @Override
    public boolean isFinished() {
        return runState >= STATE_FINISHED;
    }

    @Override
    public File getFile() {
        return script;
    }

    @Override
    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    @Override
    public boolean getKeepAlive() {
        return keepAlive;
    }
}
