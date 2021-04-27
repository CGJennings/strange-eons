package ca.cgjennings.ui.dnd;

import ca.cgjennings.graphics.ImageUtilities;
import java.awt.Image;
import java.awt.image.BufferedImage;

/**
 * A drag token combines an object with a visual representation which can be
 * dragged around the display by the user. It is part of a simple framework for
 * creating drag-and-drop interfaces that bypasses AWT/Swing drag-and-drop, and
 * thus is more consistent across platforms.
 *
 * @param T the type of object to be dragged
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see DragManager
 */
public class DragToken<T> {

    private T object;
    private BufferedImage image;
    private int xOff, yOff;

    /**
     * Creates a new token that represents the specified object with the given
     * image. The token's handle (the point by which it is dragged; typically,
     * the point at which the cursor attaches to the image) will be located at
     * the center of the image.
     *
     * @param tokenObject the object to be represented
     * @param tokenImage the image that represents the object
     * @throws NullPointerException if the image is <code>null</code>
     */
    public DragToken(T tokenObject, Image tokenImage) {
        if (tokenImage == null) {
            throw new NullPointerException("tokenImage");
        }
        object = tokenObject;
        image = ImageUtilities.ensureIntRGBFormat(ImageUtilities.imageToBufferedImage(tokenImage));
        xOff = image.getWidth() / 2;
        yOff = image.getHeight() / 2;
    }

    /**
     * Creates a new token that represents the specified object with the given
     * image. The token's handle (the point by which it is dragged; typically,
     * the point at which the cursor attaches to the image) will be located at
     * the specified offset.
     *
     * @param tokenObject the object to be represented
     * @param tokenImage the image that represents the object
     * @param handleOffsetX the x-coordinate of the handle, relative to the
     * upper-left corner of the image
     * @param handleOffsetY the y-coordinate of the handle, relative to the
     * upper-left corner of the image
     * @throws NullPointerException if the image is <code>null</code>
     */
    public DragToken(T tokenObject, Image tokenImage, int handleOffsetX, int handleOffsetY) {
        if (tokenImage == null) {
            throw new NullPointerException("tokenImage");
        }
        object = tokenObject;
        image = ImageUtilities.ensureIntRGBFormat(ImageUtilities.imageToBufferedImage(tokenImage));
        xOff = handleOffsetX;
        yOff = handleOffsetY;
    }

    /**
     * Returns the token object.
     *
     * @return the object whose representation is being dragged
     */
    public T getObject() {
        return object;
    }

    /**
     * Returns the token image. This may return a different image from the one
     * used to create the token.
     *
     * @return the image used to represent the dragged object
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * Returns the X coordinate of the image at which the cursor should be
     * attached while dragging.
     *
     * @return the X-coordinate of the handle, relative to the upper-left corner
     * of the image
     */
    public int getHandleOffsetX() {
        return xOff;
    }

    /**
     * Returns the Y coordinate of the image at which the cursor should be
     * attached while dragging.
     *
     * @return the Y-coordinate of the handle, relative to the upper-left corner
     * of the image
     */
    public int getHandleOffsetY() {
        return yOff;
    }

    /**
     * Returns a string string description of the token. The description will
     * include the string value of the represented object.
     *
     * @return a debugging string for the token
     */
    @Override
    public String toString() {
        return "DragToken{" + object + '}';
    }
}
