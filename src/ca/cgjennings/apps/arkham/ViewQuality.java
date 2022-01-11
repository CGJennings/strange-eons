package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import java.awt.Graphics2D;
import static java.awt.RenderingHints.*;
import java.util.Locale;
import resources.RawSettings;
import resources.Settings;

/**
 * An enumeration of the different preview quality settings available from the
 * <b>View</b> menu. Objects that wish to modify their behaviour based on the
 * view quality setting should always use the static {@link #get()} method of
 * this class to obtain the current setting and not try to read the value
 * directly from settings themselves.
 *
 * <p>
 * Objects can be notified when the view quality changes by registering a
 * property change listener for the property named
 * {@link StrangeEonsAppWindow#VIEW_QUALITY_PROPERTY} with the
 * {@link StrangeEonsAppWindow}. For example, the following Java code would
 * print a message to the standard error stream whenever the quality changes:
 * <pre>
 * StrangeEons.getWindow().addPropertyChangeListener(
 * 	StrangeEonsAppWindow.VIEW_QUALITY_PROPERTY,
 * 	new PropertyChangeListener() {
 * 		public void propertyChange( PropertyChangeEvent evt ) {
 * 			final ViewQuality oldVQ = (ViewQuality) evt.getOldValue();
 * 			final ViewQuality newVQ = (ViewQuality) evt.getNewValue();
 * 			System.err.println( "The quality changed from " + oldVQ + " to " + newVQ );
 * 			// do something useful...
 * 		}
 * 	}
 * );
 * </pre>
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public enum ViewQuality {
    LOW(RenderTarget.FAST_PREVIEW, 1d) {
        @Override
        public void applyPreviewWindowHints(Graphics2D g) {
            g.setRenderingHint(KEY_RENDERING, VALUE_RENDER_SPEED);
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_DEFAULT);
            g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setRenderingHint(KEY_ALPHA_INTERPOLATION, VALUE_ALPHA_INTERPOLATION_SPEED);
            g.setRenderingHint(KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_SPEED);
        }
    },
    MEDIUM(RenderTarget.PREVIEW, 1d) {
        @Override
        public void applyPreviewWindowHints(Graphics2D g) {
            g.setRenderingHint(KEY_RENDERING, VALUE_RENDER_SPEED);
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_DEFAULT);
            g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(KEY_ALPHA_INTERPOLATION, VALUE_ALPHA_INTERPOLATION_SPEED);
            g.setRenderingHint(KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_SPEED);
        }
    },
    HIGH(RenderTarget.PREVIEW, 2d) { // 150 ppi -> 200 ppi
        @Override
        public void applyPreviewWindowHints(Graphics2D g) {
            g.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(KEY_ALPHA_INTERPOLATION, VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.setRenderingHint(KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_QUALITY);
        }
    },
    ULTRAHIGH(RenderTarget.EXPORT, 3d) { // 150 ppi -> 300 ppi
        @Override
        public void applyPreviewWindowHints(Graphics2D g) {
            g.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(KEY_ALPHA_INTERPOLATION, VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.setRenderingHint(KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_QUALITY);
        }
    };

    ViewQuality(RenderTarget quality, double sheetUpsample) {
        q = quality;
        upsample = sheetUpsample;
    }

    private final RenderTarget q;
    private final double upsample;

    /**
     * Returns an appropriate rendering target to use when rendering sheets at
     * this view quality.
     *
     * @return a rendering quality appropriate for the view quality
     */
    public RenderTarget getRenderTarget() {
        return q;
    }

    /**
     * Returns the upsampling factor to use for a sheet in the sheet viewer. To
     * render a sheet, multiply this by {@link Sheet#getTemplateResolution()}.
     *
     * @param sheet the sheet to determine the upsample factor for
     * @return the suggested upsample factor for the sheet at this quality level
     */
    public double getSheetViewerUpsample(Sheet sheet) {
        return Math.max(upsample, sheet.getSuggestedUpsampleFactor());
    }

    public abstract void applyPreviewWindowHints(Graphics2D g);

    private static ViewQuality current;
    private static boolean auto;

    /**
     * Sets the current view quality. If the new value differs from the current
     * value, then the application window will fire a
     * {@link StrangeEonsAppWindow#VIEW_QUALITY_PROPERTY} property change.
     *
     * @param newQuality the new quality setting to apply
     */
    synchronized static void set(ViewQuality newQuality) {
        if (newQuality == null) {
            throw new NullPointerException("quality");
        }
        if (current == null) {
            get();
        }
        if (current != newQuality) {
            ViewQuality old = current;
            current = newQuality;
            StrangeEonsAppWindow app = StrangeEons.getWindow();
            RawSettings.setUserSetting(KEY, current.name().toLowerCase(Locale.CANADA));
            if (app == null) {
                StrangeEons.log.warning("null window");
            } else {
                app.propertyChange(StrangeEonsAppWindow.VIEW_QUALITY_PROPERTY, old, current);
            }
        }
    }

    /**
     * Returns the current view quality.
     *
     * @return the current quality setting
     */
    public synchronized static ViewQuality get() {
        if (current == null) {
            String v = RawSettings.getSetting(KEY);
            if (v != null) {
                try {
                    current = valueOf(v.toUpperCase(Locale.CANADA));
                } catch (IllegalArgumentException e) {
                }
            }
            if (current == null) {
                current = HIGH;
            }
            v = RawSettings.getSetting(AUTO);
            if (v != null) {
                auto = Settings.yesNo(v);
            } else {
                auto = true;
            }
        }
        if (auto) {
            return HIGH;
        }
        return current;
    }

    synchronized static void setManagedAutomatically(boolean manage) {
        if (manage != auto) {
            auto = manage;
        }
    }

    /**
     * Returns {@code true} if the view quality is set to automatic management
     * mode. When automatic management is enabled, {@link #get()} will return a
     * fixed value but specific rendering systems may choose to use a different
     * quality setting depending on system performance.
     *
     * @return {@code true} if rendering systems may manage performance
     * automatically
     */
    public synchronized static boolean isManagedAutomatically() {
        if (current == null) {
            get();
        }
        return auto;
    }

    private static final String KEY = "view-quality";
    private static final String AUTO = "view-quality-auto";
}
