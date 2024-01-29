package ca.cgjennings.i18n;

import java.util.Locale;
import java.util.regex.PatternSyntaxException;
import resources.Language;
import static resources.Language.string;

public class PatternExceptionLocalizer {

    private PatternExceptionLocalizer() {
    }

    public static String localize(String pattern, PatternSyntaxException ex) {
        String message = ex.getDescription();
        if (message.equals("Unexpected internal error") && pattern.endsWith("\\")) {
            return string("regexp-incomplete-escape-sequence");
        }
        if (message.startsWith("Unexpected character '")) {
            return string("regexp-unexpected-character") + " '" + message.substring(message.length() - 2, 1) + '\'';
        }
        if (message.startsWith("Dangling meta character '")) {
            return string("regexp-dangling-meta-character") + " '" + message.substring(message.length() - 2, 1) + '\'';
        }
        if (message.startsWith("Unknown character property name {")) {
            return string("regexp-unknown-character-property-name") + message.substring("Unknown character property name".length());
        }
        String key = "regexp-" + message.toLowerCase(Locale.CANADA).replace(' ', '-');
        if (Language.getInterface().isKeyDefined(key)) {
            return string(key);
        } else {
            return message;
        }
    }
}
