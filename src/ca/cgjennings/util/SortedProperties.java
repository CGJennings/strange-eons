package ca.cgjennings.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A subclass of <code>java.util.Properties</code> that stores its entries in
 * sorted order.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 1.5
 */
@SuppressWarnings("serial")
public class SortedProperties extends Properties {

    /**
     * Creates a new, empty sorted properties instance with no parent.
     */
    public SortedProperties() {
        super();
    }

    /**
     * Creates a new, empty sorted properties instance with the specified
     * parent.
     *
     * @param defaults the properties instance that determines the default
     * values for this instance
     */
    public SortedProperties(Properties defaults) {
        super(defaults);
    }

    /**
     * Returns the keys of all properties stored in this instance, in sorted
     * order.
     *
     * @return an enumeration of the property keys, in sorted order
     */
    @Override
    public synchronized Enumeration<Object> keys() {
        SortedSet<Object> sorted = new TreeSet<>(getComparator());
        sorted.addAll(super.keySet());
        return Collections.enumeration(sorted);
    }

    /**
     * Return a comparator that will be used to sort the keys. If
     * <code>null</code> is returned, keys will be sorted according to their
     * natural order.
     *
     * @return the comparator implementing the sort order, or <code>null</code>
     */
    public synchronized Comparator<Object> getComparator() {
        return comparator;
    }

    /**
     * Set the comparator that will be used to sort the keys. If
     * <code>null</code>, keys will be sorted according to their natural order.
     *
     * @param comparator the comparator implementing the sort order, or
     * <code>null</code>
     */
    public synchronized void setComparator(Comparator<Object> comparator) {
        this.comparator = comparator;
    }

    private Comparator<Object> comparator = null;
}
