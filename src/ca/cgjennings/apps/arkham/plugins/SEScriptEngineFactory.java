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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ErrorReporter;
import ca.cgjennings.script.util.*;
import java.util.*;
import javax.script.*;

/**
 * A factory for creating script engines for the JSR 223 specification
 * (Scripting for the Java Platform) that are capable of interpreting Strange
 * Eons scripts. This provides a lower-level interface to the scripting system
 * than a {@link ScriptMonkey}. It is only provided to allow access to the
 * script system via the {@code javax.scripting} API; plug-in developers
 * will have no need to use this as a general rule.
 *
 * @author Mike Grogan
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class SEScriptEngineFactory extends ScriptEngineFactoryBase {

    private static SEContextFactory contextFactory;

    private volatile static boolean warningsEnabled = true;
    private volatile static int optimizationLevel = 0;
    private volatile static boolean debugInfoEnabled = false;

    private static final List<String> names;
    private static final List<String> mimeTypes;
    private static final List<String> extensions;

    static {
        names = Collections.unmodifiableList(Arrays.asList(
                "rhino-se", "strange-rhino", "js", "JavaScript",
                "javascript", "ECMAScript", "ecmascript"
        ));
        mimeTypes = Collections.unmodifiableList(Arrays.asList(
                "application/javascript", "application/ecmascript",
                "text/javascript", "text/ecmascript"
        ));
        extensions = Collections.unmodifiableList(Arrays.asList(
                "js", "ajs"
        ));
    }

    /**
     * Creates a script engine factory for Strange Eons scripts.
     */
    public SEScriptEngineFactory() {
        synchronized (SEScriptEngineFactory.class) {
            if (!ContextFactory.hasExplicitGlobal()) {
                contextFactory = new SEContextFactory();
                ContextFactory.initGlobal(contextFactory);
            }
        }
    }

    @Override
    public List<String> getExtensions() {
        return extensions;
    }

    @Override
    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    @Override
    public List<String> getNames() {
        return names;
    }

    @Override
    public Object getParameter(String key) {
        switch (key) {
            case ScriptEngine.NAME:
                return "javascript";
            case ScriptEngine.ENGINE:
                return "Strange Rhino";
            case ScriptEngine.ENGINE_VERSION:
            case ScriptEngine.LANGUAGE_VERSION:
                return "1.7.13-SE";
            case ScriptEngine.LANGUAGE:
                return "ECMAScript";
            case "THREADING":
                return "MULTITHREADED";
            default:
                throw new IllegalArgumentException("Invalid key");
        }
    }

    @Override
    public ScriptEngine getScriptEngine() {
        SEScriptEngine ret = new SEScriptEngine();
        ret.setEngineFactory(this);
        return ret;
    }

    static class SEContextFactory extends ContextFactory {

        public SEContextFactory() {
        }

        @Override
        protected Context makeContext() {
            Context cx = super.makeContext();
            final int opt = getOptimizationLevel();
            cx.setOptimizationLevel(opt);
            cx.setLanguageVersion(Context.VERSION_ES6);

            cx.setGeneratingSource(opt < 2);

            cx.setInstructionObserverThreshold(0);
            cx.setGenerateObserverCount(false);

            if (opt == -1) {
                cx.setMaximumInterpreterStackDepth(1_000);
            }

            if (opt < 1 || isDebugInfoEnabled()) {
                cx.setGeneratingDebug(true);
            }

            if (warningsEnabled) {
                ErrorReporter er = WarningErrorReporter.getShared(
                        cx.getErrorReporter()
                );
                cx.setErrorReporter(er);
            }

            return cx;
        }

        @Override
        protected boolean hasFeature(Context cx, int featureIndex) {
            boolean enable;
            switch (featureIndex) {
                case Context.FEATURE_E4X:
                    enable = false;
                    break;
                case Context.FEATURE_LOCATION_INFORMATION_IN_ERROR:
                case Context.FEATURE_STRICT_EVAL:
                case Context.FEATURE_STRICT_VARS:
                case Context.FEATURE_STRICT_MODE:
                case Context.FEATURE_INTEGER_WITHOUT_DECIMAL_PLACE:
                    enable = true;
                    break;
                default:
                    enable = super.hasFeature(cx, featureIndex);
            }
            return enable;
        }
    }

    /**
     * Returns the context factory used by this factory to create scripting
     * contexts. This is needed by the script debugger; it is not useful to
     * plug-in developers.
     *
     * @return the script context factory
     */
    public static ContextFactory getContextFactory() {
        return contextFactory;
    }

    @Override
    public String getMethodCallSyntax(String obj, String method, String... args) {
        String ret = obj + "." + method + "(";
        int len = args.length;
        if (len == 0) {
            ret += ")";
            return ret;
        }

        for (int i = 0; i < len; i++) {
            ret += args[i];
            if (i != len - 1) {
                ret += ",";
            } else {
                ret += ")";
            }
        }
        return ret;
    }

    @Override
    public String getOutputStatement(String toDisplay) {
        return "print(" + toDisplay + ")";
    }

    @Override
    public String getProgram(String... statements) {
        int len = statements.length;
        String ret = "";
        for (int i = 0; i < len; i++) {
            ret += statements[i] + ";";
        }

        return ret;
    }

    /**
     * Returns {@code true} the scripting system will report warnings as
     * well as errors.
     *
     * @return {@code true} if warnings are reported
     * @see #setWarningReportingEnabled(boolean)
     */
    public static boolean isWarningReportingEnabled() {
        return warningsEnabled;
    }

    /**
     * Sets whether the scripting system will report warnings as well as errors.
     *
     * @param warningsEnabled {@code true} if warnings are reported
     * @see #isWarningReportingEnabled()
     */
    public static void setWarningReportingEnabled(boolean warningsEnabled) {
        SEScriptEngineFactory.warningsEnabled = warningsEnabled;
    }

    /**
     * Returns the optimization level that will be applied to scripts.
     *
     * @return the current optimization level
     * @see #setOptimizationLevel(int)
     */
    public static int getOptimizationLevel() {
        return optimizationLevel;
    }

    /**
     * Sets the optimization level that will be applied to scripts. Valid values
     * include -1 (interpret only, no compilation), 0 (compile without
     * optimization), or 1 through 9 (enable successive degrees of optimization;
     * in practice not all levels may have distinct effects).
     *
     * @param optimizationLevel the new optimization level
     * @throws IllegalArgumentException if the new level is outside of the legal
     * range
     * @see #getOptimizationLevel()
     */
    public static void setOptimizationLevel(int optimizationLevel) {
        if (optimizationLevel < -1 || optimizationLevel > 9) {
            throw new IllegalArgumentException("invalid optimization level: " + optimizationLevel);
        }
        SEScriptEngineFactory.optimizationLevel = optimizationLevel;
    }

    /**
     * Returns whether debugging information will be generated for parsed
     * scripts.
     *
     * @return {@code true} if generation of debugging info is enabled
     */
    public static boolean isDebugInfoEnabled() {
        return debugInfoEnabled;
    }

    /**
     * Sets whether debugging information will be generated for parsed scripts.
     * If this is enabled, it may limit the optimization level that is
     * effectively applied to executed scripts.
     *
     * @param debugInfoEnabled {@code true} to enable generation of
     * debugging info
     */
    public static void setDebugInfoEnabled(boolean debugInfoEnabled) {
        SEScriptEngineFactory.debugInfoEnabled = debugInfoEnabled;
    }
}
