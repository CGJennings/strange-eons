package ca.cgjennings.apps.arkham.plugins.typescript;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.plugins.SEScriptEngine;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import javax.script.ScriptException;

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
        try {
            engine = new SEScriptEngine();
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
        }
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
                    StandardCharsets.UTF_8
            )) {
            engine.eval(r);
            r.close();
        } catch(IOException ex) {
            throw new AssertionError("unable to load " + resourceFile, ex);
        }
    }
}
