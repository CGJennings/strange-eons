package ca.cgjennings.graphics.composites;

import ca.cgjennings.graphics.ImageUtilities;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.lang.reflect.InvocationTargetException;

/**
 * An abstract base class for composites that support compositing (A)RGB image
 * data. Concrete subclasses must implement {@link #createContext} to return a
 * concrete subclass of {@link AbstractCompositeContext}. Concrete subclasses
 * will transparently take advantage of multiple CPUs to increase performance.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public abstract class AbstractARGBComposite implements Composite {

    protected float alpha;

    public AbstractARGBComposite() {
        this(1f);
    }

    public AbstractARGBComposite(float alpha) {
        if (alpha < 0f || alpha > 1f) {
            throw new IllegalArgumentException("alpha must be between 0 and 1 inclusive: " + alpha);
        }
        this.alpha = alpha;
    }

    public AbstractARGBComposite derive(float alpha) {
        if (alpha < 0f || alpha > 1f) {
            throw new IllegalArgumentException("alpha must be between 0 and 1 inclusive: " + alpha);
        }
        AbstractARGBComposite comp;
        try {
            comp = getClass().getConstructor().newInstance();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException("unable to instantiate class for derivation", ex);
        }
        comp.alpha = alpha;
        return comp;
    }

    public float getAlpha() {
        return alpha;
    }

    /**
     * Blends one image directly into another using this composite. If
     * {@code destination} is {@code null}, a new compatible image
     * will be created. If the source image is not in a format supported format,
     * a copy of the image, suitably converted, will be used. This will also be
     * done for a non-{@code null} destination in an unsupported format; in
     * this case the converted destination is returned rather than the one
     * passed to the method.
     *
     * @param source the source image
     * @param x the x-offset into the destination at which to draw the source
     * @param y the y-offset into the destination at which to draw the source
     * @param destination the image to be painted into
     * @return the destination image
     */
    public BufferedImage compose(BufferedImage source, int x, int y, BufferedImage destination) {
        source = convertIfRequired(source);
        if (destination == null) {
            destination = ImageUtilities.createCompatibleIntRGBFormat(source);
        } else {
            destination = convertIfRequired(source);
        }
        Graphics2D g = null;
        try {
            g = destination.createGraphics();
            g.setComposite(this);
            g.drawImage(source, x, y, null);
        } finally {
            if (g != null) {
                g.dispose();
            }
        }
        return destination;
    }

    private static BufferedImage convertIfRequired(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_ARGB_PRE) {
            source = ImageUtilities.ensureImageHasType(source, BufferedImage.TYPE_INT_ARGB);
        } else {
            source = ImageUtilities.ensureIntRGBFormat(source);
        }
        return source;
    }

    @Override
    public abstract AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints);
}
