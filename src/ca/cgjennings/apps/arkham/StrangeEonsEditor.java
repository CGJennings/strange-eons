package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.commands.AbstractCommand;
import ca.cgjennings.apps.arkham.commands.Commandable;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.apps.arkham.component.GameComponent;
import java.io.File;
import java.util.EventListener;
import javax.swing.Icon;
import javax.swing.JPopupMenu;

/**
 * This interface specifies the base level of functionality provided by every
 * Strange Eons editor. You are not expected to implement this interface
 * directly. Concrete editors are subclasses of
 * {@link AbstractStrangeEonsEditor}, and in practice all editors are of one of
 * two subclasses of this class: either {@link AbstractGameComponentEditor} or
 * {@link AbstractSupportEditor}. Game component editors are used to edit
 * {@link GameComponent}s, while support editors are used to edit non-component
 * project files. The purpose of this interface is to describe the basic
 * functionality that is common to both game component editors and support
 * editors without getting distracted by the details of the base class used to
 * provide the UI functionality.
 *
 * <p>
 * The methods and interfaces defined in this interface can be divided into the
 * following categories: interface management (such as setting the title and
 * icon, selecting or closing the window, and so on), the editing framework
 * (which support file and content management and some standard commands like
 * {@link Commands#SAVE} and {@link Commands#PRINT}), and event services (such
 * as registering listeners for various editor-specific events).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see AbstractStrangeEonsEditor
 * @see AbstractGameComponentEditor
 * @see AbstractSupportEditor
 * @see StrangeEonsAppWindow#addEditor
 * @see StrangeEonsAppWindow#getEditors
 */
public interface StrangeEonsEditor extends Commandable {

    ///////////////////////////
    // INTERFACE MANAGEMENT  //////////////////////////////////////////////
    ///////////////////////////
    /**
     * Sets the title of this document. As it shown in the document's tab, the
     * title text should be kept short. The <code>title</code> value may be
     * <code>null</code>, which is equivalent to an empty title.
     *
     * @param title the text of the title to display
     */
    public void setTitle(String title);

    /**
     * Returns the title used to describe this editor. The returned value is
     * never <code>null</code>; if a <code>null</code> title is set with
     * {@link #setTitle} then this method returns an empty string.
     *
     * @return the current title, which is guaranteed not to be
     * <code>null</code>
     */
    public String getTitle();

    /**
     * Sets the preferred icon to use for this editor window. This icon may be
     * used to represent the window in various parts of the application
     * interface. Possible example uses include the following: the document tab,
     * the editor's item in the Window menu, and the frame icon of the editor's
     * detached window. Note, however, that there is no guarantee that the icon
     * will be used, or how. Furthermore, the icon may be used in modified form.
     *
     * @param icon the preferred icon for the editor window
     * @see #getFrameIcon()
     */
    public void setFrameIcon(Icon icon);

    /**
     * Returns the editor window icon.
     *
     * @return the icon for the editor, or <code>null</code> if no icon is set
     */
    public Icon getFrameIcon();

    /**
     * Sets the tool tip text to display for the editor's tab.
     *
     * @param toolTipText the text to display, or <code>null</code> to clear the
     * tool tip
     */
    public void setToolTipText(String toolTipText);

    /**
     * Returns the tool tip text displayed for the editor's tab. If no tool tip
     * has been explicitly set and {@link #getFile()} returns a
     * non-<code>null</code> value, then a default tool tip will be returned.
     *
     * @return the tool tip text to display, or <code>null</code> to clear the
     * tool tip
     */
    public String getToolTipText();

    /**
     * Selects this editor, making it the active editor.
     *
     * @see StrangeEonsAppWindow#getActiveEditor()
     */
    public void select();

    /**
     * Closes the editor window. If there are unsaved changes, the user will be
     * prompted first and may cancel the close attempt.
     *
     * @return returns <code>true</code> if the editor was actually closed, or
     * <code>false</code> if the user cancelled the attempt or closing was
     * otherwise prevented
     */
    public boolean close();

    /**
     * Attaches or detaches an editor from the tab strip. A detached editor is
     * shown in its own window, independent of the main application window.
     *
     * @param attach if <code>true</code>, attach a detached editor; otherwise
     * detach the editor from the tab strip
     */
    public void setAttached(boolean attach);

    /**
     * Returns <code>true</code> if the editor is attached to the tab strip.
     *
     * @return <code>true</code> if the editor is attached to the main
     * application window
     */
    public boolean isAttached();

    /**
     * Returns a popup menu for this editor when the editor's tab is right
     * clicked in the document tab strip. Because a new menu instance is created
     * each time it is called, subclasses may call a super implementation to
     * obtain a default menu and then modify it.
     *
     * @return the popup menu that will be displayed when the user right clicks
     * on the document's tab
     */
    public JPopupMenu getTabStripPopupMenu();

    ///////////////////////
    // EDITING FRAMEWORK //////////////////////////////////////////////////
    ///////////////////////
    /**
     * Returns the edited game component, or <code>null</code> if this editor is
     * not editing a game component.
     *
     * @return the game component currently being edited by this editor, or
     * <code>null</code>
     */
    public GameComponent getGameComponent();

    /**
     * Returns the standard file extension for the type of content displayed in
     * this editor.
     *
     * @return a file extension, such as <code>"txt"</code>
     * @see #getFileTypeDescription()
     */
    public String getFileNameExtension();

    /**
     * Returns a description of the content displayed in this editor. For
     * English descriptions, this should be plural as it is used to describe the
     * file type in save dialogs.
     *
     * @return a human-friendly description of the content associated with the
     * editor, such as "Text Files"
     * @see #getFileNameExtension()
     */
    public String getFileTypeDescription();

    /**
     * Sets the preferred file to use when saving the edited component.
     *
     * @param newFile the file to use for Save operations
     * @see #getFile
     */
    public void setFile(File newFile);

    /**
     * Returns the file used to save this component, or <code>null</code> if it
     * is a new, unsaved file or the editor is not associated with any
     * particular file.
     *
     * @return the save file
     * @since 2.1a5
     * @see #setFile
     */
    public File getFile();

    /**
     * Returns <code>true</code> if this editor is editing a file that has
     * unsaved changes. If the content cannot be saved or does not use files,
     * then this method should return <code>false</code>.
     *
     * @return <code>true</code> if the editor's content may be different from
     * the saved version
     */
    public boolean hasUnsavedChanges();

    /**
     * {@inheritDoc}
     */
    @Override
    boolean canPerformCommand(AbstractCommand command);

    /**
     * {@inheritDoc}
     */
    @Override
    boolean isCommandApplicable(AbstractCommand command);

    /**
     * {@inheritDoc}
     */
    @Override
    void performCommand(AbstractCommand command);

    /**
     * Saves this editor's content to its current save location. If it has not
     * been saved previously, and the command is supported, the user will be
     * prompted to select a file (see {@link #saveAs}).
     *
     * <p>
     * Note that an editor can support the SAVE_AS command without supporting
     * the SAVE command, but not vice-versa.
     *
     * @throws UnsupportedOperationException if the SAVE command is not
     * supported
     * @see #canPerformCommand
     */
    public void save();

    /**
     * Saves this editor's content to a file selected by the user. The user may
     * cancel the operation. If the save is performed, then the selected file
     * will become the new save path for the component.
     *
     * @throws UnsupportedOperationException if the SAVE_AS command is not
     * supported
     * @see #canPerformCommand
     */
    public void saveAs();

    /**
     * Clears the contents of this component to a blank state, if the user gives
     * permission.
     *
     * @throws UnsupportedOperationException if the CLEAR command is not
     * supported
     * @see #canPerformCommand
     */
    public void clear();

    /**
     * Exports the editor content as a different file format. The procedure for
     * exporting and the exact format(s) available will vary with the editor.
     *
     * @throws UnsupportedOperationException if the EXPORT command is not
     * supported
     * @see #canPerformCommand
     */
    public void export();

    /**
     * Open the print dialog for this editor, allowing the user to print the
     * edited component. If this operation is not supported by this editor, an
     * <code>UnsupportedOperationException</code> is thrown.
     *
     * @throws UnsupportedOperationException if the PRINT command is not
     * supported
     * @see #canPerformCommand
     */
    public void print();

    /**
     * Creates a duplicate of this editor. A new editor containing a copy of
     * this editor's content will be created and added to the application. The
     * new component is a deep copy, not a reference (changes to the new
     * component do not affect the original, and vice-versa).
     *
     * @return the new, duplicate editor
     * @throws UnsupportedOperationException if the SPIN_OFF command is not
     * supported
     * @see #canPerformCommand
     */
    public StrangeEonsEditor spinOff();

    /**
     * Adds a new {@link EditorListener} to this editor. An editor listener can
     * also be added using {@link StrangeEonsAppWindow#addEditorListener}
     * interface, in which case it will apply to all editors. If a listener is
     * removed, it must be removed through the same interface as it was added
     * (the application or a specific editor).
     *
     * @param el the listener to add
     */
    public void addEditorListener(EditorListener el);

    /**
     * Removes a new {@link EditorListener} from this editor.
     *
     * @param el the listener to remove
     */
    public void removeEditorListener(EditorListener el);

    /**
     * A listener that is called when an editor becomes active, inactive, or is
     * closed.
     */
    public interface EditorListener extends EventListener {

        /**
         * An editor has become the selected editor.
         *
         * @param editor the editor that was activated
         */
        public void editorSelected(StrangeEonsEditor editor);

        /**
         * An editor is no longer the selected editor.
         *
         * @param editor the editor that was deactivated
         */
        public void editorDeselected(StrangeEonsEditor editor);

        /**
         * An editor is about to close and will be removed from the application.
         *
         * @param editor the editor is closing
         */
        public void editorClosing(StrangeEonsEditor editor);

        /**
         * An editor has been detached from the application.
         *
         * @param editor the editor that was detached
         */
        public void editorDetached(StrangeEonsEditor editor);

        /**
         * An editor has been reattached to the application.
         *
         * @param editor the editor that was reattached
         */
        public void editorAttached(StrangeEonsEditor editor);
    }

    /**
     * A heartbeat listener is notified by an editor once per heartbeat. An
     * editor's heartbeat is used to performed tasks that must be repeated on a
     * timed basis. The primary example is the synchronization of preview window
     * updates with editing by an {@link AbstractGameComponentEditor}, in which
     * the editor waits to redraw the preview window until the current round of
     * quick edits has finished. (For example, if the user types a word and then
     * pauses, the preview window would typically be redrawn during the pause
     * rather than while the user was typing.) Other kinds of editors may use
     * the editor's heartbeat for different purposes. Even editors that do not
     * require a heartbeat themselves are expected to provide one for use by
     * plug-ins.
     *
     * <p>
     * Editors that use the heartbeat to synchronize preview updates will
     * respond to a boolean value returned by the listener. If this method
     * returns <code>true</code>, it is assumed that the listener has made
     * changes to the edited content. These changes may or may not be detectable
     * by the editor; the editor should thus redraw the preview from scratch at
     * the next opportunity.
     *
     * @see AbstractStrangeEonsEditor#addHeartbeatListener
     * @see AbstractStrangeEonsEditor#createTimer
     */
    public static interface HeartbeatListener extends EventListener {

        /**
         * Called to indicate that a heartbeat is taking place in the indicated
         * editor. If a listener has attached to an editor for the purpose of
         * modifying the edited content, it should return <code>true</code> on
         * those heartbeats where a genuine modification takes place. In any
         * other case, the listener should return <code>false</code>.
         *
         * <p>
         * <b>Important:</b> Editors that synchronize redrawing using the
         * heartbeat, including nearly all game component editors, will
         * <i>never</i> update the preview display if a heartbeat listener
         * always returns <code>true</code>.
         *
         * @param editor the editor that is processing a heartbeat
         * @return <code>true</code> if the edited content was modified
         */
        public boolean heartbeat(StrangeEonsEditor editor);
    }

    /**
     * A listener to be called by a game component editor when it has updated
     * its edit controls by copying from the current component.
     */
    public static interface FieldPopulationListener extends EventListener {

        /**
         * Called when the specified editor has copied the state from the
         * component it is editing into its editing controls.
         *
         * @param editor the editor that has updated its fields
         */
        public void fieldsPopulated(StrangeEonsEditor editor);
    }
}
