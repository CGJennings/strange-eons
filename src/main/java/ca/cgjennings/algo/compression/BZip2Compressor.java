package ca.cgjennings.algo.compression;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A {@link Compressor} that implements the BZip2 compression algorithm.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class BZip2Compressor extends AbstractCompressor {

    @Override
    public void compress(InputStream in, OutputStream out) throws IOException {
        OutputStream bzo = filter(out);
        // using NIO seems to cause problems
        pumpStream(in, bzo, false);
        ((BZip2OutputStream) bzo).finish();
    }

    @Override
    public void decompress(InputStream in, OutputStream out) throws IOException {
        in = filter(in);
        pumpStream(in, out, false);
    }

    @Override
    public void compress(File in, File out) throws IOException {
        // High compression levels offer no benefit when compressing large
        // files. This code reduces the effective compression level to the optimal
        // value for small files. (The level is limited to the number of 100k byte
        // blocks in the source file.)
        final int sizeLimitedLevel = (int) Math.min(9L, (in.length() + 99_999L) / 100_000L);
        final int level = Math.max(1, Math.min(getCompressionLevel(), sizeLimitedLevel));
        final int oldLevel = getCompressionLevel();

        try {
            setCompressionLevel(level);
            super.compress(in, out);
        } finally {
            setCompressionLevel(oldLevel);
        }
    }

    @Override
    public OutputStream filter(OutputStream out) throws IOException {
        out.write('B');
        out.write('Z');
        // BZ accepts levels between 1 and 9 rather than 0 and 9
        final int level = Math.max(1, getCompressionLevel());
        return new BZip2OutputStream(out, level);
    }

    @Override
    public InputStream filter(InputStream in) throws IOException {
        if (in.read() != 'B' || in.read() != 'Z') {
            throw new IOException("BZip2 marker missing from stream");
        }
        return new BZip2InputStream(in);
    }
}
