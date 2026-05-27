package ca.cgjennings.apps.arkham.plugins;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.strangeeons.scripts.ScriptException;
import org.strangeeons.scripts.ScriptLanguage;
import org.strangeeons.scripts.ScriptSession;

/**
 * Test {@link ScriptLanguage} registered through {@code META-INF/services}
 * for the {@code script-language} dispatch tests.
 *
 * <p>The session it returns ignores eval source content and records every
 * call so tests can assert what the host did.
 */
public class TestScriptLanguage implements ScriptLanguage {

    public static final String ID = "test-language";

    /** Captures the most recently created session so tests can inspect it. */
    public static volatile RecordingSession lastSession;

    /**
     * Returns to install on every newly created session's {@code returns}
     * map, simulating script functions the host probes for. Tests populate
     * this <em>before</em> calling {@link ScriptLanguagePlugin#initializePlugin}.
     */
    public final Map<String, Object> defaultReturns = new HashMap<>();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Test Language";
    }

    @Override
    public Set<String> fileExtensions() {
        return Set.of(".test");
    }

    @Override
    public ScriptSession createSession() {
        RecordingSession s = new RecordingSession();
        s.returns.putAll(defaultReturns);
        lastSession = s;
        return s;
    }

    /**
     * Session impl that records every method invocation. Tests configure
     * {@link #returns} to dictate what {@link #callIfDefined} returns for
     * a named function (defaults to {@code null}, i.e. "not defined").
     */
    public static final class RecordingSession implements ScriptSession {

        public final Map<String, Object> bindings = new HashMap<>();
        public final Map<String, Object> returns = new HashMap<>();
        public int evalCount;
        public String lastEvalSourceName;
        public boolean closed;

        @Override
        public Object eval(String source, String sourceName) {
            evalCount++;
            lastEvalSourceName = sourceName;
            return null;
        }

        @Override
        public Object call(String name, Object... args) throws ScriptException {
            if (!returns.containsKey(name)) {
                throw new ScriptException("undefined: " + name);
            }
            return returns.get(name);
        }

        @Override
        public Object callIfDefined(String name, Object... args) {
            return returns.get(name);
        }

        @Override
        public void bind(String name, Object value) {
            bindings.put(name, value);
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
