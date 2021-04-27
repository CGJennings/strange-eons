package ca.cgjennings.graphics.filters;

import ca.cgjennings.graphics.ImageUtilities;

/**
 * Applies a gamma correction function to the source image. The alpha channel is
 * unaffected. Gamma correction adjusts images to account for the property of
 * the human visual system that it is more sensitive to the shadows in an image
 * than it is to the highlights.
 *
 * <p>
 * This filter can also be used to recolour an image by first
 * {@linkplain ImageUtilities#desaturate(java.awt.image.BufferedImage) converting}
 * it to {@linkplain GreyscaleFilter greyscale} and then applying a gamma
 * correction in the direction of the desired colour. A gamma correction value
 * of 1 results in no change to the affected channel; values greater than 1
 * increase the values in the channel and values less than 1 decrease the values
 * in the channel and therefore also increase the relative amount of the
 * channel's colour complement:
 * <pre>
 *                      &lt;1           1          &gt;1
 * Red gamma         More Cyan   No Change   More Red
 * Green gamma    More Magenta   No Change   More Green
 * Blue gamma      More Yellow   No Change   More Blue
 * </pre>
 *
 * <p>
 * For example, the following gamma correction values will approximate a sepia
 * tone effect: R=1.5, G=1, B=0.5.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class GammaCorrectionFilter extends AbstractPixelwiseFilter {

    private int[] rTable, gTable, bTable;
    private double r, g, b;

    public GammaCorrectionFilter() {
        this(1d, 1d, 1d);
    }

    public GammaCorrectionFilter(double gamma) {
        this(gamma, gamma, gamma);
    }

    public GammaCorrectionFilter(double rGamma, double gGamma, double bGamma) {
        setGamma(rGamma, gGamma, bGamma);
    }

    public void setGamma(double rGamma, double gGamma, double bGamma) {
        rTable = createGammaLookupTable(rGamma, 256);
        if (gGamma == rGamma) {
            gTable = rTable;
        } else {
            gTable = createGammaLookupTable(gGamma, 256);
        }
        if (bGamma == gGamma) {
            bTable = gTable;
        } else if (bGamma == rGamma) {
            bTable = rTable;
        } else {
            bTable = createGammaLookupTable(bGamma, 256);
        }
        r = rGamma;
        g = gGamma;
        b = bGamma;
    }

    public double getRedGamma() {
        return r;
    }

    public void setRedGamma(double rGamma) {
        setGamma(rGamma, g, b);
    }

    public double getGreenGamma() {
        return g;
    }

    public void setGreenGamma(double gGamma) {
        setGamma(r, gGamma, b);
    }

    public double getBlueGamma() {
        return b;
    }

    public void setBlueGamma(double bGamma) {
        setGamma(r, g, bGamma);
    }

    public static int[] createGammaLookupTable(double gamma, int levels) {
        int[] table = new int[levels];
        double inverseGamma = 1d / gamma;
        double max = (levels - 1);
        for (int i = 0; i < levels; ++i) {
            int level = (int) (0.5d + (Math.pow(i / max, inverseGamma) * max));
            table[i] = level > 255 ? 255 : level;
        }
        return table;
    }

    @Override
    public void filterPixels(int[] argb, int start, int end) {
        for (int p = start; p < end; ++p) {
            int rgb = argb[p];
            argb[p] = (rgb & 0xff00_0000)
                    | (rTable[(rgb >> 16) & 0xff] << 16)
                    | (gTable[(rgb >> 8) & 0xff] << 8)
                    | (bTable[rgb & 0xff]);
        }
    }
}
