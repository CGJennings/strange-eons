package ca.cgjennings.apps.arkham.sheet;

import ca.cgjennings.graphics.AbstractGraphics2DAdapter;
import ca.cgjennings.graphics.PrototypingGraphics2D;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * Collects portrait debug region painting information from a sheet so that it
 * can be painted at the end of painting a sheet, preventing boxes from being
 * overdrawn by other elements. (Painting a decorative frame over the edges of a
 * portrait region is extremely common.)
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.3
 */
final class PortraitDebugPainter {

    private PortraitDebugPainter() {
    }

    public static Graphics2D createFor(Graphics2D g) {
        return new PortraitDebugGraphics(g, new PortraitDebugPainter());
    }

    /**
     * Adds a new portrait region to be painted. Does not check if debug paint
     * is enabled.
     *
     * @see Sheet#drawPortraitBox(java.awt.Graphics2D,
     * java.awt.geom.Rectangle2D, java.awt.image.BufferedImage, double, double,
     * double, double)
     */
    public static void add(Graphics2D g, Rectangle2D region, BufferedImage portraitImage, double panX, double panY, double scale, double angle) {
        if (!(g instanceof PortraitDebugGraphics)) {
            for (;;) {
                if (g instanceof PortraitDebugGraphics) {
                    break;
                }
                if (g instanceof PrototypingGraphics2D) {
                    g = ((PrototypingGraphics2D) g).getUnrestrictedGraphics();
                } else {
                    ca.cgjennings.apps.arkham.StrangeEons.log.info("could not find debug graphics to queue portrait box");
                    PortraitDebugPainter fallback = new PortraitDebugPainter();
                    fallback.enqueue(g, region, portraitImage, panX, panY, scale, angle);
                    fallback.paint(g);
                    return;
                }
            }
        }

        ((PortraitDebugGraphics) g).debugPainter.enqueue(g, region, portraitImage, panX, panY, scale, angle);
    }

    private void enqueue(Graphics2D g, Rectangle2D region, BufferedImage portraitImage, double panX, double panY, double scale, double angle) {
        if (queue == null) {
            queue = new PaintInfo[8];
            next = 0;
        }
        if (next >= queue.length) {
            queue = Arrays.copyOf(queue, queue.length + 8);
        }

        PaintInfo pi = new PaintInfo();
        pi.g = g;
        pi.region = region.getBounds2D();
        pi.imageBounds = new Rectangle2D.Double(0, 0, portraitImage.getWidth(), portraitImage.getHeight());
        final double centerX = portraitImage.getWidth() * scale / 2d;
        final double centerY = portraitImage.getHeight() * scale / 2d;
        AffineTransform xform = AffineTransform.getTranslateInstance(
                region.getCenterX() - centerX + panX,
                region.getCenterY() - centerY + panY
        );
        xform.concatenate(AffineTransform.getRotateInstance(angle * DEGREES_TO_RADIANS, centerX, centerY));
        xform.concatenate(AffineTransform.getScaleInstance(scale, scale));
        pi.xform = xform;

        queue[next++] = pi;
    }

    /**
     * Paints all queued portrait region debug boxes.
     *
     * @param g a graphics context for the sheet being painted
     */
    private void paint(Graphics2D g) {
        if (next <= 0) {
            return;
        }

        Paint oldPaint = g.getPaint();
        Stroke oldStroke = g.getStroke();
        try {
            for (int i = 0, alien = 0; i < next; ++i) {
                final PaintInfo pi = queue[i];
                if (pi == null) {
                    continue;
                }
                queue[i] = null;
                if (pi.g != g) {
                    queue[alien++] = pi;
                } else {
                    pi.paint();
                }
            }
        } catch (Exception ex) {
            ca.cgjennings.apps.arkham.StrangeEons.log.log(
                    Level.WARNING, "uncaught exception while painting boxes", ex
            );
        } finally {
            g.setStroke(oldStroke);
            g.setPaint(oldPaint);
        }
        next = 0;
    }

    private int next = 0;
    private PaintInfo[] queue;

    private static class PaintInfo {

        Graphics2D g;
        Rectangle2D region;
        Rectangle2D imageBounds;
        AffineTransform xform;

        private void paint() {
            g.setColor(REGION_PAINT);
            g.setStroke(SOLID);
            g.draw(region);

            g.setColor(IMAGE_PAINT);
            g.setStroke(DASHED);
            g.draw(new Path2D.Double(imageBounds, xform));
        }
    }

    static final Color REGION_PAINT = Color.CYAN;
    static final Color IMAGE_PAINT = Color.MAGENTA;
    static final BasicStroke SOLID = new BasicStroke(1f);
    static final BasicStroke DASHED = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0f, new float[]{4f, 4f}, 0f);
    private static final double DEGREES_TO_RADIANS = -0.0174532925d;

    private static class PortraitDebugGraphics extends AbstractGraphics2DAdapter {

        private PortraitDebugPainter debugPainter;

        public PortraitDebugGraphics(Graphics2D g, PortraitDebugPainter debug) {
            super(g);
            this.debugPainter = debug;
        }

        @Override
        protected AbstractGraphics2DAdapter createImpl(Graphics2D newG) {
            return new PortraitDebugGraphics(newG, debugPainter);
        }

        @Override
        public void dispose() {
            debugPainter.paint(this);
            debugPainter = null;
            super.dispose();
        }
    }
}
