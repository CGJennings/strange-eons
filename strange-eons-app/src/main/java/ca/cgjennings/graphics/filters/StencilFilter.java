package ca.cgjennings.graphics.filters;

import java.awt.image.BufferedImage;

/**
 * Set the alpha channel of an image using the pixels of another image as a
 * stencil. The same effect can be achieved with a composite, but the filter is
 * optimized for the case where the stencil image will be reused multiple times.
 *
 * <p>
 * <b>In-place filtering:</b> This class supports in-place filtering (the source
 * and destination images may be the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class StencilFilter extends AbstractRowwiseFilter {

    /**
     * Creates a new filter with a transparent empty 1 pixel by 1 pixel image as
     * the stencil.
     */
    public StencilFilter() {
        stencilAlpha = new int[1];
        stWidth = stHeight = 1;
    }

    /**
     * Creates a new stencil filter with the specified stencil image.
     *
     * @param stencil the stencil image whose alpha channel will mask the source
     * image
     */
    public StencilFilter(BufferedImage stencil) {
        createStencil(stencil);
    }

    /**
     * Sets the stencil to the specified image. The image's alpha channel data
     * is copied, so modifications made to the source image after this method
     * returns will not affect the behaviour of the stencil.
     *
     * @param stencil the stencil image whose alpha channel will mask the source
     * image
     */
    public void setStencil(BufferedImage stencil) {
        createStencil(stencil);
    }

    /**
     * Sets whether the stencil is applied in alpha replace mode, in which the
     * output image's alpha channel is copied unmodified from the stencil image.
     * This is the default. The alternative, when this is set to {@code false},
     * is that the alpha values from the source and stencil image are blended by
     * multiplying them together.
     *
     * @param replaceMode {@code true} if the stencil's alpha values should
     * simply replace the alpha values in the source; {@code false} if the alpha
     * values should be multiplied
     * @see #isAlphaReplaceMode()
     */
    public void setAlphaReplaceMode(boolean replaceMode) {
        replaceAlpha = replaceMode;
    }

    /**
     * Returns whether the stencil's alpha values are copied over
     * ({@code true}), or blended with the source image ({@code false}).
     *
     * @return {@code true} if the stencil's alpha values should simply replace
     * the alpha values in the source; {@code false} if the alpha values should
     * be multiplied
     * @see #setAlphaReplaceMode(boolean)
     */
    public boolean isAlphaReplaceMode() {
        return replaceAlpha;
    }

    private void createStencil(BufferedImage stencil) {
        if (stencil == null) {
            throw new NullPointerException("stencil");
        }

        stWidth = stencil.getWidth();
        stHeight = stencil.getHeight();
        stencilAlpha = getARGB(stencil, null);
        for (int i = 0; i < stencilAlpha.length; ++i) {
            stencilAlpha[i] &= 0xff000000;
        }
    }

    @Override
    public BufferedImage filter(BufferedImage source, BufferedImage dest) {
        if (source.getWidth() != stWidth || source.getHeight() != stHeight) {
            throw new IllegalArgumentException(
                    "source (" + source.getWidth() + 'x' + source.getHeight() + ") and stencil (" + stWidth + 'x' + stHeight + ") must be same size"
            );
        }
        currentSourceIsOpaque = source.getTransparency() == BufferedImage.OPAQUE;
        return super.filter(source, dest);
    }

    @Override
    public void filterPixels(int y, int[] argb) {
        int stOff = y * stWidth;
        if (currentSourceIsOpaque || replaceAlpha) {
            for (int i = 0; i < argb.length; ++i) {
                argb[i] = (argb[i] & 0x00ffffff) | stencilAlpha[stOff++];
            }
        } else {
            for (int i = 0; i < argb.length; ++i) {
                final int rgb = argb[i];
                int a = ((stencilAlpha[stOff++] >>> 24) * (rgb >>> 24)) / 255;
                argb[i] = (rgb & 0x00ffffff) | (a << 24);
            }
        }
    }

    private int[] stencilAlpha;
    private int stWidth, stHeight;
    private boolean replaceAlpha = true;
    private boolean currentSourceIsOpaque;
}
