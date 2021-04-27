package ca.cgjennings.ui.textedit.completion;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.spelling.MultilanguageSupport;
import ca.cgjennings.spelling.SpellingChecker;
import ca.cgjennings.ui.textedit.EditorCommands;
import ca.cgjennings.ui.textedit.InputHandler;
import ca.cgjennings.ui.textedit.JSourceCodeEditor;
import ca.cgjennings.ui.textedit.SourceDocument;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.text.BadLocationException;

/**
 * A code completer for plain text that offers to complete partially typed words
 * using a spelling checker dictionary. This can also be used as a base class
 * for creating a completer for text that is a mixture of code and plain
 * language, such as HTML.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class PlainTextCompleter implements CodeCompleter {

    private SpellingChecker sc;

    /**
     * Creates a plain text completer that uses words from the default spelling
     * checker.
     */
    public PlainTextCompleter() {
        this(null);
    }

    /**
     * Creates a plain text completer that uses words from the specified
     * spelling checker.
     *
     * @param sc the spelling checker to use, or <code>null</code> for the
     * default
     * @see MultilanguageSupport#getChecker(java.util.Locale)
     */
    public PlainTextCompleter(SpellingChecker sc) {
        if (sc == null) {
            sc = SpellingChecker.getSharedInstance();
        }
        this.sc = sc;
    }

    @Override
    public Set<CodeAlternative> getCodeAlternatives(JSourceCodeEditor editor) {
        // Step 1: determine word under caret
        String prefix = null;
        SourceDocument d = editor.getDocument();
        int caret = editor.getCaretPosition();
        int mark = editor.getMarkPosition();

        // if there are no previous chars or the previous char is not a letter,
        // then there is nothing to complete
        try {
            if (caret == 0 || !Character.isLetter(d.getText(caret - 1, 1).charAt(0))) {
                return null;
            }
        } catch (BadLocationException ex) {
            StrangeEons.log.log(Level.WARNING, null, ex);
            return null;
        }

        d.beginCompoundEdit();
        try {
            editor.select(caret, caret);
            InputHandler ih = editor.getInputHandler();
            ih.executeAction(EditorCommands.SELECT_PREV_WORD, editor, null);
            prefix = editor.getSelectedText();
            editor.select(mark, caret);
        } finally {
            d.endCompoundEdit();
        }

        // Step 2: if there was a word there, generate and return words
        if (prefix != null) {
            LinkedHashSet<CodeAlternative> results = new LinkedHashSet<>();
            CompletionUtilities.addWords(editor, results, prefix, sc);
            return results;
        }
        return null;
    }
}
