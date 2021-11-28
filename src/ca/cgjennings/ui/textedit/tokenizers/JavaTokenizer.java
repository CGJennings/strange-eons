package ca.cgjennings.ui.textedit.tokenizers;

import ca.cgjennings.ui.textedit.*;
import java.util.EnumSet;
import javax.swing.text.Segment;

/**
 * Tokenizer for editing Java source files.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class JavaTokenizer extends Tokenizer {

    public JavaTokenizer() {
        this(getKeywords());
    }

    public JavaTokenizer(KeywordMap keywords) {
        this.keywords = keywords;
    }

    @Override
    public String getCommentPrefix() {
        return "//";
    }

    @Override
    public boolean isBraceIndented() {
        return true;
    }

    @Override
    public EnumSet<TokenType> getNaturalLanguageTokenTypes() {
        return EnumSet.of(TokenType.COMMENT1, TokenType.COMMENT2, TokenType.LITERAL_STRING1);
    }

    @Override
    protected TokenType tokenizeImpl(TokenType token, Segment line, int lineIndex) {
        char[] array = line.array;
        int offset = line.offset;
        lastOffset = offset;
        lastKeyword = offset;
        int length = line.count + offset;
        boolean backslash = false;
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
                case PLAIN:
                    switch (c) {
                        case '@':
                            if (backslash) {
                                backslash = false;
                            }
                            break;
                        case '"':
                            doKeyword(line, i, c);
                            if (backslash) {
                                backslash = false;
                            } else {
                                addToken(i - lastOffset, token);
                                token = TokenType.LITERAL_STRING1;
                                lastOffset = lastKeyword = i;
                            }
                            break;
                        case '\'':
                            doKeyword(line, i, c);
                            if (backslash) {
                                backslash = false;
                            } else {
                                addToken(i - lastOffset, token);
                                token = TokenType.LITERAL_STRING2;
                                lastOffset = lastKeyword = i;
                            }
                            break;
                        case ':':
                            if (lastKeyword == offset) {
                                if (doKeyword(line, i, c)) {
                                    break;
                                }
                                backslash = false;
                                addToken(i1 - lastOffset, TokenType.LABEL);
                                lastOffset = lastKeyword = i1;
                            } else if (doKeyword(line, i, c)) {
                                break;
                            }
                            break;
                        case '/':
                            backslash = false;
                            doKeyword(line, i, c);
                            if (length - i > 1) {
                                switch (array[i1]) {
                                    case '*':
                                        addToken(i - lastOffset, token);
                                        lastOffset = lastKeyword = i;
                                        if (length - i > 2 && array[i + 2] == '*') {
                                            token = TokenType.COMMENT2;
                                        } else {
                                            token = TokenType.COMMENT1;
                                        }
                                        break;
                                    case '/':
                                        addToken(i - lastOffset, token);
                                        addToken(length - i, TokenType.COMMENT1);
                                        lastOffset = lastKeyword = length;
                                        break loop;
                                }
                            }
                            break;
                        default:
                            backslash = false;
                            if (!Character.isLetterOrDigit(c) && c != '_') {
                                doKeyword(line, i, c);//					if( Character.isDigit(c) || c =='.' ) {
//					    addToken( i - lastOffset, token );
//					    lastOffset = lastKeyword = i;
//					}
                            }
                            break;
                    }
                    break;
                case COMMENT1:
                case COMMENT2:
                    backslash = false;
                    if (c == '*' && length - i > 1) {
                        if (array[i1] == '/') {
                            i++;
                            addToken((i + 1) - lastOffset, token);
                            token = TokenType.PLAIN;
                            lastOffset = lastKeyword = i + 1;
                        }
                    }
                    break;
                case LITERAL_STRING1:
                    if (backslash) {
                        backslash = false;
                    } else if (c == '"') {
                        addToken(i1 - lastOffset, token);
                        token = TokenType.PLAIN;
                        lastOffset = lastKeyword = i1;
                    }
                    break;
                case LITERAL_STRING2:
                    if (backslash) {
                        backslash = false;
                    } else if (c == '\'') {
                        addToken(i1 - lastOffset, TokenType.LITERAL_STRING1);
                        token = TokenType.PLAIN;
                        lastOffset = lastKeyword = i1;
                    }
                    break;
//			    case TokenType.LITERAL_SPECIAL_1:
//				if( !Character.isDigit(c) ) {
//
//				}
//				break;
                default:
                    throw new InternalError("Invalid state: " + token);
            }
        }

        if (token == TokenType.PLAIN) {
            doKeyword(line, length, '\0');
        }
        switch (token) {
            case LITERAL_STRING1:
            case LITERAL_STRING2:
                addToken(length - lastOffset, TokenType.INVALID);
                token = TokenType.PLAIN;
                break;
            case KEYWORD2:
                addToken(length - lastOffset, token);
                if (!backslash) {
                    token = TokenType.PLAIN;
                }
            default:
                addToken(length - lastOffset, token);
                break;
        }

        return token;
    }

    public static KeywordMap getKeywords() {
        if (javaKeywords == null) {
            javaKeywords = new KeywordMap(false);
            javaKeywords.add("package", TokenType.KEYWORD2);
            javaKeywords.add("import", TokenType.KEYWORD2);

            javaKeywords.add("byte", TokenType.KEYWORD3);
            javaKeywords.add("char", TokenType.KEYWORD3);
            javaKeywords.add("short", TokenType.KEYWORD3);
            javaKeywords.add("int", TokenType.KEYWORD3);
            javaKeywords.add("long", TokenType.KEYWORD3);
            javaKeywords.add("float", TokenType.KEYWORD3);
            javaKeywords.add("double", TokenType.KEYWORD3);
            javaKeywords.add("boolean", TokenType.KEYWORD3);
            javaKeywords.add("void", TokenType.KEYWORD3);

            javaKeywords.add("class", TokenType.KEYWORD1);
            javaKeywords.add("interface", TokenType.KEYWORD1);
            javaKeywords.add("abstract", TokenType.KEYWORD1);
            javaKeywords.add("final", TokenType.KEYWORD1);
            javaKeywords.add("private", TokenType.KEYWORD1);
            javaKeywords.add("protected", TokenType.KEYWORD1);
            javaKeywords.add("public", TokenType.KEYWORD1);
            javaKeywords.add("static", TokenType.KEYWORD1);
            javaKeywords.add("synchronized", TokenType.KEYWORD1);
            javaKeywords.add("native", TokenType.KEYWORD1);
            javaKeywords.add("volatile", TokenType.KEYWORD1);
            javaKeywords.add("transient", TokenType.KEYWORD1);
            javaKeywords.add("break", TokenType.KEYWORD1);
            javaKeywords.add("case", TokenType.KEYWORD1);
            javaKeywords.add("continue", TokenType.KEYWORD1);
            javaKeywords.add("default", TokenType.KEYWORD1);
            javaKeywords.add("do", TokenType.KEYWORD1);
            javaKeywords.add("else", TokenType.KEYWORD1);
            javaKeywords.add("for", TokenType.KEYWORD1);
            javaKeywords.add("if", TokenType.KEYWORD1);
            javaKeywords.add("instanceof", TokenType.KEYWORD1);
            javaKeywords.add("new", TokenType.KEYWORD1);
            javaKeywords.add("return", TokenType.KEYWORD1);
            javaKeywords.add("switch", TokenType.KEYWORD1);
            javaKeywords.add("while", TokenType.KEYWORD1);
            javaKeywords.add("throw", TokenType.KEYWORD1);
            javaKeywords.add("try", TokenType.KEYWORD1);
            javaKeywords.add("catch", TokenType.KEYWORD1);
            javaKeywords.add("extends", TokenType.KEYWORD1);
            javaKeywords.add("finally", TokenType.KEYWORD1);
            javaKeywords.add("implements", TokenType.KEYWORD1);
            javaKeywords.add("throws", TokenType.KEYWORD1);
            javaKeywords.add("this", TokenType.KEYWORD1);
            javaKeywords.add("null", TokenType.KEYWORD1);
            javaKeywords.add("super", TokenType.KEYWORD1);
            javaKeywords.add("true", TokenType.KEYWORD1);
            javaKeywords.add("false", TokenType.KEYWORD1);
            javaKeywords.add("assert", TokenType.KEYWORD1);

            javaKeywords.add("@Deprecated", TokenType.KEYWORD2);
            javaKeywords.add("@Override", TokenType.KEYWORD2);
            javaKeywords.add("@SuppressWarnings", TokenType.KEYWORD2);
            javaKeywords.add("@interface", TokenType.KEYWORD2);
            javaKeywords.add("@Documented", TokenType.KEYWORD2);
            javaKeywords.add("@Inherited", TokenType.KEYWORD2);
            javaKeywords.add("@Retention", TokenType.KEYWORD2);
            javaKeywords.add("@Target", TokenType.KEYWORD2);
        }
        return javaKeywords;
    }	// private members
    private static KeywordMap javaKeywords;
    private KeywordMap keywords;
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
