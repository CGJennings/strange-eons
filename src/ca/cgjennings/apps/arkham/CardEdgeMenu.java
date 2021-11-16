package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.sheet.EdgeStyle;
import java.awt.event.ActionEvent;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import static resources.Language.string;
import resources.ResourceKit;

final class CardEdgeMenu extends JMenu {

    private final ButtonGroup group = new ButtonGroup();

    public CardEdgeMenu() {
        super(string("app-card-edge"));
        addEdgeStyleOption(EdgeStyle.CUT);
        addSeparator();
        addEdgeStyleOption(EdgeStyle.BLEED);
        addEdgeStyleOption(EdgeStyle.NO_BLEED);
        addEdgeStyleOption(EdgeStyle.RAW);
        addSeparator();
        addEdgeStyleOption(EdgeStyle.HIGHLIGHT);
    }

    private void addEdgeStyleOption(EdgeStyle style) {
        EdgeStyleMenuItem item = new EdgeStyleMenuItem(this, style);
        if (style == EdgeStyle.getPreviewEdgeStyle()) {
            item.setSelected(true);
            setIcon(item.getIcon());
        }
        group.add(item);
        add(item);
    }

    private static final class EdgeStyleMenuItem extends JRadioButtonMenuItem {

        private final CardEdgeMenu menu;
        private final EdgeStyle style;

        public EdgeStyleMenuItem(CardEdgeMenu menu, EdgeStyle style) {
            String id = style.name().toLowerCase().replace('_', '-');
            setText(string("app-edge-style-" + id));
            setIcon(ResourceKit.getIcon("ui/view/edge-style-" + id + ".png"));
            this.menu = menu;
            this.style = style;
        }

        @Override
        protected void fireActionPerformed(ActionEvent event) {
            menu.setIcon(getIcon());
            EdgeStyle.setPreviewEdgeStyle(style);
            StrangeEons.getWindow().redrawPreviews();
            super.fireActionPerformed(event);
        }
    }
}
