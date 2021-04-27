package ca.cgjennings.apps.arkham.deck.item;

import java.awt.Color;

/**
 * Interface implemented by filled shapes.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface ShapeStyle extends Style {

    /**
     * Returns the fill colour used when painting the item.
     *
     * @return the background colour
     */
    Color getFillColor();

    /**
     * Sets the fill colour used when painting the item.
     *
     * @param color the fill colour to use
     */
    void setFillColor(Color color);
}
