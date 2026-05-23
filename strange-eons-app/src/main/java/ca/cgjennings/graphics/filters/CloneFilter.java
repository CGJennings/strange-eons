package ca.cgjennings.graphics.filters;

import ca.cgjennings.graphics.ImageUtilities;

/**
 * A filter that creates an identical copy of the source image.
 *
 * <p>
 * <b>In-place filtering:</b> This class supports in-place filtering (the source
 * and destination images may be the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @see ImageUtilities#clone()
 */
public final class CloneFilter extends AbstractPixelwiseFilter {

    public CloneFilter() {
    }

    @Override
    public void filterPixels(int[] argb, int start, int end) {
    }
}
