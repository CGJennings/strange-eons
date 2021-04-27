package ca.cgjennings.graphics.filters;

/**
 * An image filter that adjusts the brightness and contrast of an image. The
 * brightness and contrast adjustments are specified as values in the range -1
 * to +1 (inclusive). A value of 0 indicates no change, while values greater or
 * less than 0 increase or decrease (respectively) the brightness or contrast.
 *
 * <p>
 * <b>In-place filtering:</b> This class supports in-place filtering (the source
 * and destination images may be the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class BrightnessContrastFilter extends AbstractFunctionFilter {

    private double brightness = 0d;
    private double contrast = 0d;

    /**
     * Creates a new filter with default brightness and contrast settings of 0,
     * resulting in no change.
     */
    public BrightnessContrastFilter() {
    }

    /**
     * Creates a new filter with the requested brightness and contrast
     * adjustments.
     *
     * @param brightness the brightness adjustment factor to apply
     * @param contrast the contrast adjustment factor to apply
     * @throws IllegalArgumentException if <code>brightness</code> or
     * <code>contrast</code> are not in the range -1 to 1 inclusive
     */
    public BrightnessContrastFilter(double brightness, double contrast) {
        setBrightness(brightness);
        setContrast(contrast);
    }

    /**
     * Returns the current brightness adjustment setting, between -1 and 1
     * inclusive.
     *
     * @return the brightness adjustment setting
     */
    public double getBrightness() {
        return brightness;
    }

    /**
     * Sets the current brightness adjustment setting, between -1 and 1
     * inclusive. A value of 0 results in no brightness change, while negative
     * and positive values decrease and increase brightness, respectively.
     *
     * @param brightness the brightness adjustment to apply to filtered images
     */
    public void setBrightness(double brightness) {
        if (brightness < -1d || brightness > 1d) {
            throw new IllegalArgumentException("brightness outside of range -1..+1: " + brightness);
        }
        if (this.brightness != brightness) {
            this.brightness = brightness;
            functionChanged();
        }
    }

    /**
     * Returns the current contrast adjustment setting, between -1 and 1
     * inclusive.
     *
     * @return the contrast adjustment setting
     */
    public double getContrast() {
        return contrast;
    }

    /**
     * Sets the current contrast adjustment setting, between -1 and 1 inclusive.
     * A value of 0 results in no contrast change, while negative and positive
     * values decrease and increase contrast, respectively.
     *
     * @param contrast the contrast adjustment to apply to filtered images
     */
    public void setContrast(double contrast) {
        if (contrast < -1d || contrast > 1d) {
            throw new IllegalArgumentException("contrast outside of range -1..+1: " + contrast);
        }
        if (this.contrast != contrast) {
            this.contrast = contrast;
            functionChanged();
        }
    }

    /**
     * A transfer function for the current brightness and contrast settings. See
     * {@link AbstractFunctionFilter#f(double)}.
     *
     * @param x the input value
     * @return the output value after applying the brightness and contrast
     * adjustment
     */
    @Override
    public double f(double x) {
        if (brightness < 0d) {
            x *= (brightness + 1d);
        } else {
            x += (1d - x) * brightness;
        }

        x = (x - 0.5d) * Math.tan((contrast + 1d) * Math.PI / 4d) + 0.5d;

        return x;
    }
}
