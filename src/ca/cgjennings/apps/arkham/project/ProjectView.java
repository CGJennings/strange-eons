package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.BusyDialog;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsAppWindow;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.i18n.PatternExceptionLocalizer;
import ca.cgjennings.io.ConnectionSupport;
import ca.cgjennings.io.FileChangeListener;
import ca.cgjennings.io.FileChangeMonitor;
import ca.cgjennings.io.FileChangeMonitor.ChangeType;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.spelling.ui.JSpellingTextArea;
import ca.cgjennings.ui.BlankIcon;
import ca.cgjennings.ui.DocumentEventAdapter;
import ca.cgjennings.ui.IconProvider;
import ca.cgjennings.ui.JLabelledField;
import ca.cgjennings.ui.StyleUtilities;
import ca.cgjennings.ui.TreeLabelExposer;
import ca.cgjennings.ui.anim.AnimationUtilities;
import ca.cgjennings.ui.text.ErrorSquigglePainter;
import ca.cgjennings.ui.theme.Theme;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter.HighlightPainter;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import resources.Language;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * A view that displays the open project.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public final class ProjectView extends javax.swing.JPanel {

    /**
     * The property name of a property change event that is fired when this view
     * is about to close. To listen for the active project being closed, rather
     * than a specific view, register a {@code ProjectEventListener} with
     * the application window instead.
     *
     * @see ca.cgjennings.apps.arkham.StrangeEonsAppWindow.ProjectEventListener
     */
    public static final String VIEW_CLOSING_PROPERTY = "view-closing";

    /**
     * Project property that stores list of open folders.
     */
    private static final String PROJECT_OPEN_FOLDER_LIST = "open-folders";
    /**
     * Project property that stores list of selected objects.
     */
    private static final String PROJECT_SELECTION_LIST = "selected-members";
    /**
     * Project property that stores the topmost visible row.
     */
    private static final String PROJECT_FIRST_ROW = "first-row";

    /**
     * Returns the view of the currently open project, or {@code null} if
     * there is no open project.
     *
     * @return the view of the open project
     */
    static ProjectView getCurrentView() {
        final StrangeEons se = StrangeEons.getApplication();
        if (se == null) {
            return null;
        }
        final StrangeEonsAppWindow af = StrangeEons.getWindow();
        if (af == null) {
            return null;
        }
        return af.getOpenProjectView();
    }

    private Project project;

    /**
     * Creates a new project view.
     */
    public ProjectView() {
        initComponents();
        new TreeLabelExposer(projTree);
        projTree.setTransferHandler(new ProjectTransferHandler());
        Action cut = new AbstractAction("cut") {
            @Override
            public void actionPerformed(ActionEvent e) {
                cut();
            }
        };
        projTree.getActionMap().put("cut", cut);
        projTree.setFocusable(true);

        putClientProperty(Commands.HELP_CONTEXT_PROPERTY, "proj-intro");
        findPanel.putClientProperty(Commands.HELP_CONTEXT_PROPERTY, "proj-search");

        // for some reason the NetBeans form editor keeps setting this to false
        closeFindBtn.setOpaque(true);

        addTaskBtn.setAutoFollowLinks(false);
        projTree.setCellRenderer(new Renderer());
        Dimension d = getMinimumSize();
        d.width = 0;
        setMinimumSize(d);

        addDefaultShortcuts();

        // init property table
        final PropertyTable table = new PropertyTable();
        properties.setModel(table);
        properties.setDefaultRenderer(Object.class, new PropertyRenderer());

        projTree.addTreeSelectionListener((TreeSelectionEvent e) -> {
            TreePath path = e.getNewLeadSelectionPath();
            if (path == null) {
                table.showPropertiesFor(null);
            } else {
                table.showPropertiesFor(treePathToMember(path));
            }
            fireProjectSelectionChanged();
        });
        table.showPropertiesFor(project);
        initKeyColumnWidth(properties, table);

        // try to restore previous UI layout
        Settings s = Settings.getUser();
        final String divPos = s.get(KEY_NOTES_DIVIDER);

        // a hack to ensure that a proportional size can be set
        splitPane.setDividerLocation(-1);
        if (divPos != null) {
            try {
                final int n = Settings.integer(divPos);
                if (n >= 0) {
                    splitPane.setDividerLocation(n);
                }
            } catch (Settings.ParseError e) {
            }
        }

        ((JLabelledField) projFindField).setLabel(string("prj-l-search-project"));
        findPanel.setVisible(Settings.yesNo(s.get(KEY_SHOW_SEARCH, "no")));

        searchCaseSense.setSelected(Settings.yesNo(s.get(KEY_FIP_CASE_SENSE, "no")));
        searchRegExp.setSelected(Settings.yesNo(s.get(KEY_FIP_REG_EXP, "no")));
        projFindField.getDocument().addDocumentListener(new DocumentEventAdapter() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                createPattern();
            }
        });

        try {
            createViewTabs();
        } catch (Exception e) {
            StrangeEons.log.log(Level.SEVERE, null, e);
        }
    }

    private void createViewTabs() {
        if (viewTabs != null) {
            StrangeEons.log.log(Level.INFO, "Installing view tabs for project {0}", project);
            for (ViewTab vt : viewTabs) {
                final String name = vt.getViewTabName();
                StrangeEons.log.log(Level.FINE, "Creating tab {0}", name);
                Component view = vt.createViewForProject(this, project);
                view.setName(name);
                tabs.add(vt.getLabel(), view);
            }
        }
        propsScroll.setName(ID_PROPERTIES);
        noteScroll.setName(ID_NOTES);

        final String tabID = Settings.getUser().get(KEY_NOTES_TAB);
        if (tabID != null) {
            for (int t = 0; t < tabs.getTabCount(); ++t) {
                final Component viewTabComponent = tabs.getComponentAt(t);
                if (tabID.equals(viewTabComponent.getName())) {
                    tabs.setSelectedIndex(t);
                    return;
                }
            }
            StrangeEons.log.log(Level.WARNING, "Default project view tab {0} missing: plug-in not installed?", tabID);
        }
    }

    private static final String KEY_SHOW_SEARCH = "show-find-in-project";

    public void setFindInProjectsVisible(boolean visible) {
        Settings.getUser().set(KEY_SHOW_SEARCH, visible ? "yes" : "no");
        if (visible) {
            boolean wasVisible = findPanel.isVisible();
            findPanel.setVisible(true);
            projFindField.requestFocusInWindow();
            if (wasVisible) {
                AnimationUtilities.attentionFlash(projFindField, null);
            }
        } else {
            closeFindBtnActionPerformed(null);
        }
    }

    private void initKeyColumnWidth(JTable table, PropertyTable model) {
        int cellWidth = 0;

        TableCellRenderer tcr = table.getDefaultRenderer(model.getColumnClass(0));
        for (int i = 0; i < model.getRowCount(); ++i) {
            Component rc = tcr.getTableCellRendererComponent(table, model.getValueAt(i, 0), false, false, i, 0);
            cellWidth = Math.max(cellWidth, rc.getPreferredSize().width);
        }

        JTableHeader th = properties.getTableHeader();
        th.getColumnModel().getColumn(0).setPreferredWidth(cellWidth);
        Font thf = th.getFont();
        thf = thf.deriveFont(Font.BOLD, thf.getSize2D() - 1f);
        th.setFont(thf);
    }

    public void setProject(Project project) {
        if (this.project == project) {
            return;
        }
        if (this.project != null) {
            try {
                storeTreeState();
            } catch (Throwable t) {
                StrangeEons.log.log(Level.WARNING, null, t);
            }
            this.project.getSettings().set("notes", noteField.getText());
            this.project.setJTreeModel(null);
        }
        if (project != null) {
            this.project = project;
            DefaultTreeModel model = new DefaultTreeModel(project.asTreeNode());
            projTree.setModel(model);
            project.setJTreeModel(model);
            noteField.setText(project.getSettings().get("notes"));

            try {
                recoverTreeState();
            } catch (Exception e) {
                StrangeEons.log.log(Level.SEVERE, null, e);
            }
            if (projTree.getSelectionCount() == 0) {
                select(project);
            }
        }
    }

    public Project getProject() {
        return project;
    }

    /**
     * Updates the visual representation of the specified member.
     *
     * @param m the member whose visual representation should be repainted
     */
    public void repaint(Member m) {
        if (m != null) {
            TreePath tp = memberToTreePath(m);
            int row = projTree.getRowForPath(tp);
            if (row >= 0) {
                // getRowBounds calls getPathBounds internally
                Rectangle r = projTree.getPathBounds(tp);
                if (r != null) {
                    projTree.repaint(r);
                }
            }
        }
    }

    /**
     * Scrolls the view, if required, so that the specified member is visible.
     *
     * @param m the member to scroll into view
     */
    public void ensureVisible(Member m) {
        projTree.scrollPathToVisible(memberToTreePath(m));
    }

    /**
     * Selects the specified member in the project view.
     *
     * @param m the member to select
     */
    public void select(Member m) {
        projTree.setSelectionPath(memberToTreePath(m));
    }

    /**
     * Selects the specified member in the project view without clearing the
     * current selection.
     *
     * @param m the member to add to the current selection
     */
    public void addToSelection(Member m) {
        projTree.getSelectionModel().addSelectionPath(memberToTreePath(m));
    }

    private TreePath memberToTreePath(Member m) {
        int dist = 0;
        Member temp = m;
        while (temp != null) {
            ++dist;
            temp = temp.getParent();
        }
        Object[] path = new Object[dist];
        while (m != null) {
            path[--dist] = m.asTreeNode();
            m = m.getParent();
        }
        return new TreePath(path);
    }

    /**
     * Clears the current project member selection.
     */
    public void clearSelection() {
        projTree.clearSelection();
    }

    /**
     * Returns an array of the currently selected members. If no members are
     * selected, an empty array is returned.
     *
     * @return an array of the selected members
     */
    public Member[] getSelectedMembers() {
        TreePath[] sel = projTree.getSelectionPaths();
        if (sel == null) {
            return EMPTY_SELECTION;
        }
        return treePathsToMembers(sel);
    }
    private final Member[] EMPTY_SELECTION = new Member[0];

    /**
     * Adds a new selection listener that will be notified when then project
     * selection changes.
     *
     * @param li the listener to add
     * @see #removeSelectionListener
     */
    public void addSelectionListener(SelectionListener li) {
        listenerList.add(SelectionListener.class, li);
    }

    /**
     * Removes a previously registered selection listener.
     *
     * @param li the listener to remove
     * @see #addSelectionListener
     */
    public void removeSelectionListener(SelectionListener li) {
        listenerList.remove(SelectionListener.class, li);
    }

    /**
     * Notifies registered listeners that the selection has changed.
     */
    protected void fireProjectSelectionChanged() {
        ProjectViewEvent event = null;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == SelectionListener.class) {
                if (event == null) {
                    event = new ProjectViewEvent(this);
                }
                ((SelectionListener) listeners[i + 1]).projectSelectionChanged(event);
            }
        }
    }

    /**
     * A listener that is notified of changes to the project selection.
     */
    public static interface SelectionListener extends EventListener {

        void projectSelectionChanged(ProjectViewEvent pve);
    }

    /**
     * The event object supplied to listeners of project view events when an
     * event is fired.
     */
    public static class ProjectViewEvent {

        private ProjectView v;
        private Member[] selection;

        /**
         * Creates a new project view event.
         *
         * @param view the view to create an event for
         */
        public ProjectViewEvent(ProjectView view) {
            if (view == null) {
                throw new NullPointerException("view");
            }
            v = view;
            selection = v.getSelectedMembers();
        }

        /**
         * Returns the view associated with this event.
         *
         * @return the project view in which the selection changed
         */
        public ProjectView getSource() {
            return v;
        }

        /**
         * Returns the project associated with this event.
         *
         * @return the project displayed in the view
         */
        public Project getProject() {
            return v.getProject();
        }

        /**
         * Returns the selection at the time occurred.
         *
         * @return a copy of the selected members
         */
        public Member[] getSelection() {
            if (selection.length == 0) {
                return selection;
            } else {
                return selection.clone();
            }
        }
    }

    /**
     * If the specified member is a folder, task, or project with children, the
     * member's visual representation will be expanded to make those children
     * visible.
     *
     * @param m the member whose children should be displayed
     * @see #collapseFolder
     * @see #isFolderCollpased
     */
    public void expandFolder(Member m) {
        projTree.expandPath(memberToTreePath(m));
    }

    /**
     * If the specified member is a folder, task, or project with children, the
     * member's visual representation will be collapsed to make those children
     * invisible.
     *
     * <p>
     * If the view displays project members as a tree with nodes that can be
     * opened and closed to display or hide their contents, then this method
     * closes the tree node that represents the member. Other possible view
     * implementations might use a different representation for tree nodes, in
     * which case this method will have a different visual interpretation or may
     * even have no effect at all.
     *
     * @param m the member whose children should be displayed
     */
    public void collapseFolder(Member m) {
        projTree.collapsePath(memberToTreePath(m));
    }

    /**
     * Returns {@code true} if the specified member is a folder, task, or
     * project with children, and the member's visual representation is
     * currently collapsed.
     *
     * @param m the member whose status is being queried
     * @return {@code true} if the member is visually collapsed
     */
    public boolean isFolderCollpased(Member m) {
        return projTree.isCollapsed(memberToTreePath(m));
    }

    /**
     * Collapses the specified folder and all of its children.
     *
     * @param folder the folder to collapse
     * @see #collapseFolder
     */
    public void collapseAll(Member folder) {
        if (!folder.hasChildren()) {
            return;
        }
        for (int i = 0; i < folder.getChildCount(); ++i) {
            if (folder.getChildAt(i).hasChildren()) {
                collapseAll(folder.getChildAt(i));
            }
        }
        collapseFolder(folder);
    }

    /**
     * Expands the specified folder and all of its children.
     *
     * @param folder the folder to expand
     * @see #expandFolder
     */
    public void expandAll(Member folder) {
        if (!folder.hasChildren()) {
            return;
        }
        for (int i = 0; i < folder.getChildCount(); ++i) {
            if (folder.getChildAt(i).hasChildren()) {
                expandAll(folder.getChildAt(i));
            }
        }
        expandFolder(folder);
    }

    /**
     * Copies the currently selected members to the clipboard as a file
     * selection.
     */
    public void copy() {
        TransferHandler.getCopyAction().actionPerformed(
                new ActionEvent(projTree, 0, "copy")
        );
    }

    /**
     * Cuts the currently selected members to the clipboard as a file selection.
     * Note that the members are not deleted immediately, but will only be
     * deleted if the clipboard contents are pasted. On some platforms, cut will
     * only work delete the original files when they are pasted within the
     * application; otherwise they will be copied instead.
     */
    public void cut() {
        setCutList(getSelectedMembers());
        ((ProjectTransferHandler) projTree.getTransferHandler()).isCutting = true;
        try {
            copy();
        } finally {
            ((ProjectTransferHandler) projTree.getTransferHandler()).isCutting = false;
        }
    }

    private void setCutList(Member[] clipping) {
        if (cutList == clipping) {
            return;
        }
        if (clipping == null) {
            cutList = null;
            cutSet = null;
        } else {
            cutList = ProjectUtilities.merge(clipping);
            cutSet = new HashSet<>(cutList.length * 2);
            for (Member m : cutList) {
                buildCutList(m);
            }
        }
        repaint();
    }

    private void buildCutList(Member kid) {
        cutSet.add(kid.getFile());
        if (kid.hasChildren()) {
            for (Member grandkid : kid.getChildren()) {
                buildCutList(grandkid);
            }
        }
    }
    private Member[] cutList;
    private HashSet<File> cutSet;

    private boolean isOnCutList(Member m) {
        if (cutList == null) {
            return false;
        }
        return cutSet.contains(m.getFile());
    }

    /**
     * Pastes the current clipboard contents into the selected project member,
     * is possible. The following clipboard data types can be pasted into a
     * project:
     * <dl>
     * <dt>files</dt>
     * <dd>The appropriate file is copied into the destination folder.</dd>
     * <dt>images</dt>
     * <dd>The image will be written into a new <tt>.png</tt> image file. Be
     * aware that some operating systems (notably Windows) do not include
     * transparency information (the alpha channel) for images on the clipboard.
     * Typically this means that if the copied image has transparency it will be
     * placed on the clipboard as if drawn over a solid black background).</dd>
     * <dt>URLs (as copied text)</dt>
     * <dd>If the text on the clipboard appears to be a URL, then the contents
     * of the URL will be downloaded and written to a new file in the
     * folder.</dd>
     * </dl>
     */
    public void paste() {
        TransferHandler.getPasteAction().actionPerformed(
                new ActionEvent(projTree, 0, "paste")
        );
    }

    /**
     * Place a window near the (inferred) locus of attention in the project
     * view. This would be used, for example, to open a small editor or dialog
     * box in response to the user selecting a command in the project view. This
     * is a convenince for {@code moveToLocusOfAttention( w, 0 )}.
     *
     * @param window the window to position
     */
    public void moveToLocusOfAttention(Window window) {
        moveToLocusOfAttention(window, 0);
    }

    /**
     * Place a window near the (inferred) locus of attention in the project
     * view. This would be used, for example, to open a small editor or dialog
     * box in response to the user selecting a command in the project view.
     *
     * @param window the window to position
     * @param cascade if positioning multiple related windows, use this to
     * offset the resulting windows
     */
    public void moveToLocusOfAttention(Window window, int cascade) {
        // infer a locus of attention (LoA) from the state of the view
        int lastSelectedRow = projTree.getSelectionModel().getLeadSelectionRow();
        if (lastSelectedRow < 0) {
            lastSelectedRow = 0;
        }

        projTree.scrollRowToVisible(lastSelectedRow);

        Rectangle bounds = projTree.getRowBounds(lastSelectedRow);
        Rectangle treeBounds = projTree.getBounds();
        bounds.x = treeBounds.x;
        bounds.width = treeBounds.width;

        int offset = Math.max(16, bounds.width * 2 / 3);

        int x = bounds.x + offset;
        int y = bounds.y + bounds.height / 2 - offset;

        // center small windows vertically around locus
        // (default is to move top a little above locus)
        if (window.getHeight() < offset * 2) {
            y = y + offset - window.getHeight() / 2;
        }

        // translate (LoA) to screen coordinates
        Point los = projTree.getLocationOnScreen();
        x += los.x + cascade * 20;
        y += los.y + cascade * 20;

        // restrict to screen bounds
        // get bounds of screen(s) that the app window is on
        StrangeEonsAppWindow app = StrangeEons.getWindow();
        Rectangle ss = app.getGraphicsConfiguration().getBounds();

        // adjust for system elements (task bar, system menu bar, etc.)
        Insets insets = app.getToolkit().getScreenInsets(app.getGraphicsConfiguration());
        ss.x += insets.left;
        ss.width -= (insets.left + insets.right);
        ss.y += insets.top;
        ss.height -= (insets.top + insets.bottom);

        if (x + window.getWidth() >= ss.x + ss.width) {
            x = (ss.x + ss.width) - window.getWidth() - 1;
        }
        if (x < ss.x) {
            x = ss.x;
        }

        if (y + window.getHeight() >= ss.y + ss.height) {
            y = (ss.y + ss.height) - window.getHeight() - 1;
        }
        if (y < ss.y) {
            y = ss.y;
        }

        window.setLocation(x, y);
    }

    private void addDefaultShortcuts() {
        setAccelerator(Actions.findActionByName("open"), "ENTER", false);
        setAccelerator(Actions.findActionByName("delete"), "DELETE", true);
        setAccelerator(Actions.findActionByName("delete"), "DELETE", false);
        setAccelerator(Actions.findActionByName("rename"), "F2", false);
        setAccelerator(Actions.findActionByName("rename"), "R", true);
        setAccelerator(Actions.findActionByName("debug"), "F3", false);
        setAccelerator(Actions.findActionByName("run"), "F5", false);
        setAccelerator(Actions.findActionByName("scriptedfactorybuild"), "F9", false);
//		setAccelerator( Actions.findActionByName( "factorybuild" ),  "shift F9", false );
//		setAccelerator( Actions.findActionByName( "factorybuildall" ),  "F9", false );
        setAccelerator(Actions.findActionByName("clean"), "shift F11", false);

        Action expandCollapseAll = new AbstractAction("Expand/Collapse All") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Member[] sel = getSelectedMembers();

                // heuristic when only selected item has no children: use parent instead
                if (sel.length == 1 && !sel[0].hasChildren() && sel[0].getParent() != null) {
                    sel[0] = sel[0].getParent();
                    select(sel[0]);
                }

                // find the first member with children and use it to decide
                // if we are collapsing or expanding, then collapse or expand
                // each member recursively
                boolean expand = true;
                boolean found = false;
                for (int i = 0; i < sel.length; ++i) {
                    if (!sel[i].hasChildren()) {
                        continue;
                    }

                    if (!found) {
                        expand = isFolderCollpased(sel[i]);
                        found = true;
                    }
                    if (expand) {
                        expandAll(sel[i]);
                    } else {
                        collapseAll(sel[i]);
                    }
                }
            }
        };

        InputMap imap = projTree.getInputMap();
        ActionMap amap = projTree.getActionMap();
        amap.put("EXPAND_COLLAPSE_ALL", expandCollapseAll);
        imap.put(KeyStroke.getKeyStroke("SPACE"), "EXPAND_COLLAPSE_ALL");
    }

    /**
     * Sets an accelerator used by this view for a {@code TaskAction}. If
     * {@code command} is true, then a key stroke should represent a plain
     * key (such as "F1"); a modifier key prefix will be added automatically
     * based on the platform.
     *
     * @param ta the action to set an accelerator for
     * @param keystroke a description of the accelerator key
     * @param command whether modifiers should be generated automatically
     */
    public void setAccelerator(TaskAction ta, String keystroke, boolean command) {
        Action a = createActionWrapper(ta);
        InputMap imap = projTree.getInputMap();
        ActionMap amap = projTree.getActionMap();
        String name = ta.getActionName();
        amap.put(name, a);

        KeyStroke accel;
        if (command) {
            KeyStroke winKey = KeyStroke.getKeyStroke("ctrl " + keystroke);
            imap.put(winKey, name);
            KeyStroke osxKey = KeyStroke.getKeyStroke("meta " + keystroke);
            imap.put(osxKey, name);
            accel = PlatformSupport.PLATFORM_IS_MAC ? osxKey : winKey;
        } else {
            accel = KeyStroke.getKeyStroke(keystroke);
            imap.put(accel, name);
        }
        Actions.setAcceleratorHint(ta, accel);
    }

    private Action createActionWrapper(final TaskAction ta) {
        return new AbstractAction() {
            @Override
            public boolean isEnabled() {
                Member[] m = getSelectedMembers();
                return ta.appliesToSelection(m);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                StrangeEons.setWaitCursor(true);
                try {
                    Member[] m = getSelectedMembers();
                    if (ta.appliesToSelection(m)) {
                        ta.performOnSelection(m);
                    }
                } finally {
                    StrangeEons.setWaitCursor(false);
                }
            }
        };
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        searchPopup = new javax.swing.JPopupMenu();
        searchCopyItem = new javax.swing.JMenuItem();
        searchPasteItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        searchCaseSense = new javax.swing.JCheckBoxMenuItem();
        searchRegExp = new javax.swing.JCheckBoxMenuItem();
        splitPane = new javax.swing.JSplitPane();
        treePanel = new javax.swing.JPanel();
        javax.swing.JScrollPane treeScroll = new javax.swing.JScrollPane();
        projTree = new javax.swing.JTree();
        titlePanel = new javax.swing.JPanel();
        treeLabel = new javax.swing.JLabel();
        addTaskBtn = new ca.cgjennings.ui.JLinkLabel();
        toolCloseBtn = new ca.cgjennings.apps.arkham.ToolCloseButton();
        findPanel = new javax.swing.JPanel();
        findIcon = new javax.swing.JLabel();
        projFindField =  new JLabelledField() ;
        ((JLabelledField) projFindField).setTextForeground(UIManager.getColor(Theme.PROJECT_FIND_FOREGROUND));
        closeFindBtn = new ca.cgjennings.apps.arkham.ToolCloseButton();
        notePanel = new javax.swing.JPanel();
        tabs = new javax.swing.JTabbedPane();
        StyleUtilities.mini( tabs );
        propsScroll = new javax.swing.JScrollPane();
        properties =  new HeaderlessTable() ;
        noteScroll = new javax.swing.JScrollPane();
        noteField =  new JSpellingTextArea() ;

        searchPopup.setName("searchPopup"); // NOI18N

        searchCopyItem.setText(string( "copy" )); // NOI18N
        searchCopyItem.setName("searchCopyItem"); // NOI18N
        searchCopyItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchCopyItemActionPerformed(evt);
            }
        });
        searchPopup.add(searchCopyItem);

        searchPasteItem.setText(string( "paste" )); // NOI18N
        searchPasteItem.setName("searchPasteItem"); // NOI18N
        searchPasteItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchPasteItemActionPerformed(evt);
            }
        });
        searchPopup.add(searchPasteItem);

        jSeparator1.setName("jSeparator1"); // NOI18N
        searchPopup.add(jSeparator1);

        searchCaseSense.setSelected(true);
        searchCaseSense.setText(string( "find-case-sense" )); // NOI18N
        searchCaseSense.setName("searchCaseSense"); // NOI18N
        searchCaseSense.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findCheckActionPerformed(evt);
            }
        });
        searchPopup.add(searchCaseSense);

        searchRegExp.setSelected(true);
        searchRegExp.setText(string( "find-reg-exp" )); // NOI18N
        searchRegExp.setName("searchRegExp"); // NOI18N
        searchRegExp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findCheckActionPerformed(evt);
            }
        });
        searchPopup.add(searchRegExp);

        setName("Form"); // NOI18N
        setLayout(new java.awt.BorderLayout());

        splitPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        splitPane.setDividerSize(8);
        splitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.66);
        splitPane.setName("splitPane"); // NOI18N
        splitPane.setOneTouchExpandable(true);
        splitPane.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                splitPanePropertyChange(evt);
            }
        });

        treePanel.setName("treePanel"); // NOI18N
        treePanel.setLayout(new java.awt.BorderLayout());

        treeScroll.setBorder(null);
        treeScroll.setName("treeScroll"); // NOI18N

        projTree.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        projTree.setDragEnabled(true);
        projTree.setDropMode(javax.swing.DropMode.ON);
        projTree.setName("projTree"); // NOI18N
        projTree.setShowsRootHandles( false );
        projTree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                projTreeMouseClicked(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                projTreeMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                projTreeMouseReleased(evt);
            }
        });
        treeScroll.setViewportView(projTree);

        treePanel.add(treeScroll, java.awt.BorderLayout.CENTER);

        titlePanel.setName("titlePanel"); // NOI18N
        titlePanel.setLayout(new java.awt.GridBagLayout());

        treeLabel.setBackground(UIManager.getColor(Theme.PROJECT_HEADER_BACKGROUND));
        treeLabel.setFont(treeLabel.getFont().deriveFont(treeLabel.getFont().getStyle() | java.awt.Font.BOLD));
        treeLabel.setForeground(java.awt.Color.white);
        treeLabel.setText(string( "prj-l-view-tree" )); // NOI18N
        treeLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 4, 2, 4));
        treeLabel.setName("treeLabel"); // NOI18N
        treeLabel.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        titlePanel.add(treeLabel, gridBagConstraints);

        addTaskBtn.setBackground(UIManager.getColor(Theme.PROJECT_HEADER_BACKGROUND));
        addTaskBtn.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createMatteBorder(0, 1, 0, 1, new java.awt.Color(99, 99, 99)), javax.swing.BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        addTaskBtn.setForeground(new java.awt.Color(57, 120, 171));
        addTaskBtn.setText(string( "pa-add-task" )); // NOI18N
        addTaskBtn.setFont(addTaskBtn.getFont().deriveFont(addTaskBtn.getFont().getStyle() | java.awt.Font.BOLD, addTaskBtn.getFont().getSize()-1));
        addTaskBtn.setName("addTaskBtn"); // NOI18N
        addTaskBtn.setOpaque(true);
        addTaskBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                addTaskBtnMousePressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weighty = 1.0;
        titlePanel.add(addTaskBtn, gridBagConstraints);

        toolCloseBtn.setBackground(UIManager.getColor(Theme.PROJECT_HEADER_BACKGROUND));
        toolCloseBtn.setName("toolCloseBtn"); // NOI18N
        toolCloseBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toolCloseBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weighty = 1.0;
        titlePanel.add(toolCloseBtn, gridBagConstraints);

        treePanel.add(titlePanel, java.awt.BorderLayout.NORTH);

        findPanel.setBackground(UIManager.getColor(Theme.PROJECT_FIND_BACKGROUND));
        findPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, new java.awt.Color(0, 0, 0)));
        findPanel.setComponentPopupMenu(searchPopup);
        findPanel.setName("findPanel"); // NOI18N
        findPanel.setLayout(new java.awt.GridBagLayout());

        findIcon.setIcon( ResourceKit.getIcon( "ui/find-sm.png" ) );
        findIcon.setMinimumSize(new java.awt.Dimension(11, 10));
        findIcon.setName("findIcon"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        findPanel.add(findIcon, gridBagConstraints);

        projFindField.setBackground(UIManager.getColor(Theme.PROJECT_FIND_BACKGROUND));
        projFindField.setFont(projFindField.getFont().deriveFont(projFindField.getFont().getSize()-1f));
        projFindField.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4));
        projFindField.setCaretColor(UIManager.getColor(Theme.PROJECT_FIND_FOREGROUND));
        projFindField.setComponentPopupMenu(searchPopup);
        projFindField.setName("projFindField"); // NOI18N
        projFindField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                projFindFieldActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        findPanel.add(projFindField, gridBagConstraints);

        closeFindBtn.setBackground(UIManager.getColor(Theme.PROJECT_FIND_BACKGROUND));
        closeFindBtn.setForeground(UIManager.getColor(Theme.PROJECT_FIND_FOREGROUND));
        closeFindBtn.setBorder(null);
        closeFindBtn.setBorderPainted(false);
        closeFindBtn.setContentAreaFilled(false);
        closeFindBtn.setName("closeFindBtn"); // NOI18N
        closeFindBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeFindBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        findPanel.add(closeFindBtn, gridBagConstraints);

        treePanel.add(findPanel, java.awt.BorderLayout.SOUTH);

        splitPane.setLeftComponent(treePanel);

        notePanel.setName("notePanel"); // NOI18N
        notePanel.setLayout(new java.awt.BorderLayout());

        tabs.setBackground(UIManager.getColor(Theme.PROJECT_HEADER_BACKGROUND));
        tabs.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        tabs.setFont(tabs.getFont().deriveFont(tabs.getFont().getSize()-2f));
        tabs.setName("tabs"); // NOI18N
        tabs.setOpaque(true);
        tabs.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabsStateChanged(evt);
            }
        });

        propsScroll.setBorder(null);

        properties.setAutoCreateRowSorter(true);
        properties.setFont(properties.getFont().deriveFont(properties.getFont().getSize()-1f));
        properties.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        properties.setFillsViewportHeight(true);
        properties.setName("properties"); // NOI18N
        propsScroll.setViewportView(properties);

        tabs.addTab(string("properties"), propsScroll); // NOI18N

        noteScroll.setBorder(null);

        noteField.setBackground(UIManager.getColor(Theme.NOTES_BACKGROUND));
        noteField.setFont(new java.awt.Font("Monospaced", 0, 14)); // NOI18N
        noteField.setForeground(UIManager.getColor(Theme.NOTES_FOREGROUND));
        noteField.setLineWrap(true);
        noteField.setTabSize(4);
        noteField.setWrapStyleWord(true);
        noteField.setCaretColor(new java.awt.Color(0, 0, 4));
        noteField.setName("noteField"); // NOI18N
        noteScroll.setViewportView(noteField);

        tabs.addTab(string("prj-l-view-notes"), noteScroll); // NOI18N

        notePanel.add(tabs, java.awt.BorderLayout.CENTER);

        splitPane.setRightComponent(notePanel);

        add(splitPane, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

	private void projTreeMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_projTreeMousePressed
            if (evt.isPopupTrigger()) {
                showPopupMenu(evt);
            }
	}//GEN-LAST:event_projTreeMousePressed

	private void projTreeMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_projTreeMouseReleased
            if (evt.isPopupTrigger()) {
                showPopupMenu(evt);
            }
	}//GEN-LAST:event_projTreeMouseReleased

	private void addTaskBtnMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_addTaskBtnMousePressed
            StrangeEons.setWaitCursor(true);
            try {
                TaskGroup parent = project;
                Member[] m = getSelectedMembers();
                if (m.length == 1 && m[0] instanceof TaskGroup) {
                    parent = (TaskGroup) m[0];
                } else {
                    select(project);
                }
                NewTaskDialog d = new NewTaskDialog(parent);
                moveToLocusOfAttention(d);
                d.setVisible(true);
            } finally {
                StrangeEons.setWaitCursor(false);
            }
	}//GEN-LAST:event_addTaskBtnMousePressed

	private void projTreeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_projTreeMouseClicked
            // when an item(s) is double-clicked, attempt to performOnSelection
            // the open action on it, if it applies

            if (evt.getClickCount() != 2 || evt.getButton() != MouseEvent.BUTTON1) {
                return;
            }

            TreePath[] sel = projTree.getSelectionPaths();
            if (sel == null) {
                return;
            }

            Member[] members = treePathsToMembers(sel);

            // find the default action: the action with the highest priority
            // that has a priority of at least 0; if no such action exists, do nothing
            TaskAction ta = null;
            for (int i = Actions.PRIORITY_MAX; i >= 0 && ta == null; --i) {
                List<TaskAction> tas = Actions.getActionsForPriority(i);
                if (tas.size() > 0) {
                    for (TaskAction candidate : tas) {
                        if (candidate.appliesToSelection(members)) {
                            ta = candidate;
                            break;
                        }
                    }
                }
            }
            if (ta == null) {
                return;
            }

            boolean culled = false;
            for (int i = 0; i < members.length; ++i) {
                if (members[i].isFolder()) {
                    members[i] = null;
                    culled = true;
                }
            }
            if (culled) {
                LinkedList<Member> filtered = new LinkedList<>();
                for (int i = 0; i < members.length; ++i) {
                    if (members[i] != null) {
                        filtered.add(members[i]);
                    }
                }
                members = filtered.toArray(new Member[filtered.size()]);
            }

            if (ta.appliesToSelection(members)) {
                StrangeEons.setWaitCursor(true);
                try {
                    ta.performOnSelection(members);
                } finally {
                    StrangeEons.setWaitCursor(false);
                }
            }
	}//GEN-LAST:event_projTreeMouseClicked

	private void splitPanePropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_splitPanePropertyChange
            if (evt.getPropertyName().equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)) {
                Settings.getUser().set(KEY_NOTES_DIVIDER, "" + splitPane.getDividerLocation());
            }
	}//GEN-LAST:event_splitPanePropertyChange

	private void projFindFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_projFindFieldActionPerformed
            StrangeEons.setWaitCursor(true);
            try {
                Pattern pattern = createPattern();
                if (pattern == null) {
                    getToolkit().beep();
                    return;
                }
                String title = string("prj-l-search-project-title", projFindField.getText());
                boolean caseSense = searchCaseSense.isSelected();
                boolean regExp = searchRegExp.isSelected();
                if (caseSense || regExp) {
                    title = title + " ["
                            + (caseSense ? string("find-case-sense").toLowerCase() : "")
                            + (caseSense && regExp ? ", " : "")
                            + (regExp ? string("find-reg-exp").toLowerCase() : "")
                            + ']';
                }

                SearchResults results = new SearchResults(title, project, pattern);
                Point loc = findPanel.getLocationOnScreen();
                int bottom = getLocationOnScreen().y + getHeight();
                results.setLocation(loc.x + findPanel.getWidth(), loc.y);
                int width = StrangeEons.getWindow().getWidth() - getWidth();
                width = Math.min(width, Math.min(width / 2, 600));
                results.setSize(width, bottom - loc.y);

                results.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        openSearchResults.remove(e.getWindow());
                    }
                });
                openSearchResults.add(results);
                results.setVisible(true);
            } finally {
                StrangeEons.setWaitCursor(false);
            }
	}//GEN-LAST:event_projFindFieldActionPerformed

    private Pattern createPattern() {
        String patternText = projFindField.getText();
        if (errorHighlight != null) {
            projFindField.getHighlighter().removeHighlight(errorHighlight);
        }
        if (patternText.isEmpty()) {
            findPanel.setToolTipText(null);
            return null;
        }

        Pattern pattern;
        try {
            int flags = Pattern.UNICODE_CASE;
            if (!searchCaseSense.isSelected()) {
                flags |= Pattern.CASE_INSENSITIVE;
            }
            if (!searchRegExp.isSelected()) {
                flags |= Pattern.LITERAL;
            }
            pattern = Pattern.compile(patternText, flags);
            findPanel.setToolTipText(null);
        } catch (PatternSyntaxException e) {
            String message = PatternExceptionLocalizer.localize(patternText, e);
            findPanel.setToolTipText(message);
            int pos = e.getIndex();
            if (pos >= 0) {
                try {
                    if (pos == patternText.length()) {
                        --pos;
                    }
                    errorHighlight = projFindField.getHighlighter().addHighlight(pos, pos + 1, ORANGE_SQUIGGLE);
                } catch (BadLocationException ble) {
                    StrangeEons.log.log(Level.WARNING, null, ble);
                }
            }
            projFindField.requestFocusInWindow();
            return null;
        }
        return pattern;
    }

    private Object errorHighlight;
    private static HighlightPainter ORANGE_SQUIGGLE = new ErrorSquigglePainter(new Color(0xcc_5600));

    @SuppressWarnings("unchecked")
	private void closeFindBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeFindBtnActionPerformed
        for (SearchResults sr : (HashSet<SearchResults>) openSearchResults.clone()) {
            sr.dispose();
        }
        findPanel.setVisible(false);
	}//GEN-LAST:event_closeFindBtnActionPerformed

	private void searchCopyItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchCopyItemActionPerformed
            projFindField.copy();
	}//GEN-LAST:event_searchCopyItemActionPerformed

	private void searchPasteItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchPasteItemActionPerformed
            projFindField.paste();
	}//GEN-LAST:event_searchPasteItemActionPerformed

	private void findCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findCheckActionPerformed
            Settings s = Settings.getUser();
            s.set(KEY_FIP_CASE_SENSE, searchCaseSense.isSelected() ? "yes" : "no");
            s.set(KEY_FIP_REG_EXP, searchRegExp.isSelected() ? "yes" : "no");

            // we need to force a pattern parse attempt to add/remove error
            // highlights when changing regexp modes
            if (evt.getSource() == searchRegExp) {
                createPattern();
            }
	}//GEN-LAST:event_findCheckActionPerformed

	private void toolCloseBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_toolCloseBtnActionPerformed
            StrangeEons.getWindow().closeProject();
	}//GEN-LAST:event_toolCloseBtnActionPerformed

	private void tabsStateChanged( javax.swing.event.ChangeEvent evt ) {//GEN-FIRST:event_tabsStateChanged
            int index = tabs.getSelectedIndex();
            if (index >= 0) {
                String name = tabs.getComponentAt(index).getName();
                // The name of the built-in tabs will be null while the
                // view is being set up; we ignore such events since
                // the tab will be selected explicitly later during the init.
                // (See createViewTabs())
                if (name != null) {
                    Settings.getUser().set(KEY_NOTES_TAB, name);
                }
            }
	}//GEN-LAST:event_tabsStateChanged

    private static final String KEY_FIP_CASE_SENSE = "find-in-projects-case-sense";
    private static final String KEY_FIP_REG_EXP = "find-in-projects-regular-expression";

    private HashSet<SearchResults> openSearchResults = new HashSet<>();

    private void showPopupMenu(MouseEvent evt) {
        Point loc = evt.getPoint();
        TreePath path = projTree.getPathForLocation(loc.x, loc.y);
        TreePath[] sel = projTree.getSelectionPaths();
        if (path != null) {
            boolean replaceSelection = false;
            if (sel == null) {
                replaceSelection = true;
            } else {
                int i;
                for (i = 0; i < sel.length; ++i) {
                    if (path.equals(sel[i])) {
                        break;
                    }
                }
                if (i >= sel.length) {
                    replaceSelection = true;
                }
            }
            if (replaceSelection) {
                projTree.setSelectionPath(path);
                sel = projTree.getSelectionPaths();
            }
        }

        Member[] members;
        if (sel == null || sel.length == 0) {
            members = new Member[]{project};
            //return;
        } else {
            members = treePathsToMembers(sel);
        }

        JPopupMenu menu = Actions.buildMenu(members);
        menu.show(projTree, loc.x, loc.y);
    }

    private Member[] treePathsToMembers(TreePath[] paths) {
        Member[] members = new Member[paths.length];
        for (int i = 0; i < paths.length; ++i) {
            members[i] = ((Member.MemberTreeNode) paths[i].getLastPathComponent()).asMember();
        }
        return members;
    }

    private Member treePathToMember(TreePath path) {
        return ((Member.MemberTreeNode) path.getLastPathComponent()).asMember();
    }

    private String treePathsToPropertyValue(TreePath[] paths) {
        StringBuilder b = new StringBuilder(1024);
        if (paths != null) {
            LinkedList<String> stack = new LinkedList<>();
            for (int i = 0; i < paths.length; ++i) {
                b.append(':');
                Member m = treePathToMember(paths[i]);
                if (m != null) {
                    while (m != project && m != null) {
                        stack.push(m.getName());
                        m = m.getParent();
                    }
                    while (!stack.isEmpty()) {
                        b.append(stack.pop()).append('/');
                    }
                    b.deleteCharAt(b.length() - 1);
                    stack.clear();
                }
            }
        }
        return b.toString();
    }

    private TreePath[] propertyValueToTreePaths(String value) {
        TreePath[] tp = null;
        if (value != null && !value.isEmpty()) {
            LinkedList<TreePath> list = new LinkedList<>();
            String[] tokens = value.split("\\:", -1);
            // every token starts with : so that we can tell an empty list
            // from the project; hence we expect an empty string at index 0
            for (int i = 1; i < tokens.length; ++i) {
                String token = tokens[i];
                if (token.isEmpty()) {
                    list.add(memberToTreePath(project));
                } else {
                    try {
                        Member m = project.findMember(new URL("project:" + token));
                        if (m != null) {
                            TreePath p = memberToTreePath(m);
                            if (p != null) {
                                list.add(p);
                            }
                        }
                    } catch (MalformedURLException ex) {
                        StrangeEons.log.log(Level.WARNING, null, ex);
                    }
                }
            }
            tp = list.toArray(new TreePath[list.size()]);
        }
        if (tp == null) {
            tp = new TreePath[0];
        }
        return tp;
    }

    /**
     * Stores the current tree state (open folders and selections) to the
     * project's settings.
     */
    private void storeTreeState() {
        TreePath[] tp = projTree.getSelectionPaths();
        project.getSettings().set(PROJECT_SELECTION_LIST, treePathsToPropertyValue(tp));

        int rowCount = projTree.getRowCount();
        LinkedList<TreePath> op = new LinkedList<>();
        for (int r = 0; r < rowCount; ++r) {
            if (projTree.isExpanded(r)) {
                op.add(projTree.getPathForRow(r));
            }
        }
        project.getSettings().set(PROJECT_OPEN_FOLDER_LIST, treePathsToPropertyValue(op.toArray(new TreePath[op.size()])));

        Point topmostPoint = ((JViewport) projTree.getParent()).getViewPosition();
        int row = projTree.getClosestRowForLocation(0, topmostPoint.y);
        project.getSettings().setInt(PROJECT_FIRST_ROW, row);
    }

    /**
     * Restores the tree state (open folders and selections) from the project's
     * settings.
     */
    private void recoverTreeState() {
        String s = project.getSettings().get(PROJECT_SELECTION_LIST);
        TreePath[] tp = propertyValueToTreePaths(s);
        projTree.setSelectionPaths(tp);

        s = project.getSettings().get(PROJECT_OPEN_FOLDER_LIST);
        for (TreePath p : propertyValueToTreePaths(s)) {
            projTree.expandPath(p);
        }

        int row = project.getSettings().getInt(PROJECT_FIRST_ROW, 0);
        Rectangle r = projTree.getRowBounds(row);
        if (r != null) {
            ((JViewport) projTree.getParent()).setViewPosition(new Point(0, r.y));
//			projTree.scrollRowToVisible( row );
        }
    }

    /**
     * Releases resources held by this view. Failure to call this when closing a
     * project could result in a resource leak. After calling this method, this
     * view should not be added to a container or otherwise made visible.
     */
    public void dispose() {
        try {
            firePropertyChange(VIEW_CLOSING_PROPERTY, false, true);
        } finally {
            ((PropertyTable) properties.getModel()).dispose();
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private ca.cgjennings.ui.JLinkLabel addTaskBtn;
    private javax.swing.JButton closeFindBtn;
    private javax.swing.JLabel findIcon;
    private javax.swing.JPanel findPanel;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JTextArea noteField;
    private javax.swing.JPanel notePanel;
    private javax.swing.JScrollPane noteScroll;
    private javax.swing.JTextField projFindField;
    private javax.swing.JTree projTree;
    private javax.swing.JTable properties;
    private javax.swing.JScrollPane propsScroll;
    private javax.swing.JCheckBoxMenuItem searchCaseSense;
    private javax.swing.JMenuItem searchCopyItem;
    private javax.swing.JMenuItem searchPasteItem;
    private javax.swing.JPopupMenu searchPopup;
    private javax.swing.JCheckBoxMenuItem searchRegExp;
    private javax.swing.JSplitPane splitPane;
    private javax.swing.JTabbedPane tabs;
    private javax.swing.JPanel titlePanel;
    private ca.cgjennings.apps.arkham.ToolCloseButton toolCloseBtn;
    private javax.swing.JLabel treeLabel;
    private javax.swing.JPanel treePanel;
    // End of variables declaration//GEN-END:variables

    private class Renderer extends DefaultTreeCellRenderer {

        public Renderer() {
            if (UIManager.getLookAndFeel().getName().equals("Nimbus")) {
                setBackgroundSelectionColor(new Color(0, 0, 0, 0));
            }
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (value == null) {
                value = "";
            }

            if (value instanceof Member.MemberTreeNode) {
                Member m = ((Member.MemberTreeNode) value).asMember();
                super.getTreeCellRendererComponent(tree, m, sel, expanded, leaf, row, hasFocus);

                setIcon(m.getIcon());

                Font f = tree.getFont();
                if (isOnCutList(m)) {
                    f = f.deriveFont(cutAttributes);
                }
                for (StrangeEonsEditor ed : StrangeEons.getWindow().getEditorsShowingFile(m.getFile())) {
                    if (ed.hasUnsavedChanges()) {
                        f = f.deriveFont(unsavedAttributes);
                        break;
                    }
                }
                setFont(f);
            } else {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                if (blank == null) {
                    blank = new BlankIcon(18);
                }
                setIcon(blank);
                setFont(tree.getFont());
            }
            return this;
        }
        private final Map<TextAttribute, Object> cutAttributes;

        {
            cutAttributes = new HashMap<>();
            cutAttributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
            cutAttributes.put(TextAttribute.FOREGROUND, Color.GRAY);
        }
        private final Map<TextAttribute, ? extends Object> unsavedAttributes = Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        private Icon blank;
    }

    class ProjectTransferHandler extends TransferHandler {

        private boolean isCutting;

        public ProjectTransferHandler() {
        }

        @Override
        public void exportAsDrag(JComponent comp, InputEvent e, int action) {
            super.exportAsDrag(comp, e, action);
        }

        private TreePath getTarget(TransferHandler.TransferSupport support) {
            if (support.isDrop()) {
                return ((JTree.DropLocation) support.getDropLocation()).getPath();
            } else {
                return projTree.getAnchorSelectionPath();
            }
        }

        private List<URL> extractURLs(String s) {
            LinkedList<URL> urls = new LinkedList<>();
            Pattern p = Pattern.compile("(?i)\\b((?:[a-z][\\w-]+:(?:/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))", Pattern.CASE_INSENSITIVE);
            for (String line : s.split("(\r\n)|(\n)|(\r)")) {
                line = line.trim();
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    try {
                        int colon = line.indexOf(':');
                        int slash = line.indexOf('/');
                        if (colon < 0 || colon > 8 || (slash >= 0 && slash < colon)) {
                            line = "http://" + line;
                        }
                        urls.add(new URL(line));
                        continue;
                    } catch (Exception e) {
                    }
                } else {
                    // not a URL, try interpreting it as a file
                    File f = new File(line);
                    if (f.exists()) {
                        try {
                            urls.add(f.toURI().toURL());
                        } catch (MalformedURLException ex) {
                        }
                        continue;
                    }
                }

                // bail as soon as we can't find something valid
                break;
            }
            return urls;
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            // Special data type handling

            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                if (support.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                    if (support.isDrop()) {
                        support.setShowDropLocation(true);
                        support.setDropAction(TransferHandler.COPY);
                    }
                    return true;
                }

                if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    boolean accept = true;
                    try {
                        Transferable t = support.getTransferable();
                        String s = (String) t.getTransferData(DataFlavor.stringFlavor);
                        if (extractURLs(s).isEmpty()) {
                            accept = false;
                        }
                    } catch (Exception e) {
                        // can't get the data, so assume we can import for now
                    }
                    if (support.isDrop()) {
                        support.setShowDropLocation(true);
                        support.setDropAction(TransferHandler.COPY);
                    }
                    return accept;
                }
            }

            // Standard file handling
            boolean isMove = false;
            if (support.isDrop()) {
                support.setShowDropLocation(true);
                if (support.getDropAction() == TransferHandler.LINK) {
                    support.setDropAction(TransferHandler.COPY);
                }
                if (support.getDropAction() == TransferHandler.MOVE) {
                    isMove = true;
                }
            }

            TreePath path = getTarget(support);
            if (path == null) {
                return false;
            }
            Member member = ((Member.MemberTreeNode) path.getLastPathComponent()).asMember();

            // can only move/copy into a folder
            if (!member.isFolder()) {
                return false;
            }

            File target = member.getFile();
            File[] files = getFilesFromTransferable(support.getTransferable());

            if (files == null) {
                // Workaround: under Windows, dragging a file into the project
                // getFilesFromTransferable causes an InvalidDnDOperationException
                // which ends up returning null here. In this case we simply verify
                // that the data flavor is correct. This method will be called
                // again when an attempt is made to actually import the data.
                if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return true;
                }
                return false;
            }

            // can't move a file into its immediate parent
            if (isMove) {
                for (int i = 0; i < files.length; ++i) {
                    if (files[i].getParentFile().equals(target)) {
                        return false;
                    }
                }
            }

            // can't copy/move to yourself
            // can't copy/move into a folder that is a subfolder of the moved folder
            // can't copy/move a task except into a task group
            // (copying a project will eventually convert it into a task group)
            File projFile = project.getFile();
            for (int i = 0; i < files.length; ++i) {
                if (files[i].equals(target)) {
                    return false;
                }
                if (files[i].isDirectory() && ProjectUtilities.contains(files[i], target)) {
                    return false;
                }
                if (Task.isTaskFolder(files[i])) {
                    if (!(member instanceof TaskGroup)) {
                        return false;
                    }
                    if (files[i].equals(projFile)) {
                        return false;
                    }
                }
            }

            return true;
        }

        private File[] getFilesFromTransferable(Transferable t) {
            if (!t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return null;
            }
            try {
                List<File> list = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                return list.toArray(new File[list.size()]);
            } catch (InvalidDnDOperationException e) {
                // Can't get transfer data during a drag from
                // Windows into the app---see "Workaround", above.
                return null;
            } catch (UnsupportedFlavorException e) {
                StrangeEons.log.log(Level.WARNING, null, e);
                throw new AssertionError();
            } catch (IOException e) {
                ErrorDialog.displayError("prj-err-transfer", e);
                StrangeEons.log.log(Level.WARNING, null, e);
                return null;
            }
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            Member[] members = getSelectedMembers();
            if (members.length > 0) {
                members = ProjectUtilities.merge(members);

                if (!isCutting) {
                    setCutList(null);
                }

                return new FileTransferable(members);
            }
            return null;
        }

        @Override
        protected void exportDone(JComponent c, Transferable data, int action) {
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY_OR_MOVE;
        }

        private File importNonFileData(File targetFile, TransferHandler.TransferSupport support) {
            if (support.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                StrangeEons.setWaitCursor(true);
                File destFile = ProjectUtilities.getAvailableFile(new File(targetFile, string("paste").toLowerCase() + ".png"));
                try {
                    Image image = (Image) support.getTransferable().getTransferData(DataFlavor.imageFlavor);
                    BufferedImage bi = ImageUtilities.imageToBufferedImage(image);
                    ImageIO.write(bi, "png", destFile);
                } catch (Exception e) {
                    Toolkit.getDefaultToolkit().beep();
                    StrangeEons.log.log(Level.WARNING, null, e);
                    return null;
                } finally {
                    StrangeEons.setWaitCursor(false);
                }
                return destFile;
            }

            if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    List<URL> urls = extractURLs((String) support.getTransferable().getTransferData(DataFlavor.stringFlavor));
                    if (urls.isEmpty()) {
                        Toolkit.getDefaultToolkit().beep();
                        return null;
                    }
                    $firstFile = null;
                    cancelDownload = false;
                    for (final URL url : urls) {
                        final File dest = targetFile;
                        new BusyDialog(string("paste"), () -> {
                            BusyDialog bd = BusyDialog.getCurrentDialog();
                            bd.setStatusText(url.toExternalForm());
                            InputStream in = null;
                            OutputStream out = null;
                            String fileName = url.getPath();
                            int slash = fileName.lastIndexOf('/');
                            while (slash >= 0 && slash == fileName.length() - 1) {
                                fileName = fileName.substring(0, fileName.length() - 1);
                                slash = fileName.lastIndexOf('/');
                            }
                            if (slash >= 0) {
                                fileName = fileName.substring(slash + 1);
                            }
                            if (fileName.isEmpty()) {
                                fileName = string("paste").toLowerCase();
                            }
                            File outFile = ProjectUtilities.getAvailableFile(new File(dest, ResourceKit.makeStringFileSafe(fileName)));
                            if ($firstFile == null) {
                                $firstFile = outFile;
                            }
                            byte[] buffer = new byte[64 * 1_024];
                            try {
                                out = new BufferedOutputStream(new FileOutputStream(outFile), 64 * 1_024);
                                URLConnection connection = url.openConnection();
                                ConnectionSupport.enableCompression(connection);
                                int downloadSize = connection.getContentLength();
                                in = ConnectionSupport.openStream(connection, -1);
                                if (downloadSize != -1) {
                                    bd.setProgressMaximum(downloadSize);
                                    bd.setProgressCurrent(0);
                                }
                                int bytesDownloaded = 0;
                                int bytesRead;
                                while ((bytesRead = in.read(buffer)) > 0) {
                                    if (bd.isCancelled()) {
                                        cancelDownload = true;
                                        return;
                                    }
                                    out.write(buffer, 0, bytesRead);
                                    bytesDownloaded += bytesRead;
                                    bd.setProgressCurrent(bytesDownloaded);
                                }
                                
                            } catch (IOException e) {
                                cancelDownload = true;
                                ErrorDialog.displayError(string(""), e);
                            } finally {
                                if (out != null) {
                                    try {
                                        out.close();
                                    } catch (IOException ie) {
                                        StrangeEons.log.log(Level.WARNING, null, ie);
                                    }
                                }
                                if (in != null) {
                                    try {
                                        in.close();
                                    } catch (IOException ie) {
                                        StrangeEons.log.log(Level.WARNING, null, ie);
                                    }
                                }
                                if (cancelDownload) {
                                    outFile.delete();
                                }
                            }
                        }, BusyDialog.NO_CANCEL_ACTION);
                        if (cancelDownload) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    Toolkit.getDefaultToolkit().beep();
                    StrangeEons.log.log(Level.WARNING, null, e);
                    return null;
                }

                if (!cancelDownload) {
                    File retval = $firstFile;
                    $firstFile = null;
                    return retval;
                } else {
                    $firstFile = null;
                    return null;
                }
            }

            return null;
        }
        private volatile boolean cancelDownload;
        private File $firstFile;

        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            // Determine the folder that will be the parent of the import
            TreePath targetPath = getTarget(support);
            if (targetPath == null) {
                return false;
            }

            Member target = ((Member.MemberTreeNode) targetPath.getLastPathComponent()).asMember();
            // For files, canImport will have ruled out anything but directories.
            // However, special data types can be pasted anywhere to keep things simple.
            if (!target.isFolder()) {
                target = target.getParent();
                if (target == null) {
                    return false;
                }
            }
            File targetFile = target.getFile();

            // Handle special data types
            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                File firstFile = importNonFileData(targetFile, support);
                target.synchronize();
                projTree.expandPath(targetPath);
                if (firstFile != null) {
                    Member firstMember = target.findChild(firstFile);
                    if (firstMember != null) {
                        select(firstMember);
                        Rename rename = (Rename) Actions.getUnspecializedAction("rename");
                        if (rename != null) {
                            rename.performOnSelection(new Member[]{firstMember});
                        }
                    }
                    return true;
                }
                return false;
            }

            // Handle file operations, including possible drag and drop within the tree
            File[] files = getFilesFromTransferable(support.getTransferable());

            boolean isMove = support.isDrop() && (support.getDropAction() == TransferHandler.MOVE);

            // if this isn't a drop, and there is a cut list, and the cut list matches
            // our file set exactly, then this is a cut, so convert copy -> move
            if (!support.isDrop() && cutList != null) {
                boolean isCut = true;
                HashSet<File> cutSet = new HashSet<>();
                for (Member m : cutList) {
                    cutSet.add(m.getFile());
                }
                for (File f : files) {
                    if (!cutSet.contains(f)) {
                        isCut = false;
                        break;
                    }
                    cutSet.remove(f);
                }
                if (!cutSet.isEmpty()) {
                    isCut = false;
                }
                if (isCut) {
                    isMove = true;
                }
            }
            setCutList(null);

            StrangeEons.setWaitCursor(true);
            try {
                // user hasn't chosen to "Replace All" yet
                replaceAllMode = false;
                for (File source : files) {

                    try {
                        showedMoveFailure = false;
                        if (!copyFolder(source, targetFile, isMove)) {
                            break;
                        }
                    } catch (IOException e) {
                        String message;
                        if (isMove) {
                            message = string("prj-err-move", source.getName());
                        } else {
                            message = string("prj-err-copy", source.getName());
                        }
                        ErrorDialog.displayError(message, e);
                    }

                    Member update = project.findMember(source);
                    if (update != null) {
                        update.synchronize();
                    }
                    File sourceParent = source.getParentFile();
                    if (sourceParent != null && (update = project.findMember(sourceParent)) != null) {
                        update.synchronize();
                    }
                }
            } catch (Exception e) {
                ErrorDialog.displayError(string("prj-err-transfer"), e);
            } finally {
                StrangeEons.setWaitCursor(false);
            }

            target.synchronize();
            projTree.expandPath(targetPath);

            return true;
        }
    }

    private boolean replaceAllMode;

    private boolean copyFolder(File original, File parent, boolean move) throws IOException {
        if (!parent.isDirectory()) {
            throw new IllegalArgumentException("parent is not a folder");
        }
        // CGJ: what's the reason for this, if any? I don't remember
        if (parent.isHidden()) {
            return true;
        }

        File dest = new File(parent, original.getName());
        if (original.isDirectory()) {
            dest.mkdirs();
            for (File kid : original.listFiles()) {
                if (Project.isFileExcluded(kid)) {
                    continue;
                }
                if (!copyFolder(kid, dest, move)) {
                    return false;
                }
            }
        } else {
            if (dest.exists()) {
                int action;
                if (original.equals(dest)) {
                    action = ReplaceDialog.RENAME;
                } else if (replaceAllMode) {
                    action = ReplaceDialog.REPLACE;
                } else {
                    ReplaceDialog d = new ReplaceDialog(StrangeEons.getWindow(), original, dest);
                    moveToLocusOfAttention(d);
                    action = d.showDialog();
                    if (action == ReplaceDialog.REPLACE_ALL) {
                        action = ReplaceDialog.REPLACE;
                        replaceAllMode = true;
                    } else if (action == ReplaceDialog.CANCEL) {
                        return false;
                    }
                }

                if (action == ReplaceDialog.RENAME) {
                    dest = ProjectUtilities.getAvailableFile(dest);
                }
            }

            ProjectUtilities.copyFile(original, dest);

            // update any editors showing the moved file
            if (move) {
                for (StrangeEonsEditor ed : StrangeEons.getWindow().getEditorsShowingFile(original)) {
                    ed.setFile(dest);
                }
            }
        }
        if (move) {
            // update to remove from project watcher
            Member originalMember = project.findMember(original);
            if (originalMember != null && originalMember.isFolder()) {
                project.getWatcher().unregister(originalMember);
            }
            if (!original.delete() && !showedMoveFailure) {
                showedMoveFailure = true;
                ErrorDialog.displayError(string("prj-err-move", original.getName()), null);
            }
            Rename.fireRenameEvent(project, null, original, dest);
        }
        return true;
    }
    private boolean showedMoveFailure;

    static class FileTransferable implements Transferable {

        public FileTransferable(List<File> files) {
            this.files = files;
        }

        public FileTransferable(File[] files) {
            this.files = new LinkedList<>();
            for (File f : files) {
                this.files.add(f);
            }
        }

        public FileTransferable(Member[] members) {
            files = new LinkedList<>();
            for (Member m : members) {
                files.add(m.getFile());
            }
        }

        private static DataFlavor[] flavors = new DataFlavor[]{DataFlavor.javaFileListFlavor};
        private List<File> files;

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.javaFileListFlavor == flavor;
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            return files;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder("File Transferable:");
            for (File f : files) {
                b.append(f.getAbsolutePath()).append('\n');
            }
            return super.toString();
        }
    }

    private class PropertyTable extends AbstractTableModel {

        private ArrayList<String> keys = new ArrayList<>();
        private ArrayList<Object> values = new ArrayList<>();
        private Member current = null;
        private PropertyConsumer currentConsumer = null;

        private FileChangeListener fcListener = new FileChangeListener() {
            @Override
            public void fileChanged(File f, ChangeType type) {
                showPropertiesFor(current);
            }
        };

        public synchronized void showPropertiesFor(Member m) {
            if (currentConsumer != null) {
                currentConsumer.invalidate();
            }
            keys.clear();
            values.clear();

            if (current != m) {
                if (current != null) {
                    FileChangeMonitor.getSharedInstance().removeFileChangeListener(fcListener);
                }
                if (m != null) {
                    FileChangeMonitor.getSharedInstance().addFileChangeListener(fcListener, m.getFile());
                }
                current = m;
            }

            if (m != null) {
                currentConsumer = new PropertyConsumer(this);
                m.getMetadataSource().fillInMetadata(m, currentConsumer);
            } else {
                currentConsumer = null;
                fireTableDataChanged();
            }
        }

        public void dispose() {
            FileChangeMonitor.getSharedInstance().removeFileChangeListener(fcListener);
        }

        @Override
        public int getRowCount() {
            synchronized (this) {
                return keys.size();
            }
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return "";
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? String.class : Object.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            synchronized (this) {
                return columnIndex == 0 ? keys.get(rowIndex) : values.get(rowIndex);
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        }
    }

    private class PropertyConsumer implements MetadataSource.PropertyConsumer {

        boolean valid = true;
        final PropertyTable owner;

        public PropertyConsumer(PropertyTable owner) {
            this.owner = owner;
        }

        public void invalidate() {
            synchronized (owner) {
                valid = false;
            }
        }

        @Override
        public boolean isValid() {
            synchronized (owner) {
                return valid;
            }
        }

        @Override
        public void addProperty(Member m, String name, Object value) {
            synchronized (owner) {
                if (!isValid()) {
                    return;
                }
                owner.keys.add(name);
                owner.values.add(value);
            }
        }

        @Override
        public void doneAddingBlock() {
            synchronized (owner) {
                if (isValid()) {
                    EventQueue.invokeLater(() -> {
                        if (!isValid()) {
                            return;
                        }
                        owner.fireTableDataChanged();
                    });
                }
            }
        }

        @Override
        public void doneAddingProperties() {
            synchronized (owner) {
                if (isValid()) {
                    owner.currentConsumer = null;
                    EventQueue.invokeLater(() -> {
                        if (!isValid()) {
                            return;
                        }
                        owner.fireTableDataChanged();
                        TableModel model = properties.getModel();
                        for (int row = 0; row < model.getRowCount(); ++row) {
                            Object v = model.getValueAt(row, 1);
                            int h = 16;
                            if (v instanceof Icon) {
                                h = Math.max(h, ((Icon) v).getIconHeight() + 2);
                            }
                            properties.setRowHeight(row, h);
                        }
                    });
                }
            }
        }
    }

    private static class PropertyRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof Icon) {
                setIcon((Icon) value);
                setText(null);
            } else {
                setIcon(null);
                setText(value.toString());
            }
            return this;
        }

    }

    private static class HeaderlessTable extends JTable {

        public HeaderlessTable(Object[][] rowData, Object[] columnNames) {
            super(rowData, columnNames);
        }

        public HeaderlessTable(int numRows, int numColumns) {
            super(numRows, numColumns);
        }

        public HeaderlessTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
            super(dm, cm, sm);
        }

        public HeaderlessTable(TableModel dm, TableColumnModel cm) {
            super(dm, cm);
        }

        public HeaderlessTable(TableModel dm) {
            super(dm);
        }

        public HeaderlessTable() {
        }

        @Override
        protected void configureEnclosingScrollPane() {
            Container p = getParent();
            if (p instanceof JViewport) {
                Container gp = p.getParent();
                if (gp instanceof JScrollPane) {
                    JScrollPane scrollPane = (JScrollPane) gp;
                    // Make certain we are the viewPort's view and not, for
                    // example, the rowHeaderView of the scrollPane -
                    // an implementor of fixed columns might do this.
                    JViewport viewport = scrollPane.getViewport();
                    if (viewport == null || viewport.getView() != this) {
                        return;
                    }
                    //                scrollPane.setColumnHeaderView(getTableHeader());
                    // configure the scrollpane for any LAF dependent settings
                    //configureEnclosingScrollPaneUI();
                }
            }
        }
    }

    private static final String KEY_NOTES_DIVIDER = "project-notes-divider-pos";
    private static final String KEY_NOTES_TAB = "project-notes-tab-index";

    /**
     * Registers a new custom view tab to be displayed in project views.
     *
     * @param viewTab the view tab instance to register
     * @throws NullPointerException if the view tab is {@code null}
     * @throws IllegalArgumentException if the view tab's name is
     * {@code null}
     * @see ViewTab#getViewTabName()
     */
    public static void registerViewTab(ViewTab viewTab) {
        if (viewTab == null) {
            throw new NullPointerException("viewTab");
        }
        final String id = viewTab.getViewTabName();
        if (id == null) {
            throw new IllegalArgumentException("viewTab.getViewTabName()");
        }
        if (id.equals(ID_NOTES) || id.equals(ID_PROPERTIES)) {
            throw new IllegalArgumentException("reserved tab name: " + id);
        }

        if (viewTabs == null) {
            viewTabs = new TreeSet<>((ViewTab o1, ViewTab o2) -> Language.getInterface().getCollator().compare(o1.getLabel(), o2.getLabel()));
        } else {
            for (ViewTab vt : viewTabs) {
                if (id.equals(vt.getViewTabName())) {
                    throw new IllegalArgumentException("view tab name already registered: " + id);
                }
            }
        }
        viewTabs.add(viewTab);

        // if there is a project open, find out where the new view should be
        // inserted
        final ProjectView pv = getCurrentView();
        if (pv != null) {
            int tab = BUILT_IN_TABS; // skip the built-in tabs
            for (ViewTab vt : viewTabs) {
                if (vt.getViewTabName().equals(viewTab.getViewTabName())) {
                    Icon icon = null;
                    if (vt instanceof IconProvider) {
                        icon = ((IconProvider) vt).getIcon();
                    }
                    pv.tabs.insertTab(vt.getLabel(), icon, vt.createViewForProject(pv, pv.getProject()), null, tab);
                    break;
                }
                ++tab;
            }
        }
    }

    private static final String ID_NOTES = "NOTES";
    private static final String ID_PROPERTIES = "PROPERTIES";

    /**
     * Unregisters a previously registered custom view tab.
     *
     * @param viewTab the view tab instance to unregister
     */
    public static void unregisterViewTab(ViewTab viewTab) {
        if (viewTabs != null && viewTab != null) {
            final ProjectView pv = getCurrentView();
            if (pv != null) {
                int tab = BUILT_IN_TABS; // skip the built-in tabs
                for (ViewTab vt : viewTabs) {
                    if (vt.getViewTabName().equals(viewTab.getViewTabName())) {
                        pv.tabs.remove(tab);
                        break;
                    }
                    ++tab;
                }
            }
            viewTabs.remove(viewTab);
        }
    }

    private static TreeSet<ViewTab> viewTabs;

    private static final int BUILT_IN_TABS = 2;
}
