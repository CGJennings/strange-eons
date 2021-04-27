package ca.cgjennings.graphics.filters;

import java.awt.image.BufferedImage;

/**
 * A filter that increases the overall opacity of an image. The effect is
 * similar to painting the source image overtop of itself a number of times
 * equal to the filter strength.
 *
 * <p>
 * <b>In-place filtering:</b> This class supports in-place filtering (the source
 * and destination images may be the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class AlphaStrengthenFilter extends AbstractPixelwiseFilter {

    private int strength = 1;

    public AlphaStrengthenFilter() {
    }

    public AlphaStrengthenFilter(int strength) {
        setStrengthFactor(strength);
    }

    public int getStrengthFactor() {
        return strength;
    }

    /**
     * Sets the strength of the effect. A strength of zero will result in the
     * filter having no effect. Higher values will result in translucent pixels
     * becoming successively more opaque, although completely transparent pixels
     * (alpha == 0) are never affected.
     *
     * @param strength
     */
    public void setStrengthFactor(int strength) {
        if (strength < 0) {
            throw new IllegalArgumentException("strength < 0: " + strength);
        }
        if (this.strength != strength) {
            this.strength = strength;
            tableIsDirty = true;
        }
    }

    private int[] table = new int[256];
    private boolean tableIsDirty = true;

    private void updateTable() {
        for (int t = 0; t < 256; ++t) {
            float alpha = t / 255f;
            for (int s = 0; s < strength; ++s) {
                alpha += alpha * alpha;
            }
            table[t] = Math.min(255, (int) (alpha * 255f + 0.5f));
        }
        tableIsDirty = false;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        if (tableIsDirty) {
            updateTable();
        }
        return super.filter(src, dest);
    }

    @Override
    public void filterPixels(int[] argb, int start, int end) {
        for (int i = start; i < end; ++i) {
            argb[i] = (table[argb[i] >>> 24] << 24) | (argb[i] & 0xffffff);
        }
    }

    @Override
    public int filterPixel(int argb) {
        if (tableIsDirty) {
            updateTable();
        }
        return (table[argb >>> 24] << 24) | (argb & 0xffffff);
    }
}
