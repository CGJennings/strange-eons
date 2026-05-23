package ca.cgjennings.apps.arkham.deck.item;

import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;

/**
 * Adjusts the primary point (location) of a line.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
class C1Handle extends DragHandle {

    Point2D p;

    public C1Handle(Curve owner) {
        super(owner);
    }

    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    }

    @Override
    public void paintHandle(Graphics2D g) {
        Curve owner = (Curve) getOwner();
        Point2D.Double cp = owner.getControlPoint();
        paintDragEndpoint(g, cp.x, cp.y);
    }

    @Override
    public void paintDragState(Graphics2D g) {
        Curve o = (Curve) getOwner();
        Point2D.Double p1 = o.getStartPoint();
        Point2D.Double p2 = o.getEndPoint();

        Stroke oldStroke = g.getStroke();
        g.setStroke(controlStroke);
        controlLine.x1 = p1.x;
        controlLine.y1 = p1.y;
        controlLine.x2 = p.getX();
        controlLine.y2 = p.getY();
        g.draw(controlLine);
        controlLine.x1 = p2.x;
        controlLine.y1 = p2.y;
        g.draw(controlLine);
        g.setStroke(oldStroke);

        curve.setCurve(o.getStartPoint(), p, o.getEndPoint());
        g.draw(curve);
        paintDragEndpoint(g, p.getX(), p.getY());
    }

    @Override
    public boolean hitTest(Point2D p) {
        final double rsq = (HANDLE_RADIUS + 1) * (HANDLE_RADIUS + 1);
        return ((Curve) getOwner()).getControlPoint().distanceSq(p) <= rsq;
    }

    @Override
    public void beginDrag(Point2D startPoint, MouseEvent e) {
        super.beginDrag(startPoint, e);
        p = startPoint;
    }

    @Override
    public void drag(Point2D point, MouseEvent e) {
        super.drag(point, e);
        p = point;
    }

    @Override
    public void endDrag() {
        super.endDrag();
        Curve owner = (Curve) getOwner();
        owner.setControlPoint(p);
    }

    protected void paintHandle(Graphics2D g, Point2D.Double p, double dist) {
        dist /= 2d;
        if (dist < 2) {
            dist = 2;
        }
        Line2D.Double line = new Line2D.Double(p.x, p.y - dist, p.x, p.y + dist);
        g.draw(line);
        line.setLine(p.x - dist, p.y, p.x + dist, p.y);
        g.draw(line);
    }

    protected void paintDragEndpoint(Graphics2D g, double x1, double y1) {
        Ellipse2D dot = new Ellipse2D.Double();
        dot.setFrameFromCenter(x1, y1, x1 + HANDLE_RADIUS, y1 + HANDLE_RADIUS);
        g.fill(dot);
    }

    protected Rectangle2D.Double hitRectFromPoint(Point2D p) {
        double width = HANDLE_RADIUS;
        if (width < 2d) {
            width = 2d;
        }
        return new Rectangle2D.Double(
                p.getX() - width / 2d, p.getY() - width / 2d, width, width);
    }
    private static final double HANDLE_RADIUS = 3d;

    private final QuadCurve2D.Double curve = new QuadCurve2D.Double();
    Line2D.Double controlLine = new Line2D.Double();
    private static final BasicStroke controlStroke = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[]{0.5f, 0.5f}, 0);
}
