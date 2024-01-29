package ca.cgjennings.graphics.filters;

import java.util.Arrays;

/**
 * A filter that can apply multiple {@link AbstractPixelwiseFilter}s in
 * sequence. Using this filter to combine multiple filters is faster than
 * applying each filter in sequence because it avoids the overhead of unpacking
 * and repacking the image data multiple times.
 *
 * <p>
 * <b>In-place filtering:</b> This class supports in-place filtering (the source
 * and destination images may be the same).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class CompoundPixelwiseFilter extends AbstractPixelwiseFilter {

    AbstractPixelwiseFilter[] filters;

    /**
     * Creates a new compound filter with an empty filter list. If the list is
     * not changed, this filter will have the same effect as a
     * {@link CloneFilter}.
     */
    public CompoundPixelwiseFilter() {
        filters = new AbstractPixelwiseFilter[0];
    }

    /**
     * Creates a new compound filter with the specified filter list.
     *
     * @param filters an array of filters to apply in sequence
     */
    public CompoundPixelwiseFilter(AbstractPixelwiseFilter... filters) {
        setFilters(filters);
    }

    /**
     * Returns a copy of the filter list as an array.
     *
     * @return the filters applied by this compound filter
     */
    public AbstractPixelwiseFilter[] getFilters() {
        return filters.clone();
    }

    /**
     * Sets the filters to be applied by this compound filter.
     *
     * @param filters an array of filters to apply in sequence
     */
    public void setFilters(AbstractPixelwiseFilter... filters) {
        if (filters == null) {
            throw new NullPointerException("filters");
        }
        filters = filters.clone();
        for (int i = 0; i < filters.length; ++i) {
            if (filters[i] == null) {
                throw new NullPointerException("filters[" + i + "]");
            }
        }
        this.filters = filters.clone();
    }

    /**
     * Appends a new filter to the current filter list, replacing the existing
     * list.
     *
     * @param filter the new filter to append
     */
    public void appendFilter(AbstractPixelwiseFilter filter) {
        if (filter == null) {
            throw new NullPointerException("filter");
        }
        final int len = filters.length + 1;
        filters = Arrays.copyOf(filters, len + 1, AbstractPixelwiseFilter[].class);
        filters[len] = filter;
    }

    /**
     * Returns the filter at the specified index in the list of filters.
     *
     * @param index the index in the filter list, from 0 to
     * {@link #getSize()}-1.
     * @return the filter at the specified index
     */
    public AbstractPixelwiseFilter getFilter(int index) {
        if (index < 0 || index >= filters.length) {
            throw new IndexOutOfBoundsException("index: " + index);
        }
        return filters[index];
    }

    /**
     * Returns the number of filters that will be applied by this compound
     * filter.
     *
     * @return the number of filters in the list
     */
    public int getSize() {
        return filters.length;
    }

    @Override
    public void filterPixels(int[] argb, int start, int end) {
        for (int i = 0; i < filters.length; ++i) {
            filters[i].filterPixels(argb, start, end);
        }
    }

    @Override
    protected float workFactor() {
        float sum = 0.5f;
        for (int i = 0; i < filters.length; ++i) {
            sum += filters[i].workFactor() - 0.5f;
        }
        return Math.max(sum, 1f);
    }
}
