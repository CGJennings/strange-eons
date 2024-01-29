package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.component.conversion.ConversionException;
import ca.cgjennings.apps.arkham.component.conversion.ConversionSession;
import ca.cgjennings.apps.arkham.component.conversion.ConversionTrigger;
import static ca.cgjennings.apps.arkham.dialog.ErrorDialog.displayError;
import ca.cgjennings.apps.arkham.diy.DIY;
import gamedata.ConversionMap;
import gamedata.ConversionMap.Conversion;
import gamedata.ConversionMap.Group;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import static resources.Language.string;

/**
 * The menu that lists the possible conversion options for the active game
 * component. The menu is automatically updated when the parent menu is opened
 * and the active game component type has changed.
 *
 * @author Henrik Rostedt
 */
final class ConvertMenu extends JMenu {

    private String className = null;
    private boolean classNameUpdated = false;

    public ConvertMenu(JMenu parent) {
        super("app-convert");
        parent.addMenuListener(new MenuListener() {

            @Override
            public void menuSelected(MenuEvent e) {
                updateClassName();
                createMenuItems();
                setEnabled(getMenuComponentCount() > 0);
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });
    }

    private void updateClassName() {
        GameComponent gc = StrangeEons.getActiveGameComponent();
        String newClassName;
        if (gc == null) {
            newClassName = null;
        } else if (gc instanceof DIY) {
            DIY diy = (DIY) gc;
            newClassName = "diy:" + diy.getHandlerScript();
        } else {
            newClassName = gc.getClass().getName();
        }
        if (newClassName == null || !newClassName.equals(className)) {
            classNameUpdated = true;
        }
        className = newClassName;
    }

    private void createMenuItems() {
        if (!classNameUpdated) {
            return;
        }
        // remove the previous menu items
        removeAll();
        ConversionMap conversionMap;
        try {
            conversionMap = ConversionMap.getShared();
        } catch (IOException e) {
            StrangeEons.log.log(Level.SEVERE, "unable to load conversion maps");
            return;
        }
        // create group conversions
        for (Entry<Group, Set<Conversion>> entry : conversionMap.getGroupConversions(className).entrySet()) {
            JMenu groupMenu = new JMenu(entry.getKey().getName());
            createConversionItems(entry.getValue(), groupMenu);
            add(groupMenu);
        }
        // create direct conversions
        createConversionItems(conversionMap.getDirectConversions(className), this);
        classNameUpdated = false;
    }

    private void createConversionItems(Set<Conversion> conversions, JMenu parent) {
        for (Conversion conversion : conversions) {
            parent.add(new ConversionItem(conversion));
        }
    }

    private static class ConversionItem extends JMenuItem {

        private final Conversion conversion;

        public ConversionItem(Conversion conversion) {
            super(conversion.getName());
            this.conversion = conversion;
        }

        @Override
        protected void fireActionPerformed(ActionEvent event) {
            super.fireActionPerformed(event);
            GameComponent gc = StrangeEons.getActiveGameComponent();
            if (gc == null) {
                return;
            }
            ConversionTrigger trigger = conversion.createManualConversionTrigger();
            GameComponent converted;
            try {
                converted = ConversionSession.convertGameComponent(trigger, gc, true);
            } catch (ConversionException ex) {
                displayError(string("rk-conv-err", gc.getName()), ex);
                return;
            }
            StrangeEons.addEditor(converted.createDefaultEditor());
        }
    }
}
