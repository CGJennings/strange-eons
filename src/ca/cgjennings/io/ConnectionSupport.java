package ca.cgjennings.io;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Provides some static helper methods that make it easier to enable features of
 * HTTP connections such as server-side compression.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class ConnectionSupport {

    private ConnectionSupport() {
    }

    /**
     * Sets request properties on the connection that allow the server to send
     * compressed data.
     *
     * @param c the connection to modify
     */
    public static void enableCompression(URLConnection c) {
        c.setRequestProperty("Accept-Encoding", "gzip, deflate");
    }

    /**
     * Returns a buffered input stream for the connection. Connects to the
     * server and obtains an input stream. If the server is sending compressed
     * data, the input stream will be wrapped in an appropriate decompressing
     * filter stream so that any compression is transparent to the client.
     *
     * <p>
     * Although the returned stream is guaranteed to be buffered, it is not
     * guaranteed to be assignable to {@code BufferedInputStream}.
     *
     * @param c the connection to obtain an input stream for
     * @param bufferSizeHint the suggested buffer size, or -1 to use default
     * values
     * @return a buffered input stream for the content that will transparently
     * decompress the stream if compressed
     * @throws IOException if an I/O error occurs while connecting to the server
     */
    public static InputStream openStream(URLConnection c, int bufferSizeHint) throws IOException {
        c.connect();
        String encoding = c.getContentEncoding();

        if (bufferSizeHint < 1) {
            bufferSizeHint = 64 * 1_024; // default 64k buffer size
        }

        InputStream in = c.getInputStream();
        InputStream wrapped = null;

        if (encoding != null) {
            if (encoding.equalsIgnoreCase("gzip")) {
                StrangeEons.log.log(Level.FINE, "reading server-compressed gzip data");
                wrapped = new GZIPInputStream(in, bufferSizeHint);
            } else if (encoding.equalsIgnoreCase("deflate")) {
                StrangeEons.log.log(Level.FINE, "reading server-compressed deflate data");
                wrapped = new InflaterInputStream(in, new Inflater(true), bufferSizeHint);
            }
        }

        if (wrapped == null) {
            wrapped = new BufferedInputStream(in, bufferSizeHint);
        }
        return wrapped;
    }
}
