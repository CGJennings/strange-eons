package ca.cgjennings.ui.wizard;

/**
 * A base class for {@link WizardListener}s that provides no-op implementations
 * for all of the listener methods. This allows you to create listeners that
 * only implement the methods you are interested in.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class WizardAdapter implements WizardListener {

    @Override
    public void wizardPageOrderChanged(WizardEvent e) {
    }

    @Override
    public void wizardReset(WizardEvent e) {
    }

    @Override
    public void wizardPageChanged(WizardEvent e) {
    }

    @Override
    public void wizardHidingPage(WizardEvent e) {
    }

    @Override
    public void wizardShowingPage(WizardEvent e) {
    }

    @Override
    public void wizardFinished(WizardEvent e) {
    }

    @Override
    public void wizardBlockStateChanged(WizardEvent e) {
    }
}
