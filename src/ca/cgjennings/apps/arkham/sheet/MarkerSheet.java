package ca.cgjennings.apps.arkham.sheet;

import ca.cgjennings.apps.arkham.component.Marker;
import ca.cgjennings.apps.arkham.component.Portrait;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.layout.MarkupRenderer;
import ca.cgjennings.layout.TextStyle;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import resources.Settings;

/**
 * Draws the front and back face of markers (tokens).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class MarkerSheet extends Sheet<Marker> {

    private final boolean isBack;
    private final MarkupRenderer textLayout;

    public MarkerSheet(Marker marker, boolean isBack) {
        super(marker, "marker-sheet-template");
        this.isBack = isBack;

        Settings s = marker.getSettings();
        textLayout = new MarkupRenderer(getTemplateResolution());
        textLayout.setLineTightness(0.5f);
        textLayout.setMarkBadBox(s.getYesNo("highlight-bad-boxes"));
        textLayout.setScalingLimit(s.getFloat("min-text-scaling-factor"));
        textLayout.setTightnessLimit(s.getFloat("min-text-spacing-factor"));
        doStandardRendererInitialization(textLayout);
        textLayout.setAlignment(MarkupRenderer.LAYOUT_CENTER | MarkupRenderer.LAYOUT_MIDDLE);
        TextStyle def = textLayout.getDefaultStyle();
        def.add(TextAttribute.FAMILY, Font.SERIF,
                TextAttribute.SIZE, s.getPointSize("marker"),
                TextAttribute.FOREGROUND, Color.WHITE
        );
    }

    @Override
    public PrintDimensions getPrintDimensions() {
        final BufferedImage template = getTemplateImage();
        if (cachedDimensions == null || template != lastTemplateUsedToComputeDimensions) {
            double rawDPI = getTemplateResolution();
            double oldUpsamp = getScalingFactor();
            BufferedImage trimmed = paint(RenderTarget.PRINT, getTemplateResolution());
            cachedDimensions = new PrintDimensions(
                    trimmed.getWidth() / 150d * 72d,
                    trimmed.getHeight() / 150d * 72d
            );
            lastTemplateUsedToComputeDimensions = template;
        }
        return cachedDimensions;
    }
    private PrintDimensions cachedDimensions;
    private BufferedImage lastTemplateUsedToComputeDimensions;

    @Override
    public double getBleedMargin() {
        return getGameComponent().getBleedMargin();
    }

    @Override
    protected void paintSheet(RenderTarget target) {
        final Marker marker = getGameComponent();

        // because the image returns true for isTransparent, it is recreated
        // as a new bitmap on each draw; since the size of the result should
        // depend on the stencil, we set the template image to the stencil
        // image before creating the sheet graphics (which also creates the
        // image bitmap)
        BufferedImage stencil = marker.getStencil();
        BufferedImage orientedStencil;
        replaceTemplateImage(stencil);

        Graphics2D g = createGraphics();
        try {

            Portrait p = marker.getPortrait(isBack ? 1 : 0);
            BufferedImage portrait = p.getImage();
            double scale = p.getScale();
            double panX = p.getPanX();
            double panY = p.getPanY();
            if (isBack) {
                AffineTransform at = java.awt.geom.AffineTransform.getScaleInstance(-1, 1);
                at.translate(-stencil.getWidth(), 0);
                AffineTransformOp op = new java.awt.image.AffineTransformOp(at, java.awt.image.AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                orientedStencil = op.filter(stencil, null);
            } else {
                orientedStencil = stencil;
            }

            double centerX = portrait.getWidth() * scale / 2d;
            double centerY = portrait.getHeight() * scale / 2d;
            double regionX = orientedStencil.getWidth() / 2d;
            double regionY = orientedStencil.getHeight() / 2d;

            AffineTransform xform = AffineTransform.getTranslateInstance(regionX - centerX + panX, regionY - centerY + panY);
            xform.concatenate(AffineTransform.getScaleInstance(scale, scale));
            AffineTransformOp xformop = new AffineTransformOp(xform, target.getTransformInterpolationType());
            g.drawImage(portrait, xformop, 0, 0);

            String text = isBack ? marker.getBackText() : marker.getFrontText();
            if (text.length() > 0) {
                textLayout.setMarkupText(text);
                textLayout.draw(g, new Rectangle2D.Double(0d, 0d, stencil.getWidth(), stencil.getHeight()));
            }
        } finally {
            g.dispose();
        }

        // as a final step, we will replace the image to be returned with a
        // version that is "cut out" according to the current stencil shape
        BufferedImage image = getDestinationBuffer();
        BufferedImage sizedStencil = ImageUtilities.resample(orientedStencil, image.getWidth(), image.getHeight());
        Graphics2D sg = null;
        try {
            sg = image.createGraphics();
            sg.setComposite(AlphaComposite.DstIn);
            sg.drawImage(sizedStencil, 0, 0, null);
        } finally {
            if (sg != null) {
                sg.dispose();
            }
        }
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

    @Override
    public boolean isVariableSize() {
        return true;
    }
}
