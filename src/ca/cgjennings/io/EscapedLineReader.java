package ca.cgjennings.io;

import ca.cgjennings.apps.arkham.TextEncoding;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;

/**
 * A buffered input stream reader that transparently converts escaped characters
 * into their original form. The escape sequences it recognizes are based on
 * those used in {@code .properties} files. All escape sequences begin with a
 * backslash ({@code \}). This can be followed by {@code r} (carriage return),
 * {@code n} (newline), {@code s} (space), {@code \} (a backslash), {@code #}
 * (hash), {@code !} (bang), or {@code uxxxx} (to encode any Unicode
 * character; {@code xxxx} are the 4 hex digits of a UTF-16 character).
 *
 * <p>
 * Lines are trimmed of leading and trailing whitespace before being returned.
 * (Use escape characters to prevent this trimming.) A trimmed line that starts
 * with an unescaped # or ! character will be treated as a comment. The reader
 * will silently skip these lines.
 *
 * <p>
 * If a line ends with a backslash, the following line will automatically be
 * merged with this line. For example, the following would be read in as a
 * single line:
 * <pre>
 * Pease porridge hot, pease porridge cold, \
 * Pease porridge in the pot, nine days old; \
 * Some like it hot, some like it cold, \
 * Some like it in the pot, nine days old.
 * </pre>
 *
 * <p>
 * The reader tracks the {@linkplain #getLineNumber() line number} of each line
 * as it is read in, for use in reporting errors. Line numbering starts at 1.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class EscapedLineReader extends LineNumberReader {

    /**
     * Creates a line reader using the specified reader and a default buffer
     * size.
     *
     * @param in a reader that will provide the raw character stream
     */
    public EscapedLineReader(Reader in) {
        super(in);
        setLineNumber(1);
    }

    /**
     * Creates a line reader using the specified reader and buffer size.
     *
     * @param in a reader that will provide the raw character stream
     * @param bufferSize the size of the input buffer
     */
    public EscapedLineReader(Reader in, int bufferSize) {
        super(in, bufferSize);
        setLineNumber(1);
    }

    /**
     * Creates a line reader for the specified input stream using a default
     * buffer size. The encoding is assumed to be ISO-8859-15.
     *
     * @param in the input stream to read from
     */
    public EscapedLineReader(InputStream in) {
        super(new InputStreamReader(in, TextEncoding.PARSED_RESOURCE_CS));
        setLineNumber(1);
    }

    /**
     * Creates a line reader for the specified input stream using a default
     * buffer size and the specified character set encoding.
     *
     * @param in the input stream to read from
     * @param charset the name of the character set to use, such as "UTF-8"
     * @throws java.io.UnsupportedEncodingException
     */
    public EscapedLineReader(InputStream in, String charset) throws UnsupportedEncodingException {
        super(new InputStreamReader(in, charset));
        setLineNumber(1);
    }

    /**
     * Creates a line reader for the specified input stream using a default
     * buffer size and the specified character set encoding.
     *
     * @param in the input stream to read from
     * @param charset the character set to use
     */
    public EscapedLineReader(InputStream in, Charset charset) {
        super(new InputStreamReader(in, charset));
        setLineNumber(1);
    }

    /**
     * Creates a line reader that reads from the specified URL, using a default
     * buffer size and the ISO-8859-15 encoding.
     *
     * @param url the URL to read from
     * @throws java.io.IOException
     */
    public EscapedLineReader(URL url) throws IOException {
        super(new InputStreamReader(url.openStream(), TextEncoding.PARSED_RESOURCE_CS));
        setLineNumber(1);
    }

    /**
     * Creates a line reader for the specified URL using a default buffer size
     * and the specified character set encoding.
     *
     * @param url the URL to read from
     * @param charset the name of the character set to use, such as "UTF-8"
     * @throws java.io.IOException
     */
    public EscapedLineReader(URL url, String charset) throws IOException {
        super(new InputStreamReader(url.openStream(), charset));
        setLineNumber(1);
    }

    /**
     * Creates a line reader that reads from the specified file, using a default
     * buffer size and the ISO-8859-15 encoding.
     *
     * @param f the file to read from
     * @throws java.io.IOException
     */
    public EscapedLineReader(File f) throws IOException {
        this(f, TextEncoding.PARSED_RESOURCE);
    }

    /**
     * Creates a line reader that reads from the specified file, using a default
     * buffer size and the specified encoding.
     *
     * @param f the file to read from
     * @param charset the name of the character set to use, such as "UTF-8"
     * @throws java.io.IOException
     */
    public EscapedLineReader(File f, String charset) throws IOException {
        this(f.toURI().toURL(), charset);
    }

    /**
     * Returns the next logical line from the stream, or null if the end of the
     * stream has been reached.The next logical line may consist of several
     * "natural" lines in the original file.Natural lines are concatenated into
     * a single logical line when a backslash character occurs immediately
     * before the line separator between them. For example:
     *
     * <pre>
     * These two natural lines will \
     * form a single logical line.
     * </pre>
     *
     * <p>
     * A backslash is also used to introduce escape sequences. The sequences \n,
     * \r, \t, and \f are replaced by a newline, return, tab, or formfeed
     * character, respectively. The sequence \u0000 (where 0000 is a sequence of
     * four hexadecimal digits) is replaced by the 16-bit Unicode character with
     * the specified value. A backslash followed by any other character
     * (including another backslash) is replaced by the following character,
     * effectively deleting the initial backslash.
     * <p>
     * Lines whose first non-whitespace character is '#' or '!' are considered
     * comments and will be skipped. If a line continuation backslash occurs
     * before a comment line, the first non-comment line that follows will be
     * concatenated to the continued line. If a line continuation backslash
     * occurs on a comment line, it is ignored. To include a comment character
     * as the first character of a non-comment line, escape it with a backslash.
     *
     * @return the next converted line
     * @throws java.io.IOException
     */
    @Override
    public String readLine() throws IOException {
        return unescapeLine(readLineUnescaped());
    }

    private String readLineUnescaped() throws IOException {
        String line = fetchNextNoncommentLine();
        if (line == null) {
            return null;
        }

        boolean hasContinuation = EscapedTextCodec.isWrappedLine(line);
        if (hasContinuation) {
            StringBuilder b = new StringBuilder(line.length() + 80);
            b.append(line, 0, line.length() - 1);
            do {
                String nextLine = fetchNextNoncommentLine();
                if (nextLine == null) {
                    hasContinuation = false;
                } else {
                    hasContinuation = EscapedTextCodec.isWrappedLine(nextLine);
                    b.append(nextLine, 0, nextLine.length() - (hasContinuation ? 1 : 0));
                }
            } while (hasContinuation);
            line = b.toString();
        }
        return line;
    }

    /**
     * Reads a [key,value] pair, skipping empty lines. This is a cover for
     * {@code readProperty( true )}.
     *
     * @return a key, value pair or {@code null}
     * @throws IOException
     */
    public String[] readProperty() throws IOException {
        return readProperty(true, null);
    }

    /**
     * Reads a [key,value] pair. This is done by reading a line as with
     * {@link #readLine} and then splitting the line into a key and value.
     * Splitting is performed by locating the first unescaped equals sign; text
     * prior to this point becomes the key while text after this point becomes
     * the value. The key and value are both trimmed before being returned. If
     * the line does not have an unescaped equals sign, then the entire line is
     * treated as the key and the value will be an empty string.
     *
     * @param skipEmptyLines if {@code true}, any empty lines will be
     * silently skipped
     * @return a key, value pair or {@code null}
     * @throws IOException
     */
    public String[] readProperty(boolean skipEmptyLines) throws IOException {
        return readProperty(skipEmptyLines, null);
    }
    
    private String[] readProperty(boolean skipEmptyLines, String[] entry) throws IOException {
        String line = skipEmptyLines ? readNonemptyLineUnescaped() : readLineUnescaped();
        if (line == null) {
            return null;
        }

        int div = 0;
        for (;;) {
            div = line.indexOf('=', div);
            // special case: there is no '='
            if (div < 0) {
                return new String[]{line.trim(), ""};
            }

            if (div == 0 || line.charAt(div - 1) != '\\') {
                break;
            }
            ++div;
        }
        final String key = unescapeLine(line.substring(0, div).trim()).trim();
        final String value = unescapeLine(line.substring(div + 1)).trim();

        if (entry == null) {
            entry = new String[]{key, value}; 
        } else {
            entry[0] = key;
            entry[1] = value;
        }
        
        return entry;
    }

    /**
     * Reads all remaining lines as a series of [key, value] pairs and stores
     * them in the specified map.
     *
     * @param props the non-null map to add the read properties to
     * @returns the map that was passed in
     * @throws IOException if an I/O error occurs
     */
    public Map<String,String> readProperties(Map<String,String> props) throws IOException {
        Objects.requireNonNull(props, "props");
        String[] kv = readProperty(true, null);
        while (kv != null) {
            props.put(kv[0], kv[1]);
            kv = readProperty(true, kv);
        }
        return props;
    }

    /**
     * Return the next line which is not a comment and which contains
     * non-whitespace characters, or {@code null} if the end of the stream
     * is reached.
     *
     * @return the next non-comment, non-empty line, or null
     * @throws java.io.IOException
     */
    public String readNonemptyLine() throws IOException {
        return unescapeLine(readNonemptyLineUnescaped());
    }

    private String readNonemptyLineUnescaped() throws IOException {
        String line;
        do {
            line = readLineUnescaped();
        } while (line != null && line.length() == 0);
        return line;
    }

    private String fetchNextNoncommentLine() throws IOException {
        String line;
        boolean isCommentLine;
        boolean isReadingBlock = firstCommentBlock == null;
        StringBuilder commentBlock = null;
        do {
            isCommentLine = false;
            line = super.readLine();
            if (line != null) {
                line = line.trim();
                if (line.length() >= 1) {
                    char startChar = line.charAt(0);
                    if (startChar == '#' || startChar == '!') {
                        isCommentLine = true;
                        // if this is a comment line AND we don't have a comment
                        // block yet, add this line to the comment block
                        if (isReadingBlock) {
                            if (commentBlock == null) {
                                commentBlock = new StringBuilder(256);
                            } else {
                                commentBlock.append('\n');
                            }
                            if (line.length() >= 2 && line.charAt(1) == ' ') {
                                commentBlock.append(line.substring(2));
                            } else {
                                commentBlock.append(line.substring(1));
                            }
                        }
                    }
                }
            }
        } while (isCommentLine);
        if (commentBlock != null) {
            firstCommentBlock = commentBlock.toString();
        }
        return line;
    }

    private String firstCommentBlock;

    /**
     * If the reader has read past the first contiguous block of comment lines
     * in the stream, this method returns that comment block. Otherwise, it
     * returns {@code null}.
     *
     * @return the first comment block in the file, if it has been read
     */
    public String getFirstCommentBlock() {
        return firstCommentBlock;
    }

    /**
     * Remove escape sequences from a line, if any. Returns null if the given
     * line is null. For a list of sequences and their effects, see
     * {@link #readLineUnescaped()}.
     *
     * @param line the line to unescape
     * @return the unescaped line
     */
    public static String unescapeLine(String line) {
        if (line == null) {
            return null;
        }

        // assuming that no escapes is the common case, save time by not
        // allocating a buffer and processing the line
        if (line.indexOf('\\') == -1) {
            return line;
        }

        char[] buff = new char[line.length()];
        int bi = 0; // index into output buffer

        final int TEXT = 0, ESCAPE = 1;
        int state = TEXT;
        for (int i = 0; i < line.length(); ++i) {
            char c = line.charAt(i);
            switch (state) {
                case TEXT:
                    if (c == '\\') {
                        state = ESCAPE;
                    } else {
                        buff[bi++] = c;
                    }
                    break;
                case ESCAPE:
                    state = TEXT;
                    switch (c) {
                        case 'n':
                            buff[bi++] = '\n';
                            break;
                        case 't':
                            buff[bi++] = '\t';
                            break;
                        case 'r':
                            buff[bi++] = '\r';
                            break;
                        case 'f':
                            buff[bi++] = '\f';
                            break;
                        case 's':
                        case ' ':
                            buff[bi++] = ' ';
                            break;
                        case ':':
                        case '=':
                        case '#':
                        case '!':
                            buff[i++] = c;
                            break;
                        case 'u':
                            ++i; // skip the 'u'

                            int value = 0;
                            for (int j = 0; j < 4 && i < line.length(); ++i, ++j) {
                                char digit = line.charAt(i);
                                switch (digit) {
                                    case '0':
                                    case '1':
                                    case '2':
                                    case '3':
                                    case '4':
                                    case '5':
                                    case '6':
                                    case '7':
                                    case '8':
                                    case '9':
                                        value = (value << 4) + (digit - '0');
                                        break;
                                    case 'a':
                                    case 'b':
                                    case 'c':
                                    case 'd':
                                    case 'e':
                                    case 'f':
                                        value = (value << 4) + (10 + digit - 'a');
                                        break;
                                    case 'A':
                                    case 'B':
                                    case 'C':
                                    case 'D':
                                    case 'E':
                                    case 'F':
                                        value = (value << 4) + (10 + digit - 'A');
                                        break;
                                    default:
                                        throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
                                }
                            }
                            --i; // unskip whatever letter follows the escape

                            buff[bi++] = (char) value;
                            break;
                        default:
                            // this is not an escape we recognize; insert the backslash and the
                            // escape character unmodified, assuming that it is an escape intended
                            // for higher-level processing
                            buff[bi++] = '\\';
                            buff[bi++] = c;
                            break;
                    }
                    break;
            }
        }
        return new String(buff, 0, bi);
    }
}
