/*
 * SEJavaScriptTokenMaker.java - Parses a document into JavaScript tokens.
 *
 * This version is heavily modified for Strange Rhino: removes E4X support,
 * adds several keywords, adds standard types, adds some
 * special SE-specific global objects and functions, and recognizes special
 * string lookup identifiers.
 * 
 * Adapted from the RSyntaxTextArea JavaScript tokenizer. See about
 * dialog tempate for license information.
 */
package ca.cgjennings.ui.textedit;

import java.io.*;
import javax.swing.text.Segment;
import java.util.Stack;

import org.fife.ui.rsyntaxtextarea.*;


/**
 * Scanner for SE JavaScript files. Unlike standard JavaScript, this
 * will recognize special string lookup identifiers starting with
 * {@code @} and {@code #}.
 *
 * <p>
 * <strong>
 * This file is generated automatically.
 * See {@code build-tools/README.md} for details.
 * </strong>
 */
%%

%public
%class SEJavaScriptTokenMaker
%extends AbstractJFlexCTokenMaker
%unicode
%type org.fife.ui.rsyntaxtextarea.Token


%{

	/**
     * Token type specifying we're in a JavaScript multiline comment.
     */
    static final int INTERNAL_IN_JS_MLC				= -8;

    /**
     * Token type specifying we're in a JavaScript documentation comment.
     */
    static final int INTERNAL_IN_JS_COMMENT_DOCUMENTATION = -9;

    /**
     * Token type specifying we're in an invalid multi-line JS string.
     */
    static final int INTERNAL_IN_JS_STRING_INVALID	= -10;

    /**
     * Token type specifying we're in a valid multi-line JS string.
     */
    static final int INTERNAL_IN_JS_STRING_VALID		= -11;

    /**
     * Token type specifying we're in an invalid multi-line JS single-quoted string.
     */
    static final int INTERNAL_IN_JS_CHAR_INVALID	= -12;

    /**
     * Token type specifying we're in a valid multi-line JS single-quoted string.
     */
    static final int INTERNAL_IN_JS_CHAR_VALID		= -13;

    /**
     * Token type specifying we're in a valid multi-line template literal.
     */
    static final int INTERNAL_IN_JS_TEMPLATE_LITERAL_VALID = -23;

    /**
     * Token type specifying we're in an invalid multi-line template literal.
     */
    static final int INTERNAL_IN_JS_TEMPLATE_LITERAL_INVALID = -24;

    /**
     * When in the JS_STRING state, whether the current string is valid.
     */
    private boolean validJSString;

    /**
     * Language state set on JS tokens.  Must be 0.
     */
    private static final int LANG_INDEX_DEFAULT	= 0;

    private Stack<Boolean> varDepths;

    /**
     * Constructor.  This must be here because JFlex does not generate a
     * no-parameter constructor.
     */
    public SEJavaScriptTokenMaker() {
        super();
    }


    /**
     * Adds the token specified to the current linked list of tokens as an
     * "end token;" that is, at <code>zzMarkedPos</code>.
     *
     * @param tokenType The token's type.
     */
    private void addEndToken(int tokenType) {
        addToken(zzMarkedPos,zzMarkedPos, tokenType);
    }


    /**
     * Adds the token specified to the current linked list of tokens.
     *
     * @param tokenType The token's type.
     * @see #addToken(int, int, int)
     */
    private void addHyperlinkToken(int start, int end, int tokenType) {
        int so = start + offsetShift;
        addToken(zzBuffer, start,end, tokenType, so, true);
    }


    /**
     * Adds the token specified to the current linked list of tokens.
     *
     * @param tokenType The token's type.
     */
    private void addToken(int tokenType) {
        addToken(zzStartRead, zzMarkedPos-1, tokenType);
    }


    /**
     * Adds the token specified to the current linked list of tokens.
     *
     * @param tokenType The token's type.
     */
    private void addToken(int start, int end, int tokenType) {
        int so = start + offsetShift;
        addToken(zzBuffer, start,end, tokenType, so);
    }


    /**
     * Adds the token specified to the current linked list of tokens.
     *
     * @param array The character array.
     * @param start The starting offset in the array.
     * @param end The ending offset in the array.
     * @param tokenType The token's type.
     * @param startOffset The offset in the document at which this token
     *                    occurs.
     */
    @Override
    public void addToken(char[] array, int start, int end, int tokenType, int startOffset) {
        super.addToken(array, start,end, tokenType, startOffset);
        zzStartRead = zzMarkedPos;
    }


    /**
     * Returns the closest {@link TokenTypes} "standard" token type for a given
     * "internal" token type (e.g. one whose value is <code>&lt; 0</code>).
     */
     @Override
    public int getClosestStandardTokenTypeForInternalType(int type) {
        switch (type) {
            case INTERNAL_IN_JS_MLC:
                return TokenTypes.COMMENT_MULTILINE;
            case INTERNAL_IN_JS_COMMENT_DOCUMENTATION:
                return TokenTypes.COMMENT_DOCUMENTATION;
            case INTERNAL_IN_JS_STRING_INVALID:
            case INTERNAL_IN_JS_STRING_VALID:
            case INTERNAL_IN_JS_CHAR_INVALID:
            case INTERNAL_IN_JS_CHAR_VALID:
                return TokenTypes.LITERAL_STRING_DOUBLE_QUOTE;
            case INTERNAL_IN_JS_TEMPLATE_LITERAL_VALID:
                return TokenTypes.LITERAL_BACKQUOTE;
            case INTERNAL_IN_JS_TEMPLATE_LITERAL_INVALID:
                return TokenTypes.ERROR_STRING_DOUBLE;
        }
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getLineCommentStartAndEnd(int languageIndex) {
        return new String[] { "//", null };
    }


    /**
     * Returns the first token in the linked list of tokens generated
     * from <code>text</code>.  This method must be implemented by
     * subclasses so they can correctly implement syntax highlighting.
     *
     * @param text The text from which to get tokens.
     * @param initialTokenType The token type we should start with.
     * @param startOffset The offset into the document at which
     *        <code>text</code> starts.
     * @return The first <code>Token</code> in a linked list representing
     *         the syntax highlighted text.
     */
    @Override
    public Token getTokenList(Segment text, int initialTokenType, int startOffset) {

        resetTokenList();
        this.offsetShift = -text.offset + startOffset;
        validJSString = true;
        int languageIndex = LANG_INDEX_DEFAULT;

        // Start off in the proper state.
        int state;
        switch (initialTokenType) {
            case INTERNAL_IN_JS_MLC:
                state = JS_MLC;
                break;
            case INTERNAL_IN_JS_COMMENT_DOCUMENTATION:
                state = JS_DOCCOMMENT;
                start = text.offset;
                break;
            case INTERNAL_IN_JS_STRING_INVALID:
                state = JS_STRING;
                validJSString = false;
                break;
            case INTERNAL_IN_JS_STRING_VALID:
                state = JS_STRING;
                break;
            case INTERNAL_IN_JS_CHAR_INVALID:
                state = JS_CHAR;
                validJSString = false;
                break;
            case INTERNAL_IN_JS_CHAR_VALID:
                state = JS_CHAR;
                break;
            case INTERNAL_IN_JS_TEMPLATE_LITERAL_VALID:
                state = JS_TEMPLATE_LITERAL;
                validJSString = true;
                break;
            case INTERNAL_IN_JS_TEMPLATE_LITERAL_INVALID:
                state = JS_TEMPLATE_LITERAL;
                validJSString = false;
                break;
            default:
                // Shouldn't happen
                state = YYINITIAL;
        }

        setLanguageIndex(languageIndex);
        start = text.offset;
        s = text;
        try {
            yyreset(zzReader);
            yybegin(state);
            return yylex();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return new TokenImpl();
        }

    }

    /**
     * Refills the input buffer.
     *
     * @return      <code>true</code> if EOF was reached, otherwise
     *              <code>false</code>.
     */
    private boolean zzRefill() {
        return zzCurrentPos>=s.offset+s.count;
    }


    /**
     * Resets the scanner to read from a new input stream.
     * Does not close the old reader.
     *
     * All internal variables are reset, the old input stream
     * <b>cannot</b> be reused (internal buffer is discarded and lost).
     * Lexical state is set to <tt>YY_INITIAL</tt>.
     *
     * @param reader   the new input stream
     */
    public final void yyreset(java.io.Reader reader) {
        // 's' has been updated.
        zzBuffer = s.array;
        /*
         * We replaced the line below with the two below it because zzRefill
         * no longer "refills" the buffer (since the way we do it, it's always
         * "full" the first time through, since it points to the segment's
         * array).  So, we assign zzEndRead here.
         */
        //zzStartRead = zzEndRead = s.offset;
        zzStartRead = s.offset;
        zzEndRead = zzStartRead + s.count - 1;
        zzCurrentPos = zzMarkedPos = zzPushbackPos = s.offset;
        zzLexicalState = YYINITIAL;
        zzReader = reader;
        zzAtBOL  = true;
        zzAtEOF  = false;
    }


%}

Whitespace			= ([ \t\f]+)
LineTerminator			= ([\n])

Letter							= [A-Za-z]
NonzeroDigit						= [1-9]
Digit							= ("0"|{NonzeroDigit})
HexDigit							= ({Digit}|[A-Fa-f])
OctalDigit						= ([0-7])
LetterOrDigit					= ({Letter}|{Digit})
EscapedSourceCharacter				= ("u"{HexDigit}{HexDigit}{HexDigit}{HexDigit})
NonSeparator						= ([^\t\f\r\n\ \(\)\{\}\[\]\;\,\.\=\>\<\!\~\?\:\+\-\*\/\&\|\^\%\"\'\`]|"#"|"\\")
IdentifierStart					= ({Letter}|"_")
IdentifierPart						= ({IdentifierStart}|{Digit}|("\\"{EscapedSourceCharacter}))
KeyIdentifierStart              = ("$"|"@"|"#")
KeyIdentifierPart                   = ({IdentifierPart}|{KeyIdentifierStart}|"-")
JS_MLCBegin				= "/*"
JS_DocCommentBegin			= "/**"
JS_MLCEnd					= "*/"
JS_LineCommentBegin			= "//"
JS_IntegerHelper1			= (({NonzeroDigit}{Digit}*)|"0")
JS_IntegerHelper2			= ("0"(([xX]{HexDigit}+)|({OctalDigit}*)))
JS_IntegerLiteral			= ({JS_IntegerHelper1}[lL]?)
JS_HexLiteral				= ({JS_IntegerHelper2}[lL]?)
JS_FloatHelper1			= ([fFdD]?)
JS_FloatHelper2			= ([eE][+-]?{Digit}+{JS_FloatHelper1})
JS_FloatLiteral1			= ({Digit}+"."({JS_FloatHelper1}|{JS_FloatHelper2}|{Digit}+({JS_FloatHelper1}|{JS_FloatHelper2})))
JS_FloatLiteral2			= ("."{Digit}+({JS_FloatHelper1}|{JS_FloatHelper2}))
JS_FloatLiteral3			= ({Digit}+{JS_FloatHelper2})
JS_FloatLiteral			= ({JS_FloatLiteral1}|{JS_FloatLiteral2}|{JS_FloatLiteral3}|({Digit}+[fFdD]))
JS_ErrorNumberFormat		= (({JS_IntegerLiteral}|{JS_HexLiteral}|{JS_FloatLiteral}){NonSeparator}+)
JS_Separator				= ([\(\)\{\}\[\]\]])
JS_Separator2				= ([\;,.])
JS_NonAssignmentOperator		= ("+"|"-"|"<="|"^"|"++"|"<"|"*"|">="|"%"|"--"|">"|"/"|"!="|"?"|">>"|"!"|"|"|"&"|"=="|":"|">>"|"~"|"||"|"&&"|">>>")
JS_AssignmentOperator		= ("="|"-="|"*="|"/="|"|="|"&="|"^="|"+="|"%="|"<<="|">>="|">>>=")
JS_Operator				= ({JS_NonAssignmentOperator}|{JS_AssignmentOperator})
JS_Identifier				= ({IdentifierStart}{IdentifierPart}*)
JS_KeyIdentifier            = ({KeyIdentifierStart}{KeyIdentifierPart}*)
JS_ErrorIdentifier			= ({NonSeparator}+)
JS_Regex					= ("/"([^\*\\/]|\\.)([^/\\]|\\.)*"/"[gim]*)

JS_BlockTag					= ("abstract"|"access"|"alias"|"augments"|"author"|"borrows"|
								"callback"|"classdesc"|"constant"|"constructor"|"constructs"|
								"copyright"|"default"|"deprecated"|"desc"|"enum"|"event"|
								"example"|"exports"|"external"|"file"|"fires"|"global"|
								"ignore"|"inner"|"instance"|"kind"|"lends"|"license"|
								"link"|"member"|"memberof"|"method"|"mixes"|"mixin"|"module"|
								"name"|"namespace"|"param"|"private"|"property"|"protected"|
								"public"|"readonly"|"requires"|"return"|"returns"|"see"|"since"|
								"static"|"summary"|"this"|"throws"|"todo"|
								"type"|"typedef"|"variation"|"version")
JS_InlineTag				= ("link"|"linkplain"|"linkcode"|"tutorial")
JS_TemplateLiteralExprStart	= ("${")

URLGenDelim				= ([:\/\?#\[\]@])
URLSubDelim				= ([\!\$&'\(\)\*\+,;=])
URLUnreserved			= ({LetterOrDigit}|"_"|[\-\.\~])
URLCharacter			= ({URLGenDelim}|{URLSubDelim}|{URLUnreserved}|[%])
URLCharacters			= ({URLCharacter}*)
URLEndCharacter			= ([\/\$]|{LetterOrDigit})
URL						= (((https?|f(tp|ile))"://"|"www.")({URLCharacters}{URLEndCharacter})?)


%state JS_STRING
%state JS_CHAR
%state JS_MLC
%state JS_DOCCOMMENT
%state JS_EOL_COMMENT
%state JS_TEMPLATE_LITERAL
%state JS_TEMPLATE_LITERAL_EXPR

%%

<YYINITIAL> {
	// ECMA keywords
	"break" |
    "case" |
    "catch" |
    "const"|
	"continue" |
    "debugger" |
    "default" |
	"delete" |
    "do" |
    "each" |
	"else" |
    "finallly" |
	"for" |
	"function" |
    "get" |
	"if" |
	"in" |
    "instanceof" |
    "let" |
	"new" |
    "of" |
    "return" |
    "set" |
    "switch" |
    "throw" |
    "try" |
	"typeof" |
	"var" |
	"void" |
	"while" |
	"with" |
    "yield"
    		{ addToken(Token.RESERVED_WORD); }

    "arguments" |
    "this" |
    "global" |
    "self" |
    "globalThis" |
    "Component" |
    "Console" |
    "Editor" |
    "Eons" |
    "PluginContext"
            { addToken(Token.RESERVED_WORD_2); }
	
    // Data types
    "Array" |
    "ArrayBuffer" |
    "BigInt" |
    "Boolean" |
    "Date" |
    "Error" |
    "EvalError" |
    "Function" |
    "JSON" |
    "Math" |
    "Map" |
    "Number" |
    "Object" |
    "Promise" |
    "RangeError" |
    "ReferenceError" |
    "RegExp" |
    "Set" |
    "String" |
    "Symbol" |
    "SyntaxError" |
    "TypeError" |
    "URIError" |
    "WeakSet" |
    "WeakMap" |
    "Int8Array" |
    "Uint8Array" |
    "Uint8ClampedArray" |
    "Int16Array" |
    "Uint16Array" |
    "Int32Array" |
    "Uint32Array" |
    "Float32Array" |
    "Float64Array" |
    "DataView"
            { addToken(Token.DATA_TYPE); }

	// Reserved (but unused) keywords
    "abstract" |
    "boolean" |
    "byte" |
    "char" |
	"double" |
    "enum" |
    "export" |
    "extends" |
    "final" |
    "float" |
    "goto" |
    "implements" |
    "import" |
    "int" |
    "interface" |
    "long" |
    "native" |
    "package" |
    "private" |
    "protected" |
    "public" |
    "short" |
    "static" |
    "super" |
    "synchronized" |
    "throws" |
    "transient" |
    "volatile" |
    "class" { addToken(Token.ERROR_IDENTIFIER); }


	// Literals.
	"false" |
	"true"						{ addToken(Token.LITERAL_BOOLEAN); }

	"NaN"						{ addToken(Token.RESERVED_WORD); }
	"Infinity"					{ addToken(Token.RESERVED_WORD); }
	"null"						{ addToken(Token.RESERVED_WORD); }
    "undefined" 				{ addToken(Token.RESERVED_WORD); }
    

	// Functions.
    "require" |
    "importClass" |
    "importPackage" |
    "useLibrary" |

	"eval" |
    "decodeURI" |
    "decodeURIComponent" |
    "encodeURI" |
    "encodeURIComponent" |
	"parseInt" |
	"parseFloat" |
	"isNaN" |
	"isFinite"						{ addToken(Token.FUNCTION); }

	{LineTerminator}				{ addNullToken(); return firstToken; }
	{JS_Identifier}					{ addToken(Token.IDENTIFIER); }
    {JS_KeyIdentifier}              { addToken(Token.ANNOTATION); }
	{Whitespace}					{ addToken(Token.WHITESPACE); }

	/* String/Character literals. */
	[\']							{ start = zzMarkedPos-1; validJSString = true; yybegin(JS_CHAR); }
	[\"]							{ start = zzMarkedPos-1; validJSString = true; yybegin(JS_STRING); }
	[\`]							{ start = zzMarkedPos-1; validJSString = true; yybegin(JS_TEMPLATE_LITERAL); }

	/* Comment literals. */
	"/**/"							{ addToken(Token.COMMENT_MULTILINE); }
	{JS_MLCBegin}					{ start = zzMarkedPos-2; yybegin(JS_MLC); }
	{JS_DocCommentBegin}			{ start = zzMarkedPos-3; yybegin(JS_DOCCOMMENT); }
	{JS_LineCommentBegin}			{ start = zzMarkedPos-2; yybegin(JS_EOL_COMMENT); }

	/* Attempt to identify regular expressions (not foolproof) - do after comments! */
	{JS_Regex}						{
										boolean highlightedAsRegex = false;
										if (firstToken==null) {
											addToken(Token.REGEX);
											highlightedAsRegex = true;
										}
										else {
											// If this is *likely* to be a regex, based on
											// the previous token, highlight it as such.
											Token t = firstToken.getLastNonCommentNonWhitespaceToken();
											if (RSyntaxUtilities.regexCanFollowInJavaScript(t)) {
												addToken(Token.REGEX);
												highlightedAsRegex = true;
											}
										}
										// If it doesn't *appear* to be a regex, highlight it as
										// individual tokens.
										if (!highlightedAsRegex) {
											int temp = zzStartRead + 1;
											addToken(zzStartRead, zzStartRead, Token.OPERATOR);
											zzStartRead = zzCurrentPos = zzMarkedPos = temp;
										}
									}

	/* Separators. */
	{JS_Separator}					{ addToken(Token.SEPARATOR); }
	{JS_Separator2}					{ addToken(Token.IDENTIFIER); }

	/* Operators. */
	[\+]?"="{Whitespace}*"<"		{
										int start = zzStartRead;
										int operatorLen = yycharat(0)=='+' ? 2 : 1;
										int yylen = yylength(); // Cache before first addToken() invalidates it
										addToken(zzStartRead,zzStartRead+operatorLen-1, Token.OPERATOR);
										if (yylen>operatorLen+1) {
											addToken(start+operatorLen,zzMarkedPos-2, Token.WHITESPACE);
										}
										zzStartRead = zzCurrentPos = zzMarkedPos = zzMarkedPos - 1;
									}
	{JS_Operator}					{ addToken(Token.OPERATOR); }

	/* Numbers */
	{JS_IntegerLiteral}				{ addToken(Token.LITERAL_NUMBER_DECIMAL_INT); }
	{JS_HexLiteral}				{ addToken(Token.LITERAL_NUMBER_HEXADECIMAL); }
	{JS_FloatLiteral}				{ addToken(Token.LITERAL_NUMBER_FLOAT); }
	{JS_ErrorNumberFormat}			{ addToken(Token.ERROR_NUMBER_FORMAT); }

	{JS_ErrorIdentifier}			{ addToken(Token.ERROR_IDENTIFIER); }

	/* Ended with a line not in a string or comment. */
	<<EOF>>						{ addNullToken(); return firstToken; }

	/* Catch any other (unhandled) characters and flag them as bad. */
	.							{ addToken(Token.ERROR_IDENTIFIER); }

}

<JS_STRING> {
	[^\n\\\"]+				{}
	\\x{HexDigit}{2}		{}
	\\x						{ /* Invalid latin-1 character \xXX */ validJSString = false; }
	\\u{HexDigit}{4}		{}
	\\u						{ /* Invalid Unicode character \\uXXXX */ validJSString = false; }
	\\.						{ /* Skip all escaped chars. */ }
	\\						{ /* Line ending in '\' => continue to next line. */
								if (validJSString) {
									addToken(start,zzStartRead, Token.LITERAL_STRING_DOUBLE_QUOTE);
									addEndToken(INTERNAL_IN_JS_STRING_VALID);
								}
								else {
									addToken(start,zzStartRead, Token.ERROR_STRING_DOUBLE);
									addEndToken(INTERNAL_IN_JS_STRING_INVALID);
								}
								return firstToken;
							}
	\"						{ int type = validJSString ? Token.LITERAL_STRING_DOUBLE_QUOTE : Token.ERROR_STRING_DOUBLE; addToken(start,zzStartRead, type); yybegin(YYINITIAL); }
	\n |
	<<EOF>>					{ addToken(start,zzStartRead-1, Token.ERROR_STRING_DOUBLE); addNullToken(); return firstToken; }
}

<JS_CHAR> {
	[^\n\\\']+				{}
	\\x{HexDigit}{2}		{}
	\\x						{ /* Invalid latin-1 character \xXX */ validJSString = false; }
	\\u{HexDigit}{4}		{}
	\\u						{ /* Invalid Unicode character \\uXXXX */ validJSString = false; }
	\\.						{ /* Skip all escaped chars. */ }
	\\						{ /* Line ending in '\' => continue to next line. */
								if (validJSString) {
									addToken(start,zzStartRead, Token.LITERAL_CHAR);
									addEndToken(INTERNAL_IN_JS_CHAR_VALID);
								}
								else {
									addToken(start,zzStartRead, Token.ERROR_CHAR);
									addEndToken(INTERNAL_IN_JS_CHAR_INVALID);
								}
								return firstToken;
							}
	\'						{ int type = validJSString ? Token.LITERAL_CHAR : Token.ERROR_CHAR; addToken(start,zzStartRead, type); yybegin(YYINITIAL); }
	\n |
	<<EOF>>					{ addToken(start,zzStartRead-1, Token.ERROR_CHAR); addNullToken(); return firstToken; }
}

<JS_TEMPLATE_LITERAL> {
	[^\n\\\$\`]+				{}
	\\x{HexDigit}{2}		{}
	\\x						{ /* Invalid latin-1 character \xXX */ validJSString = false; }
	\\u{HexDigit}{4}		{}
	\\u						{ /* Invalid Unicode character \\uXXXX */ validJSString = false; }
	\\.						{ /* Skip all escaped chars. */ }

	{JS_TemplateLiteralExprStart}	{
								addToken(start, zzStartRead - 1, Token.LITERAL_BACKQUOTE);
								start = zzMarkedPos-2;
								if (varDepths==null) {
									varDepths = new Stack<>();
								}
								else {
									varDepths.clear();
								}
								varDepths.push(Boolean.TRUE);
								yybegin(JS_TEMPLATE_LITERAL_EXPR);
							}
	"$"						{ /* Skip valid '$' that is not part of template literal expression start */ }
	
	\`						{ int type = validJSString ? Token.LITERAL_BACKQUOTE : Token.ERROR_STRING_DOUBLE; addToken(start,zzStartRead, type); yybegin(YYINITIAL); }

	/* Line ending in '\' => continue to next line, though not necessary in template strings. */
	\\ |
	\n |
	<<EOF>>					{
								if (validJSString) {
									addToken(start, zzStartRead - 1, Token.LITERAL_BACKQUOTE);
									addEndToken(INTERNAL_IN_JS_TEMPLATE_LITERAL_VALID);
								}
								else {
									addToken(start,zzStartRead - 1, Token.ERROR_STRING_DOUBLE);
									addEndToken(INTERNAL_IN_JS_TEMPLATE_LITERAL_INVALID);
								}
								return firstToken;
							}
}

<JS_TEMPLATE_LITERAL_EXPR> {
	[^\}\$\n]+			{}
	"}"					{
							if (!varDepths.empty()) {
								varDepths.pop();
								if (varDepths.empty()) {
									addToken(start,zzStartRead, Token.VARIABLE);
									start = zzMarkedPos;
									yybegin(JS_TEMPLATE_LITERAL);
								}
							}
						}
	{JS_TemplateLiteralExprStart} { varDepths.push(Boolean.TRUE); }
	"$"					{}
	\n |
	<<EOF>>				{
							// TODO: This isn't right.  The expression and its depth should continue to the next line.
							addToken(start,zzStartRead-1, Token.VARIABLE); addEndToken(INTERNAL_IN_JS_TEMPLATE_LITERAL_INVALID); return firstToken;
						}
}

<JS_MLC> {
	// JavaScript MLC's.  This state is essentially Java's MLC state.
	[^hwf\n\*]+			{}
	{URL}					{ int temp=zzStartRead; addToken(start,zzStartRead-1, Token.COMMENT_MULTILINE); addHyperlinkToken(temp,zzMarkedPos-1, Token.COMMENT_MULTILINE); start = zzMarkedPos; }
	[hwf]					{}
	{JS_MLCEnd}				{ yybegin(YYINITIAL); addToken(start,zzStartRead+1, Token.COMMENT_MULTILINE); }
	\*						{}
	\n |
	<<EOF>>					{ addToken(start,zzStartRead-1, Token.COMMENT_MULTILINE); addEndToken(INTERNAL_IN_JS_MLC); return firstToken; }
}

<JS_DOCCOMMENT> {
	[^hwf\@\{\n\<\*]+			{}
	{URL}						{ int temp=zzStartRead; addToken(start,zzStartRead-1, Token.COMMENT_DOCUMENTATION); addHyperlinkToken(temp,zzMarkedPos-1, Token.COMMENT_DOCUMENTATION); start = zzMarkedPos; }
	[hwf]						{}

	"@"{JS_BlockTag}			{ int temp=zzStartRead; addToken(start,zzStartRead-1, Token.COMMENT_DOCUMENTATION); addToken(temp,zzMarkedPos-1, Token.COMMENT_KEYWORD); start = zzMarkedPos; }
	"@"							{}
	"{@"{JS_InlineTag}[^\}]*"}"	{ int temp=zzStartRead; addToken(start,zzStartRead-1, Token.COMMENT_DOCUMENTATION); addToken(temp,zzMarkedPos-1, Token.COMMENT_KEYWORD); start = zzMarkedPos; }
	"{"							{}
	\n							{ addToken(start,zzStartRead-1, Token.COMMENT_DOCUMENTATION); addEndToken(INTERNAL_IN_JS_COMMENT_DOCUMENTATION); return firstToken; }
	"<"[/]?({Letter}[^\>]*)?">"	{ int temp=zzStartRead; addToken(start,zzStartRead-1, Token.COMMENT_DOCUMENTATION); addToken(temp,zzMarkedPos-1, Token.COMMENT_MARKUP); start = zzMarkedPos; }
	\<							{}
	{JS_MLCEnd}					{ yybegin(YYINITIAL); addToken(start,zzStartRead+1, Token.COMMENT_DOCUMENTATION); }
	\*							{}
	<<EOF>>						{ yybegin(YYINITIAL); addToken(start,zzEndRead, Token.COMMENT_DOCUMENTATION); addEndToken(INTERNAL_IN_JS_COMMENT_DOCUMENTATION); return firstToken; }
}

<JS_EOL_COMMENT> {
	[^hwf\n]+				{}
	{URL}					{ int temp=zzStartRead; addToken(start,zzStartRead-1, Token.COMMENT_EOL); addHyperlinkToken(temp,zzMarkedPos-1, Token.COMMENT_EOL); start = zzMarkedPos; }
	[hwf]					{}
	\n |
	<<EOF>>					{ addToken(start,zzStartRead-1, Token.COMMENT_EOL); addNullToken(); return firstToken; }
}
