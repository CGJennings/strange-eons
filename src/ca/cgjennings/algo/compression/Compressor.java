package ca.cgjennings.algo.compression;

import ca.cgjennings.algo.ProgressListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A generic interface for simple compression and decompression of data streams
 * using various compression algorithms. In addition to supporting the filtered
 * stream model used in the Java I/O libraries, <code>Compressors</code> use a
 * pipe-style model in which data is transformed as it copied through from an
 * input stream to an output stream. This style is more convenient in many
 * applications.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface Compressor {

    /**
     * Sets the compression level for future compression operations. The
     * compression level is an integer between 0 and 9 inclusive that indicates
     * whether the compressor should favour low resource requirements (such as
     * CPU time and memory) or high compression ratios (9=maximum compression).
     * For some implementations, this value may have no effect.
     *
     * <p>
     * Each compressor has its own default compression level chosen to balance
     * the amount of compression against the resources required to compress
     * and/or decompress the data. Setting the compression level higher than its
     * default may result in a sharp increase in resource use, particularly
     * during compression.
     *
     * @param level the compression level from 0 to 9, with higher values
     * suggesting that the algorithm should try harder to compress the data
     * @throws IllegalArgumentException if the requested <code>level</code> is
     * out of range
     */
    void setCompressionLevel(int level);

    /**
     * Returns the current compression level.
     *
     * @return the current compression level, from 0 to 9 inclusive
     */
    int getCompressionLevel();

    /**
     * Reads the data from the input stream, compresses it, and writes it to the
     * output stream.
     *
     * @param in the source for data to compress
     * @param out the sink for compressed data
     * @throws IOException if an error occurs during compression
     */
    void compress(InputStream in, OutputStream out) throws IOException;

    /**
     * Reads compressed data from the input stream, decompresses it, and writes
     * it to the output stream.
     *
     * @param in the source of data to decompress
     * @param out the sink for decompressed data
     * @throws IOException if an error occurs during decompression
     */
    void decompress(InputStream in, OutputStream out) throws IOException;

    /**
     * Compresses a file, writing the result to another file.
     *
     * @param in the file to compress
     * @param out the destination
     * @throws IOException if an error occurs
     */
    void compress(File in, File out) throws IOException;

    /**
     * Compresses a file, writing the result to another file.
     *
     * @param in the file to compress
     * @param out the destination
     * @param li a listener that will be updated with compression progress
     * @throws IOException if an error occurs
     */
    void compress(File in, File out, ProgressListener li) throws IOException;

    /**
     * Decompresses a file, writing the result to another file.
     *
     * @param in the file to decompress
     * @param out the destination
     * @throws IOException if an error occurs
     */
    void decompress(File in, File out) throws IOException;

    /**
     * Decompresses a file, writing the result to another file.
     *
     * @param in the file to decompress
     * @param out the destination
     * @param li a listener that will be updated with the decompression progress
     * @throws IOException if an error occurs
     */
    void decompress(File in, File out, ProgressListener li) throws IOException;

    /**
     * Returns an output stream that compresses data as it writes it to an
     * existing stream.
     *
     * @param out the stream to write compressed data to
     * @return a filter stream that will compress data and write it to
     * <code>out</code>
     * @throws IOException if an error occurs while creating the stream
     */
    OutputStream filter(OutputStream out) throws IOException;

    /**
     * Returns an input stream that reads decompresses data from a stream of
     * compressed data.
     *
     * @param in the stream to read compressed data from
     * @return a filter stream that will decompress data from <code>in</code> as
     * it is read from
     * @throws IOException if an error occurs while creating the stream
     */
    InputStream filter(InputStream in) throws IOException;
}
