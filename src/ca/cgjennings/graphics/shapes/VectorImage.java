package ca.cgjennings.graphics.shapes;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Interface implemented by classes that represent vector images. Described
 * algorithmically, pure vector images can be scaled to arbitrary sizes without
 * losing definition and becoming blocky as bitmap images do.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface VectorImage {

    /**
     * Returns the width of the image in an unspecified unit. Typically this
     * reflects the "natural" size of the image in the unit used by the
     * designer.
     *
     * @return the image width in image-specific units
     */
    double getWidth();

    /**
     * Returns the height of the image in an unspecified unit. Typically this
     * reflects the "natural" size of the image in the unit used by the
     * designer.
     *
     * @return the image height in image-specific units
     */
    double getHeight();

    /**
     * Paints the image at the specified location in the user coordinates of the
     * provided graphics context.
     *
     * @param g the graphics context to render into
     * @param x the x-coordinate to render at
     * @param y the y-coordinate to render at
     */
    void paint(Graphics2D g, double x, double y);

    /**
     * Paints the image at the specified location and size in the user
     * coordinates of the provided graphics context. If the
     * <code>fitToSize</code> parameter is <code>true</code>, then the aspect
     * ratio of the vector image will be maintained. If the aspect ratio of the
     * vector image does not match the specified width and height, then the
     * vector image will be scaled to just fit within the specified size, and
     * centered over the drawing area.
     *
     * @param g the graphics context to render into
     * @param x the x-coordinate to render at
     * @param y the y-coordinate to render at
     * @param width the width to paint the image at
     * @param height the height to paint the image at
     * @param fitToSize if <code>true</code>, the aspect ratio of the vector
     * image will be maintained
     */
    void paint(Graphics2D g, double x, double y, double width, double height, boolean fitToSize);

    /**
     * Paints the image to cover the specified rectangle in the user coordinates
     * of the provided graphics context. If the <code>fitToSize</code> parameter
     * is <code>true</code>, then the aspect ratio of the vector image will be
     * maintained. If the aspect ratio of the vector image does not match the
     * specified width and height, then the vector image will be scaled to just
     * fit within the specified size, and centered over the drawing area.
     *
     * @param g the graphics context to render into
     * @param paintRectangle the rectangle that the image should cover
     * @param fitToSize if <code>true</code>, the aspect ratio of the vector
     * image will be maintained
     */
    void paint(Graphics2D g, Rectangle2D paintRectangle, boolean fitToSize);

    /**
     * Creates a raster image from the vector image at the specified resolution,
     * assuming that the image width and height are measured in points.
     *
     * @param ppi the resolution to render the image at
     * @return a new bitmap image at the requested resolution
     */
    BufferedImage createRasterImage(double ppi);

    /**
     * Creates a raster image from the vector image with the specified width and
     * height (in pixels). If the <code>fitToSize</code> parameter is
     * <code>true</code>, then the aspect ratio of the vector image will be
     * maintained. If the aspect ratio of the vector image does not match the
     * specified width and height, then the vector image will be scaled to just
     * fit within the specified size, and centered over the drawing area.
     *
     * @param width the image width, in pixels
     * @param height the image height, in pixels
     * @param fitToSize if <code>true</code>, the aspect ratio of the vector
     * image will be maintained
     * @return a rendering of the vector image at the specified size
     */
    BufferedImage createRasterImage(int width, int height, boolean fitToSize);
}
