package ca.cgjennings.graphics;

import ca.cgjennings.graphics.filters.AbstractImageFilter;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * An image histogram contains information about the distribution of channel
 * values in an image.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class Histogram {

    private int area = 0;
    private final int count[] = new int[256];
    private boolean ignoreZeroAlpha = true;
    private int channelMask = 0xff_ffff;

    /**
     * A mask value to include the blue channel in the histogram.
     */
    public static final int BLUE = 0xff;
    /**
     * A mask value to include the green channel in the histogram.
     */
    public static final int GREEN = 0xff00;
    /**
     * A mask value to include the red channel in the histogram.
     */
    public static final int RED = 0xff_0000;
    /**
     * A mask value to include the alpha channel in the histogram.
     */
    public static final int ALPHA = 0xff00_0000;

    /**
     * Creates a new, empty histogram. The histogram will not be valid unit it
     * has measured one or more image samples. The histogram will measure the
     * red, green, and blue channels and ignore pixels with an alpha value of
     * zero.
     */
    public Histogram() {
    }

    /**
     * Returns {@code true} if pixels with an alpha of zero will not be included
     * in the histogram.
     *
     * @return {@code true} if transparent pixels are ignored
     */
    public boolean isZeroAlphaIgnored() {
        return ignoreZeroAlpha;
    }

    /**
     * Sets whether pixels with an alpha value of zero should be ignored.
     * Ignored pixels are not counted in the histogram statistics.
     *
     * @param ignoreZeroAlpha {@code true} to ignore transparent pixels
     */
    public void setZeroAlphaIgnored(boolean ignoreZeroAlpha) {
        this.ignoreZeroAlpha = ignoreZeroAlpha;
    }

    /**
     * Returns the channel mask for the histogram.
     *
     * @return the bitwise-or of mask values for the channels included in the
     * histogram
     * @see #setChannelMask(int)
     */
    public int getChannelMask() {
        return channelMask;
    }

    /**
     * Sets the channel mask for the histogram. The channel mask determines
     * which channels of the image will be included in the histogram count. For
     * a given pixel, the level measured by the histogram will be the mean of
     * the channels included by the mask. The mask value is a bitwise-or of some
     * combination of {@link #RED}, {@link #GREEN}, {@link #BLUE}, and
     * {@link #ALPHA}. At least one channel must be specified by the mask.
     *
     * @param mask the bitwise-or of the channel masks for the channels to
     * include
     */
    public void setChannelMask(int mask) {
        // check that each channel is either 0x00 or 0xff
        int maskTemp = mask;
        int channels = 0;
        for (int i = 0; i < 4; ++i) {
            int test = maskTemp & 0xff;
            if (test != 0x00 && test != 0xff) {
                throw new IllegalArgumentException("invalid mask for channel " + i);
            }
            if (test == 0xff) {
                ++channels;
            }
            maskTemp >>= 8;
        }
        if (channels == 0) {
            throw new IllegalArgumentException("no channels requested by channel mask");
        }

        channelMask = mask;
    }

    /**
     * Measures the samples in an image and incorporates them into the histogram
     * statistics. All channels indicated by the {@code channelMask} will be
     * averaged together to determine the histogram bucket.
     *
     * @param image the image to analyze
     */
    public void measure(BufferedImage image) {
        measure(AbstractImageFilter.getARGB(image, null));
    }

    /**
     * Measures the samples in an array of pixel data and incorporates them into
     * the histogram statistics. All channels indicated by the
     * {@code channelMask} will be averaged together to determine the histogram
     * bucket.
     *
     * @param image the image to analyze
     */
    public void measure(int[] image) {
        area += image.length;
        int channels = Integer.bitCount(channelMask);
        channels /= 8;
        for (int i = 0; i < image.length; ++i) {
            if (ignoreZeroAlpha && (image[i] & 0xff00_0000) == 0) {
                --area;
                continue;
            }
            int sample = image[i] & channelMask;
            int sum = 0;
            sum += (sample & ALPHA) >>> 24;
            sum += (sample & RED) >>> 16;
            sum += (sample & GREEN) >>> 8;
            sum += (sample & BLUE);
            ++count[sum / channels];
        }
    }

    /**
     * Resets the histogram statistics.
     */
    public void reset() {
        Arrays.fill(count, 0);
        area = 0;
    }

    /**
     * Returns the number of samples counted for the given level.
     *
     * @param level the level to return the histogram count for (0-255)
     * @return the number of samples counted at this level
     */
    public int getCountForLevel(int level) {
        if (area == 0) {
            throw new IllegalStateException("no images have been measured");
        }
        if (level < 0 || level > 255) {
            throw new IllegalArgumentException("level not in [0..255]: " + level);
        }
        return count[level];
    }

    /**
     * Returns the relative number of samples counted for the given level.
     *
     * @param level the level to return the histogram count for (0-255)
     * @return the number of samples counted at this level over the total
     * samples counted, from 0 to 1
     */
    public double getRelativeCountForLevel(int level) {
        double s = getCountForLevel(level);
        return s / area;
    }

    /**
     * Returns the maximum count for any level.
     *
     * @return the largest count value
     */
    public int getMaximumCount() {
        int max = 0;
        for (int i = 0; i < 256; ++i) {
            max = Math.max(max, count[i]);
        }
        return max;
    }

    /**
     * Returns the total number of samples that have been measured.
     *
     * @return the number of pixels processed
     */
    public int getSampleCount() {
        return area;
    }

    /**
     * A utility method that creates an image of the histogram for an image,
     * averaging the red, green, and blue channels together.
     *
     * @param src the image to analyze
     * @return a simple graph of the image's histogram
     */
    public static BufferedImage createHistogramImage(BufferedImage src) {
        BufferedImage hImage = new BufferedImage(256 * GRAPH_SCALE, 101 * GRAPH_SCALE, BufferedImage.TYPE_INT_ARGB);
        Histogram hist = new Histogram();
        hist.measure(src);
        if (hist.getSampleCount() == 0) {
            return hImage;
        }

        Graphics2D g = hImage.createGraphics();
        try {
            g.scale(GRAPH_SCALE, GRAPH_SCALE);
//			if( GRAPH_SCALE > 1 ) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//			}
            g.setComposite(AlphaComposite.SrcOver.derive(0.8f));
            g.setColor(Color.BLACK);
            double max = hist.getMaximumCount();

            Path2D path = new Path2D.Double();
            path.moveTo(0d, 101d);
            for (int i = 0; i < 256; ++i) {
                int y2 = 100 - (int) (hist.getCountForLevel(i) / max * 100d + 0.5d);
                path.lineTo(i, y2);
            }
            path.lineTo(255, 101);
            path.lineTo(0, 101);
            g.fill(path);
        } finally {
            g.dispose();
        }
        return hImage;
    }

    private static final int GRAPH_SCALE = 1;

    /**
     * A utility method that creates an image of the histogram for an image,
     * showing separate red, green, and blue channels.
     *
     * @param src the image to analyze
     * @return a simple graph of the image's histogram
     */
    public static BufferedImage createRGBHistogramImage(BufferedImage src) {
        BufferedImage hImage = new BufferedImage(256 * GRAPH_SCALE, 101 * GRAPH_SCALE, BufferedImage.TYPE_INT_ARGB);

        Histogram[] hist = new Histogram[]{new Histogram(), new Histogram(), new Histogram()};

        int[] pixels = AbstractImageFilter.getARGB(src, null);
        double max = 0d;
        int mask = 0xff;
        for (int h = 0; h < 3; ++h) {
            hist[h].setChannelMask(0xff << (h * 8));
            hist[h].measure(pixels);
            mask <<= 8;
            max = Math.max(max, hist[h].getMaximumCount());
        }

        Graphics2D g = hImage.createGraphics();
        try {
            g.scale(GRAPH_SCALE, GRAPH_SCALE);
            g.setComposite(AlphaComposite.SrcOver.derive(0.8f));
//			if( GRAPH_SCALE > 1 ) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//			}
            g.setColor(new Color(0x77));
            Path2D path = new Path2D.Double();
            for (int h = 0; h < 3; ++h) {
                if (hist[h].getSampleCount() == 0) {
                    continue;
                }
                path.moveTo(0d, 101d);
                for (int i = 0; i < 256; ++i) {
                    int y2 = 100 - (int) (hist[h].getCountForLevel(i) / max * 100d + 0.5d);
                    path.lineTo(i, y2);
                }
                path.lineTo(255d, 101d);
                path.lineTo(0d, 101d);
                g.fill(path);
                path.reset();
                if (h == 0) {
                    g.setColor(new Color(0x7700));
                } else {
                    g.setColor(new Color(0x77_0000));
                }
            }
        } finally {
            g.dispose();
        }
        return hImage;
    }

//	private static void drawGrid( Graphics2D g ) {
//		g.setColor( Color.GRAY );
//		g.drawLine( 0,   0, 255,   0 );
//		g.drawLine( 0, 100, 255, 100 );
//		Stroke s = g.getStroke();
//		g.setStroke( new BasicStroke( 1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, new float[] {4, 4}, 0f ) );
//		g.drawLine( 0,  50, 255,  50 );
////		g.drawLine( 0,  25, 255,  25 );
////		g.drawLine( 0,  75, 255,  75 );
//		g.setStroke( s );
//	}
//	public static void main( String[] args ) {
//		try {
//			BufferedImage t = ImageIO.read( new File("d:\\splash.png") );
//			BufferedImage h = createHistogramImage( t );
//			ImageIO.write( h, "png", new File("d:\\test-out.png") );
//			h = createRGBHistogramImage( t );
//			ImageIO.write( h, "png", new File("d:\\test-out2.png") );
//		} catch( Exception e ) {
//			e.printStackTrace();
//		}
//	}
}
