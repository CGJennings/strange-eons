package ca.cgjennings.apps.arkham.dialog.prefs;

import java.util.EventListener;

/**
 * A listener that is notified when the user changes preferences in the
 * {@link Preferences} dialog. To register a listener, see
 * {@link Preferences#addPreferenceUpdateListener}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface PreferenceUpdateListener extends EventListener {

    void preferencesUpdated();
}
