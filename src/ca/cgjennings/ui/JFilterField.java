package ca.cgjennings.ui;

import ca.cgjennings.apps.arkham.MarkupTargetFactory;
import javax.swing.border.Border;
import javax.swing.text.Document;
import resources.ResourceKit;

/**
 * A text field that allows users to search a list or other component.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class JFilterField extends JLabelledField {

    public JFilterField(Document doc, String text, int columns) {
        super(doc, text, columns);
        init();
    }

    public JFilterField(String text, int columns) {
        super(text, columns);
        init();
    }

    public JFilterField(int columns) {
        super(columns);
        init();
    }

    public JFilterField(String text) {
        super(text);
        init();
    }

    public JFilterField() {
        super();
        init();
    }

    private void init() {
        setSearchBorder(getBorder());
        setFont(getFont().deriveFont(getFont().getSize2D() - 1f));
        MarkupTargetFactory.enableTargeting(this, false);
    }

    /**
     * Filter fields use a special border that includes a search icon. If you
     * want to change the filter field's border but keep the search icon, call
     * this method with your new border. If you want to replace the border
     * completely, including the search icon, call <code>setBorder</code>
     * instead.
     *
     * @param b the new border to set
     */
    public void setSearchBorder(Border b) {
        setBorder(b);
        new IconBorder(ResourceKit.getIcon("ui/find-sm.png")).install(this);
    }

    protected void fireFilterChangedEvent() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == FilteredListModel.FilterChangeListener.class) {
                ((FilteredListModel.FilterChangeListener) listeners[i + 1]).filterChanged(this);
            }
        }
    }

    public void addFilterChangedListener(FilteredListModel.FilterChangeListener l) {
        listenerList.add(FilteredListModel.FilterChangeListener.class, l);
    }

    public void removeFilterChangedListener(FilteredListModel.FilterChangeListener l) {
        listenerList.remove(FilteredListModel.FilterChangeListener.class, l);
    }

    public Object getFilterValue() {
        return getText();
    }
}
