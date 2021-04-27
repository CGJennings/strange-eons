package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import ca.cgjennings.apps.arkham.plugins.InstalledPlugin;
import ca.cgjennings.apps.arkham.plugins.Plugin;
import java.awt.Component;
import java.util.logging.Level;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 * The <b>Toolbox</b> menu automatically rebuilds its menu items when plug-ins
 * are loaded or unloaded.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
class ToolboxMenu extends JMenu {

    private Component toolboxMenuSeparator;

    public ToolboxMenu() {
        addSeparator();
        toolboxMenuSeparator = getMenuComponent(0);

        StrangeEons.getApplication().addPluginLoadingListener(new StrangeEons.PluginLoadingListener() {
            @Override
            public void pluginsLoaded(int eventType) {
                if (eventType == PLUGIN_LOAD_EVENT) {
                    populateToolboxMenu();
                } else if (eventType == PLUGIN_UNLOAD_EVENT) {
                    depopulateToolboxMenu();
                }
            }

            @Override
            public String toString() {
                return "Toolbox menu";
            }
        });

        addMenuListener(new MenuListener() {
            @Override
            public void menuCanceled(MenuEvent e) {
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuSelected(MenuEvent e) {
                for (int i = 0; i < getMenuComponentCount(); ++i) {
                    Component c = getMenuComponent(i);
                    if (c instanceof PluginMenuItem) {
                        ((PluginMenuItem) c).update();
                    }
                }
            }
        });
    }

    private void depopulateToolboxMenu() {
        setEnabled(false);
        // clear menu items until there are none left or a special
        // separator marker is reached
        while (getMenuComponentCount() > 0) {
            Component c = getMenuComponent(0);
            if (c == toolboxMenuSeparator) {
                break;
            }
            remove(c);
        }
    }

    private void populateToolboxMenu() {
        // Note: plug-ins are always unloaded before they are loaded, so the
        // menu is empty of plug-in items at the start of this call.

        // Get a sorted list of plug-ins and build menu items for them;
        // only started, ACTIVATED plug-ins are listed and a separator is
        // inserted between plug-ins in different priority groups.
        InstalledPlugin[] plugins = BundleInstaller.getInstalledPlugins();

        int menuPosition = 0;
        InstalledPlugin lastAdded = null;
        final JMenuBar menuBar = (JMenuBar) getParent();

        for (int i = 0; i < plugins.length; ++i) {
            final InstalledPlugin ip = plugins[i];

            try {
                // disabled plug-ins will not have been started by the plug-in loader
                if (!ip.isStarted() || ip.getPluginType() != Plugin.ACTIVATED) {
                    continue;
                }

                // try creating the item before adding a separator, so in the
                // event that it throws an exception we can't end up with
                // empty separator gaps
                PluginMenuItem pmi = new PluginMenuItem(menuBar, ip, ip.getPlugin());

                // has the load priority changed since the last item we added?
                if (lastAdded != null && lastAdded.getPriority() != ip.getPriority()) {
                    add(new JPopupMenu.Separator(), menuPosition++);
                }

                // add an item for this plug-in
                add(pmi, menuPosition++);
                lastAdded = ip;
            } catch (Throwable t) { // must catch anything here
                StrangeEons.log.log(Level.SEVERE, "Exception creating menu item for " + ip.getBundle().getFile().getName(), t);
            }
        }

        setEnabled(true);
    }
}
