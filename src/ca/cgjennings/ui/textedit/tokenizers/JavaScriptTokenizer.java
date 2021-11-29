package ca.cgjennings.ui.textedit.tokenizers;

import ca.cgjennings.ui.textedit.*;
import ca.cgjennings.ui.textedit.completion.CodeCompleter;
import ca.cgjennings.ui.textedit.completion.ScriptCompleter;
import java.util.EnumSet;
import javax.swing.text.Segment;

/**
 * Tokenizer for editing JavaScript source files.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class JavaScriptTokenizer extends Tokenizer {

    public JavaScriptTokenizer() {
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
    public WordDefiner getWordDefiner() {
        return WORD_DEFINER;
    }
    private static final WordDefiner WORD_DEFINER = (int codePoint) -> {
        if (Character.isAlphabetic(codePoint)) {
            return true;
        }
        switch (codePoint) {
            case '$':
            case '#':
            case '_':
            case '@':
                return true;
        }
        return false;
    };

    @Override
    public EnumSet<TokenType> getNaturalLanguageTokenTypes() {
        return EnumSet.of(TokenType.COMMENT1, TokenType.COMMENT2, TokenType.LITERAL_STRING1, TokenType.LITERAL_STRING2);
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
                                token = TokenType.LITERAL_STRING1;
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
                                    token = TokenType.LITERAL_SPECIAL_2;
                                    lastOffset = lastKeyword = i;
                                }
                            }
                            break;
                        case '@':
                        case '#':
                            // @ and # cannot appear *inside* a var name, only at start
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
                                    token = TokenType.LITERAL_SPECIAL_1;
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
                                token = TokenType.LITERAL_STRING2;
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
                                    token = TokenType.LITERAL_REGEX;
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
                        addToken(i1 - lastOffset, TokenType.LITERAL_STRING2);
                        token = TokenType.PLAIN;
                        lastOffset = lastKeyword = i1;
                    }
                    break;

                case LITERAL_SPECIAL_1:
                case LITERAL_SPECIAL_2:
                    if (!(Character.isJavaIdentifierPart(c) || c == '-')) {
                        addToken(i - lastOffset, token);
                        token = TokenType.PLAIN;
                        lastOffset = lastKeyword = i;
                        --i;
                    }
                    break;
                case LITERAL_REGEX:
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
                        addToken(i1 - lastOffset, TokenType.LITERAL_REGEX);
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
            case LITERAL_STRING1:
            case LITERAL_STRING2:
            case LITERAL_REGEX:
                if (backslash) {
                    addToken(length - lastOffset, token);
                } else {
                    addToken(length - lastOffset, TokenType.INVALID);
                    token = TokenType.PLAIN;
                }
                break;

            case LITERAL_SPECIAL_1:
            case LITERAL_SPECIAL_2:
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
        if (jsKeywords == null) {
            jsKeywords = new KeywordMap(false);
            jsKeywords.add("function", TokenType.KEYWORD1);
            jsKeywords.add("var", TokenType.KEYWORD1);
            jsKeywords.add("const", TokenType.KEYWORD1);
            jsKeywords.add("void", TokenType.KEYWORD1);

            jsKeywords.add("else", TokenType.KEYWORD1);
            jsKeywords.add("for", TokenType.KEYWORD1);
            jsKeywords.add("if", TokenType.KEYWORD1);
            jsKeywords.add("in", TokenType.KEYWORD1);
            jsKeywords.add("new", TokenType.KEYWORD1);
            jsKeywords.add("delete", TokenType.KEYWORD1);
            jsKeywords.add("return", TokenType.KEYWORD1);
            jsKeywords.add("do", TokenType.KEYWORD1);
            jsKeywords.add("while", TokenType.KEYWORD1);
            jsKeywords.add("with", TokenType.KEYWORD1);
            jsKeywords.add("break", TokenType.KEYWORD1);
            jsKeywords.add("switch", TokenType.KEYWORD1);
            jsKeywords.add("case", TokenType.KEYWORD1);
            jsKeywords.add("continue", TokenType.KEYWORD1);
            jsKeywords.add("default", TokenType.KEYWORD1);

            jsKeywords.add("try", TokenType.KEYWORD1);
            jsKeywords.add("throw", TokenType.KEYWORD1);
            jsKeywords.add("catch", TokenType.KEYWORD1);
            jsKeywords.add("finally", TokenType.KEYWORD1);
            jsKeywords.add("instanceof", TokenType.KEYWORD1);
            jsKeywords.add("typeof", TokenType.KEYWORD1);

            jsKeywords.add("null", TokenType.KEYWORD1);
            jsKeywords.add("this", TokenType.KEYWORD1);
            jsKeywords.add("true", TokenType.KEYWORD1);
            jsKeywords.add("false", TokenType.KEYWORD1);
            jsKeywords.add("undefined", TokenType.KEYWORD1);

            // standard global objects, functions and properties
            jsKeywords.add("eval", TokenType.KEYWORD2);
            jsKeywords.add("decodeURI", TokenType.KEYWORD2);
            jsKeywords.add("decodeURIComponent", TokenType.KEYWORD2);
            jsKeywords.add("encodeURI", TokenType.KEYWORD2);
            jsKeywords.add("encodeURIComponent", TokenType.KEYWORD2);
            jsKeywords.add("isFinite", TokenType.KEYWORD2);
            jsKeywords.add("isNaN", TokenType.KEYWORD2);
            jsKeywords.add("parseFloat", TokenType.KEYWORD2);
            jsKeywords.add("parseInt", TokenType.KEYWORD2);
            jsKeywords.add("Infinity", TokenType.KEYWORD2);
            jsKeywords.add("NaN", TokenType.KEYWORD2);
            // arguments is only valid in function scope
            jsKeywords.add("arguments", TokenType.KEYWORD2);

            jsKeywords.add("Object", TokenType.KEYWORD2);
            jsKeywords.add("Array", TokenType.KEYWORD2);
            jsKeywords.add("Boolean", TokenType.KEYWORD2);
            jsKeywords.add("Date", TokenType.KEYWORD2);
            jsKeywords.add("Error", TokenType.KEYWORD2);
            jsKeywords.add("EvalError", TokenType.KEYWORD2);
            jsKeywords.add("RangeError", TokenType.KEYWORD2);
            jsKeywords.add("ReferenceError", TokenType.KEYWORD2);
            jsKeywords.add("SyntaxError", TokenType.KEYWORD2);
            jsKeywords.add("TypeError", TokenType.KEYWORD2);
            jsKeywords.add("URIError", TokenType.KEYWORD2);
            jsKeywords.add("Function", TokenType.KEYWORD2);
            jsKeywords.add("Math", TokenType.KEYWORD2);
            jsKeywords.add("Number", TokenType.KEYWORD2);
            jsKeywords.add("RegExp", TokenType.KEYWORD2);
            jsKeywords.add("String", TokenType.KEYWORD2);

            jsKeywords.add("JSON", TokenType.KEYWORD2);
            jsKeywords.add("Symbol", TokenType.KEYWORD2);
            jsKeywords.add("Set", TokenType.KEYWORD2);
            jsKeywords.add("Map", TokenType.KEYWORD2);
            jsKeywords.add("WeakSet", TokenType.KEYWORD2);
            jsKeywords.add("WeakMap", TokenType.KEYWORD2);
            jsKeywords.add("ArrayBuffer", TokenType.KEYWORD2);
            jsKeywords.add("Int8Array", TokenType.KEYWORD2);
            jsKeywords.add("Uint8Array", TokenType.KEYWORD2);
            jsKeywords.add("Uint8ClampedArray", TokenType.KEYWORD2);
            jsKeywords.add("Int16Array", TokenType.KEYWORD2);
            jsKeywords.add("Uint16Array", TokenType.KEYWORD2);
            jsKeywords.add("Int32Array", TokenType.KEYWORD2);
            jsKeywords.add("Uint32Array", TokenType.KEYWORD2);
            jsKeywords.add("Float32Array", TokenType.KEYWORD2);
            jsKeywords.add("Float64Array", TokenType.KEYWORD2);
            jsKeywords.add("DataView", TokenType.KEYWORD2);


            // Newer versions of JS
            jsKeywords.add("debugger", TokenType.KEYWORD1);
            jsKeywords.add("yield", TokenType.KEYWORD1);
            jsKeywords.add("let", TokenType.KEYWORD1);
            jsKeywords.add("each", TokenType.KEYWORD1);
            jsKeywords.add("get", TokenType.KEYWORD1);
            jsKeywords.add("set", TokenType.KEYWORD1);
            
            jsKeywords.add("of", TokenType.KEYWORD1);
            jsKeywords.add("in", TokenType.KEYWORD1);


            // unimplemented reserved words
            jsKeywords.add("abstract", TokenType.INVALID);
            jsKeywords.add("boolean", TokenType.INVALID);
            jsKeywords.add("byte", TokenType.INVALID);
            jsKeywords.add("char", TokenType.INVALID);
            jsKeywords.add("class", TokenType.INVALID);
            jsKeywords.add("double", TokenType.INVALID);
            jsKeywords.add("enum", TokenType.INVALID);
            jsKeywords.add("export", TokenType.INVALID);
            jsKeywords.add("extends", TokenType.INVALID);
            jsKeywords.add("final", TokenType.INVALID);
            jsKeywords.add("float", TokenType.INVALID);
            jsKeywords.add("goto", TokenType.INVALID);
            jsKeywords.add("implements", TokenType.INVALID);
            jsKeywords.add("import", TokenType.INVALID);
            jsKeywords.add("int", TokenType.INVALID);
            jsKeywords.add("interface", TokenType.INVALID);
            jsKeywords.add("long", TokenType.INVALID);
            jsKeywords.add("native", TokenType.INVALID);
            jsKeywords.add("package", TokenType.INVALID);
            jsKeywords.add("private", TokenType.INVALID);
            jsKeywords.add("protected", TokenType.INVALID);
            jsKeywords.add("public", TokenType.INVALID);
            jsKeywords.add("short", TokenType.INVALID);
            jsKeywords.add("static", TokenType.INVALID);
            jsKeywords.add("super", TokenType.INVALID);
            jsKeywords.add("synchronized", TokenType.INVALID);
            jsKeywords.add("throws", TokenType.INVALID);
            jsKeywords.add("transient", TokenType.INVALID);
            jsKeywords.add("volatile", TokenType.INVALID);

            // Rhino JavaScript stuff
            jsKeywords.add("importClass", TokenType.KEYWORD2);
            jsKeywords.add("importPackage", TokenType.KEYWORD2);

            // SE global functions and properties
            jsKeywords.add("useLibrary", TokenType.KEYWORD2);

            jsKeywords.add("global", TokenType.KEYWORD2);
            jsKeywords.add("self", TokenType.KEYWORD2);
            jsKeywords.add("globalThis", TokenType.KEYWORD2);
            jsKeywords.add("Eons", TokenType.KEYWORD2);
            jsKeywords.add("PluginContext", TokenType.KEYWORD2);
            jsKeywords.add("Editor", TokenType.KEYWORD2);
            jsKeywords.add("Component", TokenType.KEYWORD2);
            jsKeywords.add("Console", TokenType.KEYWORD2);
            jsKeywords.add("Patch", TokenType.KEYWORD2);
        }
        return jsKeywords;
    }
    private static KeywordMap jsKeywords;
}
