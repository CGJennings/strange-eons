package ca.cgjennings.io;

import java.io.File;

/**
 * A listener for file change events.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface FileChangeListener {

    /**
     * This method is called when a change is detected in a file being monitored
     * by a {@link FileChangeMonitor}. Note that this method will typically be
     * called from a different thread than the one that requested notification.
     *
     * @param f the file that has changed
     */
    public abstract void fileChanged(File f, FileChangeMonitor.ChangeType type);
}
