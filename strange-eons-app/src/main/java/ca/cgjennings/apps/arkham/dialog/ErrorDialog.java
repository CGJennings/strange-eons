package ca.cgjennings.apps.arkham.dialog;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsAppWindow;
import ca.cgjennings.apps.arkham.plugins.catalog.AutomaticUpdater;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.JUtilities;
import ca.cgjennings.ui.theme.ThemeInstaller;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.util.HashSet;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import org.mozilla.javascript.RhinoException;
import resources.ResourceKit;

/**
 * Displays error messages to the user. Error dialogs are not instantiated
 * directly, but are instead created and shown by calling one of the following
 * static methods:<br>
 * {@link #displayError(java.lang.String, java.lang.Throwable)}<br>
 * {@link #displayErrorOnce(java.lang.String, java.lang.String, java.lang.Throwable)}<br>
 * {@link #displayFatalError(java.lang.String, java.lang.Throwable)}<br>
 * {@link #nyi()}<br> {@link #outOfMemory()}<br>
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public final class ErrorDialog extends javax.swing.JDialog {

    private ErrorDialog(Window parent) {
        super(parent, ModalityType.TOOLKIT_MODAL);
        initComponents();
        getRootPane().setDefaultButton(closeBtn);
        try {
            setIconImages(StrangeEonsAppWindow.getApplicationFrameIcons());
        } catch (Throwable t) {
        }

        // prepare to do the update check
        updateLink.setVisible(false);
        updateCheckIcon.setIcon(ResourceKit.createWaitIcon(updateCheckIcon));
        updateCheckIcon.setText(null);

        EventQueue.invokeLater(new Thread("Update check") {
            @Override
            public void run() {
                boolean update = false;
                try {
                    updateLink.setURI(new URI("https://strangeeons.cgjennings.ca/"));
                    update = AutomaticUpdater.isApplicationUpdateAvailable();
                } catch (Throwable t) {
                    StrangeEons.log.log(Level.SEVERE, null, t);
                }
                final boolean fiUpdate = update;
                EventQueue.invokeLater(() -> {
                    updateLink.setVisible(fiUpdate);
                    updateCheckIcon.setVisible(false);
                });
            }
        }::start);
    }

    /**
     * Displays the "not yet implemented" message. This is displayed by
     * development versions as a warning that a feature is not yet complete.
     */
    public static void nyi() {
        if (!EventQueue.isDispatchThread()) {
            StrangeEons.log.warning(string("nyi"));
        } else {
            JOptionPane.showMessageDialog(
                    StrangeEons.getSafeStartupParentWindow(),
                    string("nyi"), "Strange Eons", JOptionPane.WARNING_MESSAGE
            );
        }
    }

    /**
     * Displays an error message. If a non-{@code null} exception is passed, the
     * error message will include additional details about the error taken from
     * the exception, including the localized message (if any), and the class
     * name of the exception.
     *
     * @param message the message to display to the user describing the nature
     * of the error
     * @param t an optional exception related to the error
     */
    public static void displayError(String message, Throwable t) {
        if (t == null) {
            StringBuilder b = new StringBuilder(message).append('\n');
            appendTrace(b, new Error(), 1);
            StrangeEons.log.log(Level.WARNING, b.toString());
            b = null;
        } else {
            StrangeEons.log.log(Level.WARNING, message, t);
            if (t instanceof RhinoException) {
                StrangeEons.log.warning("script trace:\n" + ((RhinoException) t).getScriptStackTrace());
            }
        }

        StringBuilder buffer = new StringBuilder(128);
        buffer.append("<html><b>").append(message).append("</b>");
        if (t != null) {
            buffer.append('\n').append(messageForThrowable(t));
        }
        Messenger.displayErrorMessage(null, buffer.toString());
    }

    private static String messageForThrowable(Throwable t) {
        String throwableMessage = t.getLocalizedMessage();
        if (throwableMessage != null) {
            throwableMessage = throwableMessage.trim();
            if (!throwableMessage.startsWith("<html>")) {
                throwableMessage = ResourceKit.makeStringHTMLSafe(throwableMessage);
            }
            return throwableMessage + '\n' + niceClassName(t);
        }
        return niceClassName(t);
    }

    private static String niceClassName(Throwable t) {
        String name = t.getClass().getSimpleName();
        StringBuilder b = new StringBuilder(name.length() + 8);
        boolean wasLC = false;
        for (int i = 0; i < name.length(); ++i) {
            char ch = name.charAt(i);
            boolean isLC = Character.isLowerCase(ch);
            if (!isLC && wasLC) {
                b.append(' ');
            }
            wasLC = isLC;
            b.append(ch);
        }
        return b.toString();
    }

    /**
     * Displays an error message for a class of error identified by a key, but
     * only if no error message has been previously displayed for that key. For
     * example, if a resource file is missing, you might call this method to
     * report the error using the resource identifier as the key in order to
     * prevent the error dialog from popping up over and over each time the
     * resource is requested.
     *
     * @param exclusionKey the key that uniquely identifies the class of error
     * messages
     * @param errorMessage the message to display
     * @param exception an optional exception that occurred as part of the error
     */
    public synchronized static void displayErrorOnce(String exclusionKey, String errorMessage, Throwable exception) {
        if (reportedMissing == null) {
            reportedMissing = new HashSet<>();
        } else if (reportedMissing.contains(exclusionKey)) {
            return;
        }
        reportedMissing.add(exclusionKey);
        displayError(errorMessage, exception);
    }
    private static HashSet<String> reportedMissing;

    /**
     * Displays a dialog to describe a serious error that prevents the
     * application from continuing. This method is not for general use; it is
     * normally only called during startup when a critical error occurs that
     * Strange Eons cannot recover from. The displayed error dialog will include
     * a stack trace for the provided {@code cause}, and include an option to
     * fill out a bug report. If the {@code cause} is null, then a stack trace
     * will be generated for the point at which the method was called.
     *
     * @param message the message to display
     * @param cause an optional exception related to the error; this will be
     * displayed along with the message to help diagnose the problem
     */
    public static void displayFatalError(final String message, final Throwable cause) {
        StrangeEons.log.log(Level.SEVERE, message, cause);

        // create a stack trace
        try {
            StringBuilder b = new StringBuilder();
            if (cause == null) {
                b.append("Call stack trace:");
                appendTrace(b, new Error(), 1);
            } else {
                b.append(cause.toString()).append('\n');
                appendTrace(b, cause, 0);
            }
            final String stackTrace = b.toString();
            final Runnable showDialog = () -> {
                try {
                    ThemeInstaller.ensureBaselineLookAndFeelInstalled();
                } catch (Throwable t) {
                    StrangeEons.log.log(Level.SEVERE, null, t);
                }
                try {
                    display(message, stackTrace, true, cause);
                } catch (Throwable t) {
                    StrangeEons.log.log(Level.SEVERE, null, t);
                }
            };
            if (EventQueue.isDispatchThread()) {
                showDialog.run();
            } else {
                EventQueue.invokeAndWait(showDialog);
            }
        } catch (Throwable t) {
            StrangeEons.log.log(Level.SEVERE, null, t);
        }
    }

    /**
     * Called when an out of memory condition is detected to display a standard
     * error message.
     */
    public static void outOfMemory() {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastOutOfMemory) < OUT_OF_MEMORY_INTERVAL) {
            return;
        }
        lastOutOfMemory = currentTime;

        try {
            String message;
            long maxmem = Runtime.getRuntime().maxMemory();

            if (maxmem == Long.MAX_VALUE) {
                message = string("app-err-outofmem") + string("app-err-outofmem-nomax");
            } else {
                message = string("app-err-outofmem") + String.format(string("app-err-outofmem-hasmax"), maxmem / (1024L * 1024L), "(translation out of date)");
            }
            ErrorDialog.displayError(message, null);
        } catch (OutOfMemoryError e) {
            StrangeEons.log.log(Level.SEVERE, "Out of Memory: unable to create out of memory dialog");
        }
    }

    // used to limit how often the OOM error can be displayed; the app can get
    // stuck trying to paint something that throws OOM on each paint attempt,
    // causing 6 gajillion of these dialogs to appear
    private static final long OUT_OF_MEMORY_INTERVAL = 7_500L;
    private static long lastOutOfMemory = System.currentTimeMillis() - OUT_OF_MEMORY_INTERVAL;

    private static void appendTrace(StringBuilder b, Throwable t, int start) {
        StackTraceElement[] el = t.getStackTrace();
        // skip el[0], the call to fatalError
        for (int i = start; i < el.length; ++i) {
            b.append('\n').append(el[i]);
        }
    }

    private static void display(final String message, final String stackTrace, final boolean isFatal, final Throwable t) {
        final Window parent = StrangeEons.getSafeStartupParentWindow();

        ErrorDialog ed = new ErrorDialog(parent);

        if (parent == null) {
            try {
                ed.setIconImage(ImageIO.read(resources.ResourceKit.class.getResource("/resources/icons/application/app@2x.png")));
            } catch (Throwable ex) {
                StrangeEons.log.log(Level.WARNING, "unable to load fatal dialog icon", ex);
            }
        }

        // set title, close btn text
        if (isFatal) {
            ed.heading.setText(string("rk-err-fatal-title"));
            ed.closeBtn.setText(string(PlatformSupport.PLATFORM_IS_MAC ? "exit-osx" : "exit"));
        } else {
            ed.heading.setText(string("rk-err-nonfatal-title"));
            ed.closeBtn.setText(string("resume"));
        }
        ed.setTitle(ed.heading.getText());

        // fill in stack trace info
        if (stackTrace == null || stackTrace.isEmpty()) {
            ed.traceLabel.setVisible(false);
            ed.trace.setVisible(false);
            ed.copyTraceBtn.setVisible(false);
        } else {
            ed.trace.setText(stackTrace);
            ed.trace.select(0, 0);
        }

        // create a plain version of the message for logging
        // and sending bug reports
        ed.plainErrorMessage = plainMessage(message);
        
        // fill in message label with wrapped message text
        String msg = "<html><b>" + message + "</b>";
        if (isFatal) {
            msg += "<br>" + string("rk-err-fatal");
        } else {
            if (t != null) {
                if (t.getLocalizedMessage() != null) {
                    msg += "<br>" + t.getLocalizedMessage();
                }
                msg += "<br>" + t.getClass().getSimpleName();
            }
        }
        ed.message.setText(msg);

        // include error in log
        if (t != null) {
            StrangeEons.log.log(Level.SEVERE, ed.plainErrorMessage, t);
        } else {
            StrangeEons.log.severe(ed.plainErrorMessage);
        }

        ed.pack();

        if (parent != null) {
            parent.setVisible(true);
            ed.setLocationRelativeTo(parent);
        } else {
            ed.setLocationByPlatform(true);
        }

        ed.setVisible(true);
        if (isFatal) {
            System.exit(20);
        } else {
            ed.dispose();
        }
    }

    /**
     * Given a message that may contain basic HTML markup and escapes,
     * return a plain text version. Break tags are converted to spaces,
     * other tags erased, and a few common HTML escapes are converted.
     * The result is only meant for logging and bug reporting, it is
     * not secure or complete.
     * 
     * @param message the message to convert
     * @return a plain text version of the message
     */
    private static String plainMessage(String message) {
        if (message == null) {
            return "";
        }
        message = message.replaceAll("<br\\s*/?>", " ")
                .replaceAll("<[^>]+>", "")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&nbsp;", " ");
                return message;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        message = new javax.swing.JLabel();
        heading = new ca.cgjennings.ui.JHeading();
        reportBtn = new javax.swing.JButton();
        closeBtn = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        trace = new javax.swing.JTextArea();
        traceLabel = new javax.swing.JLabel();
        copyTraceBtn = new javax.swing.JButton();
        updateLink = new ca.cgjennings.ui.JLinkLabel();
        updateCheckIcon = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);

        message.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        message.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray), javax.swing.BorderFactory.createEmptyBorder(0, 0, 6, 0)));
        message.setIconTextGap(16);

        heading.setText(string("rk-err-fatal-title")); // NOI18N

        reportBtn.setIcon(ResourceKit.getIcon("bug-report"));
        reportBtn.setText(string("app-report-bug")); // NOI18N
        reportBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reportBtnActionPerformed(evt);
            }
        });

        closeBtn.setText(string("close")); // NOI18N
        closeBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeBtnActionPerformed(evt);
            }
        });

        trace.setColumns(20);
        trace.setEditable(false);
        trace.setFont(new java.awt.Font("Monospaced", 0, 11)); // NOI18N
        trace.setLineWrap(true);
        trace.setTabSize(4);
        trace.setWrapStyleWord(true);
        jScrollPane1.setViewportView(trace);

        traceLabel.setFont(traceLabel.getFont().deriveFont(traceLabel.getFont().getSize()-1f));
        traceLabel.setText(string("rk-err-l-trace")); // NOI18N

        copyTraceBtn.setFont(copyTraceBtn.getFont().deriveFont(copyTraceBtn.getFont().getSize()-2f));
        copyTraceBtn.setText(string("copy")); // NOI18N
        copyTraceBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 6, 3, 6));
        copyTraceBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyTraceBtnActionPerformed(evt);
            }
        });

        updateLink.setText(string("rk-err-update")); // NOI18N
        updateLink.setFont(updateLink.getFont().deriveFont(updateLink.getFont().getStyle() | java.awt.Font.BOLD));

        updateCheckIcon.setText(" ");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(traceLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 201, Short.MAX_VALUE)
                                .addComponent(copyTraceBtn))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addComponent(reportBtn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(updateCheckIcon)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(updateLink, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(closeBtn))))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(heading, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE)
                            .addComponent(message, javax.swing.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(heading, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(message, javax.swing.GroupLayout.DEFAULT_SIZE, 75, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(copyTraceBtn)
                    .addComponent(traceLabel))
                .addGap(1, 1, 1)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 113, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(reportBtn)
                    .addComponent(closeBtn)
                    .addComponent(updateLink, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(updateCheckIcon))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

	private void copyTraceBtnActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_copyTraceBtnActionPerformed
            StringSelection s = new StringSelection(trace.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(s, s);
	}//GEN-LAST:event_copyTraceBtnActionPerformed

	private void reportBtnActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_reportBtnActionPerformed
            reportBtn.setEnabled(false);
            JUtilities.showWaitCursor(this);
            StrangeEons.getApplication().fileBugReport(
                    "Message: " + plainErrorMessage + "\n" + trace.getText(), null
            );
            JUtilities.hideWaitCursor(this);
	}//GEN-LAST:event_reportBtnActionPerformed
    private String plainErrorMessage;

	private void closeBtnActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_closeBtnActionPerformed
            dispose();
	}//GEN-LAST:event_closeBtnActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton closeBtn;
    private javax.swing.JButton copyTraceBtn;
    private ca.cgjennings.ui.JHeading heading;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel message;
    private javax.swing.JButton reportBtn;
    private javax.swing.JTextArea trace;
    private javax.swing.JLabel traceLabel;
    private javax.swing.JLabel updateCheckIcon;
    private ca.cgjennings.ui.JLinkLabel updateLink;
    // End of variables declaration//GEN-END:variables

    private static String string(String key) {
        if (!failedToLoad) {
            try {
                return resources.Language.string(key);
            } catch (Throwable t) {
                failedToLoad = true;
            }
        }
        try {
            return ResourceBundle.getBundle("resources/text/interface/eons-text").getString(key);
        } catch (MissingResourceException mre) {
            return "MISSING: " + key;
        } catch (Throwable t) {
            StrangeEons.log.log(Level.SEVERE, null, t);
            return "<?? String table failure>";
        }
    }

    private static boolean failedToLoad;
}
