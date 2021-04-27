package resources;

/**
 * A <code>CacheMetrics</code> instance provides information about a cache of
 * objects, and provides limited control over cache behaviour. A cache can
 * register its <code>CacheMetrics</code> instance with the
 * {@link ResourceKit#registerCacheMetrics(resources.CacheMetrics) ResourceKit}
 * to make it available for programmatic access.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see AbstractResourceCache
 */
public interface CacheMetrics {

    /**
     * Returns the number of items held in the cache, or -1 if this information
     * is not available.
     *
     * @return the number of cached objects
     */
    int getItemCount();

    /**
     * Returns an estimate of the total number of bytes consumed by all of the
     * currently cached objects, or -1 if this information is not available.
     *
     * @return the approximate memory footprint of the cache
     */
    long getByteSize();

    /**
     * Clears the cache; requesting a previously cached object through the
     * caching mechanism will cause it to be reloaded. This can be useful during
     * plug-in development if the cache is holding an out-of-date version of a
     * resource. Some caches may not support this feature; in this case the
     * method does nothing.
     */
    void clear();

    /**
     * Returns <code>true</code> if the underlying cache supports clearing with
     * the {@link #clear()} method.
     *
     * @return <code>true</code> if <code>clear()</code> affects the cache
     */
    boolean isClearSupported();

    /**
     * Returns the class that most closely represents the type of cached
     * content.
     *
     * @return the type of objects cached by the cache mechanism represented by
     * this metrics instance
     */
    Class<?> getContentType();

    /**
     * Returns a string that describes the cache and the type of cached content,
     * e.g., "Image Cache".
     *
     * @return a description of the cache
     */
    @Override
    String toString();

    /**
     * Returns a string describing the status of the cache. A typical
     * implementation might be:
     * <pre>
     * return String.format( "%,d items (%,d KiB)",
     *     getItemCount(), (getByteSize() + 512L)/1024L
     * );
     * </pre>
     *
     * @return a string describing the cache state
     */
    String status();
}
