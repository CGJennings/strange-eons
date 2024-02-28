package ca.cgjennings.apps.arkham.generic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.TextAttribute;
import java.awt.geom.RoundRectangle2D;
import ca.cgjennings.apps.arkham.component.DefaultPortrait;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import ca.cgjennings.graphics.shapes.ShapeUtilities;
import ca.cgjennings.layout.MarkupRenderer;
import ca.cgjennings.layout.TextStyle;
import resources.ResourceKit;
import resources.Settings;
import resources.Settings.Region;

/**
 * The sheet implementation used to render the front face of
 * {@linkplain GenericCardBase generic cards}.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class GenericCardFrontSheet extends Sheet<GenericCardBase>{
    public GenericCardFrontSheet(GenericCardBase card) {
        super(card);
        setCornerRadius(GenericCardBase.DEFAULT_CORNER_RADIUS);
        frontFace = (DefaultPortrait) card.getPortrait(1);
        portrait = (DefaultPortrait) card.getPortrait(0);
        initializeTemplate(
            "generic-" + card.getId() + "-front",
            card.getTemplateImage(),
            "generic-" + card.getId() + "-front-expsym",
            card.getTemplateResolution(),
            1d
        );
        markupRenderer = new MarkupRenderer(card.getTemplateResolution());
        doStandardRendererInitialization(markupRenderer);
        initInteriorFillAndClipShapes();
    }

    private final DefaultPortrait frontFace;
    private final DefaultPortrait portrait;
    private final MarkupRenderer markupRenderer;
    private final Color textColor = Color.BLACK;

    private Shape textFillClip;
    private Shape fullTextFillClip;    

    /**
     * Initializes the shapes used to draw and clip to the
     * the interior fill. Since the card's layout is fixed
     * once constructed, we only create these once.
     * 
     * This means that if you want to "hack" the layout,
     * you need to do it before the sheets are creates,
     * or you need to create new sheets after each change.
     */
    private void initInteriorFillAndClipShapes() {
        final double MM_TO_PIXELS = 0.0393701d * getTemplateResolution();
        final double RADIUS = 5.5d * MM_TO_PIXELS;

        final GenericCardBase gc = getGameComponent();
        final Settings s = gc.getSettings();

        final Region safeRegion = s.getRegion(gc.key("-safe"));
        fullTextFillClip = new RoundRectangle2D.Double(
            safeRegion.x, safeRegion.y,
            safeRegion.width, safeRegion.height,
            RADIUS, RADIUS
        );

        final Region portraitRegion = s.getRegion(gc.key("-portrait-clip"));
        textFillClip = ShapeUtilities.intersect(
            fullTextFillClip, new Region(
                0, portraitRegion.getY2(),
                getTemplateWidth(), getTemplateHeight()
            )
        );
    }

    @Override
    protected void paintSheet(RenderTarget target) {
        final GenericCardBase gc = getGameComponent();
        final Settings s = gc.getSettings();
        Graphics2D g = createGraphics();
        try {
            // fill the entire card including the bleed area, since
            // we have no idea what the card face graphic will cover
            g.setColor(Color.WHITE);
            Region bleedSurface = s.getRegion(gc.key("-bleed"));
            g.fillRect(bleedSurface.x, bleedSurface.y, bleedSurface.width, bleedSurface.height);

            if (gc.isPortraitUnderFace()) {
                paintPortrait(g, gc, target);
                frontFace.paint(g, target);
            } else {
                frontFace.paint(g, target);
                paintPortrait(g, gc, target);
            }

            paintInteriorFill(g, gc);
            paintText(g, gc);
        } finally {
            g.dispose();
        }
    }

    protected void paintText(Graphics2D g, GenericCardBase gc) {
        g.setColor(textColor);
        updateTextStyles();
        Region textRegion = gc.getSettings().getRegion(
            gc.key(gc.isTextOnly() ? "-full-text" : "-text")
        );
        String title = gc.getName().trim();
        if (!title.isEmpty()) title = "<h1>" + title + "</h1>";
        markupRenderer.setMarkupText(title + gc.getText());
        markupRenderer.draw(g, textRegion);
    }

    protected void paintPortrait(Graphics2D g, GenericCardBase gc, RenderTarget target) {
        if (gc.isTextOnly()) return;
        if (!gc.isPortraitUnderFace() && gc.isInteriorFilled()) {
            Shape oldClip = g.getClip();
            g.clip(fullTextFillClip);
            portrait.paint(g, target);
            g.setClip(oldClip);
        } else {
            portrait.paint(g, target);
        }
    }

    protected void paintInteriorFill(Graphics2D g, GenericCardBase gc) {
        if (!gc.isInteriorFilled()) return;
        g.setColor(interiorFillColor);
        g.fill(gc.isTextOnly() ? fullTextFillClip : textFillClip);

        if (outlineStroke == null) {
            outlineStroke = new BasicStroke((float) (2d/72d * getTemplateResolution()));
        }
        g.setStroke(outlineStroke);
        g.setColor(outlineColor);
        g.draw(fullTextFillClip);
    }
    private BasicStroke outlineStroke;

    private void updateTextStyles() {
        final GenericCardBase gc = getGameComponent();
        String titleFamily = gc.getTitleFamily();
        String textFamily = gc.getTextFamily();
        float baseSize = gc.getBaseFontSize();

        if (
            baseSize == baseSizeCache
            && titleFamily.equals(titleCache)
            && textFamily.equals(textCache)
        ) {
            return;
        }
        titleCache = titleFamily;
        textCache = textFamily;
        baseSizeCache = baseSize;

        TextStyle ts = markupRenderer.getDefaultStyle();
        ts.add(TextAttribute.FAMILY, selectFontFamily(textFamily));
        ts.add(TextAttribute.SIZE, baseSize);

        String loadedTitleFamily = selectFontFamily(titleFamily);
        ts = markupRenderer.getStyleForTag("h1");
        ts.add(TextAttribute.FAMILY, loadedTitleFamily);
        ts.add(TextAttribute.SIZE, baseSize * 1.44f);
        ts.add(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);

        ts = markupRenderer.getStyleForTag("h2");
        if (ts == null) {
            ts = new TextStyle();
            markupRenderer.setStyleForTag("h2", ts);
        }
        ts.add(TextAttribute.FAMILY, loadedTitleFamily);
        ts.add(TextAttribute.SIZE, baseSize * 1.2f);        
        ts.add(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);

        // clear the cache so that the layout is rebuilt with the new styles
        markupRenderer.setMarkupText("");
    }
    private String titleCache = null;
    private String textCache = null;
    private float baseSizeCache = -1f;
    private Color interiorFillColor = new Color(0xaaffffff, true);
    private Color outlineColor = Color.WHITE;

    private static String selectFontFamily(String family) {
        family = ResourceKit.normalizeFontFamilyName(family);
        if (family.isEmpty()) {
            family = ResourceKit.getBodyFamily();
        }
        return family;
    }
}
