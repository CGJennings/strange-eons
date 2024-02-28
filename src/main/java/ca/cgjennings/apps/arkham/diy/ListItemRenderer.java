package ca.cgjennings.apps.arkham.diy;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;

/**
 * A cell renderer for lists and combo boxes that correctly displays
 * {@link ListItem}s.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ListItemRenderer extends DefaultListCellRenderer {

    /**
     * Creates a new list item renderer. Instead of creating a new renderer for
     * each list or combo box, it is recommend that you use {@link #getShared()}
     * to obtain a shared instance.
     */
    public ListItemRenderer() {
    }

    /**
     * Returns a component that can be used to paint the representation of the
     * list item.
     *
     * @param list the list being painted
     * @param value the list item being painted in the list
     * @param index the index of the list item in the list
     * @param isSelected {@code true} if the item is selected
     * @param cellHasFocus {@code true} if the item has focus
     * @return a component capable of painting list item
     */
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        String label;
        Icon icon;

        if (value instanceof ListItem) {
            ListItem item = (ListItem) value;
            label = item.getLabel();
            icon = item.getIcon();
        } else {
            label = String.valueOf(value);
            icon = null;
        }

        super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
        setIcon(icon);

        return this;
    }

    /**
     * Returns a shared list item renderer that can be used instead of creating
     * a new renderer for each control.
     *
     * @return a shared list item renderer
     */
    public static ListItemRenderer getShared() {
        return shared;
    }
    private static final ListItemRenderer shared = new ListItemRenderer();
}
