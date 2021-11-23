package ca.cgjennings.io;

import ca.cgjennings.apps.arkham.TextEncoding;
import ca.cgjennings.text.LineWrapper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

/**
 * A writer that complements {@link EscapedLineReader} by automatically escaping
 * characters in the lines that it writes. Long lines are wrapped and marked
 * with the line wrap escape (last character in line is backslash).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see EscapedTextCodec
 */
public class EscapedLineWriter extends BufferedWriter {

    private boolean escapeUnicode = true;

    private int getEscapeFlags() {
        if (escapeUnicode) {
            return EscapedTextCodec.WHITESPACE | EscapedTextCodec.BACKSLASH | EscapedTextCodec.UNICODE;
        }
        return EscapedTextCodec.WHITESPACE | EscapedTextCodec.BACKSLASH;
    }
    
    public EscapedLineWriter(File f) throws IOException {
        this(new FileOutputStream(f));
    }

    public EscapedLineWriter(File f, String charset) throws IOException {
        this(new FileOutputStream(f), charset);
    }

    public EscapedLineWriter(OutputStream out) throws IOException {
        super(new OutputStreamWriter(out, TextEncoding.PARSED_RESOURCE_CS));
    }

    public EscapedLineWriter(OutputStream out, String charset) throws IOException {
        super(new OutputStreamWriter(out, charset));
    }

    public EscapedLineWriter(Writer out, int sz) {
        super(out, sz);
    }

    public EscapedLineWriter(Writer out) {
        super(out);
    }

    /**
     * Returns {@code true} if Unicode escapes will be used.
     *
     * @return {@code true} if characters outside of ISO-8859 are escaped
     * @see #setUnicodeEscaped(boolean)
     */
    public boolean isUnicodeEscaped() {
        return escapeUnicode;
    }

    /**
     * Sets whether uxxxx escapes should be used when writing characters outside
     * of the ISO-8859 encoding range. Default is {@code true}; can be set
     * to {@code false} if the output file can represent all unicode
     * characters directly (e.g., UTF-8).
     *
     * @param escape if {@code true}, Unicode escapes will be used
     * @see #isUnicodeEscaped()
     */
    public void setUnicodeEscaped(boolean escape) {
        this.escapeUnicode = escape;
    }

    /**
     * Writes a line to the output stream, escaping appropriate characters and
     * wrapping long lines by inserting line breaks prefixed by backslashes. The
     * line should not contain a trailing newline; calls to {@link #writeLine},
     * {@link #writeComment}, and {@link #writeProperty} will automatically
     * insert line breaks.
     *
     * @param str the string to write
     * @throws IOException if an I/O error occurs while writing the comment
     */
    public void writeLine(String str) throws IOException {
        writeWrapped(escapeLine(str));
    }

    /**
     * Writes one or more comment lines. The string will be divided into lines
     * at newline (\n) characters and each line will be prefixed with <tt>#
     * </tt>
     * and written to the file.
     *
     * @param str the string to write as comment text
     * @throws IOException if an I/O error occurs while writing the comment
     */
    public void writeComment(String str) throws IOException {
        if (str.indexOf('\n') != -1) {
            String[] lines = str.split("\n");
            for (int i = 0; i < lines.length; ++i) {
                writeUnwrapped("# " + escape(lines[i]));
            }
        } else {
            writeUnwrapped("# " + escape(str));
        }
    }

    /**
     * Writes a key, value string pair with escaping and line breaking.
     *
     * @param key the name of the key
     * @param value the value of the key
     * @throws IOException if an I/O error occurs
     */
    public void writeProperty(String key, String value) throws IOException {
        writeWrapped(escapeLine(key) + " = " + escape(value));
    }

    /**
     * Writes a collection of properties as key, value pairs with escaping
     * and line breaking.
     *
     * @param props the map of properties to write
     * @throws IOException if an I/O error occurs
     */
    public void writeProperties(Map<String,String> props) throws IOException {
        for (Map.Entry<String,String> kv : props.entrySet()) {
            writeProperty(kv.getKey(), kv.getValue());
        }
    }

    private void writeUnwrapped(String line) throws IOException {
        if (firstLine) {
            firstLine = false;
        } else {
            write('\n');
        }
        super.write(line);
    }

    private void writeWrapped(String line) throws IOException {
        if (firstLine) {
            firstLine = false;
        } else {
            write('\n');
        }
        super.write(wrapper.wrap(line));
    }
    private boolean firstLine = true;
    private LineWrapper wrapper = new LineWrapper("\\\n    ", 80, 80, 6);

    private String escapeLine(String str) {
        return escapeLineStart(escape(str));
    }

    private static String escapeLineStart(String line) {
        // should be able to replace with:
        // return EscapedTextCodec.escape( line, getEscapeFlags()|EscapedTextCodec.INITIAL_COMMENT );
        if (!line.isEmpty()) {
            final int len = line.length();
            for (int i = 0; i < len; ++i) {
                final char c = line.charAt(i);
                if (Character.isSpaceChar(c)) {
                    continue;
                }
                if (c == '#' || c == '!') {
                    return line.substring(0, i) + '\\' + line.substring(i);
                }
                break;
            }
        }
        return line;
    }

    private String escape(String line) {
        return EscapedTextCodec.escape(line, getEscapeFlags());
    }
}
