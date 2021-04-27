package ca.cgjennings.platform;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.ui.JUtilities;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * This utility class implements essentially the same functionality as the
 * {@link java.awt.Desktop} API, with the following differences:
 * <ol>
 * <li> Its methods are static; there is no need to call a
 * {@link java.awt.Desktop#getDesktop()}-type method to use it.
 * <li> When a method is not supported, it simply does nothing instead of
 * throwing an exception. There are public constants available to determine if
 * an operation is supported beforehand.
 * <li> The edit and print actions will fall back to the open action if they are
 * not supported but the open action is supported.
 * <li> You can optionally supply a UI component, in which case a wait cursor
 * will be set for a few seconds to provide feedback that the operation is in
 * progress.
 * <li> Additional desktop actions are available.
 * </ol>
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class DesktopIntegration {

    private DesktopIntegration() {
    }

    /**
     * Displays a Web page in the system browser.
     *
     * @param uri the URI to display
     * @throws IOException if an I/O error occurs while starting the browser
     */
    public static void browse(URI uri) throws IOException {
        browse(uri, null);
    }

    /**
     * Displays a Web page in the system browser.
     *
     * @param uri the URI to display
     * @param feedbackComponent an optional component used to display feedback
     * @throws IOException if an I/O error occurs while starting the browser
     */
    public static void browse(URI uri, Component feedbackComponent) throws IOException {
        if (BROWSE_SUPPORTED) {
            Desktop.getDesktop().browse(uri);
            showFeedback(feedbackComponent);
        }
    }

    /**
     * This is <code>true</code> if the {@link #browse} method is supported.
     */
    public static final boolean BROWSE_SUPPORTED = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);

    /**
     * Opens the default mail application to send an email based on the
     * specified <code>mailto</code> URI. If the URI is <code>null</code>, opens
     * the mail application to compose an email, but does not specify any
     * details of the email.
     *
     * @param uri the <code>mailto</code> URI for the message
     * @throws IOException if an I/O error occurs while starting the application
     */
    public static void mail(URI uri) throws IOException {
        mail(uri, null);
    }

    /**
     * Opens the default mail application to send an email based on the
     * specified <code>mailto</code> URI. If the URI is <code>null</code>, opens
     * the mail application to compose an email, but does not specify any
     * details of the email.
     *
     * @param uri
     * @param feedbackComponent an optional component used to display feedback
     * @throws IOException if an I/O error occurs while starting the application
     */
    public static void mail(URI uri, Component feedbackComponent) throws IOException {
        if (MAIL_SUPPORTED) {
            if (uri == null) {
                Desktop.getDesktop().mail();
            } else {
                Desktop.getDesktop().mail(uri);
            }
            showFeedback(feedbackComponent);
        }
    }

    /**
     * This is <code>true</code> if the {@link #mail} method is supported.
     */
    public static final boolean MAIL_SUPPORTED = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL);

    /**
     * Opens the specified file using the default application on this system.
     *
     * @param file the file to open
     * @throws IOException if an I/O error occurs while starting the application
     */
    public static void open(File file) throws IOException {
        open(file, null);
    }

    /**
     * Opens the specified file using the default application on this system.
     *
     * @param file the file to open
     * @param feedbackComponent an optional component used to display feedback
     * @throws IOException if an I/O error occurs while starting the application
     */
    public static void open(File file, Component feedbackComponent) throws IOException {
        if (OPEN_SUPPORTED) {
            Desktop.getDesktop().open(file);
            showFeedback(feedbackComponent);
        }
    }

    /**
     * This is <code>true</code> if the {@link #open} method is supported.
     */
    public static final boolean OPEN_SUPPORTED = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN);

    /**
     * Opens the specified file for editing using the default application on
     * this system. On some platforms it may be possible to register different
     * applications for editing versus opening a file, in which case opening the
     * file would typically open some kind of preview application.
     *
     * <p>
     * If the underlying system does not support this feature, it will be
     * {@link #open}ed instead.
     *
     * @param file the file to open for editing
     * @throws IOException if an I/O error occurs while starting the application
     */
    public static void edit(File file) throws IOException {
        edit(file, null);
    }

    /**
     * Opens the specified file for editing using the default application on
     * this system. On some platforms it may be possible to register different
     * applications for editing versus opening a file, in which case opening the
     * file would typically open some kind of preview application.
     *
     * <p>
     * If the underlying system does not support this feature, it will be
     * {@link #open}ed instead.
     *
     * @param file the file to open for editing
     * @param feedbackComponent an optional component used to display feedback
     * @throws IOException if an I/O error occurs while starting the application
     */
    public static void edit(File file, Component feedbackComponent) throws IOException {
        if (EDIT_SUPPORTED_NATIVELY) {
            try {
                Desktop.getDesktop().edit(file);
                showFeedback(feedbackComponent);
                return;
            } catch (IOException e) {
            }
        }
        open(file, feedbackComponent);
    }

    private static final boolean EDIT_SUPPORTED_NATIVELY = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.EDIT);

    /**
     * This is <code>true</code> if the {@link #open} method is supported.
     */
    public static final boolean EDIT_SUPPORTED = EDIT_SUPPORTED_NATIVELY | OPEN_SUPPORTED;

    /**
     * Prints the specified file using the default application on this system.
     *
     * <p>
     * If the underlying system does not support this feature, it will be
     * {@link #open}ed instead.
     *
     * @param file the file to print
     * @throws IOException if an I/O error occurs while starting the application
     */
    public static void print(File file) throws IOException {
        print(file, null);
    }

    /**
     * Prints the specified file using the default application on this system.
     *
     * <p>
     * If the underlying system does not support this feature, it will be
     * {@link #open}ed instead.
     *
     * @param file the file to print
     * @param feedbackComponent an optional component used to display feedback
     * @throws IOException if an I/O error occurs while starting the application
     */
    public static void print(File file, Component feedbackComponent) throws IOException {
        if (PRINT_SUPPORTED_NATIVELY) {
            try {
                Desktop.getDesktop().print(file);
                showFeedback(feedbackComponent);
                return;
            } catch (IOException e) {
            }
        }
        open(file, feedbackComponent);
    }

    private static final boolean PRINT_SUPPORTED_NATIVELY = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.EDIT);

    /**
     * This is <code>true</code> if the {@link #open} method is supported.
     */
    public static final boolean PRINT_SUPPORTED = PRINT_SUPPORTED_NATIVELY | OPEN_SUPPORTED;

    /**
     * Shows a specified file's icon in the graphical system shell. If the file
     * is a directory, that directory is displayed using the graphical shell
     * (e.g., Explorer, Finder). If it is a file, then the parent directory is
     * displayed using the shell. If supported by the host platform, the file
     * will also be selected if possible. If there is an error such that the
     * folder cannot be shown, this method returns <code>false</code>.
     *
     * @param f the file to display and select
     * @return <code>false</code> if the folder could not be displayed
     */
    public static boolean showInShell(File f) {
        return showInShell(f, null);
    }

    /**
     * Shows a specified file's icon in the graphical system shell. If the file
     * is a directory, that directory is displayed using the graphical shell
     * (e.g., Explorer, Finder). If it is a file, then the parent directory is
     * displayed using the shell. If supported by the host platform, the file
     * will also be selected if possible. If there is an error such that the
     * folder cannot be shown, this method returns <code>false</code>.
     *
     * @param f the file to display and select
     * @param feedbackComponent an optional component used to display feedback
     * @return <code>false</code> if the folder could not be displayed
     */
    public static boolean showInShell(File f, Component feedbackComponent) {
        if (f == null) {
            throw new NullPointerException("file");
        }
        if (!f.exists()) {
            return false;
        }

        // Ways have been identified to display files in the shell for both
        // Windows and OS X 10.6+. At the time of this writing, there is no
        // equivalent for the "nautilus" launcher.
        if (!f.isDirectory()) {
            try {
                if (PlatformSupport.PLATFORM_IS_WINDOWS) {
                    Runtime.getRuntime().exec(new String[]{
                        "explorer", "/select," + f.getAbsolutePath()
                    });
                    showFeedback(feedbackComponent);
                    return true;
                } else if (PlatformSupport.PLATFORM_IS_OSX) {
                    Runtime.getRuntime().exec(new String[]{
                        "open", "-R", f.getAbsolutePath()
                    });
                    showFeedback(feedbackComponent);
                    return true;
                }
            } catch (IOException e) {
                // will try again via Desktop
            }

            f = f.getParentFile();
            if (f == null) {
                StrangeEons.log.warning("What an unusual file system you have, Grandma");
                return false;
            }
        }

        // Fallback: it is a directory or we don't know how to select it
        // or there was an error trying to select it.
        try {
            open(f, feedbackComponent);
            return true;
        } catch (IOException e) {
        }

        // there was an error or Desktop not available, return false
        return false;
    }

    /**
     * This is <code>true</code> if the {@link #showInShell} method is
     * supported.
     */
    public static final boolean SHOW_IN_SHELL_SUPPORTED = OPEN_SUPPORTED;

    private static void showFeedback(Component c) {
        if (c == null) {
            c = StrangeEons.getWindow();
        } else {
            c = SwingUtilities.getWindowAncestor(c);
        }
        if (c == null) {
            return;
        }

        final Component target = c;
        Timer t = new Timer(4_000, (ActionEvent e) -> {
            try {
                if (target != StrangeEons.getWindow()) {
                    JUtilities.hideWaitCursor(target);
                }
            } finally {
                StrangeEons.setWaitCursor(false);
            }
        });
        t.setRepeats(false);
        t.start();

        try {
            if (target != StrangeEons.getWindow()) {
                JUtilities.showWaitCursor(target);
            }
        } finally {
            StrangeEons.setWaitCursor(true);
        }
    }
}
