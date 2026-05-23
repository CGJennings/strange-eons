package ca.cgjennings.apps.arkham.deck.item;

/**
 * Implemented by deck cards that allow themselves to be rotated in 90 degree
 * increments and mirrored.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface FlippablePageItem extends PageItem {

    public void setOrientation(int orientation);

    public void turnLeft();

    public void turnRight();

    public void flip();
}
