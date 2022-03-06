package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.editors.AbbreviationTableManager;
import ca.cgjennings.ui.theme.ThemedIcon;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;
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
    private AbbreviationTable abbreviations;
    private CodeType type;
    private boolean ready = false;

    /**
     * Creates a new code editor control.
     */
    public CodeEditorBase() {
        super(new BorderLayout());

        textArea = new SyntaxTextArea(this);
        try {
            textArea.setFont(ResourceKit.getEditorFont());
        } catch (Exception ex) {
            // this may fail if SE is not running (e.g., in IDE palette);
            // we can ignore and keep default font
        }

        setCodeType(null);

        scroll = new RTextScrollPane(textArea);
        scroll.setIconRowHeaderEnabled(true);
        add(scroll, BorderLayout.CENTER);

        errorStrip = new ErrorStrip(textArea);
        add(errorStrip, BorderLayout.LINE_END);

        addKeyBindings();
        
        ready = true;
    }

    protected void addKeyBindings() {
        addKeyBinding("TAB", new InsertTabOrAbbreviationAction());
        addKeyBinding("AS+UP", RSyntaxTextAreaEditorKit.rtaLineUpAction);
        addKeyBinding("AS+DOWN", RSyntaxTextAreaEditorKit.rtaLineDownAction);
    }

    final SyntaxTextArea getTextArea() {
        return textArea;
    }

    final RTextScrollPane getScrollPane() {
        return scroll;
    }    

    // forward all focus requests to the text area
    @Override
    public void requestFocus() {
        textArea.requestFocus();
        textArea.requestFocusInWindow();
    }

    @Override
    public void requestFocus(FocusEvent.Cause cause) {
        textArea.requestFocus(cause);
        textArea.requestFocusInWindow();
    }

    @Override
    public boolean requestFocus(boolean temporary) {
        boolean gotFocus = textArea.requestFocus(temporary);
        textArea.requestFocusInWindow();
        return gotFocus;
    }

    @Override
    public boolean requestFocusInWindow() {
        return textArea.requestFocusInWindow();
    }

    @Override
    public boolean requestFocusInWindow(FocusEvent.Cause cause) {
        return textArea.requestFocusInWindow(cause);
    }
    
    public final void addDocumentListener(DocumentListener listener) {
        textArea.getDocument().addDocumentListener(listener);
    }

    public final void removeDocumentListener(DocumentListener listener) {
        textArea.getDocument().removeDocumentListener(listener);
    }

    public final void addCaretListener(CaretListener listener) {
        textArea.addCaretListener(listener);
    }

    public final void removeCaretListener(CaretListener listener) {
        textArea.removeCaretListener(listener);
    }
    
    @Override
    public final void addFocusListener(FocusListener listener) {
        textArea.addFocusListener(listener);
    }
    
    @Override
    public final void removeFocusListener(FocusListener listener) {
        textArea.removeFocusListener(listener);
    }
    
    public Font getFont() {
        return ready ? textArea.getFont() : super.getFont();
    }
    
    public void setFont(Font font) {
        if (ready) textArea.setFont(font);
    }
    
    /**
     * Returns a named editor action.
     * 
     * @param actionName the name of the action to return
     * @return the action matching the name, or null
     */
    public Action getAction(String actionName) {
        return textArea.getActionMap().get(actionName);
    }
    
    public Action[] getActions() {
        return textArea.getActions();
    }
    
    public boolean canPerformAction(String actionName) {
        return performActionImpl(actionName, true);
    }
    
    public boolean performAction(String actionName) {
        return performActionImpl(actionName, false);
    }
    
    private boolean performActionImpl(String actionName, boolean testOnly) {
        Action a = getAction(actionName);
        if (a == null || !a.isEnabled()) return false;
        if (testOnly) return true;
        a.actionPerformed(new ActionEvent(textArea, 0, actionName));
        return true;
    }

    
    /**
     * Sets the abbreviation table for this editor, replacing any default
     * table determined by the code type.
     * 
     * @param table the new table, or null to disable abbreviations
     */
    public void setAbbreviationTable(AbbreviationTable table) {
        abbreviations = table;
    }
    
    /**
     * Returns the current abbreviation table for this editor.
     * @return the current table, or null if none is set
     */
    public AbbreviationTable getAbbreviationTable() {
        return abbreviations;
    }    

    /**
     * Returns the edited document.
     *
     * @return the document object
     */
    public final Document getDocument() {
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
     * @param action the name of the action to perform, which must be in the
     * action map
     */
    public void addKeyBinding(String keyStroke, String action) {
        addKeyBinding(parseKeyStroke(keyStroke), action);
    }

    /**
     * Add a key binding to trigger an editor action.
     *
     * @param keyStroke the key stroke that should trigger the action
     * @param action the name of the action to perform, which must be in the
     * action map
     */
    public void addKeyBinding(KeyStroke ks, String action) {
        if (textArea.getActionMap().get(action) == null) {
            StrangeEons.log.log(Level.WARNING, "there is no action with name {0}", action);
        }
        textArea.getInputMap().put(ks, action);
    }

    /**
     * Changes the code type used for syntax highlighting and enables any
     * additional support features that are available for the language.
     *
     * @param type the new type of code being edited; if null, {@code PLAIN}
     * type is used
     */
    public void setCodeType(CodeType type) {
        setCodeType(type, true);
    }

    /**
     * Changes the code type used for syntax highlighting.
     * If advanced editing features are requested, the editor may also install
     * additional features such as code completion and syntax checking, where
     * supported. Otherwise, only basic syntax highlighting is provided.
     *
     * @param type the new type of code being edited; if null, {@code PLAIN}
     * type is used
     * @param enableAdvancedFeatures if true, enables additional high-level
     * language support where available; if false, provides only basic
     * syntax highlighting support
     * 
     */
    public void setCodeType(CodeType type, boolean enableAdvancedFeatures) {
        if (type == null) {
            type = CodeType.PLAIN;
        }
        
        // uninstall old code support before switching type, so the support
        // can look up the existing type during uninstall
        if (codeSupport != null) {
            codeSupport.uninstall(this);
            codeSupport = null;
        }

        // now set the new type prospectively, so that the new code support can
        // also look up the type during install
        this.type = type;
        
        if (enableAdvancedFeatures) {
            setCodeSupport(type.createCodeSupport());
            setAbbreviationTable(AbbreviationTableManager.getTable(type));
        } else {
            setCodeSupport(new DefaultCodeSupport());
            abbreviations = null;            
        }
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
     * Installs code editing support. A default support package is installed
     * by setting the code type, but this can be used to install custom support.
     * 
     * @param support the code support to apply to the editor
     */
    public final void setCodeSupport(CodeSupport support) {
        if (support == codeSupport) return;
        
        if (codeSupport != null) {
            codeSupport.uninstall(this);
            codeSupport = null;
        }
       
        if (support != null ) {
            support.install(this);
            if (file != null) {
                support.fileChanged(file);
            }
        } else {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        }

        codeSupport = support;
    }
    
    /**
     * Returns the currently installed code support.
     * 
     * @return the installed code support
     */
    public final CodeSupport getCodeSupport() {
        return codeSupport;
    }
    
    private CodeSupport codeSupport;
    
    /**
     * Associates the contents of the editor with a file.
     * The editor itself does not use this information, but an attached
     * code support may use this information to offer advanced editing
     * support.
     * 
     * @param file the new, or null to remove any prior association
     */
    public void setFile(File file) {
        if (!Objects.equals(this.file, file)) {
            this.file = file;
            if (codeSupport != null) {
                codeSupport.fileChanged(file);
            }
        }
    }
    
    /**
     * Returns the file associated with this editor, if any.
     * @return the associated file, or null
     */
    public File getFile() {
        return file;
    }
    
    private File file;

    /**
     * Sets whether this editor should be editable. If false, the editor is
     * read-only.
     *
     * @param editable whether to allow modifications to the document
     */
    public void setEditable(boolean editable) {
        if (editable == textArea.isEditable()) return;
        textArea.setEditable(editable);
        textArea.setHighlightCurrentLine(editable);
    }

    /**
     * Returns whether this editor is editable. If false, the editor is
     * read-only.
     *
     * @return whether the editor allows modifications
     */
    public boolean isEditable() {
        return textArea.isEditable();
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
            StrangeEons.log.log(Level.WARNING, "uncaught", ble);
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
        int start = getLineStartOffset(line), end = getLineEndOffset(line);
        // trim off \n from range to return
        if (end > start && getTextRange(end-1, end).equals("\n")) {
            --end;
        }
        return getTextRange(start, end);
    }

    /**
     * Returns the text of the current line, that is, the line containing
     * the caret.
     *
     * @return the line text
     */    
    public String getLineText() {
        return getLineText(getCaretLine());
    }

    /**
     * Returns the length of the specified line.
     *
     * @param line the zero-based line number
     * @return the line length in characters
     */
    public int getLineLength(int line) {
        int len = 0;
        try {
            len = textArea.getLineEndOffset(line) - textArea.getLineStartOffset(line);
        } catch (BadLocationException ble) {
            StrangeEons.log.log(Level.WARNING, "uncaught", ble);
        }
        return len;
    }

    /**
     * Returns the currently selected text.
     *
     * @return the current selection, or an empty string if there is none
     */
    public String getSelectedText() {
        return hasSelection() ? textArea.getSelectedText() : "";
    }

    /**
     * Replaces the selection with the specified text, selecting the new text.
     *
     * @param text the text to insert
     */
    public void setSelectedText(String text) {
        beginCompoundEdit();
        try {
            replaceSelection(null);
            int start = getCaretPosition();
            replaceSelection(text);
            select(start, getCaretPosition());
        } finally {
            endCompoundEdit();
        }
    }

    /**
     * Returns the selected lines as an array of strings.
     *
     * @param doNotIncludeLineIfCaretAtStart if the selection ends at offset 0
     * within a line (i.e., it immediately follows a newline), then the line
     * that includes the selection end will not be included
     * @return an array of strings, where each string is one line of the
     * selection
     */
    public String[] getSelectedLineText(boolean doNotIncludeLineIfCaretAtStart) {
        int[] sel = getSelectedLineRange(doNotIncludeLineIfCaretAtStart);
        int firstLine = sel[0];
        int lastLine = sel[1];
        if (firstLine > lastLine) {
            int swap = firstLine;
            firstLine = lastLine;
            lastLine = swap;
        }
        String[] lines = new String[lastLine - firstLine + 1];
        for (int i = 0; i < lines.length; ++i) {
            lines[i] = getLineText(firstLine + i);
        }

        int selStart = getLineStartOffset(firstLine);
        int selEnd = getLineEndOffset(lastLine) - 1;
        select(selStart, selEnd);

        return lines;
    }

    /**
     * Returns an array of two integers that describe the line numbers covered
     * by the current selection. The order is selection start first, then
     * selection end. Therefore, the second value may be greater than the first.
     *
     * @param doNotIncludeLineIfCaretAtStart
     * @return the array {@code [startLine, endLine]}
     */
    private int[] getSelectedLineRange(boolean doNotIncludeLineIfCaretAtStart) {
        int firstLine = getSelectionStartLine();
        int lastLine = getSelectionEndLine();
        if (doNotIncludeLineIfCaretAtStart) {
            if (firstLine > lastLine) {
                if (getLineStartOffset(firstLine) == getSelectionStart()) {
                    --firstLine;
                }
            } else if (lastLine > firstLine) {
                if (getLineStartOffset(lastLine) == getSelectionEnd()) {
                    --lastLine;
                }
            }
        }
        return new int[]{firstLine, lastLine};
    }

    /**
     * Replaces the selection with the lines in the provided array.
     *
     * @param lines the lines of text, one per line per element
     */
    public void setSelectedLineText(String[] lines) {
        int len = 0;
        for (int i = 0; i < lines.length; ++i) {
            len += lines[i].length();
        }
        StringBuilder b = new StringBuilder(len + lines.length);
        for (int i = 0; i < lines.length; ++i) {
            if (i > 0) {
                b.append('\n');
            }
            b.append(lines[i]);
        }

        setSelectedText(b.toString());
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
     * Replaces the selection with new text. If there is no
     * selection, inserts text at the caret position.
     * 
     * @param text the text to insert
     */
    public void replaceSelection(String text) {
        textArea.replaceSelection(text);
    }
    
    /**
     * Replaces the specified text range with new text.
     * 
     * @param text the text to insert
     * @param start the start offset of the range to replace
     * @param end the end offset of the range to replace
     */
    public void replaceRange(String text, int start, int end) {
        textArea.replaceRange(text, start, end);
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
     * Returns the line of the selection start.
     *
     * @return the selection start line
     */
    public int getSelectionStartLine() {
        return getLineOfOffset(getSelectionStart());
    }

    /**
     * Returns the line of the selection end.
     *
     * @return the selection end line
     */
    public int getSelectionEndLine() {
        return getLineOfOffset(getSelectionEnd());
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
            StrangeEons.log.log(Level.WARNING, "uncaught", ble);
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
            StrangeEons.log.log(Level.WARNING, "uncaught", ble);
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
            StrangeEons.log.log(Level.WARNING, "uncaught", ble);
            return line < 0 ? 0 : getLength();
        }
    }

    /**
     * Returns the position of the text insertion caret for the text component.
     *
     * @return the position of the text insertion caret for the text component ≥
     * 0
     */
    public int getCaretPosition() {
        return textArea.getCaretPosition();
    }

    /**
     * Sets the position of the text insertion caret for the text component.
     *
     * @param offset the new caret position, from 0 to the document length
     */
    public void setCaretPosition(int offset) {
        textArea.setCaretPosition(offset);
    }

    /**
     * Returns the position of the mark for the text component. This is the end
     * of the selection that is opposite the caret.
     *
     * @return the position of the mark for the text component ≥ 0
     */
    public int getMarkPosition() {
        return textArea.getCaretPosition() == textArea.getSelectionStart()
                ? textArea.getSelectionEnd()
                : textArea.getSelectionStart();
    }

    /**
     * Sets the position of the mark for the text component.
     *
     * @param offset the position of the mark for the text component ≥ 0
     */
    public void setMarkPosition(int offset) {
        offset = Math.max(0, Math.min(offset, getLength()));
        if (textArea.getCaretPosition() == textArea.getSelectionStart()) {
            textArea.setSelectionEnd(offset);
        } else {
            textArea.setSelectionStart(offset);
        }
    }

    /**
     * Returns the line number of the text insertion caret.
     *
     * @return the line number ≥ 0
     */
    public int getCaretLine() {
        return textArea.getCaretLineNumber();
    }

    /**
     * Moves the caret to the start of the specified line.
     *
     * @param line the zero-based line number
     */
    public void setCaretLine(int line) {
        int start = getLineStartOffset(line);
        select(start, start);
    }

    /**
     * Returns the offset of the text insertion caret from the start of its
     * line.
     *
     * @return the offset of the text insertion caret from the line start offset
     * ≥ 0
     */
    public int getCaretOffsetFromLineStart() {
        return textArea.getCaretOffsetFromLineStart();
    }

    /**
     * Returns the line number of the line at the top of the view.
     *
     * @return the first visible line ≥ 0
     */
    public int getFirstDisplayedLine() {
        Rectangle rect = textArea.getVisibleRect();
        int offset = textArea.viewToModel2D(rect.getLocation());
        if (offset < 0) {
            return 0;
        }
        return getLineOfOffset(offset);
    }

    /**
     * Scroll the editor view to display the specified line.
     *
     * @param line the line number ≥ 0
     */
    public void scrollToLine(int line) {
        int y;
        try {
            line = Math.max(0, Math.min(line, getLineCount()));
            y = textArea.yForLine(line);
            JViewport vp = scroll.getViewport();
            scroll.getVerticalScrollBar().setValue(Math.max(0, y - vp.getHeight() / 2));
            scroll.getHorizontalScrollBar().setValue(0);
        } catch (BadLocationException ble) {
            StrangeEons.log.log(Level.WARNING, "uncaught", ble);
        }
    }

    /**
     * Scroll the editor view to display the specified offset.
     *
     * @param offset the offset to scroll into view
     */
    public void scrollToOffset(int offset) {
        try {
            offset = Math.max(0, Math.min(offset, getLength()));
            Rectangle2D rect = textArea.modelToView2D(offset);
            JViewport vp = scroll.getViewport();
            scroll.getVerticalScrollBar().setValue(Math.max(0, (int) rect.getY() - vp.getHeight() / 2));
            scroll.getHorizontalScrollBar().setValue(Math.max(0, (int) rect.getX() - vp.getWidth() / 2));
        } catch (BadLocationException ble) {
            StrangeEons.log.log(Level.WARNING, "uncaught", ble);
        }
    }

    /**
     * Sets whether white space should be visible.
     *
     * @param visible if true, white space is rendered as symbols
     * @see #isWhitespaceVisible() 
     */
    public void setWhitespaceVisible(boolean visible) {
        textArea.setWhitespaceVisible(visible);
        textArea.setEOLMarkersVisible(visible);
    }

    /**
     * Returns whether white space should be visible.
     *
     * @return true if white space is rendered as symbols
     * @see #setWhitespaceVisible(boolean) 
     */
    public boolean isWhitespaceVisible() {
        return textArea.isWhitespaceVisible();
    }
    
    /**
     * Sets whether line numbers are shown. 
     * @param visible if true, lines are numbered
     * @see #isNumberLineVisible() 
     */
    public void setNumberLineVisible(boolean visible) {
        scroll.setLineNumbersEnabled(visible);
    }
    
    /**
     * Returns whether line numbers are shown.
     * @return true if lines are numbered
     * @see #setNumberLineVisible(boolean) 
     */
    public boolean isNumberLineVisible() {
        return scroll.getLineNumbersEnabled();
    }
    
    /**
     * Sets whether feedback on the document content, such as icons
     * marking rows with errors, is visible.
     * 
     * @param visible if true, feedback will be visible
     * @see #isContentFeedbackVisible() 
     */
    public void setContentFeedbackVisible(boolean visible) {
        if (visible == isContentFeedbackVisible()) return;
        if (visible) {
            add(errorStrip, BorderLayout.LINE_END);   
        } else {
            remove(errorStrip);
        }
        scroll.setIconRowHeaderEnabled(visible);
    }
    
    /**
     * Sets whether feedback on the document content is visible. 
     * @return true if feedback is visible
     * @see #setContentFeedbackVisible(boolean)
     */
    public boolean isContentFeedbackVisible() {
        return errorStrip.getParent() == this;
    }
    
    /**
     * Sets whether or not code folding is enabled for supported code types.
     * @param enable if true, code blocks can be hidden by clicking the fold
     * control in the margin
     * @see #isCodeFoldingEnabled() 
     */
    public void setCodeFoldingEnabled(boolean enable) {
        if (enable == isCodeFoldingEnabled()) return;
        
        textArea.setCodeFoldingEnabled(enable);
        scroll.setFoldIndicatorEnabled(enable);
    }
    
    /**
     * Returns whether or not code folding is enabled for supported code types.
     * @return true if code blocks can be hidden by clicking the fold
     * control in the margin
     * @see #setCodeFoldingEnabled(boolean) 
     */    
    public boolean isCodeFoldingEnabled() {
        return textArea.isCodeFoldingEnabled();
    }

    /**
     * Tag used to mark gutter icons added by Parsers.
     */
    static final String PARSER_TAG = "parser";
    /**
     * Gutter icon for warnings.
     */
    static final Icon ICON_WARNING = new GutterIcon(NavigationPoint.ICON_WARNING);
    /**
     * Gutter icon for errors.
     */
    static final Icon ICON_ERROR = new GutterIcon(NavigationPoint.ICON_ERROR);

    /** Wraps an icon to center it properly in the gutter area. */
    static final class GutterIcon implements Icon {
        private Icon icon;
        public GutterIcon(Icon wrapped) {
            icon = ThemedIcon.create(wrapped).derive(12);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            icon.paintIcon(c, g, x + 2, y);
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    }
    
    
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
     * Simulates pressing the specified key in the editor.
     * 
     * @param keyStroke the non-null key to simulate
     */
    public void type(KeyStroke keyStroke) {
        KeyEvent event = new KeyEvent(textArea, keyStroke.getKeyEventType(), System.currentTimeMillis(), keyStroke.getModifiers(), keyStroke.getKeyCode(), keyStroke.getKeyChar());
        textArea.dispatchEvent(event);
    }

    /**
     * Begins a search of the document. When a new search is started or the
     * parameters of an ongoing search are changed, this will restart searching
     * from the current position, selecting the initial match if any.
     * In this case, the method will return a {@link Result} that describes the
     * initial match (or no match if none occurs). If this is called with the
     * same parameters as an ongoing search, null is returned and no initial
     * search is performed. (A return value of null does not, therefore, mean
     * that there are no matches.)
     * 
     * @param pattern the pattern to search for
     * @param matchCase true if the search should be case sensitive
     * @param wholeWord true to match only whole words
     * @param regexp true if the pattern is a regular expression
     * @param wrap true if the search should wrap around the document start or end
     */
    public Result beginSearch(String pattern, boolean matchCase, boolean wholeWord, boolean regexp, boolean wrap) {
        Objects.requireNonNull(pattern, "pattern");

        boolean doInitialSearch = false;

        if (searchContext == null) {
            searchContext = new SearchContext();
            searchContext.setMarkAll(true);
            doInitialSearch = true;
        }
        
        if (!pattern.equals(searchContext.getSearchFor())) {
            searchContext.setSearchFor(pattern);
            doInitialSearch = true;
        }
        if (searchContext.getMatchCase() != matchCase) {
            searchContext.setMatchCase(matchCase);
            doInitialSearch = true;
        }
        if (searchContext.getWholeWord() != wholeWord) {
            searchContext.setWholeWord(wholeWord);
            doInitialSearch = true;
        }
        if (searchContext.isRegularExpression() != regexp) {
            searchContext.setRegularExpression(regexp);
            doInitialSearch = true;
        }
        if (searchContext.getSearchWrap() != wrap) {
            searchContext.setSearchWrap(wrap);
            doInitialSearch = true;
        }
        
        if (doInitialSearch) {
            // disable symbol occurrence highlighting during search as it
            // tends to look confusing
            textArea.setMarkOccurrences(false);
            
            SearchEngine.markAll(textArea, searchContext);

            int start = Math.min(getSelectionStart(), getSelectionEnd());
            setCaretPosition(start);
            searchContext.setSearchForward(true);
            return new Result(SearchEngine.find(textArea, searchContext), searchContext);
        }
        
        return null;
    }
    
    /**
     * Searches for the next match of the current search.
     * @param forward if true, searches forward through the document; else backward
     * @return the result of the search operation
     */
    public Result findNext(boolean forward) {
        if (searchContext == null) return new Result(forward);
        searchContext.setSearchForward(forward);
        return new Result(SearchEngine.find(textArea, searchContext), searchContext);
    }  

   /**
     * Replaces the next match of the current search.
     * @param forward if true, searches forward through the document; else backward
     * @param replacement the string to replace the next match with
     * @return the result of the search operation
     */
    public Result replaceNext(boolean forward, String replacement) {
        if (searchContext == null) return new Result(forward);
        searchContext.setSearchForward(forward);
        searchContext.setReplaceWith(replacement);
        Result result = new Result(SearchEngine.replace(textArea, searchContext), searchContext);
        if (result.found) {
            SearchEngine.markAll(textArea, searchContext);
        }
        return result;
    }
    
    /**
     * Replaces all occurrences of the current search.
     * @param replacement the string to replace all occurrences with
     * @return the result of the search operation
     */
    public Result replaceAll(String replacement) {
        if (searchContext == null) return new Result(true);
        searchContext.setReplaceWith(replacement);
        Result result = new Result(SearchEngine.replaceAll(textArea, searchContext), searchContext);
        if (result.found) {
            SearchEngine.markAll(textArea, searchContext);
        }
        return result;
    }
    
    /**
     * Ends the current search, if any.
     */
    public void endSearch() {
        if (searchContext == null) return;
        // clear search highlighting
        searchContext.setSearchFor("");
        SearchEngine.markAll(textArea, searchContext);
        searchContext = null;
        // re-enable symbol occurrence highlighting
        textArea.setMarkOccurrences(true);
    }
    
    /**
     * The result of a search or replace operation.
     */
    public static class Result {
        private Result(boolean forward) {
            this.wrapped = false;
            this.found = false;
            this.matchCount = 0;
            this.wasForward = forward;
        }
        private Result(SearchResult source, SearchContext searchContext) {
            this.wrapped = source.isWrapped();
            this.found = source.wasFound();
            this.matchCount = source.getCount();
            this.wasForward = searchContext.getSearchForward();
        }
        /** True if a result was found or replaced. */
        public final boolean found;
        /** The number of matches (will be 0 or 1 unless the result of a replace all. */
        public final int matchCount;
        /** True if the search wrapped past the start or end of the document. */
        public final boolean wrapped;
        /** True if the search direction was forward. */
        public final boolean wasForward;
    }
    
    private SearchContext searchContext;

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
                } catch (ReflectiveOperationException | RuntimeException e) {
                    StrangeEons.log.log(Level.WARNING, "invalid key stroke: {0}", keyStroke);
                    return null;
                }

                return KeyStroke.getKeyStroke(code, modifiers);
        }
    }


    
    private class InsertTabOrAbbreviationAction extends RSyntaxTextAreaEditorKit.InsertTabAction {
        private static final long serialVersionUID = 1L;
        private boolean isExpanding;
        
        @Override
        public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
            // check if an abbreviation can be expanded at this location, and
            // if so, expand it and return; otherwise proceed with the standard
            // Tab insert behaviour
            if (textArea.isEditable() && textArea.isEnabled() && !hasSelection() && !isExpanding) {
                isExpanding = true;
                try {
                    if (abbreviations != null && abbreviations.expandAbbreviation(CodeEditorBase.this)) {
                        return;
                    }
                } finally {
                    isExpanding = false;
                }
            }
            super.actionPerformedImpl(e, textArea);
        }
    }
}
