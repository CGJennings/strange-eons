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
    private Object ts;
    private Object json;

    /**
     * Creates a new service provider. As the language services typically
     * require several seconds to initialize, this class is typically
     * instantiated in a separate thread.
     */
    public TypeScriptServiceProvider() {
        try (Reader lib = readTSServicesLib()) {
            if(lib == null) throw new AssertionError("could not load services lib");
            engine = new SEScriptEngine();
            engine.eval(lib);
            ts = engine.get("ts");
            json = engine.get("JSON");
        } catch(ScriptException | IOException ex) {
            throw new AssertionError("failed to parse library", ex);
        }
    }
    
    private static String unwrapString(Object s) {
        return s == null ? null :  String.valueOf(s);
    }

    public String transpile(String source) {
        return unwrapString(ts("transpile", source));
    }



    /**
     * Invoke a method on the TypeScript service instance. If the method does
     * not exist or the attempt results in an error, a message is logged and
     * null is returned.
     *
     * @param method the method name
     * @param args arguments to pass
     * @return the return value
     */
    public Object ts(String method, Object... args) {
        return invoke(ts, method, args);
    }

    /**
     * Invoke a method or function on any object in the TypeScript service runtime.
     * If the method does not exist or the attempt results in an error, a
     * message is logged and null is returned.
     *
     * @param thisObj the script object of the instance whose method will be invoked;
     *   if null, then a function in the global scope with the given name is called instead
     * @param method the method name
     * @param args arguments to pass
     * @return the return value
     */
    public Object invoke(Object thisObj, String method, Object... args) {
        try {
            if(thisObj == null) {
                return engine.invokeFunction(method, args);
            }
            return engine.invokeMethod(thisObj, method, args);
        } catch(NoSuchMethodException nsm) {
            StrangeEons.log.log(Level.SEVERE, "no such method: {0}", method);
        } catch(ScriptException ex) {
            StrangeEons.log.log(Level.SEVERE, "invoke failed: " + method, ex);
        }
        return null;
    }

    /**
     * Get an object by name from the global scope.
     *
     * @param nameInGlobalScope name of the object
     * @return the object's value, or null if undefined
     */
    public Object object(String nameInGlobalScope) {
        return engine.get(nameInGlobalScope);
    }

    /**
     * Returns a JavaScript string literal equivalent to the specified unsafe
     * string.
     *
     * @param unsafe the string to convert to a string literal
     * @return a JavaScript string literal of the string, or null if null was
     *   specified
     */
    public String quote(String unsafe) {
        if(unsafe == null) return unsafe;
        return unwrapString(invoke(json, "stringify", unsafe));
    }

    /**
     * Returns an input stream reader that reads the bundled TypeScript
     * language services library.
     * 
     * <p>
     * The bundled library can be updated from
     * <a href="https://rawgit.com/Microsoft/TypeScript/master/lib/typescriptServices.js">this URL</a>.
     * 
     * @return a reader for the library source code, or null if it is missing
     */
    private static Reader readTSServicesLib() {
        InputStream in = TypeScriptServiceProvider.class.getResourceAsStream("typescriptServices.js");
        if(in == null) return null;
        return new InputStreamReader(in, StandardCharsets.UTF_8);
    }
}
