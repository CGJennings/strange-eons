package ca.cgjennings.graphics.cloudfonts;

/**
 * A font axis descriptor. Fonts with variable axes can be
 * adjusted along these axes to create a wide variety of
 * styles. For example, instead of offering "regular" and
 * "bold" versions of a font, a variable font might offer
 * a "weight" axis that can be adjusted to any value between
 * 100 and 900. Standard axes should be used automatically
 * when a font with the relveant attributes is requested.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class Axis {
    Axis(String tag, float min, float max) {
        this.tag = tag;
        this.min = min;
        this.max = max;
        validate();
    }

    Axis(String descriptor) {
        String[] parts = descriptor.split(",");
        this.tag = parts[0];
        this.min = parts.length >= 2 ? Float.parseFloat(parts[1]) : Float.NaN;
        this.max = parts.length >= 3 ? Float.parseFloat(parts[2]) : Float.NaN;
        validate();
    }

    private void validate() {
        if (tag == null || tag.length() != 4) {
            throw new AssertionError("invalid tag: " + tag);
        }
        if (min == min && max == max && min > max) {
            throw new AssertionError("invalid range: " + min + " to " + max);
        }
    }

    /**
     * Returns a string representation of the axis.
     */
    @Override
    public String toString() {
        return '[' + tag + ']'
        + (min == min ? " min: " + min : "")
        + (max == max ? " max: " + max : "");
    }

    public final String tag;
    public final float min;
    public final float max;
}
