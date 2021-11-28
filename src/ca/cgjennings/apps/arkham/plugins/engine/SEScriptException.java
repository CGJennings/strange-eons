package ca.cgjennings.apps.arkham.plugins.engine;

import ca.cgjennings.script.util.ExtendedScriptException;

/**
 * The exception class that wraps all exceptions thrown by the underlying script
 * engine(s).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
final class SEScriptException extends ExtendedScriptException {

    public SEScriptException(String message, String fileName, int lineNumber, int columnNumber) {
        super(message, fileName, lineNumber, columnNumber);
    }

    public SEScriptException(Throwable cause, String message, String fileName, int lineNumber) {
        super(cause, message, fileName, lineNumber);
    }

    public SEScriptException(String message, String fileName, int lineNumber) {
        super(message, fileName, lineNumber);
    }

    public SEScriptException(Exception e) {
        super(e);
    }

    public SEScriptException(String s) {
        super(s);
    }

    public SEScriptException(Throwable cause, String message, String fileName, int lineNumber, int columnNumber) {
        super(cause, message, fileName, lineNumber, columnNumber);
    }
}
