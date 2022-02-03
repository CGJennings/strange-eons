package ca.cgjennings.ui.theme;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

/**
 * Custom subclass of the Nimbus look-and-feel.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
class StrangeNimbus extends NimbusLookAndFeel {

    public StrangeNimbus() {
    }

    @Override
    public Icon getDisabledIcon(JComponent component, Icon icon) {
        if (icon != null) {
            icon = Theme.getDisabledIcon(component, icon);
            if (icon == null) {
                icon = super.getDisabledIcon(component, icon);
            }
        }
        return icon;
    }
}
