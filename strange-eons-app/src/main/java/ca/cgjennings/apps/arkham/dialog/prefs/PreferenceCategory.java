package ca.cgjennings.apps.arkham.dialog.prefs;

import javax.swing.Icon;
import javax.swing.JPanel;

/**
 * A preference category allows the user to manage a related set of user
 * preferences.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface PreferenceCategory {

    /**
     * Returns the localized name of this preference category.
     *
     * @return the category name, in the interface locale
     */
    String getTitle();

    /**
     * Returns the icon used to represent this category visually. The returned
     * icon should be 48x48 pixels in size. If {@code null} is returned, a
     * default icon will be used.
     *
     * @return an icon representing this category
     */
    Icon getIcon();

    /**
     * Returns the panel of controls that allows the user to edit the settings
     * for this category. The returned panel should match the design of standard
     * panels, such as the use of a white background and gold headings.
     *
     * @return a panel of user interface controls
     */
    JPanel getPanel();

    /**
     * This method is called when the preferences dialog is about to be
     * displayed so that the category's controls can be initialized from the
     * current user settings.
     */
    void loadSettings();

    /**
     * This method is called when the user accepts the changes in the
     * preferences dialog. It should update the settings values based on the
     * current value of the controls in the panel.
     */
    void storeSettings();

    /**
     * This method is called after {@link #storeSettings()} when the user
     * accepts the changes in the preferences dialog. It should return
     * {@code true} if and only if the user has changed a preference in such a
     * way that the change cannot take effect until the application is
     * restarted.
     *
     * @return {@code true} if the application must be restarted for changes to
     * take effect
     */
    boolean isRestartRequired();
}
