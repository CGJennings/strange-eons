package ca.cgjennings.apps.arkham.sheet;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.deck.item.PageItem;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.graphics.PrototypingGraphics2D;
import ca.cgjennings.layout.MarkupRenderer;
import gamedata.Expansion;
import gamedata.Game;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import resources.CacheMetrics;
import resources.Language;
import resources.ResourceKit;
import resources.Settings;
import resources.Settings.ParseError;

/**
 * An abstract base class for objects that paint one face (side) of a
 * {@link GameComponent}. Subclasses must provide a <i>template key</i>
 * before the sheet can be used. This is normally  {@linkplain #Sheet(ca.cgjennings.apps.arkham.component.GameComponent, java.lang.String)
 * provided during construction}, but sheets with more complex initialization
 * may delay this step and call
 * {@link #initializeTemplate(java.lang.String) initializeTemplate} later to set
 * the key.
 *
 * <p>
 * Sheets maintain an internal image buffer that is used when painting. This
 * buffer is only guaranteed to be valid while the face is being painted, and
 * then only after you call either {@link #createGraphics() createGraphics} to
 * get a graphics context for the image buffer, or
 * {@link #getDestinationBuffer() getDestinationBuffer} to access it as a
 * <code>BufferedImage</code>.
 *
 * <p>
 * This class provides a number of static helper methods for drawing tasks that
 * are commonly needed when drawing components, such as drawing text within
 * rectangular regions of the face  ({@link #drawTitle(java.awt.Graphics2D, java.lang.String, java.awt.Rectangle, java.awt.Font, float, int) drawTitle},
 * {@link #fitTitle(java.awt.Graphics2D, java.lang.String, java.awt.Rectangle, java.awt.Font, float, int) fitTitle}
 * {@link #drawOutlinedTitle(java.awt.Graphics2D, java.lang.String, java.awt.Rectangle, java.awt.Font, float, float, java.awt.Paint, java.awt.Paint, int, boolean) drawOutlinedTitle},
 * {@link #drawRotatedTitle(java.awt.Graphics2D, java.lang.String, java.awt.Rectangle, java.awt.Font, float, int, int) drawRotatedTitle}),
 * performing custom handling of expansion symbols
 * ({@link #getExpansionSymbol getExpansionSymbol}, {@link #parseExpansionList parseExpansionList}),
 * and setting up markup text boxes  ({@link #doStandardRendererInitialization doStandardRendererInitialization},
 * {@link #setNamesForRenderer setNamesForRenderer}).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public abstract class Sheet<G extends GameComponent> {

    /**
     * The name of the function called in an on-paint script. If the game
     * component associated with this sheet has a setting key whose name equals
     * the value of {@link ScriptMonkey#ON_PAINT_EVENT_KEY}, then the value of
     * that key will be executed as script code and then the function with this
     * name ("onPaint") will be called. The function is passed a graphics
     * context for the sheet image buffer, a reference to the game component,
     * and a reference to this sheet. Plug-ins can use this to produce special
     * effects or to customize existing component types.
     */
    public static final String ON_PAINT_EVENT_METHOD = "onPaint";

    private G gameComponent;

    private BufferedImage template;
    private BufferedImage image;
    private double dpi;
    private double upsampleFactor = 1d;
    private double preferredUpsample = 1d;

    // stores the template key set with initializeTemplate
    private String templateKey;
    // stores the base key name derived from template key
    private String keybase;

    // set by markChanged
    private boolean changeFlag = true;
    /**
     * The key used to fetch the expansion symbol region; this is normally set
     * automatically by {@link #initializeTemplate}. However, subclasses can
     * change the key during the call to {@link #paintSheet} to modify the
     * default expansion symbol painting algorithm in response to the component
     * state.
     */
    private String expsymKey;

    // if true we are currently painting a sheet
    private boolean drawLock;

    private void checkUnlocked() {
        if (drawLock) {
            throw new IllegalStateException("cannot be called while painting the sheet");
        }
    }

    private void checkLocked() {
        if (!drawLock) {
            throw new IllegalStateException("can only be called while painting the sheet");
        }
    }

    private Sheet() {
    } // subclasses *must* provide a component

    /**
     * Creates a component face for a game component. Concrete subclasses must
     * call {@link #initializeTemplate} at some point in their constructor.
     *
     * @param gameComponent the component that this sheet draws for
     */
    public Sheet(G gameComponent) {
        if (gameComponent == null) {
            throw new NullPointerException("gameComponent");
        }
        this.gameComponent = gameComponent;
    }

    /**
     * Creates a component face for a game component that will use a template
     * image defined by the value of the <code>templateKey</code> setting. Note
     * that the sheet is not required to actually use the template image. For
     * example, the actual template image that gets drawn may be selected from
     * one of several different images depending on settings in the associated
     * game component.
     *
     * @param gameComponent the component that this sheet draws for
     * @param templateKey the key that defines the template image
     */
    public Sheet(G gameComponent, String templateKey) {
        if (gameComponent == null) {
            throw new NullPointerException("gameComponent");
        }
        this.gameComponent = gameComponent;
        initializeTemplate(templateKey);
    }

    /**
     * Initializes the template image, resolution, default expansion symbol
     * location, and upsample rate from a base key name. If the constructor that
     * takes a template key as a parameter was used to create this sheet, then
     * this method will already have been called. Otherwise, you must call this
     * method from within the subclass constructor.
     *
     * <p>
     * The initialization process consists of the following steps:
     * <ol>
     * <li> The {@link Settings} of the game component associated with this
     * sheet are fetched to look up the keys below.
     * <li> The template image is loaded using <i>templateKey</i>.
     * <li> If <i>templateKey</i> ends with <code>-template</code>, this is
     * removed.
     * <li> The default region for drawing expansion symbols, if any, is read
     * from <code><i>templateKey</i>-expsym</code>.
     * <li> The template image resolution, in pixels per inch, is read from
     * <code><i>templateKey</i>-ppi</code>; if undefined, the default is 150
     * ppi. (The suffix <code>-dpi</code> can also be used.)
     * <li> The preferred display upsample factor is read from
     * <code><i>templateKey</i>-upsample</code>; if undefined, the default is 1;
     * this is multiplied by the template resolution to determine the default
     * resolution for rendering. The default resolution is used by the preview
     * window; components with extremely small text can be made more legible by
     * increasing this value.
     * </ol>
     *
     * @param templateKey the base key name to use to initialize the template
     * for this sheet
     * @throws NullPointerException if the template key is <code>null</code>
     */
    protected final void initializeTemplate(String templateKey) {
        if (templateKey == null) {
            throw new NullPointerException("templateKey");
        }
        this.templateKey = templateKey;

        Settings settings = gameComponent.getSettings();
        template = settings.getImageResource(templateKey);

        keybase = templateKey;
        if (templateKey.endsWith("-template")) {
            keybase = keybase.substring(0, keybase.length() - "-template".length());
        }

        expsymKey = keybase + "-expsym";

        if (settings.get(keybase + "-ppi") != null) {
            dpi = settings.getDouble(keybase + "-ppi");
        } else if (settings.get(keybase + "-dpi") != null) {
            dpi = settings.getDouble(keybase + "-dpi");
        } else {
            dpi = 150d;
        }

        if (settings.get(keybase + "-upsample") != null) {
            preferredUpsample = settings.getDouble(keybase + "-upsample");
        }

        image = null;
        markChanged();
    }

    /**
     * Initializes the sheet from raw values instead of from settings.
     *
     * @param pseudoKey the key value to report from {@link #getTemplateKey()}
     * @param template the template image for the sheet
     * @param expansionSymbolKey the key to check for an expansion symbol region
     * for default expansion symbol painting
     * @param resolution the resolution of the template image, in pixels per
     * inch
     * @param upsampleFactor the upsample factor that determines the default
     * resolution
     * @throws NullPointerException if the template or expansion symbol key is
     * null
     * @throws IllegalArgumentException if the resolution or upsample factor is
     * not positive
     */
    protected final void initializeTemplate(String pseudoKey, BufferedImage template, String expansionSymbolKey, double resolution, double upsampleFactor) {
        templateKey = pseudoKey;

        if (template == null) {
            throw new NullPointerException("template");
        }
        if (expsymKey == null) {
            throw new NullPointerException("expansionSymbolKey");
        }
        if (resolution <= 0d) {
            throw new IllegalArgumentException("resolution");
        }
        if (upsampleFactor <= 0d) {
            throw new IllegalArgumentException("upsampleFactor");
        }

        this.template = template;
        expsymKey = expansionSymbolKey;
        dpi = resolution;
        preferredUpsample = upsampleFactor;

        image = null;
        markChanged();
    }

    /**
     * If this sheet represents an embedded marker, returns the layout style of
     * the marker. For regular sheets, this method returns <code>null</code>.
     * (The base class returns <code>null</code>.)
     *
     * @return the token style of the embedded token, or <code>null</code> for
     * standard sheets
     */
    public MarkerStyle getMarkerStyle() {
        return null;
    }

    private boolean isPrototype = Settings.getUser().getBoolean("render-as-prototype");

    /**
     * Sets whether the sheet is rendered in prototype mode, with a white background
     * and only text and shapes (no images).
     *
     * @param prototype true if prototype mode should be enabled
     */
    public void setPrototypeRenderingModeEnabled(boolean prototype) {
        checkUnlocked();
        if(isPrototype != prototype) {
            isPrototype = prototype;
            markChanged();
            image = null;
        }
    }

    /**
     * Returns whether prototype rendering mode is enabled.
     *
     * @return true if prototype rendering is enabled
     */
    public boolean isPrototypeRenderingModeEnabled() {
        return isPrototype;
    }

    private static void fillPrototypeModeBackground(BufferedImage bi) {
        Graphics2D g = bi.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, bi.getWidth(), bi.getHeight());
        g.dispose();
    }

    /**
     * Returns an image of the sheet content. The caller must not modify the
     * contents of the returned image, as the image may be reused to speed up
     * subsequent paint requests. (If you need to modify the returned image,
     * make a copy first and modify the copy; see
     * {@link ImageUtilities#copy(java.awt.image.BufferedImage)}.)
     *
     * <p>
     * If the sheet needs to be redrawn to satisfy the request, this method will
     * call {@link #paintSheet} to allow the subclass to paint the
     * component-specific content. It will then execute the game component's on
     * paint event handler ({@link ScriptMonkey#ON_PAINT_EVENT_KEY}), if any,
     * and perform default expansion symbol drawing before returning the image.
     *
     * <p>
     * The target is a hint to the sheet as to how the result will be used.
     * Several methods in this class will take advantage of this hint
     * automatically, and it is also passed to {@link #paintSheet}.
     *
     * <p>
     * The resolution parameter determines the resolution of the result, in
     * pixels per inch. If -1, the resolution will be the default resolution for
     * the card, which is equal to {@link #getTemplateResolution()} multiplied
     * by {@link #getSuggestedUpsampleFactor()}.
     *
     * @param target the target hint to use for painting
     * @param resolution the resolution of the returned image, or -1 for the
     * sheet's default resolution
     * @return an image representing the content of the component face
     * represented by this sheet
     * @throws NullPointerException if target is <code>null</code>
     * @throws IllegalArgumentException if the resolution is less than 1 but not
     * the special default value (-1)
     * @throws ConcurrentModificationException if the sheet is already being
     * painted
     * @see #paintSheet
     * @see #applyContextHints
     */
    public final BufferedImage paint(RenderTarget target, double resolution) {
        // not intended to be thread safe, this is just to help detect issues,
        // not to guarantee that there are none
        if (drawLock) {
            throw new ConcurrentModificationException("already painting the sheet");
        }

        drawLock = true;
        try {
            if (target == null) {
                throw new NullPointerException("target");
            }
            if (resolution == -1d) {
                resolution = dpi * preferredUpsample;
            }
            if (resolution < 1d) {
                throw new IllegalArgumentException("resolution < 1: " + resolution);
            }

            dirtyCacheHint = target != activeTarget;
            activeTarget = target;

            final double requiredUpsample = resolution / dpi;
            if (upsampleFactor != requiredUpsample) {
                upsampleFactor = requiredUpsample;
                image = null;
                changeFlag = true;
                dirtyCacheHint = true;
            }

            if (changeFlag || dirtyCacheHint) { // i.e. hasChanged or target/resolution is different
                StrangeEons.setWaitCursor(true);
                try {
                    long nanos = 0L;
                    final boolean logPainting = StrangeEons.log.isLoggable(Level.INFO);
                    if (logPainting) {
                        nanos = System.nanoTime();
                    }

                    // if the card is transparent, we need to recreate the bitmap
                    // on each draw to prevent overdraw
                    if (isTransparent()) {
                        image = null;
                    }

                    if(isPrototype && image != null) {
                        fillPrototypeModeBackground(image);
                    }

                    paintSheet(target);
                    paintSheetOverlays();

                    if (isTransparent() && isVariableSize()) {
                        image = ImageUtilities.trim(image);
                    }

                    if (logPainting) {
                        nanos = System.nanoTime() - nanos;
                        StrangeEons.log.log(
                                Level.INFO, "rendered {0} in {1} ms at {2} dpi, {3} target", new Object[]{
                                    getGameComponent().getName(), nanos / 1000000d,
                                    getPaintingResolution(),
                                    target.toString()
                                });
                    }
                } finally {
                    changeFlag = false;
                    StrangeEons.setWaitCursor(false);
                }
            }
        } finally {
            drawLock = false;
        }
        return image;
    }

    // if rendering is not active, this tracks the target of the last request
    // to help set the dirtyCacheHint; while a rendering is active, this
    // stores the current target level; createGraphics uses this to set
    // the correct hints
    private RenderTarget activeTarget = null;
    // set during getSheetImage for cache invalidation check; true if image is null or previous target was different
    private boolean dirtyCacheHint;

    /**
     * Returns an image of the sheet content as it should appear when printed or
     * exported. If the component has a designed bleed margin (that is, if
     * {@link #getBleedMargin()} returns a positive value), then this is
     * equivalent to <code>paint( target, resolution )</code>. Otherwise, the
     * image returned by that method will be passed to
     * {@link #synthesizeBleedMargin(java.awt.image.BufferedImage, boolean, double)}
     * as a starting image, and the sheet will be given the opportunity to
     * modify the base image, or not, with a synthetic bleed margin. Note that
     * the method is called even if the parameter
     * <code>synthesizeBleedMargin</code> is <code>false</code>; some sheet
     * implementations may choose to decorate the image when a bleed margin is
     * not requested, for example, by adding rounded corners.
     *
     * @param target the target hint to use for painting
     * @param resolution the resolution of the returned image, or -1 for the
     * sheet's default resolution
     * @return an image representing the content of the component face
     * represented by this sheet
     * @throws NullPointerException if target is <code>null</code>
     * @throws IllegalArgumentException if the resolution is less than 1 but not
     * the special default value (-1)
     * @throws ConcurrentModificationException if the sheet is already being
     * painted
     * @see #paintSheet
     * @see #applyContextHints
     * @since 3.0.3680
     */
    public final BufferedImage paint(RenderTarget target, double resolution, boolean synthesizeBleedMargin) {
        BufferedImage bi = paint(target, resolution);
        if (getBleedMargin() <= 0d) {
            bi = synthesizeBleedMargin(bi, synthesizeBleedMargin, resolution);
        }
        return bi;
    }

    /**
     * Paints standard sheet overlays: on paint code and expansion symbols.
     */
    private void paintSheetOverlays() {
        Settings settings = gameComponent.getSettings();

        String onPaintSourceCode = settings.get(ScriptMonkey.ON_PAINT_EVENT_KEY);
        String expSymValue = settings.get(expsymKey + "-region");
        Rectangle expSymRegion = null;
        if (expSymValue != null && !expSymValue.isEmpty()) {
            try {
                if (expSymValue.charAt(0) == 'd') {
                    expSymRegion = settings.getRegion(expsymKey);
                } else {
                    expSymRegion = Settings.region(expSymValue);
                }
            } catch (ParseError e) {
            }
        }

        if (onPaintSourceCode == null && expSymRegion == null) {
            return;
        }

        Graphics2D g = createGraphics();
        try {
            applyContextHints(g);
            if (onPaintSourceCode != null) {
                //
                // NOTE: the use of != instead of !String.equals is intentional
                //
                if (onPaintSourceCode != cacheOnPaintSource || cacheOnPaintMonkey == null) {
                    cacheOnPaintMonkey = new ScriptMonkey(ScriptMonkey.ON_PAINT_EVENT_KEY);
                    cacheOnPaintMonkey.eval(onPaintSourceCode);
                    cacheOnPaintSource = onPaintSourceCode;
                }
                cacheOnPaintMonkey.call(ON_PAINT_EVENT_METHOD, g, gameComponent, this);
            }
            if (expSymRegion != null) {
                String gameCode = settings.get(Game.GAME_SETTING_KEY, null);
                if (gameCode != null) {
                    Game game = Game.get(gameCode);
                    if (game != null && game.getSymbolTemplate().isCustomDrawn()) {
                        return;
                    }
                }

                String expValue = settings.getExpansionCode();
                if (!expValue.equals("NX")) {
                    final Expansion expansion = Expansion.get(expValue);
                    Expansion[] expansions;
                    if (expansion != null) {
                        oneExp[0] = expansion;
                        expansions = oneExp;
                    } else {
                        expansions = parseExpansionList(expValue);
                    }

                    if (expansions == null || expansions.length == 0) {
                        return;
                    }

                    // determine the symbol variant
                    String variant = settings.get(Expansion.VARIANT_SETTING_KEY);
                    if (variant == null) {
                        variant = settings.get(expsymKey + '-' + expValue + "-invert");
                        if (variant == null) {
                            variant = settings.get(expsymKey + "-invert", "0");
                        }
                    }

                    // check for an expansion-specific region:
                    Rectangle rSpecialized = settings.getRegion(expsymKey + "-" + expValue);
                    if (rSpecialized != null) {
                        expSymRegion = rSpecialized;
                    }

                    // handle a single code as a special case
                    int targetWidth = Math.max(1, (int) (expSymRegion.width * getScalingFactor()));
                    if (expansions.length == 1) {
                        BufferedImage img = getExpansionSymbol(expansion, variant, targetWidth);
                        g.drawImage(img, expSymRegion.x, expSymRegion.y, expSymRegion.width, expSymRegion.height, null);
                    } else {
                        // count the number of non-null expansions
                        int expCount = 0;
                        for (int i = 0; i < expansions.length; ++i) {
                            if (expansions[i] != null) {
                                ++expCount;
                            }
                        }

                        float pointMargin = settings.getFloat(expsymKey + "-margin", 6f);
                        int pixelMargin = (int) ((pointMargin + getBleedMargin()) / 72f * getTemplateResolution() + 0.5f);
                        int gap = expSymRegion.width / 6;

                        // set the starting X-position:
                        //   center the row of icons over the middle of where the
                        //   single icon would have been
                        int totalWidth = expSymRegion.width * expCount + gap * (expCount - 1);
                        expSymRegion.x = (expSymRegion.x + expSymRegion.width / 2) - totalWidth / 2;
                        if (expSymRegion.x + totalWidth > (getTemplateWidth() - pixelMargin)) {
                            expSymRegion.x = getTemplateWidth() - pixelMargin - totalWidth;
                        }
                        if (expSymRegion.x < pixelMargin) {
                            expSymRegion.x = pixelMargin;
                        }

                        // if there are too many icons to fit in one row, shrink them
                        if (totalWidth > (getTemplateWidth() - pixelMargin * 2)) {
                            int space = getTemplateWidth() - pixelMargin * 2;
                            float scale = space / (float) totalWidth;
                            int oldHeight = expSymRegion.height;
                            expSymRegion.width = (int) (expSymRegion.width * scale);
                            expSymRegion.height = (int) (expSymRegion.height * scale);
                            expSymRegion.y += (oldHeight - expSymRegion.height) / 2;
                            gap = (int) (gap * scale);
                            totalWidth = expSymRegion.width * expCount + gap * (expCount - 1);
                            expSymRegion.x = pixelMargin + (space - totalWidth) / 2;
                        }

                        for (int i = 0; i < expansions.length; ++i) {
                            if (expansions[i] != null) {
                                BufferedImage img = getExpansionSymbol(expansions[i], variant, targetWidth);
                                g.drawImage(img, expSymRegion.x, expSymRegion.y, expSymRegion.width, expSymRegion.height, null);
                                expSymRegion.x += expSymRegion.width + gap;
                            }
                        }

                    }
                }
            }
        } finally {
            g.dispose();
        }
    }
    // an array to hold a single expansion, used in the common case when there
    // is only one expansion selected
    private Expansion[] oneExp = new Expansion[1];
    private String cacheOnPaintSource;
    private ScriptMonkey cacheOnPaintMonkey;

    /**
     * Marks this face as out of date. When any part of a component that is
     * drawn by this face changes, the component must call this method to
     * indicate that the sheet needs to be redrawn to reflect the changes.
     */
    public final void markChanged() {
        checkUnlocked();
        changeFlag = true;
    }

    /**
     * Returns <code>true</code> if the sheet is currently marked as out of
     * date.
     *
     * @return <code>true</code> if the {@link #markChanged()} has been called
     * since the last time the card was drawn
     */
    public final boolean hasChanged() {
        return changeFlag;
    }

    /**
     * Returns the game component that this sheet was created for.
     *
     * @return this sheet's game component
     */
    public final G getGameComponent() {
        return gameComponent;
    }

    /**
     * Returns <code>true</code> if the sheets for this card are transparent.
     * The base class returns <code>false</code>; subclasses that want to create
     * non-rectangular card faces must override this method. When this method
     * returns <code>true</code>, the framework guarantees the following:
     * <ol>
     * <li> the image buffer used for drawing will have an alpha channel (that
     * is, it will keep track of how opaque each pixel is)
     * <li> the image buffer used for drawing will be completely transparent
     * (alpha = 0) at the start of each call to {@link #paint}
     * </ol>
     *
     * @return <code>true</code> if the card face may contain transparent or
     * translucent areas
     */
    public boolean isTransparent() {
        return false;
    }

    /**
     * Returns <code>true</code> if the sheets created by this card can vary in
     * size. Subclasses that wish to create variably-sized sheets must override
     * this method to return <code>true</code>.
     *
     * <p>
     * Typically a variably-sized sheet is also transparent. When this is the
     * case, and the sheet image has one or more edges that are completely
     * transparent after {@link #paint} returns, then the transparent edges will
     * be trimmed from the outside of the sheet and the size adjusted
     * accordingly. Alternatively (or additionally), the size can be altered by
     * replacing the template image at the start of painting.
     *
     * @return true if this card can have different sizes
     * @see #isTransparent
     * @see #replaceTemplateImage
     */
    public boolean isVariableSize() {
        return false;
    }

    /**
     * Returns the name of the settings key that will be used for the default
     * expansion painting mechanism.
     *
     * @return the key used for expansion symbol painting
     */
    public final String getExpansionSymbolKey() {
        return expsymKey;
    }

    /**
     * Sets the name of the settings key that will be used by the default
     * expansion symbol painting mechanism. Subclasses can call this during
     * {@link #paintSheet} to modify the default expansion symbol painting
     * algorithm in response to the component state.
     *
     * @param expsymKey the name of the key to use for default expansion symbol
     * painting
     * @see #getExpansionSymbolKey()
     */
    protected final void setExpansionSymbolKey(String expsymKey) {
        this.expsymKey = expsymKey;
    }

    /**
     * A deck snapping hint is used the describe the general behaviour of a
     * sheet when it is placed in a deck and an attempt is made to snap it
     * against another object. The hint values represent different basic roles
     * that object might play in a deck.
     */
    public static enum DeckSnappingHint {
        /**
         * The default hint value. Cards snap to other cards, and also to the
         * page grid.
         */
        CARD,
        /**
         * Overlays do not snap to anything by default. They are typically used
         * for decorations that may be placed anywhere over other deck objects.
         */
        OVERLAY,
        /**
         * Inlays snap to each other and to tiles. When snapped to tiles, they
         * snap to the inside edge rather the outside. A collection of inlays is
         * typically used together to build up a more complex structure overtop
         * (inside of) a tile.
         */
        INLAY,
        /**
         * Tiles snap to other tiles and the page grid, and are snapped into by
         * inlays. They are typically used to lay down the basic blueprint or
         * background for a game board, with inlays placed overtop.
         */
        TILE,
        /**
         * This class can be used for objects that don't fit any of the above
         * categories. By default, these objects snap only to each other.
         */
        OTHER;

        /**
         * Applies this hint to the snapping behaviour of a page item.
         *
         * @param item
         */
        public void apply(PageItem item) {
            item.setSnapClass(getDefaultSnapClass());
            item.setSnapTarget(getDefaultSnapTarget());
            item.setClassesSnappedTo(getDefaultClassesSnappedTo());
        }

        /**
         * Returns the default snap class assigned to page items with this hint.
         *
         * @return the snap class for this hint
         * @see
         * PageItem#setSnapClass(ca.cgjennings.apps.arkham.deck.item.PageItem.SnapClass)
         */
        public PageItem.SnapClass getDefaultSnapClass() {
            PageItem.SnapClass sc;
            switch (this) {
                case CARD:
                    sc = PageItem.SnapClass.SNAP_CARD;
                    break;
                case OVERLAY:
                    sc = PageItem.SnapClass.SNAP_OVERLAY;
                    break;
                case INLAY:
                    sc = PageItem.SnapClass.SNAP_INLAY;
                    break;
                case TILE:
                    sc = PageItem.SnapClass.SNAP_TILE;
                    break;
                case OTHER:
                    sc = PageItem.SnapClass.SNAP_OTHER;
                    break;
                default:
                    throw new AssertionError();
            }
            return sc;
        }

        /**
         * Returns the default snap target assigned to page items with this
         * hint.
         *
         * @return the snap class for this hint
         * @see
         * PageItem#setSnapTarget(ca.cgjennings.apps.arkham.deck.item.PageItem.SnapTarget)
         */
        public PageItem.SnapTarget getDefaultSnapTarget() {
            PageItem.SnapTarget st;
            switch (this) {
                case INLAY:
                    st = PageItem.SnapTarget.TARGET_MIXED;
                    break;
                default:
                    st = PageItem.SnapTarget.TARGET_OUTSIDE;
                    break;
            }
            return st;
        }

        /**
         * Returns the default snap classes snapped to by page items with this
         * hint.
         *
         * @return the default classes to snap to for this hint
         * @see PageItem#setClassesSnappedTo(java.util.EnumSet)
         */
        public EnumSet<PageItem.SnapClass> getDefaultClassesSnappedTo() {
            EnumSet<PageItem.SnapClass> cst;
            switch (this) {
                case CARD:
                    cst = DEFAULT_SNAPTO;
                    break;
                case INLAY:
                    cst = TILE_SNAPTO;
                    break;
                case OVERLAY:
                    cst = PageItem.SnapClass.SNAP_SET_NONE;
                    break;
                case TILE:
                    cst = TILE_SNAPTO;
                    break;
                default:
                    cst = OTHER_SNAPTO;
                    break;
            }
            return cst.clone();
        }

        private static final EnumSet<PageItem.SnapClass> DEFAULT_SNAPTO = EnumSet.of(
                PageItem.SnapClass.SNAP_CARD, PageItem.SnapClass.SNAP_PAGE_GRID
        );
        private static final EnumSet<PageItem.SnapClass> TILE_SNAPTO = EnumSet.of(
                PageItem.SnapClass.SNAP_TILE, PageItem.SnapClass.SNAP_PAGE_GRID
        );
        private static final EnumSet<PageItem.SnapClass> INLAY_SNAPTO = EnumSet.of(
                PageItem.SnapClass.SNAP_TILE, PageItem.SnapClass.SNAP_INLAY, PageItem.SnapClass.SNAP_PAGE_GRID
        );
        private static final EnumSet<PageItem.SnapClass> OTHER_SNAPTO = EnumSet.of(
                PageItem.SnapClass.SNAP_OTHER
        );
    };

    /**
     * Returns a hint describing how this sheet should behave when snapped in a
     * deck. The default is <code>CARD</code>, meaning that the sheet behaves
     * like one face of a playing card.
     *
     * @return a hint describing the default behaviour of this face when it is
     * snapped against other objects in a deck
     */
    public DeckSnappingHint getDeckSnappingHint() {
        return DeckSnappingHint.CARD;
    }

    /**
     * Returns the size of the margin around the component edge that should be
     * cropped off, measured in points. (The bleed margin will be the same on
     * all sides.) This allows you to create components with an extra margin
     * that allows for slight misalignment when cutting. The height of the card
     * after cutting will be the height of the card produced by the sheet, less
     * twice this margin. Likewise for the width. If {@link #hasCropMarks()}
     * returns <code>true</code>, then the automatic crop marks will be moved
     * toward the inside of this card by an amount equal to the bleed margin.
     * <p>
     * In the example below, the actual card to be cut and kept is indicated by
     * the blank area, while the X'd area indicates the bleed margin. Card
     * content covers the entire area, including the bleed margin, but nothing
     * important should appear in the bleed margin or within a distance about
     * the same size as the bleed margin on the card interior.
     *
     * <pre>
     * XXXXXXXXXXXXXXXXXX
     * XX              XX
     * X                X
     * X                X
     * X     Actual     X
     * X      Card      X
     * X     Content    X
     * X       (tm)     X
     * X                X
     * X                X
     * X                X
     * XX              XX
     * XXXXXXXXXXXXXXXXXX
     * </pre>
     *
     * <p>
     * The base class defines a margin of 0.
     *
     * @return the size of the bleed margin, in points (1 point = 1/72 inch)
     * @see #hasCropMarks
     */
    public double getBleedMargin() {
        return 0d;
    }

    /**
     * Returns true if this sheet should have automatic crop and fold marks
     * added when printed or placed in a deck. If this method returns
     * <code>true</code>, then crop marks will be created automatically around
     * the edges of the face; the bleed margin of these marks is determined by
     * {@link #getBleedMargin()}. In certain circumstances, some of these crop
     * marks will be converted automatically into fold marks. Typically, this
     * happens when the front and back face of a card are snapped next to each
     * other such that folding along the line indicated by the crop mark would
     * produce a complete two-sided card.
     *
     * <p>
     * The base class implementation returns <code>true</code>.
     *
     * @return <code>true</code> if the deck should generate crop marks
     */
    public boolean hasCropMarks() {
        return true;
    }

    /**
     * Returns <code>true</code> if this sheet should have special fold marks
     * added when printed. When this returns <code>true</code>, one or more fold
     * marks will be shown at locations determined by {@link #getFoldMarks}.
     * This can be used to produce complex 3D components that require assembly
     * before use.
     *
     * <p>
     * Note that the fold marks produced by this method are completely
     * independent of the marks that are sometimes generated automatically from
     * crop marks (see {@link #hasCropMarks()}).
     *
     * <p>
     * The base class returns <code>false</code>.
     */
    public boolean hasFoldMarks() {
        return false;
    }

    /**
     * Returns an array that describes the extra fold marks that should appear
     * on this face when it is placed in a deck. This method is only called if
     * {@link #hasFoldMarks} returns <code>true</code>. It should return an
     * array of double values. Each fold mark to be added by the deck is
     * described by a sequence of four doubles in the array. The first pair is
     * the location of a start point for the fold mark (in x, y order). The
     * location is relative to the width and height of the face, so for example
     * the pair (0.5, 0) would start a fold mark in the center of the top edge.
     * The second pair is a unit vector that describes the direction that the
     * fold mark should extend from the start point. (Recall that a unit vector
     * is a vector of length one, that is, sqrt(x<sup>2</sup>+y<sup>2</sup>) ==
     * 1.)
     * <p>
     * Fold marks typically come in pairs, one on each side of the face. For
     * example, the following fold mark array would indicate the face should be
     * folded in half along the vertical axis by placing fold marks at the
     * centers of the top and bottom edges:
     * <pre>[0.5, 0,  0, -1,     0.5, 1,  0, 1]</pre>
     *
     * <p>
     * The base class returns <code>null</code>.
     *
     * @return an array of quadruples that describe the location and angle of
     * additional fold marks to be drawn around the component
     */
    public double[] getFoldMarks() {
        return null;
    }

    /**
     * Return the printed size of the sheet, measured in points.
     *
     * @return the dimensions of the bounding rectangle of this sheet when
     * printed
     */
    public PrintDimensions getPrintDimensions() {
        if (!isVariableSize()) {
            return new PrintDimensions(template, getTemplateResolution());
        }

        throw new UnsupportedOperationException("called base class getPrintDimensions() implementation on variable-size component: " + this);
    }

    /**
     * Returns the setting key that defines the template image for this face.
     *
     * @return the template key used to initialize this face
     */
    public final String getTemplateKey() {
        if (templateKey == null) {
            throw new IllegalStateException("initializeResources has not been called");
        }
        return templateKey;
    }

    /**
     * Returns the template image; this will be the image determined by the
     * template key unless the image was changed by calling
     * {@link #replaceTemplateImage}.
     *
     * @return the current template image that
     */
    public final BufferedImage getTemplateImage() {
        return template;
    }

    /**
     * Replaces the template image for the sheet. This can be used simply to
     * substitute a different background graphic, or, for variably-sized faces,
     * it can be used to make the sheet larger than its original size. Replacing
     * the template image can only be safely done within {@link #paint}
     * <i>before</i> calling {@link #createGraphics} because calling it may
     * invalidate the internal sheet image buffer.
     *
     * @param newTemplate the new template image
     * @throws NullPointerException if the new template is <code>null</code>
     * @see #createGraphics(java.awt.image.BufferedImage, boolean, boolean)
     */
    protected final void replaceTemplateImage(BufferedImage newTemplate) {
        if (template != newTemplate) {
            if (newTemplate == null) {
                throw new NullPointerException("newTemplate");
            }
            if (image != null) {
                if ((template.getWidth() != newTemplate.getWidth()) || (template.getHeight() != newTemplate.getHeight())) {
                    image = null;
                }
            }
            template = newTemplate;
            changeFlag = true;
        }
    }

    /**
     * Returns the width of the template image used by this sheet.
     *
     * @return the width of the template image
     * @see #getTemplateHeight
     * @see #getTemplateResolution
     */
    public int getTemplateWidth() {
        return template.getWidth();
    }

    /**
     * Returns the height of the template image used by this sheet.
     *
     * @return the height of the template image
     * @see #getTemplateWidth
     * @see #getTemplateResolution
     */
    public int getTemplateHeight() {
        return template.getHeight();
    }

    /**
     * Returns the base resolution of the template image, in pixels per inch
     * (ppi). The base resolution is determined from the value of the
     * <code><i>templateKey</i>-dpi</code> key. If this key is not defined, the
     * resolution defaults to 150 ppi. The template resolution, together with
     * the its width and height, determine the physical size of the template
     * image. Unless the sheet is variably-sized, this is also the physical size
     * of the sheet as a whole.
     *
     * @return the resolution of the template image, in pixels per inch
     */
    public final double getTemplateResolution() {
        return dpi;
    }

    /**
     * Returns the current scaling factor; this is the ratio of the resolution
     * the sheet is being painted at to the template resolution. If called when
     * the sheet is not being painted, it returns the value that was used during
     * the last paint request.
     *
     * @return the scaling factor for the painting resolution
     */
    public final double getScalingFactor() {
        return upsampleFactor;
    }

    /**
     * Returns the resolution that the sheet is being painted at, in pixels per
     * inch. If called when the sheet is not being painted, returns the value
     * that was active during the last paint request.
     */
    public final double getPaintingResolution() {
        return dpi * upsampleFactor;
    }

    /**
     * Returns a suggested scaling factor to use when previewing the image
     * onscreen. Typically, this is equal to 1, but some components with small
     * text size and/or small to medium template resolution will set this to a
     * higher value to make onscreen previews more legible.
     *
     * @return a suggested multiplier for the template resolution when
     * previewing onscreen
     * @see #initializeTemplate
     * @see #getTemplateResolution
     */
    public double getSuggestedUpsampleFactor() {
        return preferredUpsample;
    }

    //
    // PAINTING SPECIFIC
    //
    /**
     * Creates a graphics context that can be used to draw this sheet. The
     * context will be scaled for the current resolution and set up
     * appropriately for the current rendering target. This is a convenience
     * method equivalent to calling
     * <code>createGraphics( null, true, true )</code>.
     *
     * <p>
     * <b>Safe only when painting</b>
     *
     * @return a context suitable for drawing the sheet
     */
    public Graphics2D createGraphics() {
        return createGraphics(null, true, true);
    }

    /**
     * This method creates a graphics context for drawing sheet graphics. If the
     * <code>bufferSource</code> is <code>null</code>, then the returned
     * graphics context will draw to this sheet's internal sheet image buffer.
     * Alternatively, passing in a <code>BufferedImage</code> of your choice
     * will create a graphics context for that image as if by calling
     * <code>bufferSource.createGraphics()</code>, and then apply scaling and
     * hints as described below. This allows you to create intermediate images
     * as part of the rendering process.
     *
     * <p>
     * If <code>scaleForResolution</code> is <code>true</code>, the graphics
     * context will be scaled so that 1 unit in the context is equal to 1 pixel
     * in the template image. This assumes that the passed-in image has been
     * scaled according to the upsample factor. (The internal sheet image buffer
     * is always correctly scaled, as will an image obtained by calling
     * {@link #createTemporaryImage(int, int, boolean)}.) If the context is not
     * scaled now, it can be scaled later by calling {@link #applyContextScale}.
     *
     * <p>
     * If <code>applyHints</code> is <code>true</code>, then the graphics
     * context's rendering settings will be initialized for the active target as
     * if by calling {@link #applyContextHints}.
     *
     * <p>
     * The caller is responsible for disposing of the returned graphics context.
     * To ensure that the context is always disposed of properly, the drawing
     * code should be placed in a <code>try</code> block, and
     * <code>dispose()</code> called in the <code>finally</code> clause, e.g.:
     * <pre>
     * Graphics2D g = sheet.createGraphics( null, true, true );
     * try {
     *     // paint the sheet
     * } finally {
     *     g.dispose();
     * }
     * </pre>
     *
     * <p>
     * <b>Safe only when painting</b>
     *
     * @param bufferSource the image to create a context for, or
     * <code>null</code> to draw on the sheet image
     * @param scaleForResolution if <code>true</code>, the context is scaled to
     * suit the current resolution
     * @param applyHints if <code>true</code>, rendering hints suited to the
     * current target will be applied to the context
     * @return a graphics context for the requested destination, optionally
     * scaled and hinted
     * @see #applyContextScale(java.awt.Graphics2D)
     * @see #applyContextHints(java.awt.Graphics2D)
     */
    public Graphics2D createGraphics(BufferedImage bufferSource, boolean scaleForResolution, boolean applyHints) {
        checkLocked();

        if (bufferSource == null) {
            if (image == null) {
                recreateImageBuffer();
            }
            bufferSource = image;
        }

        Graphics2D g = bufferSource.createGraphics();

        if (scaleForResolution && upsampleFactor != 1d) {
            applyContextScale(g);
        }

        if (applyHints) {
            applyContextHints(g);
        }

        if(isPrototype) {
            g = new PrototypingGraphics2D(g);
        }

        return g;
    }

    private void recreateImageBuffer() {
        int width = (int) (template.getWidth() * upsampleFactor);
        int height = (int) (template.getHeight() * upsampleFactor);

        if (width <= 0) {
            width = 1;
        }
        if (height <= 0) {
            height = 1;
        }

        image = new BufferedImage(
                width, height,
                (isTransparent() && !isPrototype) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB
        );

        if(isPrototype) {
            fillPrototypeModeBackground(image);
        }
    }

    /**
     * Prepares a graphics context to render a sheet by setting up the context's
     * rendering hints. The specific hints set depend on the current rendering
     * target.
     *
     * <p>
     * When implementing {@link #paintSheet}, subclasses may wish to perform
     * some setup operations with different rendering settings before the main
     * drawing process begins. In such cases, perform the initial drawing steps
     * before calling this method. Otherwise, call this method at the start of
     * drawing. Subclasses may also wish to override this method to customize
     * hinting; the base class simply calls
     * {@link RenderTarget#applyTo(java.awt.Graphics2D)}.
     *
     * <p>
     * <b>Safe only when painting</b>
     *
     * @param g the graphics context to modify
     * @see #createGraphics(java.awt.image.BufferedImage, boolean, boolean)
     * @see #createTemporaryImage(int, int, boolean)
     */
    protected void applyContextHints(Graphics2D g) {
        activeTarget.applyTo(g);
    }

    /**
     * Scales a graphics context to reflect the requested drawing resolution.
     *
     * <p>
     * <b>Safe only when painting</b>
     *
     * @param g the context to scale
     * @see #createGraphics(java.awt.image.BufferedImage, boolean, boolean)
     */
    protected void applyContextScale(Graphics2D g) {
        g.scale(upsampleFactor, upsampleFactor);
    }

    /**
     * Returns a view of the internal sheet image buffer as a
     * {@link BufferedImage} object. To ensure that the sheet image remains
     * consistent, do not manipulate the pixels in this image while there is
     * also an active graphics context on the image buffer. (Dispose of the
     * graphics context before using the image, and create a new graphics
     * context afterward if you wish to continue drawing.)
     *
     * <p>
     * Note that the returned image buffer will vary in size depending on the
     * current drawing resolution.
     *
     * <p>
     * If the internal image buffer is invalid, it will be validated before this
     * method returns.
     *
     * <p>
     * <b>Safe only when painting</b>
     *
     * @return a view of the internal buffer as an image
     * @see #createGraphics
     */
    protected final BufferedImage getDestinationBuffer() {
        checkLocked();

        if (image == null) {
            recreateImageBuffer();
        }
        return image;
    }

    /**
     * Creates a new temporary image to use while painting a sheet face. This
     * can be used to hold intermediate results while processing or to cache
     * results to speed up future drawing. The width and height are specified
     * relative to the template image. The dimensions of the actual image will
     * be scaled for the current drawing resolution. (To get a suitable graphics
     * context for the image, use
     * <code>createGraphics( tempImage, true, true )</code>.) Passing 0 for
     * either the width or height is equivalent to using
     * <code>getTemplateWidth()</code> or <code>getTemplateHeight()</code>,
     * respectively.
     *
     * <p>
     * <b>Safe only when painting</b>
     *
     * @param templateWidth the width of the area of the template image to
     * cover, or 0 to cover the entire width
     * @param templateHeight the height of the area of the template image to
     * cover, or 0 to cover the entire height
     * @param includeAlphaChannel if <code>true</code>, the temporary image will
     * have an alpha channel (pixels can be translucent or transparent)
     * @return an image that can be used to render temporary results that cover
     * a region that is <code>templateWidth</code> by
     * <code>templateHeight</code> pixels on the template image
     * @throws IllegalArgumentException if the given width or height is less
     * than one
     */
    public BufferedImage createTemporaryImage(int templateWidth, int templateHeight, boolean includeAlphaChannel) {
        if (templateWidth < 0) {
            throw new IllegalArgumentException("templateWidth < 0: " + templateWidth);
        }
        if (templateHeight < 0) {
            throw new IllegalArgumentException("templateHeight < 0: " + templateHeight);
        }
        if (templateWidth == 0) {
            templateWidth = template.getWidth();
        }
        if (templateHeight == 0) {
            templateHeight = template.getHeight();
        }

        int width = (int) (templateWidth * upsampleFactor);
        int height = (int) (templateHeight * upsampleFactor);

        if (width <= 0) {
            width = 1;
        }
        if (height <= 0) {
            height = 1;
        }

        return new BufferedImage(width, height, includeAlphaChannel ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
    }

    /**
     * Returns a hint value to help manage partial rendering caches.
     * Sophisticated sheet implementations can speed up the rendering of complex
     * areas by rendering once to a buffer and then using this buffer to paint
     * results on subsequent calls. This buffer can become out of date in two
     * ways: if the relevant game component attributes change, or if the
     * requested target or resolution change. This method tests the second
     * condition: during a call to {@link #paint}, if this method returns
     * <code>true</code> then the target or resolution have changed since the
     * last call.
     *
     * <p>
     * <b>Safe only when painting</b>
     *
     * @return <code>true</code> if the target or resolution may have changed
     * since the last time the sheet was rendered
     */
    public final boolean isCachedTemporaryImageInvalid() {
        return dirtyCacheHint;
    }

    /**
     * Paints the content of this sheet. This method will be called by the
     * framework whenever the sheet needs to be redrawn.
     *
     * <p>
     * A typical procedure to set up for painting is to call
     * {@link #createGraphics()} to get a graphics context for the sheet's
     * internal image buffer. The returned context will be scaled so that 1 unit
     * in the graphics context is equal to 1 pixel in the template image, and
     * the context will be configured appropriately for the current rendering
     * target. In some cases you may wish to perform these steps in stages, in
     * which case you can use {@link #createGraphics(java.awt.image.BufferedImage, boolean, boolean)},
     * {@link #applyContextHints(java.awt.Graphics2D)} and
     * {@link #applyContextScale(java.awt.Graphics2D)}.
     *
     * @param target the rendering target
     */
    abstract protected void paintSheet(RenderTarget target);

    /**
     * Returns the size of the synthetic bleed margin produced by this sheet, in
     * points.
     *
     * @return the size of the synthetic bleed margin, or zero
     * @see #synthesizeBleedMargin
     */
    public double getSyntheticBleedMargin() {
        return isTransparent() ? 0d : 9d;
    }

    /**
     * Generates a simulated bleed margin for components that do not provide
     * one. The method is called with an already-rendered sheet image to which a
     * margin should be added. If the component type does not allow a synthetic
     * bleed margin, or if the margin size is zero, the original image may be
     * returned. Otherwise, it should create a new image, larger than the
     * original, that contains the original image in the center and the desired
     * margin around the outside.
     *
     * <p>
     * If the sheet has an explicit bleed margin, as indicated by
     * {@link #getBleedMargin()} returning a non-zero value, then that margin is
     * built into the component design. The explicit margin should be respected,
     * and this method should not be called.
     *
     * <p>
     * The base class implementation will synthesize an adequate 9&nbsp;pt bleed
     * margin for a wide variety of components. It operates as follows:
     * <ul>
     * <li> If the sheet {@linkplain #isTransparent() is transparent}, then the
     * sheet image is returned unmodified.
     * <li> If, after sampling a few pixels around the outside of the image, the
     * sheet appears to have a border of a single solid colour, then that border
     * is extended on all sides by the margin size. If the sheet has rounded
     * corners, these are painted over.
     * <li> Otherwise, a margin is synthesized by mirroring the sheet image into
     * the surrounding space.
     * </ul>
     *
     * @param sheetImage the image, previously returned from
     * {@link #paint(ca.cgjennings.apps.arkham.sheet.RenderTarget, double) paint}
     * to synthesize a margin for
     * @param synthesize if <code>true</code>, then the border should be
     * synthesized; if <code>false</code>, subclasses may decorate the the image
     * to accordingly (for example, by painting on rounded corners)
     * @param resolution the resolution, in pixels per inch, of the sheet image
     * @return a new image with the requested margin, or
     */
    protected BufferedImage synthesizeBleedMargin(BufferedImage sheetImage, boolean synthesize, double resolution) {
        final double margin = getSyntheticBleedMargin();
        if (!synthesize || margin == 0d) {
            return sheetImage;
        }

        final int w = sheetImage.getWidth();
        final int h = sheetImage.getHeight();
        final int m = Math.min((int) Math.ceil(margin / 72d * resolution), Math.min(w, h));
        final int m2 = m * 2;

        // heuristic to infer if the card has a solid border
        boolean solid = false;
        int rgb = sheetImage.getRGB(w / 2, 0);
        if (similar(rgb, sheetImage.getRGB(w / 2, h - 1))) {
            if (similar(rgb, sheetImage.getRGB(0, h / 3))) {
                if (similar(rgb, sheetImage.getRGB(w - 1, h / 3))) {
                    if (similar(rgb, sheetImage.getRGB(0, h * 2 / 3))) {
                        if (similar(rgb, sheetImage.getRGB(w - 1, h * 2 / 3))) {
                            solid = true;
                        }
                    }
                }
            }
        }

        BufferedImage bi = new BufferedImage(sheetImage.getWidth() + m2, sheetImage.getHeight() + m2, sheetImage.getType());
        Graphics2D g = bi.createGraphics();
        try {
            if (solid) {
                g.setColor(new Color(rgb, true));
                g.fillRect(0, 0, m, h + m2);
                g.fillRect(w + m, 0, m, h + m2);
                g.fillRect(m, 0, w, m);
                g.fillRect(m, h + m, w, m);

                g.drawImage(sheetImage, m, m, null);

                // fill corners
                final int w1 = w - 1;
                final int h1 = h - 1;
                boolean fillCorners = false;
                if (!similar(rgb, sheetImage.getRGB(0, 0)) && !similar(rgb, sheetImage.getRGB(w1, 0)) && !similar(rgb, sheetImage.getRGB(0, h1)) && !similar(rgb, sheetImage.getRGB(w1, h1))) {
                    fillCorners = true;
                }
                if (fillCorners) {
                    int j;
                    int[] xp = new int[3];
                    int[] yp = new int[3];
                    g.translate(m, m);

                    j = findInset(sheetImage, rgb, 0, 0, 1, 0);
                    xp[1] = j;
                    j = findInset(sheetImage, rgb, 0, 0, 0, 1);
                    yp[2] = j;
                    g.fillPolygon(xp, yp, 3);

                    yp[0] = h;
                    j = findInset(sheetImage, rgb, 0, h1, 0, -1);
                    xp[1] = 0;
                    yp[1] = j;
                    j = findInset(sheetImage, rgb, 0, h1, 1, 0);
                    xp[2] = j;
                    yp[2] = h;
                    g.fillPolygon(xp, yp, 3);

                    xp[0] = w;
                    xp[1] = w;
                    j = findInset(sheetImage, rgb, w1, h1, 0, -1);
                    yp[1] = j;
                    j = findInset(sheetImage, rgb, w1, h1, -1, 0);
                    xp[2] = j;
                    g.fillPolygon(xp, yp, 3);

                    yp[0] = 0;
                    j = findInset(sheetImage, rgb, w1, 0, -1, 0);
                    xp[1] = j;
                    yp[1] = 0;
                    j = findInset(sheetImage, rgb, w1, 0, 0, 1);
                    xp[2] = w;
                    yp[2] = j;
                    g.fillPolygon(xp, yp, 3);
                }
            } else {
                g.drawImage(sheetImage, m - w, m - h, m, m, w, h, 0, 0, null);
                g.drawImage(sheetImage, m, m - h, m + w, m, 0, h, w, 0, null);
                g.drawImage(sheetImage, m + w, m - h, m + w + w, m, w, h, 0, 0, null);

                g.drawImage(sheetImage, m - w, m, m, m + h, w, 0, 0, h, null);
                g.drawImage(sheetImage, m + w, m, m + w + w, m + h, w, 0, 0, h, null);

                g.drawImage(sheetImage, m - w, m + h, m, m + h + h, w, h, 0, 0, null);
                g.drawImage(sheetImage, m, m + h, m + w, m + h + h, 0, h, w, 0, null);
                g.drawImage(sheetImage, m + w, m + h, m + w + w, m + h + h, w, h, 0, 0, null);

                g.drawImage(sheetImage, m, m, null);
            }
        } finally {
            g.dispose();
        }
        return bi;
    }

    private static int findInset(BufferedImage bi, int rgb, int x0, int y0, int dx, int dy) {
        int limit = Math.min(bi.getWidth(), bi.getHeight()) / 3;
        for (int i = 0; i < limit; ++i) {
            if (similar(rgb, bi.getRGB(x0, y0))) {
                break;
            }
            x0 += dx;
            y0 += dy;
        }
        return dx == 0 ? y0 : x0;
    }

    private static boolean similar(int rgb1, int rgb2) {
        int da = (rgb1 >>> 24) - (rgb2 >>> 24);
        int dr = ((rgb1 >> 16) & 0xff) - ((rgb2 >> 16) & 0xff);
        int dg = ((rgb1 >> 8) & 0xff) - ((rgb2 >> 8) & 0xff);
        int db = (rgb1 & 0xff) - (rgb2 & 0xff);

        return (da * da + dr * dr + dg * dg + db * db) <= MAX_DIST;
    }

    private static int MAX_DIST = 36;

    /**
     * Releases cached resources used in sheet drawing, freeing up memory for
     * other purposes.
     */
    public void freeCachedResources() {
        checkUnlocked();
        image = null;
    }

    //
    // DRAWING HELPER METHODS
    //
    public static void doStandardRendererInitialization(MarkupRenderer r) {
        Settings settings = Settings.getShared();

        r.setMarkBadBox(settings.getYesNo("highlight-bad-boxes"));
        r.setScalingLimit(settings.getDouble("min-text-scaling-factor"));
        r.setTightnessLimit(settings.getFloat("min-text-spacing-factor"));
        r.setScalingFractionalLimit(settings.getInt("text-scaling-precision"));
        r.setTextFitting(settings.getTextFittingMethod("default"));
    }

    public static void setNamesForRenderer(MarkupRenderer r, String first, String last, String fullname) {
        if (first == null) {
            first = "";
        }
        if (last == null) {
            last = first;
        }
        if (fullname == null) {
            fullname = first;
        }

        first = first.trim();
        last = last.trim();
        int spacePos = first.indexOf(' ');
        if (spacePos >= 0) {
            first = first.substring(0, spacePos);
        }
        r.setReplacementForTag("name", first);
        r.setReplacementForTag("lastname", last);
        r.setReplacementForTag("fullname", fullname);
    }

    /**
     * Draws a magenta outline of the specified rectangle if layout debugging
     * has been enabled. If the rectangle is <code>null</code> or has an area of
     * zero, a warning will be logged and nothing will be drawn.
     *
     * @param g the sheet graphics context to draw on
     * @param region the rectangle to highlight
     */
    public final void drawRegionBox(Graphics2D g, Rectangle region) {
        if (MarkupRenderer.DEBUG) {
            if (region == null) {
                StrangeEons.log.log(Level.WARNING, "null region rectangle", new NullPointerException());
            } else if (region.width == 0 || region.height == 0) {
                StrangeEons.log.log(Level.WARNING, "zero area rectangle", new AssertionError());
            } else {
                Paint p = g.getPaint();
                g.setPaint(Color.MAGENTA);
                g.drawRect(region.x, region.y, region.width, region.height);
                g.setPaint(p);
            }
        }
    }

    /**
     * Constant for left alignment of text when using the drawing helper
     * methods.
     */
    public static final int ALIGN_LEFT = -1;
    /**
     * Constant for center alignment of text when using the drawing helper
     * methods.
     */
    public static final int ALIGN_CENTER = 0;
    /**
     * Constant for right alignment of text when using the drawing helper
     * methods.
     */
    public static final int ALIGN_RIGHT = 1;
    /**
     * Constant for alignment of text to normal reading order: left if the game
     * language is written left-to-right, and right otherwise.
     */
    public static final int ALIGN_LEADING;
    /**
     * Constant for alignment of text to reverse reading order: right if the
     * game language is written left-to-right, and left otherwise.
     */
    public static final int ALIGN_TRAILING;

    static {
        ComponentOrientation co = ComponentOrientation.getOrientation(Language.getGameLocale());
        if (co.isLeftToRight()) {
            ALIGN_LEADING = -1;
            ALIGN_TRAILING = 1;
        } else {
            ALIGN_LEADING = 1;
            ALIGN_TRAILING = -1;
        }
    }

    /**
     * Draws text within a region. The text will be centered vertically in the
     * region. The horizontal alignment is determined by the alignment
     * parameter, which can be any of {@link Sheet#ALIGN_LEFT},
     * {@link Sheet#ALIGN_CENTER}, {@link Sheet#ALIGN_RIGHT},
     * {@link Sheet#ALIGN_LEADING}, or {@link Sheet#ALIGN_TRAILING}. The text is
     * drawn as a single line and it does not interpret markup tags.
     *
     * @param g the graphics context to use for drawing
     * @param text the text to draw
     * @param region the region to draw the text within
     * @param font the font to use for the text
     * @param size the point size to use for the text
     * @param alignment the horizontal alignment of the text within the
     * rectangle
     */
    public void drawTitle(Graphics2D g, String text, Rectangle region, Font font, float size, int alignment) {
        Font f = font.deriveFont(size * (float) dpi / 72f);
        FontMetrics fm = g.getFontMetrics(f);

        if (MarkupRenderer.DEBUG) {
            drawRegionBox(g, region);
        }

        int x;
        if (alignment < 0) {
            x = region.x;
        } else if (alignment == 0) {
            x = region.x + (region.width - fm.stringWidth(text)) / 2;
        } else {
            x = region.x + region.width - fm.stringWidth(text);
        }
        int y = region.y + fm.getAscent() + (region.height - fm.getHeight()) / 2;

        g.setFont(f);
        g.drawString(text, x, y);
    }

    /**
     * Draws text within a region; if the text is wider than the region, it will
     * be scaled down to fit. The text will be centered vertically and the
     * horizontal alignment is determined by the alignment parameter, which can
     * be any of {@link Sheet#ALIGN_LEFT},
     * {@link Sheet#ALIGN_CENTER}, {@link Sheet#ALIGN_RIGHT},
     * {@link Sheet#ALIGN_LEADING}, or {@link Sheet#ALIGN_TRAILING}. The text is
     * drawn as a single line and it does not interpret markup tags.
     *
     * @param g the graphics context to use for drawing
     * @param text the text to draw
     * @param region the region to draw the text within
     * @param font the font to use for the text
     * @param maxSize the point size to use for the text
     * @param alignment the horizontal alignment of the text within the
     * rectangle
     */
    public void fitTitle(Graphics2D g, String text, Rectangle region, Font font, float maxSize, int alignment) {
        Font f = font.deriveFont(maxSize * (float) dpi / 72f);
        FontMetrics fm = g.getFontMetrics(f);

        if (MarkupRenderer.DEBUG) {
            drawRegionBox(g, region);
        }

        int textWidth = fm.stringWidth(text);
        if (textWidth > region.width) {
            float scale = region.width / (float) fm.stringWidth(text);
            f = font.deriveFont(scale * maxSize * (float) dpi / 72f);
            fm = g.getFontMetrics(f);
            textWidth = fm.stringWidth(text);
        }

        g.setFont(f);
        int x = region.x;
        if (alignment == ALIGN_CENTER) {
            x += (region.width - textWidth) / 2;
        } else if (alignment >= ALIGN_RIGHT) {
            x += region.width - textWidth;
        }
        int y = region.y + fm.getAscent() + (region.height - fm.getHeight()) / 2;
        g.drawString(text, x, y);
    }

    /**
     * Draws text within a region. The text is drawn as if by {@link #fitTitle},
     * but each glyph (drawn character) will be outlined. The text is drawn as a
     * single line and it does not interpret markup tags.
     *
     * @param g the graphics context to use for drawing
     * @param text the text to draw
     * @param region the region to draw the text within
     * @param font the font to use for the text
     * @param maxSize the point size to use for the text
     * @param outlineSize the point size to use for the outline
     * @param textColor the colour of the text
     * @param outlineColor the colour of the outline
     * @param alignment the horizontal alignment of the text within the
     * rectangle
     * @param outlineUnderneath if <code>true</code>, the outline is drawn
     * underneath the text, otherwise overtop of it
     */
    public void drawOutlinedTitle(Graphics2D g, String text, Rectangle region, Font font, float maxSize, float outlineSize, Paint textColor, Paint outlineColor, int alignment, boolean outlineUnderneath) {
        Font f = font.deriveFont(maxSize * (float) dpi / 72f);
        GlyphVector gv = f.createGlyphVector(g.getFontRenderContext(), text);
        Rectangle2D bounds = gv.getLogicalBounds();

        if (bounds.getWidth() > region.getWidth()) {
            f = f.deriveFont((float) (region.getWidth() / bounds.getWidth()) * maxSize * (float) dpi / 72f);
            gv = f.createGlyphVector(g.getFontRenderContext(), text);
            bounds = gv.getLogicalBounds();
        }

        float y = region.y + (float) (region.height - bounds.getHeight()) / 2f;
        float x;
        if (alignment > 0) {
            x = (float) (region.x + region.width - bounds.getWidth());
        } else if (alignment < 0) {
            x = region.x;
        } else {
            x = (float) (region.x + (region.width - bounds.getWidth()) / 2f);
        }

        Shape s = gv.getOutline(x, y + g.getFontMetrics(f).getAscent());
        Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (!outlineUnderneath) {
            g.setPaint(textColor);
            g.fill(s);
        }

        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke(outlineSize * (float) dpi / 72f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(outlineColor);
        g.draw(s);
        g.setStroke(oldStroke);

        if (outlineUnderneath) {
            g.setPaint(textColor);
            g.fill(s);
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);

        if (MarkupRenderer.DEBUG) {
            drawRegionBox(g, region);
        }
    }

    /**
     * Constant to rotate text 0 degrees when drawn with
     * {@link #drawRotatedTitle}.
     */
    public static final int ROTATE_NONE = 0;
    /**
     * Constant to rotate text 90 degrees when drawn with
     * {@link #drawRotatedTitle}.
     */
    public static final int ROTATE_LEFT = 1;
    /**
     * Constant to rotate text 180 degrees when drawn with
     * {@link #drawRotatedTitle}.
     */
    public static final int ROTATE_UPSIDE_DOWN = 2;
    /**
     * Constant to rotate text 270 degrees when drawn with
     * {@link #drawRotatedTitle}.
     */
    public static final int ROTATE_RIGHT = 3;

    /**
     * Draws text within a region. The text is drawn as if by {@link #fitTitle},
     * but the text is rotated the specified number of 90 degree anticlockwise
     * turns. The constants
     * {@link #ROTATE_NONE}, {@link #ROTATE_LEFT}, {@link #ROTATE_RIGHT}, and
     * {@link #ROTATE_UPSIDE_DOWN} may also be used to specify an orientation.
     * The text is drawn as a single line and it does not interpret markup tags.
     * The interpretation of alignment is rotated along with the text. For
     * example, if the text is rotated left, then a left alignment will align
     * the text to the bottom of the region.
     *
     * @param g the graphics context to use for drawing
     * @param text the text to draw
     * @param region the region to draw the text within
     * @param font the font to use for the text
     * @param maxSize the point size to use for the text
     * @param alignment the horizontal alignment of the text within the
     * rectangle
     * @param turns the number of turns to make, or one of the <tt>ROTATE</tt>
     * constants defined in this class
     */
    public void drawRotatedTitle(Graphics2D g, String text, Rectangle region, Font font, float maxSize, int alignment, int turns) {
        Rectangle rotatedRegion;
        double theta;

        switch (turns & 3) {
            case 0:
                // Instead of rotating by 0 radians, we'll just draw the
                // text and return:
                // rot = r;
                // theta = 0d;
                // break;
                fitTitle(g, text, region, font, maxSize, alignment);
                return;
            case 1:
                rotatedRegion = new Rectangle(
                        -region.height - region.y,
                        region.x,
                        region.height,
                        region.width
                );
                theta = -Math.PI / 2d;
                break;
            case 2:
                rotatedRegion = new Rectangle(
                        -region.width - region.x,
                        -region.height - region.y,
                        region.width,
                        region.height
                );
                theta = Math.PI;
                break;
            case 3:
                rotatedRegion = new Rectangle(
                        region.y,
                        -region.width - region.x,
                        region.height,
                        region.width
                );
                theta = Math.PI / 2d;
                break;
            default:
                // impossible since we used (turns & 3)
                throw new AssertionError();
        }
        final AffineTransform oldAT = g.getTransform();
        g.rotate(theta);
        fitTitle(g, text, rotatedRegion, font, maxSize, alignment);
        g.setTransform(oldAT);
    }

    /**
     * Draws a title string centered within the specified rectangle.
     *
     * @deprecated Included for backwards compatibility with old plug-ins; use
     * {@link #drawTitle} instead.
     *
     * @param g the sheet graphics context to render to
     * @param text the title string to draw
     * @param r the rectangle to center the text within
     * @param font the font to use to draw the title
     * @param size the point size to draw the title at
     * @param embolden if <code>true</code>, use a bolder version of the font
     */
    @Deprecated
    public void centerTitle(Graphics2D g, String text, Rectangle r, Font font, float size, boolean embolden) {
        Font f = font.deriveFont(size * (float) getTemplateResolution() / 72f);
        if (embolden) {
            if (f.isBold()) {
                f = f.deriveFont(Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_EXTRABOLD));
            } else {
                f = f.deriveFont(f.getStyle() | Font.BOLD);
            }
        }

        FontMetrics fm = g.getFontMetrics(f);

        if (MarkupRenderer.DEBUG) {
            drawRegionBox(g, r);
        }
        g.setFont(f);
        int x = r.x + (r.width - fm.stringWidth(text)) / 2;
        int y = r.y + fm.getAscent() + (r.height - fm.getHeight()) / 2;
        g.drawString(text, x, y);
    }

    /**
     * Returns the symbol to be painted on cards for an expansion. If the
     * expansion requested is <code>null</code>, then <code>null</code> is
     * returned.
     *
     * <p>
     * If the variant string is <code>null</code> or indicates boolean
     * <code>false</code>, then the expansion's 0th symbol is returned. If it
     * indicates boolean <code>true</code>, then the expansion's 1st symbol is
     * returned. Otherwise, the string is interpreted as an integer indicating
     * the index of the symbol to return. If the string is invalid, the 0th
     * symbol is used.
     *
     * Several versions of the master image registered with the
     * {@link Expansion} may be maintained. Each such version is optimized for
     * drawing at a certain size (in effect, each version is best for a
     * different sheet resolution). Which version is returned will be determined
     * by the {@code targetWidth} parameter. If possible, the returned image's
     * width will be between {@code targetWidth} and {@code 2 * targetWidth}.
     * Typically, you should request a target width equal to the width of the
     * region in which the symbol is painted, multiplied by
     * {@link #getScalingFactor()}.
     *
     * @param expansion the expansion to obtain a symbol for
     * @param booleanOrIntegerVariant a string describing the desired variant
     * @param targetWidth the desired image width, in actual pixels (i.e., not
     * scaled for the current drawing resolution)
     * @return a variant of the expansion symbol image that is optimized for
     * <code>targetWidth</code>, or <code>null</code>
     * @throws IllegalArgumentException if the target width is not positive
     */
    public static BufferedImage getExpansionSymbol(Expansion expansion, String booleanOrIntegerVariant, int targetWidth) {
        if (targetWidth < 1) {
            throw new IllegalArgumentException("targetWidth must be >= 1");
        }
        if (expansion == null) {
            return null;
        }

        int index = 0; // fallback on the default symbol
        if (booleanOrIntegerVariant != null && !booleanOrIntegerVariant.isEmpty()) {
            char prefix = booleanOrIntegerVariant.charAt(0);
            if (prefix >= '0' && prefix <= '9') {
                try {
                    index = Integer.parseInt(booleanOrIntegerVariant);
                } catch (NumberFormatException e) {
                    StrangeEons.log.log(Level.WARNING, "invalid expansion symbol variant: {0}", booleanOrIntegerVariant);
                }
            } else {
                if (booleanOrIntegerVariant.equalsIgnoreCase("yes") || booleanOrIntegerVariant.equalsIgnoreCase("true")) {
                    index = 1;
                }
            }
        }

        BufferedImage master = expansion.getSymbol(index);
        if (master == null || master.getWidth() <= targetWidth) {
            return master;
        }

        BufferedImage[] mipmap;
        synchronized (mipmapMap) {
            mipmap = mipmapMap.get(master);

            if (mipmap == null) {
                int levels = Math.min(
                        31 - Integer.numberOfLeadingZeros(master.getWidth()),
                        31 - Integer.numberOfLeadingZeros(master.getHeight())
                );
                mipmap = new BufferedImage[levels];
                mipmap[0] = master;
                for (int i = 1; i < levels; ++i) {
                    mipmap[i] = ImageUtilities.resample(
                            mipmap[i - 1], mipmap[i - 1].getWidth() / 2, mipmap[i - 1].getHeight() / 2,
                            false, RenderingHints.VALUE_INTERPOLATION_BICUBIC, null
                    );
                }
                mipmapMap.put(master, mipmap);
            }
        }

        for (int i = 1; i < mipmap.length; ++i) {
            if (mipmap[i].getWidth() < targetWidth) {
                return mipmap[i - 1];
            }
        }
        return mipmap[mipmap.length - 1];
    }
    private static final Map<BufferedImage, BufferedImage[]> mipmapMap = new HashMap<>();

    // Create and register the MIP map cache's metrics
    static {
        ResourceKit.registerCacheMetrics(new CacheMetrics() {
            @Override
            public int getItemCount() {
                int count = 0;
                synchronized (mipmapMap) {
                    Set<BufferedImage> keys = mipmapMap.keySet();
                    for (BufferedImage key : keys) {
                        BufferedImage[] values = mipmapMap.get(key);
                        if (values != null) {
                            count += values.length;
                        }
                    }
                }
                return count;
            }

            @Override
            public long getByteSize() {
                long bytes = 0;
                synchronized (mipmapMap) {
                    Set<BufferedImage> keys = mipmapMap.keySet();
                    for (BufferedImage key : keys) {
                        BufferedImage[] values = mipmapMap.get(key);
                        if (values != null) {
                            for (BufferedImage bi : values) {
                                if (bi != null) {
                                    bytes += bi.getWidth() * bi.getHeight() * 4;
                                }
                            }
                        }
                    }
                }
                return bytes;
            }

            @Override
            public void clear() {
                synchronized (mipmapMap) {
                    mipmapMap.clear();
                }
            }

            @Override
            public Class getContentType() {
                return BufferedImage.class;
            }

            @Override
            public boolean isClearSupported() {
                return true;
            }

            @Override
            public String toString() {
                return "Expansion symbol MIP map cache";
            }

            @Override
            public String status() {
                return String.format(
                        "%,d images (%,d KiB)", getItemCount(), (getByteSize() + 512L) / 1_024L
                );
            }
        });
    }

    /**
     * Given a list of expansion codes separated by commas (the format used to
     * set expansion icons in a game component's private settings), return an
     * array of the expansions represented by the codes. Any undefined
     * expansions will appear as <code>null</code> values in the array. If
     * settingValue is null or empty, this method returns null.
     *
     * @param settingValue the setting value to parse
     * @return an array of the expansions listed by settingValue
     * @see
     * Expansion#getComponentExpansionSymbols(ca.cgjennings.apps.arkham.component.GameComponent)
     * @see Settings#getExpansionCode()
     * @see Settings#getExpansionVariant(ca.cgjennings.apps.arkham.sheet.Sheet)
     */
    public static Expansion[] parseExpansionList(String settingValue) {
        if (settingValue == null || settingValue.isEmpty()) {
            return null;
        }

        int tokens = 1;
        for (int i = 0; i < settingValue.length(); ++i) {
            if (settingValue.charAt(i) == ',') {
                ++tokens;
            }
        }

        if (tokens == 1) {
            return new Expansion[]{Expansion.get(settingValue)};
        }

        Expansion[] exps = new Expansion[tokens];
        int start = 0, expansion = 0;
        for (int i = 0; i < settingValue.length(); ++i) {
            if (settingValue.charAt(i) == ',') {
                exps[expansion++] = Expansion.get(settingValue.substring(start, i));
                start = i + 1;
            }
        }
        exps[expansion] = Expansion.get(settingValue.substring(start));
        return exps;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{template=" + getTemplateKey() + '}';
    }
}
