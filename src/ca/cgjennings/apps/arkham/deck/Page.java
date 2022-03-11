package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.BusyDialog;
import ca.cgjennings.apps.arkham.deck.item.CardFace;
import ca.cgjennings.apps.arkham.deck.item.Group;
import ca.cgjennings.apps.arkham.deck.item.PageItem;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Objects;

/**
 * A page layout of card sheets.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class Page implements Serializable, Cloneable {

    static final long serialVersionUID = 7253298809588649754L;

    private Page() {
    }

    /**
     *
     */
    public Page(Deck deck) {
        this.deck = deck;
    }

    /**
     * Clone a PageItem and add the clone to the page; the new clone is
     * returned.
     */
    public PageItem addCardFromTemplate(PageItem template) {
        PageItem clone = template.clone();
        clone.setX(margin());
        clone.setY(margin());
        if (getDeck() != null) {
            getDeck().getDefaultStyleApplicator().apply(clone);
        }
        addCard(clone, true);
        return clone;
    }

    /**
     * Add a new PageItem to this page.
     */
    public void addCard(PageItem pageItem) {
        addCard(pageItem, true);
    }

    public void addCard(PageItem pageItem, boolean fit) {
        assert (!cards.contains(Objects.requireNonNull(pageItem, "pageItem")));

        if (pageItem instanceof CardFace) {
            CardFace cf = (CardFace) pageItem;
            cf.setBleedMarginWidth(deck.getBleedMarginWidth());
            cf.setFinishStyle(deck.getFinishStyle());
        }

        pageItem.setPage(this);
        cards.add(pageItem);

        if (fit) {
            fitCard(pageItem);
        } else {
            refreshView();
        }

        getDeck().markUnsavedChanges();
    }

    public void moveCardToFront(PageItem card) {
        boolean found = false;
        for (int i = 0; i < cards.size(); ++i) {
            if (!found) {
                if (cards.get(i) == card) {
                    found = true;
                }
            } else {
                cards.set(i - 1, cards.get(i));
            }
        }
        if (found) {
            cards.set(cards.size() - 1, card);
        }
        getDeck().markUnsavedChanges();
    }

    public void moveCardToBack(PageItem card) {
        int lowestUnlocked = 0;
        // if the card is unlocked, do not move it below
        // a locked card unless it is already just above
        // a locked card
        if (!card.isSelectionLocked()) {
            for (int i = 0; i < cards.size(); ++i) {
                if (!cards.get(i).isSelectionLocked()) {
                    lowestUnlocked = i;
                    break;
                }
            }
        }

        for (int i = 0; i < cards.size(); ++i) {
            if (cards.get(i) == card) {
                if (i <= lowestUnlocked) {
                    lowestUnlocked = 0;
                }
                cards.remove(i);
                cards.add(lowestUnlocked, card);
                getDeck().markUnsavedChanges();
                break;
            }
        }
    }

    /**
     * Centers the content on the page. Finds the bounding box of all cards,
     * determines the translation needed to center that bounding box on the
     * page, and then applies the translation to each card.
     */
    public void centerContent() {
        if (cards.size() > 0) {
            // find the bounding box of all cards on the page
            Rectangle2D bounds = cards.get(0).getRectangle();
            for (int i = 1; i < cards.size(); ++i) {
                bounds = bounds.createUnion(cards.get(i).getRectangle());
            }

            // set (dx,dy) to the point to place the upper-left corner
            // of the bounding box at in order to center the cards
            double dx = (pageWidth() - bounds.getWidth()) / 2d;
            double dy = (pageHeight() - bounds.getHeight()) / 2d;

            // then determine the translation needed to move the actual bounds
            // for the cards to that center point; (dx,dy) is now a delta
            // to apply to each card position
            dx -= bounds.getX();
            dy -= bounds.getY();

            // if not already centered, apply the delta needed to center
            if (dx != 0d || dy != 0d) {
                for (int i = 0; i < cards.size(); ++i) {
                    PageItem c = cards.get(i);
                    c.setX(c.getX() + dx);
                    c.setY(c.getY() + dy);
                }
                refreshView();
                getDeck().markUnsavedChanges();
            }
        }
    }

    /**
     * Shifts a card to an empty area of the page, if possible.
     *
     * @param card
     */
    private void fitCard(PageItem card) {
        Point2D p = card.getLocation();
        PageItem precedent;
        int emergencyBrake = 0; // prevent infinite loop in any case
        while ((precedent = getOverlappedCard(card)) != null && (emergencyBrake++ < FIT_LOOP_LIMIT)) {
            card.setX(precedent.getRectangle().getMaxX() + 0.00001d);
            if (card.getX() + card.getWidth() > pageWidth()) {
                card.setX(p.getX());
                card.setY(precedent.getRectangle().getMaxY() + 0.00001d);
                if (card.getY() + card.getHeight() > pageHeight()) {
                    // give up and use original location
                    card.setLocation(p);
                    break;
                }
            }
        }
        if (emergencyBrake >= FIT_LOOP_LIMIT) {
            card.setLocation(p);
        }

        refreshView();
        getDeck().markUnsavedChanges();
    }

    private static final int FIT_LOOP_LIMIT = 10000;

    /**
     * Snap an item into place relative to the highest intersecting item, if
     * any.
     */
    public void snapCard(PageItem card) {
        snapCard(new PageItem[]{card});
    }

    /**
     * Snaps a group of cards into place as a unit.
     */
    public void snapCard(PageItem cards[]) {
        if (cards.length == 0) {
            return;
        }
        Point2D dp = getSnapPosition(cards);
        PageItem card = cards[cards.length - 1];

        double dx, dy;
        dx = dp.getX() - card.getX();
        dy = dp.getY() - card.getY();

        for (PageItem c : cards) {
            c.setX(c.getX() + dx);
            c.setY(c.getY() + dy);
        }

        refreshView();
        getDeck().markUnsavedChanges();
    }

    /**
     * Return the position that cards will snap to if snapped from their current
     * position.
     *
     * @param cards the cards to determine the snap position for
     * @return the point that the cards would snap to
     */
    public Point2D getSnapPosition(PageItem[] cards) {
        if (cards == null || cards.length == 0) {
            throw new IllegalArgumentException("empty card list");
        }

        PageItem card = cards[cards.length - 1];
        Point2D.Double point = new Point2D.Double(card.getX(), card.getY());
        PageItem precedent = getCardToSnapTo(card, card.getClassesSnappedTo());

        // the card is over another card; snap to a card edge
        if (precedent != null) {
            // determine if snapping will be to the inside or the outside of the snap edge
            boolean snapToInner = false;
            if (card.getSnapTarget() == PageItem.SnapTarget.TARGET_INSIDE) {
                snapToInner = true;
            }
            if (card.getSnapTarget() == PageItem.SnapTarget.TARGET_MIXED) {
                snapToInner = card.getSnapClass() != precedent.getSnapClass();
            }
            Rectangle2D r = card.getRectangle();
            Rectangle2D p = precedent.getRectangle();

            double px = p.getX(), py = p.getY();
            double pcx = p.getX() + p.getWidth() / 2;
            double pcy = p.getY() + p.getHeight() / 2;

            double rcx = r.getX() + r.getWidth() / 2;
            double rcy = r.getY() + r.getHeight() / 2;
            double dx = rcx - pcx, dy = rcy - pcy;

            if (Math.abs(dx) >= Math.abs(dy)) {
                boolean alignRight = dx > 0d;
                if (alignRight) {
                    if (snapToInner) {
                        point.x = px + p.getWidth() - r.getWidth();
                    } else {
                        point.x = px + p.getWidth();
                    }
                } else {
                    if (snapToInner) {
                        point.x = px;
                    } else {
                        point.x = px - r.getWidth();
                    }
                }

                boolean alignBottom = dy > 0d;
                if (p.getHeight() - r.getHeight() < 0) {
                    alignBottom = !alignBottom;
                }
                if (alignBottom) {
                    point.y = py + p.getHeight() - r.getHeight();
                } else {
                    point.y = py;
                }

            } else {

                boolean alignRight = dx > 0d;
                if (p.getWidth() - r.getWidth() < 0) {
                    alignRight = !alignRight;
                }
                if (alignRight) {
                    point.x = px + p.getWidth() - r.getWidth();
                } else {
                    point.x = px;
                }

                boolean alignBottom = dy > 0d;
                if (alignBottom) {
                    if (snapToInner) {
                        point.y = py + p.getHeight() - r.getHeight();
                    } else {
                        point.y = py + p.getHeight();
                    }
                } else {
                    if (snapToInner) {
                        point.y = py;
                    } else {
                        point.y = py - r.getHeight();
                    }
                }
            }
        } // not over a card, snap to grid position
        else {
            if (card.getClassesSnappedTo().contains(PageItem.SnapClass.SNAP_PAGE_GRID)) {
                double snap = gridSeparation() / 2d;
                if (snap > 0) {
                    point.x = Math.rint(card.getX() / snap) * snap;
                    point.y = Math.rint(card.getY() / snap) * snap;
                }
            }
        }

        return point;
    }

    /**
     * Remove a card from this page.
     */
    public void removeCard(PageItem card) {
        if (card.getPage() != this) {
            throw new IllegalArgumentException("item is not on this page: " + card);
        }

        if (card.getGroup() != null) {
            card.getGroup().remove(card);
        }

        card.setPage(null);
        cards.remove(card);

        if (deck.getSelectionPage() == this) {
            deck.removeFromSelection(card);
        }

        refreshView();
        getDeck().markUnsavedChanges();
    }

    public PageItem getCard(int i) {
        return cards.get(i);
    }

    public PageItem[] getCards() {
        return cards.toArray(new PageItem[0]);
    }

    public int getCardCount() {
        return cards.size();
    }

    private void refreshView() {
        if (view != null) {
            view.pageChangeEvent();
        }
    }

    public PageItem getCardAtLocation(Point2D point) {
        for (int i = cards.size() - 1; i >= 0; --i) {
            if (cards.get(i).hitTest(point)) {
                return cards.get(i);
            }
        }
        return null;
    }

    public PageItem[] getAllCardsAtLocation(Point2D point) {
        ArrayList<PageItem> list = new ArrayList<>();
        for (int i = cards.size() - 1; i >= 0; --i) {
            if (cards.get(i).hitTest(point)) {
                list.add(cards.get(i));
            }
        }
        return list.toArray(new PageItem[0]);
    }

    /**
     * Returns the card with the highest Z-index that overlaps with this card,
     * or {@code null} if the card does not overlap with any other card.
     */
    public PageItem getOverlappedCard(PageItem card) {
        Rectangle2D r = card.getRectangle();
        for (int i = cards.size() - 1; i >= 0; --i) {
            PageItem candidate = cards.get(i);
            if (candidate == card) {
                continue;
            }
            if (r.intersects(candidate.getRectangle())) {
                return candidate;
            }
        }
        return null;
    }

    public PageItem getCardToSnapTo(PageItem card, EnumSet<PageItem.SnapClass> snapToClass) {
        Rectangle2D r = card.getRectangle();
        double shortestSqDist = Double.MAX_VALUE;
        PageItem overlapping = null;
        Group g = card.getGroup();

        double cx = card.getX() + card.getWidth() / 2;
        double cy = card.getY() + card.getHeight() / 2;
        for (int i = cards.size() - 1; i >= 0; --i) {
            PageItem candidate = cards.get(i);

            // objects in the same group shouldn't snap to each other
            if (g != null && candidate.getGroup() == g) {
                continue;
            }

            if (card != candidate && snapToClass.contains(candidate.getSnapClass()) && candidate.getRectangle().intersects(r)) {
                double dx = cx - (candidate.getX() + candidate.getWidth() / 2);
                double dy = cy - (candidate.getY() + candidate.getHeight() / 2);
                double sqDist = (dx * dx) + (dy * dy);
                if (sqDist < shortestSqDist) {
                    shortestSqDist = sqDist;
                    overlapping = candidate;
                }
            }
        }
        return overlapping;
    }

    @Override
    public Page clone() {
        try {
            Page p = (Page) super.clone();
            p.cards = new ArrayList<>();
            for (int i = 0; i < cards.size(); ++i) {
                PageItem item = cards.get(i).clone();
                item.setPage(p);
                p.cards.add(item);
            }
            return p;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("clone");
        }
    }
    private ArrayList<PageItem> cards = new ArrayList<>();
    private transient PageView view;
    private Deck deck;

    public PageView getView() {
        return view;
    }

    public void setView(PageView view) {
        this.view = view;
        view.setPage(this);
    }

    protected double pageWidth() {
        return getDeck().getPaperProperties().getPageWidth();
    }

    protected double pageHeight() {
        return getDeck().getPaperProperties().getPageHeight();
    }

    protected double gridSeparation() {
        return getDeck().getPaperProperties().getGridSeparation();
    }

    protected double margin() {
        return getDeck().getPaperProperties().getMargin();
    }

    public Deck getDeck() {
        return deck;
    }

    void setDeck(Deck deck) {
        this.deck = deck;
    }

    public void setTitle(String name) {
        title = name;
    }

    public String getTitle() {
        return title;
    }

    private String title;

    private static final int CURRENT_VERSION = 4;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(CURRENT_VERSION);

        out.writeObject(deck);
        out.writeObject(title);
        out.writeInt(cards.size());
        for (int i = 0; i < cards.size(); ++i) {
            out.writeObject(cards.get(i));
        }
    }

    @SuppressWarnings("deprecation")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();

        deck = (Deck) in.readObject();
        if (version >= 4) {
            title = (String) in.readObject();
        }
        if (version == 1) {
            @SuppressWarnings("unchecked")
            ArrayList<Card> oldCards = (ArrayList<Card>) in.readObject();
            cards = new ArrayList<>();
            for (Card oldCard : oldCards) {
                cards.add(oldCard.createCompatiblePageItem());
            }
        } else {
            BusyDialog busy = BusyDialog.getCurrentDialog();

            int size = in.readInt();
            cards = new ArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                Object item = in.readObject();
                if (item instanceof Card) {
                    item = ((Card) item).createCompatiblePageItem();
                }
                cards.add((PageItem) item);

                if (busy != null) {
                    busy.setProgressCurrent(busy.getProgressCurrent() + 1);
                }
            }
        }
    }
}
