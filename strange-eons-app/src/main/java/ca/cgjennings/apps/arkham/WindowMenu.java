package ca.cgjennings.apps.arkham;

import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.ui.BlankIcon;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.Collator;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import resources.Language;

/**
 * The application Window menu; automatically lists all editors and tracked
 * windows when opened.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
final class WindowMenu extends JMenu implements MenuListener {

    public WindowMenu() {
        addMenuListener(this);
    }

    @Override
    public void menuCanceled(MenuEvent e) {
    }

    @Override
    public void menuDeselected(MenuEvent e) {
    }

    @Override
    public void menuSelected(MenuEvent e) {
        removeAll();

        // ATTACHED EDITOR TAB MENU ITEMS //////////////////////////////////////
        final StrangeEonsEditor[] editors = AppFrame.getApp().getAttachedEditors();
        JMenuItem[] editorItems = new JMenuItem[editors.length];
        for (int i = 0; i < editors.length; ++i) {
            AbstractStrangeEonsEditor ed = (AbstractStrangeEonsEditor) editors[i];
            Icon icon = restrictSize(ed.getFrameIcon());
            JMenuItem item = new JMenuItem(ed.getTitle(), icon);
            item.addActionListener(createEditorWindowAction(ed));
            if (ed.hasUnsavedChanges()) {
                item.setFont(item.getFont().deriveFont(Font.BOLD));
            }
            editorItems[i] = item;
        }

        Arrays.sort(editorItems, sorter);
        for (JMenuItem m : editorItems) {
            add(m);
        }

        // TRACKED MENU ITEMS (INCL. DETACHED TABS) ////////////////////////////
        appendTrackedItems(true); // detached tabs
        appendTrackedItems(false); // other items
    }

    private void appendTrackedItems(boolean detachedTabs) {
        LinkedList<JMenuItem> items = new LinkedList<>();
        for (TrackedWindow w : trackedWindows.keySet()) {
            if (w instanceof TDetachedEditor) {
                if (detachedTabs) {
                    items.add(trackedWindows.get(w));
                }
            } else {
                if (!detachedTabs) {
                    items.add(trackedWindows.get(w));
                }
            }
        }

        if (!items.isEmpty()) {
            if (getMenuComponentCount() > 0) {
                addSeparator();
            }
            Collections.sort(items, sorter);
            for (JMenuItem i : items) {
                add(i);
            }
        }
    }

    private ActionListener createEditorWindowAction(final StrangeEonsEditor ed) {
        return (ActionEvent e) -> {
            ed.select();
        };
    }

    private ActionListener createTrackedWindowAction(final TrackedWindow w) {
        return (ActionEvent e) -> {
            w.setVisible(true);
            w.toFront();
            w.requestFocusInWindow();
        };
    }

    static Icon restrictSize(Icon icon) {
        if (icon == null) {
            if (blankIcon == null) {
                blankIcon = new BlankIcon(ICON_SIZE);
            }
            icon = blankIcon;
        } else if (icon.getIconWidth() != ICON_SIZE || icon.getIconHeight() != ICON_SIZE) {
            icon = ImageUtilities.ensureIconHasSize(icon, ICON_SIZE);
        }
        return icon;
    }
    private static Icon blankIcon;

    public void trackWindow(final TrackedWindow w, boolean startTracking) {
        if (w == null) {
            throw new NullPointerException();
        }

        if (startTracking) {
            if (trackedWindows.containsKey(w)) {
                return;
            }

            // the title is updated just before the menu is shown, so we don't need to set it now
            JMenuItem item = new JMenuItem(w.getTitle(), restrictSize(w.getIcon()));
            item.addActionListener(createTrackedWindowAction(w));
            trackedWindows.put(w, item);
        } else {
            trackedWindows.remove(w);
        }
    }

    private HashMap<TrackedWindow, JMenuItem> trackedWindows = new HashMap<>();

    private final Comparator<JMenuItem> sorter = new Comparator<JMenuItem>() {
        @Override
        public int compare(JMenuItem o1, JMenuItem o2) {
            String s1 = o1.getText(), s2 = o2.getText();
            try {
                if (s1 == null) {
                    s1 = "";
                }
                if (s2 == null) {
                    s2 = "";
                }
                return c.compare(s1, s2);
            } catch (Exception e) {
                if (!comparatorExWarned) {
                    comparatorExWarned = true;
                    StrangeEons.log.log(Level.SEVERE, null, e);
                }
                return s1.compareTo(s2);
            }
        }
        private final Collator c = Language.getInterface().getCollator();
    };
    private static boolean comparatorExWarned = false;

    static final int ICON_SIZE = 18;

    Set<TrackedWindow> getTrackedWindows() {
        return trackedWindows.keySet();
    }
}
