package ca.cgjennings.ui.textedit.tokenizers;

import ca.cgjennings.ui.textedit.*;
import java.util.EnumSet;
import javax.swing.text.Segment;

/**
 * Tokenizer that highlights differences between two text files. This tokenizer
 * is intended for use with read-only document content.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class DiffTokenizer extends Tokenizer {

    @Override
    public EnumSet<TokenType> getNaturalLanguageTokenTypes() {
        return EnumSet.noneOf(TokenType.class);
    }

    @Override
    protected TokenType tokenizeImpl(TokenType token, Segment line, int lineIndex) {
        if (line.count == 0) {
            return TokenType.PLAIN;
        }

        switch (line.array[line.offset]) {
            case '+':
            case '>':
                addToken(1, TokenType.INSERTED_BLOCK);
                fillLine(line, TokenType.INSERTED);
                break;
            case '-':
            case '<':
            case 'X':
                addToken(1, TokenType.DELETED_BLOCK);
                fillLine(line, TokenType.DELETED);
                break;
            case '@':
            case '*':
                addToken(line.count, TokenType.KEYWORD1);
                break;
            default:
                addToken(1, TokenType.UNCHANGED_BLOCK);
                fillLine(line, TokenType.UNCHANGED);
                break;
        }
        return TokenType.PLAIN;
    }

    private void fillLine(Segment line, TokenType type) {
        if (line.count > 1) {
            addToken(1, TokenType.PLAIN);
            if (line.count > 2) {
                addToken(line.count - 2, type);
            }
        }
    }

    @Override
    public boolean isMultilineTokenizationRequired() {
        return false;
    }
}
