package ca.cgjennings.ui.textedit;

import ca.cgjennings.text.LineWrapper;

/**
 * Code support for the plain text.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class PlainTextSupport extends DefaultCodeSupport {
    @Override
    public Formatter createFormatter() {
        return new TextFormatter();
    }
    
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
}
