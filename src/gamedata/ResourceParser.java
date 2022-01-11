package gamedata;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.io.EscapedLineReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import resources.Language;
import resources.ResourceKit;
import resources.Settings;

/**
 * An abstract base class for building parsers that convert descriptive text
 * files (such as tile sets, class maps, or silhouette files) into resources.
 *
 * <p>
 * Resource parsers can be created in <i>gentle</i> mode, in which case they
 * will avoid throwing parsing exceptions. When an error occurs in gentle mode,
 * the parser will log a warning message. Concrete subclasses should either skip
 * that resource or else substitute default values. When not running in gentle
 * mode, the parser should throw a {@link ResourceParserException}.
 *
 * <p>
 * Parsers built with this class create resources by reading lines from a text
 * file. Support is included for lines that use a [key, value] syntax like
 * {@linkplain Settings settings files}, but any file format can be used. The
 * supplied line-reading methods automatically skip comment lines, and
 * concatenate lines that end in a backslash. For more details, see the
 * description of {@link EscapedLineReader}; this class conforms exactly to the
 * behaviour of that class when reading lines.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see StrangeEons#log
 */
public abstract class ResourceParser<R> implements Closeable {

    private boolean gentle;
    private Language lang = Language.getInterface();
    private EscapedLineReader r;
    private String resource;
    private boolean close;

    /**
     * Creates a parser for the specified resource file.
     *
     * @param resource the location of the desired tile set resource
     * @param gentle if {@code true}, parses in gentle mode
     */
    public ResourceParser(String resource, boolean gentle) throws IOException {
        this(resource, null, gentle);
    }

    /**
     * Creates a parser for the specified resource file and encoding.
     *
     * @param resource the location of the desired tile set resource
     * @param charset the name of the character set to use, such as "UTF-8"
     * @param gentle if {@code true}, parses in gentle mode
     */
    public ResourceParser(String resource, String charset, boolean gentle) throws IOException {
        if (resource == null) {
            throw new NullPointerException("resource");
        }
        URL url = ResourceKit.composeResourceURL(resource);
        if (url == null) {
            if (gentle) {
                StrangeEons.log.log(Level.WARNING, "missing resource: {0}", resource);
                r = null;
            } else {
                throw new FileNotFoundException(resource);
            }
        }

        this.resource = resource;
        this.gentle = gentle;
        close = true;
        r = charset == null ? new EscapedLineReader(url) : new EscapedLineReader(url, charset);
    }

    /**
     * Creates a parser for the specified input stream.
     *
     * @param in the input stream to read from
     * @param gentle if {@code true}, parses in gentle mode
     * @throws IOException if an I/O error occurs
     */
    public ResourceParser(InputStream in, boolean gentle) throws IOException {
        this(in, null, gentle);
    }

    /**
     * Creates a parser for the specified input stream and encoding.
     *
     * @param in the input stream to read from
     * @param charset the name of the character set to use, such as "UTF-8"
     * @param gentle if {@code true}, parses in gentle mode
     * @throws IOException if an I/O error occurs
     */
    public ResourceParser(InputStream in, String charset, boolean gentle) throws IOException {
        r = charset == null ? new EscapedLineReader(in) : new EscapedLineReader(in, charset);
        this.gentle = gentle;
    }

    /**
     * Returns the next resource listed in the file, or {@code null} if there
     * are no more resources available.
     *
     * @return the next resource
     * @throws IOException if an I/O error occurs
     * @throws ResourceParserException if a parsing exception occurs and the
     * parser is not in gentle mode, or if the parser cannot recover from the
     * error even in gentle mode
     */
    public abstract R next() throws IOException;

    /**
     * Sets the language that the parser will use to look up any localizable
     * values in the resource file.
     *
     * @param language the language to use; must not be {@code null}
     */
    public final void setLanguage(Language language) {
        if (language == null) {
            throw new NullPointerException("language");
        }
        lang = language;
    }

    /**
     * Returns the language that the parser will use to look up localizable
     * values in the resource file. The default is
     * {@link Language#getInterface()}.
     *
     * @return the interface language used to look up tile names
     */
    public final Language getLanguage() {
        return lang;
    }

    /**
     * Returns {@code true} if the parser was created in gentle mode.
     *
     * @return {@code true} if bad entries will be skipped where possible
     */
    public final boolean isParsingGently() {
        return gentle;
    }

    /**
     * Returns an identifier that can be used in error messages. If the parser
     * was created with the resource-based constructor, this will default to the
     * resource name. Otherwise it will default to {@code null}, but can be
     * changed with {@link #setIdentifier}. If the identifier is {@code null},
     * then this will return {@code &lt;???&gt;}.
     *
     * @return the identifier, or the "unknown identifier" if the identifier is
     * {@code null}
     */
    public String getIdentifierString() {
        return resource == null ? "<???>" : resource;
    }

    /**
     * Returns the identifier. If {@code null}, then {@code null} is returned.
     * Otherwise, this is exactly the same as {@link #getIdentifierString()}.
     *
     * @return the identifier, or {@code null}
     */
    public String getIdentifier() {
        return resource;
    }

    /**
     * Sets the identifier to use to identify this resource in error messages.
     *
     * @param id the identifier value to use to identify the source
     * @see #getIdentifierString()
     */
    public void setIdentifier(String id) {
        resource = id;
    }

    /**
     * Returns an error message that includes the current identifier string and
     * line number.
     *
     * @param message a message to include as part of the returned string
     * @return a string that includes the identifier, line number, and message
     * in a standard format
     */
    protected String errorMessage(String message) {
        return String.format("%s at %s:%d", message, getIdentifierString(), getLineNumber());
    }

    /**
     * A convenience method that may be called by subclasses when an error
     * occurs. If gentle parsing is enabled, it will log the message, resource
     * identifier, and line number. Otherwise, it will throw a
     * {@link ResourceParserException}.
     *
     * @param message the error message to include
     */
    protected void error(String message) {
        if (isParsingGently()) {
            StrangeEons.log.severe(String.format("%s at %s:%d", message, getIdentifierString(), getLineNumber()));
        } else {
            throw new ResourceParserException(resource, message, r);
        }
    }

    /**
     * A convenience method that may be called by subclasses to log a warning
     * message. It is similar to {@link #error}, but never throws an exception.
     * If gentle parsing is enabled, it will log the specified message. If
     * strict parsing is enabled, it does nothing.
     *
     * @param message the warning message to include
     */
    protected void warning(String message) {
        if (!isParsingGently()) {
            StrangeEons.log.warning(String.format("%s at %s:%d", message, getIdentifierString(), getLineNumber()));
        }
    }

    /**
     * Returns the next non-comment line. This is called by subclasses to
     * implement parsing.
     *
     * @return the next non-comment line, or {@code null} if the end of file was
     * reached
     * @throws IOException if an I/O error occurs while reading from the source
     */
    protected final String readLine() throws IOException {
        if (r == null) {
            return null;
        }
        return r.readLine();
    }

    /**
     * Returns the next line that is neither a comment nor empty. An empty line
     * is a line that contains only whitespace. This is called by subclasses to
     * implement parsing.
     *
     * @return the next non-empty line, or {@code null} if the end of file was
     * reached
     * @throws IOException if an I/O error occurs while reading from the source
     */
    protected final String readNonemptyLine() throws IOException {
        if (r == null) {
            return null;
        }
        return r.readNonemptyLine();
    }

    /**
     * Parses and returns the next line as a [key, value] pair. This is a cover
     * for {@code readProperty( true )}.
     *
     * @return the next non-empty line split into a [key, value] pair, or
     * {@code null} if the end of file was reached
     * @throws IOException if an I/O error occurs while reading from the source
     */
    protected final String[] readProperty() throws IOException {
        if (r == null) {
            return null;
        }
        return r.readProperty();
    }

    /**
     * Parses and returns the next line as a [key, value] pair. If there is no
     * value, it will be an empty string. If empty lines are not skipped, and an
     * empty line is read, then both the key and value will be empty strings.
     *
     * @param skipEmptyLines if {@code true}, empty lines are skipped and the
     * next non-empty line is parsed
     * @return the next line split into a [key, value] pair, or {@code null} if
     * the end of file was reached
     * @throws IOException if an I/O error occurs while reading from the source
     */
    protected final String[] readProperty(boolean skipEmptyLines) throws IOException {
        if (r == null) {
            return null;
        }
        return r.readProperty(skipEmptyLines);
    }

    /**
     * Returns the current line number in the file. This is used by parsers to
     * help compose meaningful error messages.
     *
     * @return the current line number (starting from 1)
     */
    protected final int getLineNumber() {
        if (r == null) {
            return 1;
        }
        return r.getLineNumber() - 1;
    }

    /**
     * If this parser was created directly for a resource identifier, then
     * calling this method closes the input stream that was created for the
     * resource. If this parser was created using an input stream supplied by
     * the caller, then this method does nothing.
     *
     * @throws IOException if an I/O error occurs while trying to close the
     * resource
     */
    @Override
    public final void close() throws IOException {
        if (close && r != null) {
            r.close();
        }
    }
}
