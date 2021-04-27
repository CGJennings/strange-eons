package ca.cgjennings.layout;

import java.awt.Graphics2D;
import java.awt.font.GraphicAttribute;
import java.awt.geom.Rectangle2D;
import java.util.Objects;
import resources.StrangeImage;

/**
 * Used to create styles applied to the Unicode replacement character in order
 * to render inline images. The attribute is responsible for sizing and painting
 * the image as part of a text layout.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0.3696; based on SizedImageGraphicAttribute, which was retired
 */
final class StrangeImageAttribute extends GraphicAttribute {

    private StrangeImage image;
    private float fImageWidth, fImageHeight;
    private float fOriginX, fOriginY;

    /**
     * Constructs a graphic attribute for a {@link StrangeImage}, to be rendered
     * at an explicit size. The offset (<tt>originX</tt>, <tt>originY</tt>) in
     * the image will be located at the attribute's origin within the
     * surrounding text. The alignment must be one of the values defined in
     * {@link GraphicAttribute}: <tt>TOP_ALIGNMENT</tt>,
     * <tt>BOTTOM_ALIGNMENT</tt>,
     * <tt>CENTER_ALIGNMENT</tt>, <tt>ROMAN_ALIGNMENT</tt>, or
     * <tt>HANGING_ALIGNMENT</tt>. The width and height specify the desired
     * width and height of the image in the same units of the graphics context
     * that renders the text.
     *
     * @param image the image to render
     * @param alignment the vertical alignment that the origin is relative to
     * @param originX the x-coordinate of the point within the image to place at
     * the origin
     * @param originY the y-coordinate of the point within the image to place at
     * the origin
     * @param width the image width
     * @param height the image height
     */
    public StrangeImageAttribute(StrangeImage image, int alignment, float originX, float originY, float width, float height) {
        super(alignment);
        this.image = image;

        fImageWidth = width;
        fImageHeight = height;

        fOriginX = originX;
        fOriginY = originY;
    }

    /**
     * Returns the ascent of this attribute.
     *
     * @return the distance from the top of the image to the origin
     */
    @Override
    public float getAscent() {
        return Math.max(0, fOriginY);
    }

    /**
     * Returns the descent of this attribute.
     *
     * @return the distance from the origin to the bottom of the image
     */
    @Override
    public float getDescent() {
        return Math.max(0, fImageHeight - fOriginY);
    }

    /**
     * Returns the advance of this attribute.
     *
     * @return the distance from the origin to the right edge of the image
     */
    @Override
    public float getAdvance() {
        return Math.max(0, fImageWidth - fOriginX);
    }

    /**
     * Returns a bounding rectangle for the attribute, relative to the rendering
     * position. A graphic can be rendered beyond its origin, ascent, descent,
     * or advance; but if it does, this method's implementation must indicate
     * where the graphic is rendered.
     *
     * @return a bounding rectangle for the attribute's rendered content
     */
    @Override
    public Rectangle2D getBounds() {
        return new Rectangle2D.Float(-fOriginX, -fOriginY, fImageWidth, fImageHeight);
    }

    @Override
    public void draw(Graphics2D g, float x, float y) {
        image.paint(g, x - fOriginX, y - fOriginY, fImageWidth, fImageHeight, false);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.image);
        hash = 17 * hash + Float.floatToIntBits(this.fImageWidth);
        hash = 17 * hash + Float.floatToIntBits(this.fImageHeight);
        hash = 17 * hash + Float.floatToIntBits(this.fOriginX);
        hash = 17 * hash + Float.floatToIntBits(this.fOriginY);
        return hash;
    }

    @Override
    public boolean equals(Object rhs) {
        if (rhs instanceof StrangeImageAttribute) {
            return equals((StrangeImageAttribute) rhs);
        }
        return false;
    }

    public boolean equals(StrangeImageAttribute rhs) {
        if (rhs == null) {
            return false;
        }
        if (this == rhs) {
            return true;
        }
        if (fOriginX != rhs.fOriginX || fOriginY != rhs.fOriginY || fImageWidth != rhs.fImageWidth || fImageHeight != rhs.fImageHeight) {
            return false;
        }
        if (getAlignment() != rhs.getAlignment()) {
            return false;
        }
        if (!image.equals(rhs.image)) {
            return false;
        }
        return true;
    }
}
