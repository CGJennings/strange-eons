/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met: Redistributions of source code
 * must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution. Neither the name of the Sun Microsystems nor the names of
 * is contributors may be used to endorse or promote products derived from this software
 * without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.plugins.debugging.ScriptDebugging;
import ca.cgjennings.script.mozilla.javascript.Context;
import ca.cgjennings.script.mozilla.javascript.Function;
import ca.cgjennings.script.mozilla.javascript.ImporterTopLevel;
import ca.cgjennings.script.mozilla.javascript.JavaScriptException;
import ca.cgjennings.script.mozilla.javascript.LazilyLoadedCtor;
import ca.cgjennings.script.mozilla.javascript.RhinoException;
import ca.cgjennings.script.mozilla.javascript.Script;
import ca.cgjennings.script.mozilla.javascript.Scriptable;
import ca.cgjennings.script.mozilla.javascript.ScriptableObject;
import ca.cgjennings.script.mozilla.javascript.Synchronizer;
import ca.cgjennings.script.mozilla.javascript.Undefined;
import ca.cgjennings.script.mozilla.javascript.Wrapper;
import ca.cgjennings.script.util.InterfaceImplementor;
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import javax.script.*;

/**
 * Implementation of {@code ScriptEngine} for the modified Mozilla Rhino
 * build used for Strange Eons scripts.
 */
public final class SEScriptEngine extends AbstractScriptEngine implements Invocable, Compilable {

    /* Scope where standard JavaScript objects and our
     * extensions to it are stored. Note that these are not
     * user defined engine level global variables. These are
     * variables that have to be there on all compliant ECMAScript
     * scopes. We put these standard objects in this top level.
     */
    private ScriptableObject topLevel;

    /* map used to store indexed properties in engine scope
     * refer to comment on 'indexedProps' in ExternalScriptable.java.
     */
    private Map indexedProps;

    private SEScriptEngineFactory factory;
    private final InterfaceImplementor implementor;
    /**
     * Creates a new instance of SEScriptEngine
     */
    public SEScriptEngine() {

        Context cx = enterContext();

        try {
            topLevel = new ImporterTopLevel(cx, false);
            new LazilyLoadedCtor(topLevel, "JSAdapter",
                    "ca.cgjennings.apps.arkham.plugins.JSAdapter",
                    false);
            // add top level functions
            String names[] = { /*"bindings", "scope",*/"sync"};
            topLevel.defineFunctionProperties(names, SEScriptEngine.class, ScriptableObject.DONTENUM);
        } finally {
            Context.exit();
        }

        indexedProps = new HashMap();

        //construct object used to implement getInterface
        implementor = new InterfaceImplementor(this) {
            @Override
            protected Object convertResult(Method method, Object res)
                    throws ScriptException {
                Class desiredType = method.getReturnType();
                if (desiredType == Void.TYPE) {
                    return null;
                } else {
                    return Context.jsToJava(res, desiredType);
                }
            }
        };
    }

    @Override
    public Object eval(Reader reader, ScriptContext ctxt)
            throws ScriptException {
        Object ret;

        ScriptDebugging.prepareToEnterContext();
        Context cx = enterContext();
        try {
            Scriptable scope = getRuntimeScope(ctxt);
            scope.put("context", scope, ctxt);

            // NOTE (RRC) - why does it look straight into the engine instead of asking
            // the given ScriptContext object?
            // Modified to use the context
            // String filename = (String) get(ScriptEngine.FILENAME);
            String filename = null;
            if (ctxt != null && ctxt.getBindings(ScriptContext.ENGINE_SCOPE) != null) {
                filename = (String) ctxt.getBindings(ScriptContext.ENGINE_SCOPE).get(ScriptEngine.FILENAME);
            }
            if (filename == null) {
                filename = (String) get(ScriptEngine.FILENAME);
            }

            filename = filename == null ? "<Unknown source>" : filename;

            ret = cx.evaluateReader(scope, preProcessScriptSource(reader), filename, 1, null);
        } catch (JavaScriptException jse) {
            int line = (line = jse.lineNumber()) == 0 ? -1 : line;
            Object value = jse.getValue();
            String str = (value != null && value.getClass().getName().equals("ca.cgjennings.script.mozilla.javascript.NativeError")
                    ? value.toString()
                    : jse.toString());
            throw new SEScriptException(jse, str, jse.sourceName(), line);
        } catch (RhinoException re) {
            int line = (line = re.lineNumber()) == 0 ? -1 : line;
            throw new SEScriptException(re, re.toString(), re.sourceName(), line);
        } catch (IOException ee) {
            throw new ScriptException(ee);
        } finally {
            Context.exit();
        }

        return unwrapReturnValue(ret);
    }

    @Override
    public Object eval(String script, ScriptContext ctxt) throws ScriptException {
        if (script == null) {
            throw new NullPointerException("null script");
        }
        return eval(preProcessScriptSource(new StringReader(script)), ctxt);
    }

    @Override
    public ScriptEngineFactory getFactory() {
        if (factory != null) {
            return factory;
        } else {
            return new SEScriptEngineFactory();
        }
    }

    @Override
    public Bindings createBindings() {
        return new SettingBindings(new SimpleBindings());
    }

    //Invocable methods
    @Override
    public Object invokeFunction(String name, Object... args)
            throws ScriptException, NoSuchMethodException {
        return invokeMethod(null, name, args);
    }

    @Override
    public Object invokeMethod(Object thiz, String name, Object... args)
            throws ScriptException, NoSuchMethodException {

        Context cx = enterContext();
        try {
            if (name == null) {
                throw new NullPointerException("method name is null");
            }

            if (thiz != null && !(thiz instanceof Scriptable)) {
                thiz = Context.toObject(thiz, topLevel);
            }

            Scriptable engineScope = getRuntimeScope(context);
            Scriptable localScope = (thiz != null) ? (Scriptable) thiz
                    : engineScope;
            Object obj = ScriptableObject.getProperty(localScope, name);
            if (!(obj instanceof Function)) {
                throw new NoSuchMethodException("no such method: " + name);
            }

            Function func = (Function) obj;
            Scriptable scope = func.getParentScope();
            if (scope == null) {
                scope = engineScope;
            }
            Object result = func.call(cx, scope, localScope,
                    wrapArguments(args));
            return unwrapReturnValue(result);
        } catch (JavaScriptException jse) {
            int line = (line = jse.lineNumber()) == 0 ? -1 : line;
            Object value = jse.getValue();
            String str = (value != null && value.getClass().getName().equals("ca.cgjennings.script.mozilla.javascript.NativeError")
                    ? value.toString()
                    : jse.toString());
            throw new SEScriptException(jse, str, jse.sourceName(), line);
        } catch (RhinoException re) {
            int line = (line = re.lineNumber()) == 0 ? -1 : line;
            throw new SEScriptException(re, re.toString(), re.sourceName(), line);
        } finally {
            Context.exit();
        }
    }

    @Override
    public <T> T getInterface(Class<T> clasz) {
        try {
            return implementor.getInterface(null, clasz);
        } catch (ScriptException e) {
            return null;
        }
    }

    @Override
    public <T> T getInterface(Object thiz, Class<T> clasz) {
        if (thiz == null) {
            throw new IllegalArgumentException("script object can not be null");
        }

        try {
            return implementor.getInterface(thiz, clasz);
        } catch (ScriptException e) {
            return null;
        }
    }

    public Scriptable getRuntimeScope(ScriptContext ctxt) {
        if (ctxt == null) {
            throw new NullPointerException("null script context");
        }

        // we create a scope for the given ScriptContext
        Scriptable newScope = new ExternalScriptable(ctxt, indexedProps);

        // Set the prototype of newScope to be 'topLevel' so that
        // JavaScript standard objects are visible from the scope.
        newScope.setPrototype(topLevel);

        // define "context" variable in the new scope
        newScope.put("context", newScope, ctxt);

        return newScope;
    }

    //Compilable methods
    @Override
    public CompiledScript compile(String script) throws ScriptException {
        return compile(preProcessScriptSource(new StringReader(script)));
    }

    @Override
    public CompiledScript compile(java.io.Reader script) throws ScriptException {
        CompiledScript ret = null;
        Context cx = enterContext();

        try {
            String filename = (String) get(ScriptEngine.FILENAME);
            if (filename == null) {
                filename = "<Unknown Source>";
            }
            Scriptable scope = getRuntimeScope(context);
            @SuppressWarnings("deprecation")
            Script scr = cx.compileReader(preProcessScriptSource(script), filename, 1, null);
            ret = new SECompiledScript(this, scr);
        } catch (Exception e) {
            throw new ScriptException(e);
        } finally {
            Context.exit();
        }
        return ret;
    }

    //package-private helpers
    @SuppressWarnings("deprecation")
    static Context enterContext() {
        // call this always so that initializer of this class runs
        // and initializes custom wrap factory and class shutter.
        return Context.enter();
    }

    void setEngineFactory(SEScriptEngineFactory fac) {
        factory = fac;
    }

    Object[] wrapArguments(Object[] args) {
        if (args == null) {
            return Context.emptyArgs;
        }
        Object[] res = new Object[args.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = Context.javaToJS(args[i], topLevel);
        }
        return res;
    }

    Object unwrapReturnValue(Object result) {
        if (result instanceof Wrapper) {
            result = ((Wrapper) result).unwrap();
        }

        return result instanceof Undefined ? null : result;
    }

    protected Reader preProcessScriptSource(Reader reader) throws ScriptException {
        return reader;
    }

    /**
     * The sync function creates a synchronized function (in the sense of a Java
     * synchronized method) from an existing function. The new function
     * synchronizes on the {@code this} object of its invocation.
     */
    public static Object sync(Context cx, Scriptable thisObj, Object[] args,
            Function funObj) {
        if (args.length == 1 && args[0] instanceof Function) {
            return new Synchronizer((Function) args[0]);
        } else {
            throw Context.reportRuntimeError("wrong argument(s) for sync");
        }
    }

    /**
     * Information about a single frame on the script call stack.
     */
    public final static class ScriptTraceElement {

        private String file;
        private int line;

        public ScriptTraceElement(String file, int line) {
            if (file == null) {
                throw new NullPointerException("file");
            }
            this.file = file;
            this.line = line;
        }

        public String getFile() {
            return file;
        }

        public int getLine() {
            return line;
        }

        @Override
        public String toString() {
            return file + ":" + line;
        }
    }

    /**
     * Return an array of {@link ScriptTraceElement}s that represents the script
     * stack frames for the current thread.
     *
     * @return returns a script stack trace for the current thread
     */
    public static ScriptTraceElement[] getStackTrace() {
        LinkedList<ScriptTraceElement> stack = new LinkedList<>();

        CharArrayWriter writer = new CharArrayWriter();
        try {
            Context.throwAsScriptRuntimeEx(new RuntimeException());
        } catch (Throwable t) {
            t.printStackTrace(new PrintWriter(writer));
        }

        String s = writer.toString();
        int open = -1;
        int close = -1;
        int colon = -1;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ':') {
                colon = i;
            } else if (c == '(') {
                open = i;
            } else if (c == ')') {
                close = i;
            } else if (c == '\n' && open != -1 && close != -1 && colon != -1
                    && open < colon && colon < close) {
                String file = s.substring(open + 1, colon);
                if (!file.endsWith(".java")) {
                    int line = -1;
                    String lineStr = s.substring(colon + 1, close);
                    try {
                        line = Integer.parseInt(lineStr);
                        if (line < 0) {
                            line = 0;
                        }
                        stack.add(new ScriptTraceElement(file, line));
                    } catch (NumberFormatException e) {
                    }
                }
                open = close = colon = -1;
            }
        }

        return stack.toArray(new ScriptTraceElement[stack.size()]);
    }
}
