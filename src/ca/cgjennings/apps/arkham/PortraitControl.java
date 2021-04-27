package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.component.Portrait;
import ca.cgjennings.graphics.filters.ColorOverlayFilter;
import ca.cgjennings.ui.dnd.FileDrop;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Level;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;

/**
 * A control used to adjust a portrait's settings kinesthetically. This control
 * makes up part of a portrait panel; it is not intended to be used separately.
 * <b>This class is public only so that the control can be manipulated by GUI
 * editing tools.</b>
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public final class PortraitControl extends JComponent {

    private Portrait p;
    private int w, h;
    private BufferedImage bi;

    public PortraitControl() {
        MARGIN_COLOR = UIManager.getColor("nimbusBlueGrey");
        if (MARGIN_COLOR == null) {
            MARGIN_COLOR = Color.LIGHT_GRAY;
        }
        BACKGROUND_COLOR = MARGIN_COLOR.darker().darker();

        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
        addMouseWheelListener(wheelListener);
        setAutoscrolls(true);
        setOpaque(true);
        new FileDrop(this, (File[] files) -> {
            PortraitPanel p1 = getPortraitPanel();
            if (p1 == null) {
                return;
            }
            p1.setSource(files[0].getAbsolutePath());
        });
    }

    private PortraitPanel getPortraitPanel() {
        Container p = getParent();
        while (p != null && !(p instanceof PortraitPanel)) {
            p = p.getParent();
        }
        return (PortraitPanel) p;
    }

    public void setPortrait(Portrait p) {
        if (p == null) {
            // disable control
            p = null;
            w = h = 0;
        } else {
            this.p = p;
            Dimension d = p.getClipDimensions();
            w = d.width;
            h = d.height;
            bi = p.getImage();
            isRotatable = p.getFeatures().contains(Portrait.Feature.ROTATE);
            updateLocal();
        }
        repaint();
    }

    /**
     * Synchronize the state of the control with the state of the portrait
     * panel; called by the panel when, for example, the selected image is
     * changed.
     */
    public void synchronize() {
        setPortrait(p);
    }

    @Override
    protected void paintComponent(Graphics g1) {
        paintInsets = getInsets(paintInsets);
        if (p == null || !isEnabled()) {
            g1.setColor(getBackground());
            g1.fillRect(
                    paintInsets.left,
                    paintInsets.top,
                    getWidth() - (paintInsets.left + paintInsets.right),
                    getHeight() - (paintInsets.top + paintInsets.bottom)
            );
            return;
        }

        Graphics2D g = (Graphics2D) g1;

        // apply rendering hints so that we draw faster when the mouse is dragged
        if (isAdjusting) {
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        } else {
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        final Shape oldClip = g.getClip();
        final Stroke oldStroke = g.getStroke();
        final AffineTransform oldAT = g.getTransform();
        final Composite oldComposite = g.getComposite();

        g.translate(paintInsets.left, paintInsets.top);
        int compW = getWidth() - (paintInsets.left + paintInsets.right);
        int compH = getHeight() - (paintInsets.top + paintInsets.bottom);
        g.clipRect(0, 0, compW, compH);

        g.setPaint(MARGIN_COLOR);
        g.fillRect(0, 0, compW, compH);

        viewScale = 1d;
        {
            // How much do we want to display?
            // The portrait clip dimensions + X%
            final int paddedW = w + w / MARGIN_DIVISOR;
            final int paddedH = h + h / MARGIN_DIVISOR;
            // compute the scale that scales the clip window to the component
            final double xScale = compW / (double) paddedW;
            final double yScale = compH / (double) paddedH;
            viewScale = Math.min(xScale, yScale);
        }
        pixelSize = 1d / viewScale;

        // Transform the view so that (0,0) is the center of the portrait area
        m2v = AffineTransform.getTranslateInstance(compW / 2d, compH / 2d);
        m2v.scale(viewScale, viewScale);
        g.transform(m2v);

        // Paint the a shape representing the portrait space
        Rectangle2D.Double clip = new Rectangle2D.Double(-w / 2d, -h / 2d, w, h);
        g.setPaint(BACKGROUND_COLOR);
        Shape xformClip = g.getClip();
        g.clip(clip);
        // instead of filling clipRect, we fill a larger area while the clip is active:
        // clipping and filling round to integer coords differently, so this ensures
        // that the grey empty rectangle and the portrait cover the same area
        g.fillRect(-w, -h, w * 2, h * 2);

        // draw the portrait: the portrait is visible within the clip window,
        // but only the frame is visible outside
        if (bi != null) {
            // create a transform to draw the image/frame
            double centerX = bi.getWidth() * scale / 2d;
            double centerY = bi.getHeight() * scale / 2d;
            portraitAT = AffineTransform.getTranslateInstance(-centerX + tx, -centerY + ty);
            if (isRotatable) {
                portraitAT.rotate(theta * DEGREES_TO_RADIANS, centerX, centerY);
            }
            portraitAT.scale(scale, scale);

            // draw the clipped image
            g.drawImage(bi, portraitAT, null);

            // remove the clip so we can draw the frame unobscured
            g.setClip(xformClip);

            // if the portrait provides a clipping stencil shape, draw it now
            BufferedImage clipStencil = prepareClipStencil();
            if (clipStencil != null) {
                AffineTransform clipAT = AffineTransform.getTranslateInstance(-clipStencil.getWidth() / 2d, -clipStencil.getHeight() / 2d);
                g.drawImage(clipStencil, clipAT, null);
            }

            // draw the image again with alpha, to show a obscured areas as a ghost
            if (isAdjusting) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.33f));
                g.drawImage(bi, portraitAT, null);
                g.setComposite(oldComposite);
            }

            // create a rectangle that frames the portrait; we keep this in a
            // shape so we can use it for hit testing
            imageFrame = new Path2D.Double(new Rectangle2D.Double(0, 0, bi.getWidth(), bi.getHeight()), portraitAT);
            g.setColor(FRAME_COLOR);
            {
                final float fPixelSize = (float) pixelSize;
                g.setStroke(new BasicStroke(fPixelSize, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{fPixelSize * 2f, fPixelSize * 2f}, 0f));
                g.draw(imageFrame);
                g.setStroke(oldStroke);
            }

            if (isRotatable) {
                paintRotationHandle(g);
            }
        }

        // this graphics context is reused to paint the border (if any),
        // so we need to restore these
        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
        g.setTransform(oldAT);
        g.setClip(oldClip);

        // the view to model transform is no longer valid; recompute it if
        // viewToModel is called
        v2m = null;
    }

    private void paintRotationHandle(Graphics2D g) {
        if (rotationHandleUnderCursor != null) {
            g.setPaint(ACTIVE_COLOR);
        }

        // draw in center of image
        Point2D centre = portraitToModel(new Point2D.Double(bi.getWidth() / 2d, bi.getHeight() / 2d));
        double cx = centre.getX();
        double cy = centre.getY();

        Ellipse2D.Double circle = new Ellipse2D.Double(
                cx - (ROT_HANDLE_SIZE / 2d * pixelSize),
                cy - (ROT_HANDLE_SIZE / 2d * pixelSize),
                ROT_HANDLE_SIZE * pixelSize, ROT_HANDLE_SIZE * pixelSize
        );
        g.fill(circle);

        double angle = (theta + 90d) * DEGREES_TO_RADIANS;

        double x2, y2;
        if (rotationHandleUnderCursor != null) {
            x2 = rotationHandleUnderCursor.getX();
            y2 = rotationHandleUnderCursor.getY();
        } else {
            x2 = cx + Math.cos(angle) * ROT_ARM_LENGTH * pixelSize;
            y2 = cy + Math.sin(angle) * ROT_ARM_LENGTH * pixelSize;
        }

        g.setStroke(new BasicStroke((float) pixelSize * 2f));
        g.draw(new Line2D.Double(cx, cy, x2, y2));
    }

    private static final double ROT_HANDLE_SIZE = 6d;
    private static final double ROT_ARM_LENGTH = ROT_HANDLE_SIZE * 1.5d;

    private Color MARGIN_COLOR;
    private Color BACKGROUND_COLOR;
    private static final Color FRAME_COLOR = Color.MAGENTA;
    private static final Color ACTIVE_COLOR = Color.YELLOW;

    /**
     * Determines how much space to show outside the clip bounds; the image
     * width or height (depending on whether the scale factor is width or height
     * bound) is divided by this value, and that amount is added as padding. For
     * example, a divisor of 5 adds 20% of the image size in padding (10% on
     * each edge).
     */
    private static final int MARGIN_DIVISOR = 5;

    private Insets paintInsets;
    private boolean isAdjusting, isRotating, isScaling;
    private int cornerAdjustmentMode;
    private AffineTransform m2v, v2m; // model to view, view to model
    private AffineTransform portraitAT; // transform applied to the portrait within the model
    private Shape imageFrame; // the shape that bounds the portrait image

    private BufferedImage prepareClipStencil() {
        BufferedImage clip = p.getClipStencil();
        if (clip == null) {
            return null;
        }
        if (lastSrcClip != clip) {
            lastSrcClip = clip;
            lastDstClip = new ColorOverlayFilter(MARGIN_COLOR.getRGB()).filter(lastSrcClip, null);
            // create a padded margin of solid colour around the outside
            // to compensate for rounding issues when the stencil is drawn
            int w = clip.getWidth();
            int h = clip.getHeight();
            int padsize = Math.max(4, Math.max(w, h) / 16);
            BufferedImage bi = new BufferedImage(w + padsize * 2, h + padsize * 2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bi.createGraphics();
            try {
                g.setPaint(MARGIN_COLOR);
                g.fillRect(0, 0, bi.getWidth(), padsize);
                g.fillRect(0, padsize, padsize, h);
                g.fillRect(w + padsize, padsize, padsize, h);
                g.fillRect(0, h + padsize, bi.getWidth(), padsize);
                g.drawImage(lastDstClip, padsize, padsize, null);
            } finally {
                g.dispose();
            }
            lastDstClip = bi;
        }
        return lastDstClip;
    }
    private BufferedImage lastSrcClip, lastDstClip;

    // we keep a private copy of the portrait attributes; this way we can
    // allow the user to drag the image around in the control without updating
    // the portrait, so the sheet preview isn't redrawn until the mouse is released
    private double tx, ty;
    private double scale;
    private double theta;
    private boolean isRotatable;

    private double viewScale, pixelSize;

    private void updatePortraitOnMouseRelease() {
        p.setPanX(tx);
        p.setPanY(ty);
        p.setScale(scale);
        if (isRotatable) {
            p.setRotation(theta);
        }
        repaint();
    }

    /**
     * Copy the pan, scale, and angle from the portrait to our variables.
     */
    private void updateLocal() {
        tx = p.getPanX();
        ty = p.getPanY();
        scale = p.getScale();
        if (isRotatable) {
            theta = p.getRotation();
        }
    }

    private Point2D viewToModel(Point p) {
        if (m2v == null) {
            return new Point(0, 0); // disabled
        }
        if (v2m == null) {
            try {
                v2m = m2v.createInverse();
            } catch (NoninvertibleTransformException e) {
                // shouldn't happen since scale can't be 0
                StrangeEons.log.log(Level.SEVERE, null, e);
            }
        }
        return v2m.transform(p, null);
    }

    private Point modelToView(Point2D p) {
        p = m2v.transform(p, null);
        return new Point((int) (p.getX() + 0.5d), (int) (p.getY() + 0.5d));
    }

    private MouseInputListener mouseListener = new MouseInputAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            if (p == null || !isEnabled()) {
                return;
            }
            if (SwingUtilities.isRightMouseButton(e)) {
                // pressing the right button resets the portrait to its default state
                PortraitPanel p = getPortraitPanel();
                if (p != null) {
                    p.setSource(p.getSource());
                }
            } else if (SwingUtilities.isLeftMouseButton(e)) {
                Point2D p = viewToModel(e.getPoint());
                int corn = frameCorner(p);
                cornerAdjustmentMode = corn;
                if ((e.isAltDown() || e.isControlDown()) && isRotatable) {
                    isAdjusting = true;
                    isRotating = true;
                    cornerAdjustmentMode = 0;
                } else if (e.isShiftDown()) {
                    isAdjusting = true;
                    isScaling = true;
                    cornerAdjustmentMode = 0;
                } else {
                    isAdjusting = true;
                }

                if (isAdjusting) {
                    dragStart = e.getPoint();
                    dragStartModel = p;
                    scaleAtDragStart = scale;
                    imageCenterAtDragStart = portraitToModel(new Point2D.Double(bi.getWidth() / 2d, bi.getHeight() / 2d));
                    if (cornerAdjustmentMode == ROT) {
                        // this will make the handle get
                        // drawn in the highlight colour
                        // when the mouse is down
                        rotationHandleUnderCursor = p;
                    }
                    repaint();
                }
            }
        }

        private double scaleAtDragStart;
        private Point2D dragStartModel;
        private Point2D imageCenterAtDragStart;

        @Override
        public void mouseReleased(MouseEvent e) {
            if (isAdjusting) {
                isAdjusting = false;
                isRotating = false;
                isScaling = false;
                rotationHandleUnderCursor = null;
                updatePortraitOnMouseRelease();
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (p == null) {
                return;
            }
            if (isAdjusting) {
                if (isRotating || isScaling) {
                    int dy = e.getY() - dragStart.y;
                    applyScaleOrRotationDelta(dy, isScaling);
                } else if (cornerAdjustmentMode > IN) {
                    if (cornerAdjustmentMode == ROT) {
                        // user is dragging the rotation handle
                        // calculate the new angle
                        Point2D p = viewToModel(e.getPoint());
                        double cx = imageCenterAtDragStart.getX();
                        double cy = imageCenterAtDragStart.getY();
                        theta = Math.atan2(p.getX() - cx, cy - p.getY());

                        // constrain to 45 degrees
                        if (e.isShiftDown() || e.isAltDown()) {
                            theta = Math.round(theta / Math.PI * 4d) * Math.PI / 4d;
                        }

                        theta /= DEGREES_TO_RADIANS;
                        rotationHandleUnderCursor = p;
                    } else {
                        // user is dragging a corner:
                        // determine the dist from the drag start to portrait center,
                        //   current mouse position to portrait center, and use this
                        //   as the factor to adjust the scale by
                        Point2D p = viewToModel(e.getPoint());
                        double oldDist = imageCenterAtDragStart.distance(dragStartModel);
                        double newDist = imageCenterAtDragStart.distance(p);
                        if (oldDist < pixelSize) {
                            oldDist = pixelSize;
                        }
                        scale = scaleAtDragStart * (newDist / oldDist);
                        if (scale < 0.01d) {
                            scale = 0.01d;
                        } else if (scale > 1000d) {
                            scale = 1000d;
                        }
                    }
                } else {
                    Point2D end = viewToModel(e.getPoint());
                    Point2D start = viewToModel(dragStart);

                    tx += end.getX() - start.getX();
                    ty += end.getY() - start.getY();
                }

                dragStart = e.getPoint();
                repaint();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (p == null) {
                return;
            }
            Point2D p = viewToModel(e.getPoint());
            int corn = frameCorner(p);
            setCursor(cursorForCorner(corn));
        }

        private Point dragStart;
    };

    private Point2D rotationHandleUnderCursor;

    private MouseWheelListener wheelListener = new MouseWheelListener() {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (p == null) {
                return;
            }
            applyScaleOrRotationDelta(e.getWheelRotation(), !(e.isAltDown() || e.isControlDown()));
        }
    };

    private void applyScaleOrRotationDelta(int deltaSteps, boolean scaleOp) {
        if (scaleOp) {
            int iScale = (int) (scale * 100d + 0.5d);
            iScale += deltaSteps * 10;

            if (iScale < 1) {
                iScale = 1;
            } else if (iScale > 1_000) {
                iScale = 1_000;
            }

            scale = iScale / 100d;
        } else {
            if (!isRotatable) {
                return;
            }
            theta -= deltaSteps;
        }
        updatePortraitOnMouseRelease();
    }

    private int frameCorner(Point2D p) {
        int c = OUT;

        Point2D.Double corner = new Point2D.Double(0, 0);
        double x2 = bi.getWidth() - 1, y2 = bi.getHeight() - 1;

        if (pointsNearInModel(p, portraitToModel(corner))) {
            c = UL;
        } else {
            corner.setLocation(x2, 0);
            if (pointsNearInModel(p, portraitToModel(corner))) {
                c = UR;
            } else {
                corner.setLocation(0, y2);
                if (pointsNearInModel(p, portraitToModel(corner))) {
                    c = LL;
                } else {
                    corner.setLocation(x2, y2);
                    if (pointsNearInModel(p, portraitToModel(corner))) {
                        c = LR;
                    } else if (isRotatable) {
                        corner.setLocation(x2 / 2d, y2 / 2d);
                        if (pointsNearInModel(p, portraitToModel(corner))) {
                            c = ROT;
                        }
                    }
                }
            }
        }

        if (c == OUT && imageFrame != null && imageFrame.contains(p)) {
            c = IN;
        }
        return c;
    }

    private Point2D portraitToModel(Point2D ptInModel) {
        if (portraitAT == null) {
            StrangeEons.log.info("null portrait transform");
            return new Point2D.Double(0d, 0d);
        }
        return portraitAT.transform(ptInModel, null);
    }

    private boolean pointsNearInModel(Point2D ptInModel, Point2D pt2InModel) {
        // i.e. maxDist^2 * pixelSize^2
        return ptInModel.distanceSq(pt2InModel) < (ROT_HANDLE_SIZE * ROT_HANDLE_SIZE) * (pixelSize * pixelSize);
    }

    private Cursor cursorForCorner(int corner) {
        // if the cursor should be a size drag handle,
        // check if the image is rotated, in which case
        // we may need to choose a rotated version of the cursor, too
        if (corner > IN && corner <= 4) {
            double degrees = theta;
            // adjust so that the flip points are *centered* over the corners
            degrees += 45d;
            int shifts = 0;
            if (degrees >= 90d) {
                shifts = 1;
                if (degrees >= 180d) {
                    shifts = 2;
                }
                if (degrees >= 270d) {
                    shifts = 3;
                }
            }
            if (shifts > 0) {
                corner -= 1;
                corner = (corner + shifts) % 4;
                corner += 1;
            }
        }

        int c;
        switch (corner) {
            case IN:
                c = Cursor.MOVE_CURSOR;
                break;
            case UL:
                c = Cursor.NW_RESIZE_CURSOR;
                break;
            case UR:
                c = Cursor.NE_RESIZE_CURSOR;
                break;
            case LL:
                c = Cursor.SW_RESIZE_CURSOR;
                break;
            case LR:
                c = Cursor.SE_RESIZE_CURSOR;
                break;
            case ROT:
                c = Cursor.HAND_CURSOR;
                break;
            default:
                c = Cursor.DEFAULT_CURSOR;
                break;
        }
        return Cursor.getPredefinedCursor(c);
    }

    // codes for the location of the mouse in the portrait frame:
    // outside, inside, and the four corners
    private static final int OUT = -1, IN = 0, LR = 1, UR = 2, UL = 3, LL = 4, ROT = 5;

    private static final double DEGREES_TO_RADIANS = -0.0174532925d;
}
