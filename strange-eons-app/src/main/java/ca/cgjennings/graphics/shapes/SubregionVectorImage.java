package ca.cgjennings.graphics.shapes;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/**
 * A vector image that consists of a rectangular region within another vector
 * image.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class SubregionVectorImage extends AbstractVectorImage {

    private AbstractVectorImage source;

    /**
     * Creates a new vector image that consists of a rectangular subregion of a
     * source vector image.
     *
     * @param sourceImage the image that this image is a subimage of
     * @param x the x-offset of the upper-left corner of the subregion
     * @param y the y-offset of the upper-left corner of the subregion
     * @param width the width of the subregion
     * @param height the height of the subregion
     */
    public SubregionVectorImage(AbstractVectorImage sourceImage, double x, double y, double width, double height) {
        if (sourceImage == null) {
            throw new NullPointerException("sourceImage");
        }

        source = sourceImage;
        tx = sourceImage.tx - x;
        ty = sourceImage.ty - y;
        iw = Math.max(0d, width);
        ih = Math.max(0d, height);
    }

    /**
     * Creates a new vector image that consists of a rectangular subregion of a
     * source vector image.
     *
     * @param sourceImage the image that this image is a subimage of
     * @param subregion a rectangle that defines the region of the source image
     * covered by this image
     */
    public SubregionVectorImage(AbstractVectorImage sourceImage, Rectangle2D subregion) {
        if (sourceImage == null) {
            throw new NullPointerException("sourceImage");
        }
        if (subregion == null) {
            throw new NullPointerException("subregion");
        }

        source = sourceImage;
        tx = sourceImage.tx - subregion.getX();
        ty = sourceImage.ty - subregion.getY();
        iw = Math.max(0d, subregion.getWidth());
        ih = Math.max(0d, subregion.getHeight());
    }

    /**
     * Returns the source image that this image is a subregion of.
     *
     * @return the source image used to create this subimage
     */
    public AbstractVectorImage getSourceImage() {
        return source;
    }

    @Override
    protected void render(Graphics2D g) {
        source.render(g);
    }
}
