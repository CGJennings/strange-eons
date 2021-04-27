package ca.cgjennings.algo;

/**
 * A <code>MonitoredAlgorithm</code> is an algorithm that can report on its
 * progress as it completes. It supports a single listener (although that
 * listener can chain to other listeners if needed) that will be notified as the
 * algorithm progresses.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface MonitoredAlgorithm {

    /**
     * Sets the progress listener that will listen for progress on this
     * algorithm, replacing the existing listener (if any). A listener should
     * only be set before the algorithm begins executing, not while it is
     * already in progress.
     *
     * @param li the listener to set (may be <code>null</code>)
     * @return the previous listener, or <code>null</code>
     */
    ProgressListener setProgressListener(ProgressListener li);

}
