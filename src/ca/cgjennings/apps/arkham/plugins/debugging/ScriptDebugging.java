package ca.cgjennings.apps.arkham.plugins.debugging;

import ca.cgjennings.apps.arkham.DefaultCommandFormatter;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.Subprocess;
import ca.cgjennings.apps.arkham.TextEncoding;
import ca.cgjennings.apps.arkham.plugins.engine.SEScriptEngineFactory;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import ca.cgjennings.util.CommandFormatter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import resources.Settings;

/**
 * Controls whether script debugging is enabled. Also allows use of alternative
 * debugger implementations. Only one script debugging system may be in use at a
 * time. This class allows you to install/uninstall different implementations.
 *
 * <p>
 * Debugger implementations must provide a class that implements the
 * {@link DebuggerInstaller} interface. This class acts as a controller for
 * their implementation: installing and uninstalling on demand and providing
 * high-level access to its features.
 *
 * <p>
 * A default debugging engine is included that provides debugging services
 * through a {@linkplain DefaultScriptDebugger server} running within the main
 * application, paired with a separate {@linkplain Client client} application.
 * The initial installer implementation is based on this default implementation,
 * so the built-in debugging system can be started just by calling
 * {@link #install()}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class ScriptDebugging {

    private ScriptDebugging() {
    }
    private static boolean installed = false;

    /**
     * Sets the installer that is used to install a debugger. If a debugger is
     * currently installed, it will be uninstalled before switching installers.
     * A new debugger is not automatically installed.
     *
     * @param newInstaller the installer to use
     */
    public static synchronized void setInstaller(DebuggerInstaller newInstaller) {
        if (newInstaller == null) {
            throw new NullPointerException("newInstaller");
        }
        if (installer == newInstaller) {
            return;
        }
        if (installed) {
            uninstall();
        }
        installer = newInstaller;
    }

    /**
     * Sets the installer used to install a debugger by its class name.
     *
     * @param className the fully qualified name of a class with a no-arg public
     * constructor that implements the {@link DebuggerInstaller} interface
     * @throws IllegalArgumentException if an installer cannot be created from
     * the class name
     */
    public static synchronized void setInstaller(String className) {
        try {
            DebuggerInstaller i = (DebuggerInstaller) Class.forName(className).getConstructor().newInstance();
            setInstaller(i);
        } catch (Throwable t) {
            throw new IllegalArgumentException("unable to create " + className, t);
        }
    }

    /**
     * Returns the current installer.
     *
     * @return the installer used to create a debugger
     */
    public static synchronized DebuggerInstaller getInstaller() {
        return installer;
    }

    /**
     * Install script debugging using the current installer.
     */
    public synchronized static void install() {
        if (installed) {
            uninstall();
        }
        installed = true;
        try {
            installer.install();
        } catch (RuntimeException e) {
            installed = false;
        }
        for (String s : ppScripts) {
            preprocessScript(s);
        }
    }

    /**
     * Uninstall script debugging using the current installer.
     */
    public synchronized static void uninstall() {
        if (!installed) {
            return;
        }
        installer.uninstall();
        installed = false;
    }

    /**
     * Returns {@code true} if a debugger is currently installed.
     *
     * @return whether a debugger is installed
     */
    public synchronized static boolean isInstalled() {
        return installed;
    }

    /**
     * This method can be called when a script has been discovered but has not
     * yet been run in order to make the debugger aware of the script. If
     * supported by the debugger, this allows breakpoints to be set on the
     * script before it is run the first time. This method locates the script
     * automatically; it can handle plug-in root file entries and class map
     * entries, as well as
     * <tt>useLibrary</tt> sources.
     *
     * @param location the resource that contains the script
     */
    public synchronized static void preprocessScript(String location) {
        if (location == null) {
            throw new NullPointerException("location");
        }

        ppScripts.add(location);
        if (!installed) {
            return; // no installer, just remember it for now
        }
        String source = null;
        if (location.startsWith("script:")) {
            location = location.substring("script:".length());
            try {
                source = ProjectUtilities.getResourceText(location, TextEncoding.SOURCE_CODE);
            } catch (IOException e) {
            }
        } else if (location.startsWith("diy:")) {
            location = location.substring("diy:".length());
            try {
                source = ProjectUtilities.getResourceText(location, TextEncoding.SOURCE_CODE);
            } catch (IOException e) {
            }
        } else {
            try {
                source = ScriptMonkey.getLibrary(location);
            } catch (IOException e) {
            }
        }

        if (source != null) {
            installer.preprocessScript(location, source);
            // look for useLibrary calls and preload those as well, recursively
            Matcher m = Pattern.compile("use[lL]ibrary\\s*\\(\\s*['\"]([^'\"])\\s*\\)").matcher(source);
            while (m.find()) {
                preprocessScript(m.group(1));
            }
        } else {
            System.err.println("Preload failed: " + location);
        }
    }
    private static Set<String> ppScripts = new LinkedHashSet<>();

    /**
     * Set a break as soon as possible by calling the installer's
     * {@link DebuggerInstaller#setBreak()} method. If a debugger is not
     * installed, the current debugger will be installed first.
     */
    public static void setBreak() {
        if (!installed) {
            install();
        }
        installer.setBreak();
    }

    /**
     * If debugging is installed, calls its {@code prepareToEnterContext}
     * method.
     */
    public static synchronized void prepareToEnterContext() {
        if (installed) {
            installer.prepareToEnterContext();
        }
    }

    /**
     * Implemented by classes that can install a script debugger. This is
     * similar to a factory, but only one debugger can be installed at a time.
     * This interface allows a debugger to be installed or uninstalled, and
     * provides very high-level access to the debugger once installed.
     */
    public interface DebuggerInstaller {

        /**
         * Called to start the debugging service.
         */
        public void install();

        /**
         * Called to stop the debugging service.
         */
        public void uninstall();

        /**
         * Called to allow the debugger to process a script that has not been
         * compiled or run yet, but that the user of the debugger might wish to
         * have available (for example, to set breakpoints before the script is
         * actually run).
         *
         * @param location the file or other identifier for the script
         * @param scriptSource the source code for the script
         */
        public void preprocessScript(String location, String scriptSource);

        /**
         * Called before entering a thread context.
         */
        public void prepareToEnterContext();

        /**
         * Set a breakpoint at the earliest opportunity. The effect of calling
         * this when the matching debugger is not installed is undefined.
         */
        public void setBreak();

        /**
         * Start or display the debug client window.
         */
        public void startClient() throws IOException;

        /**
         * Returns {@code true} if the debugging client is currently
         * running.
         *
         * @return {@code true} if the client is active
         */
        public boolean isClientRunning();
    }

    private static DebuggerInstaller installer = new DebuggerInstaller() {
        @Override
        public void install() {
            DefaultScriptDebugger.install();
        }

        @Override
        public void uninstall() {
            DefaultScriptDebugger.uninstall();
        }

        @Override
        public void preprocessScript(String location, String scriptSource) {
            try {
                DefaultScriptDebugger.preloadTopLevelScript(location, scriptSource);
            } catch (Throwable t) {
                StrangeEons.log.log(Level.WARNING, "exception while preprocessing script for debugger", t);
            }
        }

        @Override
        public void prepareToEnterContext() {
            DebuggingCallback.create(SEScriptEngineFactory.getContextFactory());
        }

        @Override
        public void setBreak() {
            DebuggingCallback dc = DebuggingCallback.getCallback();
            if (dc != null) {
                dc.setBreak();

                if (Settings.getShared().getYesNo("script-debug-client-autostart")) {
                    try {
                        startClient();
                    } catch (IOException e) {
                        StrangeEons.log.log(Level.WARNING, "failed to start client with default command", e);
                    }
                }
            }
        }

        @Override
        public String toString() {
            return DefaultScriptDebugger.getDescription();
        }

        @Override
        public void startClient() throws IOException {
            if (isClientRunning()) {
                return;
            }

            final DefaultScriptDebugger server = DefaultScriptDebugger.getInstance();
            final String host = server.getHost();
            final String port = String.valueOf(server.getPort());

            String userOverride = Settings.getUser().get("script-debug-client-launch", null);
            if(userOverride != null) {
                CommandFormatter cf = new DefaultCommandFormatter();
                cf.setVariable('h', host);
                cf.setVariable('p', port);
                client = new Subprocess(cf.formatCommand(userOverride));
            } else {
                client = Subprocess.launch(
                        "-Xshare:off", "-Xms64m",
                        "debugger",
                        "--host", host,
                        "--port", port
                );
            }
            client.setSurvivor(true);
            client.setExitCodeShown(false);
            client.setStreamIORedirected(false);
            client.start();
        }

        private volatile Subprocess client;

        @Override
        public boolean isClientRunning() {
            Subprocess theClient = client;
            return (theClient != null && theClient.isRunning()) || DefaultScriptDebugger.isClientConnected();
        }
    };
}
