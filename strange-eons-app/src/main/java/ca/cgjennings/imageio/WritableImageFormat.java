package ca.cgjennings.imageio;

/**
 * This interface describes an image format that can be used to write an image
 * to a file.
 */
public interface WritableImageFormat {

    /**
     * Returns a short name for the format, similar to what might be found in a
     * file chooser, e.g., JPEG (*.jpg).
     *
     * @return short format name
     */
    String getName();

    /**
     * A full name for the format. May be {@code null}.
     *
     * @return the long name of the format
     */
    String getFullName();

    /**
     * A description of the trade-offs of using the format. May be {@code null}.
     *
     * @return the description of the format
     */
    String getDescription();

    /**
     * Returns the file extension for the format, e.g., jpg.
     *
     * @return the file extension to use for images
     */
    String getExtension();

    /**
     * Creates a simple image writer that can be used to write images in this
     * format. The image writer must be {@code dispose()}d of after use.
     *
     * @return a new image writer for the format
     */
    SimpleImageWriter createImageWriter();
}
