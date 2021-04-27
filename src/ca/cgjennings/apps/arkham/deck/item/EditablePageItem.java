package ca.cgjennings.apps.arkham.deck.item;

/**
 * Implemented by cards that can be edited. If the card needs to take control of
 * the page's interface during editing, it must call <code>setEditingCard</code>
 * to set the edited card to this when it begins editing, and call it again with
 * <code>null</code> when it is finished.
 * <p>
 * Typically, a page will call an editable item's {@link #beginEditing} method
 * when the item is double-clicked.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface EditablePageItem {

    public abstract void beginEditing();
}
