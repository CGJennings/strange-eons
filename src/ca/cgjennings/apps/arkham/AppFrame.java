package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.StrangeEonsEditor.EditorListener;
import ca.cgjennings.apps.arkham.commands.AbstractCommand;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.apps.arkham.component.*;
import ca.cgjennings.apps.arkham.deck.PropertyPalette;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.dialog.Messenger;
import ca.cgjennings.apps.arkham.dialog.prefs.PreferenceCategory;
import ca.cgjennings.apps.arkham.dialog.prefs.Preferences;
import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import ca.cgjennings.apps.arkham.plugins.PluginBundle;
import ca.cgjennings.apps.arkham.plugins.ScriptConsole;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.apps.arkham.plugins.catalog.AutomaticUpdater;
import ca.cgjennings.apps.arkham.plugins.catalog.Catalog;
import ca.cgjennings.apps.arkham.plugins.catalog.CatalogDialog;
import ca.cgjennings.apps.arkham.plugins.catalog.Listing;
import ca.cgjennings.apps.arkham.project.*;
import ca.cgjennings.platform.PlatformSupport;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.RhinoException;
import ca.cgjennings.ui.JUtilities;
import ca.cgjennings.ui.anim.Animation;
import ca.cgjennings.ui.dnd.FileDrop;
import ca.cgjennings.ui.theme.Theme;
import gamedata.Game;
import java.awt.*;
import java.awt.desktop.QuitStrategy;
import java.awt.event.*;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import javax.script.ScriptException;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;
import resources.CoreComponents;
import resources.Language;
import static resources.Language.string;
import resources.RawSettings;
import resources.ResourceKit;
import resources.Settings;

/**
 * The actual main application window for Strange Eons, i.e., the concrete
 * {@link StrangeEonsAppWindow} implementation.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
final class AppFrame extends StrangeEonsAppWindow {

    /**
     * Creates new form AppFrame
     */
    public AppFrame() {
        JUtilities.threadAssert();
        if (appFrame != null) {
            throw new AssertionError("tried to create second app frame");
        }
        appFrame = this;
        installExceptionHandler();
        initComponents();
        localizeTitle();

        if (PlatformSupport.PLATFORM_IS_MAC) {
            installMacOsDesktopHandlers();
        }

        setIconImages(getApplicationFrameIcons());

        for (int i = 0; i < menuBar.getMenuCount(); ++i) {
            PolledMenuItem.installMenuHandler(menuBar.getMenu(i));
        }

        installMenuHandlers();

        // listen for and open dropped files
        new FileDrop(getRootPane(), null, (File[] files) -> {
            for (File f : files) {
                addFileToOpenQueue(f);
            }
            openFilesInQueue();
        });

        /**
         * Intercepts keystrokes for command accelerators and makes sure that
         * the command is updated before the accelerator can activate.
         * Otherwise, a disabled menu item will prevent the command from working
         * even if the command should now be enabled.
         */
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                // only check unconsumed events where a modifier and non-modifier
                // are down, e.g. Ctrl + S
                if ((!e.isConsumed()) && (e.getKeyCode() != KeyEvent.VK_UNDEFINED) && ((e.getModifiersEx() & (KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK | KeyEvent.META_DOWN_MASK)) != 0)) {
                    final KeyStroke ks = KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiersEx());
                    final JMenuItem match = findItem(getJMenuBar(), ks);
                    if (match != null) {
                        if (match.getAction() instanceof AbstractCommand) {
                            AbstractCommand command = (AbstractCommand) match.getAction();
                            command.update();
                        }
                    }
                }
                return false;
            }

            private JMenuItem findItem(JMenuBar bar, KeyStroke accel) {
                if (accel == null) {
                    StrangeEons.log.log(Level.SEVERE, "null accelerator");
                    return null;
                }
                final int len = bar.getMenuCount();
                for (int i = 0; i < len; ++i) {
                    final JMenuItem match = findItem(bar.getMenu(i), accel);
                    if (match != null) {
                        return match;
                    }
                }
                return null;
            }

            private JMenuItem findItem(JMenu menu, KeyStroke accel) {
                if (menu == null) {
                    return null;
                }
                final int len = menu.getMenuComponentCount();
                for (int i = 0; i < len; ++i) {
                    Component c = menu.getMenuComponent(i);
                    if (c instanceof JMenu) {
                        final JMenuItem child = findItem((JMenu) c, accel);
                        if (child != null) {
                            return child;
                        }
                    } else if (c instanceof JMenuItem) {
                        final JMenuItem child = (JMenuItem) c;
                        if (accel.equals(child.getAccelerator())) {
                            return child;
                        }
                    }
                }
                return null;
            }
        });

        // displayApplication() will be called right at the end
        // of the initialization process; it is important that it not be called
        // until after the game database is locked because it may attempt to
        // create an editor or open a file. It is normally called from StrangeEons
        // during an invokeLater that makes the app visible.
    }

    private void initWindowSize() {
        JUtilities.threadAssert();

        if (Settings.getShared().applyWindowSettings("appframe", this)) {
            return;
        }

        setExtendedState(MAXIMIZED_BOTH);
    }

    void displayApplication() {
        JUtilities.threadAssert();

        final StrangeEons app = StrangeEons.getApplication();
        final CommandLineArguments args = app.commandLineArguments;

        /////////////////////////////////////////////////////////
        // FINISH ANY SLOW TASKS HERE BEFORE DISPLAYING WINDOW ////////////////
        /////////////////////////////////////////////////////////
        startTracking(new TrackedWindowProxy(Language.string("de-opal-title"), ResourceKit.getIcon("application/deck-palette.png")) {
            @Override
            public Object createWindow() {
                return PropertyPalette.getShared();
            }
        });

        // might be used by some plug-ins, and certainly used to create proj. view
        newEditorDialog = new NewEditorDialog(false);

        app.setStartupActivityMessage(string("init-plugins"));
        app.loadPlugins();
        toolboxMenu.setEnabled(true);

        // open last project
        String recentProject = Settings.getUser().get(LAST_PROJECT_KEY);
        if (!args.xDisableProjectRestore && recentProject != null && !BundleInstaller.hasTestBundles()) {
            try {
                app.setStartupActivityMessage(string("init-last-proj"));
                File projectFile = new File(recentProject);
                if (Project.isProjectFolder(projectFile) || Project.isProjectPackage(projectFile)) {
                    openProject(projectFile, !StrangeEons.isNonInteractive());
                }
            } catch (IOException e) {
            }
        }

        app.setStartupActivityMessage(string("init-startup"));

        ///////////////////////////////////////////////////////////////////////
        // show wait cursor until all done (cleared from delayed action listener)
        setWaitCursor();
        initWindowSize();
//		validate();

        TDetachedEditor.installCatalogSearchHandler(this);

        if (!StrangeEons.isNonInteractive()) {
            setVisible(true);
        }

        boolean enable = Settings.getShared().getYesNo("show-context-bar");
        if (enable) {
            ContextBar.getShared().setEnabled(true);
        }

        // restore previously open files
        if (args.plugintest == null && !args.xDisableFileRestore) {
            String tabList = Settings.getUser().get("tab-list");
            if (tabList != null && !tabList.isEmpty()) {
                for (String tab : tabList.split("\0")) {
                    if (tab.length() > 2 && tab.charAt(1) == ' ') {
                        fileOpenQueue.add(tab);
                    }
                }
            }
        }

        // initialize layout of any opened editors
        // OK to use desktopPane since none can be detached yet
        JInternalFrame[] frames = desktopPane.getAllFrames();
        for (int i = 0; i < frames.length; ++i) {
            try {
                frames[i].setMaximum(true);
            } catch (PropertyVetoException e) {
            }
            frames[i].setVisible(true);
        }

        // we need the app to be displayed before loading the plugins or
        // else we get weird effects on some platforms as they may write
        // to the console or create other windows with this as the parent;
        // however, we want the app to have a chance to paint everything first
        // and basically get things in order because the plug-in load may take
        // a while and we don't want a big blank window while it runs
        final Timer delayedStartupActions = new Timer(1_000, (ActionEvent ae) -> {
            try {
                toFront();
                checkIfInstallationWasUpdated();
                // load initial editors from command line
                openFilesInQueue();
                // OK to use desktopPane since nothing detached yet
                if (getOpenProject() == null && !StrangeEons.isNonInteractive() && desktopPane.getAllFrames().length == 0) {
                    SwingUtilities.invokeLater(() -> {
                        Commands.NEW_GAME_COMPONENT.actionPerformed(null);
                    });
                }
                // ask for the console window to force its creation (but not its display)
                // this keeps the Window menu from possibly being empty
                ScriptConsole con = ScriptMonkey.getSharedConsole();
                if (con != null && !con.isVisible()) {
                    // the dispose() is due to another 100% CPU issue
                    con.dispose();
                }
                // start posting any queued messages
                Messenger.setQueueProcessingEnabled(true);

                // run script runner mode script, if any
                if (StrangeEons.getScriptRunner() != null) {
                    ((ScriptRunnerModeHelper) StrangeEons.getScriptRunner()).run();
                }
            } finally {
                setDefaultCursor();
            }

            app.runStartupTasks();
        });
        delayedStartupActions.setRepeats(false);
        delayedStartupActions.start();

        AutomaticUpdater.startAutomaticUpdateTimer();
    }

    /**
     * Checks if this appears to be a new or updated install and guides the user
     * through updating/installing plug-ins. If it is a new install, offer to
     * install cores and games. If it is an update, offers to check for updates.
     * As a special case, if any plug-ins failed to start, offer to check for
     * new versions. This message takes priority over other messages. This check
     * is skipped if the build is not an "official release" with a build number.
     */
    private void checkIfInstallationWasUpdated() {
        final int thisBuild = StrangeEons.getBuildNumber();
        if (thisBuild == StrangeEons.INTERNAL_BUILD_NUMBER) {
            return;
        }

        Settings s = Settings.getUser();

        String message = null;
        String catalogFilter = null;
        String popup = null;
        int lastBuild = Settings.integer(s.get("last-run-build-number", "0"));

        // check for failed plug-ins
        if (!BundleInstaller.getFailedUUIDs().isEmpty()) {
            message = "cat-new-version-fail";
            catalogFilter = "!state=" + Catalog.VersioningState.UP_TO_DATE.name();
        } // otherwise, check if version has changed
        else if (thisBuild != lastBuild) {
            // new installation
            if (lastBuild < 3_000 && Game.getGames(true).length < 2) {
                message = "cat-new-version-check-fresh";
                catalogFilter = "tags=game";
                popup = "cat-new-version-popup-fresh";
            } // upgrade
            else {
                message = "cat-new-version-check";
                catalogFilter = null;
            }
        }

        if (message != null) {
            s.setInt("last-run-build-number", thisBuild);
            message = string(message);
            int choice = JOptionPane.showConfirmDialog(
                    AppFrame.this, message, string("cat-new-version-title"), JOptionPane.YES_NO_OPTION, 0,
                    ResourceKit.getIcon("application/app.png").derive(128, 128)
            );
            if (choice == JOptionPane.YES_OPTION) {
                // load a fresh (non-cached) copy of the default catalog
                CatalogDialog d = new CatalogDialog(AppFrame.this, null, false);
                if (catalogFilter != null) {
                    d.setListingFilter(catalogFilter);
                }
                if (popup != null) {
                    d.setPopupText(string(popup));
                }
                d.setVisible(true);
            }
        }
    }

    private EventListenerList listeners = new EventListenerList();

    @Override
    public void addEditorAddedListener(EditorAddedListener eal) {
        listeners.add(EditorAddedListener.class, eal);
    }

    @Override
    public void removeEditorAddedListener(EditorAddedListener eal) {
        listeners.remove(EditorAddedListener.class, eal);
    }

    private void fireEditorAdded(StrangeEonsEditor editor) {
        Object[] list = listeners.getListenerList();
        for (int i = 0; i < list.length; i += 2) {
            if (list[i] == EditorAddedListener.class) {
                ((EditorAddedListener) list[i + 1]).editorAdded(editor);
            }
        }
    }

    @Override
    public void addEditorListener(EditorListener eal) {
        listeners.add(EditorListener.class, eal);
    }

    @Override
    public void removeEditorListener(EditorListener eal) {
        listeners.remove(EditorListener.class, eal);
    }

    void fireEditorSelected(StrangeEonsEditor editor) {
        Object[] list = listeners.getListenerList();
        for (int i = 0; i < list.length; i += 2) {
            if (list[i] == EditorListener.class) {
                ((EditorListener) list[i + 1]).editorSelected(editor);
            }
        }
    }

    void fireEditorDeselected(StrangeEonsEditor editor) {
        Object[] list = listeners.getListenerList();
        for (int i = 0; i < list.length; i += 2) {
            if (list[i] == EditorListener.class) {
                ((EditorListener) list[i + 1]).editorDeselected(editor);
            }
        }
    }

    void fireEditorClosing(StrangeEonsEditor editor) {
        Object[] list = listeners.getListenerList();
        for (int i = 0; i < list.length; i += 2) {
            if (list[i] == EditorListener.class) {
                ((EditorListener) list[i + 1]).editorClosing(editor);
            }
        }
    }

    void fireEditorDetached(StrangeEonsEditor editor) {
        Object[] list = listeners.getListenerList();
        for (int i = 0; i < list.length; i += 2) {
            if (list[i] == EditorListener.class) {
                ((EditorListener) list[i + 1]).editorDetached(editor);
            }
        }
    }

    void fireEditorAttached(StrangeEonsEditor editor) {
        Object[] list = listeners.getListenerList();
        for (int i = 0; i < list.length; i += 2) {
            if (list[i] == EditorListener.class) {
                ((EditorListener) list[i + 1]).editorAttached(editor);
            }
        }
    }

    @Override
    public boolean canPerformCommand(AbstractCommand command) {
        return false;
    }

    @Override
    public boolean isCommandApplicable(AbstractCommand command) {
        return false;
    }

    @Override
    public void performCommand(AbstractCommand command) {
        // perform "secret" commands
        // these commands are performed by the app frame because it has
        // the priviledges to do so, but it does not officially support the command
        // (canPerformCommand returns false)
        if (command == Commands.NEW_PROJECT) {
            NewProjectDialog npd = new NewProjectDialog(this);
            Project p = npd.showDialog();
            if (p != null) {
                File f = p.getPackageFile();
                if (f == null) {
                    f = p.getFile();
                }
                RecentFiles.addRecentProject(f);
                createProjectView(p, true);
                project = p;
            }
            return;
        }
        if (command == Commands.OPEN) {
            setWaitCursor();
            try {
                File[] files = ResourceKit.showMultiOpenDialog(this);
                if (files != null) {
                    for (File f : files) {
                        addFileToOpenQueue(f);
                    }
                    openFilesInQueue();
                }
            } catch (OutOfMemoryError oom) {
                ErrorDialog.outOfMemory();
            } finally {
                setDefaultCursor();
            }
            return;
        }

        if (!isCommandApplicable(command)) {
            if (!canPerformCommand(command)) {
                StrangeEons.log.log(Level.WARNING, "not a supported command: {0}", command);
            }
            return;
        }

        // perform officially supported commands here...
    }

    private void localizeTitle() {
        String title;
        if (BundleInstaller.hasTestBundles()) {
            title = "Strange Eons [PLUG-IN TEST: ";
            File[] bf = BundleInstaller.getTestBundles();
            for (int i = 0, len = bf.length; i < len; ++i) {
                if (i > 0) {
                    title += ", ";
                }
                title += bf[i].getName();
            }
            title += ']';
        } else {
            title = "Strange Eons " + StrangeEons.getVersionString();
        }
        setTitle(title);
    }

    private void installMenuHandlers() {
        installMarkupMenuHandler();

        final JMenuBar bar = getJMenuBar();
        final MenuListener updater = new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                JMenu menu = (JMenu) e.getSource();
                updateSubmenu(menu);
            }

            private boolean updateSubmenu(JMenu menu) {
                boolean enableThisMenu = false;

                for (int i = 0; i < menu.getMenuComponentCount(); ++i) {
                    Component c = menu.getMenuComponent(i);

                    if (c instanceof JMenu) {
                        JMenu submenu = (JMenu) c;
                        boolean enable = updateSubmenu(submenu);
                        submenu.setEnabled(enable);
                        enableThisMenu |= enable;
                    } else if (c instanceof JMenuItem) {
                        JMenuItem item = (JMenuItem) c;
                        if (item.getAction() instanceof AbstractCommand) {
                            ((AbstractCommand) item.getAction()).update();
                        }
                        enableThisMenu |= item.isEnabled();
                    }
                }

                return enableThisMenu;
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        };
        for (int m = 0; m < bar.getMenuCount(); ++m) {
            JMenu menu = bar.getMenu(m);
            menu.addMenuListener(updater);
        }
    }

    private void installMarkupMenuHandler() {
        markupMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuCanceled(MenuEvent e) {
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuSelected(MenuEvent e) {
                boolean enable = StrangeEons.getApplication().getMarkupTarget() != null;
                for (int i = 0; i < markupMenu.getMenuComponentCount(); ++i) {
                    Component c = markupMenu.getMenuComponent(i);
                    if (!((c instanceof JSeparator) || (c == markupAbbreviationsItem))) {
                        c.setEnabled(enable);
                    }
                }
            }
        });
    }

    /**
     * Add a custom component to the custom component bar at the top of the
     * application window. This can be used to provide custom controls for a
     * plug-in. Custom components are added in a horizontal strip above the pane
     * where editor windows are displayed.
     *
     * @param comp the component to add
     * @throws NullPointerException if {@code comp} is {@code null}
     * @since 2.00a13
     */
    @Override
    public void addCustomComponent(final Component comp) {
        if (comp == null) {
            throw new NullPointerException("component is null");
        }
        addCustomComponent(comp, -1);
    }

    /**
     * Add a custom component to the top of the application window. The
     * component is added at a specific position with respect to the existing
     * components.
     *
     * @param comp the component to be added
     * @param index the position at which to insert the component, or -1 to
     * append the component to the end
     * @throws NullPointerException if {@code comp} is {@code null}
     * @since 2.00a13
     */
    @Override
    public void addCustomComponent(final Component comp, final int index) {
        if (comp == null) {
            throw new NullPointerException("component is null");
        }
        if (index < -1) {
            throw new IllegalArgumentException("invalid component index: " + index);
        }
        if (topToolBar == null) {
            topToolBar = createToolBar();
        }
//		if( comp instanceof JComponent ) {
//			JComponent jComp = (JComponent) comp;
//			Border spacer = BorderFactory.createEmptyBorder( 0, 0, 0, 4 );
//			if( jComp.getBorder() != null ) {
//				jComp.setBorder( BorderFactory.createCompoundBorder( spacer, jComp.getBorder() ) );
//			} else {
//				jComp.setBorder( spacer );
//			}
//		}
        topToolBar.add(comp, index);
        topToolBar.validate();
    }

    /**
     * Add a separator to the custom component bar at the top of the application
     * window. A reference to the added separator is returned. This may be used
     * to remove the separator later.
     *
     * @return the separator component that was added
     */
    @Override
    public Component addCustomComponentSeparator() {
        if (topToolBar == null) {
            topToolBar = createToolBar();
        }
        JComponent sep = new JToolBar.Separator();
        topToolBar.add(sep);
        topToolBar.validate();
        return sep;
    }

    /**
     * Removes a previously added custom component from the top of the
     * application window component bar.
     *
     * @param comp the component to remove
     * @throws NullPointerException if {@code comp} is {@code null}
     * @throws IllegalArgumentException if {@code comp} has not been added as a
     * custom component
     * @since 2.00a13
     */
    @Override
    public void removeCustomComponent(Component comp) {
        if (comp == null) {
            throw new NullPointerException("component is null");
        }
        if (topToolBar != null) {
            for (int i = 0; i < topToolBar.getComponentCount(); ++i) {
                if (topToolBar.getComponentAtIndex(i) == comp) {
                    removeCustomComponent(i);
                    return;
                }
            }
        }
        throw new IllegalArgumentException("component not in custom component bar: " + comp);
    }

    /**
     * Remove the custom component at position {@code index} in the custom
     * component area.
     *
     * @param index the 0-based index into the list of custom components
     * @throws IndexOutOfBoundsException if {@code index} &lt; 0 or
     * {@code index} &gt;= {@link #getCustomComponentCount()}
     * @since 2.00a13
     */
    @Override
    public void removeCustomComponent(int index) {
        if (topToolBar == null || index < 0 || index > topToolBar.getComponentCount()) {
            throw new IndexOutOfBoundsException("invalid component index: " + index);
        }
        Component comp = topToolBar.getComponentAtIndex(index);
        comp.setVisible(false);
        topToolBar.remove(comp);
        topToolBar.validate();
        if (topToolBar.getComponentCount() == 0) {
            topToolBar.getParent().remove(topToolBar);
            topToolBar = null;
            validate();
        }
    }

    /**
     * Return the number of custom components that have been added to the top of
     * the application window.
     *
     * @return the non-negative number of components that have been added
     * @since 2.00a13
     */
    @Override
    public int getCustomComponentCount() {
        if (topToolBar == null) {
            return 0;
        } else {
            return topToolBar.getComponentCount();
        }
    }

    /**
     * Return the custom component at position {@code index} in the custom
     * component area.
     *
     * @param index the 0-based index into the list of custom components
     * @return the component at position {@code index}
     * @throws IndexOutOfBoundsException if {@code index} &lt; 0 or
     * {@code index} &gt;= {@link #getCustomComponentCount()}
     * @since 2.00a13
     */
    @Override
    public Component getCustomComponent(int index) {
        if (topToolBar == null || index < 0 || index > topToolBar.getComponentCount()) {
            throw new IndexOutOfBoundsException("invalid component index: " + index);
        }
        return topToolBar.getComponent(index);
    }

    private JToolBar createToolBar() {
        JToolBar tb = new JToolBar();
        tb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(1, 1, 1, 1)
        )
        );
        tb.setFloatable(false);
        tb.setVisible(Settings.getShared().getYesNo(TOOL_BAR_ALLOWED_KEY));
        tb.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
        getContentPane().add(tb, java.awt.BorderLayout.NORTH);
        return tb;
    }
    private static final String TOOL_BAR_ALLOWED_KEY = "allow-custom-components";
    private JToolBar topToolBar;

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        tabHolder = new javax.swing.JPanel();
        desktopPane = new javax.swing.JDesktopPane();
        javax.swing.JPanel editorTabBugWorkaround = new javax.swing.JPanel();
        editorTab = new TEditorTabPane();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem newItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem newProjectItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator21 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem openItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem openProjectItem = new javax.swing.JMenuItem();
        openRecentMenu = new RecentFiles.RecentFileMenu( fileMenu );
        javax.swing.JPopupMenu.Separator jSeparator6 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem closeItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem closeProjectItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator7 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem saveItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem saveAsItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem saveAllItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator1 = new javax.swing.JPopupMenu.Separator();
        exportItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator4 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem printItem = new javax.swing.JMenuItem();
        exitSeparator = new javax.swing.JPopupMenu.Separator();
        exitItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem clearItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem cloneItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator23 = new javax.swing.JPopupMenu.Separator();
        convertMenu = new ConvertMenu(editMenu);
        javax.swing.JPopupMenu.Separator jSeparator9 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem cutItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem copyItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem pasteItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator20 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem selectAllItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator18 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem findItem = new javax.swing.JMenuItem();
        findInProjectItem = new javax.swing.JMenuItem();
        preferencesSeparator = new javax.swing.JPopupMenu.Separator();
        preferencesItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        inkSaverMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator38 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenu viewQualityMenu =  new ViewQualityMenu() ;
        javax.swing.JMenu previewBackdropItem =  new PreviewBackgroundMenu() ;
        cardEdgeMenu = new FinishStyleMenu();
        javax.swing.JPopupMenu.Separator jSeparator39 = new javax.swing.JPopupMenu.Separator();
        viewRegionBoxesItem = new javax.swing.JCheckBoxMenuItem();
        viewPortraitBoxesItem = new javax.swing.JCheckBoxMenuItem();
        viewEdgeOutlinesItem = new javax.swing.JCheckBoxMenuItem();
        viewUnsafeRegionsItem = new javax.swing.JCheckBoxMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator19 = new javax.swing.JPopupMenu.Separator();
        viewContextBarItem = new javax.swing.JCheckBoxMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator22 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JCheckBoxMenuItem deckHandesItem = new javax.swing.JCheckBoxMenuItem();
        javax.swing.JCheckBoxMenuItem deckGridItem = new javax.swing.JCheckBoxMenuItem();
        javax.swing.JCheckBoxMenuItem deckMarginItem = new javax.swing.JCheckBoxMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator34 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JCheckBoxMenuItem viewSourceNavItem = new javax.swing.JCheckBoxMenuItem();
        expansionSymbolMenu =  new ExpansionSymbolMenu() ;
        createExpansionItem = new javax.swing.JMenuItem();
        jSeparator13 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem copyExpItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem pasteExpItem = new javax.swing.JMenuItem();
        jSeparator17 = new javax.swing.JPopupMenu.Separator();
        variantMenu =  new ExpansionVariantMenu() ;
        expansionSeparator = new javax.swing.JPopupMenu.Separator();
        jSeparator10 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem chooseExpItem = new javax.swing.JMenuItem();
        markupMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem alignmentItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem insertColourItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem insertFontItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator12 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem insertImageItem = new javax.swing.JMenuItem();
        insertCharsItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator14 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem h1Item = new javax.swing.JMenuItem();
        javax.swing.JMenuItem h2Item = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator2 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem boldItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem italicItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem underlineItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem strikethroughItem = new javax.swing.JMenuItem();
        jSeparator36 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem superscriptItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem subscriptItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator26 = new javax.swing.JPopupMenu.Separator();
        markupAbbreviationsItem = new javax.swing.JMenuItem();
        deckMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem toFrontItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem toBackItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator30 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenu turnMirrorMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem turnLeftItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem turnRightItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem turnOverItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator29 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem mirrorItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem mirrorVertItem = new javax.swing.JMenuItem();
        alignMenu = new javax.swing.JMenu();
        alignLeftItem = new javax.swing.JMenuItem();
        alignCenterItem = new javax.swing.JMenuItem();
        alignRightItem = new javax.swing.JMenuItem();
        jSeparator35 = new javax.swing.JPopupMenu.Separator();
        alignTopItem = new javax.swing.JMenuItem();
        alignMiddleItem = new javax.swing.JMenuItem();
        alignBottomItem = new javax.swing.JMenuItem();
        jSeparator37 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem distHorzItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem distVertItem = new javax.swing.JMenuItem();
        snapMenu = new SnapMenu( deckMenu );
        javax.swing.JPopupMenu.Separator jSeparator31 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem groupItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem ungroupItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator32 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem editStyleItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem copyStyleItem = new javax.swing.JMenuItem();
        pasteStyleItem = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem lockItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem unlockItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem unlockAllItem = new javax.swing.JMenuItem();
        jSeparator33 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem centerContentItem = new javax.swing.JMenuItem();
        sourceMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem runFileItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem debugFileItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator28 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem makeBundleItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem testBundleItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator27 = new javax.swing.JPopupMenu.Separator();
        formatCodeItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem trimLinesItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator16 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem srcShiftUpItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem srcShiftDownItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem commentItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator15 = new javax.swing.JPopupMenu.Separator();
        sortSourceItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator3 = new javax.swing.JPopupMenu.Separator();
        sortSourceItem1 = new javax.swing.JMenuItem();
        javax.swing.JMenuItem abbreviationsItem = new javax.swing.JMenuItem();
        toolboxMenu =  new ToolboxMenu() ;
        pluginUpdatesItem = new javax.swing.JMenuItem();
        jSeparator24 = new javax.swing.JPopupMenu.Separator();
        pluginCatalogItem = new javax.swing.JMenuItem();
        managePluginsItem = new javax.swing.JMenuItem();
        windowMenu =  new WindowMenu() ;
        helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem helpItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem userManualItem = new javax.swing.JMenuItem();
        jSeparator11 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem devManualItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem translatorManualItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem devJavaApiItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem devJsApiItem = new javax.swing.JMenuItem();
        jSeparator25 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem bugReportItem = new javax.swing.JMenuItem();
        aboutSeparator = new javax.swing.JPopupMenu.Separator();
        aboutItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setName("Strange Eons App Window"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        tabHolder.setBackground( UIManager.getDefaults().getColor( Theme.EDITOR_TAB_BACKGROUND ) );
        tabHolder.setName("tabHolder"); // NOI18N
        tabHolder.setLayout(new java.awt.BorderLayout());

        desktopPane.setName("desktopPane"); // NOI18N
        desktopPane.setOpaque(false);
        tabHolder.add(desktopPane, java.awt.BorderLayout.CENTER);
        TAttachedEditor.editorTab = (TEditorTabPane) editorTab;

        editorTabBugWorkaround.setBackground( UIManager.getDefaults().getColor( Theme.EDITOR_TAB_BACKGROUND ) );
        editorTabBugWorkaround.setName("editorTabBugWorkaround"); // NOI18N
        editorTabBugWorkaround.setLayout(new java.awt.GridBagLayout());

        editorTab.setBackground( UIManager.getDefaults().getColor( Theme.EDITOR_TAB_BACKGROUND ) );
        editorTab.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 1, 1, 1,  UIManager.getDefaults().getColor( Theme.EDITOR_TAB_BACKGROUND ) ));
        editorTab.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        editorTab.setTabPlacement(UIManager.getBoolean(Theme.ALTERNATE_DOCUMENT_TAB_ORIENTATION) ? JTabbedPane.TOP : JTabbedPane.BOTTOM);
        editorTab.setName("editorTab"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        editorTabBugWorkaround.add(editorTab, gridBagConstraints);

        tabHolder.add(editorTabBugWorkaround, java.awt.BorderLayout.NORTH);

        getContentPane().add(tabHolder, java.awt.BorderLayout.CENTER);

        menuBar.setDoubleBuffered(true);
        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(string("app-file")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        newItem.setAction( Commands.NEW_GAME_COMPONENT );
        newItem.setName("newItem"); // NOI18N
        fileMenu.add(newItem);

        newProjectItem.setAction( Commands.NEW_PROJECT );
        newProjectItem.setName("newProjectItem"); // NOI18N
        fileMenu.add(newProjectItem);

        jSeparator21.setName("jSeparator21"); // NOI18N
        fileMenu.add(jSeparator21);

        openItem.setAction( Commands.OPEN );
        openItem.setName("openItem"); // NOI18N
        fileMenu.add(openItem);

        openProjectItem.setAction( Commands.OPEN_PROJECT );
        openProjectItem.setName("openProjectItem"); // NOI18N
        fileMenu.add(openProjectItem);

        openRecentMenu.setText(string("app-open-recent")); // NOI18N
        openRecentMenu.setName("openRecentMenu"); // NOI18N
        fileMenu.add(openRecentMenu);

        jSeparator6.setName("jSeparator6"); // NOI18N
        fileMenu.add(jSeparator6);

        closeItem.setAction( Commands.CLOSE );
        closeItem.setName("closeItem"); // NOI18N
        fileMenu.add(closeItem);

        closeProjectItem.setAction( Commands.CLOSE_PROJECT );
        closeProjectItem.setName("closeProjectItem"); // NOI18N
        fileMenu.add(closeProjectItem);

        jSeparator7.setName("jSeparator7"); // NOI18N
        fileMenu.add(jSeparator7);

        saveItem.setAction( Commands.SAVE );
        saveItem.setName("saveItem"); // NOI18N
        fileMenu.add(saveItem);

        saveAsItem.setAction( Commands.SAVE_AS );
        saveAsItem.setName("saveAsItem"); // NOI18N
        fileMenu.add(saveAsItem);

        saveAllItem.setAction( Commands.SAVE_ALL );
        saveAllItem.setName("saveAllItem"); // NOI18N
        fileMenu.add(saveAllItem);

        jSeparator1.setName("jSeparator1"); // NOI18N
        fileMenu.add(jSeparator1);

        exportItem.setAction( Commands.EXPORT );
        exportItem.setName("exportItem"); // NOI18N
        fileMenu.add(exportItem);

        jSeparator4.setName("jSeparator4"); // NOI18N
        fileMenu.add(jSeparator4);

        printItem.setAction( Commands.PRINT );
        printItem.setName("printItem"); // NOI18N
        fileMenu.add(printItem);

        exitSeparator.setName("exitSeparator"); // NOI18N
        fileMenu.add(exitSeparator);

        exitItem.setAction( Commands.EXIT );
        exitItem.setName("exitItem"); // NOI18N
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        editMenu.setText(string("app-edit")); // NOI18N
        editMenu.setName("editMenu"); // NOI18N

        clearItem.setAction( Commands.CLEAR );
        clearItem.setName("clearItem"); // NOI18N
        editMenu.add(clearItem);

        cloneItem.setAction( Commands.SPIN_OFF );
        cloneItem.setName("cloneItem"); // NOI18N
        editMenu.add(cloneItem);

        jSeparator23.setName("jSeparator23"); // NOI18N
        editMenu.add(jSeparator23);

        convertMenu.setText(string("app-convert")); // NOI18N
        convertMenu.setName("convertMenu"); // NOI18N
        editMenu.add(convertMenu);

        jSeparator9.setName("jSeparator9"); // NOI18N
        editMenu.add(jSeparator9);

        cutItem.setAction( Commands.CUT );
        cutItem.setName("cutItem"); // NOI18N
        editMenu.add(cutItem);

        copyItem.setAction( Commands.COPY );
        copyItem.setName("copyItem"); // NOI18N
        editMenu.add(copyItem);

        pasteItem.setAction( Commands.PASTE );
        pasteItem.setName("pasteItem"); // NOI18N
        editMenu.add(pasteItem);

        jSeparator20.setName("jSeparator20"); // NOI18N
        editMenu.add(jSeparator20);

        selectAllItem.setAction( Commands.SELECT_ALL );
        selectAllItem.setName("selectAllItem"); // NOI18N
        editMenu.add(selectAllItem);

        jSeparator18.setName("jSeparator18"); // NOI18N
        editMenu.add(jSeparator18);

        findItem.setAction( Commands.FIND );
        findItem.setName("findItem"); // NOI18N
        editMenu.add(findItem);

        findInProjectItem.setAction( Commands.FIND_IN_PROJECT );
        findInProjectItem.setName("findInProjectItem"); // NOI18N
        editMenu.add(findInProjectItem);

        preferencesSeparator.setName("preferencesSeparator"); // NOI18N
        editMenu.add(preferencesSeparator);

        preferencesItem.setAction( Commands.PREFERENCES );
        preferencesItem.setName("preferencesItem"); // NOI18N
        editMenu.add(preferencesItem);

        menuBar.add(editMenu);

        viewMenu.setText(string("app-view")); // NOI18N
        viewMenu.setName("viewMenu"); // NOI18N

        inkSaverMenuItem.setAction( Commands.VIEW_INK_SAVER );
        inkSaverMenuItem.setName("inkSaverMenuItem"); // NOI18N
        viewMenu.add(inkSaverMenuItem);

        jSeparator38.setName("jSeparator38"); // NOI18N
        viewMenu.add(jSeparator38);

        viewQualityMenu.setText(string("app-quality")); // NOI18N
        viewQualityMenu.setName("viewQualityMenu"); // NOI18N
        viewMenu.add(viewQualityMenu);

        previewBackdropItem.setText(string("app-backdrop")); // NOI18N
        previewBackdropItem.setName("previewBackdropItem"); // NOI18N
        viewMenu.add(previewBackdropItem);

        cardEdgeMenu.setText(string("app-card-edge")); // NOI18N
        cardEdgeMenu.setName("cardEdgeMenu"); // NOI18N
        viewMenu.add(cardEdgeMenu);

        jSeparator39.setName("jSeparator39"); // NOI18N
        viewMenu.add(jSeparator39);

        viewRegionBoxesItem.setAction(Commands.VIEW_REGION_BOXES);
        viewRegionBoxesItem.setName("viewRegionBoxesItem"); // NOI18N
        viewMenu.add(viewRegionBoxesItem);

        viewPortraitBoxesItem.setAction(Commands.VIEW_PORTRAIT_BOXES);
        viewPortraitBoxesItem.setName("viewPortraitBoxesItem"); // NOI18N
        viewMenu.add(viewPortraitBoxesItem);

        viewEdgeOutlinesItem.setAction(Commands.VIEW_EDGE_BOXES);
        viewEdgeOutlinesItem.setName("viewEdgeOutlinesItem"); // NOI18N
        viewMenu.add(viewEdgeOutlinesItem);

        viewUnsafeRegionsItem.setAction(Commands.VIEW_UNSAFE_BOXES);
        viewUnsafeRegionsItem.setName("viewUnsafeRegionsItem"); // NOI18N
        viewMenu.add(viewUnsafeRegionsItem);

        jSeparator19.setName("jSeparator19"); // NOI18N
        viewMenu.add(jSeparator19);

        viewContextBarItem.setAction( Commands.VIEW_CONTEXT_BAR );
        viewContextBarItem.setName("viewContextBarItem"); // NOI18N
        viewMenu.add(viewContextBarItem);

        jSeparator22.setName("jSeparator22"); // NOI18N
        viewMenu.add(jSeparator22);

        deckHandesItem.setAction( Commands.VIEW_DECK_HANDLES );
        deckHandesItem.setName("deckHandesItem"); // NOI18N
        viewMenu.add(deckHandesItem);

        deckGridItem.setAction( Commands.VIEW_DECK_GRID );
        deckGridItem.setName("deckGridItem"); // NOI18N
        viewMenu.add(deckGridItem);

        deckMarginItem.setAction( Commands.VIEW_DECK_MARGIN );
        deckMarginItem.setName("deckMarginItem"); // NOI18N
        viewMenu.add(deckMarginItem);

        jSeparator34.setName("jSeparator34"); // NOI18N
        viewMenu.add(jSeparator34);

        viewSourceNavItem.setAction( Commands.VIEW_SOURCE_NAVIGATOR );
        viewSourceNavItem.setName("viewSourceNavItem"); // NOI18N
        viewMenu.add(viewSourceNavItem);

        menuBar.add(viewMenu);

        expansionSymbolMenu.setText(string("app-expansion")); // NOI18N
        expansionSymbolMenu.setName("expansionSymbolMenu"); // NOI18N

        createExpansionItem.setAction( Commands.EXPANSION_NEW );
        createExpansionItem.setName("createExpansionItem"); // NOI18N
        expansionSymbolMenu.add(createExpansionItem);

        jSeparator13.setName("jSeparator13"); // NOI18N
        expansionSymbolMenu.add(jSeparator13);

        copyExpItem.setAction( Commands.EXPANSION_COPY );
        copyExpItem.setName("copyExpItem"); // NOI18N
        expansionSymbolMenu.add(copyExpItem);

        pasteExpItem.setAction( Commands.EXPANSION_PASTE );
        pasteExpItem.setName("pasteExpItem"); // NOI18N
        expansionSymbolMenu.add(pasteExpItem);
        expansionSymbolMenu.add(jSeparator17);

        variantMenu.setText(string("app-exp-var")); // NOI18N
        variantMenu.setName("variantMenu"); // NOI18N
        expansionSymbolMenu.add(variantMenu);

        expansionSeparator.setName("expansionSeparator"); // NOI18N
        expansionSymbolMenu.add(expansionSeparator);

        jSeparator10.setName("jSeparator10"); // NOI18N
        expansionSymbolMenu.add(jSeparator10);

        chooseExpItem.setAction( Commands.EXPANSION_CHOOSE );
        chooseExpItem.setName("chooseExpItem"); // NOI18N
        expansionSymbolMenu.add(chooseExpItem);

        menuBar.add(expansionSymbolMenu);

        markupMenu.setText(string("app-markup")); // NOI18N
        markupMenu.setName("markupMenu"); // NOI18N

        alignmentItem.setAction(Commands.MARKUP_ALIGNMENT);
        alignmentItem.setName("alignmentItem"); // NOI18N
        markupMenu.add(alignmentItem);

        insertColourItem.setAction( Commands.MARKUP_INSERT_COLOUR );
        insertColourItem.setName("insertColourItem"); // NOI18N
        markupMenu.add(insertColourItem);

        insertFontItem.setAction( Commands.MARKUP_INSERT_FONT );
        insertFontItem.setName("insertFontItem"); // NOI18N
        markupMenu.add(insertFontItem);

        jSeparator12.setName("jSeparator12"); // NOI18N
        markupMenu.add(jSeparator12);

        insertImageItem.setAction( Commands.MARKUP_INSERT_IMAGE );
        insertImageItem.setName("insertImageItem"); // NOI18N
        markupMenu.add(insertImageItem);

        insertCharsItem.setAction( Commands.MARKUP_INSERT_CHARACTERS );
        insertCharsItem.setName("insertCharsItem"); // NOI18N
        markupMenu.add(insertCharsItem);

        jSeparator14.setName("jSeparator14"); // NOI18N
        markupMenu.add(jSeparator14);

        h1Item.setAction( Commands.MARKUP_HEADING );
        h1Item.setName("h1Item"); // NOI18N
        markupMenu.add(h1Item);

        h2Item.setAction( Commands.MARKUP_SUBHEADING );
        h2Item.setName("h2Item"); // NOI18N
        markupMenu.add(h2Item);

        jSeparator2.setName("jSeparator2"); // NOI18N
        markupMenu.add(jSeparator2);

        boldItem.setAction( Commands.MARKUP_BOLD );
        boldItem.setName("boldItem"); // NOI18N
        markupMenu.add(boldItem);

        italicItem.setAction( Commands.MARKUP_ITALIC );
        italicItem.setName("italicItem"); // NOI18N
        markupMenu.add(italicItem);

        underlineItem.setAction( Commands.MARKUP_UNDERLINE );
        underlineItem.setName("underlineItem"); // NOI18N
        markupMenu.add(underlineItem);

        strikethroughItem.setAction( Commands.MARKUP_STRIKETHROUGH);
        strikethroughItem.setName("strikethroughItem"); // NOI18N
        markupMenu.add(strikethroughItem);

        jSeparator36.setName("jSeparator36"); // NOI18N
        markupMenu.add(jSeparator36);

        superscriptItem.setAction( Commands.MARKUP_SUPERSCRIPT);
        superscriptItem.setName("superscriptItem"); // NOI18N
        markupMenu.add(superscriptItem);

        subscriptItem.setAction( Commands.MARKUP_SUBSCRIPT);
        subscriptItem.setName("subscriptItem"); // NOI18N
        markupMenu.add(subscriptItem);

        jSeparator26.setName("jSeparator26"); // NOI18N
        markupMenu.add(jSeparator26);

        markupAbbreviationsItem.setAction( Commands.MARKUP_ABBREVIATIONS );
        markupAbbreviationsItem.setName("markupAbbreviationsItem"); // NOI18N
        markupMenu.add(markupAbbreviationsItem);

        menuBar.add(markupMenu);

        deckMenu.setText(string("app-deck")); // NOI18N
        deckMenu.setName("deckMenu"); // NOI18N

        toFrontItem.setAction( Commands.TO_FRONT);
        toFrontItem.setName("toFrontItem"); // NOI18N
        deckMenu.add(toFrontItem);

        toBackItem.setAction( Commands.TO_BACK);
        toBackItem.setName("toBackItem"); // NOI18N
        deckMenu.add(toBackItem);

        jSeparator30.setName("jSeparator30"); // NOI18N
        deckMenu.add(jSeparator30);

        turnMirrorMenu.setText(string("app-turn-menu")); // NOI18N
        turnMirrorMenu.setName("turnMirrorMenu"); // NOI18N

        turnLeftItem.setAction( Commands.TURN_LEFT );
        turnLeftItem.setName("turnLeftItem"); // NOI18N
        turnMirrorMenu.add(turnLeftItem);

        turnRightItem.setAction( Commands.TURN_RIGHT );
        turnRightItem.setName("turnRightItem"); // NOI18N
        turnMirrorMenu.add(turnRightItem);

        turnOverItem.setAction( Commands.TURN_180  );
        turnOverItem.setName("turnOverItem"); // NOI18N
        turnMirrorMenu.add(turnOverItem);

        jSeparator29.setName("jSeparator29"); // NOI18N
        turnMirrorMenu.add(jSeparator29);

        mirrorItem.setAction( Commands.FLIP_HORZ );
        mirrorItem.setName("mirrorItem"); // NOI18N
        turnMirrorMenu.add(mirrorItem);

        mirrorVertItem.setAction( Commands.FLIP_VERT );
        mirrorVertItem.setName("mirrorVertItem"); // NOI18N
        turnMirrorMenu.add(mirrorVertItem);

        deckMenu.add(turnMirrorMenu);

        alignMenu.setText(string("app-align")); // NOI18N
        alignMenu.setName("alignMenu"); // NOI18N

        alignLeftItem.setAction( Commands.ALIGN_LEFT );
        alignLeftItem.setName("alignLeftItem"); // NOI18N
        alignMenu.add(alignLeftItem);

        alignCenterItem.setAction( Commands.ALIGN_CENTER );
        alignCenterItem.setName("alignCenterItem"); // NOI18N
        alignMenu.add(alignCenterItem);

        alignRightItem.setAction( Commands.ALIGN_RIGHT );
        alignRightItem.setName("alignRightItem"); // NOI18N
        alignMenu.add(alignRightItem);

        jSeparator35.setName("jSeparator35"); // NOI18N
        alignMenu.add(jSeparator35);

        alignTopItem.setAction( Commands.ALIGN_TOP );
        alignTopItem.setName("alignTopItem"); // NOI18N
        alignMenu.add(alignTopItem);

        alignMiddleItem.setAction( Commands.ALIGN_MIDDLE );
        alignMiddleItem.setName("alignMiddleItem"); // NOI18N
        alignMenu.add(alignMiddleItem);

        alignBottomItem.setAction( Commands.ALIGN_BOTTOM);
        alignBottomItem.setName("alignBottomItem"); // NOI18N
        alignMenu.add(alignBottomItem);

        jSeparator37.setName("jSeparator37"); // NOI18N
        alignMenu.add(jSeparator37);

        distHorzItem.setAction( Commands.DISTRIBUTE_HORZ );
        distHorzItem.setName("distHorzItem"); // NOI18N
        alignMenu.add(distHorzItem);

        distVertItem.setAction( Commands.DISTRIBUTE_VERT );
        distVertItem.setName("distVertItem"); // NOI18N
        alignMenu.add(distVertItem);

        deckMenu.add(alignMenu);

        snapMenu.setText(string("app-snap-class")); // NOI18N
        snapMenu.setName("snapMenu"); // NOI18N
        deckMenu.add(snapMenu);

        jSeparator31.setName("jSeparator31"); // NOI18N
        deckMenu.add(jSeparator31);

        groupItem.setAction( Commands.GROUP);
        groupItem.setName("groupItem"); // NOI18N
        deckMenu.add(groupItem);

        ungroupItem.setAction( Commands.UNGROUP);
        ungroupItem.setName("ungroupItem"); // NOI18N
        deckMenu.add(ungroupItem);

        jSeparator32.setName("jSeparator32"); // NOI18N
        deckMenu.add(jSeparator32);

        editStyleItem.setAction( Commands.EDIT_STYLE );
        editStyleItem.setName("editStyleItem"); // NOI18N
        deckMenu.add(editStyleItem);

        copyStyleItem.setAction( Commands.COPY_STYLE );
        copyStyleItem.setName("copyStyleItem"); // NOI18N
        deckMenu.add(copyStyleItem);

        pasteStyleItem.setAction( Commands.PASTE_STYLE );
        pasteStyleItem.setName("pasteStyleItem"); // NOI18N
        deckMenu.add(pasteStyleItem);

        jSeparator5.setName("jSeparator5"); // NOI18N
        deckMenu.add(jSeparator5);

        lockItem.setAction( Commands.LOCK );
        lockItem.setName("lockItem"); // NOI18N
        deckMenu.add(lockItem);

        unlockItem.setAction( Commands.UNLOCK );
        unlockItem.setName("unlockItem"); // NOI18N
        deckMenu.add(unlockItem);

        unlockAllItem.setAction( Commands.UNLOCK_ALL );
        unlockAllItem.setName("unlockAllItem"); // NOI18N
        deckMenu.add(unlockAllItem);

        jSeparator33.setName("jSeparator33"); // NOI18N
        deckMenu.add(jSeparator33);

        centerContentItem.setAction( Commands.CENTER_CONTENT );
        centerContentItem.setName("centerContentItem"); // NOI18N
        deckMenu.add(centerContentItem);

        menuBar.add(deckMenu);

        sourceMenu.setText(string("app-source")); // NOI18N
        sourceMenu.setName("sourceMenu"); // NOI18N

        runFileItem.setAction( Commands.RUN_FILE );
        runFileItem.setName("runFileItem"); // NOI18N
        sourceMenu.add(runFileItem);

        debugFileItem.setAction( Commands.DEBUG_FILE );
        debugFileItem.setName("debugFileItem"); // NOI18N
        sourceMenu.add(debugFileItem);

        jSeparator28.setName("jSeparator28"); // NOI18N
        sourceMenu.add(jSeparator28);

        makeBundleItem.setAction( Commands.MAKE_BUNDLE );
        makeBundleItem.setName("makeBundleItem"); // NOI18N
        sourceMenu.add(makeBundleItem);

        testBundleItem.setAction( Commands.TEST_BUNDLE );
        testBundleItem.setName("testBundleItem"); // NOI18N
        sourceMenu.add(testBundleItem);

        jSeparator27.setName("jSeparator27"); // NOI18N
        sourceMenu.add(jSeparator27);

        formatCodeItem.setAction(Commands.FORMAT_CODE);
        formatCodeItem.setName("formatCodeItem"); // NOI18N
        sourceMenu.add(formatCodeItem);

        trimLinesItem.setAction( Commands.REMOVE_TRAILING_SPACES );
        trimLinesItem.setName("trimLinesItem"); // NOI18N
        sourceMenu.add(trimLinesItem);

        jSeparator16.setName("jSeparator16"); // NOI18N
        sourceMenu.add(jSeparator16);

        srcShiftUpItem.setAction( Commands.MOVE_LINES_UP );
        srcShiftUpItem.setName("srcShiftUpItem"); // NOI18N
        sourceMenu.add(srcShiftUpItem);

        srcShiftDownItem.setAction( Commands.MOVE_LINES_DOWN );
        srcShiftDownItem.setName("srcShiftDownItem"); // NOI18N
        sourceMenu.add(srcShiftDownItem);

        commentItem.setAction( Commands.COMMENT_OUT );
        commentItem.setName("commentItem"); // NOI18N
        sourceMenu.add(commentItem);

        jSeparator15.setName("jSeparator15"); // NOI18N
        sourceMenu.add(jSeparator15);

        sortSourceItem.setAction( Commands.SORT );
        sortSourceItem.setName("sortSourceItem"); // NOI18N
        sourceMenu.add(sortSourceItem);

        jSeparator3.setName("jSeparator3"); // NOI18N
        sourceMenu.add(jSeparator3);

        sortSourceItem1.setAction( Commands.COMPLETE_CODE );
        sortSourceItem1.setName("sortSourceItem1"); // NOI18N
        sourceMenu.add(sortSourceItem1);

        abbreviationsItem.setAction( Commands.CODE_ABBREVIATIONS );
        abbreviationsItem.setName("abbreviationsItem"); // NOI18N
        sourceMenu.add(abbreviationsItem);

        menuBar.add(sourceMenu);

        toolboxMenu.setText(string("app-toolbox")); // NOI18N
        toolboxMenu.setEnabled(false);
        toolboxMenu.setName("toolboxMenu"); // NOI18N

        pluginUpdatesItem.setAction( Commands.CONFIGURE_UPDATES );
        pluginUpdatesItem.setName("pluginUpdatesItem"); // NOI18N
        toolboxMenu.add(pluginUpdatesItem);

        jSeparator24.setName("jSeparator24"); // NOI18N
        toolboxMenu.add(jSeparator24);

        pluginCatalogItem.setAction( Commands.PLUGIN_CATALOG );
        pluginCatalogItem.setName("pluginCatalogItem"); // NOI18N
        toolboxMenu.add(pluginCatalogItem);

        managePluginsItem.setAction( Commands.PLUGIN_MANAGER );
        managePluginsItem.setName("managePluginsItem"); // NOI18N
        toolboxMenu.add(managePluginsItem);

        menuBar.add(toolboxMenu);

        windowMenu.setText(string("app-window")); // NOI18N
        windowMenu.setName("windowMenu"); // NOI18N
        menuBar.add(windowMenu);

        helpMenu.setText(string("app-help")); // NOI18N
        helpMenu.setActionCommand(string("app-help")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        helpItem.setAction( Commands.HELP );
        helpItem.setName("helpItem"); // NOI18N
        helpMenu.add(helpItem);

        userManualItem.setAction( Commands.HELP_USER_MANUAL );
        userManualItem.setName("userManualItem"); // NOI18N
        helpMenu.add(userManualItem);

        jSeparator11.setName("jSeparator11"); // NOI18N
        helpMenu.add(jSeparator11);

        devManualItem.setAction( Commands.HELP_DEV_MANUAL );
        devManualItem.setName("devManualItem"); // NOI18N
        helpMenu.add(devManualItem);

        translatorManualItem.setAction( Commands.HELP_TRANSLATOR_MANUAL );
        translatorManualItem.setName("translatorManualItem"); // NOI18N
        helpMenu.add(translatorManualItem);

        devJavaApiItem.setAction( Commands.HELP_DEV_JAVA_API);
        devJavaApiItem.setName("devJavaApiItem"); // NOI18N
        helpMenu.add(devJavaApiItem);

        devJsApiItem.setAction( Commands.HELP_DEV_JS_API);
        devJsApiItem.setName("devJsApiItem"); // NOI18N
        helpMenu.add(devJsApiItem);

        jSeparator25.setName("jSeparator25"); // NOI18N
        helpMenu.add(jSeparator25);

        bugReportItem.setAction( Commands.FILE_BUG_REPORT );
        bugReportItem.setName("bugReportItem"); // NOI18N
        helpMenu.add(bugReportItem);

        aboutSeparator.setName("aboutSeparator"); // NOI18N
        helpMenu.add(aboutSeparator);

        aboutItem.setAction( Commands.ABOUT );
        aboutItem.setName("aboutItem"); // NOI18N
        helpMenu.add(aboutItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        setSize(new java.awt.Dimension(800, 600));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    @Override
    public StrangeEonsEditor selectNextEditor() {
        if (editorTab.getTabCount() > 0) {
            int i = editorTab.getSelectedIndex() + 1;
            editorTab.setSelectedIndex(i == editorTab.getTabCount() ? 0 : i);
        }
        return getActiveEditor();
    }

    @Override
    public StrangeEonsEditor selectPreviousEditor() {
        if (editorTab.getTabCount() > 0) {
            int i = editorTab.getSelectedIndex() - 1;
            editorTab.setSelectedIndex(i == -1 ? editorTab.getTabCount() - 1 : i);
        }
        return getActiveEditor();
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        exitApplication(false);
    }//GEN-LAST:event_formWindowClosing

    @Override
    public void showPreferencesDialog(Component parent, PreferenceCategory displayCategory) {
        if (parent == null) {
            parent = this;
        }
        Preferences d = new Preferences();
        if (displayCategory != null) {
            d.setSelectedCategory(displayCategory);
        }
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
    }

    /**
     * Show the about dialog for the application.
     */
    @Override
    public void showAboutDialog() {
        setWaitCursor();
        try {
            About dlg = new About();
            dlg.setVisible(true);
        } finally {
            setDefaultCursor();
        }
    }

    @Override
    public boolean exitApplication(final boolean restart) {
        if ((getExtendedState() & ICONIFIED) != 0) {
            setExtendedState(getExtendedState() & ~ICONIFIED);
        }

        final MultiCloseDialog mcd = new MultiCloseDialog(this);
        if (mcd.showDialog()) {
            Settings.getUser().storeWindowSettings("appframe", this);

            // Prevent any more messages from trying to display during shutdown
            // This must be called before hiding the window or displaying a message
            // will throw an exception.
            Messenger.setQueueProcessingEnabled(false);

            setVisible(false);
            try {
                if (project != null) {
                    File projFile = project.getPackageFile() != null ? project.getPackageFile() : project.getFile();
                    Settings.getUser().set(LAST_PROJECT_KEY, projFile.getAbsolutePath());
                    closeProject();
                } else {
                    Settings.getUser().reset(LAST_PROJECT_KEY);
                }

                dispose();
                StrangeEons.getApplication().unloadPlugins();
                BundleInstaller.unloadExtensions();
                StrangeEons.getApplication().runExitTasks();
                AutomaticUpdater.writePendingUpdateInformation();
                RawSettings.writeUserSettingsImmediately();
            } finally {
                // do an invokeLater to synch with any exit tasks that need to invokeLater
                EventQueue.invokeLater(() -> {
                    try {
                        if (restart) {
                            Restarter.launchRestartProcess();
                        }
                    } catch (Throwable t) {
                        StrangeEons.log.log(Level.SEVERE, "exception while trying to restart", t);
                    } finally {
                        System.exit(0);
                    }
                });
            }
            return true;
        }
        return false;
    }

    /**
     * Tracks whether the window has ever been visible, which cancels any
     * non-interactive mode.
     */
    boolean hasEverBeenMadeVisible = false;

    @Override
    @SuppressWarnings("deprecation")
    public void show() {
        // all calls to setVisible, etc., eventually lead to show()
        hasEverBeenMadeVisible = true;
        super.show();
    }

    private static final String LAST_PROJECT_KEY = "recent-project";

    @Override
    public StrangeEonsEditor getActiveEditor() {
        int tab = editorTab.getSelectedIndex();
        if (tab < 0) {
            return null;
        }
        TEditorTabLink elp = (TEditorTabLink) editorTab.getComponentAt(tab);
        return (StrangeEonsEditor) elp.getLinkedEditor();

//		JInternalFrame f = desktopPane.getSelectedFrame();
//		if( f == null ) {
//			JInternalFrame frames[] = desktopPane.getAllFrames();
//			if( frames.length >= 1 ) {
//				f = frames[0];
//			}
//		}
//		return (StrangeEonsEditor) f;
    }

    // used by the window menu since detached editors are also tracked windows;
    // this keeps them from being listed twice
    StrangeEonsEditor[] getAttachedEditors() {
        JInternalFrame[] frames = desktopPane.getAllFrames();
        StrangeEonsEditor[] editors = new StrangeEonsEditor[frames.length];
        for (int i = 0; i < frames.length; ++i) {
            editors[i] = (StrangeEonsEditor) frames[i];
        }
        return editors;
    }

    @Override
    public StrangeEonsEditor[] getEditors() {
        final int count = editorTab.getTabCount();
        final StrangeEonsEditor[] editors = new StrangeEonsEditor[count];
        for (int i = 0; i < editors.length; ++i) {
            final TEditorTabLink link = (TEditorTabLink) editorTab.getComponentAt(i);
            editors[i] = (StrangeEonsEditor) link.getLinkedEditor();
        }
        return editors;
    }

    @Override
    public void redrawPreviews() {
        for (StrangeEonsEditor ed : getEditors()) {
            if (ed instanceof AbstractGameComponentEditor) {
                ((AbstractGameComponentEditor) ed).redrawPreview();
            }
        }
    }

    /**
     * @deprecated Replaced by {@link #redrawPreviews()}.
     */
    @Deprecated
    public void forceRerender() {
        redrawPreviews();
    }

    @Override
    public void addEditor(StrangeEonsEditor editor) {
        if (editor == null) {
            throw new NullPointerException("null editor");
        }

        final AbstractStrangeEonsEditor frame = (AbstractStrangeEonsEditor) editor;

        setWaitCursor();
        try {
            installTextEditorFont(frame);

            TEditorTabLink elp = new TEditorTabLink(frame);
            desktopPane.add(frame);
            editorTab.add("", elp);
            frame.setEditorLinkPanel(elp);

            try {
                frame.pack();
                frame.setMaximum(true);
            } catch (PropertyVetoException e) {
            }

            EventQueue.invokeLater(() -> {
                try {
                    frame.setMaximum(true);
                    frame.updateTab();
                } catch (PropertyVetoException e) {
                }
            });

            frame.setVisible(true);
            try {
                frame.setSelected(true);
            } catch (PropertyVetoException e) {
            }
            fireEditorAdded(editor);
            frame.updateTab();
        } finally {
            setDefaultCursor();
        }
    }

    private void installTextEditorFont(JComponent parent) {
        if (parent instanceof PortraitPanel) {
            return;
        }
        if (parent instanceof HSBPanel) {
            return;
        }

        if (parent instanceof JTextComponent && parent.getClientProperty(NO_EDITOR_FONT) == null) {
            JTextComponent textComponent = (JTextComponent) parent;
            if (textComponent.isEditable()) {
                textComponent.setFont(ResourceKit.getEditorFont());
            }
        } else {
            for (int i = 0; i < parent.getComponentCount(); ++i) {
                Component c = parent.getComponent(i);
                if (c instanceof JComponent) {
                    installTextEditorFont((JComponent) c);
                }
            }
        }
    }

    @Override
    public void setWaitCursor() {
        JUtilities.showWaitCursor(this);
    }

    @Override
    public void setDefaultCursor() {
        JUtilities.hideWaitCursor(this);
    }

    /**
     * Adds a new listener for changes to the current project.
     *
     * @param li the listener to call when a project is opened or closed
     */
    @Override
    public void addProjectEventListener(ProjectEventListener li) {
        listeners.add(ProjectEventListener.class, li);
    }

    /**
     * Removes a listener for changes to the current project.
     *
     * @param li the listener to be removed
     */
    @Override
    public void removeProjectEventListener(ProjectEventListener li) {
        listeners.remove(ProjectEventListener.class, li);
    }

    private Project project;

    @Override
    public Project getOpenProject() {
        return project;
    }

    @Override
    public boolean setOpenProject(File projectFolder) {
        return openProject(projectFolder, true);
    }

    private boolean openProject(File projectFolder, boolean showOpenAnimation) {
        setWaitCursor();
        try {
            Project proj = null;
            try {
                String exclude = Settings.getShared().get("exclude-from-projects");
                if (exclude != null && !exclude.isEmpty()) {
                    String[] pats = exclude.trim().split("\\s*,\\s*");
                    for (int i = 0; i < pats.length; ++i) {
                        pats[i] = pats[i].trim();
                    }
                    Member.setExcludedFilePatterns(pats);
                }
                proj = Project.open(projectFolder);

                // check the minver and requires settings to see if the project
                // requires a newer version or missing components
                if (isVisible()) {
                    final Settings s = proj.getSettings();
                    final String build = s.get(Listing.MINIMUM_VERSION);
                    if (build != null && !build.isEmpty()) {
                        try {
                            int minBuild = Integer.parseInt(build.trim());
                            if (minBuild > StrangeEons.getBuildNumber()) {
                                final String ok = string("app-warn-proj-minver-ok");
                                final int choice = JOptionPane.showOptionDialog(
                                        this, string("app-warn-proj-minver"), "",
                                        0, JOptionPane.WARNING_MESSAGE, null,
                                        new Object[]{ok, string("cancel")}, ok
                                );
                                if (choice != 0) {
                                    return false;
                                }
                            }
                        } catch (NumberFormatException e) {
                            StrangeEons.log.log(Level.WARNING, "invalid requires-build value in project: \"{0}\"", build);
                        }
                    }

                    final String requires = s.get(Listing.REQUIRES);
                    if (requires != null) {
                        final String coreSpec = requires.replaceAll("\\s*,\\s*", "\n");
                        CoreComponents.validateCoreComponents(coreSpec);
                    }
                }

            } catch (Exception e) {
                ErrorDialog.displayError(string("prj-err-open-proj"), e);
                return false;
            }

            if (project != null) {
                closeProjectImpl();
            }
            project = proj;
            createProjectView(proj, showOpenAnimation);
            RecentFiles.addRecentProject(projectFolder);

            // fire project event
            Object[] li = listeners.getListenerList();
            for (int i = li.length - 2; i >= 0; i -= 2) {
                if (li[i] == ProjectEventListener.class) {
                    try {
                        ((ProjectEventListener) li[i + 1]).projectOpened(proj);
                    } catch (Exception e) {
                        StrangeEons.log.log(Level.WARNING, "uncaught exception in ProjectEventListener", e);
                    }
                }
            }

            return true;
        } finally {
            setDefaultCursor();
        }
    }

    @Override
    public void closeProject() {
        if (projView == null) {
            return;
        }

        // fire project event
        Project proj = projView.getProject();
        Object[] li = listeners.getListenerList();
        for (int i = li.length - 2; i >= 0; i -= 2) {
            if (li[i] == ProjectEventListener.class) {
                try {
                    ((ProjectEventListener) li[i + 1]).projectClosing(proj);
                } catch (Exception e) {
                    StrangeEons.log.log(Level.WARNING, "uncaught exception in ProjectEventListener " + li[i + 1], e);
                }
            }
        }

        closeProjectImpl();
        if (projView != null) {
            Settings.getUser().setInt(KEY_PROJECT_DIVIDER, projectSplitPane.getDividerLocation());
            getContentPane().remove(projectSplitPane);
            getContentPane().add(tabHolder, BorderLayout.CENTER);
            projView.dispose();
            projView = null;
            validate();
        }
    }

    private void closeProjectImpl() {
        setWaitCursor();
        try {
            if (project != null) {
                if (projView != null) {
                    // force pending view changes to apply now
                    // (such as updating project notes)
                    projView.setProject(null);
                }
                project.close();
                project = null;
                newEditorDialog.updateProjectStatus(null);
            }
        } finally {
            setDefaultCursor();
        }
    }

    @Override
    public ProjectView getOpenProjectView() {
        return projView;
    }

    private ProjectView projView;

    private void createProjectView(Project p, boolean showOpenAnimation) {
        if (projView == null) {
            projView = new ProjectView();
            if (projectSplitPane == null) {
                projectSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
                projectSplitPane.setDividerSize(8);
                projectSplitPane.setBorder(BorderFactory.createEmptyBorder());
            }
            projectSplitPane.setLeftComponent(projView);
            projectSplitPane.setRightComponent(tabHolder);

            getContentPane().add(projectSplitPane, BorderLayout.CENTER);

            projectSplitPane.setDividerLocation(0);
            String dividerPosValue = Settings.getShared().get(KEY_PROJECT_DIVIDER);
            final int dividerPos = dividerPosValue == null ? 300 : (Settings.integer(dividerPosValue) < 0 ? 300 : Settings.integer(dividerPosValue));

            if (showOpenAnimation) {
                new Animation(0.5f) {
                    @Override
                    public void composeFrame(float position) {
                        projectSplitPane.setDividerLocation((int) (dividerPos * position + 0.5f));
                    }
                }.play(KEY_PROJECT_DIVIDER);
            } else {
                projectSplitPane.setDividerLocation(dividerPos);
            }
        }
        projView.setProject(p);
        newEditorDialog.updateProjectStatus(p);
    }

    private JSplitPane projectSplitPane;
    private static final String KEY_PROJECT_DIVIDER = "project-divider-pos";

    @Override
    public StrangeEonsEditor[] getEditorsShowingFile(File f) {
        int matches = 0;
        StrangeEonsEditor[] eds = getEditors();
        for (int i = 0; i < eds.length; ++i) {
            File t = eds[i].getFile();
            if (t == null) {
                if (f == null) {
                    ++matches;
                }
            } else {
                if (t.equals(f)) {
                    ++matches;
                }
            }
        }

        int j = 0;
        StrangeEonsEditor[] match = new StrangeEonsEditor[matches];
        for (int i = 0; i < eds.length; ++i) {
            File t = eds[i].getFile();
            if (t == null) {
                if (f == null) {
                    match[j++] = eds[i];
                }
            } else {
                if (t.equals(f)) {
                    match[j++] = eds[i];
                }
            }
        }
        return match;
    }

    @Override
    public void openFile(final File file) {
        if (file == null) {
            throw new NullPointerException("null path");
        }
        SwingUtilities.invokeLater(() -> {
            setWaitCursor();
            try {
                addFileToOpenQueue(file);
                openFilesInQueue();
            } finally {
                setDefaultCursor();
            }
        });
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    private AbstractGameComponentEditor<? extends GameComponent> openGameComponentImpl(File f) {
        GameComponent gameComponent = ResourceKit.getGameComponentFromFile(f);
        if (gameComponent == null) {
            return null;
        }

        AbstractGameComponentEditor editor = gameComponent.createDefaultEditor();
        if (editor.getFrameIcon() == AbstractGameComponentEditor.DEFAULT_EDITOR_ICON) {
            editor.setFrameIcon(newEditorDialog.getIconForComponent(gameComponent));
        }

        RecentFiles.addRecentDocument(f);
        editor.handleOpenRequest(gameComponent, f);
        newEditorDialog.setVisible(false);
        return editor;
    }

    private static final BlockingQueue<String> fileOpenQueue = new LinkedBlockingQueue<>();

    /**
     * Adds a file specification to a threadsafe list of files to open. This is
     * called to add command line arguments, to implement single instance
     * support (a second instance will find this instance, pass along any files,
     * then exit; this supports opening documents from the desktop), and via
     * {@link #openFile}. This method is threadsafe.
     *
     * @param file the file path to open
     */
    static void addFileToOpenQueue(File file) {
        try {
            fileOpenQueue.put("F " + file.getAbsolutePath());
        } catch (InterruptedException e) {
            StrangeEons.log.log(Level.SEVERE, null, e);
        }
    }

    /**
     * Opens files that have been added to the queue via
     * {@link #addFileToOpenQueue(java.lang.String)}.
     */
    void openFilesInQueue() {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException("must be called from EDT");
        }

        int bundleFlags = 0;

        Open openAction = null;
        View viewAction = null;

        StrangeEonsEditor[] editors = getEditors();

        while (fileOpenQueue.size() > 0) {
            String fileSpec = null;
            try {
                fileSpec = fileOpenQueue.take();
            } catch (InterruptedException e) {
                // get it next time
            }
            if (fileSpec == null || fileSpec.length() < 2 || fileSpec.charAt(1) != ' ') {
                if (fileSpec != null) {
                    StrangeEons.log.log(Level.SEVERE, "bad queue syntax");
                }
                continue;
            }

            char entryType = fileSpec.charAt(0);
            fileSpec = fileSpec.substring(2);

            if (entryType == 'U') {
                // skip URLs open from previous build
//                APIBrowser browser = new APIBrowser(fileSpec);
//                browser.setURL(fileSpec);
//                StrangeEons.addEditor(browser);
                continue;
            }

            if (entryType != 'F') {
                StrangeEons.log.log(Level.SEVERE, "unknown type " + entryType);
                continue;
            }

            File file = new File(fileSpec);
            boolean alreadyOpen = false;
            if (file != null) {
                if (file.isDirectory() || Project.isProjectPackage(file)) {
                    // see if we can find a project somewhere in this chain
                    try {
                        File projRoot = file;
                        if (file.isDirectory()) {
                            while (projRoot != null && !Project.isProjectFolder(projRoot)) {
                                projRoot = projRoot.getParentFile();
                            }
                        }
                        if (projRoot != null) {
                            setOpenProject(projRoot);
                        }
                    } catch (IOException e) {
                        ErrorDialog.displayError(string("prj-err-open"), e);
                    }
                    continue;
                }

                // check if user double-clicked a plug-in
                if (PluginBundle.getBundleType(file) != PluginBundle.TYPE_UNKNOWN) {
                    Object[] options = new Object[]{string("plug-autoinstall-ok"), string("pt-import-plugin-name"), string("cancel")};
                    if (getOpenProject() == null || getOpenProject().findMember(file) != null) {
                        JButton disabledImport = new JButton(options[1].toString());
                        disabledImport.setEnabled(false);
                        options[1] = disabledImport;
                    }
                    int selection = JOptionPane.showOptionDialog(this, string("plug-autoinstall-prompt", file.getName()),
                            string("plug-autoinstall-title"), 0, JOptionPane.QUESTION_MESSAGE,
                            PluginBundle.getIcon(file, false), options, options[0]);

                    // clicked Cancel, short circuit out
                    if (selection == 2 || selection == JOptionPane.CLOSED_OPTION) {
                        continue;
                    }

                    // clicked Import, copy the file to the project and import it then leave
                    if (selection == 1) {
                        // copy the bundle into the project
                        Project openProj = getOpenProject();
                        File dest = new File(openProj.getFile(), file.getName());
                        dest = ProjectUtilities.getAvailableFile(dest);
                        try {
                            ProjectUtilities.copyFile(file, dest);
                        } catch (IOException e) {
                            ErrorDialog.displayError(string("prj-err-copy", file.getName()), e);
                            continue;
                        }
                        // import the bundle
                        Task importedTask = PluginImportTask.createTaskFromBundle(openProj, file);
                        // no problems, make sure the new task folder is visible
                        if (importedTask != null) {
                            openProj.synchronize();
                            continue;
                        }
                    }

                    if (selection == 0) {
                        try {
                            bundleFlags |= BundleInstaller.installPluginBundle(file);
                        } catch (IOException e) {
                            ErrorDialog.displayError(string("cat-err-install", file.getName()), e);
                            continue;
                        }
                    }
                    // avoid loading as .eon file in any event
                    continue;
                }

                // nope, not a plug-in: assume it is a file in .eon format
                // or a project file type that can be opened internally
                for (StrangeEonsEditor editor : editors) {
                    if (file.equals(editor.getFile())) {
                        alreadyOpen = true;
                        editor.select();
                        break;
                    }
                }
                if (!alreadyOpen) {
                    // doesn't look like a game component, try internal open first
                    if (!file.getName().endsWith(".eon")) {
                        if (openAction == null) {
                            openAction = (Open) Actions.getUnspecializedAction("open");
                            if (openAction == null) {
                                openAction = new Open();
                            }
                        }
                        try {
                            if (openAction.tryInternalOpen(project, null, file)) {
                                continue;
                            }
                        } catch (IOException e) {
                            ErrorDialog.displayError(string("app-err-open", file.getName()), e);
                            continue;
                        }
                        if (viewAction == null) {
                            viewAction = (View) Actions.getUnspecializedAction("view");
                            if (viewAction == null) {
                                viewAction = new View();
                            }
                        }
                        if (viewAction.tryInternalView(project, null, file)) {
                            continue;
                        }
                    }
                    // when all else fails, assume it's a game component
                    // regardless of extension
                    final StrangeEonsEditor ed = openGameComponentImpl(file);
                    if (ed != null) {
                        addEditor(ed);
                        editors = getEditors();
                    }
                }
            }
        }

        if (bundleFlags != 0) {
            BundleInstaller.finishBundleInstallation(bundleFlags);
            if ((bundleFlags & (BundleInstaller.INSTALL_FLAG_THEME | BundleInstaller.INSTALL_FLAG_EXTENSION)) != 0) {
                StringBuilder msg = new StringBuilder(140);
                if ((bundleFlags & BundleInstaller.INSTALL_FLAG_THEME) != 0) {
                    msg.append(string("plug-autoinstall-theme"));
                }
                if ((bundleFlags & BundleInstaller.INSTALL_FLAG_EXTENSION) != 0) {
                    if (msg.length() > 0) {
                        msg.append("<p>&nbsp;<p>");
                    }
                    msg.append(string("plug-autoinstall-extension"));
                }
                JOptionPane.showMessageDialog(this, msg.toString(),
                        string("plug-autoinstall-title"), JOptionPane.INFORMATION_MESSAGE);
            }
            if ((bundleFlags & BundleInstaller.INSTALL_FLAG_PLUGIN) != 0) {
                StrangeEons.getApplication().loadPlugins();
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutItem;
    private javax.swing.JPopupMenu.Separator aboutSeparator;
    private javax.swing.JMenuItem alignBottomItem;
    private javax.swing.JMenuItem alignCenterItem;
    private javax.swing.JMenuItem alignLeftItem;
    private javax.swing.JMenu alignMenu;
    private javax.swing.JMenuItem alignMiddleItem;
    private javax.swing.JMenuItem alignRightItem;
    private javax.swing.JMenuItem alignTopItem;
    private javax.swing.JMenu cardEdgeMenu;
    private javax.swing.JMenu convertMenu;
    private javax.swing.JMenuItem createExpansionItem;
    private javax.swing.JMenu deckMenu;
    private javax.swing.JDesktopPane desktopPane;
    private javax.swing.JMenu editMenu;
    private javax.swing.JTabbedPane editorTab;
    private javax.swing.JMenuItem exitItem;
    private javax.swing.JPopupMenu.Separator exitSeparator;
    private javax.swing.JPopupMenu.Separator expansionSeparator;
    private javax.swing.JMenu expansionSymbolMenu;
    private javax.swing.JMenuItem exportItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem findInProjectItem;
    private javax.swing.JMenuItem formatCodeItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JCheckBoxMenuItem inkSaverMenuItem;
    private javax.swing.JMenuItem insertCharsItem;
    private javax.swing.JPopupMenu.Separator jSeparator10;
    private javax.swing.JPopupMenu.Separator jSeparator11;
    private javax.swing.JPopupMenu.Separator jSeparator13;
    private javax.swing.JPopupMenu.Separator jSeparator17;
    private javax.swing.JPopupMenu.Separator jSeparator24;
    private javax.swing.JPopupMenu.Separator jSeparator25;
    private javax.swing.JPopupMenu.Separator jSeparator33;
    private javax.swing.JPopupMenu.Separator jSeparator35;
    private javax.swing.JPopupMenu.Separator jSeparator36;
    private javax.swing.JPopupMenu.Separator jSeparator37;
    private javax.swing.JPopupMenu.Separator jSeparator38;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JMenuItem managePluginsItem;
    private javax.swing.JMenuItem markupAbbreviationsItem;
    private javax.swing.JMenu markupMenu;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenu openRecentMenu;
    private javax.swing.JMenuItem pasteStyleItem;
    private javax.swing.JMenuItem pluginCatalogItem;
    private javax.swing.JMenuItem pluginUpdatesItem;
    private javax.swing.JMenuItem preferencesItem;
    private javax.swing.JPopupMenu.Separator preferencesSeparator;
    private javax.swing.JMenu snapMenu;
    private javax.swing.JMenuItem sortSourceItem;
    private javax.swing.JMenuItem sortSourceItem1;
    private javax.swing.JMenu sourceMenu;
    private javax.swing.JPanel tabHolder;
    private javax.swing.JMenu toolboxMenu;
    private javax.swing.JMenu variantMenu;
    private javax.swing.JCheckBoxMenuItem viewContextBarItem;
    private javax.swing.JCheckBoxMenuItem viewEdgeOutlinesItem;
    private javax.swing.JMenu viewMenu;
    private javax.swing.JCheckBoxMenuItem viewPortraitBoxesItem;
    private javax.swing.JCheckBoxMenuItem viewRegionBoxesItem;
    private javax.swing.JCheckBoxMenuItem viewUnsafeRegionsItem;
    private javax.swing.JMenu windowMenu;
    // End of variables declaration//GEN-END:variables

    /**
     * Installs an exception handler that tries to provide feedback during out
     * of memory conditions or when a script that has been converted to an
     * interface throws an exception.
     */
    static void installExceptionHandler() {
        // Install a default exception handler if this is the first instance
        // of the application created (normally there would only be one anyway)
        if (originalExceptionHandler == null) {
            Thread currentThread = Thread.currentThread();
            originalExceptionHandler = currentThread.getUncaughtExceptionHandler();
            currentThread.setUncaughtExceptionHandler((Thread t, Throwable e) -> {
                boolean handled = false;
                try {
                    if (e instanceof OutOfMemoryError) {
                        StrangeEons.log.log(Level.WARNING, null, e);
                        ErrorDialog.outOfMemory();
                        handled = true;
                    } else if ((e instanceof JavaScriptException) || (e instanceof RhinoException) || (e instanceof ScriptException)) {
                        ScriptMonkey.scriptError(e);
                        handled = true;
                    }
                } catch (Throwable ex) {
                }
                if (!handled) {
                    originalExceptionHandler.uncaughtException(t, e);
                }
            });
        }
    }
    private static UncaughtExceptionHandler originalExceptionHandler = null;

    private NewEditorDialog newEditorDialog;

    @Override
    public void startTracking(TrackedWindow window) {
        ((WindowMenu) windowMenu).trackWindow(window, true);
    }

    @Override
    public void stopTracking(TrackedWindow window) {
        ((WindowMenu) windowMenu).trackWindow(window, false);
    }

    /**
     * Return the main application frame, or null if none is registered. This is
     * used by dialogs to determine what their parent should be when no parent
     * is specified.
     *
     * @return the application frame, which is the concrete implementation of
     * {@link StrangeEonsAppWindow}
     */
    static AppFrame getApp() {
        return appFrame;
    }
    private static AppFrame appFrame;

    @Override
    public String toString() {
        return "StrangeEonsApplicationWindow: ["
                + "size:" + getWidth() + "x" + getHeight()
                + ", editors: " + getEditors().length
                + "]";
    }

    @Override
    public void suggestRestart(final String message) {
        Restarter.offer(message);
    }

    /**
     * Returns the desktop that hosts attached editors. (This method is needed
     * by the T* helper classes such as {@link TAttachedEditor}.)
     *
     * @return the desktop component
     */
    JDesktopPane getDesktop() {
        return desktopPane;
    }

    /**
     * Inserts a menu item at a standard location in the application menu. This
     * is a convenient way to add new items to a menu. If you want more control
     * over menu placement, or to scan or replace existing items, see the
     * <tt>uiutils</tt> scripting library.
     *
     * @param parent an identifier for the menu you wish to insert into
     * @param item the item you wish to insert; this should either be a
     * separator or a menu item
     * @since 2.1a8
     */
    @Override
    public void addMenuItem(AppMenu parent, JComponent item) {
        initMenuGrid();

        final int MENUS = 0, PARENT = 1, INSERT = 2, SEPS = 3, COUNT = 4;
        final int MENU_COUNT = appMenuGrid[0].length;

        // locate the index in appMenuGrid for parent
        int m = 0;
        for (; m < MENU_COUNT; ++m) {
            if (parent == appMenuGrid[MENUS][m]) {
                break;
            }
        }
        if (m == MENU_COUNT) {
            throw new AssertionError("not in appMenuGrid: " + parent);
        }

        // check that the item isn't already there
        JMenu menu = (JMenu) appMenuGrid[PARENT][m];
        if (item.getParent() == menu) {
            return;
        }

        // find the standard menu item after which new items are to be inserted
        int i = 0;
        for (; i < menu.getMenuComponentCount(); ++i) {
            if (menu.getMenuComponent(i) == appMenuGrid[INSERT][m]) {
                break;
            }
        }
        if (i == menu.getMenuComponentCount()) {
            throw new AssertionError("missing insert item in appMenuGrid: " + parent);
        }

        ++i; // skip past tagged item to insertion point
        // if no items added yet, add separator
        if (((JSeparator) appMenuGrid[SEPS][m]).getParent() == null) {
            menu.add((JSeparator) appMenuGrid[SEPS][m], i);
        }
        ++i; // skip past our separator

        // skip past already added items and update count
        int count = (Integer) appMenuGrid[COUNT][m];
        i += count;

        menu.add(item, i);
        appMenuGrid[COUNT][m] = ++count;
    }

    /**
     * Removes a menu item previously added with {@link #addMenuItem}. If you
     * have added an item to a menu with {@code addMenuItem}, you must use this
     * method to remove that item before changing the item's parent or the
     * application menus may be corrupted.
     *
     * @param parent an identifier for the menu you inserted this menu item into
     * @param item the item you wish to remove
     * @since 2.1a8
     */
    @Override
    public void removeMenuItem(AppMenu parent, JComponent item) {
        if (appMenuGrid == null) {
            return;
        }

        final int MENUS = 0, PARENT = 1, INSERT = 2, SEPS = 3, COUNT = 4;
        final int MENU_COUNT = appMenuGrid[0].length;

        // locate the index in appMenuGrid for parent
        int m = 0;
        for (; m < MENU_COUNT; ++m) {
            if (parent == appMenuGrid[MENUS][m]) {
                break;
            }
        }
        if (m == MENU_COUNT) {
            throw new AssertionError("not in appMenuGrid: " + parent);
        }

        // check that the item was previously installed
        JMenu menu = (JMenu) appMenuGrid[PARENT][m];
        int i = 0;
        for (; i < menu.getMenuComponentCount(); ++i) {
            if (menu.getMenuComponent(i) == item) {
                break;
            }
        }
        if (i == menu.getMenuComponentCount()) {
            return;
        }

        menu.remove(i);

        // if this was the last item, also remove the separator
        int count = (Integer) appMenuGrid[COUNT][m];

        if (--count <= 0) {
            appMenuGrid[COUNT][m] = 0;

            for (i = 0; i < menu.getMenuComponentCount(); ++i) {
                if (menu.getMenuComponent(i) == appMenuGrid[SEPS][m]) {
                    break;
                }
            }
            if (i == menu.getMenuComponentCount()) {
                return;
            }

            menu.remove(i);
        }
        appMenuGrid[COUNT][m] = count;
    }

    private Object[][] appMenuGrid;

    /**
     * Initializes {@code appMenuGrid} with an array that determines where new
     * menu items will be added by {@link #addMenuItem}.
     */
    private void initMenuGrid() {
        if (appMenuGrid != null) {
            return;
        }
        appMenuGrid = new Object[][]{
            new AppMenu[]{AppMenu.FILE, AppMenu.EDIT, AppMenu.VIEW, AppMenu.EXPANSION, AppMenu.MARKUP, AppMenu.DECK, AppMenu.SOURCE, AppMenu.TOOLBOX, AppMenu.HELP},
            new JMenu[]{fileMenu, editMenu, viewMenu, expansionSymbolMenu, markupMenu, deckMenu, sourceMenu, toolboxMenu, helpMenu},
            new JMenuItem[]{exportItem, findInProjectItem, viewContextBarItem, createExpansionItem, insertCharsItem, pasteStyleItem, sortSourceItem, pluginUpdatesItem, aboutItem},
            new JSeparator[]{sep(), sep(), sep(), sep(), sep(), sep(), sep(), sep(), sep()},
            new Integer[]{0, 0, 0, 0, 0, 0, 0, 0, 0}
        };
    }

    /**
     * Returns a new menu separator. Helper used to init the menu grid.
     */
    private static JSeparator sep() {
        return new JPopupMenu.Separator();
    }

    /**
     * Re-arranges menus and installs OS-specific handling for MacOS.
     */
    private void installMacOsDesktopHandlers() {
        Desktop desktop = Desktop.getDesktop();
        desktop.setDefaultMenuBar(menuBar);
        desktop.setAboutHandler(ev -> this.showAboutDialog());
        desktop.setOpenFileHandler(ev -> ev.getFiles().forEach(this::openFile));
        desktop.setPreferencesHandler(ev -> this.showPreferencesDialog(this, null));
        desktop.setQuitStrategy(QuitStrategy.CLOSE_ALL_WINDOWS);
        desktop.setQuitHandler((ev, response) -> {
            try {
                if(!exitApplication(false)) {
                    response.cancelQuit();
                    return;
                }
            } catch(Exception ex) {
                // user still chose to quit
            }
            response.performQuit();
        });

        exitSeparator.setVisible(false);
        exitItem.setVisible(false);
        aboutSeparator.setVisible(false);
        aboutItem.setVisible(false);
        preferencesSeparator.setVisible(false);
        preferencesItem.setVisible(false);
    }
}
