package ca.cgjennings.ui.theme;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.Painter;

/**
 * This painter wraps another painter, buffering the wrapped painter so that it
 * only needs to be called when the width or height changes. This can
 * significantly increase painting speed if the wrapped painter is
 * computationally expensive. To use this class, simply pass the painter that
 * you wish to wrap to the constructor.
 */
public class CachingPainter implements Painter {

    private Painter painter;
    private BufferedImage image;
    private int lastWidth;
    private int lastHeight;
    private boolean paintOnce;

    /**
     * Cache painting calls for the specified painter, only using the wrapped
     * painter when the size of the paint area changes.
     *
     * @param painter the painter to wrap
     */
    public CachingPainter(Painter painter) {
        this.painter = painter;
    }

    /**
     * Using this constructor will create a cache image at a single standard
     * size, after which only that image will be used (resizing if necessary).
     * The wrapped painter will never be called again.
     *
     * @param painter the painter to wrap
     * @param standardWidth the width of the fixed image to create
     * @param standardHeight the height of the fixed image to create
     */
    public CachingPainter(Painter painter, int standardWidth, int standardHeight) {
        this(painter);
        paintOnce = true;
        createBuffer(null, standardWidth, standardHeight);
    }

    /**
     * {@inheritDoc }
     *
     * @param g a graphics context for painting
     * @param width the width of the visible console area
     * @param height the height of the visible console area
     */
    @Override
    public void paint(Graphics2D g, Object c, int width, int height) {
        if (paintOnce) {
            g.drawImage(image, 0, 0, width, height, null);
        } else {
            if (image == null || lastWidth != width || lastHeight != height) {
                createBuffer(c, width, height);
                lastWidth = width;
                lastHeight = height;
            }
            g.drawImage(image, 0, 0, null);
        }
    }

    private void createBuffer(Object c, int width, int height) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D ig = image.createGraphics();
        try {
            painter.paint(ig, c, width, height);
        } finally {
            ig.dispose();
        }
    }

    /**
     * Invalidates any cached version of the paint area, so that the next call
     * to paint will have to be passed through to the wrapped painter. This has
     * no effect if the "paint once" constructor was used.
     */
    public void invalidate() {
        if (paintOnce) {
            return;
        }
        image = null;
    }

    /**
     * Returns the wrapped painter that will be delegated to.
     *
     * @return the wrapped painter
     */
    public Painter getWrappedPainter() {
        return painter;
    }
}
