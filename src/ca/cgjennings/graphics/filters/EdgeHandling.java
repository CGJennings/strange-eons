package ca.cgjennings.graphics.filters;

/**
 * An enumeration of the options for handling off-image source pixels. When a
 * filter needs to refer (conceptually) to a pixel that lies outside of the
 * actual image data, the edge handling mode describes what value that pixel
 * will be treated as having.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public enum EdgeHandling {
    /**
     * When a pixel value in the output image cannot be determined without
     * referring to an off-image pixel, the output pixel will be zero (a black
     * pixel, with alpha=0 if the destination image has an alpha channel).
     */
    ZERO,
    /**
     * Treat off-edge pixels as equivalent to the pixel value at the edge of the
     * image.
     */
    REPEAT,
    /**
     * Treat the image as if it were on an infinite plane tiled with the image
     * in all directions. (This can also be thought of as wrapping the image
     * around to the opposite edge or mapping it to the surface of a torus.)
     */
    WRAP
}
