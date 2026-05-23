package ca.cgjennings.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JLabel;

/**
 * Shows a gradient of relative hue changes.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class HueLabel extends JLabel {

    private float hue;

    public void setHue(float hue) {
        this.hue = hue;
        repaint();
    }

    public float getHue(float hue) {
        return hue;
    }

    @Override
    public void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;

        int SEGMENTS = getWidth();
        if (SEGMENTS > 256) {
            SEGMENTS = 256;
        }

        boolean enabled = isEnabled();

        float delta = 1f / SEGMENTS;
        float lhs = hue - 0.5f + delta / 2f;
        float segwidth = getWidth() / (float) SEGMENTS;
        Rectangle2D.Float r = new Rectangle2D.Float(0f, 0f, segwidth < 1 ? 1 : segwidth, getHeight());

        for (int i = 0; i < SEGMENTS; ++i) {
            if (enabled) {
                float hue = lhs + delta * i;
                if (Math.abs(hue) < 0.00001f) {
                    hue = 0f;
                }
                g.setColor(new Color(Color.HSBtoRGB(hue, 1f, 1f)));
            } else {
                float b = delta * i - 0.5f;
                g.setColor(new Color(Color.HSBtoRGB(0f, 0f, (b < 0f ? -b : b) + 0.25f)));
            }
            g.fill(r);
            r.x += segwidth;
        }
    }
}
