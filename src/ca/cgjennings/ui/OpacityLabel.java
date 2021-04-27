package ca.cgjennings.ui;

import ca.cgjennings.graphics.paints.CheckeredPaint;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import javax.swing.JLabel;

/**
 * Shows a gradient of relative hue changes.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class OpacityLabel extends JLabel {

    public OpacityLabel() {
        super();
        setForeground(Color.RED);
        setOpaque(true);
    }

    private CheckeredPaint bgPaint = new CheckeredPaint(2);

    @Override
    public void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;

        Color base = getForeground();
        if (!isEnabled()) {
            float[] hsb = Color.RGBtoHSB(base.getRed(), base.getBlue(), base.getGreen(), null);
            base = new Color(Color.HSBtoRGB(hsb[0], 0f, hsb[2]));
        }
        Color bg = new Color(base.getRGB() & 0xff_ffff, true);

        g.setPaint(bgPaint);
        g.fillRect(0, 0, getWidth(), getHeight());

        LinearGradientPaint lg = new LinearGradientPaint(0, 0, getWidth(), 0, new float[]{0f, 1f}, new Color[]{bg, base});
        g.setPaint(lg);
        g.fillRect(0, 0, getWidth(), getHeight());

        /*

		boolean enabled = isEnabled();

		float delta = 1f / (float) SEGMENTS;
		float segwidth = (float) getWidth() / (float) SEGMENTS;
		Rectangle2D.Float r = new Rectangle2D.Float( 0f, 0f, segwidth, (float) getHeight() );

		float opacity = 0f;
		for( int i = 0; i <= SEGMENTS; ++i ) {
			g.setPaint( mix( base, bg, opacity ) );
			g.fill( r );
			r.x += segwidth;
			opacity += delta;
//			if( i == SEGMENTS - 1 ) {
//				r.width = (float) getWidth() - r.x;
//			}
		}
         */
    }

//	public static void main( String[] args ) {
//		JFrame f = new JFrame();
//		f.setDefaultCloseOperation( f.EXIT_ON_CLOSE );
//		f.add( new OpacityLabel() );
//		f.setSize( 320, 48 );
//		f.setVisible( true );
//	}
}
