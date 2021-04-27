package ca.cgjennings.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;

/**
 * A border that creates an arc along two sides of the component.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class ArcBorder extends AbstractBorder {

    public static final int ARC_LEFT_TOP = 0;
    public static final int ARC_TOP_RIGHT = 1;
    public static final int ARC_RIGHT_BOTTOM = 2;
    public static final int ARC_BOTTOM_LEFT = 3;

    public ArcBorder() {
    }

    public ArcBorder(int arcEdges, int thickness) {
        this(arcEdges, 24, thickness, 0.2f);
    }

    public ArcBorder(int arcEdges, Color color, int thickness) {
        this(arcEdges, color, 24, thickness, 0.2f);
    }

    public ArcBorder(int arcEdges, int arcSize, int thickness, float hardening) {
        this(arcEdges, null, arcSize, thickness, hardening);
    }

    public ArcBorder(int arcEdges, Color color, int arcSize, int thickness, float hardening) {
        setColor(color);
        this.arcSize = arcSize;
        this.thickness = thickness;
        setHardening(hardening);
        setArcEdges(arcEdges);
    }

    public int getArcEdges() {
        return arcEdges;
    }

    public void setArcEdges(int arcEdges) {
        if (arcEdges < 0 || arcEdges > ARC_BOTTOM_LEFT) {
            throw new IllegalArgumentException("invalid edge: " + arcEdges);
        }
        this.arcEdges = arcEdges;
        arc = null;
    }

    public int getArcSize() {
        return arcSize;
    }

    public void setArcSize(int arcSize) {
        this.arcSize = arcSize;
        arc = null;
    }

    public float getHardening() {
        return hardening;
    }

    public void setHardening(float hardening) {
        if (hardening < 0f || hardening > 1f) {
            throw new IllegalArgumentException("hardening must be in range 0 to 1 inclusive");
        }
        this.hardening = hardening;
        arc = null;
    }

    public int getThickness() {
        return thickness;
    }

    public void setThickness(int thickness) {
        this.thickness = thickness;
        arc = null;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        if (color == null) {
            throw new NullPointerException("color");
        }
        this.color = color;
    }

    public void setComponentBackgroundAdjusted(boolean adjust) {
        snapBackground = adjust;
    }

    public boolean isComponentBackgroundAdjusted() {
        return snapBackground;
    }

    private boolean snapBackground = true;

    private void doSnap(Component c) {
        if (snapBackground && c != null) {
            snapBackground = false;
            c.setBackground(getDefaultPanelBackgroundColor());
        }
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return getBorderInsets(c, null);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        doSnap(c);
        int t = 0, l = 0, r = 0, b = 0;
        int i = arcSize / 2;
        switch (arcEdges) {
            case ARC_LEFT_TOP:
            case ARC_BOTTOM_LEFT:
                l = i;
                break;
            case ARC_TOP_RIGHT:
            case ARC_RIGHT_BOTTOM:
                r = i;
                break;
        }
        if (insets == null) {
            insets = new Insets(t, l, b, r);
        } else {
            insets.set(t, l, b, r);
        }
        return insets;
    }

    @Override
    public void paintBorder(Component c, Graphics g1, int x, int y, int width, int height) {
        createShape(c, x, y, width, height);
        Graphics2D g = (Graphics2D) g1;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Container p = c.getParent();
        if (p != null && !p.getBackground().equals(c.getBackground())) {
            g.setColor(p.getBackground());
            g.fill(exterior);
        }

        g.setStroke(pen);

        Color arcColor = color;
        if (color == null) {
            arcColor = c.getBackground().darker();
        }
        g.setColor(arcColor);
        g.draw(arc);
    }

    private void createShape(Component c, int x1, int y1, int w, int h) {
        if (arc != null && cx == x1 && cy == y1 && cw == w && ch == h) {
            return;
        }
        doSnap(c);
        cx = x1;
        cy = y1;
        cw = w;
        ch = h;

        final int penOffset = thickness / 2;
        int x2 = x1 + w, y2 = y1 + h;
        int fx1 = x1, fx2 = x2, fy1 = y1, fy2 = y2;
        x1 += penOffset;
        y1 += penOffset;
        x2 -= penOffset + 1;
        y2 -= penOffset + 1;

        final int hr = (int) (arcSize * hardening * 0.5f + 0.5f);

        exterior = new Path2D.Float();
        arc = new Path2D.Float();
        switch (arcEdges) {
            case ARC_LEFT_TOP:
                arc.moveTo(x1, y2);
                arc.lineTo(x1, y1 + arcSize);
                arc.quadTo(x1 + hr, y1 + hr, x1 + arcSize, y1);
                arc.lineTo(x2, y1);

                exterior.moveTo(fx1, fy1 + arcSize);
                exterior.quadTo(fx1 + hr, y1 + hr, fx1 + arcSize, fy1);
                exterior.lineTo(fx1, fy1);
                exterior.closePath();
                break;

            case ARC_TOP_RIGHT:
                arc.moveTo(x1, y1);
                arc.lineTo(x2 - arcSize, y1);
                arc.quadTo(x2 - hr, y1 + hr, x2, y1 + arcSize);
                arc.lineTo(x2, y2);

                exterior.moveTo(fx2 - arcSize, fy1);
                exterior.quadTo(fx2 - hr, fy1 + hr, fx2, fy1 + arcSize);
                exterior.lineTo(fx2, fy1);
                exterior.closePath();
                break;

            case ARC_RIGHT_BOTTOM:
                arc.moveTo(x2, y1);
                arc.lineTo(x2, y2 - arcSize);
                arc.quadTo(x2 - hr, y2 - hr, x2 - arcSize, y2);
                arc.lineTo(x1, y2);

                exterior.moveTo(fx2, fy2 - arcSize);
                exterior.quadTo(fx2 - hr, fy2 - hr, fx2 - arcSize, fy2);
                exterior.lineTo(fx2, fy2);
                exterior.closePath();
                break;

            case ARC_BOTTOM_LEFT:
                arc.moveTo(x2, y2);
                arc.lineTo(x1 + arcSize, y2);
                arc.quadTo(x1 + hr, y2 - hr, x1, y2 - arcSize);
                arc.lineTo(x1, y1);

                exterior.moveTo(fx1 + arcSize, fy2);
                exterior.quadTo(fx1 + hr, fy2 - hr, fx1, fy2 - arcSize);
                exterior.lineTo(fx1, fy2);
                exterior.closePath();
        }

        pen = new BasicStroke(thickness);
    }
    private int cx, cy, cw, ch;

    private int arcEdges = ARC_TOP_RIGHT;
    private int arcSize = 24;
    private int thickness = 4;
    private float hardening = 0.2f;
    private Color color = null;

    private BasicStroke pen;
    private Path2D exterior;
    private Path2D arc;

    public static Color getDefaultPanelBackgroundColor() {
        UIDefaults uid = UIManager.getDefaults();
        Color c = uid.getColor("nimbusBlueGrey");
        if (c == null) {
            c = Color.GRAY;
        }
        return c;
    }

//	public static void main( String[] args ) {
//		EventQueue.invokeLater( new Runnable() {
//			@Override
//			public void run() {
//				JFrame f = new JFrame( "Yo" );
//				KeyStrokeField ksf = new KeyStrokeField();
//				ksf.setBorder( new ArcBorder() );
//				ksf.setOpaque( true );
//				ksf.setBackground( Color.RED );
//
//				f.add( ksf );
//				f.pack();
//				f.setLocationRelativeTo( null );
//				f.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
//				f.setVisible( true );
//			}
//		});
//	}
}
