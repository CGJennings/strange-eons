package ca.cgjennings.apps.arkham.generic;

import java.awt.Color;
import java.awt.Graphics2D;

import ca.cgjennings.apps.arkham.component.DefaultPortrait;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import resources.Settings;
import resources.Settings.Region;

/**
 * The sheet implementation used to render the back face of
 * {@linkplain AbstractGenericCard generic cards}.
 */
public class GenericCardBackSheet extends Sheet<AbstractGenericCard>{
    public GenericCardBackSheet(AbstractGenericCard card) {
        super(card);
        backFace = (DefaultPortrait) card.getPortrait(2);
        initializeTemplate(
            "generic-" + card.getId(),
            card.getTemplateImage(),
            "generic-back-expsym",
            card.getTemplateResolution(),
            1d
        );
    }

    private DefaultPortrait backFace;

    @Override
    protected void paintSheet(RenderTarget target) {
        final AbstractGenericCard card = getGameComponent();
        final Settings s = card.getSettings();
        Graphics2D g = createGraphics();
        try {
            g.setColor(Color.WHITE);
            Region bleedSurface = s.getRegion(card.key("-bleed"));
            g.fillRect(bleedSurface.x, bleedSurface.y, bleedSurface.width, bleedSurface.height);
            backFace.paint(g, target);
        } finally {
            g.dispose();
        }
    }
}