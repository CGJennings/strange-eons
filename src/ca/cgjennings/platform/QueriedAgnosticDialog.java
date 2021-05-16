package ca.cgjennings.platform;

import javax.swing.JButton;

/**
 * A dialog may implement this instead of {@link AgnosticDialog} to have more
 * control over how the OK and Cancel buttons are modified.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface QueriedAgnosticDialog extends AgnosticDialog {

    /**
     * Called to swap the buttons of this agnostic dialog. When this interface
     * is used, {@link PlatformSupport#makeAgnosticDialog} will not swap any of
     * the properties of the buttons itself. Instead, it will call this method
     * to allow this to perform the swap itself. {@code PlatformSupport}
     * will still register listeners for {@link AgnosticDialog#handleOKAction}
     * and {@link AgnosticDialog#handleCancelAction}.
     * <p>
     * This method will always be called, even if the buttons are not being
     * swapped from their reported order. This allows the implementer to simply
     * place all initialization code for the buttons in this method, if desired.
     *
     * @param swapped {@code true} if the buttons should be swapped based
     * on the order reported to {@code PlatformSupport}
     * @param newOKButton the button that should be made the "OK" button
     * @param newCancelButton the button that should be made the "Cancel" button
     */
    public abstract void performAgnosticButtonSwap(boolean swapped, JButton newOKButton, JButton newCancelButton);
}
