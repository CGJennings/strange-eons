package ca.cgjennings.ui.textedit.tokenizers;

import ca.cgjennings.ui.textedit.TokenType;
import ca.cgjennings.ui.textedit.Tokenizer;
import java.util.EnumSet;
import javax.swing.text.Segment;

/**
 * Tokenizer for editing settings files, string tables
 * ({@code .properties}), and related files.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class PropertyTokenizer extends Tokenizer {

    public PropertyTokenizer() {
    }

    public PropertyTokenizer(boolean ignoreColons) {
        ignoreColon = ignoreColons;
    }

    private boolean ignoreColon;

    @Override
    public String getCommentPrefix() {
        return "#";
    }
    public static final TokenType VALUE = TokenType.KEYWORD1;

    @Override
    public EnumSet<TokenType> getNaturalLanguageTokenTypes() {
        return EnumSet.of(TokenType.COMMENT1, TokenType.LITERAL1);
    }

    @Override
    protected TokenType tokenizeImpl(TokenType token, Segment line, int lineIndex) {
        char[] array = line.array;
        int offset = line.offset;
        int lastOffset = offset;
        int length = line.count + offset;

        boolean nonWhitespace = false;

        loop:
        for (int i = offset; i < length; i++) {
            int i1 = (i + 1);
            char c = array[i];
            if (!Character.isWhitespace(c) && c != '#' && c != '!') {
                nonWhitespace = true;
            }
            switch (token) {
                case PLAIN:
                    switch (c) {
                        case '#':
                        case '!':
                            if (!nonWhitespace) {
                                addToken(line.count, TokenType.COMMENT1);
                                lastOffset = length;
                                break loop;
                            }
                            break;
                        case '=':
                            if (i == offset || array[i - 1] != '\\') {
                                addToken(i - lastOffset, TokenType.KEYWORD1);
                                addToken(1, TokenType.PLAIN);
                                lastOffset = i + 1;
                                token = TokenType.LITERAL1;
                            }
                            break;
                        case ':':
                            if (!ignoreColon) {
                                if (i == offset || array[i - 1] != '\\') {
                                    addToken(i - lastOffset, TokenType.KEYWORD1);
                                    addToken(1, TokenType.PLAIN);
                                    lastOffset = i + 1;
                                    token = TokenType.LITERAL1;
                                }
                            }
                            break;
                        case ' ':
                            if (spacesBreakKey && nonWhitespace && (i == offset || array[i - 1] != '\\')) {
                                addToken(i - lastOffset, TokenType.KEYWORD1);
                                token = TokenType.LITERAL1;
                                lastOffset = i;
                            }
                            break;

                    }
                    break;
                case LITERAL1:
                    switch (c) {
                        case '#':
                        case '!':
                            if (!nonWhitespace) {
                                addToken(i - lastOffset, TokenType.COMMENT1);
                                lastOffset = length;
                                break loop;
                            }
                            break;

                    }
                    break;
            }
        }

        if (lastOffset != length) {
            addToken(length - lastOffset, token);
        }

        setTokenizationContinuedOnNextLine(false);
        if (token == TokenType.LITERAL1) {
            if (length == 0 || array[length - 1] != '\\') {
                token = TokenType.PLAIN;
            } else {
                token = TokenType.LITERAL1;
                setTokenizationContinuedOnNextLine(true);
            }
        } else {
            token = TokenType.PLAIN;
        }

        // if the line ends in \, stay in the same state for the next line
        return token;
    }
    private boolean spacesBreakKey = false;

    @Override
    public boolean isMultilineTokenizationRequired() {
        return true;
    }
}
