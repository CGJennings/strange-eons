package org.strangeeons.scripts;

/**
 * An isolated execution context for scripts in one of the languages
 * registered through {@link ScriptLanguage}. A session retains
 * language-defined state (parsed scripts, function definitions, bindings)
 * across calls until {@link #close()} releases it.
 *
 * <p>This Tier 1 surface is deliberately minimal: evaluate source, call a
 * previously-defined function, bind a value into the global scope, and
 * release. Compilation, interface-implementation, and language metadata
 * back-references will be added back as concrete callers demand them.
 *
 * @since 3.5
 */
public interface ScriptSession extends AutoCloseable {

    /**
     * Evaluates the given source. The {@code sourceName} is used by the
     * language runtime for error messages and stack traces and should
     * identify the script's origin (e.g. a file path or a synthetic name
     * such as {@code "<console>"}).
     *
     * @return the value of the last expression in the source, or
     *     {@code null} if the language has no such notion
     * @throws ScriptException if the script fails to parse or run
     */
    Object eval(String source, String sourceName) throws ScriptException;

    /**
     * Invokes a function defined by previously-evaluated source. The
     * function is resolved against the session's global scope.
     *
     * @return the function's return value, or {@code null} if it returns
     *     no value
     * @throws ScriptException if the function is missing or raises an
     *     error during execution
     */
    Object call(String name, Object... args) throws ScriptException;

    /**
     * Binds {@code value} to {@code name} in the session's global scope so
     * subsequent {@link #eval} or {@link #call} calls can access it. The
     * legal name space is up to the implementation.
     */
    void bind(String name, Object value);

    /**
     * Releases the session and any resources held by the underlying
     * runtime. Implementations must accept repeated calls without raising.
     */
    @Override
    void close();
}
