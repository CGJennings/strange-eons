package ca.cgjennings.apps.arkham.deck.item;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Adjusts the primary point (location) of a line.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
class P1Handle extends DragHandle {

    Line2D.Double line;

    public P1Handle(Line owner) {
        super(owner);
        line = new Line2D.Double();
    }

    @Override
    public void beginDrag(Point2D startPoint, MouseEvent e) {
        super.beginDrag(startPoint, e);
        Line owner = (Line) getOwner();
        Point2D p2 = owner.getEndPoint();

        line.x1 = startPoint.getX();
        line.y1 = startPoint.getY();
        line.x2 = p2.getX();
        line.y2 = p2.getY();

        constrain(e);
    }

    protected final void constrain(MouseEvent e) {
        if (e.isShiftDown() || e.isAltDown()) {
            // determine nearest octant for line by coverting to angle then
            // converting angle to nearest 8ths of a circle
            double theta = Math.atan2(line.x1 - line.x2, line.y1 - line.y2);
            int octant = (int) Math.round(theta / Math.PI * 4d) + 4;
            if (octant > 7) {
                octant = 0;
            }

            double ux = line.x2 + UNIT_VECTORS_X[octant];
            double uy = line.y2 + UNIT_VECTORS_Y[octant];
            double dx = UNIT_VECTORS_X[octant];
            double dy = UNIT_VECTORS_Y[octant];

            double t = ((line.x1 - ux) * dx + (line.y1 - uy) * dy) / (dx * dx + dy * dy);

            line.x1 = line.x2 + t * dx;
            line.y1 = line.y2 + t * dy;
        }
    }

    private static final double[] UNIT_VECTORS_X = new double[]{
        0, 1, 1, 1, 0, -1, -1, -1
    };

    private static final double[] UNIT_VECTORS_Y = new double[]{
        1, 1, 0, -1, -1, -1, 0, 1
    };

    @Override
    public void cancelDrag() {
        super.cancelDrag();
    }

    @Override
    public void drag(Point2D point, MouseEvent e) {
        super.drag(point, e);
        line.x1 = point.getX();
        line.y1 = point.getY();

        constrain(e);
    }

    @Override
    public void endDrag() {
        super.endDrag();
        Line owner = (Line) getOwner();
        owner.setLocation(line.getP1());
        owner.setEndPoint(line.getP2());
    }

    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    }

    @Override
    public void paintHandle(Graphics2D g) {
        Line owner = (Line) getOwner();
        Point2D.Double p = owner.getStartPoint();
        paintHandle(g, p, Math.max(2d, owner.getLineWidth()));
    }

    protected void paintHandle(Graphics2D g, Point2D.Double p, double dist) {
        dist /= 3d;
        if (dist < 2) {
            dist = 2;
        }
        Line2D.Double line = new Line2D.Double(p.x, p.y - dist, p.x, p.y + dist);
        g.draw(line);
        line.setLine(p.x - dist, p.y, p.x + dist, p.y);
        g.draw(line);
    }

    @Override
    public void paintDragState(Graphics2D g) {
        g.draw(line);
        paintDragEndpoint(g, line.x1, line.y1);
        paintDragEndpoint(g, line.x2, line.y2);
    }

    protected void paintDragEndpoint(Graphics2D g, double x1, double y1) {
        dot.setFrameFromCenter(x1, y1, x1 + HANDLE_RADIUS, y1 + HANDLE_RADIUS);
        g.fill(dot);
    }
    protected Ellipse2D dot = new Ellipse2D.Double();

    @Override
    public boolean hitTest(Point2D p) {
        return hitRectFromPoint(((Line) getOwner()).getStartPoint()).contains(p);
    }

    protected Rectangle2D.Double hitRectFromPoint(Point2D p) {

        /////////////
        // WARNING //
        //         ///////////////////////////////////////////////////
        //                                                          //
        // If this code is modified, then Line.hasExteriorHandles() //
        // must be updated to match or else drag handles may not be //
        // detected as the pointer moves.                           //
        //                                                          //
        //////////////////////////////////////////////////////////////
        Line owner = (Line) getOwner();
        final double width = Math.max(6d, owner.getLineWidth());
        final double halfWidth = width / 2d;
        return new Rectangle2D.Double(
                p.getX() - halfWidth, p.getY() - halfWidth,
                width, width
        );
    }
    protected static final double HANDLE_RADIUS = 3d;
}
