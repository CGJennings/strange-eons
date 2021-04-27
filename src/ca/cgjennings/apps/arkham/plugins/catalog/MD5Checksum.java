package ca.cgjennings.apps.arkham.plugins.catalog;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;

/**
 * Creates checksums for byte sequences using the MD5 algorithm, as obtained
 * from the crypto API. In the the event that the algorithm is unavailable or
 * throws an exception while computing the checksum, the caller will be shielded
 * from this failure while the checksum would normally be computed. However,
 * when the checksum is requested after such an algorithm failure,
 * <code>null</code> will be returned, and if a failed checksum is compared to a
 * known checksum using {@link #matches}, it will always return
 * <code>true</code>.
 *
 * <p>
 * <b>Security Warning:</b> Note that this class is only intended to be used to
 * detect issues such as possibly corrupt downloads. It must not be used when
 * the security of a system depends on the results.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class MD5Checksum {

    private static final String CHECKSUM_ALGORITHM = "MD5";

    private MessageDigest md5;
    private byte[] digest;
    private boolean done;
    private static volatile boolean warned;

    /**
     * Creates a new checksum instance.
     */
    public MD5Checksum() {
        reset();
    }

    /**
     * Resets this checksum instance. Resetting the checksum prepares it to
     * compute a new checksum; the result of the previously computed checksum,
     * if any, will be lost.
     */
    public void reset() {
        digest = null;
        done = false;
        if (md5 == null) {
            try {
                md5 = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
            } catch (NoSuchAlgorithmException ex) {
                // md5 instance will be null
                if (!warned) {
                    warned = true;
                    StrangeEons.log.warning("MD5 not available from crypto provider");
                }
            }
        } else {
            md5.reset();
        }
    }

    /**
     * Processes the next group of bytes to include in the checksum. This is
     * equivalent to <code>update( bytes, 0, bytes.length )</code>.
     *
     * @param bytes an array of bytes representing the next block of data in the
     * stream
     * @throws NullPointerException if the buffer is <code>null</code>
     * @throws IllegalStateException if the checksum has been completed and
     * {@link #reset()} has not been called
     * @see #update(byte[], int, int)
     */
    public void update(byte[] bytes) {
        update(bytes, 0, bytes.length);
    }

    /**
     * Processes the next block of bytes to be included in the checksum.
     *
     * @param buffer the buffer that holds the block of data
     * @param offset the offset into the buffer at which the data block starts
     * @param length the size of the block of data
     * @throws NullPointerException if the buffer is <code>null</code>
     * @throws IllegalStateException if the checksum has been completed and
     * {@link #reset()} has not been called
     * @throws IllegalArgumentException if the length is negative
     * @throws ArrayIndexOutOfBoundsException if the offset or length do not
     * represent valid buffer positions
     * @see #getChecksum()
     */
    public void update(byte[] buffer, int offset, int length) {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        if (done) {
            throw new IllegalStateException("not reset");
        }
        if (md5 == null) {
            return;
        }
        if (length < 0) {
            throw new IllegalArgumentException("length < 0");
        }
        if (offset < 0 || (offset + length > buffer.length)) {
            throw new ArrayIndexOutOfBoundsException("bad offset or length");
        }
        try {
            md5.update(buffer, offset, length);
        } catch (Exception e) {
            md5 = null;
            StrangeEons.log.log(Level.WARNING, "internal checksum fault", e);
        }
    }

    /**
     * Completes the checksum operation and returns the checksum bytes. If the
     * checksum operation has already been completed, this will return the same
     * checksum bytes until the next call to {@link #reset()}. If there has been
     * any internal failure of the checksum algorithm, this method will return
     * <code>null</code>.
     *
     * @return the checksum bytes, or <code>null</code>
     */
    public byte[] getChecksum() {
        if (!done) {
            done = true;
            if (md5 != null) {
                try {
                    digest = md5.digest();
                } catch (Exception e) {
                    md5 = null;
                    StrangeEons.log.log(Level.WARNING, "internal checksum fault", e);
                }
            }
        }
        return digest;
    }

    /**
     * Completes the checksum and returns a string that represents the checksum
     * value. If there has been any internal failure of the checksum algorithm,
     * this method will return <code>null</code>.
     *
     * @return a string of hexadecimal digits representing the checksum value
     * @see #getChecksum()
     */
    public String getChecksumString() {
        byte[] bytes = getChecksum();
        if (bytes == null) {
            return null;
        }

        StringBuilder b = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; ++i) {
            b.append(DIGITS[(bytes[i] >> 4) & 15]).append(DIGITS[bytes[i] & 15]);
        }
        return b.toString();
    }
    private static final char[] DIGITS = new char[]{
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * Completes the checksum and returns <code>true</code> if this checksum
     * matches a previously produced checksum string. If there has been any
     * internal failure of the checksum algorithm, or if the comparison string
     * is <code>null</code>, this method will return <code>true</code>.
     *
     * @param checksumString the checksum string to compare to
     * @return <code>false</code> if and only if the comparison string is not
     * <code>null</code>, this check has non-<code>null</code> checksum bytes,
     * and this checksum's checksum string is equal to the comparison string
     * except for differences in letter case
     */
    public boolean matches(String checksumString) {
        // must be called first to meet the "completes the checksum" part of the contract
        String lhs = getChecksumString();

        if (checksumString == null) {
            return true;
        }
        if (lhs == null) {
            return true;
        }
        return lhs.equalsIgnoreCase(checksumString);
    }

    /**
     * Returns a new checksum that contains a checksum value for the given file.
     *
     * @param f the file to compute a checksum for
     * @throws IOException if an I/O error occurs
     */
    public static MD5Checksum forFile(File f) throws IOException {
        MD5Checksum md5 = new MD5Checksum();
        InputStream in = null;
        try {
            in = new FileInputStream(f);
            in = new BufferedInputStream(in, 64 * 1_024);
            byte[] buff = new byte[64 * 1_024];
            int read;
            while ((read = in.read(buff)) > 0) {
                md5.update(buff, 0, read);
            }
            md5.getChecksum(); // finish the checksum
            return md5;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    @Override
    public String toString() {
        return "MD5Checksum(" + (done ? getChecksumString() : "in progress") + ')';
    }
}
