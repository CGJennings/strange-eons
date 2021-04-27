package ca.cgjennings.apps.arkham.sheet;

import ca.cgjennings.apps.arkham.Length;
import java.awt.image.BufferedImage;

/**
 * An immutable representation of the printed size of a sheet, measured in
 * points.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class PrintDimensions {

    double w, h;

    /**
     * Creates a new print dimensions instance with the specified width and
     * height (in points).
     *
     * @param width the print width, in points
     * @param height the print height, in points
     */
    public PrintDimensions(double width, double height) {
        w = width;
        h = height;
    }

    /**
     * Creates a new print dimensions instance based on dimensions measured in
     * pixels.
     *
     * @param pixelWidth the width, in pixels
     * @param pixelHeight the height, in pixels
     * @param pixelsPerInch the print resolution, in pixels per inch
     */
    public PrintDimensions(int pixelWidth, int pixelHeight, double pixelsPerInch) {
        w = (pixelWidth / pixelsPerInch) * 72d;
        h = (pixelHeight / pixelsPerInch) * 72d;
    }

    /**
     * Creates a new print dimensions instance based on an image.
     *
     * @param image the image whose pixel width and height will be used
     * @param pixelsPerInch the print resolution, in pixels per inch
     * @throws NullPointerException if <code>image</code> is <code>null</code>
     */
    public PrintDimensions(BufferedImage image, double pixelsPerInch) {
        this(image.getWidth(), image.getHeight(), pixelsPerInch);
    }

    /**
     * Returns the width of the printed item, in points.
     *
     * @return the print width
     */
    public double getWidth() {
        return w;
    }

    /**
     * Returns the height of the printed item, in points.
     *
     * @return the print height
     */
    public double getHeight() {
        return h;
    }

    /**
     * Returns the width of the printed item, in a specific unit.
     *
     * @param unit one of the {@link Length} unit constants
     * @return the print width, in the requested unit
     */
    public double getWidthInUnit(int unit) {
        return Length.convert(w, Length.PT, unit);
    }

    /**
     * Returns the height of the printed item, in a specific unit.
     *
     * @param unit one of the {@link Length} unit constants
     * @return the print height, in the requested unit
     */
    public double getHeightInUnit(int unit) {
        return Length.convert(h, Length.PT, unit);
    }

    /**
     * Returns <code>true</code> if <code>obj</code> is a print dimensions
     * instance that represents the same printed size as this instance.
     *
     * @param obj the object to compare this to
     * @return <code>true</code> if and only if they are print dimensions of the
     * same size, within one one hundred thousandth of a point in any dimension
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (PrintDimensions.class != obj.getClass()) {
            return false;
        }
        final PrintDimensions other = (PrintDimensions) obj;
        return equals(other);
    }

    /**
     * Returns <code>true</code> if <code>other</code> represents the same
     * printed size as this.
     *
     * @param other the object to compare this to
     * @return <code>true</code> if and only if they are print dimensions of the
     * same size, within one one hundred thousandth of a point in any dimension
     */
    public boolean equals(PrintDimensions other) {
        final double EPSILON = 0.00001;
        if (Math.abs(this.w - other.w) > EPSILON) {
            return false;
        }
        if (Math.abs(this.h - other.h) > EPSILON) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return (int) w + ((int) h << 15);
    }

    @Override
    public String toString() {
        return "PrintDimensions{width=" + w + " pt, height=" + h + " pt}";
    }
}
