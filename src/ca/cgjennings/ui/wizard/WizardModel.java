package ca.cgjennings.ui.wizard;

import javax.swing.JComponent;

/**
 * Models the steps and step transitions in a wizard dialog.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface WizardModel {

    /**
     * Resets the model. This is called when the model is installed in a dialog.
     * It allows the model to be reused in multiple dialogs. This will reset the
     * current page to its initial value and will typically reset the interface
     * components that represent the wizard pages to an initial state.
     * (Alternatively, any existing page components may be thrown out and a new
     * set created.)
     */
    void reset();

    /**
     * Returns the current order of pages in the wizard. This method is called
     * to set up the panel and before each page transition. This allows the
     * model to change the page order in response to user choices.
     *
     * <p>
     * Unless otherwise stated, the caller must assume that the returned array
     * should be considered immutable. Changing the values of elements in the
     * array is forbidden.
     *
     * @return an array of components (typically panels) in the order they
     * should be displayed
     */
    JComponent[] getPageOrder();

    /**
     * Returns the index of the current page.
     *
     * @return the index of the element in {@link #getPageOrder()} that
     * represents the current page
     */
    int getCurrentPage();

    /**
     * Sets the current page of the internal model.
     *
     * @param index the new page index
     * @throws IndexOutOfBoundsException if the index does not fall within
     *
     */
    void setCurrentPage(int index);

    /**
     * Returns the number of pages in the current set of pages.
     *
     * @return the number of pages in the wizard
     */
    int getPageCount();

    /**
     * Returns <code>true</code> if there is a page after the current page. This
     * method must always return <code>false</code> when progress is blocked.
     *
     * @return whether the next button should be enabled
     * @see #isProgressBlocked()
     */
    boolean canGoForward();

    /**
     * Returns <code>true</code> if there is a page before the current page.
     *
     * @return whether the previous button should be enabled
     */
    boolean canGoBackward();

    /**
     * Returns <code>true</code> if the wizard can be finished in its current
     * state. This method must always return <code>false</code> when progress is
     * blocked.
     *
     * @return whether the finish button should be enabled
     * @see #isProgressBlocked()
     */
    boolean canFinish();

    /**
     * Causes the model's internal representation to move to the next page.
     *
     * @return the new page index
     * @throws IllegalStateException if there is no next page
     * @see #canGoForward()
     */
    int forward();

    /**
     * Causes the model's internal representation to move to the previous page.
     *
     * @return the new page index
     * @throws IllegalStateException if there is no previous page
     * @see #canGoBackward()
     */
    int backward();

    /**
     * Completes the wizard, causing any relevant actions to take place. It may
     * optionally return an arbitrary object to represent this result.
     *
     * @return an optional result, or <code>null</code>
     * @throws IllegalStateException if finishing is not currently possible
     * @see #canFinish()
     */
    Object finish();

    /**
     * Sets whether or not progress is blocked. When progress is blocked,
     * {@link #canGoForward()} and {@link #canFinish()} must always return
     * <code>false</code>. The progress blocking state can be cleared by calling
     * this method with false, and is cleared automatically if the page changes.
     * Blocking provides a mechanism for individual pages to prevent progress if
     * they are missing required information.
     *
     * @param block if <code>true</code>, prevents the user from going to the
     * next page or finishing the dialog.
     */
    void setProgressBlocked(boolean block);

    /**
     * Returns <code>true</code> if progress is currently blocked.
     *
     * @return <code>true</code> if the user is blocked from continuing
     */
    boolean isProgressBlocked();

    /**
     * Called before a page is displayed. The following procedure describes how
     * the model is consulted when moving to a new page:
     * <ol>
     * <li> User clicks next (or back).
     * <li> {@link #aboutToHide} called for current page.
     * <li> {@link #forward} (or {@link #backward}) is called.
     * <li> {@link #getPageOrder} and {@link #getCurrentPage} called to
     * determine which page to display.
     * <li> {@link #aboutToShow} called for new current page.
     * <li> New page is displayed. Back, next, and finish buttons are updated to
     * enable them based on {@link #canGoBackward()} and related methods.
     * </ol>
     *
     * @param index the index of the page to be shown
     * @param page the page component that corresponds to the index
     * @see #aboutToHide
     */
    void aboutToShow(int index, JComponent page);

    /**
     * Called before the current page is hidden when switching pages. After this
     * is called, but before {@link #aboutToShow} is called,
     * {@link #getPageOrder} will be called to determine if the page order has
     * changed.
     *
     * @param index the index of the currently displayed page
     * @param page the page component that corresponds to the index
     * @see #aboutToShow
     */
    void aboutToHide(int index, JComponent page);

    /**
     * Adds a listener that will receive {@link WizardEvent}s from the model.
     *
     * @param li the listener to add
     * @throws NullPointerException if the listener is <code>null</code>
     */
    void addWizardListener(WizardListener li);

    /**
     * Removes a listener from the list of listeners so that it no longer
     * receives {@link WizardEvent}s from the model.
     *
     * @param li the listener to removes
     * @throws NullPointerException if the listener is <code>null</code>
     */
    void removeWizardListener(WizardListener li);
}
