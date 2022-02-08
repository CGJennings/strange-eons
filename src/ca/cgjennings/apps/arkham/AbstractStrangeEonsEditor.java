package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.commands.AbstractCommand;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.apps.arkham.component.AbstractGameComponent;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.dialog.prefs.Preferences;
import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import ca.cgjennings.apps.arkham.project.Member;
import ca.cgjennings.apps.arkham.project.Project;
import ca.cgjennings.apps.arkham.project.ProjectView;
import ca.cgjennings.ui.BlankIcon;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterAbortException;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Objects;
import java.util.logging.Level;
import javax.print.PrintException;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * The abstract base class from which all editors that can appear in the
 * document tab strip are derived.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public abstract class AbstractStrangeEonsEditor extends TAttachedEditor implements StrangeEonsEditor {

    /**
     * The default icon used by new editors.
     */
    public static final Icon DEFAULT_EDITOR_ICON;

    static {
        Icon i;
        try {
            i = ResourceKit.getIcon("res://editors/blank-editor-icon.png");
        } catch (Throwable t) {
            i = new BlankIcon(18, 18);
            StrangeEons.log.log(Level.WARNING, null, t);
        }
        DEFAULT_EDITOR_ICON = i;
    }

    /**
     * Creates a new abstract editor.
     */
    public AbstractStrangeEonsEditor() {
        setResizable(true);
        setFrameIcon(DEFAULT_EDITOR_ICON);

        // convert internal frame events to EditorListener events
        addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameClosing(InternalFrameEvent e) {
                fireEditorClosing();
                AppFrame.getApp().fireEditorClosing(AbstractStrangeEonsEditor.this);
            }

            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                fireEditorSelected();
                AppFrame.getApp().fireEditorSelected(AbstractStrangeEonsEditor.this);
            }

            @Override
            public void internalFrameDeactivated(InternalFrameEvent e) {
                fireEditorDeselected();
                AppFrame.getApp().fireEditorDeselected(AbstractStrangeEonsEditor.this);
            }
        });

        // If the subclass does not explicitly create a heartbeat timer in
        // its constructor, create a timer at the default rate.
        EventQueue.invokeLater(() -> {
            if (timer == null) {
                createTimer();
            }
        });
    }

    private void updateProjectNode() {
        ProjectView v = AppFrame.getApp().getOpenProjectView();
        if (v != null) {
            File f = getFile();
            if (f != null) {
                Project p = v.getProject();
                Member m = p.findMember(f);
                if (m != null) {
                    v.repaint(m);
                }
            }
        }
    }

    @Override
    public final void select() {
        if (!isVisible()) {
            setVisible(true);
        }
        try {
            setSelected(true);
        } catch (PropertyVetoException e) {
            StrangeEons.log.log(Level.WARNING, null, e);
        }
    }

    @Override
    public boolean close() {
        doDefaultCloseAction();
        return isClosed();
    }

    @Override
    public void doDefaultCloseAction() {
        if (confirmLossOfUnsavedChanges()) {
            fireEditorClosing();
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            super.doDefaultCloseAction();
            if (isClosed() && !isAttached()) {
                TDetachedEditor ded = getDetachedEditor();
                // actively stop tracking so if we are quitting we know the
                // window is closed regardless of when the window's listener
                // would have fired
                AppFrame.getApp().stopTracking(ded);
                ded.dispose();
            }
            updateProjectNode();
        }
    }

    /**
     * Sets the title of this document. As it shown in the document's tab, the
     * title text should be kept short. The {@code title} value may be
     * {@code null}, which is equivalent to an empty title.
     *
     * @param title the text of the title to display
     */
    @Override
    public void setTitle(String title) {
        showDefaultTitle = false;
        super.setTitle(title);
    }

    /**
     * Returns the title used to describe this editor. The returned value is
     * never {@code null}; if a {@code null} title is set with {@link #setTitle}
     * then this method returns an empty string.
     *
     * <p>
     * If no title has been explicitly set, then a default title will be
     * returned. The default title will be the game component name for game
     * component editors, and otherwise the file name. If there is no name or
     * file available, then a dummy title consisting of a single space is
     * returned.
     *
     * @return the current title, which is guaranteed not to be {@code null}
     */
    @Override
    public String getTitle() {
        if (showDefaultTitle) {
            String name;
            final GameComponent gc = getGameComponent();
            if (gc != null) {
                name = getGameComponent().getFullName();
                if (name == null) {
                    name = " ";
                } else {
                    name = AbstractGameComponent.filterComponentText(name);
                }
            } else {
                File f = getFile();
                if (f != null) {
                    name = f.getName();
                } else {
                    name = " ";
                }
            }
            return name;
        }
        return super.getTitle();
    }

    private boolean showDefaultTitle = true;

    /**
     * Sets the title of this document. As it shown in the document's tab, the
     * title text should be kept short. The {@code title} value may be
     * {@code null}, which is equivalent to an empty title.
     *
     * @param toolTipText the text of the title to display
     */
    @Override
    public void setToolTipText(String toolTipText) {
        showDefaultToolTip = false;
        super.setToolTipText(toolTipText);
    }

    /**
     * Returns the tool tip text displayed for the editor's tab. If no tool tip
     * has been explicitly set, a default tool tip will be returned. The default
     * tool tip will get the name of the file, if {@link #getFile()} returns a
     * non-{@code null} value, or else the localized interface text with key
     * {@code ae-unsaved} ("Untitled").
     *
     * @return the tool tip text to display, or {@code null} to clear the tool
     * tip
     */
    @Override
    public String getToolTipText() {
        if (showDefaultToolTip) {
            String name, source = null;
            File f = getFile();
            if (f == null) {
                String showing = Objects.toString(getClientProperty("showing"), null);
                if (showing == null) {
                    name = string("ae-unsaved");
                } else {
                    int sep = Math.max(showing.lastIndexOf('/'), showing.lastIndexOf(File.separatorChar));
                    if (sep < 0) {
                        name = showing;
                    } else {
                        name = showing.substring(sep + 1);
                        source = showing.substring(0, sep);
                    }
                }
            } else {
                name = f.getName();
                f = f.getParentFile();
                if (f != null) {
                    Project p = StrangeEons.getOpenProject();
                    if (p != null) {
                        Member m = p.findMember(f);
                        if (m != null) {
                            try {
                                source = URLDecoder.decode(m.getURL().toExternalForm(), "utf-8");
                            } catch (UnsupportedEncodingException uee) {
                            }
                        }
                    }
                    if (source == null) {
                        source = f.getPath();
                    }
                }
            }

            StringBuilder b = new StringBuilder(80).append("<html><b>")
                    .append(ResourceKit.makeStringHTMLSafe(name));
            if (source != null) {
                b.append("</b><br><font size=2>")
                        .append(ResourceKit.makeStringHTMLSafe(source))
                        .append("</font>");
            }
            return b.toString();
        }
        return super.getToolTipText();
    }

    private boolean showDefaultToolTip = true;

    @Override
    public JPopupMenu getTabStripPopupMenu() {
        JMenuItem item;
        final File f = getFile();
        final AppFrame app = AppFrame.getApp();
        JPopupMenu menu = new JPopupMenu();

        if (canPerformCommand(Commands.SAVE)) {
            item = new JMenuItem(string("save"));
            item.addActionListener(new CommandWrapper(Commands.SAVE));
            menu.add(item);
            menu.addSeparator();
        }

        item = new JMenuItem(string("close"));
        item.addActionListener((ActionEvent e) -> {
            close();
        });
        menu.add(item);

        item = new JMenuItem(string("app-pu-close-other"));
        item.addActionListener((ActionEvent e) -> {
            for (StrangeEonsEditor ed : AppFrame.getApp().getEditors()) {
                if (ed != AbstractStrangeEonsEditor.this) {
                    ed.close();
                }
            }
        });
        menu.add(item);

        if (isAttached()) {
            item = new JMenuItem(string("app-pu-detach"));
        } else {
            item = new JMenuItem(string("app-pu-attach"));
        }
        item.addActionListener((ActionEvent e) -> {
            EventQueue.invokeLater(() -> {
                setAttached(!isAttached());
            });
        });
        menu.addSeparator();
        menu.add(item);

        final Project proj = app.getOpenProject();
        final Member member = (f == null || proj == null) ? null : proj.findMember(f);

        if (member != null) {
            item = new JMenuItem();
            item.setText(string("app-pu-select"));
            item.addActionListener((ActionEvent e) -> {
                proj.getView().select(member);
            });
            menu.addSeparator();
            menu.add(item);
        }

        return menu;
    }

    /**
     * Action listener that performs a specified command on this editor (which
     * may or may not be the current editor).
     */
    private class CommandWrapper implements ActionListener {

        private final AbstractCommand c;

        CommandWrapper(AbstractCommand c) {
            this.c = c;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            performCommand(c);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The {@code AbstractStrangeEonsEditor} implementation always returns
     * {@code null}.
     */
    @Override
    public GameComponent getGameComponent() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The base class will return {@code false} for all commands.
     */
    @Override
    public boolean canPerformCommand(AbstractCommand command) {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * The base class returns the value of {@link #canPerformCommand}, except
     * that if the command is SAVE, it returns false if the editor does not have
     * unsaved changes.
     */
    @Override
    public boolean isCommandApplicable(AbstractCommand command) {
        boolean cpc = canPerformCommand(command);
        if (cpc && command == Commands.SAVE && !hasUnsavedChanges()) {
            cpc = false;
        }
        return cpc;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The base class provides a framework for handling common document
     * commands. To use the framework, override {@link #canPerformCommand} to
     * return true for the commands you wish to support. Then this method will
     * call the following methods (as appropriate): null null null     {@link #clear()}, {@link #save}, {@link #saveAs}, {@link #export},
	 * {@link #spinOff()}, or {@link #print()}. These methods handle common
     * details of implementing these commands, then pass control to a matching
     * implementation method which the subclass must override to perform those
     * aspects of the command which are unique to the editor's content.
     *
     * <p>
     * For example, to implement document saving, a subclass would override
     * {@link #canPerformCommand} to return true for the SAVE and SAVE_AS
     * commands, then override {@link #saveImpl} to write the editor content to
     * the provided file.
     */
    @Override
    public void performCommand(AbstractCommand command) {
        if (!isCommandApplicable(command)) {
            if (!canPerformCommand(command)) {
                throw new UnsupportedOperationException(command.toString());
            }
            return;
        }
        if (command == Commands.CLEAR) {
            clear();
        } else if (command == Commands.EXPORT) {
            export();
        } else if (command == Commands.PRINT) {
            print();
        } else if (command == Commands.SAVE) {
            save();
        } else if (command == Commands.SAVE_AS) {
            saveAs();
        } else if (command == Commands.SPIN_OFF) {
            spinOff();
        } else {
            StrangeEons.log.log(Level.WARNING, "unhandled command: {0}", command);
        }
    }

    /**
     * If {@link #hasUnsavedChanges()} returns {@code true}, this will display a
     * dialog box asking the user to confirm whether it is acceptable to lose
     * unsaved changes. The method returns {@code true} if the user wants to
     * continue (and lose the unsaved changes), otherwise {@code false}. If
     * {@code hasUnsavedChanges()} returns {@code false}, then this method will
     * return {@code true} without showing a dialog.
     *
     * <p>
     * <b>Note:</b> If the application is running in bundle test mode, then the
     * method will return {@code true} without showing a dialog box. (This makes
     * testing faster since the tester can experiment with the tested plug-in
     * without having to click on confirm buttons when the test finishes and the
     * test application instance is closed.)
     *
     * @return {@code true} if the operation can proceed
     * @see CommandLineArguments#plugintest
     */
    protected boolean confirmLossOfUnsavedChanges() {
        if (hasUnsavedChanges() && !BundleInstaller.hasTestBundles()) {
            AppFrame af = AppFrame.getApp();
            if ((af.getExtendedState() & JFrame.ICONIFIED) != 0) {
                af.setExtendedState(af.getExtendedState() & ~JFrame.ICONIFIED);
                af.toFront();
            }
            int option = JOptionPane.showConfirmDialog(
                    this, new String[]{string("ae-warn-unsaved")},
                    getTitle(),
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (option == JOptionPane.CANCEL_OPTION) {
                return false;
            }
            if (option == JOptionPane.YES_OPTION) {
                save();
            }
        }
        return true;
    }

    private File file;

    @Override
    public void setFile(File newFile) {
        if ((file == null && newFile != null) || (file != null && !file.equals(newFile))) {
            setUnsavedChanges(true);
        }
        file = newFile;
    }

    @Override
    public File getFile() {
        return file;
    }

    private boolean dirty;

    /**
     * Returns true if this document has unsaved changes. calling
     * {@code {@link #setUnsavedChanges}(&nbsp;true&nbsp;)}.
     *
     * @return {@code true} if changes would be lost if the document was closed
     * without saving
     * @see #setUnsavedChanges(boolean)
     */
    @Override
    public boolean hasUnsavedChanges() {
        return dirty;
    }

    /**
     * Marks whether the document has unsaved changes. The application uses this
     * to determine whether it is safe to close an editor without saving and to
     * update the document's visual state. This flag is cleared whenever the
     * file is saved successfully.
     *
     * @param isDirty if {@code true}, tells the application that this editor
     * has unsaved changes
     */
    public void setUnsavedChanges(boolean isDirty) {
        if (dirty != isDirty) {
            dirty = isDirty;
            updateTab();
            updateProjectNode();
        }
    }

    /**
     * {@inheritDoc}
     *
     * This implementation handles the details of determining the file to be
     * written and exception handling. If {@link #getFile()} returns
     * {@code null}, this method calls {@link #saveAs()} to determine the file
     * to save to. Otherwise, it calls {@link #saveImpl} to write the file. To
     * use it, override {@link #saveImpl(java.io.File)}.
     */
    @Override
    public void save() {
        if (!canPerformCommand(Commands.SAVE)) {
            throw new UnsupportedOperationException();
        }
        if (!isCommandApplicable(Commands.SAVE)) {
            return;
        }

        final File f = getFile();
        if (f == null) {
            saveAs(); // will call save again with selected file
        } else {
            StrangeEons.setWaitCursor(true);
            try {
                saveImpl(f);
                setUnsavedChanges(false);
                RecentFiles.addRecentDocument(f);
            } catch (Exception e) {
                ErrorDialog.displayError(string("ae-err-save"), e);
            } finally {
                StrangeEons.setWaitCursor(false);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * This implementation handles the details of determining the file to be
     * written and exception handling. It shows a save dialog and, if the user
     * accepts the dialog, calls {@code setFile} to set the save file based on
     * the user's selection. It then calls {@link #save()} to save the file to
     * the new destination. To use it, override {@link #getFileNameExtension()}
     * and {@link #getFileTypeDescription()} to describe the file type's
     * standard extension and type, and override {@link #saveImpl(java.io.File)}
     * to write the file.
     */
    @Override
    public void saveAs() {
        if (!canPerformCommand(Commands.SAVE_AS)) {
            throw new UnsupportedOperationException();
        }
        if (!isCommandApplicable(Commands.SAVE_AS)) {
            return;
        }

        File f = ResourceKit.showGenericSaveDialog(
                StrangeEons.getWindow(), getFile(),
                getFileTypeDescription(), getFileNameExtension()
        );
        if (f == null) {
            return;
        }
        setFile(f);
        save();
    }

    /**
     * Saves the editor content to the specified file. This method is called by
     * {link #save} in response to save requests. Subclasses must override it to
     * write an appropriate file for the document if the editor supports saving.
     *
     * @param f the file to write content to
     * @throws IOException if the save fails
     */
    protected void saveImpl(File f) throws IOException {
        throw new AssertionError("saveImpl not implemented");
    }

    /**
     * {@inheritDoc}
     *
     * This implementation allows the user to cancel the clear operation if
     * there are unsaved changes, and then calls {@code clearImpl} to handle the
     * details of clearing the edited content. To use it, override
     * {@link #clearImpl} to perform the clear operation.
     */
    @Override
    public void clear() {
        if (!canPerformCommand(Commands.CLEAR)) {
            throw new UnsupportedOperationException();
        }

        if (isCommandApplicable(Commands.CLEAR)) {
            if (confirmLossOfUnsavedChanges()) {
                clearImpl();
            }
        }
    }

    /**
     * Subclasses should override this to clear the edited document if CLEAR is
     * a supported command.
     */
    protected void clearImpl() {
        throw new AssertionError("clearImpl not implemented");
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The base class does not support exports, and will throw an exception if
     * this method is called. Subclasses that support the EXPORT command must
     * override this method to implement appropriate behaviour.
     *
     * @throws AssertionError if called when the EXPORT command is supported
     */
    @Override
    public void export() {
        if (!canPerformCommand(Commands.EXPORT)) {
            throw new UnsupportedOperationException();
        }
        throw new AssertionError("export not implemented");
    }

    /**
     * {@inheritDoc}
     *
     * This implementation creates a default {@code PrinterJob} and then passes
     * it to {@link #printImpl(java.awt.print.PrinterJob)}. It also performs
     * exception handling, including the {@code PrinterAbortException} that is
     * thrown if the user cancels a print. To implement printing, override
     * {@code printImpl} to print the content using the provided job. Note that
     * this method does not display a print dialog; you can do this from
     * {@code printImpl} using {@code job.printDialog()}.
     *
     * @see #printImpl
     */
    @Override
    public void print() {
        if (!canPerformCommand(Commands.PRINT)) {
            throw new UnsupportedOperationException();
        }
        if (!isCommandApplicable(Commands.PRINT)) {
            return;
        }
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(getTitle());

        StrangeEons.setWaitCursor(true);
        try {
            printImpl(job);
        } catch (PrinterAbortException e) {
        } catch (Exception e) {
            ErrorDialog.displayError(string("ae-err-print"), e);
        } finally {
            StrangeEons.setWaitCursor(false);
        }
    }

    /**
     * Subclasses should override this to print the edited document if PRINT is
     * a supported command.
     *
     * @param job the printer job to use for printing
     * @throws PrintException if a printing error occurs
     * @throws PrinterException if a printing error occurs
     */
    protected void printImpl(PrinterJob job) throws PrintException, PrinterException {
        throw new AssertionError("printImpl not implemented");
    }

    /**
     * {@inheritDoc}
     *
     * This implementation calls {@link #spinOffImpl} to create the new editor,
     * then adds the editor to the application.
     */
    @Override
    public StrangeEonsEditor spinOff() {
        if (!canPerformCommand(Commands.SPIN_OFF)) {
            throw new UnsupportedOperationException();
        }
        if (!isCommandApplicable(Commands.SPIN_OFF)) {
            return null;
        }
        StrangeEonsEditor ed = spinOffImpl();
        ed.setFile(null);
        StrangeEons.getWindow().addEditor(ed);
        return ed;
    }

    /**
     * Subclasses should override this to clone the edited document if SPIN_OFF
     * is a supported command.
     *
     * @return a new editor with an exact copy of this document
     */
    protected StrangeEonsEditor spinOffImpl() {
        throw new AssertionError("spinOffImpl not implemented");
    }

    @Override
    public void addEditorListener(EditorListener eal) {
        listenerList.add(EditorListener.class, eal);
    }

    @Override
    public void removeEditorListener(EditorListener eal) {
        listenerList.remove(EditorListener.class, eal);
    }

    private void fireEditorSelected() {
        Object[] list = listenerList.getListenerList();
        for (int i = 0; i < list.length; i += 2) {
            if (list[i] == EditorListener.class) {
                ((EditorListener) list[i + 1]).editorSelected(this);
            }
        }
    }

    private void fireEditorDeselected() {
        Object[] list = listenerList.getListenerList();
        for (int i = 0; i < list.length; i += 2) {
            if (list[i] == EditorListener.class) {
                ((EditorListener) list[i + 1]).editorDeselected(this);
            }
        }
    }

    private void fireEditorClosing() {
        Object[] list = listenerList.getListenerList();
        for (int i = 0; i < list.length; i += 2) {
            if (list[i] == EditorListener.class) {
                ((EditorListener) list[i + 1]).editorClosing(this);
            }
        }
    }

    /**
     * Creates a heartbeat timer that ticks at the default rate. The default
     * rate is determined from the setting key {@code update-rate}, and can be
     * changed in the {@link Preferences} dialog.
     *
     * @see #createTimer(int)
     */
    protected void createTimer() {
        if (getGameComponent() == null) {
            createTimer(Settings.getShared().getInt("update-rate"));
        } else {
            createTimer(getGameComponent().getSettings().getInt("update-rate"));
        }
    }

    /**
     * Creates a heartbeat timer at the specified rate, in milliseconds. Note
     * that an update period of longer than a few seconds may cause the
     * interface to appear to be responding sluggishly.
     *
     * @param updatePeriod the approximate time between heartbeats, in
     * milliseconds
     */
    protected void createTimer(int updatePeriod) {
        if (updatePeriod > 5_000) {
            StrangeEons.log.log(Level.WARNING, "editor using low update period ({0} ms)", updatePeriod);
        }

        destroyTimer();
        Timer t = new Timer(updatePeriod, (evt) -> {
            try {
                onHeartbeat();
            } catch (Exception e) {
                StrangeEons.log.log(Level.SEVERE, "uncaught exception during heartbeat of " + this, e);
            }
        });
        this.timer = t;
        t.start();
    }

    /**
     * Stops updating the preview pane when the game component is modified.
     *
     * @see #resumeTimedUpdates()
     */
    public void stopTimedUpdates() {
        if (timer != null) {
            timer.stop();
        }
    }

    /**
     * Resumes stopped preview updates.
     *
     * @see #stopTimedUpdates()
     */
    public void resumeTimedUpdates() {
        SwingUtilities.invokeLater(() -> {
            if (timer != null) {
                timer.start();
            }
        });
    }

    private void destroyTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }
    private Timer timer;

    /**
     * This method is called to allow the editor to perform heartbeat
     * processing. The base class will call {@link #fireHeartbeatEvent()} to
     * notify any attached listeners.
     */
    protected void onHeartbeat() {
        if (fireHeartbeatEvent()) {
            StrangeEons.log.log(Level.WARNING, "HeartbeatListener returned true, but this editor ignores that value: {0}", this);
        }

        // synch the tab state to changes in default title/tooltip
        updateTab();
    }

    /**
     * This method is called by {@link #onHeartbeat} to notify heartbeat
     * listeners. Subclasses that override the default heartbeat handling
     * without calling the super implementation must call this to ensure that
     * registered listeners are correctly notified.
     *
     * @return {@code true} if any registered listener returns {@code true} to
     * indicate content changes
     */
    protected final boolean fireHeartbeatEvent() {
        boolean changed = false;

        Object[] list = listenerList.getListenerList();
        for (int i = 0; i < list.length; i += 2) {
            if (list[i] == HeartbeatListener.class) {
                final HeartbeatListener hl = (HeartbeatListener) list[i + 1];
                changed |= hl.heartbeat(this);
            }
        }

        return changed;
    }

    /**
     * Adds a new {@code HeartbeatListener} to this editor.
     *
     * @param hl the listener to add
     * @since 2.00 (final)
     */
    public void addHeartbeatListener(HeartbeatListener hl) {
        listenerList.add(HeartbeatListener.class, hl);
    }

    /**
     * Removes a {@code HeartbeatListener} from this editor.
     *
     * @param hl the listener to remove
     * @since 2.00 (final)
     */
    public void removeHeartbeatListener(HeartbeatListener hl) {
        listenerList.remove(HeartbeatListener.class, hl);
    }

    /**
     * Removes all listeners for events specific to Strange Eons. This method is
     * called when the editor is being disposed of. It removes any editor and
     * heartbeat event listeners that were added through the editor. Subclasses
     * are responsible for overriding this method to remove any additional
     * listener types that they define and to call the superclass
     * implementation.
     */
    protected void removeAllStrangeEonsListeners() {
        Object[] list = listenerList.getListenerList();
        for (int i = list.length - 1; i >= 0; --i) {
            Object o = list[i];
            if (o instanceof HeartbeatListener) {
                removeHeartbeatListener((HeartbeatListener) o);
            } else if (o instanceof EditorListener) {
                removeEditorListener((EditorListener) o);
            }
        }
    }

    /**
     * Returns a string describing the editor, including the class name, size,
     * edited file, and game component (if any).
     *
     * @return a string describing the editor
     */
    @Override
    public String toString() {
        return "StrangeEonsEditor ("
                + getClass().getSimpleName()
                + "): ["
                + "size:" + getWidth() + "x" + getHeight() + ", "
                + "component: " + getGameComponent() + ", "
                + "file: \"" + getFile() + "\""
                + "]";
    }

    /**
     * Releases resources when the editor will no longer be used. Note that
     * although windows can typically be redisplayed after being disposed of by
     * simply making them visible, editors are not designed to be used after
     * this method is called.
     *
     * <p>
     * <b>Note:</b> This method should not typically be called by plug-in code.
     * If you wish to close an open editor, call its {@link #close()} method
     * instead.
     */
    @Override
    public void dispose() {
        destroyTimer();
        super.dispose();
        removeAllStrangeEonsListeners();
    }
}
