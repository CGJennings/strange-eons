package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import ca.cgjennings.ui.theme.ThemeInstaller;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.ImageIcon;
import static resources.Language.string;

/**
 * An page item representing a quadratic parametric curve segment.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class Curve extends Line {

    private double cx = 288d, cy = 72d;

    public Curve() {
        setName(string("de-move-curve-name"));
        cx = getX() + 36d;
        cy = getY2() / 2;
        setEndPoint(new Point2D.Double(getX(), getY2()));
        dsc = new DropShadowLineCache(this);
    }

    @Override
    public ImageIcon getThumbnailIcon() {
        if (cachedIcon == null) {
            BufferedImage icon = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = icon.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, ICON_SIZE, ICON_SIZE);
            g.setColor(new Color(0x808080));
            g.drawRect(2, 2, ICON_SIZE - 4, ICON_SIZE - 4);
            g.setColor(new Color(0xc0c0c0));
            g.drawRect(3, 3, ICON_SIZE - 6, ICON_SIZE - 6);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.setStroke(new BasicStroke(6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            g.setColor(new Color(0x333333));
            QuadCurve2D c = new QuadCurve2D.Float(
                    8, ICON_SIZE - 12,
                    ICON_SIZE - 8, ICON_SIZE - 8,
                    ICON_SIZE - 12, 8
            );
            g.draw(c);
            g.setStroke(new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            g.setColor(new Color(0x79B7EC));
            g.draw(c);
            g.dispose();
            icon = ThemeInstaller.getInstalledTheme().applyThemeToImage(icon);
            cachedIcon = new ImageIcon(icon);
        }
        return cachedIcon;
    }

    public Point2D.Double getControlPoint() {
        return new Point2D.Double(getX() + cx, getY() + cy);
    }

    public void setControlPoint(Point2D cp) {
        cx = cp.getX() - getX();
        cy = cp.getY() - getY();
    }

    public double getCX() {
        return getX() + cx;
    }

    public double getCY() {
        return getY() + cy;
    }

    @Override
    public void paint(Graphics2D g, RenderTarget target, double renderResolutionHint) {
        Stroke oldStroke = g.getStroke();
        Paint oldPaint = g.getPaint();

        updateCurve();
        if (isShadowed()) {
            dsc.paintDropShadow(g, target, renderResolutionHint);
        }
        applyStrokeAndPaint(g);
        g.draw(curve);

        g.setStroke(oldStroke);
        g.setPaint(oldPaint);
    }

    @Override
    void paintShadowCurve(Graphics2D g, Paint p) {
        applyStrokeAndPaint(g);
        if (p != null) {
            g.setPaint(p);
        }
        g.draw(curve);
    }

    @Override
    public Shape getOutline() {
        updateCurve();
        // maing the cap square instead of butt will include the handles in the shape
        BasicStroke stroke = new BasicStroke(getLineWidth(), BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL);
        return stroke.createStrokedShape(curve);
    }

    private void updateCurve() {
        double x = getX(), y = getY();
        curve.setCurve(x, y, x + cx, y + cy, getX2(), getY2());
    }

    @Override
    public DragHandle[] getDragHandles() {
        if (dragHandles == null) {
            dragHandles = new DragHandle[]{
                new CurveP1Handle(this), new CurveP2Handle(this),
                new C1Handle(this)
            };
        }
        return dragHandles;
    }

    private transient QuadCurve2D.Double curve = new QuadCurve2D.Double();

    static class CurveP1Handle extends P1Handle {

        Point2D cp;

        @Override
        public void beginDrag(Point2D startPoint, MouseEvent e) {
            super.beginDrag(startPoint, e);
            cp = ((Curve) getOwner()).getControlPoint();
        }

        @Override
        public void endDrag() {
            super.endDrag();
            ((Curve) getOwner()).setControlPoint(cp);
        }

        public CurveP1Handle(Line owner) {
            super(owner);
        }

        @Override
        public void paintDragState(Graphics2D g) {
            curve.setCurve(line.getP1(), cp, line.getP2());
            g.draw(curve);

            paintDragEndpoint(g, line.x1, line.y1);
            paintDragEndpoint(g, line.x2, line.y2);
        }

        QuadCurve2D.Double curve = new QuadCurve2D.Double();
    }

    static class CurveP2Handle extends P2Handle {

        Point2D cp;

        @Override
        public void beginDrag(Point2D startPoint, MouseEvent e) {
            super.beginDrag(startPoint, e);
            cp = ((Curve) getOwner()).getControlPoint();
        }

        @Override
        public void endDrag() {
            super.endDrag();
            ((Curve) getOwner()).setControlPoint(cp);
        }

        public CurveP2Handle(Line owner) {
            super(owner);
        }

        @Override
        public void paintDragState(Graphics2D g) {
            curve.setCurve(line.getP1(), cp, line.getP2());
            g.draw(curve);
            paintDragEndpoint(g, line.x1, line.y1);
            paintDragEndpoint(g, line.x2, line.y2);
        }

        QuadCurve2D.Double curve = new QuadCurve2D.Double();
    }

    @Override
    public boolean hasExteriorHandles() {
        return true;
    }

    private static final int CURVE_VERSION = 1;

    @Override
    protected void writeImpl(ObjectOutputStream out) throws IOException {
        super.writeImpl(out);

        out.writeInt(CURVE_VERSION);

        out.writeDouble(cx);
        out.writeDouble(cy);
    }

    @Override
    protected void readImpl(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readImpl(in);

        /* final int version = */ in.readInt();

        cx = in.readDouble();
        cy = in.readDouble();

        dsc = new DropShadowLineCache(this);
        curve = new QuadCurve2D.Double();

        dragHandles = null;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeImpl(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readImpl(in);
    }

    private static final long serialVersionUID = 8903653963560359856L;
}
