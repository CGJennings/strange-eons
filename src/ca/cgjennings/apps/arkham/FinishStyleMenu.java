package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.sheet.FinishStyle;
import java.awt.event.ActionEvent;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;

/**
 * A menu that displays options for choosing a {@link ViewQuality}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.3
 */
@SuppressWarnings("serial")
final class FinishStyleMenu extends JMenu {
    private final ButtonGroup group = new ButtonGroup();
    private FinishStyle current;
    
    public FinishStyleMenu() {
        current = FinishStyle.getPreviewStyle();
        
        add(new FinishItem(FinishStyle.ROUND));
        add(new FinishItem(FinishStyle.SQUARE));
        add(new FinishItem(FinishStyle.MARGIN));
    }

    private final class FinishItem extends JRadioButtonMenuItem {
        private FinishStyle style;
        
        public FinishItem(FinishStyle fs) {
            this.style = fs;
            setText(fs.toString());
            setIcon(fs.getIcon());
            group.add(this);
            if (style == current) {
                setSelected(true);
            }
        }
        
        @Override
        protected void fireActionPerformed(ActionEvent event) {
            current = style;
            style.setAsPreviewStyle();
            StrangeEons.getWindow().redrawPreviews();
            super.fireActionPerformed(event);
            FinishStyleMenu.this.setIcon(getIcon());
        }
    }
}