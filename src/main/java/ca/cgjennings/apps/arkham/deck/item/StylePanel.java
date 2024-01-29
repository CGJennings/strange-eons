package ca.cgjennings.apps.arkham.deck.item;

import java.awt.Component;

/**
 * A style panel is an interface component that allows the user to edit a
 * {@link Style}. The dialog for editing an item's style is built by creating a
 * style panel for each of the style interfaces that the item implements.
 *
 * @param <S> the {@link Style} interface edited by this panel
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface StylePanel<S extends Style> {

    /**
     * Returns a localized title that describes the style edited by this panel.
     *
     * @return the name of the edited style
     */
    String getTitle();

    /**
     * Returns the component (typically a {@code JPanel}) that contains the
     * controls used to edit the style.
     *
     * @return the editing control
     */
    Component getPanelComponent();

    /**
     * Copies the current state of a capture into the controls on this panel.
     *
     * @param capture updates the panel controls to reflect the capture
     */
    void populatePanelFromCapture(StyleCapture capture);

    /**
     * Copies the current state of the panel controls into a capture.
     *
     * @param capture the capture to copy this panel state into
     */
    void populateCaptureFromPanel(StyleCapture capture);

    /**
     * Returns the group that his panel belongs to. The group number determines
     * the order that the style panels are presented in.
     *
     * @return the group number for this panel
     */
    int getPanelGroup();

    /**
     * The style editor that has created this panel will call this method with a
     * callback instance. The instance should be used to notify the editor that
     * the style information in the panel has changed by calling the
     * {@link StyleEditorCallback#styleChanged()} method. Style panels should
     * gracefully handle the case where a callback is not set in case the panel
     * is used for other purposes.
     *
     * @param callback the callback that the panel must notify
     */
    void setCallback(StyleEditorCallback callback);

    /**
     * An object of this type will be passed to the {@link StylePanel} after it
     * is created. It allows the style panel to notify the style editor when the
     * user has edited style information in the panel.
     */
    public interface StyleEditorCallback {

        /**
         * Style panels must call this method when the user edits the style
         * information in the panel.
         */
        void styleChanged();
    }
}
