package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.util.Collections;
import java.util.Locale;
import java.util.logging.Level;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import org.fife.ui.rtextarea.RTextScrollPane;
import resources.ResourceKit;

/**
 * This is the base class for all code editing controls.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class CodeEditorBase extends JPanel {

    private SyntaxTextArea textArea;
    private RTextScrollPane scroll;
    private ErrorStrip errorStrip;
    private CodeType type;

    /**
     * Creates a new code editor control.
     */
    public CodeEditorBase() {
        super(new BorderLayout());

        textArea = new SyntaxTextArea(this);
        textArea.setFont(ResourceKit.getEditorFont());

        setCodeType(null);

        scroll = new RTextScrollPane(textArea);
        scroll.setIconRowHeaderEnabled(true);
        add(scroll, BorderLayout.CENTER);

        errorStrip = new ErrorStrip(textArea);
        add(errorStrip, BorderLayout.LINE_END);
        
        addKeyBindings();
    }

    protected void addKeyBindings() {
        addKeyBinding("AS+UP", RSyntaxTextAreaEditorKit.rtaLineUpAction);
        addKeyBinding("AS+DOWN", RSyntaxTextAreaEditorKit.rtaLineDownAction);
    }

    RSyntaxTextArea getTextArea() {
        return textArea;
    }

    RTextScrollPane getScrollPane() {
        return scroll;
    }

    /**
     * Returns the edited document.
     *
     * @return the document object
     */
    public Document getDocument() {
        return textArea.getDocument();
    }

    /**
     * Add a key binding using a string description of the key.
     *
     * @param keyStroke the key stroke that should trigger the action
     * @param action the action to perform
     */
    public void addKeyBinding(String keyStroke, Runnable action) {
        addKeyBinding(parseKeyStroke(keyStroke), action);
    }

    /**
     * Add a key binding to trigger an editor action.
     *
     * @param keyStroke the key stroke that should trigger the action
     * @param action the action to perform
     */
    public void addKeyBinding(KeyStroke keyStroke, Runnable action) {
        Action proxy = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        };
        textArea.getActionMap().put(action, proxy);
        textArea.getInputMap().put(keyStroke, action);
    }
    
    /**
     * Add a key binding to trigger an editor action.
     *
     * @param keyStroke the key stroke that should trigger the action
     * @param action the action to perform
     */
    public void addKeyBinding(String keyStroke, ActionListener action) {
        addKeyBinding(parseKeyStroke(keyStroke), action);
    }    

    /**
     * Add a key binding to trigger an editor action.
     *
     * @param keyStroke the key stroke that should trigger the action
     * @param action the action to perform
     */
    public void addKeyBinding(KeyStroke ks, ActionListener action) {
        Action proxy = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.actionPerformed(e);
            }
        };
        textArea.getActionMap().put(action, proxy);
        textArea.getInputMap().put(ks, action);
    }
    
    /**
     * Add a key binding to trigger an editor action.
     *
     * @param keyStroke the key stroke that should trigger the action
     * @param action the action to perform
     */
    public void addKeyBinding(String keyStroke, Action action) {
        addKeyBinding(parseKeyStroke(keyStroke), action);
    }    

    /**
     * Add a key binding to trigger an editor action.
     *
     * @param keyStroke the key stroke that should trigger the action
     * @param action the action to perform
     */
    public void addKeyBinding(KeyStroke ks, Action action) {
        textArea.getActionMap().put(action, action);
        textArea.getInputMap().put(ks, action);
    }
    
    /**
     * Add a key binding to trigger an editor action.
     *
     * @param keyStroke the key stroke that should trigger the action
     * @param action the name of the action to perform, which must be in the action map
     */
    public void addKeyBinding(String keyStroke, String action) {
        addKeyBinding(parseKeyStroke(keyStroke), action);
    }    

    /**
     * Add a key binding to trigger an editor action.
     *
     * @param keyStroke the key stroke that should trigger the action
     * @param action the name of the action to perform, which must be in the action map
     */
    public void addKeyBinding(KeyStroke ks, String action) {
        if (textArea.getActionMap().get(action) == null) {
            StrangeEons.log.log(Level.WARNING, "there is no action with name {0}", action);
        }
        textArea.getInputMap().put(ks, action);
    }

    /**
     * Changes the code type used for syntax highlighting and related features.
     *
     * @param type the new type of code being edited
     */
    public void setCodeType(CodeType type) {
        if (type == null) {
            type = CodeType.PLAIN;
        }
        this.type = type;
        textArea.setSyntaxEditingStyle(type.getLanguageIdentifier());
        textArea.setCodeFoldingEnabled(true);

        type.installSyntaxParser(this);
    }

    /**
     * Returns the code type used for syntax highlighting and related features.
     *
     * @return the type of code being edited
     */
    public CodeType getCodeType() {
        return type;
    }

    /**
     * Sets the initial contents of the editor. This treats the text as if it
     * were the initial content of a new editor (for example, as if editing a
     * newly opened file). It is similar to calling
     * {@link #setText(java.lang.String)} except that the caret is moved to the
     * start of the document and the undo history is cleared.
     *
     * @param text the text to set
     */
    public void setInitialText(String text) {
        setText(text);
        select(0, 0);
        clearUndoHistory();
    }

    /**
     * Sets the contents of the editor.
     *
     * @param text the new text to edit
     */
    public void setText(String text) {
        textArea.setText(text);
    }

    /**
     * Returns the contents of the editor.
     *
     * @return the edited text
     */
    public String getText() {
        return textArea.getText();
    }

    /**
     * Returns the text of the specified document region.
     *
     * @param start the start offset
     * @param length the length of the desired region
     * @return the text in the specified range
     */
    public String getText(int start, int length) {
        try {
            return textArea.getText(start, length);
        } catch (BadLocationException ble) {
            StrangeEons.log.log(Level.WARNING, ble, null);
            return "";
        }
    }

    /**
     * Returns the text of the document region specified by two offsets.
     *
     * @param start the start offset
     * @param end the end offset
     * @return the text between the two offsets, inclusive
     */
    public String getTextRange(int start, int end) {
        return getText(start, end - start);
    }

    /**
     * Returns the text of the specified line.
     *
     * @param line the zero-based line
     * @return the line text
     */
    public String getLineText(int line) {
        return getText(getLineStartOffset(line), getLineEndOffset(line));
    }

    /**
     * Returns the currently selected text.
     *
     * @return the current selection
     */
    public String getSelectedText() {
        return textArea.getSelectedText();
    }

    /**
     * Replaces the selection with the specified text. If there is no selection,
     * inserts the text at the caret position.
     *
     * @param text the text to insert
     */
    public void setSelectedText(String text) {
        textArea.replaceSelection(text);
    }

    /**
     * Inserts the specified text at the given offset.
     *
     * @param text the text to insert
     * @param offset the document offset
     */
    public void insert(String text, int offset) {
        textArea.insert(text, offset);
    }

    /**
     * Returns the document length, in characters.
     *
     * @return the number of characters in the document
     */
    public int getLength() {
        return textArea.getDocument().getLength();
    }

    /**
     * Returns the selected text's start position. Returns 0 for an empty
     * document, or the value of dot if no selection.
     *
     * @return the selection start offset
     */
    public int getSelectionStart() {
        return textArea.getSelectionStart();
    }

    /**
     * Returns the selected text's end position. Returns 0 if the document is
     * empty, or the value of dot if there is no selection.
     *
     * @return the selection end offset
     */
    public int getSelectionEnd() {
        return textArea.getSelectionEnd();
    }

    /**
     * Sets the current selection range. If the start and end are the same, any
     * current selection will be cleared.
     *
     * @param start the start offset
     * @param end the end offset
     */
    public void select(int start, int end) {
        textArea.select(start, end);
    }

    /**
     * Selects the entire document.
     */
    public void selectAll() {
        textArea.selectAll();
    }

    /**
     * Returns true if the editor currently has a selection.
     */
    public boolean hasSelection() {
        return textArea.getSelectionStart() != textArea.getSelectionEnd();
    }

    /**
     * Begins a sequence of zero or more document edits that will be treated as
     * a single action when undoing and redoing. This should always be used in a
     * {@code try-finally} block with the {@code finally} clause calling
     * {@link #endCompoundEdit()}.
     */
    public void beginCompoundEdit() {
        textArea.beginAtomicEdit();
    }

    /**
     * Ends a previously started compound edit operation.
     */
    public void endCompoundEdit() {
        textArea.endAtomicEdit();
    }

    /**
     * Returns whether there are edits that can be undone.
     */
    public boolean canUndo() {
        return textArea.canUndo();
    }

    /**
     * Undoes the most recent edit.
     */
    public void undo() {
        textArea.undoLastAction();
    }

    /**
     * Returns whether there is an undone edit can currently be redone.
     */
    public boolean canRedo() {
        return textArea.canRedo();
    }

    /**
     * Redoes the most recently undone edit.
     */
    public void redo() {
        textArea.redoLastAction();
    }

    /**
     * Clears the undo history, making undo impossible until another edit
     * occurs.
     */
    public void clearUndoHistory() {
        textArea.discardAllEdits();
    }

    /**
     * Returns the number of lines in the document.
     */
    public int getLineCount() {
        return textArea.getLineCount();
    }

    /**
     * Returns the zero-based line number of the specified document offset.
     *
     * @param offset the offset from the document start
     * @return the line number containing that offset
     */
    public int getLineOfOffset(int offset) {
        try {
            return textArea.getLineOfOffset(offset);
        } catch (BadLocationException ble) {
            StrangeEons.log.log(Level.WARNING, ble, null);
            return offset < 0 ? 0 : getLength();
        }
    }

    /**
     * Returns the document offset at which the specified line begins.
     *
     * @param line the zero-based line number
     * @return the line start offset
     */
    public int getLineStartOffset(int line) {
        try {
            return textArea.getLineStartOffset(line);
        } catch (BadLocationException ble) {
            StrangeEons.log.log(Level.WARNING, ble, null);
            return line < 0 ? 0 : getLength();
        }
    }

    /**
     * Returns the document offset at which the specified line ends.
     *
     * @param line the zero-based line number
     * @return the line end offset
     */
    public int getLineEndOffset(int line) {
        try {
            return textArea.getLineEndOffset(line);
        } catch (BadLocationException ble) {
            StrangeEons.log.log(Level.WARNING, ble, null);
            return line < 0 ? 0 : getLength();
        }
    }

    /**
     * Requests that the editor control gain focus.
     */
    public void requestFocus() {
        textArea.requestFocusInWindow();
    }

    /**
     * Sets whether white space should be visible.
     *
     * @param visible if true, white space is rendered as symbols
     */
    public void setWhitespaceVisible(boolean visible) {
        textArea.setWhitespaceVisible(visible);
        textArea.setEOLMarkersVisible(visible);
    }

    /**
     * Returns whether white space should be visible.
     *
     * @return true if white space is rendered as symbols
     */
    public boolean isWhitespaceVisible() {
        return textArea.isWhitespaceVisible();
    }

    /**
     * Tag used to mark gutter icons added by Parsers.
     */
    static final String PARSER_TAG = "parser";
    /**
     * Gutter icon for warnings.
     */
    static final Icon ICON_WARNING = ResourceKit.getIcon("ui/warning.png");
    /**
     * Gutter icon for errors.
     */
    static final Icon ICON_ERROR = ResourceKit.getIcon("ui/error.png");

    /**
     * Sets a popup menu builder, which can customize the popup menu before it
     * is displayed.
     *
     * @param pmb the menu builder to set, or null to use a default menu
     */
    public void setPopupMenuBuilder(PopupMenuBuilder pmb) {
        textArea.setPopupMenuBuilder(pmb);
    }

    /**
     * Returns the popup menu builder, which can customize the popup menu before
     * it is displayed.
     *
     * @return the menu builder, or null if none is set
     */
    public PopupMenuBuilder getPopupMenuBuilder() {
        return textArea.getPopupMenuBuilder();
    }

    /**
     * Interface implemented by code editor users to create custom popup menus.
     */
    @FunctionalInterface
    public static interface PopupMenuBuilder {

        /**
         * Called to create the popup menu for the current editor state.
         *
         * @param editor the editor component which will display a menu
         * @param menu a default menu that can be extended
         * @return the menu to display, or null
         */
        JPopupMenu buildMenu(CodeEditorBase editor, JPopupMenu menu);
    }

    /**
     * Converts a string to a single key stroke. The string may use one of two
     * formats: the verbose format use by the {@link KeyStroke} class, or a
     * compact format that requires less typing. The compact format uses the
     * form <code>[modifiers*+]key</code>. Here,<code>modifiers</code>
     * represents a sequence of one or more modifier keys. Each modifier is
     * represented by a single letter:</p>
     *
     * <table border=0>
     * <tr valign=top><th><code>P</code> <td>Platform-specific menu accelerator
     * key (Control on most platforms; Command on OS X)
     * <tr valign=top><th><code>M</code> <td>Meta (Command)
     * <tr valign=top><th><code>C</code> <td>Control
     * <tr valign=top><th><code>A</code> <td>Alt
     * <tr valign=top><th><code>S</code> <td>Shift
     * <tr valign=top><th><code>G</code> <td>AltGr (not recommended for shortcut
     * keys)
     * </table>
     *
     * <p>
     * <b>Examples:</b><br>
     * <code>HOME</code><br>
     * <code>ctrl X</code><br>
     * <code>ctrl alt DELETE</code><br>
     * <code>C+X</code><br>
     * <code>CA+DELETE</code>
     *
     * @param keyStroke the string description of the key stroke, in one of the
     * two supported formats
     * @return a <code>KeyStroke</code> for the string description, or
     * <code>null</code>
     */
    public static KeyStroke parseKeyStroke(String keyStroke) {
        if (keyStroke == null) {
            return null;
        }

        int modifiers = 0;
        int index = keyStroke.lastIndexOf('+');
        if (index >= 0) {
            for (int i = 0; i < index; i++) {
                final char keyCode = Character.toUpperCase(keyStroke.charAt(i));
                switch (keyCode) {
                    case 'A':
                        modifiers |= InputEvent.ALT_DOWN_MASK;
                        break;
                    case 'C':
                        modifiers |= InputEvent.CTRL_DOWN_MASK;
                        break;
                    case 'M':
                        modifiers |= InputEvent.META_DOWN_MASK;
                        break;
                    case 'S':
                        modifiers |= InputEvent.SHIFT_DOWN_MASK;
                        break;
                    case 'G':
                        modifiers |= InputEvent.ALT_GRAPH_DOWN_MASK;
                        break;
                    case 'P':
                        modifiers |= Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
                        break;
                    case ' ':
                    case '+':
                        break;
                    default:
                        StrangeEons.log.log(Level.WARNING, "ignored unknown modifier '{0}'", keyCode);
                }
            }
        } else if (keyStroke.indexOf(' ') >= 0) {
            // the string doesn't use "M*+KEY" syntax, but does have a space:
            // parse as the standard "modifier* KEY" format (e.g. "shift X")
            return KeyStroke.getKeyStroke(keyStroke);
        }
        String key = keyStroke.substring(index + 1).trim().toUpperCase(Locale.ROOT);
        switch (key.length()) {
            case 0:
                StrangeEons.log.log(Level.WARNING, "invalid key stroke: {0}", keyStroke);
                return null;
            case 1:
                char ch = key.charAt(0);
                if (modifiers == 0) {
                    return KeyStroke.getKeyStroke(ch);
                } else {
                    return KeyStroke.getKeyStroke(ch, modifiers);
                }
            default:
                int code;

                try {
                    code = KeyEvent.class.getField("VK_".concat(key)).getInt(null);
                } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                    StrangeEons.log.log(Level.WARNING, "invalid key stroke: {0}", keyStroke);
                    return null;
                }

                return KeyStroke.getKeyStroke(code, modifiers);
        }
    }
}
