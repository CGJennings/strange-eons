/*
 * ResourceFileTokenMaker.java - Scanner for SE parsed resource files.
 * 
 * Adapted from the RSyntaxTextArea properties file tokenizer. See about
 * dialog tempate for license information.
 */
package ca.cgjennings.ui.textedit;

import java.io.*;
import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.*;


/**
 * This class splits up text into tokens representing a SE resource file, which is similar
 * to a properties file but does not accept ':' as a separator between a key and a value.
 *
 * <p>
 * <strong>
 * This file is generated automatically.
 * See {@code build-tools/README.md} for details.
 * </strong>
 */
%%

%public
%class ResourceFileTokenMaker
%extends AbstractJFlexTokenMaker
%unicode
%type org.fife.ui.rsyntaxtextarea.Token


%{


	/**
	 * Constructor.  This must be here because JFlex does not generate a
	 * no-parameter constructor.
	 */
	public ResourceFileTokenMaker() {
		super();
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
	 * {@inheritDoc}
	 */
	@Override
	public String[] getLineCommentStartAndEnd(int languageIndex) {
		return new String[] { "#", null };
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
	public Token getTokenList(Segment text, int initialTokenType, int startOffset) {

		resetTokenList();
		this.offsetShift = -text.offset + startOffset;

		// Start off in the proper state.
		int state = Token.NULL;
		switch (initialTokenType) {
			case Token.LITERAL_STRING_DOUBLE_QUOTE:
				state = VALUE;
				start = text.offset;
				break;
			default:
				state = Token.NULL;
		}

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
	 * @exception   IOException  if any I/O-Error occurs.
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

Equals				= ([=])
Name					= ([^= \t\n#!]*)
Whitespace			= ([ \t]+)
Comment				= ([#!].*)
SingleQuote			= (')

%state VALUE

%%

<YYINITIAL> {
	{Name}			{ addToken(Token.RESERVED_WORD); }
	{Equals}			{ start = zzMarkedPos; addToken(Token.OPERATOR); yybegin(VALUE); }
	{Whitespace}		{ addToken(Token.WHITESPACE); }
	{Comment}			{ addToken(Token.COMMENT_EOL); }
	<<EOF>>			{ addNullToken(); return firstToken; }
}

<VALUE> {
	{SingleQuote}[^']*{SingleQuote}?	{ addToken(start, zzMarkedPos-1, Token.LITERAL_STRING_DOUBLE_QUOTE); start = zzMarkedPos; }
	[^'\{\\]+						{}
	"{"[^\}]*"}"?					{ int temp=zzStartRead; addToken(start, zzStartRead-1, Token.LITERAL_STRING_DOUBLE_QUOTE); addToken(temp, zzMarkedPos-1, Token.VARIABLE); start = zzMarkedPos; }
	[\\].							{}
	[\\]							{ addToken(start, zzEndRead, Token.LITERAL_STRING_DOUBLE_QUOTE); return firstToken; }
	<<EOF>>							{ addToken(start,zzStartRead-1, Token.LITERAL_STRING_DOUBLE_QUOTE); addNullToken(); return firstToken; }
}