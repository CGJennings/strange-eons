package ca.cgjennings.apps.arkham.plugins.typescript;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

/**
 * Given a Java import, returns a {@code .d.ts} type definition file.
 */
public class JavaTypes {
    private JavaTypes() {
    }
    
    private static long FLUSH_CHECK_PERIOD = 60*1000L;
    private static long FLUSH_AFTER = 5*60*1000L;

    private static class CacheEntry {
        long time;
        String value;
    }
    
    private static Map<String,CacheEntry> cache = new HashMap<>();
    private static Timer cacheFlushTimer;
    
    private static void scheduleCacheFlush() {
        synchronized (JavaTypes.class) {
            if (cacheFlushTimer == null) {
                cacheFlushTimer = new Timer("TS type cache cleanup", true);
                cacheFlushTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        flushCache();
                    }
                }, Math.max(FLUSH_AFTER, FLUSH_CHECK_PERIOD), FLUSH_CHECK_PERIOD);
            }
        }
    }
    
    private static void flushCache() {
        synchronized (JavaTypes.class) {
            final long ageLimit = System.currentTimeMillis() - FLUSH_AFTER;
            var it = cache.entrySet().iterator();
            while(it.hasNext()) {
                var e = it.next();
                if (e.getValue().time < ageLimit) {
                    it.remove();
                }
            }
            if (cache.isEmpty()) {
                cacheFlushTimer.cancel();
                cacheFlushTimer = null;
                cache = null;
            }
        }
    }
    
    /**
     * Given a Java import like "java.io.File", returns a {@code .d.ts} type
     * definition file for the class.
     * 
     * @param importName the class name to import
     * @return a TypeScript type definition for the class
     */
    public static String getTypeScriptTypeInfo(String importName) {
        synchronized (JavaTypes.class) {
            final long now = System.currentTimeMillis();
            final var cached = cache.get(importName);
            if (cached != null) {
                cached.time = now;
                return cached.value;
            }

            Class<?> klass;
            try {
                klass = Class.forName(importName);
            } catch (ClassNotFoundException cnf) {
                // do not cache: might be added to classpath later
                return null;
            }
            
            try {
                var entry = new CacheEntry();
                entry.time = now;
                entry.value = define(klass);
                cache.put(importName, entry);
                scheduleCacheFlush();
                return entry.value;
            } catch (Throwable t) {
                StrangeEons.log.log(Level.SEVERE, "exception while typing " + importName, t);
            }
            return null;
        }
    }
    
    private static String define(Class<?> c) {
        String name = c.getSimpleName();
        return "declare type " + name + " = any;";
    }
}
