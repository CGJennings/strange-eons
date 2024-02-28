package ca.cgjennings.apps.arkham;

import ca.cgjennings.algo.SplitJoin;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import ca.cgjennings.apps.arkham.plugins.InstalledPlugin;
import ca.cgjennings.apps.arkham.plugins.Plugin;
import ca.cgjennings.apps.arkham.plugins.PluginBundle;
import ca.cgjennings.apps.arkham.plugins.PluginContext;
import ca.cgjennings.apps.arkham.plugins.PluginContextFactory;
import ca.cgjennings.apps.arkham.plugins.PluginException;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.apps.arkham.plugins.StrangeEonsEvaluatorFactory;
import ca.cgjennings.apps.arkham.plugins.catalog.Catalog;
import ca.cgjennings.apps.arkham.plugins.catalog.Catalog.VersioningState;
import ca.cgjennings.apps.arkham.plugins.catalog.NetworkProxy;
import ca.cgjennings.apps.arkham.plugins.debugging.ScriptDebugging;
import ca.cgjennings.apps.arkham.project.MetadataSource;
import ca.cgjennings.apps.arkham.project.Project;
import ca.cgjennings.apps.util.InstanceController;
import ca.cgjennings.graphics.cloudfonts.CloudFonts;
import ca.cgjennings.imageio.JPEG2000;
import ca.cgjennings.io.FileChangeMonitor;
import ca.cgjennings.layout.MarkupRenderer;
import ca.cgjennings.platform.DesktopIntegration;
import ca.cgjennings.platform.PlatformFileSystem;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.spelling.SpellingChecker;
import ca.cgjennings.spelling.policy.AcceptPolicy;
import ca.cgjennings.ui.JUtilities;
import ca.cgjennings.ui.StyleUtilities;
import ca.cgjennings.ui.theme.ThemeInstaller;
import ca.cgjennings.util.BriefLogFormatter;
import gamedata.ClassMap;
import gamedata.ConversionMap;
import gamedata.Lock;
import gamedata.Silhouette;
import java.awt.DisplayMode;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import resources.CoreComponents;
import resources.Language;
import static resources.Language.string;
import resources.RawSettings;
import resources.ResourceKit;
import resources.Settings;

/**
 * This is the core Strange Eons application. There is only one instance of this
 * class available, which can be obtained by calling {@link #getApplication()}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class StrangeEons {
    private static final int VER_MAJOR;
    private static final int VER_MINOR;
    private static final int VER_PATCH;
    /**
     * The build number, which increases monotonically on each release.
     */
    private static final int VER_BUILD;
    /**
     * The release type describes whether this is a beta version, etc.
     */
    private static final ReleaseType VER_TYPE;
    /**
     * Special build number indicating that this is not running as a packaged
     * app.
     */
    static final int INTERNAL_BUILD_NUMBER = 99_999;
    
    /**
     * Minimum JRE version to enforce during startup.
     */
    private static final int JAVA_VERSION_MIN = 11;
    /**
     * Maximum JRE version to enforce during startup.
     * May be the same as {@link #JAVA_VERSION_MIN}.
     */
    private static final int JAVA_VERSION_MAX = 11;    

    static {
        // Detect version details that were generated during app packaging;
        // otherwise fall back to development build settings
        int ver_build = INTERNAL_BUILD_NUMBER;
        int ver_major = 3;
        int ver_minor = 4;
        int ver_patch = 0;
        ReleaseType ver_type = ReleaseType.DEVELOPMENT;

        InputStream releaseInfoIn = StrangeEons.class.getResourceAsStream("rev");
        if (releaseInfoIn != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(releaseInfoIn, StandardCharsets.UTF_8))) {
                ver_build = Integer.parseInt(br.readLine().trim());
                ver_major = Integer.parseInt(br.readLine().trim());
                ver_minor = Integer.parseInt(br.readLine().trim());
                ver_patch = Integer.parseInt(br.readLine().trim());
                String type = br.readLine();
                if (type == null || type.isEmpty()) {
                    ver_type = ReleaseType.GENERAL;
                } else {
                    ver_type = ReleaseType.valueOf(type.trim().toUpperCase(Locale.ROOT));
                }
            } catch (IllegalArgumentException | NullPointerException syntaxEx) {
                // logger not initialized yet, print to stderr
                System.err.println("invalid revision syntax");
                syntaxEx.printStackTrace();
            } catch (IOException e) {
                // if missing or unreadable fall back to dev build defaults
            }
        }

        VER_MAJOR = ver_major;
        VER_MINOR = ver_minor;
        VER_BUILD = ver_build;
        VER_PATCH = ver_patch;
        VER_TYPE = ver_type;
    }

    /**
     * The shared logger for application log messages.
     */
    public static final Logger log;
    // the field name "logBuffer" is looked up via reflection by dev tools plug-in
    // private static final StringBuffer logBuffer = new StringBuffer(16 * 1024);

    private static final List<LogEntry> logEntries = new LinkedList<>() {
        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            for (LogEntry e : this) {
                b.append(e.message);
            }
            return b.toString();
        }
    };
    private static final List<LogEntry> immutableLogEntries = Collections.unmodifiableList(logEntries);

    /** A record added to the application log. */
    public static final class LogEntry {
        public final String message;
        public final Level level;

        public LogEntry(String message, Level level) {
            this.message = message;
            this.level = level;
        }
    }

    private static ScriptRunnerModeHelper scriptRunnerMode;

    private static void initScriptRunnerMode(File scriptOrNull) {
        if (scriptOrNull != null) {
            scriptRunnerMode = new ScriptRunnerModeHelper(scriptOrNull);
        }
    }

    // Must be initialized up top so other initializers can access it
    static {
        // Note: the initial ALL level supports quick testing of standalone
        // classes during development. Once init() is called, the level is
        // reset to the the user-requested value.
        log = Logger.getLogger(StrangeEons.class.getName());
        log.setLevel(Level.ALL);

        final Handler handler = new Handler() {
            @Override
            public synchronized void publish(LogRecord record) {
                if (!isLoggable(record)) {
                    return;
                }
                if (
                        "invoke".equals(record.getSourceMethodName())
                        && "org.mozilla.javascript.MemberBox".equals(record.getSourceClassName())
                ) {
                    record.setSourceClassName("script");
                    record.setSourceMethodName(ScriptMonkey.getCurrentScriptLocation());
                }
                String msg;
                try {
                    msg = getFormatter().format(record);
                } catch (Exception ex) {
                    reportError(null, ex, ErrorManager.FORMAT_FAILURE);
                    return;
                }
                try {
                    System.err.print(msg);
                    flush();
                    logEntries.add(new LogEntry(msg, record.getLevel()));
                } catch (Exception ex) {
                    reportError(null, ex, ErrorManager.WRITE_FAILURE);
                }
            }

            @Override
            public void flush() {
                System.err.flush();
            }

            @Override
            public void close() throws SecurityException {
            }
        };
        handler.setFormatter(new BriefLogFormatter());
        handler.setLevel(Level.ALL);

        Logger root = Logger.getLogger("");
        for (Handler h : root.getHandlers()) {
            root.removeHandler(h);
        }
        root.addHandler(handler);
    }

    //
    // VERSION INFORMATION
    //
    /**
     * Returns a string that describes this release of Strange Eons.
     *
     * @return the public version description of the application
     */
    public static String getVersionString() {
        StringBuilder b = new StringBuilder(40);
        b.append(VER_MAJOR);
        if (VER_MINOR != 0) {
            b.append('.').append(VER_MINOR);
        }
        if (VER_BUILD == 99_999 || VER_TYPE == ReleaseType.DEVELOPMENT) {
            b.append(" (development build)");
        } else if (VER_TYPE == ReleaseType.GENERAL) {
            if (VER_PATCH != 0) {
                b.append('.').append(VER_PATCH);
            }
        } else {
            b.append(VER_TYPE.suffix);
            if (VER_PATCH != 0) {
                b.append(VER_PATCH);
            }
            b.append(" (").append(VER_BUILD).append(')');
        }
        return b.toString();
    }

    /**
     * Returns the edition number of this version of the application. For
     * example, releases of Strange Eons 3 would return the value 3. Different
     * editions of Strange Eons typically have extensive differences in their
     * public APIs.
     *
     * @return the edition for this version of Strange Eons
     */
    public static int getEditionNumber() {
        return VER_MAJOR;
    }

    /**
     * Returns the build number of this version of the application. The build
     * number uniquely identifies each release of Strange Eons, and is the most
     * accurate way to determine the version of the application that is running.
     * Higher build numbers represent later releases. The build number can be
     * used to conditionally disable features that rely on a certain version of
     * Strange Eons when the user is running an older version. For example, if a
     * feature used by your plug-in was introduced in build 2942, you can check
     * for it with code like the following:
     * <pre>
     * if( StrangeEons.getBuildNumber() &gt;= 2942 ) {
     *     // enable feature
     * } else {
     *     // disable feature
     * }
     * </pre>
     *
     * @return the build number of this release
     */
    public static int getBuildNumber() {
        return VER_BUILD;
    }

    /**
     * Describes the type of release by which this build was made available to
     * the public.
     *
     * @see #getReleaseType()
     */
    public static enum ReleaseType {
        /**
         * A general release is intended to be stable and feature complete.
         */
        GENERAL(),
        /**
         * A beta release is not yet ready to be considered a general release.
         * The feature set is generally complete, but it has not been thoroughly
         * tested and it may still contain significant bugs. Development
         * releases will display the build number and the Greek letter beta
         * (\u03b2) in the application title bar.
         */
        BETA('\u03b2'),
        /**
         * An alpha release means the the current version is still being very
         * actively developed. Features and public APIs may be incomplete, and
         * may change radically or be added or removed from release to release.
         * While mature features are likely to work normally, newer features may
         * only have seen minimal testing. Development releases will display the
         * build number and the Greek letter alpha (\u03b1) in the application
         * title bar.
         */
        ALPHA('\u03b1'),
        /**
         * A development release is sometimes made available to plug-in
         * developers to prepare for major new features. Development releases do
         * not appear on the regular download page. A development release has
         * all of the qualities of an alpha release, only moreso. Development
         * releases will display the build number and the Greek letter pi
         * (\u03c0) in the application title bar.
         */
        DEVELOPMENT('\u03c0');

        private ReleaseType() {
        }

        private ReleaseType(char suffix) {
            this.suffix = suffix;
        }
        private char suffix;
    };

    /**
     * Returns the type of this release of the application.
     *
     * @return the kind of release, such as {@code ALPHA} or {@code BETA}.
     */
    public static ReleaseType getReleaseType() {
        return VER_TYPE == null ? ReleaseType.DEVELOPMENT : VER_TYPE;
    }

    /**
     * Returns a description of the version of Java that the application is
     * running on as an array of integers. For example, if the official version
     * string is 1.6.0_25, then the returned value is the array [1,6,0,25].
     *
     * <p>
     * The returned array will always include 4 elements. If the version string
     * has additional tokens, they are dropped. If the version string has fewer
     * tokens, extra elements in the returned array will be set to 0.
     *
     * <p>
     * If for some reason the version cannot be parsed (for example, if the
     * property is not defined or it uses a non-standard format), then the
     * failure will be logged and every element of the returned array will be
     * set to {@code Integer.MAX_VALUE}.
     *
     * @return an array of version information
     */
    private static int[] getJavaVersion() {
        final int[] ver = new int[4];
        final String v = System.getProperty("java.version");
        try {
            String[] t = v.split("\\D+");
            for (int i = 0; i < t.length && i < ver.length; ++i) {
                ver[i] = Integer.parseInt(t[i]);
            }
        } catch (NumberFormatException e) {
            for (int i = 0; i < ver.length; ++i) {
                ver[i] = Integer.MAX_VALUE;
            }
            log.log(Level.SEVERE, "exception parsing Java version string: \"{0}'", v);
        }
        return ver;
    }

    /**
     * Returns {@code true} if the installed version of Java is equal to or
     * greater than the version specified. For example, to check if the
     * installed version is Java 6 update 10 or newer, call:
     * {@code isJavaVersion( 1, 6, 0, 10 )}.
     *
     * @param versionTokens the version number tokens to check; index 1 is the
     * major version number, index 2 is the minor version number, and index 3 is
     * the update number (index 0 has never been anything but 1 at the time of
     * this writing)
     * @return {@code true} if the installed version of Java is equal to or
     * greater than the specified version
     */
    public static boolean testJavaVersion(int... versionTokens) {
        int[] ver = getJavaVersion();
        for (int i = 0; i < ver.length && i < versionTokens.length; ++i) {
            if (ver[i] < versionTokens[i]) {
                return false; // older version
            }
            if (ver[i] > versionTokens[i]) {
                return true; // newer version
            }
        }
        return true; // same version
    }

    private static final String DELETE_ON_STARTUP_KEY_PREFIX = "delete-on-startup-";
    private static StrangeEons instance;

    /**
     * The recommended <b>minimum</b> value for the maximum memory setting
     * ({@code -Xmx}) for the virtual machine, in megabytes. Higher values are
     * better if your system can support it.
     */
    public static final int RECOMMENDED_MEMORY_IN_MB = 1024;

    /**
     * This class cannot be instantiated. To obtain the single global instance
     * of this class (once it exists), call {@link #getApplication()}.
     *
     * @param args the command line arguments
     */
    private StrangeEons(String[] arguments) throws Throwable {
        commandLineArguments = CommandLineArguments.create(arguments);
    }

    /**
     * Top-level initialization function, called from {@code main()} immediately
     * after instance is created.
     * 
     * <p>This <strong>must</strong> be called before Java2D initializes.
     */
    private void initialize() {
        try {
            if (commandLineArguments.version) {
                System.out.println(StrangeEons.getBuildNumber());
                System.exit(0);
            }

            if (commandLineArguments.xDisablePluginLoading) {
                BundleInstaller.disablePluginLoading();
                log.info("disabled plug-in loading");
            }

            boolean enableAccel = PlatformSupport.PLATFORM_IS_WINDOWS
                    ? commandLineArguments.xEnableWindowsAcceleration
                    : true;
            if (commandLineArguments.xDisableAcceleration) {
                enableAccel = false;
            }
            
            String rendererLogMsg = applyGraphicsOptions(enableAccel, commandLineArguments.xOpenGL, commandLineArguments.xDisableAnimation);
            String textAALogMsg = applyTextAntialiasingOptions(commandLineArguments.xAAText);
            initStage1(rendererLogMsg, textAALogMsg);
        } catch (final Throwable t) {
            log.log(Level.SEVERE, "Uncaught Exception in Stage 1", t);
            EventQueue.invokeLater(() -> {
                // try to localize the error message, but have a fallback ready
                String message = "Uncaught exception during initialization";
                try {
                    if (Language.getInterface().isKeyDefined("rk-err-uncaught")) {
                        message = string("rk-err-uncaught");
                    }
                    ErrorDialog.displayFatalError(message, t);
                } catch (Exception e) {
                    log.log(Level.SEVERE, "couldn't display error message", e);
                }
            });
        }
    }

    /**
     * Apply options to prefer OpenGL (over DirectX) for for default or
     * user-specified text antialiasing settings. This must be
     * called before the AWT/Swing is initialized.
     *
     * @param textAA the user-specified setting, or null for a platform default
     */    
    private static String applyGraphicsOptions(boolean enableAcceleration, boolean preferOpenGl, boolean avoidAnimation) {
        // start with all accelerated renderers disabled, then selectively enable
        boolean d3d = false, opengl = false, xrender = false, metal = false;
        if (enableAcceleration) {
            if (PlatformSupport.PLATFORM_IS_WINDOWS) {
                if (preferOpenGl) {
                    opengl = true;
                } else {
                    d3d = true;
                }
            } else if (PlatformSupport.PLATFORM_IS_OTHER) {
                if (preferOpenGl) {
                    opengl = true;
                } else {
                    xrender = true;
                }
            } else if (PlatformSupport.PLATFORM_IS_MAC) {
                metal = true;
                opengl = true;
                if (preferOpenGl) {
                    metal = false;
                }
            }
        }
        
        System.setProperty("sun.java2d.d3d", d3d ? "true" : "false");
        
        System.setProperty("sun.java2d.opengl", opengl ? "True" : "false");
        System.setProperty("sun.java2d.opengl.fbobject", "false");
        
        System.setProperty("sun.java2d.xrender", xrender ? "True" : "false");
        
        System.setProperty("sun.java2d.metal", metal ? "true" : "false");

        if (avoidAnimation) {
            System.setProperty("ca.cgjennings.anim.enabled", "false");
        }

        // compose the log message, but defer logging until later
        if (log.isLoggable(Level.INFO)) {
            return "renderer selection matrix: Software=true, Direct3D="
                + d3d + ", Metal=" + metal + ", OpenGL=" + opengl + ", XRender=" + xrender;
        }
        return null;
    }

    /**
     * Apply default or user-specified text antialiasing settings. This must be
     * called before the AWT/Swing is initialized.
     *
     * @param textAA the user-specified setting, or null for a platform default
     */
    private static String applyTextAntialiasingOptions(String textAA) {
        if (textAA == null) {
            textAA = PlatformSupport.PLATFORM_IS_OTHER ? "lcd" : "auto";
        }

        // stick with Java's detected settings
        if (textAA.equals("auto")) {
            return null;
        }

        switch (textAA) {
            case "auto":
            case "off":
            case "on":
            case "gasp":
            case "lcd":
            case "lcd_hrgb":
            case "lcd_hbgr":
            case "lcd_vrgb":
            case "lcd_vbgr":
                break;
            default:
                System.err.println("invalid text antialiasing option: " + textAA);
                System.exit(20);
                break;
        }

        // enforce requested settings
        System.setProperty("awt.useSystemAAFontSettings", textAA);
        if (System.getProperty("swing.aatext") == null) {
            System.setProperty("swing.aatext", "true");
        }
        
        return "text antialiasing mode: " + textAA;
    }

    /**
     * Returns the single instance of {@code StrangeEons} once the application
     * has been initialized. (The application becomes initialized after
     * libraries and themes have been loaded, but just before extension plug-ins
     * are loaded.) If the application is not yet initialized, this method
     * returns {@code null}.
     *
     * @return the application instance
     */
    public static StrangeEons getApplication() {
        return instance;
    }

    /**
     * Returns the main application window. If called while the application is
     * starting, the window might not yet exist, in which case {@code null} is
     * returned. (The window is guaranteed to exists before extension plug-ins
     * begin to load.)
     *
     * @return the main application window
     */
    public static StrangeEonsAppWindow getWindow() {
        return AppFrame.getApp();
    }

    /**
     * Returns the active editor, or {@code null} if no editor is active. This
     * is a convenience method that returns {@code null} if the application
     * window has not been created yet, or else forwards the call to the
     * application window.
     *
     * @return the active editor, or {@code null}
     */
    public static StrangeEonsEditor getActiveEditor() {
        StrangeEonsAppWindow w = getWindow();
        if (w == null) {
            return null;
        }
        return w.getActiveEditor();
    }

    /**
     * Returns the game component edited by the active editor, or {@code null}
     * if there either is no active editor, or if the active editor is not a
     * game component editor.
     *
     * @return the actively edited component, or {@code null}
     */
    public static GameComponent getActiveGameComponent() {
        StrangeEonsEditor ed = getActiveEditor();
        if (ed == null) {
            return null;
        }
        return ed.getGameComponent();
    }

    /**
     * Creates a new editor for the component type specified by a key from a
     * class map. The new editor will be added to the main application window
     * and a reference to it will be returned.
     *
     * <p>
     * <b>Resource Script Entries:</b>
     * If the class map key is associated with a resource script rather than a
     * component class or DIY script, then the script might create any number of
     * editors, including zero. In this case, the new editor will be inferred by
     * observing whether the active editor changes after running the script. If
     * it does change, then it is assumed that the newly active editor was
     * created by the script; otherwise it is assumed that the script did not
     * create an editor and {@code null} is returned.
     *
     * @param classMapKey a key for an editor specified in a
     * {@linkplain gamedata.ClassMap#add loaded class map file}
     * @return an editor for the given key, or {@code null} if no new editor is
     * created
     * @throws IllegalArgumentException if the class map key is not a valid key
     * from an entry that was added this session
     * @throws CoreComponents.MissingCoreComponentException if a core library is
     * required to create the editor and the user declines to install it
     * @throws InstantiationException if an error occurs while instantiating the
     * editor
     * @see gamedata.ClassMap
     * @see gamedata.ClassMap.Entry#getKey()
     */
    public StrangeEonsEditor createEditor(String classMapKey) throws InstantiationException {
        try {
            return NewEditorDialog.getSharedInstance().createEditorFromClassMapKey(classMapKey);
        } catch (IllegalArgumentException e) {
            // NewEditorDialog is package private; this prevents implementation
            // details from leaking
            throw new IllegalArgumentException(e.getLocalizedMessage());
        }
    }

    /**
     * Adds a new editor, causing it it to become visible and available to the
     * user. The editor will be placed on the main application window tab strip
     * (that is, the editor will start attached). This is a convenience method
     * that forwards the call to the application window.
     *
     * @param editor the editor to be added
     * @throws NullPointerException if {@code editor} is {@code null}
     * @throws IllegalStateException if the application window doesn't exist
     */
    public static void addEditor(StrangeEonsEditor editor) {
        StrangeEonsAppWindow app = getWindow();
        if (app == null) {
            throw new IllegalStateException("no app window");
        }
        app.addEditor(editor);
    }

    /**
     * Returns the current open project, or {@code null} if no project is open.
     * (At most one project can be open at any given time.) This is a
     * convenience method that returns {@code null} if the application window
     * has not been created yet, or else forwards the call to the application
     * window.
     *
     * @return the current open project, or {@code null}
     * @see StrangeEonsAppWindow#getOpenProject()
     */
    public static Project getOpenProject() {
        StrangeEonsAppWindow w = getWindow();
        if (w == null) {
            return null;
        }
        return w.getOpenProject();
    }

    /**
     * Returns whether the app is running in non-interactive mode. When running
     * in non-interactive mode, windows are not shown automatically (including
     * the splash screen). Non-interactive mode is normally used to perform some
     * automated task, such as running a script.
     *
     * <p>
     * Making the main app window visible will immediately end non-interactive
     * mode if enabled.
     *
     * <p>
     * Note that performing an action that assumes or relies on the app window
     * may throw an exception when running in non-interactive mode.
     *
     * @return true if running in non-interactive mode
     */
    public static boolean isNonInteractive() {
        // currently only script runner mode activates the non-interactive state
        if (scriptRunnerMode != null) {
            // started non-interactive, but left if window made visible
            final AppFrame af = AppFrame.getApp();
            return af == null ? true : !af.hasEverBeenMadeVisible;
        }
        return false;
    }

    /**
     * If the app is in "script runner" mode, returns an object that can be
     * queried for additional information and used to manage the mode's
     * behaviour. Returns null if the app is running normally. Script runner
     * mode is activated by passing a command line argument {@code -run} (or
     * {@code --run}) and a file path for a script file. When running in run
     * script mode, the app begins in a non-interactive mode without
     * automatically showing the app window.
     *
     * @return the run script mode helper responsible for running the command
     * line script file, or null
     * @see CommandLineArguments#run
     */
    public static ScriptRunnerState getScriptRunner() {
        return scriptRunnerMode;
    }

    /**
     * Reloaded plug-ins that are of the {@link Plugin#ACTIVATED ACTIVATED} or
     * {@link Plugin#INJECTED INJECTED} types. Currently loaded plug-ins of
     * these types will first be {@linkplain #unloadPlugins() unloaded}, and
     * then all plug-ins found in
     * {@linkplain BundleInstaller#loadPluginBundles() plug-in bundles} that are
     * currently {@linkplain InstalledPlugin#setEnabled(boolean) enabled} will
     * be
     * {@linkplain Plugin#initializePlugin(ca.cgjennings.apps.arkham.plugins.PluginContext) started}.
     * This method has no effect on null null null null null null null null null
     * null null     {@linkplain BundleInstaller#loadLibraryBundles libraries},
	 * {@linkplain BundleInstaller#loadThemeBundles themes}, or
     * {@linkplain BundleInstaller#loadExtensionBundles extension plug-ins}.
     *
     * @throws AssertionError if not called from the event dispatch thread
     * @see #unloadPlugins()
     * @see BundleInstaller#loadPluginBundles()
     * @see BundleInstaller#getInstalledPlugins()
     */
    public synchronized void loadPlugins() {
        JUtilities.threadAssert();

        setWaitCursor(true);
        try {
            // if there are existing plug-ins, unload them
            // if there are not, assume that the plug-in context information
            //   has not been initialized and initialize it
            unloadPlugins();

            // make a list of bundles marked uninstalled to cross reference with
            HashSet<File> uninstalledBundles = new HashSet<>();
            for (int index = 1; RawSettings.getUserSetting(DELETE_ON_STARTUP_KEY_PREFIX + index) != null; ++index) {
                uninstalledBundles.add(new File(RawSettings.getUserSetting(DELETE_ON_STARTUP_KEY_PREFIX + index)));
            }

            // get a list of plug-ins and sort by priority, then name
            InstalledPlugin[] plugins = BundleInstaller.getInstalledPlugins();

            // start 'em up
            for (final InstalledPlugin ip : plugins) {
                startPlugin(ip, uninstalledBundles);
            }

            // inform listeners, including the Toolbox menu
            firePluginLoading(PluginLoadingListener.PLUGIN_LOAD_EVENT);
        } finally {
            setWaitCursor(false);
        }
    }

    /**
     * Helper that starts an individual non-extension plug-in.
     *
     * @param ip non-null plug-in to start
     * @param uninstalledBundles bundle files marked uninstalled in prefs
     */
    private void startPlugin(InstalledPlugin ip, Set<File> uninstalledBundles) {
        // first check if we should ignore the plug-in for various reasons:
        if (!ip.isEnabled()) {
            StrangeEons.log.log(Level.FINEST, "skipping disabled plug-in {0}", ip.getPluginClass());
            return;
        }

        if (ip.getBundle() != null && uninstalledBundles.contains(ip.getBundle().getFile())) {
            StrangeEons.log.log(Level.FINEST, "skipping uninstalled plug-in {0}", ip.getPluginClass());
            return;
        }

        final int type = ip.getPluginType();
        if (type != Plugin.ACTIVATED && type != Plugin.INJECTED) {
            if (type == Plugin.EXTENSION) {
                StrangeEons.log.log(Level.WARNING, "skipping EXTENSION plug-in in wrong bundle {0}", ip.getPluginClass());
                return;
            }
            StrangeEons.log.log(Level.SEVERE, "skipping plug-in with unknown type {0}", ip.getPluginClass());
            return;
        }

        // OK, we really want to start it
        final Plugin p;
        try {
            p = ip.startPlugin();
            StrangeEons.log.log(Level.FINE, "started plug-in {0} ({1})", new Object[]{ip.getPluginClass(), ip.getName()});
        } catch (PluginException t) {
            StrangeEons.log.log(Level.WARNING, "plug-in failed to start " + ip.getPluginClass(), t);
            return;
        }

        // injected plug-ins are "shown" exactly once, when they are started
        if (type == Plugin.INJECTED) {
            try {
                p.showPlugin(PluginContextFactory.createContext(ip, 0), true);
            } catch (PluginException | RuntimeException ex) {
                ErrorDialog.displayError(string("rk-err-plugin-init", ip.getPluginClass()), ex);
            }
        }
    }

    /**
     * Causes all currently loaded {@link Plugin#ACTIVATED ACTIVATED} and
     * {@link Plugin#INJECTED INJECTED} plug-ins to be unloaded. This method has
     * no effect on {@linkplain BundleInstaller#loadLibraryBundles libraries},
     * {@linkplain BundleInstaller#loadThemeBundles themes}, or
     * {@linkplain BundleInstaller#loadExtensionBundles extension plug-ins}.
     *
     * @see #loadPlugins()
     * @see ca.cgjennings.apps.arkham.StrangeEons.PluginLoadingListener
     */
    public synchronized void unloadPlugins() {
        setWaitCursor(true);
        try {
            InstalledPlugin[] plugins = BundleInstaller.getInstalledPlugins();
            for (int i = 0; i < plugins.length; ++i) {
                if (plugins[i].isStarted()) {
                    try {
                        plugins[i].stopPlugin();
                        log.log(Level.FINEST, "stopped plug-in instance: {0}", plugins[i].getPluginClass());
                    } catch (PluginException | RuntimeException ex) {
                        log.log(Level.WARNING, "uncaught exception while stopping plug-in: " + plugins[i].getPluginClass(), ex);
                    }
                }
            }
            firePluginLoading(PluginLoadingListener.PLUGIN_UNLOAD_EVENT);
        } finally {
            setWaitCursor(false);
        }
    }

    /**
     * An event interface implemented by classes that will listen for plug-ins
     * to be loaded or unloaded. Code that wishes to behave differently
     * depending on whether a (non-extension) plug-in is loaded should listen
     * for plug-ins to be reloaded and update their behaviour accordingly. For
     * example, suppose you have a game component editor that can make use of a
     * tool provided by a plug-in, if that plug-in is installed. The editor can
     * add a button to its interface which is only enabled if the plug-in is
     * loaded. It should
     * {@linkplain BundleInstaller#getBundleFileForUUID(java.util.UUID) check if the plug-in is installed}
     * when the editor is first created and set the button's initial enabled
     * state accordingly. It should also
     * {@linkplain #addPluginLoadingListener add a listener} so it can update
     * the button whenever plug-ins are reloaded. Finally, it should
     * {@linkplain #removePluginLoadingListener remove the listener} when the
     * {@linkplain StrangeEonsEditor#addEditorListener editor is closed} to
     * ensure that editor can be garbage collected.
     *
     * @see #addPluginLoadingListener
     * @see #loadPlugins()
     * @see #unloadPlugins()
     */
    public interface PluginLoadingListener extends EventListener {

        /**
         * Event type code indicating that the application has just loaded (or
         * reloaded) the plug-in set.
         */
        static int PLUGIN_LOAD_EVENT = 1;
        /**
         * Event type code indicating that the application has just unloaded all
         * loaded plug-ins.
         */
        static int PLUGIN_UNLOAD_EVENT = 2;

        /**
         * Called after the application loads, reloads, or unloads plug-ins. The
         * value of {@code eventType} describes the type of load event:<br>
         * {@code PLUGIN_LOAD_EVENT}: plug-ins have been loaded (or
         * reloaded)<br> {@code PLUGIN_UNLOAD_EVENT}: plug-ins have been
         * unloaded
         *
         * @param eventType the type of load event
         */
        void pluginsLoaded(int eventType);
    }

    /**
     * Adds a new listener to be notified when plug-ins are loaded or unloaded.
     *
     * @param listener the listener to add
     * @throws NullPointerException if the listener is {@code null}
     * @see PluginLoadingListener
     */
    public void addPluginLoadingListener(PluginLoadingListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        if (pluginListeners.add(listener)) {
            StrangeEons.log.log(Level.FINEST, "added plug-in event listener <{0}>", listener);
        } else {
            StrangeEons.log.log(Level.WARNING, "listener added twice: <{0}>", listener);
        }
    }

    /**
     * Removes a previously added listener for plug-in load events.
     *
     * @param listener the listener to remove
     */
    public void removePluginLoadingListener(PluginLoadingListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        if (pluginListeners.remove(listener)) {
            StrangeEons.log.log(Level.FINEST, "removed plug-in event listener <{0}>", listener);
        } else {
            StrangeEons.log.log(Level.WARNING, "removed a listener that wasn't added: <{0}>", listener);
        }
    }

    private void firePluginLoading(int eventType) {
        if (eventType != PluginLoadingListener.PLUGIN_LOAD_EVENT && eventType != PluginLoadingListener.PLUGIN_UNLOAD_EVENT) {
            throw new IllegalArgumentException("unknown event type: " + eventType);
        }
        pluginListeners.forEach(li -> li.pluginsLoaded(eventType));
    }

    private Set<PluginLoadingListener> pluginListeners = new LinkedHashSet<>();

    /**
     * Given the class name of a plug-in, return its {@link Plugin Plugin}
     * instance if it is currently installed and loaded.If the plug-in class is
     * not installed, is {@linkplain InstalledPlugin#isEnabled() disabled}, or
     * it failed to {@linkplain Plugin#initializePlugin initialize properly},
     * this method returns {@code null}.
     *
     * @param pluginIdentifier
     * @return the {@code Plugin} instance for the class, or {@code null}
     * @throws NullPointerException if {@code pluginClass} is {@code null}
     * @see BundleInstaller#getInstalledPlugins()
     * @see BundleInstaller#getBundleFileForUUID(java.util.UUID)
     * @see PluginBundle#getPluginRoot()
     */
    public synchronized Plugin getLoadedPlugin(String pluginIdentifier) {
        if (pluginIdentifier == null) {
            throw new NullPointerException("pluginClass");
        }
        for (InstalledPlugin p : BundleInstaller.getInstalledPlugins()) {
            if (p.getPluginClass().equals(pluginIdentifier)) {
                // will be null if plug-in not started, as it should be
                return p.getPlugin();
            }
        }
        return null;
    }

    /**
     * Finds a loaded {@link Plugin Plugin} with the specified name.If there is
     * no matching plug-in, returns {@code null}. Plug-ins are not required to
     * have unique names: if more than one plug-in matches, this method returns
     * one of them at random. Moreover, some plug-ins are localized and will
     * have a different name depending on the current
     * {@linkplain Language#getInterface() interface language}. To guarantee a
     * specific plug-in class, either use
     * {@link #getLoadedPlugin(java.lang.String)} or find the plug-in by its
     * {@linkplain BundleInstaller#getBundleFileForUUID(java.util.UUID) UUID}.
     *
     * @param name the plug-in name presented to the user
     * @return the {@code Plugin} instance for the class, or {@code null}
     * @throws NullPointerException if {@code pluginClass} is {@code null}
     */
    public synchronized Plugin getLoadedPluginByName(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        for (InstalledPlugin p : BundleInstaller.getInstalledPlugins()) {
            if (name.equals(p.getName())) {
                return p.getPlugin();
            }
        }
        return null;
    }

    /**
     * Shows an {@code ACTIVATED} plug-in that is currently loaded. If the
     * plug-in is currently showing then it will be hidden first, then re-shown.
     *
     * @param plugin the plug-in to activate
     * @see #activatePlugin(ca.cgjennings.apps.arkham.plugins.Plugin, int,
     * boolean)
     */
    public void activatePlugin(Plugin plugin) {
        if (plugin.getPluginType() != Plugin.ACTIVATED) {
            throw new IllegalArgumentException("wrong plug-in type: " + plugin);
        }
        if (plugin.isPluginShowing()) {
            activatePlugin(plugin, 0, false);
        }
        activatePlugin(plugin, 0, true);
    }

    /**
     * Shows or hides an {@code ACTIVATED} plug-in that is currently loaded. A
     * {@link PluginContext} will be created for the plug-in and used to
     * activate it as if the user had selected it from the <b>Toolbox</b>
     * menu. The value of {@code modifiers} is a logical or of modifier key
     * codes (see
     * {@link ca.cgjennings.apps.arkham.plugins.PluginContext#getModifiers})
     * that will be marked as pressed. To activate the plug-in without
     * indicating any special modifier keys, pass 0. If {@code show} is
     * {@code true}, then the plug-in will be asked to show itself, otherwise to
     * hide itself. The exact behaviours implied by the terms "show" and "hide"
     * depend on the plug-in, but generally a show request executes the plug-in
     * behaviour or makes the plug-in's dialog visible, while a hide request
     * stops the behaviour or makes the dialog invisible. Many plug-ins are
     * implemented as modal dialogs and therefore do not have a hide behaviour,
     * as they stop the main application until the plug-in window is closed.
     *
     * <p>
     * <b>Note:</b> The general contract for plug-in behaviour states that a
     * plug-in will only be shown if its {@code isPluginShowing} method returns
     * {@code false}, and vice-versa. Although most plug-ins do not rely on this
     * guarantee, you should not violate it unless the plug-in's documentation
     * explicitly permits it.
     *
     * @param plugin the plug-in to be activated
     * @param modifiers the set of control key modifiers to activate the plug-in
     * with
     * @param show if {@code true}, the plug-in should show/start itself,
     * otherwise, hide/stop itself
     * @throws IllegalArgumentException if {@code plugin} is not a
     * {@code Plugin.ACTIVATED} type
     */
    public void activatePlugin(Plugin plugin, int modifiers, boolean show) {
        if (plugin.getPluginType() != Plugin.ACTIVATED) {
            throw new IllegalArgumentException("plug-in is not of type Plugin.ACTIVATED");
        }
        if (!plugin.isPluginUsable()) {
            return;
        }
        plugin.showPlugin(PluginContextFactory.createContext(plugin, modifiers), show);
    }

    /**
     * Returns a (possibly empty) array of the currently loaded plug-ins.
     *
     * @return an array of the activated or injected plug-ins that are loaded
     * @see #loadPlugins()
     */
    public synchronized Plugin[] getLoadedPlugins() {
        LinkedList<Plugin> set = new LinkedList<>();
        for (InstalledPlugin ip : BundleInstaller.getInstalledPlugins()) {
            if (ip.isStarted()) {
                set.add(ip.getPlugin());
            }
        }
        return set.toArray(Plugin[]::new);
    }

    /**
     * Returns a file that can be used to store user data. For example, a plug-in
     * might use this to create a folder for storing cached data. All of the
     * files returned by this method will be located in a special
     * system-dependent storage folder. If {@code child} is {@code null}, then
     * the storage folder itself will be returned. Otherwise, {@code child} is
     * taken to be a relative path, and the returned file will combine the
     * storage folder with this relative location. For example, on a UNIX
     * system, calling this method with child "test.txt" might return
     * {@code ~/.StrangeEons3/test.txt}.
     *
     * <p>
     * To locate a subfolder in user storage, specify a relative path using '/'
     * as the directory separator character. (Slashes will be converted to the
     * platform-specific separator character automatically.) For example,
     * {@code "myplugin/cachefile"} would refer to the file
     * <i>cachefile</i> in the <i>myplugin</i> subfolder of the user storage
     * folder.
     * 
     * <p>An effort will be made to create any needed folders between the
     * storage folder and the requested file, if they do not already exist.
     * If {@code child} ends with {@code /}, then the last path entry in
     * {@code child} is also treated as a folder name and created if mising.
     *
     * @param child
     * @return the folder in which per-user files are stored, or the
     * {@code child} within that folder
     * @throws IllegalArgumentException if the returned file would not be
     * located in the system-dependent storage folder (for example, if
     * {@code child} starts with "..")
     */
    public static File getUserStorageFile(String child) {
        File f = USER_STORAGE_FOLDER;
        if (child != null) {
            boolean folderRequest = child.endsWith("/");
            if (folderRequest) {
                child = child.substring(0, child.length() - 1);
            }

            f = new File(f, child.replace('/', File.separatorChar));
            
            if (!isInUserStorage(f)) {
                throw new IllegalArgumentException("file is not in storage folder: " + f);
            }

            if (folderRequest) {
                f.mkdirs();
            } else if (child.contains("/")) {
                f.getParentFile().mkdirs();
            }
        }
        return f;
    }

    ////////////////////////////////////////////////////////////////////////////
    // WARNING: If the storage folder location algorithm is changed, you must //
    // also update the debugger client's preference saving code.              //
    ////////////////////////////////////////////////////////////////////////////
    private static final File USER_STORAGE_FOLDER;

    static {
        File usf = null;
        String user = System.getenv("STRANGE_EONS_USER_DIR");
        if (user != null) {
            usf = new File(user);
            if (!usf.exists()) {
                usf.mkdirs();
            }
            if (!usf.isDirectory()) {
                StrangeEons.log.log(Level.WARNING, "STRANGE_EONS_USER_DIR is not a directory: {0}", usf);
                usf = null;
            }
        }
        if (usf == null && PlatformSupport.PLATFORM_IS_WINDOWS) {
            String appdata = System.getenv("AppData");
            if (appdata != null) {
                usf = new File(appdata, "StrangeEons3");
            } else {
                StrangeEons.log.warning("AppData environment variable not defined");
                // will try UNIX-style directory
            }
        }
        if (usf == null) {
            usf = new File(System.getProperty("user.home"), ".StrangeEons3");
        }
        if (usf.exists() && !usf.isDirectory()) {
            usf = new File(System.getProperty("java.io.tmpdir"));
            StrangeEons.log.severe("no suitable location for user storage folder available, using default temp directory");
        }
        if (!usf.exists()) {
            usf.mkdirs();
            PlatformFileSystem.makeWritableByUser(usf);
        }
        USER_STORAGE_FOLDER = usf;
    }

    private static boolean isInUserStorage(File f) {
        if (f.isAbsolute()) {
            while (f != null) {
                if (USER_STORAGE_FOLDER.equals(f)) {
                    return true;
                }
                f = f.getParentFile();
            }
        }
        return false;
    }

    /**
     * Adds or removes a marker indicating that a file should be deleted when
     * the application is next started. On the next application start, an
     * attempt is made to delete all files so marked. Any file marked for
     * deletion with this method must be located in the user storage folder.
     *
     * @param toDelete the file to delete at startup
     * @param delete whether the record to delete should be added or removed
     * @throws NullPointerException if the file is {@code null}
     * @throws IllegalArgumentException if the file to be deleted is not in the
     * user storage folder
     */
    public void deleteOnStartup(File toDelete, boolean delete) {
        if (toDelete == null) {
            throw new NullPointerException("toDelete");
        }
        if (!toDelete.isAbsolute()) {
            toDelete = getUserStorageFile(toDelete.getPath());
        }
        if (!isInUserStorage(toDelete)) {
            throw new IllegalArgumentException("file is not in storage folder: " + toDelete);
        }

        // If this file incldues symbolic links, etc., try to eliminate them
        // so that every toDelete that represents the same file will map
        // to only one possible entry in the list.
        try {
            toDelete = toDelete.getCanonicalFile();
        } catch (IOException e) {
            log.log(Level.WARNING, "unable to get canonical mapping: " + toDelete, e);
        }

        boolean foundOnList = false;
        int index;
        for (index = 1; RawSettings.getUserSetting(DELETE_ON_STARTUP_KEY_PREFIX + index) != null; ++index) {
            if (new File(RawSettings.getUserSetting(DELETE_ON_STARTUP_KEY_PREFIX + index)).equals(toDelete)) {
                foundOnList = true;
                break;
            }
        }

        // list already in the desired state
        if (delete == foundOnList) {
            return;
        }

        if (delete) {
            // add new entry to end of list
            RawSettings.setUserSetting(DELETE_ON_STARTUP_KEY_PREFIX + index, toDelete.getPath());
        } else {
            // move remaining records down one index to remove the record
            String next;
            for (;;) {
                next = RawSettings.getUserSetting(DELETE_ON_STARTUP_KEY_PREFIX + (index + 1));
                if (next == null) {
                    RawSettings.removeUserSetting(DELETE_ON_STARTUP_KEY_PREFIX + index);
                    break;
                } else {
                    RawSettings.setUserSetting(DELETE_ON_STARTUP_KEY_PREFIX + index, next);
                }
                ++index;
            }
        }
    }

    /**
     * Returns {@code true} if a file has been marked for deletion on the next
     * application start by passing it to
     * {@link #deleteOnStartup(java.io.File, boolean)}. Note that it is not
     * guaranteed that the file will actually be deleted, only that it is marked
     * and that an attempt to delete it will be made.
     *
     * @param toCheck the file to check
     * @return {@code true} if the file will be deleted the next time the
     * application starts
     * @throws NullPointerException if the file is {@code null}
     */
    public boolean willDeleteOnStartup(File toCheck) {
        if (toCheck == null) {
            throw new NullPointerException("toCheck");
        }
        if (!toCheck.isAbsolute()) {
            toCheck = getUserStorageFile(toCheck.getPath());
        }
        try {
            toCheck = toCheck.getCanonicalFile();
        } catch (IOException e) {
        }
        int delNum;
        for (delNum = 1; RawSettings.getUserSetting(DELETE_ON_STARTUP_KEY_PREFIX + delNum) != null; ++delNum) {
            File candidate = new File(RawSettings.getUserSetting(DELETE_ON_STARTUP_KEY_PREFIX + delNum));
            if (!candidate.isAbsolute()) {
                candidate = getUserStorageFile(candidate.getPath());
            }
            try {
                candidate = candidate.getCanonicalFile();
            } catch (IOException e) {
            }

            if (candidate.equals(toCheck)) {
                // found file on list
                return true;
            }
        }
        return false;
    }

    /**
     * Registers a task to be performed when Strange Eons is exiting. Exit tasks
     * are run only when it is certain that the application will exit; for
     * example, the user will already have been given a chance to save editors
     * with unsaved changes.
     *
     * <p>
     * Exit tasks are performed calling the {@code run()} method of the
     * {@code task} object. A description of each task will be logged at
     * {@code Level.INFO} by calling the task's {@code toString()} method just
     * before it runs. If a task throws an exception, it will be logged but it
     * will not prevent other exit tasks from running.
     *
     * <p>
     * Tasks are not guaranteed to run in a particular order; they may run from
     * any thread and may even run concurrently. In general, do not expect to be
     * able to open a window or perform other actions on the event dispatch
     * thread; to do so safely you would need to use {@code invokeLater} and the
     * application will probably exit before such code will get a chance to run.
     *
     * <p>
     * Tasks are not guaranteed to run if the application is terminated
     * abnormally, including use of the terminate command from the script
     * debugger.
     *
     * @param task code to be called as part of the application shutdown process
     */
    public synchronized void addExitTask(final Runnable task) {
        exitTasks.add(Objects.requireNonNull(task, "task"));
        log.log(Level.INFO, "added exit task [{0}]", task);
    }
    private LinkedList<Runnable> exitTasks = new LinkedList<>();

    /**
     * Called by AppFrame during shutdown to run registered exit tasks.
     */
    synchronized void runExitTasks() {
        final long start = System.currentTimeMillis();
        log.info("running exit tasks");

        if (exitTasks.size() <= 1 || Runtime.getRuntime().availableProcessors() == 1) {
            while (!exitTasks.isEmpty()) {
                wrapExitTask(exitTasks.removeLast()).run();
            }
        } else {
            // run concurrently for faster shutdown
            Runnable[] tasks = new Runnable[exitTasks.size()];
            for (int i = 0; i < tasks.length; ++i) {
                tasks[i] = wrapExitTask(exitTasks.removeLast());
            }
            SplitJoin.getInstance().runUnchecked(tasks);
        }

        log.log(Level.INFO, "completed all exit tasks in {0} ms", System.currentTimeMillis() - start);
    }

    /**
     * Wraps an exit task with another Runnable that prevents exceptions from
     * escaping and adds logging.
     *
     * @param task the task to wrap
     * @return a wrapped task that performs the task without letting exceptions
     * escape
     */
    private static Runnable wrapExitTask(final Runnable task) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    log.log(Level.INFO, "running exit task [{0}]", task);
                    task.run();
                } catch (Exception ex) {
                    log.log(Level.SEVERE, "uncaught exception in exit task", ex);
                }
            }

            @Override
            public String toString() {
                return task.toString();
            }
        };
    }

    /**
     * Registers a task to be performed when Strange Eons is has started up and
     * is ready to use. If called after Strange Eons has already finished
     * starting, the task will run immediately.
     *
     * <p>
     * Startup tasks are performed calling the {@code run()} method of the
     * {@code task} object. A description of each task will be logged at
     * {@code Level.INFO} by calling the task's {@code toString()} method just
     * before it runs. If a task throws an exception, it will be logged but it
     * will not prevent other tasks from running.
     *
     * <p>
     * Unlike exit tasks, startup tasks run on the event dispatch thread in the
     * order that they were registered.
     *
     * @param task code to be called once the app has started
     */
    public synchronized void addStartupTask(Runnable task) {
        task = Objects.requireNonNull(task, "task");
        if (startupTasks == null) {
            // create a true list, in case not in EDT and more tasks added
            startupTasks = new LinkedList<>();
            startupTasks.add(task);
            runStartupTasks();
            return;
        }
        startupTasks.add(task);
        log.log(Level.INFO, "added startup task [{0}]", task);
    }

    synchronized void runStartupTasks() {
        if (startupTasks == null || startupTasks.isEmpty()) {
            return;
        }
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(this::runStartupTasks);
            return;
        }

        List<Runnable> taskList = startupTasks;
        startupTasks = null;

        log.info("running startup tasks");
        for (Runnable task : taskList) {
            try {
                log.log(Level.INFO, "running startup task [{0}]", task);
                task.run();
            } catch (Exception ex) {
                log.log(Level.SEVERE, "uncaught exception in startup task", ex);
            }
        }
    }

    private List<Runnable> startupTasks = new LinkedList<>();

    /**
     * Returns the most recently valid markup target. Returns {@code null} if
     * there has either never been a valid target, or if the most recently valid
     * target is no currently valid (for example, if it has been disabled since
     * it became the current target). When responding to user action, this is
     * normally the markup target that you want since intervening user action is
     * likely to change the current target. For example, if the user selects a
     * menu item to activate your code, the act of opening the menu would clear
     * the current target, causing {@link #getCurrentMarkupTarget()} to return
     * {@code null}.
     *
     * @return the most recent valid markup target, or {@code null}
     * @see #getCurrentMarkupTarget()
     */
    public MarkupTarget getMarkupTarget() {
        if (lastTarget == null) {
            return null;
        }
        return MarkupTargetFactory.createMarkupTarget(lastTarget, true);
    }

    /**
     * Returns the current application-wide markup target, or {@code null} if
     * there is no currently valid target.
     *
     * @return the current markup target, or {@code null}
     * @see #getMarkupTarget()
     * @see #requestNewMarkupTarget(java.lang.Object)
     */
    public MarkupTarget getCurrentMarkupTarget() {
        if (currentTarget == null) {
            return null;
        }
        return MarkupTargetFactory.createMarkupTarget(currentTarget, true);
    }

    /**
     * Explicitly requests that the current application-wide
     * {@link MarkupTarget} be set to a specific object. If the request
     * succeeds, {@code true} is returned. In order to succeed, the new target
     * must either be {@code null} (to clear the current target) or a (strictly)
     * valid markup target. If the current target changes, a property change
     * event will be fired for {@code MARKUP_TARGET_PROPERTY}.
     *
     * @param potentialTarget the object which may become the new target
     * @return {@code true} if the requested object is the markup target when
     * this method returns
     * @see MarkupTargetFactory#isValidTarget(java.lang.Object, boolean)
     */
    public boolean requestNewMarkupTarget(Object potentialTarget) {
        if (potentialTarget == null || MarkupTargetFactory.isValidTarget(potentialTarget, true)) {
            if (currentTarget != potentialTarget) {
                Object oldValue = currentTarget;
                currentTarget = potentialTarget;
                if (currentTarget != null) {
                    lastTarget = currentTarget;
                }
                pcs.firePropertyChange(MARKUP_TARGET_PROPERTY, oldValue, potentialTarget);
            }
            return true;
        }
        return false;
    }

    private Object currentTarget; // has focus + valid
    private Object lastTarget; // most recent non-null currentTarget

    /**
     * Inserts a string into the most recently valid application-wide markup
     * target. The current selection, if any, will be replaced. If there is no
     * such target, this method has no effect.
     *
     * @param markup the markup text to insert into the markup target
     * @see MarkupTarget#setSelectedText(java.lang.String)
     */
    public void insertMarkup(String markup) {
        MarkupTarget t = getMarkupTarget();
        if (t != null) {
            t.setSelectedText(markup);
        }
    }

    /**
     * Surrounds the selection in the current markup target with the prefix and
     * suffix. If the selection is already surrounded by prefix and suffix, or
     * if the selection already includes the prefix and suffix, then the prefix
     * and suffix are instead deleted from the selection. If no markup target is
     * available, this method has no effect.
     *
     * @param prefix the text to insert before the selection
     * @param suffix the text to insert after the selection
     * @see MarkupTarget#tagSelectedText(java.lang.String, java.lang.String,
     * boolean)
     */
    public void insertMarkupTags(String prefix, String suffix) {
        MarkupTarget t = getMarkupTarget();
        if (t != null) {
            t.tagSelectedText(prefix, suffix, false);
        }
    }

    /**
     * Returns the global named object database for the application. The named
     * object database associates names with objects so that they can be shared
     * between scripts. Two common uses for this database:
     * <ol>
     * <li> Creating an object to hold functions that will be used by multiple
     * scripts later on. For example, a plug-in that adds new game component
     * types could register a named object in its extension plug-in that defines
     * a library of functions used by the individual DIY scripts. This can be
     * more efficient than calling {@code useLibrary} in every component script
     * because the library code only needs to be compiled once.
     * <li> Sharing information between scripts, or between different runs of
     * the same script. For example, one script could store information about a
     * game that is used by other scripts later on. The scripts that use this
     * data might not even be from the same plug-in.
     * </ol>
     *
     * <p>
     * Once set, a named object will remain available until it is explicitly
     * removed or the application terminates. If scripts in different plug-ins
     * need to access a named object, you should set the bundle priorities in
     * the root files of the various plug-ins to ensure that the plug-in that
     * creates the named object is loaded before the plug-ins that rely on it.
     *
     * <p>
     * The database returned from this method provides special support for
     * scripting. When accessed from a script, the database appears to be an
     * object whose properties are the names of stored objects. Named objects
     * can thus be set and looked up using array and property syntax:
     * <pre>
     * Eons.namedObjects.myNamedObject = function() { println( 'hello' ); };
     * Eons.namedObjects.myNamedObject();
     * // A name cannot be used if it already has an object assigned to it,
     * // but you can remove the association with the delete keyword:
     * delete Eons.namedObjects.myNamedObject;
     * </pre>
     *
     * @return the global shared named object database
     */
    public NamedObjectDatabase getNamedObjects() {
        return nob;
    }
    private NamedObjectDatabase nob = new ScriptableNamedObjectDatabase();

    /**
     * Initiates a bug report that can be submitted by the user. An optional
     * description and exception information will be included as part of the
     * report if provided. The exact details of how the report is filed and what
     * steps the user must complete are subject to change.
     *
     * @param description text to include with the report, or {@code null}
     * @param ex an exception that is relevant to the bug being reported, or
     * {@code null}
     */
    public void fileBugReport(String description, Throwable ex) {
        setWaitCursor(true);
        StringBuilder reportBuff = new StringBuilder(BUG_REPORT_CHAR_LIMIT + 256);
        try {
            description = description == null ? "" : description.trim();
            if (description.isEmpty()) {
                description = "<please describe the problem>";
            }

            final Properties p = System.getProperties();
            reportBuff.append(description)
                    .append("\n\nBug: Strange Eons ").append(getBuildNumber())
                    .append("\nPlatform: ").append(p.getProperty("os.name"))
                    .append(" (v").append(p.getProperty("os.version"))
                    .append('-').append(p.getProperty("os.arch"))
                    .append('-').append(Runtime.getRuntime().availableProcessors())
                    .append(" CPUs)\nJRE: ").append(p.getProperty("java.version"))
                    .append(' ').append(p.getProperty("java.vendor")).append('\n');

            if (ex != null) {
                reportBuff.append("\nException:\n").append(ex);
                StackTraceElement[] el = ex.getStackTrace();
                int i = 0;
                for (; i < el.length && reportBuff.length() < BUG_REPORT_CHAR_LIMIT; ++i) {
                    final String entry = el[i].toString()
                            .replace("ca.cgjennings.apps.arkham.", "arkham.")
                            .replace("ca.cgjennings.", "ca...");
                    reportBuff.append('\n').append(entry);
                }
            }

            // convert message to URL, then limit encoded URL to max length
            String url;
            String message = reportBuff.toString();
            do {
                url = "https://cgjennings.ca/contact/?"
                        + URLEncoder.encode(message, "utf-8");
                if (url.length() > BUG_REPORT_CHAR_LIMIT) {
                    url = null;
                    int lengthToTry = Math.min(BUG_REPORT_CHAR_LIMIT - 32, message.length() - 16);
                    message = message.substring(0, lengthToTry) + "...";
                }
            } while (url == null);
            DesktopIntegration.browse(new URI(url));
        } catch (IOException | URISyntaxException | UnsupportedOperationException e) {
            UIManager.getLookAndFeel().provideErrorFeedback(null);
            log.log(Level.SEVERE, "exception while opening browser, report put on clipboard", e);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(reportBuff.toString()), null);
        } finally {
            setWaitCursor(false);
        }
    }
    private static final int BUG_REPORT_CHAR_LIMIT = 2000; // approx limit for IE < 11

    /**
     * Returns the current contents of the application log. The application log
     * contains the messages posted to the logger, except those filtered out
     * because of the logging level that was active at the time.
     *
     * @return the contents of the application log
     */
    public static List<LogEntry> getLogEntries() {
        return immutableLogEntries;
    }

    /**
     * This is a convenience method that sets or unsets a wait cursor on the
     * main application window. If the application window is not available yet,
     * then this method does nothing. Otherwise, it forwards the call to the
     * application window as if you had called
     * {@code StrangeEons.getWindow().setWaitCursor( appIsBusy )}.
     *
     * <p>
     * This method nests: if you set a wait cursor four times, then you must
     * unset it four times before a regular cursor will be restored. To ensure
     * that the wait cursor is removed, you should place the call to unset the
     * wait cursor in a {@code finally} clause:
     * <pre>
     * StrangeEons.setWaitCursor( true );
     * try {
     *     // do some things
     * } finally {
     *     StrangeEons.setWaitCursor( false );
     * }
     * </pre>
     *
     * <p>
     * This method can be called from outside of the event dispatch thread.
     *
     * @param appIsBusy {@code true} to set the wait cursor, {@code false} to
     * unset it
     */
    public static void setWaitCursor(boolean appIsBusy) {
        AppFrame af = AppFrame.getApp();
        if (af != null) {
            if (appIsBusy) {
                af.setWaitCursor();
            } else {
                af.setDefaultCursor();
            }
        }
    }

    /**
     * Returns the command line arguments that were passed to the application
     * from the command line when the application was started.
     *
     * @return the command line arguments passed to the application
     */
    public CommandLineArguments getCommandLineArguments() {
        return commandLineArguments.clone();
    }

    /**
     * Shared instance of the command line arguments for internal use only. The
     * fields of the returned object must not be modified.
     *
     * @see #getCommandLineArguments()
     */
    final CommandLineArguments commandLineArguments;

    /**
     * Sets the information message displayed on the splash window during
     * application startup. If the splash window is not being shown, this method
     * has no effect.
     *
     * @param message the message to display
     * @throws NullPointerException if {@code message} is {@code null}
     */
    void setStartupActivityMessage(String message) {
        if (message == null) {
            throw new NullPointerException("message");
        }
        SplashWindow lSplash;
        synchronized (SplashWindow.class) {
            lSplash = splash;
            if (lSplash == null) {
                return;
            }
        }
        lSplash.setActivity(message);
    }
    private SplashWindow splash;

    /**
     * Returns a window that can be used as a parent window for dialog boxes.
     * During startup, this method will return the splash window. Once the
     * application window is visible, that will be returned instead. In the
     * event that this method is called before the splash window appears, this
     * method will return {@code null}; however it is not normally possible for
     * plug-in code to execute before the splash window is available.
     *
     * <p>
     * Note that plug-ins should not routinely open dialog boxes during
     * application startup; this method is meant to be used primarily for
     * displaying critical error messages.
     *
     * @return a window that can safely be used as a parent window
     * @see ErrorDialog#displayError(java.lang.String, java.lang.Throwable)
     * @throws AssertionError if called from outside the Event Dispatch Thead
     */
    public static Window getSafeStartupParentWindow() {
        JUtilities.threadAssert();
        AppFrame af = AppFrame.getApp();
        if (af == null || !(af.isVisible())) {
            if (instance != null) {
                SplashWindow splash = instance.splash;
                if (splash != null) {
                    splash.setAlwaysOnTop(false);
                }
                return splash;
            }
            return null;
        }
        return af;
    }

    /**
     * This method is called when the application first starts as part of
     * passing control of the process to the application. Calling this method
     * again will throw an exception.
     *
     * @param args the command line arguments being passed to the application
     * @throws IllegalStateException if the method has already been called
     */
    public static void main(String[] args) {
        synchronized (StrangeEons.class) {
            if (alreadyRanMain) {
                throw new IllegalStateException("main() already called");
            }
            alreadyRanMain = true;
        }
        try {
            // The constructor itself records the instance since it is needed during
            // the initialization process, so we do nothing with the return value.
            instance = new StrangeEons(args);
            instance.initialize();
        } catch (Throwable t) {
            // Log the message now, in case fatalError fails
            log.log(Level.SEVERE, "Uncaught exception creating app instance", t);

            // default message in case string() fails
            String message = "Uncaught exception during initialization";
            try {
                message = string("rk-err-uncaught");
            } catch (Exception ex) {
                log.log(Level.SEVERE, null, t);
            }
            ErrorDialog.displayFatalError(message, t);
        }
    }
    private static boolean alreadyRanMain;

    /**
     * Stage 1 is meant to perform the lowest-level possible initialization
     * before handing startup off to Stage 2 (which takes place in the EDT). For
     * a number of reasons, Stage 1 must not be called from the EDT. (One
     * reason, though, is that the animation of the splash window depends on
     * this.)
     *
     * @throws Throwable any unhandled exceptions will be caught by a top-level
     * catch
     */
    private void initStage1(String rendererLogMsg, String textAALogMsg) throws Throwable {
        if (EventQueue.isDispatchThread()) {
            throw new AssertionError("must NOT be called from EDT");
        }

        final long startTime = System.currentTimeMillis();

        // Adjust logger level from ALL to command line level
        log.setLevel(commandLineArguments.loglevel);

        initCheckSystemConfig(rendererLogMsg, textAALogMsg);

        // if restarting the app, wait for the previous instance to exit
        if (commandLineArguments.xRestartLock != null) {
            initAcquireRestartLock(commandLineArguments.xRestartLock);
        }

        // must be done before splash screen is shown
        initScriptRunnerMode(commandLineArguments.run);

        // Read user prefs: needed so that we can check
        // if user wants single instance; has no effect if the --resetprefs
        // flag was set on the command line
        RawSettings.readUserSettings();

        if (commandLineArguments.plugintest == null) {
            // If in single instance mode and another instance exists, this
            // method will not return.
            initApplySingleInstanceLimit();
        } else {
            // Never limit to single instance when a plug-in test is done,
            // since there is (typically) a parent instance starting us.
            boolean allAccepted = true;
            LinkedList<File> bundles = new LinkedList<>();
            for (String f : commandLineArguments.plugintest.split(Pattern.quote(File.pathSeparator))) {
                if (f.isEmpty()) {
                    continue;
                }
                File bf = new File(f).getAbsoluteFile();
                if (bf.exists() && !bf.isDirectory()) {
                    bundles.add(bf);
                    StrangeEons.log.log(Level.INFO, "accepted test bundle {0}", bf);
                } else {
                    System.err.println("not a plug-in bundle: \"" + f + '"');
                    allAccepted = false;
                }
            }
            if (bundles.isEmpty()) {
                System.err.println("no plug-in bundle listed");
                allAccepted = false;
            }
            if (!allAccepted) {
                System.exit(10);
            }
            BundleInstaller.setTestBundles(bundles.toArray(File[]::new));
        }

        if (commandLineArguments.xDisableFilterThreads) {
            SplitJoin.setParallelExecutionEnabled(false);
        }

        // At this point, if we have temporarily started a second instance
        // (say, by opening an .eon file while the app is already running)
        // then control has passed to that instance and this instance has stopped.
        // Therefore, we can now open the splash window without fear of confusing
        // the user.
        //
        // Note: Consider carefully before putting any code above this line.
        //       Until this method is called, there is no parent window to use
        //       in case an error or other dialog must be shown.
        initSplashWindow();

        // Init UI and game language
        splash.setActivity(string("init-starting"));
        splash.setPercent(0);
        initLanguage();

        // check Java version and display localized message if not new enough
        initCheckJREVersion(!commandLineArguments.xDisableJreCheck);

        // Load default global settings:
        // deferred until just after Language is set up since
        // it may display an error dialog
        RawSettings.loadGlobalSettings("default.settings");

        splash.setPercent(3);
        initUserStorageFolders();
        initNetworkProtocols();
        initImageIO();
        splash.setPercent(5);

        initBundleUpdater();
        splash.setPercent(10);

        // Install libs before spelling componenents since the spelling engine
        // wants the dictionary library if it exists, and also before the EDT init
        // since libs are guaranteed to be installed before themes.
        splash.setActivity(string("init-libs"));
        BundleInstaller.loadLibraryBundles();
        splash.setPercent(15);

        // Both Stage 1 AND the initial boot thread will end at this point:
        // Stage 2 will be executed from the EDT, although it may also start
        // separate background threads to perform some tasks in parallel.
        EventQueue.invokeLater(() -> {
            try {
                if (commandLineArguments.xDebugException) {
                    throw new InternalError("--XDebugException flag was set on command line");
                }
                initStage2(startTime);
            } catch (Throwable t) {
                log.log(Level.SEVERE, "Uncaught Exception in Stage 2", t);
                ErrorDialog.displayFatalError(string("rk-err-uncaught"), t);
            }
        });
    }

    /**
     * Performs most of the application startup process. Stage 1 will cause this
     * to be called from the EDT.
     *
     * @param startTime a nanosecond timestamp taken during Stage 1; Stage 2
     * uses this to log how much wall clock time was required for startup
     */
    private void initStage2(final long startTime) throws Throwable {
        JUtilities.threadAssert();
        // Start spelling engine, but don't load the dictionary
        // (that will happen momentarily, in parallel, to speed startup).
        splash.setActivity(string("init-spelling"));
        initSpellingChecker();
        splash.setPercent(20);

        // Starts some initialization steps that doesn't need the GUI or EDT
        // in a background thread.
        startBackgroundInit();

        // If there are SE2 settings but no SE3 settings, this will silently
        // migrate compatible settings.
        RawSettings.migratePreferences();

        // Install L&F based on settings
        ThemeInstaller.install();

        // Once the L&F is installed, we can start doing things that might
        // create windows or take other L&F-sensitive actions. The app window needs
        // to exist for this, since it will normally be the parent of such windows.
        // For example, it must exist before starting extensions since
        // they might write to the console window, and the main app window
        // is the parent of the console window.
        new AppFrame();
        splash.setPercent(30);

        // Due to some bugs in Java, it can take a long time to create
        // the first file chooser in the system. It is better have such
        // a delay now while the app starts then when we want to display one.
        ResourceKit.initializeFileDialogs();
        splash.setPercent(40);
        splash.setActivity(string("init-plugins"));
        initMarkupTargetTracking();

        // If script debugging is enabled, start the debug server before
        // any script code runs (when plug-ins are installed, below). This
        // also has to be run after the app window exists so that it can
        // attach its bug icon.
        if (Settings.getShared().getYesNo("enable-script-debugger") || BundleInstaller.hasTestBundles()) {
            ScriptDebugging.install();
        }

        // init core fonts lib, if available
        ResourceKit.getBodyFamily();
        // Wait for the background init thread to finish before continuing:
        // we want to make sure it is done before extensions are loaded in case
        // they rely on anything it sets up.
        waitForBackgroundInit();
        splash.setPercent(50);

        BundleInstaller.loadExtensionBundles((Object source, float progress) -> {
            if (splash != null) {
                splash.setActivity(source.toString());
                splash.setPercent(50 + (int) (25f * progress));
            }
            return true;
        });
        splash.setPercent(75);

        // Lock the game database now, before loading regular plug-ins
        Lock.setLocked(true);

        splash.setPercent(85);
        BundleInstaller.loadPluginBundles();

        splash.setActivity("Iä! Iä! Cthulhu F'thagn!");
        splash.setPercent(95);

        // Finally!
        // The app window was created earlier, so
        // that it was available as a parent window, but it isn't visible yet.
        // This call finishes setting up the GUI window and makes it visible.
        ((AppFrame) StrangeEons.getWindow()).displayApplication();

        // The window is now theoretically visible, but it actually takes a
        // while for the native window system to do its thing. The invoke later
        // gives the window a chance to appear before hiding the splash window.
        EventQueue.invokeLater(() -> {
            try {
                final SplashWindow sp = splash;
                synchronized (SplashWindow.class) {
                    splash = null;
                }
                final double time = (System.currentTimeMillis() - startTime) / 1000d;
                log.log(Level.INFO, String.format("startup took %.2f s", time));

                // Before closing the splash window, check if it has any
                // child windows attached via getSafeStartupParentWindow.
                // If so, move it to the front and wait for the user to
                // close those windows.
                final int lingerCount = checkForLingeringStartupWindows(sp, true);
                if (lingerCount > 0) {
                    StrangeEons.log.log(Level.INFO, "{0} lingering startup window(s) detected", lingerCount);
                    new LingeringStartupWindowPresenter(sp, lingerCount);
                } else {
                    sp.dispose();
                }
            } catch (Exception e) {
                log.logp(Level.SEVERE, getClass().getName(), "initStage2", "exception while closing splash window", e);
            }
        });
    }

    private static int checkForLingeringStartupWindows(Window startupParent, boolean moveToFront) {
        int lingerCount = 0;
        for (Window w : startupParent.getOwnedWindows()) {
            ++lingerCount;
            if (moveToFront) {
                moveToFront = false;
                startupParent.toFront();
                w.toFront();
            }
        }
        return lingerCount;
    }

    private static class LingeringStartupWindowPresenter {

        int lastCount;
        Window splash;
        boolean moveToFront;

        public LingeringStartupWindowPresenter(Window owner, int startCount) {
            lastCount = startCount;
            splash = owner;
            new Timer(200, (ActionEvent e) -> {
                int lingerCount = checkForLingeringStartupWindows(splash, moveToFront);

                if (lingerCount == 0) {
                    StrangeEons.log.info("all lingering startup windows closed");
                    ((Timer) e.getSource()).stop();
                    splash.dispose();
                    return;
                }

                if (lingerCount != lastCount) {
                    StrangeEons.log.log(Level.INFO, "{0} lingering startup window(s) remain", lingerCount);
                    moveToFront = true;
                    lastCount = lingerCount;
                }
            }).start();
        }
    }

    /**
     * Creates and displays the splash window. Before this method returns, the
     * splash window will exist, be visible, and have completed its opening
     * animation. (The last point is important because otherwise the animation
     * will fight with initStage2() for access to the event thread.
     *
     * @throws Throwable any exception that occurs while creating the window
     */
    private void initSplashWindow() throws Throwable {
        synchronized (this) {
            GraphicsConfiguration gc = null;
            String displayID = RawSettings.getUserSetting("appframe-device");
            if (displayID != null) {
                GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
                for (GraphicsDevice gd : env.getScreenDevices()) {
                    if (displayID.equals(gd.getIDstring())) {
                        gc = gd.getDefaultConfiguration();
                    }
                }
            }
            final GraphicsConfiguration SELECTED_GC = gc;
            if (EventQueue.isDispatchThread()) {
                splash = new SplashWindow(SELECTED_GC, !commandLineArguments.xDisableAnimation);
            } else {
                try {
                    EventQueue.invokeAndWait(() -> {
                        synchronized (SplashWindow.class) {
                            splash = new SplashWindow(SELECTED_GC, !commandLineArguments.xDisableAnimation);
                        }
                    });
                } catch (InvocationTargetException ite) {
                    throw ite.getCause();
                }
            }
            try {
                if (!commandLineArguments.xDisableAnimation) {
                    // we will be notified when the animation completes;
                    // won't be interrupted, but if it is won't be a problem;
                    // just repaints the window
                    wait(SplashWindow.ANIMATION_TIME_MS + 10_000);
                }
            } catch (InterruptedException e) {
                // not a problem
            }
        }
    }

    /**
     * Checks and logs basic system configuration: build, Java version, OS,
     * architecture and processor count.
     */
    private static void initCheckSystemConfig(String rendererLogMsg, String textAALogMsg) {
        MemoryMXBean b = ManagementFactory.getMemoryMXBean();
        long heap = b.getHeapMemoryUsage().getMax();
        long nonHeap = b.getNonHeapMemoryUsage().getMax();

        // Log basic system configuration for debugging
        if (log.isLoggable(Level.INFO)) {
            log.log(Level.INFO, "Build {0} on JRE {1}, {2} v{3}, {4} with {5} CPU(s), {6} MB heap size", new Object[]{
                getBuildNumber(), System.getProperty("java.version"),
                System.getProperty("os.name"), System.getProperty("os.version"),
                System.getProperty("os.arch"), Runtime.getRuntime().availableProcessors(),
                heap < 0 ? "<undefined>" : Integer.valueOf((int) Math.ceil(heap / (1024d * 1024d)))
            });
            StrangeEons.log.log(Level.INFO, "User storage folder located at \"{0}\"", USER_STORAGE_FOLDER);

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] devices = ge.getScreenDevices();
            for (int dev=0; dev<devices.length; ++dev) {
                final GraphicsDevice gd = devices[dev];
                DisplayMode dm = gd.getDisplayMode();
                GraphicsConfiguration gc = gd.getDefaultConfiguration();
                Rectangle bounds = gc.getBounds();
                double scale = Math.round(gc.getDefaultTransform().getScaleX() * 100d);
                StrangeEons.log.log(Level.INFO,
                        "screen{0} x = {1} y = {2}, {3} × {4} @ {5}%, {6} bit, {7} Hz",
                        new Object[]{
                            dev, bounds.x, bounds.y, bounds.width, bounds.height, scale,
                            dm.getBitDepth(), dm.getRefreshRate()
                        }
                );
                StrangeEons.log.info(gc.getColorModel().toString());
            }

            if (rendererLogMsg != null) {
                StrangeEons.log.log(Level.INFO, rendererLogMsg);
            }
            if (textAALogMsg != null) {
                StrangeEons.log.log(Level.INFO, textAALogMsg);
            }
        }

        // Check that we were started with enough memory and log a warning if we were not
        // The minumum total memory we will accept; we set this to a high percentage
        // of the actual desired number of bytes, because some of the memory set
        // with -Xmx is not accounted for in heap + nonheap.
        final long MIN_MEM_IN_BYTES = RECOMMENDED_MEMORY_IN_MB * (1024L * 1024L) * (95L / 100L);

        // The memory values are allowed to be -1 if undefined; in which case we
        // have to assume that we have whatever we need.
        long checksum = (heap < 0 ? MIN_MEM_IN_BYTES : heap) + (nonHeap < 0 ? 0 : nonHeap);

        if (checksum < MIN_MEM_IN_BYTES) {
            log.log(Level.WARNING, "the maximum memory setting is less than the recommended value (-Xmx{0}m)", RECOMMENDED_MEMORY_IN_MB);
        }
    }
    
    /**
     * Checks that installed version of Java is compatible. If it isn't,
     * displays a dialog and exits. This is separate from the rest of
     * {@link #initCheckSystemConfig()} so that it can display a localized
     * dialog box.
     */
    @SuppressWarnings("all") // comparison of possibly equal versions
    private void initCheckJREVersion(boolean fatalIfNotSupported) {
        int[] ver = getJavaVersion();
        final boolean isSupportedVersion = (ver[0] >= JAVA_VERSION_MIN) && (ver[0] <= JAVA_VERSION_MAX);
        if (!isSupportedVersion) {
            if (fatalIfNotSupported) {
                try {
                    EventQueue.invokeAndWait(() -> {
                        // no L&F installed yet
                        try {
                            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException t) {
                            // ignore, use whatever L&F is available
                        }
                        final String errorMessage;
                        if (JAVA_VERSION_MIN == JAVA_VERSION_MAX) {
                            errorMessage = string("rk-err-java-version", Integer.toString(JAVA_VERSION_MIN));
                        } else {
                            errorMessage = string("rk-err-java-version-range", Integer.toString(JAVA_VERSION_MIN), Integer.toString(JAVA_VERSION_MAX));
                        }
                        JOptionPane.showMessageDialog(
                                getSafeStartupParentWindow(),
                                errorMessage, "Strange Eons",
                                JOptionPane.ERROR_MESSAGE
                        );
                        System.exit(20);
                    });
                } catch (InterruptedException | InvocationTargetException e) {
                    log.log(Level.SEVERE, "JRE version check failed", e);
                }
            } else {
                log.log(Level.WARNING, "running on unsupported JRE version {0}", ver[0]);
            }
        }
    }

    /**
     * Try to delete the locked temp file created by the parent instance.
     *
     * @param lockFile the file to acquire the lock for and delete
     */
    private void initAcquireRestartLock(File lockFile) {
        log.info("restarted instance is running, trying to acquire lock");

        // try waiting up to N*DELAY ms for the last instance to terminate
        final int N = 30;

        boolean gotLock = false;
        final int DELAY = 200;
        final int ATTEMPTS = N * 1_000 / DELAY;

        for (int i = 0; i < ATTEMPTS; ++i) {
            if (!lockFile.exists() || lockFile.delete()) {
                gotLock = true;
                break;
            }
            try {
                // dirty but I'm open to reliable replacements
                Thread.sleep(DELAY);
            } catch (InterruptedException e) {
            }
        }
        if (gotLock) {
            log.info("acquired lock, starting application");
        } else {
            log.warning("did not acquire lock; will continue starting anyway");
        }
    }

    /**
     * If limited to a single instance, establish this as that instance or else
     * find the already running instance and pass our file arguments to it
     * before exiting.
     */
    private void initApplySingleInstanceLimit() {
        String limit = RawSettings.getUserSetting("limit-to-single-instance");
        if (limit == null) {
            limit = "yes";
        }
        limit = limit.toLowerCase(Locale.CANADA);
        if (limit.equals("yes") || limit.equals("true")) {
            String[] filesToOpen = commandLineArguments.getFiles();
            if (!InstanceController.makeExclusive("Strange Eons", 0x5445, filesToOpen, (boolean isInitialInstance, String[] filesToOpen1) -> {
                // The open queue will be checked when the app starts
                // and periodically thereafter. When this is the initial
                // instance starting, this places any files on the command
                // line in the queue to open later. When a second instance
                // starts, its file arguments will be sent to the primary
                // instance via a socket connection by InstanceController,
                // added to the queue by this code, and eventually opened.
                for (String file : filesToOpen1) {
                    AppFrame.addFileToOpenQueue(new File(file));
                }
                // If the window exists, then we are getting args from
                // another instance and should ensure that the window is
                // visible and the files get opened.
                final AppFrame win = AppFrame.getApp();
                if (win != null) {
                    EventQueue.invokeLater(() -> {
                        if ((win.getExtendedState() & Frame.ICONIFIED) != 0) {
                            win.setExtendedState(win.getExtendedState() & ~Frame.ICONIFIED);
                        }
                        win.toFront();
                        win.requestFocusInWindow();
                        win.openFilesInQueue();
                    });
                }
                return true;
            })) {
                System.exit(0);
            }
        }
    }

    /**
     * Initializes the UI and game language based on the command line, user
     * settings, and/or default locale.
     */
    private void initLanguage() {
        Locale uiLoc = pickLocale(commandLineArguments.ulang, "default-ui-locale");
        Locale gLoc = pickLocale(commandLineArguments.glang, "default-game-locale");
        Language.setInterfaceLocale(uiLoc); // also sets default locale
        Language.setGameLocale(gLoc);
    }

    /**
     * Picks the first available locale in order of preference. If the command
     * line locale is not {@code null}, returns that. Otherwise, if the user
     * setting key specifies a valid locale, returns that. Otherwise, returns
     * {@code Locale.getDefault()}.
     *
     * @param commandLine the locale specified on the command line, or
     * {@code null}
     * @param userSettingKey the user setting key to check for a locale
     * @return the first available locale in command line, user settings,
     * default order
     */
    private static Locale pickLocale(Locale commandLine, String userSettingKey) {
        if (commandLine != null) {
            return commandLine;
        }
        String value = RawSettings.getUserSetting(userSettingKey);
        if (value != null && !value.isEmpty()) {
            return Language.parseLocaleDescription(value);
        }
        return Locale.getDefault();
    }

    /**
     * Ensures that ~/.StrangeEons3/plugins exists, creating it if it doesn't.
     * If either folder exists but is not a directory, a fatal error is raised.
     */
    private static void initUserStorageFolders() {
        if (!USER_STORAGE_FOLDER.exists()) {
            USER_STORAGE_FOLDER.mkdirs();
        }
        if (!USER_STORAGE_FOLDER.isDirectory()) {
            ErrorDialog.displayFatalError(string("plug-err-plugin-nodir", USER_STORAGE_FOLDER.getAbsolutePath()), null);
        }
        File plugins = BundleInstaller.PLUGIN_FOLDER;
        if (!plugins.exists()) {
            plugins.mkdir();
        }
        if (!plugins.isDirectory()) {
            ErrorDialog.displayFatalError(string("plug-err-plugin-nodir", plugins.getAbsolutePath()), null);
        }
    }

    /**
     * Installs proxy support and URL handlers.
     */
    private static void initNetworkProtocols() {
        // set a custom user agent for HTTP requests
        System.setProperty("http.agent", "StrangeEons/" + StrangeEons.getBuildNumber());
        // init proxy support
        NetworkProxy.install();
        // install custom protocol handlers
        ca.cgjennings.io.protocols.MappedURLHandler.install();
    }

    /**
     * Initializes JPEG2000 support and other image I/O features.
     */
    private static void initImageIO() {
        if (JPEG2000.registerServiceProviders(true)) {
            log.info("using Strange Eons JPEG2000 lib; JP2 I/O will not use native code");

            // Parallel decoder disabled: entropy coder never lets its threads die
//			final int cpus = Runtime.getRuntime().availableProcessors();
//			if( cpus > 1 ) {
//				System.setProperty( "jj2000.j2k.entropy.encoder.StdEntropyCoder.nthreads", String.valueOf( cpus ) );
//				log.info( "multiple CPUs detected: enabled concurrent entropy encoder" );
//			}
        } else {
            log.info("discovered external JPEG2000 lib; JP2 I/O might use native code");
        }
        ImageIO.setUseCache(false);
    }

    /**
     * Attempts to delete files submitted for startup deletion. Called by
     * {@link #initBundleUpdater()}.
     *
     * @param warn logs a warning if the delete fails
     * @param keepTrying if deletion fails, do not remove from list
     */
    private void initDeleteMarkedFiles(boolean warn, boolean keepTrying) {
        LinkedList<String> failedFiles = new LinkedList<>();

        for (int delNum = 1; RawSettings.getUserSetting(DELETE_ON_STARTUP_KEY_PREFIX + delNum) != null; ++delNum) {
            String name = RawSettings.getUserSetting(DELETE_ON_STARTUP_KEY_PREFIX + delNum);
            RawSettings.removeUserSetting(DELETE_ON_STARTUP_KEY_PREFIX + delNum);
            File ftd = new File(name);
            if (ftd.exists() && isInUserStorage(ftd)) {
                if (!ftd.delete()) {
                    if (warn) {
                        log.log(Level.WARNING, "failed to delete on startup: {0}", ftd);
                    }
                    if (keepTrying) {
                        failedFiles.add(name);
                    }
                }
            }
        }

        // write new list of failed files if keepTrying == true
        int i = 1;
        for (String file : failedFiles) {
            RawSettings.setUserSetting(DELETE_ON_STARTUP_KEY_PREFIX + (i++), file);
        }
    }

    /**
     * Deletes files marked for startup-time deletion (typically uninstalled
     * bundles) and then copies or renames .autoupdate bundles.
     */
    private void initBundleUpdater() {
        // process delete-on-startup files
        initDeleteMarkedFiles(true, true);
        // added to fix a problem with bundles not being re-installable;
        // should be reinvestigated
        RawSettings.removeUserSetting("uninstalled-plugins");
        // apply autoupdates
        BundleInstaller.applyPendingBundleUpdates();
    }

    /**
     * Creates the global spelling checker instance and tries to load the user's
     * learned word list.
     */
    private static void initSpellingChecker() {
        SpellingChecker s = new SpellingChecker(Language.getGameLocale());
        SpellingChecker.setSharedInstance(s);

        File userFile = StrangeEons.getUserStorageFile("learned-spelling-words");
        s.setUserDictionaryFile(userFile);
        s.setAutoWriteUserDictionaryFile(true);

        if (userFile.exists()) {
            try {
                s.readUserDictionary();
            } catch (IOException e) {
                log.log(Level.SEVERE, "unable to read user dictionary", e);
                ErrorDialog.displayError(string("rk-err-load-user-dictionary"), e);
            }
        }
    }

    /**
     * Loads the standard spelling dictionary for the game language, if
     * installed. If not installed, a listener is installed that will listen for
     * the dictionary plug-in to be installed and load the dictionary then.
     */
    private static synchronized void initSpellingDictionaries() {
        if (loadedSpellingDictionaries) {
            return;
        }

        final SpellingChecker s = SpellingChecker.getSharedInstance();

        if (CoreComponents.SPELLING_COMPONENTS.getInstallationState() != VersioningState.NOT_INSTALLED) {
            s.installDefaultPolicy();
            try {
                s.installDefaultDictionary();
                log.log(Level.FINE, "loaded spelling dictionary for {0}", Language.getGame());
            } catch (IOException e) {
                log.log(Level.WARNING, "failed to load spelling dictionary", e);
            }
            loadedSpellingDictionaries = true;
            // if the user doesn't actually want spelling checking, setting this
            // policy will prevent any words from being flagged
            if (!Settings.getUser().getYesNo("spelling-enabled")) {
                s.setPolicy(new AcceptPolicy());
            }
        } else {
            Catalog.addPostInstallationHook(new Runnable() {
                @Override
                public void run() {
                    if (CoreComponents.SPELLING_COMPONENTS.getInstallationState() != VersioningState.NOT_INSTALLED) {
                        Catalog.removePostInstallationHook(this);
                        initSpellingDictionaries();
                    }
                }

                @Override
                public String toString() {
                    return "dictionary loader";
                }
            });
            // Since no dictionary is available, we set the AcceptPolicy
            // as otherwise everything (except whatever is in the user
            // dictionary) will be flagged.
            s.setPolicy(new AcceptPolicy());
            log.info("spelling dictionary library missing; installed catalog hook");
        }
    }
    private static boolean loadedSpellingDictionaries;

    private void initBuiltinEditors() {
        try {
            ClassMap.add("editors/minimal.classmap");
            ConversionMap.add("editors/minimal.conversionmap");
            splash.setPercent(45);
            Silhouette.add("silhouettes/standard.silhouettes");
        } catch (IOException | RuntimeException ex) {
            log.log(Level.SEVERE, "failed to create built-in editor data", ex);
            ErrorDialog.displayError(string("rk-err-gamedata"), ex);
        }
    }

    /**
     * Tweaks settings of various UI classes by platform and effects settings.
     */
    private static void initSpecialEffects() {
        // disable special effects if requested
        if (!Settings.getShared().getYesNo("advanced-ui-effects")) {
            StyleUtilities.setOpacityChangeEnabled(false);
        }
    }

    private void initMarkupTargetTracking() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", (PropertyChangeEvent e) -> {
            requestNewMarkupTarget(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
        });
    }

    private Thread backgroundInitThread;

    /**
     * Starts running initialization tasks that can be performed in the
     * background. To function correctly, background initialization must be
     * started after libraries are loaded and must be completed before
     * extensions are loaded. These tasks may not actually run in the
     * background, if disabled by the
     * {@linkplain CommandLineArguments#xDisableStartupThreads command line arguments}.
     *
     * @see #waitForBackgroundInit()
     */
    private void startBackgroundInit() {
        final Runnable backgroundInit = () -> {
            try {
                // Ensure cloud fonts user has reserved are up to date;
                // if at least one, this will also update the font metadata
                CloudFonts.installReservedFamilies();

                // Dicionary loading is expensive and I/O bound, so probably
                // a good candidate to do in parallel.
                initSpellingDictionaries();

                // MetadataSource has a LOT of icons to load. This line of code causes
                // the class to be loaded now, because otherwise it loads just as the
                // app window is becoming visible and freezes it for several seconds.
                Class.forName(MetadataSource.class.getName(), true, getClass().getClassLoader());

                // Populate GameData for built-in editors
                initBuiltinEditors();

                // Init the delay for polling open files for changes.
                FileChangeMonitor.setDefaultCheckPeriod(Settings.getShared().getInt("file-monitoring-period"));

                initSpecialEffects();

                // Allow the SE script system to evaluate <script> tags in markup boxes.
                MarkupRenderer.setEvaluatorFactory(new StrangeEonsEvaluatorFactory());
            } catch (ClassNotFoundException | RuntimeException ex) {
                log.log(Level.SEVERE, "Uncaught Exception during background initialization: showing fatal error", ex);
                if (splash != null) {
                    try {
                        splash.dispose();
                    } catch (Exception disposeEx) {
                        // just try to show errror
                    }
                }
                ErrorDialog.displayFatalError(string("rk-err-uncaught"), ex);
            }
        };

        if (commandLineArguments.xDisableStartupThreads) {
            log.info("performing \"background\" initialization in main thread");
            backgroundInit.run();
        } else {
            log.info("starting background initialization");
            backgroundInitThread = new Thread(backgroundInit, "Background init thread");
            backgroundInitThread.setDaemon(true);
            backgroundInitThread.start();
        }
    }

    /**
     * Waits for the background initialization thread to finish and then
     * returns. This method is called to synchronize the initialization process.
     * It can only be called from the EDT.
     */
    private void waitForBackgroundInit() {
        JUtilities.threadAssert();
        if (backgroundInitThread != null) {
            log.info("waiting for background initialization to complete");
            long start = System.currentTimeMillis();
            boolean done = false;
            while (!done) {
                try {
                    backgroundInitThread.join();
                    done = true;
                } catch (InterruptedException intEx) {
                    // won't be interrupted, but in any case it must complete
                }
            }
            backgroundInitThread = null; // allow GC
            log.log(Level.FINE, "background initialization completed (waited {0}s)", (System.currentTimeMillis() - start) / 1000d);
        }
    }

    /**
     * Returns a basic string description of the application, in the following
     * format: {@code [StrangeEons <i>version</i>, <i>release type</i>
     * release, build <i>number</i>]}.
     *
     * @return a string describing the application
     */
    @Override
    public String toString() {
        return "[StrangeEons " + VER_MAJOR + '.' + VER_MINOR + ", "
                + VER_TYPE.toString().toLowerCase(Locale.CANADA)
                + " release, build " + VER_BUILD + ']';
    }

    ////////////////////////////////////
    // PROPERTY CHANGE IMPLEMENTATION ////////////////////////////////////////
    ////////////////////////////////////
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /**
     * The name of a property change event that is fired when the current markup
     * target changes.
     */
    public static String MARKUP_TARGET_PROPERTY = "markupTarget";

    /**
     * Adds a new property change listener that will be notified when any
     * property change event is fired by the application.
     *
     * @param li the listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener li) {
        pcs.addPropertyChangeListener(li);
    }

    /**
     * Removes a previously added listener that listens for all property change
     * events.
     *
     * @param li the listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener li) {
        pcs.removePropertyChangeListener(li);
    }

    /**
     * Adds a new property change listener that will be notified when property
     * change events for the named property are fired by the application.
     *
     * @param property name of the property to listen for
     * @param li the listener to add
     */
    public void addPropertyChangeListener(String property, PropertyChangeListener li) {
        pcs.addPropertyChangeListener(property, li);
    }

    /**
     * Removes a previously added listener that listens for the named property
     * to change.
     *
     * @param property name of the property to stop listening for
     * @param li the listener to remove
     */
    public void removePropertyChangeListener(String property, PropertyChangeListener li) {
        pcs.removePropertyChangeListener(property, li);
    }

    /**
     * Register a new export container capable of exporting file collections.
     *
     * @param ec the container type to register
     * @throws NullPointerException if the container or its identifier are
     * {@code null}
     * @throws IllegalArgumentException if a container type with this type's
     * identifier is already registered
     */
    public static synchronized void registerExportContainer(ExportContainer ec) {
        if (ec == null) {
            throw new NullPointerException("ec");
        }
        String id = ec.getIdentifier();
        if (id == null) {
            throw new NullPointerException("null identifier");
        }
        for (ExportContainer i : exportContainers) {
            if (id.equals(i.getIdentifier())) {
                throw new IllegalArgumentException("identifier already in use: " + id);
            }
        }
        exportContainers.add(ec);
    }

    /**
     * Unregister a registered export container type.
     *
     * @param ec the container type to unregister
     */
    public static synchronized void unregisterExportContainer(ExportContainer ec) {
        exportContainers.remove(ec);
    }

    /**
     * Returns an array of the registered export containers.
     *
     * @return a (possibly empty) array of all container types that are
     * currently registered
     */
    public static synchronized ExportContainer[] getRegisteredExportContainers() {
        return exportContainers.toArray(ExportContainer[]::new);
    }

    private static final LinkedHashSet<ExportContainer> exportContainers;

    static {
        exportContainers = new LinkedHashSet<>();
        exportContainers.add(new FolderExportContainer());
        exportContainers.add(new ZIPExportContainer());
    }

    /**
     * Returns the full URL of a Strange Eons documentation page given a base
     * file name. If the name superficially looks like it is already a
     * {@code http[s]} URL then it is returned unchanged. A doc page can be the
     * main index page ({@code index}) or a page in one of the submanuals: the
     * user manual (prefix {@code um-}), the dev manual (prefix {@code dm-}), or
     * the translation manual (prefix {@code tm-}). If the base name is not
     * {@code index} and does not start with one of the specified manual
     * prefixes, it is assumed to be a shortened name for a user manual page and
     * that prefix is prepended. The name can include a hash fragment after the
     * base name to link to a specific section of the page.
     *
     * <p>
     * Note that these rules may change in the future if the documentation is
     * reorganized.
     *
     * @param baseDocFileName base name of a documentation page, such as
     * {@code dm-first-plugin}
     * @return a complete URL for the document
     */
    public static String getUrlForDocPage(String baseDocFileName) {
        baseDocFileName = Objects.requireNonNull(baseDocFileName, "baseDocFileName").trim();
        if (baseDocFileName.isEmpty()) {
            throw new IllegalArgumentException();
        }

        if (baseDocFileName.startsWith("http:") || baseDocFileName.startsWith("https:")) {
            return baseDocFileName;
        }

        String hash = "";
        final int hashIndex = baseDocFileName.indexOf('#');
        if (hashIndex >= 0) {
            hash = baseDocFileName.substring(hashIndex);
            baseDocFileName = baseDocFileName.substring(0, hashIndex);
        }

        final int dot = baseDocFileName.indexOf('.');
        if (dot >= 0) {
            baseDocFileName = baseDocFileName.substring(0, dot);
        }

        if (!baseDocFileName.equals("index") && !baseDocFileName.startsWith("um-")
                && !baseDocFileName.startsWith("dm-")
                && !baseDocFileName.startsWith("tm-")) {
            baseDocFileName = "um-" + baseDocFileName;
        }

        try {
            return "https://cgjennings.github.io/se3docs/"
                    + URLEncoder.encode(baseDocFileName, "utf-8") + ".html" + hash;
        } catch (UnsupportedEncodingException uee) {
            throw new AssertionError(uee); // UTF-8 always supported
        }
    }
}
