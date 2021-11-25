package ca.cgjennings.ui.textedit.tokenizers;

import ca.cgjennings.ui.textedit.*;
import ca.cgjennings.ui.textedit.completion.CodeCompleter;
import ca.cgjennings.ui.textedit.completion.ScriptCompleter;
import java.util.EnumSet;
import javax.swing.text.Segment;

/**
 * Tokenizer for editing TypeScript source files.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.3
 */
public class TypeScriptTokenizer extends Tokenizer {

    public TypeScriptTokenizer() {
        keywords = getKeywords();
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
    public CodeCompleter getCodeCompleter() {
        if (completer == null) {
//			completer = new ScriptCodeCompleter();
            completer = new ScriptCompleter();
        }
        return completer;
    }
    private static CodeCompleter completer;

    @Override
    public EnumSet<TokenType> getNaturalLanguageTokenTypes() {
        return EnumSet.of(TokenType.COMMENT1, TokenType.COMMENT2, TokenType.LITERAL1, TokenType.LITERAL2);
    }

    @Override
    protected TokenType tokenizeImpl(TokenType token, Segment line, int lineIndex) {
        char[] array = line.array;
        int offset = line.offset;
        lastOffset = offset;
        lastKeyword = offset;
        int length = line.count + offset;
        boolean backslash = false;

        boolean lastWasRegExpPunct = true;
        boolean thisIsRegExpPunct = true;

        // convert special regexp marker token back into PLAIN
        if (token == TokenType.INTERNAL1) {
            thisIsRegExpPunct = false;
            token = TokenType.PLAIN;
        }

        char c;
        char cPrev = '\0';
        loop:
        for (int i = offset; i < length; i++, cPrev = c) {
            int i1 = (i + 1);

            c = array[i];
            char c1 = (i1 == length) ? '\0' : array[i1];

            if (!Character.isSpaceChar(c)) {
                lastWasRegExpPunct = thisIsRegExpPunct;
                thisIsRegExpPunct = !(c == ')' || c == ']' || Character.isJavaIdentifierPart(c) || c == '#' || c == '$' || c == '@');
            }

            if (c == '\\') {
                backslash = !backslash;
                continue;
            }

            switch (token) {
                case PLAIN:
                    switch (c) {
//                        case '0': case '1': case '2': case '3': case '4':
//                        case '5': case '6': case '7': case '8': case '9':
//                            if( backslash ) {
//                                backslash = false;
//                            } else {
//
//                            }

                        case '"':
                            doKeyword(line, i);
                            if (backslash) {
                                backslash = false;
                            } else {
                                addToken(i - lastOffset, token);
                                token = TokenType.LITERAL1;
                                lastOffset = lastKeyword = i;
                            }
                            break;
                        case '$':
                            if (!Character.isJavaIdentifierStart(cPrev)) {
                                doKeyword(line, i);
                                if (backslash) {
                                    backslash = false;
                                } else {
                                    addToken(i - lastOffset, token);
                                    token = TokenType.LITERAL5;
                                    lastOffset = lastKeyword = i;
                                }
                            }
                            break;
                        case '@':
                            // @ cannot appear *inside* a var name, only at start
                            if (Character.isJavaIdentifierPart(cPrev) && i > offset) {
                                doKeyword(line, i);
                                addToken(i - lastOffset, token);
                                token = TokenType.INVALID;
                                lastOffset = lastKeyword = i;
                            } else {
                                doKeyword(line, i);
                                if (backslash) {
                                    backslash = false;
                                } else {
                                    addToken(i - lastOffset, token);
                                    token = TokenType.LITERAL3;
                                    lastOffset = lastKeyword = i;
                                }
                            }
                            break;
                        case '\'':
                            doKeyword(line, i);
                            if (backslash) {
                                backslash = false;
                            } else {
                                addToken(i - lastOffset, token);
                                token = TokenType.LITERAL2;
                                lastOffset = lastKeyword = i;
                            }
                            break;
                        case ':':
                            if (lastKeyword == offset) {
                                if (doKeyword(line, i)) {
                                    break;
                                }
                                backslash = false;
                                addToken(i1 - lastOffset, TokenType.LABEL);
                                lastOffset = lastKeyword = i1;
                            } else if (doKeyword(line, i)) {
                                break;
                            }
                            break;
                        case '/':
                            backslash = false;

                            doKeyword(line, i);
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
                            // no comment detected, check for RegExp
                            if (token == TokenType.PLAIN) {
                                if (lastWasRegExpPunct) {
                                    addToken(i - lastOffset, token);
                                    lastOffset = lastKeyword = i;
                                    token = TokenType.LITERAL4;
                                }
                            }
                            break;
                        default:
                            backslash = false;
                            // the c != '.' is a quick hack to prevent object.keyword from being invalid
                            if (!Character.isLetterOrDigit(c) && c != '_' && c != '$' && c != '.') {
                                doKeyword(line, i);
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
                case LITERAL1:
                    if (backslash) {
                        backslash = false;
                    } else if (c == '"') {
                        addToken(i1 - lastOffset, token);
                        token = TokenType.PLAIN;
                        lastOffset = lastKeyword = i1;
                    }
                    break;
                case LITERAL2:
                    if (backslash) {
                        backslash = false;
                    } else if (c == '\'') {
                        addToken(i1 - lastOffset, TokenType.LITERAL2);
                        token = TokenType.PLAIN;
                        lastOffset = lastKeyword = i1;
                    }
                    break;
                case LITERAL3:
                case LITERAL5:
                    if (!(Character.isJavaIdentifierPart(c) || c == '-')) {
                        addToken(i - lastOffset, token);
                        token = TokenType.PLAIN;
                        lastOffset = lastKeyword = i;
                        --i;
                    }
                    break;
                case LITERAL4:
                    // Regular Expression Literal
                    if (backslash) {
                        backslash = false;
                    } else if (c == '/') {
                        // reached end of literal, now we need to check for flags
                        while (i1 < length && Character.isLetter(c1)) {
                            ++i1;
                            ++i;
                            c1 = (i1 == length) ? '\0' : array[i1];
                        }
                        addToken(i1 - lastOffset, TokenType.LITERAL4);
                        token = TokenType.PLAIN;
                        lastOffset = lastKeyword = i1;
                    }
                    backslash = false;
                    break;

                case INVALID:
                    // When a token is marked invalid within this loop it is
                    // due to a bad variable name; keep going until we get to
                    // some non-identifier char
                    if (!Character.isLetterOrDigit(c) && c != '_' && c != '$' && c != '@' && c != '#') {
                        addToken(i - lastOffset, TokenType.INVALID);
                        token = TokenType.PLAIN;
                        lastOffset = lastKeyword = i;
                        --i;
                    }
                    break;

                default:
                    throw new IllegalStateException("Invalid state: " + token);
            }
        }

        if (token == TokenType.PLAIN) {
            doKeyword(line, length);
        }

        switch (token) {
            case INVALID:
                // this is a hanging identifier (# or @ at end of line)
                // and should not carry over to the next line
                addToken(length - lastOffset, TokenType.INVALID);
                token = TokenType.PLAIN;
                break;
            case LITERAL1:
            case LITERAL2:
            case LITERAL4:
                if (backslash) {
                    addToken(length - lastOffset, token);
                } else {
                    addToken(length - lastOffset, TokenType.INVALID);
                    token = TokenType.PLAIN;
                }
                break;

            case LITERAL3:
            case LITERAL5:
            case KEYWORD2:
                addToken(length - lastOffset, token);
                if (!backslash) {
                    token = TokenType.PLAIN;
                }
                break;
            default:
                addToken(length - lastOffset, token);
                break;
        }

        // this special token marks that the next line
        // will *not* start with a regexp if the line starts with /
        if ((token == TokenType.PLAIN) && !thisIsRegExpPunct) {
            token = TokenType.INTERNAL1;
        }

        return token;
    }
    private final KeywordMap keywords;
    private int lastOffset;
    private int lastKeyword;

    private boolean doKeyword(Segment line, int i) {
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

    public static KeywordMap getKeywords() {
        if (tsKeywords == null) {
            tsKeywords = new KeywordMap(false);
            tsKeywords.add("function", TokenType.KEYWORD1);
            tsKeywords.add("var", TokenType.KEYWORD1);
            tsKeywords.add("const", TokenType.KEYWORD1);

            tsKeywords.add("else", TokenType.KEYWORD1);
            tsKeywords.add("for", TokenType.KEYWORD1);
            tsKeywords.add("if", TokenType.KEYWORD1);
            tsKeywords.add("in", TokenType.KEYWORD1);
            tsKeywords.add("new", TokenType.KEYWORD1);
            tsKeywords.add("delete", TokenType.KEYWORD1);
            tsKeywords.add("return", TokenType.KEYWORD1);
            tsKeywords.add("do", TokenType.KEYWORD1);
            tsKeywords.add("while", TokenType.KEYWORD1);
            tsKeywords.add("with", TokenType.KEYWORD1);
            tsKeywords.add("break", TokenType.KEYWORD1);
            tsKeywords.add("switch", TokenType.KEYWORD1);
            tsKeywords.add("case", TokenType.KEYWORD1);
            tsKeywords.add("continue", TokenType.KEYWORD1);
            tsKeywords.add("default", TokenType.KEYWORD1);

            tsKeywords.add("try", TokenType.KEYWORD1);
            tsKeywords.add("throw", TokenType.KEYWORD1);
            tsKeywords.add("catch", TokenType.KEYWORD1);
            tsKeywords.add("finally", TokenType.KEYWORD1);
            tsKeywords.add("instanceof", TokenType.KEYWORD1);
            tsKeywords.add("typeof", TokenType.KEYWORD1);

            tsKeywords.add("null", TokenType.KEYWORD1);
            tsKeywords.add("this", TokenType.KEYWORD1);
            tsKeywords.add("true", TokenType.KEYWORD1);
            tsKeywords.add("false", TokenType.KEYWORD1);
            tsKeywords.add("undefined", TokenType.KEYWORD1);

            // standard global objects, functions and properties
            tsKeywords.add("eval", TokenType.KEYWORD2);
            tsKeywords.add("decodeURI", TokenType.KEYWORD2);
            tsKeywords.add("decodeURIComponent", TokenType.KEYWORD2);
            tsKeywords.add("encodeURI", TokenType.KEYWORD2);
            tsKeywords.add("encodeURIComponent", TokenType.KEYWORD2);
            tsKeywords.add("isFinite", TokenType.KEYWORD2);
            tsKeywords.add("isNaN", TokenType.KEYWORD2);
            tsKeywords.add("parseFloat", TokenType.KEYWORD2);
            tsKeywords.add("parseInt", TokenType.KEYWORD2);
            tsKeywords.add("Infinity", TokenType.KEYWORD2);
            tsKeywords.add("NaN", TokenType.KEYWORD2);
            // arguments is only valid in function scope
            tsKeywords.add("arguments", TokenType.KEYWORD2);

            tsKeywords.add("Object", TokenType.KEYWORD2);
            tsKeywords.add("Array", TokenType.KEYWORD2);
            tsKeywords.add("Boolean", TokenType.KEYWORD2);
            tsKeywords.add("Date", TokenType.KEYWORD2);
            tsKeywords.add("Error", TokenType.KEYWORD2);
            tsKeywords.add("EvalError", TokenType.KEYWORD2);
            tsKeywords.add("RangeError", TokenType.KEYWORD2);
            tsKeywords.add("ReferenceError", TokenType.KEYWORD2);
            tsKeywords.add("SyntaxError", TokenType.KEYWORD2);
            tsKeywords.add("TypeError", TokenType.KEYWORD2);
            tsKeywords.add("URIError", TokenType.KEYWORD2);
            tsKeywords.add("Function", TokenType.KEYWORD2);
            tsKeywords.add("Math", TokenType.KEYWORD2);
            tsKeywords.add("Number", TokenType.KEYWORD2);
            tsKeywords.add("RegExp", TokenType.KEYWORD2);
            tsKeywords.add("String", TokenType.KEYWORD2);

            // TS
            tsKeywords.add("any", TokenType.KEYWORD2);
            tsKeywords.add("boolean", TokenType.KEYWORD2);
            tsKeywords.add("string", TokenType.KEYWORD2);
            tsKeywords.add("void", TokenType.KEYWORD2);

            // Newer versions of JS
            tsKeywords.add("debugger", TokenType.KEYWORD1);
            tsKeywords.add("yield", TokenType.KEYWORD1);
            tsKeywords.add("let", TokenType.KEYWORD1);
            tsKeywords.add("each", TokenType.KEYWORD1);
            tsKeywords.add("get", TokenType.KEYWORD1);
            tsKeywords.add("set", TokenType.KEYWORD1);
            
            tsKeywords.add("of", TokenType.KEYWORD1);
            tsKeywords.add("in", TokenType.KEYWORD1);

            // TS
            tsKeywords.add("abstract", TokenType.KEYWORD1);
            tsKeywords.add("class", TokenType.KEYWORD1);
            tsKeywords.add("enum", TokenType.KEYWORD1);
            tsKeywords.add("import", TokenType.KEYWORD1);
            tsKeywords.add("export", TokenType.KEYWORD1);
            tsKeywords.add("extends", TokenType.KEYWORD1);
            tsKeywords.add("final", TokenType.KEYWORD1);
            tsKeywords.add("implements", TokenType.KEYWORD1);
            tsKeywords.add("interface", TokenType.KEYWORD1);
            tsKeywords.add("private", TokenType.KEYWORD1);
            tsKeywords.add("protected", TokenType.KEYWORD1);
            tsKeywords.add("public", TokenType.KEYWORD1);
            tsKeywords.add("static", TokenType.KEYWORD1);
            tsKeywords.add("super", TokenType.KEYWORD1);
            tsKeywords.add("type", TokenType.KEYWORD1);
            tsKeywords.add("typeof", TokenType.KEYWORD1);
            tsKeywords.add("as", TokenType.KEYWORD1);

            // unimplemented reserved words
            tsKeywords.add("byte", TokenType.INVALID);
            tsKeywords.add("char", TokenType.INVALID);
            tsKeywords.add("double", TokenType.INVALID);
            tsKeywords.add("float", TokenType.INVALID);
            tsKeywords.add("goto", TokenType.INVALID);
            tsKeywords.add("int", TokenType.INVALID);
            tsKeywords.add("long", TokenType.INVALID);
            tsKeywords.add("native", TokenType.INVALID);
            tsKeywords.add("package", TokenType.INVALID);
            tsKeywords.add("short", TokenType.INVALID);
            tsKeywords.add("synchronized", TokenType.INVALID);
            tsKeywords.add("throws", TokenType.INVALID);
            tsKeywords.add("transient", TokenType.INVALID);
            tsKeywords.add("volatile", TokenType.INVALID);

            // Rhino JavaScript stuff
            tsKeywords.add("importClass", TokenType.KEYWORD2);
            tsKeywords.add("importPackage", TokenType.KEYWORD2);

            // SE global functions and properties
            tsKeywords.add("useLibrary", TokenType.KEYWORD2);

            tsKeywords.add("Eons", TokenType.KEYWORD2);
            tsKeywords.add("PluginContext", TokenType.KEYWORD2);
            tsKeywords.add("Editor", TokenType.KEYWORD2);
            tsKeywords.add("Component", TokenType.KEYWORD2);
            tsKeywords.add("Console", TokenType.KEYWORD2);
            tsKeywords.add("Patch", TokenType.KEYWORD2);
        }
        return tsKeywords;
    }
    private static KeywordMap tsKeywords;
}
