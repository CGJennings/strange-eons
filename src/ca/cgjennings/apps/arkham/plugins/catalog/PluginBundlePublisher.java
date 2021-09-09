package ca.cgjennings.apps.arkham.plugins.catalog;

import ca.cgjennings.algo.compression.BZip2Compressor;
import ca.cgjennings.algo.compression.Compressor;
import ca.cgjennings.algo.compression.DeflateCompressor;
import ca.cgjennings.algo.compression.LZMACompressor;
import ca.cgjennings.apps.arkham.StrangeEons;
import static ca.cgjennings.apps.arkham.StrangeEons.log;
import ca.cgjennings.apps.arkham.plugins.PluginBundle;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Locale;
import java.util.SortedMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import resources.Settings;

/**
 * Utility methods for converting plug-in bundles to and from published catalog
 * bundles (such as ".pbz" files). A published bundle is the network transport
 * format used to transmit plug-in bundles from a remote catalog server to
 * Strange Eons.
 *
 * <p>
 * A published bundle compresses an existing bundle with a high-efficiency
 * compression method. For best results, the existing bundle should first
 * be re-packed so that the underlying ZIP/JAR does not compress any entries
 * (i.e. all entries are written with {@code STORE}). This will reduce the
 * overall file size compared to compressing an already compressed bundle.
 * (The project command to publish a bundle performs this step automatically.)
 *
 * <p><b>Note:</b> Previously, a two-process was used in which the original
 * bundle (a type of JAR file) was compressed with Pack200, then the result
 * compressed as described above. However, support for Pack200 was removed
 * from Java. As the required tools are no longer distributed with JREs
 * starting in Java 14, bundles published with older versions of the app
 * can no longer be decompressed by newer versions of the app. (They can
 * be unpacked by an older version, then the bundle installed in the newer
 * version.)
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class PluginBundlePublisher {

    private PluginBundlePublisher() {
    }

    /**
     * An enumeration of the possible methods (algorithms) that may be used to
     * compress plug-in bundles for network transport.
     */
    public static enum CompressionMethod {
        /**
         * The BZip2 compression method (based on the Burrows–Wheeler
         * algorithm). Typically, BZip2 yields the best compression ratio
         * (smallest download), but decompression is significantly slower.
         * Published bundles use extension ".pbz".
         */
        BZIP2,
        /**
         * The Gzip DEFLATE compression method (a combination of LZ77 and
         * Huffman coding). Typically, Gzip yields the worst compression ratio
         * (largest download), but decompresses very quickly. Published bundles
         * use extension ".pgz".
         */
        GZIP,
        /**
         * The LZMA (Lempel-Ziv-Markov chain) compression method. Typically,
         * LZMA yields the bext compression ratio and faster decompression times
         * than {@linkplain #BZIP2 BZip2} (but still slower than than
         * {@linkplain #GZIP Gzip DEFLATE}). Published bundles use extension
         * ".plzm".
         */
        LZMA,;

        /**
         * Returns the standard file name extension used by published bundles
         * compressed with this method.
         *
         * @return the bundle extension used to identify the compression method
         */
        public String getPublishedBundleExtension() {
            String ext;
            switch (this) {
                case BZIP2:
                    ext = ".pbz";
                    break;
                case GZIP:
                    ext = ".pgz";
                    break;
                case LZMA:
                    ext = ".plzm";
                    break;
                default:
                    throw new AssertionError();
            }
            return ext;
        }

        /**
         * Create a compressor/decompressor for this compression type.
         *
         * @return a new instance of a compressor that uses this compression
         * method
         */
        public Compressor createCompressor() {
            Compressor c;
            switch (this) {
                case BZIP2:
                    c = new BZip2Compressor();
                    break;
                case GZIP:
                    c = new DeflateCompressor();
                    break;
                case LZMA:
                    c = new LZMACompressor();
                    break;
                default:
                    throw new AssertionError();
            }
            return c;
        }

        /**
         * Returns the first build number that supports this compression method.
         * Returns 0 if the compression method has "always" been available, that
         * is, has been supported since the first release that included
         * published bundles.
         *
         * @return the minimum version of Strange Eons needed to unpack a bundle
         * published with this compression method, or 0
         * @see StrangeEons#getBuildNumber()
         */
        public int getMinimumBuildNumber() {
            return this == LZMA ? 3707 : 0;
        }

        /**
         * Returns the compression method associated with the extension part of
         * a file name in a string. For example, given
         * "OmicronPlugin.seplugin.pbz", returns {@code BZIP2}. If no
         * extension is recognized, returns {@code null}.
         *
         * @param name the file name to examine
         * @return the correct compression method for the extension, or
         * {@code null}
         */
        public static CompressionMethod forExtension(String name) {
            if (name == null) {
                throw new NullPointerException("name");
            }
            name = name.toLowerCase(Locale.CANADA);
            for (CompressionMethod cm : CompressionMethod.values()) {
                if (name.endsWith(cm.getPublishedBundleExtension())) {
                    return cm;
                }
            }
            return null;
        }

        /**
         * Returns the default compression method for plug-in bundles. The
         * default method can change from version to version as better
         * compression methods become available.
         *
         * @return the default compression method for this release
         */
        public static CompressionMethod getDefault() {
            return LZMA;
        }
    }

    /**
     * Converts a bundle into an uncompressed, packed representation format. The
     * output file must still be compressed to complete the publication process.
     *
     * <p>
     * The specified bundle file is expected to use the
     * {@link PluginBundle#getPlainFile() plain format}. This method <i>does not
     * verify the format</i>, and the result of using a non-plain bundle is
     * undefined.
     *
     * @param source the bundle file to convert
     * @param dest the packed file to create
     * @throws IOException if an error occurs
     * @throws NullPointerException if either file is {@code null}
     * @see #unpackBundle
     *
     * @deprecated The Pack200 tools used by this method have been removed
     * from Java. Calling this method will copy the file without changes.
     */
    @Deprecated
    public static void packBundle(File source, File dest) throws IOException {
        if (source == null) {
            throw new NullPointerException("fin");
        }
        if (dest == null) {
            throw new NullPointerException("fout");
        }

        log.warning("copying bundle instead of packing with Pack200");
        ProjectUtilities.copyFile(source, dest);
    }

    /**
     * Returns whether the specified file appears to be packed with the
     * Pack200 algorithm by checking for a magic number. Does not rely
     * on the pack tools being available to the Java runtime.
     *
     * @param toTest the file to test
     * @return true if the file appears to be packed
     * @throws IOException  if an error occurs while reading the file
     */
    static boolean isPack200Compressed(File toTest) throws IOException {
        boolean isPacked = false;
        try(FileInputStream magicIns = new FileInputStream(toTest)) {
            if(magicIns.read() == 0xca && magicIns.read() == 0xfe
                    && magicIns.read() == 0xd0 && magicIns.read() == 0x0d) {
                isPacked = true;
            }
        }
        return isPacked;
    }

    /**
     * Converts an uncompressed, packed file into a plain, uncompressed plug-in
     * bundle that can be loaded by the Strange Eons plug-in system. A published
     * bundle must be {@linkplain #decompress decompressed} before it can be
     * unpacked.
     *
     * @param source the packed file to convert
     * @param dest the bundle file to create
     * @throws IOException if an error occurs
     * @throws NullPointerException if either file is {@code null}
     * @see #packBundle
     *
     * @deprecated The Pack200 tools used by this method have been removed
     * from Java. Calling this method on a packed file will throw an exception.
     */
    public static void unpackBundle(File source, File dest) throws IOException {
        if (source == null) {
            throw new NullPointerException("fin");
        }
        if (dest == null) {
            throw new NullPointerException("fout");
        }

        // check if the bundle is actually packed with Pack200
        if(isPack200Compressed(source)) {
            throw new IOException("detected Pack200 compression, which is no longer supported");
        } else {
            log.info("bundle not Pack200 compressed");
            ProjectUtilities.copyFile(source, dest);
            return;
        }
    }

    /**
     * Compresses a file using the specified compression method. Compression is
     * applied after packing to complete the publication process. The final
     * output file should have the same name as the original bundle with the
     * file extension of the compression method appended. For example, if the
     * bundle "OmicronPlugin.seplugin" is compressed using BZip2 compression,
     * the resulting file should be named "OmicronPlugin.seplugin.pbz".
     *
     * @param source the uncompressed source file
     * @param dest the compressed file to create
     * @param method the compression method to use; if {@code null}, the
     * default method is selected
     * @throws IOException if an error occurs
     * @throws NullPointerException if either file is {@code null}
     */
    public static void compress(File source, File dest, CompressionMethod method) throws IOException {
        if (source == null) {
            throw new NullPointerException("fin");
        }
        if (dest == null) {
            throw new NullPointerException("fout");
        }

        if (method == null) {
            method = CompressionMethod.getDefault();
        }

        log.log(Level.INFO, "compressing bundle with {0}", method);

        Compressor c = method.createCompressor();
        c.compress(source, dest);
    }

    /**
     * Decompresses a file previously compressed with the specified compression
     * method. If the method is {@code null}, then a compression method
     * will be chosen based on the input file name. If no method can be detected
     * from the file name, an {@code IOException} will be thrown.
     *
     * @param source the compressed source file
     * @param dest the uncompressed file to create
     * @param method the compression method used to compress the file, or
     * {@code null} to choose based on the input file name
     * @throws IOException if an error occurs
     * @throws NullPointerException if either file is {@code null}
     */
    public static void decompress(File source, File dest, CompressionMethod method) throws IOException {
        if (source == null) {
            throw new NullPointerException("fin");
        }
        if (dest == null) {
            throw new NullPointerException("fout");
        }

        if (method == null) {
            method = CompressionMethod.forExtension(source.getName());
            if (method == null) {
                throw new IOException("unknown compression method for " + source.getName());
            }
        }
        log.log(Level.INFO, "decompressing bundle with {0}", method);
        Compressor c = method.createCompressor();
        c.decompress(source, dest);
    }

    /**
     * Converts a published bundle file to a standard plug-in bundle,
     * decompressing and unpacking it with a single method call. An optional
     * listener can be provided that will be notified of the progress of the
     * bundle conversion.
     *
     * <p>
     * The compression method is detected automatically from the file name. If
     * the destination is {@code null}, a file in the same folder with the
     * publication extension (such as {@code .pbz}) removed will be used.
     * An optional listener may be supplied that will be notified of the
     * unpacking progress.
     *
     * @param source the source file to convert
     * @param dest the destination bundle file; may be {@code null} to
     * create a destination based on the source
     * @return the name of the destination file; useful if {@code null} was
     * specified to determine the name automatically
     * @throws NullPointerException if the source file is {@code null}
     * @throws IOException if an I/O error occurs during unpacking
     */
    public static File publishedBundleToPluginBundle(final File source, File dest) throws IOException {
        if (source == null) {
            throw new NullPointerException("source");
        }

        CompressionMethod method = CompressionMethod.forExtension(source.getName());
        if (method == null) {
            throw new IOException("unknown compression method for " + source.getName());
        }
        if (dest == null) {
            dest = removeExtension(source);
        }

        // decompress
        log.log(Level.INFO, "decompressing bundle with {0}", method);
        File decompTemp = File.createTempFile("se-unpack-", null);
        decompTemp.deleteOnExit();
        Compressor c = method.createCompressor();
        c.decompress(source, decompTemp, null);

        if(isPack200Compressed(decompTemp)) {
            throw new IOException("detected Pack200 compression, which is no longer supported");
        }

        log.info("copying bundle to destination");
        ProjectUtilities.copyFile(decompTemp, dest);
        decompTemp.delete();

        return dest;
    }

    /**
     * Removes the last file extension from a file, if any, and returns a file
     * for the trimmed name. If standard naming conventions are followed, this
     * can be applied to a published bundle's file name to retrieve the original
     * plug-in file name. For example, the file "bundle.seext.pbz" would become
     * "bundle.seext".
     *
     * @param f the file to remove an extension from
     * @return the file without the final extension, if any
     */
    private static File removeExtension(File f) {
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return f;
        }

        name = name.substring(0, dot);
        return new File(f.getParentFile(), name);
    }
}
