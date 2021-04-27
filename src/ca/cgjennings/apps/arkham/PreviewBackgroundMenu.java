package ca.cgjennings.apps.arkham;

import ca.cgjennings.graphics.paints.CheckeredPaint;
import gamedata.SymbolVariantUtilities;
import java.awt.Component;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import static resources.Language.string;
import resources.Settings;

/**
 * A menu that provides options for the background colour of game component
 * previewers.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
class PreviewBackgroundMenu extends JMenu {

    public PreviewBackgroundMenu() {
        String[] labels = {"dark", "light", "check"};
        Paint[] paints = {SheetViewer.DEFAULT_DARK_BACKGROUND, SheetViewer.DEFAULT_LIGHT_BACKGROUND, SheetViewer.DEFAULT_CHECKERED_BACKGROUND};
        ButtonGroup group = new ButtonGroup();
        int sel = Math.max(0, Settings.getShared().getInt("preview-backdrop"));

        for (int i = 0; i < labels.length; ++i) {
            Paint p = paints[i];
            if (p instanceof CheckeredPaint) {
                CheckeredPaint cp = (CheckeredPaint) p;
                p = new CheckeredPaint(cp.getBoxSize() / 2, cp.getColor1(), cp.getColor2());
            }
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(
                    string("app-backdrop-" + labels[i]),
                    SymbolVariantUtilities.createDefaultVariantIcon(p)
            );
            group.add(item);
            add(item);
            if (i == sel) {
                item.setSelected(true);
            }
            item.addActionListener(li);
        }
    }

    @Override
    public Icon getIcon() {
        int count = getMenuComponentCount();
        for (int i = 0; i < count; ++i) {
            Component c = getMenuComponent(i);
            if (c instanceof JRadioButtonMenuItem) {
                JRadioButtonMenuItem b = (JRadioButtonMenuItem) c;
                if (b.isSelected()) {
                    return b.getIcon();
                }
            }
        }
        return null;
    }

    private ActionListener li = (ActionEvent e) -> {
        int i = 0;
        for (; i < getMenuComponentCount(); ++i) {
            if (getMenuComponent(i) == e.getSource()) {
                break;
            }
        }
        if (i == getMenuComponentCount()) {
            throw new AssertionError("did not find selected item in menu");
        }
        
        Settings user = Settings.getUser();
        int old = user.getInt("preview-backdrop", 0);
        if (old != i) {
            user.setInt("preview-backdrop", i);
            AppFrame.getApp().propertyChange(StrangeEonsAppWindow.VIEW_BACKDROP_PROPERTY, old, i);
        }
    };
}
