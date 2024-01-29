package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.TextEncoding;
import ca.cgjennings.apps.arkham.plugins.engine.SEScriptEngine;
import ca.cgjennings.apps.arkham.plugins.engine.SEScriptEngineFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.logging.Level;
import javax.script.ScriptException;

/**
 * A code formatter that leverages a JavaScript-based formatting engine.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
final class ScriptedFormatter implements Formatter {

    String functionName;
    String sourceFile;

    ScriptedFormatter(String sourceFile, String function) {
        this.functionName = function;
        this.sourceFile = sourceFile;
    }

    @Override
    public String format(String code) {
        try {
            SEScriptEngine engine = getEngine(sourceFile);
            return (String) engine.invokeFunction(functionName, code);
        } catch (Exception ex) {
            // if *anything* goes wrong we can safely log the failure and
            // just return the original string unchanged
            StrangeEons.log.log(Level.SEVERE, "formatter failed: " + sourceFile + '/' + functionName, ex);
        }
        return code;
    }

    private static final HashMap<String, SoftReference<SEScriptEngine>> engineCache = new HashMap<>(8);

    private static SEScriptEngine getEngine(String sourceFile) throws IOException, ScriptException {
        SEScriptEngine engine = null;
        SoftReference<SEScriptEngine> engineRef = engineCache.get(sourceFile);
        if (engineRef != null) {
            engine = engineRef.get();
        }

        if (engine == null) {
            try (InputStream in = ScriptedFormatter.class.getResourceAsStream(sourceFile)) {
                engine = SEScriptEngineFactory.getDefaultScriptEngine();
                engine.eval(new InputStreamReader(in, TextEncoding.SOURCE_CODE));
            }
            engineCache.put(sourceFile, new SoftReference<>(engine));
        }

        return engine;
    }
}
