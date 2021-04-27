package ca.cgjennings.ui.theme;

import ca.cgjennings.graphics.ImageUtilities;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

/**
 * A custom subclass of the Nimbus look-and-feel with customizations specific to
 * Strange Eons.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
class StrangeNimbus extends NimbusLookAndFeel {

    public StrangeNimbus() {
    }

    @Override
    public Icon getDisabledIcon(JComponent component, Icon icon) {
        if (icon != null) {
            Object fo = UIManager.get(Theme.DISABLED_ICON_FILTER);
            if (fo instanceof BufferedImageOp) {
                BufferedImage bi = ImageUtilities.iconToImage(icon);
                bi = ((BufferedImageOp) fo).filter(bi, null);
                icon = new ImageIcon(bi);
            } else {
                icon = ImageUtilities.createDisabledIcon(icon);
            }
        }
        return icon;
    }
}
