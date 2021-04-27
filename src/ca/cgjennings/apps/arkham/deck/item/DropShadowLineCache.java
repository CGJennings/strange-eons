package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Used to cache a line's shadow in an image instead of recreating it on each
 * view draw.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
class DropShadowLineCache {

    protected Line pi;

    public DropShadowLineCache(Line pi) {
        this.pi = pi;
    }

    public void invalidate() {
        image = null;
    }

    public void paintDropShadow(Graphics2D g, RenderTarget target, double resHint) {
        this.target = target;
        verifyCache();
        if (image == null) {
            createCacheImage();
        }
        g.drawImage(image, at, null);
    }

    /**
     * createCacheImage sets this to an image for the shadow
     */
    protected BufferedImage image;
    /**
     * createCacheImage sets this to a transform that will paint it correctly in
     * the deck
     */
    protected AffineTransform at;

    private double lineW, _lineW, cx, _cx, cy, _cy, x1,
            _x1, y1, _y1, x2, _x2, y2, _y2, _dpi;
    private DashPattern _dash;
    private RenderTarget target, _target;
    private int _end;

    protected static final boolean ch(double a, double b) {
        return Math.abs(a - b) > 0.000001;
    }

    protected void verifyCache() {
        x1 = pi.getX();
        y1 = pi.getY();
        x2 = pi.getX2() - x1;
        y2 = pi.getY2() - y1;
        lineW = pi.getLineWidth();

        if (ch(x2, _x2)) {
            image = null;
        }
        if (ch(y2, _y2)) {
            image = null;
        }

        // TODO: should be handled through subclassing
        if (pi instanceof Curve) {
            Curve c = (Curve) pi;
            cx = c.getCX() - x1;
            cy = c.getCY() - y1;
            if (ch(cx, _cx)) {
                image = null;
            }
            if (ch(cy, _cy)) {
                image = null;
            }
            _cx = cx;
            _cy = cy;
        }

        if (_lineW != lineW) {
            image = null;
        }
        if (_target != target) {
            image = null;
        }
        if (_end != pi.getLineCap().toInt()) {
            image = null;
        }
        if (_dash != pi.getLineDashPattern()) {
            image = null;
        }

        // the translation of the entire shape has changed
        // but it still has the same shape: just update the
        // transform
        if ((ch(x1, _x1) || ch(y1, _y1)) && image != null) {
            Rectangle2D.Double bounds = pi.getRectangle();
            double shadowSize = lineW;
            at = AffineTransform.getTranslateInstance(
                    bounds.x - shadowSize, bounds.y - shadowSize
            );
            at.concatenate(AffineTransform.getScaleInstance(
                    72d / _dpi, 72d / _dpi
            ));
        }

        _lineW = lineW;
        _x1 = x1;
        _y1 = y1;
        _x2 = x2;
        _y2 = y2;
        _target = target;
        _end = pi.getLineCap().toInt();
    }

    protected void createCacheImage() {
        double dpi;
        switch (target) {
            case FAST_PREVIEW:
                dpi = 36;
                break;
            case PREVIEW:
                dpi = 72;
                break;
            case EXPORT:
            case PRINT:
                dpi = 150;
                break;
            default:
                throw new AssertionError();
        }
        double pt2px = dpi / 72d;

        double shadowSize = lineW;
        Rectangle2D.Double bounds = pi.getRectangle();
        int pShadowSize = (int) Math.ceil(shadowSize * pt2px);
        int pWidth = (int) Math.ceil(bounds.width * pt2px);
        int pHeight = (int) Math.ceil(bounds.height * pt2px);

        image = new BufferedImage(pWidth + pShadowSize * 2, pHeight + pShadowSize * 2, BufferedImage.TYPE_INT_ARGB);

        Graphics2D sg = image.createGraphics();
        try {
            sg.setPaint(Color.BLACK);
            sg.scale(pt2px, pt2px);
            sg.translate(shadowSize - bounds.x, shadowSize - bounds.y);
            pi.paintShadowCurve(sg, Color.BLACK);
        } finally {
            sg.dispose();
        }

        OldDropShadow ds = new DropShadow(Color.BLACK, pShadowSize, 2f);
        image = ds.createShadowImage(image);

        at = AffineTransform.getTranslateInstance(
                bounds.x - shadowSize, bounds.y - shadowSize
        );
        at.concatenate(AffineTransform.getScaleInstance(
                72d / dpi, 72d / dpi
        ));
        _dpi = dpi;
    }
}
