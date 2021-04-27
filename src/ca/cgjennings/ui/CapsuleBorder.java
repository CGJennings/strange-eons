package ca.cgjennings.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import javax.swing.border.AbstractBorder;

/**
 * Draws a border with a horizontal capsule shape, rounded on the ends and with
 * an optional thin border along the top and/or bottom.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class CapsuleBorder extends AbstractBorder {

    private int capWidth = 8;
    private Paint borderPaint, capPaint;
    private int top = 1, bottom = 1;

    public CapsuleBorder() {
    }

    public CapsuleBorder(int top, int bottom, int capWidth) {
        this(null, null, top, bottom, capWidth);
    }

    public CapsuleBorder(Paint borderPaint, Paint capPaint) {
        this(borderPaint, capPaint, 1, 1, 8);
    }

    public CapsuleBorder(Paint borderPaint, Paint capPaint, int top, int bottom, int capWidth) {
        this.top = top;
        this.bottom = bottom;
        this.capWidth = capWidth;

        this.borderPaint = borderPaint;
        this.capPaint = capPaint;
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(top, capWidth, bottom, capWidth);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.top = top;
        insets.left = capWidth;
        insets.bottom = bottom;
        insets.right = capWidth;
        return insets;
    }

    @Override
    public boolean isBorderOpaque() {
        return true;
    }

    @Override
    public void paintBorder(Component c, Graphics g1, int x, int y, int width, int height) {
        Paint black, white;

        white = borderPaint;
        if (white == null) {
            if (c != null) {
                white = c.getBackground();
            }
            if (white == null) {
                white = Color.WHITE;
            }
        }

        black = capPaint;
        if (black == null) {
            if (c != null) {
                black = c.getForeground();
            }
            if (black == null) {
                black = Color.BLACK;
            }
        }

        Graphics2D g = (Graphics2D) g1;
        g.setPaint(black);
        if (top > 0) {
            g.fillRect(0, 0, width, top);
        }
        if (bottom > 0) {
            g.fillRect(0, height - bottom, width, bottom);
        }
        if (capWidth > 0) {
            g.fillRect(0, top, capWidth, height);
            g.fillRect(x + width - capWidth, top, capWidth, height);

            Shape oldClip = g.getClip();
            Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

            g.setPaint(white);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.setClip(0, top, capWidth, height);
            g.fillOval(1, top, capWidth * 2 - 1, height - top - bottom);

            g.setClip(x + width - capWidth, top, capWidth, height);
            g.fillOval(x + width - capWidth * 2 - 1, top, capWidth * 2, height - top - bottom);

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
            g.setClip(oldClip);
        }
    }

    public int getCapWidth() {
        return capWidth;
    }

    public void setCapWidth(int capWidth) {
        if (capWidth < 0) {
            throw new IllegalArgumentException("capWidth < 0");
        }
        this.capWidth = capWidth;
    }

    public Paint getCapPaint() {
        return capPaint;
    }

    public void setCapPaint(Paint capPaint) {
        this.capPaint = capPaint;
    }

    public Paint getBorderPaint() {
        return borderPaint;
    }

    public void setBorderPaint(Paint borderPaint) {
        this.borderPaint = borderPaint;
    }

    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        if (top < 0) {
            throw new IllegalArgumentException("top < 0");
        }
        this.top = top;
    }

    public int getBottom() {
        return bottom;
    }

    public void setBottom(int bottom) {
        if (bottom < 0) {
            throw new IllegalArgumentException("bottom < 0");
        }
        this.bottom = bottom;
    }

//	public static void main(String[] args) {
//		EventQueue.invokeLater(new Runnable() {
//			@Override
//			public void run() {
//				JFrame f = new JFrame( "Border WIP" );
//				JLabel t = new JLabel( "                 " );
//				t.setBorder( new CompoundBorder(
//						new CapsuleBorder(),
//						new LineBorder( Color.RED, 1 )
//				) );
//				f.add( t );
//				f.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
//				f.setSize( 300, 64 );
//				f.setLocationRelativeTo( null );
//				f.setVisible( true );
//			}
//		});
//	}
}
