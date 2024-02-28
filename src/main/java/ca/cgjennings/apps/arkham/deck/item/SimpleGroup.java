package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.deck.Deck;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.logging.Level;

/**
 * A basic implementation of the group interface for storing selection groups.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class SimpleGroup implements Group {

    private static final long serialVersionUID = 2452391439644716752L;
    private LinkedHashSet<PageItem> members;
    private transient boolean isBeingAdded = false;
//	private transient Rectangle2D.Double rect = null;

    public SimpleGroup() {
        members = new LinkedHashSet<>();
    }

    public SimpleGroup(Collection<? extends PageItem> items) {
        this();
        for (PageItem i : items) {
            add(i);
        }
    }

    public SimpleGroup(PageItem[] items) {
        this();
        for (PageItem i : items) {
            add(i);
        }
    }

    @Override
    public void addToSelection(Deck d) {
        if (isBeingAdded) {
            return;
        }
        isBeingAdded = true;
        for (PageItem i : members) {
            d.addToSelection(i);
        }
        isBeingAdded = false;
    }

    @Override
    public void removeFromSelection(Deck d) {
        if (isBeingAdded) {
            return;
        }
        isBeingAdded = true;
        for (PageItem i : members) {
            d.removeFromSelection(i);
        }
        isBeingAdded = false;
    }

    @Override
    public void add(PageItem p) {
        Group old = p.getGroup();
        if (old == this) {
            return;
        }
        if (old != null) {
            old.remove(p);
        }

        members.add(p);
        p.setGroup(this);
//		rect = null;
    }

    @Override
    public void remove(PageItem p) {
        if (p.getGroup() != this) {
            throw new IllegalArgumentException("tried to remove non-member");
        }

        members.remove(p);
        p.setGroup(null);
//		rect = null;
    }

    @Override
    public void clear() {
        for (PageItem p : members) {
            p.setGroup(null);
        }
        members.clear();
//		rect = null;
    }

    @Override
    public boolean contains(PageItem p) {
        boolean member = p.getGroup() == this;
        if (StrangeEons.log.isLoggable(Level.WARNING)) {
            boolean internalMember = members.contains(p);
            if ((member && !internalMember) || (!member && members.contains(p))) {
                StrangeEons.log.warning("mismatch between p.getGroup() and group membership");
            }
        }
        return member;
    }

    @Override
    public Iterator<PageItem> iterator() {
        return new Iterator<PageItem>() {
            Iterator<PageItem> it = members.iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public PageItem next() {
                return it.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Rectangle2D.Double getRectangle() {
//		if( rect != null ) return rect;
        if (members.isEmpty()) {
            return null;
        }
        Rectangle2D.Double rect = null;
        for (PageItem p : members) {
            if (rect == null) {
                rect = p.getRectangle();
            } else {
                rect.add(p.getRectangle());
            }
        }
        return rect;
    }

    private static final int VERSION = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(VERSION);
        out.writeObject(members);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        /* final int version = */ in.readInt();
        members = (LinkedHashSet<PageItem>) in.readObject();
    }
}
