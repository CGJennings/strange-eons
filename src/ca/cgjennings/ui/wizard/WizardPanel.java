package ca.cgjennings.ui.wizard;

import java.awt.CardLayout;
import java.awt.LayoutManager;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * An interface panel that acts as a view for a {@link WizardModel}. It is not
 * directly linked to the surrounding dialog or other container, but listens for
 * events from the model. So, for example, to implement the Next and Back
 * buttons, you would give them action listeners that call the model's
 * {@link WizardModel#forward()} and {@link WizardModel#backward()} methods.
 * Typically, you don't set up this logic yourself but instead create a
 * {@link WizardController} to handle it for you.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class WizardPanel extends JPanel {

    /**
     * Creates a new panel for viewing the pages of a {@link WizardModel}. The
     * panel will have a {@code null} model.
     */
    public WizardPanel() {
        this(null);
    }

    /**
     * Creates a new panel for viewing the pages of a {@link WizardModel}. The
     * panel will use the provided model.
     *
     * @param model the model to be used by the panel
     */
    public WizardPanel(WizardModel model) {
        super.setLayout(layout);
        setModel(model);
    }

    /**
     * Sets the model used by the panel to determine which pages to show.
     *
     * @param m the new model
     */
    public void setModel(WizardModel m) {
        if (model == m) {
            return;
        }

        // stop listening to old model
        if (model != null) {
            model.removeWizardListener(li);
        }

        model = m;

        // attach listener to new model
        if (model != null) {
            model.addWizardListener(li);
        }

        // update children of the panel and show current page
        pagesChanged();
    }

    /**
     * Returns the model used to determine which pages to display and to
     * navigate between pages. May be {@code null}.
     *
     * @return the current page model
     */
    public WizardModel getModel() {
        return model;
    }

    /**
     * Called when the model or its pages change. Removes the current children
     * and adds the children from the new model (if non-null).
     */
    private void pagesChanged() {
        removeAll();
        if (model != null) {
            JComponent[] pages = model.getPageOrder();
            for (int i = 0; i < pages.length; ++i) {
                add(pages[i], String.valueOf(i));
            }
            showCurrentPage();
        }
    }

    /**
     * Show the model's reported current page in the panel.
     */
    private void showCurrentPage() {
        if (model != null) {
            layout.show(this, String.valueOf(model.getCurrentPage()));
            repaint();
        }
    }

    private CardLayout layout = new CardLayout();
    private WizardModel model;

    private WizardListener li = new WizardAdapter() {
        @Override
        public void wizardPageOrderChanged(WizardEvent e) {
            pagesChanged();
        }

        @Override
        public void wizardPageChanged(WizardEvent e) {
            if (model != e.getSource()) {
                throw new AssertionError();
            }
            showCurrentPage();
        }
    };

    /**
     * This method is overridden to have no effect. The wizard panel must use a
     * specific layout manager in order to control page flipping correctly, so
     * it ignores requests to change that manager. This method does nothing
     * rather than throwing an exception in order to avoid issues with GUI
     * layout editing tools.
     *
     * @param lm the new layout manager (ignored)
     * @deprecated This class uses its own internal layout manager and does not
     * support setting a different manager.
     */
    @Override
    @Deprecated
    public void setLayout(LayoutManager lm) {
    }
}
