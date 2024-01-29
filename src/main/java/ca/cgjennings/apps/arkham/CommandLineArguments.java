package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.CommandLineParser;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import java.io.File;
import java.util.Locale;
import java.util.logging.Level;

/**
 * An instance of this class can be obtained from the application to access the
 * command line options that were passed to the application. The fields of this
 * class map one-to-one with the command line option names, and therefore may
 * change over time, although an effort is made to keep options compatible
 * whenever possible.
 *
 * <p>
 * Non-standard options start with an 'x'. These may be revoked at any time in
 * future versions.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @see StrangeEons#getCommandLineArguments()
 * @since 3.0
 */
public class CommandLineArguments implements Cloneable {

    /**
     * When present, this option causes the application to simply print the
     * build number and exit.
     */
    public boolean version = false;

    /**
     * This option specifies a locale to use for the game language.
     */
    public Locale glang = null;

    /**
     * This option specifies a locale to use for the user interface language.
     */
    public Locale ulang = null;

    /**
     * This option defines the minimum level of messages which will be logged
     * via {@link StrangeEons#log}.
     */
    public Level loglevel = Level.WARNING;

    /**
     * When present, all user preferences will be reset to their default values
     * as if the application had just been installed for the first time.
     */
    public boolean resetprefs = false;

    /**
     * When present, this option forces an attempt to migrate settings from an
     * older Strange Eons 1 or 2 installation, if one exists on this system.
     * Normally, preference migration is only attempted when the (SE3) user
     * preference file does not exist.
     */
    public boolean migrateprefs = false;

    /**
     * This option runs the application in plug-in test mode, testing the
     * specified bundle.
     */
    public String plugintest = null;

    /**
     * Location of the user resource folder.
     */
    public File resfolder = null;

    /**
     * Script file to run in script mode; see {@link ScriptRunnerModeHelper}.
     */
    public File run = null;

    /**
     * If set, display help for non-standard options and exit.
     */
    public boolean x = false;

    /**
     * Non-standard quality-tuning option.
     * <p>
     * This option forces the method used to antialias (AA) text. Java attempts
     * to read system-wide AA settings, but this often fails on Linux. So by
     * default Strange Eons uses the default settings on Windows and macOS but
     * will enforce LCD antialiasing otherwise. Setting this option forces a
     * different method:
     *
     * <dl>
     * <dt>auto</dt> <dd>use the value found in system settings</dd>
     * <dt>off</dt>  <dd>disable antialiasing</dd>
     * <dt>on</dt>   <dd>greyscale antialiasing for all fonts</dd>
     * <dt>gasp</dt> <dd>greyscale antialiasing based on the font's GASP
     * table</dd>
     * <dt>lcd</dt>  <dd>subpixel antialiasing for the most common LCD layout
     * (same as lcd_hrgb)</dd>
     * <dt>lcd_hbgr, lcd_vrgb, lcd_vbgr</dt>
     * <dd>subpixel antialiasing for other LCD layouts</dd>
     * </dl>
     */
    public String xAAText = null;

    /**
     * Non-standard development option.
     * <p>
     * This option prevents Strange Eons from checking the JRE version at
     * startup. Normally, Strange Eons checks which version of Java it is
     * running against and will refuse to start unless it is a known compatible
     * version.
     */
    public boolean xDisableJreCheck = false;

    /**
     * Non-standard development option.
     * <p>
     * This option causes an exception to be thrown during startup. It is used
     * to test how {@link ErrorDialog} handles uncaught startup exceptions.
     */
    public boolean xDebugException = false;

    /**
     * Non-standard debugging option.
     * <p>
     * This option prevents Strange Eons from attempting to re-open the project
     * that was open the last time the application exited. This may be useful if
     * this is preventing the application from starting normally. This option is
     * set implicitly if the {@link #plugintest} option is used.
     */
    public boolean xDisableProjectRestore = false;

    /**
     * Non-standard debugging option.
     * <p>
     * This option prevents Strange Eons from attempting to re-open the same
     * files that were in use the last time the application exited. This may be
     * useful if this is preventing the application from starting normally. This
     * option is set implicitly if the {@link #plugintest} option is used.
     */
    public boolean xDisableFileRestore = false;

    /**
     * Non-standard debugging and performance option.
     * <p>
     * This option disables animation effects that may not be played correctly
     * on some platforms.
     */
    public boolean xDisableAnimation = false;

    /**
     * Non-standard debugging option.
     * <p>
     * If this option is set, certain initialization steps that are normally
     * performed in a separate thread to decrease startup time will instead be
     * performed in the main startup thread. Try setting this option if you
     * encounter unexplained problems while starting the application.
     */
    public boolean xDisableBackgroundInit = false;

    /**
     * Non-standard debugging option.
     * <p>
     * Disables the use of threads to accelerate image filters in the package
     * {@code ca.cgjennings.graphics.filters} and use of multiple threads by
     * {@link ca.cgjennings.algo.SplitJoin}.
     */
    public boolean xDisableFilterThreads = false;

    /**
     * Non-standard debugging and development option.
     * <p>
     * Prevents plug-ins from being loaded. This can be used to facilitate
     * building an <em>application class-data sharing archive</em>.
     */
    public boolean xDisablePluginLoading = false;

    /**
     * Non-standard debugging option.
     * <p>
     * This option disables use of the system menu bar on OS X. It has no effect
     * on other platforms.
     */
    public boolean xDisableSystemMenu = false;

    /**
     * Non-standard debugging and performance option.
     * 
     * <p>
     * Setting this option will disable optional hardware acceleration
     * regardless of platform. This option supersedes all other
     * acceleration-related options.
     */
    public boolean xDisableAcceleration = false;
    
    /**
     * Non-standard debugging and performance option.
     * 
     * <p>
     * Since many video card drivers do not fully support hardware acceleration,
     * it is disabled by default on Windows.
     * Setting this option will enable it, which means enabling 
     * Direct3D acceleration unless the {@link #xOpenGL} option is also set.
     */
    public boolean xEnableWindowsAcceleration = false;
    
    /**
     * Non-standard debugging and performance option.
     * <p>
     * This option attempts to enable the OpenGL rendering pipeline for graphics
     * instead of using the default pipeline. It may result in improved
     * performance on some systems, although it may also result in rendering
     * issues if driver support is poor.
     * 
     * <p>
     * On Windows, the {@link xWinAccel#xEnableWindowsAcceleration} option must also be set. Adding this
     * switch will request OpenGL instead of Direct3D acceleration.
     * 
     * <p>
     * On Linux, the default is to try to use xrender-based acceleration.
     * Adding this switch will attempt to use OpenGL instead. Either option
     * can fail, in which case software rendering is used.
     */
    public boolean xOpenGL = false;

    /**
     * Internal use option.
     * <p>
     * This option is not meant to be set by the user. It is supplied
     * automatically when the application is attempting to restart itself. The
     * supplied file is a temporary file locked by the application instance that
     * initiated the restart; the restarting instance will repeatedly attempt to
     * acquire its own lock in order to detect when the initiating instance has
     * terminated.
     */
    public File xRestartLock = null;

    /**
     * Internal use options.
     * <p>
     * Allows some Windows launcher applications to enable "console mode"
     * without the option being rejected.
     */
    public boolean console;

    /**
     * Returns an array of the files included on the command line to be opened
     * when the application starts. Note that the returned array is a copy, so
     * callers may modify it freely.
     *
     * @return a possibly empty array of the files specified on the command line
     */
    public String[] getFiles() {
        return files.clone();
    }

    private String[] files;

    /**
     * This class cannot be instantiated. To obtain the command line arguments
     * for the application, call
     * {@code StrangeEons.getApplication().getCommandLineArguments()}.
     */
    private CommandLineArguments() {
    }

    static CommandLineArguments create(String[] args) {
        CommandLineArguments cla = new CommandLineArguments();
        CommandLineParser p = new CommandLineParser();
        p.setUsageText(
                "Strange Eons " + StrangeEons.getEditionNumber() + " build " + StrangeEons.getBuildNumber() + '\n'
                + "Use: [options...] [files...] where:\n"
                + "[files...] is a list of zero or more files to open and\n"
                + "[options...] may be one or more of:\n"
                + "  --version             Prints the build number and exits.\n"
                + "  --glang ll_RR         Sets preferred locale for game components. See (1).\n"
                + "  --ulang ll_RR         Sets preferred locale for the user interface.\n"
                + "  --logLevel level      Sets minimum severity of log messages. See (2).\n"
                + "  --resFolder           Names a folder with additional application resources.\n"
                + "  --pluginTest file(s)  Runs in test mode with this plug-in bundle; use " + File.pathSeparator + "\n"
                + "                        to list multiple bundles.\n"
                + "  --resetPrefs          Resets all preferences to their default values.\n"
                + "  --migratePrefs        Forces preference migration from an earlier version.\n"
                + "  --run file            Run the script file non-interactively.\n"
                + "  --x                   Displays help for non-standard options and exits.\n"
                + "\nNotes:\n"
                + "  (1) Locales are defined using a two-letter ISO-639 language code, optionally\n"
                + "      followed by an underscore and a two-letter ISO-3166 region code.\n"
                + "  (2) Level is one of: off, severe, warning, info, config, all. The default\n"
                + "      level is warning.\n"
                + "\nOption names are not case sensitive."
        );
        p.parse(cla, args);

        if (cla.x) {
            System.out.println(
                    "Non-standard options:\n"
                    + "  --xAAText                 Force method of antialiasing onscreen text:\n"
                    + "                              auto|off|on|gasp|lcd|lcd_hbgr|lcd_vrgb|lcd_vbgr\n"
                    + "  --xDisableProjectRestore  Do not re-open the project in use at the last exit\n"
                    + "  --xDisableFileRestore     Do not re-open files in use at the last exit\n"
                    + "  --xDisablePluginLoading   Do not load plug-ins (except test bundles)\n"
                    + "  --xDisableStartupThreads  Do not use threads to speed application startup\n"
                    + "  --xDisableFilterThreads   Do not use threads to accelerate image filters\n"
                    + "  --xDisableAnimation       Do not use animation effects\n"
                    + "  --xDisableAcceleration    Do not use hardware accelerated graphics\n"
                    + "  --xOpenGL                 If possible use OpenGL instead of default renderer\n"
                    + "  --xEnableWindowsAcceleration\n"
                    + "                            Enable acceleration on Windows\n"
                    + "  --xDebugException         Throws a test exception during startup\n"
            );
            System.exit(0);
        }

        cla.files = p.getPlainArguments();
        return cla;
    }

    @Override
    public CommandLineArguments clone() {
        try {
            return (CommandLineArguments) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
