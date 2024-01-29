package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.deck.item.PageItem;
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

/**
 * Renderer for page item lists in the deck editor.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
final class PageItemRenderer extends DefaultListCellRenderer {

    private String searchTerm = null;

    public PageItemRenderer() {
        setOpaque(true);
        setHorizontalAlignment(JLabel.LEFT);
        setVerticalAlignment(JLabel.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        PageItem item = (PageItem) value;
        super.getListCellRendererComponent(list, item.getName(), index, isSelected, cellHasFocus);
        setIcon(item.getThumbnailIcon());

        if (getSearchTerm() != null && getSearchTerm().length() > 0) {
            if (!((PageItem) value).getName().toLowerCase().contains(searchTerm)) {
                setEnabled(false);
            }
        }
        return this;
    }

//	@Override
//	protected void paintComponent(Graphics g) {
//		// this prevents the renderer from breaking long HTML labels over multiple lines
//		setSize(100000, getHeight());
//		super.paintComponent(g);
//	}
    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }
}
