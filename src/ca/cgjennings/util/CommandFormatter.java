package ca.cgjennings.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Parses strings that represent a command line into a list of arguments,
 * possibly performing simple variable replacements.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class CommandFormatter {

    private HashMap<Character, Object> symbolTable = new HashMap<>(8);

    /**
     * Creates a new command formatter that has no variables defined except the
     * reserved % variable.
     */
    public CommandFormatter() {
    }

    /**
     * Creates a new command formatter, populating the table of variables from
     * an array of strings. This array must contain an even number of strings,
     * forming a number of pairs. The first string of each pair is a one
     * character variable name, and the second is the value to assign to that
     * name.
     *
     * @param variableDefinitions an array of name, value pairs
     * @throws IllegalArgumentException if a variable name is missing a value,
     * or if any name is not exactly one character
     * @throws NullPointerException if the array or any element in it is
     * {@code null}
     */
    public CommandFormatter(String... variableDefinitions) {
        if (variableDefinitions == null) {
            throw new NullPointerException("variableDefinitions");
        }
        final int len = variableDefinitions.length;
        if ((len & 1) == 1) {
            throw new IllegalArgumentException("variable has no value: " + variableDefinitions[len]);
        }

        for (int i = 0; i < len; i += 2) {
            String name = variableDefinitions[i];
            String value = variableDefinitions[i + 1];
            if (name == null) {
                throw new NullPointerException("variableDefinitions[" + i + "]");
            }
            if (value == null) {
                throw new NullPointerException("variableDefinitions[" + (i + 1) + "]");
            }
            if (name.length() != 1) {
                throw new IllegalArgumentException("variable name must be one character: " + name);
            }
            setVariable(name.charAt(0), value);
        }
    }

    /**
     * Creates a new command formatter, populating the table of variables from a
     * map.
     *
     * @param variableMap a map from variable names to their values
     * @throws NullPointerException if any map entry is {@code null}
     * @throws IllegalArgumentException if any key does not have length 1 or is
     * the % symbol
     */
    public CommandFormatter(Map<String, String> variableMap) {
        if (variableMap == null) {
            throw new NullPointerException("variableMap");
        }
        for (Entry<String, String> e : variableMap.entrySet()) {
            if (e.getKey() == null) {
                throw new NullPointerException("null key");
            }
            if (e.getValue() == null) {
                throw new NullPointerException("null value for " + e.getKey());
            }
            if (e.getKey().length() != 1) {
                throw new IllegalArgumentException("key must have length 1: " + e.getKey());
            }
            setVariable(e.getKey().charAt(0), e.getValue());
        }
    }

    /**
     * Defines a variable that will replaced when formatting a command. When a
     * percent sign (%) occurs in a command template and is followed by this
     * variable name, it will be replaced by calling {@code value.toString()}.
     * Setting a variable name to {@code null} will remove the variable, if it
     * has been defined. The variable % is reserved so that the sequence "%%"
     * always produces "%" in the formatted command.
     *
     * @param name the variable name
     * @param value the value for the variable
     * @throws IllegalArgumentException if {@code name} is the percent sign
     */
    public void setVariable(char name, Object value) {
        if (name == '%') {
            throw new IllegalArgumentException("cannot redefine reserved variable %");
        }
        Character v = name;
        if (value == null) {
            symbolTable.remove(v);
        } else {
            symbolTable.put(v, value);
        }
    }

    /**
     * Returns the value of a variable, or {@code null} if the variable is not
     * defined.
     *
     * @param name the variable name
     * @return the object assigned to the variable
     */
    public Object getVariable(char name) {
        if (name == '%') {
            return "%";
        }
        return symbolTable.get(name);
    }

    /**
     * Evaluates a variable to return its string expansion in a formatted
     * command. This is done by obtaining the variable's assigned value and
     * calling its {@code toString()} method.
     *
     * @param name the name of the variable to evaluate
     * @return a string representing the value of the variable
     * @throws IllegalArgumentException if the variable has no assigned value
     */
    public String evaluateVariable(char name) {
        Object v = getVariable(name);
        if (v == null) {
            throw new IllegalArgumentException("undefined variable " + name);
        }
        return v.toString();
    }

    /**
     * Given a template for a command line that may include variables, returns
     * an array of the tokens that make up the command. Tokens are separated by
     * whitespace, except that tokens
     *
     * @param template the command template to tokenize
     * @return the tokens in the command, with variables replaced
     * @throws IllegalArgumentException if a variable is used that has no
     * assigned value (last character of the message is the variable name)
     */
    public String[] formatCommand(String template) {
        LinkedList<String> tokens = new LinkedList<>();
        StringBuilder token = new StringBuilder();
        int state = 0;
        for (int i = 0; i < template.length(); ++i) {
            char c = template.charAt(i);
            switch (state) {
                case 0: // regular mode
                    if (Character.isWhitespace(c)) {
                        // this check prevents making empty tokens on multiple spaces
                        if (token.length() > 0) {
                            tokens.add(token.toString());
                            token.delete(0, token.length());
                        }
                    } else if (c == '\"') {
                        state = 1;
                    } else if (c == '%') {
                        state = 2;
                    } else {
                        token.append(c);
                    }
                    break;
                case 1: // quote mode
                    if (c == '\"') {
                        state = 0;
                        // clever trick: inside a quote, "" will produce a
                        // quote in the token without (effectively) leaving quote mode
                        // the first quote will leave, the second brings us back, the
                        // check below inserts a quote in the token
                        if (i < template.length() - 1 && template.charAt(i + 1) == '\"') {
                            token.append('\"');
                        }
                    } else if (c == '%') {
                        state = 3;
                    } else {
                        token.append(c);
                    }
                    break;
                case 2: // variable escape, not in quote
                case 3: // variable escape, in quote
                    token.append(evaluateVariable(c));
                    state -= 2;
                    break;
                default:
                    throw new AssertionError("unknown state: " + state);
            }
        }
        // add last token, if any
        if (token.length() > 0) {
            tokens.add(token.toString());
        }
        return tokens.toArray(new String[0]);
    }
}
