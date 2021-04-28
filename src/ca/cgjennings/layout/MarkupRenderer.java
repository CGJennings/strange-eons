package ca.cgjennings.layout;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.font.TransformAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import static java.lang.Math.max;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

//
// 🐲 HERE THERE BE DRAGONS 🐲
//
// This is probably the oldest class in Strange Eons that still
// exists in more-or-less its original form. It is one of the first
// things I wrote for this project and it is frightening to behold.
//
// Would you like to make Strange Eons better?
// Write a test harness for this thing so it can be tinkered with and
// ultimately replaced without breaking compatibility.
//

/**
 * This class lays out paragraphs that use a specialized HTML-like markup
 * language. The markup does not require any special introductory tags, such as
 * &lt;markup&gt; or &lt;body&gt;. Tags which are not understood are ignored.
 * Unlike the usual rules for HTML, sequences of more than one whitespace
 * character are significant and retained.
 * <p>
 * Users may add arbitrary new tags to the system, but the following set of tags
 * are understood by default (as well as their closing tag when appropriate):
 * <p>
 * Renderers support six kinds of tags: built-in, replacement, user-defined,
 * non-parametric style, parametric style, and interpreted tags.
 * <p>
 * Built-in tags perform special functions within the renderer environment and
 * are always available.
 * <p>
 * Replacement tags are defined by the (programmer) user. They allow tags to be
 * replaced by arbitrary text strings.
 * <p>
 * User-defined tags are essentially the same as replacement tags, but they are
 * defined by the end user using the &lt;define&gt; built-in tag.
 * <p>
 * Non-parametric style tags are tags that do not take parameters and apply a
 * {@link TextStyle} to the text they enclose. Examples include the &lt;b&gt;
 * (bold) and &lt;i&gt; (italic) tags. <code>TextStyle</code>s consist of one or
 * more <code>TextAttribute</code>s and their values; a single non-parametric
 * tag can therefore have multiple effects.
 * <p>
 * Parametric style tags are tags that can include parameters which are parsed
 * by a {@link ParametricStyleFactory}. The factory will return a
 * <code>TextStyle</code> based on the tag's parameters, which is then applied
 * to the block it encloses. Examples include the &lt;color&gt;/&lt;colour&gt;
 * and &lt;image&gt; tags.
 * <p>
 * Interpreted tags include all tags not recognized as any of the other types.
 * These tags are passed to {@link #handleUnknownTag}, which is given the
 * opportunity to do something with it. Generally, if it wishes to handle the
 * tag it will return a replacement string. The <code>MarkupRenderer</code> base
 * class uses this to replace Unicode character tags with their equivalent
 * Unicode characters. The <code>GenderAwareMarkupRenderer</code> subclass uses
 * this to parse tags that include masculine and feminine variants that are
 * selected based upon a gender supplied to the renderer (as in
 * &lt;his/her&gt;).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class MarkupRenderer {

    /**
     * The marked-up text to be printed.
     */
    private String markup;
    /**
     * Attributed strings for each paragraph of the source string.
     */
    private StyledParagraph[] paragraphs;
    /**
     * Tracks the location of tabs in each paragraph of the source string.
     */
    private TabManager tabs = new TabManager();
    private boolean markBadBoxes = false;
    private float tightness = 1f;
    private float maxTightness = 1f;
    private float minTightness = 0.5f;
    private double minScalingFactor = 0.5;
    private int baseJustify = LAYOUT_LEFT | LAYOUT_TOP;
    private int headingJustify = LAYOUT_CENTER;
    private int textFitting = FIT_NONE;
    private boolean defaultAutoclose = false;
    /**
     * The default style for text which has no other markup applied.
     */
    private TextStyle defaultStyle;
    /**
     * The resolution to be used to determine physical sizes when drawing.
     */
    private double dpi;
    /**
     * This is a map from tag names (e.g. "b") to styles (e.g.
     * TextStyle.BOLD_STYLE).
     */
    private HashMap<String, TextStyle> styleMap;
    /**
     * This is a map from tag names to {@link ParametricStyleFactory} instances.
     */
    private HashMap<String, ParametricStyleFactory> parametricStyleMap;
    /**
     * Tracks replacements associated with tags.
     */
    private HashMap<String, String> replacements;
    /**
     * Tracks user-created definitions made within the markup.
     */
    private HashMap<String, String> definitions = new HashMap<>();
    /**
     * Do not try to force text that is too long to fit in the draw rectangle.
     */
    public static final int FIT_NONE = 0;
    /**
     * Try to fit text that is too long for the draw rectangle by reducing
     * interline spacing.
     */
    public static final int FIT_TIGHTEN_LINE_SPACING = 1;
    /**
     * Try to fit text that is too long for the draw rectangle by scaling down
     * the text. Note that this can be time consuming, and that it will augment
     * your specified styles with <code>TextTransform</code> style
     * <code>TextAttribute</code>s.
     */
    public static final int FIT_SCALE_TEXT = 2;
    /**
     * Try to fit text that is too long for the draw rectangle by first reducing
     * spacing, then scaling down the text if that is not enough.
     */
    public static final int FIT_BOTH = 3;
    /**
     * Lines are left-justified.
     */
    public final static int LAYOUT_LEFT = 1;
    /**
     * Lines are centered.
     */
    public final static int LAYOUT_CENTER = 2;
    /**
     * Lines are right-justified.
     */
    public final static int LAYOUT_RIGHT = 4;
    /**
     * Paragraphs are aligned to the top of the rectangle.
     */
    public final static int LAYOUT_TOP = 8;
    /**
     * Paragraphs are centered in the middle of the rectangle.
     */
    public final static int LAYOUT_MIDDLE = 16;
    /**
     * Paragraphs are aligned to the bottom of the rectangle.
     */
    public final static int LAYOUT_BOTTOM = 32;
    /**
     * Paragraphs should be justified.
     */
    public final static int LAYOUT_JUSTIFY = 64;

    /**
     * Shape to apply to left and right edges.
     */
    private PageShape pageShape = PageShape.RECTANGLE_SHAPE;

    public void setPageShape(PageShape shape) {
        if (shape == null) {
            shape = PageShape.RECTANGLE_SHAPE;
        }
        if (!this.pageShape.equals(shape)) {
            this.pageShape = shape;
            lastLaidOutRectangle = null;
        }
    }

    public PageShape getPageShape() {
        return pageShape;
    }

    /**
     * The internal instance of {@link GraphicStyleFactory} used to create
     * inline images.
     */
    private GraphicStyleFactory graphicStyle;
    private int scalingFractionalLimit = 1_000;
    /**
     * The factory used to handle script tags.
     */
    private static EvaluatorFactory evaluatorFactory;
    /**
     * The evaluator that will be used for this renderer.
     */
    private Evaluator evaluator;

    /**
     * Set the factory used to handle script tags.
     *
     * @param factory
     */
    public static void setEvaluatorFactory(EvaluatorFactory factory) {
        evaluatorFactory = factory;
    }

    /**
     * Get the factory used to handle script tags.
     *
     * @return the script evaluator factory
     */
    public static EvaluatorFactory getEvaluatorFactory() {
        return evaluatorFactory;
    }
    private int bufferLimit;
    private static int DEFAULT_BUFFER_LIMIT = 75_000;

    public static void setDefaultExpansionLimit(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be >= 1: " + limit);
        }
        DEFAULT_BUFFER_LIMIT = limit;
    }

    public static int getDefaultExpansionLimit() {
        return DEFAULT_BUFFER_LIMIT;
    }

    public int getExpansionLimit() {
        return bufferLimit;
    }

    public void setExpansionLimit(int limit) {
        this.bufferLimit = limit;
    }

    /**
     * Create a new markup layout engine with a set of default styles. A
     * resolution of 72 dots per inch (typical for screen display) is used.
     */
    public MarkupRenderer() {
        this(72d);
    }

    /**
     * Create a new markup layout engine with a set of default styles. The dpi
     * value allows scaling the output for a given resolution. This is important
     * so that, for example, font point sizes are interpreted correctly.
     */
    public MarkupRenderer(double dpi) {
        this.dpi = dpi;
        bufferLimit = DEFAULT_BUFFER_LIMIT;
        markup = "";
        defaultStyle = new TextStyle();
        defaultStyle.add(TextAttribute.FAMILY, "Serif", TextAttribute.SIZE, 9f);
        defaultStyle.add(TextStyle.KERNING_ON);
        defaultStyle.add(TextStyle.LIGATURES_ON);
        defaultStyle.add(TextStyle.JUSTIFY);

        styleMap = new HashMap<>();
        parametricStyleMap = new HashMap<>();
        replacements = new HashMap<>();

        graphicStyle = new GraphicStyleFactory(dpi);

        createDefaultStyleMap();
        createDefaultParametricStyleMap();
        createDefaultReplacementMap();
    }

    /**
     * Add default non-parametric style tags.
     */
    protected void createDefaultStyleMap() {
        setStyleForTag("h1", new TextStyle(TextAttribute.FAMILY, "SansSerif", TextAttribute.SIZE, 14f));
        setStyleForTag("b", TextStyle.BOLD_STYLE);
        setStyleForTag("i", TextStyle.ITALIC_STYLE);
        setStyleForTag("sup", TextStyle.SUPERSCRIPT_STYLE);
        setStyleForTag("sub", TextStyle.SUBSCRIPT_STYLE);
        setStyleForTag("u", TextStyle.UNDERLINE_STYLE);
        setStyleForTag("del", TextStyle.STRIKETHROUGH_STYLE);
        setStyleForTag("no kerning", TextStyle.KERNING_OFF);
        setStyleForTag("no ligatures", TextStyle.LIGATURES_OFF);
        setStyleForTag("tt", TextStyle.TYPEWRITER_STYLE);
        setStyleForTag("n", TextStyle.PLAIN_STYLE);

        setStyleForTag("black", TextStyle.COLOR_BLACK);
        setStyleForTag("blue", TextStyle.COLOR_BLUE);
        setStyleForTag("brown", TextStyle.COLOR_BROWN);
        setStyleForTag("dark grey", TextStyle.COLOR_DKGREY);
        setStyleForTag("dark gray", TextStyle.COLOR_DKGREY);
        setStyleForTag("green", TextStyle.COLOR_GREEN);
        setStyleForTag("grey", TextStyle.COLOR_GREY);
        setStyleForTag("gray", TextStyle.COLOR_GREY);
        setStyleForTag("light grey", TextStyle.COLOR_LTGREY);
        setStyleForTag("light gray", TextStyle.COLOR_LTGREY);
        setStyleForTag("orange", TextStyle.COLOR_ORANGE);
        setStyleForTag("purple", TextStyle.COLOR_PURPLE);
        setStyleForTag("red", TextStyle.COLOR_RED);
        setStyleForTag("white", TextStyle.COLOR_WHITE);
        setStyleForTag("yellow", TextStyle.COLOR_YELLOW);
    }

    /**
     * Add default parametric style tags.
     */
    protected void createDefaultParametricStyleMap() {
        ForegroundColorStyleFactory fg = ForegroundColorStyleFactory.getShared();
        BackgroundColorStyleFactory bg = BackgroundColorStyleFactory.getShared();
        setParametricStyleForTag("colour", fg);
        setParametricStyleForTag("bgcolour", bg);
        setParametricStyleForTag("color", fg);
        setParametricStyleForTag("bgcolor", bg);
        setParametricStyleForTag("size", FontSizeStyleFactory.getShared());
        setParametricStyleForTag("family", FontFamilyStyleFactory.getShared());
        setParametricStyleForTag("tracking", TrackingStyleFactory.getShared());
        setParametricStyleForTag("width", WidthStyleFactory.getShared());
        setParametricStyleForTag("weight", WeightStyleFactory.getShared());
    }

    /**
     * Add default replacement tags.
     */
    protected void createDefaultReplacementMap() {
        String[] replacements = new String[]{
            "infinity", "\u221e",
            "lq", "\u201c",
            "rq", "\u201d",
            "\"", "\u201c",
            "/\"", "\u201d",
            "lsq", "\u2018",
            "rsq", "\u2019",
            "'", "\u2018",
            "/'", "\u2019",
            "endash", "\u2013",
            "emdash", "\u2014",
            "--", "\u2013",
            "---", "\u2014",
            "...", "\u2026",
            "lg", "\u00ab",
            "rg", "\u00bb",
            "lsg", "\u2039",
            "rsg", "\u203a",
            "nbsp", "\u00a0",
            " ", "\u00a0",
            "thsp", "\u2009",
            "emsp", "\u2003",
            "ensp", "\u2002",
            "hsp", "\u200a",};
        for (int i = 0; i < replacements.length; i += 2) {
            setReplacementForTag(replacements[i], replacements[i + 1]);
        }
    }

    /**
     * Clear cached information about how markup should be processed. When a
     * given markup string is first drawn or measured, some information will be
     * cached to speed up subsequent redrawing.
     * <p>
     * The caching process will include a final version of the text with all of
     * the markup processed out. As a result, subclasses implement their own
     * tags via {@link #handleUnknownTag} and that will return different results
     * depending on some system state should call this method whenever the state
     * changes. For example, the {@link GenderAwareMarkupRenderer} calls this
     * method whenever its gender setting is changed. Otherwise, repeated
     * renderings with the same markup would always draw text that reflected the
     * active gender at the time the markup was first drawn or measured.
     * <p>
     * This method must never be called from {@link #handleUnknownTag}, as this
     * is called while the markup is being processed (that is, while the cache
     * is being constructed).
     */
    protected void invalidateLayoutCache() {
        paragraphs = null;
    }

    /**
     * Set the style associated with a non-parametric tag. If a style was set
     * previously for this tag, it will be replaced by the new style. Do not
     * include the angle brackets in the tag name. If a replacement was
     * associated with this tag, the replacement will be removed.
     */
    public void setStyleForTag(String tagName, TextStyle style) {
        removeTag(tagName);
        styleMap.put(tagName, style);
    }

    /**
     * Return the {@link TextStyle} associated with the tag, or {@code null} if
     * the tag does not have a non-parametric style set (including if it has a
     * replacement or parametric style set).
     */
    public TextStyle getStyleForTag(String tagName) {
        return styleMap.get(tagName);
    }

    /**
     * Set a parameter-based style for the given tag. When this tag occurs in
     * the markup text, the factory will be used to create the actual style
     * based on the tag's parameters.
     */
    public void setParametricStyleForTag(String tagName, ParametricStyleFactory factory) {
        removeTag(tagName);
        parametricStyleMap.put(tagName, factory);
    }

    /**
     * Return the {@link ParametricStyleFactory} associated with the tag, or
     * {@code null} if the tag does not have a style set (including if it has a
     * replacement or style set).
     */
    public ParametricStyleFactory getParametricStyleForTag(String tagName) {
        return parametricStyleMap.get(tagName);
    }

    /**
     * Set a replacement string to be associated with a particular tag. When
     * this tag occurs in the markup text, it will be replaced by the specified
     * repalcement.
     */
    public void setReplacementForTag(String tagName, String replacement) {
        removeTag(tagName);
        replacements.put(tagName, replacement);
    }

    /**
     * Return the replacement {@link String} associated with the tag, or
     * {@code null} if the tag does not have a replacement set (including if it
     * has a style or parametric style set).
     */
    public String getReplacementForTag(String tagName) {
        return replacements.get(tagName);
    }

    /**
     * If a style or replacement is associated with this tag, the style will be
     * removed and the tag will be treated as an unknown tag.
     */
    public void removeTag(String tagName) {
        if (styleMap.containsKey(tagName)) {
            styleMap.remove(tagName);
        } else if (parametricStyleMap.containsKey(tagName)) {
            parametricStyleMap.remove(tagName);
        } else if (replacements.containsKey(tagName)) {
            replacements.remove(tagName);
        }
        paragraphs = null;
    }

    /**
     * Removes the style information associated with all tags. Break and heading
     * tags will still cause line breaks.
     */
    public void removeAllTags() {
        styleMap.clear();
        parametricStyleMap.clear();
        replacements.clear();
        paragraphs = null;
    }

    /**
     * Set the default style applied to text before any tags are applied. It is
     * permissible to modify this style even if it was not created by the
     * caller.
     */
    public void setDefaultStyle(TextStyle style) {
        lastLaidOutRectangle = null;
        defaultStyle = style;
    }

    /**
     * Return the current default style applied to text before any tags are
     * applied.
     */
    public TextStyle getDefaultStyle() {
        return defaultStyle;
    }

    /**
     * Set whether tags close themselves at the end of each line by default.
     * This is a default that can be changed within a markup block using
     * &lt;autoclose&gt; or &lt;manualclose&gt;.
     */
    public void setAutoclose(boolean autoclose) {
        defaultAutoclose = autoclose;
    }

    /**
     * Return whether outstanding tags are automatically closed at the end of
     * each line by default.
     */
    public boolean getAutoclose() {
        return defaultAutoclose;
    }

    /**
     * Set the markup string to be rendered.
     */
    public void setMarkupText(String markup) {
        if (this.markup == null || !this.markup.equals(markup)) {
            this.markup = markup;
            // force this to be recreated when the string is rendered
            paragraphs = null;
        }
    }

    /**
     * Return the current markup string to be rendered by this instance.
     */
    public String getMarkupText() {
        return markup;
    }

    /**
     * Set whether text that exceeds its bounding rectangle is marked with a red
     * box.
     */
    public void setMarkBadBox(boolean mark) {
        markBadBoxes = mark;
    }

    /**
     * Get whether text that exceeds its bounding rectangle is marked with a red
     * box.
     */
    public boolean getMarkBadBox() {
        return markBadBoxes;
    }

    /**
     * Set the fitting methods that will be used to shrink text that is too long
     * to fit in its rendering rectangle. One of FIT_NONE,
     * FIT_TIGHTEN_LINE_SPACING, FIT_SCALE_TEXT, or FIT_BOTH.
     */
    public void setTextFitting(int fittingStyle) {
        lastLaidOutRectangle = null;
        textFitting = fittingStyle;
    }

    /**
     * Get the current text fitting method.
     */
    public int getTextFitting() {
        return textFitting;
    }

    /**
     * Limit the text scaling algorithm to scale down to no more than
     * <code>factor</code> * 100% of the original size.
     */
    public void setScalingLimit(double factor) {
        if (factor <= 0 || factor > 1) {
            throw new IllegalArgumentException("invalid scale: " + factor);
        }
        lastLaidOutRectangle = null;
        minScalingFactor = factor;
    }

    /**
     * Set the fractional increment at which text is scaled; the precision of
     * text scaling is limited to 1/<code>limit</code>.
     */
    public void setScalingFractionalLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("invalid precision denominator: " + limit);
        }
        lastLaidOutRectangle = null;
        scalingFractionalLimit = limit;
    }

    public int getScalingFractionalLimit() {
        return scalingFractionalLimit;
    }

    /**
     * Get the current limit on text scaling to fit text in a rendering
     * rectangle.
     */
    public double getScalingLimit() {
        return minScalingFactor;
    }

    /**
     * Set the minimum tightness to use when using line tightening to reduce
     * text size.
     */
    public void setTightnessLimit(float tightnessLimit) {
        if (tightnessLimit == minTightness) {
            return;
        }
        lastLaidOutRectangle = null;
        minTightness = tightnessLimit;
        if (minTightness > maxTightness) {
            maxTightness = tightnessLimit;
        }
    }

    /**
     * Get the current minimum tighness to use when using line tightening to
     * reduce text size.
     */
    public float getTightnessLimit() {
        return minTightness;
    }

    /**
     * Set the maximum line spacing value. Normal line spacing is 1.0; double
     * spaced would be 2.0. For tighter lines, use a value less than 1.0. This
     * is a maximum value; if the text fitting method allows line tightening,
     * the actual line spacing may be less. For minimal space between lines, use
     * a tightnes of 0.
     */
    public void setLineTightness(float tightness) {
        if (tightness == maxTightness) {
            return;
        }
        lastLaidOutRectangle = null;
        maxTightness = tightness;
        if (tightness < minTightness) {
            minTightness = tightness;
        }
    }

    /**
     * Get the current maximum line tightness.
     */
    public float getLineTightness() {
        return maxTightness;
    }

    /**
     * Sets the default horizontal and vertical alignment of text within the
     * draw rectangle. This is a binary or of one of
     * {@link #LAYOUT_LEFT}, {@link #LAYOUT_RIGHT}, and {@link #LAYOUT_CENTER}
     * with one of {@link #LAYOUT_TOP}, {@link #LAYOUT_BOTTOM}, or
     * {@link #LAYOUT_MIDDLE}, and optionally with {@link #LAYOUT_JUSTIFY}.
     *
     * @param alignment the new alignment value
     */
    public void setAlignment(int alignment) {
        alignment = normalizeAlignment(alignment, false);
        if (baseJustify != alignment) {
            lastLaidOutRectangle = null;
            baseJustify = alignment;
        }
    }

    private static int normalizeAlignment(int align, boolean horizOnly) {
        int halign = align & (LAYOUT_LEFT | LAYOUT_CENTER | LAYOUT_RIGHT);
        if (halign == 0) {
            halign = LAYOUT_LEFT;
        } else {
            halign = Integer.lowestOneBit(halign);
        }
        if (horizOnly) {
            return halign;
        }

        int valign = align & (LAYOUT_TOP | LAYOUT_MIDDLE | LAYOUT_BOTTOM);
        int jalign = align & LAYOUT_JUSTIFY;
        if (valign == 0) {
            valign = LAYOUT_TOP;
        } else {
            valign = Integer.lowestOneBit(valign);
        }
        return halign | valign | jalign;
    }

    /**
     * Returns the default alignment of text within the draw rectangle; the
     * alignment can be changed using markup.
     *
     * @return the base alignment value
     */
    public int getAlignment() {
        return baseJustify;
    }

    /**
     * Set the horizontal alignment of heading text tags (h1, h2, etc.).
     */
    public void setHeadlineAlignment(int alignment) {
        alignment = normalizeAlignment(alignment, true);
        if (headingJustify != alignment) {
            lastLaidOutRectangle = null;
            headingJustify = alignment;
        }
    }

    /**
     * Returns the default alignment of headline text within the draw rectangle.
     *
     * @return the base alignment value
     */
    public int getHeadlineAlignment() {
        return headingJustify;
    }

    /**
     * Return the distance, in inches, between tab stops.
     */
    public double getTabWidth() {
        return tabWidths[0];
    }

    public double[] getTabWidths() {
        return tabWidths.clone();
    }

    /**
     * Set the distance, in inches, between tab stops.
     */
    public void setTabWidth(double gapInInches) {
        if (tabWidths.length == 1) {
            if (tabWidths[0] != gapInInches) {
                tabWidths[0] = gapInInches;
                lastLaidOutRectangle = null;
            }
        } else {
            tabWidths = new double[]{gapInInches};
            lastLaidOutRectangle = null;
        }
    }

    public void setTabWidths(double[] tabStops) {
        if (tabStops == null) {
            throw new NullPointerException("tabStops");
        }
        if (tabStops.length == 0) {
            throw new IllegalArgumentException("tabStops.length must be > 0");
        }

        if (!Arrays.equals(tabWidths, tabStops)) {
            tabWidths = tabStops.clone();
            lastLaidOutRectangle = null;
        }
    }

    /**
     * The current distance between tab stops.
     */
    private double[] tabWidths = new double[]{1d / 2.54d};

    /**
     * Lay out the current markup text using the current styles and settings
     * within the specified rectangle. Returns the y-coordinate where the next
     * line would start.
     */
    public double draw(Graphics2D g, Rectangle2D r) {
        if (DEBUG) {
            drawBadBox(g, r.getX(), r.getY(), r.getWidth(), r.getHeight(), true);
        }
        createStyledText();
        if (!textHasWhitespace && (getTextFitting() & FIT_SCALE_TEXT) != 0) {
            drawAsSingleLine(g, r);
            return lastLaidOutYOffset;
        }
        return layout(g, r, true);
    }

    public double drawAsSingleLine(Graphics2D g, Rectangle2D r) {
        if (DEBUG) {
            drawBadBox(g, r.getX(), r.getY(), r.getWidth(), r.getHeight(), true);
        }

        lastLaidOutFontRenderContext = g.getFontRenderContext();
        createStyledText();
        scaleText(1d);
        tightness = 0f;
        double textHeight = renderText(g, r, 0d, lastLaidOutFontRenderContext, true, true);

        if (lastLineTextWidth > r.getWidth()) {
            scaleText(r.getWidth() / lastLineTextWidth);
            textHeight = renderText(g, r, 0d, lastLaidOutFontRenderContext, true, true);
        }

        double yOffset = 0f;
        if ((currentVerticalJustification & LAYOUT_MIDDLE) != 0) {
            yOffset = (r.getHeight() - textHeight) / 2;
        } else if ((currentVerticalJustification & LAYOUT_BOTTOM) != 0) {
            yOffset = r.getHeight() - textHeight;
        }
        if (yOffset < 0) {
            yOffset = 0;
        }

        boolean oldMarkBad = markBadBoxes;
        markBadBoxes = false;
        renderText(g, r, yOffset, lastLaidOutFontRenderContext, false, true);
        markBadBoxes = oldMarkBad;

        return lastLineTextWidth;
    }

    /**
     * Simulate rendering the current markup without drawing anything, returning
     * the height that the text would have covered.
     *
     * @return the layout height
     */
    public double measure(Graphics2D g, Rectangle2D r) {
        return layout(g, r, false);
    }

    /**
     * Instead of drawing the text, appends a sequence of {@link LayoutSegment}s
     * list; these segments can then be used by the caller to evaluate,
     * customize and/or draw the layout. For example, by creating a segment list
     * for an infinitely high rectangle, the resulting list of segments could be
     * laid out on a sequence of pages for printing by adjusting their
     * y-coordinates.
     */
    public void appendSegments(Graphics2D g, Rectangle2D r, List<LayoutSegment> appendList) {
        segmentAppendList = appendList;
        try {
            layout(g, r, false);
        } finally {
            segmentAppendList = null;
        }
    }
    private List<LayoutSegment> segmentAppendList = null;
    /**
     * This is set to the most recently laid rectangle. If the rectangle and
     * markup do not change, we can avoid recomputing the scale. This is reset
     * to null if the layout changed or the scaling/space shrinking options are
     * changed.
     */
    private Rectangle2D lastLaidOutRectangle = null;
    private double lastLaidOutYOffset;
    private FontRenderContext lastLaidOutFontRenderContext = null;
    private FontRenderContext scaledFontRenderContext;

    /**
     * Lay out the current markup text using the current styles and settings
     * within the specified rectangle. Returns the y-coordinate where the next
     * line would start. If render is false, does not draw the text and returns
     * the height of the text render area (one line less than the usual return
     * value).
     */
    protected double layout(Graphics2D g, Rectangle2D r, boolean render) {
        // parse the markup into styled paragraphs (if it has changed)
        createStyledText();
        FontRenderContext frc = g.getFontRenderContext();

        if (lastLaidOutRectangle == null || !r.equals(lastLaidOutRectangle) || lastLaidOutFontRenderContext == null || !lastLaidOutFontRenderContext.equals(frc)) {
            lastLaidOutRectangle = r;
            lastLaidOutFontRenderContext = frc;

            // TODO: need to reset the scale if the rectangle changes and we
            //       are asked to render the same markup, or if the markup
            //       changes
            tightness = maxTightness;
            scaleText(1d);

            double textHeight = renderText(g, r, 0f, scaledFontRenderContext, true, false);
            double rectHeight = r.getHeight();

            if ((textFitting & FIT_TIGHTEN_LINE_SPACING) != 0 && (textHeight > rectHeight)) {
                // First try to make the text fit by tightening the line spacing
                tightness = minTightness;
                textHeight = renderText(g, r, 0f, scaledFontRenderContext, true, false);

                // That will work, now look for a compromise.
                // Could use a binary search here, but it is not that many iterations.
                if (textHeight <= rectHeight) {
                    float newTightness = minTightness;
                    for (float t = 0.20f; t < 1f; t += 0.20f) {
                        tightness = minTightness + (maxTightness - minTightness) * t;
                        if (renderText(g, r, 0f, scaledFontRenderContext, true, false) <= rectHeight) {
                            newTightness = tightness;
                        } else {
                            break;
                        }
                    }
                    tightness = newTightness;
                }
            }

            if ((textFitting & FIT_SCALE_TEXT) != 0 && (textHeight > rectHeight)) {
                final int FRACTION = scalingFractionalLimit;
                // we do a binary search; we use integer search indices that
                // represent a scale factor of index/FRACTION; the search will find
                // the smallest index that does NOT fit in the rectangle
                int high = FRACTION;

                int minScale = (int) (minScalingFactor * FRACTION);
                int low = minScale - 1;

                while (low + 1 < high) {
                    int middle = (low + high) / 2;

                    // measure text at this scale
                    final double scale = middle / (double) FRACTION;
                    if (scale > 0d) {
                        scaleText(scale);
                        textHeight = renderText(g, r, 0f, scaledFontRenderContext, true, false);

                        if (textHeight <= rectHeight) {
                            low = middle;
                        } else {
                            high = middle;
                        }
                    } else {
                        log.log(Level.WARNING, "binary search reached invalid scale: {0}", scale);
                        low = middle;
                    }
                }

                int match = high - 1;
                if (match >= FRACTION) {
                    match = FRACTION - 1;
                }
                if (match < minScale) {
                    match = minScale;
                }
                if (match < 1) {
                    match = 1;
                }

                double scale = match / (double) FRACTION;
                scaleText(scale);
                textHeight = renderText(g, r, 0f, scaledFontRenderContext, true, false);
            }

            if (render == false) {
                return textHeight;
            }

            // position pen for rendering according to the vertical draw setting
            double yOffset = 0f;
            if ((currentVerticalJustification & LAYOUT_MIDDLE) != 0) {
                yOffset = (rectHeight - textHeight) / 2;
            } else if ((currentVerticalJustification & LAYOUT_BOTTOM) != 0) {
                yOffset = rectHeight - textHeight;
            }
            if (yOffset < 0) {
                yOffset = 0;
            }
            lastLaidOutYOffset = yOffset;
        }

        return renderText(g, r, lastLaidOutYOffset, scaledFontRenderContext, false, false);
    }

    /**
     * Scale the prepared markup by a constant factor.
     */
    private void scaleText(double scaleFactor) {
        if (scaleFactor == 0d) {
            throw new IllegalArgumentException();
        }
        double normalizedScaleFactor = scaleFactor * dpi / 72d;
        AffineTransform at = AffineTransform.getScaleInstance(normalizedScaleFactor, normalizedScaleFactor);
        TransformAttribute a = new TransformAttribute(at);
        for (int i = 0; i < paragraphs.length; ++i) {
            paragraphs[i].addAttribute(TextAttribute.TRANSFORM, a);
        }

        at = AffineTransform.getScaleInstance(1d / normalizedScaleFactor, 1d / normalizedScaleFactor);
        at.preConcatenate(lastLaidOutFontRenderContext.getTransform());
        scaledFontRenderContext = new FontRenderContext(
                at,
                lastLaidOutFontRenderContext.getAntiAliasingHint(),
                lastLaidOutFontRenderContext.getFractionalMetricsHint()
        );
    }

    private double[] createTabStopArray(double x, double width) {
        double[] tabStops;
        if (tabWidths.length == 1) {
            double tabSize = tabWidths[0] * dpi;
            int tabCount = (int) Math.ceil(width / tabSize) + 1;
            if (tabCount < 0) {
                tabCount = 0;
            }
            tabStops = new double[tabCount];
            double tabOffset = x;
            for (int t = 0; t < tabCount; ++t) {
                tabStops[t] = tabOffset;
                tabOffset += tabSize;
            }
        } else {
            final double x2 = x + width;
            int index = 0;
            int tabCount = 0;
            double pos = x;
            while (pos <= x2) {
                pos += tabWidths[index++] * dpi;
                ++tabCount;
                if (index == tabWidths.length) {
                    index = 0;
                }
            }
            ++tabCount;
            tabStops = new double[tabCount];
            pos = x;
            index = 0;
            for (int i = 0; i < tabCount; ++i) {
                pos += tabWidths[index++] * dpi;
                tabStops[i] = pos;
                if (index == tabWidths.length) {
                    index = 0;
                }
            }
        }
        return tabStops;
    }

    /**
     * Render the markup on a graphics context within a specified region. A
     * y-coordinate indicating where a subsequent line would begin is returned.
     * If <code>measureOnly</code> is set to true, a draw is computed but no
     * actual rendering performed. In this case, the return value is the height
     * of the region needed to render the text.
     */
    @SuppressWarnings("empty-statement")
    protected double renderText(Graphics2D g, Rectangle2D r, double yAlignmentOffset, FontRenderContext frc, boolean measureOnly, boolean restrictToSingleLine) {
        double rectWidth = r.getWidth();
        double leftMargin = r.getX();
        double rightMargin = r.getX() + rectWidth;

        PageShape shape = restrictToSingleLine ? PageShape.RECTANGLE_SHAPE : pageShape;

        double xPosition = r.getX();
        double yPosition = r.getY();

        double lastLineBottom = yPosition;

        // TODO: convert all processing to double or float precision
        // We could compute a tab position, but instead we will create this array
        // of tab stops. This will make it easy if we want to allow definable
        // tab stops later on.
        double[] tabStops = null;
        if (!restrictToSingleLine) {
            tabStops = createTabStopArray(leftMargin, rectWidth);
        }

        ArrayList<TextLayout> layouts = new ArrayList<>(1);
        ArrayList<Float> penPositions = new ArrayList<>(1);
        final BreakIterator breakIterator = createBreakIterator();

        for (int line = 0; line < paragraphs.length; ++line) {
            int tabCurrent = 0;
            int[] tabLocations = tabs.getTabList(line);
            boolean isLeftToRight = false;

            StyledParagraph paragraph = paragraphs[line];
            AttributedCharacterIterator styledText = paragraph.getIterator();
            LineBreakMeasurer measurer = new LineBreakMeasurer(styledText, breakIterator, frc);

            // determine the initial ascent/descent for the page shaper
            // (not guaranteed correct)
            double wrappingMargin = SINGLE_LINE_STANDIN_WIDTH;
            double maxAscent = 0d, maxDescent = 0d;
            if (shape != PageShape.RECTANGLE_SHAPE) {
                TextLayout tl = measurer.nextLayout((float) rectWidth, tabLocations[0] + 1, false);
                maxAscent = tl.getAscent();
                maxDescent = tl.getDescent();
                // reset measurer to the start of the line
                measurer.setPosition(0);
            }

            while (measurer.getPosition() < styledText.getEndIndex()) {
                // break the paragraph into a sequence of lines and optionally draw each line

                boolean lineContainsText = false;
                boolean lineCompleted = false;
                double maxAdvance = 0f;

                double shapedX1 = pageShape.getShapedX1(leftMargin, yPosition, maxAscent, maxDescent);
                double shapedX2 = pageShape.getShapedX2(rightMargin, yPosition, maxAscent, maxDescent);
                if (!restrictToSingleLine) {
                    wrappingMargin = shapedX2;
                }
                xPosition = shapedX1;

                // BUG FIX:
                // we estimate the upcoming line height from the previous line,
                // then reset it here so that the max values don't accumulate
                // over the whole paragraph
                maxAscent = maxDescent = 0d;

                // find and store the layouts for each segment of the line
                while (!lineCompleted) {
                    double wrappingWidth = wrappingMargin - xPosition;

                    // this can happen if the page shape crosses the left & right margins
                    if (wrappingWidth <= 0) {
                        wrappingWidth = Float.MIN_VALUE;
                    }

                    TextLayout layout = measurer.nextLayout((float) wrappingWidth, tabLocations[tabCurrent] + 1, lineContainsText);

                    if (!lineContainsText) {
                        if (layout != null) {
                            isLeftToRight = layout.isLeftToRight();
                        } else {
                            isLeftToRight = true;
                        }
                    }

                    // reached end of paragraph
                    boolean atEndOfParagraph = false;
                    if (measurer.getPosition() == styledText.getEndIndex()) {
                        lineCompleted = true;
                        atEndOfParagraph = true;
                    }

                    // we are not at a tab, therefore we ran out of space and need to break the line
                    boolean atATab = true;
                    if (measurer.getPosition() != tabLocations[tabCurrent] + 1) {
                        lineCompleted = true;
                        atATab = false;
                    }

                    if (layout != null) {
                        // if we are supposed to justify the text...
                        if ((!measureOnly || segmentAppendList != null) && (currentJustification & LAYOUT_JUSTIFY) != 0) {
                            // and this is not the last line in the paragraph...
                            if (!atEndOfParagraph && !atATab) {
                                // then perform justification on this line
                                layout = layout.getJustifiedLayout((float) (shapedX2 - shapedX1));
                            }
                        }

                        layouts.add(layout);
                        penPositions.add((float) xPosition);

                        // fix: at end of line, add *visible* advance to get
                        // correct line width for centering
                        if (lineCompleted) {
                            xPosition += layout.getVisibleAdvance();
                        } else {
                            xPosition += layout.getAdvance();
                        }

                        maxAscent = max(maxAscent, layout.getAscent());
                        maxDescent = max(maxDescent, layout.getDescent());
                        maxAdvance = max(maxAdvance, layout.getDescent() + layout.getLeading());

                        // some fonts don't have good leading info, which can
                        // lead to lines with no space between or even overlap
                        // we substitute a minimal advance of 1/2pt in such cases
                        if (maxAdvance <= 0.0001d) {
                            maxAdvance = 1d / 144d * dpi;
                        }
                    } else {
                        lineCompleted = true;
                    }
                    lineContainsText = true;

                    if (!lineCompleted) {
                        // update the index into tabLocations to point to the next tab
                        tabCurrent++;
                        // skip xPosition to the next tab stop---if rendering as a
                        // single line, tabStops==null and tabs count as 0-width spaces
                        if (!restrictToSingleLine) {
                            int t;
                            final int stopIndex = tabStops.length - 1;
                            for (t = 0; t < stopIndex && xPosition > tabStops[t]; ++t);
                            xPosition = tabStops[t]; // we always stop before t == tabStops.length
                        }
                    }
                }

                yPosition += maxAscent;
                if (paragraph.isTitleLine()) {
                    yPosition += maxDescent / 2d;
                }

                double textWidth = xPosition - leftMargin;
                double offset = 0;

                int justify = paragraph.isTitleLine() ? headingJustify : paragraph.getAlignment();
                if (!isLeftToRight) {
                    if ((justify & LAYOUT_RIGHT) != 0) {
                        justify = LAYOUT_LEFT;
                    } else if ((justify & LAYOUT_LEFT) != 0) {
                        justify = LAYOUT_RIGHT;
                    }
                }
                if ((justify & LAYOUT_RIGHT) != 0) {
                    offset = (shapedX1 - leftMargin) + (shapedX2 - shapedX1) - textWidth;
                } else if ((justify & LAYOUT_CENTER) != 0) {
                    offset = ((shapedX1 - leftMargin) + (shapedX2 - shapedX1) - textWidth) / 2d;
                }

                // set this field so we can scale the
                // line to fit when doing drawAsSingleLine
                lastLineTextWidth = textWidth;

                if (!measureOnly || segmentAppendList != null) {
                    boolean startOfLine = true;
                    Iterator<Float> position = penPositions.iterator();
                    for (TextLayout layout : layouts) {
                        float xOffset = (float) (offset + position.next());
                        float yOffset = (float) (yAlignmentOffset + yPosition);
                        if (!measureOnly) {
                            layout.draw(g, xOffset, yOffset);
                        }
                        if (segmentAppendList != null) {
                            segmentAppendList.add(
                                    new LayoutSegment(layout, xOffset, (float) yPosition, (float) (yPosition + maxDescent), startOfLine, line)
                            );
                            startOfLine = false;
                        }
                    }
                }

                lastLineBottom = yPosition + maxDescent;

                if (paragraph.getLineTightness() >= 0) {
                    if (paragraph.isTitleLine()) {
                        yPosition += maxAdvance / 2d * (1d + tightness);
                    }

                    // add the leading as modified by the line spacing adjustment for text fitting
                    yPosition += maxAdvance * tightness;

                    // for loose lines, add an extra full advance
                    if (paragraph.getLineTightness() >= 1) {
                        yPosition += maxAdvance;
                    }
                }

                layouts.clear();
                penPositions.clear();
            }
        }

        if (measureOnly) {
            return lastLineBottom - r.getY();
        }

        if (markBadBoxes && (lastLineBottom - r.getY() > r.getHeight())) {
            drawBadBox(g, leftMargin, (float) r.getY(), rectWidth, lastLineBottom - (float) r.getY(), false);
        }

        return yAlignmentOffset + yPosition;
    }
    /**
     * After a call to renderText, this will be set to the x-position of the end
     * of the last line that was drawn.
     */
    private double lastLineTextWidth;
    private static final double SINGLE_LINE_STANDIN_WIDTH = Double.MAX_VALUE;

    /**
     * Draw a box highlighting text that has exceeded its margins; if debugBox
     * is true, draw a thinner box used to show where the requested rectangle
     * is.
     */
    private void drawBadBox(Graphics2D g, double x, double y, double width, double height, boolean debugBox) {
        Paint oldPaint = g.getPaint();
        Stroke oldStroke = g.getStroke();

        float point = (float) (dpi / 72d);
        if (debugBox) {
            g.setStroke(new BasicStroke(point / 2f));
            g.setPaint(Color.MAGENTA);
        } else {
            g.setStroke(new BasicStroke(point * 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{point * 8f, point * 8f}, point * 4f));
            g.setPaint(Color.RED);
        }
        g.draw(new Rectangle2D.Double(x, y, width, height));

        if (pageShape != PageShape.RECTANGLE_SHAPE) {
            g.setPaint(Color.BLUE);
            g.setStroke(new BasicStroke(point / 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{point * 2f, point * 1f}, 0f));
            pageShape.debugDraw(g, new Rectangle2D.Double(x, y, width, height));
        }

        g.setPaint(oldPaint);
        g.setStroke(oldStroke);
    }

    /**
     * Convert current markup into an array of StyledParagraph objects, one for
     * each paragraph.
     */
    protected void createStyledText() {
        // we have already styled this text
        if (paragraphs != null) {
            return;
        }

        // reset this flag, which is used to special case single word texts---
        // they will be drawn as a single line if text scaling is allowed;
        // this handles the most common case where a single word is too long
        // to fit on one line and is broken between letters
        textHasWhitespace = false;

        // force the text to be scaled and spaced again
        lastLaidOutRectangle = null;

        // attributed strings can't be empty
        if (markup == null || markup.length() == 0) {
            markup = " ";
        }

        String[] lines = breakIntoParagraphs(markup);
        if (lines.length > 1) {
            textHasWhitespace = true;
        }

        tabs.beginRecording(lines.length);

        // the user can adjust the line justification of body text
        // we will start at whatever the default is
        currentJustification = baseJustify;
        currentAutoclose = defaultAutoclose;
        currentVerticalJustification = baseJustify;

        // allow punctuation replacement (" --> ``, etc.) initially
        currentPunctuationReplacement = 0;

        // these are used by openStyle, closeStyle, etc. to manage
        // the ranges to which text styles will be applied
        styleStackMap.clear();
        finishedStyles.clear();

        // parse each line in turn
        paragraphs = new StyledParagraph[lines.length];
        for (int line = 0; line < lines.length; ++line) {
            tabs.setCurrentLine(line);
            try {
                paragraphs[line] = createStyledParagraph(lines[line]);
            } catch (StackOverflowError soe) {
                paragraphs[line] = new StyledParagraph(soe.getLocalizedMessage());
            }
        }

        tabs.endRecording();

        // clear out temporary structures, allowing them to be gc'd
        if (!retainDefinitions) // needed by setLibrary()
        {
            definitions.clear();
        }
    }

    /**
     * Process a markup string, retaining only any definitions that are created.
     * Once some markup is processed (other than a library) all definitions will
     * be cleared again. Therefore, any required libraries must be added before
     * <i>each</i> call to <code>setMarkupText</code>.
     * <p>
     * This method clears the current markup text.
     */
    public void parseLibrary(String library) {
        retainDefinitions = true;
        setMarkupText(library);
        createStyledText();
        setMarkupText(null);
        retainDefinitions = false;
    }
    private boolean retainDefinitions = false;

    /**
     * Use {@link #parseLibrary(java.lang.String)}.
     *
     * @param library the text of the library to glean definitions from
     * @deprecated
     */
    @Deprecated
    public void addLibrary(String library) {
        parseLibrary(library);
    }

    protected StyledParagraph[] getStyledText() {
        return paragraphs;
    }

    /**
     * During parsing, open a new style starting at some position in the text.
     */
    private void openStyle(String tag, TextStyle style, int start) {
        LinkedList<StyleMark> stack = styleStackMap.get(tag);

        if (stack == null) {
            stack = new LinkedList<>();
            styleStackMap.put(tag, stack);
        }

        stack.add(new StyleMark(style, start));
    }

    /**
     * During parsing, close the most recent instance of tag at some position in
     * the text.
     */
    private void closeStyle(String tag, int end) {
        LinkedList<StyleMark> stack = styleStackMap.get(tag);

        if (stack != null) {
            if (!stack.isEmpty()) {
                StyleMark mark = stack.pop();
                mark.end = end;
                if (mark.start < mark.end) {
                    finishedStyles.add(mark);
                }
            }
        }
    }

    Object getStyleInCurrentContext(TextAttribute ta) {
        if (styleStackMap == null) {
            throw new IllegalStateException("no context available because not parsing markup");
        }
        int maxStart = -1;
        Object value = defaultStyle.get(ta);

        // for every open style tag, check if the style for that tag
        // affects the text attribute of interest. if it does, and if
        // its start position is later than our cuurent maxStart, adopt
        // that value as the most recent one; at the end, the value is
        // the "current" one at our position in the text
        for (String tag : styleStackMap.keySet()) {
            LinkedList<StyleMark> stack = styleStackMap.get(tag);
            if (!stack.isEmpty()) {
                StyleMark mark = styleStackMap.get(tag).getLast();
                if (mark.start > maxStart && mark.style.contains(ta)) {
                    maxStart = mark.start;
                    value = mark.style.get(ta);
                }
            }
        }
        // if null set to default
        return value;
    }

    /**
     * Close any styles that were not closed by the end of this line. If
     * {@code carryOver} is false, styles that are not closed do not carry over
     * to subsequent lines. Otherwise, open styles will be remembered across
     * lines.
     */
    private void closeOutstandingStyles(int endOfLine, boolean carryOver) {
        for (LinkedList<StyleMark> stack : styleStackMap.values()) {
            for (StyleMark mark : stack) {
                // we must copy the style because if carryOver is true the
                // original must stay on the stack with different values
                if (mark.start < endOfLine) {
                    finishedStyles.add(new StyleMark(mark.style, mark.start, endOfLine));
                }
                mark.start = 0; // style carries over from the start of the next line

                mark.end = -1;
            }
        }

        if (!carryOver) {
            styleStackMap.clear();
        }
    }
    protected int currentIndex;
    protected int currentTagStartingIndex;
    protected int currentTagEndingIndex;
    protected int currentJustification;
    protected int currentTightness;
    protected int currentPositionInUntaggedText;
    protected int currentVerticalJustification;
    protected boolean currentHeadlineStatus;
    protected boolean currentAutoclose;
    protected int currentPunctuationReplacement;
    protected String currentTag;
    protected String currentTagNoCase;
    protected String currentParametricTag;
    protected StringBuilder currentSourceLine;
    protected HashMap<String, LinkedList<StyleMark>> styleStackMap = new HashMap<>();
    protected LinkedList<StyleMark> finishedStyles = new LinkedList<>();
    protected boolean textHasWhitespace;

    @SuppressWarnings("fallthrough")
    protected StyledParagraph createStyledParagraph(String source) {
        boolean quote = false;
        boolean backslash = false;

        currentTagStartingIndex = 0;
        currentTightness = 0;
        currentHeadlineStatus = false;

        // this resets the automatic quote conversion for a new line
        resetPunctuationSelectorState();

        // we convert the line into a mutable form; this will allow
        // us to replace and then re-process a section of text; a replacement
        // tag may replace itself with tagged material to be parsed
        currentSourceLine = new StringBuilder(source);
        final int MAXIMUM_SOURCE_SIZE = source.length() + bufferLimit;

        // holds the raw text (without tags) as we process the line
        StringBuilder text = new StringBuilder();
        // holds the tag we are currently processing
        StringBuilder tag = new StringBuilder();

        final int TEXT = 0, TAG = 1, TAGQ = 2, TAGQBS = 3, TAGSTART = 4;
        int state = TEXT;

        for (currentIndex = 0; currentIndex < currentSourceLine.length(); ++currentIndex) {
            if (currentSourceLine.length() > MAXIMUM_SOURCE_SIZE) {
                throw new StackOverflowError(
                        "Probable infinite recursion while expanding definition: " + currentSourceLine.substring(currentSourceLine.length() - 80));
            }
            char c = currentSourceLine.charAt(currentIndex);

            switch (state) {
                case TEXT:
                    if (Character.isWhitespace(c)) {
                        textHasWhitespace = true;
                    }
                    if (c == '<') {
                        currentTagStartingIndex = currentIndex;
                        tag.delete(0, tag.length());
                        state = TAGSTART;
                    } else {
                        if (c == '\t') {
                            tabs.addTab(text.length());
                        } else if (c == '"' || c == '\'' || c == '-' || c == '\u2013' || c == '.') {
                            c = selectPunctuation(c, text);
                        }
                        text.append(c);
                    }
                    break;
                case TAGSTART:
                    if (c != '>') {
                        tag.append(c);
                        if (Character.isWhitespace(c)) {
                            state = TAG;
                        }
                        break;
                    }
                // fallthrough for '>' handling
                case TAG:
                    if (c == '>') {
                        currentTagEndingIndex = currentIndex;
                        currentTag = tag.toString();
                        currentTagNoCase = currentTag.toLowerCase();
                        currentParametricTag = getParametricTagName();
                        currentPositionInUntaggedText = text.length();
                        state = TEXT;

                // we need to special case <lt>, <gt> because we want the symbol put
                // directly into the untagged text buffer; if we use replaceTagWith,
                // the resulting "<" or ">" will be re-parsed
                switch (currentTagNoCase) {
                    case "lt":
                        text.append("<");
                        break;
                    case "gt":
                        text.append(">");
                        break;
                    default:
                        handleTag();
                        break;
                }
                    } else {
                        tag.append(c);
                        if (c == '"') {
                            state = TAGQ;
                        }
                    }
                    break;
                case TAGQ:
                    if (c == '\\') {
                        state = TAGQBS;
                    } else {
                        tag.append(c);
                        if (c == '"') {
                            state = TAG;
                        }
                    }
                    break;
                case TAGQBS:
                    // keep \ at this stage; getTagParameters will remove it

//					if (c != '"' && c != '\\' ) {
//						tag.append('\\');
//					}
                    tag.append('\\');
                    tag.append(c);
                    state = TAGQ;
                    break;
                default:
                    throw new AssertionError("Unknown state in finite state machine");
            }
        }

        String lineText = text.toString();
        if (lineText.length() == 0) {
            lineText = " ";
        }
        StyledParagraph line = new StyledParagraph(lineText);
        tabs.addTab(lineText.length());

        defaultStyle.applyStyle(line);

        closeOutstandingStyles(lineText.length(), !currentAutoclose);
        for (StyleMark mark : finishedStyles) {
            mark.applyStyle(line);
        }
        finishedStyles.clear();

//		if( (currentJustification & LAYOUT_JUSTIFY) != 0 ) {
//			TextStyle.JUSTIFY.applyStyle( line );
//		}
        line.setAlignment(currentJustification);
        line.setLineTightness(currentTightness);
        line.setTitleLine(currentHeadlineStatus);

        return line;
    }

    protected void resetPunctuationSelectorState() {
        openQuotes = 0;
    }

    protected char selectPunctuation(char curr, StringBuilder writtenText) {
        if (currentPunctuationReplacement < 0) {
            return curr;
        }

        char prev = writtenText.length() == 0 ? ' ' : writtenText.charAt(writtenText.length() - 1);

        // Straight quotes to open and close quotes
        if (curr == '"' || curr == '\'') {
            boolean useCloseQuote = true;

            if (Character.isWhitespace(prev)) {
                useCloseQuote = false;
            } else {
                int type = Character.getType(prev);
                if (type == Character.DASH_PUNCTUATION) {
                    useCloseQuote = openQuotes > 0;
                }
            }

            if (useCloseQuote) {
                if (--openQuotes < 0) {
                    openQuotes = 0;
                }
                curr = (curr == '"') ? '\u201d' : '\u2019';
            } else {
                ++openQuotes;
                curr = (curr == '"') ? '\u201c' : '\u2018';
            }
        } // Hyphens to en-dash and em-dash
        else if (curr == '-') {
            if (prev == '-' || prev == '\u2013') {
                deletePreviousPunctuation(writtenText, 1);
                curr = (prev == '-') ? '\u2013' : '\u2014';
            }
        } // 3 periods to ellipsis
        else if (curr == '.' && prev == '.') {
            if (writtenText.length() >= 2 && writtenText.charAt(writtenText.length() - 2) == '.') {
                deletePreviousPunctuation(writtenText, 2);
                curr = '\u2026';
            }
        }
        return curr;
    }

    /**
     * Deletes the previous count characters in order to replace them with an
     * automatic punctuation replacement.
     */
    private void deletePreviousPunctuation(StringBuilder text, int count) {
        assert (count > 0);
        text.delete(text.length() - count, text.length());
    }
    /**
     * Depth to which quotation marks are nested, for doing punctuation
     * replacement.
     */
    protected int openQuotes;

    private void handleTag() {
        // check if the tag should be replaced with other text
        String replacement = findReplacementForTag();
        if (replacement != null) {
            replaceTagWith(replacement);
            return;
        }

        if (currentParametricTag.equals("script")) {
            String[] params = getTagParameters();
            if (params.length >= 1) {
                if (evaluator == null) {
                    evaluator = evaluatorFactory.createEvaluator(this);
                }
                Object result = evaluator.evaluateScript(params);
                if (result == null) {
                    result = "";
                }
                replaceTagWith(result.toString());
            } else {
                replaceTagWith("");
            }
            return;
        }

        if (currentParametricTag.equals("eval")) {
            String[] params = getTagParameters();
            if (params.length >= 1) {
                if (evaluator == null) {
                    evaluator = evaluatorFactory.createEvaluator(this);
                }
                String expression = params[0];
                if (params.length > 1) {
                    StringBuilder b = new StringBuilder(params[0]);
                    for (int i = 1; i < params.length; ++i) {
                        b.append(' ').append(params[i]);
                    }
                    expression = b.toString();
                }
                Object result = evaluator.evaluateExpression(expression);
                if (result == null) {
                    result = "";
                }
                replaceTagWith(result.toString());
            } else {
                replaceTagWith("");
            }
            return;
        }

        // check if this tag is a definition
        if (currentParametricTag.startsWith("define")) {
            String[] params = getTagParameters();
            if (params.length >= 2) {
                setDefinitionForTag(params[0], params[1]);
            }
            return;
        }

        // check if a style is defined for this tag
        boolean isCloseTag = currentTagNoCase.startsWith("/");
        String agnosticTag = currentTagNoCase;
        if (isCloseTag) {
            agnosticTag = agnosticTag.substring(1);
        }

        TextStyle style = styleMap.get(agnosticTag);
        if (style != null) {
            if (isCloseTag) {
                closeStyle(agnosticTag, currentPositionInUntaggedText);
            } else {
                style = adjustStyleForContext(style);
                openStyle(agnosticTag, style, currentPositionInUntaggedText);
            }

            // set the headline status if the tag is an <Hn>---these have
            // their own alignment setting: we do this here in case the
            // user replaces or redefines <Hn>s so it is no longer a heading
            if (currentTagNoCase.length() == 2 && currentTagNoCase.charAt(0) == 'h') {
                char c = currentTagNoCase.charAt(1);
                if (c >= '1' && c <= '7') {
                    currentHeadlineStatus = true;
                }
            }
            return;
        } // check if this is a punctutation replacement control tag
        // NOTE: we need to do this before re-defining agnosticTag,
        //       or else restore agnosticTag first
        else if (agnosticTag.equals("no punctuation")) {
            if (isCloseTag) {
                ++currentPunctuationReplacement;
            } else {
                --currentPunctuationReplacement;
            }
            return;
        }

        // check if a style factory is defined for this tag
        if (!isCloseTag) {
            agnosticTag = currentParametricTag;
        }

        ParametricStyleFactory factory = parametricStyleMap.get(agnosticTag);
        if (factory != null) {
            if (isCloseTag) {
                closeStyle(agnosticTag, currentPositionInUntaggedText);
            } else {
                openStyle(agnosticTag, factory.createStyle(this, getTagParameters()), currentPositionInUntaggedText);
            }
            return;
        }

        // these are leftovers that affect parameters of the
        // line or subsequent lines without generating text or styles
        // check if this is a top stop setting tag
        if (currentParametricTag.equals("tabwidth")) {
            String[] params = getTagParameters();
            if (params.length >= 1) {
                double[] tabs = new double[params.length];
                for (int i = 0; i < params.length; ++i) {
                    double m = parseMeasurement(params[i]);
                    if (m != m || m < 0d) {
                        m = 0d;
                    }
                    tabs[i] = m;
                }
                setTabWidths(tabs);
            }
            return;
        } // check if this is a loop request
        else if (currentParametricTag.equals("repeat")) {
            String[] params = getTagParameters();

            if (params.length == 2) {
                int repeats = -1;
                try {
                    repeats = Integer.parseInt(params[0]);
                } catch (NumberFormatException e) {
                }

                if (repeats > 0 && repeats <= 10_000) {
                    StringBuilder b = new StringBuilder();
                    for (int i = 0; i < repeats; ++i) {
                        b.append(params[1]);
                    }
                    replaceTagWith(b.toString());
                }
            }
            return;
        } // check if this is a justification tag
        else if (currentTagNoCase.equals("left")) {
            currentJustification = LAYOUT_LEFT | (currentJustification & LAYOUT_JUSTIFY);
            return;
        } else if (currentTagNoCase.equals("center") || currentTagNoCase.equals("centre")) {
            currentJustification = LAYOUT_CENTER | (currentJustification & LAYOUT_JUSTIFY);
            return;
        } else if (currentTagNoCase.equals("right")) {
            currentJustification = LAYOUT_RIGHT | (currentJustification & LAYOUT_JUSTIFY);
            return;
        } else if (currentTagNoCase.equals("justified")) {
            currentJustification = currentJustification & ~LAYOUT_JUSTIFY | LAYOUT_JUSTIFY;
        } else if (currentTagNoCase.equals("ragged")) {
            currentJustification &= ~LAYOUT_JUSTIFY;
        } else if (currentTagNoCase.equals("top")) {
            currentVerticalJustification = LAYOUT_TOP;
            return;
        } else if (currentTagNoCase.equals("middle")) {
            currentVerticalJustification = LAYOUT_MIDDLE;
            return;
        } else if (currentTagNoCase.equals("bottom")) {
            currentVerticalJustification = LAYOUT_BOTTOM;
            return;
        } // check is this is a line tightness tag
        else if (currentTagNoCase.equals("tight")) {
            --currentTightness;
            return;
        } else if (currentTagNoCase.equals("loose")) {
            ++currentTightness;
            return;
        } // check if this is an auto/manual close tag
        else if (currentTagNoCase.equals("autoclose")) {
            currentAutoclose = true;
            return;
        } else if (currentTagNoCase.equals("manualclose")) {
            currentAutoclose = false;
            return;
        }

        // finally: it's not any tag our system knows about, let's ask the subclass
        // if it wants to process the tag
        replacement = handleUnknownTag(currentTagNoCase, currentTag);
        if (replacement != null) {
            replaceTagWith(replacement);
        }
    }

    protected TextStyle adjustStyleForContext(TextStyle style) {
        /*
		// TODO: convert nested <i> to plain

		// special handling for some built-in styles
		if( style == TextStyle.ITALIC_STYLE ) {
//			System.err.println( getStyleInCurrentContext( TextAttribute.POSTURE ) );
			if( getStyleInCurrentContext( TextAttribute.POSTURE ) == TextAttribute.POSTURE_OBLIQUE ) {
				style = TextStyle.UPRIGHT_STYLE;
			}
		}
         */
        return style;
    }

    /**
     * If this tag is a replacement tag (either one defined programmatically
     * using setReplacementForTag or one set by the user with a &lt;define&gt;
     * tag), return the replacement text for this tag. Otherwise, return null.
     */
    protected String findReplacementForTag() {
        // is the full tag name a user-defined replacement?
        String replacement = definitions.get(currentTagNoCase);

        // is the parametric tag name a user-defined replacement with parameters?
        // if so, we will replace @1;, @2;, etc., in the definition with the
        // parameters supplied to the tag
        if (replacement == null) {
            replacement = definitions.get(currentParametricTag);
            if (replacement != null) {
                String[] params = getTagParameters();
                replacement = replaceMacroParameters(replacement, params, null, null);
            }
        } else {
            replacement = replaceMacroParameters(replacement, EMPTY_PARAMETER_ARRAY, null, null);
        }

        // finally, since the tag is not a user-defined replacement, we will
        // check to see if it is a replacement set with setReplacementForTag
        if (replacement == null) {
            replacement = replacements.get(currentTagNoCase);
        }

        return replacement;
    }
    private static final String[] EMPTY_PARAMETER_ARRAY = new String[0];

    /**
     * Returns a string where instances of @n; in {@code text} are replaced with
     * the text of {@code parameter[n-1]}.
     */
    protected String replaceMacroParameters(String text, String[] parameters, String repeatKey, String repeatValue) {
        StringBuilder b = new StringBuilder();
        StringBuilder n = new StringBuilder();
        boolean inText = true;
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (inText) {
                if (c == '@') {
                    inText = false;
                    n.delete(0, n.length());
                } else {
                    b.append(c);
                }
            } else {
                if (c == ';') {
                    inText = true;
                    int number = -1;
                    String value = n.toString();
                    if (repeatKey != null && value.equals(repeatKey)) {
                        b.append(repeatValue);
                    } else {
                        try {
                            number = Integer.valueOf(value) - 1;
                        } catch (NumberFormatException e) {
                        }
                        if (number < 0 || number >= parameters.length) {
                            b.append('?').append(n.toString()).append('?');
                        } else {
                            b.append(parameters[number]);
                        }
                    }
                } else {
                    n.append(c);
                }
            }
        }
        return b.toString();
    }

    /**
     * Returns the tag name of a tag that may include parameters. That is, it
     * returns the current tag name, in lowercase, up to but not including the
     * first whitespace character. Returns the entire tag if there is no
     * whitespace.
     */
    private String getParametricTagName() {
        int i = 0;
        for (; i < currentTagNoCase.length(); ++i) {
            if (Character.isWhitespace(currentTagNoCase.charAt(i))) {
                break;
            }
        }
        return currentTagNoCase.substring(0, i);
    }

    /**
     * Return an array of parameters included in the current tag. If there are
     * no parameters, return an empty array. Parameters are separated from the
     * main tag name and each other by whitespace. A parameter surrounded by
     * quotes (") may contain whitespace. A parameter surrounded by quotes may
     * use the escape \" to include a quote without ending the parameter.
     */
    protected String[] getTagParameters() {
        ArrayList<String> params = new ArrayList<>(4);

        final int NOTOKE = 0, TOKE = 1, QTOKE = 2, QTOKEBS = 3;
        int state = 0;

        // skip tag name
        int i = 0;
        for (; i < currentTag.length() && !Character.isWhitespace(currentTag.charAt(i)); ++i) {
        }

        // process remainder of string in finite state machine
        StringBuilder token = new StringBuilder();
        for (; i < currentTag.length(); ++i) {
            char c = currentTag.charAt(i);

            switch (state) {
                case NOTOKE:
                    if (!Character.isWhitespace(c)) {
                        if (c == '"') {
                            state = QTOKE;
                            token.delete(0, token.length());
                        } else {
                            state = TOKE;
                            token.delete(0, token.length());
                            token.append(c);
                        }
                    }
                    break;
                case TOKE:
                    if (Character.isWhitespace(c)) {
                        state = NOTOKE;
                        params.add(token.toString());
                    } else {
                        token.append(c);
                    }
                    break;
                case QTOKE:
                    if (c == '\\') {
                        state = QTOKEBS;
                    } else if (c == '"') {
                        state = NOTOKE;
                        params.add(token.toString());
                    } else {
                        token.append(c);
                    }
                    break;
                case QTOKEBS:
                    state = QTOKE;
                    if (c != '"' && c != '\\') {
                        token.append('\\');
                    }
                    token.append(c);
                    break;
                default:
                    throw new AssertionError("Unknown state in state machine");
            }
        }

        // add the final token if we were working on one
        if (state != NOTOKE && token.length() > 0) {
            params.add(token.toString());
        }

        return params.toArray(new String[params.size()]);
    }

    /**
     * Replaces the tag currently being parsed with arbitrary text. This text
     * may contain tags, and if so, they will also be parsed.
     */
    protected void replaceTagWith(String replacement) {
        currentIndex = currentTagStartingIndex - 1; // -1 since index will be incremented

        currentSourceLine.delete(currentTagStartingIndex, currentTagEndingIndex + 1);
        currentSourceLine.insert(currentTagStartingIndex, replacement);
    }

    protected void replaceTagWithErrorMessage(String message) {
        replaceTagWith("<color bb3333><b>" + message + "</b></color>");
    }

    /**
     * Create a new user-defined definition.
     */
    public void setDefinitionForTag(String name, String replacement) {
        definitions.put(name.toLowerCase(), replacement);
    }

    /**
     * This method is called when a tag is encountered that does not match any
     * existing defined or built-in tag. Subclasses may override it as a simple
     * way to extend the behaviour of this class.
     * <p>
     * If the method returns a string, that string will be interpreted exactly
     * as if it had appeared instead of the unknown tag. It may include both
     * text and tags.
     * <p>
     * If the method does not have a suitable replacement for the unknown tag,
     * it should return {@code null}.
     * <p>
     * The base class parses tags of the form &lt;u+xxxx&gt; to insert Unicode
     * characters from their hexidecimal code and parses &lt;image&gt; tags to
     * insert images into the text stream. To remove these behaviours, override
     * and return {@code null}. To extend them, return the value of the
     * superclass implementation once you have decided you do not want to handle
     * the tag yourself.
     */
    protected String handleUnknownTag(String tagnameLowercase, String tagnameOriginalCase) {
        if (tagnameLowercase.length() <= "u+0000".length() && tagnameLowercase.startsWith("u+")) {
            try {
                char unicodeChar = (char) Integer.parseInt(tagnameLowercase.substring("u+".length()), 16);
                if (unicodeChar == '<') {
                    return "<lt>";
                } else if (unicodeChar == '>') {
                    return "<gt>";
                } else {
                    return String.valueOf(unicodeChar);
                }
            } catch (NumberFormatException e) {
            } // will return no replacement as usual

        }
        if (tagnameLowercase.length() >= 6 && tagnameLowercase.startsWith("image") && Character.isWhitespace(tagnameLowercase.charAt(5))) {
            openStyle("image", graphicStyle.createStyle(this, getTagParameters()), currentPositionInUntaggedText);
            closeStyle("image", currentPositionInUntaggedText + 1);
            return "\uFFFC";
        }

        return null;
    }

    /**
     * Breaks a source string into an array of individual lines. Lines are
     * broken on newline characters, break tags (br), and headline tags (h1, h2,
     * h3, h4, h5, h6, h7).
     */
    @SuppressWarnings("fallthrough")
    protected String[] breakIntoParagraphs(String source) {
        ArrayList<String> paras = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        StringBuilder tag = new StringBuilder();

        boolean suppressingEmptyNewlineAfterHeading = false;
        int len = source.length();
        final int TEXT = 0, TAG = 1, TAGQ = 2, TAGQBS = 3, TEXTBS = 4, TAGSTART = 5;
        int state = TEXT;

        for (int i = 0; i < len; ++i) {
            char c = source.charAt(i);
            switch (state) {
                case TEXTBS:
                    if (Character.isWhitespace(c)) {
                        continue;
                    }
                    state = TEXT;
                // !! fallthrough
                case TEXT:
                    if (c == '<') {
                        tag.delete(0, tag.length());
                        tag.append('<');
                        state = TAGSTART;
                    } else {
                        if (c == '\\') {
                            state = TEXTBS;
                        } else if (c == '\n') {
                            if (!suppressingEmptyNewlineAfterHeading) {
                                paras.add(line.toString());
                                line.delete(0, line.length());
                            }
                        } else {
                            line.append(c);
                        }
                        suppressingEmptyNewlineAfterHeading = false;
                    }
                    break;
                case TAGSTART:
                    if (c != '>') {
                        tag.append(c);
                        if (Character.isWhitespace(c)) {
                            state = TAG;
                        }
                        break;
                    }
                // fallthrough for '>' handling
                case TAG:
                    if (c == '>') {
                        boolean appendTag = true;
                        int tagLen = tag.length();
                        int closeTag = ((tagLen >= 2) && (tag.charAt(1) == '/')) ? 1 : 0;
                        if ((tagLen == 3 && closeTag == 0) || (tagLen == 4 && closeTag == 1)) {
                            char c1 = Character.toLowerCase(tag.charAt(1 + closeTag));
                            char c2 = Character.toLowerCase(tag.charAt(2 + closeTag));
                            if (c1 == 'b' && c2 == 'r') {
                                if (!suppressingEmptyNewlineAfterHeading) {
                                    paras.add(line.toString());
                                    line.delete(0, line.length());
                                }
                                suppressingEmptyNewlineAfterHeading = false;
                                appendTag = false;
                            } else if (c1 == 'h' && c2 >= '1' && c2 <= '7') {
                                if (line.length() > 0) {
                                    if (!suppressingEmptyNewlineAfterHeading) {
                                        paras.add(line.toString());
                                        line.delete(0, line.length());
                                    }
                                }
                                suppressingEmptyNewlineAfterHeading = (closeTag == 1);
                            }
                        }

                        if (appendTag) {
                            line.append(tag).append('>');
                        }
                        state = TEXT;
                    } else {
                        tag.append(c);
                        if (c == '"') {
                            state = TAGQ;
                        }
                    }
                    break;
                case TAGQ:
                    if (c == '\\') {
                        state = TAGQBS;
                    } else {
                        tag.append(c);
                        if (c == '"') {
                            state = TAG;
                        }
                    }
                    break;
                case TAGQBS:
                    // we need to keep the backslash to parse the arg correctly later
                    tag.append('\\');
                    tag.append(c);
                    state = TAGQ;
                    break;
                default:
                    throw new AssertionError("Unknown state in finite state machine");
            }
        }
        if (line.length() > 0) {
            paras.add(line.toString());
        }
        return paras.toArray(new String[paras.size()]);
    }

    /**
     * Converts a <code>Strings</code> containing &lt;br&gt; tags into one that
     * uses newlines instead. This is not required before sending markup to the
     * draw system. Rather, this is intended for use when deserializing markup
     * that has had newlines converted into &lt;br&gt; tags for convenience of
     * encoding.
     * <p>
     * By design, this method recognizes only the exact sequence &lt;, b, r,
     * &gt;: it is case-sensitive and does not recognize &lt;/br&gt;. It does
     * not differentiate between regular tags and nested tags that form
     * arguments of other tags.
     */
    public static String convertTagsToLinebreaks(String source) {
        return source.replace("<br>", "\n");
    }

    /**
     * Try to parse {@code p} as a measurement. If is a valid measurement,
     * returns the measurement in inches. Otherwise, returns a negative number.
     * Measurements consist of a positive floating point number optionally
     * followed by a unit (with no intervening whitespace). Valid units are cm,
     * in (inches), and pt (points). The default unit is cm.
     */
    public static double parseMeasurement(String p) {
        double conversion = 1d / 2.54d;
        p = p.toLowerCase();

        boolean hasUnit = false;
        if (p.endsWith("pt")) {
            conversion = 1d / 72d;
            hasUnit = true;
        } else if (p.endsWith("in")) {
            conversion = 1d;
            hasUnit = true;
        } else if (p.endsWith("cm")) {
            hasUnit = true;
        }
        if (hasUnit) {
            p = p.substring(0, p.length() - 2);
        }

        double measure = -1d;
        try {
            measure = Double.parseDouble(p);
            measure *= conversion;
        } catch (NumberFormatException e) {
        }
        return measure;
    }
    public static boolean DEBUG = false;

    /**
     * This class tracks the character indices where tabs occur in a collection
     * of {@link StyledParagraph}s. The locations of the tabs must be recorded
     * so that when segments of text are prepared during layout and rendering,
     * the segments can be broken at both line and tab boundaries (and for tabs,
     * the drawing cursor moved to the next tab stop).
     */
    class TabManager {

        public TabManager() {
        }

        public void beginRecording(int lineCount) {
            line = 0;
            tabLists = new ArrayList[lineCount];
        }

        public void setCurrentLine(int line) {
            this.line = line;
        }

        public void addTab(int index) {
            ArrayList<Integer> list = tabLists[line];
            if (list == null) {
                list = new ArrayList<>();
                tabLists[line] = list;
            }
            list.add(index);
        }

        public void endRecording() {
            rawLists = new int[tabLists.length][];
            for (int i = 0; i < tabLists.length; ++i) {
                ArrayList<Integer> list = tabLists[i];
                if (list != null) {
                    int len = list.size();
                    int[] newlist = new int[list.size()];
                    for (int j = 0; j < len; ++j) {
                        newlist[j] = list.get(j);
                    }
                    rawLists[i] = newlist;
                }
            }
        }

        public int[] getTabList(int line) {
            return rawLists[line];
        }
        private ArrayList<Integer>[] tabLists;
        private int line;
        private int[][] rawLists;
    }

    /**
     * This class provides an <code>AttributedString</code> with extended
     * information on how the string should be drawn.
     */
    protected class StyledParagraph extends AttributedString {

        public StyledParagraph(String text) {
            super(text);
            titleLine = false;
            lineTightness = 0;
        }

        public boolean isTitleLine() {
            return titleLine;
        }

        public void setTitleLine(boolean isATitle) {
            titleLine = isATitle;
        }

        public int getLineTightness() {
            return lineTightness;
        }

        public void setLineTightness(int tightness) {
            lineTightness = tightness;
        }

        public void setAlignment(int alignment) {
            this.alignment = alignment;
        }

        public int getAlignment() {
            return alignment;
        }
        private boolean titleLine;
        private int lineTightness;
        private int alignment;
    }

    /**
     * This class groups a style and a range of characters it applies to. It is
     * used when parsing markup to keep track of pending style applications.
     * <p>
     * When parsing, a map is maintained from tag names to stacks of
     * {@code StyleMark}s. When an open tag occurs, a new {@code StyleMark} is
     * placed on the stack for that tag indicating the style that will be
     * applied and where in the text it starts.
     * <p>
     * When a close tag occurs, the most recent {@code StyleMark} is popped off
     * of the stack for that tag name. The end position is filled in and then it
     * is added to a list of finished styles.
     * <p>
     * Once the line is parsed, the list of finished styles can be applied to
     * the (now tagless) document text.
     */
    static class StyleMark {

        public StyleMark(TextStyle style, int start) {
            this.style = style;
            this.start = start;
            end = -1;
        }

        public StyleMark(TextStyle style, int start, int end) {
            this.style = style;
            this.start = start;
            this.end = end;
        }

        public void applyStyle(AttributedString s) {
            style.applyStyle(s, start, end);
        }

        @Override
        public String toString() {
            return "(" + start + "--" + end + "): " + style;
        }
        public TextStyle style;
        public int start;
        public int end;
    }

    private BreakIterator createBreakIterator() {
        return createBreakIterator(Locale.getDefault());
    }

    private BreakIterator createBreakIterator(Locale loc) {
        BreakIterator bi;

        if (USE_PATCHED_LINE_BREAKER) {
            bi = new LineBreakIterator(loc);
        } else {
            bi = BreakIterator.getLineInstance(loc);
        }

        if (USE_FAST_LINE_BREAKER) {
            bi = new FastBreakIterator(bi);
        }

        return bi;
    }
    public static boolean USE_PATCHED_LINE_BREAKER = true;
    public static boolean USE_FAST_LINE_BREAKER = true;

    private File baseFile;

    /**
     * Returns the base file that the formatter may optionally use to complete
     * relative path names, or <code>null</code> if none is defined.
     *
     * @return the base file for relative paths, or <code>null</code>
     */
    public File getBaseFile() {
        return baseFile;
    }

    /**
     * Sets the base file that the formatter may optionally use to complete
     * relative path names.
     *
     * @param baseFile the base file name to use
     */
    public void setBaseFile(File baseFile) {
        this.baseFile = baseFile;
    }

    @Override
    public String toString() {
        return "MarkupRenderer{alignment=" + getAlignment()
                + ", defaultStyle=" + getDefaultStyle()
                + '}';
    }

    static final Logger log = Logger.getLogger(MarkupRenderer.class.getPackage().getName());
}
