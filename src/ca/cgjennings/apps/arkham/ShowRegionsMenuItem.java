package ca.cgjennings.apps.arkham;

import ca.cgjennings.layout.MarkupRenderer;
import java.awt.event.ActionEvent;
import javax.swing.JCheckBoxMenuItem;
import resources.Settings;

/**
 * Menu item that controls whether debug boxes are shown on components.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
class ShowRegionsMenuItem extends JCheckBoxMenuItem {

    private static final String KEY_SHOW_DEBUG_BOXES = "show-debug-boxes";

    public ShowRegionsMenuItem() {
        final boolean debug = Settings.getShared().getYesNo(KEY_SHOW_DEBUG_BOXES);
        MarkupRenderer.DEBUG = debug;
        setSelected(debug);
    }

    @Override
    protected void fireActionPerformed(ActionEvent event) {
        final boolean debug = isSelected();
        MarkupRenderer.DEBUG = debug;
        Settings.getUser().set(KEY_SHOW_DEBUG_BOXES, debug ? "yes" : "no");
        StrangeEons.getWindow().redrawPreviews();
        super.fireActionPerformed(event);
    }
}
