package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * An abstract base class for undoable events in the deck editor.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
abstract class DeckUndoable extends AbstractUndoableEdit {

    PageView view;
    Page page;
    Deck deck;

    public DeckUndoable(PageView v) {
        view = v;
        page = v.getPage();
        deck = page.getDeck();
    }

    public DeckUndoable(Page p) {
        page = p;
        deck = page.getDeck();
    }

    public DeckUndoable(Deck d) {
        deck = d;
        page = d.getActivePage();
    }

    @Override
    public final void undo() throws CannotUndoException {
        super.undo();
        StrangeEons.log.severe("uncomment this code to start working on deck undo!");
//		++deck.undoIsActive;
//		try {
//			undoImpl();
//		} finally {
//			--deck.undoIsActive;
//		}
    }

    @Override
    public final void redo() throws CannotRedoException {
        super.redo();
//		++deck.undoIsActive;
//		try {
//			redoImpl();
//		} finally {
//			--deck.undoIsActive;
//		}
    }

    protected abstract void undoImpl();

    protected abstract void redoImpl();

    /**
     * Returns the editor that is editing this deck. If no editor is editing the
     * deck, returns <code>null</code>.
     *
     * @return the editing deck
     */
    protected DeckEditor findEditor() {
        for (StrangeEonsEditor ed : StrangeEons.getWindow().getEditors()) {
            if (ed.getGameComponent() == deck && (ed instanceof DeckEditor)) {
                return (DeckEditor) ed;
            }
        }
        return null;
    }
}
