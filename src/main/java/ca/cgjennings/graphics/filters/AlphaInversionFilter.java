package ca.cgjennings.graphics.filters;

/**
 * An image filter that inverts the alpha channel of a source image: transparent
 * areas will become solid and vice-versa. The RGB values of the pixels are
 * unaffected.
 *
 * <p>
 * <b>In-place filtering:</b> This class supports in-place filtering (the source
 * and destination images may be the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @see ChannelSwapFilter
 * @since 3.0
 */
public class AlphaInversionFilter extends AbstractPixelwiseFilter {

    @Override
    public void filterPixels(int[] argb, int start, int end) {
        for (int x = start; x < end; ++x) {
            int pixel = argb[x];
            argb[x] = ((0xff000000 - (pixel & 0xff000000)) & 0xff000000)
                    | (pixel & 0xffffff);
        }
    }
}
