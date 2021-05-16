package ca.cgjennings.apps.arkham.deck.item;

import java.awt.BasicStroke;
import java.awt.Stroke;

/**
 * A factory class that creates rendering attributes that are suitable for
 * particular {@link Style}s. Using the factory ensures that rendering effects
 * will be consistent for all {@link PageItem}s.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class RenderingAttributeFactory {

    private RenderingAttributeFactory() {
    }

    /**
     * Create a stroke for drawing lines and other path elements.
     *
     * @param style the style to create a stroke for
     * @return a suitable {@code Stroke} for drawing
     */
    public static Stroke createLineStroke(LineStyle style) {
        final float lineWidth = style.getLineWidth();
        final int lineCap = style.getLineCap().toInt();
        final int lineJoin = style.getLineJoin().toInt();
        return new BasicStroke(
                lineWidth,
                lineCap,
                lineJoin,
                10f,
                style.getLineDashPattern().createDashArray(lineWidth, lineCap == BasicStroke.CAP_BUTT ? 0f : lineWidth),
                0f
        );
    }

    /**
     * Create a stroke for drawing shape outlines.
     *
     * @param style the style to create a stroke for
     * @return a suitable {@code Stroke} for drawing
     */
    public static Stroke createOutlineStroke(OutlineStyle style) {
        final float lineWidth = style.getOutlineWidth();
        final int lineCap = style.getOutlineCap().toInt();
        final int lineJoin = style.getOutlineJoin().toInt();
        return new BasicStroke(
                lineWidth,
                lineCap,
                lineJoin,
                10f,
                style.getOutlineDashPattern().createDashArray(lineWidth, lineCap == BasicStroke.CAP_BUTT ? 0f : lineWidth),
                0f
        );
    }

    /**
     * Create a stroke from the specified drawing attributes.
     *
     * @param width the stroke width
     * @param cap the line cap style
     * @param join the line join style
     * @param dashPattern the dash pattern style
     * @return a stroke for the specified attributes
     */
    public static Stroke createStroke(float width, LineCap cap, LineJoin join, DashPattern dashPattern) {
        final int lineCap = cap.toInt();
        final int lineJoin = join.toInt();
        return new BasicStroke(
                width,
                lineCap,
                lineJoin,
                10f,
                dashPattern.createDashArray(width, lineCap == BasicStroke.CAP_BUTT ? 0f : width),
                0f
        );
    }
}
