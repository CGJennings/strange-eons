
import ca.cgjennings.algo.ProgressListener;
import ca.cgjennings.algo.compression.Compressor;
import ca.cgjennings.algo.compression.CompressorFactory;
import ca.cgjennings.apps.CommandLineParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A command line utility that (de)compresses files using a compression method
 * supported by {@link CompressorFactory}.
 * <p>
 * To use this from a command line, use a command like the following:<br>
 * <pre>java -cp strange-eons.jar compress [arguments...]</pre>
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class compress {

    /**
     * Value of --m (compression level) command line option.
     */
    public int m = 9;
    /**
     * Value of --a (algorithm) command line option.
     */
    public CompressorFactory a = CompressorFactory.BZIP2;
    /**
     * Value of --d (decompress flag) command line option.
     */
    public boolean d = false;

    /**
     * A simple command-line compression utility. Usage:
     * <pre>
     * compress [--a A] [--m 0-9] [--d] infile outfile
     * Where:
     *   --a is the compression algorithm
     *     (A is one of deflate, gzip, bzip2, lzma; default: bzip2)
     *   --m is the compression level, 0-9; default: 9
     *   --d decompresses infile instead of compressing it
     * </pre>
     *
     * @param args
     */
    public static void main(String[] args) {
        compress pargs = new compress();
        CommandLineParser clp = new CommandLineParser();
        clp.setUsageText(
                "compress [--a A] [--m 0-9] [--d] infile outfile\n"
                + "Where:\n"
                + "  --a is the compression algorithm\n"
                + "    (A is one of deflate, gzip, bzip2, lzma)\n"
                + "  --m is the compression level, 0-9\n"
                + "  --d decompresses infile instead of compressing it\n"
        );
        clp.parse(pargs, args);
        File[] files = clp.getPlainFiles();
        if (files.length != 2) {
            System.err.println("need exactly two files");
            System.exit(20);
        }
        Compressor comp = pargs.a.getCompressor();
        comp.setCompressionLevel(Math.max(1, Math.min(pargs.m, 9)));

        InputStream in = null;
        OutputStream out = null;
        try {
            ProgressListener li = (Object source, float progress) -> {
                System.out.printf("%.0f%%\r", progress * 100f);
                return false;
            };

            if (pargs.d) {
                comp.decompress(files[0], files[1], li);
            } else {
                comp.compress(files[0], files[1], li);
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

//	public static void main( String[] args ) {
//		_main( new String[] {"--c", "bzip2", "d:\\test", "d:\\out"} );
//	}
}
