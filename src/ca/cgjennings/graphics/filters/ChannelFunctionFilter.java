package ca.cgjennings.graphics.filters;

import ca.cgjennings.math.Fn;
import java.awt.image.BufferedImage;

/**
 * A image filter that applies a function to each channel. The function takes a
 * value between 0 and 1 for a pixel's channel value, and returns a value
 * between 0 and 1 for the output value. The returned value is clamped to lie in
 * the allowed range, if necessary. That is, if the function returns a value
 * less than 0, it is clamped to 0. Likewise, if it returns a value greater than
 * 1, it is clamped to 1.
 *
 * <p>
 * <b>In-place filtering:</b> This class supports in-place filtering (the source
 * and destination images may be the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class ChannelFunctionFilter extends AbstractPixelwiseFilter {

    private Fn alphaFunction = Fn.IDENTITY;
    private Fn redFunction = Fn.IDENTITY;
    private Fn greenFunction = Fn.IDENTITY;
    private Fn blueFunction = Fn.IDENTITY;
    private boolean valid;
    private int[] a, r, g, b;

    /**
     * Creates a new function filter with all channel functions set to an
     * identity function.
     *
     * @see Fn#IDENTITY
     */
    public ChannelFunctionFilter() {
    }

    /**
     * Creates a new function filter with the specified channel functions.
     *
     * @param alpha the function applied to the alpha channel
     * @param red the function applied to the red channel
     * @param green the function applied to the green channel
     * @param blue the function applied to the blue channel
     */
    public ChannelFunctionFilter(Fn alpha, Fn red, Fn green, Fn blue) {
        setAlphaFunction(alpha);
        setRedFunction(red);
        setGreenFunction(green);
        setBlueFunction(blue);
    }

    public Fn getAlphaFunction() {
        return alphaFunction;
    }

    public void setAlphaFunction(Fn alphaFunction) {
        if (alphaFunction == null) {
            throw new NullPointerException("alphaFunction");
        }
        this.alphaFunction = alphaFunction;
        valid = false;
    }

    public Fn getRedFunction() {
        return redFunction;
    }

    public void setRedFunction(Fn redFunction) {
        if (redFunction == null) {
            throw new NullPointerException("redFunction");
        }
        this.redFunction = redFunction;
        valid = false;
    }

    public Fn getGreenFunction() {
        return greenFunction;
    }

    public void setGreenFunction(Fn greenFunction) {
        if (greenFunction == null) {
            throw new NullPointerException("greenFunction");
        }
        this.greenFunction = greenFunction;
        valid = false;
    }

    public Fn getBlueFunction() {
        return blueFunction;
    }

    public void setBlueFunction(Fn blueFunction) {
        if (blueFunction == null) {
            throw new NullPointerException("blueFunction");
        }
        this.blueFunction = blueFunction;
        valid = false;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        if (!valid) {
            validate();
        }
        return super.filter(src, dest);
    }

    private void validate() {
        if (a == null) {
            a = new int[256];
            r = new int[256];
            g = new int[256];
            b = new int[256];
        }
        validate(a, alphaFunction);
        validate(r, redFunction);
        validate(g, greenFunction);
        validate(b, blueFunction);
        valid = true;
    }

    private static void validate(int[] table, Fn f) {
        for (int i = 0; i < 256; i++) {
            int v = (int) (255 * f.f(i / 255.0d) + 0.5d);
            table[i] = clampBoth(v);
        }
    }

    @Override
    public void filterPixels(int[] argb, int start, int end) {
        for (int i = start; i < end; ++i) {
            final int p = argb[i];
            final int av = a[p >>> 24];
            final int rv = r[(p >> 16) & 0xff];
            final int gv = g[(p >> 8) & 0xff];
            final int bv = b[p & 0xff];
            argb[i] = (av << 24) | (rv << 16) | (gv << 8) | (bv);
        }
    }
}
