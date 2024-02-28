package ca.cgjennings.apps.arkham.deck.item;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * A draggable handle for resizing items.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
class ResizeHandle extends DragHandle {

    protected int corner;
    protected Rectangle2D.Double newRect;
    protected Point2D.Double oppositeCorner;
    public static final int CORNER_NW = 0;
    public static final int CORNER_NE = 1;
    public static final int CORNER_SW = 2;
    public static final int CORNER_SE = 3;
    protected double aspectRatio;
    protected boolean alwaysConstrain;

    public ResizeHandle(PageItem owner, int corner) {
        this(owner, corner, false);
    }

    public ResizeHandle(PageItem owner, int corner, boolean alwaysConstrain) {
        super(owner);
        if (corner < 0 || corner > 3) {
            throw new IllegalArgumentException("invalid corner: " + corner);
        }
        this.corner = corner;
        this.alwaysConstrain = alwaysConstrain;
    }

    protected Path2D createHandlePath() {
        Rectangle2D.Double r = getRectangle();
        double x1 = r.x, y1 = r.y;
        double x2 = r.x + r.width;
        double y2 = r.y + r.height;
        Path2D p = new Path2D.Double();

        if (corner == CORNER_NW || corner == CORNER_SW) {
            x1 += HANDLE_MARGIN;
        } else {
            x2 -= HANDLE_MARGIN;
        }

        if (corner == CORNER_NE || corner == CORNER_NW) {
            y1 += HANDLE_MARGIN;
        } else {
            y2 -= HANDLE_MARGIN;
        }

        switch (corner) {
            case CORNER_NW:
                p.moveTo(x1, y2);
                p.lineTo(x1, y1);
                p.lineTo(x2, y1);
                break;
            case CORNER_NE:
                p.moveTo(x1, y1);
                p.lineTo(x2, y1);
                p.lineTo(x2, y2);
                break;
            case CORNER_SW:
                p.moveTo(x1, y1);
                p.lineTo(x1, y2);
                p.lineTo(x2, y2);
                break;
            case CORNER_SE:
                p.moveTo(x1, y2);
                p.lineTo(x2, y2);
                p.lineTo(x2, y1);
                break;
        }
        return p;
    }

    @Override
    public void paintHandle(Graphics2D g) {
        Path2D p = createHandlePath();
        g.draw(p);
    }

    @Override
    public void paintDragState(Graphics2D g) {
        g.draw(newRect);
    }

    public Rectangle2D.Double getRectangle() {
        double width = HANDLE_SIZE + HANDLE_MARGIN;
        Rectangle2D.Double or = getOwner().getRectangle();
        Rectangle2D.Double r = new Rectangle2D.Double(or.x, or.y, width, width);

        if (corner == CORNER_NE || corner == CORNER_SE) {
            r.x = or.x + or.width - width;
        }
        if (corner == CORNER_SE || corner == CORNER_SW) {
            r.y = or.y + or.height - width;
        }
        return r;
    }

    @Override
    public boolean hitTest(Point2D p) {
        return getRectangle().contains(p);
    }

    @Override
    public Cursor getCursor() {
        int id;
        switch (corner) {
            case CORNER_NW:
                id = Cursor.NW_RESIZE_CURSOR;
                break;
            case CORNER_NE:
                id = Cursor.NE_RESIZE_CURSOR;
                break;
            case CORNER_SW:
                id = Cursor.SW_RESIZE_CURSOR;
                break;
            case CORNER_SE:
                id = Cursor.SE_RESIZE_CURSOR;
                break;
            default:
                throw new AssertionError();
        }
        return Cursor.getPredefinedCursor(id);
    }

    @Override
    public void beginDrag(Point2D startPoint, MouseEvent e) {
        super.beginDrag(startPoint, e);
        newRect = getOwner().getRectangle();
        aspectRatio = newRect.width / newRect.height;
        oppositeCorner = new Point2D.Double(newRect.x, newRect.y);
        if (corner == CORNER_NW || corner == CORNER_SW) {
            oppositeCorner.x += newRect.width;
        }
        if (corner == CORNER_NE || corner == CORNER_NW) {
            oppositeCorner.y += newRect.height;
        }
    }

    @Override
    public void cancelDrag() {
        super.cancelDrag();
    }

    @Override
    public void drag(Point2D point, MouseEvent e) {
        super.drag(point, e);
        newRect.setFrame(oppositeCorner.x, oppositeCorner.y, 0d, 0d);
        newRect.add(point);

        if (alwaysConstrain || e.isShiftDown()) {
            double rW = newRect.width;
            double rH = newRect.height;
            double sW = rH * aspectRatio;
            double sH = rW / aspectRatio;
            double dx, dy;
            if (sW * rH > sH * rW) {
                dx = rW;
                dy = sH;
            } else {
                dx = sW;
                dy = rH;
            }
            if (point.getX() < oppositeCorner.x) {
                dx = -dx;
            }
            if (point.getY() < oppositeCorner.y) {
                dy = -dy;
            }
            newRect.setFrame(oppositeCorner.x, oppositeCorner.y, 0d, 0d);
            newRect.add(oppositeCorner.x + dx, oppositeCorner.y + dy);
        }
    }

    @Override
    public void endDrag() {
        super.endDrag();
        SizablePageItem item = (SizablePageItem) getOwner();
        item.setLocation(newRect.x, newRect.y);
        item.setSize(newRect.width, newRect.height);
    }
    protected static final double HANDLE_SIZE = 8d;
    protected static final double HANDLE_MARGIN = 4d;
}
