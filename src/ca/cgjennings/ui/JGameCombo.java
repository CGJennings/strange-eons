package ca.cgjennings.ui;

import gamedata.Game;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;

/**
 * A combo box that allows the selection of a game. The items in the combo are
 * instances of {@link Game}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.00
 */
@SuppressWarnings("serial")
public class JGameCombo extends JIconComboBox {

    private boolean includeAllGame;
    private boolean doneInit;

    public JGameCombo() {
        super();
        setAllGameIncluded(true);
        doneInit = true;
    }

    public JGameCombo(boolean includeAllGame) {
        super(Game.getGames(includeAllGame));
        this.includeAllGame = !includeAllGame;
        setAllGameIncluded(includeAllGame);
        doneInit = true;
    }

    public void setAllGameIncluded(boolean includeAllGame) {
        if (this.includeAllGame != includeAllGame) {
            try {
                this.includeAllGame = includeAllGame;
                super.setModel(new DefaultComboBoxModel(Game.getGames(includeAllGame)));
            } catch (Exception e) {
                // this allows instantiation for inclusion in the palette
                e.printStackTrace();
            }
        }
    }

    public boolean isAllGameIncluded() {
        return includeAllGame;
    }

    @Override
    public Game getItemAt(int index) {
        return (Game) super.getItemAt(index);
    }

    @Override
    public Game getSelectedItem() {
        return (Game) super.getSelectedItem();
    }

    public void setSelectedItem(Game game) {
        setSelectedItem((Object) game);
    }

    @Override
    public void setModel(ComboBoxModel aModel) {
        if (doneInit) {
            throw new UnsupportedOperationException();
        } else {
            super.setModel(aModel);
        }
    }
}
