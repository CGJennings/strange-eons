package gamedata;

import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.graphics.filters.AbstractImageFilter;
import ca.cgjennings.math.Interpolation;
import ca.cgjennings.ui.BlankIcon;
import java.awt.Paint;
import java.awt.image.BufferedImage;
import javax.swing.Icon;

/**
 * An abstract base class that provides default implementations for some methods
 * in {@link ExpansionSymbolTemplate}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public abstract class AbstractExpansionSymbolTemplate implements ExpansionSymbolTemplate {

    /**
     * {@inheritDoc}
     * <p>
     * The abstract base class will return the same value as
     * {@code getVariantCount()}.
     */
    @Override
    public int getLogicalVariantCount() {
        return getVariantCount();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The abstract base class will return the same value as
     * {@code getVariantName( variant )}.
     */
    @Override
    public String getLogicalVariantName(int variant) {
        return getVariantName(variant);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The abstract base class will return the same value as
     * {@code getVariantIcon()}.
     */
    @Override
    public Icon getLogicalVariantIcon(int variant) {
        return getVariantIcon(variant);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The abstract base class returns a icons that match the default variant
     * styles.
     *
     * @see SymbolVariantUtilities#createDefaultVariantIcon
     */
    @Override
    public Icon getVariantIcon(int variant) {
        if (VARIANT_ICONS == null) {
            VARIANT_ICONS = new Icon[VARIANT_RGB.length];
            for (int i = 0; i < VARIANT_RGB.length; ++i) {
                VARIANT_ICONS[i] = SymbolVariantUtilities.createDefaultVariantIcon(VARIANT_RGB[i]);
            }
        }
        if (variant < 0) {
            throw new IndexOutOfBoundsException("variant: " + variant);
        }
        if (variant >= VARIANT_ICONS.length) {
            return new BlankIcon(Expansion.ICON_WIDTH, Expansion.ICON_HEIGHT);
        }
        return VARIANT_ICONS[variant];
    }

    /**
     * {@inheritDoc}
     * <p>
     * This abstract base class always returns {@code null}, yielding a default
     * backdrop paint.
     */
    @Override
    public Paint getDesignBackdropForVariant(int variant) {
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This abstract base class returns {@code false}.
     *
     * @return {@code true} if {@link #generateVariant} is supported
     */
    @Override
    public boolean canGenerateVariantsAutomatically() {
        return false;
    }

    /**
     * Given an example image, generates a symbol variant automatically.
     *
     * <p>
     * If {@link #canGenerateVariantsAutomatically()} returns {@code false},
     * then this method throws an {@code UnsupportedOperationException}.
     * Otherwise, the abstract base class provides a default variant-generating
     * algorithm based on the icons for Arkham Horror. This can be used as-is
     * when appropriate, or if useful it can be used as a starting point for
     * generating an appropriate image for the game in question. (For example,
     * you could request variant 0 to get an image that is known to be
     * black-and-white and have an alpha channel as described below.)
     *
     * <p>
     * <b>Details of the default implementation:</b>
     * In Arkham Horror, there are two different styles for expansion symbols.
     * Older expansions used a single-colour dark logo which was inverted on
     * cards with a dark background. Newer expansions use a single logo embedded
     * in a circle. The circle has a dark outline and a light background, over
     * which is laid a dark logo. This allows the same graphic to be seen easily
     * on both light and dark cards. With Miskatonic Horror, a new "required"
     * variant has been added that replaces the dark colour with a golden one.
     *
     * <p>
     * To recreate these styles, this method proceeds as follows:
     * <ol>
     * <li> Convert the image to greyscale.
     * <li> Assume one of three cases:
     * <ol>
     * <li>The user has passed in a single-colour logo with proper transparency.
     * <li>The user has passed in a single-colour logo with no transparency,
     * such as a scan of a paper sketch.
     * <li>The user has passed in a mixed-colour logo (the new style) with
     * proper transparency.
     * </ol>
     * To determine which case applies, the image pixels are analyzed. If none
     * of the pixels have any transparency (alpha=255), case 2 is assumed. If
     * there is a small range of pixel values, case 1 is assumed (the small
     * range allows for some noise). Otherwise, case 3 is assumed.
     * <li> Cases 1 and 2 are based on the older single-colour logos. A logo is
     * created by setting each pixel to an appropriate colour for the variant
     * type. For case 1, the original transparency is kept. For case 2, the
     * transparency is taken from the pixel brightness: white pixels (paper)
     * will become transparent.
     * <li> For case 3, the range of brightness values present in the original
     * image will be mapped to a range of colours appropriate for the variant.
     * For variants 0 and 1, this is dark to white, For variant 2, this is
     * golden to white.
     * </ol>
     */
    @Override
    public BufferedImage generateVariant(BufferedImage baseSymbol, int variant) {
        if (!canGenerateVariantsAutomatically()) {
            throw new UnsupportedOperationException();
        }
        if (baseSymbol == null) {
            throw new NullPointerException("baseSymbol");
        }
        if (variant < 0 || variant > 2) {
            throw new IllegalArgumentException("variant: " + variant);
        }

        baseSymbol = ImageUtilities.ensureImageHasAlphaChannel(baseSymbol);
        BufferedImage grey = ImageUtilities.desaturate(baseSymbol);

        int[] pixels = AbstractImageFilter.getARGB(grey, null);

        int min = 255, max = 0;
        boolean opaque = true;
        for (int i = 0; i < pixels.length; ++i) {
            final int alpha = (pixels[i] >>> 24);
            if (alpha == 0) {
                opaque = false;
            } else if (alpha < 8) {
                // low alpha values tend to have very noisy pixel colours
                // (you can't see them, so you don't know they're wrong)
                // this will throw off our min/max calculations, so we treat
                // them as alpha = 0;
                opaque = false;
                pixels[i] = 0;
            } else {
//					// note: these lines increase the contrast of the original shade (b)
//					final int b = pixels[i] & 0xff;
//					final int v = Interpolation.clamp( (b - 128) * (int)(255 * 1.2f) + 128, 0, 255 );

                final int b = pixels[i] & 0xff;
                min = Math.min(b, min);
                max = Math.max(b, max);
            }
        }
        final int range = max - min;

        // RGB will contain the colour value for the selected variant
        // (dark, white, or golden)
        int RGB = VARIANT_RGB[variant];

        // (Case 2 in the description above)
        // If the image has no transparency, create it from  brightness
        // (assume black picture on white background); this is similar
        // to how the token editor creates its stencils...
        if (opaque) {
            for (int i = 0; i < pixels.length; ++i) {
                pixels[i] = ((0xff - (pixels[i] & 0xff)) << 24) | RGB;
            }
        } // (Case 1 in the description above)
        // ...else if there is a small range of brightness values,
        // assume that this is a single colour symbol...
        else if (range < 32) {
            for (int i = 0; i < pixels.length; ++i) {
                pixels[i] = (pixels[i] & 0xff00_0000) | RGB;
            }
        } // (Case 3 in the description above)
        // ...else scale the image between the target colour and white
        else {
            // Under Case 3, variants 0 and 1 produce the same image; if
            // we don't change this we'll be "scaling" between white and white
            if (variant == 1) {
                RGB = DARK;
            }

            // We will use this to determine the relative proportion of the
            // target colour to use in the output colour. While a simple
            // linear interpolation would work, this method will bias the
            // pixels towards the two ends of the scale (target and white).
            // A simple linear interpolation could be accomplished by
            // replacing the line "final float scale = ..." with:
            // final float scale = (float) ((pixels[i] & 0xff) - min) / (float) range
            Interpolation.CubicSpline splineFunction = new Interpolation.CubicSpline(
                    new double[]{min, min + range / 32, min + range / 2, max - range / 32, max},
                    new double[]{0d, 0d, 0.5d, 1d, 1d}
            );

            // split the target colour into separate R, G, and B values
            // to be scaled independently
            final int R = (RGB >> 16) & 0xff;
            final int G = (RGB >> 8) & 0xff;
            final int B = RGB & 0xff;

            for (int i = 0; i < pixels.length; ++i) {
                final int alphaChannel = pixels[i] & 0xff00_0000;
                if (alphaChannel != 0) {
                    final float scale = (float) splineFunction.f(pixels[i] & 0xff);

                    // use the scale value to interpolate between the target colour
                    // for the variant type and white
                    final int r = Math.min(Interpolation.lerp(scale, R, 0xff), 255);
                    final int g = Math.min(Interpolation.lerp(scale, G, 0xff), 255);
                    final int b = Math.min(Interpolation.lerp(scale, B, 0xff), 255);

                    // pack the (r,g,b) values back into pixels, keeping the alpha channel
                    pixels[i] = (alphaChannel) | (r << 16) | (g << 8) | b;
                }
            }
        }

        // We have now transformed the pixel data into something appropriate
        // for the requested variant; replace the data in our copy (the
        // greyscale image we made at the start) and return it.
        AbstractImageFilter.setARGB(grey, pixels);
        return grey;
    }

    private static final int DARK = 0x23_1f20;
    private static final int LIGHT = 0xff_ffff;
    private static final int GOLD = 0xc5_8930;
    private static final int[] VARIANT_RGB = {DARK, LIGHT, GOLD};
    private static Icon[] VARIANT_ICONS;

    /**
     * {@inheritDoc}
     * <p>
     * This abstract base class returns {@code false}.
     *
     * @return {@code true} if the component is responsible for drawing
     */
    @Override
    public boolean isCustomDrawn() {
        return false;
    }
}
