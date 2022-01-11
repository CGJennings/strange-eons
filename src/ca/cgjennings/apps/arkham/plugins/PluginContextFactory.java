package ca.cgjennings.apps.arkham.plugins;

import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A factory for {@link PluginContext} instances.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class PluginContextFactory {

    private PluginContextFactory() {
    }

    /**
     * Creates a plug-in context for the plug-in currently managed by the
     * specified installed plug-in instance.
     *
     * @param ip the installed plug-in whose plug-in instance will use the
     * context
     * @param modifiers a bitmap of the modifier keys to indicate are currently
     * pressed, or 0
     * @return a suitable plug-in context
     * @throws PluginException if an exception occurs while accessing the
     * plug-in
     */
    @SuppressWarnings("deprecation")
    public static PluginContext createContext(AbstractInstalledPlugin ip, int modifiers) throws PluginException {
        if (ip == null) {
            throw new NullPointerException("ip");
        }
        Plugin p = ip.startPlugin();
        return new PluginContextImpl(ip, p, modifiers, false);
    }

    /**
     * Creates a dual probe-and-run context. This is used internally when
     * starting extension plug-ins.
     *
     * @param ip the installed plug-in whose plug-in instance will use the
     * context
     * @param p the extension plug-in to be probed
     * @return a suitable plug-in context
     * @throws PluginException if an exception occurs while accessing the
     * plug-in
     */
    @SuppressWarnings("deprecation")
    static PluginContext createDualContext(AbstractInstalledPlugin ip, Plugin p) throws PluginException {
        if (ip == null) {
            throw new NullPointerException("ip");
        }
        if (p == null) {
            throw new NullPointerException("p");
        }
        return new PluginContextImpl(ip, p, 0, false);
    }

    /**
     * Creates a plug-in context suitable for probing the plug-in currently
     * managed by the specified installed plug-in instance.
     *
     * @param ip the installed plug-in whose plug-in instance will use the
     * context
     * @param p the plug-in to be probed
     * @return a suitable plug-in context
     * @throws PluginException if an exception occurs while accessing the
     * plug-in
     */
    @SuppressWarnings("deprecation")
    public static PluginContext createProbeContext(AbstractInstalledPlugin ip, Plugin p) throws PluginException {
        if (ip == null) {
            throw new NullPointerException("ip");
        }
        if (p == null) {
            throw new NullPointerException("p");
        }
        return new PluginContextImpl(ip, p, 0, true);
    }

    /**
     * Creates a context for a plug-in. The plug-in must be the active plug-in
     * instance for an {@link InstalledPlugin}.
     *
     * @param plugin the plug-in to look up the installed plug-in for
     * @param modifiers the modifier keys to indicate are currently pressed
     * @return a suitable plug-in context
     * @throws IllegalArgumentException if the plug-in is not being managed by
     * any installed plug-in
     * @see BundleInstaller#getInstalledPlugins()
     */
    public static PluginContext createContext(Plugin plugin, int modifiers) {
        if (plugin == null) {
            throw new NullPointerException("plugin");
        }
        for (InstalledPlugin ip : BundleInstaller.getInstalledPlugins()) {
            if (ip.getPlugin() == plugin) {
                try {
                    return createContext(ip, modifiers);
                } catch (PluginException e) {
                    throw new AssertionError(e);
                }
            }
        }
        throw new IllegalArgumentException("plug-in is not attached to any InstalledPlugin");
    }

    /**
     * Creates a context suitable for probing a plug-in. The plug-in must be the
     * active plug-in instance for an {@link InstalledPlugin}.
     *
     * @param plugin the plug-in to look up the installed plug-in for
     * @return a suitable plug-in context
     * @throws IllegalArgumentException if the plug-in is not being managed by
     * any installed plug-in
     * @see BundleInstaller#getInstalledPlugins()
     */
    public static PluginContext createProbeContext(Plugin plugin) {
        if (plugin == null) {
            throw new NullPointerException("plugin");
        }
        for (InstalledPlugin ip : BundleInstaller.getInstalledPlugins()) {
            if (ip.getPlugin() == plugin) {
                try {
                    return createProbeContext(ip, plugin);
                } catch (PluginException e) {
                    throw new AssertionError(e);
                }
            }
        }
        throw new IllegalArgumentException("plug-in is not attached to any InstalledPlugin");
    }

    /**
     * Returns a dummy context. Dummy contexts are used when a script is run
     * from a code editor. They provide basic functionality so that plug-in
     * developers can test plug-in scripts by running the script directly from
     * the editor.
     *
     * @return a dummy plug-in context
     */
    public static PluginContext createDummyContext() {
        return createDummyContext(0);
    }

    /**
     * Returns a dummy context. Dummy contexts are used when a script is run
     * from a code editor. They provide basic functionality so that plug-in
     * developers can test plug-in scripts by running the script directly from
     * the editor.
     *
     * @param modifiers a bit mask of the modifier keys that the context will
     * report as being held down at activation
     * @return a dummy plug-in context
     */
    @SuppressWarnings("deprecation")
    public synchronized static PluginContext createDummyContext(int modifiers) {
        try {
            if (modifiers == 0) {
                if (dummy == null) {
                    dummy = new PluginContextImpl(null, null, 0, false);
                }
                return dummy;
            }
            return new PluginContextImpl(null, null, modifiers, false);
        } catch (PluginException pe) {
            // not thrown when InstalledPlugin is null
            throw new AssertionError(pe);
        }
    }

    @SuppressWarnings("deprecation")
    private static PluginContextImpl dummy;

    /**
     * Once JDK10+ only, the interface can define this directly.
     */
    static int getMenuShortcutKeyMask() {
        Toolkit tk = Toolkit.getDefaultToolkit();
        int maskEx = 0;
        try {
            // JDK 10+
            Method mGetMenuShortcutKeyMaskEx = Toolkit.class.getMethod("getMenuShortcutKeyMaskEx");
            maskEx = (int) mGetMenuShortcutKeyMaskEx.invoke(tk);
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
            // JDK <10
            final int mask = tk.getMenuShortcutKeyMask();
            if ((mask & InputEvent.META_MASK) == InputEvent.META_MASK) {
                maskEx |= InputEvent.META_DOWN_MASK;
            }
            if ((mask & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK) {
                maskEx |= InputEvent.CTRL_DOWN_MASK;
            }
            if ((mask & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK) {
                maskEx |= InputEvent.SHIFT_DOWN_MASK;
            }
            if ((mask & InputEvent.ALT_MASK) == InputEvent.ALT_MASK) {
                maskEx |= InputEvent.ALT_DOWN_MASK;
            }
        }
        return maskEx;
    }
}
