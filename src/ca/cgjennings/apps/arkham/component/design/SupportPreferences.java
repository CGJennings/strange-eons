package ca.cgjennings.apps.arkham.component.design;

import ca.cgjennings.apps.arkham.sheet.Sheet;
import resources.Settings;

/**
 * This is a utility class that can be used by design support implementations to
 * look up the current state of the various design support preference settings.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class SupportPreferences {

    private SupportPreferences() {
    }

    /**
     * Returns <code>true</code> if the user has requested that game component
     * editors show design support information for components with design
     * support.
     *
     * @return <code>true</code> if editors should show design support
     */
    public static boolean isDesignSupportEnabled() {
        return Settings.getUser().getBoolean("show-consequence-displays");
    }

    /**
     * Returns the level of feedback that the user has requested when a
     * component is considered invalid. Game components that consider validity
     * (game balance, fairness) as part of their design support analysis should
     * also add support to the component's {@link Sheet}s to display feedback
     * when the component is invalid. This setting indicates how bold this
     * feedback should be.
     *
     * @return a value that reflects how obviously invalid components should be
     * marked
     */
    public static FeedbackLevel getValidationFeedbackLevel() {
        Settings s = Settings.getUser();
        if (s.getBoolean("use-validation")) {
            if (s.getBoolean("less-obvious-balance-warning")) {
                return FeedbackLevel.SUBTLE;
            }
            return FeedbackLevel.OBVIOUS;
        }
        return FeedbackLevel.DISABLED;
    }

    /**
     * Returns <code>true</code> if the user has requested that components
     * should be validated more strictly. The exact interpretation of this is
     * left up to the design support, but generally it means that borderline
     * cases that may have simply been noted in the design support view ought to
     * invalidate the component.
     *
     * @return <code>true</code> if validation is enabled and the user has
     * requested strict validation mode
     */
    public static boolean isStrictValidationEnabled() {
        Settings s = Settings.getUser();
        if (s.getBoolean("use-validation")) {
            return s.getBoolean("use-strict-validation");
        }
        return false;
    }

    /**
     * An enumeration of the different levels of validation feedback that can be
     * requested for component sheets.
     */
    public static enum FeedbackLevel {
        /**
         * Do not change how components are drawn when invalid.
         */
        DISABLED,
        /**
         * When a component is invalid, its sheets should indicate this in a
         * subtle way.
         */
        SUBTLE,
        /**
         * When a component is invalid, its sheets should indicate this in a way
         * that is impossible to miss, even at a glance.
         */
        OBVIOUS
    }
}
