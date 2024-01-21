package ca.cgjennings.apps.arkham;

import org.mozilla.javascript.Context;
import ca.cgjennings.ui.theme.Theme;
import java.awt.Cursor;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import static resources.Language.string;

/**
 * Dialog subclass used to implement the {@link BusyDialog}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
final class BusyDialogImpl extends javax.swing.JDialog {
    private final Runnable operation;
    private ActionListener cancelAction;
    private final AtomicInteger maximum = new AtomicInteger(0);
    private final AtomicInteger current = new AtomicInteger(0);
    private boolean visible;

    private static final LinkedList<BusyDialogImpl> stack = new LinkedList<>();
    // note: access only while synch'd on stack
    private static final HashMap<Thread, BusyDialogImpl> threadMap = new HashMap<>();

    private long lastStatusTime;
    private long lastProgressTime;
    private volatile boolean cancelled;

    private final BusyDialog owner;

    /**
     * Creates a new busy dialog with the specified title message that will
     * perform the specified operation in a separate thread. The dialog's parent
     * frame will default to the main application window if {@code null}. The
     * dialog will include a Cancel button that can be clicked by the user. When
     * pressed, the specified listener's {@code actionPerformed} method will be
     * called from the event dispatch thread.
     *
     * @param owner the {@link BusyDialog} instance that delegates to this
     * dialog
     * @param parent the parent frame
     * @param title the initial title message
     * @param operation the operation to perform
     * @param cancelAction the action to perform when the user presses the
     * Cancel button
     */
    public BusyDialogImpl(BusyDialog owner, JFrame parent, String title, Runnable operation, ActionListener cancelAction) {
        super(parent = (parent == null ? StrangeEons.getWindow() : parent), true);

        // prevent look and feels from adding any decorations
        JRootPane root = getRootPane();
        root.setWindowDecorationStyle(JRootPane.NONE);
        root.setBorder(UIManager.getBorder(Theme.MESSAGE_BORDER_DIALOG));
        initComponents();
        content.setBackground(UIManager.getColor(Theme.MESSAGE_BACKGROUND));

        this.owner = owner;
        owner.setImplementation(this);

        setLocationRelativeTo(parent);
        busyLabel.setText(title);
        this.operation = operation;

        if (cancelAction == null) {
            cancelBtn.setVisible(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            this.cancelAction = cancelAction;
        }

        progressBar.setSize(progressBar.getPreferredSize());
        pack();

        lastStatusTime = 0L;
        lastProgressTime = 0L;

        synchronized (stack) {
            stack.push(this);
        }
        Thread t = new Thread() {
            @Override
            public void run() {
                BusyDialogImpl.this.run();
            }
        };
        t.start();
        setVisible(true);
    }

    /**
     * Returns the {@link BusyDialog} for which this is the true dialog.
     *
     * @return the busy dialog that delegates to this implementation
     */
    public BusyDialog getPublicOwner() {
        return owner;
    }

    /**
     * Sets the title of the <i>current</i> busy dialog. (The initial title is
     * set in the constructor.) If no busy dialog is open, this method has no
     * effect.
     *
     * <p>
     * This method can be called from any thread.
     *
     * @param text the title text displayed in large print at the top of the
     * dialog
     * @see #setTitleText
     * @see #getCurrentDialog()
     */
    public static void titleText(String text) {
        BusyDialogImpl bd = BusyDialogImpl.getCurrentDialog();
        if (bd != null) {
            bd.setTitleText(text);
        }
    }

    /**
     * Sets the status text of the <i>current</i> busy dialog. If no busy dialog
     * is open, this method has no effect.
     *
     * <p>
     * This method can be called from any thread.
     *
     * @param text the status text displayed in small print at the bottom of the
     * dialog
     * @see #setStatusText
     * @see #getCurrentDialog()
     */
    public static void statusText(String text) {
        BusyDialogImpl bd = BusyDialogImpl.getCurrentDialog();
        if (bd != null) {
            bd.setStatusText(text);
        }
    }

    /**
     * Sets the status text of the <i>current</i> busy dialog. This version of
     * the method will not actually update the text more often than once per
     * {@code delay} milliseconds. This prevents status text updates from
     * dominating the time spent doing real work If no busy dialog is open, this
     * method has no effect.
     *
     * <p>
     * This method can be called from any thread.
     *
     * @param text the status text displayed in small print at the bottom of the
     * dialog
     * @param delay the minimum time between status text changes, in
     * milliseconds
     * @see #setStatusText
     * @see #getCurrentDialog()
     */
    public static void statusText(String text, int delay) {
        BusyDialogImpl bd = BusyDialogImpl.getCurrentDialog();
        if (bd != null) {
            long newTime = System.currentTimeMillis();
            if ((newTime - bd.lastStatusTime) >= delay) {
                bd.setStatusText(text);
                bd.lastStatusTime = newTime;
            }
        }
    }

    /**
     * Sets the current progress value of the <i>current</i> busy dialog. If no
     * busy dialog is open, this method has no effect.
     *
     * <p>
     * This method can be called from any thread.
     *
     * @param currentValue the number of units of work that have been completed
     * @see #setProgressCurrent
     * @see #getCurrentDialog()
     */
    public static void currentProgress(int currentValue) {
        BusyDialogImpl bd = BusyDialogImpl.getCurrentDialog();
        if (bd != null) {
            bd.setProgressCurrent(currentValue);
        }
    }

    /**
     * Sets the current progress value of the <i>current</i> busy dialog. This
     * version of the method will not actually update the progress more often
     * than once per {@code delay} milliseconds. This prevents progress updates
     * from dominating the time spent doing real work If no busy dialog is open,
     * this method has no effect.
     *
     * <p>
     * This method can be called from any thread.
     *
     * @param currentValue the number of units of work that have been completed
     * @param delay the minimum time between status text changes, in
     * milliseconds
     * @see #setProgressCurrent
     * @see #getCurrentDialog()
     */
    public static void currentProgress(int currentValue, int delay) {
        BusyDialogImpl bd = BusyDialogImpl.getCurrentDialog();
        if (bd != null) {
            long newTime = System.currentTimeMillis();
            if ((newTime - bd.lastProgressTime) >= delay) {
                bd.setProgressCurrent(currentValue);
                bd.lastProgressTime = newTime;
            }
        }
    }

    /**
     * Sets the maximum progress value of the <i>current</i> busy dialog. If no
     * busy dialog is open, this method has no effect.
     *
     * <p>
     * This method can be called from any thread.
     *
     * @param maximumValue the total number of units of work that must be
     * performed, or -1 if unknown
     * @see #setProgressMaximum
     * @see #getCurrentDialog()
     */
    public static void maximumProgress(int maximumValue) {
        BusyDialogImpl bd = BusyDialogImpl.getCurrentDialog();
        if (bd != null) {
            bd.setProgressMaximum(maximumValue);
        }
    }

    /**
     * Returns the busy dialog most appropriate for the caller. If this is
     * called from the Runnable that was passed to a BusyDialog constructor,
     * then it will return the dialog that is displaying progress for that
     * Runnable. Otherwise, it returns the most recently created BusyDialog that
     * is still open. If no BusyDialog is open, it will return {@code null}. The
     * static methods
     * {@link #titleText}, {@link #statusText}, {@link #currentProgress}, and
     * {@link #maximumProgress} all modify the state of the current dialog.
     *
     * <p>
     * This method can be called from any thread.
     *
     * @return the busy dialog that this thread should modify, or {@code null}
     */
    public static BusyDialogImpl getCurrentDialog() {
        BusyDialogImpl bd = null;
        synchronized (stack) {
            bd = threadMap.get(Thread.currentThread());
            if (bd == null && !stack.isEmpty()) {
                bd = stack.peek();
            }
        }
        return bd;
    }

//	protected void localizeForPlatform() {
//		progressBar.putClientProperty( "JProgressBar.style", "circular" );
//	}
    /**
     * Sets the title of this busy dialog. (The initial title is set in the
     * constructor.)
     *
     * <p>
     * This method can be called from any thread.
     *
     * @param text the title text displayed in large print at the top of the
     * dialog
     */
    public void setTitleText(final String text) {
        SwingUtilities.invokeLater(() -> {
            if (cancelBtn.isEnabled()) {
                busyLabel.setText(text);
            }
        });
    }

    /**
     * Sets the status text of this busy dialog.
     *
     * <p>
     * This method can be called from any thread.
     *
     * @param text the status text displayed in small print at the bottom of the
     * dialog
     */
    public void setStatusText(final String text) {
        SwingUtilities.invokeLater(() -> {
            spacingLabel.setText(text);
        });
    }

    /**
     * Sets the current progress value of this busy dialog.
     *
     * <p>
     * This method can be called from any thread.
     *
     * @param currentValue the number of units of work that have been completed
     */
    public void setProgressCurrent(int currentValue) {
        if (current.getAndSet(currentValue) == currentValue) {
            return;
        }
        current.set(currentValue);
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(current.get());
        });
    }

    /**
     * Returns the current progress value of this busy dialog.
     *
     * @return the number of work units that have been completed
     * @see #setProgressCurrent
     * @see #setProgressMaximum
     */
    public int getProgressCurrent() {
        return current.get();
    }

    /**
     * Sets the maximum progress value of this busy dialog. This is a value that
     * represents the total number of units of work to be performed. Setting the
     * maximum progress value to -1 will cause the progress to enter
     * <i>indeterminate mode</i>. This mode should be used when the total amount
     * of work is unknown or the length of time required to complete a work unit
     * is so variable that the progress bar is not a meaningful indicator of how
     * long the task will take to complete.
     *
     * <p>
     * When this method is called and the {@code maximumValue} is not negative,
     * the current progress value is reset to 0.
     *
     * <p>
     * This method can be called from any thread.
     *
     * @param maximumValue the total number of units of work that must be
     * performed
     */
    public void setProgressMaximum(int maximumValue) {
        if (maximumValue < -1) {
            maximumValue = -1;
        }
        maximum.set(maximumValue);
        SwingUtilities.invokeLater(() -> {
            int max = maximum.get();
            if (max < 0) {
                progressBar.setIndeterminate(true);
            } else {
                progressBar.setValue(0);
                progressBar.setMaximum(max);
                progressBar.setIndeterminate(false);
//					progressBar.setSize( progressBar.getPreferredSize() );
            }
//				if( PlatformSupport.PLATFORM_IS_OSX ) {
//					progressBar.putClientProperty( "JProgressBar.style", null );
//					Dimension size = progressBar.getPreferredSize();
//					if( size.width < 146 ) {
//						size.width = 146;
//					}
//					progressBar.setPreferredSize( size );
//					validate();
//				}
        });
    }

    /**
     * Returns the current maximum progress value, or -1 if in indeterminate
     * mode.
     *
     * @return the total number of work units to complete, or -1
     */
    public int getProgressMaximum() {
        return maximum.get();
    }

    /**
     * Returns {@code true} if this dialog has a cancel button and it has been
     * pressed by the user.
     *
     * @return {@code true} if the user has indicated that they wish to cancel
     * the operation
     */
    public boolean isCancelled() {
        return cancelled;
    }

    private void run() {
        visible = false;

        // Create a script context in case the operation is script code
        Context.enter();
        StrangeEons.setWaitCursor(true);
        try {
            synchronized (stack) {
                threadMap.put(Thread.currentThread(), this);
            }
            // Wait for the busy dialog to open before running the task
            while (!visible) {
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        visible = isVisible();
                    });
                } catch (InterruptedException e) {
                } catch (InvocationTargetException e) {
                    throw new AssertionError(e.getCause());
                }
                if (!visible) {
                    try {
                        Thread.sleep(75);
                    } catch (InterruptedException e) {
                    }
                }
            }

            try {
                operation.run();
            } catch (CancellationException e) {
                // some tasks throw this to indicate that
                // the user cancelled the operation: do nothing
            } catch (Exception e) {
                StrangeEons.log.log(Level.SEVERE, "BusyDialog operation threw uncaught exception", e);
            }
        } finally {
            synchronized (stack) {
                stack.remove(this);
                threadMap.remove(Thread.currentThread());
            }            
            Context.exit();
            SwingUtilities.invokeLater(() -> {
                StrangeEons.setWaitCursor(false);
                dispose();
                owner.setImplementation(null);
            });
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        content = new javax.swing.JPanel();
        progressBar = new javax.swing.JProgressBar();
        busyLabel = new javax.swing.JLabel();
        cancelBtn = new javax.swing.JButton();
        spacingLabel = new javax.swing.JLabel();

        FormListener formListener = new FormListener();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setUndecorated(true);

        content.setBackground(java.awt.Color.white);
        content.setLayout(new java.awt.GridBagLayout());

        progressBar.setIndeterminate(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(8, 96, 12, 96);
        content.add(progressBar, gridBagConstraints);

        busyLabel.setFont(busyLabel.getFont().deriveFont(busyLabel.getFont().getStyle() | java.awt.Font.BOLD, busyLabel.getFont().getSize()+3));
        busyLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        busyLabel.setText("Working...");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipady = 14;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        content.add(busyLabel, gridBagConstraints);

        cancelBtn.setText(string("cancel")); // NOI18N
        cancelBtn.addActionListener(formListener);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        content.add(cancelBtn, gridBagConstraints);

        spacingLabel.setFont(spacingLabel.getFont().deriveFont(spacingLabel.getFont().getSize()-2f));
        spacingLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        spacingLabel.setText("    ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipady = 7;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 4, 4);
        content.add(spacingLabel, gridBagConstraints);

        getContentPane().add(content, java.awt.BorderLayout.CENTER);
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == cancelBtn) {
                BusyDialogImpl.this.cancelBtnActionPerformed(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

private void cancelBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelBtnActionPerformed
    cancelled = true;
    busyLabel.setText(string("cancelled"));
    cancelBtn.setEnabled(false);
    cancelAction.actionPerformed(evt);
}//GEN-LAST:event_cancelBtnActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel busyLabel;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JPanel content;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel spacingLabel;
    // End of variables declaration//GEN-END:variables

}
