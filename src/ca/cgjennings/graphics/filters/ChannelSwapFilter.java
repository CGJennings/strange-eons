package ca.cgjennings.graphics.filters;

/**
 * A filter that allows channels in the source image to be swapped around,
 * inverted, or filled with constant values. A source is set for each channel in
 * the destination image. If the source value is between 0 and 255 (inclusive),
 * then that channel is filled with that constant value. Otherwise, the source
 * can be a special value that indicates any channel in the source image, and
 * the value of that channel in the source image will be copied to the
 * destination channel.
 *
 * <p>
 * As an example, the following filter would set the alpha value of every
 * destination pixel to 255, and copy the red channel to all of the other
 * channels:
 * <pre>
 * ChannelSwapFilter csf = new ChannelSwapFilter( 255, RED, RED, RED );
 * </pre>
 *
 * <p>
 * <b>In-place filtering:</b> This class supports in-place filtering (the source
 * and destination images may be the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class ChannelSwapFilter extends AbstractPixelwiseFilter {

    private int aSrc = ALPHA, rSrc = RED, gSrc = GREEN, bSrc = BLUE;

    /**
     * The value 0, which indicates that the channel should be zeroed out.
     */
    public static final int ZERO = 0;
    /**
     * The value 255, which indicates that the channel should set to its maximum
     * value.
     */
    public static final int FILL = 255;
    /**
     * The target channel's value will be copied from the alpha channel of the
     * source.
     */
    public static final int ALPHA = 256;
    /**
     * The target channel's value will be copied from the red channel of the
     * source.
     */
    public static final int RED = 257;
    /**
     * The target channel's value will be copied from the green channel of the
     * source.
     */
    public static final int GREEN = 258;
    /**
     * The target channel's value will be copied from the blue channel of the
     * source.
     */
    public static final int BLUE = 259;
    /**
     * The target channel's value will be copied from the alpha channel of the
     * source and inverted.
     */
    public static final int ALPHA_INVERTED = 260;
    /**
     * The target channel's value will be copied from the red channel of the
     * source and inverted.
     */
    public static final int RED_INVERTED = 261;
    /**
     * The target channel's value will be copied from the green channel of the
     * source and inverted.
     */
    public static final int GREEN_INVERTED = 262;
    /**
     * The target channel's value will be copied from the blue channel of the
     * source and inverted.
     */
    public static final int BLUE_INVERTED = 263;
    /**
     * The target channel's value will be set to the brightness of the source
     * pixel as if it were converted to {@linkplain GreyscaleFilter greyscale}.
     */
    public static final int GREY = 264;
    /**
     * The target channel's value will be set to the inverse of the brightness
     * of the source pixel (255-{@link #GREY GREY}).
     */
    public static final int GREY_INVERTED = 265;

    private static final int MIN_VALUE = 0;
    private static final int MAX_VALUE = 265;

    /**
     * Creates a <code>ChannelSwapFilter</code> that copies each channel from
     * the the same channel in the source image, reproducing the original image.
     */
    public ChannelSwapFilter() {
    }

    /**
     * Creates a <code>ChannelSwapFilter</code> that copies channels from the
     * indicated source channel.
     *
     * @param alphaSource the source channel to use for the destination's alpha
     * channel
     * @param redSource the source channel to use for the destination's red
     * channel
     * @param greenSource the source channel to use for the destination's green
     * channel
     * @param blueSource the source channel to use for the destination's blue
     * channel
     */
    public ChannelSwapFilter(int alphaSource, int redSource, int greenSource, int blueSource) {
        this();
        setSources(alphaSource, redSource, greenSource, blueSource);
    }

    /**
     * Sets the channel source for all channels with a single call.
     *
     * @param alphaSource the source channel to use for the destination's alpha
     * channel
     * @param redSource the source channel to use for the destination's red
     * channel
     * @param greenSource the source channel to use for the destination's green
     * channel
     * @param blueSource the source channel to use for the destination's blue
     * channel
     */
    public final void setSources(int alphaSource, int redSource, int greenSource, int blueSource) {
        aSrc = checkChannel(alphaSource);
        rSrc = checkChannel(redSource);
        gSrc = checkChannel(greenSource);
        bSrc = checkChannel(blueSource);
    }

    /**
     * Returns a new array containing the current channel sources for all
     * channels in the order alpha, red, green, blue.
     *
     * @return the current channel sources, as an array
     * @see #setSources
     */
    public final int[] getSources() {
        return new int[]{aSrc, rSrc, gSrc, bSrc};
    }

    /**
     * Sets the alpha source channel.
     *
     * @param alphaSource the source channel to use for the destination's alpha
     * channel
     */
    public final void setAlphaSource(int alphaSource) {
        aSrc = checkChannel(alphaSource);
    }

    /**
     * Sets the red source channel.
     *
     * @param redSource the source channel to use for the destination's red
     * channel
     */
    public final void setRedSource(int redSource) {
        rSrc = checkChannel(redSource);
    }

    /**
     * Sets the green source channel.
     *
     * @param greenSource the source channel to use for the destination's green
     * channel
     */
    public final void setGreenSource(int greenSource) {
        gSrc = checkChannel(greenSource);
    }

    /**
     * Sets the blue source channel.
     *
     * @param blueSource the source channel to use for the destination's blue
     * channel
     */
    public final void setBlueSource(int blueSource) {
        bSrc = checkChannel(blueSource);
    }

    /**
     * Returns the alpha source channel.
     *
     * @return the source channel to use for the destination's alpha channel
     */
    public final int getAlphaSource() {
        return aSrc;
    }

    /**
     * Returns the red source channel.
     *
     * @return the source channel to use for the destination's red channel
     */
    public final int getRedSource() {
        return rSrc;
    }

    /**
     * Returns the green source channel.
     *
     * @return the source channel to use for the destination's green channel
     */
    public final int getGreenSource() {
        return gSrc;
    }

    /**
     * Returns the blue source channel.
     *
     * @return the source channel to use for the destination's blue channel
     */
    public final int getBlueSource() {
        return bSrc;
    }

    /**
     * Checks a source channel value to make sure it is valid. If not, an
     * <code>IllegalArgumentException</code> is thrown. Otherwise, the channel
     * value is returned.
     *
     * @param ch the channel value to check
     * @return <code>ch</code>
     * @throws IllegalArgumentException if the channel value is invalid
     */
    private static int checkChannel(int ch) {
        if (ch < MIN_VALUE || ch > MAX_VALUE) {
            throw new IllegalArgumentException("invalid source value: " + ch);
        }
        return ch;
    }

    @Override
    public void filterPixels(int[] pixels, int start, int end) {
        boolean calcGrey = aSrc >= GREY || bSrc >= GREY || gSrc >= GREY;
        int sGrey = 0;

        for (int i = start; i < end; ++i) {
            final int argb = pixels[i];

            final int sA = (argb >>> 24);
            final int sR = (argb >>> 16) & 0xff;
            final int sG = (argb >>> 8) & 0xff;
            final int sB = argb & 0xff;
            if (calcGrey) {
                sGrey = (77 * sR + 150 * sG + 28 * sB) / 255;
            }

            int out, v;

            switch (aSrc) {
                case ALPHA:
                    v = sA;
                    break;
                case ALPHA_INVERTED:
                    v = 255 - sA;
                    break;
                case RED:
                    v = sR;
                    break;
                case RED_INVERTED:
                    v = 255 - sR;
                    break;
                case GREEN:
                    v = sG;
                    break;
                case GREEN_INVERTED:
                    v = 255 - sG;
                    break;
                case BLUE:
                    v = sB;
                    break;
                case BLUE_INVERTED:
                    v = 255 - sB;
                    break;
                case GREY:
                    v = sGrey;
                    break;
                case GREY_INVERTED:
                    v = 255 - sGrey;
                    break;
                default:
                    v = aSrc;
            }

            out = v << 24;

            switch (rSrc) {
                case ALPHA:
                    v = sA;
                    break;
                case ALPHA_INVERTED:
                    v = 255 - sA;
                    break;
                case RED:
                    v = sR;
                    break;
                case RED_INVERTED:
                    v = 255 - sR;
                    break;
                case GREEN:
                    v = sG;
                    break;
                case GREEN_INVERTED:
                    v = 255 - sG;
                    break;
                case BLUE:
                    v = sB;
                    break;
                case BLUE_INVERTED:
                    v = 255 - sB;
                    break;
                case GREY:
                    v = sGrey;
                    break;
                case GREY_INVERTED:
                    v = 255 - sGrey;
                    break;
                default:
                    v = rSrc;
            }

            out |= v << 16;

            switch (gSrc) {
                case ALPHA:
                    v = sA;
                    break;
                case ALPHA_INVERTED:
                    v = 255 - sA;
                    break;
                case RED:
                    v = sR;
                    break;
                case RED_INVERTED:
                    v = 255 - sR;
                    break;
                case GREEN:
                    v = sG;
                    break;
                case GREEN_INVERTED:
                    v = 255 - sG;
                    break;
                case BLUE:
                    v = sB;
                    break;
                case BLUE_INVERTED:
                    v = 255 - sB;
                    break;
                case GREY:
                    v = sGrey;
                    break;
                case GREY_INVERTED:
                    v = 255 - sGrey;
                    break;
                default:
                    v = gSrc;
            }

            out |= v << 8;

            switch (bSrc) {
                case ALPHA:
                    v = sA;
                    break;
                case ALPHA_INVERTED:
                    v = 255 - sA;
                    break;
                case RED:
                    v = sR;
                    break;
                case RED_INVERTED:
                    v = 255 - sR;
                    break;
                case GREEN:
                    v = sG;
                    break;
                case GREEN_INVERTED:
                    v = 255 - sG;
                    break;
                case BLUE:
                    v = sB;
                    break;
                case BLUE_INVERTED:
                    v = 255 - sB;
                    break;
                case GREY:
                    v = sGrey;
                    break;
                case GREY_INVERTED:
                    v = 255 - sGrey;
                    break;
                default:
                    v = bSrc;
            }

            pixels[i] = out | v;
        }
    }
}
