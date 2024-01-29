package ca.cgjennings.apps.arkham.plugins.debugging;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Token;

/**
 * Detects sequences that appear to be property expressions
 * ({@code instance.field1.field2}). Powers tool tips in the source code view of
 * the debug client, so intended to be quick rather than accurate.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
class ExpressionDetector {

    private RSyntaxDocument model;

    public ExpressionDetector(String text) {
        createModel();
        try {
            model.insertString(0, text, null);
        } catch (BadLocationException ble) {
            // impossible
        }
    }

    public ExpressionDetector(String[] text) {
        createModel();
        try {
            for (int i = 0; i < text.length; ++i) {
                if (i > 0) {
                    model.insertString(model.getLength(), "\n", null);
                }
                model.insertString(model.getLength(), text[i], null);
            }
        } catch (BadLocationException ble) {
            // impossible
        }
    }

    private void createModel() {
        model = new RSyntaxDocument(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
    }

    private Token tokenAt(int line, int offset) {
        Token t = model.getTokenListForLine(line);

        int i = 0;
        while (t != null) {
            i += t.length();
            if (offset < i) {
                break;
            }
            t = t.getNextToken();
        }

        return t;
    }

    /**
     * Returns the text of the document to analyze.
     *
     * @return the document text
     */
    public String getText() {
        try {
            return model.getText(0, model.getLength());
        } catch (BadLocationException ex) {
            // impossible
            throw new AssertionError();
        }
    }

    /**
     * Returns whether the character at the specified line and offset appears to
     * be part of an identifier.
     *
     * @param line the document line, zero-based
     * @param offset the offset into the line
     * @return true if an identifier is detected at the position
     */
    public boolean isIdentifierAt(int line, int offset) {
        Token t = tokenAt(line, offset);
        return t != null && t.isIdentifier();
    }

    /**
     * Returns the identifier expression at the specified line and offset. An
     * identifier expression is a sequence of one or more identifiers separated
     * by dots. For example, {@code gold} or {@code list.head}.
     *
     * @param line the document line, zero-based
     * @param offset the offset into the line
     * @return an identifier expression including the position, or null
     */
    @SuppressWarnings("empty-statement")
    public String getIdentifierExpressionAt(int line, int offset) {
        if (!isIdentifierAt(line, offset)) {
            return null;
        }

        String text;
        Element el = model.getDefaultRootElement().getElement(line);
        try {
            int liStart = el.getStartOffset();
            int liEnd = el.getEndOffset();
            text = model.getText(liStart, liEnd - liStart);
        } catch (BadLocationException ble) {
            throw new AssertionError(ble);
        }

        // Note that we simplify things quite a bit so as to avoid doing
        // a true parse of the document. E.g., this will find <obj.prop>
        // but not <obj .prop> or <obj./**/prop>
        // if char at the offset is '.' then we actually want to start
        // one character earlier so the expression doesn't end in '.'
        if (text.charAt(offset) == '.') {
            if (offset == 0 || !isIdentifierAt(line, offset - 1)) {
                return null;
            }
            --offset;
        }

        // find the end of the identifier: <prop|erty.x> should find "erty"
        int end = offset + 1;
        for (; end < text.length() && isIdentChar(text.charAt(end)); ++end);

        // finding the start of the identifier: <abc.d|ef> should find "abc.d"
        int start = offset - 1;
        while (start >= 0) {
            char ch = text.charAt(start);
            if (ch != '.' && !isIdentChar(ch)) {
                break;
            }
            --start;
        }
        ++start;

        // check for <.part> case
        if (text.charAt(start) == '.') {
            return null;
        }

        return text.substring(start, end);
    }

    /**
     * Returns true if the character should be considered part of an identifier.
     *
     * @param ch the character to test
     * @return true if the character can be used in an identifier
     */
    protected boolean isIdentChar(char ch) {
        return ch == '#' || ch == '@' || Character.isJavaIdentifierStart(ch) || Character.isJavaIdentifierPart(ch);
    }
}
