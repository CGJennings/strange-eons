package ca.cgjennings.apps.arkham.plugins.debugging;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.script.mozilla.javascript.ContextFactory;
import ca.cgjennings.script.mozilla.javascript.Scriptable;
import ca.cgjennings.script.mozilla.javascript.tools.debugger.Dim;
import ca.cgjennings.script.mozilla.javascript.tools.debugger.Dim.ContextData;
import ca.cgjennings.script.mozilla.javascript.tools.debugger.Dim.SourceInfo;
import ca.cgjennings.script.mozilla.javascript.tools.debugger.Dim.StackFrame;
import ca.cgjennings.script.mozilla.javascript.tools.debugger.GuiCallback;
import ca.cgjennings.script.mozilla.javascript.tools.debugger.ScopeProvider;

/**
 * Processes Rhino debugging events for the {@link ScriptDebugger}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
class DebuggingCallback {

    private Dim dim;
    private static volatile DebuggingCallback callback = null;

    public synchronized static void dispose() {
        DebuggingCallback dc = callback;
        callback = null;
        dc.dim.dispose();
    }

    public static synchronized DebuggingCallback create(ContextFactory factory) {
        if (callback == null) {
            callback = new DebuggingCallback(factory, (ScopeProvider) () -> {
                StrangeEons.log.warning("unexpected call to getScope()");
                return null;
            });
        }
        return callback;
    }

    public static DebuggingCallback getCallback() {
        return callback;
    }

    private DebuggingCallback(ContextFactory factory, Object scopeProvider) {
        StrangeEons.log.info("installed debugging callbacks");
        dim = new Dim();
        dim.setGuiCallback(new GuiCallback() {
            @Override
            public void updateSourceText(SourceInfo sourceInfo) {
                DebuggingCallback.this.updateSourceText(sourceInfo);
            }

            @Override
            public void enterInterrupt(StackFrame lastFrame, String threadTitle, String alertMessage) {
                DebuggingCallback.this.enterInterrupt(lastFrame, threadTitle, alertMessage);
            }

            @Override
            public boolean isGuiEventThread() {
                return false;
            }

            @Override
            public void dispatchNextGuiEvent() throws InterruptedException {
            }
        });
        dim.attachTo(factory);

        if (scopeProvider instanceof ScopeProvider) {
            dim.setScopeProvider((ScopeProvider) scopeProvider);
        } else {
            final Scriptable scope = (Scriptable) scopeProvider;
            dim.setScopeProvider(() -> scope);
        }
    }

    private volatile SourceInfo lastSourceInfo;
    private volatile StackFrame lastFrame;
    private volatile String lastThread;
    private volatile String lastAlertMessage;

    private void resetStackFrame() {
        lastFrame = null;
        lastThread = null;
        lastAlertMessage = null;
    }

    public boolean isInterrupted() {
        return lastFrame != null;
    }

    public void go() {
        if (isInterrupted()) {
            resetStackFrame();
            dim.setReturnValue(Dim.GO);
        }
    }

    public void stepOver() {
        if (isInterrupted()) {
            resetStackFrame();
            dim.setReturnValue(Dim.STEP_OVER);
        }
    }

    public void stepInto() {
        if (isInterrupted()) {
            resetStackFrame();
            dim.setReturnValue(Dim.STEP_INTO);
        }
    }

    public void stepOut() {
        if (isInterrupted()) {
            resetStackFrame();
            dim.setReturnValue(Dim.STEP_OUT);
        }
    }

    public void setBreak() {
        //resetStackFrame();
        dim.setBreak();
    }

    public SourceInfo getLastSourceInfo() {
        return lastSourceInfo;
    }

    public StackFrame getStackFrame() {
        return lastFrame;
    }

    public String getInterruptedThread() {
        return lastThread;
    }

    public String getInterruptedMessage() {
        return lastAlertMessage;
    }

    public String[] getTopLevelScriptURLs() {
        return dim.getTopLevelScriptURLs();
    }

    public SourceInfo getSourceInfoForScript(String url) {
        return dim.getSourceInfoForScript(url);
    }

    private boolean breakOnEnter = false;
    private boolean breakOnReturn = false;
    private boolean breakOnExceptions = false;

    public void setBreakOnEnter(boolean b) {
        breakOnEnter = b;
        dim.setBreakOnEnter(b);
    }

    public boolean getBreakOnEnter() {
        return breakOnEnter;
    }

    public void setBreakOnReturn(boolean b) {
        breakOnReturn = b;
        dim.setBreakOnReturn(b);
    }

    public boolean getBreakOnReturn() {
        return breakOnReturn;
    }

    public void setBreakOnExceptions(boolean b) {
        breakOnExceptions = b;
        dim.setBreakOnExceptions(b);
    }

    public boolean getBreakOnExceptions() {
        return breakOnExceptions;
    }

    public void setBreakOnStatement(boolean b) {
        dim.setBreakOnStatement(b);
    }

    public boolean getBreakOnStatement() {
        return dim.getBreakOnStatement();
    }

    public String eval(String expr, StackFrame frame) {
        return dim.eval(expr, frame);
    }

    public Object getObjectInScope(Object scope, String name) {
        Object result = "undefined";
        if (scope == null) {
            return result;
        }

        String remainder = null;
        int dot = name.indexOf('.');
        if (dot >= 0) {
            remainder = name.substring(dot + 1);
            name = name.substring(0, dot);
        }

        Object[] ids = getObjectIds(scope);
        for (int i = 0; i < ids.length; ++i) {
            if (ids[i].toString().equals(name)) {
                result = getObjectProperty(scope, ids[i]);
                break;
            }
        }

        if (remainder != null) {
            result = getObjectInScope(result, remainder);
        }

        return result;
    }

    public String getObjectAsString(Object obj) {
        try {
            if (obj == null) {
                return "null";
            }
            if (!(obj instanceof Scriptable)) {
                if (obj instanceof ca.cgjennings.script.mozilla.javascript.Undefined) {
                    return "undefined";
                }
                return obj.toString();
            }
            return dim.objectToString(obj);
        } catch (Throwable t) {
            return t.toString();
        }
    }

    public Object[] getObjectIds(Object obj) {
        return dim.getObjectIds(obj);
    }

    public Object getObjectProperty(Object obj, Object id) {
        try {
            return dim.getObjectProperty(obj, id);
        } catch (Throwable t) {
            return "<" + t.getClass().getSimpleName() + ">";
        }
    }

    public StackFrame[] getStack() {
        if (!isInterrupted()) {
            return new StackFrame[0];
        }
        ContextData cd = dim.currentContextData();
        if (cd == null) {
            return new StackFrame[0];
        }

        StackFrame[] frames = new StackFrame[cd.frameCount()];
        for (int i = 0; i < cd.frameCount(); ++i) {
            frames[i] = cd.getFrame(i);
        }
        return frames;
    }

    private void updateSourceText(SourceInfo sourceInfo) {
        lastSourceInfo = sourceInfo;
        if (getListener() != null) {
            getListener().updateSourceText(sourceInfo);
        }
    }

    private void enterInterrupt(StackFrame lastFrame, String threadTitle, String alertMessage) {
        this.lastFrame = lastFrame;
        this.lastThread = threadTitle;
        this.lastAlertMessage = alertMessage;
        if (getListener() != null) {
            getListener().enterInterrupt(lastFrame, threadTitle, alertMessage);
        }
    }

    private DebugEventListener listener;

    /**
     * @return the listener
     */
    public DebugEventListener getListener() {
        return listener;
    }

    /**
     * The debugger that will listen for updates.
     *
     * @param listener the listener to set
     */
    public void setListener(DebugEventListener listener) {
        this.listener = listener;
    }

    /**
     * A listener implemented by the debugger that will listen to this callback
     * for relevant updates.
     */
    public static interface DebugEventListener {

        public void updateSourceText(SourceInfo sourceInfo);

        public void enterInterrupt(StackFrame lastFrame, String threadTitle, String alertMessage);
    }
}
