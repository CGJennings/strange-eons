package ca.cgjennings.io;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.imageio.plugins.jpeg2000.J2KImageWriteParam;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.logging.Level;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import resources.Settings;

/**
 * An {@code ObjectOutputStream} with additional features for Strange Eons.
 * This output stream includes special support for serializing images.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class SEObjectOutputStream extends ObjectOutputStream {

    public SEObjectOutputStream(OutputStream out) throws IOException {
        super(out);
    }

    /**
     * Writes an image to the input stream using lossless compression.
     *
     * @param image the image to write to the stream
     * @throws IOException if an I/O exception occurs
     */
    public void writeImage(BufferedImage image) throws IOException {
        if (image == null) {
            writeObject(null);
            return;
        }
        if (buff == null) {
            // assume compression of at least 25%; hence "* 3"
            buff = new ByteArrayOutputStream(image.getWidth() * image.getHeight() * 3);
        } else {
            // reuse the existing buffer when writing multiple images
            buff.reset();
        }
        ImageIO.write(image, "png", buff);

        if (Settings.getUser().getBoolean("portraits-use-wavelet-compression")) {
            ImageWriter iw = null;
            try {
                // there is a weird issue with the J2K compressor that some images
                // get mucked up on saving; for some reason, writing the image to PNG
                // then reading it back fixes the problem---perhaps it is an issue with
                // the colour space and PNG has the right one?
                BufferedImage src = ImageIO.read(new ByteArrayInputStream(buff.toByteArray()));
                ImageTypeSpecifier imageType = ImageTypeSpecifier.createFromRenderedImage(src);
                J2KImageWriteParam iwparam = new J2KImageWriteParam();
                iwparam.setEncodingRate(Double.MAX_VALUE);
                Iterator<ImageWriter> it = ImageIO.getImageWriters(imageType, "JPEG2000");
                if (!it.hasNext()) {
                    throw new AssertionError("no j2k support");
                }
                iw = it.next();
                ByteArrayOutputStream waveletBuff = new ByteArrayOutputStream(buff.size());
                try (ImageOutputStream ios = ImageIO.createImageOutputStream(waveletBuff)) {
                    iw.setOutput(ios);
                    iw.write(null, new IIOImage(src, null, null), iwparam);
                    iw.setOutput(null);
                }
                // in rare cases, the PNG image may be the smaller of the two
                if (waveletBuff.size() < buff.size()) {
                    buff = waveletBuff;
                }
            } catch (Throwable t) {
                StrangeEons.log.log(Level.WARNING, "failed to save portrait as wavelet; using RLE instead", t);
            } finally {
                if (iw != null) {
                    iw.dispose();
                }
            }
        }

        writeObject(buff.toByteArray());
    }

    private ByteArrayOutputStream buff;
}
