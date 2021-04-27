package ca.cgjennings.ui.textedit.tokenizers;

import ca.cgjennings.ui.textedit.TokenType;
import ca.cgjennings.ui.textedit.Tokenizer;
import ca.cgjennings.ui.textedit.completion.CodeCompleter;
import ca.cgjennings.ui.textedit.completion.PlainTextCompleter;
import javax.swing.text.Segment;

/**
 * A basic tokenizer that marks all text as <code>PLAIN</code>.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class PlainTextTokenizer extends Tokenizer {

    @Override
    protected TokenType tokenizeImpl(TokenType token, Segment line, int lineIndex) {
        addToken(line.count, TokenType.PLAIN);
        return TokenType.PLAIN;
    }

    @Override
    public boolean isMultilineTokenizationRequired() {
        return false;
    }

    @Override
    public CodeCompleter getCodeCompleter() {
        if (completer == null) {
            completer = new PlainTextCompleter();
        }
        return completer;
    }
    private static CodeCompleter completer;
}
