package ca.cgjennings.algo.compression;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A {@link Compressor} that implements GZIP compression; this is the deflate
 * compression algorithm with the header and footer expected by GZIP tools.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class GZIPCompressor extends AbstractCompressor {

    @Override
    public void compress(InputStream in, OutputStream out) throws IOException {
        GZIPOutputStream def = (GZIPOutputStream) filter(out);
        pumpStream(in, def, true);
        def.finish();
    }

    @Override
    public OutputStream filter(OutputStream out) throws IOException {
        return new GZIPOutputStream(out);
    }

    @Override
    public InputStream filter(InputStream in) throws IOException {
        return new GZIPInputStream(in);
    }

}
