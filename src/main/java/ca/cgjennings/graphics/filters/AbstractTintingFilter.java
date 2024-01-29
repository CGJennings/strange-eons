package ca.cgjennings.graphics.filters;

/**
 * An abstract base class for {@link TintingFilter}s.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public abstract class AbstractTintingFilter extends AbstractPixelwiseFilter implements TintingFilter {

    protected float hFactor, sFactor, bFactor;

    public AbstractTintingFilter() {
        this(0f, 1f, 1f);
    }

    public AbstractTintingFilter(float hFactor, float sFactor, float bFactor) {
        setFactors(hFactor, sFactor, bFactor);
    }

    @Override
    public float getHFactor() {
        return hFactor;
    }

    @Override
    public float getSFactor() {
        return sFactor;
    }

    @Override
    public float getBFactor() {
        return bFactor;
    }

    public void setHFactor(float hFactor) {
        setFactors(hFactor, sFactor, bFactor);
    }

    public void setSFactor(float sFactor) {
        setFactors(hFactor, sFactor, bFactor);
    }

    public void setBFactor(float bFactor) {
        setFactors(hFactor, sFactor, bFactor);
    }

    /**
     * Sets the hue, saturation, and brightness factors to use for tinting.
     * <p>
     * Implementation Note: if saturation or brightness are less than 0, they
     * are clamped to 0. Values over 1 are not clamped. If your subclass
     * requires that saturation or brightness are clamped at 1, override this
     * method to do so, then call the super implementation.
     *
     * @param h the hue factor
     * @param s the saturation factor
     * @param b the brightness factor
     */
    @Override
    public void setFactors(float h, float s, float b) {
        if (s < 0) {
            s = 0;
        }
        if (b < 0) {
            b = 0;
        }
        hFactor = h;
        sFactor = s;
        bFactor = b;
    }

    @Override
    public boolean isIdentity() {
        return false;
    }
}
