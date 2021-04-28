package ca.cgjennings.algo.compression;

/**
 * Compressor factory supports programmatic selection and creation of
 * {@link Compressor} instances. For example, it can return a compressor that is
 * suited to a particular file type.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public enum CompressorFactory {
    /**
     * The deflate (GZIP with no header or footer) compressor factory.
     */
    DEFLATE(DeflateCompressor.class, "deflate"),
    /**
     * The GZIP compressor factory.
     */
    GZIP(GZIPCompressor.class, "gz", "tgz"),
    /**
     * The BZip2 compressor factory.
     */
    BZIP2(BZip2Compressor.class, "bz2", "bz", "tbz", "tbz2"),
    /**
     * The LZMA compressor factory.
     */
    LZMA(LZMACompressor.class, "lz2"),;

    private CompressorFactory(Class className, String... extensions) {
        this.className = className;
        this.extensions = extensions;
    }
    private final Class className;
    private final String[] extensions;

    /**
     * Returns a {@link Compressor} for this algorithm.
     *
     * @return a compressor instance
     */
    public Compressor getCompressor() {
        try {
            return (Compressor) className.getConstructor().newInstance();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Returns the most common file extension for this algorithm.
     *
     * @return the suggested file extension for compressed files
     */
    public String getExtension() {
        return extensions[0];
    }

    /**
     * Returns an array of all known extensions for this algorithm.
     *
     * @return an array of suitable file extensions
     */
    public String[] getAllExtensions() {
        return extensions.clone();
    }

    /**
     * Returns <code>true</code> if one of this algorithm's extensions matches
     * the given name. If name contains a dot, then only the text after the
     * final dot is considered when matching extensions.
     *
     * @param name a name with an extension to match against this algorithm's
     * extensions
     * @return <code>true</code> if the name's extension is a known extension
     * for this algorithm
     * @throws NullPointerException if the name is <code>null</code>
     */
    public boolean matchesExtension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            name = name.substring(dot + 1);
        }
        for (int i = 0; i < extensions.length; ++i) {
            if (extensions[i].equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Chooses a compressor for a specific application. The parameters allow you
     * to specify which performance aspects of the resulting compressor are most
     * important you, and returns a factory to suit your that satisfies
     *
     * @param small if <code>true</code>, small compressed output (high
     * compression ratios) is important
     * @param fast if <code>true</code>, fast decompression is important
     * @param standard if <code>true</code>, being able to decompress data with
     * common command line tools is important
     * @return a factory that will create a compressor that satisfies as many of
     * your needs as possible
     */
    public static CompressorFactory choose(boolean small, boolean fast, boolean standard) {
        if (small) {
            if (standard) {
                return BZIP2;
            }
            return LZMA;
        } else {
            if (fast || standard) {
                return GZIP;
            }
            return LZMA;
        }
    }

    /**
     * Returns a factory that matches a name, or <code>null</code> if no factory
     * matches. If name contains a dot, then only the text after the final dot
     * is considered when matching extensions.
     *
     * @param name the file name or extension to match
     * @return a compressor factory that creates compressors for the matched
     * file type, or <code>null</code> if no match was found
     * @throws NullPointerException if the name is <code>null</code>
     * @see #matchesExtension(java.lang.String)
     */
    public static CompressorFactory forExtension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            name = name.substring(dot + 1);
        }
        for (CompressorFactory cf : CompressorFactory.values()) {
            if (cf.matchesExtension(name)) {
                return cf;
            }
        }
        return null;
    }
}
