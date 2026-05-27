package org.strangeeons.scripts;

/**
 * Thrown by {@link ScriptSession} when a script fails to evaluate or a
 * function call into the script raises an error.
 *
 * <p>Mirrors the convention of {@code javax.script.ScriptException}
 * (checked, message + cause) without depending on JSR-223. Implementations
 * are free to subclass to carry additional location data.
 *
 * @since 3.5
 */
public class ScriptException extends Exception {

    private static final long serialVersionUID = 1L;

    public ScriptException(String message) {
        super(message);
    }

    public ScriptException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScriptException(Throwable cause) {
        super(cause);
    }
}
