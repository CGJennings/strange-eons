package ca.cgjennings.apps.arkham.plugins.typescript;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.TextEncoding;
import ca.cgjennings.apps.arkham.plugins.SEScriptEngine;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import ca.cgjennings.apps.arkham.plugins.SEScriptEngineFactory;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.Level;
import javax.script.ScriptException;
import javax.swing.SwingUtilities;

/**
 * A low-level Java interface to TypeScript language services.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class TypeScriptServiceProvider {
    private SEScriptEngine engine;
    private TypeScriptServices services;
    private Object ts;
    private Object json;

    /**
     * Creates a new service provider. As the language services typically
     * require several seconds to initialize, this class is typically
     * instantiated in a separate thread.
     */
    public TypeScriptServiceProvider() {
        long startTime = System.currentTimeMillis();
        StrangeEons.log.log(Level.INFO, "TS starting new service provider: {0}", Thread.currentThread());
        Context cx = Context.enter();
        try {
            if (!SwingUtilities.isEventDispatchThread()) {
                SEScriptEngineFactory.makeCurrentThreadAUtilityThread();
            }
            engine = SEScriptEngineFactory.getStandardScriptEngine();
            // This file is stored in lib/typescript-services.jar to
            // reduce build times and prevent IDEs from trying to
            // process it for errors, code completions, etc.
            //
            // To update, check:
            // https://rawgit.com/Microsoft/TypeScript/master/lib/typescriptServices.js
            load("typescriptServices.js");
            load("javaBridge.js");
            final Object bridgeImpl = engine.get("bridge");
            services = engine.getInterface(bridgeImpl, TypeScriptServices.class);
        } catch(ScriptException ex) {
            throw new AssertionError("failed to parse library", ex);
        } finally {
            if (cx != null) Context.exit();
        }
        StrangeEons.log.log(Level.INFO, "TS service provider started in {0} ms", System.currentTimeMillis() - startTime );
    }

    /**
     * Returns an object that provides direct, synchronous access to the
     * available services. The {@link TypeScript} class provides the same
     * services asynchronously.
     * 
     * @return a non-null concrete implementation of the service interface
     */
    public TypeScriptServices getServices() {
        return services;
    }

    /**
     * Returns the TypeScript compiler API script object. This can be used
     * to access the API directly from script code.
     *
     * @return the compiler API
     */
    public Object getTs() {
        return engine.get("ts");
    }

    /**
     * Evaluate part of the JS implementation in the underlying script engine.
     * @param resourceFile the file to evaluate, relative to this class
     */
    private void load(String resourceFile) throws ScriptException {
        try(Reader r = new InputStreamReader(
                    TypeScriptServiceProvider.class.getResourceAsStream(resourceFile),
                    TextEncoding.SOURCE_CODE
            )) {
            engine.eval(r);
            r.close();
        } catch(IOException ex) {
            throw new AssertionError("unable to load " + resourceFile, ex);
        }
    }

    private static class TSEngineErrorReporter implements ErrorReporter {
        private ErrorReporter parent;

        TSEngineErrorReporter(ErrorReporter parent) {
            this.parent = parent;
        }

        @Override
        public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
        }

        @Override
        public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
            StrangeEons.log.severe("error in TS service lib");
            if (parent != null) {
                parent.error(message, sourceName, line, lineSource, lineOffset);
            }
        }

        @Override
        public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
            StrangeEons.log.severe("runtime error in TS service lib");
            if (parent != null) {
                return parent.runtimeError(message, sourceName, line, lineSource, lineOffset);
            }
            return null;
        }
    }
}
