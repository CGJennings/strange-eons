package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.sheet.FinishStyle;

/**
 * Implemented by page items that can include a bleed margin.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface BleedMarginStyle extends Style {

    /**
     * Returns the finish style set on this item, or null if the deck default
     * style should be used.
     *
     * @return the finish style to use
     */
    FinishStyle getFinishStyle();

    /**
     * Sets the edge finish style to use for this item, or null if the deck
     * default option should be used.
     *
     * @param style the style to use
     */
    void setFinishStyle(FinishStyle style);

    /**
     * Returns the bleed margin width to use when the relevant finish option is
     * selected.
     *
     * @return the positive width in points
     */
    double getBleedMarginWidth();

    /**
     * Sets the bleed margin width to use when the relevant finish option is
     * selected.
     *
     * @param widthInPoints the positive bleed margin width
     */
    void setBleedMarginWidth(double widthInPoints);
}
