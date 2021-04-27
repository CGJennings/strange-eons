package ca.cgjennings.apps.arkham;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;

/**
 * Displays a pop-up dialog to indicate that the application is busy while
 * performing operations that take a long time to complete. The dialog can
 * display a heading, a smaller status message, the current progress as a
 * progress bar, and may optionally include a Cancel button. The progress bar
 * will initially indicate that the length of the operation is indeterminate;
 * setting a non-negative maximum progress value will change the progress bar to
 * determinate mode.
 *
 * <p>
 * Note that the dialog will be displayed (and the operation to be performed may
 * start running) immediately, before the constructor returns. There is no need
 * to explicitly "start" the operation.
 *
 * <p>
 * To use a BusyDialog from script code, use the
 * <code>Thread.busyWindow( task, title, canCancel )</code> function in the
 * <code>threads</code> library.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public final class BusyDialog extends javax.swing.JDialog {

    private BusyDialogImpl impl;

    /**
     * Updates this dialog instance with the implementation instance. This must
     * be set from the implementation itself.
     *
     * @param peer the implementation owned by this dialog instance
     */
    void setImplementation(BusyDialogImpl peer) {
        impl = peer;
    }

    /**
     * Creates a new busy dialog with the specified title message that will
     * perform the specified operation in a separate thread. The dialog's parent
     * window will be the main application window.
     *
     * @param title the initial title message
     * @param operation the operation to perform
     */
    public BusyDialog(String title, Runnable operation) {
        this(null, title, operation);
    }

    /**
     * Creates a new busy dialog with the specified title message that will
     * perform the specified operation in a separate thread. The dialog's parent
     * frame will default to the main application window if <code>null</code>.
     *
     * @param parent the parent frame
     * @param title the initial title message
     * @param operation the operation to perform
     */
    public BusyDialog(JFrame parent, String title, Runnable operation) {
        this(parent == null ? StrangeEons.getWindow() : parent, title, operation, null);
    }

    /**
     * Creates a new busy dialog with the specified title message that will
     * perform the specified operation in a separate thread. The dialog's parent
     * window will be the main application window. The dialog will include a
     * Cancel button that can be clicked by the user. When pressed, the
     * specified listener's <code>actionPerformed</code> method will be called
     * from the event dispatch thread.
     *
     * @param title the initial title message
     * @param operation the operation to perform
     * @param cancelAction the action to perform when the user presses the
     * Cancel button
     */
    public BusyDialog(String title, Runnable operation, ActionListener cancelAction) {
        this(null, title, operation, cancelAction);
    }

    /**
     * Creates a new busy dialog with the specified title message that will
     * perform the specified operation in a separate thread. The dialog's parent
     * frame will default to the main application window if <code>null</code>.
     * The dialog will include a Cancel button that can be clicked by the user.
     * When pressed, the specified listener's <code>actionPerformed</code>
     * method will be called from the event dispatch thread.
     *
     * @param parent the parent frame
     * @param title the initial title message
     * @param operation the operation to perform
     * @param cancelAction the action to perform when the user presses the
     * Cancel button
     */
    public BusyDialog(JFrame parent, String title, Runnable operation, ActionListener cancelAction) {
        // BusyDialogImpl will set our impl value
        new BusyDialogImpl(this, parent, title, operation, cancelAction);
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
        BusyDialogImpl.titleText(text);
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
        BusyDialogImpl.statusText(text);
    }

    /**
     * Sets the status text of the <i>current</i> busy dialog. This version of
     * the method will not actually update the text more often than once per
     * <code>delay</code> milliseconds. This prevents status text updates from
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
        BusyDialogImpl.statusText(text, delay);
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
        BusyDialogImpl.currentProgress(currentValue);
    }

    /**
     * Sets the current progress value of the <i>current</i> busy dialog. This
     * version of the method will not actually update the progress more often
     * than once per <code>delay</code> milliseconds. This prevents progress
     * updates from dominating the time spent doing real work If no busy dialog
     * is open, this method has no effect.
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
        BusyDialogImpl.currentProgress(currentValue, delay);
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
        BusyDialogImpl.maximumProgress(maximumValue);
    }

    /**
     * Returns the busy dialog most appropriate for the caller. If this is
     * called from the Runnable that was passed to a BusyDialog constructor,
     * then it will return the dialog that is displaying progress for that
     * Runnable. Otherwise, it returns the most recently created BusyDialog that
     * is still open. If no BusyDialog is open, it will return
     * <code>null</code>. The static methods
     * {@link #titleText}, {@link #statusText}, {@link #currentProgress}, and
     * {@link #maximumProgress} all modify the state of the current dialog.
     *
     * <p>
     * This method can be called from any thread.
     *
     * @return the busy dialog that this thread should modify, or
     * <code>null</code>
     */
    public static BusyDialog getCurrentDialog() {
        BusyDialogImpl curImpl = BusyDialogImpl.getCurrentDialog();
        return curImpl == null ? null : curImpl.getPublicOwner();
    }

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
        impl.setTitleText(text);
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
        impl.setStatusText(text);
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
        impl.setProgressCurrent(currentValue);
    }

    /**
     * Returns the current progress value of this busy dialog.
     *
     * @return the number of work units that have been completed
     * @see #setProgressCurrent
     * @see #setProgressMaximum
     */
    public int getProgressCurrent() {
        return impl.getProgressCurrent();
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
     * When this method is called and the <code>maximumValue</code> is not
     * negative, the current progress value is reset to 0.
     *
     * <p>
     * This method can be called from any thread.
     *
     * @param maximumValue the total number of units of work that must be
     * performed
     */
    public void setProgressMaximum(int maximumValue) {
        impl.setProgressMaximum(maximumValue);
    }

    /**
     * Returns the current maximum progress value, or -1 if in indeterminate
     * mode.
     *
     * @return the total number of work units to complete, or -1
     */
    public int getProgressMaximum() {
        return impl.getProgressMaximum();
    }

    /**
     * Returns <code>true</code> if this dialog has a cancel button and it has
     * been pressed by the user.
     *
     * @return <code>true</code> if the user has indicated that they wish to
     * cancel the operation
     */
    public boolean isCancelled() {
        return impl.isCancelled();
    }

    /**
     * This is a shared action listener that performs no action. It can be
     * passed to the constructor to create a busy dialog with a Cancel button
     * but with no explicit cancel action attached. The Runnable representing
     * the operation being performed can still determine if the Cancel button
     * has been pressed by calling {@link #isCancelled()}.
     */
    public static final ActionListener NO_CANCEL_ACTION = (ActionEvent e) -> {
    };
}
