package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.StrangeEonsEditor.EditorListener;
import ca.cgjennings.apps.arkham.commands.Commandable;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.dialog.prefs.PreferenceCategory;
import ca.cgjennings.apps.arkham.project.Project;
import ca.cgjennings.apps.arkham.project.ProjectView;
import ca.cgjennings.ui.theme.Theme;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.List;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import resources.ResourceKit;

/**
 * This class defines the interface for interacting with the Strange Eons main
 * application window. There is exactly one main application window for any
 * running instance of the application, and it can be obtained by calling
 * {@link ca.cgjennings.apps.arkham.StrangeEons#getWindow()}.
 *
 * <p>
 * Note that the application window is created after the {@link Theme} has been
 * installed but before any extension plug-ins are loaded. However, it will not
 * be made visible until the entire application startup procedure has completed.
 * Extensions may create windows that use this window as their parent, but they
 * should not rely on those windows being visible or usable until the
 * application is fully started. To create a window that will be visible during
 * startup (for example, to display a more complex error message than
 * {@link ErrorDialog} allows), use
 * {@link ca.cgjennings.apps.arkham.StrangeEons#getSafeStartupParentWindow()} as
 * the parent.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public abstract class StrangeEonsAppWindow extends JFrame implements Commandable {

    /**
     * Returns a list of images suitable for use as the icons of a
     * {@code JFrame}. Plug-ins that create their own windows may use these
     * images to get the same window icons as the main application window.
     *
     * @return a list of frame icons at various sizes
     */
    public static List<Image> getApplicationFrameIcons() {
        if (frameIcons == null) {
            frameIcons = new ArrayList<>(5);
            frameIcons.add(ResourceKit.getThemedImage("icons/application/app.png"));
            for(int x = 2; x <= 16; x *= 2) {
                frameIcons.add(ResourceKit.getThemedImage("icons/application/app@" + x + "x.png"));
            }
            frameIcons = Collections.unmodifiableList(frameIcons);
        }
        return frameIcons;
    }
    private static List<Image> frameIcons;

    /**
     * Forces all editor windows that are displaying a game component to redraw
     * their component preview. This is equivalent to looping over all of the
     * available component editors and calling each one's
     * {@link AbstractGameComponentEditor#redrawPreview()} method.
     *
     * @since 3.0
     */
    public abstract void redrawPreviews();

    /**
     * Adds a new editor to the application window. This will cause the editor
     * to be made visible and available to the user.
     *
     * @param editor the editor to be added
     * @throws NullPointerException if {@code editor} is {@code null}
     */
    public abstract void addEditor(StrangeEonsEditor editor);

    /**
     * Returns the currently active editor window, or <tt>null</tt> if there is
     * no active editor.
     *
     * @return the active component editor, or <tt>null</tt>
     */
    public abstract StrangeEonsEditor getActiveEditor();

    /**
     * Returns an array of all currently open editor windows.
     *
     * @return an array of open editors
     * @since 2.00
     */
    public abstract StrangeEonsEditor[] getEditors();

    /**
     * Returns an array of all currently open editor windows that are editing a
     * certain file. If no editors currently have the file set as their save
     * file, returns an empty array.
     *
     * @param file the file to return active editors for
     * @return an array of open editors which have a save location that is equal
     * to the given file
     * @since 2.1a9
     */
    public abstract StrangeEonsEditor[] getEditorsShowingFile(File file);

    /**
     * Selects the next editor after the one that is currently active.
     *
     * @return the editor that becomes active
     * @since 2.00 (final)
     */
    public abstract StrangeEonsEditor selectNextEditor();

    /**
     * Selects the previous editor before the one that is currently active.
     *
     * @return the editor that becomes active
     * @since 2.00 (final)
     */
    public abstract StrangeEonsEditor selectPreviousEditor();

    /**
     * Attempts to open a file in the application as if the user had used the
     * <b>File | Open</b> menu item and selected the specified file. Although a
     * successful attempt to open the file typically results in a new editor
     * being added to the application, be aware that the new editor will not
     * generally be open yet when this method returns.
     *
     * <p>
     * <b>Note:</b> This method is threadsafe: it can be can be called from
     * outside of the event dispatch thread.
     *
     * @param file the file to opened
     * @throws NullPointerException if the file is {@code null}
     */
    public abstract void openFile(File file);

    /**
     * Sets a wait cursor on the application frame. This method nests, so if a
     * wait cursor is set multiple times, the normal interaction cursor will not
     * be restored until {@code setDefaultCursor()} is called a number of times
     * equal to the number of times this method was called. This method should
     * be paired with {@code setDefaultCursor()} using a {@code try ... finally}
     * block to ensure that the cursor is properly restored even if an exception
     * is thrown.:
     * <pre>
     * app.setWaitCursor();
     * try {
     *     // do lengthy job that requires wait cursor
     * } finally {
     *     app.setDefaultCursor();
     * }
     * </pre>
     */
    public abstract void setWaitCursor();

    /**
     * Restore the standard cursor in the application frame. See {link
     * #setWaitCursor()} for details on use.
     */
    public abstract void setDefaultCursor();

    /**
     * Displays the application preferences dialog.
     *
     * @param parent a component used to position the preferences dialog (may be
     * {@code null})
     * @param displayCategory the category that should be selected initially
     * (may be {@code null})
     * @since 3.0
     */
    public abstract void showPreferencesDialog(Component parent, PreferenceCategory displayCategory);

    /**
     * Displays the application's About dialog.
     */
    public abstract void showAboutDialog();

    /**
     * Exits the application. The user will be given the opportunity to save
     * files with unsaved changes. This method will only return if the user
     * cancels the exit action.
     * <p>
     * If {@code restart} is {@code true}, then an attempt is made to relaunch
     * the application after exiting. This may be useful, for example, when a
     * plug-in update is pending.
     *
     * @param restart restart after exit
     * @since 2.1a8
     */
    public abstract boolean exitApplication(boolean restart);

    /**
     * Displays a message to inform the user that the application should be
     * restarted for optimal performance. The user will be allowed to choose
     * whether or not to restart.
     *
     * @param message the reason for the restart; if {@code null}, a default
     * message is displayed
     * @since 2.1a8
     */
    public abstract void suggestRestart(String message);

    /**
     * Returns the view of the open project, or {@code null} if no project is
     * open.
     *
     * @return the open project's view, or {@code null}
     */
    public abstract ProjectView getOpenProjectView();

    /**
     * Adds a custom component to the top of the application window. This can be
     * used to provide custom controls for a plug-in. Custom components are
     * added in a horizontal strip above the pane where editor windows are
     * displayed.
     *
     * @param comp the component to add
     * @throws NullPointerException if {@code comp} is {@code null}
     * @since 2.00a13
     */
    public abstract void addCustomComponent(Component comp);

    /**
     * Adds a custom component to the top of the application window. The
     * component is added at a specific position with respect to the existing
     * components. If {@code index} is -1, {@code comp} will be added to the end
     * of the custom component bar. This is equivalent to
     * {@code addCustomComponent( comp )}.
     *
     * @param comp the component to be added
     * @param index the position at which to insert the component, or -1 to
     * append the component to the end
     * @throws NullPointerException if {@code comp} is {@code null}
     * @since 2.00a13
     */
    public abstract void addCustomComponent(Component comp, int index);

    /**
     * Adds a separator to the custom component bar at the top of the
     * application window. A reference to the added separator is returned. This
     * may be used to remove the separator later.
     *
     * @return the separator component that was added
     * @since 2.00a13
     */
    public abstract Component addCustomComponentSeparator();

    /**
     * Removes a previously added custom component from the top of the
     * application window.
     *
     * @param comp the component to remove
     * @throws NullPointerException if {@code comp} is {@code null}
     * @throws IllegalArgumentException if {@code comp} has not been added as a
     * custom component
     * @since 2.00a13
     */
    public abstract void removeCustomComponent(Component comp);

    /**
     * Removes the custom component at position {@code index} in the custom
     * component area. Nearby separators are not affected.
     *
     * @param index the 0-based index into the list of custom components
     * @throws IndexOutOfBoundsException if {@code index} &lt; 0 or
     * {@code index} &gt;= {@link #getCustomComponentCount()}
     * @since 2.00a13
     */
    public abstract void removeCustomComponent(int index);

    /**
     * Returns the number of custom components that have been added to the top
     * of the application window.
     *
     * @return the non-negative number of custom components that have been added
     * @since 2.00a13
     */
    public abstract int getCustomComponentCount();

    /**
     * Returns the custom component at position {@code index} in the custom
     * component area.
     *
     * @param index the 0-based index into the list of custom components
     * @return the component at position {@code index}
     * @throws IndexOutOfBoundsException if {@code index} &lt; 0 or
     * {@code index} &gt;= {@link #getCustomComponentCount()}
     * @since 2.00a13
     */
    public abstract Component getCustomComponent(int index);

    /**
     * Adds a new {@link EditorAddedListener} to this editor.
     *
     * @param eal the listener to add
     * @since 2.00 (final)
     */
    public abstract void addEditorAddedListener(EditorAddedListener eal);

    /**
     * Removes a new {@link EditorAddedListener} from this editor.
     *
     * @param eal the listener to remove
     * @since 2.00
     */
    public abstract void removeEditorAddedListener(EditorAddedListener eal);

    /**
     * A listener that is called whenever a new editor is added to the
     * application.
     *
     * @since 2.00
     */
    public static interface EditorAddedListener extends EventListener {

        /**
         * Called when a new editor has been opened in the application.
         *
         * @param editor the editor that has been added
         */
        public void editorAdded(StrangeEonsEditor editor);
    }

    /**
     * Adds a new {@link EditorListener} that will apply to all editors. An
     * editor listener can also be added through the {@link StrangeEonsEditor}
     * interface, in which case it will apply to only that editor.
     *
     * @param el the listener to add
     * @since 2.1a12
     */
    public abstract void addEditorListener(EditorListener el);

    /**
     * Removes an {@link EditorListener} that was added to all editors through
     * {@link #addEditorListener}. Note an editor listener must be removed from
     * the same object as the object it was added to; that is, a listener added
     * to the application window must be removed from the application window,
     * and a listener added to an individual editor must be removed from that
     * editor.
     *
     * @param el the listener to remove
     * @since 2.1a12
     */
    public abstract void removeEditorListener(EditorListener el);

    /**
     * Returns the current open {@link Project}, or {@code null} if no project
     * is open.
     *
     * @return the project currently open in the application window
     * @since 2.1 alpha 2
     */
    public abstract Project getOpenProject();

    /**
     * Opens the project contained in the folder {@code projectFolder}. If the
     * project can be opened and there is already a project open in the editor,
     * the current project is closed automatically before opening the new
     * project. If the project cannot be opened, an error message is displayed.
     *
     * @param projectFolderOrCrateFile the base folder of a project or an
     * <tt>.seproject</tt> file containing a project
     * @return {@code true} if the project is opened successfully
     * @since 2.1a2
     */
    public abstract boolean setOpenProject(File projectFolderOrCrateFile);

    /**
     * Closes the current open project, if any.
     *
     * @since 2.1a2
     */
    public abstract void closeProject();

    /**
     * A listener that is called when a project is opened or closed.
     *
     * @since 2.1a7
     */
    public interface ProjectEventListener extends EventListener {

        public void projectOpened(Project proj);

        public void projectClosing(Project proj);
    }

    /**
     * Adds a new listener for changes to the current project.
     *
     * @param li the listener to call when a project is opened or closed
     */
    public abstract void addProjectEventListener(ProjectEventListener li);

    /**
     * Removes a listener for changes to the current project.
     *
     * @param li the listener to be removed
     */
    public abstract void removeProjectEventListener(ProjectEventListener li);

    /**
     * Asks the application to add a window to the <b>Window</b> menu, such as a
     * modeless dialog, a frame window, or a palette window. The <b>Window</b>
     * menu automatically adds menu items for every open editor. Plug-ins can
     * use this method to add items for special windows that they create. When
     * the user selects the window's menu item, the window will be made visible,
     * moved to the front, and given focus.
     *
     * <p>
     * Tracked windows will continue to be listed in the menu until they are
     * explicitly removed by calling {@link #stopTracking}.
     *
     * <p>
     * Note that the the <b>Toolbox</b> menu offers similar (but more
     * comprehensive) functionality that is sufficient for most plug-ins.
     * Tracked windows are best suited to floating palettes and cases like the
     * script output window, which the user should think of as always existing,
     * even if momentarily hidden.
     *
     * @param window the window to track (or stop tracking)
     * @throws NullPointerException if the window is {@code null}
     * @since 3.0
     * @see #stopTracking
     */
    public abstract void startTracking(TrackedWindow window);

    /**
     * Stops tracking a previously tracked window, removing it from the
     * application's
     * <b>Window</b> menu. If the window was not being tracked, this method has
     * no effect.
     *
     * @param window the window to stop tracking
     * @since 3.0
     * @see #startTracking
     */
    public abstract void stopTracking(TrackedWindow window);

    /**
     * Inserts a menu item at a standard location in the application menu. This
     * allows you to add items to menus other than the <b>Toolbox</b>
     * and <b>Window</b> menus when another menu makes more sense This is a
     * convenient way to add new items to a menu. If you want more control over
     * menu placement, or to scan or replace existing items, see the
     * <tt>uiutils</tt> scripting library.
     *
     * @param parent identifies the menu you wish to insert an item into
     * @param item the item you wish to insert; this should be a
     * {@code JMenuItem} of some kind
     * @since 2.1a8
     */
    public abstract void addMenuItem(AppMenu parent, JComponent item);

    /**
     * Removes a menu item previously added with {@link #addMenuItem}. This is a
     * convenient way to add new items to a menu. If you want more control over
     * menu placement, or to scan or replace existing items, see the
     * <tt>uiutils</tt> scripting library.
     *
     * @param parent an identifier for the menu you wish to insert into
     * @param item the item you wish to insert; this should either be a
     * separator or a menu item
     * @since 2.1a8
     */
    public abstract void removeMenuItem(AppMenu parent, JComponent item);

    /**
     * Standard application menus that can be extended with
     * {@link #addMenuItem}.
     *
     * @since 2.1a8
     */
    public enum AppMenu {
        /**
         * The <b>File</b> menu, containing commands related to document files
         * and the project folder.
         */
        FILE,
        /**
         * The <b>Edit</b> menu, containing generic edit commands that apply in
         * a wide variety of contexts.
         */
        EDIT,
        /**
         * The <b>View</b> menu, which gives the user control over how editors
         * display their content.
         */
        VIEW,
        /**
         * The <b>Expansion</b> menu, which allows the user to change the
         * expansion(s) associated with a game component.
         */
        EXPANSION,
        /**
         * The <b>Markup</b> menu, containing commands that allow the user to
         * format text content.
         */
        MARKUP,
        /**
         * The <b>Deck</b> menu, which contains commands specific to editing
         * decks, expansion boards, and other arranged graphical objects.
         */
        DECK,
        /**
         * The <b>Source</b> menu, which contains commands specific to plug-in
         * development.
         */
        SOURCE,
        /**
         * The <b>Toolbox</b> menu, which contains commands related to installed
         * plug-ins.
         */
        TOOLBOX,
        /**
         * The <b>Help</b> menu, which provides access to documentation.
         */
        HELP
    }

    /**
     * A menu item that can be asked if it is currently usable. If you add
     * instances of this class to an application menu, your item will be enabled
     * or disabled on the fly.
     *
     * @since 2.1a11
     */
    public static class PolledMenuItem extends JMenuItem implements ActionListener {

        public PolledMenuItem(String text, int mnemonic) {
            super(text, mnemonic);
            init();
        }

        public PolledMenuItem(String text, Icon icon) {
            super(text, icon);
            init();
        }

        public PolledMenuItem(Action a) {
            super(a);
            init();
        }

        public PolledMenuItem(String text) {
            super(text);
            init();
        }

        public PolledMenuItem(Icon icon) {
            super(icon);
            init();
        }

        public PolledMenuItem() {
            init();
        }

        private void init() {
            addActionListener(this);
        }

        /**
         * This method will be called when the menu item is about to be shown,
         * and the item will be enabled or disabled accordingly.
         *
         * @param application a reference to Strange Eons
         * @return {@code true} if the item should be enabled
         */
        public boolean isUsable(StrangeEons application) {
            return true;
        }

        /**
         * This method is called when the menu item is selected. The base class
         * implementation does nothing. It can be overridden to provide custom
         * command handling.
         *
         * @param event the associated event information
         */
        @Override
        public void actionPerformed(ActionEvent event) {
        }

        /**
         * Installs an event handler on a menu that processes polled menu items.
         * Once installed, polled menu items in that menu will be enabled or
         * disabled automatically. Note that you do not need to use this method
         * for any of the standard application menu items, as a handler will
         * already be installed for you.
         *
         * @param menu the menu to install a handler on
         */
        public static void installMenuHandler(final JMenu menu) {
            menu.addMenuListener(new MenuListener() {
                @Override
                public void menuCanceled(MenuEvent e) {
                }

                @Override
                public void menuDeselected(MenuEvent e) {
                }

                @Override
                public void menuSelected(MenuEvent e) {
                    for (int i = 0; i < menu.getMenuComponentCount(); ++i) {
                        Component c = menu.getMenuComponent(i);
                        if (c instanceof PolledMenuItem) {
                            c.setEnabled(((PolledMenuItem) c).isUsable(StrangeEons.getApplication()));
                        }
                    }
                }
            });
        }
    }

    /**
     * Used by helper classes to fire property changes as if they were the app
     * window.
     *
     * @param name the property name to fire
     * @param oldValue the old value, or {@code null}
     * @param newValue the new value, or {@code null}
     */
    void propertyChange(String name, Object oldValue, Object newValue) {
        firePropertyChange(name, oldValue, newValue);
    }

    /**
     * The name of a property for which a change is fired when the user chooses
     * a different view quality setting.
     */
    public static final String VIEW_QUALITY_PROPERTY = "viewQuality";

    /**
     * The name of a property for which a change is fired when the user chooses
     * a different preview backdrop setting.
     */
    public static final String VIEW_BACKDROP_PROPERTY = "previewBackdrop";

    /**
     * By default, when an editor is added to the application window its text
     * components will automatically have the user's selected editor font set on
     * them. However, text fields that have this client property set to true
     * will be ignored.
     */
    public static final String NO_EDITOR_FONT = "eons.ignoreFont";
}
