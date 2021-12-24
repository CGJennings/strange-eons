package ca.cgjennings.apps.arkham.sheet;

import ca.cgjennings.apps.arkham.component.GameComponent;
import java.awt.Graphics2D;

/**
 * A component face that simply draws the template image. This is useful for
 * plain card backs that never change.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class UndecoratedCardBack extends Sheet<GameComponent> {
    private final double bleedMargin;

    public UndecoratedCardBack(GameComponent component, String templateKey) {
        this(component, templateKey, 0d, -1d);
    }

    public UndecoratedCardBack(GameComponent component, String templateKey, double bleedMargin) {
        this(component, templateKey, bleedMargin, -1d);
    }

    public UndecoratedCardBack(GameComponent component, String templateKey, double bleedMargin, double cornerRadius) {
        super(component, templateKey);
        this.bleedMargin = bleedMargin;

        // retain default set by key unless an explicit radius was passed
        if (cornerRadius >= 0d) {
            setCornerRadius(cornerRadius);
        }
    }

    @Override
    protected void paintSheet(RenderTarget target) {
        Graphics2D g = createGraphics();
        try {
            g.drawImage(getTemplateImage(), 0, 0, null);
        } finally {
            g.dispose();
        }
    }

    @Override
    public double getBleedMargin() {
        return bleedMargin;
    }
}
