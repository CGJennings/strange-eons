package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.component.conversion.ConversionException;
import ca.cgjennings.apps.arkham.component.conversion.ConversionSession;
import ca.cgjennings.apps.arkham.component.conversion.ConversionTrigger;
import ca.cgjennings.apps.arkham.diy.DIY;
import gamedata.ConversionMap;
import gamedata.ConversionMap.ConversionEntry;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Set;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

final class ConvertMenu extends JMenu {

    public ConvertMenu(JMenu parent) {
        super("app-convert");
        parent.addMenuListener(new MenuListener() {

            @Override
            public void menuSelected(MenuEvent e) {
                GameComponent gc = StrangeEons.getActiveGameComponent();
                if (gc == null) {
                    setEnabled(false);
                    return;
                }
                String className;
                if (gc instanceof DIY) {
                    DIY diy = (DIY) gc;
                    className = "diy:" + diy.getHandlerScript();
                } else {
                    className = gc.getClass().getName();
                }
                removeAll();
                createMenuItems(className);
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

    private void createMenuItems(String className) {
        ConversionMap conversionMap;
        try {
            conversionMap = ConversionMap.getGlobalInstance();
        } catch (IOException e) {
            return;
        }
        // create direct conversions
        createConversionItems(conversionMap.getDirectConversions(className), this);
    }

    private void createConversionItems(Set<ConversionEntry> entries, JMenu parent) {
        for (ConversionEntry entry : entries) {
            parent.add(new ConversionItem(entry));
        }
    }

    private static class ConversionItem extends JMenuItem {

        private final ConversionEntry entry;

        public ConversionItem(ConversionEntry entry) {
            super(entry.getName());
            this.entry = entry;
        }

        @Override
        protected void fireActionPerformed(ActionEvent event) {
            super.fireActionPerformed(event);
            GameComponent gc = StrangeEons.getActiveGameComponent();
            if (gc == null) {
                return;
            }
            ConversionTrigger trigger = entry.createManualConversionTrigger();
            GameComponent converted;
            try {
                converted = ConversionSession.convertGameComponent(trigger, gc, true);
            } catch (ConversionException ex) {
                // show error
                return;
            }
            StrangeEons.addEditor(converted.createDefaultEditor());
        }
    }
}
