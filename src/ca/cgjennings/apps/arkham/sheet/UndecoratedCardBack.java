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

    public UndecoratedCardBack(GameComponent component, String templateKey) {
        super(component, templateKey);
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
}
