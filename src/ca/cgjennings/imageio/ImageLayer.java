package ca.cgjennings.imageio;

import ca.cgjennings.graphics.composites.BlendMode;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * A layer in a multilayer image.
 */
public final class ImageLayer {
    private int x;
    private int y;
    private BufferedImage image;
    private float alpha;
    private boolean visible = true;
    private BlendingMode blendMode;

    /**
     * Layer blending modes used in PSD images.
     */
    public static enum BlendingMode {
        NORMAL,
        DARKEN,
        LIGHTEN,
        HUE,
        SATURATION,
        COLOR,
        LUMINOSITY,
        MULTIPLY,
        SCREEN,
        DISSOLVE,
        OVERLAY,
        HARD_LIGHT,
        SOFT_LIGHT,
        DIFFERENCE,
        UNKNOWN;

        public Composite createGraphicsComposite(float alpha) {
            switch (this) {
                case DARKEN:
                    return BlendMode.Darken.derive(alpha);
                case LIGHTEN:
                    return BlendMode.Lighten.derive(alpha);
                case HUE:
                    return BlendMode.Hue.derive(alpha);
                case SATURATION:
                    return BlendMode.Saturation.derive(alpha);
                case COLOR:
                    return BlendMode.Color.derive(alpha);
                case LUMINOSITY:
                    return BlendMode.Luminosity.derive(alpha);
                case MULTIPLY:
                    return BlendMode.Multiply.derive(alpha);
                case SCREEN:
                    return BlendMode.Screen.derive(alpha);
                case OVERLAY:
                    return BlendMode.Overlay.derive(alpha);
                case HARD_LIGHT:
                    return BlendMode.HardLight.derive(alpha);
                case SOFT_LIGHT:
                    return BlendMode.SoftLight.derive(alpha);
                case DIFFERENCE:
                    return BlendMode.Difference.derive(alpha);
                case DISSOLVE:
                default:
                    return AlphaComposite.SrcOver.derive(alpha);
            }
        }
    }

    public ImageLayer(BufferedImage image) {
        this(image, 0, 0);
    }

    public ImageLayer(BufferedImage image, int x, int y) {
        this(image, x, y, 1f);
    }

    public ImageLayer(BufferedImage image, int x, int y, float alpha) {
        this(image, x, y, 1f, BlendingMode.NORMAL);
    }

    public ImageLayer(BufferedImage image, int x, int y, float alpha, BlendingMode blendMode) {
        setImage(image);
        setX(x);
        setY(y);
        setAlpha(alpha);
        setBlendMode(blendMode);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public BufferedImage getImage() {
        return image;
    }

    public float getAlpha() {
        return alpha;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setImage(BufferedImage image) {
        if (image == null) {
            throw new NullPointerException("image");
        }
        this.image = image;
    }

    public void setAlpha(float alpha) {
        if (alpha < 0f || alpha > 1f) {
            throw new IllegalArgumentException("alpha outside of 0 to 1: " + alpha);
        }
        this.alpha = alpha;
    }

    public BlendingMode getBlendMode() {
        return blendMode;
    }

    public void setBlendMode(BlendingMode blendMode) {
        this.blendMode = blendMode;
    }

    public int getWidth() {
        return image == null ? 0 : image.getWidth();
    }

    public int getHeight() {
        return image == null ? 0 : image.getHeight();
    }

    public Point getLocation() {
        return new Point(getX(), getY());
    }

    public void setLocation(Point p) {
        setX(p.x);
        setY(p.y);
    }

    public Rectangle getRectangle() {
        return new Rectangle(getX(), getY(), getWidth(), getHeight());
    }

    public Dimension getSize() {
        return new Dimension(getWidth(), getHeight());
    }

    public void paint(Graphics2D g) {
        if (alpha == 0f) {
            return;
        }

        Composite oldComp = g.getComposite();
        try {
            g.setComposite(getBlendMode().createGraphicsComposite(alpha));
            g.drawImage(getImage(), getX(), getY(), null);
        } finally {
            g.setComposite(oldComp);
        }
    }

    public BufferedImage createStyledImage() {
        BufferedImage si = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = null;
        try {
            g = si.createGraphics();
            paint(g);
        } finally {
            if (g != null) {
                g.dispose();
            }
        }
        return si;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
