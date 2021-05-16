package ca.cgjennings.text;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A template processor assists in the automated writing of scripts and other
 * text files by filling in a template file using variables and simple boolean
 * conditions. Sections of the template that are to be generated automatically
 * are marked with <i>symbols</i>. A symbol consists of a special token placed
 * between braces. The tokens begin with a specific marker character and are
 * followed by an identifier. The identifier may consist of any characters
 * except spaces or the closing brace. The following codes are recognized:
 * <p>
 * <b>{%<i>variable</i>}</b><br>
 * Replaces the symbol with the value of the variable. Throws an exception if
 * the variable is undefined.
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
public class TemplateProcessor {

    private Locale locale;
    private Map<String, String> vars;
    private Map<String, Boolean> conds;

    /**
     * Creates a new processor that uses the default locale when adding format
     * strings.
     */
    public TemplateProcessor() {
        this(Locale.getDefault());
    }

    /**
     * Creates a new processor that uses the specified locale when adding format
     * strings.
     *
     * @param locale the locale to use when formatting strings
     */
    public TemplateProcessor(Locale locale) {
        this.locale = locale;
        vars = new HashMap<>();
        conds = new HashMap<>();
    }

    /**
     * Creates a new processor that copies its locales, variables, and
     * conditions from another processor.
     *
     * @param source
     */
    public TemplateProcessor(TemplateProcessor source) {
        this(source.getLocale());
        vars.putAll(source.vars);
        conds.putAll(source.conds);
    }

    /**
     * Returns the locale used to process format strings.
     *
     * @return the formatting locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Sets the locale used to process format strings. If {@code null}, the
     * default locale is set.
     */
    public void setLocale(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        this.locale = locale;
    }

    /**
     * Sets a variable.
     *
     * @param name the variable name
     * @param value the string value of the variable
     * @return this processor, so that the method can be chained
     */
    public TemplateProcessor set(String name, String value) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }
        vars.put(name, value);
        return this;
    }

    /**
     * Sets a variable to the result of formatting the string using the template
     * processor's locale.
     *
     * @param name the variable name
     * @param formatString the string that describes the format
     * @param formatArgs the arguments used to complete the format string
     * @return this processor, so that the method can be chained
     */
    public TemplateProcessor set(String name, String formatString, Object... formatArgs) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        vars.put(name, String.format(getLocale(), formatString, formatArgs));
        return this;
    }

    /**
     * Sets a group of variable definitions from an array. The elements of the
     * array form pairs of variable names and values, with the names at even
     * array indices. Example:<br>
     * {@code setAll( "var1", "apple", "var2", "3.14159", "var3", "sun dance" )}
     *
     * @param nameValuePairs the pairs of variable names and values to set
     * @return this processor, so that the method can be chained
     */
    public TemplateProcessor setAll(String... nameValuePairs) {
        if (nameValuePairs == null) {
            throw new NullPointerException("nameValuePairs");
        }
        if ((nameValuePairs.length & 1) != 0) {
            throw new IllegalArgumentException("arguments must be provided in pairs");
        }
        for (int i = 0; i < nameValuePairs.length; i += 2) {
            set(nameValuePairs[i], nameValuePairs[i + 1]);
        }
        return this;
    }

    /**
     * Returns the value of a variable.
     *
     * @param name the variable name
     * @return the value of the variable
     * @throws NullPointerException if the variable name is {@code null}
     * @throws IllegalArgumentException if the variable name is undefined
     */
    public String get(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        String v = vars.get(name);
        if (v == null) {
            throw new IllegalArgumentException("undefined variable: " + name);
        }
        return v;
    }

    /**
     * Sets the value of a condition.
     *
     * @param name the condition name
     * @param value {@code true} if the condition is to be set
     * @return this processor, so that the method can be chained
     */
    public TemplateProcessor setCondition(String name, boolean value) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        conds.put(name, value);
        return this;
    }

    /**
     * Returns {@code true} if the named condition is set.
     *
     * @param name the name of the condition
     * @return {@code true} if the named condition is set
     * @throws NullPointerException if the variable name is {@code null}
     * @throws IllegalArgumentException if the variable name is undefined
     */
    public boolean isConditionSet(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        Boolean v = conds.get(name);
        if (v == null) {
            throw new IllegalArgumentException("undefined condition: " + name);
        }
        return v;
    }

    /**
     * Processes a template, replacing the symbols as specified in the class
     * description.
     *
     * @param template the template text to process
     * @return the filled-in template
     */
    public String process(String template) {
        StringBuilder o = new StringBuilder(template.length() * 125 / 100);
        for (;;) {
            Matcher m = findSymbol(template);
            if (m == null) {
                // no more symbols, just append whatever is left
                o.append(template);
                break;
            } else {
                int start = m.start();
                o.append(template, 0, start); // everything up to the symbol
                template = processSymbol(o, m.group(), m.group(1).charAt(0), m.group(2), template.substring(m.end()));
            }
        }
        return o.toString();
    }

    /**
     * Finds the first symbol starting from the start of this string, returning
     * a matcher with info about the symbol or {@code null} if there is no
     * symbol.
     *
     * @param source the source being filled in
     * @return a matcher at the first match, or {@code null}
     */
    private Matcher findSymbol(String source) {
        Matcher m = SYMPAT.matcher(source);
        if (m.find()) {
            return m;
        } else {
            return null;
        }
    }

    /**
     * This method is called to process a symbol. Subclasses may override it to
     * extend the template processor's capabilities. The pattern for doing so is
     * to first check the code against those codes that you wish to handle and
     * then either handle the symbol in the subclass or return the result of the
     * super implementation. A typical code pattern is:
     * <pre>
     * protected String processSymbol( StringBuilder buffer, String symbol, char code, String name, String remainder ) {
     *     switch( code ) {
     *         case '*':
     *             buffer.append( "text for a *-code with variable name: " + name );
     *             break;
     *         // ...
     *         default:
     *             remainder = super( buffer, symbol, code, name, remainder );
     *     }
     *     return remainder;
     * }
     * </pre>
     *
     * @param buffer a buffer that the symbol's replacement text, if any, should
     * be appended to
     * @param symbol the full text of the symbol, e.g., {%variable}
     * @param code the single-character code identifying the symbol type, e.g.,
     * %
     * @param name the name part of the the symbol following the code, e.g.,
     * variable
     * @param remainder the template text that follows the symbol
     * @return the template text that follows the symbol after the symbol is
     * processed (usually just {@code remainder})
     * @throws IllegalArgumentException if there is a syntax error in the
     * template or a runtime error (such as an undefined variable)
     */
    protected String processSymbol(StringBuilder buffer, String symbol, char code, String name, String remainder) {
        switch (code) {
            case '%':
                String v = get(name);
                if (v.indexOf('{') >= 0) {
                    remainder = v + remainder; // allow the replacement to be parsed
                } else {
                    buffer.append(v);
                }
                break;
            case '/':
                error("unmatched close symbol: " + symbol, remainder);
                break;
            case '?':
            case '!':
                boolean includeText = isConditionSet(name);
                if (code == '!') {
                    includeText = !includeText;
                }
                Matcher close = findCloseSymbol(remainder, code, name);
                if (close == null) {
                    error("unmatched open symbol: " + symbol, remainder);
                }
                if (includeText) {
                    remainder = remainder.substring(0, close.start()) + remainder.substring(close.end());
                } else {
                    remainder = remainder.substring(close.end());
                }
                break;
            default:
                // unknown code: assume that it is literal text and pass it through
                buffer.append(symbol);
        }
        return remainder;
    }

    /**
     * Throws an exception to describe a parsing error. The value of remainder
     * is used to provide context about the error.
     *
     * @param message the error message to provide
     * @param remainder the remaining unprocessed text (may be
     * {@code null})
     * @throws IllegalArgumentException
     */
    protected void error(String message, String remainder) throws IllegalArgumentException {
        String context = "";
        if (remainder != null) {
            context = remainder.substring(0, Math.min(20, remainder.length()));
        }
        if (context.isEmpty()) {
            throw new IllegalArgumentException(message);
        } else {
            throw new IllegalArgumentException(message + " (" + context + "...)");
        }
    }

    /**
     * Returns offset of the start of the first close symbol that matches the
     * description, or -1. A close symbol is a symbol that with the code '/'
     * whose name is the code and name of another symbol. For example, a
     * conditional is processed by finding the close symbol with the
     * conditional's code and name.
     *
     * @param remainder the remainder text to search
     * @param code the code of the symbol being closed
     * @param name the name of the symbol being closed
     * @return the offset of the start of the first close symbol in remainder,
     * or -1
     */
    private Matcher findCloseSymbol(String remainder, char code, String name) {
        Pattern close = Pattern.compile(
                "\\{\\/" + Pattern.quote(String.valueOf(code + name)) + "\\}"
        );
        Matcher m = close.matcher(remainder);
        if (m.find()) {
            return m;
        } else {
            return null;
        }
    }

    private final Pattern SYMPAT = Pattern.compile("\\{([^}\\s])([^}\\s]*)\\}");
}
