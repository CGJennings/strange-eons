package ca.cgjennings.apps.arkham.deck.item;

/**
 * Implemented by items that can be resized.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface SizablePageItem extends PageItem {

    /**
     * Set the new size of this item, in points.
     *
     * @param width the new width of the item
     * @param height the new height of the item
     */
    public void setSize(double width, double height);
}
