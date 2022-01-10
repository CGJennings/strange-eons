package ca.cgjennings.ui;

import gamedata.Expansion;
import gamedata.Game;
import java.util.List;
import javax.swing.DefaultListModel;

/**
 * A list control for selecting one or more expansions.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class JExpansionList extends JIconList<Expansion> {

    public JExpansionList() {
        this("AH");
    }

    public JExpansionList(Game game) {
        this(game.getCode());
    }

    public JExpansionList(String game) {
        setGame(game);
        setSelectionModel(new ToggleSelectionModel());
        Expansion.Listeners.addListener((Object instance, boolean isRegistration) -> {
            Expansion[] sel = getSelectedExpansions();
            setGame(getGame());
            setSelectedExpansions(sel);
        });
    }

    public void setGame(Game game, boolean includeGenerics) {
        if (game == null) {
            game = Game.getAllGamesInstance();
        }
        DefaultListModel<Expansion> list = new DefaultListModel<>();
        Expansion[] exps = Expansion.getExpansionsForGame(game, includeGenerics);
        for (Expansion e : exps) {
            list.addElement(e);
        }
        setModel(list);
        selectAll();
        this.game = game;
    }

    public void setGame(Game game) {
        setGame(game, true);
    }

    public void setGame(String game) {
        setGame(Game.get(game), true);
    }

    public void setGame(String game, boolean includeGenerics) {
        setGame(Game.get(game), includeGenerics);
    }

    private Game game;

    public Game getGame() {
        return game;
    }

    public void selectAll() {
        if (getModel().getSize() > 0) {
            setSelectionInterval(0, getModel().getSize() - 1);
        }
    }

    public String[] getSelectedExpansionCodes() {
        List<Expansion> sel = getSelectedValuesList();
        String[] exp = new String[sel.size()];
        for (int i = 0; i < sel.size(); ++i) {
            exp[i] = sel.get(i).getCode();
        }
        return exp;
    }

    public Expansion[] getSelectedExpansions() {
        List<Expansion> sel = getSelectedValuesList();
        return sel.toArray(new Expansion[sel.size()]);
    }

    public void setSelectedExpansionCodes(String[] selection) {
        clearSelection();
        DefaultListModel m = (DefaultListModel) getModel();
        for (String s : selection) {
            for (int i = 0; i < m.getSize(); ++i) {
                Expansion e = (Expansion) m.get(i);
                if (e.getCode().equals(s)) {
                    getSelectionModel().addSelectionInterval(i, i);
                }
            }
        }
    }

    public void setSelectedExpansions(Expansion[] selection) {
        clearSelection();
        DefaultListModel m = (DefaultListModel) getModel();
        for (Expansion s : selection) {
            for (int i = 0; i < m.getSize(); ++i) {
                Expansion e = (Expansion) m.get(i);
                if (e.equals(s)) {
                    getSelectionModel().addSelectionInterval(i, i);
                }
            }
        }
    }
}
