package ca.cgjennings.algo;

import java.awt.EventQueue;
import javax.swing.JProgressBar;

/**
 * A progress listener that safely updates a progress bar from any thread.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class ProgressBarAdapter implements ProgressListener {

    private final JProgressBar bar;

    /**
     * Creates a new progress bar adapter that listens for progress updates and
     * updates the specified progress bar accordingly.
     *
     * @param bar the progress bar to update
     */
    public ProgressBarAdapter(JProgressBar bar) {
        this.bar = bar;
    }

    @Override
    public synchronized boolean progressUpdate(final Object source, final float progress) {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(() -> {
                progressUpdate(source, progress);
            });
        } else {
            if (bar.getMinimum() != 0) {
                bar.setMinimum(0);
            }
            if (bar.getMaximum() != 10_000) {
                bar.setMaximum(10_000);
            }
            if (bar.isIndeterminate()) {
                bar.setIndeterminate(false);
            }
            int pos = (int) (10000f * progress + 0.5f);
            if (bar.getValue() != pos) {
                bar.setValue(pos);
            }
        }
        return false;
    }
}
