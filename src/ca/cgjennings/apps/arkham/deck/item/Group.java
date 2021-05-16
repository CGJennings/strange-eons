package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.deck.Deck;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.Iterator;

/**
 * A grouping of {@code PageItem}s that are selected and unselected as a
 * unit.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface Group extends Serializable, Iterable<PageItem> {

    public void addToSelection(Deck d);

    public void removeFromSelection(Deck d);

    public void add(PageItem p);

    public void remove(PageItem p);

    public void clear();

    public boolean contains(PageItem p);

    @Override
    public Iterator<PageItem> iterator();

    public Rectangle2D.Double getRectangle();
}
