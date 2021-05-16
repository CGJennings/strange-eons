package ca.cgjennings.io;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Listeners that register a file with a {@code FileChangeMonitor} receive
 * notification when it changes.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class FileChangeMonitor {

    /**
     * The kind of change detected by the monitor.
     */
    public enum ChangeType {
        /**
         * The file previously did not exist but now does.
         */
        CREATED,
        /**
         * The file previously existed but has been deleted.
         */
        DELETED,
        /**
         * The file has been modified.
         */
        MODIFIED
    }
    /**
     * A single shared instance.
     */
    private static final FileChangeMonitor shared = new FileChangeMonitor();
    /**
     * The default {@code checkPeriod} used if none is given.
     */
    private static long defaultCheckPeriod = 4_000;
    /**
     * The timer that triggers file checks.
     */
    protected Timer timer;
    /**
     * Map from listeners to their tasks.
     */
    protected Map<String, MonitorTask> taskMap;

    /**
     * Creates a new file change monitor which is independent of the shared
     * instance. It is generally preferable to use the shared instance.
     *
     * @see #getSharedInstance()
     */
    public FileChangeMonitor() {
        timer = new Timer(toString(), true);
        taskMap = Collections.synchronizedMap(new HashMap<>());
    }

    /**
     * Return a shared instance of {@code FileChangeMonitor}.
     *
     * @return an shared instance
     */
    public static FileChangeMonitor getSharedInstance() {
        return shared;
    }

    /**
     * Sets a hint describing how often files should be checked for changes by
     * default. Note that this class makes no guarantee about how it monitors
     * files for changes, so this value may have no effect.
     *
     * @param periodInMS the default check period, in milliseconds
     */
    public static synchronized void setDefaultCheckPeriod(long periodInMS) {
        defaultCheckPeriod = periodInMS;
    }

    /**
     * Returns a hint describing how often files should be checked for changes
     * by default. Note that this class makes no guarantee about how it monitors
     * files for changes, so this value may have no effect. (It will only have
     * an effect if the class must poll the file system for changes, and even
     * then the exact delay between checks cannot be guaranteed.)
     *
     * @return periodInMS the default check period, in milliseconds
     */
    public static synchronized long getDefaultCheckPeriod() {
        return defaultCheckPeriod;
    }

    /**
     * Removes all listeners being monitored and ends the monitoring thread.
     * Once a monitor has been disposed, it cannot have any more listeners
     * added. If this method is called on the shared instance, it will throw an
     * {@code UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException if called on
     * {@link #getSharedInstance()}
     */
    public void dispose() {
        if (this == shared) {
            throw new UnsupportedOperationException("cannot dispose the shared instance");
        }
        timer.cancel();
        timer = null;
        taskMap = null;
    }

    /**
     * Begin monitoring a file for changes. Changes will cause the listener's
     * {@link FileChangeListener#fileChanged} method to be called.
     *
     * @param listener the listener to notify when {@code file} changes
     * @param file the file to monitor
     * @throws NullPointerException if {@code listener} or
     * {@code file} is {@code null}
     * @throws IllegalStateException if this monitor has been {@link #dispose}d
     */
    public void addFileChangeListener(FileChangeListener listener, File file) {
        addFileChangeListener(listener, file, defaultCheckPeriod);
    }

    /**
     * Begin monitoring a file for changes. Changes will cause the listener's
     * {@link FileChangeListener#fileChanged} method to be called. The value of
     * {@code updatePeriod} is an approximate <i>maximum</i> delay, in
     * milliseconds, before a change in file state is detected. The monitor does
     * not guarantee that {@code updatePeriod} will be honoured. If
     * {@code listener} has already been registered to monitor
     * {@code file}, the existing monitor will be removed and it will be
     * replaced by one with the new {@code updatePeriod}. If the
     * {@code updatePeriod} is 0, then the monitor is not added (it is
     * removed if it already exists).
     *
     * @param listener the listener to notify when {@code file} changes
     * @param file the file to monitor
     * @param updatePeriod the approximate maximum delay before change
     * notification takes place
     * @throws NullPointerException if {@code listener} or
     * {@code file} is {@code null}
     * @throws IllegalArgumentException if {@code updatePeriod} is negative
     * @throws IllegalStateException if this monitor has been {@link #dispose}d
     * of
     * @see #getDefaultCheckPeriod()
     */
    public void addFileChangeListener(FileChangeListener listener, File file, long updatePeriod) {
        if (updatePeriod < 0) {
            throw new IllegalArgumentException("updatePeriod cannot be negative: " + updatePeriod);
        }
        if (taskMap == null) {
            throw new IllegalStateException("monitor has been disposed");
        }
        if (listener == null) {
            throw new NullPointerException("listener cannot be null");
        }
        if (file == null) {
            throw new NullPointerException("file cannot be null");
        }

        if (PURGE_RATE > 0 && ++listenersAddedSinceLastPurge == PURGE_RATE) {
            listenersAddedSinceLastPurge = 0;
            purge();
        }

        String id = createListenerID(listener, file);
        if (taskMap.containsKey(id)) {
            removeFileChangeListener(listener, file);
        }

        // don't really add if period == 0; allows for "disable" setting
        // without complicating client code
        if (updatePeriod == 0) {
            removeFileChangeListener(listener, file);
            return;
        }

        MonitorTask task = new MonitorTask(file, listener);
        taskMap.put(id, task);
        timer.schedule(task, updatePeriod, updatePeriod);
    }

    /**
     * Stop monitoring a specific file registered with this listener. Note that
     * the listener might be called one more time after this method is called if
     * a file change was already being handled when the method was called.
     *
     * @param listener the listener to stop notifying
     * @param file the file to stop notifying the listener about
     */
    public void removeFileChangeListener(FileChangeListener listener, File file) {
        String id = createListenerID(listener, file);
        MonitorTask task = taskMap.get(id);
        if (task != null) {
            taskMap.remove(id);
            task.cancel();
            timer.purge();
        }
    }

    /**
     * Stop monitoring all files that are registered with a listener. This is
     * more efficient than removing each file individually.
     *
     * @param listener the listener to stop monitoring
     */
    public void removeFileChangeListener(FileChangeListener listener) {
        LinkedList<String> ids = new LinkedList<>();
        String idPrefix = String.valueOf(System.identityHashCode(listener)) + ":";

        // find all tasks that belong to this listener
        synchronized (taskMap) {
            for (String s : taskMap.keySet()) {
                if (s.startsWith(idPrefix)) {
                    ids.add(s);
                }
            }
        }

        // stop each of the discovered tasks
        for (String id : ids) {
            MonitorTask task = taskMap.get(id);
            taskMap.remove(id);
            stopMonitorTask(task);
        }

        timer.purge();
    }

    /**
     * Remove all monitor tasks that are no longer valid because the listener
     * was garbage collected without removing itself. This may be done
     * automatically from time to time, but you can force it to occur at any
     * time by calling this method.
     */
    public void purge() {
        LinkedList<String> ids = new LinkedList<>();

        // find all tasks with invalid listeners
        synchronized (taskMap) {
            for (String s : taskMap.keySet()) {
                if (!taskMap.get(s).isListenerValid()) {
                    ids.add(s);
                }
            }
        }

        // stop each of the discovered tasks
        for (String id : ids) {
            MonitorTask task = taskMap.get(id);
            taskMap.remove(id);
            stopMonitorTask(task);
        }

        timer.purge();
    }

    /**
     * If positive, purge() is called every time PURGE_RATE listeners have been
     * added.
     */
    private static final int PURGE_RATE = 100;
    private int listenersAddedSinceLastPurge = 0;

    private void stopMonitorTask(MonitorTask task) {
        task.cancel();
    }

    /**
     * Converts a ({@code FileChangeListener}, {@code File}) pair into
     * a unique identifying string.
     *
     * @param FileChangeListener the listener of the pair
     * @param File the file of the pair
     * @return a {@code String} that uniquely identifies this pair
     */
    private String createListenerID(FileChangeListener listener, File file) {
        return String.valueOf(System.identityHashCode(listener)) + ":" + file.getAbsolutePath();
    }

    /**
     * A {@code TimerTask} that polls files for changes and fires change
     * events as needed.
     */
    private static class MonitorTask extends TimerTask {

        protected File monitoredFile;
        protected long timeStamp;
        protected boolean exists;
        protected SoftReference<FileChangeListener> listener;

        public MonitorTask(File file, FileChangeListener listener) {
            monitoredFile = file;
            this.listener = new SoftReference<>(listener);
            updateFileState(false);
        }

        protected boolean isListenerValid() {
            return listener.get() != null;
        }

        /**
         * Update or initialize the records of the file's state.
         *
         * @param fireEvent if {@code true}, fire a change event on the
         * listener if state has changed
         */
        protected void updateFileState(boolean fireEvent) {
            boolean newExists;
            long newTimeStamp;

            if (monitoredFile.exists()) {
                newExists = true;
                newTimeStamp = monitoredFile.lastModified();
            } else {
                newExists = false;
                newTimeStamp = 0L;
            }

            if (fireEvent) {
                FileChangeListener listener = this.listener.get();
                if (listener != null) {
                    if (newExists != exists) {
                        if (newExists) {
                            listener.fileChanged(monitoredFile, ChangeType.CREATED);
                        } else {
                            listener.fileChanged(monitoredFile, ChangeType.DELETED);
                        }
                    } else if (newTimeStamp != timeStamp) {
                        listener.fileChanged(monitoredFile, ChangeType.MODIFIED);
                    }
                }
            }

            exists = newExists;
            timeStamp = newTimeStamp;
        }

        @Override
        public void run() {
            updateFileState(true);
        }
    }
}
