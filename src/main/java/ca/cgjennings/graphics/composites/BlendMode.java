package ca.cgjennings.graphics.composites;

import java.awt.AlphaComposite;
import static java.awt.Color.HSBtoRGB;
import static java.awt.Color.RGBtoHSB;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;

/**
 * Composites that have similar effects to the layer blending modes in popular
 * paint programs. This class itself is an abstract superclass of such
 * composites; it provides concrete implementations for several common modes as
 * immutable static members.
 *
 * <p>
 * <b>Note:</b> Because this extends {@link AbstractARGBComposite}, it only
 * supports 32-bit integer (A)RGB colour models. To avoid potential problems, do
 * not apply this composite directly to a screen, printer, or other arbitrary
 * graphics context. Instead, create a temporary image in {@code TYPE_INT_RGB}
 * or {@code TYPE_INT_ARGB} mode, composite into this image, and then draw the
 * image to the destination.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public abstract class BlendMode extends AbstractARGBComposite {

    public BlendMode() {
    }

    public BlendMode(float alpha) {
        super(alpha);
    }

    /**
     * {@code Normal} mixes the source and destination using the weighted
     * average {@code source/alpha + destination/(1-alpha)}. {@code Normal} is
     * identical to the {@code AlphaComposite.SrcOver} composite. It is included
     * here for completeness.
     */
    public static final AlphaComposite Normal = AlphaComposite.SrcOver;

    /**
     * For each channel, {@code Lighten} chooses the lighter of the source and
     * destination values.
     */
    public static final BlendMode Lighten = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = sR > dR ? sR : dR;
                        sG = sG > dG ? sG : dG;
                        sB = sB > dB ? sB : dB;

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void compose44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = mix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = sR > dR ? sR : dR;
                        sG = sG > dG ? sG : dG;
                        sB = sB > dB ? sB : dB;

                        dst[i] = mix(sA, sR) + mix(dA, dR);
                        dst[i + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[i + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void composeFxx(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = fmix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = sR > dR ? sR : dR;
                        sG = sG > dG ? sG : dG;
                        sB = sB > dB ? sB : dB;

                        dst[j] = fmix(sA, sR) + fmix(dA, dR);
                        dst[j + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[j + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }

                @Override
                protected void composeF44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = fmix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = sR > dR ? sR : dR;
                        sG = sG > dG ? sG : dG;
                        sB = sB > dB ? sB : dB;

                        dst[i] = fmix(sA, sR) + fmix(dA, dR);
                        dst[i + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[i + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * For each channel, {@code Darken} chooses the darker of the source and
     * destination values.
     */
    public static final BlendMode Darken = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = sR < dR ? sR : dR;
                        sG = sG < dG ? sG : dG;
                        sB = sB < dB ? sB : dB;

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void compose44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = mix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = sR < dR ? sR : dR;
                        sG = sG < dG ? sG : dG;
                        sB = sB < dB ? sB : dB;

                        dst[i] = mix(sA, sR) + mix(dA, dR);
                        dst[i + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[i + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void composeFxx(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = fmix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = sR < dR ? sR : dR;
                        sG = sG < dG ? sG : dG;
                        sB = sB < dB ? sB : dB;

                        dst[j] = fmix(sA, sR) + fmix(dA, dR);
                        dst[j + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[j + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }

                @Override
                protected void composeF44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = fmix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = sR < dR ? sR : dR;
                        sG = sG < dG ? sG : dG;
                        sB = sB < dB ? sB : dB;

                        dst[i] = fmix(sA, sR) + fmix(dA, dR);
                        dst[i + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[i + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * For each channel, {@code Add} takes the sum of the of the source and
     * destination values. The result is always lighter unless the destination
     * is white or the source is black. {@code Add} is sometimes called
     * <i>Linear Dodge</i>.
     */
    public static final BlendMode Add = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = Math.min(255, sR + dR);
                        sG = Math.min(255, sG + dG);
                        sB = Math.min(255, sB + dB);

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void compose44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = mix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = Math.min(255, sR + dR);
                        sG = Math.min(255, sG + dG);
                        sB = Math.min(255, sB + dB);

                        dst[i] = mix(sA, sR) + mix(dA, dR);
                        dst[i + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[i + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void composeFxx(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = fmix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = Math.min(255, sR + dR);
                        sG = Math.min(255, sG + dG);
                        sB = Math.min(255, sB + dB);

                        dst[j] = fmix(sA, sR) + fmix(dA, dR);
                        dst[j + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[j + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }

                @Override
                protected void composeF44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = fmix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = Math.min(255, sR + dR);
                        sG = Math.min(255, sG + dG);
                        sB = Math.min(255, sB + dB);

                        dst[i] = fmix(sA, sR) + fmix(dA, dR);
                        dst[i + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[i + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * {@code LinearDodge} is the same as {@link #Add}.
     */
    public static final BlendMode LinearDodge = Add;

    /**
     * For each channel, {@code Subtract} subtracts the source value from the
     * destination value.
     */
    public static final BlendMode Subtract = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = Math.max(0, dR - sR);
                        sG = Math.max(0, dG - sG);
                        sB = Math.max(0, dB - sB);

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void compose44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = mix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = Math.max(0, dR - sR);
                        sG = Math.max(0, dG - sG);
                        sB = Math.max(0, dB - sB);

                        dst[i] = mix(sA, sR) + mix(dA, dR);
                        dst[i + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[i + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void composeFxx(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = fmix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = Math.max(0, dR - sR);
                        sG = Math.max(0, dG - sG);
                        sB = Math.max(0, dB - sB);

                        dst[j] = fmix(sA, sR) + fmix(dA, dR);
                        dst[j + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[j + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }

                @Override
                protected void composeF44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = fmix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = Math.max(0, dR - sR);
                        sG = Math.max(0, dG - sG);
                        sB = Math.max(0, dB - sB);

                        dst[i] = fmix(sA, sR) + fmix(dA, dR);
                        dst[i + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[i + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * For each channel, {@code Difference} takes the absolute value of the
     * difference of the source and destination values.
     */
    public static final BlendMode Difference = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = sR > dR ? sR - dR : dR - sR;
                        sG = sG > dG ? sG - dG : dG - sG;
                        sB = sB > dB ? sB - dB : dB - sB;

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void compose44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = mix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = sR > dR ? sR - dR : dR - sR;
                        sG = sG > dG ? sG - dG : dG - sG;
                        sB = sB > dB ? sB - dB : dB - sB;

                        dst[i] = mix(sA, sR) + mix(dA, dR);
                        dst[i + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[i + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void composeFxx(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = fmix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = sR > dR ? sR - dR : dR - sR;
                        sG = sG > dG ? sG - dG : dG - sG;
                        sB = sB > dB ? sB - dB : dB - sB;

                        dst[j] = fmix(sA, sR) + fmix(dA, dR);
                        dst[j + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[j + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }

                @Override
                protected void composeF44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = fmix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = sR > dR ? sR - dR : dR - sR;
                        sG = sG > dG ? sG - dG : dG - sG;
                        sB = sB > dB ? sB - dB : dB - sB;

                        dst[i] = fmix(sA, sR) + fmix(dA, dR);
                        dst[i + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[i + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * {@code Exclusion} produces an effect similar to {@link #Difference}, but
     * lower in contrast.
     */
    public static final BlendMode Exclusion = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = dR + mul(sR, 255 - dR - dR);
                        sG = dG + mul(sG, 255 - dG - dG);
                        sB = dB + mul(sB, 255 - dB - dB);

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void compose44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = mix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = dR + mul(sR, 255 - dR - dR);
                        sG = dG + mul(sG, 255 - dG - dG);
                        sB = dB + mul(sB, 255 - dB - dB);

                        dst[i] = mix(sA, sR) + mix(dA, dR);
                        dst[i + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[i + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void composeFxx(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = fmix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = dR + mul(sR, 255 - dR - dR);
                        sG = dG + mul(sG, 255 - dG - dG);
                        sB = dB + mul(sB, 255 - dB - dB);

                        dst[j] = fmix(sA, sR) + fmix(dA, dR);
                        dst[j + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[j + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }

                @Override
                protected void composeF44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = fmix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = dR + mul(sR, 255 - dR - dR);
                        sG = dG + mul(sG, 255 - dG - dG);
                        sB = dB + mul(sB, 255 - dB - dB);

                        dst[i] = fmix(sA, sR) + fmix(dA, dR);
                        dst[i + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[i + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * {@code Negation} produces a similar effect to {@link #Difference}, but
     * the values are inverted so that the destination becomes lighter rather
     * than darker.
     */
    public static final BlendMode Negation = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = 255 - (sR > dR ? sR - dR : dR - sR);
                        sG = 255 - (sG > dG ? sG - dG : dG - sG);
                        sB = 255 - (sB > dB ? sB - dB : dB - sB);

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void compose44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = mix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = 255 - (sR > dR ? sR - dR : dR - sR);
                        sG = 255 - (sG > dG ? sG - dG : dG - sG);
                        sB = 255 - (sB > dB ? sB - dB : dB - sB);

                        dst[i] = mix(sA, sR) + mix(dA, dR);
                        dst[i + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[i + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void composeFxx(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = fmix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = 255 - (sR > dR ? sR - dR : dR - sR);
                        sG = 255 - (sG > dG ? sG - dG : dG - sG);
                        sB = 255 - (sB > dB ? sB - dB : dB - sB);

                        dst[j] = fmix(sA, sR) + fmix(dA, dR);
                        dst[j + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[j + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }

                @Override
                protected void composeF44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = fmix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = 255 - (sR > dR ? sR - dR : dR - sR);
                        sG = 255 - (sG > dG ? sG - dG : dG - sG);
                        sB = 255 - (sB > dB ? sB - dB : dB - sB);

                        dst[i] = fmix(sA, sR) + fmix(dA, dR);
                        dst[i + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[i + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * For each channel, {@code Multiply} takes the product of the of the source
     * and destination values. The result is always darker unless the
     * destination is black.
     */
    public static final BlendMode Multiply = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        dst[j] = mix(sA, mul(src[i], dR)) + mix(dA, dR);
                        dst[j + 1] = mix(sA, mul(src[i + 1], dG)) + mix(dA, dG);
                        dst[j + 2] = mix(sA, mul(src[i + 2], dB)) + mix(dA, dB);
                    }
                }

                @Override
                protected void compose44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = mix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        dst[i] = mix(sA, mul(src[i], dst[i])) + mix(dA, dR);
                        dst[i + 1] = mix(sA, mul(src[i + 1], dst[i + 1])) + mix(dA, dG);
                        dst[i + 2] = mix(sA, mul(src[i + 2], dst[i + 2])) + mix(dA, dB);
                    }
                }

                @Override
                protected void composeFxx(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = fmix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        dst[j] = fmix(sA, mul(src[i], dR)) + fmix(dA, dR);
                        dst[j + 1] = fmix(sA, mul(src[i + 1], dG)) + fmix(dA, dG);
                        dst[j + 2] = fmix(sA, mul(src[i + 2], dB)) + fmix(dA, dB);
                    }
                }

                @Override
                protected void composeF44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = fmix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        dst[i] = fmix(sA, mul(src[i], dst[i])) + fmix(dA, dR);
                        dst[i + 1] = fmix(sA, mul(src[i + 1], dst[i + 1])) + fmix(dA, dG);
                        dst[i + 2] = fmix(sA, mul(src[i + 2], dst[i + 2])) + fmix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * For each channel, {@code Screen} takes the inverse product of the source
     * and destination values. The result is always lighter unless the
     * destination is white. Visually, the effect is comparable to projecting
     * one photographic slide over another.
     */
    public static final BlendMode Screen = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        int t = (255 - dR) * (255 - sR) + 0x80;
                        sR = 255 - (((t >> 8) + t) >> 8);
                        t = (255 - dG) * (255 - sG) + 0x80;
                        sG = 255 - (((t >> 8) + t) >> 8);
                        t = (255 - dB) * (255 - sB) + 0x80;
                        sB = 255 - (((t >> 8) + t) >> 8);

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void compose44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = mix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        int t = (255 - dR) * (255 - sR) + 0x80;
                        sR = 255 - (((t >> 8) + t) >> 8);
                        t = (255 - dG) * (255 - sG) + 0x80;
                        sG = 255 - (((t >> 8) + t) >> 8);
                        t = (255 - dB) * (255 - sB) + 0x80;
                        sB = 255 - (((t >> 8) + t) >> 8);

                        dst[i] = mix(sA, sR) + mix(dA, dR);
                        dst[i + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[i + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void composeFxx(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = fmix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        int t = (255 - dR) * (255 - sR) + 0x80;
                        sR = 255 - (((t >> 8) + t) >> 8);
                        t = (255 - dG) * (255 - sG) + 0x80;
                        sG = 255 - (((t >> 8) + t) >> 8);
                        t = (255 - dB) * (255 - sB) + 0x80;
                        sB = 255 - (((t >> 8) + t) >> 8);

                        dst[j] = fmix(sA, sR) + fmix(dA, dR);
                        dst[j + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[j + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }

                @Override
                protected void composeF44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = fmix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        int t = (255 - dR) * (255 - sR) + 0x80;
                        sR = 255 - (((t >> 8) + t) >> 8);
                        t = (255 - dG) * (255 - sG) + 0x80;
                        sG = 255 - (((t >> 8) + t) >> 8);
                        t = (255 - dB) * (255 - sB) + 0x80;
                        sB = 255 - (((t >> 8) + t) >> 8);

                        dst[i] = fmix(sA, sR) + fmix(dA, dR);
                        dst[i + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[i + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * For each channel, {@code Burn} decreases the brightness of the
     * destination relative to the brightness of the source.
     */
    public static final BlendMode Burn = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = clamp(255 - (((255 - Math.min(254, sR)) << 8) / (dR + 1)));
                        sG = clamp(255 - (((255 - Math.min(254, sG)) << 8) / (dG + 1)));
                        sB = clamp(255 - (((255 - Math.min(254, sB)) << 8) / (dB + 1)));

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void compose44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = mix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = clamp(255 - (((255 - Math.min(254, sR)) << 8) / (dR + 1)));
                        sG = clamp(255 - (((255 - Math.min(254, sG)) << 8) / (dG + 1)));
                        sB = clamp(255 - (((255 - Math.min(254, sB)) << 8) / (dB + 1)));

                        dst[i] = mix(sA, sR) + mix(dA, dR);
                        dst[i + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[i + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void composeFxx(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = fmix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = clamp(255 - (((255 - Math.min(254, sR)) << 8) / (dR + 1)));
                        sG = clamp(255 - (((255 - Math.min(254, sG)) << 8) / (dG + 1)));
                        sB = clamp(255 - (((255 - Math.min(254, sB)) << 8) / (dB + 1)));

                        dst[j] = fmix(sA, sR) + fmix(dA, dR);
                        dst[j + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[j + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }

                @Override
                protected void composeF44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = fmix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = clamp(255 - (((255 - Math.min(254, sR)) << 8) / (dR + 1)));
                        sG = clamp(255 - (((255 - Math.min(254, sG)) << 8) / (dG + 1)));
                        sB = clamp(255 - (((255 - Math.min(254, sB)) << 8) / (dB + 1)));

                        dst[i] = fmix(sA, sR) + fmix(dA, dR);
                        dst[i + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[i + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * For each channel, {@code ColorBurn} decreases the contrast of the
     * destination relative to the brightness of the source.
     */
    public static final BlendMode ColorBurn = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        if (sR != 0) {
                            sR = Math.max(0, 255 - (((255 - dR) << 8) / sR));
                        }
                        if (sG != 0) {
                            sG = Math.max(0, 255 - (((255 - dG) << 8) / sG));
                        }
                        if (sB != 0) {
                            sB = Math.max(0, 255 - (((255 - dB) << 8) / sB));
                        }

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void compose44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = mix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        if (sR != 0) {
                            sR = Math.max(255 - (((255 - dR) << 8) / sR), 0);
                        }
                        if (sG != 0) {
                            sG = Math.max(255 - (((255 - dG) << 8) / sG), 0);
                        }
                        if (sB != 0) {
                            sB = Math.max(255 - (((255 - dB) << 8) / sB), 0);
                        }

                        dst[i] = mix(sA, sR) + mix(dA, dR);
                        dst[i + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[i + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void composeFxx(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = fmix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        if (sR != 0) {
                            sR = Math.max(255 - (((255 - dR) << 8) / sR), 0);
                        }
                        if (sG != 0) {
                            sG = Math.max(255 - (((255 - dG) << 8) / sG), 0);
                        }
                        if (sB != 0) {
                            sB = Math.max(255 - (((255 - dB) << 8) / sB), 0);
                        }

                        dst[j] = fmix(sA, sR) + fmix(dA, dR);
                        dst[j + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[j + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }

                @Override
                protected void composeF44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = fmix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        if (sR != 0) {
                            sR = Math.max(255 - (((255 - dR) << 8) / sR), 0);
                        }
                        if (sG != 0) {
                            sG = Math.max(255 - (((255 - dG) << 8) / sG), 0);
                        }
                        if (sB != 0) {
                            sB = Math.max(255 - (((255 - dB) << 8) / sB), 0);
                        }

                        dst[i] = fmix(sA, sR) + fmix(dA, dR);
                        dst[i + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[i + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * For each channel, {@code LinearBurn} darkens the destination by
     * increasing the contrast relative to the source.
     */
    public static final BlendMode LinearBurn = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = Math.max(0, sR + dR - 255);
                        sG = Math.max(0, sG + dG - 255);
                        sB = Math.max(0, sB + dB - 255);

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void compose44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = mix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = Math.max(0, sR + dR - 255);
                        sG = Math.max(0, sG + dG - 255);
                        sB = Math.max(0, sB + dB - 255);

                        dst[i] = mix(sA, sR) + mix(dA, dR);
                        dst[i + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[i + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void composeFxx(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = fmix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = Math.max(0, sR + dR - 255);
                        sG = Math.max(0, sG + dG - 255);
                        sB = Math.max(0, sB + dB - 255);

                        dst[j] = fmix(sA, sR) + fmix(dA, dR);
                        dst[j + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[j + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }

                @Override
                protected void composeF44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = fmix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = Math.max(0, sR + dR - 255);
                        sG = Math.max(0, sG + dG - 255);
                        sB = Math.max(0, sB + dB - 255);

                        dst[i] = fmix(sA, sR) + fmix(dA, dR);
                        dst[i + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[i + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * For each channel, {@code Dodge} increases the brightness of the
     * destination relative to the brightness of the source.
     */
    public static final BlendMode Dodge = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = clamp((sR << 8) / (256 - dR));
                        sG = clamp((sG << 8) / (256 - dG));
                        sB = clamp((sB << 8) / (256 - dB));

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void compose44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = mix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = clamp((sR << 8) / (256 - dR));
                        sG = clamp((sG << 8) / (256 - dG));
                        sB = clamp((sB << 8) / (256 - dB));

                        dst[i] = mix(sA, sR) + mix(dA, dR);
                        dst[i + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[i + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void composeFxx(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = fmix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = clamp((sR << 8) / (256 - dR));
                        sG = clamp((sG << 8) / (256 - dG));
                        sB = clamp((sB << 8) / (256 - dB));

                        dst[j] = fmix(sA, sR) + fmix(dA, dR);
                        dst[j + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[j + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }

                @Override
                protected void composeF44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = fmix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = clamp((sR << 8) / (256 - dR));
                        sG = clamp((sG << 8) / (256 - dG));
                        sB = clamp((sB << 8) / (256 - dB));

                        dst[i] = fmix(sA, sR) + fmix(dA, dR);
                        dst[i + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[i + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * For each channel, {@code ColorDodge} brightens the destination by
     * decreasing the contrast relative to the source.
     */
    public static final BlendMode ColorDodge = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        if (sR != 255) {
                            sR = Math.min((dR << 8) / (255 - sR), 255);
                        } else {
                            sR = dR == 0 ? 0 : 255;
                        }
                        if (sG != 255) {
                            sG = Math.min((dG << 8) / (255 - sG), 255);
                        } else {
                            sG = dG == 0 ? 0 : 255;
                        }
                        if (sB != 255) {
                            sB = Math.min((dB << 8) / (255 - sB), 255);
                        } else {
                            sB = dB == 0 ? 0 : 255;
                        }

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void compose44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = mix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        if (sR != 255) {
                            sR = Math.min((dR << 8) / (255 - sR), 255);
                        } else {
                            sR = dR == 0 ? 0 : 255;
                        }
                        if (sG != 255) {
                            sG = Math.min((dG << 8) / (255 - sG), 255);
                        } else {
                            sG = dG == 0 ? 0 : 255;
                        }
                        if (sB != 255) {
                            sB = Math.min((dB << 8) / (255 - sB), 255);
                        } else {
                            sB = dB == 0 ? 0 : 255;
                        }

                        dst[i] = mix(sA, sR) + mix(dA, dR);
                        dst[i + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[i + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void composeFxx(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = fmix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        if (sR != 255) {
                            sR = Math.min((dR << 8) / (255 - sR), 255);
                        } else {
                            sR = dR == 0 ? 0 : 255;
                        }
                        if (sG != 255) {
                            sG = Math.min((dG << 8) / (255 - sG), 255);
                        } else {
                            sG = dG == 0 ? 0 : 255;
                        }
                        if (sB != 255) {
                            sB = Math.min((dB << 8) / (255 - sB), 255);
                        } else {
                            sB = dB == 0 ? 0 : 255;
                        }

                        dst[j] = fmix(sA, sR) + fmix(dA, dR);
                        dst[j + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[j + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }

                @Override
                protected void composeF44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = fmix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        if (sR != 255) {
                            sR = Math.min((dR << 8) / (255 - sR), 255);
                        } else {
                            sR = dR == 0 ? 0 : 255;
                        }
                        if (sG != 255) {
                            sG = Math.min((dG << 8) / (255 - sG), 255);
                        } else {
                            sG = dG == 0 ? 0 : 255;
                        }
                        if (sB != 255) {
                            sB = Math.min((dB << 8) / (255 - sB), 255);
                        } else {
                            sB = dB == 0 ? 0 : 255;
                        }

                        dst[i] = fmix(sA, sR) + fmix(dA, dR);
                        dst[i + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[i + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * For each channel, {@code Overlay} either multiplies or screens the
     * values, depending on the destination. The effect is to retain shadows
     * from the destination while taking on colour from the source.
     */
    public static final BlendMode Overlay = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        int t;

                        if (dR < 128) {
                            t = dR * sR + 0x80;
                            sR = 2 * (((t >> 8) + t) >> 8);
                        } else {
                            t = (255 - dR) * (255 - sR) + 0x80;
                            sR = 2 * (255 - (((t >> 8) + t) >> 8));
                        }

                        if (dG < 128) {
                            t = dG * sG + 0x80;
                            sG = 2 * (((t >> 8) + t) >> 8);
                        } else {
                            t = (255 - dG) * (255 - sG) + 0x80;
                            sG = 2 * (255 - (((t >> 8) + t) >> 8));
                        }

                        if (dB < 128) {
                            t = dB * sB + 0x80;
                            sB = 2 * (((t >> 8) + t) >> 8);
                        } else {
                            t = (255 - dB) * (255 - sB) + 0x80;
                            sB = 2 * (255 - (((t >> 8) + t) >> 8));
                        }

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * For each channel, {@code HardLight} either multiplies or screens the
     * values, depending on the source value. Visually, the effect is similar to
     * shining a hard spotlight over the destination.
     */
    public static final BlendMode HardLight = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        if (sR < 128) {
                            sR = 2 * mul(sR, dR);
                        } else {
                            sR = 255 - 2 * mul(255 - sR, 255 - dR);
                        }

                        if (sG < 128) {
                            sG = 2 * mul(sG, dG);
                        } else {
                            sG = 255 - 2 * mul(255 - sG, 255 - dG);
                        }

                        if (sB < 128) {
                            sB = 2 * mul(sB, dB);
                        } else {
                            sB = 255 - 2 * mul(255 - sB, 255 - dB);
                        }

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void compose44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = mix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        if (sR < 128) {
                            sR = 2 * mul(sR, dR);
                        } else {
                            sR = 255 - 2 * mul(255 - sR, 255 - dR);
                        }

                        if (sG < 128) {
                            sG = 2 * mul(sG, dG);
                        } else {
                            sG = 255 - 2 * mul(255 - sG, 255 - dG);
                        }

                        if (sB < 128) {
                            sB = 2 * mul(sB, dB);
                        } else {
                            sB = 255 - 2 * mul(255 - sB, 255 - dB);
                        }

                        dst[i] = mix(sA, sR) + mix(dA, dR);
                        dst[i + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[i + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void composeFxx(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = fmix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        if (sR < 128) {
                            sR = 2 * mul(sR, dR);
                        } else {
                            sR = 255 - 2 * mul(255 - sR, 255 - dR);
                        }

                        if (sG < 128) {
                            sG = 2 * mul(sG, dG);
                        } else {
                            sG = 255 - 2 * mul(255 - sG, 255 - dG);
                        }

                        if (sB < 128) {
                            sB = 2 * mul(sB, dB);
                        } else {
                            sB = 255 - 2 * mul(255 - sB, 255 - dB);
                        }

                        dst[j] = fmix(sA, sR) + fmix(dA, dR);
                        dst[j + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[j + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }

                @Override
                protected void composeF44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = fmix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        if (sR < 128) {
                            sR = 2 * mul(sR, dR);
                        } else {
                            sR = 255 - 2 * mul(255 - sR, 255 - dR);
                        }

                        if (sG < 128) {
                            sG = 2 * mul(sG, dG);
                        } else {
                            sG = 255 - 2 * mul(255 - sG, 255 - dG);
                        }

                        if (sB < 128) {
                            sB = 2 * mul(sB, dB);
                        } else {
                            sB = 255 - 2 * mul(255 - sB, 255 - dB);
                        }

                        dst[i] = fmix(sA, sR) + fmix(dA, dR);
                        dst[i + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[i + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * For each channel, {@code SoftLight} either dodges or burns the values,
     * depending on the source value. Visually, the effect is similar to shining
     * a diffuse spotlight over the destination.
     */
    public static final BlendMode SoftLight = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        int t = mul(sR, dR);
                        sR = t + mul(dR, 255 - mul(255 - dR, 255 - sR) - t);
                        t = mul(sG, dG);
                        sG = t + mul(dG, 255 - mul(255 - dG, 255 - sG) - t);
                        t = mul(sB, dB);
                        sB = t + mul(dB, 255 - mul(255 - dB, 255 - sB) - t);

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void compose44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = mix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        int t = mul(sR, dR);
                        sR = t + mul(dR, 255 - mul(255 - dR, 255 - sR) - t);
                        t = mul(sG, dG);
                        sG = t + mul(dG, 255 - mul(255 - dG, 255 - sG) - t);
                        t = mul(sB, dB);
                        sB = t + mul(dB, 255 - mul(255 - dB, 255 - sB) - t);
                        dst[i] = mix(sA, sR) + mix(dA, dR);
                        dst[i + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[i + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void composeFxx(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = fmix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        int t = mul(sR, dR);
                        sR = t + mul(dR, 255 - mul(255 - dR, 255 - sR) - t);
                        t = mul(sG, dG);
                        sG = t + mul(dG, 255 - mul(255 - dG, 255 - sG) - t);
                        t = mul(sB, dB);
                        sB = t + mul(dB, 255 - mul(255 - dB, 255 - sB) - t);

                        dst[j] = fmix(sA, sR) + fmix(dA, dR);
                        dst[j + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[j + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }

                @Override
                protected void composeF44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = fmix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        int t = mul(sR, dR);
                        sR = t + mul(dR, 255 - mul(255 - dR, 255 - sR) - t);
                        t = mul(sG, dG);
                        sG = t + mul(dG, 255 - mul(255 - dG, 255 - sG) - t);
                        t = mul(sB, dB);
                        sB = t + mul(dB, 255 - mul(255 - dB, 255 - sB) - t);

                        dst[i] = fmix(sA, sR) + fmix(dA, dR);
                        dst[i + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[i + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * For each channel, {@code LinearLight} either dodges or burns the values
     * by adjusting brightness, depending on the source value. Visually, the
     * effect is similar to shining a diffuse spotlight over the destination.
     */
    public static final BlendMode LinearLight = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = clamp(sR < 128 ? 2 * sR + dR - 255 : (2 * (sR - 128)) + dR);
                        sG = clamp(sG < 128 ? 2 * sG + dG - 255 : (2 * (sG - 128)) + dG);
                        sB = clamp(sB < 128 ? 2 * sB + dB - 255 : (2 * (sB - 128)) + dB);

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void compose44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = mix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = clamp(sR < 128 ? 2 * sR + dR - 255 : (2 * (sR - 128)) + dR);
                        sG = clamp(sG < 128 ? 2 * sG + dG - 255 : (2 * (sG - 128)) + dG);
                        sB = clamp(sB < 128 ? 2 * sB + dB - 255 : (2 * (sB - 128)) + dB);

                        dst[i] = mix(sA, sR) + mix(dA, dR);
                        dst[i + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[i + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void composeFxx(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = fmix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = clamp(sR < 128 ? 2 * sR + dR - 255 : (2 * (sR - 128)) + dR);
                        sG = clamp(sG < 128 ? 2 * sG + dG - 255 : (2 * (sG - 128)) + dG);
                        sB = clamp(sB < 128 ? 2 * sB + dB - 255 : (2 * (sB - 128)) + dB);

                        dst[j] = fmix(sA, sR) + fmix(dA, dR);
                        dst[j + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[j + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }

                @Override
                protected void composeF44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = fmix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = clamp(sR < 128 ? 2 * sR + dR - 255 : (2 * (sR - 128)) + dR);
                        sG = clamp(sG < 128 ? 2 * sG + dG - 255 : (2 * (sG - 128)) + dG);
                        sB = clamp(sB < 128 ? 2 * sB + dB - 255 : (2 * (sB - 128)) + dB);

                        dst[i] = fmix(sA, sR) + fmix(dA, dR);
                        dst[i + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[i + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * For each channel, {@code PinLight} may replace the destination value with
     * the source value. When the source value is light, the darker of the
     * source and destination is selected. When the source value is dark, the
     * lighter of the source and destination is selected.
     */
    public static final BlendMode PinLight = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = sR > 127 ? Math.max(2 * (sR - 128), dR) : Math.min(2 * sR, dR);
                        sG = sG > 127 ? Math.max(2 * (sG - 128), dG) : Math.min(2 * sG, dG);
                        sB = sB > 127 ? Math.max(2 * (sB - 128), dB) : Math.min(2 * sB, dB);

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void compose44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = mix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = sR > 127 ? Math.max(2 * (sR - 128), dR) : Math.min(2 * sR, dR);
                        sG = sG > 127 ? Math.max(2 * (sG - 128), dG) : Math.min(2 * sG, dG);
                        sB = sB > 127 ? Math.max(2 * (sB - 128), dB) : Math.min(2 * sB, dB);

                        dst[i] = mix(sA, sR) + mix(dA, dR);
                        dst[i + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[i + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }

                @Override
                protected void composeFxx(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = fmix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sR = sR > 127 ? Math.max(2 * (sR - 128), dR) : Math.min(2 * sR, dR);
                        sG = sG > 127 ? Math.max(2 * (sG - 128), dG) : Math.min(2 * sG, dG);
                        sB = sB > 127 ? Math.max(2 * (sB - 128), dB) : Math.min(2 * sB, dB);

                        dst[j] = fmix(sA, sR) + fmix(dA, dR);
                        dst[j + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[j + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }

                @Override
                protected void composeF44(int[] src, int[] dst, final int aFactor) {
                    for (int i = 0; i < src.length; i += 4) {
                        int sA = fmix(aFactor, src[i + 3]);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[i], dG = dst[i + 1], dB = dst[i + 2];

                        sR = sR > 127 ? Math.max(2 * (sR - 128), dR) : Math.min(2 * sR, dR);
                        sG = sG > 127 ? Math.max(2 * (sG - 128), dG) : Math.min(2 * sG, dG);
                        sB = sB > 127 ? Math.max(2 * (sB - 128), dB) : Math.min(2 * sB, dB);

                        dst[i] = fmix(sA, sR) + fmix(dA, dR);
                        dst[i + 1] = fmix(sA, sG) + fmix(dA, dG);
                        dst[i + 2] = fmix(sA, sB) + fmix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * For each channel, {@code VividLight} either dodges or burns the values by
     * adjusting contrast, depending on the source value. Visually, the effect
     * is similar to shining a diffuse spotlight over the destination.
     */
    public static final BlendMode VividLight = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        if (sR < 128) {
                            sR = 2 * sR;
                            if (sR != 0) {
                                sR = Math.max(0, 255 - (((255 - dR) << 8) / sR));
                            }
                        } else {
                            sR = 2 * (sR - 128);
                            if (sR != 255) {
                                sR = Math.min((dR << 8) / (255 - sR), 255);
                            } else {
                                sR = dR == 0 ? 0 : 255;
                            }
                        }

                        if (sG < 128) {
                            sG = 2 * sG;
                            if (sG != 0) {
                                sG = Math.max(0, 255 - (((255 - dG) << 8) / sG));
                            }
                        } else {
                            sG = 2 * (sG - 128);
                            if (sG != 255) {
                                sG = Math.min((dG << 8) / (255 - sG), 255);
                            } else {
                                sG = dG == 0 ? 0 : 255;
                            }
                        }

                        if (sB < 128) {
                            sB = 2 * sB;
                            if (sB != 0) {
                                sB = Math.max(0, 255 - (((255 - dB) << 8) / sB));
                            }
                        } else {
                            sB = 2 * (sB - 128);
                            if (sB != 255) {
                                sB = Math.min((dB << 8) / (255 - sB), 255);
                            } else {
                                sB = dB == 0 ? 0 : 255;
                            }
                        }

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * For each channel, {@code HardMix} drives the value to either be fully off
     * or fully on depending on the sum of the source and destination values.
     * Thus, each pixel will be one of black, red, green, blue, yellow, cyan,
     * magenta, or white.
     */
    public static final BlendMode HardMix = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        if (sR < 128) {
                            sR = 2 * sR;
                            if (sR != 0) {
                                sR = Math.max(0, 255 - (((255 - dR) << 8) / sR));
                            }
                        } else {
                            sR = 2 * (sR - 128);
                            if (sR != 255) {
                                sR = Math.min((dR << 8) / (255 - sR), 255);
                            } else {
                                sR = dR == 0 ? 0 : 255;
                            }
                        }
                        sR = sR < 128 ? 0 : 255;

                        if (sG < 128) {
                            sG = 2 * sG;
                            if (sG != 0) {
                                sG = Math.max(0, 255 - (((255 - dG) << 8) / sG));
                            }
                        } else {
                            sG = 2 * (sG - 128);
                            if (sG != 255) {
                                sG = Math.min((dG << 8) / (255 - sG), 255);
                            } else {
                                sG = dG == 0 ? 0 : 255;
                            }
                        }
                        sG = sG < 128 ? 0 : 255;

                        if (sB < 128) {
                            sB = 2 * sB;
                            if (sB != 0) {
                                sB = Math.max(0, 255 - (((255 - dB) << 8) / sB));
                            }
                        } else {
                            sB = 2 * (sB - 128);
                            if (sB != 255) {
                                sB = Math.min((dB << 8) / (255 - sB), 255);
                            } else {
                                sB = dB == 0 ? 0 : 255;
                            }
                        }
                        sB = sB < 128 ? 0 : 255;

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * {@code Reflect} lightens the destination based on the source using a
     * non-linear function. It is useful for adding shine or highlights to the
     * destination.
     */
    public static final BlendMode Reflect = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        if (sR != 255) {
                            sR = Math.min(255, dR * dR / (255 - sR));
                        } else {
                            sR = Math.min(255, dR * dR);
                        }
                        if (sG != 255) {
                            sG = Math.min(255, dG * dG / (255 - sG));
                        } else {
                            sG = Math.min(255, dG * dG);
                        }
                        if (sB != 255) {
                            sB = Math.min(255, dB * dB / (255 - sB));
                        } else {
                            sB = Math.min(255, dB * dB);
                        }

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * {@code Glow} has the same effect as {@link #Reflect}, but is used when
     * the source and destination are reversed.
     */
    public static final BlendMode Glow = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        if (dR != 255) {
                            sR = Math.min(255, sR * sR / (255 - dR));
                        } else {
                            sR = Math.min(255, sR * sR);
                        }
                        if (dG != 255) {
                            sG = Math.min(255, sG * sG / (255 - dG));
                        } else {
                            sG = Math.min(255, sG * sG);
                        }
                        if (dB != 255) {
                            sB = Math.min(255, sB * sB / (255 - dB));
                        } else {
                            sB = Math.min(255, sB * sB);
                        }

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * {@code Hue} mixes the hue of the source pixel but keeps the saturation
     * and brightness of the destination.
     */
    public static final BlendMode Hue = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    float[] sHSB = null;
                    float[] dHSB = null;
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sHSB = RGBtoHSB(sR, sG, sB, sHSB);
                        dHSB = RGBtoHSB(dR, dG, dB, dHSB);

                        int rgb = HSBtoRGB(sHSB[0], dHSB[1], dHSB[2]);

                        sR = (rgb >>> 16) & 0xff;
                        sG = (rgb >>> 8) & 0xff;
                        sB = rgb & 0xff;

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * {@code Saturation} mixes the saturation of the source pixel but keeps the
     * hue and brightness of the destination.
     */
    public static final BlendMode Saturation = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    float[] sHSB = null;
                    float[] dHSB = null;
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sHSB = RGBtoHSB(sR, sG, sB, sHSB);
                        dHSB = RGBtoHSB(dR, dG, dB, dHSB);

                        int rgb = HSBtoRGB(dHSB[0], sHSB[1], dHSB[2]);

                        sR = (rgb >>> 16) & 0xff;
                        sG = (rgb >>> 8) & 0xff;
                        sB = rgb & 0xff;

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * {@code Color} mixes the hue and saturation of the source pixel but keeps
     * the brightness of the destination.
     */
    public static final BlendMode Color = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    float[] sHSB = null;
                    float[] dHSB = null;
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sHSB = RGBtoHSB(sR, sG, sB, sHSB);
                        dHSB = RGBtoHSB(dR, dG, dB, dHSB);

                        int rgb = HSBtoRGB(sHSB[0], sHSB[1], dHSB[2]);

                        sR = (rgb >>> 16) & 0xff;
                        sG = (rgb >>> 8) & 0xff;
                        sB = rgb & 0xff;

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }
            };
        }
    };

    /**
     * {@code Luminosity} mixes the brightness of the source pixel but keeps the
     * hue and saturation of the destination.
     */
    public static final BlendMode Luminosity = new BlendMode() {
        @Override
        public AbstractCompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new AbstractCompositeContext(alpha, srcColorModel, dstColorModel, hints) {
                @Override
                protected void compose(int[] src, int[] dst, int sBands, int dBands, final int aFactor) {
                    float[] sHSB = null;
                    float[] dHSB = null;
                    for (int i = 0, j = 0; i < src.length; i += sBands, j += dBands) {
                        int sA = mix(aFactor, sBands == 4 ? src[i + 3] : 255);
                        int dA = 255 - sA;

                        int sR = src[i], sG = src[i + 1], sB = src[i + 2];
                        int dR = dst[j], dG = dst[j + 1], dB = dst[j + 2];

                        sHSB = RGBtoHSB(sR, sG, sB, sHSB);
                        dHSB = RGBtoHSB(dR, dG, dB, dHSB);

                        int rgb = HSBtoRGB(dHSB[0], dHSB[1], sHSB[2]);

                        sR = (rgb >>> 16) & 0xff;
                        sG = (rgb >>> 8) & 0xff;
                        sB = rgb & 0xff;

                        dst[j] = mix(sA, sR) + mix(dA, dR);
                        dst[j + 1] = mix(sA, sG) + mix(dA, dG);
                        dst[j + 2] = mix(sA, sB) + mix(dA, dB);
                    }
                }
            };
        }
    };

//	public static final BlendMode T = new BlendMode() {
//		@Override
//		public AbstractCompositeContext createContext( ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints ) {
//			return new AbstractCompositeContext( alpha, srcColorModel, dstColorModel, hints ) {
//				@Override
//				protected void compose( int[] src, int[] dst, int sBands, int dBands, final int aFactor ) {
//					for( int i = 0, j = 0; i < src.length; i += sBands, j += dBands ) {
//						int sA = mix( aFactor, sBands == 4 ? src[i+3] : 255 );
//						int dA = 255 - sA;
//
//						int sR = src[i], sG = src[i+1], sB = src[i+2];
//						int dR = dst[j], dG = dst[j+1], dB = dst[j+2];
//
//						// set sR, sG, sB to blended values
//
//						dst[j]   = mix( sA, sR ) + mix( dA, dR );
//						dst[j+1] = mix( sA, sG ) + mix( dA, dG );
//						dst[j+2] = mix( sA, sB ) + mix( dA, dB );
//					}
//				}
//				@Override
//				protected void compose44( int[] src, int[] dst, final int aFactor ) {
//					for ( int i = 0; i < src.length; i += 4 ) {
//						int sA = mix( aFactor, src[i+3] );
//						int dA = 255 - sA;
//
//						int sR = src[i], sG = src[i+1], sB = src[i+2];
//						int dR = dst[i], dG = dst[i+1], dB = dst[i+2];
//
//						// set sR, sG, sB to blended values
//
//						dst[i]   = mix( sA, sR ) + mix( dA, dR );
//						dst[i+1] = mix( sA, sG ) + mix( dA, dG );
//						dst[i+2] = mix( sA, sB ) + mix( dA, dB );
//					}
//				}
//				@Override
//				protected void composeFxx( int[] src, int[] dst, int sBands, int dBands, final int aFactor ) {
//					for( int i = 0, j = 0; i < src.length; i += sBands, j += dBands ) {
//						int sA = fmix( aFactor, sBands == 4 ? src[i+3] : 255 );
//						int dA = 255 - sA;
//
//						int sR = src[i], sG = src[i+1], sB = src[i+2];
//						int dR = dst[j], dG = dst[j+1], dB = dst[j+2];
//
//						// set sR, sG, sB to blended values
//
//						dst[j]   = fmix( sA, sR ) + fmix( dA, dR );
//						dst[j+1] = fmix( sA, sG ) + fmix( dA, dG );
//						dst[j+2] = fmix( sA, sB ) + fmix( dA, dB );
//					}
//				}
//				@Override
//				protected void composeF44( int[] src, int[] dst, final int aFactor ) {
//					for ( int i = 0; i < src.length; i += 4 ) {
//						int sA = fmix( aFactor, src[i+3] );
//						int dA = 255 - sA;
//
//						int sR = src[i], sG = src[i+1], sB = src[i+2];
//						int dR = dst[i], dG = dst[i+1], dB = dst[i+2];
//
//						// set sR, sG, sB to blended values
//
//						dst[i]   = fmix( sA, sR ) + fmix( dA, dR );
//						dst[i+1] = fmix( sA, sG ) + fmix( dA, dG );
//						dst[i+2] = fmix( sA, sB ) + fmix( dA, dB );
//					}
//				}
//			};
//		}
//	};
}
