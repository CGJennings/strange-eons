package ca.cgjennings.ui.textedit.tokenizers;

import ca.cgjennings.ui.textedit.*;
import java.util.EnumSet;
import javax.swing.text.Segment;

/**
 * Tokenizer for editing HTML and XML files.
 *
 * @author Slava Pestov
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class HTMLTokenizer extends Tokenizer {

    public HTMLTokenizer() {
        this(true);
    }

    public HTMLTokenizer(boolean js) {
        this.js = js;
        keywords = JavaScriptTokenizer.getKeywords();
    }

    @Override
    public boolean isMultilineTokenizationRequired() {
        return true;
    }

    @Override
    public String getCommentPrefix() {
        return "<!--";
    }

    @Override
    public String getCommentSuffix() {
        return "-->";
    }

    @Override
    public EnumSet<TokenType> getNaturalLanguageTokenTypes() {
        return EnumSet.of(TokenType.PLAIN, TokenType.COMMENT1, TokenType.COMMENT2, TokenType.COMMENT3);
    }

    @Override
    protected TokenType tokenizeImpl(TokenType token, Segment line, int lineIndex) {
        char[] array = line.array;
        int offset = line.offset;
        lastOffset = offset;
        lastKeyword = offset;
        int length = line.count + offset;
        boolean backslash = false;
        char quoteType = '\0';

        // if we are continuing a quote from the previous line,
        // we need to restore quoteType to match the correct character
        if (token == TokenType.INTERNAL2) {
            quoteType = '\'';
            token = TokenType.LITERAL3;
        } else if (token == TokenType.INTERNAL3) {
            quoteType = '"';
            token = TokenType.LITERAL3;
        }

        loop:
        for (int i = offset; i < length; i++) {
            int i1 = (i + 1);

            char c = array[i];
            char c1 = (i1 == length) ? '\0' : array[i1];

            if (c == '\\') {
                backslash = !backslash;
                continue;
            }

            switch (token) {
                case PLAIN: // HTML text
                    backslash = false;
                    switch (c) {
                        case '<':
                            addToken(i - lastOffset, token);
                            lastOffset = lastKeyword = i;
                            if (regionMatches(false, line, i1, "!--")) {
                                i += 3;
                                token = TokenType.COMMENT1;
                            } else if (js && regionMatches(true, line, i1, "script>")) {
                                addToken(8, TokenType.KEYWORD1);
                                lastOffset = lastKeyword = (i += 8);
                                token = TokenType.INTERNAL1;
                            } else {
                                token = TokenType.KEYWORD1;
                            }
                            break;
                        case '&':
                            addToken(i - lastOffset, token);
                            lastOffset = lastKeyword = i;
                            token = TokenType.KEYWORD2;
                            break;
                    }
                    break;
                case LITERAL3: // string literal inside a tag
                    if (c == quoteType) {
                        quoteType = '\0';
                        addToken(i1 - lastOffset, token);
                        lastOffset = lastKeyword = i1;
                        if (c1 == '>') {
                            token = TokenType.KEYWORD1;
                        } else {
                            token = TokenType.KEYWORD3;
                        }
                    }
                    break;
                case KEYWORD3: // attributes inside a tag
                    if (c1 == '>') {
                        addToken(i1 - lastOffset, token);
                        lastOffset = lastKeyword = i1;
                        token = TokenType.KEYWORD1;
                    } else if (c == '\'' || c == '\"') {
                        quoteType = c;
                        addToken(i - lastOffset, token);
                        lastOffset = lastKeyword = i;
                        token = TokenType.LITERAL3;
                    }
                    break;
                case KEYWORD1: // Inside a tag
                    backslash = false;
                    if (c == '>') {
                        addToken(i1 - lastOffset, token);
                        lastOffset = lastKeyword = i1;
                        token = TokenType.PLAIN;
                    } else if (c == ' ' && c1 != '>') {
                        addToken(i1 - lastOffset, token);
                        lastOffset = lastKeyword = i1;
                        token = TokenType.KEYWORD3;
                    }
                    break;
                case KEYWORD2: // Inside an entity
                    backslash = false;
                    if (c == ';') {
                        addToken(i1 - lastOffset, token);
                        lastOffset = lastKeyword = i1;
                        token = TokenType.PLAIN;
                        break;
                    }
                    break;
                case COMMENT1: // Inside a comment
                    backslash = false;
                    if (regionMatches(false, line, i, "-->")) {
                        addToken((i + 3) - lastOffset, token);
                        lastOffset = lastKeyword = i + 3;
                        token = TokenType.PLAIN;
                    }
                    break;

                case INTERNAL1: // Inside a JavaScript
                    switch (c) {
                        case '<':
                            backslash = false;
                            doKeyword(line, i, c);
                            if (regionMatches(true, line, i1, "/script>")) {
                                addToken(i - lastOffset,
                                        TokenType.PLAIN);
                                addToken(9, TokenType.KEYWORD1);
                                lastOffset = lastKeyword = (i += 9);
                                token = TokenType.PLAIN;
                            }
                            break;
                        case '"':
                            if (backslash) {
                                backslash = false;
                            } else {
                                doKeyword(line, i, c);
                                addToken(i - lastOffset, TokenType.PLAIN);
                                lastOffset = lastKeyword = i;
                                token = TokenType.LITERAL1;
                            }
                            break;
                        case '\'':
                            if (backslash) {
                                backslash = false;
                            } else {
                                doKeyword(line, i, c);
                                addToken(i - lastOffset, TokenType.PLAIN);
                                lastOffset = lastKeyword = i;
                                token = TokenType.LITERAL2;
                            }
                            break;
                        case '/':
                            backslash = false;
                            doKeyword(line, i, c);
                            if (length - i > 1) {
                                addToken(i - lastOffset, TokenType.PLAIN);
                                lastOffset = lastKeyword = i;
                                if (array[i1] == '/') {
                                    addToken(length - i, TokenType.COMMENT2);
                                    lastOffset = lastKeyword = length;
                                    break loop;
                                } else if (array[i1] == '*') {
                                    token = TokenType.COMMENT2;
                                }
                            }
                            break;
                        default:
                            backslash = false;
                            if (!Character.isLetterOrDigit(c) && c != '_') {
                                doKeyword(line, i, c);
                            }
                            break;
                    }
                    break;
                case LITERAL1: // JavaScript "..."
                    if (backslash) {
                        backslash = false;
                    } else if (c == '"') {
                        addToken(i1 - lastOffset, TokenType.LITERAL1);
                        lastOffset = lastKeyword = i1;
                        token = TokenType.INTERNAL1;
                    }
                    break;
                case LITERAL2: // JavaScript '...'
                    if (backslash) {
                        backslash = false;
                    } else if (c == '\'') {
                        addToken(i1 - lastOffset, TokenType.LITERAL1);
                        lastOffset = lastKeyword = i1;
                        token = TokenType.INTERNAL1;
                    }
                    break;
                case COMMENT2: // Inside a JavaScript comment
                    backslash = false;
                    if (c == '*' && length - i > 1 && array[i1] == '/') {
                        addToken((i += 2) - lastOffset, TokenType.COMMENT2);
                        lastOffset = lastKeyword = i;
                        token = TokenType.INTERNAL1;
                    }
                    break;
                default:
                    throw new InternalError("Invalid state: " + token);
            }
        }

        switch (token) {
            case LITERAL1:
            case LITERAL2:
                addToken(length - lastOffset, TokenType.INVALID);
                token = TokenType.INTERNAL1;
                break;
            case KEYWORD2:
                addToken(length - lastOffset, TokenType.INVALID);
                token = TokenType.PLAIN;
                break;
            case INTERNAL1:
                doKeyword(line, length, '\0');
                addToken(length - lastOffset, TokenType.PLAIN);
                break;
            case LITERAL3:
                addToken(length - lastOffset, TokenType.LITERAL3);
                token = quoteType == '\'' ? TokenType.INTERNAL2 : TokenType.INTERNAL3;
                break;
            default:
                addToken(length - lastOffset, token);
                break;
        }

        return token;
    }

    private KeywordMap keywords;
    private boolean js;
    private int lastOffset;
    private int lastKeyword;

    private boolean doKeyword(Segment line, int i, char c) {
        int i1 = i + 1;

        int len = i - lastKeyword;
        TokenType id = keywords.get(line, lastKeyword, len);
        if (id != TokenType.PLAIN) {
            if (lastKeyword != lastOffset) {
                addToken(lastKeyword - lastOffset, TokenType.PLAIN);
            }
            addToken(len, id);
            lastOffset = i;
        }
        lastKeyword = i1;
        return false;
    }
}
