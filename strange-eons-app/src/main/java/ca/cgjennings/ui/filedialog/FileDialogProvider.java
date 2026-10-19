package ca.cgjennings.ui.filedialog;

import java.awt.Component;

/**
 * Shows file dialogs on behalf of the application.
 */
public interface FileDialogProvider {

    /**
     * Shows a modal dialog for the request.
     *
     * @param parent the component the dialog is shown over
     * @param request the dialog to show
     * @return the selection, or {@code null} if the dialog was cancelled
     */
    FileDialogResult show(Component parent, FileDialogRequest request);

    /**
     * Returns whether the dialog itself asks before replacing an existing
     * file in {@link FileDialogRequest.Mode#SAVE} mode.
     */
    boolean confirmsReplace();
}
