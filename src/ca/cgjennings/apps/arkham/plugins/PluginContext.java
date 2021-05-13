package ca.cgjennings.apps.arkham.plugins;

import java.awt.event.InputEvent;
import java.awt.Toolkit;
import java.util.Locale;
import resources.Language;
import resources.Settings;

/**
 * A plug-in context is passed to a plug-in when it is initialized, shown, or
 * hidden. It provides information about the context in which the plug-in code
 * is being executed.
 *
 * <p>
 * To ease plug-in development, when a script is run directly from a code editor
 * window the script will be provided with a <i>dummy context instance</i>. A
 * dummy context is mostly compatible with a true context, but its
 * {@link #getPlugin} and {@link #getInstalledPlugin} methods will both return
 * <code>null</code>. Also, all dummy contexts share a single setting namespace,
 * so any plug-in settings that you create may vary depending on whether the
 * script is running from an installed plug-in or is being tested by running it
 * from a script editor.
 *
 * <p>
 * <b>Note:</b> The scope and purpose of the plug-in context has changed
 * significantly in Strange Eons 3. Most things that a plug-in context was used
 * for in previous releases, such as looking up the game locale, can now be
 * accessed directly by the plug-in itself. The interface definition has changed
 * accordingly, meaning that older compiled plug-ins will need to be updated.
 * Scripts, however, will be given a context that provides backwards-compatible
 * implementations of most of the 2.x methods if the application is running in
 * compatibility mode.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see Plugin
 * @see PluginContextFactory
 */
public interface PluginContext {

    /**
     * Returns the plug-in instance that this context was created for. Returns
     * <code>null</code> if this is a script running with a dummy context.
     *
     * @return the plug-in instance that this context was passed to
     */
    Plugin getPlugin();

    /**
     * Returns an {@link AbstractInstalledPlugin} instance that is managing the
     * plug-in instance returned by {@link #getPlugin()}. This will either be an
     * instance of {@link InstalledPlugin} or {@link InstalledExtension},
     * depending on the type of plug-in. Returns <code>null</code> if this is a
     * script running with a dummy context.
     *
     * @return the installed plug-in created by the {@link BundleInstaller} for
     * the plug-in that this context was created for
     */
    AbstractInstalledPlugin getInstalledPlugin();

    /**
     * Provides a hint about whether the plug-in should perform optional
     * initialization steps. If this method returns <code>true</code>, then the
     * plug-in is only being initialized to determine its name, version,
     * description, and/or type. The plug-in may use this as a hint that it need
     * only perform enough initialization to provide these services. This
     * instance of the plug-in will never be shown.
     *
     * @return <code>true</code> if the plug-in will only be queried for
     * information
     */
    boolean isInformationProbe();

    /**
     * Returns a {@link Settings} instance that can be used to read and write
     * user settings using this plug-in's namespace. Any settings that you
     * create through this settings instance will exist in user settings (and
     * hence be saved between application runs), but have its keys transparently
     * decorated with a unique prefix so that this plug-in's settings cannot
     * clash with the main application settings or the settings of other
     * plug-ins.
     *
     * @return a <code>Settings</code> instance that uses this plug-in's
     * namespace
     */
    public Settings getSettings();

    /**
     * Returns a bit mask representing the modifier keys that were held down
     * when the plug-in was activated. This method is only useful for
     * <code>ACTIVATED</code> plug-ins, and only for the context passed in when
     * the plug-in's
     * {@link Plugin#showPlugin(ca.cgjennings.apps.arkham.plugins.PluginContext, boolean)}
     * method is called. In all other cases, it will return 0.
     *
     * <p>
     * To use the result of this method, combine the modifiers that you wish to
     * test for using binary or (|) to create a mask value. Then check that the
     * bitwise and (&amp;) of the return value and your mask are equal to your mask:
     * <pre>
     * // check if both Shift and Ctrl and held down
     * mask = PluginContext.SHIFT | PluginContext.CONTROL;
     * if( (context.getModifiers() &amp; mask) == mask ) {
     *     // both keys are down
     * }
     * </pre>
     *
     * <p>
     * Obtaining the modifiers held when a plug-in was activated can be used to
     * provide alternate modes of operation for a plug-in. Be aware, however,
     * that the user may assign a keyboard accelerator to the plug-in that
     * conflicts with your interpretation of the modifier values.
     *
     * @return a bit mask of modifier constants
     */
    public int getModifiers();

    /**
     * A modifier constant indicating that a Shift key was held down.
     */
    public static final int SHIFT = InputEvent.SHIFT_MASK;
    /**
     * A modifier constant indicating that an Alt key was held down.
     */
    public static final int ALT = InputEvent.ALT_MASK;
    /**
     * A modifier constant indicating that a Control or Ctrl key was held down.
     */
    public static final int CONTROL = InputEvent.CTRL_MASK;
    /**
     * A modifier constant indicating that a Control or Ctrl key was held down.
     */
    public static final int CTRL = InputEvent.CTRL_MASK;
    /**
     * A modifier constant indicating that a Meta key was held down.
     */
    public static final int META = InputEvent.META_MASK;
    /**
     * A modifier constant indicating that the Command key (on Apple computers)
     * was held down.
     */
    public static final int COMMAND = InputEvent.META_MASK;
    /**
     * A modifier constant indicating that the key used as a menu accelerator
     * key on this platform was held down. (For example, it maps to Ctrl on
     * Windows and Command on Mac.)
     */
    public static final int MENU = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    // Backwards compatibility

    @Deprecated
    default Locale getUILocale() {
        return Language.getInterfaceLocale();
    }

    @Deprecated
    default Locale getGameLocale() {
        return Language.getGameLocale();
    }

    @Deprecated
    default void addUIText(Locale locale, String resource) {
        Language.getInterface().addStrings(resource);
    }

    @Deprecated
    default void addGameText(Locale locale, String resource) {
        Language.getGame().addStrings(resource);
    }
}
