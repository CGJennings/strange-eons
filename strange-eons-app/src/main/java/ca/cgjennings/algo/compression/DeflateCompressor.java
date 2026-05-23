package ca.cgjennings.algo.compression;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * A {@link Compressor} that implements the deflate compression algorithm; this
 * is equivalent to GZIP but without the header and footer.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class DeflateCompressor extends AbstractCompressor {

    @Override
    public void compress(InputStream in, OutputStream out) throws IOException {
        DeflaterOutputStream def = (DeflaterOutputStream) filter(out);
        pumpStream(in, def, true);
        def.finish();
    }

    @Override
    public OutputStream filter(OutputStream out) throws IOException {
        return new DeflaterOutputStream(out, new Deflater(getCompressionLevel()));
    }

    @Override
    public InputStream filter(InputStream in) throws IOException {
        return new InflaterInputStream(in);
    }
}
