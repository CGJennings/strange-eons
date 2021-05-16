package ca.cgjennings.algo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.CancellationException;

/**
 * An input stream that can be monitored using a {@link ProgressListener}. This
 * is similar to a {@code ProgressMonitorInputStream}, except that it is
 * more flexible in how the progress updates are used.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class MonitoredInputStream extends FilterInputStream {

    /**
     * Creates a new monitored input stream. The size of the incoming input
     * stream will be estimated automatically.
     *
     * @param li the listener to update
     * @param in the input stream to monitor
     * @throws IOException if an I/O error occurs
     */
    public MonitoredInputStream(ProgressListener li, InputStream in) throws IOException {
        this(li, -1L, in);
    }

    /**
     * Creates a new monitored input stream.
     *
     * @param li the listener to update
     * @param size the number of bytes in the input stream
     * @param in the input stream to monitor
     * @throws IOException if an I/O error occurs
     */
    public MonitoredInputStream(ProgressListener li, long size, InputStream in) throws IOException {
        super(in);
        if (li == null) {
            throw new NullPointerException("listener");
        }
        if (size == -1) {
            size = in.available();
        }
        this.li = li;
        this.size = size;
    }

    /**
     * Creates a new monitored input stream that reads from a file.
     *
     * @param li the listener to update
     * @param in the file to monitor
     * @throws IOException if an I/O error occurs
     */
    public MonitoredInputStream(ProgressListener li, File in) throws IOException {
        this(
                li,
                in.length() > Integer.MAX_VALUE ? -1 : (int) in.length(),
                new FileInputStream(in));
    }

    /**
     * Overrides {@code FilterInputStream.read} to update progress after
     * the read.
     */
    @Override
    public int read() throws IOException {
        int c = in.read();
        if (c >= 0) {
            ++nread;
            updateProgress();
        }
        return c;
    }

    /**
     * Overrides {@code FilterInputStream.read} to update progress after
     * the read.
     */
    @Override
    public int read(byte b[]) throws IOException {
        int nr = in.read(b);
        if (nr > 0) {
            nread += nr;
            updateProgress();
        }
        return nr;
    }

    /**
     * Overrides {@code FilterInputStream.read} to update progress after
     * the read.
     */
    @Override
    public int read(byte b[],
            int off,
            int len) throws IOException {
        int nr = in.read(b, off, len);
        if (nr > 0) {
            nread += nr;
            updateProgress();
        }
        return nr;
    }

    /**
     * Overrides {@code FilterInputStream.skip} to update progress after
     * the skip.
     */
    @Override
    public long skip(long n) throws IOException {
        long nr = in.skip(n);
        if (nr > 0) {
            nread += nr;
            updateProgress();
        }
        return nr;
    }

    /**
     * Overrides {@code FilterInputStream.reset} to reset progress listener
     * as well as the stream.
     */
    @Override
    public synchronized void reset() throws IOException {
        in.reset();
        nread = size - in.available();
        updateProgress();
    }

    private void updateProgress() throws InterruptedIOException {
        long now = System.currentTimeMillis();
        if (now - lastUpdate > UPDATE_DELAY_MS) {
            float p = Math.max(0, Math.min(1, nread / (float) size));
            if (p != p) {
                p = 1f;
            }
            p = Math.max(p, maxP);
            maxP = p;
            if (li.progressUpdate(this, p)) {
                throw new CancellationException();
            }
            lastUpdate = now;
        }
    }
    private float maxP = 0;
    private static final long UPDATE_DELAY_MS = 250;
    private long lastUpdate = System.currentTimeMillis();
    private ProgressListener li;
    private long size;
    private long nread;
}
