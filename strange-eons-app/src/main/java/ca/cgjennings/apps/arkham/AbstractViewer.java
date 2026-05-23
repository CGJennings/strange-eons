package ca.cgjennings.apps.arkham;

import ca.cgjennings.graphics.paints.CheckeredPaint;
import ca.cgjennings.math.Interpolation;
import ca.cgjennings.ui.EyeDropper;
import ca.cgjennings.ui.anim.Animation;
import ca.cgjennings.ui.anim.TimeShiftedComposer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import javax.swing.JPanel;
import resources.Settings;

/**
 * An abstract base class for zoomable previewers.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public abstract class AbstractViewer extends JPanel {

    private Paint background;
    private boolean showZoom = true;

    public AbstractViewer() {
        super();
        new EventHandler();
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        setEnabled(true);
        setOpaque(true);
    }

    /**
     * Forwards to {@link #setBackgroundPaint}.
     *
     * @param c the new background color
     */
    @Override
    public void setBackground(Color c) {
        setBackgroundPaint(c);
    }

    /**
     * If current background is a color, returns it; otherwise returns default
     * color. Use {@link #getBackgroundPaint} instead.
     */
    @Override
    public Color getBackground() {
        if (background instanceof Color) {
            return (Color) background;
        }
        return (Color) DEFAULT_DARK_BACKGROUND;
    }

    protected Paint getDefaultBackgroundPaint() {
        int v = 0;
        try {
            v = Settings.getShared().getInt("preview-backdrop");
        } catch (Exception e) {
            // try-catch is here so that forms that embed the viewer will work in GUI editors
        }
        switch (v) {
            case 1:
                return DEFAULT_LIGHT_BACKGROUND;
            case 2:
                return DEFAULT_CHECKERED_BACKGROUND;
            default:
                return DEFAULT_DARK_BACKGROUND;
        }
    }
    static final Paint DEFAULT_DARK_BACKGROUND = new Color(0x191919);
    static final Paint DEFAULT_LIGHT_BACKGROUND = new Color(0xf6f6f6);
    static final Paint DEFAULT_CHECKERED_BACKGROUND = new CheckeredPaint();

    public Paint getBackgroundPaint() {
        if (background == null) {
            return getDefaultBackgroundPaint();
        }
        return background;
    }

    public void setBackgroundPaint(Paint p) {
        if (p == null) {
            p = getDefaultBackgroundPaint();
        }
        if (!p.equals(background)) {
            background = p;
            if (isShowing()) {
                repaint();
            }
        }
    }

    public void setShowZoomLevel(boolean show) {
        showZoom = show;
        repaint(50);
    }

    public boolean getShowZoomLevel() {
        return showZoom;
    }

    public void setTranslation(double tx, double ty) {
        this.tx = tx;
        this.ty = ty;
        repaint(50);
    }

    /**
     * Returns the current image to be drawn in the viewer.
     *
     * @return the image the viewer should display
     */
    protected abstract BufferedImage getCurrentImage();
    protected double userScaleMultiplier = 1d, tx = 0d, ty = 0d;
    protected boolean autoFitToWindow = true;

    protected Insets borderInsets;

    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        g.setPaint(getBackgroundPaint());
        g.fillRect(0, 0, getWidth(), getHeight());
        Shape oldClip = g.getClip();

        // adjust for border insets
        final double compWidth, compHeight;
        {
            borderInsets = getInsets(borderInsets);
            final int cw = getWidth() - (borderInsets.left + borderInsets.right);
            final int ch = getHeight() - (borderInsets.top + borderInsets.bottom);
            g.translate(borderInsets.left, borderInsets.top);
            g.clipRect(0, 0, cw, ch);
            compWidth = cw - 4d;
            compHeight = ch - 4d;
        }

        BufferedImage currentImage = getCurrentImage();
        if (currentImage == null) {
            return;
        }

        double hscale = compWidth / currentImage.getWidth();
        double vscale = compHeight / currentImage.getHeight();
        double scale = hscale < vscale ? hscale : vscale;
        if (scale > 1d) {
            scale = 1d;
        }
        if (autoFitToWindow) {
            scale *= userScaleMultiplier;
        } else {
            scale = userScaleMultiplier;
        }

        double newWidth = currentImage.getWidth() * scale;
        double newHeight = currentImage.getHeight() * scale;
        double x = 2d + (compWidth - newWidth) / 2;
        double y = 2d + (compHeight - newHeight) / 2;

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int ix = (int) Math.round(x + tx);
        int iy = (int) Math.round(y + ty);
        int w = Math.round((float) newWidth);
        int h = Math.round((float) newHeight);
        g.drawImage(currentImage, ix, iy, w, h, null);

        paintZoomLabel(g, scale);

        if (scale < 1.0) {
            paintLoupe(g, currentImage, ix, iy, newWidth, newHeight);
        }

        g.setClip(oldClip);
    }

    protected static int MINIMUM_LOUPE_SIZE = 64;
    protected static int MAXIMUM_LOUPE_SIZE = 144;

    protected Color[] loupeColors = {Color.DARK_GRAY, Color.ORANGE, Color.WHITE};
    protected Stroke[] loupeStrokes = {new BasicStroke(3.5f), new BasicStroke(2.5f), new BasicStroke(1.5f)};
    protected Font labelFont = new Font("SansSerif", Font.PLAIN, 11).deriveFont(AffineTransform.getScaleInstance(1.4d, 1d));

    protected void paintLoupe(Graphics2D g, BufferedImage currentImage, double imX, double imY, double imWidth, double imHeight) {
        Point p = getMousePosition();
        if (!isEnabled() || p == null || EyeDropper.isCurrentlySampling()) {
            return;
        }

        double xProportion = (p.getX() - imX) / imWidth;
        double yProportion = (p.getY() - imY) / imHeight;

        // draw no zoom box when off the image
        if (xProportion < 0d || xProportion > 1d || yProportion < 0d || yProportion > 1d) {
            return;
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int zoomArea = (getWidth() - 4) / 7;
        final int zoomAlt = (getHeight() - 4) / 7;
        if (zoomAlt < zoomArea) {
            zoomArea = zoomAlt;
        }
        zoomArea = Math.max(MINIMUM_LOUPE_SIZE, Math.min(zoomArea, MAXIMUM_LOUPE_SIZE));

        Ellipse2D loupeShape = new Ellipse2D.Float(p.x - zoomArea, p.y - zoomArea, zoomArea * 2, zoomArea * 2);

        // for transparent images: repaint bg so unzoomed image doesn't show
        g.setPaint(getBackgroundPaint());
        g.fill(loupeShape);

        Shape clip = g.getClip();
        // reduce the clipping region to the loupe rectangle, making
        // sure that it does not extend beyond the the view window
        //g.setClip( g.getClipBounds().intersection( r ) );
        g.clip(loupeShape);
        g.drawImage(currentImage, p.x - (int) (xProportion * currentImage.getWidth()), p.y - (int) (yProportion * currentImage.getHeight()), null);
        g.setClip(clip);

        // Loupe ring
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Stroke s = g.getStroke();
        for (int i = 0; i < loupeColors.length; ++i) {
            g.setColor(loupeColors[i]);
            g.setStroke(loupeStrokes[i]);
            g.draw(loupeShape);
        }
        g.setStroke(s);
    }

    protected void paintZoomLabel(Graphics2D g, double scale) {
        if (!getShowZoomLevel()) {
            return;
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        String label = zoomFormatter.format(scale);
        g.setFont(labelFont);
        FontMetrics fm = g.getFontMetrics();
        int width = fm.stringWidth(label);
        int height = fm.getAscent() + fm.getDescent();
        int xp = getWidth() - width - LABEL_GAP_X - LABEL_MARGIN * 2, yp = getHeight() - height - LABEL_GAP_Y;
        g.setColor(new Color(0x77ffffff, true));
        g.fillRoundRect(xp, yp + 1, width + 4 + LABEL_MARGIN * 2, height + 2, LABEL_ARC, LABEL_ARC);
        g.setColor(Color.BLACK);
        g.drawRoundRect(xp, yp + 1, width + 4 + LABEL_MARGIN * 2, height + 2, LABEL_ARC, LABEL_ARC);
        yp += fm.getAscent();
        g.drawString(label, xp + 2 + LABEL_MARGIN, yp + 2);
    }
    private static final int LABEL_GAP_X = 8;
    private static final int LABEL_GAP_Y = 8;
    private static final int LABEL_MARGIN = 6;
    private static final int LABEL_ARC = 6;

    private final NumberFormat zoomFormatter = NumberFormat.getPercentInstance();

    class EventHandler implements MouseListener, MouseMotionListener, MouseWheelListener {

        public EventHandler() {
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!isEnabled()) {
                return;
            }

            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            dragging = false;
            repaint(50);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (!isEnabled()) {
                return;
            }

            int button = e.getButton();
            if (button == MouseEvent.BUTTON1) {
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                dragX = e.getXOnScreen();
                dragY = e.getYOnScreen();
                dragging = true;
            } else if (button == MouseEvent.BUTTON3) {
                tx = ty = 0d;
                userScaleMultiplier = 1d;
                zoomIndex = ZOOM_1_INDEX;
            }
            repaint(50);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (!isEnabled()) {
                return;
            }
            repaint(50);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            if (!isEnabled()) {
                return;
            }
            repaint(50);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (!isEnabled()) {
                return;
            }

            repaint(50);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (!isEnabled()) {
                return;
            }
            repaint(50);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!isEnabled()) {
                return;
            }

            if (dragging) {
                int x = e.getXOnScreen();
                int y = e.getYOnScreen();
                tx += (x - dragX);
                ty += (y - dragY);
                dragX = x;
                dragY = y;
                repaint(50);
            }
        }
        private boolean dragging = false;
        private int dragX, dragY;

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (!isEnabled()) {
                return;
            }
            int rotation = e.getWheelRotation();
            if (!Settings.getShared().getYesNo("invert-wheel-zoom")) {
                rotation = -rotation;
            }

            zoomIndex = Math.max(0, Math.min(zoomIndex + rotation, zoomLevels.length - 1));

            final double startMultiplier = userScaleMultiplier;
            final double stopMultiplier = zoomLevels[zoomIndex];

            if (startMultiplier != stopMultiplier) {
                Animation anim = new Animation(0.25f) {
                    @Override
                    public void composeFrame(float position) {
                        changeZoom(Interpolation.lerp(position, startMultiplier, stopMultiplier));
                    }
                };
                new TimeShiftedComposer(anim);
                anim.play(this);
            }
        }
    }

    private void changeZoom(double newZoom) {
        tx *= newZoom / userScaleMultiplier;
        ty *= newZoom / userScaleMultiplier;
        userScaleMultiplier = newZoom;
        repaint();
    }

    @Override
    public void setEnabled(boolean enable) {
        super.setEnabled(enable);
        if (enable) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private static final int ZOOM_1_INDEX = 3; // index of 100% in the zoom array
    private static final double[] zoomLevels = {
        //		0.125, 0.167, 0.25, 0.33, 0.5, 0.667, 0.75, 1.0, 1.5, 2.0, 3.0, 4.0, 6.0, 8.0, 10.0, 12.0, 14.0, 16.0, 20.0, 24.0, 28.0, 32.0
        18.75 / 150d, 37.5d / 150d, 75d / 150d, 150d / 150d, 225d / 150d, 300d / 150d, 450d / 150d, 600d / 150d, 750d / 150d, 900d / 150d, 1100d / 150d
    };
    private int zoomIndex = 3;
}
