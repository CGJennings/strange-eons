package ca.cgjennings.ui;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.ListModel;

/**
 * A list control that will draw icons for any added items that are
 * {@link IconProvider}s.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.00
 */
@SuppressWarnings("serial")
public class JIconList extends JList {

    public JIconList() {
        setCellRenderer(new IconRenderer());
    }

    public JIconList(Object[] listData) {
        super(listData);
        setCellRenderer(new IconRenderer());
    }

    public JIconList(ListModel dataModel) {
        super(dataModel);
        setCellRenderer(new IconRenderer());
    }

    /**
     * Returns the current default icon for this list. The default icon will be
     * used by list items that either do not implement {@link IconProvider} or
     * return <code>null</code> from {@link IconProvider#getIcon()}.
     *
     * @return the default icon
     */
    public Icon getDefaultIcon() {
        return defaultIcon;
    }

    /**
     * Change the default icon to use for items that do not provide their own.
     * The initial value is <code>null</code>, meaning that these items will not
     * show an icon.
     *
     * @param defaultIcon the default icon to set
     */
    public void setDefaultIcon(Icon defaultIcon) {
        this.defaultIcon = defaultIcon;
    }

    private Icon defaultIcon;

    public static class IconRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Icon i = null;
            if (value instanceof IconProvider) {
                i = ((IconProvider) value).getIcon();
            }
            if (i == null) {
                if (list instanceof JIconList) {
                    i = ((JIconList) list).getDefaultIcon();
                }
            }
            setIcon(i);
            return this;
        }
    }

    /**
     * Provides basic list items that will show a specified label and icon in a
     * {@link JIconList}.
     */
    public static class IconItem implements IconProvider {

        private String name;
        private Icon icon;

        /**
         * Creates a new item that will display the specified label and icon
         * when included in a {@link JIconList}. A <code>null</code> label will
         * be treated as an empty string.
         *
         * @param label the item's label
         * @param icon the item's icon
         */
        public IconItem(String label, Icon icon) {
            if (label == null) {
                label = "";
            }
            this.name = label;
            this.icon = icon;
        }

        /**
         * Returns the icon that was specified at construction.
         *
         * @return the item's icon
         */
        @Override
        public Icon getIcon() {
            return icon;
        }

        /**
         * Returns the item label that was specified at construction.
         *
         * @return the item's label
         */
        @Override
        public String toString() {
            return name;
        }
    }
}
