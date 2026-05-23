package ca.cgjennings.algo;

/**
 * A helper class for creating {@link MonitoredAlgorithm}s.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class ProgressHelper implements MonitoredAlgorithm {

    private Object source = this;

    /**
     * Creates a new helper with no listener that will report itself as the
     * event source.
     */
    public ProgressHelper() {
    }

    /**
     * Creates a new helper with the specified source and listener.
     *
     * @param source the source that will be passed to listener events
     * @param li the listener to set (may be {@code null})
     */
    public ProgressHelper(Object source, ProgressListener li) {
        this(source, li, 0);
    }

    /**
     * Creates a new helper with the specified source, listener, and step count.
     *
     * @param source the source that will be passed to listener events
     * @param li the listener to set (may be {@code null})
     * @param stepCount the number of steps that must be executed before
     * completion
     */
    public ProgressHelper(Object source, ProgressListener li, int stepCount) {
        setSource(source);
        setProgressListener(li);
        setStepCount(stepCount);
    }

    /**
     * Sets the progress listener that will listen for progress on this
     * algorithm, replacing the existing listener (if any). A listener should
     * only be set before the algorithm begins executing, not while it is
     * already in progress.
     *
     * @param li the listener to set (may be {@code null})
     * @return the previous listener, or {@code null}
     */
    @Override
    public ProgressListener setProgressListener(ProgressListener li) {
        ProgressListener old = this.li;
        this.li = li;
        return old;
    }

    /**
     * Returns the source reported by listener events.
     *
     * @return the source that will be passed to listener events
     */
    public Object getSource() {
        return source;
    }

    /**
     * Sets the object that will be reported as the source of listener events.
     * The default source is {@code this}, which is suitable if the algorithm
     * subclasses this class.
     *
     * @param source the apparent source of events
     */
    public void setSource(Object source) {
        this.source = source;
    }

    /**
     * Sets the number of steps required to complete the algorithm and resets
     * the currently completed number of steps to 0. If the algorithm is
     * multithreaded, this should be called before the threads begin executing.
     *
     * @param steps the number of steps that must be executed before completion
     */
    public void setStepCount(int steps) {
        max = steps;
        current = 0;
    }

    /**
     * Resets the number of completed steps. If the algorithm is multithreaded,
     * this should be called before (or after) the threads start (or finish)
     * executing.
     */
    public void resetProgress() {
        current = 0;
        if (li != null) {
            li.progressUpdate(getSource(), 0);
        }
    }

    /**
     * Called when progress has been made to update the progress listener, if
     * any. The listener will receive a new progress event if at least 100 ms
     * have passed since the last event was fired.
     *
     * @param stepCount the number of steps completed
     */
    public void setCompletedSteps(int stepCount) {
        if (li == null) {
            return;
        }
        if (stepCount == 0) {
            return;
        }
        if (stepCount < 0) {
            throw new IllegalArgumentException("negative step count: " + stepCount);
        }
        synchronized (this) {
            current = stepCount;
            if (current > max) {
                current = max;
            }
            long now = System.nanoTime();
            if (now - last > UPDATE_DELAY) { // 100 ms
                last = now;
                li.progressUpdate(getSource(), current / (float) max);
            }
        }
    }

    /**
     * Called when progress has been made to update the progress listener, if
     * any. This method adds to the current total number of completed steps
     * rather than setting an explicit total, and so is suited to use with
     * multithreaded algorithms. The listener will receive a new progress event
     * if at least 100 ms have passed since the last event was fired.
     *
     * @param stepCount the number of steps completed
     */
    public void addCompletedSteps(int stepCount) {
        if (li == null) {
            return;
        }
        if (stepCount == 0) {
            return;
        }
        if (stepCount < 0) {
            throw new IllegalArgumentException("negative step count: " + stepCount);
        }
        synchronized (this) {
            current += stepCount;
            if (current > max) {
                current = max;
            }
            long now = System.nanoTime();
            if (now - last > UPDATE_DELAY) { // 100 ms
                last = now;
                li.progressUpdate(getSource(), current / (float) max);
            }
        }
    }

    private long last = System.nanoTime();
    private int max;
    private int current;
    private ProgressListener li;
    private static final long UPDATE_DELAY = 200_000_000L;
}
