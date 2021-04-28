package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.plugins.InstalledPlugin;
import ca.cgjennings.apps.arkham.plugins.Plugin;
import ca.cgjennings.apps.arkham.plugins.PluginContext;
import ca.cgjennings.apps.arkham.plugins.PluginContextFactory;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.BlankIcon;
import ca.cgjennings.ui.JUtilities;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuBar;
import javax.swing.KeyStroke;
import resources.Settings;

/**
 * A <code>JMenuItem</code> that is based on an activated plug-in.
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

    public static Icon getIconForPlugin(Plugin plugin, int size) {
        BufferedImage image = null;

        if (Settings.getShared().getYesNo("use-plugin-icons")) {
            image = plugin.getRepresentativeImage();
        }

        Icon icon = null;
        if (image != null) {
            icon = ImageUtilities.createIconForSize(image, size);
        } else {
            icon = getDummyIcon(size);
        }
        return icon;
    }

    private static Icon getDummyIcon(int size) {
        if (dummyIcon == null || dummyIcon.getIconWidth() != size) {
            dummyIcon = new BlankIcon(size);
        }
        return dummyIcon;
    }

    private static Icon dummyIcon;

    private void initPluginIcon() {
        setIcon(getIconForPlugin(plugin, getPreferredSize().height - 2));
    }

    public void update() {
        setEnabled(plugin.isPluginUsable());
        setSelected(plugin.isPluginShowing());
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
