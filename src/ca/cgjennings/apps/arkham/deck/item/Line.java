package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import ca.cgjennings.ui.theme.ThemeInstaller;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.ImageIcon;
import static resources.Language.string;

/**
 * An page item representing a straight line segment.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class Line extends AbstractItem implements SizablePageItem, LineStyle, ShadowStyle {

    private double dx = DEFAULT_DX, dy = DEFAULT_DY;
    private String name;
    private Color lineColour = DEFAULT_LINE_COLOR;
    private int lineCap = DEFAULT_LINE_CAP;
    private int lineJoin = DEFAULT_LINE_JOIN;
    private DashPattern lineDash = DEFAULT_DASH_PATTERN;
    private float lineWidth = DEFAULT_LINE_WIDTH;
    private boolean shadow = false;

    public static final float DEFAULT_LINE_WIDTH = 2f;
    public static final Color DEFAULT_LINE_COLOR = Color.BLACK;
    public static final int DEFAULT_LINE_CAP = BasicStroke.CAP_BUTT;
    public static final int DEFAULT_LINE_JOIN = BasicStroke.JOIN_MITER;
    public static final DashPattern DEFAULT_DASH_PATTERN = DashPattern.SOLID;

    private static final Color SHADOW_EDGE_COLOR = new Color(0, true);
    private static final Color SHADOW_MIDDLE_COLOR = Color.BLACK;

    private static final double DEFAULT_DX = 54d;
    private static final double DEFAULT_DY = 54d;

    public Line() {
        name = string("de-move-line-name");
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
            g.setColor(new Color(0x80_8080));
            g.drawRect(2, 2, ICON_SIZE - 4, ICON_SIZE - 4);
            g.setColor(new Color(0xc0_c0c0));
            g.drawRect(3, 3, ICON_SIZE - 6, ICON_SIZE - 6);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            g.setColor(new Color(0x33_3333));
            g.drawLine(8, 8, ICON_SIZE - 8, ICON_SIZE - 8);
            g.setStroke(new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            g.setColor(new Color(0x79_B7EC));
            g.drawLine(8, 8, ICON_SIZE - 8, ICON_SIZE - 8);
            g.dispose();
            icon = ThemeInstaller.getInstalledTheme().applyThemeToImage(icon);
            cachedIcon = new ImageIcon(icon);
        }
        return cachedIcon;
    }
    protected transient ImageIcon cachedIcon = null;

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Color getLineColor() {
        return lineColour;
    }

    @Override
    public void setLineColor(Color lineColour) {
        if (this.lineColour != lineColour) {
            this.lineColour = lineColour;
            itemChanged();
        }
    }

    @Override
    public LineCap getLineCap() {
        return LineCap.fromInt(lineCap);
    }

    @Override
    public void setLineCap(LineCap lineCap) {
        int lc = lineCap.toInt();
        if (this.lineCap != lc) {
            this.lineCap = lc;
            itemChanged();
        }
    }

    @Override
    public LineJoin getLineJoin() {
        return LineJoin.fromInt(lineJoin);
    }

    @Override
    public void setLineJoin(LineJoin join) {
        int bj = join.toInt();
        if (this.lineJoin != bj) {
            this.lineJoin = bj;
            itemChanged();
        }
    }

    @Override
    public void setLineWidth(float lineWidth) {
        if (this.lineWidth != lineWidth) {
            this.lineWidth = lineWidth;
            itemChanged();
        }
    }

    @Override
    public DashPattern getLineDashPattern() {
        return lineDash;
    }

    @Override
    public void setLineDashPattern(DashPattern pat) {
        if (lineDash != pat) {
            lineDash = pat;
            itemChanged();
        }
    }

    @Override
    public boolean isShadowed() {
        return shadow;
    }

    @Override
    public void setShadowed(boolean enable) {
        if (shadow != enable) {
            shadow = enable;
            itemChanged();
        }
    }

    public double getX2() {
        return getX() + dx;
    }

    public double getY2() {
        return getY() + dy;
    }

    @Override
    public Rectangle2D.Double getRectangle() {
        Rectangle2D r = getOutline().getBounds2D();
        if (r instanceof Rectangle2D.Double) {
            return (Rectangle2D.Double) r;
        }
        return new Rectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public void setSize(double width, double height) {
        if (dx < 0) {
            dx = -width;
        } else {
            dx = width;
        }
        if (dy < 0) {
            dy = -height;
        } else {
            dy = height;
        }
        itemChanged();
    }

    @Override
    public double getWidth() {
        if (dx >= 0d) {
            return dx;
        } else {
            return -dx;
        }
    }

    @Override
    public double getHeight() {
        if (dy >= 0d) {
            return dy;
        } else {
            return -dy;
        }
    }

    public Point2D.Double getStartPoint() {
        return new Point2D.Double(getX(), getY());
    }

    public Point2D.Double getEndPoint() {
        return new Point2D.Double(getX() + dx, getY() + dy);
    }

    public void setStartPoint(Point2D p) {
        setLocation(p);
    }

    public void setEndPoint(Point2D p) {
        dx = p.getX() - getX();
        dy = p.getY() - getY();
    }

    @Override
    public float getLineWidth() {
        return lineWidth;
    }

    @Override
    public DragHandle[] getDragHandles() {
        if (dragHandles == null) {
            dragHandles = new DragHandle[]{
                new P1Handle(this), new P2Handle(this)
            };
        }
        return dragHandles;
    }

    @Override
    public Shape getOutline() {
        double x = getX(), y = getY();
        Line2D.Double line = new Line2D.Double(x, y, x + dx, y + dy);
        // making the cap square instead of butt will include the handles in the shape
        BasicStroke stroke = new BasicStroke(lineWidth, lineCap, lineJoin); //BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL );
        return stroke.createStrokedShape(line);
    }

    @Override
    public void paint(Graphics2D g, RenderTarget target, double renderResolutionHint) {
        double x = getX(), y = getY();
        Line2D.Double line = new Line2D.Double(x, y, x + dx, y + dy);

        if (target == RenderTarget.FAST_PREVIEW) {
            paintFast(g, line, target, renderResolutionHint);
        } else {
            Stroke oldStroke = g.getStroke();
            Paint oldPaint = g.getPaint();

            if (lineWidth > 0) {
                if (isShadowed()) {
                    dsc.paintDropShadow(g, target, renderResolutionHint);
                }
                applyStrokeAndPaint(g);
                g.draw(line);
            }

            g.setStroke(oldStroke);
            g.setPaint(oldPaint);
        }
    }

    public void paintFast(Graphics2D g, Line2D.Double line, RenderTarget target, double renderResolutionHint) {
        Paint oldPaint = g.getPaint();
        Stroke oldStroke = g.getStroke();

        if (lineWidth > 0f) {
            if (shadow) {
                float shadowWidth = lineWidth * 2f;
                Line2D.Double gradientLine = getPerpendicular(line, shadowWidth);
                BasicStroke shadowStroke = new BasicStroke(shadowWidth, lineCap, BasicStroke.JOIN_BEVEL);
                Shape shadowShape = shadowStroke.createStrokedShape(line);
                g.setPaint(
                        new LinearGradientPaint(
                                gradientLine.getP1(), gradientLine.getP2(),
                                new float[]{0f, 0.5f, 1},
                                new Color[]{SHADOW_EDGE_COLOR, SHADOW_MIDDLE_COLOR, SHADOW_EDGE_COLOR},
                                CycleMethod.REFLECT
                        )
                );
                g.fill(shadowShape);
            }

            applyStrokeAndPaint(g);
            g.draw(line);
        }

        g.setStroke(oldStroke);
        g.setPaint(oldPaint);
    }

    protected Stroke applyStrokeAndPaint(Graphics2D g) {
        g.setPaint(lineColour);
        Stroke stroke = RenderingAttributeFactory.createLineStroke(this);
        g.setStroke(stroke);
        return stroke;
    }

    void paintShadowCurve(Graphics2D g, Paint p) {
        applyStrokeAndPaint(g);
        if (p != null) {
            g.setPaint(p);
        }
        double x = getX(), y = getY();
        Line2D.Double line = new Line2D.Double(x, y, x + dx, y + dy);
        g.draw(line);
    }

    private Line2D.Double getPerpendicular(Line2D.Double source, double length) {
        // convert line to a vector (0,0) - (dx,dy)
        double vx = source.x2 - source.x1;
        double vy = source.y2 - source.y1;

        // find the unit vector
        double norm = Math.sqrt(vx * vx + vy * vy);
        vx /= norm;
        vy /= norm;

        // the two normals of (vx,vy) are (-vy,vx) and (vy,-vx)
        //   move each normal distance length/2 away from (0,0)
        //   to create a line length units long centered at (0,0)
        double halfLength = length / 2d;
        Line2D.Double dest = new Line2D.Double(
                -vy * halfLength, vx * halfLength,
                vy * halfLength, -vx * halfLength
        );

        // translate the center of the line back to (x1,y1)
        dest.x1 += source.x1;
        dest.y1 += source.y1;
        dest.x2 += source.x1;
        dest.y2 += source.y1;
        return dest;
    }

    @Override
    public PageItem clone() {
        Line li = (Line) super.clone();
        li.dragHandles = null;
        // must use its own drop shadow cache!
        li.dsc = new DropShadowLineCache(li);
        return li;
    }

    protected transient DropShadowLineCache dsc;

    @Override
    public boolean hasExteriorHandles() {
        // Note: this return value depends heavily on the handle-drawing code
        // in P1Handle and P2Handle
        return lineCap == BasicStroke.CAP_BUTT || lineWidth < 6f;
    }

    private static final int LINE_VERSION = 3;

    @Override
    protected void writeImpl(ObjectOutputStream out) throws IOException {
        super.writeImpl(out);

        out.writeInt(LINE_VERSION);

        out.writeObject(getName());
        out.writeDouble(dx);
        out.writeDouble(dy);
        out.writeObject(lineColour);
        out.writeInt(lineCap);
        out.writeInt(lineJoin);
        out.writeFloat(lineWidth);
        out.writeObject(lineDash);
        out.writeBoolean(shadow);
    }

    @Override
    protected void readImpl(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readImpl(in);

        int version = in.readInt();

        setName((String) in.readObject());
        dx = in.readDouble();
        dy = in.readDouble();
        setLineColor((Color) in.readObject());
        setLineCap(LineCap.fromInt(in.readInt()));
        if (version >= 3) {
            setLineJoin(LineJoin.fromInt(in.readInt()));
        } else {
            switch (getLineCap()) {
                case BUTT:
                    lineJoin = BasicStroke.JOIN_BEVEL;
                    break;
                case ROUND:
                    lineJoin = BasicStroke.JOIN_ROUND;
                    break;
                case SQUARE:
                    lineJoin = BasicStroke.JOIN_MITER;
                    break;
                default:
                    throw new AssertionError();
            }
        }
        setLineWidth(in.readFloat());
        if (version >= 2) {
            lineDash = (DashPattern) in.readObject();
        } else {
            lineDash = DashPattern.SOLID;
        }
        setShadowed(in.readBoolean());

        dragHandles = null;
        dsc = new DropShadowLineCache(this);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeImpl(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readImpl(in);
    }
    private static final long serialVersionUID = -2_897_559_979_679_067_492L;
}
