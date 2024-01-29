package ca.cgjennings.io;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.component.GameComponent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import resources.ResourceKit;

/**
 * An {@code ObjectInputStream} with additional features for Strange Eons. This
 * input stream includes special support for serializing images. It also
 * performs some transparent conversions to provide backwards compatibility with
 * previous releases.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class SEObjectInputStream extends ObjectInputStream {

    public SEObjectInputStream(InputStream in) throws IOException {
        super(in);
        enableResolveObject(true);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        final String className = desc.getName();
        String replacement = null;

        if (className.startsWith("ca.cgjennings.apps.arkham.character.")) {
            replacement = "ca.cgjennings.apps.arkham.component."
                    + desc.getName().substring("ca.cgjennings.apps.arkham.character.".length());
        }

        if (replacement != null) {
            try {
                return Class.forName(replacement, false, ClassLoader.getSystemClassLoader());
            } catch (Throwable e) {
                // in case someone put their own subclass in c.c.a.a.character,
                // we'll try using the original name
                StrangeEons.log.log(Level.WARNING, "Expected to replace " + desc + " with " + replacement + " but failed", e);
            }
        }
        return super.resolveClass(desc);
    }

    @Override
    @SuppressWarnings({"deprecated", "deprecation"})
    protected Object resolveObject(Object obj) throws IOException {
        if (obj != null) {
            if (obj instanceof GameComponent) {
                try {
                    String className = obj.getClass().getName();
                    // silently upgrade deprecated card classes
                    if (className.equals("ca.cgjennings.apps.arkham.component.MiscellaneousSmall")) {
                        obj = Class.forName("ca.cgjennings.apps.arkham.component.MiscellaneousSmall2")
                                .getMethod("upgradeOldInstance", obj.getClass())
                                .invoke(null, obj);
                    }
                } catch (Exception e) {
                    throw new IOException("failed to upgrade from old file format", e);
                }
            } else if (obj instanceof ca.cgjennings.apps.arkham.sheet.ComponentFace.DeckSnappingHint) {
                return ca.cgjennings.apps.arkham.sheet.Sheet.DeckSnappingHint.valueOf(((Enum) obj).name());
            } else if (obj instanceof ca.cgjennings.apps.arkham.sheet.CharacterSheet.DeckSnappingHint) {
                return ca.cgjennings.apps.arkham.sheet.Sheet.DeckSnappingHint.valueOf(((Enum) obj).name());
            }
        }
        return obj;
    }

    /**
     * This is a convenience method that calls {@code readObject} and casts the
     * result to {@code String} before returning it.
     *
     * @return a string written to the stream at the current position
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the true class of the object cannot be
     * loaded
     * @throws ClassCastException if the object at this point in the stream is
     * not a string
     */
    public String readString() throws IOException, ClassNotFoundException {
        return (String) readObject();
    }

    /**
     * Reads an image from the stream that was previously written with
     * {@link SEObjectOutputStream#writeImage(java.awt.image.BufferedImage)}.
     *
     * @return an image in {@code TYPE_INT_RGB} or {@code TYPE_INT_ARGB} format
     * @throws IOException if an I/O error occurs
     * @throws InvalidClassException if no image is found at this point in the
     * stream
     */
    public BufferedImage readImage() throws IOException {
        byte[] imagedata = null;
        try {
            imagedata = (byte[]) readObject();
            // originally wrote a null image
            if (imagedata == null) {
                return null;
            }
        } catch (ClassNotFoundException | ClassCastException e) {
            // dealt with later, since img will be null
        }

        BufferedImage img = null;
        if (imagedata != null) {
            try {
                img = ImageIO.read(new ByteArrayInputStream(imagedata));
            } catch (Exception e) {
                // dealt with later, since img will be null
                StrangeEons.log.log(Level.WARNING, "invalid image data", e);
            }
        }
        if (img == null) {
            throw new InvalidClassException("no image appears to have been written at this point in the stream");
        }
        img = ResourceKit.prepareNewImage(img);
        return img;
    }
}
