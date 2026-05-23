package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.text.LineWrapper;
import java.awt.Point;
import java.util.List;
import java.util.logging.Level;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Segment;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProviderBase;
import org.fife.ui.autocomplete.ParameterizedCompletion;

/**
 * Code support for plain text.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class PlainTextSupport extends DefaultCodeSupport {

    private AutoCompletion ac;

    @Override
    public void install(CodeEditorBase editor) {
        ac = new AutoCompletion(new WordCompletionProvider());
        ac.install(editor.getTextArea());
    }

    @Override
    public void uninstall(CodeEditorBase editor) {
        ac.uninstall();
        ac = null;
    }

    @Override
    public Formatter createFormatter() {
        return new TextFormatter();
    }

// TODO impl spelling parser/spelling completion
//        if (t != null) {
//            EnumSet<TokenType> toSpellCheck = t.getNaturalLanguageTokenTypes();
//            if (toSpellCheck != null && !toSpellCheck.isEmpty()) {
//                ed.addHighlighter(new SpellingHighlighter(toSpellCheck));
//                SpellingHighlighter.ENABLE_SPELLING_HIGHLIGHT = Settings.getUser().getBoolean("spelling-code-enabled");
//            }
//        }
    
    private static class TextFormatter implements Formatter {

        private final LineWrapper wrapper;

        TextFormatter() {
            wrapper = new LineWrapper();
        }

        @Override
        public String format(String code) {
            String[] lines = code.split("\n");
            StringBuilder b = new StringBuilder(code.length() * 11 / 10);
            for (String li : lines) {
                b.append(wrapper.wrap(li)).append('\n');
            }
            return b.toString();
        }
    }

    static class WordCompletionProvider extends CompletionProviderBase {

        @Override
        protected List<Completion> getCompletionsImpl(JTextComponent jtc) {
            String prefix = getAlreadyEnteredText(jtc);
            return CompletionUtils.getWordCompletions(this, prefix, null, true);
        }

        @Override
        public String getAlreadyEnteredText(JTextComponent comp) {
            Document doc = comp.getDocument();

            int dot = comp.getCaretPosition();
            Element root = doc.getDefaultRootElement();
            int index = root.getElementIndex(dot);
            Element elem = root.getElement(index);
            int start = elem.getStartOffset();
            int len = dot - start;
            try {
                doc.getText(start, len, seg);
            } catch (BadLocationException ble) {
                StrangeEons.log.log(Level.SEVERE, "uncaught", ble);
                return "";
            }

            int segEnd = seg.offset + len;
            start = segEnd - 1;
            while (start >= seg.offset && isCompletionPrefixChar(seg.array[start])) {
                start--;
            }
            start++;

            len = segEnd - start;
            return len == 0 ? "" : new String(seg.array, start, len);

        }

        protected boolean isCompletionPrefixChar(char ch) {
            return Character.isLetter(ch) || ch == '-';
        }

        private Segment seg = new Segment();

        @Override
        public List<Completion> getCompletionsAt(JTextComponent jtc, Point point) {
            return null;
        }

        @Override
        public List<ParameterizedCompletion> getParameterizedCompletions(JTextComponent jtc) {
            return null;
        }
    }
}
