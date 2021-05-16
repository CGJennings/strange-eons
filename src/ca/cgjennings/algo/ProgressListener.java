package ca.cgjennings.algo;

import java.util.EventListener;

/**
 * An interface implemented by classes that wish to listen to a
 * {@link MonitoredAlgorithm} to be notified when progress is made.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface ProgressListener extends EventListener {

    /**
     * Called to indicate that an algorithm has made progress towards
     * completion. If the listener returns {@code true}, it is a hint that
     * the operation should be cancelled if possible.
     *
     * @param source the source of the progress event
     * @param progress a value between 0 (none) and 1 (algorithm complete)
     * @return {@code true} if the callee wishes to hint that the algorithm
     * be cancelled
     */
    boolean progressUpdate(Object source, float progress);
}
