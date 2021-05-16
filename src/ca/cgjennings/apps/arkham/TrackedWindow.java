package ca.cgjennings.apps.arkham;

import ca.cgjennings.ui.IconProvider;

/**
 * This interface is implemented by windows to be listed in the <b>Window</b>
 * menu. Except for {@link #getIcon}, this interface contains only methods
 * already implemented by windows, so windows to be tracked can either implement
 * this interface directly or else submit a proxy object that creates the window
 * on demand the first time it is made visible.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see StrangeEonsAppWindow#startTracking
 * @see TrackedWindowProxy
 */
public interface TrackedWindow extends IconProvider {

    /**
     * Returns the window title.
     *
     * @return the title of the window, as shown in the menu
     */
    String getTitle();

    /**
     * Makes the tracked window visible or invisible.
     *
     * @param visible whether the window should be made visible
     */
    void setVisible(boolean visible);

    /**
     * Moves the tracked window to the front; only called after the window is
     * made visible.
     */
    void toFront();

    /**
     * Requests that the window be given focus, if it is focusable.
     *
     * @return {@code false} if the request will certainly fail
     */
    boolean requestFocusInWindow();
}
