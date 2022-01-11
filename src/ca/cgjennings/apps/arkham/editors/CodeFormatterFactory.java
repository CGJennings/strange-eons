package ca.cgjennings.apps.arkham.editors;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.TextEncoding;
import ca.cgjennings.apps.arkham.plugins.engine.SEScriptEngine;
import ca.cgjennings.apps.arkham.plugins.engine.SEScriptEngineFactory;
import ca.cgjennings.text.LineWrapper;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

/**
 * Support editors are used to edit content other than game components.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class CodeFormatterFactory {

    public static interface Formatter {

        /**
         * Format and return the given source code string.
         *
         * @param code the source code to format
         * @return the formatted code
         */
        String format(String code);
    }

    private CodeFormatterFactory() {
    }

    /**
     * Returns a formatter implementation for the specified code type, or null
     * if none is available.
     *
     * @param type the code type to format
     */
    public static Formatter getFormatter(CodeEditor.CodeType type) {
        Formatter f;
        switch (type) {
            case PLAIN:
            case PLAIN_UTF8:
                if (text == null) {
                    text = new TextFormatter();
                }
                f = text;
                break;
            case AUTOMATION_SCRIPT:
            case JAVASCRIPT:
            case TYPESCRIPT:
                if (js == null) {
                    js = new JSFormatter();
                }
                f = js;
                break;
            case CSS:
                if (css == null) {
                    css = new CSSFormatter();
                }
                f = css;
                break;
            case HTML:
                if (html == null) {
                    html = new HTMLFormatter();
                }
                f = html;
                break;
            default:
                f = null;
        }
        return f;
    }

    private static Formatter text;
    private static Formatter js;
    private static Formatter css;
    private static Formatter html;

    private static class TextFormatter implements Formatter {

        private final LineWrapper wrapper;

        TextFormatter() {
            wrapper = new LineWrapper();
        }

        public String format(String code) {
            String[] lines = code.split("\n");
            StringBuilder b = new StringBuilder(code.length() * 11 / 10);
            for (String li : lines) {
                b.append(wrapper.wrap(li)).append('\n');
            }
            return b.toString();
        }
    }

    private static class ScriptedFormatter implements Formatter {

        private String functionName;
        private String sourceFile;
        private SEScriptEngine engine;

        ScriptedFormatter(String sourceFile, String function) {
            this.functionName = function;
            this.sourceFile = sourceFile;
        }

        public String format(String code) {
            try {
                if (engine == null) {
                    InputStream in = getClass().getResourceAsStream(sourceFile);
                    engine = SEScriptEngineFactory.getDefaultScriptEngine();
                    engine.eval(new InputStreamReader(in, TextEncoding.SOURCE_CODE));
                    in.close();
                }
                return (String) engine.invokeFunction(functionName, code);
            } catch (Throwable ex) {
                StrangeEons.log.log(Level.SEVERE, "formatter failed", ex);
            }
            return code;
        }
    }

    private static class JSFormatter extends ScriptedFormatter {

        JSFormatter() {
            super("beautify-js.min.js", "js_beautify");
        }
    }

    private static class CSSFormatter extends ScriptedFormatter {

        CSSFormatter() {
            super("beautify-css.min.js", "css_beautify");
        }
    }

    private static class HTMLFormatter extends ScriptedFormatter {

        HTMLFormatter() {
            super("beautify-html.min.js", "html_beautify");
        }
    }
}
