package ca.cgjennings.apps.arkham;

import ca.cgjennings.ui.MultiResolutionImageResource;
import ca.cgjennings.ui.StyleUtilities;
import ca.cgjennings.ui.anim.Animation;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import resources.ResourceKit;

/**
 * The splash window shown during application startup.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
class SplashWindow extends JFrame {

    private Image splashImage;
    private Color textColor = new Color(0x563e11);
    private Color versionColor = new Color(0x563e11);
    private Color gradient1 = new Color(0xaa24c9ae, true);
    private Color gradient2 = new Color(0xaaadedff, true);
    private Font textFont = ResourceKit.enableKerningAndLigatures(
            new Font("SansSerif", Font.PLAIN, 10).deriveFont(
                    AffineTransform.getScaleInstance(1.2d, 1d)
            )
    );

    private final int actX = 17, actY = 143, actW = 351, actH = 56;
    private final int actTX = 20, actTY = 167;
    private final int barX = 18, barY = 183, barW = 315, barH = 7;
    private GradientPaint barGradient;

    private String currentActivity = " ";
    private int percentComplete = 0;

    volatile boolean animationDone = false;

    /**
     * Creates a new splash window. The splash window performs a brief animation
     * on opening, then passes control to {@code callback}.
     *
     * @param callback the code to execute once the window is visible
     */
    public SplashWindow(GraphicsConfiguration gc, final boolean animate) {
        super("Strange Eons", gc);
        setUndecorated(true);
        getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        try {
            setIconImages(AppFrame.getApplicationFrameIcons());
        } catch (Exception e) {
            StrangeEons.log.log(Level.WARNING, "unable to load icon for splash window", e);
        }
        setAlwaysOnTop(true);
        setFocusable(true);
        loadSplashImage();
        painter = new SplashPainter();
        add(painter);
        if (splashImage != null) {
            setSize(splashImage.getWidth(null), splashImage.getHeight(null));
        } else {
            setSize(200, 200);
        }

        barGradient = new GradientPaint(barX, barY, gradient1, barX, (barY + barH / 2), gradient2, true);

        if (gc == null) {
            setLocationRelativeTo(null);
        } else {
            Rectangle bounds = gc.getBounds();
            setLocation(bounds.x + (bounds.width - getWidth()) / 2, bounds.y + (bounds.height - getHeight()) / 2);
        }

        StyleUtilities.setWindowOpacity(this, 0f);

        if (!StrangeEons.isNonInteractive()) {
            setVisible(true);
        }

        new Animation(ANIMATION_TIME_MS / 1000f) {
            @Override
            public void composeFrame(float position) {
                if (position == 1f) {
                    animationDone = true;
                    updateSplash();
                    // tell StrangeEons.init() to continue starting the app
                    StrangeEons app = StrangeEons.getApplication();
                    if (animate) {
                        synchronized (app) {
                            app.notify();
                        }
                    }
                }
                final float alpha = Math.min(1f, position / ALPHA_CHOKE);
                StyleUtilities.setWindowOpacity(SplashWindow.this, alpha);
            }
        }.play();
    }

    /**
     * The minimum amount of time that StrangeEons.init() should wait for the
     * opening animation to complete.
     */
    static final int ANIMATION_TIME_MS = 300;
    private static final float ALPHA_CHOKE = 0.8f;

    synchronized public void setActivity(String activity) {
        if (activity == null) {
            throw new NullPointerException("activity");
        }
        currentActivity = activity;
        updateSplash(actX, actY, actW, actH);
    }

    synchronized public String getActivity() {
        return currentActivity;
    }

    synchronized public void setPercent(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("percent: " + percent);
        }
        // the bar can never go down, unless reset to 0
        // this lets us update it from competing threads
        if (percent != 0 && percent < percentComplete) {
            return;
        }
        percentComplete = percent;
        updateSplash(barX, barY, barW, barH);
    }

    synchronized public int getPercent() {
        return percentComplete;
    }

    // painting is implemented via a JComponent so we can access a
    // double-buffered paintImmediately method
    private class SplashPainter extends JComponent {

        public SplashPainter() {
            if (splashImage != null) {
                final int w = splashImage.getWidth(null);
                final int h = splashImage.getHeight(null);
                Dimension d = new Dimension(w, h);
                setPreferredSize(d);
                setSize(w, h);
            }
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (splashImage != null) {
                paintFrame((Graphics2D) g);
            } else {
                g.setColor(Color.RED);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }

    private SplashPainter painter;

    private void updateSplash(final int x, final int y, final int w, final int h) {
        if (EventQueue.isDispatchThread()) {
            painter.paintImmediately(x, y, w, h);
        } else {
            EventQueue.invokeLater(() -> {
                repaint(x, y, w, h);
            });
        }
    }

    private void updateSplash() {
        updateSplash(0, 0, getWidth(), getHeight());
    }

    private void paintFrame(Graphics2D g) {
        if (splashImage == null) {
            return;
        }
        g.drawImage(splashImage, 0, 0, null);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(textFont);
        FontMetrics fm = g.getFontMetrics();

        g.setColor(versionColor);
        final String version = String.valueOf(StrangeEons.getBuildNumber());
        g.drawString(version, 330 - fm.stringWidth(version), 20 + fm.getAscent());

        g.setColor(textColor);
        g.drawString(getActivity(), actTX, actTY + fm.getAscent());

        g.clipRect(18, 183, 315, 7);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(barGradient);
        g.fillRoundRect(barX - 4, barY, 4 + barW * getPercent() / 100, barH, 3, 3);
    }

    private void loadSplashImage() {
        String resource = "splash";
        Calendar today = GregorianCalendar.getInstance();
        final int MONTH = today.get(Calendar.MONTH);
        final int DAY = today.get(Calendar.DATE);

        if (MONTH == Calendar.OCTOBER) {
            if (DAY >= 28 && DAY <= 31) {
                resource = "splash10";
            }
        } else if (MONTH == Calendar.MARCH) {
            if (DAY == 28) {
                resource = "splash3";
                textColor = new Color(0, true);
            }
        }

        try {
            splashImage = new MultiResolutionImageResource("icons/splash/" + resource + ".png");
        } catch (Exception e) {
            StrangeEons.log.log(Level.SEVERE, "INIT: unable to load splash image {0}", resource);
        }
    }
}
