package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.deck.PageView;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * A handle that can be dragged to rotate a rotatable tile.
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
class RotationHandle extends DragHandle {

    protected double angleAtDragStart;

    public RotationHandle(RotatableTile owner) {
        super(owner);
        handleSize = Math.min(owner.getWidth(), owner.getHeight()) / 4d;
        if (handleSize < DEFAULT_HANDLE_SIZE) {
            if (handleSize < 1d) {
                handleSize = 1d;
            }
        } else {
            handleSize = DEFAULT_HANDLE_SIZE;
        }
        handleSizeSq = (handleSize + handleSize / 3d) * (handleSize + handleSize / 3d);
    }

    @Override
    public void beginDrag(Point2D startPoint, MouseEvent e) {
        super.beginDrag(startPoint, e);
        angleAtDragStart = ((RotatableTile) getOwner()).getRotation();
    }

    @Override
    public void cancelDrag() {
        super.cancelDrag();
        ((RotatableTile) getOwner()).setRotation(angleAtDragStart);
    }

    @Override
    public void drag(Point2D point, MouseEvent e) {
        super.drag(point, e);
        RotatableTile owner = (RotatableTile) getOwner();
        Rectangle2D r = owner.getRectangle();
        double theta = Math.atan2(point.getX() - r.getCenterX(), r.getCenterY() - point.getY());

        // constrain to 45 degrees
        if (e.isShiftDown() || e.isAltDown()) {
            theta = Math.round(theta / Math.PI * 4d) * Math.PI / 4d;
        }

        owner.setRotation(theta);
    }

    private Stroke replaceStroke(Graphics2D g) {
        Stroke oldStroke = g.getStroke();
        if (handleSize < DEFAULT_HANDLE_SIZE && (oldStroke instanceof BasicStroke)) {
            BasicStroke s = (BasicStroke) oldStroke;
            g.setStroke(new BasicStroke(
                    s.getLineWidth() * (float) (handleSize / DEFAULT_HANDLE_SIZE),
                    s.getEndCap(), s.getLineJoin(), s.getMiterLimit(), s.getDashArray(), s.getDashPhase()));
        }
        return oldStroke;
    }

    @Override
    public void paintHandle(Graphics2D g) {
        Stroke oldStroke = replaceStroke(g);
        RotatableTile owner = (RotatableTile) getOwner();
        double angle = owner.getRotation();
        Rectangle2D.Double r = owner.getRectangle();
        double cx = r.getCenterX(), cy = r.getCenterY();

        Ellipse2D.Double c = new Ellipse2D.Double();
        c.setFrameFromCenter(cx, cy, cx + handleSize, cy + handleSize);
        g.draw(c);

        if (isDragging()) {
            return;
        }

        Color handleCol = g.getColor();
        g.setColor(getArmColor(handleCol));

        double radius = handleSize / 2d;
        c.setFrameFromCenter(cx, cy, cx + radius, cy + radius);
        g.fill(c);

        double armLength = handleSize * 5d / 4d;
        angle -= Math.PI / 2d;
        g.draw(new Line2D.Double(cx, cy, cx + Math.cos(angle) * armLength, cy + Math.sin(angle) * armLength));

        g.setColor(handleCol);
        g.setStroke(oldStroke);
    }

    protected static Color getArmColor(Color src) {
        return PageView.ACTIVE_HANDLE;
    }

    @Override
    public void paintDragState(Graphics2D g) {
        Stroke oldStroke = replaceStroke(g);

        double px = last.getX(), py = last.getY();
        RotatableTile owner = (RotatableTile) getOwner();
        Rectangle2D.Double r = owner.getRectangle();
        double cx = r.getCenterX(), cy = r.getCenterY();
        g.draw(new Line2D.Double(cx, cy, px, py));

        double radius = handleSize / 2d;
        Ellipse2D dot = new Ellipse2D.Double();
        dot.setFrameFromCenter(cx, cy, cx + radius, cy + radius);
        g.fill(dot);

        dot.setFrameFromCenter(px, py, px + radius, py + radius);
        g.fill(dot);

        g.setStroke(oldStroke);
    }

    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    }

    @Override
    public boolean hitTest(Point2D p) {
        Rectangle2D.Double r = getOwner().getRectangle();
        return p.distanceSq(r.getCenterX(), r.getCenterY()) <= handleSizeSq;
    }
    private static final double DEFAULT_HANDLE_SIZE = 6d;
    private double handleSize;
    // this is used for hit detection; we add a small margin to make it easier to grab
    private final double handleSizeSq;
}
