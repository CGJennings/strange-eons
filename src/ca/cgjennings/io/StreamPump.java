package ca.cgjennings.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Provides high-performance copying from input streams to output streams and
 * from readers to writers. This class is thread safe.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class StreamPump {

    /**
     * This class cannot be instantiated.
     */
    private StreamPump() {
    }

    /**
     * Copy the bytes from one stream to another without closing either.
     *
     * @param in the stream to read from
     * @param out the stream to copy the read bytes to
     * @throws IOException if an I/O error occurs while copying
     * @throws NullPointerException if either parameter is {@code null}
     */
    public static void copy(final InputStream in, final OutputStream out) throws IOException {
        if (in == null) {
            throw new NullPointerException("in");
        }
        if (out == null) {
            throw new NullPointerException("out");
        }

        ReadableByteChannel inCh = Channels.newChannel(in);
        WritableByteChannel outCh = Channels.newChannel(out);

        // transferTo is about 4x slower on my system
//		if( inCh instanceof FileChannel ) {
//			final FileChannel inFch = (FileChannel) inCh;
//			inFch.transferTo( inFch.position(), inFch.size(), outCh );
//		} else if( outCh instanceof FileChannel ) {
//			final FileChannel outFch = (FileChannel) outCh;
//			outFch.transferFrom( inCh, outFch.position(), Long.MAX_VALUE );
//		}
        final ByteBuffer buffer = allocateByteBuffer();
        try {
            while (inCh.read(buffer) != -1) {
                // prepare the buffer to be drained
                buffer.flip();
                // write to the channel, may block
                outCh.write(buffer);
                // If partial transfer, shift remainder down
                // If buffer is empty, same as doing clear()
                buffer.compact();
            }
            // EOF will leave buffer in fill state
            buffer.flip();
            // make sure the buffer is fully drained.
            while (buffer.hasRemaining()) {
                outCh.write(buffer);
            }
        } finally {
            buffer.clear();
            recycleByteBuffer(buffer);
        }
    }

    /**
     * Reads an input stream until the end of the stream is reached. This is
     * typically used when the stream is being read for the side effects of one
     * or more stream filters attached to it, such as computing a CRC or hash
     * value.
     *
     * @param in the stream to empty
     * @throws IOException if an I/O error occurs while reading the stream
     */
    public static void drain(final InputStream in) throws IOException {
        if (in == null) {
            throw new NullPointerException("in");
        }

        ReadableByteChannel inCh = Channels.newChannel(in);

        final ByteBuffer buffer = allocateByteBuffer();
        try {
            while (inCh.read(buffer) != -1) {
                buffer.clear();
            }
        } finally {
            buffer.clear();
            recycleByteBuffer(buffer);
        }
    }

    /**
     * Copy the characters from a reader to a writer without closing either.
     *
     * @param in the source to copy from
     * @param out the destination to copy to (may use a different encoding)
     * @throws IOException if an I/O error occurs while copying
     * @throws NullPointerException if either parameter is {@code null}
     */
    public static void copy(final Reader in, final Writer out) throws IOException {
        if (in == null) {
            throw new NullPointerException("in");
        }
        if (out == null) {
            throw new NullPointerException("out");
        }

        final char[] buffer = allocateCharBuffer();
        try {
            int read = -1;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        } finally {
            recycleCharBuffer(buffer);
        }
    }

    private static ByteBuffer allocateByteBuffer() {
        // see if there is a buffer we can reuse
        synchronized (byteBuffers) {
            for (Iterator<SoftReference<ByteBuffer>> it = byteBuffers.iterator(); it.hasNext();) {
                final ByteBuffer buff = it.next().get();
                it.remove();
                if (buff != null) {
                    return buff;
                }
            }
        }

        return ByteBuffer.allocateDirect(BUFFER_SIZE);
    }

    private static void recycleByteBuffer(ByteBuffer buffer) {
        buffer.clear();
        synchronized (byteBuffers) {
            // check if setBufferSize called since we got our buffer
            if (buffer.capacity() == BUFFER_SIZE) {
                byteBuffers.add(new SoftReference<>(buffer));
            }
        }
    }

    private static char[] allocateCharBuffer() {
        // see if there is a buffer we can reuse
        // Note: we synch on byteBuffers instead of charBuffers to avoid
        // possible race conditions
        synchronized (byteBuffers) {
            for (Iterator<SoftReference<char[]>> it = charBuffers.iterator(); it.hasNext();) {
                final char[] buff = it.next().get();
                it.remove();
                if (buff != null) {
                    return buff;
                }
            }
        }

        return new char[BUFFER_SIZE];
    }

    private static void recycleCharBuffer(final char[] buffer) {
        synchronized (byteBuffers) {
            // check if setBufferSize called since we got our buffer
            if (buffer.length == BUFFER_SIZE) {
                charBuffers.add(new SoftReference<>(buffer));
            }
        }
    }

    private static final LinkedList<SoftReference<ByteBuffer>> byteBuffers = new LinkedList<>();
    private static final LinkedList<SoftReference<char[]>> charBuffers = new LinkedList<>();

    private static int BUFFER_SIZE = 128 * 1_024;

    /**
     * Sets the buffer size, in bytes, used for copy operations. Any copy
     * operations that are currently in use will continue to use the old buffer
     * size, but all new copy operations after this method returns will use the
     * new buffer size. The default buffer size was chosen after experimentation
     * in a variety of conditions. Changing the buffer size is only recommended
     * if you intend to experimentally customize the buffer size to a particular
     * applicaiton.
     *
     * @param buffSize the new buffer size, in bytes
     * @throws IllegalArgumentException if the requested buffer size is less
     * than 1 byte
     */
    public static void setBufferSize(int buffSize) {
        if (buffSize < 1) {
            throw new IllegalArgumentException("buffer size must be >= 1: " + buffSize);
        }
        synchronized (byteBuffers) {
            if (buffSize != BUFFER_SIZE) {
                BUFFER_SIZE = buffSize;
                byteBuffers.clear();
                charBuffers.clear();
            }
        }
    }

    /**
     * Returns the current buffer size used for copy operations.
     *
     * @return the buffer size, in bytes
     * @see #setBufferSize(int)
     */
    public static int getBufferSize() {
        synchronized (byteBuffers) {
            return BUFFER_SIZE;
        }
    }
}
