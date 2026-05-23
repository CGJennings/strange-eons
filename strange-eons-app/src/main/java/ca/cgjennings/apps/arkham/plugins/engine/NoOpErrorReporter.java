package ca.cgjennings.apps.arkham.plugins.engine;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

/**
 * An error reporter that does not generate any output. This is useful primarily
 * when performing internal processing on scripts.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class NoOpErrorReporter implements ErrorReporter {

    @Override
    public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
    }

    @Override
    public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
    }

    @Override
    public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
        return new EvaluatorException(message, sourceName, lineOffset, lineSource, line);
    }
}
