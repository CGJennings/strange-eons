package ca.cgjennings.apps.arkham.dialog;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.text.LineWrapper;
import ca.cgjennings.ui.JUtilities;
import ca.cgjennings.ui.theme.ThemeInstaller;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Window;
import java.util.LinkedList;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import resources.ResourceKit;

/**
 * The <code>Messenger</code> manages the display of information and error
 * messages for top-level windows. A queue of messages is maintained for each
 * window. If a message is already displayed and a new message is posted, it
 * will be added to a queue and shown after the current message and all queued
 * messages have been displayed.
 *
 * <p>
 * Messages that do not explicitly return a result will return immediately,
 * without waiting for the message to be displayed or dismissed.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class Messenger {

    private Messenger() {
    }

    /**
     * Displays a warning message to the user. An warning message is used to
     * warn that the user that an action they are about to take may have
     * unexpected or irreversible consequences. (Compare this to
     * {@link #displayErrorMessage(java.awt.Component, java.lang.String)}.)
     *
     * @param parent the component or window that this message relates to; if
     * <code>null</code>, then the main application window will be the parent
     * @param message the text of the message
     * @throws NullPointerException if <code>message</code> is <code>null</code>
     */
    public static void displayWarningMessage(Component parent, String message) {
        Icon icon = null;
        if (ThemeInstaller.getInstalledTheme() != null) {
            icon = UIManager.getIcon("OptionPane.warningIcon");
        }
        if (icon == null) {
            icon = ResourceKit.getIcon("application/warning.png");
        }
        displayMessage(parent, icon, message);
    }

    /**
     * Displays an error message to the user. An error message is used to report
     * that a problem has occurred. (Compare this to
     * {@link #displayWarningMessage(java.awt.Component, java.lang.String)}.)
     *
     * @param parent the component or window that this message relates to; if
     * <code>null</code>, then the main application window will be the parent
     * @param message the text of the message
     * @throws NullPointerException if <code>message</code> is <code>null</code>
     */
    public static void displayErrorMessage(Component parent, String message) {
        Icon icon = null;
        if (ThemeInstaller.getInstalledTheme() != null) {
            icon = UIManager.getIcon("OptionPane.errorIcon");
        }
        if (icon == null) {
            icon = ResourceKit.getIcon("application/error.png");
        }
        displayMessage(parent, icon, message);
    }

    /**
     * Displays a message to the user.
     *
     * @param parent the component or window that this message relates to; if
     * <code>null</code>, then the main application window will be the parent
     * @param iconResource an optional image resource to display beside the
     * message; this will be loaded using
     * {@link ResourceKit#getIcon(java.lang.String)}
     * @param message the text of the message
     * @throws NullPointerException if <code>message</code> is <code>null</code>
     */
    public static void displayMessage(Component parent, String iconResource, String message) {
        if (message == null) {
            throw new NullPointerException("message");
        }
        Icon icon = iconResource == null ? null : ResourceKit.getIcon(iconResource);
        displayMessage(parent, icon, message, null);
    }

    /**
     * Displays a message to the user.
     *
     * @param parent the component or window that this message relates to; if
     * <code>null</code>, then the main application window will be the parent
     * @param message the text of the message
     * @throws NullPointerException if <code>message</code> is <code>null</code>
     */
    public static void displayMessage(Component parent, String message) {
        if (message == null) {
            throw new NullPointerException("message");
        }
        displayMessage(parent, null, message, null);
    }

    /**
     * Displays a message to the user.
     *
     * @param parent the component or window that this message relates to; if
     * <code>null</code>, then the main application window will be the parent
     * @param icon an optional icon to display beside the message
     * @param message the text of the message
     * @throws NullPointerException if <code>message</code> is <code>null</code>
     */
    public static void displayMessage(Component parent, Icon icon, String message) {
        if (message == null) {
            throw new NullPointerException("message");
        }
        displayMessage(parent, icon, message, null);
    }

    /**
     * Displays a message to the user.
     *
     * @param parent the component or window that this message relates to; if
     * <code>null</code>, then the main application window will be the parent
     * @param icon an optional icon to display beside the message
     * @param message the text of the message
     * @param inlineComponents an array of interface components that will be
     * included as part of the message by stacking them, one per row, under the
     * main message text
     * @throws NullPointerException if <code>message</code> is <code>null</code>
     */
    public static void displayMessage(Component parent, Icon icon, String message, JComponent[] inlineComponents) {
        if (message == null) {
            throw new NullPointerException("message");
        }
        String[] array = splitMessage(message);
        Message m = new Message(parent, icon, array, inlineComponents);
        show(m);
    }

    /**
     * A message that can be displayed to the user using the
     * {@link MessageQueue}.
     */
    static final class Message {

        Component parent;
        Icon icon;
        String[] message;
        JComponent[] inlineComponents;

        public Message(Component parent, Icon icon, String[] message, JComponent[] inlineComponents) {
            if (message == null) {
                throw new NullPointerException("message");
            }
            this.parent = parent;
            this.icon = icon;
            this.message = message;
            this.inlineComponents = inlineComponents;
        }

        public boolean isDialogStyle() {
            return false;
        }
    }

    private static String[] splitMessage(String message) {
        if (message == null) {
            throw new NullPointerException("message");
        }
        if (message.isEmpty()) {
            return new String[0];
        }
        boolean isHTML = message.length() > 6 && message.regionMatches(true, 0, "<html>", 0, 6);
        if (!isHTML) {
            message = ResourceKit.makeStringHTMLSafe(message);
        }

        LinkedList<String> lines = new LinkedList<>();
        LineWrapper wr = new LineWrapper("<br>", 68, 68, 4);
        for (String s : message.split("\n|(<br>)")) {
            lines.add("<html>" + wr.wrap(s));
        }
        return lines.toArray(new String[lines.size()]);
    }

    private static void show(final Message m) {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(() -> {
                show(m);
            });
            return;
        }

        if (!processQueue) {
            if (!m.isDialogStyle() && m.parent == StrangeEons.getSafeStartupParentWindow() || m.parent == StrangeEons.getWindow() || m.parent == null) {
                if (startupQueue == null) {
                    startupQueue = new LinkedList<>();
                }
                m.parent = null;
                startupQueue.addLast(m);
            }
            return;
        }

        if (m.parent == null) {
            m.parent = StrangeEons.getWindow();
        }
        Window c = SwingUtilities.getWindowAncestor(m.parent);
        if (c instanceof Frame) {
            new MessageDialog((Frame) c, m);
        } else if (c instanceof Dialog) {
            new MessageDialog((Dialog) c, m);
        } else if (c instanceof Window) {
            new MessageDialog(c, m);
        } else {
            new MessageDialog(StrangeEons.getWindow(), m);
        }
    }

    /**
     * Enables or disables message queue processing. While queue processing is
     * disabled, messages will continue to be queued but will not be displayed.
     * When queue processing is reenabled, the display of messages in the queue
     * will resume.
     *
     * @param enable whether queued messages should be displayed
     * (<code>true</code>) or held for future display (<code>false</code>)
     * @see #isQueueProcessingEnabled()
     */
    public static void setQueueProcessingEnabled(boolean enable) {
        JUtilities.threadAssert();
        if (enable != processQueue) {
            if (enable && StrangeEons.getWindow() == null) {
                throw new IllegalStateException("can't enable until app window exists");
            }
            processQueue = enable;
            if (enable) {
                MessageDialog.restartQueues();
                if (startupQueue != null) {
                    for (Message m : startupQueue) {
                        show(m);
                    }
                    startupQueue = null;
                }
            }
        }
    }

    /**
     * Returns <code>true</code> if the messages in message queues are actively
     * being displayed.
     *
     * @return <code>true</code> if queue processing is enabled
     */
    public static boolean isQueueProcessingEnabled() {
        return processQueue;
    }

    private static boolean processQueue;

    // used to queue up messages for the main app window during startup
    private static LinkedList<Message> startupQueue;
}
