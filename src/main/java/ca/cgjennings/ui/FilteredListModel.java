package ca.cgjennings.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.AbstractListModel;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.event.DocumentEvent;

/**
 * A model for {@code JList}s that supports filtering. The {@code ListModel}
 * methods {@code getElementAt} and {@code getSize} return values appropriate
 * for the applied filter. It is recommended that you get and set the selection
 * using values rather than indices.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class FilteredListModel<E> extends AbstractListModel<E> {

    private final List<E> filtered = new ArrayList<>();
    private final List<E> list = new ArrayList<>();

    public FilteredListModel() {
    }

    public FilteredListModel(E[] items) {
        this();
        for (int i = 0; i < items.length; ++i) {
            add(items[i]);
        }
    }

    public FilteredListModel(Collection<? extends E> items) {
        this();
        for (E it : items) {
            add(it);
        }
    }

    public void add(E item) {
        list.add(item);
        if (test(item)) {
            int index = filtered.size();
            filtered.add(item);
            fireIntervalAdded(this, index, index);
        }
    }

    public void add(int index, E item) {
        list.add(index, item);
        if (test(item)) {
            index = getFilteredIndex(index);
            filtered.add(index, item);
            fireIntervalAdded(this, index, index);
        }
    }

    /**
     * Returns the item at the <i>unfiltered</i> index.
     *
     * @param index the model index
     * @return the item at the specified index
     */
    public Object getItem(int index) {
        return list.get(index);
    }

    /**
     * Returns the size of the <i>unfiltered</i> list.
     *
     * @return the unfiltered model size
     */
    public int getItemCount() {
        return list.size();
    }

    /**
     * Removes all elements from the list.
     */
    public void clear() {
        int index1 = filtered.size();
        filtered.clear();
        list.clear();
        if (index1 > 0) {
            fireIntervalRemoved(this, 0, index1 - 1);
        }
    }

    /**
     * Returns the index of the first object equal to {@code o}, ignoring the
     * current filter. This is useful when the list is an index into a list of
     * objects and the indexed object must be retrieved.
     *
     * @param o the object to find the index of
     * @return the unfiltered index of the first such object, or -1
     */
    public int getUnfilteredIndex(Object o) {
        return list.indexOf(o);
    }

    private int getFilteredIndex(int unfilteredIndex) {
        if (!test(list.get(unfilteredIndex))) {
            return -1;
        }
        int findex = 0;
        for (int i = 0; i < unfilteredIndex; ++i) {
            if (test(list.get(i))) {
                ++findex;
            }
        }
        return findex;
    }

    public void setFilter(ListFilter f) {
        filter = f;
        if (list.isEmpty()) {
            return;
        }
        int fsize = filtered.size();
        if (fsize > 0) {
            fireIntervalRemoved(this, 0, fsize - 1);
        }
        filtered.clear();
        int size = list.size();
        for (int i = 0; i < size; ++i) {
            E item = list.get(i);
            if (test(item)) {
                filtered.add(item);
            }
        }
        fsize = filtered.size();
        if (fsize > 0) {
            fireIntervalAdded(this, 0, fsize - 1);
        }
    }

    /**
     * Returns true if the item is allowed by the current filter.
     */
    private boolean test(Object o) {
        if (filter == null) {
            return true;
        }
        return filter.include(this, o);
    }

    private ListFilter filter;

    public interface ListFilter {

        public boolean include(FilteredListModel model, Object item);
    }

    /**
     * A list filter that can be selected from a list of filters.
     */
    public static abstract class ChoosableListFilter implements IconProvider {

        private final Icon icon;
        private final String name;

        public ChoosableListFilter(String name, Icon icon) {
            this.name = name;
            this.icon = icon;
        }

        @Override
        public Icon getIcon() {
            return icon;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static ListFilter createRegexFilter(final String regex) {
        return new ListFilter() {
            Pattern p = Pattern.compile(regex);

            @Override
            public boolean include(FilteredListModel model, Object item) {
                if (item == null) {
                    return false;
                }
                return p.matcher(item.toString()).find();
            }
        };
    }

    public static ListFilter createStringFilter(String substring) {
        return createRegexFilter("(?i)(?u)" + Pattern.quote(substring));
    }

    /**
     * This is a convenience method that attaches this model to the current
     * document of the specified search field. When the text in the document
     * changes, the text from the field will be used to create a string filter,
     * which will then be applied to the list.
     *
     * @param field the field to link this model to
     * @param list the list whose items are filtered
     * @param restoreSelection if true, any selection is restore after updating
     * the list
     * @param T the item type of the target list
     */
    public <T> void linkTo(final JFilterField field, final JList<T> list, final boolean restoreSelection) {
        field.getDocument().addDocumentListener(new DocumentEventAdapter() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                List<T> selValues = restoreSelection ? list.getSelectedValuesList() : null;
                setFilter(createStringFilter(field.getText()));
                if (selValues != null) {
                    for (Object sel : selValues) {
                        for (int i = 0; i < getSize(); ++i) {
                            if (sel.equals(getElementAt(i))) {
                                list.addSelectionInterval(i, i);
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public E getElementAt(int index) {
        return filtered.get(index);
    }

    @Override
    public int getSize() {
        return filtered.size();
    }

    public static final ListFilter ACCEPT_ALL_FILTER = (FilteredListModel mode, Object item) -> true;

    public interface FilterChangeListener extends EventListener {

        void filterChanged(Object source);
    }
}
