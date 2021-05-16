package ca.cgjennings.ui.wizard;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;

/**
 * Controllers encapsulate the logic needed to coordinate the various components
 * of a wizard dialog. By using a controller, you do not need to subclass a
 * specific wizard dialog base class. Instead, you can convert any group of
 * appropriate controls to have wizard-like functionality.
 *
 * <p>
 * The controller links a group of {@code JButton}s, a {@link WizardPanel},
 * and a {@link WizardModel}. The buttons are used to control the page turning
 * and finish actions of the panel. The controller will attach appropriate
 * listeners to the buttons and use these to modify the current page in the
 * model or finish the wizard.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class WizardController {

    protected final JButton nextBtn;
    protected final JButton prevBtn;
    protected final JButton finishBtn;
    protected final WizardPanel panel;
    protected final WizardModel model;
    private ActionListener btnListener;
    private WizardListener wizListener;

    /**
     * Creates a new controller that makes a {@link WizardPanel} interactive by
     * linking it with a set of controls and a {@link WizardModel}.
     *
     * @param nextBtn the button that will advance to the next page
     * @param prevBtn the button that will go back to the previous page
     * @param finishBtn the button that will finish the wizard procedure
     * @param panel the panel that displays pages from the model
     * @param model the model that controls the page order and fires events
     */
    public WizardController(JButton nextBtn, JButton prevBtn, JButton finishBtn, WizardPanel panel, WizardModel model) {
        this.nextBtn = nextBtn;
        this.prevBtn = prevBtn;
        this.finishBtn = finishBtn;
        this.panel = panel;
        this.model = model;

        install();
    }

    /**
     * This is called by the constructor to install event handlers on the
     * buttons. Subclasses may override this to alter the controller logic.
     */
    protected void install() {
        if (btnListener != null) {
            throw new IllegalStateException("already installed");
        }

        btnListener = (ActionEvent e) -> {
            Object src = e.getSource();
            if (src == nextBtn) {
                handleNextPageButton();
            } else if (src == prevBtn) {
                handlePreviousPageButton();
            } else if (src == finishBtn) {
                handleFinishButton();
            }
        };
        nextBtn.addActionListener(btnListener);
        prevBtn.addActionListener(btnListener);
        finishBtn.addActionListener(btnListener);

        wizListener = new WizardAdapter() {
            @Override
            public void wizardPageOrderChanged(WizardEvent e) {
                updateButtonStates();
            }

            @Override
            public void wizardReset(WizardEvent e) {
                updateButtonStates();
            }

            @Override
            public void wizardPageChanged(WizardEvent e) {
                updateButtonStates();
            }

            @Override
            public void wizardFinished(WizardEvent e) {
                updateButtonStates();
            }

            @Override
            public void wizardBlockStateChanged(WizardEvent e) {
                updateButtonStates();
            }

        };
        model.addWizardListener(wizListener);

        updateButtonStates();
    }

    /**
     * Updates the enabled states of the buttons to reflect the current state of
     * the model.
     */
    protected void updateButtonStates() {
        nextBtn.setEnabled(model.canGoForward());
        prevBtn.setEnabled(model.canGoBackward());
        finishBtn.setEnabled(model.canFinish());
    }

    /**
     * Called to handle the user pressing the Next/Continue button. Can be
     * overridden to modify controller behaviour.
     */
    protected void handleNextPageButton() {
        if (model.canGoForward()) {
            model.forward();
        } else if (nextBtn == finishBtn && model.canFinish()) {
            handleFinishButton();
        }
    }

    /**
     * Called to handle the user pressing the Back/Go Back button. Can be
     * overridden to modify controller behaviour.
     */
    protected void handlePreviousPageButton() {
        if (model.canGoBackward()) {
            model.backward();
        }
    }

    /**
     * Called to handle the user pressing the Finish button. Can be overridden
     * to modify controller behaviour.
     */
    protected void handleFinishButton() {
        if (model.canFinish()) {
            model.finish();
        }
    }

    /**
     * Removes all listeners installed by the controller and causes it to cease
     * functioning as a controller. This should be called if you wish to replace
     * this controller with another one.
     */
    public void dispose() {
        if (btnListener != null) {
            nextBtn.removeActionListener(btnListener);
            prevBtn.removeActionListener(btnListener);
            finishBtn.removeActionListener(btnListener);
            btnListener = null;
        }
        if (wizListener != null) {
            model.removeWizardListener(wizListener);
            wizListener = null;
        }
    }

    /**
     * Returns the button that advances to the next page.
     *
     * @return the Next button
     */
    public JButton getNextPageButton() {
        return nextBtn;
    }

    /**
     * Returns the button that goes back to the next page.
     *
     * @return the Go Back button
     */
    public JButton getPreviousPageButton() {
        return prevBtn;
    }

    /**
     * Returns the button that finishes the wizard.
     *
     * @return the Finish button
     */
    public JButton getFinishButton() {
        return finishBtn;
    }

    /**
     * Returns the panel that displays the pages of the wizard.
     *
     * @return the panel linked to the controller
     */
    public WizardPanel getPanel() {
        return panel;
    }

    /**
     * Returns the model that controls the page logic.
     *
     * @return the model linked to the controller
     */
    public WizardModel getModel() {
        return model;
    }
}
