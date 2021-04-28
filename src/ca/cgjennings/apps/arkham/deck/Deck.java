package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.component.ComponentMetadata;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.deck.item.CardFace;
import ca.cgjennings.apps.arkham.deck.item.Curve;
import ca.cgjennings.apps.arkham.deck.item.DependentPageItem;
import ca.cgjennings.apps.arkham.deck.item.FlippablePageItem;
import ca.cgjennings.apps.arkham.deck.item.Group;
import ca.cgjennings.apps.arkham.deck.item.Line;
import ca.cgjennings.apps.arkham.deck.item.PageItem;
import ca.cgjennings.apps.arkham.deck.item.SimpleGroup;
import ca.cgjennings.apps.arkham.deck.item.StyleApplicator;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import gamedata.Game;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import resources.CoreComponents;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * A deck of game component sheets, organized into pages for printing.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class Deck implements Serializable, GameComponent, Cloneable {

    static final long serialVersionUID = 4_560_886_736_606_557_242L;

    /**
     *     */
    public Deck() {
        paper = PaperSets.getDefaultPaper(null);
        printerPaper = paper;
        splitPaper = false;
        splitBorder = 8;
        splitBorderColour = Color.BLACK;

        pages = new ArrayList<>();
        selection = new LinkedHashSet<>();
        unsaved = false;
        comment = "";

        installGameChangeListener();
    }

    /**
     * Returns a new deck editor for this deck. A {@linkplain #coreCheck()
     * core check} will be performed using the value of the <tt>cores</tt>
     * private setting, if any.
     *
     * @return a new editor for the deck
     */
    @Override
    public DeckEditor createDefaultEditor() {
        coreCheck();
        return new DeckEditor(this);
    }

    public void beginCompoundEdit() {
    }

    public void endCompoundEdit() {
    }

    /**
     * Return the number of pages in this deck.
     */
    public int getPageCount() {
        return pages.size();
    }

    /**
     * Adds a <code>PropertyChangeListener</code> to the deck. The listener is
     * registered for all properties. The same listener object may be added more
     * than once, and will be called as many times as it is added. If
     * <code>listener</code> is null, no exception is thrown and no action is
     * taken.
     *
     * @param pcl the listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        pc.addPropertyChangeListener(pcl);
    }

    /**
     * Removes a <code>PropertyChangeListener</code> from the deck. This removes
     * a listener that was registered for all properties. If the listener was
     * added more than once, it will be notified one less time after being
     * removed. If the listener is <code>null</code>, or was never added, no
     * exception is thrown and no action is taken.
     *
     * @param pcl the listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        pc.removePropertyChangeListener(pcl);
    }

    void firePropertyChange(String name, Object oldValue, Object newValue) {
        pc.firePropertyChange(name, oldValue, newValue);
    }

    void firePropertyChange(String name, boolean oldValue, boolean newValue) {
        pc.firePropertyChange(name, oldValue, newValue);
    }

    void firePropertyChange(String name, int oldValue, int newValue) {
        pc.firePropertyChange(name, oldValue, newValue);
    }

    private transient PropertyChangeSupport pc = new PropertyChangeSupport(this);

    /**
     * A property change that is fired with the page number when a page is
     * added.
     */
    public static final String PROPERTY_PAGE_ADDED = "newPage";
    /**
     * A property change that is fired with the page number when a page is
     * removed.
     */
    public static final String PROPERTY_PAGE_REMOVED = "delPage";
    /**
     * A property change that is fired with the old and new page numbers when
     * the page order changes.
     */
    public static final String PROPERTY_PAGE_REORDERED = "movePage";

    /**
     * Adds a new page to the end of this deck.
     */
    public final Page addNewPage() {
        return addNewPage(getPageCount());
    }

    /**
     * Adds a new page which will have the given page index. The newly created
     * page will be made the active page and returned.
     *
     * @param index the page index, from 0 to {@link #getPageCount()} inclusive
     * @return the newly added blank page
     */
    public Page addNewPage(final int index) {
        if (index < 0 || index > getPageCount()) {
            throw new IllegalArgumentException("index: " + index);
        }

        Page p = new Page(this);
        pages.add(index, p);
        setActivePage(p);

//		if( undoIsActive == 0 ) {
//			undoSupport.postEdit( new DeckUndoable(this) {
//				@Override
//				public void undoImpl() throws CannotUndoException {
//					deck.removePage( index );
//				}
//				@Override
//				public void redoImpl() throws CannotRedoException {
//					deck.addNewPage( index );
//				}
//			});
//		}
        markUnsavedChanges();
        firePropertyChange(PROPERTY_PAGE_ADDED, index, index);
        return p;
    }

    /**
     * Removes the page at the specified page index.
     *
     * @param index the index of the page to remove
     */
    public void removePage(int index) {
        // check if any of the selection groups use this page:
        // if so, make them GCable
        Page p = pages.get(index);
        for (int i = 0; i < selectionGroup.length; ++i) {
            if (selectionGroupPage[i] == p) {
                clearSelectionGroup(i);
            }
        }

        pages.remove(index);

        markUnsavedChanges();
        firePropertyChange(PROPERTY_PAGE_REMOVED, index, index);
    }

    /**
     * Returns the page with the specified index.
     *
     * @param index the index of the page to return
     * @return the requested page
     * @see #getPageCount()
     */
    public Page getPage(int index) {
        return pages.get(index);
    }

    /**
     * The maximum size for any dimension in a page format that is allowed in a
     * deck. It is equal to 7200 points (100 inches, 8<sup>1</sup>/<sub>3</sub>
     * feet, or 2.54 metres).
     */
    public static final double MAX_PAPER_SIZE = 100d * 72d;

    private static void checkPaperMetrics(double... values) {
        for (int i = 0; i < values.length; ++i) {
            if (values[i] < 0 || values[i] > MAX_PAPER_SIZE) {
                throw new IllegalArgumentException("invalid paper measure: " + values[i]);
            }
        }
    }

    /**
     * Returns the page that containing the currently selected page items, or
     * <code>null</code> if there is no selection.
     *
     * @return the page containing
     */
    public Page getSelectionPage() {
        return selectionPage;
    }

    /**
     * Unselects all currently selected page items, if any.
     */
    public void clearSelection() {
        clearSelectionImpl(true);
    }

    /**
     * Unselects all currently selected page items, if any.
     *
     * @param replaceReselectSet if <code>true</code>, the group used to store
     * the "reselection" items is replaced
     */
    private void clearSelectionImpl(boolean replaceReselectSet) {
        Page oldpage = getSelectionPage();

        if (replaceReselectSet) {
            storeSelectionInNumberedGroup(RESELECT_GROUP);
        }

        selectionPage = null;
        selection.clear();

        if (oldpage != null && oldpage.getView() != null) {
            oldpage.getView().pageChangeEvent();
        }
        fireSelectionChanged();
    }

    /**
     * Adds a page item to the current selection. If the new item is on a
     * different page than the existing selection, the current selection is
     * cleared first.
     *
     * @param item the item to add to the selection
     */
    public void addToSelection(PageItem item) {
        if (selectionPage != null && selectionPage != item.getPage()) {
            clearSelection();
        }
        selectionPage = item.getPage();
        selection.add(item);

        if (item.getGroup() != null) {
            item.getGroup().addToSelection(this);
        }

        if (getSelectionPage().getView() != null) {
            getSelectionPage().getView().pageChangeEvent();
        }
        fireSelectionChanged();
    }

    /**
     * Removes a page item from the current selection. Has no effect if the item
     * is not selected.
     *
     * @param item the item to deselect
     */
    public void removeFromSelection(PageItem item) {
        Page oldpage = selectionPage;
        if (!selection.remove(item)) {
            return;
        }

        if (selection.isEmpty()) {
            selectionPage = null;
        }
        if (item.getGroup() != null) {
            item.getGroup().removeFromSelection(this);
        }
        if (oldpage != null && oldpage.getView() != null) {
            oldpage.getView().pageChangeEvent();
        }
        fireSelectionChanged();
    }

    /**
     * Invert the set of selected page items on the page where the selection
     * occurs. If there is no selection, all cards on the active page are
     * selected.
     */
    public void invertSelection() {
        Page affectedPage;

        if (selection.isEmpty()) {
            affectedPage = getActivePage();
            selectAll(affectedPage);
        } else {
            affectedPage = getSelectionPage();
            clearSelection();
            for (PageItem pi : selectionGroupPage[RESELECT_GROUP].getCards()) {
                if (!selectionGroup[RESELECT_GROUP].contains(pi)) {
                    addToSelection(pi);
                }
            }
        }

        if (affectedPage.getView() != null) {
            affectedPage.getView().pageChangeEvent();
        }
        fireSelectionChanged();
    }

    @SuppressWarnings("serial")
    private class SelectionGroupEdit extends DeckUndoable {

        private final int g;
        private final Page oPage;
        private Page nPage;
        private final LinkedHashSet oSel;
        private LinkedHashSet nSel;

        public SelectionGroupEdit(int group, Page newPage, LinkedHashSet newSel) {
            super(Deck.this);
            oPage = selectionGroupPage[group];
            nPage = newPage;
            oSel = selectionGroup[group];
            nSel = newSel;
            g = group;
        }

        @Override
        protected void undoImpl() {
            selectionGroupPage[g] = oPage;
            selectionGroup[g] = oSel;
        }

        @Override
        protected void redoImpl() {
            selectionGroupPage[g] = nPage;
            selectionGroup[g] = nSel;
        }

        @Override
        public boolean isSignificant() {
            return false;
        }
    }

    /**
     * Stores the current selection in a numbered memory cell. There are
     * guaranteed to be at least 10 general purpose cells available to the user.
     *
     * @param group the cell number to store the selection in
     * @throws IndexOutOfBoundsException if the group number is negative or
     * equal to or greater than {@link #NUM_SELECTION_GROUPS}
     */
    public void storeSelectionInNumberedGroup(int group) {
        if (group < 0 || group >= NUM_SELECTION_GROUPS) {
            throw new IndexOutOfBoundsException("invalid group: " + group);
        }

        if (selectionPage == null || selection.size() == 0) {
            return;
        }

        LinkedHashSet<PageItem> nSel = new LinkedHashSet<>(selection);
//		if( undoIsActive == 0 ) {
//			undoSupport.postEdit( new SelectionGroupEdit( group, selectionPage, nSel ) );
//		}
        selectionGroupPage[group] = selectionPage;
        selectionGroup[group] = nSel;
    }

    /**
     * Deletes the numbered group (with undo support).
     *
     * @param group the group number
     */
    private void clearSelectionGroup(final int group) {
//		if( undoIsActive == 0 ) {
//			undoSupport.postEdit( new SelectionGroupEdit( group, null, null ) );
//		}
        selectionGroupPage[group] = null;
        selectionGroup[group] = null;
    }

    /**
     * Selects the specified numbered group. Numbered groups store selections in
     * numbered memory cells as an aid to the user.
     *
     * @param groupNumber the index of the group
     */
    public void selectNumberedGroup(int groupNumber) {
        if (groupNumber < 0 || groupNumber >= NUM_SELECTION_GROUPS) {
            throw new IndexOutOfBoundsException("invalid group: " + groupNumber);
        }

        // copy previous selection before clearing because it could be
        // the reselect group
        LinkedHashSet<PageItem> tempSel = selectionGroup[groupNumber];
        Page tempPage = selectionGroupPage[groupNumber];

        // whatever was selected is now the previous selection
        clearSelection();

        // if the group was set and the page is in this deck
        if (tempPage != null && tempSel != null && pages.contains(tempPage)) {
            // add back the previous selection, checking that each
            // item is still on the page
            for (PageItem pi : tempSel) {
                if (pi.getPage() == tempPage) {
                    addToSelection(pi);
                }
            }
        }

        if (tempPage != null && tempPage.getView() != null) {
            tempPage.getView().pageChangeEvent();
        }
        fireSelectionChanged();
    }

    /**
     * Returns the number of items in the selection stored in the specified
     * group.
     *
     * @param group the selection group number to measure
     * @return the number of items in the selection group, or 0
     */
    public int getSelectionGroupSize(int group) {
        if (group < 0 || group >= NUM_SELECTION_GROUPS) {
            throw new IndexOutOfBoundsException("invalid group: " + group);
        }

        LinkedHashSet<PageItem> g = selectionGroup[group];
        Page page = selectionGroupPage[group];
        // if the group was set and the page is in this deck
        if (page != null && g != null && pages.contains(page)) {
            int size = 0;
            for (PageItem pi : g) {
                if (pi.getPage() == page) {
                    ++size;
                }
            }
            return size;
        }
        return 0;
    }

    /**
     * Selects a single page item, clearing any existing selection.
     *
     * @param item the item to select
     */
    public void setSelection(PageItem item) {
        clearSelection();
        if (item != null) {
            addToSelection(item);
        }
        fireSelectionChanged();
    }

    /**
     * Returns the members of the current selection as a possibly empty array.
     *
     * @return the selected items
     */
    public PageItem[] getSelection() {
        return selection.toArray(new PageItem[selection.size()]);
    }

    /**
     * Returns the number of currently selected cards.
     *
     * @return the selection size, or 0 if there is no selection
     */
    public int getSelectionSize() {
        return selection.size();
    }

    /**
     * Returns <code>true</code> if the specified item is currently selected.
     *
     * @param item the item to test
     * @return <code>true</code> if the item is in the current selection
     */
    public boolean isSelected(PageItem item) {
        return selection.contains(item);
    }

    /**
     * Deletes the items in the current selection.
     */
    public void deleteSelection() {
        Page oldpage = selectionPage;
        if (oldpage == null) {
            return;
        }

        // we need a copy of the selection since removing
        // the card also removes it from the selection, which
        // would cause a ConcurrentModificationException if we
        // iterate over the selection directly
        PageItem[] itemsToDelete = getSelection();
        for (PageItem c : itemsToDelete) {
            c.getPage().removeCard(c);
        }
        clearSelectionImpl(false);

        if (oldpage.getView() != null) {
            oldpage.getView().pageChangeEvent();
        }

        markUnsavedChanges();
        fireSelectionChanged();
    }

    public void selectAll(Page page) {
        for (PageItem c : page.getCards()) {
            addToSelection(c);
        }
        fireSelectionChanged();
    }

    public void turnSelectionLeft() {
        if (getSelectionPage() == null) {
            return;
        }

        PageItem[] sel = getSelection();

        Rectangle2D.Double rect = getSelectionRectangle();

        // if there are curves, they may have control points outside of this
        // box, which won't do
        for (int i = 0; i < sel.length; ++i) {
            PageItem c = sel[i];
            if (c instanceof Curve) {
                rect.add(((Curve) c).getControlPoint());
            }
        }

        double rx = rect.getCenterX();
        double ry = rect.getCenterY();

        AffineTransform at = AffineTransform.getQuadrantRotateInstance(-1, rx, ry);

        for (int i = 0; i < sel.length; ++i) {
            PageItem c = sel[i];
            Point2D p = c.getLocation();
            if (c instanceof Line) {
                Line line = (Line) c;
                Point2D e1 = line.getStartPoint(), e2 = line.getEndPoint(), cp = null;
                if (line instanceof Curve) {
                    cp = ((Curve) line).getControlPoint();
                }
                line.setStartPoint(at.transform(e1, e1));
                line.setEndPoint(at.transform(e2, e2));
                if (cp != null) {
                    ((Curve) line).setControlPoint(at.transform(cp, cp));
                }
            } else {
                // rotate around the center of the object
                p.setLocation(p.getX() + c.getWidth() / 2d, p.getY() + c.getHeight() / 2d);
                if (c instanceof FlippablePageItem) {
                    ((FlippablePageItem) c).turnLeft();
                }
                at.transform(p, p);
                // adjust back from center to upper-left corner
                c.setLocation(p.getX() - c.getWidth() / 2d, p.getY() - c.getHeight() / 2d);
            }
        }
        centerSelectionOver(rx, ry);

        if (getSelectionPage().getView() != null) {
            getSelectionPage().getView().pageChangeEvent();
        }

        markUnsavedChanges();
        fireSelectionChanged();
    }

    public void turnSelectionRight() {
        if (getSelectionPage() == null) {
            return;
        }

        PageItem[] sel = getSelection();
        Rectangle2D.Double rect = getSelectionRectangle();

        // if there are curves, they may have control points outside of this
        // box, which won't do
        for (int i = 0; i < sel.length; ++i) {
            PageItem c = sel[i];
            if (c instanceof Curve) {
                rect.add(((Curve) c).getControlPoint());
            }
        }

        double rx = rect.getCenterX();
        double ry = rect.getCenterY();

        AffineTransform at = AffineTransform.getQuadrantRotateInstance(1, rx, ry);

        for (int i = 0; i < sel.length; ++i) {
            PageItem c = sel[i];
            Point2D p = c.getLocation();
            if (c instanceof Line) {
                Line line = (Line) c;
                Point2D e1 = line.getStartPoint(), e2 = line.getEndPoint(), cp = null;
                if (line instanceof Curve) {
                    cp = ((Curve) line).getControlPoint();
                }
                line.setStartPoint(at.transform(e1, e1));
                line.setEndPoint(at.transform(e2, e2));
                if (cp != null) {
                    ((Curve) line).setControlPoint(at.transform(cp, cp));
                }
            } else {
                // rotate around the center of the object
                p.setLocation(p.getX() + c.getWidth() / 2d, p.getY() + c.getHeight() / 2d);
                if (c instanceof FlippablePageItem) {
                    ((FlippablePageItem) c).turnRight();
                }
                at.transform(p, p);
                // adjust back from center to upper-left corner
                c.setLocation(p.getX() - c.getWidth() / 2d, p.getY() - c.getHeight() / 2d);
            }
        }
        centerSelectionOver(rx, ry);

        if (getSelectionPage().getView() != null) {
            getSelectionPage().getView().pageChangeEvent();
        }

        markUnsavedChanges();
        fireSelectionChanged();
    }

    public void flipSelection() {
        if (getSelectionPage() == null) {
            return;
        }

        Rectangle2D.Double rect = getSelectionRectangle();
        PageItem[] sel = getSelection();

        // if there are curves, they may have control points outside of this
        // box, which won't do
//		for( int i=0; i<sel.length; ++i ) {
//			PageItem c = sel[i];
//			if( c instanceof Curve ) {
//				rect.add( ((Curve) c).getControlPoint() );
//			}
//		}
        double rectX2 = rect.x + rect.width;
        double rx = rect.getCenterX();
        double ry = rect.getCenterY();

        for (int i = 0; i < sel.length; ++i) {
            PageItem c = sel[i];
            if (c instanceof Line) {
                Line line = (Line) c;
                Point2D e1 = line.getStartPoint(), e2 = line.getEndPoint(), cp = null;
                e1.setLocation(rectX2 - e1.getX(), e1.getY());
                e2.setLocation(rectX2 - e2.getX(), e2.getY());

                if (line instanceof Curve) {
                    cp = ((Curve) line).getControlPoint();
                    cp.setLocation(rectX2 - cp.getX(), cp.getY());
                }
                line.setStartPoint(e1);
                line.setEndPoint(e2);
                if (cp != null) {
                    ((Curve) line).setControlPoint(cp);
                }
            } else {
                c.setX(rectX2 - c.getX() - c.getWidth());
                if (c instanceof FlippablePageItem) {
                    ((FlippablePageItem) c).flip();
                }
            }
        }

        centerSelectionOver(rx, ry);

        if (getSelectionPage().getView() != null) {
            getSelectionPage().getView().pageChangeEvent();
        }

        markUnsavedChanges();
    }

    public void moveSelectionToFront() {
        Page page = getSelectionPage();
        if (page == null) {
            return;
        }

        for (PageItem c : getSelection()) {
            page.moveCardToFront(c);
        }

        if (page.getView() != null) {
            page.getView().pageChangeEvent();
        }

        markUnsavedChanges();
    }

    public void moveSelectionToBack() {
        Page page = getSelectionPage();
        if (page == null) {
            return;
        }

        for (PageItem c : getSelection()) {
            page.moveCardToBack(c);
        }

        if (page.getView() != null) {
            page.getView().pageChangeEvent();
        }
    }

    public void nudgeSelection(double dx, double dy) {
        Page page = getSelectionPage();
        if (page == null) {
            return;
        }

        for (PageItem c : getSelection()) {
            c.setX(c.getX() + dx);
            c.setY(c.getY() + dy);
        }

        if (page.getView() != null) {
            page.getView().pageChangeEvent();
        }

        markUnsavedChanges();
        fireSelectionChanged();
    }

    /**
     * Group the selection into a single unit.
     */
    public void groupSelection() {
        Page page = getSelectionPage();
        if (page == null) {
            return;
        }

        new SimpleGroup(getSelection());

        if (page.getView() != null) {
            page.getView().pageChangeEvent();
        }

        markUnsavedChanges();
        fireSelectionChanged();
    }

    /**
     * Break any groups in the selection.
     */
    public void ungroupSelection() {
        Page page = getSelectionPage();
        if (page == null) {
            return;
        }

        Set<Group> groups = new HashSet<>();
        for (PageItem i : getSelection()) {
            if (i.getGroup() != null) {
                groups.add(i.getGroup());
            }
        }
        if (groups.size() > 0) {
            for (Group g : groups) {
                g.clear();
            }

            if (page.getView() != null) {
                page.getView().pageChangeEvent();
            }

            markUnsavedChanges();
            fireSelectionChanged();
        }
    }

    public Rectangle2D.Double getSelectionRectangle() {
        PageItem[] sel = getSelection();
        if (sel.length == 0) {
            return null;
        }
        Rectangle2D.Double r = sel[0].getRectangle();
        for (int i = 1; i < sel.length; ++i) {
            r.add(sel[i].getRectangle());
        }
        return r;
    }

    public void reorderPage(int oldindex, int newindex) {
        if (oldindex == newindex) {
            return;
        }
        Page p = pages.get(oldindex);
        pages.remove(oldindex);
        pages.add(newindex, p);

        markUnsavedChanges();
        firePropertyChange(PROPERTY_PAGE_REORDERED, oldindex, newindex);
    }

    /**
     * The number of a selection group that is automatically populated with the
     * previous selection when the selection is cleared. This allows the
     * previous selection to be easily restored. This is the last group, and so
     * is equal to <code>NUM_SELECTION_GROUPS-1</code>.
     */
    public static final int RESELECT_GROUP = 10;
    /**
     * The number of selection groups available for use with
     * {@link #storeSelectionInNumberedGroup(int)}. This is guaranteed to be at
     * least 10.
     */
    public static final int NUM_SELECTION_GROUPS = 11;

    private LinkedHashSet[] selectionGroup = new LinkedHashSet[NUM_SELECTION_GROUPS];
    private Page[] selectionGroupPage = new Page[NUM_SELECTION_GROUPS];

    private String comment;
    private PaperProperties paper;
    private PaperProperties printerPaper;
    private boolean splitPaper;
    private float splitBorder;
    private Color splitBorderColour;
    private ArrayList<Page> pages;
    private LinkedHashSet<PageItem> selection;
    private Page selectionPage;
    private boolean cropMarksEnabled = true;
    private float cropMarkWidth = 2f;
    private float cropMarkPrintWidth = 0.25f;
    private double cropMarkLength = 14d;
    private double cropMarkDistance = 4d;
    private boolean autoBleed = false;

    @Override
    public Deck clone() {
        try {
            Deck d = (Deck) super.clone();
            d.pages = new ArrayList<>();
            for (int i = 0; i < pages.size(); ++i) {
                Page p = pages.get(i);
                Page pClone = p.clone();
                pClone.setDeck(d);
                d.pages.add(pClone);
                if (selectionPage == p) {
                    d.selectionPage = pClone;
                }
            }

            d.clearSelectionImpl(false);
            // should recreate selection and selection groups
            return d;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("clone");
        }
    }

    /**
     * Returns whether publisher's marks are displayed for the deck.
     *
     * @return if <code>true</code>, publisher's marks will be generated for
     * objects in the deck
     */
    public boolean getPublishersMarksEnabled() {
        return cropMarksEnabled;
    }

    /**
     * Sets whether publisher's marks are displayed for the deck.
     *
     * @param pubMarksEnabled if <code>true</code>, publisher's marks will be
     * generated for objects in the deck
     */
    public void setPublishersMarksEnabled(boolean pubMarksEnabled) {
        if (cropMarksEnabled == pubMarksEnabled) {
            return;
        }
        boolean old = cropMarksEnabled;
        cropMarksEnabled = pubMarksEnabled;
        markUnsavedChanges();
        firePropertyChange("publishersMarksEnabled", old, pubMarksEnabled);
        updateCropMarkManagers();
    }

    /**
     * Returns the width of publisher's marks when displayed for editing.
     *
     * @return the non-negative length, in points
     * @see #setPublishersMarkWidth(float)
     */
    public float getPublishersMarkWidth() {
        return cropMarkWidth;
    }

    /**
     * Sets the width of publisher's marks when displayed for editing.
     *
     * @param pubMarkWidth the non-negative length, in points
     * @see #getPublishersMarkWidth()
     */
    public void setPublishersMarkWidth(float pubMarkWidth) {
        if (pubMarkWidth < 0d) {
            throw new IllegalArgumentException("pubMarkWidth: " + pubMarkWidth);
        }
        if (cropMarkWidth == pubMarkWidth) {
            return;
        }
        double old = cropMarkWidth;
        cropMarkWidth = pubMarkWidth;
        markUnsavedChanges();
        firePropertyChange("publishersMarkWidth", old, pubMarkWidth);
        updateCropMarkManagers();
    }

    /**
     * Returns the width of publisher's marks when printed.
     *
     * @return the non-negative length, in points
     * @see #setPublishersMarkPrintWidth(float)
     */
    public float getPublishersMarkPrintWidth() {
        return cropMarkPrintWidth;
    }

    /**
     * Sets the width of publisher's marks when printed.
     *
     * @param pubMarkWidth the non-negative length, in points
     * @see #getPublishersMarkPrintWidth()
     */
    public void setPublishersMarkPrintWidth(float pubMarkWidth) {
        if (pubMarkWidth < 0d) {
            throw new IllegalArgumentException("pubMarkWidth: " + pubMarkWidth);
        }
        if (cropMarkPrintWidth == pubMarkWidth) {
            return;
        }
        double old = cropMarkPrintWidth;
        cropMarkPrintWidth = pubMarkWidth;
        markUnsavedChanges();
        firePropertyChange("publishersMarkPrintWidth", old, pubMarkWidth);
    }

    /**
     * Returns the length of publisher's marks.
     *
     * @return the non-negative length, in points
     */
    public double getPublishersMarkLength() {
        return cropMarkLength;
    }

    /**
     * Sets the length of publisher's marks when displayed for editing.
     *
     * @param pubMarkLength the non-negative length, in points
     */
    public void setPublishersMarkLength(double pubMarkLength) {
        if (pubMarkLength < 0d) {
            throw new IllegalArgumentException("pubMarkLength: " + pubMarkLength);
        }
        if (this.cropMarkLength == pubMarkLength) {
            return;
        }
        double old = this.cropMarkLength;
        this.cropMarkLength = pubMarkLength;
        firePropertyChange("publishersMarkLength", old, pubMarkLength);
        markUnsavedChanges();
        updateCropMarkManagers();
    }

    /**
     * Returns the size of the gap between deck objects and their publisher's
     * marks.
     *
     * @return the distance, in points
     */
    public double getPublishersMarkDistance() {
        return cropMarkDistance;
    }

    /**
     * Sets the size of the gap between deck objects and their publisher's
     * marks.
     *
     * @param pubMarkDistance the non-negative distance, in points
     */
    public void setPublishersMarkDistance(double pubMarkDistance) {
        if (pubMarkDistance < 0d) {
            throw new IllegalArgumentException("pubMarkDistance");
        }
        if (this.cropMarkDistance == pubMarkDistance) {
            return;
        }
        double old = this.cropMarkDistance;
        this.cropMarkDistance = pubMarkDistance;
        markUnsavedChanges();
        firePropertyChange("publishersMarkDistance", old, pubMarkDistance);
        updateCropMarkManagers();
    }

    /**
     * Updates the crop mark engine on each page view to reflect changes.
     */
    void updateCropMarkManagers() {
        for (int i = 0; i < getPageCount(); ++i) {
            PageView pv = getPage(i).getView();
            if (pv != null) {
                pv.updateCropMarkManager();
            }
        }
    }

    private static final int CURRENT_VERSION = 11;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(CURRENT_VERSION);

        out.writeObject(saveFileHint == null ? null : saveFileHint.getAbsolutePath());
        out.writeObject(paper);

        int totalObjects = 0;
        for (int i = 0; i < pages.size(); ++i) {
            totalObjects += pages.get(i).getCardCount();
        }
        out.writeInt(totalObjects);

        out.writeInt(pages.size());
        for (int i = 0; i < pages.size(); ++i) {
            out.writeObject(pages.get(i));
        }

        // Version 2
        out.writeObject(selection);
        out.writeObject(selectionPage);

        // Version 4
        out.writeObject(name);

        // Version 5
        out.writeBoolean(splitPaper);
        out.writeObject(printerPaper);
        out.writeFloat(splitBorder);
        // Version 10
        out.writeObject(splitBorderColour);

        // Version 6
        out.writeObject(comment);

        // Version 7
        out.writeObject(privateSettings);

        // Version 8
        out.writeObject(selectionGroupPage);
        out.writeObject(selectionGroup);

        // Version 9
        out.writeBoolean(cropMarksEnabled);
        out.writeFloat(cropMarkWidth);
        out.writeFloat(cropMarkPrintWidth);
        out.writeDouble(cropMarkLength);
        out.writeDouble(cropMarkDistance);

        // Version 11
        out.writeBoolean(autoBleed);

        markSaved();
    }

    private File saveFileHint;

    /**
     * Sets a hint value that is saved with the deck in order to assist with
     * locating linked files (such as game components used in the deck).
     *
     * @param f the file that the deck will be saved to
     */
    public void setSaveFileHint(File f) {
        if (f == null) {
            saveFileHint = null;
        } else {
            saveFileHint = f.getParentFile();
        }
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        if (!EventQueue.isDispatchThread()) {
            StrangeEons.log.warning("reading deck from outside of event dispatch thread");
        }

        final int version = in.readInt();

        // init transients
        pc = new PropertyChangeSupport(this);

        String path = (String) in.readObject();
        DeckDeserializationSupport helper = DeckDeserializationSupport.getShared();
        helper.beginDeserialization(path);
        saveFileHint = null;

        try {
            paper = (PaperProperties) in.readObject();

            final ObjectInputStream ois = in;
            final Throwable[] loadException = new Throwable[1];
//			new BusyDialog( StrangeEons.getWindow(), string( "busy-fetching" ), new Runnable() {
//				@Override
//				public void run() {
            try {
                if (version >= 3) {
                    int totalObjects = ois.readInt();
//							BusyDialog.getCurrentDialog().setProgressMaximum( totalObjects );
                    int pageCount = ois.readInt();
                    pages = new ArrayList<>(Math.max(8, pageCount + 4));
                    for (int i = 0; i < pageCount; ++i) {
//								BusyDialog.getCurrentDialog().setStatusText( string( "de-load-obj", i + 1, pageCount ) );
                        pages.add((Page) ois.readObject());
                    }
                } else {
                    pages = (ArrayList<Page>) ois.readObject();
                }
            } catch (OutOfMemoryError | Exception e) {
                loadException[0] = e;
            }
//				}
//			});
            if (loadException[0] != null) {
                Throwable t = loadException[0];
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                } else if (t instanceof IOException) {
                    throw (IOException) t;
                } else {
                    throw new IOException(t.getLocalizedMessage(), t);
                }
            }

            if (version >= 2) {
                selection = (LinkedHashSet<PageItem>) in.readObject();
                selectionPage = (Page) in.readObject();
            } else {
                selection = new LinkedHashSet<>();
            }

            if (version >= 4) {
                name = (String) in.readObject();
            } else {
                name = string("de-l-name");
            }

            if (version >= 5) {
                splitPaper = in.readBoolean();
                printerPaper = (PaperProperties) in.readObject();
                splitBorder = in.readFloat();
            } else {
                splitPaper = false;
                printerPaper = PaperSets.getDefaultPaper(null);
                splitBorder = 8;
            }
            if (version >= 10) {
                splitBorderColour = (Color) in.readObject();
            } else {
                splitBorderColour = Color.BLACK;
            }

            if (version >= 6) {
                comment = (String) in.readObject();
            } else {
                comment = "";
            }

            if (version >= 7) {
                privateSettings = (Settings) in.readObject();
            } else {
                privateSettings = new Settings();
            }

            // For backwards compatibilty with old expansion boards,
            // if no game is set, set it to AH so that the AH paper sizes
            // and tiles will be loaded.
            if (privateSettings.get(Game.GAME_SETTING_KEY) == null) {
                if (Game.get("AH") != null) {
                    privateSettings.set(Game.GAME_SETTING_KEY, "AH");
                    StrangeEons.log.warning("Setting Game for old deck to Arkham Horror for compatibility");
                } else {
                    privateSettings.set(Game.GAME_SETTING_KEY, Game.ALL_GAMES_CODE);
                    StrangeEons.log.warning("Setting Game for old deck to All Games (Arkham Horror not installed)");
                }
            }

            if (version >= 8) {
                selectionGroupPage = (Page[]) in.readObject();
                selectionGroup = (LinkedHashSet[]) in.readObject();
            } else {
                selectionGroupPage = new Page[NUM_SELECTION_GROUPS];
                selectionGroup = new LinkedHashSet[NUM_SELECTION_GROUPS];
            }

            if (version >= 9) {
                cropMarksEnabled = in.readBoolean();
                cropMarkWidth = in.readFloat();
                cropMarkPrintWidth = in.readFloat();
                cropMarkLength = in.readDouble();
                cropMarkDistance = in.readDouble();
            } else {
                cropMarksEnabled = true;
                cropMarkWidth = 2f;
                cropMarkPrintWidth = 0.25f;
                cropMarkLength = 14d;
                cropMarkDistance = 4d;
            }

            if (version >= 11) {
                autoBleed = in.readBoolean();
            } else {
                autoBleed = false;
            }

            // replace any paths that were relocated with their new paths
            for (int p = 0; p < pages.size(); ++p) {
                for (PageItem i : pages.get(p).getCards()) {
                    if (i instanceof DependentPageItem) {
                        DependentPageItem dpi = (DependentPageItem) i;
                        String currentPath = dpi.getPath();
                        String replacementPath = helper.getReplacementPath(currentPath);
                        if (replacementPath != null) {
                            dpi.setPath(replacementPath);
                            StrangeEons.log.log(Level.INFO, "replaced page item path \"{0}\" with \"{1}\"", new Object[]{currentPath, replacementPath});
                        }
                    }
                }
            }

            installGameChangeListener();
        } catch (RuntimeException rt) {
            StrangeEons.log.log(Level.SEVERE, "Uncaught exception while reading deck", rt);
            throw rt;
        } finally {
            // end deserialization helper session and mark the file as unsaved if
            // some card files had to be replaced to open the file
            if (helper.endDeserialization()) {
                markUnsavedChanges();
            } else {
                markSaved();
            }
        }
    }

    // GAME COMPONENT OVERRIDES /////////////////////////////////////
    private String name;

    public void setName(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if ((this.name == null) || !this.name.equals(name)) {
            String old = this.name;
            this.name = name;
            markUnsavedChanges();
            firePropertyChange("name", old, name);
        }
    }

    @Override
    public String getName() {
        if (name == null) {
            name = string("de-l-name");
        }
        return name;
    }

    @Override
    public String getFullName() {
        return getName();
    }

    @Override
    public boolean hasChanged() {
        return false;
    }

    @Override
    public boolean hasUnsavedChanges() {
        return unsaved;
    }

    @Override
    public void clearAll() {
        clearSelectionImpl(false);
        pages.clear();
        for (int i = 0; i < NUM_SELECTION_GROUPS; ++i) {
            selectionGroupPage[i] = null;
            selectionGroup[i] = null;
        }
    }

    /**
     * Returns an empty array, since decks do not have rendered sheets.
     */
    @Override
    public String[] getSheetTitles() {
        return new String[0];
    }

    /**
     * This is not a valid operation for a {@code Deck}. Throws
     * {@code UnsupportedOperationException}.
     */
    @Override
    public Sheet[] getSheets() {
        throw new UnsupportedOperationException();
    }

    /**
     * This is not a valid operation for a {@code Deck}. Throws
     * {@code UnsupportedOperationException}.
     */
    @Override
    public void setSheets(Sheet[] sheets) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is not a valid operation for a {@code Deck}. Throws
     * {@code UnsupportedOperationException}.
     */
    @Override
    public Sheet[] createDefaultSheets() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void markSaved() {
        if (unsaved) {
            unsaved = false;
            firePropertyChange("unsavedChanges", true, false);
        }
    }

    @Override
    public void markUnsavedChanges() {
        if (!unsaved) {
            unsaved = true;
            firePropertyChange("unsavedChanges", false, true);
        }
    }

    @Override
    public final void markChanged(int i) {
        markUnsavedChanges();
    }

    /**
     * Changes the active page. The active page is the default target for
     * commands that affect a particular page. When the deck is shown in a deck
     * editor, the currently visible page and the active page are typically
     * synchronized.
     *
     * @param pageToActivate the non-<code>null</code> page to activate
     * @see #getActivePage()
     */
    public void setActivePage(Page pageToActivate) {
        if (pageToActivate == null) {
            throw new NullPointerException("pageToActivate");
        }
        if (activePage != pageToActivate) {
            if (!pages.contains(pageToActivate)) {
                throw new IllegalArgumentException("page to activate not in page list");
            }
            Page old = activePage;
            activePage = pageToActivate;
            firePropertyChange("activePage", old, pageToActivate);
        }
    }

    /**
     * Returns the active page.
     *
     * @return the currently active page
     * @see #setActivePage
     */
    public Page getActivePage() {
        if (pages.contains(activePage)) {
            return activePage;
        }
        return null;
    }

    private Page activePage;

    /**
     * Cuts the current selection, if any. The selection is removed from the
     * deck and placed on the clipboard.
     *
     * @see #copy
     * @see #paste
     */
    public void cut() {
        copy();
        deleteSelection();
    }

    /**
     * Copies the current selection to the clipboard.
     *
     * @see #cut
     * @see #paste
     */
    public void copy() {
        PageItem[] copyItems = getSelection();
        if (copyItems.length == 0) {
            return;
        }

        clippings = new PageItem[copyItems.length];
        for (int i = 0; i < copyItems.length; ++i) {
            // TODO: when ready to end the shame, make a cloning method that can inherit (there are two other instance to fix)
            clippings[i] = copyItems[i].clone();
        }
    }

    /**
     * Pastes a previously copied selection from the clipboard.
     *
     * @see #cut
     * @see #copy
     */
    public void paste() {
        Page active = getActivePage();
        if (active == null) {
            return;
        }

        if (clippings != null && clippings.length > 0) {
            clearSelection();
            boolean pastingCardsToTheirOwnPage = clippings[0].getPage() == active;

            for (PageItem card : clippings) {
                PageItem copy = card.clone();
                active.addCard(copy, false);
                addToSelection(copy);
            }

            // if there is a view, center the pasted items over the pointer
            // (if it is over the view) or over the center of the view
            PageView v = active.getView();
            if (pastingCardsToTheirOwnPage && v != null) {
                // determine centering location in view coords
                Point pointer = v.getMousePosition();
                int sx, sy;
                if (pointer != null) {
                    sx = pointer.x;
                    sy = pointer.y;
                } else {
                    sx = v.getWidth() / 2;
                    sy = v.getHeight() / 2;
                }

                Point2D cp = v.viewToDocument(sx, sy);
                centerSelectionOver(cp.getX(), cp.getY());
            }

            // if the cards came from another page, we copy the new clones
            // over the old selection so that if the user pastes a 2nd copy
            // on this page, they will be centered as usual
            if (!pastingCardsToTheirOwnPage) {
                copy();
            }
        };
    }

    /**
     * Moves the selection so that it is centered over the point (x, y) (in
     * document space).
     *
     * @param x the x-coordinate to center over
     * @param y the y-coordinate to center over
     */
    public void centerSelectionOver(double x, double y) {
        if (selection.isEmpty()) {
            return;
        }

        Rectangle2D.Double rect = getSelectionRectangle();
        double dx = x - rect.getCenterX();
        double dy = y - rect.getCenterY();
        for (PageItem pi : selection) {
            pi.setLocation(pi.getX() + dx, pi.getY() + dy);
        }
        PageView v = selectionPage.getView();
        if (v != null) {
            v.repaint();
        }
    }

    /**
     * Returns whether the clipboard used for deck objects is empty. If empty,
     * {@linkplain #paste() pasting} will have no effect. The deck object
     * clipboard is shared by all decks in the current instance of the
     * application.
     *
     * @return <code>true</code> if the clipboard deck is empty
     */
    public static boolean isDeckClipboardEmpty() {
        return clippings == null || clippings.length == 0;
    }
    private static PageItem[] clippings;

    private boolean unsaved;

    /**
     * Sets the paper format for pages in this deck. If page splitting is
     * disabled, then this will also be the physical paper size for printing.
     * When creating large objects, such as game boards, this will typically be
     * set to a format matching the size of the physical object.
     *
     * @param paper the new paper format for deck pages
     * @throws NullPointerException if the paper format is <code>null</code>
     * @throws IllegalArgumentException if any paper measurement is larger than
     * {@link #MAX_PAPER_SIZE}
     * @see #getPaperProperties()
     */
    public void setPaperProperties(PaperProperties paper) {
        if (paper == null) {
            throw new NullPointerException("paper");
        }
        checkPaperMetrics(
                paper.getPageWidth(), paper.getPageHeight(),
                paper.getGridSeparation(), paper.getMargin()
        );

        if (!this.paper.equals(paper)) {
            PaperProperties old = this.paper;
            this.paper = paper;
            for (Page p : pages) {
                PageView v = p.getView();
                if (v != null) {
                    v.repaint();
                }
            }
            markUnsavedChanges();
            firePropertyChange("paperProperties", old, paper);
        }
    }

    /**
     * Returns the paper format for pages in this deck.
     *
     * @return the properties of the paper format for this deck
     * @see #setPaperProperties
     */
    public PaperProperties getPaperProperties() {
        return paper;
    }

    /**
     * Sets the printed page format for the deck. If page splitting is enabled,
     * then pages in the deck will be split into tiles sized for paper with this
     * physical size. This value will be ignored unless page splitting is
     * enabled.
     *
     * @param printerPaper the new paper format for printed pages
     * @throws NullPointerException if the paper format is <code>null</code>
     * @throws IllegalArgumentException if any paper measurement is larger than
     * {@link #MAX_PAPER_SIZE}
     * @see #getPrinterPaperProperties()
     */
    public void setPrinterPaperProperties(PaperProperties printerPaper) {
        if (paper == null) {
            throw new NullPointerException("paper");
        }
        checkPaperMetrics(
                paper.getPageWidth(), paper.getPageHeight(),
                paper.getGridSeparation(), paper.getMargin()
        );

        if (!paper.equals(printerPaper)) {
            PaperProperties old = this.printerPaper;
            this.printerPaper = printerPaper;
            markUnsavedChanges();
            firePropertyChange("printerPaperProperties", old, printerPaper);
        }
    }

    /**
     * Returns the paper properties of the physical paper size for pages in this
     * deck. Note that this value will be ignored unless page splitting is
     * enabled.
     *
     * @return the physical paper format for printing
     * @see #setPrinterPaperProperties
     */
    public PaperProperties getPrinterPaperProperties() {
        return printerPaper;
    }

    /**
     * Returns a set of paper sizes that are appropriate for this deck at the
     * current time. This will include all paper sizes that are registered for
     * either all games or for the particular game associated with this deck in
     * its settings (if any).
     *
     * @return a set of appropriate paper sizes for this deck
     * @see #setPaperProperties(ca.cgjennings.apps.arkham.deck.PaperProperties)
     */
    public Set<PaperProperties> getPaperSizes() {
        HashMap<Object, Object> attr = new HashMap<>();
        attr.put(PaperSets.KEY_MAXIMUM_SIZE, MAX_PAPER_SIZE);
        String code = getSettings().get(Game.GAME_SETTING_KEY, Game.ALL_GAMES_CODE);
        // make sure game is either a valid game instance or all games
        // then if the deck is for an uninstalled game we won't end up with
        // a game of null, which would return papers for ALL games
        Game game = Game.get(code);
        if (game == null) {
            game = Game.getAllGamesInstance();
        }
        attr.put(PaperSets.KEY_GAME, game);
        return PaperSets.getMatchingPapers(attr);
    }

    /**
     * Returns a set of printer paper sizes that are appropriate for this deck
     * at the current time. This set will include all <i>physical</i> paper
     * sizes that are registered for either all games or for the particular game
     * associated with this deck in its settings (if any).
     *
     * @return a set of appropriate physical paper sizes for this deck
     * @see
     * #setPrinterPaperProperties(ca.cgjennings.apps.arkham.deck.PaperProperties)
     */
    public Set<PaperProperties> getPrinterPaperSizes() {
        HashMap<Object, Object> attr = new HashMap<>();
        attr.put(PaperSets.KEY_CONCRETENESS, PaperSets.VALUE_CONCRETENESS_PHYSICAL);
        attr.put(PaperSets.KEY_MAXIMUM_SIZE, MAX_PAPER_SIZE);
        String code = getSettings().get(Game.GAME_SETTING_KEY, Game.ALL_GAMES_CODE);
        // make sure game is either a valid game instance or all games
        // then if the deck is for an uninstalled game we won't end up with
        // a game of null, which would return papers for ALL games
        Game game = Game.get(code);
        if (game == null) {
            game = Game.getAllGamesInstance();
        }
        attr.put(PaperSets.KEY_GAME, game);
        return PaperSets.getMatchingPapers(attr);
    }

    /**
     * Returns whether paper splitting is enabled.
     *
     * @return <code>true</code> if tiling is enabled
     * @see #setPaperSplitting
     */
    public boolean isPaperSplitting() {
        return splitPaper;
    }

    /**
     * Sets whether paper splitting is enabled. When enabled, each deck page is
     * split into as many tiles as necessary to print it on one physical printer
     * page.
     *
     * @param splitPaper whether the deck pages should be split into tiles for
     * the printer paper size
     * @see #isPaperSplitting()
     * @see #setPaperProperties
     * @see #setPrinterPaperProperties
     */
    public void setPaperSplitting(boolean splitPaper) {
        if (this.splitPaper != splitPaper) {
            this.splitPaper = splitPaper;
            markUnsavedChanges();
            firePropertyChange("paperSplitting", !splitPaper, splitPaper);
        }
    }

    /**
     * Returns the size of the border that will be drawn around virtual pages
     * when page splitting is enabled.
     *
     * @return the border size, in points
     * @see #setSplitBorder
     */
    public float getSplitBorder() {
        return splitBorder;
    }

    /**
     * Sets the size of the border that will be drawn round virtual pages, or 0
     * for no border.
     *
     * @param splitBorder the border size, or 0
     * @see #getSplitBorder()
     * @see #setPaperSplitting(boolean)
     */
    public void setSplitBorder(float splitBorder) {
        if (splitBorder < 0f) {
            throw new IllegalArgumentException("splitBorder < 0");
        }
        if (this.splitBorder != splitBorder) {
            float old = this.splitBorder;
            this.splitBorder = splitBorder;
            firePropertyChange("splitBorder", old, splitBorder);
            markUnsavedChanges();
        }
    }

    /**
     * Returns the colour of the border that is drawn around virtual pages.
     *
     * @return the split border colour
     * @see #setSplitBorderColor
     */
    public Color getSplitBorderColor() {
        return splitBorderColour;
    }

    /**
     * Sets the colour of the border that is drawn around virtual pages.
     *
     * @param borderColor the split border colour; <code>null</code> is treated
     * as black
     * @see #getSplitBorderColor
     * @see #setPaperSplitting
     */
    public void setSplitBorderColor(Color borderColor) {
        if (borderColor == null) {
            borderColor = Color.BLACK;
        }
        if (splitBorderColour.getRGB() != borderColor.getRGB()) {
            Color old = splitBorderColour;
            splitBorderColour = borderColor;
            markUnsavedChanges();
            firePropertyChange("splitBorderColor", old, splitBorderColour);
        }
    }

    /**
     * Sets whether synthetic bleed margins are enabled for components that do
     * not have a built-in bleed margin. Changing this value updates the
     * {@linkplain CardFace#setAutoBleedMarginEnabled(boolean) associated property}
     * in each card face in the deck.
     *
     * @param enable the value to apply to the synthetic bleed margin property
     * of card faces
     * @see #isAutoBleedMarginEnabled()
     */
    public void setAutoBleedMarginEnabled(boolean enable) {
        if (enable != autoBleed) {
            autoBleed = enable;
            for (int i = 0; i < getPageCount(); ++i) {
                Page p = getPage(i);
                for (int j = 0; j < p.getCardCount(); ++j) {
                    PageItem pi = p.getCard(j);
                    if (pi instanceof CardFace) {
                        ((CardFace) pi).setAutoBleedMarginEnabled(enable);
                    }
                }
            }
            markUnsavedChanges();
            firePropertyChange("autoBleedMarginEnabled", !enable, enable);
        }
    }

    /**
     * Returns <code>true</code> if synthetic bleed margins are the default for
     * this deck.
     *
     * @return <code>true</code> if card faces should use synthetic bleed
     * margins by default
     * @see #setAutoBleedMarginEnabled(boolean)
     */
    public boolean isAutoBleedMarginEnabled() {
        return autoBleed;
    }

    public static interface SelectionChangeListener {

        public void deckSelectionChanged(Deck source);
    }

    private transient HashSet<WeakReference<SelectionChangeListener>> selectionListeners;

    public void addSelectionListener(SelectionChangeListener l) {
        if (selectionListeners == null) {
            selectionListeners = new HashSet<>();
        }
        selectionListeners.add(new WeakReference<>(l));
    }

    public void removeSelectionListener(SelectionChangeListener l) {
        if (selectionListeners != null) {
            selectionListeners.remove(l);
        }
    }

    void fireSelectionChanged() {
        if (selectionListeners == null) {
            return;
        }
        for (WeakReference<SelectionChangeListener> wr : selectionListeners) {
            SelectionChangeListener l = wr.get();
            if (l != null) {
                l.deckSelectionChanged(this);
            }
        }
    }

    @Override
    public String getComment() {
        return comment;
    }

    /**
     * Sets the design rationale comment text associated with this deck.
     *
     * @param comment the new comment to set; <code>null</code> is treated as an
     * empty string
     */
    public void setComment(String comment) {
        if (comment == null) {
            comment = "";
        }
        if (!this.comment.equals(comment)) {
            String old = this.comment;
            this.comment = comment;
            markUnsavedChanges();
            firePropertyChange("comment", old, comment);
        }
    }

    /**
     * Returns the default item style applicator that will be used to apply a
     * style to newly added deck items. The default style applicator for a deck
     * is determined by the game
     * {@linkplain Game#setDefaultDeckStyleApplicator associated with the deck}.
     *
     * @return an applicator used to set the default style for new objects
     */
    public StyleApplicator getDefaultStyleApplicator() {
        if (defaultStyleCapture == null) {
            Game game = Game.get(getSettings().get(Game.GAME_SETTING_KEY, Game.ALL_GAMES_CODE));
            if (game == null) {
                game = Game.getAllGamesInstance();
            }
            defaultStyleCapture = game.getDefaultDeckStyleApplicator();
        }
        return defaultStyleCapture;
    }

    private transient StyleApplicator defaultStyleCapture;

    private void installGameChangeListener() {
        getSettings().addPropertyChangeListener((PropertyChangeEvent evt) -> {
            if (Game.GAME_SETTING_KEY.equals(evt.getPropertyName())) {
                defaultStyleCapture = null;
            }
        });
    }

//	public void addUndoableEditListener( UndoableEditListener li ) {
//		undoSupport.addUndoableEditListener( li );
//	}
//
//	public void removeUndoableEditListener( UndoableEditListener li ) {
//		undoSupport.removeUndoableEditListener( li );
//	}
//
//	/**
//	 * Manages the posting of undoable events from the deck.
//	 */
//	UndoableEditSupport undoSupport = new UndoableEditSupport();
//	/**
//	 * Set to true when an edit is being undone/redone so that additional
//	 * undo events are not generated.
//	 */
//	int undoIsActive;
    @Override
    public Settings getSettings() {
        return privateSettings;
    }

    private Settings privateSettings = new Settings();

    @Override
    public boolean isDeckLayoutSupported() {
        return false;
    }

    /**
     * Returns <code>true</code> if a given file represents a game component
     * that can be placed in a deck. For recent file format versions, this can
     * be determined without fully opening the file by
     * {@linkplain ComponentMetadata#isDeckLayoutSupported() examining its metadata}.
     * For older files, the component must first be read in. If for any reason
     * the value cannot be read from the file, <code>false</code> is returned.
     *
     * @return returns <code>true</code> if the game component in the specified
     * file can be placed in a deck
     * @see GameComponent#isDeckLayoutSupported()
     */
    public static boolean isDeckLayoutSupported(File f) {
        // try to get the answer quickly from the metadata header
        ComponentMetadata cm = new ComponentMetadata(f);
        if (cm.getMetadataVersion() >= 1) {
            return cm.isDeckLayoutSupported();
        }

        // go the hard way for old files
        GameComponent gc = ResourceKit.getGameComponentFromFile(f, false);
        if (gc != null) {
            return gc.isDeckLayoutSupported();
        }

        // couldn't even read the file, so, no, can't put it in a deck
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For decks, the <tt>cores</tt> private setting is checked; if it it
     * exists, its value is passed to {@link CoreComponents#validateCoreComponents
     * validateCoreComponents}.
     *
     * @see CoreComponents#validateCoreComponents(java.lang.Object)
     */
    @Override
    public void coreCheck() {
        if (privateSettings != null) {
            String cores = privateSettings.get("cores");
            if (cores != null) {
                CoreComponents.validateCoreComponents(cores);
            }
        }
    }
}
