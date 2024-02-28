package ca.cgjennings.apps.arkham.generic;

import java.awt.Graphics2D;

import ca.cgjennings.apps.arkham.component.DefaultPortrait;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import ca.cgjennings.apps.arkham.sheet.Sheet;

/**
 * The sheet implementation used to render the back face of
 * {@linkplain GenericCardBase generic cards}.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class GenericCardBackSheet extends Sheet<GenericCardBase>{
    public GenericCardBackSheet(GenericCardBase card) {
        super(card);
        setCornerRadius(GenericCardBase.DEFAULT_CORNER_RADIUS);
        backFace = (DefaultPortrait) card.getPortrait(2);
        initializeTemplate(
            "generic-" + card.getId() + "-back",
            card.getTemplateImage(),
            "generic-back-expsym",
            card.getTemplateResolution(),
            1d
        );
    }

    private final DefaultPortrait backFace;

    @Override
    protected void paintSheet(RenderTarget target) {
        Graphics2D g = createGraphics();
        try {
            // no need to fill the card area with white; the back face portrait does it for us
            backFace.paint(g, target);
        } finally {
            g.dispose();
        }
    }
}