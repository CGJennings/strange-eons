package ca.cgjennings.algo.compression;

import ca.cgjennings.algo.MonitoredInputStream;
import ca.cgjennings.algo.ProgressListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An abstract base class for {@link Compressor} implementations. Concrete
 * classes need only implement the following methods:<br>
 * {@link Compressor#compress(java.io.InputStream, java.io.OutputStream)} and
 * <br> {@link Compressor#decompress(java.io.InputStream, java.io.OutputStream)}
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public abstract class AbstractCompressor implements Compressor {

    private int level = 9;

    /**
     * Creates a compressor that defaults to the maximum compression level.
     */
    public AbstractCompressor() {
    }

    /**
     * Creates a compressor with the requested compression level.
     *
     * @param compressionLevel the compression level, from 0-9
     */
    public AbstractCompressor(int compressionLevel) {
        setCompressionLevel(compressionLevel);
    }

    @Override
    public void decompress(InputStream in, OutputStream out) throws IOException {
        in = filter(in);
        pumpStream(in, out, true);
    }

    @Override
    public void compress(File in, File out) throws IOException {
        compress(in, out, null);
    }

    @Override
    public void compress(File in, File out, ProgressListener li) throws IOException {
        InputStream ins = null;
        OutputStream outs = null;
        try {
            ins = wrap(new FileInputStream(in));
            if (li != null) {
                ins = new MonitoredInputStream(li, in.length(), ins);
            }
            outs = wrap(new FileOutputStream(out));
            compress(ins, outs);
            outs.flush();
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException e) {
                }
            }
            if (outs != null) {
                outs.close();
            }
        }
    }

    @Override
    public void decompress(File in, File out) throws IOException {
        decompress(in, out, null);
    }

    @Override
    public void decompress(File in, File out, ProgressListener li) throws IOException {
        InputStream ins = null;
        OutputStream outs = null;
        try {
            ins = wrap(new FileInputStream(in));
            if (li != null) {
                ins = new MonitoredInputStream(li, in.length(), ins);
            }
            outs = wrap(new FileOutputStream(out));
            decompress(ins, outs);
            outs.flush();
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException e) {
                }
            }
            if (outs != null) {
                outs.close();
            }
        }
    }

    @Override
    public final int getCompressionLevel() {
        return level;
    }

    @Override
    public final void setCompressionLevel(int level) {
        if (level < 0 || level > 9) {
            throw new IllegalArgumentException("level not in 0-9: " + level);
        }
        this.level = level;
    }

    /**
     * This convenience method is provided for concrete subclasses to copy the
     * content of one stream to another stream. A typical use case is to create
     * a compression (decompression) stream filter on the output (input) stream
     * and then use this method to complete the compression (decompression)
     * process.
     *
     * @param in the source
     * @param out the sink
     * @param allowNIO if {@code true}, use NIO to accelerate stream copying
     * @throws IOException if an I/O exception occurs during copying
     */
    protected void pumpStream(InputStream in, OutputStream out, boolean allowNIO) throws IOException {
        if (allowNIO) {
            ca.cgjennings.io.StreamPump.copy(in, out);
        } else {
            int bufferSize = (1 << Math.max(4, Math.min(8, getCompressionLevel()))) * 1024;
            int read;
            byte[] buff = new byte[bufferSize];
            while ((read = in.read(buff)) != -1) {
                out.write(buff, 0, read);
            }
            out.flush();
        }
    }

    /**
     * Wraps the provided input stream with a buffer if necessary.
     *
     * @param src the source stream
     * @return the (possibly wrapped) source stream
     */
    protected InputStream wrap(InputStream src) {
        if (!(src instanceof FilterInputStream)) {
            src = new BufferedInputStream(src, BUFF_SIZE);
        }
        return src;
    }

    /**
     * Wraps the provided output stream with a buffer if necessary.
     *
     * @param src the source stream
     * @return the (possibly wrapped) source stream
     */
    protected OutputStream wrap(OutputStream src) {
        if (!(src instanceof FilterOutputStream)) {
            src = new BufferedOutputStream(src, BUFF_SIZE);
        }
        return src;
    }

    private static final int BUFF_SIZE = 128 * 1024;

//	public static void main( String[] args ) {
//		try {
//			File in = new File("d:\\testdata");
//			System.err.printf( "%10d %s\n", in.length(), "uncompressed" );
//			//test( in, DeflateCompressor.class );
//			//test( in, BZip2Compressor.class );
//			test( in, LZMACompressor.class );
//		} catch( Exception e ) {
//			e.printStackTrace();
//		}
//	}
//	private static void test( File in, Class<? extends AbstractCompressor> c ) throws Exception {
//		File out = new File( in + "." + c.getSimpleName() );
//		AbstractCompressor ac = c.newInstance();
//		ac.compress( in, out );
//		System.err.printf( "%10d %s\n", out.length(), c.getSimpleName() );
//	}
}
