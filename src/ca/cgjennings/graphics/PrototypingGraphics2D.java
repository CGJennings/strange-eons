package ca.cgjennings.graphics;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;
import java.util.Objects;

/**
 * A graphics context that selectively renders different classes of content for
 * speed or prototyping purposes. For example, it could be set to draw only text
 * to save the ink that would be spent printing background graphics.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class PrototypingGraphics2D extends Graphics2D {

    private Graphics2D g;
    final private boolean drawText;
    final private boolean drawImages;
    final private boolean strokeShapes;
    final private boolean fillShapes;
    final private boolean clearRects;

    /**
     * Creates a new prototyping graphics context with default options:
     * text and shapes are drawn, but images are not.
     *
     * @param unrestrictedGraphics a graphics context to modify
     */
    public PrototypingGraphics2D(Graphics2D unrestrictedGraphics) {
        this(unrestrictedGraphics, true, false, true, true, true);
    }

    /**
     * Creates a new prototyping graphics context that selectively renders
     * the specified elements.
     *
     * @param unrestrictedGraphics a graphics context to modify
     * @param drawText if true, regular text is rendered
     * @param drawImages if true, bitmap images are rendered
     * @param strokeShapes if true, lines, polygons, and shape outlines are drawn
     * @param fillShapes if true, polygons and other shapes are filled
     * @param clearRects if true, regions are cleared when {@link #clearRect} is called
     */
    public PrototypingGraphics2D(Graphics2D unrestrictedGraphics, boolean drawText, boolean drawImages, boolean strokeShapes, boolean fillShapes, boolean clearRects) {
        g = Objects.requireNonNull(unrestrictedGraphics);
        this.drawText = drawText;
        this.drawImages = drawImages;
        this.strokeShapes = strokeShapes;
        this.fillShapes = fillShapes;
        this.clearRects = clearRects;
    }

    /**
     * Returns the graphics instance that this wraps. This can be used to
     * ensure that some content is always rendered.
     * @return the non-prototype graphics that backs this instance
     */
    public Graphics2D getUnrestrictedGraphics() {
        return g;
    }

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        if (drawImages) {
            return g.drawImage(img, xform, obs);
        }
        return true;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        if (drawImages) {
            return g.drawImage(img, x, y, observer);
        }
        return true;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        if (drawImages) {
            return g.drawImage(img, x, y, width, height, observer);
        }
        return true;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        if (drawImages) {
            return g.drawImage(img, x, y, bgcolor, observer);
        }
        return true;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        if (drawImages) {
            return g.drawImage(img, x, y, width, height, bgcolor, observer);
        }
        return true;
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        if (drawImages) {
            return g.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
        }
        return true;
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
        if (drawImages) {
            return g.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
        }
        return true;
    }

    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
        if (drawImages) {
            g.drawImage(img, op, x, y);
        }
    }

    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        if (drawImages) {
            g.drawRenderedImage(img, xform);
        }
    }

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        if (drawImages) {
            g.drawRenderableImage(img, xform);
        }
    }

    @Override
    public void drawString(String str, int x, int y) {
        if (drawText) {
            g.drawString(str, x, y);
        }
    }

    @Override
    public void drawString(String str, float x, float y) {
        if (drawText) {
            g.drawString(str, x, y);
        }
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        if (drawText) {
            g.drawString(iterator, x, y);
        }
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        if (drawText) {
            g.drawString(iterator, x, y);
        }
    }

    @Override
    public void drawBytes(byte[] data, int offset, int length, int x, int y) {
        if (drawText) {
            g.drawBytes(data, offset, length, x, y);
        }
    }

    @Override
    public void drawChars(char[] data, int offset, int length, int x, int y) {
        if (drawText) {
            g.drawChars(data, offset, length, x, y);
        }
    }

    @Override
    public void drawGlyphVector(GlyphVector gv, float x, float y) {
        if (drawText) {
            g.drawGlyphVector(gv, x, y);
        }
    }

    @Override
    public void draw(Shape s) {
        if (strokeShapes) {
            g.draw(s);
        }
    }

    @Override
    public void fill(Shape s) {
        if (fillShapes) {
            g.fill(s);
        }
    }

    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        return g.hit(rect, s, onStroke);
    }

    @Override
    public boolean hitClip(int x, int y, int width, int height) {
        return g.hitClip(x, y, width, height);
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return g.getDeviceConfiguration();
    }

    @Override
    public void setComposite(Composite comp) {
        g.setComposite(comp);
    }

    @Override
    public void setPaint(Paint paint) {
        g.setPaint(paint);
    }

    @Override
    public void setStroke(Stroke s) {
        g.setStroke(s);
    }

    @Override
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
        g.setRenderingHint(hintKey, hintValue);
    }

    @Override
    public Object getRenderingHint(RenderingHints.Key hintKey) {
        return g.getRenderingHint(hintKey);
    }

    @Override
    public void setRenderingHints(Map<?, ?> hints) {
        g.setRenderingHints(hints);
    }

    @Override
    public void addRenderingHints(Map<?, ?> hints) {
        g.addRenderingHints(hints);
    }

    @Override
    public RenderingHints getRenderingHints() {
        return g.getRenderingHints();
    }

    @Override
    public void translate(int x, int y) {
        g.translate(x, y);
    }

    @Override
    public void translate(double tx, double ty) {
        g.translate(tx, ty);
    }

    @Override
    public void rotate(double theta) {
        g.rotate(theta);
    }

    @Override
    public void rotate(double theta, double x, double y) {
        g.rotate(theta, x, y);
    }

    @Override
    public void scale(double sx, double sy) {
        g.scale(sx, sy);
    }

    @Override
    public void shear(double shx, double shy) {
        g.shear(shx, shy);
    }

    @Override
    public void transform(AffineTransform Tx) {
        g.transform(Tx);
    }

    @Override
    public void setTransform(AffineTransform Tx) {
        g.setTransform(Tx);
    }

    @Override
    public AffineTransform getTransform() {
        return g.getTransform();
    }

    @Override
    public Paint getPaint() {
        return g.getPaint();
    }

    @Override
    public Composite getComposite() {
        return g.getComposite();
    }

    @Override
    public void setBackground(Color color) {
        g.setBackground(color);
    }

    @Override
    public Color getBackground() {
        return g.getBackground();
    }

    @Override
    public Stroke getStroke() {
        return g.getStroke();
    }

    @Override
    public void clip(Shape s) {
        g.clip(s);
    }

    @Override
    public FontRenderContext getFontRenderContext() {
        return g.getFontRenderContext();
    }

    @Override
    public Graphics create() {
        return new PrototypingGraphics2D((Graphics2D) g.create(), drawText, drawImages, strokeShapes, fillShapes, clearRects);
    }

    @Override
    public Color getColor() {
        return g.getColor();
    }

    @Override
    public void setColor(Color c) {
        g.setColor(c);
    }

    @Override
    public void setPaintMode() {
        g.setPaintMode();
    }

    @Override
    public void setXORMode(Color c1) {
        g.setXORMode(c1);
    }

    @Override
    public Font getFont() {
        return g.getFont();
    }

    @Override
    public void setFont(Font font) {
        g.setFont(font);
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        return g.getFontMetrics(f);
    }

    @Override
    public FontMetrics getFontMetrics() {
        return g.getFontMetrics();
    }

    @Override
    public Rectangle getClipBounds() {
        return g.getClipBounds();
    }

    @Override
    public Rectangle getClipBounds(Rectangle r) {
        return g.getClipBounds();
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        g.clipRect(x, y, width, height);
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        g.setClip(x, y, width, height);
    }

    @Override
    public Shape getClip() {
        return g.getClip();
    }

    @Override
    public void setClip(Shape clip) {
        g.setClip(clip);
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        g.copyArea(x, y, width, height, dx, dy);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        if (strokeShapes) {
            g.drawLine(x1, y1, x2, y2);
        }
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        if (strokeShapes) {
            g.drawRect(x, y, width, height);
        }
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        if (fillShapes) {
            g.fillRect(x, y, width, height);
        }
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        if (clearRects) {
            g.clearRect(x, y, width, height);
        }
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        if (strokeShapes) {
            g.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
        }
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        if (fillShapes) {
            g.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
        }
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        if (strokeShapes) {
            g.drawOval(x, y, width, height);
        }
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        if (fillShapes) {
            g.fillOval(x, y, width, height);
        }
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        if (strokeShapes) {
            g.drawArc(x, y, width, height, startAngle, arcAngle);
        }
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        if (fillShapes) {
            g.fillArc(x, y, width, height, startAngle, arcAngle);
        }
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        if (strokeShapes) {
            g.drawPolyline(xPoints, yPoints, nPoints);
        }
    }

    @Override
    public void fillPolygon(Polygon p) {
        if (fillShapes) {
            g.fillPolygon(p);
        }
    }

    @Override
    public void drawPolygon(Polygon p) {
        if (strokeShapes) {
            g.drawPolygon(p);
        }
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        if (strokeShapes) {
            g.drawPolygon(xPoints, yPoints, nPoints);
        }
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        if (fillShapes) {
            g.fillPolygon(xPoints, yPoints, nPoints);
        }
    }

    /**
     * Disposes of the underlying, unrestricted graphics context:
     * 
     * <p>{@inheritDoc}
     */
    @Override
    public void dispose() {
        g.dispose();
        g = null;
    }

    @Override
    public String toString() {
        return "PrototypingGraphics2D{drawText=" + drawText
                + ", drawImages=" + drawImages + ", strokeShapes="
                + strokeShapes + ", fillShapes=" + fillShapes
                + ", clearRects=" + clearRects + '}';
    }
}