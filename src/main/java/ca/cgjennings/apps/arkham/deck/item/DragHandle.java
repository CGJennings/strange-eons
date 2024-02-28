package ca.cgjennings.apps.arkham.deck.item;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

/**
 * A handle that can be attached to a component and that the user can drag to
 * modify the item in some way.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public abstract class DragHandle {

    private PageItem owner;
    private boolean isDragging;
    private boolean hasMoved;

    protected Point2D start;
    protected Point2D last;
    protected double dx, dy;

    public DragHandle(PageItem owner) {
        this.owner = owner;
    }

    public abstract void paintHandle(Graphics2D g);

    public abstract void paintDragState(Graphics2D g);

    /**
     * Returns {@code true} if a point is over this handle. When this method
     * returns {@code true}, dragging the mouse would begin a drag operation.
     *
     * @param p the point to test
     * @return {@code true} if the point falls within the handle
     */
    public abstract boolean hitTest(Point2D p);

    /**
     * Returns the cursor to use when the pointer is over this handle.
     * <p>
     * {@code DragHandle} returns the default cursor.
     *
     * @return the cursor to use for this handle
     */
    public Cursor getCursor() {
        return Cursor.getDefaultCursor();
    }

    public void beginDrag(Point2D startPoint, MouseEvent e) {
        start = startPoint;
        last = startPoint;
        isDragging = true;
        hasMoved = false;
    }

    public void drag(Point2D point, MouseEvent e) {
        dx = point.getX() - start.getX();
        dy = point.getY() - start.getY();
        last = point;
        hasMoved = true;
    }

    public boolean handleMovedDuringDrag() {
        return hasMoved;
    }

    public void endDrag() {
        isDragging = false;
    }

    public void cancelDrag() {
        isDragging = false;
    }

    public PageItem getOwner() {
        return owner;
    }

    public boolean isDragging() {
        return isDragging;
    }
}
