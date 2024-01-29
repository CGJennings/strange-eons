package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.deck.Deck;
import ca.cgjennings.apps.arkham.deck.DeckEditor;
import ca.cgjennings.apps.arkham.deck.item.PageItem;
import ca.cgjennings.apps.arkham.deck.item.PageItem.SnapClass;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.EnumSet;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import static resources.Language.string;

/**
 * Self-updating menu for changing the deck selection's snap class.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
class SnapMenu extends JMenu {

    public SnapMenu(JMenu parent) {
        super("app-snap-class");
        parent.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                Deck d = getDeck();
                boolean enable = d != null && d.getSelectionSize() > 0;
                if (enable) {
                    PageItem[] sel = d.getSelection();
                    PageItem last = sel[sel.length - 1];
                    EnumSet<SnapClass> snappedTo = last.getClassesSnappedTo();
                    for (int i = 0;; ++i) {
                        Component c = getMenuComponent(i);
                        if (!(c instanceof SnapItem)) {
                            break;
                        }
                        final SnapItem item = (SnapItem) c;
                        final SnapClass itemClass = ((SnapItem) c).snapClassSet.iterator().next();
                        item.setSelected(snappedTo.contains(itemClass));
                        item.setFont(last.getSnapClass() == itemClass ? special : regular);
                    }
                }
                setEnabled(enable);
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });

        add(new SnapItem("de-l-class-paper", EnumSet.of(SnapClass.SNAP_PAGE_GRID)));
        add(new SnapItem("de-l-class-card", EnumSet.of(SnapClass.SNAP_CARD)));
        add(new SnapItem("de-l-class-tile", EnumSet.of(SnapClass.SNAP_TILE)));
        add(new SnapItem("de-l-class-overlay", EnumSet.of(SnapClass.SNAP_OVERLAY)));
        add(new SnapItem("de-l-class-inlay", EnumSet.of(SnapClass.SNAP_INLAY)));
        add(new SnapItem("de-l-class-other", EnumSet.of(SnapClass.SNAP_OTHER)));
        addSeparator();
        add(new SnapItem("de-l-class-any", SnapClass.SNAP_SET_ANY));
        add(new SnapItem("de-l-class-none", SnapClass.SNAP_SET_NONE));

        regular = getFont();
        special = regular.deriveFont(Font.BOLD);
    }

    private Font regular;
    private Font special;

    private Deck getDeck() {
        StrangeEonsEditor ed = StrangeEons.getWindow().getActiveEditor();
        if (ed instanceof DeckEditor) {
            return ((DeckEditor) ed).getDeck();
        }
        return null;
    }

    private static class SnapItem extends JCheckBoxMenuItem {

        private EnumSet<PageItem.SnapClass> snapClassSet;

        public SnapItem(String name, EnumSet<PageItem.SnapClass> snapClass) {
            super(string(name));
            this.snapClassSet = snapClass;
        }

        @Override
        public void fireActionPerformed(ActionEvent e) {
            final PageItem[] selection = getSelection();
            if (selection == null || selection.length == 0) {
                return;
            }

            final PageItem source = selection[selection.length - 1];

            if (snapClassSet.size() == 1) {
                PageItem.SnapClass group = snapClassSet.iterator().next();
                boolean add = !source.getClassesSnappedTo().contains(group);
                for (PageItem i : selection) {
                    EnumSet<PageItem.SnapClass> newClass = i.getClassesSnappedTo();
                    if (add) {
                        newClass.add(group);
                    } else {
                        newClass.remove(group);
                    }
                    i.setClassesSnappedTo(newClass);
                }
            } else {
                for (PageItem i : selection) {
                    i.setClassesSnappedTo(snapClassSet);
                }
            }

            super.fireActionPerformed(e);
        }
    }

    private static PageItem[] getSelection() {
        DeckEditor ed = getDeckEditor();
        return ed == null ? null : ed.getDeck().getSelection();
    }

    private static DeckEditor getDeckEditor() {
        StrangeEonsEditor ed = StrangeEons.getWindow().getActiveEditor();
        if (ed instanceof DeckEditor && ((DeckEditor) ed).getDeck() != null) {
            return (DeckEditor) ed;
        }
        return null;
    }
}
