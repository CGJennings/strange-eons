package ca.cgjennings.graphics.filters;

/**
 * An image filter that inverts the pixel values of the source image, producing
 * a negative image. The alpha channel is unaffected. For more fine-grained
 * control over which channels are affected, use a {@link ChannelSwapFilter}
 * instead. Or, to invert just the alpha channel, use an
 * {@link AlphaInversionFilter}.
 *
 * <p>
 * <b>In-place filtering:</b> This class supports in-place filtering (the source
 * and destination images may be the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class InversionFilter extends AbstractPixelwiseFilter {

    @Override
    public void filterPixels(int[] argb, int start, int end) {
        for (int x = start; x < end; ++x) {
            int pixel = argb[x];
            argb[x] = (pixel & 0xff000000)
                    | ((0xff0000 - (pixel & 0xff0000)) & 0xff0000)
                    | ((0xff00 - (pixel & 0xff00)) & 0xff00)
                    | ((0xff - (pixel & 0xff)) & 0xff);
        }
    }
}
