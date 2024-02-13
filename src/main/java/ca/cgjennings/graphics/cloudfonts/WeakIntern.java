package ca.cgjennings.graphics.cloudfonts;

import java.util.HashMap;

/**
 * A simple weak intern pool for strings.
 * When this object is garbage collected, the interned strings will be
 * eligible for garbage collection as soon as they are no longer
 * referenced elsewhere. Not thread safe.
 */
final class WeakIntern {
    public WeakIntern() {        
    }

    private final HashMap<String, String> interned = new HashMap<>();

    public String of(String s) {
        if (s == null) {
            return null;
        }
        String internedValue = interned.get(s);
        if (internedValue == null) {
            interned.put(s, s);
            return s;
        }
        return internedValue;
    }
}