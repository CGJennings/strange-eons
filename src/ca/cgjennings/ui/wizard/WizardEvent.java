package ca.cgjennings.ui.wizard;

import javax.swing.JComponent;

/**
 * Describes events that are fired by a {@link WizardModel} to a
 * {@link WizardListener}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class WizardEvent {

    private WizardModel source;
    private int index = -1;
    private JComponent page;

    public WizardEvent(WizardModel source) {
        this(source, -1, null);
    }

    public WizardEvent(WizardModel source, int index, JComponent page) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        if (index < -1 || index >= source.getPageCount()) {
            throw new IndexOutOfBoundsException("index");
        }

        this.source = source;
        this.index = index;
        this.page = page;
    }

    /**
     * Returns the model that fired the event.
     *
     * @return the source of the event
     */
    public WizardModel getSource() {
        return source;
    }

    /**
     * Returns the index of the page referred to by the event, or -1 if no page
     * index is relevant.
     *
     * @return the index of the page involved in the event, or -1
     */
    public int getPageIndex() {
        return index;
    }

    /**
     * Returns the page component referred to by the event, or <code>null</code>
     * if no page is relevant.
     *
     * @return the page involved in the event, or <code>null</code>
     */
    public JComponent getPage() {
        return page;
    }
}
