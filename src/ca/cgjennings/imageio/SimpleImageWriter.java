package ca.cgjennings.imageio;

//
// Jan  3 2012: Rewrittren to use IIOWritePanel.Parameters internally and
//              class renamed to SimpleImageWriter to avoid conflict with imageio
//              package.
//
// Jan 28 2013: Made metadata writing optional and dispose() on finalization

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.imageio.IIOWritePanel.Parameters;
import ca.cgjennings.imageio.plugins.jpeg2000.J2KImageWriteParam;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import org.w3c.dom.Node;

/**
 * A simple image writer writes images to output streams and files. It provides
 * a simplified mechanism for configuring basic format, compression, and
 * metadata options compared to the <code>imageio</code> library.
 * <p>
 * <b>Note:</b><br> it is important to {@code dispose()} of instances of this
 * class when finished with them, as many of the underlying image encoders
 * consume significant native resources.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 1.0
 */
public class SimpleImageWriter {

    private String format;
    private javax.imageio.ImageWriter writer;
    private IIOWritePanel.Parameters iioparam;
    private Locale locale;
    private Locale commentLocale;
    private float pixelsPerMM, pixelsPerInch;
    private String comment;
    private final static float MM_PER_INCH = 25.4f;
    private boolean writeMetadata = true;

    /**
     * Format identifier for PNG images.
     */
    public static final String FORMAT_PNG = "png";
    /**
     * Format identifier for JPEG images.
     */
    public static final String FORMAT_JPEG = "jpg";
    /**
     * Format identifier for JPEG2000 part 1 images.
     */
    public static final String FORMAT_JPEG2000 = "jp2";
    /**
     * Format identifier for GIF89a images.
     */
    public static final String FORMAT_GIF = "gif";
    /**
     * Format identifier for BMP images.
     */
    public static final String FORMAT_BMP = "bmp";

    /**
     * Creates an image writer for the PNG format using the default locale.
     *
     * @see #SimpleImageWriter(java.lang.String, java.util.Locale)
     */
    public SimpleImageWriter() {
        this("png", Locale.getDefault());
    }

    /**
     * Creates an image writer for the requested format using the default
     * locale.
     *
     * @param fileFormat the file extension or name of a supported file format
     * @see #SimpleImageWriter(java.lang.String, java.util.Locale)
     * @throws UnsupportedOperationException if {@code fileFormat} is not a
     * known type.
     */
    public SimpleImageWriter(String fileFormat) {
        this(fileFormat, Locale.getDefault());
    }

    /**
     * Create a {@code SimpleImageWriter} that will produce images in the format
     * specified by {@code fileFormat}. The {@code fileFormat} parameter must
     * either be the name or file extension of a format registered with the
     * <code>imageio</code> library.
     *
     * @param fileFormat name of file format to be produced by this writer
     * @param locale the preferred locale for labels and messages
     * @throws UnsupportedOperationException if {@code fileFormat} is not a
     * known type.
     */
    public SimpleImageWriter(String fileFormat, Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        this.locale = locale;
        format = fileFormat.toLowerCase();
        writer = createImageWriterForFormat(format, locale);
        iioparam = new IIOWritePanel.Parameters();
        iioparam.initFrom(writer.getDefaultWriteParam());

        setCompressionQuality(-1f);
        setProgressiveScan(false);
        setPixelsPerInch(72f);
    }

    /**
     * Creates an ImageIO writer for the requested format and preferred locale.
     *
     * @param format the format to create a writer for
     * @param loc the ideal locale for the writer
     * @return an image writer for the locale
     */
    private static javax.imageio.ImageWriter createImageWriterForFormat(String format, Locale loc) {
        format = format.toLowerCase();
        if (loc == null) {
            loc = Locale.getDefault();
        }

        Iterator<javax.imageio.ImageWriter> it = ImageIO.getImageWritersByFormatName(format);
        if (!it.hasNext()) {
            // didn't find it by format name, try searching in file suffixes
            it = ImageIO.getImageWritersBySuffix(format);
            if (!it.hasNext()) {
                throw new UnsupportedOperationException("format not supported: " + format);
            }
        }
        javax.imageio.ImageWriter writer = it.next();

        try {
            writer.setLocale(loc);
        } catch (IllegalArgumentException e) {
            writer.setLocale(null);
        }

        return writer;
    }

    /**
     * Get the name of the file format this instance will write files in.
     *
     * @return the file format specified at construction
     */
    public String getFormat() {
        return format;
    }

    /**
     * Returns <code>true</code> if one or more compression methods are
     * supported.
     *
     * @return <code>true</code> if compression is supported
     */
    public boolean isCompressionSupported() {
        return iioparam.canCompress;
    }

    /**
     * Returns <code>true</code> if disabling compression has any effect.
     *
     * @return <code>true</code> if compression can be disabled
     */
    public boolean isCompressionOptional() {
        return iioparam.canDisableCompress;
    }

    /**
     * Sets whether compression is enabled. If the compression setting cannot be
     * changed, this will have no effect.
     *
     * @param enable whether to enable compression
     */
    public void setCompressionEnabled(boolean enable) {
        iioparam.setCompression(enable);
    }

    /**
     * Returns <code>true</code> if compression is enabled.
     *
     * @return whether compression is enabled
     */
    public boolean isCompressionEnabled() {
        return iioparam.compressed;
    }

    /**
     * Returns an array of the supported compression methods. Most formats
     * support at most one compression method.
     *
     * @return an array of supported compression methods, or <code>null</code>
     */
    public String[] getCompressionTypes() {
        String[] types = iioparam.compressionTypes;
        return types == null ? null : types.clone();
    }

    /**
     * Sets the compression type to use. If the type is <code>null</code>, a
     * default type is selected. Otherwise, the type must be one of the types
     * specified by {@link #getCompressionTypes()}. Changing the compression
     * type may alter the quality descriptions and values.
     *
     * @param type the name of the compression type to use
     */
    public void setCompressionType(String type) {
        iioparam.setCompressionType(type);
    }

    /**
     * Return an array of locale-dependent descriptions of the available
     * compression quality settings. This method may return <code>null</code>,
     * and must return <code>null</code> if {@link #getCompressionValues()}
     * does.
     *
     * @return an array of description {@code String}s, or {@code} null if there
     * are descriptions available
     * @see SimpleImageWriter#getCompressionValues()
     */
    public String[] getCompressionQualityDescriptions() {
        if (iioparam.standardQualities != null && iioparam.standardQualityNames != null) {
            return iioparam.standardQualityNames.clone();
        }
        return null;
    }

    /**
     * Return an array of quality values indicating the ranges of compression
     * qualities covered by the strings returned by
     * {@link #getCompressionValues()}. The quality setting
     * {@code getCompressionQualities()[i]} covers all compression settings from
     * {@code getCompressionValues()[i]}, inclusive, to
     * {@code getCompressionValues()[i+1]}, exclusive. The only exception is for
     * {@code i == getCompressionQualities().length - 1}, which includes the
     * final value in the range.
     *
     * @return an array of description {@code String}s, or {@code} null if there
     * are descriptions available
     */
    public float[] getCompressionValues() {
        if (iioparam.standardQualities != null && iioparam.standardQualityNames != null) {
            return iioparam.standardQualities.clone();
        }
        return null;
    }

    /**
     * Return {@code true} if the format's compression method is lossless. A
     * lossless format does not degrade in quality when written.
     *
     * @return {@code true} if the selected format does not lose information
     */
    public boolean isLossless() {
        return iioparam.lossless;
    }

    /**
     * Set image compression according to one of the strings returned by
     * {@link #getCompressionQualityDescriptions()}. The compression level will
     * be set to the midpoint of the the {@code description}'s value range.
     *
     *
     * @param description the quality description string matching the desired
     * quality level
     * @see #getCompressionQualityDescriptions()
     * @see #setCompressionQuality(float)
     * @throws IllegalArgumentException if {@code description} is not a valid
     * quality description
     */
    public void setCompressionQuality(String description) {
        String[] descriptions = getCompressionQualityDescriptions();

        if (descriptions != null) {
            int quality;
            for (quality = 0; quality < descriptions.length; ++quality) {
                if (description.equals(descriptions[quality])) {
                    float[] qualityValues = getCompressionValues();
                    setCompressionQuality((qualityValues[quality] + qualityValues[quality + 1]) / 2f);
                    return;
                }
            }
        }
        throw new IllegalArgumentException("not a valid description in this locale/format");
    }

    /**
     * Control the compression quality during image writing. If {@code quality}
     * is between 0 and 1 (inclusive), then a relative quality is requested,
     * with a value of 0 emphasizing maximum compression and a value of 1
     * requesting maximum image quality. (If the format is lossless, then image
     * quality is not affected.) If {@code quality} is less than 0, then a
     * default compression level will be selected.
     *
     * @param quality the relative quality (0-1), or a negative value to request
     * default compression
     */
    public void setCompressionQuality(float quality) {
        if (quality < 0) {
            quality = 0.75f;
        }
        iioparam.setCompressionQuality(quality);
    }

    /**
     * Returns the current compression quality.
     *
     * @return the compression quality level, from 0 to 1
     */
    public float getCompressionQuality() {
        return iioparam.compressionQuality;
    }

    /**
     * Sets whether the writer should use progressive encoding, if possible.
     * Progressive scan images can be displayed in multiple passes of increasing
     * quality.
     *
     * @param useProgressiveScanIfPossible if {@code true}, the writer will
     * write images in progressive scans if the format supports it
     */
    public void setProgressiveScan(boolean useProgressiveScanIfPossible) {
        iioparam.setProgressive(useProgressiveScanIfPossible);
    }

    /**
     * Returns <code>true</code> if progressive encoding is enabled.
     *
     * @return <code>true</code> is progressive scan is enabled
     */
    public boolean isProgressiveScan() {
        return iioparam.isProgressive;
    }

    /**
     * Returns <code>true</code> if the progressive scan setting can be changed.
     * Some formats do not support progressive scans, while other formats
     * support it inherently.
     *
     * @return <code>true</code> if the progressive scan setting is alterable
     */
    public boolean isProgressiveScanConfigurable() {
        return iioparam.isProgressive;
    }

    /**
     * Set the pixels-per-inch resolution that will be written with the file's
     * metadata, if possible. The default resolution is 72 pixels per inch.
     *
     * @param ppi the image resolution, in pixels per inch
     */
    public void setPixelsPerInch(float ppi) {
        pixelsPerInch = ppi;
        pixelsPerMM = ppi / MM_PER_INCH;
    }

    /**
     * Return the resolution to be written with images, in pixels per inch.
     *
     * @return the image resolution, in pixels per inch
     */
    public float getPixelsPerInch() {
        return pixelsPerInch;
    }

    /**
     * Set image resolution that will be written with the file's metadata, if
     * possible. The resolution is specified in pixels per millimetre. As a
     * convenience, {@link #setPixelsPerInch(float)} is also provided.
     * <p>
     * The default resolution is 72 pixels per inch (approximately 2.8 pixels
     * per mm). Images written with the writer will specify a horizontal and
     * vertical resolution as close to this value as the file format allows.
     *
     *
     * @param ppmm the intended image resolution, in pixels per mm
     * @see SimpleImageWriter#setPixelsPerInch(float)
     */
    public void setPixelsPerMillimetre(float ppmm) {
        pixelsPerMM = ppmm;
        pixelsPerInch = ppmm / MM_PER_INCH;
    }

    /**
     * Return the resolution to be written with images, in pixels per mm. As a
     * convenience, {@link #getPixelsPerInch()} is also provided. Images written
     * with the writer will specify a horizontal and vertical resolution as
     * close to this value as the file format allows.
     *
     *
     * @return the current image resolution to use for writing, pixels per mm
     * @see SimpleImageWriter#getPixelsPerInch()
     */
    public float getPixelsPerMillimetre() {
        return pixelsPerMM;
    }

    /**
     * Set the comment text that will be written along with files created by
     * this writer. If {@code comment} is {@code null}, the current comment is
     * cleared. The comment will be assumed to be written in the language
     * specified by this writer's locale.
     *
     * @param comment the comment text to be included in the file's metadata.
     */
    public void setComment(String comment) {
        setComment(comment, null);
    }

    /**
     * Set the comment text that will be written along with files created by
     * this writer. If {@code comment} is {@code null}, the current comment (if
     * any) is cleared.
     *
     * @param comment the comment text to be included in the file's metadata.
     * @param commentLocale a locale describing the language the comment is
     * written in
     */
    public void setComment(String comment, Locale commentLocale) {
        this.comment = comment;
        this.commentLocale = commentLocale == null ? locale : commentLocale;
    }

    /**
     * Return the comment to be embedded in files written by this writer, if
     * possible. If no comment has been set, returns {@code null}.
     *
     * @return the currently set comment, or {@code null} if there is no comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * Returns <code>true</code> if comment and resolution metadata will be
     * written to output files. Metadata writing may not be supported for all
     * file types.
     *
     * @return if metadata writing is enabled
     */
    public boolean isMetadataEnabled() {
        return writeMetadata;
    }

    /**
     * Sets whether comment and resolution metadata should be written to output
     * files, if supported by the writer and image format.
     *
     * @param enable if <code>true</code>, metadata will be written if supported
     */
    public void setMetadataEnabled(boolean enable) {
        writeMetadata = enable;
    }

    /**
     * Writes the image {@code im} to a file, creating the intermediate stream
     * and closing it on completion. If the file exists, it will be overwritten.
     *
     * @param image an {@code Image} to be written
     * @param output the {@code File} which contains the destination file to be
     * written
     * @throws java.io.IOException if I/O errors occur during the write
     * operation
     */
    public void write(Image image, File output) throws IOException {
        if (image == null) {
            throw new NullPointerException("image");
        }
        if (output == null) {
            throw new IllegalArgumentException("output");
        }

        ImageOutputStream stream = null;
        try {
            output.delete();
            stream = ImageIO.createImageOutputStream(output);
            writeBufferedImage(bufferedImage(image), stream);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    /**
     * Writes the image {@code im} to an output stream. The stream is left open
     * after writing.
     *
     * @param image the {@code Image} to be written
     * @param out the {@code OutputStream} to writeBufferedImage to
     * @throws java.io.IOException if I/O errors occur during the write
     * operation
     */
    public void write(Image image, OutputStream out) throws IOException {
        if (image == null) {
            throw new NullPointerException("image");
        }
        if (out == null) {
            throw new IllegalArgumentException("output stream is null");
        }
        writeBufferedImage(bufferedImage(image), ImageIO.createImageOutputStream(out));
    }

    /**
     * Causes resources used by this writer to be released, including native
     * resources. The result of calling any method of this writer after calling
     * {@code dispose()} is undefined (although the typical result would be a
     * {@code NullPointerException}).
     */
    public void dispose() {
        if (writer != null) {
            try {
                writer.dispose();
            } catch (Throwable t) {
                StrangeEons.log.log(Level.WARNING, "unexpected dispose() exception", t);
            }
            writer = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            dispose();
        } finally {
            super.finalize();
        }
    }

    /**
     * Converts a non-rendered image to a rendered image (specifically, a
     * BufferedImage).
     *
     * @param image the image to convert to a rendered image if required
     * @return the original image, or a rendered image converted from the
     * original
     */
    private static BufferedImage bufferedImage(Image image) {
        final BufferedImage rim;
        if (!(image instanceof BufferedImage)) {
            rim = ImageUtilities.imageToBufferedImage(image);
        } else {
            rim = (BufferedImage) image;
        }
        return rim;
    }

    /**
     * This is a magic incantation that makes J2Ks that were not loaded from a
     * file work correctly. Otherwise they tend to appear washed out.
     *
     * @param im the image to transmogrify
     * @return the transmogrified image
     */
    private BufferedImage thereAndBackAgain(BufferedImage im) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(im.getWidth() * im.getHeight() * 2);
            ImageIO.write(im, "png", out);
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            out = null; // so it can be gc'd while reading the image
            return ImageIO.read(in);
        } catch (IOException e) {
            throw new AssertionError();
        }
    }

    /**
     * Writes {@code im} to the {@code ImageOutputStream ios}. All {@code write}
     * methods forward to this method. If they are supplied with an image which
     * is not a {@code BufferedImage}, they will create a {@code BufferedImage}
     * which is visually equivalent using the {@code convertImage} method.
     */
    private void writeBufferedImage(BufferedImage im, ImageOutputStream ios) throws IOException {
        // workaround for lack of transparency in JPEG89
        if (getFormat().equals(FORMAT_JPEG)) {
            if (im.getColorModel().getTransparency() != BufferedImage.OPAQUE) {
                BufferedImage opaque = new BufferedImage(im.getWidth(), im.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = opaque.createGraphics();
                try {
                    g.setPaint(Color.WHITE);
                    g.fillRect(0, 0, im.getWidth(), im.getHeight());
                    g.drawImage(im, 0, 0, null);
                } finally {
                    g.dispose();
                }
                im = opaque;
            }
        }

        // workaround for various ImageIO bugs: convert from
        // arbitrary colour model to RGB/ARGB
        im = ImageUtilities.ensureIntRGBFormat(im);

        ImageTypeSpecifier imageType = ImageTypeSpecifier.createFromRenderedImage(im);
        ImageWriteParam iwparam = iioparam.getImageWriteParam();
        IIOMetadata metadata = null;

        // workaround J2K encoder bugs
        if (iwparam instanceof J2KImageWriteParam) {
            // note: metadata == null for all J2Ks
            if (!cameFromFile) {
                // probably not needed anymore:
                // probably works because converts all images to RGB/ARGB
                im = thereAndBackAgain(im);
            }
        } else {
            metadata = writer.getDefaultImageMetadata(imageType, iwparam);

            if (isMetadataEnabled()) {
                if (!metadata.isReadOnly() && metadata.isStandardMetadataFormatSupported()) {
                    metadata.mergeTree("javax_imageio_1.0", buildStandardTreeFromSettings());
                }
                String nativeFormat = metadata.getNativeMetadataFormatName();
                if (nativeFormat.equals("javax_imageio_jpeg_image_1.0")) {
                    Node currentTree = metadata.getAsTree(nativeFormat);
                    Node mergeTree = buildJPEGTreeFromSettings(currentTree);
                    if (mergeTree != null) {
                        metadata.mergeTree(nativeFormat, mergeTree);
                    }
                }
            }
        }

        writer.setOutput(ios);
        writer.write(null, new IIOImage(im, null, metadata), iwparam);
        writer.setOutput(null);
        ios.flush();
    }

    /**
     * Create a DOM tree in the {@code javax_imageio_1.0} format that encodes
     * this writer's metadata parameters.
     */
    private Node buildStandardTreeFromSettings() {
        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
        IIOMetadataNode dim = new IIOMetadataNode("Dimension");
        IIOMetadataNode text = new IIOMetadataNode("Text");
        root.appendChild(dim);
        root.appendChild(text);

        appendToNode(dim, "HorizontalPixelSize", pixelsPerMM);
        appendToNode(dim, "VerticalPixelSize", pixelsPerMM);

        if (comment != null) {
            appendToNode(text, "TextEntry", "keyword", "Comment", "value", comment,
                    "encoding", "UTF-8", "language", commentLocale.getLanguage(), "compression", "none");
        }

        return root;
    }

    /**
     * Create resolution nodes for JPG metadata. Returns {@code null} if unable
     * to build an appropriate tree.
     */
    private Node buildJPEGTreeFromSettings(Node existingTree) {
        Node oldJfif = existingTree.getFirstChild().getFirstChild();
        if (oldJfif == null) {
            return null;
        }

        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_jpeg_image_1.0");
        IIOMetadataNode var = new IIOMetadataNode("JPEGvariety");
        IIOMetadataNode jfif = new IIOMetadataNode("app0JFIF");
        copyAttribute(oldJfif, jfif, "majorVersion");
        copyAttribute(oldJfif, jfif, "minorVersion");
        jfif.setAttribute("resUnits", "1");
        jfif.setAttribute("Xdensity", Integer.toString(Math.round(pixelsPerInch)));
        jfif.setAttribute("Ydensity", Integer.toString(Math.round(pixelsPerInch)));
        copyAttribute(oldJfif, jfif, "thumbWidth");
        copyAttribute(oldJfif, jfif, "thumbHeight");

        root.appendChild(var);
        root.appendChild(new IIOMetadataNode("markerSequence"));
        var.appendChild(jfif);

        return root;
    }

    private void copyAttribute(Node oldNode, IIOMetadataNode newNode, String name) {
        if (oldNode == null) {
            throw new NullPointerException("oldNode");
        }
        newNode.setAttribute(
                name, oldNode.getAttributes().getNamedItem(name).getNodeValue()
        );
    }

//	private Node findChild( Node parent, String name ) {
//		NodeList children = parent.getChildNodes();
//		for( int i = 0; i < children.getLength(); ++i ) {
//			if( children.item( i ).getLocalName().equals( name ) ) {
//				return children.item( i );
//			}
//		}
//		return null;
//	}
    /**
     * A helper method used when building the DOM tree to append new parameters.
     * Equivalent to {@code appendToNode( parent, nodeName, "value", value )}.
     */
    private void appendToNode(Node parent, String nodeName, Object value) {
        IIOMetadataNode child = new IIOMetadataNode(nodeName);
        child.setAttribute("value", value.toString());
        parent.appendChild(child);
    }

    /**
     * A helper method used when building the DOM tree to append new parameters.
     */
    private void appendToNode(Node parent, String nodeName, Object... attributeNamesAndValues) {
        IIOMetadataNode child = new IIOMetadataNode(nodeName);
        for (int i = 0; i < attributeNamesAndValues.length; i += 2) {
            child.setAttribute(attributeNamesAndValues[i].toString(), attributeNamesAndValues[i + 1].toString());
        }
        parent.appendChild(child);
    }

    @Override
    public String toString() {
        return String.format(
                "%s: format=%s, resolution=%.2fpx/mm, compression=%.2f, progressive=%b, comment=%s",
                getClass().getName(), getFormat(), getPixelsPerMillimetre(), getCompressionQuality(),
                isProgressiveScan(), getComment());
    }

    /**
     * Returns image format parameters as a package private structure. This
     * structure is used by other classes in the package to store and manipulate
     * the write parameters at an intermediate level. This method allows those
     * classes to share access to the internal parameters used by this writer.
     * (For example, this allows editing the compression options via an
     * {@link IIOWritePanel}. Only the compression and progressive scan settings
     * are available; the image format and metadata settings are only present in
     * this writer.
     *
     * @return the internal parameters instance
     */
    Parameters getParameters() {
        return iioparam;
    }

//	public static void main( String[] args ) {
////		BufferedImage i = new BufferedImage( 32, 32, BufferedImage.TYPE_INT_ARGB );
////		Graphics2D g = i.createGraphics();
////		g.setPaint( Color.RED );
////		g.drawLine( 0, 0, 31, 31 );
////		g.drawLine( 0, 31, 31, 0 );
////		g.dispose();
//
//		BufferedImage i = null;
//		try { i = ImageIO.read( new File( "d:\\test.png" ) ); } catch( Exception e ) { }
//
//		JPEG2000.registerReader( true );
//		JPEG2000.registerWriter( true );
//		SimpleImageWriter siw = new SimpleImageWriter( "jp2" );
//		siw.setPixelsPerInch( 300 );
//		siw.setComment( "Monkeypants!" );
//		siw.setCompressionQuality( 0.02f );
//
//		siw.setCompressionEnabled( true );
//		siw.setProgressiveScan( true );
//
//		try {
//			File f = new File( "d:\\test.jp2" );
//			siw.write( i, f );
//			BufferedImage c = ImageIO.read( f );
//			ImageIO.write( c, "png", new File( "d:\\test-out.png" ) );
//		} catch( IOException e ) {
//			e.printStackTrace();
//		}
//	}
    /**
     * Sets an internal encoding hint. These hints are typically used to work
     * around bugs with specific image format encoders.
     *
     * @param key the hint key
     * @param value the hint value
     */
    public void setEncodingHint(Object key, Object value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (key.equals("file-source")) {
            if (value instanceof Boolean) {
                cameFromFile = ((Boolean) value);
            }
        }
    }

    private boolean cameFromFile;

    /**
     * Registers a new image format supported for use with
     * {@link SimpleImageWriter}.
     *
     * @param wif a descriptor for the image format
     * @throws NullPointerException if the format is <code>null</code>
     */
    public static void registerImageFormat(WritableImageFormat wif) {
        if (wif == null) {
            throw new NullPointerException("wif");
        }
        formats.add(wif);
    }

    /**
     * Unregisters a previously registered image format.
     *
     * @param wif the format to remove
     */
    public static void unregisterImageFormat(WritableImageFormat wif) {
        formats.remove(wif);
    }

    /**
     * Returns an array of registered image formats.
     *
     * @return a new array of the supported image formats
     */
    public static WritableImageFormat[] getImageFormats() {
        return formats.toArray(new WritableImageFormat[formats.size()]);
    }

    private static Set<WritableImageFormat> formats = Collections.synchronizedSet(new LinkedHashSet<WritableImageFormat>());

    static {
        HashSet<String> types = new HashSet<>();
        for (String suffix : ImageIO.getWriterFileSuffixes()) {
            types.add(suffix.toLowerCase(Locale.CANADA));
        }
        registerBuiltInType(types, null, "png");
        registerBuiltInType(types, null, "jpg");
        registerBuiltInType(types, "j2k", "jp2");
        registerBuiltInType(types, null, "bmp");
        registerBuiltInType(types, null, "gif");
    }

    private static void registerBuiltInType(HashSet<String> supportedTypes, String keyBase, String suffix) {
        if (supportedTypes.contains(suffix)) {
            registerImageFormat(new DefaultImageFormat(keyBase == null ? suffix : keyBase, suffix));
        }
    }
}
