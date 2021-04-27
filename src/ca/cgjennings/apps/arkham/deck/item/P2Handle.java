package ca.cgjennings.apps.arkham.deck.item;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

/**
 * Adjusts the master point (location) of a line.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
class P2Handle extends P1Handle {

    public P2Handle(Line owner) {
        super(owner);
    }

    @Override
    public void beginDrag(Point2D startPoint, MouseEvent e) {
        super.beginDrag(startPoint, e);
        Line owner = (Line) getOwner();
        Point2D p2 = owner.getStartPoint();

        line.x1 = startPoint.getX();
        line.y1 = startPoint.getY();
        line.x2 = p2.getX();
        line.y2 = p2.getY();
    }

    @Override
    public void paintHandle(Graphics2D g) {
        Line owner = (Line) getOwner();
        Point2D.Double p = owner.getEndPoint();
        paintHandle(g, p, owner.getLineWidth());
    }

    @Override
    public boolean hitTest(Point2D p) {
        return hitRectFromPoint(((Line) getOwner()).getEndPoint()).contains(p);
    }
}
