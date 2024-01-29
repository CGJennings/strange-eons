package ca.cgjennings.graphics.composites;

import java.awt.RenderingHints;
import java.awt.image.ColorModel;

/**
 * Blends the source into the destination using both an overall and per-channel
 * alpha values.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class MaskComposite extends BlendMode {

    private int rFactor, gFactor, bFactor;

    public MaskComposite() {
        this(1f, 1f, 1f, 1f);
    }

    public MaskComposite(float rAlpha, float gAlpha, float bAlpha) {
        this(rAlpha, gAlpha, bAlpha, 1f);

    }

    public MaskComposite(float rAlpha, float gAlpha, float bAlpha, float alpha) {
        super(alpha);
        rFactor = AbstractCompositeContext.clamp(Math.round(rAlpha * 255));
        gFactor = AbstractCompositeContext.clamp(Math.round(gAlpha * 255));
        bFactor = AbstractCompositeContext.clamp(Math.round(bAlpha * 255));
    }

    @Override
    public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
        return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
            @Override
            protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                    int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);

                    int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                    int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                    int sRA = mix(rFactor, sA);
                    int sGA = mix(gFactor, sA);
                    int sBA = mix(bFactor, sA);

                    dst[j] = mix(sRA, sR) + mix(255 - sRA, dR);
                    dst[j + 1] = mix(sGA, sG) + mix(255 - sGA, dG);
                    dst[j + 2] = mix(sBA, sB) + mix(255 - sBA, dB);
                }
            }

            @Override
            protected void compose44(int[] src, int[] dst, final int aFactor) {
                for (int i = 0; i < src.length; i += 4) {
                    int sA = mix(aFactor, src[i + 3]);

                    int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                    int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                    int sRA = mix(rFactor, sA);
                    int sGA = mix(gFactor, sA);
                    int sBA = mix(bFactor, sA);

                    dst[i] = mix(sRA, sR) + mix(255 - sRA, dR);
                    dst[i + 1] = mix(sGA, sG) + mix(255 - sGA, dG);
                    dst[i + 2] = mix(sBA, sB) + mix(255 - sBA, dB);
                }
            }

            @Override
            protected void composeFxx(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                    int sA = fmix(aFactor, sBands == 4 ? src[i + 3] : 255);

                    int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                    int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                    int sRA = fmix(rFactor, sA);
                    int sGA = fmix(gFactor, sA);
                    int sBA = fmix(bFactor, sA);

                    dst[j] = fmix(sRA, sR) + fmix(255 - sRA, dR);
                    dst[j + 1] = fmix(sGA, sG) + fmix(255 - sGA, dG);
                    dst[j + 2] = fmix(sBA, sB) + fmix(255 - sBA, dB);
                }
            }

            @Override
            protected void composeF44(int[] src, int[] dst, final int aFactor) {
                for (int i = 0; i < src.length; i += 4) {
                    int sA = fmix(aFactor, src[i + 3]);

                    int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                    int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                    int sRA = fmix(rFactor, sA);
                    int sGA = fmix(gFactor, sA);
                    int sBA = fmix(bFactor, sA);

                    dst[i] = fmix(sRA, sR) + fmix(255 - sRA, dR);
                    dst[i + 1] = fmix(sGA, sG) + fmix(255 - sGA, dG);
                    dst[i + 2] = fmix(sBA, sB) + fmix(255 - sBA, dB);
                }
            }
        };
    }
}
