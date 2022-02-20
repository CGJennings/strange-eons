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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import javax.imageio.ImageIO;
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
 * A resolution independent icon that is drawn using glyphs (symbols) taken from
 * a font. The icon design can be composed with multiple glyphs and use
 * different colours, positions, and orientations. Glyph icons are usually
 * created using a <em>descriptor</em>. This is a compact string that describes
 * how to compose the design. A descriptor can be as simple as the single code
 * point of the symbol you want to display, or it can compose multiple glyphs
 * using a combination of sizes, positions, orientations, and colours. A
 * descriptor has the following syntax:
 *
 * <pre>[gly:]CP[!][%][,FG[,BG]][@[Z][+|-ADJ[,DX[,DY]]]][;...]</pre>
 *
 * <dl>
 * <dt><code>gly:</code><dd>This character sequence is used to distinguish a
 * glyph icon from a resource path, and is ignored here if present; it is
 * followed by one or more <em>layers</em>
 * separated by semicolons. Each layer consists of a single glyph and,
 * optionally, colour and layout information.
 * <dt><code>CP</code><dd>The hexadecimal code point of the glyph to show, or a
 * single character such as <code>A</code>. The special characters
 * <code>; ! % * , @</code> can only be indicated by hexadecimal code point. (By
 * convention the code point uses upper case letters, while colours use lower
 * case.)
 * <dt><code>!</code><dd>If present after a code point, the glyph is mirrored
 * from left-to-right.
 * <dt><code>%</code><dd>If present after a code point, the glyph is mirrored
 * from top-to-bottom.
 * <dt><code>*</code><dd>If present after a code point, the glyph is rotated 45
 * degrees clockwise. (Any rotation that is a multiple of 45 degrees can be
 * produced using a combination of <code>!</code>, <code>%</code>, and
 * <code>*</code>.)
 * <dt><code>,FG</code><dd>The foreground (glyph) colour, see below (optional).
 * <dt><code>,BG</code><dd>The background colour, see below (optional). When a
 * background colour is present, a filled circle (disc) is drawn behind the
 * glyph.
 * <dt><code>@</code><dd>This character indicates that a size and adjustment
 * block will follow. It must be present for any of the size, font size, or
 * offset components to occur. It is used to distinguish the adjustment block
 * from a colour block (which is optional).
 * <dt><code>Z</code><dd>The icon size (optional). This can be an integer number
 * of pixels/points or one of the following characters: <code>t</code> tiny,
 * <code>s</code>, <code>S</code> medium small, <code>m</code> medium,
 * <code>M</code> medium large, <code>l</code> large, <code>L</code> very large,
 * <code>g</code> gigantic. The default is small, which is intended for menu
 * items and most buttons and labels. If an icon size is set in more than one
 * layer, the one closest to the end of the descriptor is used.
 * <dt><code>+|-ADJ</code><dd>This adjusts the font size, relative to the icon's
 * design grid (see below). It consists of either a plus sign (+) or minus sign
 * (-), followed by a number indicating the relative amount to increase or
 * decrease the font size for this glyph. The number may include a radix point
 * (.) and fractional component.
 * <dt><code>,DX</code><dd>Adjusts the relative horizontal position of the glyph
 * from center, in design grid units (see below). Note that the font size
 * adjustment <em>must</em> be present to include an offset adjustment, but the
 * font size adjustment can be <code>+0</code>.
 * <dt><code>,DY</code><dd>Adjusts the relative vertical position of the glyph
 * from center, in design grid units (see below).
 * <dt><code>;...</code><dd>Glyphs may be stacked by listing multiple layers
 * separated by semicolons (;). Layers are listed in order from bottom (drawn
 * first) to top (drawn last).
 * </dl>
 *
 * <p>
 * <strong>The design grid</strong><br>
 * The icon is composed against a hypothetical design grid that is 18 units by
 * 18 units. All size adjustments and position offsets use these units. Hence, a
 * <code>DX</code> value of 1 will shift the glyph 1/18 of the icon width to the
 * right of the center.
 *
 * <p>
 * <stromg>Icon colours</strong><br>
 * A colour is either a 6 or 8 digit hex string, or one of following standard
 * values: r (red), o (orange), y (yellow), g (green), b (blue), i (indigo), v
 * (violet), p (pink), w (brown), t (teal) c (cyan), k (grey), 0 (black) or 1
 * (white). The actual colour obtained may vary depending on the installed
 * theme. Colours have light and dark variants which are chosen based on whether
 * the theme is light or dark. For example, a dark mode theme has dark
 * backgrounds, so the light variant is typically chosen. Using a capital letter
 * for a colour code will switch a light variant to dark or vice-versa. The
 * default colour when none is specified is a "typical" colour taken from the
 * theme designed to pair with standard label text.
 *
 * <p>
 * If a background colour is specified, the background is drawn as a filled
 * circle (disc) behind the glyph. When a background colour is used, the
 * background defaults to using a dark colour for the background and a light
 * colour for the glyph, and the default foreground is white.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class ThemedGlyphIcon extends AbstractThemedIcon {

    /**
     * Prefix used to identify a glyph icon descriptor to
     * {@link ResourceKit.getIcon()}. This is ignored, if present, at the start
     * of a descriptor string.
     */
    public static final String GLYPH_RESOURCE_PREFIX = "gly:";

    private static Font defaultFont;
    private String descriptor;
    private Font font;
    private Layer[] layers;

    //  All rendering is at a standard design size of 18 by 18; the graphics
    //  context is transformed as needed for other sizes.
    private static final int I_SIZE = 18;
    private static final float F_SIZE = 18f;
    private static final double D_SIZE = 18d;

    private ThemedGlyphIcon(ThemedGlyphIcon toCopy) {
        font = toCopy.font;
        width = toCopy.width;
        height = toCopy.height;
        layers = toCopy.layers;
        disabled = toCopy.disabled;
        descriptor = toCopy.descriptor;
    }

    /**
     * Creates a new glyph icon that displays the specified code point using the
     * supplied font and colours.
     *
     * @param font the font to use to draw the glyph symbol
     * @param codePoint the code point of the glyph to draw
     * @param fg the foreground (glyph) colour
     * @param bg the background; if non-null, a filled circle is drawn in this
     * colour behind the glyph
     */
    public ThemedGlyphIcon(Font font, int codePoint, Color fg, Color bg) {
        this.font = font;
        this.layers = new Layer[]{new Layer(codePoint, fg, bg, fg, bg)};
    }

    /**
     * Creates a glyph icon from a descriptor that will draw glyphs from the
     * specified font. See the class description for descriptor syntax.
     *
     * @param font the font containing the desired glyphs
     * @param descriptor the non-null icon description
     */
    public ThemedGlyphIcon(Font font, String descriptor) {
        this.font = font;
        this.descriptor = Objects.requireNonNull(descriptor);
    }

    /**
     * Creates a glyph icon for a built-in icon from a compact string
     * descriptor. See the class description for descriptor syntax.
     *
     * @param descriptor the non-null icon description
     */
    public ThemedGlyphIcon(String descriptor) {
        this.descriptor = Objects.requireNonNull(descriptor);
    }

    @Override
    public ThemedGlyphIcon derive(int newWidth, int newHeight) {
        if (newWidth < 1 || newHeight < 1) {
            throw new IllegalArgumentException("invalid size " + newWidth + 'x' + newHeight);
        }

        if (descriptor != null) {
            // required to make sure we have the right base size
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
        } else if (layers[0].fg == null) {
            // the foreground can only be null if a non-descriptor constructor
            // was used; we'll replace null with the same default as a
            // descriptor would have yielded
            if (COLOURS == null) {
                initColours();
            }
            int defaultFgIndex = COLOURS.length - (layers[0].bg != null ? 2 : 1);
            layers[0].fg = COLOURS[defaultFgIndex][LIGHT_FG];
            layers[0].fgDarkMode = COLOURS[defaultFgIndex][DARK_FG];
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
        for (int i = 0; i < layers.length; ++i) {
            paintLayer(g, layers[i], darkMode);
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

        float xAdj = layer.mirror ? -layer.xAdj : layer.xAdj;
        float yAdj = layer.flip ? -layer.yAdj : layer.yAdj;

        Font layerFont = layer.font == null ? font : layer.font;
        if (layer.fontAdj != 0f) {
            layerFont = layerFont.deriveFont(layerFont.getSize2D() + layer.fontAdj);
        }
        g.setFont(layerFont);
        FontMetrics fm = g.getFontMetrics();
        Rectangle2D bounds = fm.getStringBounds(layer.str, g);
        float sx = (F_SIZE - (float) bounds.getWidth()) / 2f - (float) bounds.getX() + xAdj;
        float sy = (F_SIZE - (float) bounds.getHeight()) / 2f - (float) bounds.getY() + yAdj;
        g.setPaint(fg);

        if (layer.mirror || layer.flip || layer.turn) {
            AffineTransform at = g.getTransform();
            g.scale(layer.mirror ? -1d : 1d, layer.flip ? -1d : 1d);
            g.translate(layer.mirror ? -D_SIZE : 0d, layer.flip ? -D_SIZE : 0d);
            if (layer.turn) {
                g.rotate(Math.PI / 4d, D_SIZE / 2d + xAdj, D_SIZE / 2d + yAdj);
            }
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

        public Layer(int codePoint, Color fg, Color bg, Color darkFg, Color darkBg) {
            this.str = codePointStr(codePoint);
            this.fg = fg;
            this.bg = bg;
            this.fgDarkMode = darkFg;
            this.bgDarkMode = darkBg;
        }
        private String str;
        private Color fg, bg, fgDarkMode, bgDarkMode;
        private boolean mirror, flip, turn;
        private float fontAdj, xAdj, yAdj;
        /**
         * Overrides icon font.
         */
        private Font font;
    }

    private static String codePointStr(int codePoint) {
        return new StringBuilder(2).appendCodePoint(codePoint).toString();
    }

    private void parseDescriptor() {
        if (COLOURS == null) {
            initColours();
        }

        // set a default size for the icon        
        int offset = 0;
        String desc = descriptor;
        if (desc.startsWith(GLYPH_RESOURCE_PREFIX)) {
            offset = GLYPH_RESOURCE_PREFIX.length();
            desc = desc.substring(offset);
        }

        if (desc.indexOf(';') < 0) {
            layers = new Layer[]{parseLayer(descriptor, desc, offset, false)};
        } else {
            boolean hasBackground = false;
            String[] layerStrs = desc.split(";");
            layers = new Layer[layerStrs.length];
            for (int i = 0; i < layerStrs.length; ++i) {
                layers[i] = parseLayer(descriptor, layerStrs[i], offset, hasBackground);
                if (layers[i].bg != null) {
                    hasBackground = true;
                }
                offset += layerStrs[i].length() + 1;
            }
        }
        descriptor = null;
    }

    /**
     * Stand-in layer used when a layer contains errors. Since parsing
     * descriptors is deferred until the first paint, we log errors rather than
     * throw an exception to avoid breaking the entire UI.
     */
    private static final Layer BROKEN_LAYER;

    static {
        final Color TRANSPARENT = new Color(0, true);
        BROKEN_LAYER = new Layer(' ', TRANSPARENT, null, TRANSPARENT, null);
    }

    private static class ParseError extends RuntimeException {

        int offset;

        public ParseError(String message, int offset) {
            super(message);
            this.offset = offset;
        }
    }

    /**
     * Parses a single layer: one glyph code with its colour and adjustments.
     *
     * @param layerStr the description substring representing the layer
     * @param previousLayerHasBackground if true, some lower-level layer
     * included a background colour, which changes the meaning of colour codes
     * @return the new layer
     */
    private Layer parseLayer(String descriptor, String layerStr, int offset, boolean previousLayerHasBackground) {
        Layer layer = new Layer();
        try {
            String cpString = layerStr, adjString = null;
            int at = cpString.indexOf('@', 1);
            if (at >= 0) {
                adjString = layerStr.substring(at + 1);
                layerStr = layerStr.substring(0, at);
                if (adjString.isEmpty()) {
                    offset += layerStr.length() + 1;
                    throw new IllegalArgumentException("Empty adjustment block");
                }
            }

            parseGlyphBlock(layer, layerStr, previousLayerHasBackground);
            if (adjString != null) {
                offset += layerStr.length() + 1;
                parseAdjustmentBlock(layer, adjString);
            }
        } catch (ParseError error) {
            String message = error.getMessage();
            offset += error.offset;
            StringBuilder b = new StringBuilder(128);
            b.append(message).append(" in glyph icon at offset ").append(offset).append(": \n")
                    .append(descriptor).append('\n');
            for (int i = 0; i < offset; ++i) {
                b.append(' ');
            }
            b.append('^');
            StrangeEons.log.warning(b.toString());
            return BROKEN_LAYER;
        }
        return layer;
    }

    private void parseGlyphBlock(Layer layer, String layerStr, boolean previousLayerHasBackground) {
        // split into tokens for code point, foreground, background
        String[] tokens = layerStr.split(",", -1);

        // parse the code point token
        String cpString = tokens[0];
        if (cpString.length() == 1 || (cpString.length() == 2 && Character.isLowSurrogate(cpString.charAt(0)))) {
            layer.str = cpString;
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
            int star = cpString.indexOf('*');
            if (star >= 0) {
                layer.turn = true;
                endOfCodePoint = Math.min(endOfCodePoint, star);
            }

            try {
                int cp = Integer.valueOf(cpString.substring(0, endOfCodePoint), 16);
                layer.str = codePointStr(cp);
            } catch (NumberFormatException nfe) {
                throw new ParseError("bad code point \"" + cpString + '"', 0);
            }
        }
        // the default font only contains PUA symbols, so if this is a
        // "normal" character, substitute a different font
        if (font == null && layer.str.codePointAt(0) < 0xF0001) {
            layer.font = new Font(Font.DIALOG, Font.BOLD, I_SIZE - 3);
        }

        // parse the foreground/background tokens
        int offset = cpString.length() + 1;
        parseColour(layer, tokens.length > 1 ? tokens[1] : "", offset, false, previousLayerHasBackground);
        offset += tokens.length > 1 ? tokens[1].length() : 0;
        parseColour(layer, tokens.length > 2 ? tokens[2] : "", offset, true, previousLayerHasBackground);
    }

    private void parseAdjustmentBlock(Layer layer, String adjString) {
        // split into tokens for icon size, font size, dx, dy
        String[] tokens = adjString.split(",", -1);

        // tracks the "next" token, bumped if there is no icon size
        int t = 1;
        String szString = tokens[0];

        // if the first token starts with '+' or '-', then the icon size
        // was not specified and this starts with a font size adjustment
        if (!szString.isEmpty() && (szString.charAt(0) == '+' || szString.charAt(0) == '-')) {
            t = 0;
            szString = "";
        }

        // icon size: letter size or number of pixels
        if (!szString.isEmpty()) {
            int size = -1;
            if (Character.isDigit(szString.charAt(0))) {
                try {
                    size = Integer.parseInt(szString);
                } catch (NumberFormatException nfe) {
                    // handled below
                }
            } else if (szString.length() == 1) {
                switch (szString.charAt(0)) {
                    case 't':
                        size = TINY;
                        break;
                    case 's':
                        size = SMALL;
                        break;
                    case 'S':
                        size = MEDIUM_SMALL;
                        break;
                    case 'm':
                        size = MEDIUM;
                        break;
                    case 'M':
                        size = MEDIUM_LARGE;
                        break;
                    case 'l':
                        size = LARGE;
                        break;
                    case 'L':
                        size = VERY_LARGE;
                        break;
                    case 'g':
                        size = GIGANTIC;
                        break;
                }
            }
            if (size < 1) {
                throw new ParseError("invalid icon size \"" + szString + '"', 0);
            }
            width = height = size;
        }

        // offset into adjString, for error reporting
        int offset = szString.length() + t;

        // font size and glyph position adjustments
        float[] adj = new float[3];
        for (int i = 0; i < 3 && t < tokens.length; ++i) {
            if (!tokens[t].isEmpty()) {
                try {
                    adj[i] = Float.parseFloat(tokens[t]);
                } catch (NumberFormatException nfe) {
                    throw new ParseError("invalid adjustment value \"" + tokens[t] + '"', offset);
                }
            }
            offset += tokens[t++].length() + 1;
        }

        if (t < tokens.length) {
            throw new ParseError("extra parameters", offset);
        }

        layer.fontAdj = adj[0];
        layer.xAdj = adj[1];
        layer.yAdj = adj[2];
    }

    /**
     * Parse a colour value, either a colour code or a 6- or 8-digit hex colour.
     *
     * @param layer the value of layer
     * @param colour the value of colour
     * @param offset offset into descriptor, for describing errors
     * @param isBg the value of isBg
     * @param previousLayerHasBackground the value of previousLayerHasBackground
     */
    private static void parseColour(Layer layer, String colour, int offset, boolean isBg, boolean previousLayerHasBackground) {
        if (colour.length() == 6 || colour.length() == 8) {
            try {
                Color c = new Color((int) Long.parseLong(colour, 16), colour.length() == 8);
                if (isBg) {
                    layer.bg = c;
                    layer.bgDarkMode = c;
                } else {
                    layer.fg = c;
                    layer.fgDarkMode = c;
                }
                return;
            } catch (NumberFormatException nfe) {
                throw new ParseError("invalid colour \"" + colour + '"', offset);
            }
        }
        if (isBg && (previousLayerHasBackground || colour.isEmpty())) {
            // the default bg is null, and if this is a stacked glyph
            // we will ignore any background that might be specified
            return;
        }
        if (colour.isEmpty()) {
            colour = "\0"; // becomes default foreground
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
            defaultVariants = new Color[]{
                itemOpaque, itemOpaque, textTransl, itemTransl
            };
        } else {
            defaultVariants = new Color[]{
                itemOpaque, itemOpaque, itemTransl, textTransl
            };
        }
        COLOURS[COLOURS.length - 1] = defaultVariants;
    }

    /**
     * Injects built-in icons into the script console window for visual
     * inspection.
     */
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

    /**
     * Starts a simple tool to help construct built-in glyph icons
     * interactively.
     */
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
            tf.setDragEnabled(true);
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
                    // left click to toggle light/dark
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        label.setBackground(label.getBackground() == light ? dark : light);
                    } // right click to toggle enabled/disabled
                    else if (e.getButton() == MouseEvent.BUTTON3) {
                        label.setEnabled(!label.isEnabled());
                    } // middle button to save as a PNG
                    else if (e.getButton() == MouseEvent.BUTTON2) {
                        BufferedImage im = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g = im.createGraphics();
                        try {
                            ThemedIcon icon = ((ThemedGlyphIcon) label.getIcon()).derive(128);
                            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            icon.paintIcon(null, g, 0, 0);
                        } finally {
                            g.dispose();
                        }
                        try {
                            File output = File.createTempFile("glyph-icon-", ".png");
                            ImageIO.write(im, "png", output);
                            System.out.println(output.getAbsolutePath());
                        } catch (IOException ex) {
                            ex.printStackTrace(System.out);
                        }
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
