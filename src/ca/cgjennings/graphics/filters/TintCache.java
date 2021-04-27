package ca.cgjennings.graphics.filters;

import ca.cgjennings.graphics.ImageUtilities;
import java.awt.image.BufferedImage;

/**
 * A tint cache applies a {@link TintFilter} to an image. It caches its most
 * recent result, and if a tinted image is requested with the same parameters it
 * returns the cached version instead of reapplying the filter.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class TintCache {

    protected TintingFilter filter;
    protected BufferedImage source;
    protected BufferedImage cache;
    protected float h, s, b;

    public TintCache(TintingFilter filter) {
        this(filter, null);
    }

    public TintCache(TintingFilter filter, BufferedImage sourceImage) {
        this.filter = filter;
        source = sourceImage;
    }

    /**
     * Returns a tinted version of the current image, using a cached result if
     * possible.
     *
     * @return a tinted version of the current image
     */
    public BufferedImage getTintedImage() {
        // "fail" fast on identity transform; if the default factors are an
        // identity transform, we might not ever even need to allocate a cache
        // image
        if (filter.isIdentity()) {
            return source;
        }

        final float fh = filter.getHFactor();
        final float fs = filter.getSFactor();
        final float fb = filter.getBFactor();
        if (cache == null || h != fh || s != fs || b != fb) {
            if (cache == null) {
                cache = ImageUtilities.createCompatibleIntRGBFormat(source);
            }
            filter.filter(source, cache);
            h = fh;
            s = fs;
            b = fb;
        }
        return cache;
    }

    /**
     * Returns the filter invoked by this cache.
     *
     * @return the filter used to create result images
     */
    public TintingFilter getFilter() {
        return filter;
    }

    /**
     * Returns the current source image.
     *
     * @return the source image
     */
    public BufferedImage getImage() {
        return source;
    }

    /**
     * Update the source image to use for tinting. If <code>im</code> is
     * different from the current image, it replaces the current image and
     * invalidates the cache.
     *
     * @param im the new source image
     */
    public void setImage(BufferedImage im) {
        if (im != source) {
            source = im;
            cache = null;
        }
    }

    /**
     * Indirectly set the adjustment factors of the filter.
     * This is a convenience. It does not matter if you set the
     * factors directly on the filter or through this method.
     * 
     * @param h the hue adjustment
     * @param s the saturation adjustment
     * @param b the brightness adjustment
     */
    final public void setFactors(float h, float s, float b) {
        filter.setFactors(h, s, b);
    }

    public float getHFactor() {
        return filter.getHFactor();
    }

    public float getSFactor() {
        return filter.getSFactor();
    }

    public float getBFactor() {
        return filter.getBFactor();
    }

    public boolean isIdentity() {
        return filter.isIdentity();
    }
}
