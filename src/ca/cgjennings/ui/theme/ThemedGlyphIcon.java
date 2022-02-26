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
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.DocumentEvent;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import resources.ResourceKit;

/**
 * A resolution independent icon that is drawn using glyphs (symbols) taken from
 * a font. A glyph icon can be created from a specific font, code point, and
 * colour description, but more commonly it is created either from a label or
 * descriptor. A label-based icon displays a letter chosen from automatically
 * from a label string, optionally with a solid circular background.
 * A descriptor is a compact string that describes how to compose the icon
 * design. A descriptor can be as simple as the single code
 * point of the symbol you want to display, or it can compose multiple glyphs
 * using a combination of sizes, positions, orientations, and colours.
 * Descriptors have the following syntax:
 *
 * <pre>[gly:]CP[!][%][,FG[,BG]][@[Z][,+|-ADJ[,DX[,DY]]]][;...]</pre>
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
 * of points or one of the following characters: <code>t</code> tiny,
 * <code>s</code> small, <code>S</code> medium small, <code>m</code> medium,
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
 * from center, in design grid units (see below).
 * <dt><code>,DY</code><dd>Adjusts the relative vertical position of the glyph
 * from center, in design grid units (see below).
 * <dt><code>;...</code><dd>Glyphs may be stacked by listing multiple layers
 * separated by semicolons (;). Layers are listed in order from bottom (drawn
 * first) to top (drawn last).
 * </dl>
 *
 * <p>
 * <stromg>Icon colours</strong><br>
 * A colour is either a Web-style hex string or a single character that
 * describes a standard palette colour. The standard palette colours are: r
 * (red), o (orange), y (yellow), g (green), b (blue), i (indigo), v (violet), p
 * (pink), w (brown), t (teal) c (cyan), k (grey), 0 (black) or 1 (white). The
 * actual colour obtained may vary depending on the installed theme. A Web-style
 * colour can be 3 or 6 hex digits (4 or 8 if an alpha value is included) in
 * ARGB order (not RGBA). They may start with <code>#</code>, which is ignored.
 * Web colours may also be modified by the theme, although most themes do not do
 * so. To explicitly forbid modifying a colour, append a <code>!</code>. For
 * example, the following would produce pure red that cannot be modified:
 * <code>#f00!</code>, as would <code>ff0000!</code>. A <code>!</code> can be
 * added to a palette colour, but it had no effect as palette colours are always
 * determined by the theme.
 *
 * <p>
 * Palette colours have light and dark variants which are chosen based on
 * whether the theme is light or dark. For example, a dark mode theme has dark
 * backgrounds, so the light variant is typically chosen. Using a capital letter
 * for the colour code will switch a light variant to dark or vice-versa. The
 * default colour when none is specified is a "typical" colour taken from the
 * theme designed to pair with standard label text.
 *
 * <p>
 * If a background colour is specified, the background is drawn as a filled
 * circle (disc) behind the glyph. When a background colour is used, the
 * background defaults to using a dark colour for the background and a light
 * colour for the glyph, and the default foreground is white.
 *
 * <p>
 * <strong>The design grid and icon position adjustments</strong><br>
 * The icon is composed against a hypothetical design grid that is 18 units by
 * 18 units. All size adjustments and position offsets use these units. Hence, a
 * <code>DX</code> value of 1 will shift the glyph 1/18 of the icon width to the
 * right of the center.
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
     * Creates a new glyph icon that displays a letter selected from a
     * specified string. For example, given the string {@code "The monkey is
     * happy."}, the icon might display the letter M (from monkey).
     * 
     * <p>
     * <strong>Note:</strong> Tag strings and descriptors, while both strings,
     * are interpreted completely differently.
     * 
     * @param label a string containing text from which a letter should be chosen
     * for the icon to display
     * @param fg an optional foreground colour, if null a default is selected
     * @param bg an optional background colour, if null then no background is
     * included
     * @see #derive(java.lang.String)
     * @see #ThemedGlyphIcon(java.lang.String) 
     */
    public ThemedGlyphIcon(String label, Color fg, Color bg) {
        if (fg == null) {
            fg = bg == null ? Palette.get.foreground.opaque.text
                    : Palette.get.foreground.translucent.white;            
        }
        Layer layer = createTagLayer(label, fg, bg);
        if (layer == BROKEN_LAYER) {
            layer = createTagLayer("X", fg, bg);
            layer.str = " ";
        }
        layers = new Layer[]{layer};
        font = layer.font;
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
        if (fg == null) {
            fg = Palette.get.contrasting(null).translucent.text;
        }
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

    /**
     * Returns a new icon with the same image as this icon, but "tagged" with a
     * letter taken from a string. For example, given the string {@code "The monkey is
     * happy."}, the icon might be tagged with the letter M (from monkey).
     *
     * <p>
     * Generally, for the tag to be legible this icon must either be blank, have
     * a background, or use a glyph that has a solid filled area around the
     * middle.
     *
     * @param label the string to choose a tag letter from
     * @return the tagged icon
     */
    public ThemedGlyphIcon derive(String label) {
        Layer tagLayer = createTagLayer(label, Palette.get.foreground.translucent.white, null);
        if (tagLayer == BROKEN_LAYER) {
            // no suitable tag letter
            return this;
        }

        ThemedGlyphIcon gi = new ThemedGlyphIcon(this);
        // check if this icon already appears to have a tag layer;
        // if so replace the tag and if not append the tag as a new
        // layer
        Layer last = gi.layers[gi.layers.length - 1];
        if (last.str != null && last.str.length() > 0
                && Character.isAlphabetic(last.str.codePointAt(0))
                && tagLayer.fg.equals(last.fg)
                && tagLayer.bg.equals(last.bg)
                && tagLayer.fgDarkMode.equals(last.fgDarkMode)
                && tagLayer.bgDarkMode.equals(last.bgDarkMode)
                && tagLayer.font.equals(last.font)
                && tagLayer.fontAdj == last.fontAdj
                && !last.flip && !last.mirror && !last.turn
                && last.xAdj == 0f && last.yAdj == 0f) {
            if (tagLayer.str.equals(last.str)) {
                return this;
            }
            gi.layers = Arrays.copyOf(gi.layers, gi.layers.length);
        } else {
            gi.layers = Arrays.copyOf(gi.layers, gi.layers.length + 1);
        }
        gi.layers[gi.layers.length - 1] = tagLayer;
        return gi;
    }

    private static Layer createTagLayer(String label, Color fg, Color bg) {
        if (label == null || label.isEmpty()) {
            return BROKEN_LAYER;
        }

        label = label.toUpperCase();
        
        int tagLetter = -1;
        String[] words = label.split("(\\s|'|_)+");
        for (String w : words) {
            if (w.isEmpty() || STOP_WORDS.contains(w)) {
                continue;
            }
            int codepoint = w.codePointAt(0);
            if (Character.isLetter(codepoint)) {
                tagLetter = codepoint;
                break;
            }
        }

        // didn't find a typical tag, try harder before giving up
        for (int attempt = 0; attempt < 2 && tagLetter < 0; ++attempt) {
            for (int i = 0; i < label.length();) {
                int codepoint = label.codePointAt(i);
                if (attempt == 0 && Character.isLetterOrDigit(codepoint)) {
                    tagLetter = codepoint;
                    break;
                }
                if (attempt == 1 && !Character.isSpaceChar(codepoint)) {
                    tagLetter = codepoint;
                    break;
                }
                i += Character.isSupplementaryCodePoint(codepoint) ? 2 : 1;
            }
        }

        // nothing suitable in string
        if (tagLetter < 0) {
            return BROKEN_LAYER;
        }

        Layer tagLayer = new Layer(tagLetter, fg, bg, fg, bg);
        tagLayer.font = getTagFont();
        tagLayer.fontAdj = -4f;

        return tagLayer;
    }

    /**
     * List of articles to ignore when choosing a word as the basis for a tag.
     */
    private static String STOP_WORDS
            = // Note that any word only needs to appear once, even
            // if it occurs in more than one language.
            //
            // English
            "A AN THE "
            + // French - L matches l'
            "L LE LA LES UN UNE DES "
            + // Italian
            "IL LO LA I GLI UNA UN' DEI DEGLI DELLE "
            + // Spanish
            "EL LOS LAS UNA UNOS UNAS "
            + // Portuguese
            "O OS AS UM UMA UNS UMAS "
            + // German
            "EIN DER EINE DIE EIN DAS "
            + // Swedish
            "EN ETT ";

    @Override
    protected void paintIcon(Component c, Graphics2D g, int x, int y) {
        if (descriptor != null) {
            parseDescriptor();
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
            try (InputStream in = ThemedGlyphIcon.class.getResourceAsStream("/resources/icons/icons.bin")) {
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
            StrangeEons.log.warning(
                    message + " in glyph icon at offset " + offset + "\n  "
                    + descriptor + "\n  "
                    + (" ".repeat(offset)) + '^'
            );
            return BROKEN_LAYER;
        }
        return layer;
    }

    private void parseGlyphBlock(Layer layer, String layerStr, boolean previousLayerHasBackground) {
        // split into tokens for code point, foreground, background
        String[] tokens = layerStr.split(",", -1);

        if (tokens.length > 3) {
            throw new ParseError("extra parameters", layerStr.length() - 1);
        }

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
        if (font == null && layer.str.codePointAt(0) < 0xf0001 && layer.str.codePointAt(0) != 0xf68c) {
            layer.font = getTagFont();
        }

        // parse the foreground/background tokens: if a background is specified,
        // it must be parsed first since the default foreground depends on it
        int fgOffset = cpString.length() + 1;
        int bgOffset = fgOffset + (tokens.length > 1 ? tokens[1].length() : 0);
        parseColour(layer, tokens.length > 2 ? tokens[2] : "", bgOffset, true, previousLayerHasBackground);
        parseColour(layer, tokens.length > 1 ? tokens[1] : "", fgOffset, false, previousLayerHasBackground);
    }

    private static Font getTagFont() {
        return new Font(Font.DIALOG, Font.BOLD, I_SIZE - 3);
    }

    private static int indexOrLength(String s, char ch) {
        int i = s.indexOf(ch);
        return i < 0 ? s.length() : i;
    }

    /**
     * As a special case, the icon size can be missing or combined with the font
     * adjustment if the font adjustment starts with an explicit + or -. For
     * example, @t+2 is equivalent to @t,2 and @-1 is equivalent to
     *
     * @,-1 which in turn is equivalent to @s,-1. This helper function handles
     * this by rewriting an adjustment string that follows these patterns so
     * that it follows the standard syntax. Then {@link #parseAdjustmentBlock}
     * can treat all cases with the same logic.
     */
    private static String injectPhantomComma(String adjString) {
        int splitPoint = Math.min(
                indexOrLength(adjString, ','),
                Math.min(indexOrLength(adjString, '+'), indexOrLength(adjString, '-'))
        );
        if (splitPoint < adjString.length() && adjString.charAt(splitPoint) != ',') {
            adjString = adjString.substring(0, splitPoint) + ',' + adjString.substring(splitPoint);
        }
        return adjString;
    }

    private void parseAdjustmentBlock(Layer layer, String adjString) {
        // split into tokens for icon size, font size, dx, dy
        String[] tokens = injectPhantomComma(adjString).split(",", -1);

        if (tokens.length > 4) {
            throw new ParseError("extra parameters", adjString.length() - 1);
        }

        // icon size: letter size or number of pixels
        String szString = tokens[0];
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
        int offset = szString.length() + 1;

        // font size and glyph position adjustments
        float[] adj = new float[3];
        for (int i = 0; i < 3 && (i + 1) < tokens.length; ++i) {
            if (!tokens[i + 1].isEmpty()) {
                try {
                    adj[i] = Float.parseFloat(tokens[i + 1]);
                } catch (NumberFormatException nfe) {
                    throw new ParseError("invalid adjustment value \"" + tokens[i + 1] + '"', offset);
                }
            }
            offset += tokens[i].length() + 1;
        }

        layer.fontAdj = adj[0];
        layer.xAdj = adj[1];
        layer.yAdj = adj[2];
    }

    /**
     * Convert a 3- or 4-digit hex value to 6 or 8 digits. Does not check if
     * color is valid, only if it has a relevant length.
     */
    private static String extendShortColor(String shortHex) {
        final int len = shortHex.length();
        if (len == 3 || len == 4) {
            StringBuilder b = new StringBuilder(len * 2);
            for (int i = 0; i < len; ++i) {
                char digit = shortHex.charAt(i);
                b.append(digit).append(digit);
            }
            return b.toString();
        }
        return shortHex;
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
        // accept Web-style # at start of colour, but do not require
        if (colour.startsWith("#")) {
            colour = colour.substring(1);
        }

        boolean mustBeExact = false;
        if (colour.endsWith("!")) {
            colour = colour.substring(0, colour.length() - 1);
            mustBeExact = true;
        }

        colour = extendShortColor(colour);

        if (colour.length() == 6 || colour.length() == 8) {
            try {
                Color c = new Color((int) Long.parseLong(colour, 16), colour.length() == 8);
                if (!mustBeExact) {
                    Theme th = ThemeInstaller.getInstalledTheme();
                    if (th != null) {
                        c = th.applyThemeToColor(c);
                    }
                }
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

        if (colour.length() > 1) {
            throw new ParseError("invalid colour \"" + colour + '"', offset);
        }

        // the default bg is null, and if this is a stacked glyph
        // we will ignore the background if one was set previously
        if (isBg && (previousLayerHasBackground || colour.isEmpty())) {
            return;
        }

        // the default fg is white if there is a background,
        // otherwise the default foreground colour
        if (colour.isEmpty()) {
            if (layer.bg != null || previousLayerHasBackground) {
                colour = "1";
            } else {
                colour = "x";
            }
        }

        char colourCode = colour.charAt(0);
        boolean reverseLightDark = Character.isUpperCase(colourCode);

        // For backgrounds, the variant (light/dark) is normally opposite the
        // theme: dark for light themes, light for dark themes.
        //
        // For foregrounds, the variant is normally opposite the theme IF
        // there is no background. When there is a background, the variant is
        // normally the same as the theme to contrast with the background
        try {
            if (isBg) {
                boolean darkIfDarkMode = reverseLightDark;
                if (darkIfDarkMode) {
                    layer.bg = Palette.get.light.opaque.fromCode(colourCode);
                    layer.bgDarkMode = Palette.get.dark.opaque.fromCode(colourCode);
                } else {
                    layer.bg = Palette.get.dark.opaque.fromCode(colourCode);
                    layer.bgDarkMode = Palette.get.light.opaque.fromCode(colourCode);
                }
            } else {
                boolean darkIfDarkMode = reverseLightDark;
                if (layer.bg != null || previousLayerHasBackground) {
                    darkIfDarkMode = !darkIfDarkMode;
                }
                if (darkIfDarkMode) {
                    layer.fg = Palette.get.light.translucent.fromCode(colourCode);
                    layer.fgDarkMode = Palette.get.dark.translucent.fromCode(colourCode);
                } else {
                    layer.fg = Palette.get.dark.translucent.fromCode(colourCode);
                    layer.fgDarkMode = Palette.get.light.translucent.fromCode(colourCode);
                }
            }
        } catch (IllegalArgumentException iae) {
            throw new ParseError("unknown colour code \"" + colour + '"', offset);
        }
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
                private int last, lastHandled;

                @Override
                public void changedUpdate(DocumentEvent e) {
                    ++last;
                    SwingUtilities.invokeLater(() -> {
                        if (lastHandled != last) {
                            lastHandled = last;
                            try {
                                String desc = tf.getText().trim();
                                System.out.println(desc);
                                if (!desc.isEmpty()) {
                                    label.setIcon(new ThemedGlyphIcon(desc));
                                }
                            } catch (Exception ex) {
                                StrangeEons.log.log(Level.SEVERE, "uncaught exception during parse", ex);
                            }
                        }
                    });
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
