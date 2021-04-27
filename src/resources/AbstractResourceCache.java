package resources;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

/**
 * An abstract base class for creating memory-sensitive caches of objects that
 * can be loaded (or created) on demand from an identifier. The cache operates
 * like a map: objects are requested by their identifier (such as a file or
 * URL). If the object matching the identifier is not already in the cache, it
 * will be
 * {@linkplain #loadResource(java.lang.Object) loaded from the identifier},
 * stored in the cache, and returned. The next time the object is requested
 * using its identifier, it will be returned immediately (without loading) if it
 * is still in the cache. However, if the object has no references to it outside
 * of the cache, then it may be cleared from the cache at any time in order to
 * make more memory available. If it is requested again in the future after
 * being cleared, it will be reloaded.
 *
 * <p>
 * It is guaranteed that every cached object that has no other references to it
 * will be cleared before an <code>OutOfMemory</code> error is thrown. The cache
 * can also be {@linkplain #clear() cleared on demand}, and individual objects
 * {@linkplain #remove(java.lang.Object) removed}.
 *
 * <p>
 * Caches based on this class also support the {@linkplain CacheMetrics
 * cache metrics} registry provided by the
 * {@link ResourceKit#getRegisteredCacheMetrics() ResourceKit}. To use this
 * feature, simply
 * {@linkplain ResourceKit#registerCacheMetrics(resources.CacheMetrics) register}
 * a metrics instance returned by {@link #createCacheMetrics(boolean)}. Then
 * your cache will be listed with others in, for example, the developer tools
 * plug-in.
 *
 * @param I the type of the identifiers used to locate resources (typically a
 * string, file, or URL)
 * @param R the type of the resources stored in the cache
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public abstract class AbstractResourceCache<I, R> {

    private final HashMap<I, SoftReference<R>> map = new HashMap<>();
    private String name;
    private Class<? extends R> type;
    private long lastCleanupTime = System.nanoTime();

    /**
     * Creates a new resource cache with the given name. If the given name is
     * <code>null</code>, a default name will be generated using the name of the
     * concrete class.
     *
     * @param name a descriptive name for the resource cache
     * @throws NullPointerException if the content type is <code>null</code>
     */
    public AbstractResourceCache(Class<? extends R> contentType, String name) {
        if (contentType == null) {
            throw new NullPointerException("contentType");
        }
        if (name == null) {
            name = getClass().getSimpleName();
        }

        type = contentType;
        this.name = name;
    }

    /**
     * Returns the resource associated with the specified identifier. The
     * resource may be returned from the cache, or if it is unavailable in the
     * cache it will be created by calling {@link #loadResource}.
     *
     * <p>
     * If the requested resource does not exist, the result depends on the
     * subclass implementation. Some subclasses may return a default resource to
     * stand in for the requested resource. Others may return <code>null</code>
     * or throw an exception.
     *
     * @param identifier the identifier of the resource to obtain
     * @return the requested resource
     * @throws NullPointerException if the identifier is <code>null</code>
     */
    public final R get(I identifier) {
        if (identifier == null) {
            throw new NullPointerException("identifier");
        }

        identifier = canonicalizeIdentifier(identifier);
        synchronized (map) {
            performScheduledMaintenance();

            SoftReference<R> ref = map.get(identifier);
            if (ref != null) {
                R resource = ref.get();
                if (resource != null) {
                    return resource;
                }
                // was in cache, but GC'd; need to reload
            }
            // resource not available from cache
            R resource = loadResource(identifier);
            if (resource != null && allowCaching(identifier, resource)) {
                map.put(identifier, new SoftReference<>(resource));
            }
            return resource;
        }
    }

    /**
     * Removes the identified object from the cache, if it is present. The
     * object will be reloaded the next time it is requested.
     *
     * @param identifier the identifier of the resource to remove from the cache
     * @throws NullPointerException if the identifier is <code>null</code>
     */
    public final void remove(I identifier) {
        if (identifier == null) {
            throw new NullPointerException("identifier");
        }
        identifier = canonicalizeIdentifier(identifier);
        synchronized (map) {
            performScheduledMaintenance();

            map.remove(identifier);
        }
    }

    /**
     * Clears all cached resources.
     */
    public final void clear() {
        synchronized (map) {
            map.clear();
            // clearing the map effectively cleans it as well
            lastCleanupTime = System.nanoTime();
        }
    }

    /**
     * Returns the number of resources that are currently cached.
     *
     * @return the number of cached objects
     */
    public final int size() {
        int size = 0;
        synchronized (map) {
            performScheduledMaintenance();

            for (SoftReference<R> ref : map.values()) {
                if (ref != null && ref.get() != null) {
                    ++size;
                }
            }
        }
        return size;
    }

    /**
     * Returns an estimate of the amount of memory currently consumed by cached
     * objects, or -1 if an estimate is not available. This method will call
     * {@link #estimateResourceMemoryUse} for each cached resource. If any
     * resource returns -1, this method will return -1. Otherwise, it returns
     * the sum of all of the size estimates of the individual resources.
     *
     * @return current estimated memory consumption, in bytes, or -1
     */
    public final long estimateMemoryUse() {
        long sum = 0L;
        synchronized (map) {
            performScheduledMaintenance();

            for (SoftReference<R> ref : map.values()) {
                if (ref == null) {
                    continue;
                }
                R resource = ref.get();
                if (resource == null) {
                    continue;
                }
                long size = estimateResourceMemoryUse(resource);
                if (size < 0L) {
                    return -1L;
                }
                sum += size;
            }
        }
        return sum;
    }

    /**
     * Normalizes an identifier before attempting to find the cached resource or
     * load the resource from a source. If the same resource can be located
     * using any of several identifiers, this method should convert these
     * equivalent forms into a canonical form in order to prevent the same
     * resource from being loaded and cached under multiple aliases.
     *
     * <p>
     * The base class will return the original identifier without changes.
     *
     * @param identifier the non-<code>null</code>identifier to convert to
     * canonical form
     * @return the canonical form of the identifier
     */
    protected I canonicalizeIdentifier(I identifier) {
        return identifier;
    }

    /**
     * Returns <code>true</code> if this resource should be cached.
     *
     * <p>
     * The base class returns <code>true</code>. Subclasses can override this if
     * they wish to decide whether individual objects should be cached. For
     * example, a subclass might only allow caching of objects if the identifier
     * refers to a read-only storage facility.
     *
     * @param canonicalIdentifier the identifier of the resource object
     * @param loadedResource the resource object, as returned from
     * {@link #loadResource}
     * @return <code>true</code> if the object should be cached
     */
    protected boolean allowCaching(I canonicalIdentifier, R loadedResource) {
        return true;
    }

    /**
     * Loads the identified resource. This method is called when the requested
     * resource is not available in the cache.
     *
     * @param canonicalIdentifier the canonicalized identifier for the resource
     * @return the object to associate with the identifier in the cache
     */
    protected abstract R loadResource(I canonicalIdentifier);

    /**
     * Returns an estimate of how much memory the cached resource is consuming,
     * or -1 if the size is unknown.
     *
     * <p>
     * The base class returns -1. Subclasses should override this if they can
     * provide a reasonable estimate.
     *
     * @param resource the resource to estimate the size of
     * @return an estimate of the memory used by the resource, or -1 if unknown
     */
    protected long estimateResourceMemoryUse(R resource) {
        return -1L;
    }

    /**
     * Called after the cache is accessed to periodically perform a cleanup on
     * the cache. This will remove entries that point to cleared references,
     * freeing up hash table slots and allowing the associated identifiers to be
     * GC'd.
     */
    private void performScheduledMaintenance() {
        final long now = System.nanoTime();
        if (now - lastCleanupTime < CLEANUP_PERIOD) {
            return;
        }

        lastCleanupTime = now;

        synchronized (map) {
            // phase I: collect all stale keys
            LinkedList<I> staleKeys = new LinkedList<>();
            for (Entry<I, SoftReference<R>> entry : map.entrySet()) {
                if (entry.getValue() == null || entry.getValue().get() == null) {
                    staleKeys.add(entry.getKey());
                }
            }

            // phase II: remove stale keys from map
            for (I key : staleKeys) {
                map.remove(key);
            }
        }
    }

    // delay between cleanings, in nanoseconds
    private static final long CLEANUP_PERIOD = 10L * 60L * 1_000_000_000L;

    /**
     * Creates a new cache metrics instance for this cache. Typically, only one
     * instance is created and it is then registered so it can be looked up by
     * support tools.
     *
     * @param allowClearing if <code>false</code>, then the returned metrics
     * instance cannot be used to clear the cache
     * @return a new cache metrics instance that reflects the state of this
     * cache
     * @see ResourceKit#registerCacheMetrics
     */
    public CacheMetrics createCacheMetrics(final boolean allowClearing) {
        return new CacheMetrics() {

            @Override
            public void clear() {
                if (!isClearSupported()) {
                    throw new UnsupportedOperationException();
                } else {
                    AbstractResourceCache.this.clear();
                }
            }

            @Override
            public boolean isClearSupported() {
                return allowClearing;
            }

            @Override
            public long getByteSize() {
                return estimateMemoryUse();
            }

            @Override
            public Class<?> getContentType() {
                return type;
            }

            @Override
            public int getItemCount() {
                return size();
            }

            @Override
            public String status() {
                long size = estimateMemoryUse();
                if (size < 0) {
                    return String.format(
                            "%s (%d %s items)",
                            name, size(), type.getSimpleName()
                    );
                } else {
                    return String.format(
                            "%s (%d %s items, %d kiB)",
                            name, size(), type.getSimpleName(), size / 1_024
                    );
                }
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    /**
     * Returns a string description of the cache useful for debugging purposes.
     *
     * @return a description of the cache
     */
    @Override
    public String toString() {
        String klass = getClass().getSimpleName();
        if (klass.isEmpty()) {
            klass = AbstractResourceCache.class.getSimpleName();
        }
        return klass + '{' + name + ':' + type.getSimpleName() + '}';
    }
}
