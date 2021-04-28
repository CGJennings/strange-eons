package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.component.GameComponent;
import gamedata.Expansion;
import gamedata.Game;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.Set;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 * The menu of expansion symbols. This menu automatically tracks registered
 * expansions.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
final class ExpansionSymbolMenu extends JMenu {

    public ExpansionSymbolMenu() {
        // This listens for changes to the list of expansions and marks the
        // menu content as out of date.
        Expansion.Listeners.addListener((Object instance, boolean isRegistration) -> {
            listIsUpToDate = false;
        });

        // This is activated whenever the menu is opened. It makes sure that
        // the list of symbols is up to date. Then it goes over the symbol items and:
        //   (1) if no editor is active, or it has no game component, disable the item
        //   (2) if the expansion does not match the game reported by the edited component, hide the item
        addMenuListener(new MenuListener() {
            @Override
            public void menuCanceled(MenuEvent e) {
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            @SuppressWarnings("unchecked")
            public void menuSelected(MenuEvent e) {
                if (!listIsUpToDate) {
                    recreateExpansionSymbolList();
                }

                StrangeEonsEditor ed = StrangeEons.getWindow().getActiveEditor();
                Set<Expansion> selection;
                boolean enable = ed != null && ed.getGameComponent() != null;
                if (enable) {
                    selection = Expansion.getComponentExpansionSymbols(ed.getGameComponent());
                } else {
                    selection = Collections.EMPTY_SET;
                }

                for (int i = 0; i < getMenuComponentCount(); ++i) {
                    final Component possibleItem = getMenuComponent(i);

                    // if this is the variant submenu, disable if only one variant
                    if (possibleItem instanceof ExpansionVariantMenu) {
                        int variants = 0;
                        if (enable) {
                            Game g = Game.get(ed.getGameComponent().getSettings().get(Game.GAME_SETTING_KEY));
                            if (g != null) {
                                variants = g.getSymbolTemplate().getLogicalVariantCount();
                            }
                        }
                        possibleItem.setEnabled(enable && variants > 1);
                        continue;
                    }

                    // neither the variant menu nor a selectable expansion
                    if (!(possibleItem instanceof JCheckBoxMenuItem)) {
                        continue;
                    }

                    // check if the item has the SE Expansion property set, which
                    // indicates an expansion selector for the expansion with
                    // the code that is the same as the property value
                    final JCheckBoxMenuItem item = (JCheckBoxMenuItem) possibleItem;
                    Object val = item.getClientProperty("SE Expansion");
                    if (val != null && selection.contains(Expansion.get((String) val))) {
                        item.setSelected(true);
                    } else {
                        item.setSelected(false);
                    }

                    // check if this expansion is allowed for the current
                    // component's game, and if not, hide/disable it
                    final Expansion expInstance = Expansion.get(val.toString());
                    boolean allowed = enable || expInstance.getGame().equals(Game.getAllGamesInstance());

                    // if expansion has a game and this card's game doesn't match, disallow
                    if (enable && expInstance != null && !expInstance.getGame().equals(Game.getAllGamesInstance())) {
                        String compGame = ed.getGameComponent().getSettings().getOverride("game");
                        if (compGame != null && !compGame.equals(expInstance.getGame().getCode())) {
                            allowed = false;
                        }
                    }
                    item.setEnabled(enable && allowed);
                    item.setVisible(allowed);
                }
            }
        });
    }

    /**
     * Tracks whether the current list of symbol menu items is up to date. If
     * false, the list will be regenerated when the menu is opened.
     */
    private boolean listIsUpToDate;

    /**
     * Rebuild the menu using the current expansion list.
     */
    private void recreateExpansionSymbolList() {
        // Step 0: determine where items should be inserted;
        //         this is based on a separator and Choose... item
        //         following the expansion list
        final int deletionIndex = getMenuComponentCount() - 3;
        final int insertionIndex = deletionIndex + 1;

        // Step 1: delete any existing generated items
        while (getMenuComponentCount() > 0) {
            if (getMenuComponent(deletionIndex) instanceof JCheckBoxMenuItem) {
                remove(deletionIndex);
            } else {
                break;
            }
        }

        // Step 2: create new menu based on current expansions
        final Expansion[] expansions = Expansion.getExpansions();
        for (int i = 0; i < expansions.length; ++i) {
            String code = expansions[i].getCode();

            final JCheckBoxMenuItem item = new JCheckBoxMenuItem();
            item.setText(expansions[i].getUIName());
            item.setIcon(expansions[i].getIcon());
            item.putClientProperty("SE Expansion", code);
            item.addActionListener(expansionSelected);
            item.addMouseListener(clickRecorder);
            add(item, insertionIndex + i);
        }

        listIsUpToDate = true;
    }

    private int modifiers = 0;

    /**
     * The ActionListener does not receive any modifiers, so this records the
     * modifiers at the time of the mouse press (when a press activates the
     * item).
     */
    private MouseListener clickRecorder = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                modifiers = e.getModifiersEx();
            }
        }
    };

    private final ActionListener expansionSelected = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            final boolean multiSelect = (modifiers & (InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK | InputEvent.META_DOWN_MASK)) != 0;

            StrangeEonsEditor ed = StrangeEons.getWindow().getActiveEditor();
            if (ed == null) {
                return;
            }
            GameComponent gc = ed.getGameComponent();
            String proposed = ((JCheckBoxMenuItem) e.getSource()).getClientProperty("SE Expansion").toString();
            if (proposed.equals("NX")) {
                gc.getSettings().reset(Expansion.EXPANSION_SETTING_KEY);
                // NX always resets the list; adding it to a list makes no sense
            } else {
                if (multiSelect) {
                    Expansion exp = Expansion.get(proposed);
                    Set<Expansion> currentList = Expansion.getComponentExpansionSymbols(gc);
                    if (currentList.contains(exp)) {
                        currentList.remove(exp);
                    } else {
                        currentList.add(exp);
                    }
                    currentList.remove(Expansion.getBaseGameExpansion());
                    Expansion.setComponentExpansionSymbols(gc, currentList);
                } else {
                    gc.getSettings().set(Expansion.EXPANSION_SETTING_KEY, proposed);
                }
            }
            gc.markUnsavedChanges();
            ((AbstractGameComponentEditor) ed).redrawPreview();

            if (multiSelect) {
                doClick(0);
            }
        }
    };
}
