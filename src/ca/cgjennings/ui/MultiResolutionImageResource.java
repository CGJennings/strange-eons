package ca.cgjennings.ui;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.graphics.filters.PixelArtUpscalingFilter;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.AbstractMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import resources.ResourceKit;

/**
 * An image loaded from a resource URL that can support multiple resolutions.
 * Multiple source images may be provided using the familiar @Nx notation, and
 * the best version for the drawing context will be used automatically. For
 * example, you could provide resource files named {@code reload.png},
 * {@code reload@2x.png}, and {@code reload@3x.png}, and when drawn to a
 * graphics context the most appropriate version will be selected automatically.
 * Missing images will be filled in by scaling from an image that is available.
 * The following "@Nx" values are currently supported: "@1.25", "@1.5", "@1.75",
 * "@2", "@2.25", "@2.5", "@3", "@3.5", "@4"
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 * @since 3.4
 */
public class MultiResolutionImageResource extends AbstractMultiResolutionImage {

    /**
     * Images not wider and taller than this may be scaled using a special
     * algorithm for images with high spatial frequency.
     */
    private static final int HIGH_FREQ_LIMIT = 32;

    // labels for the scale variant indices to make the source code clearer:
    private static final int X1 = 0;
    private static final int X1_25 = 1;
    private static final int X1_5 = 2;
    private static final int X1_75 = 3;
    private static final int X2 = 4;
    private static final int X2_25 = 5;
    private static final int X2_5 = 6;
    private static final int X3 = 7;
    private static final int X3_5 = 8;
    private static final int X4 = 9;
    private static final int NUM_VARIANTS = 10;

    /**
     * The scaling factors for each supported size variant. This is a superset
     * of the possible Windows desktop scaling factors and Retina display
     * densities.
     */
    private static final float[] scales = new float[]{
        1f, 1.25f, 1.5f, 1.75f, 2f, 2.25f, 2.5f, 3f, 3.5f, 4f
    };

    /**
     * File name tags to add to the original source path for each scale.
     */
    private static final String[] tags = new String[]{
        "", "@1.25x", "@1.5x", "@1.75x", "@2x", "@2.25x", "@2.5x", "@3x", "@3.5x", "@4x"
    };

    /**
     * If a given variant is requested but not available, this is our first
     * choice for what to try to load as an alternative. For example, the first
     * choice for scale 1.25 is scale 2.5 because that is exactly double 1.25.
     */
    private static final int[] firstChoiceAlternate = new int[]{
        X2, X2_5, X3, X3_5, X4, X3, X3, X4, X4, X3
    };

    private final BufferedImage[] cached = new BufferedImage[NUM_VARIANTS];
    private final int[] scaledWidths = new int[NUM_VARIANTS];
    private final int[] scaledHeights = new int[NUM_VARIANTS];

    private final String prefix;
    private final String suffix;

    private boolean resampleCachedVariants = true;

    public MultiResolutionImageResource(String baseResource) {
        BufferedImage base = ResourceKit.getImageQuietly(baseResource);
        if (base == null) {
            base = ResourceKit.getMissingImage();
        }
        cached[X1] = base;

        int lastDot = baseResource.lastIndexOf('.');
        if (lastDot < 0) {
            prefix = baseResource;
            suffix = "";
        } else {
            prefix = baseResource.substring(0, lastDot);
            suffix = baseResource.substring(lastDot);
        }

        float fWidth = base.getWidth();
        float fHeight = base.getHeight();
        scaledWidths[X1] = base.getWidth();
        scaledHeights[X1] = base.getHeight();
        for (int i = 1; i < NUM_VARIANTS; ++i) {
            scaledWidths[i] = Math.round(fWidth * scales[i]);
            scaledHeights[i] = Math.round(fHeight * scales[i]);
        }
    }

    /**
     * Returns the resource path used to create this image.
     *
     * @return the original resource path with no resolution tags
     */
    public String getResource() {
        return prefix + suffix;
    }

    /**
     * Sets whether the image should assume that the image will be requested at
     * certain fixed scales repeatedly. This default is to assume that this is
     * true. For example, this assumption is true when the image will be painted
     * to a UI that is being scaled on a high DPI display, since the image will
     * be requested repeatedly at the fixed DPI scaling factor.
     *
     * <p>
     * When a scale is requested for which no "@Nx" variant exists, an
     * alternative image that does exist will be chosen instead. When this
     * property is true, the alternative image will be scaled to the exact size
     * that the variant image would be <em>if it existed</em>, and this
     * pre-scaled image may be retained to speed up future requests for the same
     * image. Setting this to false may reduce the memory footprint of this
     * instance at the possible cost of slower and/or lower quality results when
     * rendering the image.
     *
     * @param isFixed if true, it is assumed that images at certain fixed scales
     * will be requested repeatedly
     */
    public final void setFixedScaleAssumed(boolean isFixed) {
        if (resampleCachedVariants != isFixed) {
            resampleCachedVariants = isFixed;
            // if we are switching from not caching at exact scale
            // to caching at exact scale, then the current cached
            // items could be incorrect
            if (isFixed) {
                for (int i = 1; i < NUM_VARIANTS; ++i) {
                    cached[i] = null;
                }
            }
        }
    }

    public final boolean isFixedScaleAssumed() {
        return resampleCachedVariants;
    }

    /**
     * Given a string describing the variant, such as {@code "@2x"}, returns the
     * resource path that would contain that variant, if it exists. Subclasses
     * can override this to modify how the variant images are located.
     */
    protected String getResourcePathForVariant(float scale, String tag) {
        return prefix + tag + suffix;
    }

    /**
     * If there is an "@Nx" version of the image available, load and return it.
     * Otherwise return null.
     */
    protected BufferedImage loadVariantResource(int variant) {
        // base image is always loaded and exact
        if (variant == X1) {
            return cached[X1];
        }

        final String path = getResourcePathForVariant(scales[variant], tags[variant]);

        BufferedImage bi = null;
        if (ResourceKit.composeResourceURL(path) != null) {
            bi = ResourceKit.getImageQuietly(path);

            // there was an @Nx image file available; check that it
            // is the expected size (within +/- 1px) and warn if wrong
            if (StrangeEons.log.isLoggable(Level.WARNING)) {
                if (Math.abs(bi.getWidth() - scaledWidths[variant]) > 1
                        || Math.abs(bi.getHeight() - scaledHeights[variant]) > 1) {
                    final String warning
                            = "image \"" + path
                            + "\" is the wrong size; expected "
                            + scaledWidths[variant] + 'x' + scaledHeights[variant]
                            + " but got " + bi.getWidth() + 'x' + bi.getHeight();
                    StrangeEons.log.warning(warning);
                }
            }
        }
        return bi;
    }

    protected BufferedImage synthesizeVariant(int variant) {
        // the 2x and 4x sources, if they exist, are generally the best
        // sources for scaling
        BufferedImage source = cached[X1];
        if (variant < X2 && cached[X2] != null) {
            source = cached[X2];
        } else if (cached[X4] != null) {
            source = cached[X4];
        }

        int sw = source.getWidth();
        int sh = source.getHeight();

        int dw = scaledWidths[variant];
        int dh = scaledHeights[variant];

        // if the image needs scaling up AND it is within the high frequency
        // image range, scale it up using the special high frequency filter
        if (dw > sw || dh > sh) {
            while (sw <= HIGH_FREQ_LIMIT && sh <= HIGH_FREQ_LIMIT && dw > sw && dh > sh) {
                source = new PixelArtUpscalingFilter().filter(source, null);
                sw *= 2;
                sh *= 2;
            }
        }

        return ImageUtilities.resample(source, dw, dh);
    }

    /**
     * Returns the best image to use for the requested scale variant.
     *
     * @param variant the index of the desired "@Nx" variant
     * @param cache if true, and the image must be loaded or generated, then it
     * will be stored in an internal cache for fast future access
     * @return the non-null variant image for the requested scale index
     */
    protected BufferedImage getImageVariant(int variant, boolean cache) {
        if (cached[variant] != null) {
            return cached[variant];
        }

        boolean usingFallback = false;
        BufferedImage bi = loadVariantResource(variant);
        if (bi == null) {
            usingFallback = true;

            // first, try using the "ideal" source image to base this variant on
            int alternate = firstChoiceAlternate[variant];
            bi = loadVariantResource(alternate);

            // if that fails, try some common sizes 2x/3x/4x
            // BUT: only try a few sizes in total to minimize the response delay
            if (bi == null) {
                if (variant < X2 && alternate != X2) {
                    bi = loadVariantResource(X2);
                }
                if (bi == null && variant < X3 && alternate != X3) {
                    bi = loadVariantResource(X3);
                }
                if (bi == null) {
                    bi = loadVariantResource(X4);
                }
                // finally, use an existing image to create the target image
                if (bi == null) {
                    bi = synthesizeVariant(variant);
                }
            }
        }

        // if we did not find the exact image requested, we may optionally
        if (usingFallback && resampleCachedVariants) {
            bi = ImageUtilities.resample(bi, scaledWidths[variant], scaledHeights[variant]);
        }

        if (DEBUG) {
            applyDebugEffect(bi, variant);
        }

        if (cache) {
            cached[variant] = bi;
        }

        return bi;
    }

    @Override
    public BufferedImage getBaseImage() {
        return cached[0];
    }

    @Override
    @SuppressWarnings("empty-statement")
    public BufferedImage getResolutionVariant(double destImageWidth, double destImageHeight) {
        int destW = (int) (destImageWidth + 0.5d);
        int destH = (int) (destImageHeight + 0.5d);

        // for both the target width and the target height,
        // find the variant that will be at least that large;
        // we will choose the variant for whichever dimension ends up
        // giving us the largest source image
        int varForW, varForH;
        for (varForW = 0; scaledWidths[varForW] < destW && varForW < scales.length; ++varForW);
        for (varForH = 0; scaledHeights[varForH] < destH && varForH < scales.length; ++varForH);

        final int variant = Math.min(scales.length - 1, Math.max(varForW, varForH));
        return getImageVariant(variant, true);
    }

    @Override
    public List<Image> getResolutionVariants() {
        List<Image> list = new ArrayList<>();
        for (int i = 0; i < scales.length; ++i) {
            list.add(getImageVariant(i, false));
        }
        return Collections.unmodifiableList(list);
    }

    // Set this to true to get feedback on which resolution is being selected
    // or to check for icons in the UI that are not multi-res
    private static final boolean DEBUG = false;

    private static void applyDebugEffect(BufferedImage src, int variant) {
        Graphics2D g = src.createGraphics();
        try {
            float fontSize = 8f * scales[variant];
            g.setColor(Color.RED);
            g.drawRect(0, 0, src.getWidth() - 1, src.getHeight() - 1);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) (fontSize + 0.5f)));
            g.drawString(tags[variant].substring(1), 1f, fontSize);
        } finally {
            g.dispose();
        }
    }
}
