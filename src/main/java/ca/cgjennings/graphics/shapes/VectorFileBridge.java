package ca.cgjennings.graphics.shapes;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * This interface is used to bridge between a concrete {@link VectorImage}
 * subclass (such as {@link SVGVectorImage}) and an optional implementation
 * library (such as Batik or SVG Salamander).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
interface VectorFileBridge {

    /**
     * Returns a bounding rectangle for the content. As the bridge represents a
     * static graphic, the bounds must be constant.
     *
     * @return the minimum rectangle that encloses the graphic content
     */
    Rectangle2D bounds();

    /**
     * Renders the content without translation or scaling. Thus, the content
     * should be drawn with the area described by {@link bounds()}.
     *
     * @param destination the object that the graphics content is drawing into,
     * such as a {@link BufferedImage}, if known (may be {@code null})
     * @param g the graphics context to use for drawing
     */
    void render(Object destination, Graphics2D g);

    /**
     * Disposes of the bridge, freeing any native resources that might be held
     * by the implementation. Once this has been called, the bridge is no longer
     * valid, and the effect of calling other methods in this interface is
     * undefined.
     */
    void dispose();
}
