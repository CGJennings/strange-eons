package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.plugins.InstalledPlugin;
import ca.cgjennings.apps.arkham.plugins.Plugin;
import ca.cgjennings.apps.arkham.plugins.PluginContext;
import ca.cgjennings.apps.arkham.plugins.PluginContextFactory;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.BlankIcon;
import ca.cgjennings.ui.JUtilities;
import ca.cgjennings.ui.theme.ThemedIcon;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuBar;
import javax.swing.KeyStroke;

/**
 * A {@code JMenuItem} that is based on an activated plug-in.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
final class PluginMenuItem extends JCheckBoxMenuItem {

    private final Plugin plugin;
    private final InstalledPlugin installedPlugin;

    public PluginMenuItem(JMenuBar menuBar, InstalledPlugin installedPlugin, Plugin plugin) {
        super(plugin.getPluginName().replace("...", "\u2026"));
        this.plugin = plugin;
        this.installedPlugin = installedPlugin;

        putClientProperty("ignoreAccelerator", Boolean.TRUE);

        // on OS X, the system menu is used rather than a Swing menu
        // so <html> in the tool tips won't work---we can fix most problems
        // by just filtering out the tags
        String toolTip = plugin.getPluginDescription();
        if (PlatformSupport.PLATFORM_IS_MAC && toolTip != null) {
            if (toolTip.length() >= 6 && toolTip.charAt(0) == '<' && toolTip.substring(0, 6).equalsIgnoreCase("<html>")) {
                toolTip = toolTip.replaceAll("\\<br\\>(?i)", " ")
                        .replaceAll("\\&nbsp\\;(?i)", " ")
                        .replaceAll("\\<[^\\>]*\\>", "");
            }
        }
        setToolTipText(toolTip);

        initPluginIcon();
        initAccelerator(menuBar);
        createListener();
    }

    private void createListener() {
        addActionListener((ActionEvent e) -> {
            PluginContext context = PluginContextFactory.createContext(plugin, e.getModifiers());
            StrangeEons.setWaitCursor(true);
            try {
                plugin.showPlugin(context, !plugin.isPluginShowing());
            } catch (Throwable t) {
                if (!(t.getClass().getName().equals("ca.cgjennings.apps.arkham.plugins.ScriptMonkey.BreakException"))) {
                    Toolkit.getDefaultToolkit().beep();
                    StrangeEons.log.log(Level.WARNING, "plug-in activation attempt threw exception", t);
                }
            } finally {
                StrangeEons.setWaitCursor(false);
            }
        });
    }

    private void initAccelerator(JMenuBar menubar) {
        KeyStroke stroke = installedPlugin.getAcceleratorKey();
        if (stroke != null && !JUtilities.isAcceleratorInUse(menubar, stroke)) {
            setAccelerator(stroke);
        }
    }

    public static ThemedIcon getIconForPlugin(Plugin plugin, int size) {
        ThemedIcon icon = plugin.getPluginIcon();
        if (icon == null) {
            icon = new BlankIcon(size);
        } else {
            icon = icon.derive(size);
        }
        return icon;
    }

    private void initPluginIcon() {
        setIcon(getIconForPlugin(plugin, ThemedIcon.SMALL));
    }

    public void update() {
        setEnabled(plugin.isPluginUsable());
        setSelected(plugin.isPluginShowing());
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
