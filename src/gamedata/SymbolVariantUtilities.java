package gamedata;

import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.graphics.filters.AbstractImageFilter;
import ca.cgjennings.graphics.filters.BlurFilter;
import ca.cgjennings.graphics.filters.ColorOverlayFilter;
import ca.cgjennings.graphics.filters.GlowFilter;
import ca.cgjennings.graphics.filters.StrokeFilter;
import ca.cgjennings.math.Interpolation;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import resources.ResourceKit;

/**
 * This class provides some static utility methods that can aid plug-in
 * developers when implementing {@link ExpansionSymbolTemplate#generateVariant}.
 * Much of the functionality in this class is also available from other parts of
 * the Strange Eons API, but this class gathers it in one place and provides a
 * consistent interface, making it easier to think it terms of the higher-level
 * steps needed to achieve the desired effect.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see ExpansionSymbolTemplate#canGenerateVariantsAutomatically()
 */
public final class SymbolVariantUtilities {

    private SymbolVariantUtilities() {
    }

    /**
     * Creates a base symbol to be used as a starting point from an arbitrary
     * image supplied by the user. The resulting image is guaranteed to:
     * <ol>
     * <li> support transparency (have an alpha channel)
     * <li> have a greyscale colour palette
     * </ol>
     * This method uses a set of heuristics to infer how the user meant the
     * image to be used as a symbol while still fulfilling the restrictions
     * above. It operates as follows:
     * <ol>
     * <li> Desaturate the image (convert it to greyscale).
     * <li> If the image does not have an alpha channel, then it is assumed that
     * the image consists of a dark symbol drawn on a light background. An alpha
     * channel will be created by setting each pixel's alpha value according to
     * the pixel's brightness (light pixels are more transparent). The
     * brightness of each pixel will the be set to 0. (The result is this a
     * black symbol with transparent areas.)
     * <li> If the image already has an alpha channel, then that alpha channel
     * is retained. If the image is greyscale, then the red, green, and blue
     * channels are also retained. Otherwise, the RGB portion of each pixel is
     * converted to black (use {@link #recolor} if you require a different base
     * colour).
     * <li> The alpha channel of the resulting image has some noise removed by
     * driving alpha values that are very close to 0 down to 0. (Noise in the
     * alpha channel will tend to be amplified by other effects that may be
     * applied.)
     * <li> The final, noise-cleaned image is then returned.
     * </ol>
     *
     * @param source the source image to convert into an image suitable for use
     * as a symbol
     * @return the converted image
     */
    public static BufferedImage extractSymbol(BufferedImage source) {
        boolean alreadyGrey = isGreyscale(source);
        source = ImageUtilities.ensureImageHasAlphaChannel(source);
        if (!alreadyGrey) {
            source = ImageUtilities.desaturate(source);
        }
        int[] pixels = AbstractImageFilter.getARGB(source, null);
        boolean hasAlpha = false;

        int min = 255, max = 0, sum = 0;
        for (int i = 0; i < pixels.length; ++i) {
            if ((pixels[i] >>> 24) != 0xff) {
                hasAlpha = true;
            }
            final int bri = pixels[i] & 0xff;
            min = Math.min(min, bri);
            max = Math.max(max, bri);
            sum += bri;
        }
        if (hasAlpha) {
            if (!alreadyGrey) {
                extractTransparent(pixels, min, max);
            }
        } else {
            extractOpaque(pixels, min, max);
        }
        // remove some noise from the alpha channel; many effects will amplify this
        for (int i = 0; i < pixels.length; ++i) {
            // clear alpha if below threshold
            int argb = pixels[i];
            if ((argb >>> 24) < 8) {
                pixels[i] = argb & 0xff_ffff;
            }
        }

        AbstractImageFilter.setARGB(source, pixels);
        return source;
    }

    private static void extractOpaque(int[] pixels, int min, int max) {
        final int range = max - min;
        if (range == 0) {
            return;
        }
        for (int i = 0; i < pixels.length; ++i) {
            int alpha = Interpolation.map(pixels[i] & 0xff, min, max, 255, 0);
            pixels[i] = alpha << 24;
        }
    }

    private static void extractTransparent(int[] pixels, int min, int max) {
        for (int i = 0; i < pixels.length; ++i) {
            pixels[i] &= 0xff00_0000;
        }
    }

    /**
     * Trims off any completely transparent edges, ensuring that the border of
     * the image contains at least one pixel with a non-zero alpha. It is a good
     * idea to call this before scaling the image to its
     * {@link #standardizeSize}. Otherwise images with transparent edges will
     * appear to be smaller than the standard size even though they are not.
     *
     * @param source the image to trim
     * @return the image, with transparent sides cropped off
     * @see ImageUtilities#trim
     */
    public static BufferedImage trim(BufferedImage source) {
        return ImageUtilities.trim(source);
    }

    /**
     * Returns a greyscale version of an image.
     *
     * @param source the image to desaturate
     * @return a greyscale version of the source image
     * @see ImageUtilities#desaturate
     */
    public static BufferedImage desaturate(BufferedImage source) {
        return ImageUtilities.desaturate(source);
    }

    /**
     * Converts the image to a standard size. It is best to choose a standard
     * size for your symbols and covert the base image to that size before
     * applying other effects. This is necessary because the image will be
     * scaled to fit the expansion symbol region of the sheet it is painted on,
     * and you want your effects to scale consistently. (For example, suppose
     * that you did not standardize the size and you apply a 4-pixel wide
     * outline to the base image. If one expansion symbol image is twice as wide
     * as another, then when it is scaled to fit into the expansion symbol box
     * it will be scaled to half the size, so it will look like you applied a
     * 2-pixel wide outline.)
     *
     * <p>
     * This method will produce a square image that is {@code size} by
     * {@code size} pixels. The largest dimension will be scaled up or down as
     * needed, and the opposite dimension will be padded with completely
     * transparent pixels, if necessary, to fill it out.
     *
     * <p>
     * The following is a good rule of thumb for choosing a standard size:
     * <ol>
     * <li> Choose the largest expansion symbol region on all of your
     * components. Take this as the starting size, <i>s</i>. If the expansion
     * symbol region isn't square, take the larger dimension (though a better
     * idea would be to make it square, if possible).
     * <li> To ensure that symbols look good when printed (assuming a large
     * enough source image was provided), multiply <i>s</i> by 300/<i>t</i>,
     * where <i>t</i> is the resolution of the template image for your
     * components. (If you have not explicitly set a resolution, it will be 150,
     * meaning that you should multiply by 2.) This is your standard size.
     * </ol>
     *
     * @param source the base image to convert to standard size
     * @param size the standard size
     * @return a square image at the standard size, containing the same design
     * as the source image
     * @see ImageUtilities#resample
     * @see ImageUtilities#center
     */
    public static BufferedImage standardizeSize(BufferedImage source, int size) {
        float scale = ImageUtilities.idealBoundingScaleForImage(size, size, source.getWidth(), source.getHeight());
        if (scale != 1f) {
            source = ImageUtilities.resample(source, scale);
        }
        if (source.getWidth() != size || source.getHeight() != size) {
            source = ImageUtilities.center(source, size, size);
        }
        return source;
    }

    /**
     * Adds transparent padding to each edge of the image equal to
     * {@code paddingSize}. This is useful when you are about to apply an effect
     * that grows from the edge of the original image.
     *
     * @param source the original image
     * @param paddingSize the number of rows or columns to add to each side of
     * the image
     * @return an image that is larger in both dimensions by twice the padding
     * size
     */
    public static BufferedImage pad(BufferedImage source, int paddingSize) {
        return ImageUtilities.pad(source, paddingSize, paddingSize, paddingSize, paddingSize);
    }

    /**
     * Adds transparent padding to each edge of the image equal. This is useful
     * when you are about to apply an effect that grows from the edge of the
     * original image.
     *
     * @param source the original image
     * @param top the number of transparent rows to add to the top edge
     * @param left the number of transparent columns to add to the left edge
     * @param bottom the number of transparent rows to add to the bottom edge
     * @param right the number of transparent columns to add to the right edge
     * @return an image that is larger in both dimensions by twice the padding
     * size
     */
    public static BufferedImage pad(BufferedImage source, int top, int left, int bottom, int right) {
        return ImageUtilities.pad(source, top, left, bottom, right);
    }

    /**
     * Returns a stroked version of the source image. Note that the outline
     * colour includes an alpha value; if you pass a colour with an alpha value
     * of zero then the outline will be invisible.
     *
     * <p>
     * This method automatically pads the source image to add enough space to
     * stroke the pixels at th edge of the image.
     *
     * @param source the image to outline
     * @param outlineColor the colour of the outline (including an alpha value)
     * @param outlineWidth the width of the outline, in pixels
     * @return an outlined version of the source image
     * @see StrokeFilter
     */
    public static BufferedImage outline(BufferedImage source, int outlineColor, int outlineWidth) {
        return new StrokeFilter(outlineColor, outlineWidth).filter(pad(source, outlineWidth + 1), null);
    }

    /**
     * Returns a stroked version of the source image. This is identical to
     * {@link #outline(java.awt.image.BufferedImage, int, int)} except that it
     * takes a {@code Color} instance for the outline colour.
     *
     * @param source the image to outline
     * @param outlineColor the colour of the outline
     * @param outlineWidth the width of the outline, in pixels
     * @return an outlined version of the source image
     * @see StrokeFilter
     */
    public static BufferedImage outline(BufferedImage source, Color outlineColor, int outlineWidth) {
        return outline(source, outlineColor.getRGB(), outlineWidth);
    }

    /**
     * Returns a version of the image with a drop shadow. Note that the shadow
     * colour includes an alpha value; if you pass a colour value with an alpha
     * of zero the shadow will be invisible.
     *
     * <p>
     * This method automatically pads the source image to make enough space for
     * the shadow.
     *
     * @param source the image to shade
     * @param angle the anti-clockwise angle from the x-axis, in degrees
     * @param distance the distance from the source image to the shadow; use
     * zero for a glow effect
     * @param shadowColor the shadow colour (including an alpha value)
     * @param size the shadow size; the higher this value, the fuzzier the
     * shadow edge will be
     * @param strength the strength of the shadow effect (must be positive; 1 is
     * standard)
     * @return an image depicting the source image floating over a drop shadow
     * with the given parameters
     * @see ColorOverlayFilter
     * @see BlurFilter
     * @see GlowFilter
     */
    public static BufferedImage shadow(BufferedImage source, double angle, int distance, int shadowColor, int size, int strength) {
        int padding = distance + size + 1;
        int stencilPadding = size + 1;

        int iterations = 1;
        if (size > 1) {
            ++iterations;
            --size;
        }

        BufferedImage shadow = new ColorOverlayFilter(shadowColor).filter(source, null);
        shadow = pad(shadow, stencilPadding);
        shadow = new BlurFilter(size, iterations).filter(shadow, null);

        BufferedImage out = new BufferedImage(source.getWidth() + padding * 2, source.getHeight() + padding * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            angle = -Math.toRadians(angle);
            int xOffset = 0, yOffset = 0;
            if (distance != 0) {
                xOffset = (int) (Math.cos(angle) * distance);
                yOffset = (int) (Math.sin(angle) * distance);
            }
            strength = Math.max(1, strength);
            for (int i = 0; i < strength; ++i) {
                g.drawImage(shadow, padding + xOffset - stencilPadding, padding + yOffset - stencilPadding, null);
            }
            g.drawImage(source, padding, padding, null);
        } finally {
            g.dispose();
        }

        return trim(out);
    }

    /**
     * Returns a version of the image with a drop shadow. This is identical to
     * {@link #shadow(java.awt.image.BufferedImage, double, int, int, int, int)}
     * except that it takes a {@code Color} instance for the shadow colour.
     *
     * @param source the image to shade
     * @param angle the anti-clockwise angle from the x-axis, in degrees
     * @param distance the distance from the source image to the shadow; use
     * zero for a glow effect
     * @param shadowColor the shadow colour
     * @param size the shadow size; the higher this value, the fuzzier the
     * shadow edge will be
     * @param strength the strength of the shadow effect (must be positive; 1 is
     * standard)
     * @return an image depicting the source image floating over a drop shadow
     * with the given parameters
     */
    public static BufferedImage shadow(BufferedImage source, double angle, int distance, Color shadowColor, int size, int strength) {
        return shadow(source, angle, distance, shadowColor.getRGB(), size, strength);
    }

    /**
     * Recolours an image with a single colour. Every pixel will be set to this
     * colour; the alpha channel will not be affected, so the shape will remain
     * the same.
     *
     * @param source the source image
     * @param color the colour value (alpha is ignored)
     * @return an image where every pixel takes on the given color but retains
     * its original transparency
     * @see ColorOverlayFilter
     */
    public static BufferedImage recolor(BufferedImage source, int color) {
        return new ColorOverlayFilter(color).filter(source, null);
    }

    /**
     * Recolours an image with a single colour. This is identical to
     * {@link #recolor(java.awt.image.BufferedImage, int)} except that it takes
     * a {@code Color} instance instead of an integer RGB value.
     *
     * @param source the source image
     * @param color the colour value (alpha is ignored)
     * @return an image where every pixel takes on the given color but retains
     * its original transparency
     * @see ColorOverlayFilter
     */
    public static BufferedImage recolor(BufferedImage source, Color color) {
        return recolor(source, color.getRGB());
    }

    /**
     * Returns {@code true} if and only if every pixel in the source image has
     * the same RGB value (the alpha channel may vary). This can determine, for
     * example, if {@link #extractSymbol} has converted the image to pure black
     * or greyscale.
     *
     * @param source the image to check
     * @return {@code true} if every pixel in the source is the same colour
     */
    public static boolean isMonochrome(BufferedImage source) {
        int[] pixels = AbstractImageFilter.getARGB(source, null);
        int exemplar = pixels[0] & 0xff_ffff;
        for (int i = 1; i < pixels.length; ++i) {
            if (exemplar != (pixels[i] & 0xff_ffff)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if and only if, for every pixel in the source image,
     * that pixel has identical red, green and blue levels.
     *
     * @param source the image to check
     * @return {@code true} if every pixel in the source is grey
     */
    public static boolean isGreyscale(BufferedImage source) {
        int[] pixels = AbstractImageFilter.getARGB(source, null);
        for (int i = 0; i < pixels.length; ++i) {
            final int rgb = pixels[i] & 0xff_ffff;
            final int r = rgb >> 16 & 0xff;
            final int g = rgb >> 8 & 0xff;
            final int b = rgb & 0xff;
            if (r != g || r != b || b != g) {
                return false;
            }
        }
        return true;
    }

    /**
     * Recolours an image. The source image is assumed to be greyscale. The
     * darkest pixels will be coloured with {@code color1}, the lightest pixels
     * will be coloured with {@code color2}, and pixels whose brightness is in
     * between will be interpolate between the two extremes. The interpolation
     * is non-linear, leaving a margin around the brightest and darkest pixel
     * values to allow for some noise in the image.
     *
     * <p>
     * <b>Note:</b> This effect is applied in-place; the returned image is the
     * same as the image that is passed in.
     *
     * @param source the source image
     * @param color1 the dark pixel colour value (alpha is ignored)
     * @param color2 the light pixel colour value (alpha is ignored)
     * @return an image where every pixel is recoloured based on its brightness
     * by interpolating between the two colours; transparency is unaffected.
     * @see ca.cgjennings.math.Interpolation.CubicSpline
     */
    public static BufferedImage recolor(BufferedImage source, int color1, int color2) {
        final int R1 = color1 >> 16 & 0xff;
        final int G1 = color1 >> 8 & 0xff;
        final int B1 = color1 & 0xff;

        final int R2 = color2 >> 16 & 0xff;
        final int G2 = color2 >> 8 & 0xff;
        final int B2 = color2 & 0xff;

        int min = 255, max = 0;
        int[] pixels = AbstractImageFilter.getARGB(source, null);
        for (int i = 0; i < pixels.length; ++i) {
            final int bri = pixels[i] & 0xff;
            min = Math.min(min, bri);
            max = Math.max(max, bri);
        }
        final int range = max - min;

        Interpolation.CubicSpline splineFunction = new Interpolation.CubicSpline(
                new double[]{min, min + range / 32, min + range / 2, max - range / 32, max},
                new double[]{0d, 0d, 0.5d, 1d, 1d}
        );

        for (int i = 0; i < pixels.length; ++i) {
            final int alpha = pixels[i] & 0xff00_0000;
            if (alpha != 0) {
                final int bri = pixels[i] & 0xff;
                final float scale = (float) splineFunction.f(bri);

                // use the scale value to interpolate between the colours
                final int r = Math.min(Interpolation.lerp(scale, R1, R2), 255);
                final int g = Math.min(Interpolation.lerp(scale, G1, G2), 255);
                final int b = Math.min(Interpolation.lerp(scale, B1, B2), 255);

                // pack the (r,g,b) values back into pixels, keeping the alpha channel
                pixels[i] = alpha | (r << 16) | (g << 8) | b;
            }
        }

        AbstractImageFilter.setARGB(source, pixels);
        return source;
    }

    /**
     * Recolours an image. This is identical to
     * {@link #recolor(java.awt.image.BufferedImage, int, int)} except that it
     * takes {@code Color} instances instead of integer RGB values.
     *
     * <p>
     * <b>Note:</b> This effect is applied in-place; the returned image is the
     * same as the image that is passed in.
     *
     * @param source the source image
     * @param color1 the dark pixel colour value (alpha is ignored)
     * @param color2 the light pixel colour value (alpha is ignored)
     * @return an image where every pixel is recoloured based on its brightness
     * by interpolating between the two colours; transparency is unaffected.
     */
    public static BufferedImage recolor(BufferedImage source, Color color1, Color color2) {
        return recolor(source, color1.getRGB(), color2.getRGB());
    }

    /**
     * Returns a copy of the source image.
     *
     * @param source the image to copy
     * @return a copy of the image
     * @see ImageUtilities#copy
     */
    public static BufferedImage copy(BufferedImage source) {
        return ImageUtilities.copy(source);
    }

//	public static void main( String[] args ) {
//		EventQueue.invokeLater( new Runnable() {
//			@Override
//			public void run() {
//				JFrame f = new JFrame();
//				f.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
//				f.setSize( 640, 800 );
//				final JLabel i = new JLabel();
//				i.setOpaque( true );
//				i.setBackground( Color.CYAN );
//				f.add( i );
//				new FileDrop(  i, new FileDrop.Listener() {
//					@Override
//					public void filesDropped( File[] files ) {
//						try {
//							BufferedImage bi = ImageIO.read( files[0] );
//							if( bi == null ) return;
//							bi = extractSymbol( bi );
//							bi = standardizeSize( bi, 78 );
////							bi = outline( bi, 0xffaa7744, 4 );
//							bi = shadow( bi, 0, 0, 0xff000000, 4, 2 );
////							bi = recolor( bi, 0xff0000, 0x00ff00 );
//
//							bi =ImageUtilities.resample( bi, 8f );
//							Graphics2D g = bi.createGraphics();
//							try {
//								g.setColor( Color.MAGENTA );
//								g.drawRect( 0, 0, bi.getWidth()-1, bi.getHeight()-1 );
//							} finally {
//								g.dispose();
//							}
//							i.setIcon( new ImageIcon( bi ) );
//						} catch( Exception e ) {
//							e.printStackTrace();
//						}
//					}
//				});
//				f.setLocationRelativeTo( null );
//				f.setVisible( true );
//			}
//		});
//	}
    /**
     * Creates an icon that can be returned from
     * {@link ExpansionSymbolTemplate#getVariantIcon(int)}. The icon uses a
     * standard design along with a characteristic Paint or Color to represent
     * the style of the variant.
     *
     * @param characteristicPaint a key design element of the variant
     * @return an icon that can be used to represent the variant visually
     */
    public static Icon createDefaultVariantIcon(Paint characteristicPaint) {
        BufferedImage circle = ResourceKit.getImage("expansiontokens/default-variant-base.png");
        BufferedImage icon = new BufferedImage(circle.getWidth(), circle.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        try {
            g.setPaint(characteristicPaint);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.fillOval(1, 1, 14, 14);
            g.drawImage(circle, 0, 0, null);
        } finally {
            g.dispose();
        }
        return new ImageIcon(icon);
    }

    /**
     * Creates an icon that can be returned from
     * {@link ExpansionSymbolTemplate#getVariantIcon(int)}. This is a
     * convenience method that creates an icon based on the colour specified by
     * the given RGB value.
     *
     * @param rgb the 24-bit RGB colour value to create the icon for
     * @return an icon that can be used to represent the variant visually
     * @see #createDefaultVariantIcon(java.awt.Paint)
     */
    public static Icon createDefaultVariantIcon(int rgb) {
        return createDefaultVariantIcon(new Color(rgb));
    }

}
