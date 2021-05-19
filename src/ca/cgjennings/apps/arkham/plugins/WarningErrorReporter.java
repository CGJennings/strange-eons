package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.dialog.prefs.Preferences;
import ca.cgjennings.apps.arkham.plugins.ScriptConsole.ConsolePrintWriter;
import ca.cgjennings.script.mozilla.javascript.ErrorReporter;
import ca.cgjennings.script.mozilla.javascript.EvaluatorException;
import java.util.ResourceBundle;
import resources.Settings;

/**
 * An error reporter that displays warnings.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
class WarningErrorReporter implements ErrorReporter {

    private ErrorReporter parent;

    public static WarningErrorReporter getShared(ErrorReporter parent) {
        if (shared == null) {
            shared = new WarningErrorReporter(parent);
        }
        if (shared.parent == parent) {
            return shared;
        }
        return new WarningErrorReporter(parent);
    }
    private static WarningErrorReporter shared;

    public WarningErrorReporter(ErrorReporter parent) {
        if (parent == null) {
            throw new NullPointerException("null parent argument");
        }
        this.parent = parent;
    }

    @Override
    public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
        if(!acceptError(message, true)) return;
        // this output follows the same format as an exception
        // so that double-clicking the console will display the file
        final ConsolePrintWriter w = ScriptMonkey.getSharedConsole().getErrorWriter();
        if (sourceName != null) {
            w.printf(
                    "WARNING: %s\n\tat %s:%d\n", message, sourceName, line
            );
        } else {
            w.println("WARNING: " + message);
        }
    }

    @Override
    public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
        parent.error(message, sourceName, line, lineSource, lineOffset);
    }

    @Override
    public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
        return parent.runtimeError(message, sourceName, line, lineSource, lineOffset);
    }

    /**
     * Returns whether an error should be considered a true error, or filtered out.
     *
     * @param message the error message
     * @param warning true if the error is a warning
     * @return true if the error should be ignored
     */
    static boolean acceptError(String message, boolean warning) {
        // The default implementation ignores "Code has no side effects" warnings;
        // this happens whenever we eval a script file
        if(WarningErrorReporter.CODE_HAS_NO_SIDE_EFECTS.equals(message)) return false;
        // ignore missing ; warnings
        if(message.contains(" ; ") && Settings.getUser().getYesNo("script-ignore-missing-semicolons")) return false;
        return true;
    }

    static final String CODE_HAS_NO_SIDE_EFECTS = ResourceBundle.getBundle(
            "ca/cgjennings/script/mozilla/javascript/resources/Messages"
    ).getString("msg.no.side.effects");
}
