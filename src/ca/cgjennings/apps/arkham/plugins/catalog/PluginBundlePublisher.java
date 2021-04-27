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
import java.util.jar.Pack200;
import java.util.jar.Pack200.Packer;
import static java.util.jar.Pack200.Packer.*;
import java.util.jar.Pack200.Unpacker;
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
 * Bundles are converted to their "published" form in a two-step process. The
 * first step, "packing", converts the bundle into an uncompressed archive with
 * reorganized data that is optimized for compression. Some extraneous
 * information about the structure of the archive and any compiled classes is
 * deleted during packing to reduce download size. The exact details may change
 * from version to version, but examples of possible changes include removing
 * empty directories, using a single timestamp for all files in the archive, and
 * removing information about classes that is used by debugging tools. (However,
 * the line and source file information displayed when an exception is thrown
 * will <i>not</i> be removed.)
 *
 * <p>
 * The second step compresses the entire packed archive file. The compression
 * method is generally chosen for maximum compression, without regard to the
 * time required to compress or decompress the archive. A number of compression
 * algorithms are supported.
 *
 * <p>
 * Likewise, to convert a published bundle to a plain bundle, it must first be
 * decompressed by the same method used to compress it, then unpacked to a
 * bundle file. As a convenience, the  {@link #publishedBundleToPluginBundle
 * publishedBundleToPluginBundle} method can perform both steps with a single
 * method call.
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
         * "OmicronPlugin.seplugin.pbz", returns <code>BZIP2</code>. If no
         * extension is recognized, returns <code>null</code>.
         *
         * @param name the file name to examine
         * @return the correct compression method for the extension, or
         * <code>null</code>
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
     * If this boolean {@linkplain Settings#getUser() user setting} is
     * <code>true</code>, then irrelevant JAR file metadata will be stripped out
     * when packing the file. This data, consisting of one or more files in the
     * META-INF archive directory, is sometimes added as a byproduct of building
     * class-based plug-ins with a Java development environment. It is not
     * needed by normal plug-in bundles, but is relevant in the rare case that a
     * plug-in bundle is meant to be used as either a plug-in or a Java
     * application. The default is <code>true</code>, that is, to strip out such
     * metadata during publishing.
     */
    public static final String STRIP_JAR_METADATA_SETTING = "pack-bundle-strip-meta";

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
     * @throws NullPointerException if either file is <code>null</code>
     * @see #unpackBundle
     */
    public static void packBundle(File source, File dest) throws IOException {
        if (source == null) {
            throw new NullPointerException("fin");
        }
        if (dest == null) {
            throw new NullPointerException("fout");
        }

        log.info("repacking with Pack200");

        Packer packer = Pack200.newPacker();
        SortedMap<String, String> p = packer.properties();

        p.put(EFFORT, "9");
        p.put(SEGMENT_LIMIT, "-1");
        p.put(MODIFICATION_TIME, LATEST);
        p.put(DEFLATE_HINT, FALSE);
        p.put(KEEP_FILE_ORDER, FALSE);
        p.put(CODE_ATTRIBUTE_PFX + "LocalVariableTable", STRIP);
        p.put(CODE_ATTRIBUTE_PFX + "LocalVariableTypeTable", STRIP);

        OutputStream out = null;
        try {
            JarFile jar;
            if (Settings.getUser().getBoolean(STRIP_JAR_METADATA_SETTING, true)) {
                jar = new StrippedJar(source);
            } else {
                jar = new JarFile(source);
            }
            out = new BufferedOutputStream(new FileOutputStream(dest), 128 * 1_024);
            packer.pack(jar, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

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
     * @throws NullPointerException if either file is <code>null</code>
     * @see #packBundle
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
            log.info("reconstituting from Pack200");
        } else {
            log.info("bundle not Pack200 compressed");
            ProjectUtilities.copyFile(source, dest);
            return;
        }

        Unpacker u = Pack200.newUnpacker();
        FileOutputStream fouts = null;
        try {
            fouts = new FileOutputStream(dest);
            try (JarOutputStream out = new JarOutputStream(new BufferedOutputStream(fouts))) {
                u.unpack(source, out);
            }
            fouts = null;
        } finally {
            if (fouts != null) {
                fouts.close();
            }
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
     * @param method the compression method to use; if <code>null</code>, the
     * default method is selected
     * @throws IOException if an error occurs
     * @throws NullPointerException if either file is <code>null</code>
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
     * method. If the method is <code>null</code>, then a compression method
     * will be chosen based on the input file name. If no method can be detected
     * from the file name, an <code>IOException</code> will be thrown.
     *
     * @param source the compressed source file
     * @param dest the uncompressed file to create
     * @param method the compression method used to compress the file, or
     * <code>null</code> to choose based on the input file name
     * @throws IOException if an error occurs
     * @throws NullPointerException if either file is <code>null</code>
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
     * the destination is <code>null</code>, a file in the same folder with the
     * publication extension (such as <code>.pbz</code>) removed will be used.
     * An optional listener may be supplied that will be notified of the
     * unpacking progress.
     *
     * @param source the source file to convert
     * @param dest the destination bundle file; may be <code>null</code> to
     * create a destination based on the source
     * @return the name of the destination file; useful if <code>null</code> was
     * specified to determine the name automatically
     * @throws NullPointerException if the source file is <code>null</code>
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

        // unpack
        if(isPack200Compressed(decompTemp)) {
            log.info("reconstituting from Pack200");
            Unpacker u = Pack200.newUnpacker();
            FileOutputStream fouts = null;
            try {
                fouts = new FileOutputStream(dest);
                try (JarOutputStream out = new JarOutputStream(new BufferedOutputStream(fouts))) {
                    u.unpack(decompTemp, out);
                }
                fouts = null;
            } finally {
                decompTemp.delete();
                if (fouts != null) {
                    fouts.close();
                }
            }
        } else {
            log.info("bundle not Pack200 compressed");
            ProjectUtilities.copyFile(decompTemp, dest);
            decompTemp.delete();
        }

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

    /**
     * A JAR file subclass that hides any content in the META-INF folder;
     * primarily the manifest. Strange Eons plug-ins don't need this since they
     * are not actually run as JARs, but they may still appear when a plug-in is
     * created using an IDE or other tool. This class is used to effectively
     * strip those files out when packing a bundle without having to rewrite
     * the JAR file to a temporary location.
     */
    private static class StrippedJar extends JarFile {

        public StrippedJar(File file) throws IOException {
            super(file);
        }

        @Override
        public Manifest getManifest() throws IOException {
            return null;
        }

        @Override
        public ZipEntry getEntry(String name) {
            return hide(name) ? null : super.getEntry(name);
        }

        @Override
        public Enumeration<JarEntry> entries() {
            return new Enumeration<JarEntry>() {
                final Enumeration<JarEntry> src = StrippedJar.super.entries();
                private JarEntry next = nextValid();

                @Override
                public boolean hasMoreElements() {
                    return next != null;
                }

                @Override
                public JarEntry nextElement() {
                    JarEntry v = next;
                    next = nextValid();
                    return v;
                }

                private JarEntry nextValid() {
                    while (src.hasMoreElements()) {
                        JarEntry e = src.nextElement();
                        if (!hide(e.getName())) {
                            return e;
                        }
                    }
                    return null;
                }
            };
        }

        private boolean hide(String name) {
            return name.equals("META-INF") || name.startsWith("META-INF/");
        }
    }
}
