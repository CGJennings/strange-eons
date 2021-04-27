package ca.cgjennings.apps.arkham.deck.item;

/**
 * Interface implemented by items that support a master opacity change.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface MasterOpacityStyle extends Style {

    /**
     * Sets the master opacity for the item to the specified value, which must
     * be between 0 (completely transparent) and 1 (completely opaque). This
     * affects the master opacity of the item: if the item contains parts with
     * an opacity other than 1, then their effective opacity will be adjusted
     * proportionally.
     *
     * @param opacity the master opacity value for the item
     */
    void setOpacity(float opacity);

    /**
     * Returns the master opacity value of the item.
     *
     * @return the opacity level, from 0 (completely transparent) to 1
     * (completely opaque)
     */
    float getOpacity();
}
