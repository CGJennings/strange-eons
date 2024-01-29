package ca.cgjennings.graphics.paints;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

/**
 * Paints shapes with a pattern of checkered boxes in two colours.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.00
 */
public class CheckeredPaint implements Paint {

    private TexturePaint paint;

    public CheckeredPaint() {
        this(8, Color.LIGHT_GRAY, Color.WHITE);
    }

    public CheckeredPaint(int boxSize) {
        this(boxSize, Color.LIGHT_GRAY, Color.WHITE);
    }

    public CheckeredPaint(Color color1, Color color2) {
        this(8, color1, color2);
    }

    private int boxSize;
    private Color color1, color2;

    public CheckeredPaint(int boxSize, Color color1, Color color2) {
        if (boxSize < 1) {
            throw new IllegalArgumentException("boxSize must be at least 1");
        }
        if (color1 == null || color2 == null) {
            throw new NullPointerException("color");
        }

        this.boxSize = boxSize;
        this.color1 = color1;
        this.color2 = color2;

        int type = color1.getAlpha() < 255 || color2.getAlpha() < 255
                ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;

        // if the box size is small, speed up painting by creating a larger image
        // and tiling the pattern over it; this can provide a dramatic performance boost
        final int TILES = Math.max(1, 128 / boxSize);

        BufferedImage texture = new BufferedImage(boxSize * 2 * TILES, boxSize * 2 * TILES, type);
        Graphics2D g = texture.createGraphics();
        try {
            for (int row = 0; row < TILES; ++row) {
                int y = boxSize * 2 * row;
                for (int col = 0; col < TILES; ++col) {
                    int x = boxSize * 2 * col;
                    g.setColor(color1);
                    g.fillRect(x, y, boxSize, boxSize);
                    g.fillRect(x + boxSize, y + boxSize, boxSize, boxSize);
                    g.setColor(color2);
                    g.fillRect(x + boxSize, y, boxSize, boxSize);
                    g.fillRect(x, y + boxSize, boxSize, boxSize);
                }
            }
        } finally {
            g.dispose();
        }
        paint = new TexturePaint(texture, new Rectangle2D.Double(0d, 0d, boxSize * TILES * 2d, boxSize * TILES * 2d));
    }

    @Override
    public final PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
        return paint.createContext(cm, deviceBounds, userBounds, xform, hints);
    }

    @Override
    public final int getTransparency() {
        return paint.getTransparency();
    }

    public int getBoxSize() {
        return boxSize;
    }

    public Color getColor1() {
        return color1;
    }

    public Color getColor2() {
        return color2;
    }
}
