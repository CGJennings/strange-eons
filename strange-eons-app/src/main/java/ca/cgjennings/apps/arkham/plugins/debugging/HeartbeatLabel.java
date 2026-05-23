package ca.cgjennings.apps.arkham.plugins.debugging;

import ca.cgjennings.math.Interpolation.CubicSpline;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 * Displays status messages and provides connection feedback for the debugger
 * client.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
final class HeartbeatLabel extends JLabel {

    public HeartbeatLabel() {
        this(null);
    }

    public HeartbeatLabel(String text) {
        super(text);
        setForeground(Color.WHITE);
        setBackground(Color.DARK_GRAY);
        setOpaque(true);
        setFont(getFont().deriveFont(Font.BOLD, getFont().getSize2D() - 1f));
        createFrames();
        setHorizontalTextPosition(LEFT);
    }

    private boolean hold = true;
    private int frame = 0;

    public void setHold(boolean hold) {
        if (this.hold != hold) {
            this.hold = hold;
            repaint();
        }
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        if (frames == null) {
            return;
        }

        if (hold || frame == frames.length) {
            frame = 0;
        }
        setIcon(frames[frame++]);

        if (frame == frames.length) {
            frame = 0;
        }
        paintImmediately(0, 0, getWidth(), getHeight());
    }

    private void createFrames() {
        final int count = 34;
        CubicSpline spline = new CubicSpline(
                new double[]{0d, 4d, 7d, 13d, 16d, 23d, 26d, 32d, 35d},
                new double[]{0d, 0d, .2d, .8d, 1d, 1d, .8d, .2d, 0d}
        );
        frames = new Icon[count];

        float lastAlpha = -1f;
        for (int i = 0; i < count; ++i) {
            float alpha = Math.max(0f, Math.min(1f, (float) spline.f(i)));
            if (Math.abs(alpha - lastAlpha) < 1f / 256f) {
                frames[i] = frames[i - 1];
            } else {
                frames[i] = createFrame(alpha);
            }
            lastAlpha = alpha;
        }
    }

    private Icon createFrame(float alpha) {
        BufferedImage bi = new BufferedImage(9, 9, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        try {
            final int w = bi.getWidth();
            final int h = bi.getHeight();
            g.setPaint(Color.DARK_GRAY);
            g.fillRect(0, 0, w, h);
            g.setPaint(new Color(.7f, .6f, .3f, alpha));
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.fillOval(1, 2, w - 3, h - 3);
        } finally {
            g.dispose();
        }
        return new ImageIcon(bi);
    }

    private Icon[] frames;
}
