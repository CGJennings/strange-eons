package ca.cgjennings.graphics.filters;

import java.util.Arrays;

/**
 * A filter that simulates oil painting. The filter takes two parameters, smear
 * radius and a number of colour levels. The smear radius determines the size of
 * the "paint daubs", and the levels setting controls the amount of colour
 * variation allowed within a daub. Note that setting the number of levels to a
 * value greater than 2<sup>smearRadius</sup> will not produce a noticeable
 * difference in the result.
 *
 * <p>
 * <b>In-place filtering:</b> This class supports in-place filtering (the source
 * and destination image may be the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class OilPaintingFilter extends AbstractImagewiseFilter {

    private int levels = 8;
    private int radius = 4;

    public OilPaintingFilter() {
    }

    public OilPaintingFilter(int levels, int smearRadius) {
        setLevels(levels);
        setSmearRadius(smearRadius);
    }

    /**
     * Returns the number of channel levels to use.
     *
     * @return the number of levels
     */
    public int getLevels() {
        return levels;
    }

    /**
     * Sets the number of channel levels to use. The greater the number of
     * levels, the more varied the image can be within a given smear radius.
     *
     * @param levels the levels to set
     */
    public void setLevels(int levels) {
        if (levels < 2 || levels > 256) {
            throw new IllegalArgumentException("levels must be in [2..256]: " + levels);
        }
        this.levels = levels;
    }

    /**
     * Returns the number of rows and columns of pixel data around each source
     * pixel are used to compute the destination pixel.
     *
     * @return the smear radius
     */
    public int getSmearRadius() {
        return radius;
    }

    /**
     * Sets the number of rows and columns of pixel data around each source
     * pixel that are used to compute the destination pixel.
     *
     * @param radius the smear radius to set
     */
    public void setSmearRadius(int radius) {
        if (radius < 1) {
            throw new IllegalArgumentException("radius must be positive: " + radius);
        }
        this.radius = radius;
    }

    @Override
    protected float workFactor() {
        return (radius * radius);
    }

    @Override
    protected void filterPixels(int[] srcPixels, int[] dstPixels, int width, int height, int y0, int rows) {
        final int y1 = y0 + rows;
        final int levels = Math.min(this.levels, radius * radius);

        final int[] rSum = new int[levels];
        final int[] gSum = new int[levels];
        final int[] bSum = new int[levels];

        final int[] rHist = new int[levels];
        final int[] gHist = new int[levels];
        final int[] bHist = new int[levels];

        int index = y0 * width;
        for (int y = y0; y < y1; y++) {
            for (int x = 0; x < width; x++) {

                Arrays.fill(rSum, 0);
                Arrays.fill(gSum, 0);
                Arrays.fill(bSum, 0);
                Arrays.fill(rHist, 0);
                Arrays.fill(gHist, 0);
                Arrays.fill(bHist, 0);

                // analyze pixels around smear radius
                for (int row = -radius; row <= radius; row++) {
                    int sy = y + row;
                    if (sy < 0 || sy >= height) {
                        continue;
                    }
                    int syStart = sy * width;
                    for (int col = -radius; col <= radius; col++) {
                        int sx = x + col;
                        if (sx < 0 || sx >= width) {
                            continue;
                        }

                        int rgb = srcPixels[syStart + sx];
                        int r = (rgb >> 16) & 0xff;
                        int g = (rgb >> 8) & 0xff;
                        int b = rgb & 0xff;

                        int rScale = r * levels / 256;
                        int gScale = g * levels / 256;
                        int bScale = b * levels / 256;

                        rSum[rScale] += r;
                        gSum[gScale] += g;
                        bSum[bScale] += b;

                        rHist[rScale]++;
                        gHist[gScale]++;
                        bHist[bScale]++;
                    }
                }

                // choose pixel colour
                int r = 0, g = 0, b = 0;
                for (int i = 1; i < levels; i++) {
                    if (rHist[i] > rHist[r]) {
                        r = i;
                    }
                    if (gHist[i] > gHist[g]) {
                        g = i;
                    }
                    if (bHist[i] > bHist[b]) {
                        b = i;
                    }
                }

                r = rSum[r] / rHist[r];
                g = gSum[g] / gHist[g];
                b = bSum[b] / bHist[b];

                dstPixels[index] = (srcPixels[index] & 0xff000000) | (r << 16) | (g << 8) | b;
                index++;
            }
        }
    }
}
