package ca.cgjennings.apps.arkham.deck.item;

import java.awt.Color;

/**
 * Implemented by page items that consist of a shape that can have an outline.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface OutlineStyle extends Style {

    /**
     * Returns the color used to draw the outline.
     *
     * @return the outline color
     */
    Color getOutlineColor();

    /**
     * Sets the color used to draw the outline.
     *
     * @param borderColor the outline color
     */
    void setOutlineColor(Color borderColor);

    /**
     * Returns the width, in points, of the outline.
     *
     * @return the outline line width
     */
    float getOutlineWidth();

    /**
     * Sets the width, in points, of the outline.
     *
     * @param borderWidth the outline width
     */
    void setOutlineWidth(float borderWidth);

    /**
     * Returns the dash pattern used to draw the outline.
     *
     * @return the dash pattern for the outline
     */
    DashPattern getOutlineDashPattern();

    /**
     * Sets the dash pattern used to draw the outline.
     *
     * @param pat the dash pattern type
     */
    void setOutlineDashPattern(DashPattern pat);

    /**
     * Returns the line cap style used on outline ends.
     *
     * @return the line cap style
     */
    LineCap getOutlineCap();

    /**
     * Sets the line cap style used on outline ends.
     *
     * @param cap the line cap type
     */
    void setOutlineCap(LineCap cap);

    /**
     * Returns the method used to join the line segments that make up the
     * outline.
     *
     * @return the line joining method
     */
    LineJoin getOutlineJoin();

    /**
     * Sets the method used to join the line segments that make up the outline.
     *
     * @param join the line joining method
     */
    void setOutlineJoin(LineJoin join);
}
