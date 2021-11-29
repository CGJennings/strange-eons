package ca.cgjennings.apps.arkham.plugins.engine;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;
import resources.Language;

/**
 * Utility methods that ease that support the script engine implementation.
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
final class EngineUtilities {

    private EngineUtilities() {
    }

    /**
     * The file name to use when the true file name cannot be determined.
     */
    public static final String FALLBACK_FILE_NAME = "<Unknown source>";

    /**
     * Given a script context, returns the file name of the script. If no file
     * name is known, returns a non-null fallback name instead.
     *
     * @param scriptContext the script scriptContext to get the file name for
     * @return a non-null file name
     */
    public static String fileNameFrom(ScriptContext scriptContext) {
        Objects.requireNonNull(scriptContext, "scriptContext");
        String fileName = null;
        try {
            Object nameObj = null;
            Bindings b = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
            if (b != null) {
                nameObj = b.get(ScriptEngine.FILENAME);
            } else {
                b = scriptContext.getBindings(ScriptContext.GLOBAL_SCOPE);
                if (b != null) {
                    nameObj = b.get(ScriptEngine.FILENAME);
                }
            }
            if (nameObj != null) {
                fileName = nameObj.toString();
            }
        } catch (Throwable t) {
            // will get fallback name
        }
        return fileName == null ? FALLBACK_FILE_NAME : fileName;
    }

    /**
     * Converts an I/O exception when reading a script into a JSR 223 script
     * exception.
     */
    public static SEScriptException convertException(IOException ex, String fileName) {
        return new SEScriptException(Language.string("rk-err-text-resource", fileName), fileName, -1, ex);
    }

    /**
     * Converts a script engine exception to a JSR 223 script exception.
     */
    public static SEScriptException convertException(Exception ex) {
        if (ex instanceof RhinoException) {
            final RhinoException rex = (RhinoException) ex;
            String message = rex.getLocalizedMessage();
            if (rex instanceof JavaScriptException) {
                final JavaScriptException jsex = (JavaScriptException) rex;
                Object value = jsex.getValue();
                if (value != null && NATIVE_ERROR_CLASS.equals(value.getClass().getName())) {
                    message = value.toString();
                }
            }
            int line = rex.lineNumber();
            return new SEScriptException(
                    message, rex.sourceName(), line == 0 ? -1 : line, rex
            );
        } else {
            return new SEScriptException(ex);
        }
    }
    private static final String NATIVE_ERROR_CLASS = "org.mozilla.javascript.NativeError";

    public static <T> T implement(final SEScriptEngine engine, final Object thisObject, final Class<T> interfaceType) {
        if (interfaceType == null || !interfaceType.isInterface()) {
            throw new IllegalArgumentException("not an interface: " + interfaceType);
        }
        final Object proxy = Proxy.newProxyInstance(
                interfaceType.getClassLoader(),
                new Class<?>[]{interfaceType},
                (proxyInstance, method, args) -> {
                    final Object result = engine.invokeMethod(thisObject, method.getName(), args);
                    final Class returnType = method.getReturnType();
                    return returnType == Void.TYPE ? null : Context.jsToJava(result, returnType);
                }
        );
        return interfaceType.cast(proxy);
    }

    public static Object unwrapJsObject(Object result) {
        if (result instanceof Wrapper) {
            result = ((Wrapper) result).unwrap();
        }
        return result instanceof Undefined ? null : result;
    }
}
