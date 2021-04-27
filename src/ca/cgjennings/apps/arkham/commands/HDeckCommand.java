package ca.cgjennings.apps.arkham.commands;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.deck.DeckEditor;
import ca.cgjennings.apps.arkham.deck.PageView;
import ca.cgjennings.apps.arkham.deck.item.PageItem;
import javax.swing.KeyStroke;

/**
 * A helper class for creating deck commands.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
abstract class HDeckCommand extends DelegatedCommand {

    public HDeckCommand(String nameKey, int minSelectionSize) {
        super(nameKey);
        this.minSelectionSize = minSelectionSize;
    }

    public HDeckCommand(String nameKey, String iconResource, int minSelectionSize) {
        super(nameKey, iconResource);
        this.minSelectionSize = minSelectionSize;
    }

    protected int minSelectionSize;

    @Override
    public boolean isDefaultActionApplicable() {
        boolean enable = false;
        DeckEditor ed = getDeckEditor();
        if (ed != null) {
            enable = ed.getDeck().getSelectionSize() >= minSelectionSize;
        }
        return enable;
    }

    protected static PageItem[] getSelection() {
        DeckEditor de = getDeckEditor();
        if (de != null) {
            return de.getDeck().getSelection();
        }
        return null;
    }

    protected static DeckEditor getDeckEditor() {
        StrangeEonsEditor ed = StrangeEons.getWindow().getActiveEditor();
        if (ed instanceof DeckEditor && ((DeckEditor) ed).getDeck() != null) {
            return (DeckEditor) ed;
        }
        return null;
    }

    final HDeckCommand key(char letter) {
        return key("" + letter);
    }

    final HDeckCommand key(String keyname) {
        putValue(PageView.PAGE_VIEW_ACTION_KEY, KeyStroke.getKeyStroke(keyname));
        return this;
    }
}
