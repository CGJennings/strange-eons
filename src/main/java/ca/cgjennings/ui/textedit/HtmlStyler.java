package ca.cgjennings.ui.textedit;

import ca.cgjennings.ui.theme.ThemeInstaller;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.swing.text.BadLocationException;
import org.fife.ui.rsyntaxtextarea.HtmlUtil;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.Theme;

/**
 * Converts plain text into syntax highlighted HTML.
 *
 * @author Chris Jennings (<https://cgjennings.ca/contact>)
 * @since 3.4, replaces {@code HTMLStyler} from 3.3 with a different API
 */
public class HtmlStyler implements Iterable<String> {

    private final SyntaxTextArea ta;

    /**
     * Creates a new styler for the specified code type. It will initially
     * contain the empty string.
     *
     * @param type the type of code to highlight
     */
    public HtmlStyler(CodeType type) {
        this(type, true, false, false);
    }

    /**
     * Creates a new styler for the specified code type. It will initially
     * contain the empty string.
     *
     * @param type the type of code to highlight
     * @param noBackgroundColor if true, the background color is not styled
     * @param noFontStyles if true, font style changes are ignored
     * @param matchTheme if true, the syntax styling will match the current theme
     */    
    public HtmlStyler(CodeType type, boolean noBackgroundColor, boolean noFontStyles, boolean matchTheme) {
        ta = new SyntaxTextArea();
        // create a simplfied theme
        String themeName = "light.xml";
        if (matchTheme && ThemeInstaller.isDark()) {
            themeName = "dark.xml";
        }

        try (InputStream in = SyntaxTextArea.class.getResourceAsStream(themeName)) {
            Theme th;
            if (noFontStyles) {
                th = Theme.load(in, new Font(Font.MONOSPACED, Font.PLAIN, 12));
            } else {
                th = Theme.load(in);
            }
            if (noBackgroundColor || noFontStyles) {
                if (noBackgroundColor) {
                    th.bgColor = null;
                }

                for (int i = 0; i < th.scheme.getStyleCount(); ++i) {
                    Style s = th.scheme.getStyle(i);
                    if (noBackgroundColor) {
                        s.background = null;
                    }
                    if (noFontStyles) {
                        s.font = null;
                    }
                }
            }
            th.apply(ta);
        } catch (IOException ioe) {
            // shouldn't happen, but fall back to default
        }
        ta.setSyntaxEditingStyle(DefaultCodeSupport.languageIdFor(type));
    }

    /**
     * Creates a styler that reflects the contents of the specified editor.
     *
     * @param editor the editor to reflect
     */
    HtmlStyler(CodeEditorBase editor) {
        ta = editor.getTextArea();
    }

    /**
     * Sets the text to be styled.
     *
     * @param text the code to highlight
     */
    public void setText(String text) {
        ta.setText(text);
        ta.discardAllEdits();
    }

    /**
     * Returns the current text.
     *
     * @return the non-null text
     */
    public String getText() {
        return ta.getText();
    }

    /**
     * Returns the entire text as highlighted HTML.
     *
     * @return an HTML representation of the document
     */
    public String styleAll() {
        return style(0, getLength());
    }

    /**
     * Returns highlighted HTML for the specified character range.
     *
     * @param start the start offset
     * @param end the end offset
     * @return an HTML representation of the document range
     */
    public String style(int start, int end) {
        return HtmlUtil.getTextAsHtml(ta, start, end);
    }

    /**
     * Returns highlighted HTML for the specified line.
     *
     * @param line the zero-based line number
     * @return an HTML representation of the document line
     */
    public String styleLine(int line) {
        try {
            int start = ta.getLineStartOffset(line);
            int end = ta.getLineEndOffset(line);
            return style(start, end);
        } catch (BadLocationException ble) {
            throw new IllegalArgumentException("line " + line);
        }
    }

    /**
     * Returns the length of the text.
     *
     * return the text length in characters
     */
    public int getLength() {
        return ta.getDocument().getLength();
    }

    /**
     * Returns the number of lines in the document.
     *
     * @return the text length in lines
     */
    public int getLineCount() {
        return ta.getLineCount();
    }

    /**
     * Returns an iterator that returns each line in sequence as styled HTML. If
     * the text is modified before the iterator completes, the result is
     * undefined.
     *
     * @return an iterator over styled document lines
     */
    @Override
    public Iterator<String> iterator() {
        return new Iterator<>() {
            int line = 0;
            final int count = getLineCount();

            @Override
            public boolean hasNext() {
                return line < count;
            }

            @Override
            public String next() {
                if (line >= count) {
                    throw new NoSuchElementException();
                }
                return styleLine(line++);
            }
        };
    }
}
