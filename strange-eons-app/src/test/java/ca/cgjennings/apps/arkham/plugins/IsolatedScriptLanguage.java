package ca.cgjennings.apps.arkham.plugins;

import java.util.Set;
import org.strangeeons.scripts.ScriptLanguage;
import org.strangeeons.scripts.ScriptSession;

/**
 * Test {@link ScriptLanguage} bundled into an isolated test JAR by
 * {@link IsolatedBundleTest}; its compiled {@code .class} bytes are read
 * from the test classpath and copied into the JAR's contents so the
 * isolated {@code URLClassLoader} can discover it via
 * {@code META-INF/services}.
 */
public class IsolatedScriptLanguage implements ScriptLanguage {

    public static final String ID = "isolated-language";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Isolated Test Language";
    }

    @Override
    public Set<String> fileExtensions() {
        return Set.of(".isolated");
    }

    @Override
    public ScriptSession createSession() {
        throw new UnsupportedOperationException("test impl");
    }
}
