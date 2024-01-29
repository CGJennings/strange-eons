package ca.cgjennings.apps.arkham.deck.item;

/**
 * A page item that depends on an external file to recreate its content.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface DependentPageItem {

    /**
     * Returns the path to the external data source.
     *
     * @return the path to the data this item depends upon
     */
    public String getPath();

    /**
     * Set the path the external data source.
     *
     * @param path the path to the data this item depends upon
     */
    public void setPath(String path);

    /**
     * Indicates to the item that it should recreate its content because the
     * external file has changed. This method should return {@code true} if the
     * item is updated successfully, or {@code false} if the item could not be
     * updated, typically because the new item is incompatible with the old one.
     *
     * @return {@code true} if the item was replaced
     */
    public boolean refresh();
}
