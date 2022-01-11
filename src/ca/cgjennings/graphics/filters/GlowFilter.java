package ca.cgjennings.graphics.filters;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * A filter that applies a glow effect around the edge defined by the
 * non-transparent parts of an image.
 *
 * <p>
 * If either the number of blurring iterations or the effect strength is zero,
 * the glow effect will not be visible.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class GlowFilter extends AbstractImageFilter {

    private int radius = 4;
    private int strength = 1;
    private int iterations = 1;
    private int strokeWidth = 2;
    private int color = 0xaaffffbb;
    private Color colorTemp;
    private boolean outer = true;
    private boolean paintSourceImage = true;

    /**
     * Creates a new glow filter with a yellow-white glow, width of 2, blur
     * radius of 4, 1 iteration, strength of 2, and an outer glow.
     */
    public GlowFilter() {
    }

    /**
     * Creates a new glow filter.
     *
     * @param color the color, specified as an sRGB integer
     * @param blurRadius the blur radius for the glow
     * @param outerGlow if {@code true}, the filter produces an outer glow
     * @param strength the strength of the glow effect
     */
    public GlowFilter(int color, int blurRadius, boolean outerGlow, int strength) {
        this(color, 2, blurRadius, 1, outerGlow, strength);
    }

    /**
     * Creates a new glow filter.
     *
     * @param color the color, specified as an sRGB integer
     * @param blurRadius the blur radius for the glow
     * @param outerGlow if {@code true}, the filter produces an outer glow
     * @param strength the strength of the glow effect
     */
    public GlowFilter(Color color, int blurRadius, boolean outerGlow, int strength) {
        this(color, 2, blurRadius, 1, outerGlow, strength);
    }

    /**
     * Creates a new glow filter.
     *
     * @param color the color, specified as an sRGB integer
     * @param strokeWidth the width of the glow, exclusive of blurring
     * @param blurRadius the blur radius for the glow
     * @param iterations the number of iterations of the blurring effect
     * @param outerGlow if {@code true}, the filter produces an outer glow
     * @param strength the strength of the glow effect
     */
    public GlowFilter(int color, int strokeWidth, int blurRadius, int iterations, boolean outerGlow, int strength) {
        this.color = color;
        setWidth(strokeWidth);
        setBlurRadius(radius);
        setIterations(iterations);
        setStrength(strength);
        outer = outerGlow;
    }

    /**
     * Creates a new glow filter.
     *
     * @param color the color, specified as an sRGB integer
     * @param strokeWidth the width of the glow, exclusive of blurring
     * @param blurRadius the blur radius for the glow
     * @param iterations the number of iterations of the blurring effect
     * @param outerGlow if {@code true}, the filter produces an outer glow
     * @param strength the strength of the glow effect
     */
    public GlowFilter(Color color, int strokeWidth, int blurRadius, int iterations, boolean outerGlow, int strength) {
        setColor(color);
        setWidth(strokeWidth);
        setBlurRadius(radius);
        setIterations(iterations);
        setStrength(strength);
        outer = outerGlow;
    }

    /**
     * Returns the current glow colour as an ARGB integer value.
     *
     * @return the glow colour in 0xAARRGGBB format
     */
    public int getColorRGB() {
        return color;
    }

    /**
     * Sets the current glow colour using an ARGB integer.
     *
     * @param color the glow colour in 0xAARRGGBB format
     */
    public void setColorRGB(int color) {
        this.color = color;
        colorTemp = null;
    }

    /**
     * Returns the current glow colour as an ARGB integer value.
     *
     * @return the glow colour
     */
    public Color getColor() {
        if (colorTemp == null) {
            colorTemp = new Color(color, true);
        }
        return colorTemp;
    }

    /**
     * Sets the current glow colour using a {@code Color} instance.
     *
     * @param color the glow color
     */
    public void setColor(Color color) {
        this.color = color.getRGB();
        colorTemp = color;
    }

    /**
     * Sets the blurring radius of the glow effect.
     *
     * @param radius the blur radius of the glow effect, in pixels
     */
    public void setBlurRadius(int radius) {
        this.radius = radius;
    }

    /**
     * Returns the radius of the blurring applied to the glow effect.
     *
     * @return the blur radius of the glow effect, in pixels
     */
    public int getBlurRadius() {
        return radius;
    }

    /**
     * Sets the blurring radius of the glow effect.
     *
     * @param blurRadius the blur radius of the glow effect, in pixels
     * @deprecated Replaced by {@link #setBlurRadius(int)}.
     */
    @Deprecated
    public void setDistance(int blurRadius) {
        setBlurRadius(blurRadius);
    }

    /**
     * Returns the radius of the blurring applied to the glow effect.
     *
     * @return the blur radius of the glow effect, in pixels
     * @deprecated Replaced by {@link #getBlurRadius()}.
     */
    @Deprecated
    public int getDistance() {
        return getBlurRadius();
    }

    /**
     * The size of glow, exclusive of blurring.
     *
     * @param size the glow size, in pixels
     */
    public void setWidth(int size) {
        strokeWidth = size;
    }

    /**
     * Returns the glow size, exclusive of blurring.
     *
     * @return the glow size, in pixels
     */
    public int getWidth() {
        return strokeWidth;
    }

    /**
     * Returns {@code true} if the filter will produce an outer glow, or
     * {@code false} if the filter will produce an inner glow.
     *
     * @return {@code true} for an outer glow type; {@code false} for an inner
     * glow type
     */
    public boolean isOuterGlow() {
        return outer;
    }

    /**
     * Sets the type of glow effect to grow inward from the edge, outward from
     * the edge, or both.
     *
     * @param outerGlow {@code true} for an outer glow type; {@code false} for
     * an inner glow type
     */
    public void setOuterGlow(boolean outerGlow) {
        outer = outerGlow;
    }

    /**
     * Returns the strength of the glow effect. Higher values produce a more
     * intense glow.
     *
     * @return the positive glow effect strength
     */
    public int getStrength() {
        return strength;
    }

    /**
     * Sets the strength of the glow effect. Any non-negative value is valid,
     * but typical values are in the range 1 to 8. Higher values will produce a
     * more intense (less translucent) glow.
     *
     * @param strength the positive intensity of the glow effect
     */
    public void setStrength(int strength) {
        if (strength < 0) {
            throw new IllegalArgumentException("strength < 0: " + strength);
        }
        this.strength = strength;
        if (strength <= 1) {
            strengthFilter = null;
        } else {
            if (strengthFilter == null) {
                strengthFilter = new AlphaStrengthenFilter();
            }
            strengthFilter.setStrengthFactor(strength - 1);
        }
    }
    private AlphaStrengthenFilter strengthFilter;

    /**
     * Sets the number of blurring iterations that will be applied to the glow
     * effect. This value may be 0, but cannot be negative. The default is one;
     * higher values will produce a smoother glow effect but will also increase
     * the extent of the glow.
     */
    public void setIterations(int iterations) {
        if (iterations < 0) {
            throw new IllegalArgumentException("iterations < 0: " + iterations);
        }
        this.iterations = iterations;
    }

    /**
     * Returns the number of blurring iterations that will be applied to the
     * glow effect. This value may be 0, but cannot be negative.
     *
     * @return the number of times the blur effect is applied
     */
    public int getIterations() {
        return iterations;
    }

    /**
     * Sets whether the original image will be included in the output. If
     * {@code true}, the glow effect will be combined with the original image.
     *
     * @param paintSource {@code true} to include the image the effect applies
     * to, {@code false} to produce the glow effect only
     */
    public void setSourceImagePainted(boolean paintSource) {
        paintSourceImage = paintSource;
    }

    /**
     * Returns {@code true} if the original source image appears in the
     * destination.
     *
     * @return {@code true} to include the image the effect applies to,
     * {@code false} to produce the glow effect only
     */
    public boolean isSourceImagePainted() {
        return paintSourceImage;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        if (dest == null) {
            dest = createCompatibleDestImage(src, src.getColorModel());
        }

        if (outer) {
            filterOuter(src, dest);
        } else {
            filterInner(src, dest);
        }

        return dest;
    }

    private void filterOuter(BufferedImage src, BufferedImage dest) {
        final int width = src.getWidth();
        final int height = src.getHeight();

        final int iterations = strength == 0 ? 0 : this.iterations;
        int[] in = getARGB(src, null);
        int[] out = new int[in.length];

        if (strokeWidth > 0) {
            in = StrokeFilter.boxOutlineFilter(in, null, width, height, strokeWidth, false, true, color);
            BlurFilter.blur(in, out, width, height, radius, radius, iterations, true, color);
        } else {
            BlurFilter.blur(in, out, width, height, radius, radius, iterations, true, color);
        }

        setARGB(dest, in);
        if (strength > 1) {
            strengthFilter.filter(dest, dest);
        }

        if (paintSourceImage) {
            Graphics2D g = dest.createGraphics();
            try {
                g.drawImage(src, 0, 0, null);
            } finally {
                g.dispose();
            }
        }
    }

    private void filterInner(BufferedImage src, BufferedImage dest) {
        final int width = src.getWidth();
        final int height = src.getHeight();

        int[] in = getARGB(src, null);
        int[] out = new int[in.length];

        // reverse the alpha channel so that the glow goes inward
        ColorOverlayFilter.overlay(in, width, height, color, true);

        final int iterations = strength == 0 ? 0 : this.iterations;

        if (strokeWidth > 0) {
            in = StrokeFilter.boxOutlineFilter(in, null, width, height, strokeWidth, false, true, color);
            BlurFilter.blur(in, out, width, height, radius, radius, iterations, true, color);
        } else {
            BlurFilter.blur(in, out, width, height, radius, radius, iterations, true, color);
        }

        // If we are painting the original object, we need a temporary image to put the
        // glow effect in since the glow needs to be painted *over* the original.
        BufferedImage t;
        if (paintSourceImage) {
            t = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        } else {
            t = dest;
            // if you do this, it looks right but you can't use the output
            // to create an inner shadow
//			multiplyAlphaChannel( in, getARGB( src, null ) );
        }

        setARGB(t, in);
        if (strength > 1) {
            strengthFilter.filter(t, t);
        }

        if (paintSourceImage) {
            Graphics2D g = dest.createGraphics();
            try {
                g.drawImage(src, 0, 0, null);
                g.setComposite(AlphaComposite.SrcAtop);
                g.drawImage(t, 0, 0, null);
            } finally {
                g.dispose();
            }
        }
    }

//	static void multiplyAlphaChannel( int[] target, int[] alphaSource ) {
//		for( int i=0; i<target.length; ++i ) {
//			final int at = (target[i] >>> 24);
//			final int as = (alphaSource[i] >>> 24);
//			target[i] = (target[i] & 0xffffff) | ((at * as / 255) << 24);
//		}
//	}
//	public static void main( String[] args ) {
//		try {
//			BufferedImage im = ImageIO.read( new File( "d:\\test.png" ) );
//			im = ImageUtilities.ensureIntRGBFormat( im );
//
//			im = new GlowFilter( 0xff00ff, 3, false ).filter( im, null );
//
//			ImageIO.write( im, "png", new File( "d:\\test-out.png" ) );
//		} catch( Exception e ) {
//			e.printStackTrace();
//		}
//	}
}
