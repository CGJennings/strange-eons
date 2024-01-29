package ca.cgjennings.apps.arkham.deck.item;

import java.awt.Color;

/**
 * Implemented by page items that include a styled border.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface LineStyle extends Style {

    /**
     * Returns the color used to draw the border.
     *
     * @return the border color
     */
    Color getLineColor();

    /**
     * Returns the width, in points, of the line.
     *
     * @return the border line width
     */
    float getLineWidth();

    /**
     * Returns the dash pattern used to draw the line.
     *
     * @return the dash pattern for the border
     */
    DashPattern getLineDashPattern();

    /**
     * Sets the color used to draw the line.
     *
     * @param borderColor
     */
    void setLineColor(Color borderColor);

    /**
     * Sets the width, in points, of the line.
     *
     * @param borderWidth
     */
    void setLineWidth(float borderWidth);

    /**
     * Sets the dash pattern used to draw the line.
     *
     * @param pat the dash pattern type
     */
    void setLineDashPattern(DashPattern pat);

    /**
     * Sets the line cap style used on line ends.
     *
     * @param cap the line cap type
     */
    void setLineCap(LineCap cap);

    /**
     * Returns the line cap style used on line ends.
     *
     * @return the line cap style
     */
    LineCap getLineCap();

    /**
     * Sets the line join style used for paths containing multiple line or curve
     * segments.
     *
     * @param join the new line join style
     */
    void setLineJoin(LineJoin join);

    LineJoin getLineJoin();
}
