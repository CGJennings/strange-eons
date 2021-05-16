package ca.cgjennings.apps.arkham.project;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import resources.CacheMetrics;
import resources.ResourceKit;

/**
 * Used by metadata sources to cache expensive information. A metadata cache can
 * associate any arbitrary object with a project member. The cache monitors file
 * modification times and will not return a cached value if they do not match.
 * The entire cache is cleared when an entry from a different project is added.
 * Cache entries are also cleared when memory runs low to free up space.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class MetadataCache<T> {

    /**
     * Creates a new, empty cache.
     */
    public MetadataCache() {
        cleanupTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                clean();
            }
        },
                TIME_TO_LIVE, TIME_TO_LIVE / 2
        );
        synchronized (metrics) {
            registry.add(new SoftReference<>(this));
        }
    }

    /**
     * Returns the cached object for the member, if available and still valid.
     * Otherwise, returns {@code null}, in which case the cache object
     * should be regenerated and added with {@link #put}.
     *
     * @param member the member to get a cached object for
     * @return the cached object, or {@code null} if the cached object is
     * out of date or not in the cache
     */
    public synchronized T get(Member member) {
        final SoftReference<Entry<T>> ref = cache.get(member);
        if (ref == null) {
            return null;
        }

        final Entry<T> e = ref.get();
        if (e == null || member.getFile().lastModified() != e.lastModified) {
            cache.remove(member);
            return null;
        }

        e.lastAccessed = System.currentTimeMillis();
        return e.item;
    }

    /**
     * Associates the specified object with the specified {@link Member} in the
     * cache.
     *
     * @param member the member to associate the item with
     * @param item the item to cache for later retrieval
     */
    public synchronized void put(Member member, T item) {
        if (member.getProject() != proj) {
            if (!cache.isEmpty()) {
                cache.clear();
            }
            proj = member.getProject();
        }

        final Entry<T> e = new Entry<>(member, item);
        cache.put(member, new SoftReference<>(e));
    }

    /**
     * Removes any item that may be associated with the specified member from
     * the cache.
     *
     * @param member the member to remove from the cache
     */
    public synchronized void remove(Member member) {
        cache.remove(member);
    }

    /**
     * Clears all entries from the cache.
     */
    public synchronized void clear() {
        cache.clear();
    }

    /**
     * Cleans up entries in the cache; for example, removes entries for deleted
     * files and entries that have not been accessed in a long time. Calling
     * this method is not required for proper cache performance, but it may
     * improve efficiency. On construction, the cache will start a timer that
     * calls this periodically.
     */
    protected synchronized void clean() {
        Iterator<Member> it = cache.keySet().iterator();
        while (it.hasNext()) {
            Member m = it.next();
            if (!m.getFile().exists()) {
                it.remove();
                continue;
            }
            SoftReference<Entry<T>> ref = cache.get(m);
            if (ref == null) {
                it.remove();
                continue;
            }
            Entry<T> e = ref.get();
            if (e == null) {
                it.remove();
                continue;
            }
            if ((System.currentTimeMillis() - e.lastAccessed) >= TIME_TO_LIVE) {
                it.remove();
            }
        }
    }
    // entries not looked up in last 15 minutes will be deleted by clean
    private static final long TIME_TO_LIVE = 1_000 * 60 * 15;

    private Project proj;
    private Map<Member, SoftReference<Entry<T>>> cache = new HashMap<>();

    private static class Entry<T> {

        Entry(Member m, T item) {
            this.item = item;
            lastModified = m.getFile().lastModified();
            lastAccessed = System.currentTimeMillis();
        }
        T item;
        long lastModified;
        long lastAccessed;
    }

    private static Timer cleanupTimer = new Timer("Project MetadataCache cleanup thread", true);

    private static final CacheMetrics metrics = new CacheMetrics() {
        private synchronized int walk(boolean clear) {
            int retval = 0;

            Iterator<SoftReference<MetadataCache<?>>> it = registry.iterator();
            while (it.hasNext()) {
                MetadataCache<?> mdc = it.next().get();
                if (mdc == null) {
                    it.remove();
                } else {
                    if (clear) {
                        mdc.clear();
                    } else {
                        synchronized (mdc) {
                            retval += mdc.cache.size();
                        }
                    }
                }
            }

            return retval;
        }

        @Override
        public int getItemCount() {
            return walk(false);
        }

        @Override
        public long getByteSize() {
            return -1L;
        }

        @Override
        public void clear() {
            walk(true);
        }

        @Override
        public boolean isClearSupported() {
            return true;
        }

        @Override
        public Class<?> getContentType() {
            return Object.class;
        }

        @Override
        public synchronized String status() {
            // get the item count first size this will clean
            // null entries from registry, making subcaches more accurate
            int items = getItemCount();
            int subcaches = registry.size();

            return String.format("%,d items in %,d typed subcaches",
                    items, subcaches
            );
        }

        @Override
        public String toString() {
            return "Project metadata cache";
        }
    };
    private static final HashSet<SoftReference<MetadataCache<?>>> registry = new HashSet<>();

    static {
        ResourceKit.registerCacheMetrics(metrics);
    }
}
