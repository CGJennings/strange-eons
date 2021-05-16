package ca.cgjennings.ui.wizard;

import javax.swing.JComponent;

/**
 * A default implementation of {@link WizardModel}.
 *
 * <p>
 * The methods {@link #aboutToHide}, {@link #aboutToShow}, and {@link #finish}
 * take no direct action but do fire events.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class DefaultWizardModel extends AbstractWizardModel {

    private JComponent[] pages;

    /**
     * Creates a default wizard model with no pages. The user must call
     * {@link #setPages} before using the model.
     */
    public DefaultWizardModel() {
    }

    /**
     * Creates a default wizard model with an initial set of pages.
     *
     * @param pages the page list
     * @throws NullPointerException if {@code pages} is {@code null}
     */
    public DefaultWizardModel(JComponent[] pages) {
        if (pages == null) {
            throw new NullPointerException("pages");
        }
        this.pages = pages;
    }

    /**
     * Sets the list of fixed pages to be used by the model. This does not reset
     * the page order, so it can be used to create non-linear page structures.
     * For example, depending on the controls selected by the user, you could
     * set an alternate page order that diverges after the current page.
     *
     * @param pages the page list
     * @throws NullPointerException if {@code pages} is {@code null}
     */
    public void setPages(JComponent[] pages) {
        if (pages == null) {
            throw new NullPointerException("pages");
        }
        if (this.pages != pages) {
            this.pages = pages;
            firePageOrderChanged();
        }
    }

    /**
     * Returns the current page order. This is identical to calling
     * {@link #getPageOrder()}; this method is provided for symmetry with
     * {@link #setPages}.
     *
     * @return the current page order, or {@code null} if no pages have
     * been set
     */
    public final JComponent[] getPages() {
        return getPageOrder();
    }

    @Override
    public JComponent[] getPageOrder() {
        return pages;
    }
}
