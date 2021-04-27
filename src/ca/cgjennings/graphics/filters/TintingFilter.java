package ca.cgjennings.graphics.filters;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

/**
 * An interface implemented by filters that filter an image by adjusting them in
 * the HSB colour space. Classes that implement this interface can be used with
 * a {@link TintCache} to optimize performance when a tinted image is drawn
 * repeatedly with infrequent factor changes.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface TintingFilter extends BufferedImageOp {

    /**
     * Returns the hue adjustment factor.
     *
     * @return hue factor
     */
    public float getHFactor();

    /**
     * Returns the saturation adjustment factor.
     *
     * @return saturation factor
     */
    public float getSFactor();

    /**
     * Returns the brightness adjustment factor.
     *
     * @return brightness factor
     */
    public float getBFactor();

    /**
     * Sets the factors that will be used during tinting. The exact effect
     * depends on the particular filter.
     *
     * @param h hue factor
     * @param s saturation factor
     * @param b brightness factor
     */
    public void setFactors(float h, float s, float b);

    /**
     * Returns <code>true</code> if applying this filter would have no effect.
     * This can be used as an optimization hint.
     *
     * @return <code>true</code> if a filtered image would not change
     */
    public boolean isIdentity();

    /**
     * Apply the filter to <code>source</code>, storing the result in
     * <code>dest</code>.
     *
     * @param source the source image
     * @param dest the destination image (may be <code>null</code>)
     * @return the destination image
     */
    @Override
    public BufferedImage filter(BufferedImage source, BufferedImage dest);
}
