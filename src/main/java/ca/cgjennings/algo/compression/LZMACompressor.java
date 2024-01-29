package ca.cgjennings.algo.compression;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A {@link Compressor} that writes a stream of LZMA2 compressed data.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class LZMACompressor extends AbstractCompressor {

    /**
     * Creates a new LZMA compressor at a default compression level.
     */
    public LZMACompressor() {
        super(7);
    }

    /**
     * Creates a new LZMA compressor with the requested compression level.
     *
     * @param compressionLevel the compression level, from 0-9
     */
    public LZMACompressor(int compressionLevel) {
        super(compressionLevel);
    }

    @Override
    public void compress(InputStream in, OutputStream out) throws IOException {
        LZMA2OutputStream def = (LZMA2OutputStream) filter(out);
        pumpStream(in, def, true);
        def.finish();
    }

    @Override
    public OutputStream filter(OutputStream out) throws IOException {
        int level = getCompressionLevel();
        out.write('L');
        out.write('Z');
        out.write('2');
        out.write(level);
        return new LZMA2OutputStream(new FinishableWrapperOutputStream(out), new LZMA2Options(level));
    }

    @Override
    public InputStream filter(InputStream in) throws IOException {
        if (in.read() != 'L' || in.read() != 'Z' || in.read() != '2') {
            throw new IOException("LZMA2 marker missing from stream");
        }
        int level = in.read();
        if (level < 0 || level > 9) {
            throw new IOException("LZMA2 invalid compression level in header");
        }

        LZMA2Options dummy = new LZMA2Options(level);
        int dictSize = dummy.getDictSize();

        return new LZMA2InputStream(in, dictSize);
    }
}
