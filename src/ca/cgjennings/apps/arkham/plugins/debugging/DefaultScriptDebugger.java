package ca.cgjennings.apps.arkham.plugins.debugging;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.TextEncoding;
import ca.cgjennings.apps.arkham.plugins.NoOpErrorReporter;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.apps.arkham.plugins.debugging.DebuggingCallback.DebugEventListener;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.tools.debugger.Dim.SourceInfo;
import org.mozilla.javascript.tools.debugger.Dim.StackFrame;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import resources.CacheMetrics;
import resources.ResourceKit;
import resources.Settings;

/**
 * The default debugger for scripts. This is a client-server implementation for
 * which this is the server. The matching client application is implemented in
 * the {@link Client} class. When debugging is enabled, the server will accept
 * network connections that use its debugging protocol and respond to the
 * commands enumerated by {@link Command}.
 *
 * <p>
 * <b>Protocol Summary:</b> Each command is sent as a sequence of UTF-8 text
 * lines. The first line must be the magic value <tt>SEDP3</tt>. The next line
 * is the command name, as given by {@code Command.name()}. This is
 * followed by zero or more lines, where each line represents one argument. The
 * server will reply with either <tt>SEDP3 OK</tt> and the reply to the command
 * (if any), or else <tt>SEDP3 ERR</tt> and an error message.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see ScriptDebugging
 */
public final class DefaultScriptDebugger {

    public static String getDescription() {
        return "Strange Eons default client/server debugger, version 3";
    }

    private DefaultScriptDebugger() {
        if (DebuggingCallback.getCallback() == null) {
            ScriptMonkey sm = new ScriptMonkey("<debugger bootstrap>");
            sm.eval(
                    "// This script ensures that the debugging engine has started"
            );

            // invariant check: server assumes that this is non-null
            if (DebuggingCallback.getCallback() == null) {
                //throw new AssertionError( "null DebuggingCallback" );
                Context.enter();
                try {
                    DebuggingCallback.create(Context.getCurrentContext().getFactory());
                } finally {
                    Context.exit();
                }
            }
        }

        dc = DebuggingCallback.getCallback();

        port = Settings.getShared().getInt("script-debug-port", 8888);

        serverThread = new Thread(interfaceAdapter, "SE Debugger (Server Thread)");
        serverThread.setDaemon(true);
        serverThread.start();

        StrangeEons.getApplication().addExitTask(new Runnable() {
            @Override
            public void run() {
                uninstall();
            }

            @Override
            public String toString() {
                return "stop script debug server if running";
            }
        });
    }

    private static volatile DefaultScriptDebugger theDebugger;

    /**
     * Installs the script debugger if it is not already installed. Once this
     * returns, the debug server will be available to debug scripts. The
     * debugger will be started automatically at application startup if user
     * preferences direct this; however, it can also be started at any time in
     * an already-running instance of the application.
     */
    public static void install() {
        // make sure there is no race to install
        synchronized (DefaultScriptDebugger.class) {
            if (theDebugger == null) {
                theDebugger = new DefaultScriptDebugger();
            } else {
                throw new IllegalStateException("debugger already installed");
            }
        }
    }

    /**
     * Uninstall the debugger. The debugger is not intended to be started and
     * restarted in a single session; this method is meant to be called at
     * application shutdown. There is normally no need to call this directly as
     * it will be called as part of normal application shutdown.
     */
    public static void uninstall() {
        synchronized (DefaultScriptDebugger.class) {
            if (theDebugger != null) {
                theDebugger.unload();
                DebuggingCallback.dispose();
                theDebugger = null;
            }
        }
    }

    /**
     * Returns {@code true} if the debugger has been installed.
     *
     * @return {@code true} if debugging is available
     */
    public static boolean isInstalled() {
        synchronized (DefaultScriptDebugger.class) {
            return theDebugger != null;
        }
    }

    /** Return the shared instance, or null. */
    static DefaultScriptDebugger getInstance() {
        synchronized (DefaultScriptDebugger.class) {
            return theDebugger;
        }
    }

    /**
     * Returns {@code true} if a client is connected to the debugger. Since
     * the client does not maintain a continuous connection, this method cannot
     * be guaranteed to be accurate. It works by tracking the last time that a
     * client connected to the server, and returning {@code true} if the
     * last connection was in the near past.
     *
     * @return {@code true} if a client application is probably available
     */
    public static boolean isClientConnected() {
        synchronized (DefaultScriptDebugger.class) {
            if (theDebugger != null) {
                final long now = System.nanoTime();
                final long then = theDebugger.lastServiceTimestamp;
                return now - then < 1_500_000_000L;
            }
        }
        return false;
    }

    /**
     * If the debugger is running, the script identified by this resource is
     * loaded and added to the debugger. This allows breakpoints to be set on
     * the script even if it has not yet been run. If the debugger is not
     * installed, this method does nothing.
     *
     * @param location the resource that contains the script
     * @param source the text to use for the identified resource
     */
    public static void preloadTopLevelScript(String location, String source) {
        if (location == null) {
            throw new NullPointerException("location");
        }
        if (source == null) {
            throw new NullPointerException("source");
        }
        DefaultScriptDebugger dg = theDebugger;
        if (dg == null) {
            return;
        }

        // check if already loaded
        if (dg.dc.getSourceInfoForScript(location) != null) {
            return;
        }

        // add the script to determine breakpoints, etc.
        Context cx = Context.enter();
        try {
            ErrorReporter old = cx.getErrorReporter();
            try {
                cx.setErrorReporter(dg.preloadReporter);
                Script node = cx.compileString(source, location, 1, null);
                cx.getDebugger().getFrame(cx, Context.getDebuggableView(node));
            } catch (EvaluatorException ex) {
            } finally {
                cx.setErrorReporter(old);
            }
        } finally {
            Context.exit();
        }
    }
    private final NoOpErrorReporter preloadReporter = new NoOpErrorReporter();

    private void unload() {
        if (serverThread != null) {
            int attempts = 0;
            while (serverThread.isAlive()) {
                if (attempts >= 5 && attempts % 5 == 0) {
                    StrangeEons.log.warning("debug server taking longer than usual to shutdown");
                }
                serverThread.interrupt();
                try {
                    serverThread.join(1_000);
                } catch (InterruptedException e) {
                }
                ++attempts;
            }
            serverThread = null;

            if (uiControl != null) {
                uiControl.uninstall();
            }
        }
    }

    private int port = 8888;
    private String host;
    private Thread serverThread;
    private ServerSocket socket;
    private DebuggingCallback dc;
    private DebuggerFeedbackProvider uiControl;

    /**
     * An adapter that forwards events to private methods in the outer class.
     */
    private class InterfaceAdapter implements DebugEventListener, Runnable {

        @Override
        public void updateSourceText(SourceInfo sourceInfo) {
            DefaultScriptDebugger.this.updateSourceText(sourceInfo);
        }

        @Override
        public void enterInterrupt(StackFrame lastFrame, String threadTitle, String alertMessage) {
            DefaultScriptDebugger.this.enterInterrupt(lastFrame, threadTitle, alertMessage);
        }

        @Override
        public void run() {
            runServer();
        }
    }
    private final InterfaceAdapter interfaceAdapter = new InterfaceAdapter();

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    /**
     * Create a server socket for the requested port (0 for any available port).
     * If user setting {@code enable-remote-debugging} is
     * {@code false}, the server socket will use the loopback address.
     *
     * @param port port to listen on, or 0 for any available
     * @return a server socket for the requested port, either on loopback or the
     * on the host's network
     * @throws IOException if an error occurs; typically, this means that the
     * port is in use
     */
    private static ServerSocket createServerSocket(int port) throws IOException {
        if (Settings.getUser().getYesNo("enable-remote-debugging")) {
            return new ServerSocket(port, 50, InetAddress.getLocalHost());
        } else {
            return new ServerSocket(port, 50, InetAddress.getLoopbackAddress());
        }
    }

    private void runServer() {
        try {
            try {
                socket = createServerSocket(port);
            } catch (IOException portInUse) {
                socket = createServerSocket(0);
                port = socket.getLocalPort();
                StrangeEons.log.log(Level.WARNING, "default debug server port in use; switched to port {0}", port);
            }
            socket.setSoTimeout(1_000);
        } catch (Exception e) {
            error("failed to start\n" + e);
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ce) {
                }
            }
            return;
        }

        dc.setListener(interfaceAdapter);

        host = socket.getInetAddress().getHostName();

        if (uiControl != null) {
            uiControl.uninstall();
        }
        uiControl = new DebuggerFeedbackProvider(this);
        uiControl.install();

        Socket client = null;
        for (;;) {
            try {
                client = socket.accept();
            } catch (SocketTimeoutException e) {
            } catch (IOException e) {
                error("exception while waiting for connection " + e);
            }
            if (Thread.interrupted()) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
                return;
            }
            if (client != null) {
                try {
                    IsolatedServerThread t = new IsolatedServerThread(client);
                    t.start();
                    try {
                        t.join(TIMEOUT);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    if (t.isAlive()) {
                        t.interrupt();
                    } else {
                        if (t.ioe != null) {
                            throw t.ioe;
                        }
                    }
                } catch (IOException t) {
                    StrangeEons.log.log(Level.SEVERE, "exception while serving request", t);
                } finally {
                    try {
                        client.close();
                    } catch (IOException e) {
                        StrangeEons.log.log(Level.WARNING, null, e);
                    }
                }
                client = null;
            }
        }
    }

    /**
     * Send error message to client.
     *
     * @param w the writer for the client connection
     * @param error the error message
     * @throws IOException
     */
    private void errorReply(PrintWriter w, String error) throws IOException {
        w.println("SEDP3 ERR");
        if (error != null) {
            w.println(error);
        }
    }

    private void service(Socket client, IsolatedServerThread thread) throws IOException {
        PrintWriter w = null;
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(client.getInputStream(), TextEncoding.DEBUGGER_CS));
            w = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), TextEncoding.DEBUGGER_CS)));

            String protocol = r.readLine();
            if (!"SEDP3".equals(protocol)) {
                return;
            }

            String request = r.readLine();

            if (request == null) {
                errorReply(w, "missing command name");
                return;
            }

            // identify the command
            Command command;
            try {
                command = Command.valueOf(request);
            } catch (IllegalArgumentException e) {
                if (request.equalsIgnoreCase("adj")) {
                    w.write(ProjectUtilities.getResourceText("/ca/cgjennings/apps/arkham/plugins/debugging/server-test", null));
                    w.flush();
                    return;
                }
                errorReply(w, "unknown command: " + request);
                return;
            }

            // get arguments for the command
            LinkedList<String> results = new LinkedList<>();
            String line;
            while (results.size() < command.getArgCount() && (line = r.readLine()) != null) {
                results.add(line);
            }
            String[] args = results.toArray(new String[results.size()]);

            if (args.length != command.getArgCount()) {
                errorReply(w, "wrong number of arguments to " + command + ": " + args.length);
                return;
            }

            String reply;
            reply = serviceImpl(command, args, w);
            if (reply != null) {
                w.println("SEDP3 OK");
                w.write(reply);
            }
            w.flush();
        } catch (Throwable t) {
            if (t instanceof ThreadDeath) {
                throw (ThreadDeath) t;
            }
            // fallback catch for all errors to prevent killing the debugger
            StrangeEons.log.log(Level.SEVERE, "uncaught exception while handling request", t);
            if (w != null) {
                errorReply(w, "uncaught exception while handling request");
                t.printStackTrace(w);
            }
        } finally {
            lastServiceTimestamp = System.nanoTime();
        }
    }

    private volatile long lastServiceTimestamp = 0L;

    /**
     * Execute the client command, appending the result to {@code apply}.
     *
     * @param isIsolated {@code true} if this method is being called from
     * an isolated thread
     * @param command the command to execute
     * @param args the arguments for the command
     * @param errorWriter the writer needed to pass to {@code errorReply}
     * if an error occurs
     * @return the string to reply with (without header), or {@code null}
     * if an error occurred and was handled
     */
    private String serviceImpl(Command command, String[] args, PrintWriter errorWriter) throws Throwable {
        StringBuilder reply;
        SourceInfo info;

        switch (command) {
            case PROBE:
                return String.valueOf(frameUpdates.get());

            case SERVERINFO: {
                String testBundles = StrangeEons.getApplication().getCommandLineArguments().plugintest;
                return guessProcessId() + '\n' +
                        Integer.toHexString(StrangeEons.getApplication().hashCode()) + '\n' +
                        StrangeEons.getBuildNumber() + '\n' +
                        StrangeEons.getVersionString() + '\n' +
                        (testBundles == null ? "" : testBundles) + '\n'
                ;
            }

            case INTERRUPTED:
                synchronized (interruptMonitor) {
                    if (dc.isInterrupted()) {
                        return "1\n"
                                + (interruptedThread == null ? "" : Command.escapeProtocolText(interruptedThread)) + "\n"
                                + (interruptCause == null ? "" : Command.escapeProtocolText(interruptCause));
                    }
                    return "0";
                }

            case STOP:
                System.exit(20);
                return ""; // never reached, but needed to make compiler happy

            case BREAK:
                dc.setBreak();
                return "";

            case CONTINUE:
                dc.go();
                return "";

            case STEPOVER:
                dc.stepOver();
                return "";

            case STEPINTO:
                dc.stepInto();
                return "";

            case STEPOUT:
                dc.stepOut();
                return "";

            case FILELIST:
                int listed = 0;
                String[] names = dc.getTopLevelScriptURLs();
                reply = new StringBuilder(names.length * 20);
                for (int i = 0; i < names.length; ++i) {
                    if (names[i].equals("<debugger bootstrap>") || names[i].startsWith(".")) {
                        continue;
                    }
                    if (listed++ > 0) {
                        reply.append('\n');
                    }
                    reply.append(names[i]);
                }
                return reply.toString();

            case SOURCE:
                info = getSourceInfo(errorWriter, args[0]);
                if (info == null) {
                    return null;
                }
                return info.source();

            case BREAKPOINTS:
                info = getSourceInfo(errorWriter, args[0]);
                if (info == null) {
                    return null;
                }
                int lines = 1;
                String source = info.source();
                for (int i = 0; i < source.length(); ++i) {
                    if (source.charAt(i) == '\n') {
                        ++lines;
                    }
                }

                reply = new StringBuilder(lines * 2);
                listed = 0;
                for (int i = 0; i < lines; ++i) {
                    if (info.breakableLine(i + 1)) {
                        if (listed++ > 0) {
                            reply.append('\n');
                        }
                        reply.append(i).append('\n');
                        if (info.breakpoint(i + 1)) {
                            reply.append('X');
                        } else {
                            reply.append('-');
                        }
                    }
                }
                return reply.toString();

            case TOGGLEBREAK:
                info = getSourceInfo(errorWriter, args[0]);
                if (info == null) {
                    return null;
                }
                int lineNum;
                try {
                    lineNum = Integer.parseInt(args[1]) + 1;
                } catch (NumberFormatException e) {
                    errorReply(errorWriter, "invalid line number: " + args[1]);
                    return null;
                }
                if (!info.breakableLine(lineNum)) {
                    errorReply(errorWriter, "not a valid breakpoint: " + (lineNum - 1));
                    return null;
                }
                boolean val = info.breakpoint(lineNum);
                info.breakpoint(lineNum, !val);
                return "";

            case CLEARBREAKPOINTS:
                info = getSourceInfo(errorWriter, args[0]);
                if (info == null) {
                    return null;
                }
                info.removeAllBreakpoints();
                return "";

            case BREAKSTATUS:
                return (dc.getBreakOnEnter() ? "1" : "0")
                        + (dc.getBreakOnReturn() ? "\n1" : "\n0")
                        + (dc.getBreakOnExceptions() ? "\n1" : "\n0")
                        + (dc.getBreakOnStatement() ? "\n1" : "\n0");

            case BREAKONENTER:
                dc.setBreakOnEnter(parseBool(args[0]));
                return "";

            case BREAKONEXIT:
                dc.setBreakOnReturn(parseBool(args[0]));
                return "";

            case BREAKONTHROW:
                dc.setBreakOnExceptions(parseBool(args[0]));
                return "";

            case BREAKONDEBUGGER:
                dc.setBreakOnStatement(parseBool(args[0]));
                return "";

            case CALLSTACK:
                StackFrame[] stack = dc.getStack();
                reply = new StringBuilder(stack.length * 26);
                for (int i = 0; i < stack.length; ++i) {
                    if (i > 0) {
                        reply.append('\n');
                    }
                    reply.append(stack[i].getUrl())
                            .append('\n')
                            .append(stack[i].getLineNumber());
                }
                return reply.toString();

            case EVAL:
                int frame = Integer.parseInt(args[0]);
                String eval;
                if (frame < 1) {
                    eval = dc.eval(args[1], dc.getStackFrame());
                } else {
                    stack = dc.getStack();
                    if (frame >= stack.length) {
                        frame = stack.length - 1;
                    }
                    eval = dc.eval(args[1], stack[frame]);
                }
                return eval;

            case SCOPE:
                StackFrame sf = parseStackFrame(args[0]);
                if (sf != null) {
                    reply = new StringBuilder(128);

                    // special case: this allows us to get the types of the
                    // scope and this objects
                    if (args[1].equals("<root>")) {
                        Object rootObj = sf.scope();
                        reply.append("<scope>\0").append(dc.getObjectIds(rootObj).length).append(':').append(objectToType(rootObj));
                        rootObj = sf.thisObj();
                        reply.append("\n<this>\0").append(dc.getObjectIds(rootObj).length).append(':').append(objectToType(rootObj));
                        return reply.toString();
                    }

                    Object scopeObj = getScopeObject(sf, args[1]);
                    Object[] ids = dc.getObjectIds(scopeObj);
                    for (int i = 0; i < ids.length; ++i) {
                        if (i > 0) {
                            reply.append('\n');
                        }
                        reply.append(ids[i].toString());
                        Object property = dc.getObjectProperty(scopeObj, ids[i]);
                        Object[] subIds = dc.getObjectIds(property);
                        reply.append('\0').append(subIds.length);
                        String type = objectToType(property);
                        if (type != null) {
                            reply.append(':').append(type);
                        }
                    }
                    return reply.toString();
                }
                return null;

            case SCOPEEVAL:
                sf = parseStackFrame(args[0]);
                if (sf != null) {
                    Object scopeObj = getScopeObject(sf, args[1]);
                    return objectToString(scopeObj);
                }
                return null;

            case INFOTABLELIST:
                names = Tables.getTableNames();
                reply = new StringBuilder(names.length * 20);
                for (int i = 0; i < names.length; ++i) {
                    if (i > 0) {
                        reply.append('\n');
                    }
                    reply.append(names[i]);
                }
                return reply.toString();

            case INFOTABLE:
                reply = new StringBuilder(2_048);
                InfoTable it = null;
                try {
                    it = Tables.generate(args[0]);
                    if (it == null) {
                        it = InfoTable.errorTable("No such table: " + args[0], null);
                    }
                } catch (Throwable t) {
                    it = InfoTable.errorTable("Exception while generating table", t);
                }
                if (it == null) {
                    throw new AssertionError();
                }
                it.serialize(reply);
                return reply.toString();

            case CACHEMETRICS:
                int toClear = Integer.parseInt(args[0]);
                CacheMetrics[] cm = ResourceKit.getRegisteredCacheMetrics();
                if (toClear >= 0 && toClear < cm.length) {
                    cm[toClear].clear();
                }
                reply = new StringBuilder(120 * cm.length + 16);
                for (CacheMetrics m : cm) {
                    if (toClear == -2) {
                        m.clear();
                    }
                    if (reply.length() > 0) {
                        reply.append('\n');
                    }
                    reply.append(m.toString().replace('\n', ' ')).append(": ")
                            .append(m.status().replace('\n', ' ')).append('\n')
                            .append(m.getContentType().getSimpleName()).append('\n')
                            .append(m.isClearSupported() ? 'Y' : 'N');
                }
                return reply.toString();

            default:
                errorReply(errorWriter, "command not implemented: " + command);
                return null;
        }
    }

    private static final int TIMEOUT = 10 * 1_000;

    /**
     * A thread used to execute commands that might deadlock or contain infinite
     * loops.
     */
    private class IsolatedServerThread extends Thread {

        private final Socket client;
        private IOException ioe;

        public IsolatedServerThread(Socket client) {
            super("Isolated script debug thread <" + client.toString() + ">");
            this.client = client;
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                service(client, this);
            } catch (IOException e) {
                ioe = e;
            }
        }
    }

    private String objectToString(Object o) {
        String val = dc.getObjectAsString(o);
        if (o instanceof Function) {
            val = val.trim();
        }
        return val;
    }

    private String objectToType(Object o) {
        if (o == null || o == Undefined.instance) {
            return null;
        }
        String type = null;
        if (o instanceof Scriptable) {
            type = ((Scriptable) o).getClassName();
            if (o instanceof NativeJavaObject) {
                type += " (" + ((NativeJavaObject) o).unwrap().getClass().getName() + ")";
            }
        }
        if (type == null || type.isEmpty()) {
            type = o.getClass().getName();
        }
        return type;
    }

    private StackFrame parseStackFrame(String id) {
        int frame = Integer.parseInt(id);
        StackFrame sf;
        if (frame < 1) {
            sf = dc.getStackFrame();
        } else {
            StackFrame[] stack = dc.getStack();
            if (frame >= stack.length) {
                frame = stack.length - 1;
            }
            sf = stack[frame];
        }
        return sf;
    }

    private Object getScopeObject(StackFrame sf, String descriptor) {
        Object scopeObj = sf.scope();
        String[] nodes = descriptor.split("\0");
        if (nodes[0].equals("<this>")) {
            scopeObj = sf.thisObj();
        }
        for (int i = 1; i < nodes.length; ++i) {
            scopeObj = dc.getObjectInScope(scopeObj, nodes[i]);
        }
        return scopeObj;
    }

    private SourceInfo getSourceInfo(PrintWriter w, String url) throws IOException {
        SourceInfo info = dc.getSourceInfoForScript(url);
        if (info == null) {
            errorReply(w, "unknown script: " + url);
            return null;
        }
        return info;
    }

    private static boolean parseBool(String arg) {
        return arg != null && arg.equals("1");
    }

    private void error(String s) {
        StrangeEons.log.log(Level.SEVERE, "Debug Server Error: {0}", s);
    }

    /////////////////
    // DebugEventListener interface implementation (called from DebuggingCallback)
    /////////////////
    private void updateSourceText(SourceInfo sourceInfo) {
    }

    private void enterInterrupt(StackFrame lastFrame, String threadTitle, String alertMessage) {
        synchronized (interruptMonitor) {
            interruptedThread = threadTitle;
            interruptCause = alertMessage;
            frameUpdates.incrementAndGet();
            interruptMonitor.notifyAll();
        }
    }
    private final Object interruptMonitor = new Object();
    private volatile AtomicInteger frameUpdates = new AtomicInteger(0);
    private String interruptedThread, interruptCause;

    protected static String getSimpleScriptURL(String name) {
        if (name.startsWith("jar:")) {
            int bang = name.indexOf('!');
            if (bang >= 0) {
                name = name.substring(bang + 1);
            }
        }
        // pick 1 of this or prev block: convert res:// to /resources
        if (name.startsWith("res:///")) {
            name = name.substring("res:/".length());
        } else if (name.startsWith("res://")) {
            name = "/resources" + name.substring("res://".length());
        }

        name = name.trim();
        if (name.length() == 0) {
            name = "/";
        }
        return Command.escapeHTML(name); // still?
    }

    private static String guessProcessId() {
        try {
            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
            return jvmName.split("@")[0];
        } catch (Exception ex) {
            return "?";
        }
    }
}
