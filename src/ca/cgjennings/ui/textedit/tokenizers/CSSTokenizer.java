package ca.cgjennings.ui.textedit.tokenizers;

import ca.cgjennings.ui.textedit.*;
import java.util.EnumSet;
import javax.swing.text.Segment;

/**
 * Tokenizer for editing Cascading Style Sheets.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class CSSTokenizer extends Tokenizer {

    public CSSTokenizer() {
    }

    @Override
    public String getCommentPrefix() {
        return "/*";
    }

    @Override
    public String getCommentSuffix() {
        return "*/";
    }

    @Override
    public boolean isBraceIndented() {
        return true;
    }

    @Override
    public EnumSet<TokenType> getNaturalLanguageTokenTypes() {
        return EnumSet.of(TokenType.COMMENT1, TokenType.COMMENT2, TokenType.COMMENT3);
    }

    // used to restore token type after comment ends
    private TokenType prevToken = TokenType.PLAIN;

    @Override
    protected TokenType tokenizeImpl(TokenType token, Segment line, int lineIndex) {
        char[] array = line.array;
        int offset = line.offset;
        lastOffset = offset;
        int length = line.count + offset;

        loop:
        for (int i = offset; i < length; i++) {
            int i1 = (i + 1);

            char c = array[i];
            char c1 = (i1 == length) ? '\0' : array[i1];

            switch (token) {
                case PLAIN:
                    switch (c) {
                        case '{':
                            addToken(i - lastOffset, TokenType.KEYWORD1);
                            addToken(1, TokenType.PLAIN);
                            token = TokenType.KEYWORD2;
                            lastOffset = i + 1;
                            break;
                        case '/':
                            if (length - i > 1 && array[i1] == '*') {
                                prevToken = token;
                                addToken(i - lastOffset, token);
                                lastOffset = i;
                                token = TokenType.COMMENT1;
                            }
                            break;
                    }
                    break;
                case COMMENT1:
                case COMMENT2:
                    if (c == '*' && length - i > 1) {
                        if (array[i1] == '/') {
                            i++;
                            addToken((i + 1) - lastOffset, token);
                            token = prevToken;
                            lastOffset = i + 1;
                        }
                    }
                    break;
                case KEYWORD2:
                    switch (c) {
                        case ':':
                            addToken(i - lastOffset, token);
                            addToken(1, TokenType.PLAIN);
                            token = TokenType.LITERAL1;
                            lastOffset = i + 1;
                            break;
                        case '}':
                            addToken(i - lastOffset, token);
                            addToken(1, TokenType.PLAIN);
                            token = TokenType.PLAIN;
                            lastOffset = i + 1;
                            break;
                        case '/':
                            if (length - i > 1 && array[i1] == '*') {
                                prevToken = token;
                                addToken(i - lastOffset, token);
                                lastOffset = i;
                                token = TokenType.COMMENT1;
                            }
                            break;
                    }
                    break;
                case LITERAL1:
                    switch (c) {
                        case ';':
                            addToken(i - lastOffset, token);
                            addToken(1, TokenType.PLAIN);
                            token = TokenType.KEYWORD2;
                            lastOffset = i + 1;
                            break;
                        case '\'':
                            addToken(i - lastOffset, token);
                            lastOffset = i;
                            token = TokenType.LITERAL2;
                            break;
                        case '\"':
                            addToken(i - lastOffset, token);
                            lastOffset = i;
                            token = TokenType.LITERAL3;
                            break;
                        case '}':
                            addToken(i - lastOffset, token);
                            addToken(1, TokenType.PLAIN);
                            token = TokenType.PLAIN;
                            lastOffset = i + 1;
                            break;
                        case '/':
                            if (length - i > 1 && array[i1] == '*') {
                                prevToken = token;
                                addToken(i - lastOffset, token);
                                lastOffset = i;
                                token = TokenType.COMMENT1;
                            }
                            break;
                    }
                    break;
                case LITERAL2:
                    if (c == '\'') {
                        addToken(i - lastOffset, token);
                        lastOffset = i;
                        token = TokenType.LITERAL1;
                    }
                    break;
                case LITERAL3:
                    if (c == '\"') {
                        addToken(i - lastOffset, token);
                        lastOffset = i;
                        token = TokenType.LITERAL1;
                    }
                    break;
                default:
                    throw new InternalError("Invalid state: " + token);
            }
        }

        setTokenizationContinuedOnNextLine(token != TokenType.PLAIN);
        switch (token) {
            case PLAIN:
                addToken(length - lastOffset, TokenType.KEYWORD1);
                break;

            case LITERAL2:
            case LITERAL3:
                addToken(length - lastOffset, TokenType.INVALID);
                token = TokenType.PLAIN;
                break;
            default:
                addToken(length - lastOffset, token);
                break;
        }

        return token;
    }

    private int lastOffset;

    @Override
    public boolean isMultilineTokenizationRequired() {
        return true;
    }
}
