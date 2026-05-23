package ca.cgjennings.apps.arkham.deck.item;

import java.awt.Component;
import javax.swing.JPanel;

/**
 * An abstract base class for implementing a {@link StylePanel} as a
 * {@code JPanel}.
 *
 * @param <S> the style that this panel edits
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public abstract class AbstractStylePanel<S extends Style> extends JPanel implements StylePanel<S> {

    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation returns {@code this}; the style editing controls
     * should be children of this {@code JPanel}.
     *
     * @return this panel
     */
    @Override
    public Component getPanelComponent() {
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation will handle the editor callback for you; when the
     * user edits the style information you need only call
     * {@link #styleChanged()}.
     *
     * @param callback
     */
    @Override
    public void setCallback(StyleEditorCallback callback) {
        this.callback = callback;
    }

    /**
     * Notifies the callback, if any, that the style has changed. Subclasses
     * must call this method when the user edits the style information in the
     * panel.
     */
    protected void styleChanged() {
        if (callback != null) {
            callback.styleChanged();
        }
    }

    private StyleEditorCallback callback;

    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation assumes that the panel implements the {@link Style}
     * subclass that it edits. (So, if this extends
     * {@code AbstractStylePanel&lt;MyStyle&gt;} then it also implements
     * {@code MyStyle}.) It populates the specified capture by simply capturing
     * this panel instance (so the panel's getter methods for the style will be
     * called to obtain the panel state). If this panel does not implement the
     * style interface, an exception will be thrown at runtime.
     *
     * @param capture the capture to fill in with the panel state
     */
    @Override
    public void populateCaptureFromPanel(StyleCapture capture) {
        capture.capture(this);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation assumes that the panel implements the {@link Style}
     * subclass that it edits. (So, if this extends
     * {@code AbstractStylePanel<MyStyle>} then it also implements
     * {@code MyStyle}.) It populates the panel by simply applying the capture
     * to the panel (so the panel's setter methods for the style will be invoked
     * accordingly). If this panel does not implement the style interface, an
     * exception will be thrown at runtime.
     *
     * @param capture the capture to load this panel's state from
     */
    @Override
    public void populatePanelFromCapture(StyleCapture capture) {
        capture.apply(this);
    }

    @Override
    public int getPanelGroup() {
        return 0;
    }
}
