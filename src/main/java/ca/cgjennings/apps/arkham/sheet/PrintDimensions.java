package ca.cgjennings.apps.arkham.sheet;

import ca.cgjennings.apps.arkham.Length;
import java.awt.image.BufferedImage;

/**
 * An immutable representation of the printed size of a sheet,
 * measured in points.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class PrintDimensions {

    private final double w;
    private final double h;

    /**
     * Creates a new print dimensions instance with the specified width and
     * height (in points).
     *
     * @param width the print width, in points; must be non-negative
     * @param height the print height, in points; must be non-negative
     */
    public PrintDimensions(double width, double height) {
        if (width < 0d) throw new IllegalArgumentException("width < 0");
        if (height < 0d) throw new IllegalArgumentException("height < 0");
        w = width;
        h = height;
    }

    /**
     * Creates a new print dimensions instance based on a source image
     * (typically, a rendering of a {@link Sheet}).
     *
     * @param image the image whose pixel width and height will be used
     * @param pixelsPerInch the image resolution, in pixels per inch
     * @param finalBleedMargin the size of the final desired bleed margin, measured in points
     * @param includedBleedMargin the size of any bleed margin already included in the image, measured in points
     * @throws NullPointerException if {@code image} is {@code null}
     */
    public PrintDimensions(BufferedImage image, double pixelsPerInch, double finalBleedMargin, double includedBleedMargin) {
        double bleedMargin = finalBleedMargin - includedBleedMargin;
        double bleedMarginAdjustment = bleedMargin * 2d;
        double imageWidth = (image.getWidth() / pixelsPerInch) * 72d;
        double imageHeight = (image.getHeight() / pixelsPerInch) * 72d;
        w = Math.max(0, imageWidth + bleedMarginAdjustment);
        h = Math.max(0, imageHeight + bleedMarginAdjustment);
    }

    // TEMPORARY: for testing, will give wrong results
    public PrintDimensions(BufferedImage image, double pixelsPerInch, double finalBleedMargin) {
        this(image, pixelsPerInch, finalBleedMargin, 0d);
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
     * Returns {@code true} if {@code obj} is a print dimensions instance that
     * represents the same printed size as this instance.
     *
     * @param obj the object to compare this to
     * @return {@code true} if and only if they are print dimensions of the same
     * size, within one one hundred thousandth of a point in any dimension
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
     * Returns {@code true} if {@code other} represents the same printed size as
     * this.
     *
     * @param other the object to compare this to
     * @return {@code true} if and only if they are print dimensions of the same
     * size, within one one hundred thousandth of a point in any dimension
     */
    public boolean equals(PrintDimensions other) {
        final double EPSILON = 0.00001;
        if (Math.abs(this.w - other.w) > EPSILON) {
            return false;
        }
        return Math.abs(this.h - other.h) <= EPSILON;
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
