package ca.cgjennings.apps.arkham;

import java.awt.Dialog;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JWindow;

/**
 * An abstract base class for tracked windows that create the true window or
 * dialog on demand.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see StrangeEonsAppWindow#startTracking
 */
public abstract class TrackedWindowProxy implements TrackedWindow {

    private String title;
    private Icon icon;
    private Object trackee;

    /**
     * Creates a new proxy for a tracked window.
     *
     * @param windowTitle the non-{@code null} window title
     */
    public TrackedWindowProxy(String windowTitle) {
        this(windowTitle, null);
    }

    /**
     * Creates a new proxy for a tracked window.
     *
     * @param windowTitle the non-{@code null} window title
     * @param windowIcon the icon to use for the menu item (may be {@code null})
     */
    public TrackedWindowProxy(String windowTitle, Icon windowIcon) {
        if (windowTitle == null) {
            throw new NullPointerException("windowTitle");
        }
        icon = windowIcon;
        title = windowTitle;
    }

    /**
     * This method is called to create the dialog or window being tracked. It
     * must return either a {@link JWindow}, a {@link JFrame}, or a modeless
     * {@link JDialog}.
     *
     * @return the window that this is acting as a proxy for
     */
    public abstract Object createWindow();

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setVisible(boolean visible) {
        Object w = getTrackee();
        if (w instanceof JFrame) {
            ((JFrame) w).setVisible(visible);
        } else if (w instanceof JDialog) {
            ((JDialog) w).setVisible(visible);
        } else if (w instanceof JWindow) {
            ((JWindow) w).setVisible(visible);
        }
    }

    @Override
    public void toFront() {
        Object w = getTrackee();
        if (w instanceof JFrame) {
            ((JFrame) w).toFront();
        } else if (w instanceof JDialog) {
            ((JDialog) w).toFront();
        } else if (w instanceof JWindow) {
            ((JWindow) w).toFront();
        }
    }

    @Override
    public boolean requestFocusInWindow() {
        Object w = getTrackee();
        if (w instanceof JFrame) {
            return ((JFrame) w).requestFocusInWindow();
        } else if (w instanceof JDialog) {
            return ((JDialog) w).requestFocusInWindow();
        } else if (w instanceof JWindow) {
            return ((JWindow) w).requestFocusInWindow();
        }
        return false;
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    /**
     * Disposes of the underlying window or frame if it has been created.
     */
    public void dispose() {
        if (trackee == null) {
            return;
        }
        if (trackee instanceof JFrame) {
            ((JFrame) trackee).dispose();
        } else if (trackee instanceof JDialog) {
            ((JDialog) trackee).dispose();
        } else if (trackee instanceof JWindow) {
            ((JWindow) trackee).dispose();
        }
    }

    /**
     * Returns whether the window is visible if it has been created, otherwise
     * returns false.
     *
     * @return true if the window is visible
     */
    public boolean isVisible() {
        boolean v = false;
        if (trackee != null) {
            if (trackee instanceof JFrame) {
                v = ((JFrame) trackee).isVisible();
            } else if (trackee instanceof JDialog) {
                v = ((JDialog) trackee).isVisible();
            } else if (trackee instanceof JWindow) {
                v = ((JWindow) trackee).isVisible();
            }
        }
        return v;
    }

    private Object getTrackee() {
        if (trackee == null) {
            Object t = createWindow();
            if (t == null) {
                throw new AssertionError("createWindow() returned null");
            } else if (!((t instanceof JWindow) || (t instanceof JDialog) || (t instanceof JFrame))) {
                throw new AssertionError("createWindow() returned wrong type of object: " + t.getClass());
            }
            if (t instanceof Dialog && ((Dialog) t).getModalityType() != Dialog.ModalityType.MODELESS) {
                throw new IllegalStateException("dialog must be modeless to be tracked");
            }
            trackee = t;
        }
        return trackee;
    }
}
