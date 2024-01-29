package ca.cgjennings.graphics;

import ca.cgjennings.graphics.filters.AbstractImageFilter;
import java.awt.image.BufferedImage;

/**
 * Gathers basic statistics about an image, including the minimum, maximum, and
 * mean of each channel.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class ImageStatistics {

    /**
     * Creates a new instance. Call {@link #analyze} to fill in the statistic
     * fields.
     */
    public ImageStatistics() {
    }

    /**
     * Creates a new instance and fills in the statistic fields from the
     * specified image.
     *
     * @param bi the image to process
     */
    public ImageStatistics(BufferedImage bi) {
        analyze(bi);
    }

    public int alphaMin;
    public int alphaMax;
    public int alphaMean;
    public int redMin;
    public int redMax;
    public int redMean;
    public int greenMin;
    public int greenMax;
    public int greenMean;
    public int blueMin;
    public int blueMax;
    public int blueMean;
    public int greyMin;
    public int greyMax;
    public int greyMean;

    private float alphaSum, redSum, greenSum, blueSum, greySum;

    /**
     * Analyzes the image, filling in the statistic fields with the results.
     *
     * @param bi the image to process
     */
    public void analyze(BufferedImage bi) {
        if (bi == null) {
            throw new NullPointerException("bi");
        }
        int w = bi.getWidth();
        int h = bi.getHeight();
        alphaSum = redSum = greenSum = blueSum = greySum = 0f;
        alphaMin = redMin = greenMin = blueMin = greyMin = 255;
        alphaMax = redMax = greenMax = blueMax = greyMax = 0;

        int[] argb = new int[w];
        for (int y = 0; y < h; ++y) {
            AbstractImageFilter.getARGB(bi, 0, y, w, 1, argb);
            for (int x = 0; x < w; ++x) {
                final int p = argb[x];
                final int a = (p >>> 24);
                final int r = (p >> 16) & 255;
                final int g = (p >> 8) & 255;
                final int b = p & 255;

                alphaMin = Math.min(alphaMin, a);
                alphaMax = Math.max(alphaMax, a);
                alphaSum += a;

                redMin = Math.min(redMin, r);
                redMax = Math.max(redMax, r);
                redSum += r;

                greenMin = Math.min(greenMin, g);
                greenMax = Math.max(greenMax, g);
                greenSum += g;

                blueMin = Math.min(blueMin, b);
                blueMax = Math.max(blueMax, b);
                blueSum += b;

                int gr = ((77 * r) + (150 * g) + (28 * b)) / 255;
                greyMin = Math.min(greyMin, gr);
                greyMax = Math.max(greyMax, gr);
                greySum += gr;
            }
        }

        float total = w * (float) h;
        alphaMean = AbstractImageFilter.clamp(alphaSum / total);
        redMean = AbstractImageFilter.clamp(redSum / total);
        greenMean = AbstractImageFilter.clamp(greenSum / total);
        blueMean = AbstractImageFilter.clamp(blueSum / total);
        greyMean = AbstractImageFilter.clamp(greySum / total);
    }
}
