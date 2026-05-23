package ca.cgjennings.ui.wizard;

/**
 * Interface implemented by objects that listen to events fired from a
 * {@link WizardModel}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface WizardListener {

    /**
     * Event fired when the set of pages in the model changes.
     *
     * @param e an object that provides more details about the event
     */
    void wizardPageOrderChanged(WizardEvent e);

    /**
     * Event fired when the model is reset to its initial state.
     *
     * @param e an object that provides more details about the event
     */
    void wizardReset(WizardEvent e);

    /**
     * Event fired when the model's current page changes.
     *
     * @param e an object that provides more details about the event
     */
    void wizardPageChanged(WizardEvent e);

    /**
     * Event fired when a page is being left.
     *
     * @param e an object that provides more details about the event
     */
    void wizardHidingPage(WizardEvent e);

    /**
     * Event fired when a page is becoming the current page.
     *
     * @param e an object that provides more details about the event
     */
    void wizardShowingPage(WizardEvent e);

    /**
     * Event fired when the model's {@link WizardModel#finish} method is called.
     *
     * @param e an object that provides more details about the event
     */
    void wizardFinished(WizardEvent e);

    /**
     * Event fired when the model's progress is blocked or unblocked.
     *
     * @param e an object that provides more details about the event
     */
    void wizardBlockStateChanged(WizardEvent e);
}
