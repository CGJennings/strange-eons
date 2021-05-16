package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.commands.AbstractCommand;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.apps.arkham.commands.DelegatedCommand;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import java.io.File;
import java.io.IOException;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * Support editors are used to edit content other than game components. To
 * create a new support editor, override {@link #canPerformCommand}, if
 * necessary, to return {@code true} for all {@link DelegatedCommand}s that
 * you wish to support. Standard editor commands null ({@link #clearImpl() clear}, {@link #exportImpl export}, {@link #saveImpl save},
 * {@link #printImpl}, and {@link #spinOffImpl spin off}) can be supported by
 * overriding the related implementation method. Other delegated commands can be
 * supported after overriding
 * {@link #canPerformCommand}, {@link #isCommandApplicable}, and
 * {@link #performCommand} to accept and implement command support. You must
 * also implement {@link #getFileNameExtension} and
 * {@link #getFileTypeDescription} to return the primary file name extension for
 * the file type that the editor supports (or the type currently being edited if
 * the editor supports multiple types). To support exporting to other formats,
 * override {@link #getExportExtensions()} and {@link #getExportDescriptions()}
 * to describe the supported file formats.
 *
 * <p>
 * To set the title used for the editor's tab (or window, depending on
 * settings), use {@link #setTitle}; to set the tab's icon, use
 * {@link #setFrameIcon}. When the user modifies the document, call
 * {@code setUnsavedChanges(&nbsp;true&nbsp;)} to update the document's
 * "dirty" state. (Do not set this if editing or saving is not supported.)
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public abstract class AbstractSupportEditor extends AbstractStrangeEonsEditor implements StrangeEonsEditor {

    /**
     * Creates new form AbstractSupportEditor
     */
    public AbstractSupportEditor() {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 394, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 294, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Returns {@code true} if the commandable wishes to handle the given
     * command. This method defines the set of commands that the commandable
     * responds to. The commandable might not be able to act on the command at
     * the current moment. For example, a commandable that responds to "Cut"
     * could return true from this method, but false from
     * {@link #isCommandApplicable} if there is currently no selection to cut.
     *
     * <p>
     * The base class for support editors returns true for the following
     * standard commands: CLEAR, EXPORT, PRINT, SAVE, SAVE_AS.
     *
     * @param command the command to be performed
     * @return {@code true} if this commandable wishes to handle the
     * command (even if it cannot execute the command currently)
     * @see Commands
     */
    @Override
    public boolean canPerformCommand(AbstractCommand command) {
        return command == Commands.CLEAR
                || command == Commands.EXPORT
                || command == Commands.PRINT
                || command == Commands.SAVE
                || command == Commands.SAVE_AS;
    }

    /**
     * Exports the editor content as a different file format. The base class
     * implementation handles the details of determining the file to be written
     * and exception handling. To use it, override {@link #exportImpl} to write
     * the document, and {@link #getExportExtensions} and
     * {@link #getExportDescriptions} to describe the possible formats.
     *
     * @throws UnsupportedOperationException if the EXPORT command is not
     * supported by this editor
     * @see #canPerformCommand
     */
    @Override
    public void export() {
        if (!canPerformCommand(Commands.EXPORT)) {
            throw new UnsupportedOperationException();
        }
        if (!isCommandApplicable(Commands.EXPORT)) {
            return;
        }

        String[] exts = getExportExtensions();
        String[] descs = getExportDescriptions();
        if (exts.length == 0) {
            throw new AssertionError("to use export framework, override getExportExtensions() and getExportDescriptions()");
        }
        if (exts.length != descs.length) {
            throw new AssertionError("number of extensions and descriptions must be equal");
        }

        File base = getFile();
        String name;
        if (base == null) {
            name = string("cni-name-default");
        } else {
            name = getFile().getName();
            int dot = name.lastIndexOf('.');
            if (dot >= 0) {
                name = name.substring(0, dot);
            }
        }

        // the "selected" export type
        int type = 0;
        File target = ResourceKit.showGenericExportFileDialog(StrangeEons.getWindow(), name, descs[type], exts[type]);
        if (target == null) {
            return;
        }

        try {
            exportImpl(type, target);
        } catch (Exception e) {
            ErrorDialog.displayError(string("rk-err-export"), e);
        }
    }

    /**
     * Subclasses should override this to export the edited content if EXPORT is
     * a supported command. The value of {@code type} is the index of the
     * element in {@link #getExportExtensions()} for the file type that was
     * selected by the user. The value of {@code file} is the destination
     * file to write content to.
     *
     * @param type the index of the file format to export
     * @param file the file to write content to
     * @throws IOException if an error occurs while writing
     */
    protected void exportImpl(int type, File file) throws IOException {
        throw new AssertionError("exportImpl not implemented");
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    /**
     * Returns the standard file extensions supported for export from this
     * editor. This implementation returns an empty array. If your editor
     * supports export, it should override this to return
     *
     * @return an array of file extensions, such as {@code "txt"}
     * <p>
     * This base implementation returns an empty array.
     */
    public String[] getExportExtensions() {
        return EMPTY_EXPORT;
    }

    /**
     * Returns a description of the file types supported for export from this
     * editor. The length and indices of this array match those returned by
     * {@link #getExportExtensions()}, that is, element 0 of this array is a
     * description of the file type represented by the element 0 of the
     * extensions array.
     *
     * @return an array of human-readable type descriptions, such as "Text
     * document"
     * <p>
     * This base implementation returns an empty array.
     */
    public String[] getExportDescriptions() {
        return EMPTY_EXPORT;
    }

    private static final String[] EMPTY_EXPORT = new String[0];
}
