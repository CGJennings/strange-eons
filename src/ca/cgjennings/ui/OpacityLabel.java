package ca.cgjennings.ui;

import ca.cgjennings.graphics.paints.CheckeredPaint;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import javax.swing.JLabel;

/**
 * Shows a gradient of relative opacity changes.
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

    private final CheckeredPaint bgPaint = new CheckeredPaint(2);

    @Override
    public void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;

        Color base = getForeground();
        if (!isEnabled()) {
            float[] hsb = Color.RGBtoHSB(base.getRed(), base.getBlue(), base.getGreen(), null);
            base = new Color(Color.HSBtoRGB(hsb[0], 0f, hsb[2]));
        }
        Color bg = new Color(base.getRGB() & 0xffffff, true);

        g.setPaint(bgPaint);
        g.fillRect(0, 0, getWidth(), getHeight());

        LinearGradientPaint lg = new LinearGradientPaint(0, 0, getWidth(), 0, new float[]{0f, 1f}, new Color[]{bg, base});
        g.setPaint(lg);
        g.fillRect(0, 0, getWidth(), getHeight());
    }
}
