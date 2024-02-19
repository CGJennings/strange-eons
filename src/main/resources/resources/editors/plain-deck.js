/*
 * plain-deck.js - version 3
 * Creates a new, empty deck with an instructional text box.
 */

function createTextBox(text, x, y, width, height, page) {
    const box = new arkham.deck.item.TextBox();
    const paint = new Color(0xffff87);
    box.text = text;
    box.fillColor = paint;
    box.outlineColor = new Color(0xa50000);
    box.outlineWidth = 6;
    box.setLocation(x, y);
    box.setSize(width, height);
    if (page) {
        page.addCard(box, false);
        page.deck.addToSelection(box);
    }
    return box;
}

var editor = new arkham.deck.DeckEditor();
createTextBox(string("de-help-box"), 1.25 * 72, 1.25 * 72, 5 * 72, 6 * 72,
        editor.deck.activePage);
editor.deck.markSaved();
editor.setFrameIcon(arkham.NewEditorDialog.sharedInstance.getIconForComponent(editor.deck));
Eons.addEditor(editor);
