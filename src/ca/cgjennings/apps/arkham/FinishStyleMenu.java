package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.commands.AbstractCommand;
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
        for (FinishStyle fs : FinishStyle.values()) {
            addFor(fs);
        }
    }

    private void addFor(final FinishStyle style) {
        AbstractCommand a = new AbstractCommand() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                current = style;
                style.setAsPreviewStyle();
                StrangeEons.getWindow().redrawPreviews();
                FinishStyleMenu.this.setIcon(getIcon());
                FinishStyleMenu.this.setDisabledIcon(getDisabledIcon());
            }

            @Override
            public void update() {
                boolean enable = false;
                StrangeEonsEditor editor = StrangeEons.getActiveEditor();
                if (editor instanceof AbstractGameComponentEditor) {
                    AbstractGameComponentEditor ed = (AbstractGameComponentEditor) editor;
                    enable = ed.getGameComponent().isDeckLayoutSupported();
                }
                setEnabled(enable);
            }
        };
        a.setName(style.toString());
        a.setIcon(style.getIcon());

        JRadioButtonMenuItem item = new JRadioButtonMenuItem(a);
        group.add(item);
        if (style == current) {
            item.setSelected(true);
            setIcon(getIcon());
        }
        add(item);
    }
}
