package ca.cgjennings.ui.theme;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.plugins.ScriptConsole;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.io.EscapedLineReader;
import ca.cgjennings.ui.DocumentEventAdapter;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.DocumentEvent;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import resources.ResourceKit;

/**
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class ThemedGlyphIcon extends AbstractThemedIcon {
    /**
     * Prefix used to identify a glyph icon descriptor to {@link ResourceKit.getIcon()}.
     * This is ignored, if present, at the start of a descriptor string.
     */
    public static final String GLYPH_RESOURCE_PREFIX = "gly:";
    

    private static Font defaultFont;
    private String descriptor;
    private Font font;
    private Layer[] layer;

    //  All rendering is at a standard design size of 18 by 18; the graphics
    //  context is transformed as needed for other sizes.
    private static final int I_SIZE = 18;
    private static final float F_SIZE = 18f;
    private static final double D_SIZE = 18d;

    private ThemedGlyphIcon(ThemedGlyphIcon toCopy) {
        font = toCopy.font;
        width = toCopy.width;
        height = toCopy.height;
        layer = toCopy.layer;
        disabled = toCopy.disabled;
        descriptor = toCopy.descriptor;
    }

    public ThemedGlyphIcon(int codePoint) {
        this(codePoint, null, null);
    }

    public ThemedGlyphIcon(int codePoint, Color fg, Color bg) {
        this(null, codePoint, fg, bg);
    }

    public ThemedGlyphIcon(Font font, int codePoint, Color fg, Color bg) {
        this.layer = new Layer[]{new Layer(codePoint, fg, bg, fg, bg, 0)};
        width = height = I_SIZE;
    }

    /**
     * Creates a glyph icon for a built-in icon from a compact string
     * descriptor. A descriptor has the following form:
     *
     * <pre>CP[!][%][,FG[,BG]][@[Z][+|-ADJ]</pre>
     * 
     * <dl>
     * <dt>CP<dd>hexadecimal code point of glyph to show, or a single character
     * <dt>!<dd>mirror the glyph left-to-right
     * <dt>%<dd>mirror the glyph top-to-bottom
     * <dt>FG<dd>foreground (glyph) colour, see below
     * <dt>BG<dd>background colour, see below
     * <dt>Z<dd>icon size: one of s (small), m (medium), l (large),
     *           g (gigantic) or an integer number of pixels; the default is
     *           small, which is suited for menu items and labels with text
     * </dl>
     * 
     * <p>A colour is either a 6 or 8 digit hex string, or one of following
     * standard values: r (red), o (orange), y (yellow), g (green), b (blue),
     * i (indigo), v (violet), p (pink), w (brown), t (teal) c (cyan), k (grey),
     * 0 (black) or 1 (white). Other than 0 or 1, the standard colours may vary
     * depending on the installed theme. Colours have light and dark variants
     * which are chosen based on whether the theme is light or dark. For example,
     * a dark mode theme has dark backgrounds, so the light variant is typically
     * chosen. Using a capital letter will switch a light variant to dark or
     * vice-versa. The default colour when none is specified is taken from the
     * standard colour for menu items and/or labels.
     * 
     * <p>If a background colour is specified, the background is drawn as a filled
     * circle (disc) behind the glyph. When a background colour is used, the background
     * defaults to using a dark colour for the background and a light colour for
     * the glyph, and the default foreground is white.
     *
     * <p>Glyphs may be stacked by listing multiple descriptors separated by
     * semicolons (;).
     *
     * @param descriptor the non-null icon description
     */
    public ThemedGlyphIcon(String descriptor) {
        this.descriptor = Objects.requireNonNull(descriptor);
        if (descriptor.startsWith(GLYPH_RESOURCE_PREFIX)) {
            this.descriptor = descriptor.substring(GLYPH_RESOURCE_PREFIX.length());
        }
    }
    
    @Override
    public ThemedGlyphIcon derive(int newWidth, int newHeight) {
        if (newWidth < 1 || newHeight < 1) {
            throw new IllegalArgumentException("invalid size " + newWidth + 'x' + newHeight);
        }

        if (descriptor != null) {
            parseDescriptor();
        }
        if (newWidth == width && newHeight == height) {
            return this;
        }

        ThemedGlyphIcon gi = new ThemedGlyphIcon(this);
        gi.width = newWidth;
        gi.height = newHeight;
        return gi;
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g, int x, int y) {
        if (descriptor != null) {
            parseDescriptor();
        } else if (layer[0].fg == null) {
            // the foreground can only be null if a non-descriptor constructor
            // was used; we'll replace null with the same default as a
            // descriptor would have yielded
            if (COLOURS == null) {
                initColours();
            }
            int defaultFgIndex = COLOURS.length - (layer[0].bg != null ? 2 : 1);
            layer[0].fg = COLOURS[defaultFgIndex][LIGHT_FG];
            layer[0].fgDarkMode = COLOURS[defaultFgIndex][DARK_FG];
        }
        if (font == null) {
            font = getDefaultFont();
        }
        
        g = (Graphics2D) g.create();
        try {
            g.addRenderingHints((Map<?, ?>) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints"));
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);            
            g.translate(x, y);
            g.clipRect(0, 0, width, height);
            g.scale(((double) width) / D_SIZE, ((double) height) / D_SIZE);

            if (disabled || (c != null && !c.isEnabled())) {
                g.setComposite(AlphaComposite.SrcOver.derive(0.4f));
            }
            
            boolean drawDarkMode;
            if (c != null) {
                Color compBg = c.getBackground();
                if (compBg != null) {
                    drawDarkMode = (compBg.getRed() + compBg.getGreen() + compBg.getBlue()) / 3 < 150;
                } else {
                    drawDarkMode = ThemeInstaller.isDark();
                }
            } else {
                drawDarkMode = ThemeInstaller.isDark();
            }

            paintGlyph(g, drawDarkMode);

        } finally {
            g.dispose();
        }
    }

    /**
     * Called with a transformed and clipped graphics context to paint the
     * icon's glyph. The context is scaled so that no matter the actual icon
     * size, the method should paint the icon as if it were 18 pixels by 18
     * pixels in size.
     *
     * @param g the graphic context
     * @param darkMode true if the icon is being painted against a dark
     * background, or if the background is unknown, in a dark theme
     */
    protected void paintGlyph(Graphics2D g, boolean darkMode) {
        for (int i = 0; i < layer.length; ++i) {
            paintLayer(g, layer[i], darkMode);
        }
    }

    private void paintLayer(Graphics2D g, Layer layer, boolean darkMode) {
        Color fg, bg;

        if (darkMode) {
            fg = layer.fgDarkMode;
            bg = layer.bgDarkMode;
        } else {
            fg = layer.fg;
            bg = layer.bg;
        }

        if (bg != null) {
            g.setPaint(bg);
            g.fillOval(0, 0, I_SIZE, I_SIZE);
        }

        Font layerFont = font;
        if (layer.fontAdj != 0) {
            layerFont = layerFont.deriveFont(font.getSize2D() + layer.fontAdj);
        }        
        g.setFont(layerFont);
        FontMetrics fm = g.getFontMetrics();
        Rectangle2D bounds = fm.getStringBounds(layer.str, g);
        float sx = (F_SIZE - (float) bounds.getWidth()) / 2f - (float) bounds.getX() + layer.xAdj;
        float sy = (F_SIZE - (float) bounds.getHeight()) / 2f - (float) bounds.getY() + layer.yAdj;
        g.setPaint(fg);

        if (layer.mirror || layer.flip) {
            AffineTransform at = g.getTransform();
            g.scale(layer.mirror ? -1d : 1d, layer.flip ? -1d : 1d);
            g.translate(layer.mirror ? -D_SIZE : 0d, layer.flip ? -D_SIZE : 0d);
            g.drawString(layer.str, sx, sy);
            g.setTransform(at);
        } else {
            g.drawString(layer.str, sx, sy);
        }
    }

    @Override
    public int getIconWidth() {
        if (descriptor != null) {
            parseDescriptor();
        }
        return width;
    }

    @Override
    public int getIconHeight() {
        if (descriptor != null) {
            parseDescriptor();
        }
        return height;
    }

    private static Font getDefaultFont() {
        if (defaultFont == null) {
            try (InputStream in = ThemedGlyphIcon.class.getResourceAsStream("/resources/icons/icon-data")) {
                defaultFont = Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(F_SIZE);
            } catch (IOException | FontFormatException ex) {
                StrangeEons.log.log(Level.SEVERE, "unable to load icon data");
                defaultFont = new Font(Font.MONOSPACED, Font.PLAIN, I_SIZE);
            }
        }
        return defaultFont;
    }

    private static final class Layer {

        public Layer() {
        }

        public Layer(int codePoint, Color fg, Color bg, Color darkFg, Color darkBg, int fontAdj) {
            this.str = codePointStr(codePoint);
            this.fg = fg;
            this.bg = bg;
            this.fgDarkMode = darkFg;
            this.bgDarkMode = darkBg;
            this.fontAdj = fontAdj;
        }
        private String str;
        private Color fg, bg, fgDarkMode, bgDarkMode;
        private boolean mirror, flip;
        private float fontAdj, xAdj, yAdj;
    }

    private static String codePointStr(int codePoint) {
        return new StringBuilder(2).appendCodePoint(codePoint).toString();
    }
    

    private void parseDescriptor() {
        if (COLOURS == null) {
            initColours();
        }

        if (descriptor.indexOf(';') < 0) {
            layer = new Layer[]{parseLayer(descriptor, false)};
        } else {
            boolean hasBackground = false;
            String[] layerStrs = descriptor.split(";");
            layer = new Layer[layerStrs.length];
            for (int i = 0; i < layerStrs.length; ++i) {
                layer[i] = parseLayer(layerStrs[i], hasBackground);
                if (layer[i].bg != null) {
                    hasBackground = true;
                }
            }
        }
        descriptor = null;
    }

    private Layer parseLayer(String layerStr, boolean previousLayerHasBackground) {
        String cpString, fgString = "", bgString = "", szString = "", adjString = "";
        
        int at = layerStr.indexOf('@');
        if (at >= 0) {
            szString = layerStr.substring(at+1);
            layerStr = layerStr.substring(0, at);
        }
        
        int comma = layerStr.indexOf(',');
        if (comma < 0) {
            cpString = layerStr;
        } else {
            cpString = layerStr.substring(0, comma);
            int comma2 = layerStr.lastIndexOf(',');
            if (comma == comma2) {
                fgString = layerStr.substring(comma + 1);
            } else {
                fgString = layerStr.substring(comma + 1, comma2);
                bgString = layerStr.substring(comma2 + 1);
            }
        }
        if (!szString.isEmpty()) {
            int plusMinus = Math.min(indexOfOrLength(szString, '-'), indexOfOrLength(szString, '+') + 1);
            if (plusMinus < szString.length()) {
                adjString = szString.substring(plusMinus);
                szString = szString.substring(0, plusMinus);
            }
        }

        Layer layer = new Layer();

        // code point
        try {
            if (cpString.length() == 1 || (cpString.length() == 2 && Character.isLowSurrogate(cpString.charAt(0)))) {
                layer.str = cpString;
                if (font == null) {
                    font = new Font(Font.DIALOG, Font.BOLD, I_SIZE - 3);
                }
            } else {
                int endOfCodePoint = cpString.length();

                // check for mirror/flip characters
                int exclam = cpString.indexOf('!');
                if (exclam >= 0) {
                    layer.mirror = true;
                    endOfCodePoint = exclam;
                }
                int perc = cpString.indexOf('%');
                if (perc >= 0) {
                    layer.flip = true;
                    endOfCodePoint = Math.min(endOfCodePoint, perc);
                }

                layer.str = codePointStr(Integer.valueOf(cpString.substring(0, endOfCodePoint), 16));
            }
        } catch (NumberFormatException nfe) {
            StrangeEons.log.log(Level.WARNING, "bad code point {0}", cpString);
            layer.str = " ";
        }

        // colours
        parseColour(bgString, layer, true, previousLayerHasBackground);
        parseColour(fgString, layer, false, previousLayerHasBackground);

        // size
        width = height = I_SIZE;
        if (!szString.isEmpty()) {
            if (Character.isDigit(szString.charAt(0))) {
                try {
                    width = height = Integer.parseInt(szString);
                } catch (NumberFormatException nfe) {
                    StrangeEons.log.log(Level.WARNING, "bad size {0}", szString);
                }
            } else {
                switch (szString.charAt(0)) {
                    case 't':
                        width = height = TINY;
                        break;
                    case 's':
                        break;
                    case 'S':
                        width = height = MEDIUM_SMALL;
                        break;
                    case 'm':
                        width = height = MEDIUM;
                        break;
                    case 'M':
                        width = height = MEDIUM_LARGE;
                        break;                        
                    case 'l':
                        width = height = LARGE;
                        break;
                    case 'L':
                        width = height = VERY_LARGE;
                        break;                        
                    case 'g':
                        width = height = GIGANTIC;
                        break;
                    default:
                        StrangeEons.log.log(Level.WARNING, "unknown size {0}", szString);
                }
            }
        }
        
        // size adjust
        if (!adjString.isEmpty()) {
            try {
                String[] parts = adjString.split(",");
                layer.fontAdj = Float.parseFloat(parts[0]);
                if (parts.length >= 2) {
                    layer.xAdj = Float.parseFloat(parts[1]);
                    if (parts.length >= 3) {
                        layer.yAdj = Float.parseFloat(parts[2]);
                    }
                }
            } catch (NumberFormatException nfe) {
                StrangeEons.log.log(Level.WARNING, "bad font size adjustment {0}", adjString);
            }
        }

        return layer;
    }
    
    private static int indexOfOrLength(String s, char ch) {
        int index = s.indexOf(ch);
        return index < 0 ? s.length() : index;
    }

    /**
     * A colour can be a 6 or 8 digit hex value, or a letter such as 'r' (red)
     * to use a predetermined colour value. The predetermined values come with
     * variations depending on whether the theme is light or dark. A glyph by
     * itself will use a light colour on a dark background, or vice-versa.
     * But if the glyph has a background, it reverses whether the foreground
     * is light or dark.
     * 
     * Colour codes can be capitalized to reverse their normal meaning. So,
     * a 'r' is dark red on light backgrounds, but 'R' is light red.
     * 
     * @param colour
     * @param layer
     * @param isBg
     * @param previousLayerHasBackground 
     */
    private static void parseColour(String colour, Layer layer, boolean isBg, boolean previousLayerHasBackground) {
        if (colour.length() == 6 || colour.length() == 8) {
            try {
                Color c = new Color(Integer.valueOf(colour, 16), colour.length() == 8);
                if (isBg) {
                    layer.bg = c;
                    layer.bgDarkMode = c;
                } else {
                    layer.fg = c;
                    layer.fgDarkMode = c;
                }
                return;
            } catch (NumberFormatException nfe) {
                StrangeEons.log.log(Level.WARNING, "bad colour {0}", colour);
                colour = "";
            }
        }
        if (isBg && (previousLayerHasBackground || colour.isEmpty())) {
            // the default bg is null, and if this is a stacked glyph
            // we will ignore any background that might be specified
            return;
        }
        if (colour.isEmpty()) {
            colour = "\0";
            // the default fg is white when a bg is set
            if (!isBg && layer.bg != null) {
                colour = "1";
            }
        }
        

        char colourCode = colour.charAt(0);
        boolean reverseLightDark = false;
        if (Character.isUpperCase(colourCode)) {
            colourCode = Character.toLowerCase(colourCode);
            reverseLightDark = true;
        }

        int c = COLOUR_CODES.indexOf(colourCode);
        if (c < 0) {
            c = COLOUR_CODES.length();
            // when no colour is specified AND there is a background,
            // treat the default as '1' (white) instead of the menu text colour
            if (layer.bg != null || previousLayerHasBackground) {
                --c;
            }
        }

        if (isBg) {
            // for backgrounds, use the dark colour on light UI
            if (reverseLightDark) {
                layer.bg = COLOURS[c][LIGHT_BG];
                layer.bgDarkMode = COLOURS[c][DARK_BG];
            } else {
                layer.bg = COLOURS[c][DARK_BG];
                layer.bgDarkMode = COLOURS[c][LIGHT_BG];
            }
            return;
        }

        // for foregrounds, use dark colour on light UI
        //               OR use light colour if there is a background        
        boolean darkIfDarkMode = layer.bg != null || previousLayerHasBackground;
        if (reverseLightDark) {
            darkIfDarkMode = !darkIfDarkMode;
        }
        if (darkIfDarkMode) {
            layer.fg = COLOURS[c][LIGHT_FG];
            layer.fgDarkMode = COLOURS[c][DARK_FG];
        } else {
            layer.fg = COLOURS[c][DARK_FG];
            layer.fgDarkMode = COLOURS[c][LIGHT_FG];
        }
    }


    private static final String COLOUR_CODES = "roygbivctwpe01";
    private static Color[][] COLOURS;
    private static final int LIGHT_BG = 0, DARK_BG = 1, LIGHT_FG = 2, DARK_FG = 3;
    
    private static void initColours() {
        int[] rgb = new int[]{
        //  light bg  dark bg   light fg    dark fg
            0xEF5350, 0xb71c1c, 0xf8e57373, 0xf8b71c1c, // (r)ed
            0xFFA726, 0xe65100, 0xf8ffb74d, 0xf8e65100, // (o)range
            0xFFEE58, 0xf9a825, 0xf8fff176, 0xf8f9a825, // (y)ellow
            0x66BB6A, 0x33691e, 0xf8aed581, 0xf833691e, // (g)reen
            0x42A5F5, 0x0d47a1, 0xf864b5f8, 0xf80d47a1, // (b)lue
            0x5C6BC0, 0x1a237e, 0xf87986cb, 0xf81a237e, // (i)ndigo
            0x7E57C2, 0x4a1f8c, 0xf89575cd, 0xf84a1f8c, // (v)iolet
            0x26C6DA, 0x006064, 0xf84dd0e1, 0xf8006064, // (c)yan
            0x26A69A, 0x004d40, 0xf84db6ac, 0xf8004d40, // (t)eal
            0x8D6E63, 0x4e342e, 0xf8a1887f, 0xf84e342e, // bro(w)n
            0xEC407A, 0x880e4f, 0xf8F06292, 0xf8880e4f, // (p)ink
            0xBDBDBD, 0x424242, 0xf8bdbdbd, 0xf8424242, // gre(e)y
            0x000000, 0x000000, 0xf8000000, 0xf8000000, // (0) force black
            0xffffff, 0xffffff, 0xf8ffffff, 0xf8ffffff, // (1) force white
        };
        COLOURS = new Color[(rgb.length / 4) + 1][]; // +1 for default row
        for (int i = 0, c = 0; i < rgb.length;) {
            Color[] variant = new Color[4];
            COLOURS[c++] = variant;
            for (int v = 0; v < 4; ++v) {
                final int clr = rgb[i++];
                variant[v] = new Color(clr, (clr & 0xff000000) != 0);
            }
        }
        // final row is for default colours, we will extract these from
        // the colours used for menu items; the idea is use the same colour
        // as menu item text (and other labels, usually) but to give it
        // some translucency so it doesn't dominate labels visually
        JMenuItem source = new JMenuItem("a");
        Color textOpaque = source.getForeground();
        Color textTransl = new Color((textOpaque.getRGB() & 0xffffff) | 0xcc000000, true);
        Color itemOpaque = source.getBackground();
        Color itemTransl = new Color((itemOpaque.getRGB() & 0xffffff) | 0xcc000000, true);
        Color[] defaultVariants;
        if (ThemeInstaller.isDark()) {
            defaultVariants = new Color[] {
                itemOpaque, itemOpaque, textTransl, itemTransl
            };                        
        } else {
            defaultVariants = new Color[] {
                itemOpaque, itemOpaque, itemTransl, textTransl
            };            
        }
        COLOURS[COLOURS.length-1] = defaultVariants;
    }
    
    /** Injects built-in icons into the script console window for visual inspection. */
    public static void debugDump() {
        final ScriptConsole con = ScriptMonkey.getSharedConsole();
        con.setVisible(true);
        try (InputStream in = ResourceKit.class.getResourceAsStream("icons/map.properties")) {
            EscapedLineReader elr = new EscapedLineReader(in);
            String[] pair;
            while ((pair = elr.readProperty()) != null) {
                javax.swing.JLabel entry = new javax.swing.JLabel(pair[0], ResourceKit.getIcon(pair[0]), javax.swing.JLabel.LEADING);
                entry.setFont(ResourceKit.getEditorFont());
                con.getWriter().insertComponent(entry);
                con.getWriter().println();
            }
            con.getWriter().flush();
        } catch (IOException | RuntimeException ioe) {
            StrangeEons.log.log(Level.SEVERE, "unable to load icon map", ioe);
        }
    }
    
    /** Starts a simple tool to help construct built-in glyph icons interactively. */
    public static void main(String[] argv) {
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new NimbusLookAndFeel() {
                    @Override
                    public Icon getDisabledIcon(JComponent c, Icon i) {
                        return i;
                    }
                });
            } catch (UnsupportedLookAndFeelException ulaf) {
            }
            JFrame f = new JFrame();
            final JLabel label = new JLabel(new ThemedGlyphIcon("gly:F0198"));
            JTextField tf = new JTextField("gly:F0198");
            tf.getDocument().addDocumentListener(new DocumentEventAdapter() {
                @Override
                public void changedUpdate(DocumentEvent e) {
                    try {
                        label.setIcon(new ThemedGlyphIcon(tf.getText().trim()));
                    } catch (Exception ex) {
                    }
                }
            });
            
            Color dark = new Color(0x323232);
            Color light = new Color(0xe0e0e0);

            final JComponent cp = (JComponent) f.getContentPane();
            label.setBackground(light);
            label.setOpaque(true);
            cp.add(label);
            cp.add(tf, BorderLayout.NORTH);
            cp.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        label.setBackground(label.getBackground() == light ? dark : light);
                    } else {
                        label.setEnabled(!label.isEnabled());
                    }
                }
            });
            f.setSize(200, 200);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setLocationByPlatform(true);
            f.setVisible(true);
        });
    }    
}
