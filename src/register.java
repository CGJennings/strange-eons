
import ca.cgjennings.apps.CommandLineParser;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.io.StreamPump;
import ca.cgjennings.platform.Shell;
import ca.cgjennings.platform.Shell.Result;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Integrates Strange Eons with a UNIX-like desktop based on freedesktop.org
 * standards.
 *
 * <p>
 * This tool requires that {@code xdg-utils} be installed on the target system.
 * Most modern distros will already have this installed.
 *
 * <p>
 * To use this from a command line, use a command like the following:<br>
 * <pre>java -cp strange-eons.jar register [arguments...]</pre>
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @version 0.1
 */
public final class register extends CommandLineParser {

    /**
     * MIME type for .eon files.
     */
    public static final String MIME_TYPE_EON = "application/x-strange-eons-component";
    /**
     * MIME type for .seproject files.
     */
    public static final String MIME_TYPE_SEPROJECT = "application/x-strange-eons-project";
    /**
     * MIME type for .seplugin files.
     */
    public static final String MIME_TYPE_SEPLUGIN = "application/x-strange-eons-plugin";
    /**
     * MIME type for .seext files.
     */
    public static final String MIME_TYPE_SEEXT = "application/x-strange-eons-extension";
    /**
     * MIME type for .setheme files.
     */
    public static final String MIME_TYPE_SETHEME = "application/x-strange-eons-theme";
    /**
     * MIME type for .selibrary files.
     */
    public static final String MIME_TYPE_SELIBRARY = "application/x-strange-eons-library";

    /**
     * Command line option that specifies that registration should be for all
     * users.
     */
    public boolean allusers;
    /**
     * Command line option that uninstalls a previous registration.
     */
    public boolean uninstall;
    /**
     * When registering for all users, read password from stdin instead of
     * console.
     */
    public boolean stdinpassword;
    /**
     * Command line option that specifies the path to a Java installation to
     * use.
     */
    public String java;
    /**
     * Command line option that specifies the path to the xdg-utils to use.
     */
    public String xdg;
    /**
     * Command line option that enables debug mode.
     */
    public boolean debug;
    /**
     * Arguments that will be passed to the JRE when launching the application.
     */
    public String jvmargs = JVM_ARGS;
    /**
     * Arguments that will be passed to the application when launched from
     * desktop.
     */
    public String args;
    /**
     * Arguments that will be passed to the JRE when launching the script
     * debugger.
     */
    public String debugjvm = DEBUG_JVM;
    /**
     * Arguments that will be passed to the script debugger when launched from
     * desktop.
     */
    public String debugargs;
    /**
     * Command line option that specifies the directory where Strange Eons is
     * installed, if different from where this command is being run from.
     */
    public File installdir;
    /**
     * Command line argument that causes the application to be installed onto
     * the desktop.
     */
    public boolean desktop;

    private static final String JVM_ARGS = "-Xmx2048m";
    private static final String DEBUG_JVM = "-Xmx128m";

    /**
     * Version number for minimum Java version we need to locate.
     */
    private static final String MIN_JAVA_VERSION = "8+"; // e.g. 8.0_56+

    @Override
    protected void displayUsageText(Object target) {
        System.out.print(
                "register [--allusers] [--java path] [--xdg path] [--jvmargs \"args\"]\n"
                + "         [--args \"args\"] [--debugjvm \"args\"] [--debugargs \"args\"]\n"
                + "         [--installdir path] [--desktop] [--debug]\n"
                + "Registers Strange Eons with the desktop on platforms that\n"
                + "support freedesktop.org standards. Requires xdg-utils.\n\n"
                + "Options:\n"
                + "  --uninstall   Uninstall a previous registration\n"
                + "  --allusers    Attempt to register for all users (superuser access required)\n"
                + "                (Add --stdinpassword to read password from stdin.)\n"
                + "  --xdg         Location of xdg-utils installation\n"
                + "  --java        Location of Java installation to use when launching application\n"
                + "  --jvmargs     Custom JVM arguments (e.g. -Xmx1024m) for application\n"
                + "  --args        Arguments passed to application (e.g. \"--loglevel info\")\n"
                + "  --debugjvm    Custom JVM arguments (e.g. -Xmx64m) for script debugger\n"
                + "  --debugargs   Arguments passed to script debugger (e.g. \"--port 2257\")\n"
                + "  --installdir  Strange Eons installation directory\n"
                + "  --desktop     Add application to desktop\n"
                + "  --debug       Prints a log of all executed commands\n\n"
                + "Defaults:\n"
                + "  --xdg         Assumed to be on PATH\n"
                + "  --java        Taken from JAVA_HOME or PATH\n"
                + "  --jvmargs     " + JVM_ARGS + '\n'
                + "  --debugjvm    " + DEBUG_JVM + '\n'
                + "  --installdir  Location of library used to run this command\n"
                + "                (" + installdir + ")\n"
        );
        System.exit(0);
    }

    /**
     * Command line tool that registers the application and document types on
     * freedesktop.org compatible systems.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        register instance = new register();
        try {

            // init default installdir
            URL url = register.class.getResource("register.class");
            if (url.getProtocol().equals("file")) {
                instance.installdir = new File(url.toURI()).getParentFile();
            } else {
                String urlPath = url.toString();
                if (urlPath.startsWith("jar:")) {
                    try {
                        // this will have a URL to the file starting after the
                        // jar: and going up to the ! (after which the path within the
                        // jar is concatenated
                        int excl = urlPath.indexOf('!');
                        if (excl != -1) {
                            urlPath = urlPath.substring(4, excl);
                            instance.installdir = new File(new URL(urlPath).toURI());
                        }
                    } catch (Exception e) {
                        instance.installdir = null;
                    }
                }
            }

            instance.parse(instance, args);
            instance.exec();
        } catch (Throwable t) {
            instance.fatal(null, t);
        }
    }

    private final Shell sh;
    private File javaBin;
    private String mode;
    private String rootpwd;

    private register() {
        sh = new Shell();
        File usf = new File(System.getProperty("user.home"), ".StrangeEons3");
        File home = new File(usf, "desktop");
        home.mkdirs();
        sh.directory(home);
    }

    /**
     * Returns a file with the given name in the desktop output directory.
     *
     * @param destName the file name
     * @return a file with the given name in {@code ~/.StrangeEons3/desktop}
     */
    private File file(String destName) {
        return new File(sh.directory(), destName);
    }

    private void extract(String resource, String destFile) throws IOException {
        item(resource);
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = resources.CacheMetrics.class.getResourceAsStream(resource);
            out = new FileOutputStream(file(destFile));
            StreamPump.copy(in, out);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    warn(null, e);
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    warn(null, e);
                }
            }
        }
    }

    private void extractIcon(String source, String destFile, int size) throws IOException {
        item(destFile + " icon " + size + " x " + size);
        destFile += size + ".png";
        BufferedImage bi = ImageIO.read(resources.CacheMetrics.class.getResource("icons/application/" + source + ".png"));
        if (bi == null) {
            throw new AssertionError("unable to read image " + source);
        }
        bi = ImageUtilities.ensureIntRGBFormat(bi);
        if (bi.getWidth() != bi.getHeight()) {
            int paddedSize = Math.max(bi.getWidth(), bi.getHeight());
            bi = ImageUtilities.center(bi, paddedSize, paddedSize);
        }
        if (bi.getWidth() != size) {
            float factor = ImageUtilities.idealCoveringScaleForImage(size, size, bi.getWidth(), bi.getHeight());
            bi = ImageUtilities.resample(bi, factor, true, RenderingHints.VALUE_INTERPOLATION_BICUBIC, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        }
        ImageIO.write(bi, "png", file(destFile));
    }

    private void extractIcons() throws IOException {
        extractIcon("128", "app", 96);
        extractIcon("64", "app", 48);
        extractIcon("32", "app", 24);
        extractIcon("16", "app", 16);

        extractIcon("db@8x", "debugger", 96);
        extractIcon("db@4x", "debugger", 48);
        extractIcon("db@2x", "debugger", 24);
        extractIcon("db", "debugger", 16);

        extractIcon("document", "eon", 96);
        extractIcon("document", "eon", 48);

        extractIcon("plugin", "seplugin", 96);
        extractIcon("plugin", "seplugin", 48);

        extractIcon("extension", "seext", 96);
        extractIcon("extension", "seext", 48);

        extractIcon("theme", "setheme", 96);
        extractIcon("theme", "setheme", 48);

        extractIcon("library", "selibrary", 96);
        extractIcon("library", "selibrary", 48);

        extractIcon("project", "seproject", 96);
        extractIcon("project", "seproject", 48);
    }

    private void registerIcon(String iconName, String mimeType) throws IOException {
        String outIconName = mimeType == null ? PREFIX + iconName : mimeType.replace('/', '-');
        item(outIconName);

        int[] sizes;
        if (mimeType == null) {
            sizes = ICON_SIZES_APP;
        } else {
            sizes = ICON_SIZES_MIME;
        }

        String context = mimeType == null ? "apps" : "mimetypes";

        for (int i = 0; i < sizes.length; ++i) {
            String size = String.valueOf(sizes[i]);
            xdg("xdg-icon-resource", "install", "--noupdate",
                    "--mode", mode,
                    "--context", context,
                    "--size", size,
                    file(iconName + size + ".png").getAbsolutePath(),
                    outIconName
            );
        }
    }

    private final int[] ICON_SIZES_APP = new int[]{96, 48, 24, 16};
    private final int[] ICON_SIZES_MIME = new int[]{96, 48};

    private void registerIcons() throws IOException {
        registerIcon("app", null);
        registerIcon("debugger", null);
        // doc icon's names must match MIME type
        registerIcon("eon", MIME_TYPE_EON);
        registerIcon("seproject", MIME_TYPE_SEPROJECT);
        registerIcon("seplugin", MIME_TYPE_SEPLUGIN);
        registerIcon("seext", MIME_TYPE_SEEXT);
        registerIcon("setheme", MIME_TYPE_SETHEME);
        registerIcon("selibrary", MIME_TYPE_SELIBRARY);
    }

    private File writeConfigFile(String fileName, String config, String extension) throws IOException {
        File configFile = file(PREFIX + fileName + '.' + extension);
        FileOutputStream out = new FileOutputStream(configFile);
        Writer w = null;
        try {
            w = new OutputStreamWriter(out, "utf-8");
            w.write(config);
        } finally {
            if (w == null) {
                out.close();
            } else {
                w.close();
            }
        }
        return configFile;
    }

    private void registerMIMEType(String mimeType, String comment, String... extensions) throws IOException {
        item(mimeType);
        StringBuilder b = new StringBuilder(256);
        b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        b.append("<mime-info xmlns='http://www.freedesktop.org/standards/shared-mime-info'>\n");
        b.append("\t<mime-type type=\"").append(mimeType).append("\">\n");
        b.append("\t\t<comment>").append(comment).append("</comment>\n");
        for (String extension : extensions) {
            b.append("\t\t<glob pattern=\"*.").append(extension).append("\"/>\n");
        }
        b.append("\t</mime-type>\n");
        b.append("</mime-info>");

        File xmlFile = writeConfigFile(extensions[0], b.toString(), "xml");
        xdg("xdg-mime", "install", "--mode", mode, xmlFile.getAbsolutePath());
    }

    private void registerMIMETypes() throws IOException {
        registerMIMEType(MIME_TYPE_EON, "Strange Eons game component", "eon");
        registerMIMEType(MIME_TYPE_SEPROJECT, "Strange Eons project package", "seproject");
        registerMIMEType(MIME_TYPE_SEPLUGIN, "Strange Eons plug-in bundle", "seplugin");
        registerMIMEType(MIME_TYPE_SEEXT, "Strange Eons extension plug-in bundle", "seext");
        registerMIMEType(MIME_TYPE_SETHEME, "Strange Eons theme bundle", "setheme");
        registerMIMEType(MIME_TYPE_SELIBRARY, "Strange Eons library bundle", "selibrary");
    }

    /**
     * Creates a registration file that can be used to register an application
     * or directory entry with the desktop.
     *
     * @param name the application or directory name
     * @param comment a comment for the entry, typically displayed as a tool tip
     * @param iconName the name of the icon to associate with the entry
     * @param exec executable command line ({@code null} for directories)
     * @param mimeTypes list of MIME type strings that the application can open
     * (empty for directories)
     *
     * @throws IOException if an error occurs while creating the entry
     */
    private File createDesktopEntry(String configFileName, String name, String comment, String iconName, String exec, String... mimeTypes) throws IOException {
        StringBuilder b = new StringBuilder(256);
        b.append("[Desktop Entry]\n");
        b.append("Encoding=UTF-8\n");
        b.append("Value=1.0\n");
        b.append("Type=").append(exec == null ? "Directory" : "Application").append('\n');
        b.append("Categories=Graphics;\n");
        b.append("Name=").append(name).append('\n');
        if (comment != null) {
            b.append("Comment=").append(comment).append('\n');
        }
        b.append("Icon=").append(iconName).append('\n');
        if (exec != null) {
            b.append("Exec=").append(exec).append('\n');
        }
        if (mimeTypes.length > 0) {
            b.append("MimeType=");
            for (String type : mimeTypes) {
                b.append(type).append(';');
            }
            b.append('\n');
        }

        return writeConfigFile(configFileName, b.toString(), exec == null ? "directory" : "desktop");
    }

    private void registerDesktopEntry(File dir, File entry) throws IOException {
        if (dir == null) {
            xdg("xdg-desktop-menu", "install", "--noupdate", "--mode", mode, entry.getAbsolutePath());
        } else {
            xdg("xdg-desktop-menu", "install", "--noupdate", "--mode", mode, dir.getAbsolutePath(), entry.getAbsolutePath());
        }
    }

    private void registerApp() throws IOException {
        File dir = createDesktopEntry("strange-eons-menu", "Strange Eons", null, PREFIX + "app", null);

        StringBuilder exec = new StringBuilder(128);
        exec.append(quote(javaBin.getAbsolutePath())).append(' ').append(jvmargs)
                .append(' ').append(quote("-javaagent:" + installdir.getAbsolutePath()))
                .append(" -cp ").append(quote(installdir.getAbsolutePath()))
                .append(" strangeeons ");
        if (args != null) {
            exec.append(args).append(' ');
        }
        exec.append("%F");
        File app = createDesktopEntry(
                "strange-eons", "Strange Eons", null, PREFIX + "app",
                exec.toString(), MIME_TYPE_EON, MIME_TYPE_SEPROJECT, MIME_TYPE_SEPLUGIN, MIME_TYPE_SEEXT, MIME_TYPE_SETHEME, MIME_TYPE_SELIBRARY
        );

        exec.delete(0, exec.length());
        exec.append(quote(javaBin.getAbsolutePath())).append(' ').append(debugjvm)
                .append(" -cp ").append(quote(installdir.getAbsolutePath()))
                .append(" debugger");
        if (debugargs != null) {
            exec.append(' ').append(debugargs);
        }
        File db = createDesktopEntry(
                "strange-eons-debugger", "Script Debugger", null, PREFIX + "debugger",
                exec.toString()
        );

        item("application");
        registerDesktopEntry(dir, app);
        item("script debugger");
        registerDesktopEntry(dir, db);

        if (desktop) {
            item("desktop icon");
            xdg("xdg-desktop-icon", "install", app.getAbsolutePath());
        }
    }

    private void refreshDesktop() throws IOException {
        item("icon database");
        xdg("xdg-icon-resource", "forceupdate", "--mode", mode);
        item("application database");
        xdg("xdg-desktop-menu", "forceupdate", "--mode", mode);
    }

    private void install() throws IOException {
        checkJava();

        section("Extracting Resources");
        extractIcons();

        section("Registering Icons");
        registerIcons();

        section("Registering MIME Types");
        registerMIMETypes();

        section("Registering Applications");
        registerApp();
    }

    private void unregisterIcon(String iconName, String mimeType) throws IOException {
        String outIconName = mimeType == null ? PREFIX + iconName : mimeType.replace('/', '-');
        item(outIconName);

        int[] sizes;
        if (mimeType == null) {
            sizes = ICON_SIZES_APP;
        } else {
            sizes = ICON_SIZES_MIME;
        }

        String context = mimeType == null ? "apps" : "mimetypes";

        for (int i = 0; i < sizes.length; ++i) {
            String size = String.valueOf(sizes[i]);
            xdg("xdg-icon-resource", "uninstall", "--noupdate",
                    "--mode", mode,
                    "--context", context,
                    "--size", size,
                    outIconName
            );
        }
    }

    private void unregisterIcons() throws IOException {
        unregisterIcon("app", null);
        unregisterIcon("debugger", null);
        unregisterIcon("eon", MIME_TYPE_EON);
        unregisterIcon("seproject", MIME_TYPE_SEPROJECT);
        unregisterIcon("seplugin", MIME_TYPE_SEPLUGIN);
        unregisterIcon("seext", MIME_TYPE_SEEXT);
        unregisterIcon("setheme", MIME_TYPE_SETHEME);
        unregisterIcon("selibrary", MIME_TYPE_SELIBRARY);
    }

    private void unregisterMIMEType(String type, String primaryExtension) throws IOException {
        item(type);
        xdg("xdg-mime", "uninstall", "--mode", mode, PREFIX + primaryExtension + ".xml");
    }

    private void unregisterMIMETypes() throws IOException {
        unregisterMIMEType(MIME_TYPE_EON, "eon");
        unregisterMIMEType(MIME_TYPE_SEPROJECT, "seproject");
        unregisterMIMEType(MIME_TYPE_SEPLUGIN, "seplugin");
        unregisterMIMEType(MIME_TYPE_SEEXT, "seext");
        unregisterMIMEType(MIME_TYPE_SETHEME, "setheme");
        unregisterMIMEType(MIME_TYPE_SELIBRARY, "selibrary");
    }

    private void unregisterApp() throws IOException {
        item("desktop icon");
        xdg("xdg-desktop-icon", "uninstall", PREFIX + "strange-eons.desktop");
        item("menu items");
        xdg("xdg-desktop-menu", "uninstall", "--noupdate", "--mode", mode, PREFIX + "strange-eons-menu.directory", PREFIX + "strange-eons-debugger.desktop", PREFIX + "strange-eons.desktop");
    }

    private void uninstall() throws IOException {
        section("Unregistering Applications");
        unregisterApp();

        section("Unregistering MIME Types");
        unregisterMIMETypes();

        section("Unregistering Icons");
        unregisterIcons();
    }

    private void exec() throws Throwable {
        mode = allusers ? "system" : "user";

        if (allusers) {
            Console con = System.console();
            if (con != null && !stdinpassword) {
                con.format("Enter password to change settings for all users:\n");
                rootpwd = new String(con.readPassword());
            } else {
                System.out.print("Enter password to change settings for all users:\n");
                System.out.flush();
                rootpwd = new BufferedReader(new InputStreamReader(System.in)).readLine();
            }
        }

        section("Configuration");
        checkXDG();

        if (uninstall) {
            uninstall();
        } else {
            install();
        }

        section("Refreshing Desktop");
        refreshDesktop();

        section("Done");
        item("You may need to log out and log back in for changes to take effect");

        if (debug) {
            System.out.println(log.toString());
        }

        System.out.flush();
    }

    /**
     * Execute an xdg-utils command, prepending command name with the specified
     * xdg path, if any.
     */
    private int xdg(List<String> tokens) throws IOException {
        return xdg(tokens.toArray(new String[0]));
    }

    /**
     * Execute an xdg-utils command, prepending command name with the specified
     * xdg path, if any.
     */
    private int xdg(String... tokens) throws IOException {
        if (xdg != null) {
            tokens[0] = xdg + File.separatorChar + tokens[0];
        }
        try {
            if (debug) {
                System.out.print('<');
                for (int i = 0; i < tokens.length; ++i) {
                    if (i > 0) {
                        System.out.print(' ');
                    }
                    System.out.print(tokens[i]);
                }
                System.out.println('>');
            }
            int code;
            if (allusers) {
                code = log(sh.sudo(tokens, null, rootpwd)).exitCode();
            } else {
                code = log(sh.exec(tokens)).exitCode();
            }
            if (code != 0) {
                xdgError(code);
            }
            return code;
        } catch (IOException e) {
            if (!debug) {
                throw e;
            }
            return 0;
        }
    }

    /**
     * Display the name of the next integration task that is about to begin.
     */
    private void section(String name) {
        System.out.println("[" + name + ']');
    }

    /**
     * Display a description of the next step in the current task.
     */
    private void item(String text) {
        System.out.println("  " + text);
        System.out.flush();
    }

    /**
     * Print line of text normally, with no indent.
     */
    private void plain(String text) {
        System.out.println(text);
        System.out.flush();
    }

    /**
     * Print a label followed by a : and no newline, to be followed by calling
     * {@link #passItem} with the result.
     *
     * @param text the check being performed
     */
    private void checkItem(String text) {
        System.out.print("  " + text + ": ");
    }

    /**
     * Prints the result of a check being performed.
     *
     * @param ok {@code true} if the check passed
     */
    private void passItem(boolean ok) {
        System.out.println(ok ? "OK" : "Failed");
        System.out.flush();
    }

    /**
     * Prints a warning message. Either the message or the throwable may be
     * {@code null}, but not both.
     *
     * @param message the message to print
     * @param t an exception to describe
     */
    private void warn(String message, Throwable t) {
        System.err.flush();
        System.out.flush();

        System.err.print("Warning: ");
        if (message != null) {
            System.err.println(message);
        }
        if (t != null) {
            t.printStackTrace(System.err);
        }
        System.err.flush();
    }

    /**
     * Prints an error message, and then quits unless running in debug mode.
     * Either the message or the throwable may be {@code null}, but not both.
     *
     * @param message the message to print
     * @param t an exception to describe
     */
    private void fatal(String message, Throwable t) {
        System.err.flush();
        System.out.flush();

        int code = 1;
        if (message != null) {
            System.err.println("Error: " + message);
            code = 2;
        } else if (t instanceof AssertionError) {
            System.err.println("Error: " + t.getMessage());
            code = 2;
        } else {
            System.err.println("Error: an unexpected error prevented registration");
            t.printStackTrace(System.err);
        }

        System.err.flush();

        if (!debug) {
            System.exit(code);
        }
    }

    /**
     * Creates a warning or fatal error for an xdg-utils exit code (if
     * non-zero).
     *
     * @param code the exit code
     */
    private void xdgError(int code) {
        switch (code) {
            case 1:
                fatal("bad xdg command syntax", null);
                break;
            case 2:
                fatal("specified file does not exist", null);
                break;
            case 3:
                fatal("xdg-utils requires a tool that could not be found", null);
                break;
            case 4:
                warn("xdg action failed", null);
                break;
            case 5:
                warn("no permission to read file", null);
                break;
            default:
        }
    }

    /**
     * Checks if xdg-utils can be found, and otherwise creates a fatal error.
     */
    private void checkXDG() {
        checkItem("Checking for xdg-utils");
        try {
            if (xdg("xdg-desktop-menu", "--version") != 0) {
                throw new AssertionError();
            }
            passItem(true);
        } catch (Throwable e) {
            passItem(false);
            fatal("install xdg-utils and try again", e);
        }
    }

    /**
     * Looks for a suitable Java installation in standard locations, printing a
     * warning message if none is not found.
     */
    private void checkJava() {
        if (java != null) {
            javaBin = new File(java, "bin" + File.separator + "java");
            item("Using explicit Java install location: " + java);
            return;
        }

        checkItem("Checking for Java " + MIN_JAVA_VERSION);
        if (!checkJavaImpl(System.getenv("JAVA_HOME"))) {
            if (!checkJavaImpl(System.getProperty("java.home"))) {
                passItem(false);
                item("Using \"java\" as JVM command; specify --java to override");
                return;
            }
        }
        plain("OK, found " + javaBin.getParentFile().getParentFile().getAbsolutePath());
    }

    /**
     * Check if the specified location is a Java installation directory. A Java
     * installation has a subdirectory "bin" containing the "java" executable.
     *
     * @param loc the candidate location
     * @return {@code true} if the directory is a Java installation directory
     * for the minimum required version
     * @see #MIN_JAVA_VERSION
     */
    private boolean checkJavaImpl(String loc) {
        if (loc == null) {
            return false;
        }
        javaBin = new File(loc + File.separatorChar + "bin" + File.separatorChar + "java");
        try {
            Result r = log(sh.exec(javaBin.getAbsolutePath(), "-version:1." + MIN_JAVA_VERSION, "-version"));
            return r.exitCode() == 0;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Quote a UNIX-style command line argument, if required.
     *
     * @param arg the original argument
     * @return the original argument, or a quoted equivalent
     */
    @SuppressWarnings("fallthrough")
    private String quote(String arg) {
        boolean quote = false;
        for (int i = 0; i < arg.length(); ++i) {
            char ch = arg.charAt(i);
            switch (ch) {
                case '\\':
                case '[':
                case '|':
                case '<':
                case '>':
                case '`':
                case '$':
                case ' ':
                case ':':
                    quote = true;
                    break;
            }
        }
        if (quote) {
            return "'" + arg.replace("'", "\\'") + "'";
        } else {
            return arg;
        }
    }

    private static final String PREFIX = "cgjennings-";

    /**
     * If run in debug mode, builds a log of all executed commands.
     *
     * @param r the result to log
     * @return the same result as was passed in
     */
    private Result log(Result r) {
        if (debug) {
            if (log == null) {
                log = new StringBuilder(8_192);
                log.append("\n-= DEBUG LOG =-\n");
            }
            log.append("COMMAND:   ").append(r.command()).append('\n')
                    .append("EXIT CODE: ").append(r.exitCode()).append('\n')
                    .append("OUTPUT:\n");
            for (String s : r.output().split("\n")) {
                log.append(s).append('\n');
            }
            log.append('\n');
        }
        return r;
    }
    private StringBuilder log;
}
