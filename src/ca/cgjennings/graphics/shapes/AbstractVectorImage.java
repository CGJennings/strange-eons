package ca.cgjennings.graphics.shapes;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * An abstract base class for creating vector images. Described algorithmically,
 * pure vector images can be scaled to arbitrary sizes without losing definition
 * and becoming blocky as bitmap images do.
 *
 * <p>
 * Concrete subclasses must set the protected members {@link #tx},
 * {@link #ty}, {@link #iw}, and {@link #ih} to appropriate values and implement
 * the {@link #render(java.awt.Graphics2D)} method.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public abstract class AbstractVectorImage implements VectorImage {

    /**
     * Translation required to move left edge of image to x=0.
     */
    protected double tx;
    /**
     * Translation required to move top edge of image to y=0.
     */
    protected double ty;
    /**
     * Width of the image, in an arbitrary unit chosen by the image provider.
     */
    protected double iw;
    /**
     * Height of the image, in an arbitrary unit chosen by the image provider.
     */
    protected double ih;

    protected AbstractVectorImage() {
    }

    /**
     * Returns the width of the image in an unspecified unit. Typically this
     * reflects the "natural" size of the image in the unit used by the
     * designer.
     *
     * @return the image width in image-specific units
     */
    @Override
    public final double getWidth() {
        return iw;
    }

    /**
     * Returns the height of the image in an unspecified unit. Typically this
     * reflects the "natural" size of the image in the unit used by the
     * designer.
     *
     * @return the image height in image-specific units
     */
    @Override
    public final double getHeight() {
        return ih;
    }

    /**
     * Paints the image at the specified location in the provided graphics
     * context.
     *
     * @param g the graphics context to render into
     * @param x the x-coordinate to render at
     * @param y the y-coordinate to render at
     */
    @Override
    public final void paint(Graphics2D g, double x, double y) {
        if (g == null) {
            throw new NullPointerException("g");
        }
        if (iw == 0d || ih == 0d) {
            return;
        }

        final Shape uc = g.getClip();
        final AffineTransform at = g.getTransform();

        g.translate(tx + x, ty + y);
        render(g);

        g.setTransform(at);
        g.setClip(uc);
    }

    /**
     * Paints the image at the specified location and size in the user
     * coordinates of the provided graphics context. If the {@code fitToSize}
     * parameter is {@code true}, then the aspect ratio of the vector image will
     * be maintained. If the aspect ratio of the vector image does not match the
     * specified width and height, then the vector image will be scaled to just
     * fit within the specified size, and centered over the drawing area.
     *
     * @param g the graphics context to render into
     * @param x the x-coordinate to render at
     * @param y the y-coordinate to render at
     * @param width the width to paint the image at
     * @param height the height to paint the image at
     * @param fitToSize if {@code true}, the aspect ratio of the vector image
     * will be maintained
     */
    @Override
    public final void paint(Graphics2D g, double x, double y, double width, double height, boolean fitToSize) {
        if (g == null) {
            throw new NullPointerException("g");
        }
        if (iw == 0d || ih == 0d || width <= 0d || height <= 0d) {
            return;
        }

        double hscale = width / iw;
        double vscale = height / ih;

        if (fitToSize) {
            if (hscale < vscale) {
                final double newHeight = ih * hscale;
                y += (height - newHeight) / 2d;
                vscale = hscale;
            } else {
                final double newWidth = iw * vscale;
                x += (width - newWidth) / 2d;
                hscale = vscale;
            }
        }

        final Shape uc = g.getClip();
        final AffineTransform at = g.getTransform();

        g.translate(x, y);
        g.scale(hscale, vscale);
        g.translate(tx, ty);
        render(g);

        g.setTransform(at);
        g.setClip(uc);
    }

    /**
     * Paints the image to cover the specified rectangle in the user coordinates
     * of the provided graphics context. If the {@code fitToSize} parameter is
     * {@code true}, then the aspect ratio of the vector image will be
     * maintained. If the aspect ratio of the vector image does not match the
     * specified width and height, then the vector image will be scaled to just
     * fit within the specified size, and centered over the drawing area.
     *
     * @param g the graphics context to render into
     * @param paintRectangle the rectangle that the image should cover
     * @param fitToSize if {@code true}, the aspect ratio of the vector image
     * will be maintained
     */
    @Override
    public final void paint(Graphics2D g, Rectangle2D paintRectangle, boolean fitToSize) {
        paint(g,
                paintRectangle.getX(), paintRectangle.getY(),
                paintRectangle.getWidth(), paintRectangle.getHeight(),
                fitToSize
        );
    }

    /**
     * Creates a bitmap image from the vector image at the specified resolution,
     * assuming that the image width and height are measured in points.
     *
     * @param ppi the resolution to render the image at
     * @return a new bitmap image at the requested resolution
     */
    @Override
    public final BufferedImage createRasterImage(double ppi) {
        int w = (int) Math.ceil(getWidth() * ppi / 72d);
        int h = (int) Math.ceil(getHeight() * ppi / 72d);
        if (w < 1) {
            w = 1;
        }
        if (h < 1) {
            h = 1;
        }

        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        try {
            double scale = w / getWidth();
            g.scale(scale, scale);
            g.translate(tx, ty);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            render(g);
        } finally {
            g.dispose();
        }

        return bi;
    }

    /**
     * Creates a bitmap image from the vector image with the specified width and
     * height (in pixels). If the {@code fitToSize} parameter is {@code true},
     * then the aspect ratio of the vector image will be maintained. If the
     * aspect ratio of the vector image does not match the specified width and
     * height, then the vector image will be scaled to just fit within the
     * specified size, and centered over the drawing area.
     *
     * @param width the image width, in pixels
     * @param height the image height, in pixels
     * @param fitToSize if {@code true}, the aspect ratio of the vector image
     * will be maintained
     * @return a rendering of the vector image at the specified size
     */
    @Override
    public final BufferedImage createRasterImage(int width, int height, boolean fitToSize) {
        if (width < 1) {
            width = 1;
        }
        if (height < 1) {
            height = 1;
        }
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            paint(g, 0, 0, width, height, fitToSize);
        } finally {
            g.dispose();
        }
        return bi;
    }

    /**
     * Renders the vector image into the specified graphics context. The upper
     * left corner of the image should be at ({@linkplain #tx -tx},
     * {@linkplain #ty -ty}) and cover an area with width {@link #iw} and height
     * {@link #ih}. Any changes to the state of the graphics context (stroke,
     * paint, clip, etc.) must be restored before the method returns.
     *
     * @param g the graphics context to render the image into
     */
    protected abstract void render(Graphics2D g);
}
