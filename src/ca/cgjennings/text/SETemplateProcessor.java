package ca.cgjennings.text;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import resources.Language;

/**
 * An extension of the basic {@link TemplateProcessor} which supports additional
 * symbols for looking up localized Strange Eons {@link Language} strings.
 * Template processors assist in the automated writing of scripts and other text
 * files by filling in a template file using variables and simple boolean
 * conditions.
 * <p>
 * Sections of the template that are to be generated automatically are marked
 * with <i>symbols</i>. A symbol consists of a special token placed between
 * braces. The tokens begin with a specific marker character and are followed by
 * an identifier. The identifier may consist of any characters except spaces or
 * the closing brace. The following codes are recognized:
 * <p>
 * <b>&#123;@key}</b><br>
 * Replaces the symbol with an interface {@link Language} string with the given
 * key.
 * <p>
 * <b>{#key}</b><br>
 * Replaces the symbol with a game {@link Language} string with the given key.
 * <p>
 * <b>{%<i>variable</i>}</b><br>
 * Replaces the symbol with the
 * {@linkplain #set(java.lang.String, java.lang.String) value of the specified variable}.
 * Throws an exception if the variable is undefined.
 * <p>
 * <b>{?<i>condition</i>} ... {/?<i>condition</i>}</b><br>
 * If the condition is set to {@code true}, then the text between the start
 * and end symbols will be included in the document. Otherwise, it will be left
 * out. Throws an exception if the condition has not been set.
 * <p>
 * <b>{!<i>condition</i>} ... {/!<i>condition</i>}</b><br>
 * If the condition is set to {@code false}, then the text between the
 * start and end symbols will be included in the document. Otherwise, it will be
 * left out. Throws an exception if the condition has not been set.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class SETemplateProcessor extends TemplateProcessor {

    private Language ui = Language.getInterface(), game = Language.getGame();

    public SETemplateProcessor(TemplateProcessor source) {
        super(source);
    }

    public SETemplateProcessor(Locale locale) {
        super(locale);
    }

    public SETemplateProcessor() {
    }

    /**
     * Sets the language to be used for @-symbols. (The default value is
     * {@code Language.getInterface()}.)
     *
     * @param ui the UI language to set
     */
    public void setInterfaceLanguage(Language ui) {
        this.ui = ui;
    }

    /**
     * Sets the language to be used for #-symbols. (The default value is
     * {@code Language.getGame()}.)
     *
     * @param game the game language to set
     */
    public void setGameLanguage(Language game) {
        this.game = game;
    }

    @Override
    protected String processSymbol(StringBuilder buffer, String symbol, char code, String name, String remainder) {
        switch (code) {
            case '@':
            case '#':
                // allow parsing of symbols in the replacement
                String v = code == '@' ? ui.get(name) : game.get(name);
                if (v.indexOf('{') >= 0) {
                    remainder = v + remainder; // allow the replacement to be parsed
                } else {
                    buffer.append(v);
                }
                break;
            default:
                remainder = super.processSymbol(buffer, symbol, code, name, remainder);
        }
        return remainder;
    }

    /**
     * Processes a template stored in the application resources. The template
     * file must use the UTF-8 text encoding.
     *
     * @param templateResource the resource to process
     * @return the completed template
     */
    public String processFromResource(String templateResource) {
        try {
            String text = ProjectUtilities.getResourceText(templateResource, ProjectUtilities.ENC_UTF8);
            if (text == null) {
                throw new IllegalArgumentException("no such resource: " + templateResource);
            }
            return process(text);
        } catch (IOException e) {
            StrangeEons.log.log(Level.SEVERE, "exception while reading resource", e);
            throw new AssertionError(e);
        }
    }

    /**
     * Creates a JavaScript string literal from a plain string. The string
     * literal will be surrounded with 'single quotes', and newlines, tabs,
     * backslashes, and single quotes within the string will be escaped.
     *
     * @param s the source string
     * @return the string literal
     */
    public static String escapeScriptString(String s) {
        StringBuilder b = new StringBuilder(s.length() + 8).append('\'');
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            switch (c) {
                case '\'':
                    b.append("\\'");
                    break;
                case '\r':
                    b.append("\\r");
                    break;
                case '\n':
                    b.append("\\n");
                    break;
                case '\t':
                    b.append("\\t");
                    break;
                case '\\':
                    b.append("\\\\");
                    break;
                case '\0':
                    b.append("\\0");
                    break;
                default:
                    b.append(c);
            }
        }
        return b.append('\'').toString();
    }

    /**
     * Creates a Java string literal from a plain string. The string literal
     * will be surrounded with "double quotes", and newlines, tabs, backslashes,
     * and double quotes will be escaped.
     *
     * @param s the source string
     * @return the string literal
     */
    public static String escapeJavaString(String s) {
        StringBuilder b = new StringBuilder(s.length() + 8).append('"');
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    b.append("\\\"");
                    break;
                case '\r':
                    b.append("\\r");
                    break;
                case '\n':
                    b.append("\\n");
                    break;
                case '\t':
                    b.append("\\t");
                    break;
                case '\\':
                    b.append("\\\\");
                    break;
                case '\0':
                    b.append("\\0");
                    break;
                default:
                    b.append(c);
            }
        }
        return b.append('"').toString();
    }
}
