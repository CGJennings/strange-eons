package ca.cgjennings.apps.arkham.sheet;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.ui.IconProvider;
import java.util.Locale;
import java.util.logging.Level;
import javax.swing.Icon;
import resources.Language;
import resources.ResourceKit;
import resources.Settings;

/**
 * Collapses the set of possible finish options into a collection of simple
 * choices for users to choose from.
 *
 * @author Henrik Rostedt
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.3
 */
public enum FinishStyle implements IconProvider {
    /**
     * Cut rounded corners if
     * {@linkplain Sheet#getCornerRadius() specified by the sheet}, otherwise
     * this is the same as {@code SQUARE}.
     */
    ROUND(-1d),
    /**
     * Use square corners with no bleed margin.
     */
    SQUARE(0d),
    /**
     * Include a bleed margin.
     */
    MARGIN(9d);

    private FinishStyle(double baseUserBleedValue) {
        bleedBase = baseUserBleedValue;    
    }

    private double bleedBase;
    private static final String KEY_DEFAULT_BLEED_MARGIN = "preview-bleed-margin";

    /**
     * Settings key that stores previewer finish style.
     */
    private static final String KEY_PREVIEW_FINISH = "preview-finish";

    /**
     * Applies this setting to a sheet. If a bleed margin is requested, applies
     * the default preview bleed margin size.
     *
     * @param target the non-null sheet to apply this option to
     */
    public void applyTo(Sheet target) {
        double ubm = bleedBase;
        if (ubm > 0d) {
            ubm = Settings.getUser().getDouble(KEY_DEFAULT_BLEED_MARGIN, 9d);
        }
        target.setUserBleedMargin(ubm);
    }

    /**
     * Applies this setting to a sheet. If a bleed margin is requested, applies
     * the the specified margin size.
     *
     * @param target the non-null sheet to apply this option to
     */
    public void applyTo(Sheet target, double marginSize) {
        double ubm = bleedBase;
        if (ubm > 0d) {
            ubm = Math.max(0d, marginSize);
        }
        target.setUserBleedMargin(ubm);
    }
    
    /**
     * Returns the suggested user bleed margin for this setting.
     * @return the user bleed margin to use for this option, or a suggested
     * margin for type {@link #MARGIN}.
     */
    public double getSuggestedBleedMargin() {
        return bleedBase;
    }

    /**
     * Convert this to a
     * {@linkplain Settings#set(java.lang.String, java.lang.String) setting value}.
     *
     * @return a suitable setting value
     */
    public String toSetting() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Convert a {@linkplain Settings#get(java.lang.String) setting value} to a
     * finish style, returning a default value if the setting key is missing or
     * malformed. The setting is decoded by matching against the declared
     * constant name, ignoring case leading/trailing space.
     *
     * @param v the setting value to decode
     * @return the decoded setting value
     */
    public static FinishStyle fromSetting(String v) {
        FinishStyle style = ROUND;
        if (v != null) {
            try {
                style = valueOf(v.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException iae) {
                // use ROUND
                StrangeEons.log.log(Level.WARNING, "ignoring bad finish style: {0}", v);
            }
        }
        return style;
    }

    /**
     * Given a sheet, converts its user bleed margin into a matching finish
     * style.
     *
     * @param s the non-null sheet to read
     * @return the sheet's finish style
     */
    public static FinishStyle fromSheet(Sheet s) {
        double ubm = s.getUserBleedMargin();
        if (ubm < 0d) {
            return ROUND;
        }
        if (ubm == 0d) {
            return SQUARE;
        }
        return MARGIN;
    }
    
    /**
     * Returns an icon representing this style.
     * @return a UI icon for the style
     */
    public Icon getIcon() {
        return ResourceKit.getIcon("ui/view/finish-" + toSetting() + ".png");
    }

    /**
     * Applies the default style for image previews to the sheet.
     *
     * @param s the sheet to set the user-selected style upon
     */
    public static void applyPreviewStyleToSheet(Sheet s) {
        getPreviewStyle().applyTo(s);
    }

    /**
     * Updates user settings with a new finish style for image previews.
     */
    public void setAsPreviewStyle() {
        Settings.getUser().set(KEY_PREVIEW_FINISH, toSetting());
    }

    /**
     * Returns the current finish style for image previews from user settings.
     *
     * @return the user-selected finish style
     */
    public static FinishStyle getPreviewStyle() {
        return fromSetting(Settings.getUser().get(KEY_PREVIEW_FINISH));
    }

    /**
     * Returns a localized description of the finish.
     *
     * @return a non-null localized string
     */
    @Override
    public String toString() {
        return Language.string("app-finish-style-" + toSetting());
    }
}
