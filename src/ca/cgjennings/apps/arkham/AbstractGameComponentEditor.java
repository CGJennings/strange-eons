package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.commands.AbstractCommand;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.apps.arkham.component.AbstractGameComponent;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.component.design.DesignSupport;
import ca.cgjennings.apps.arkham.deck.Deck;
import ca.cgjennings.apps.arkham.deck.DeckEditor;
import ca.cgjennings.apps.arkham.deck.PDFPrintSupport;
import ca.cgjennings.apps.arkham.deck.item.CardFace;
import ca.cgjennings.apps.arkham.deck.item.TextBoxEditor;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.diy.DIYEditor;
import ca.cgjennings.apps.arkham.editors.AbbreviationTableManager;
import ca.cgjennings.apps.arkham.plugins.PluginContextFactory;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.apps.arkham.project.DeckPacker;
import ca.cgjennings.apps.arkham.sheet.MarkerStyle;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.textedit.completion.AbbreviationTable;
import gamedata.Game;
import java.awt.AWTKeyStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import javax.print.PrintException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;
import resources.Language;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * The abstract base class for {@link GameComponent} editors. Editors for
 * components based on compiled code will typically be a direct subclass of
 * {@code AbstractGameComponentEditor}. Editors for components based on DIY
 * scripts use {@link DIYEditor}s.
 *
 * @param <G> the type of game component to be edited
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public abstract class AbstractGameComponentEditor<G extends GameComponent> extends AbstractStrangeEonsEditor {

    /**
     * Creates a new abstract game component editor.
     */
    public AbstractGameComponentEditor() {
        AppFrame.getApp().addPropertyChangeListener(StrangeEonsAppWindow.VIEW_BACKDROP_PROPERTY, pcl);
    }

    private final PropertyChangeListener pcl = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            // respond to changes in the selected preview background
//			if( evt.getPropertyName().equals( StrangeEonsAppWindow.VIEW_QUALITY_PROPERTY ) ) {
            if (sheets != null && sheets.length > 0) {
                repaint();
            }
//			}
        }
    };

    /**
     * Make any special changes needed to localize this editor for the host
     * platform. The base implementation is intended for game component editors.
     */
    protected void localizeForPlatform() {
        localizeComponentTree(this);
        setMinimumTabbedPaneSizes(rootPane);

        createPreviewSplitPane();
        pack();
    }

    public static void localizeComponentTree(JComponent root) {
        if (PlatformSupport.PLATFORM_IS_MAC) {
            localizeComponentForOSX(root);
        } else {
            localizeComponentGeneric(root);
        }

        for (int i = 0; i < root.getComponentCount(); ++i) {
            Component child = root.getComponent(i);
            if (child instanceof JComponent) {
                localizeComponentTree((JComponent) child);
            }
        }
    }

    /**
     * An editor's content pane consists of a left side (editor) and a right
     * side (preview). This method extracts those two components, places them in
     * a split pane, and then then places the split pane in the content pane. In
     * effect, the editor and preview areas are automagically wrapped in a split
     * pane.
     */
    private void createPreviewSplitPane() {
        Container content = rootPane.getContentPane();

        // only applicable to editors with exactly two components;
        // an editor and a preview window
        if (content.getComponentCount() != 2) {
            return;
        }

        Component editPane = content.getComponent(0);
        Component previewPane = content.getComponent(1);

        if (editPane instanceof JTabbedPane) {
            if (((JTabbedPane) editPane).getTabCount() == 0) {
                Component swapTemp = editPane;
                editPane = previewPane;
                previewPane = swapTemp;
            }
        }

        content.remove(editPane);
        content.remove(previewPane);

        previewSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, editPane, previewPane);
        previewSplitPane.setName("previewSplitPane");
        content.setLayout(new BorderLayout());
        content.add(previewSplitPane);

        // allow validation splitter to float freely over editor space
        editPane.setMinimumSize(new Dimension(0, editPane.getMinimumSize().height));
        previewSplitPane.setDividerLocation((int) editPane.getPreferredSize().getWidth());

//		previewSplitPane.setDividerLocation(-1);
        previewSplitPane.setResizeWeight(0d);
        previewSplitPane.setDividerSize(8); // Magic number: width of divider widget

        previewSplitPane.setOneTouchExpandable(true);
    }
    protected JSplitPane previewSplitPane;

    private void setMinimumTabbedPaneSizes(Container parent) {
        for (int i = 0; i < parent.getComponentCount(); ++i) {
            Component c = parent.getComponent(i);
            if (c instanceof JTabbedPane) {
                c.setMinimumSize(
                        new Dimension(c.getPreferredSize().width, c.getMinimumSize().height));
            }
            if (c instanceof Container) {
                setMinimumTabbedPaneSizes((Container) c);
            }
        }
    }

    /**
     * Sets the design support for this editor. A design support provides
     * feedback on the state of the design as the user edits it. Design supports
     * provide a view component that is displayed below the standard editing
     * tabs.
     *
     * @param ds the design support for this editor
     */
    public void setDesignSupport(DesignSupport ds) {
        if (ds == designSupport) {
            StrangeEons.log.warning("set design support to current value: " + this);
            return;
        }

        // remove existing design support
        if (designSupport != null) {
            Container content = getRootPane().getContentPane();
            Component c0 = content.getComponent(0);
            // sheet viewer split pane installed
            if (c0 instanceof JSplitPane && c0 != designSupportSplitPane) {
                ((JSplitPane) c0).setLeftComponent(designSupportEditPanel);
            } // sheet viewer split pane not installed
            else {
                content.remove(0);
                content.add(designSupportEditPanel, 0);
            }
            designSupport = null;
            designSupportSplitPane = null;
            designSupportScroll = null;
            designSupportView = null;
        }

        if (ds != null) {
            designSupport = ds;
            designSupportView = ds.createSupportView();
            designSupportScroll = new JScrollPane(designSupportView);
            designSupportScroll.setBorder(BorderFactory.createEmptyBorder());
            designSupportSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
            designSupportSplitPane.setDividerSize(8);
            designSupportSplitPane.setOneTouchExpandable(true);
            designSupportSplitPane.setBottomComponent(designSupportScroll);

            // do an update to set the initial contents
            designSupport.markChanged();
            designSupport.updateSupportView(designSupportView);

            Container content = getRootPane().getContentPane();
            Component c0 = content.getComponent(0);
            // sheet viewer split pane installed
            if (c0 instanceof JSplitPane) {
                designSupportEditPanel = ((JSplitPane) c0).getLeftComponent();
                designSupportSplitPane.setTopComponent(designSupportEditPanel);
                ((JSplitPane) c0).setLeftComponent(designSupportSplitPane);
            } // sheet viewer split pane not installed
            else {
                designSupportEditPanel = c0;
                content.add(designSupportSplitPane, 0);
                // note that this also implicitly does content.remove(c0);
                designSupportSplitPane.setTopComponent(designSupportEditPanel);
            }
            designSupportEditPanel.setMinimumSize(new Dimension(designSupportEditPanel.getMinimumSize().width, 0));
            designSupportSplitPane.setDividerLocation(designSupportEditPanel.getPreferredSize().height);
        }

    }
    // the installed support
    private DesignSupport designSupport;
    // the split pane that the design support view is embedded in
    private JSplitPane designSupportSplitPane;
    // the scroll pane that the design support view is embedded in
    private JScrollPane designSupportScroll;
    // the current design support view, if any
    private Component designSupportView;
    // the panel used to edit the control; we remember it to make
    // removing the design support easier
    private Component designSupportEditPanel;

    /**
     * Make text components non-opaque for OS X compatibility.
     *
     * @param c the root of the container tree to alter opacity of
     */
    private static void localizeComponentForOSX(JComponent c) {
//	if( c instanceof JTextComponent ) {
//	    c.setOpaque( false );
//	}

        if (OSXSmallFont == null) {
            OSXSmallFont = new Font("Lucida Grande", Font.PLAIN, 11);
        }
        if (c instanceof JComboBox) {
            JComboBox b = (JComboBox) c;
            if (!b.isEditable()) {
                b.setFont(OSXSmallFont);
            }
        } else if (c instanceof JTabbedPane) {
//	    c.putClientProperty( "Quaqua.TabbedPane.shortenTabs", Boolean.FALSE );
            c.setFont(OSXSmallFont);
        } else if (c instanceof JButton) {
            JButton b = (JButton) c;
            if (b.getIcon() != null) {
                b.putClientProperty("Quaqua.Button.style", "bevel");
            }
        }
    }
    private static Font OSXSmallFont;

    private static void localizeComponentGeneric(JComponent c) {
        if (c instanceof JTabbedPane) {

            // *** TODO: workaround until Nimbus handles small Tabs properly
            // StyleUtilities.small( c );
            Font f = c.getFont();
            c.setFont(f.deriveFont(f.getSize2D() - 2f));
//        } else if( c instanceof JSpellingComboBox ) {
//            JSpellingComboBox s = (JSpellingComboBox) c;
//            if( s.isEditable() ) {
//                s.setSize( s.getWidth(), 12 );
//				((JSpellingTextField) s.getEditor()).setBorder(
//					UIManager.getBorder( "ComboBox.border" )
//					);
//            }
        } else if (c instanceof JButton) {
            JButton b = (JButton) c;
            if (b.getIcon() != null && (b.getText() == null || b.getText().isEmpty())) {
                b.setMargin(localizedIconButtonInsets);
            }
        }
    }
    private static final Insets localizedIconButtonInsets = new Insets(0, 0, 0, 0);

    /**
     * Replace the currently edited component. This consists of the following
     * steps:
     * <ol>
     * <li> if {@code newComponent} has an on-install event in its private
     * settings, then the on-install event is called
     * <li> the editor is updated to point to {@code newComponent} and it
     * creates a new set of sheets from {@code newComponent} and installs
     * them in the preview window
     * <li> the current state of {@code newComponent} is copied from the
     * component to the editor controls
     * </ol>
     *
     * @param newComponent the new component to edit with this editor
     */
    public void replaceEditedComponent(G newComponent) {
        String eventScript = newComponent.getSettings().get(ScriptMonkey.ON_INSTALL_EVENT_KEY);
        if (eventScript != null) {
            ScriptMonkey monkey = new ScriptMonkey(ScriptMonkey.ON_INSTALL_EVENT_KEY);
            monkey.bind(PluginContextFactory.createDummyContext());
            monkey.bind(ScriptMonkey.VAR_EDITOR, this);
            monkey.bind(ScriptMonkey.VAR_COMPONENT, newComponent);
            monkey.eval(eventScript);
            monkey.call("onInstall", this, newComponent);
        }

        // set the component and create new sheets
        setGameComponent(newComponent);

        // Finally, update the editor controls to match the new character.
        populateFieldsFromComponent();
    }

    /**
     * After installing the component, make sure you update the component
     * object.
     */
    public void handleOpenRequest(G newComponent, File path) {
        if (newComponent == null) {
            throw new NullPointerException("newComponent");
        }

        // As of SE3, GameComponent.createDefaultEditor is supposed to point
        // the new editor's game component to the component, ideally using
        // a constructor that takes the component so that a dummy component
        // isn't installed first. This check verifies that the component
        // is already set correctly in case there is a nonconformant 2.x-style
        // class kicking about.
        if (getGameComponent() != newComponent) {
            StrangeEons.log.log(Level.WARNING, "createDefaultEditor did not set component: {0}", getGameComponent().getClass());
            replaceEditedComponent(newComponent);
        }

        // remember the name to check for accidentally overwriting a component
        // when spinning off an alternative
        componentNameAtLastSave = newComponent.getFullName();

        setFile(path);
        setUnsavedChanges(false);
    }

    @Override
    public void save() {
        File saveFile = getFile();
        if (saveFile != null) {
            // the save file exists (so it will be overwritten) AND the name
            // of the component changed since it was opened/saved: see if the user
            // wanted to split off a different component
            if (saveFile.exists() && (componentNameAtLastSave != null) && !getGameComponent().getFullName().equals(componentNameAtLastSave)) {
                JButton quicksaveBtn = new JButton(string("quicksave", getDefaultFileName()));
                Object[] options = new Object[]{
                    string("save-as"),
                    quicksaveBtn,
                    string("save-anyway"),
                    string("cancel")
                };

                int[] optionOffsets = new int[]{0, 1, 2, 3};

                final int SAVE_AS = 0;
                final int QUICKSAVE = 1;
                final int SAVE_ANYWAY = 2;
                final int CANCEL = 3;

                File newFile = new File(getFile().getParentFile(), getDefaultFileName());
                if (newFile.equals(getFile())) {
                    quicksaveBtn.setEnabled(false);
                }

                if (!PlatformSupport.isAgnosticOKInFirstPosition()) {
                    Object t1 = options[SAVE_AS];
                    options[SAVE_AS] = options[CANCEL];
                    options[CANCEL] = t1;
                    int t2 = optionOffsets[SAVE_AS];
                    optionOffsets[SAVE_AS] = optionOffsets[CANCEL];
                    optionOffsets[CANCEL] = t2;
                }

                int selection = JOptionPane.showOptionDialog(
                        StrangeEons.getWindow(),
                        new String[]{
                            string("aod-intro"), string("aod-detail1"),
                            string("aod-detail2"), string("aod-detail3"),
                            string("aod-detail4")
                        },
                        string("aod-title"),
                        -1, JOptionPane.WARNING_MESSAGE, null, options, optionOffsets[QUICKSAVE]
                );
                if (selection == optionOffsets[CANCEL]) {
                    return;
                } else if (selection == optionOffsets[SAVE_AS]) {
                    saveAs();
                    return;
                } else if (selection == optionOffsets[QUICKSAVE]) {
                    if (getFile() == null) {
                        saveAs();
                    } else {
                        setFile(newFile);
                        componentNameAtLastSave = getGameComponent().getFullName();
                        save();
                    }
                    return;
                }
            }

            componentNameAtLastSave = getGameComponent().getFullName();
            StrangeEons.setWaitCursor(true);
            try {
                final File file = getFile();
                saveImpl(file);
                RecentFiles.add(file);
            } catch (Exception e) {
                ErrorDialog.displayError(string("ae-err-save"), e);
            } finally {
                StrangeEons.setWaitCursor(false);
            }
        } else {
            saveAs();
        }
    }
    private String componentNameAtLastSave = null;

    @Override
    public void saveAs() {
        File f = getFile();
        String name = f == null ? getDefaultFileName() : f.getAbsolutePath();

        f = ResourceKit.showSaveDialog(this, name);
        if (f != null) {
            setFile(f);
            save();
        }
    }

    @Override
    protected void saveImpl(File f) throws IOException {
        ResourceKit.writeGameComponentToFile(f, getGameComponent());
    }

    /**
     * Returns a default file name for a game component that hasn't been saved.
     *
     * @return a default file name, including extension, based on the name of
     * the installed component
     */
    protected String getDefaultFileName() {
        String name = null;
        G gc = getGameComponent();
        if (gc != null) {
            name = gc.getName();
        }
        if (name == null) {
            name = string("ae-unsaved");
        }
        return ResourceKit.makeStringFileSafe(AbstractGameComponent.filterComponentText(
                name
        )) + ".eon";
    }

    /**
     * Returns an array of labels that can be used to describe the component's
     * sheets. The returned array uses the same order as
     * {@link GameComponent#getSheets()}. The base class implementation is
     * equivalent to calling {@code getGameComponent().getSheetTitles()}.
     *
     * <p>
     * Note that the returned array is shared. If you need to modify its
     * contents, clone the array first and modify the clone.
     *
     * @return a shared array of sheet labels
     */
    public String[] getSheetLabels() {
        if (getGameComponent() == null) {
            throw new AssertionError("called getSheetLabels with null component");
        }
        String[] titles = getGameComponent().getSheetTitles();
        if (titles == null) {
            throw new AssertionError("null sheet titles");
        }
        return titles;
    }

    /**
     * Returns the sheet displayed by the currently selected previewer.
     *
     * @return the selected sheet
     */
    public Sheet getSelectedSheet() {
        if (getGameComponent() == null) {
            return null;
        }
        int sel = sheetPreviewerPane.getSelectedIndex();
        if (sel < 0) {
            return null;
        }
        return sheets[sel];
    }

    /**
     * Returns the index of the sheet displayed by the currently selected
     * previewer.
     *
     * @return the selected sheet index, or -1
     */
    public int getSelectedSheetIndex() {
        if (getGameComponent() == null || getSheetCount() == 0) {
            return -1;
        }
        return sheetPreviewerPane.getSelectedIndex();
    }

    /**
     * Selects the previewer for the sheet with the specified index. The
     * selected previewer will be made visible.
     *
     * @param index the index of the sheet to select
     */
    public void setSelectedSheetIndex(int index) {
        if (getGameComponent() == null) {
            throw new NullPointerException("component is null");
        }
        if (index < 0 || index >= sheets.length) {
            throw new IllegalArgumentException("invalid sheet index: " + index);
        }
        sheetPreviewerPane.setSelectedIndex(index);
    }

    /**
     * Returns {@code true} if the commandable wishes to handle the given
     * command. This method defines the set of commands that the commandable
     * responds to. The commandable might not be able to act on the command at
     * the current moment. For example, a commandable that responds to "Cut"
     * could return true from this method, but false from
     * {@link #isCommandApplicable} if there is currently no selection to cut.
     *
     * <p>
     * The base class for game component editors returns true for the following
     * standard commands: {@link Commands#CLEAR CLEAR}, {@link Commands#EXPORT EXPORT},
     * {@link Commands#PRINT PRINT}, {@link Commands#SAVE SAVE}, {@link Commands#SAVE_AS SAVE_AS},
     * and {@link Commands#SPIN_OFF SPIN_OFF}.
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
                || command == Commands.SAVE_AS
                || command == Commands.SPIN_OFF;
    }

    /**
     * Exports the editor content as a different file format. The procedure for
     * exporting and the exact format(s) available will vary with the editor.
     *
     * <p>
     * This method simply checks that the EXPORT command is supported and
     * applicable, and then calls {@link #exportImpl} to perform the export.
     *
     * @throws UnsupportedOperationException if the EXPORT command is not
     * supported
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
        StrangeEons.setWaitCursor(true);
        try {
            exportImpl();
        } finally {
            StrangeEons.setWaitCursor(false);
        }
    }

    /**
     * The default export implementation for game components. This
     * implementation displays an export dialog that allows selecting an
     * {@link ExportContainer}, image file format, and other options, and then
     * writes the game component's sheet images as image files to the export
     * container.
     *
     * @see ImageExporter
     * @see ExportContainer
     */
    protected void exportImpl() {
        final ImageExporter manager = ImageExporter.getSharedInstance();
        final GameComponent gcToExport = getGameComponent().clone();
        final Sheet[] sheetsToExport = gcToExport.createDefaultSheets();

        boolean allowJoiningImages = sheetsToExport.length >= 2 && sheetsToExport[0] != sheetsToExport[1];

        boolean accepted;
        try {
            accepted = manager.beginExport(
                    gcToExport.getFullName(),
                    gcToExport.getComment(), sheetsToExport[0].getPaintingResolution(),
                    sheetsToExport[0].getPrintDimensions(),
                    allowJoiningImages, sheetsToExport.length > 1,
                    sheetsToExport.length == 3
            );
        } catch (IOException e) {
            ErrorDialog.displayError(string("rk-err-export"), e);
            return;
        }

        if (accepted) {
            new BusyDialog(StrangeEons.getWindow(), string("busy-exporting"), () -> {
                try {
                    for (int i = 0; i < sheetsToExport.length; ++i) {
                        if (i == 0 || sheetsToExport[i] != sheetsToExport[i - 1]) {
                            manager.exportSheet(getSheetLabels()[i], sheetsToExport[i]);
                        }
                    }
                } catch (IOException e) {
                    ErrorDialog.displayError(string("rk-err-export"), e);
                } catch (OutOfMemoryError oom) {
                    ErrorDialog.outOfMemory();
                } finally {
                    try {
                        manager.endExport();
                    } catch (IOException e) {
                        ErrorDialog.displayError(string("rk-err-export"), e);
                    }
                }
            });
        }
    }

    /**
     * Updates the editor with the current contents of the game component.
     * Subclasses should call the super implementation last to ensure that field
     * population events are fired to any listeners.
     */
    public void populateFieldsFromComponent() {
        fireFieldPopulationEvent();
    }

    /**
     * This method is called during heartbeats to allow the editor to update
     * properties of the game component that either cannot be updated in
     * response to events or that are more efficient when updated periodically
     * instead of immediately. If a component has no properties that fit this
     * description, then subclasses can provide an empty implementation.
     */
    protected abstract void populateComponentFromDelayedFields();

    /**
     * This method is called once for each tick of the heartbeat timer. It the
     * game component is currently {@code null}, it returns immediately.
     * Otherwise, it calls {@link #populateComponentFromDelayedFields()} and
     * then determines if it the previews should be updated. The procedure for
     * determining if previews should be updated is as follows:
     * <ol>
     * <li> If the component has changed since the last heartbeat, then there is
     * an update pending and the previews are known to be out of date.
     * <li> If the component has <i>not</i> changed since the last heartbeat,
     * and there is an update pending, then an update is performed as follows:
     * <li> If there is a design support attached to the editor, the design
     * support is asked to update the support view
     * <li> If the component has any sheets, the preview components for all
     * sheets that are out of date are told to update their sheet images. (They
     * do not update right away unless they are currently visible.)
     * </ol>
     *
     * <p>
     * Note that this means that if the user is making a series of rapid
     * changes, as might happen while typing in a text field, then the preview
     * is not actually updated until there is a pause in editing of at least one
     * heartbeat's duration.
     *
     * <p>
     * If the editor is using the default label and tool tip for text for its
     * document tab, these are also updated if they have changed.
     *
     * @see #populateComponentFromDelayedFields()
     * @see
     * #setDesignSupport(ca.cgjennings.apps.arkham.component.design.DesignSupport)
     * @see #initializeSheetViewers(javax.swing.JTabbedPane)
     * @see #createTimer
     */
    @Override
    protected void onHeartbeat() {
        if (getGameComponent() == null) {
            return;
        }

        populateComponentFromDelayedFields();

        boolean changed = getGameComponent().hasChanged();

        updatePending |= changed;
        updatePending |= fireHeartbeatEvent();

        if (!changed && updatePending) {
            updatePending = false;
            StrangeEons.setWaitCursor(true);
            try {
                // UNDO:
//							GameComponent next = (GameComponent) getGameComponent().clone();
//							StrangeEons.getWindow().registerUndoable( AbstractGameComponentEditor.this, previousComponentState, next );
//							previousComponentState = next;

                // update the design support first, in case one of the
                // sheets calls isDesignValid() while painting
                if (designSupport != null) {
                    designSupport.markChanged();
                    designSupport.updateSupportView(designSupportView);
                }

                // update the sheet viewers with new component renders
                // any sheets that have not been marked changed will re-use old image
                if (viewers != null) {
                    for (SheetViewer v : viewers) {
                        v.rerenderImage();
                    }
                }
            } finally {
                StrangeEons.setWaitCursor(false);
            }
        }

        updateTab();
    }
    /**
     * If true, the next timer event that the component hasn't changed, the
     * preview will be redrawn.
     */
    private boolean updatePending = false;

    /**
     * Forces the editor to redraw its component preview. Editors normally
     * manage this redrawing automatically. However, if you make an indirect
     * change to a component, such as changing a setting that affects how the
     * card is drawn, the editor will have no way of knowing about this change
     * and the preview will not reflect the change. Calling this method forces
     * all editors to redraw their previews from scratch.
     *
     * @see StrangeEonsAppWindow#redrawPreviews
     */
    public void redrawPreview() {
        if (sheets != null) {
            for (Sheet s : sheets) {
                s.markChanged();
            }
            for (SheetViewer v : viewers) {
                v.repaint();
            }
            StrangeEons.log.info("forcing redraw of all sheets for " + getGameComponent().getFullName());
        }
    }

    /**
     * @deprecated Replaced by {@link #redrawPreview}.
     */
    @Deprecated
    public void forceRerender() {
        redrawPreview();
    }

    private G gc;

    protected Sheet[] sheets;
    protected SheetViewer[] viewers;

    /**
     * Initializes the sheet viewers for an editor. Editors for components that
     * use sheets must call this method with the tabbed pane that will be used
     * to display previewers for the sheets. A default set of sheets will be
     * created for the component, and then the container's children will be
     * replaced by previewers for those sheets. The supplied container will be
     * remembered so that methods such as {@link #getSelectedSheetIndex()} work
     * correctly.
     *
     * @param container the control that will house previewers for the sheets
     */
    protected void initializeSheetViewers(JTabbedPane container) {
        sheets = getGameComponent().createDefaultSheets();
        viewers = new SheetViewer[sheets.length];
        container.removeAll();
        String[] labels = getSheetLabels();
        for (int i = 0; i < sheets.length; ++i) {
            viewers[i] = new SheetViewer();
            viewers[i].setSheet(sheets[i]);
            if (PlatformSupport.PLATFORM_IS_MAC || i >= 9) {
                // OS X doesn't support mnemonics on tabs
                container.add(labels[i], viewers[i]);
            } else {
                String label = i < labels.length ? labels[i] : "???";
                container.add(label, viewers[i]);
                container.setMnemonicAt(i, java.awt.event.KeyEvent.VK_1 + i);
            }
        }
        this.sheetPreviewerPane = container;
        pack();
    }
    private JTabbedPane sheetPreviewerPane;

    /**
     * Refreshes the sheets being displayed by the sheet viewers within the
     * container most recently passed to {@link #initializeSheetViewers}. This
     * can be called if the sheets set on the edited component are changed.
     * Depending on how the sheet configuration changes, the existing sheet
     * viewers may be updated to display the new sheets, or they may be replaced
     * altogether as if by calling
     * {@link #initializeSheetViewers(javax.swing.JTabbedPane)}.
     */
    protected void updateSheetViewers() {
        if (sheetPreviewerPane == null || gc == null) {
            return;
        }
        sheets = getGameComponent().createDefaultSheets();
        if (sheets.length != sheetPreviewerPane.getTabCount()) {
            initializeSheetViewers(sheetPreviewerPane);
        } else {
            String[] labels = getSheetLabels();
            for (int i = 0; i < sheetPreviewerPane.getTabCount(); ++i) {
                ((SheetViewer) sheetPreviewerPane.getComponentAt(i)).setSheet(sheets[i]);
                sheetPreviewerPane.setTitleAt(i, labels[i]);
            }
        }
    }

    /**
     * Returns the number of {@link Sheet}s being previewed by this editor (may
     * be 0).
     *
     * @return the number of sheet viewer tabs
     */
    public int getSheetCount() {
        return sheets == null ? 0 : sheets.length;
    }

    /**
     * Returns the sheet being displayed by the sheet viewer with the specified
     * index. Note that since the sheet is being displayed, you should not
     * request sheet images from the sheet yourself. If you wish to create an
     * image of one or more sheets, the safest procedure is to clone the game
     * component, request that it create a set of default sheets, and then use
     * those to draw sheet images as desired.
     *
     * @param index the index of the desired sheet
     * @return the sheet at the requested index
     * @throws IndexOutOfBoundsException if the index does not fall in
     * {@code 0 ... {@link #getSheetCount()}-1}, inclusive
     */
    public Sheet getSheet(int index) {
        if (index < 0 || index >= getSheetCount()) {
            throw new IndexOutOfBoundsException("index: " + index);
        }
        return sheets[index];
    }

    /**
     * Adds a new {@code FieldPopulationListener} to this game component
     * editor. A field population event is fired after the editor's controls are
     * modified to match the edited component. This typically happens as part of
     * setting the game component for the editor.
     *
     * @param fpl the listener to add
     * @since 2.00 (final)
     * @see #removeFieldPopulationListener
     */
    public void addFieldPopulationListener(FieldPopulationListener fpl) {
        listenerList.add(FieldPopulationListener.class, fpl);
    }

    /**
     * Removes a {@code FieldPopulationListener} from this game component
     * editor.
     *
     * @param fpl the listener to remove
     * @since 2.00 (final)
     * @see #addFieldPopulationListener
     */
    public void removeFieldPopulationListener(FieldPopulationListener fpl) {
        listenerList.remove(FieldPopulationListener.class, fpl);
    }

    /**
     * Called to fire a field population event when the editor's controls are
     * filled in to match the edited game component.
     *
     * @see #addFieldPopulationListener
     * @see #populateFieldsFromComponent()
     */
    protected final void fireFieldPopulationEvent() {
        Object[] li = listenerList.getListenerList();
        for (int i = 0; i < li.length; i += 2) {
            if (li[i] == FieldPopulationListener.class) {
                ((FieldPopulationListener) li[i + 1]).fieldsPopulated(this);
            }
        }
    }

//    /** Return a name for this window. */
//    public String getDisplayName( int unsavedChangeStarMode ) {
//		String name = getGameComponent().getFullName();
//		if( name == null ) name = " ";
//
//		if( getFile() == null ) {
//			name = name + " (" + getFile().getName() + ')';
//		} else {
//			name = name + " (" + string( "ae-unsaved" ) + ')';
//		}
//        return AbstractGameComponent.filterComponentText( name );
//    }
    // UNDO:
//	private GameComponent previousComponentState = null;
    @Override
    public void dispose() {
        JDesktopPane desktop = getDesktopPane();
        if (desktop != null) {
            desktop.remove(this);
        }
        AppFrame.getApp().removePropertyChangeListener(StrangeEonsAppWindow.VIEW_QUALITY_PROPERTY, pcl);

        // there seems to be a JInternalFrame memory leak happening in some cases
        // this should reduce the damage
        sheets = null;
        viewers = null;
        setFile(null);
        removeAll();
        setGameComponent(null);

        super.dispose();
    }

    /**
     * Iterates over the items in a combo box, replacing any whose
     * {@code toString()} value begins with the prefix string with a localized
     * string. The value of the string is determined by using the existing
     * item's {@code toString()} value as a key. This makes it easy to create
     * localized combo boxes with fixed entries in the form editor. If prefix is
     * {@code null}, all items in the list will be localized.
     */
    public static void localizeComboBoxLabels(JComboBox box, String prefix) {
        localizeComboBoxLabels(null, box, prefix);
    }

    public static void localizeComboBoxLabels(Language lang, JComboBox box, String prefix) {
        if (lang == null) {
            lang = Language.getInterface();
        }
        int sel = box.getSelectedIndex();
        int len = box.getItemCount();
        for (int i = 0; i < len; ++i) {
            Object item = box.getItemAt(0);
            String itemString = item.toString();
            if (prefix == null || itemString.startsWith(prefix)) {
                item = lang.get(itemString);
            }
            box.addItem(item);
            box.removeItemAt(0);
        }
        box.setSelectedIndex(sel);
        Dimension d = box.getPreferredSize();
        d.width += 18;
        box.setPreferredSize(d);
    }

    /**
     * Returns {@code true} if the edited game component has unsaved
     * changes.
     *
     * @return {@code true} if the game component has been edited since the
     * last time it was saved
     */
    @Override
    public boolean hasUnsavedChanges() {
        GameComponent gc = getGameComponent();
        return gc != null && gc.hasUnsavedChanges();
    }

    /**
     * Marks whether the game component has unsaved changes. This flag is
     * cleared whenever the file is saved successfully.
     *
     * <p>
     * <b>Note:</b> The game component is updated to reflect this change so that
     * it stays in synch with the editor. (The values of
     * {@link #hasUnsavedChanges()} and the game component's
     * {@link GameComponent#hasUnsavedChanges()} will match.)
     *
     * @param isDirty {@code true} if this game component has unsaved
     * changes
     */
    @Override
    public void setUnsavedChanges(boolean isDirty) {
        GameComponent gc = getGameComponent();
        if (gc != null) {
            if (isDirty) {
                gc.markUnsavedChanges();
            } else {
                gc.markSaved();
            }
        } else {
            NullPointerException npe = new NullPointerException();
            npe.fillInStackTrace();
            StrangeEons.log.log(Level.WARNING, "null gc", npe);
        }
    }

    /**
     * Implementation of the print command for standard game components. Lays
     * out the sheets to be printed on one or more pages and then prints the
     * layout.
     *
     * @param job a printer job; this value is ignored as this implementation
     * will create its own printer job
     * @throws PrintException if an error occurs during printing
     * @throws PrinterException if a printer error occurs during printing
     */
    @Override
    protected void printImpl(PrinterJob job) throws PrintException, PrinterException {
        Sheet[] printSheets = getGameComponent().clone().createDefaultSheets();
        PrintSetupDialog printDialog = new PrintSetupDialog(this);
        printDialog.setUpForSheets(printSheets);
        if (!printDialog.showDialog()) {
            return;
        }

        final BitSet facesToPrint = printDialog.getSelectedFaces();
        boolean oneFacePerPage = printDialog.isDoubleSided();

        DeckEditor layout = new DeckEditor();
        layout.getDeck().getSettings().set(Game.GAME_SETTING_KEY, getGameComponent().getSettings().get(Game.GAME_SETTING_KEY));
        layout.setPaperProperties(printDialog.getPrinterPaper());
        createCompactPrintLayout(layout, facesToPrint, sheets, oneFacePerPage);

        final File pdfFile = printDialog.getPDFDestinationFile();
        if (pdfFile == null) {
            layout.print(true);
        } else {
            PDFPrintSupport.printToPDF(layout, printDialog.getPDFDestinationFile());
        }
    }

    private void createCompactPrintLayout(final DeckEditor destination, final BitSet faces, final Sheet[] sheets, final boolean doubleSided) {
        DeckPacker p = new DeckPacker() {
            {
                final GameComponent gc = getGameComponent();
                final File dummyFile = getFile() == null ? new File("dummy") : getFile();
                final String klass = gc.getClass().getName();

                for (int i = 0; i < sheets.length; ++i) {
                    int index = i;
                    // figure out which sheet(s) to print as if we are printing everything;
                    // then modify this depending on which sheets are selected
                    CardFace front, back = null;
                    front = faces.get(i) ? new CardFace(gc.getFullName(), sheets[i], i) : null;

                    MarkerStyle style = sheets[i].getMarkerStyle();
                    if (style == MarkerStyle.COPIED || style == MarkerStyle.MIRRORED) {
                        // tokens are a single unit, so either both sides appear or neither
                        if (front != null) {
                            back = new CardFace(gc.getFullName(), sheets[i], i);
                            if (style == MarkerStyle.MIRRORED) {
                                back.flip();
                            }
                        }
                    } else if (style == null && i < sheets.length - 1) {
                        ++i;
                        if(faces.get(i)) {
                            back = new CardFace(gc.getFullName(), sheets[i], i);
                        }
                    }

                    if (front == null) {
                        if (back == null) {
                            continue;
                        } else {
                            front = back;
                            back = null;
                        }
                    }

                    addCard(new Card(dummyFile, index, klass, front, back));
                }

                Deck d = destination.getDeck();
                setPaper(d.getPaperProperties());
                setLayoutDoubleSided(doubleSided);
                setLayoutQuality(7);
                createLayout(destination);
            }
        };
    }

    /**
     * Returns the edited game component. If this editor is not editing a game
     * component, returns {@code null}.
     *
     * @return the edited game component, or {@code null} if this editor is
     * not editing a game component
     */
    @Override
    public G getGameComponent() {
        return gc;
    }

    /**
     * Sets the edited game component. If the game component is not appropriate
     * for the editor, the result is undefined.
     *
     * @param component the new game component
     */
    public void setGameComponent(G component) {
        if (component != null) {
            componentNameAtLastSave = component.getFullName();
        }
        gc = component;
        updateSheetViewers();
    }

    //
    // StrangeEonsEditor wrapper methods.
    //
    /**
     * Clears the component by calling its {@link GameComponent#clearAll()}
     * method, then repopulating the editor controls.
     *
     * @see #populateFieldsFromComponent()
     */
    @Override
    protected void clearImpl() {
        getGameComponent().clearAll();
        populateFieldsFromComponent();
    }

    @Override
    public StrangeEonsEditor spinOff() {
        if (!canPerformCommand(Commands.SPIN_OFF)) {
            throw new UnsupportedOperationException();
        }
        if (!isCommandApplicable(Commands.SPIN_OFF)) {
            return null;
        }
        StrangeEonsEditor ed = spinOffImpl();
        StrangeEons.getWindow().addEditor(ed);
        return ed;
    }

    @Override
    protected StrangeEonsEditor spinOffImpl() {
        GameComponent c = getGameComponent().clone();
        AbstractGameComponentEditor editor = c.createDefaultEditor();
        editor.setFrameIcon(getFrameIcon());
        // for backwards compatibility in case this wasn't done in createDefaultEditor
        if (editor.getGameComponent() != c) {
            StrangeEons.log.warning("createDefaultEditor did not set component: " + c.getClass());
            editor.replaceEditedComponent(c);
        }
        editor.componentNameAtLastSave = null;
        return editor;
    }

    @Override
    public String getFileNameExtension() {
        return "eon";
    }

    @Override
    public String getFileTypeDescription() {
        return string("rk-filter-eon");
    }

    @Override
    public boolean isCommandApplicable(AbstractCommand command) {
        if(command == Commands.VIEW_INK_SAVER) {
            return true;
        }
        return super.isCommandApplicable(command);
    }

    @Override
    public void performCommand(AbstractCommand command) {
        if(command == Commands.VIEW_INK_SAVER) {
            GameComponent gc = getGameComponent();
            if(gc != null) {
                Sheet[] sheets = gc.getSheets();
                if(sheets != null) {
                    boolean enable = Commands.VIEW_INK_SAVER.isSelected();
                    for(int i=0; i<sheets.length; ++i) {
                        sheets[i].setPrototypeRenderingModeEnabled(enable);
                    }
                    redrawPreview();
                }
            }
            return;
        }
        super.performCommand(command);
    }





    
    
    
    
    
    private static final Set<? extends AWTKeyStroke> TEXT_FIELD_FORWARD_TRAVERSAL_KEYS = Collections.singleton(KeyStroke.getAWTKeyStroke("ctrl TAB"));
    
    private static final Action EXPAND_ABBRV_ACTION = new AbstractAction("EXPAND_ABBRV") {
        @Override
        public void actionPerformed(ActionEvent e) {
            final Object src = e.getSource();
            if (!(src instanceof JTextComponent)) {
                return;
            }

            // We know it is a text component, so now we must either insert
            // a Tab or a replacement string.
            final JTextComponent tc = (JTextComponent) src;
            final StrangeEonsEditor ed = findEditor(tc);
            if (ed != null) {
                final GameComponent gc = ed.getGameComponent();
                if (gc != null) {
                    AbbreviationTable at = AbbreviationTableManager.getTable(
                            gc.getSettings().get(Game.GAME_SETTING_KEY, Game.ALL_GAMES_CODE)
                    );

                    if (at.expandAbbreviation(tc, tc instanceof JTextField ? '\0' : '\t')) {
                        // An abbreviation was expanded, so we return immediately
                        // rather than insert a Tab.
                        return;
                    }
                }
            }
            // No abbreviation could be applied for whatever reason, so the
            // fallback would be to insert a Tab. But the Tab is actually
            // already inserted by the keypress (it gets inserted before our
            // action takes effect).
//	    tc.replaceSelection( "\t" );
        }

        private StrangeEonsEditor findEditor(Component c) {
            Container parent = c.getParent();
            while (parent != null && !(parent instanceof StrangeEonsEditor)) {
                parent = parent.getParent();
            }
            return (StrangeEonsEditor) parent;
        }
    };    

    static {
        StrangeEons.getApplication().addPropertyChangeListener(StrangeEons.MARKUP_TARGET_PROPERTY, (PropertyChangeEvent evt) -> {
            // Figure out if the new markup target is a text field
            // in a game component editor...
            final Object newTarget = evt.getNewValue();
            if (!(newTarget instanceof JTextComponent)) {
                return;
            }
            // And if it is a text field, check if it is in a game
            // component editor...
            final JTextComponent newTextField = (JTextComponent) newTarget;
            Container parent1 = newTextField.getParent();
            while (parent1 != null && !(parent1 instanceof AbstractGameComponentEditor) && !(parent1 instanceof TextBoxEditor)) {
                parent1 = parent1.getParent();
            }
            if (parent1 == null) {
                return;
            }
            // And if it is also in a game component editor, check if it
            // has an action assigned to the Tab key...
            final KeyStroke TAB = KeyStroke.getKeyStroke('\t');
            final ActionListener existingAction = newTextField.getActionForKeyStroke(TAB);
            // If it does, but it is our listener, everything is set
            // up and we can leave
            if (existingAction == EXPAND_ABBRV_ACTION) {
                return;
            }
            // Otherwise, we need to install our action...
            newTextField.getKeymap().addActionForKeyStroke(TAB, EXPAND_ABBRV_ACTION);
            // ... and for plain text fields, move the focus change key from Tab to Ctrl+Tab
            if (newTextField instanceof JTextField) {
                newTextField.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, TEXT_FIELD_FORWARD_TRAVERSAL_KEYS);
//							newTextField.getKeymap().addActionForKeyStroke( KeyStroke.getKeyStroke( "ctrl TAB" ), FOCUS_NEXT_ACTION );
            }
        });
    }
}
