package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.dialog.Messenger;
import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.JLinkLabel;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.LinkedList;
import java.util.logging.Level;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import resources.Language;
import resources.ResourceKit;

/**
 * Support class that handles restarting the application and posting restart
 * messages.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
final class Restarter {

    private Restarter() {
    }

    /**
     * Display a message that offers to restart the application; if the message
     * is {@code null}, a default message is displayed.
     *
     * @param message the restart message
     */
    static void offer(final String message) {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(() -> {
                offer(message);
            });
        }

        String messageText = message == null ? Language.string("app-restart") : message;
        JComponent restartLink = new JLinkLabel(Language.string("app-b-restart"));
        restartLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    StrangeEons.getWindow().exitApplication(true);
                }
            }
        });
        Messenger.displayMessage(null, ResourceKit.getIcon("application/restart.png"), messageText, new JComponent[]{restartLink});
    }

    /**
     * Called by {@link AppFrame} to start the second copy of SE when doing a
     * restart.
     *
     * @throws IOException if the restart fails
     */
    static void launchRestartProcess() throws IOException {
        File restartFile = File.createTempFile("se-restart", ".lock");
        new FileOutputStream(restartFile);

        StrangeEons.log.info("attempting to relaunch application");

        // list of command arguments; this is built dynamically
        LinkedList<String> command = new LinkedList<>();

        //
        // We need to build an array of strings that represent a command
        // that will relaunch Strange Eons. This must start with a basic
        // command for launching the app, plus two special restart arguments
        // that point the new process to our lock file.
        //
        // We first check if there is a platform-specific method we can use.
        // If that fails, we use a generic method to try to identify the
        // java command and arguments that were used to start this instance.
        //
        // After the platform-specific or generic arguments are filled into
        // command, the command array will still have two unused elements at
        // the end and i will point to the first of these. Then the special
        // restart arguments will be appended.
        //
        if (PlatformSupport.PLATFORM_IS_WINDOWS) {
            try {
                final File installLocation = BundleInstaller.getApplicationLibrary().getParentFile();
                final File StrangeEonsEXE = new File(installLocation, "StrangeEons.exe");
                if (StrangeEonsEXE.exists()) {
                    command.add(StrangeEonsEXE.getAbsolutePath());
                }
            } catch (Throwable t) {
                StrangeEons.log.log(Level.WARNING, "failed to build Windows restart command", t);
            }
        } // OS X: find .app; check for Snow Leopard, which is when the --args option was added
        else if (PlatformSupport.PLATFORM_IS_MAC) {
            try {
                // try to find an .app bundle that the library is embedded in
                final File installLocation = BundleInstaller.getApplicationLibrary().getParentFile();
                File appBundle = installLocation;
                while (appBundle != null && !appBundle.getName().endsWith(".app")) {
                    appBundle = appBundle.getParentFile();
                }
                // for some reason we didn't find it; look in default install location
                if (appBundle == null) {
                    appBundle = new File("/Applications/Strange Eons.app");
                    if (!appBundle.exists()) {
                        appBundle = null;
                    }
                }
                // if we found the .app bundle, try to find the launcher within
                if (appBundle != null) {
                    File launcher = new File(appBundle, "Contents/MacOS/JavaApplicationStub");
                    if (launcher.exists()) {
                        command.add(launcher.getAbsolutePath());
                    }
                }
            } catch (Throwable t) {
                StrangeEons.log.log(Level.WARNING, "failed to build OS X restart command", t);
            }
        }

        // If for any reason a platform-specific version could not be generated,
        // we fall back on a cross-platform launch command.
        if (command.isEmpty()) {
            command.add(Subprocess.getJavaRuntimeExecutable());

            // append jvm args that were passed to this jvm instance (e.g. -Xmx)
            RuntimeMXBean rmx = ManagementFactory.getRuntimeMXBean();
            command.addAll(rmx.getInputArguments());

            // add classpath and main class name
            command.add("-cp");
            command.add(Subprocess.getClasspath());
            command.add("strangeeons");
        }

        // At this point we have our best guess for a command line that will
        // launch Strange Eons; now we add the arguments for SE itself,
        // specifically the lock file that will indicate when *this* SE
        // process has exited.
        command.add("--XRestartLock");
        command.add(restartFile.getAbsolutePath());

        // log the final command at INFO level
        if (StrangeEons.log.isLoggable(Level.INFO)) {
            StringBuilder b = new StringBuilder("relaunch command:");
            for (String token : command) {
                b.append(' ').append(token);
            }
            StrangeEons.log.info(b.toString());
        }

        Runtime.getRuntime().exec(command.toArray(new String[command.size()]));

        // the actual System.exit() will happen in the caller (AppFrame)
    }
}
