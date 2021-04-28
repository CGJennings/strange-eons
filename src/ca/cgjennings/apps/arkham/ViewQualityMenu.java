package ca.cgjennings.apps.arkham;

import ca.cgjennings.graphics.ImageUtilities;
import java.awt.Font;
import java.awt.event.ActionEvent;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * A menu that displays options for choosing a {@link ViewQuality}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
final class ViewQualityMenu extends JMenu {

    public ViewQualityMenu() {
        super(string("app-quality"));
        JMenuItem autoItem = new JRadioButtonMenuItem(string("app-auto-quality")) {
            @Override
            protected void fireActionPerformed(ActionEvent event) {
                ViewQuality.setManagedAutomatically(isSelected());
                super.fireActionPerformed(event);
            }
        };
        autoItem.setSelected(ViewQuality.isManagedAutomatically());
        autoItem.setFont(autoItem.getFont().deriveFont(Font.BOLD));
        group.add(autoItem);
        add(autoItem);
        addSeparator();
        add(new ViewQualityMenuItem(ViewQuality.LOW, group));
        add(new ViewQualityMenuItem(ViewQuality.MEDIUM, group));
        add(new ViewQualityMenuItem(ViewQuality.HIGH, group));
        add(new ViewQualityMenuItem(ViewQuality.ULTRAHIGH, group));
    }

    @Override
    public Icon getIcon() {
        ViewQuality q = ViewQuality.get();
        return ResourceKit.getIcon("ui/view/quality-" + (char) ('0' + q.ordinal()) + ".png");
    }

    private static final class ViewQualityMenuItem extends JRadioButtonMenuItem {

        private ViewQuality q;

        public ViewQualityMenuItem(ViewQuality q, ButtonGroup group) {
            setSelected(!ViewQuality.isManagedAutomatically() && ViewQuality.get() == q);
            this.q = q;
            String name;
            char iconIndex;
            switch (q) {
                case LOW:
                    name = string("app-low-quality");
                    iconIndex = '0';
                    break;
                case MEDIUM:
                    name = string("app-medium-quality");
                    iconIndex = '1';
                    break;
                case HIGH:
                    name = string("app-high-quality");
                    iconIndex = '2';
                    break;
                case ULTRAHIGH:
                    name = string("app-decadent-quality");
                    iconIndex = '3';
                    break;
                default:
                    throw new AssertionError();
            }
            group.add(this);
            Icon icon = ResourceKit.getIcon("ui/view/quality-" + iconIndex + ".png");
            setIcon(icon);
            setDisabledIcon(new ImageIcon(ImageUtilities.desaturate(ImageUtilities.iconToImage(icon))));
            setText(name);
        }

        @Override
        protected void fireActionPerformed(ActionEvent event) {
            ViewQuality.setManagedAutomatically(false);
            ViewQuality.set(q);
            StrangeEons.getWindow().redrawPreviews();
            super.fireActionPerformed(event);
        }
    }
    private final ButtonGroup group = new ButtonGroup();
}
