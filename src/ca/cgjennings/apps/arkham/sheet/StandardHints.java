package ca.cgjennings.apps.arkham.sheet;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.awt.RenderingHints;
import static java.awt.RenderingHints.*;
import java.util.logging.Level;

/**
 * Standard rendering hints, used for various {@link RenderTarget}s.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.2
 */
public final class StandardHints {
    private StandardHints() {
    }

    static final RenderingHints FAST_PREVIEW;
    static final RenderingHints PREVIEW;
    static final RenderingHints EXPORT;
    static final RenderingHints PRINT;

    static {
        PRINT = new RenderingHints(null);
        EXPORT = new RenderingHints(null);
        PREVIEW = new RenderingHints(null);
        FAST_PREVIEW = new RenderingHints(null);
        reset();
    }

    /** For testing and experimentation. Resets all hints to their default values. */
    public static void reset() {
        PRINT.clear();
        PRINT.put(KEY_ALPHA_INTERPOLATION, VALUE_ALPHA_INTERPOLATION_QUALITY);
        PRINT.put(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        PRINT.put(KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_QUALITY);
        PRINT.put(KEY_DITHERING, VALUE_DITHER_ENABLE);
        PRINT.put(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON);
        PRINT.put(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
        PRINT.put(KEY_RENDERING, VALUE_RENDER_QUALITY);
        PRINT.put(KEY_STROKE_CONTROL, VALUE_STROKE_PURE);
        PRINT.put(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);

        EXPORT.clear();
        EXPORT.add(PRINT);

        PREVIEW.clear();
        PREVIEW.add(PRINT);
        PREVIEW.put(KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_SPEED);
        PREVIEW.put(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
        PREVIEW.put(KEY_STROKE_CONTROL, VALUE_STROKE_DEFAULT);

        FAST_PREVIEW.clear();
        FAST_PREVIEW.add(PREVIEW);
        FAST_PREVIEW.put(KEY_INTERPOLATION, VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        FAST_PREVIEW.put(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_DEFAULT);
    }

    /**
     * For testing and experimentation.
     * Changes a hint for all render targets or the specified target.
     */
    public static void set(Object key, Object value, RenderTarget renderTarget) {
        if (renderTarget == null || renderTarget == RenderTarget.PRINT) {
            PRINT.put(key, value);
        }
        if (renderTarget == null || renderTarget == RenderTarget.EXPORT) {
            EXPORT.put(key, value);
        }
        if (renderTarget == null || renderTarget == RenderTarget.PREVIEW) {
            PREVIEW.put(key, value);
        }
        if (renderTarget == null || renderTarget == RenderTarget.FAST_PREVIEW) {
            FAST_PREVIEW.put(key, value);
        }
    }

    /**
     * For testing and experimentation.
     * Changes a hint for all render targets or the specified target.
     * Uses reflection to specify keys and values from strings, which do not have to
     * include the KEY_ or VALUE_ prefix.
     *
     * <p>Example script use:
     *
     * <pre>
     * arkham.sheet.StandardHints.reset();
     * arkham.sheet.StandardHints.set("TEXT_ANTIALIASING", "TEXT_ANTIALIAS_OFF");
     * Eons.window.redrawPreviews();
     * let timer = new javax.swing.Timer(2000, function() {
     *   arkham.sheet.StandardHints.reset();
     * 	 Eons.window.redrawPreviews();
     * });
     * timer.setRepeats(false);
     * timer.start();
     * </pre>
     */
    public static void set(String key, String value) {
        try {
            if (!key.startsWith("KEY_")) {
                key = "KEY_" + key;
            }
            if (!value.startsWith("VALUE_")) {
                value = "VALUE_" + value;
            }
            Object theKey = RenderingHints.class.getField(key).get(null);
            Object theValue = RenderingHints.class.getField(value).get(null);
            set(theKey, theValue, null);
        } catch (Throwable t) {
            StrangeEons.log.log(Level.WARNING, "ignored attempt to set " + key, t);
        }
    }
}
