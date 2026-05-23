package ca.cgjennings.apps.arkham.plugins.engine;

import javax.script.ScriptException;

/**
 * The exception class that wraps all exceptions thrown by the underlying script
 * engine(s). Unlike the base {@link ScriptException}, allows a cause to be
 * specified along with a file position.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class SEScriptException extends ScriptException {

    public SEScriptException(String s) {
        super(s);
    }

    public SEScriptException(Exception e) {
        super(e);
    }

    public SEScriptException(String message, String fileName, int lineNumber) {
        super(message, fileName, lineNumber);
    }

    public SEScriptException(String message, String fileName, int lineNumber, Throwable cause) {
        super(message, fileName, lineNumber);
        initCause(cause);
    }

    public SEScriptException(String message, String fileName, int lineNumber, int columnNumber) {
        super(message, fileName, lineNumber, columnNumber);
    }

    public SEScriptException(String message, String fileName, int lineNumber, int columnNumber, Throwable cause) {
        super(message, fileName, lineNumber, columnNumber);
        initCause(cause);
    }
}
