package ca.cgjennings.io;

import java.io.*;

/**
 * A transparent input stream filter that counts bytes that pass through it.
 */
public class CountingInputStream extends FilterInputStream {
    // prevent counting multiple times when read methods are called recursively

    private transient boolean isCounting;
    private long byteCount = 0;

    /**
     * Create a new counting filter that will count the bytes read through it
     * from <code>in</code>.
     *
     * @param in the stream to count bytes from
     */
    public CountingInputStream(InputStream in) {
        super(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.toString() + ": bytes read " + getBytesRead();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int read() throws IOException {
        boolean wasCounting = isCounting;
        isCounting = true;
        int i = super.read();
        isCounting = wasCounting;

        if (!isCounting && (i != -1)) {
            byteCount++;
        }
        return i;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        boolean wasCounting = isCounting;
        isCounting = true;

        int i = super.read(b, off, len);
        isCounting = wasCounting;

        if (!isCounting && (i != -1)) {
            byteCount += i;
        }

        return i;
    }

    /**
     * Returns the number of bytes read from this stream.
     *
     * @return long the number of bytes read through this stream.
     */
    public synchronized long getBytesRead() {
        return byteCount;
    }
}
