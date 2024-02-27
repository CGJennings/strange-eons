package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.component.GameComponent;
import gamedata.Expansion;
import gamedata.ExpansionSymbolTemplate;
import gamedata.Game;
import java.awt.event.ActionEvent;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import resources.ResourceKit;

/**
 * The menu that contains the list of style variants for the current component's
 * game. This menu updates its submenu for the available variants as it pops up.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
final class ExpansionVariantMenu extends JMenu {

    public ExpansionVariantMenu() {
        super();
        setIcon(ResourceKit.getIcon("ui/view/variant.png"));
        addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                recreateSubmenu();
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

        });
    }

    private void recreateSubmenu() {
        // delete variants until we get to a separator or the end
        while (getMenuComponentCount() > 0 && !(getMenuComponent(0) instanceof JSeparator)) {
            remove(0);
        }

        StrangeEonsEditor ed = StrangeEons.getWindow().getActiveEditor();
        GameComponent gc = ed == null ? null : ed.getGameComponent();
        if (gc == null) {
            createMenuForGame(Game.getAllGamesInstance(), -2);
        } else {
            String g = gc.getSettings().get(Game.GAME_SETTING_KEY, Game.ALL_GAMES_CODE);
            String v = gc.getSettings().get(Expansion.VARIANT_SETTING_KEY);
            int sel = -1;
            if (v != null) {
                try {
                    sel = Integer.parseInt(v);
                } catch (NumberFormatException e) {
                }
            }

            // else figure out the default variant, except currently we can't
//			else {
//				// try to figure out the default variant for the component type
//				Sheet[] faces = gc.getSheets();
//				for( int i=0; i<faces.length; ++i ) {
//					faces[i].get
//				}
//			}
            Game game = Game.get(g);
            if (game == null) {
                game = Game.getAllGamesInstance();
                sel = -2;
            }

            createMenuForGame(game, sel);
        }

    }

    /*
	 * selection: variant index or -1 for no selection or -2 for disable list
     */
    private void createMenuForGame(Game game, int selection) {
        ExpansionSymbolTemplate est = game.getSymbolTemplate();

        int vars = est.getLogicalVariantCount();
        for (int i = 0; i < vars; ++i) {
            VariantItem vi = new VariantItem(est.getLogicalVariantName(i), est.getLogicalVariantIcon(i), i);
            if (selection < -1) {
                vi.setEnabled(false);
            }
            if (selection == i) {
                vi.setSelected(true);
            }
            add(vi);
        }
    }

    private static class VariantItem extends JRadioButtonMenuItem {

        private final int varNum;

        VariantItem(String text, Icon icon, int variantIndex) {
            super(text, icon);
            varNum = variantIndex;
        }

        @Override
        protected void fireActionPerformed(ActionEvent event) {
            super.fireActionPerformed(event);
            StrangeEonsEditor ed = StrangeEons.getWindow().getActiveEditor();
            if (ed != null && ed.getGameComponent() != null) {
                GameComponent gc = ed.getGameComponent();
                gc.getSettings().set(Expansion.VARIANT_SETTING_KEY, String.valueOf(varNum));
                gc.markUnsavedChanges();
                ((AbstractGameComponentEditor<?>) ed).redrawPreview();
            }
        }
    }
}
