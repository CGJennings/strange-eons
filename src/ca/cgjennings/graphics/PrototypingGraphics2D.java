package ca.cgjennings.graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;

/**
 * A graphics context that selectively renders different classes of content for
 * speed or prototyping purposes. For example, it could be set to draw only text
 * to save the ink that would be spent printing background graphics.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class PrototypingGraphics2D extends AbstractGraphics2DAdapter {
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
        super(unrestrictedGraphics);

        this.drawText = drawText;
        this.drawImages = drawImages;
        this.strokeShapes = strokeShapes;
        this.fillShapes = fillShapes;
        this.clearRects = clearRects;
    }
    
    @Override
    protected PrototypingGraphics2D createImpl(Graphics2D newG) {
        return new PrototypingGraphics2D(newG, drawText, drawImages, strokeShapes, fillShapes, clearRects);
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

    @Override
    public String toString() {
        return "PrototypingGraphics2D{drawText=" + drawText
                + ", drawImages=" + drawImages + ", strokeShapes="
                + strokeShapes + ", fillShapes=" + fillShapes
                + ", clearRects=" + clearRects + '}';
    }
}