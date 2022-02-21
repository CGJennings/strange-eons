package ca.cgjennings.ui.theme;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Objects;
import javax.swing.JMenuItem;

/**
 * A theme-aware palette of standard colours. The same standard set of
 * named colours is available with variations suitable for different contexts.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class Palette {
    Palette(Subset light, Subset dark) {
        this.light = Objects.requireNonNull(light);
        this.dark = Objects.requireNonNull(dark);
        if (ThemeInstaller.isDark()) {
            foreground = light;
            background = dark;
        } else {
            foreground = dark;
            background = light;
        }
    }
    
    /**
     * Returns the number of base colours (red, orange, etc.). Each base
     * colour is available as either light or dark, and opaque or translucent.
     */
    public static final int NUM_COLORS = 16;
    
    /**
     * The current palette.
     */
    public static Palette get = createDefaultPalette();
    
    /** The light subset of the palette. */
    public final Subset light;
    
    /** The dark subset of the palette. */
    public final Subset dark;
    
    /**
     * Either the light or dark subset, chosen to contrast with the background
     * based on the theme.
     */
    public final Subset foreground;
    
    /**
     * Either the light or dark subset, chosen to contrast with the foreground
     * based on the theme.
     */
    public final Subset background;

    /**
     * Returns either the light or dark palette subset, whichever will contrast best
     * with the specified background. If the component is null or its background
     * cannot be determined, a subset based on the theme is returned.
     * 
     * @param c the component to consider as the source of the background
     */
    public final Subset contrasting(Component c) {
        Color bg = null;
        while (c != null && bg != null) {
            if (c.isOpaque()) {
                bg = c.getBackground();
            }
            c = c.getParent();
        }
        if (bg == null) {
            return ThemeInstaller.isDark() ? light : dark;
        }
        return contrasting(bg.getRGB());
    }

    /**
     * Returns either the light or dark palette subset, whichever will contrast best
     * with the specified colour. Any alpha component is ignored.
     * 
     * @param rgb the RGB value of the colour to contrast with
     */    
    public final Subset contrasting(int rgb) {
        int r = (rgb & 0xff0000) >> 16;
        int g = (rgb & 0x00ff00) >> 8;
        int b = (rgb & 0x0000ff);
        g = ((r * 30) + (g * 59) + (b *11)) / 100;
        return g <= 115 ? light : dark; 
    }

    /**
     * Returns either the light or dark palette subset, whichever will harmonize
     * best with the specified background.
     * This is simply the opposite of the subset returned by
     * {@link #contrasting(java.awt.Component)}.
     * 
     * @param c the component to consider as the source of the background
     */     
    public final Subset harmonizing(Component c) {
        return contrasting(c) == light ? dark : light;
    }

    /**
     * Returns either the light or dark palette subset, whichever will harmonize
     * best with the specified colour.
     * This is simply the opposite of the subset returned by
     * {@link #contrasting(int)}.
     * 
     * @param c the RGB value of the colour to contrast with
     */
    public final Subset harmonizing(int rgb) {
        return contrasting(rgb) == light ? dark : light;
    }    
    
    /** Encapsulates a palette subset broken into opaque and slightly translucent groups. */
    public static final class Subset {
        public Subset(Variant opaque, Variant translucent) {
            this.opaque = opaque;
            this.translucent = translucent;
        }
        /** A subset of opaque colours. */
        public final Variant opaque;
        /** A subset of translucent colours. */
        public final Variant translucent;
    }
    
    public static final class Variant {
        Variant(Color[] init) {
            red = init[0];
            orange = init[1];
            yellow = init[2];
            green = init[3];
            blue = init[4];
            indigo = init[5];
            violet = init[6];
            cyan = init[7];
            teal = init[8];
            brown = init[9];
            pink = init[10];
            grey = init[11];
            black = init[12];
            white = init[13];
            foreground = init[14];
            background = init[15];
        }
        
        /** A default foreground colour, typically similar to the standard text colour. */
        public final Color foreground;
        /** A default background colour, typically similar to the standard component or menu background. */
        public final Color background;
        /** A red colour, code <code>R</code>. */
        public final Color red;
        /** An orange colour, code <code>O</code>. */
        public final Color orange;
        /** A yellow colour, code <code>Y</code>. */
        public final Color yellow;
        /** A green colour, code <code>G</code>. */
        public final Color green;
        /** A blue colour, code <code>B</code>. */
        public final Color blue;
        /** An indigo colour, code <code>I</code>. */
        public final Color indigo;
        /** A violet colour, code <code>V</code>. */
        public final Color violet;
        /** A cyan colour, code <code>C</code>. */
        public final Color cyan;
        /** A teal colour, code <code>T</code>. */
        public final Color teal;
        /** A brown colour, code <code>W</code>. */
        public final Color brown;
        /** A pink colour, code <code>P</code>. */
        public final Color pink;
        /** A grey colour, code <code>E</code>. */
        public final Color grey;
        /** A black colour, code <code>0</code> or <code>K</code>. */
        public final Color black;
        /** A white colour, code <code>1</code> or <code>H</code>. */
        public final Color white;
        
        /**
         * Returns one of the colours in the variant, specified by code.
         *
         * @param code the case-insensitive code
         * @return the matching colour
         * @throws IllegalArgumentException if the code is invalid
         */
        public Color fromCode(char code) {
            Color c;
            switch (code) {
                case 'r': case 'R': c = red; break;
                case 'o': case 'O': c = orange; break;
                case 'y': case 'Y': c = yellow; break;
                case 'g': case 'G': c = green; break;
                case 'b': case 'B': c = blue; break;
                case 'i': case 'I': c = indigo; break;
                case 'v': case 'V': c = violet; break;
                case 'c': case 'C': c = cyan; break;
                case 't': case 'T': c = teal; break;
                case 'w': case 'W': c = brown; break;
                case 'p': case 'P': c = pink; break;
                case 'e': case 'E': c = grey; break;
                case '0': case 'k': case 'K': c = black; break;
                case '1': case 'h': case 'H': c = white; break;
                case 'x': case 'X': c = foreground; break;
                case 'd': case 'D': c = background; break;
                default:
                    throw new IllegalArgumentException("unknown colour code " + code);
            }
            return c;
        }
        
        /**
         * Returns a colour by index instead of by name.
         * 
         * @param i the index
         * @return the matching colour
         * @throws IllegalArgumentException if the code is invalid
         */
        public Color get(int i) {
            Color c;
            switch (i) {
                case 0: c = red; break;
                case 1: c = orange; break;
                case 2: c = yellow; break;
                case 3: c = green; break;
                case 4: c = blue; break;
                case 5: c = indigo; break;
                case 6: c = violet; break;
                case 7: c = cyan; break;
                case 8: c = teal; break;
                case 9: c = brown; break;
                case 10: c = pink; break;
                case 11: c = grey; break;
                case 12: c = black; break;
                case 13: c = white; break;
                case 14: c = foreground; break;
                case 15: c = background; break;
                default:
                    throw new IllegalArgumentException("unknown colour index " + i);
            }
            return c;
        }
    }
    

    /**
     * Returns a new palette filled with default values.
     * When using the palette to render content, use the
     * {@linkplain #get current palette}.
     * 
     * @return a new default palette
     */
    public static Palette createDefaultPalette() {
        JMenuItem source = new JMenuItem("a");
        int text = source.getForeground().getRGB() & 0xffffff;
        int fill = source.getBackground().getRGB() & 0xffffff;
        if (ThemeInstaller.isDark()) {
            int temp = text;
            text = fill;
            fill = temp;
        }
        
        int[] rgb = new int[] {
            // lt op  dk op     lt tr     dk tr
            0xEF5350, 0xb71c1c, 0xe57373, 0xb71c1c, // (r)ed
            0xFFA726, 0xe65100, 0xffb74d, 0xe65100, // (o)range
            0xFFEE58, 0xf9a825, 0xfff176, 0xf9a825, // (y)ellow
            0x66BB6A, 0x33691e, 0xaed581, 0x33691e, // (g)reen
            0x42A5F5, 0x0d47a1, 0x64b5f8, 0x0d47a1, // (b)lue
            0x5C6BC0, 0x1a237e, 0x7986cb, 0x1a237e, // (i)ndigo
            0x7E57C2, 0x4a1f8c, 0x9575cd, 0x4a1f8c, // (v)iolet
            0x26C6DA, 0x006064, 0x4dd0e1, 0x006064, // (c)yan
            0x26A69A, 0x004d40, 0x4db6ac, 0x004d40, // (t)eal
            0x8D6E63, 0x4e342e, 0xa1887f, 0x4e342e, // bro(w)n
            0xEC407A, 0x880e4f, 0xF06292, 0x880e4f, // (p)ink
            0xBDBDBD, 0x424242, 0xbdbdbd, 0x424242, // gr(e)y
            0x000000, 0x000000, 0x000000, 0x000000, // (0) force black
            0xffffff, 0xffffff, 0xffffff, 0xffffff, // (1) force white
            fill,     text,     fill,     text,
            text,     fill,     text,     fill
        };
        
        Variant[] variants = new Variant[4];
        for (int group=0; group<4; ++group) {
            int alpha = group < 2 ? 0xff000000 : 0xee000000;
            Color[] variant = new Color[NUM_COLORS];
            for (int i=group, v=0; i<rgb.length; i +=4, ++v) {
                variant[v] = new Color(rgb[i] | alpha, true);
            }
            variants[group] = new Variant(variant);
        }
        
        return new Palette(
                new Subset(variants[0], variants[2]),
                new Subset(variants[1], variants[3])
        );
    }
    
    /**
     * Returns the colours of this palette as an image in which y-index is the
     * base colour and the x-index is the variant. The order of the variants
     * is as follows: opaque (light then dark), followed by translucent
     * (light then dark).
     * 
     * @return a non-null image in which each pixel represents one colour in
     * the palette
     */
    public BufferedImage toImage() {
        Variant[] variants = new Variant[] { light.opaque, dark.opaque, light.translucent, dark.translucent };
        BufferedImage im = new BufferedImage(variants.length, NUM_COLORS, BufferedImage.TYPE_INT_ARGB);
        for (int y=0; y<NUM_COLORS; ++y) {
            for (int x=0; x<variants.length; ++x) {
                im.setRGB(x, y, variants[x].get(y).getRGB());
            }
        }
        return im;
    }
    
    /**
     * Creates a palette from an image. The image uses the same layout as
     * that of {@link #toImage()}. The image may be larger than necessary,
     * but an exception will be thrown if any colours are missing.
     * 
     * @param im the image to convert into a palette
     * @return the palette image
     */
    public static Palette fromImage(BufferedImage im) {
        final int NUM_VARIANTS = 4;
        Variant[] variants = new Variant[NUM_VARIANTS];
        Color[] colors = new Color[NUM_COLORS];
        for (int x=0; x<NUM_VARIANTS; ++x) {
            for (int y=0; y<NUM_COLORS; ++y) {
                colors[y] = new Color(im.getRGB(x, y), true);
            }
            variants[x] = new Variant(colors);
        }

        return new Palette(
                new Subset(variants[0], variants[2]),
                new Subset(variants[1], variants[3])
        );   
    }
    
    /**
     * Inserts a chart of this palette's colours into the script console.
     */
    public void debugDump() {
        int SIZE = 48;
        BufferedImage src = toImage();
        BufferedImage chart = new BufferedImage(src.getWidth() * SIZE, src.getHeight() * SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = chart.createGraphics();
        try {
            g.setPaint(new ca.cgjennings.graphics.paints.CheckeredPaint());
            g.fillRect(0, 0, chart.getWidth(), chart.getHeight());
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(src, 0, 0, chart.getWidth(), chart.getHeight(), null );
        } finally {
            g.dispose();
        }
        ca.cgjennings.apps.arkham.plugins.ScriptMonkey.getSharedConsole().getWriter().insertImage(chart);
    }
}