package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.project.Member;
import ca.cgjennings.apps.arkham.project.MetadataSource;
import ca.cgjennings.apps.arkham.project.Project;
import ca.cgjennings.apps.arkham.project.Rename;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import static resources.Language.string;
import resources.RawSettings;

/**
 * The shared, global list of recently opened component and project files.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class RecentFiles {

    private RecentFiles() {
    }

    /**
     * The maximum number of recent files that will be tracked.
     */
    public static final int MAXIMUM_LIST_SIZE = 25;

    private static final LinkedList<File> files = new LinkedList<>();

    static {
        // init the list from preferences
        for (int i = 0; i < MAXIMUM_LIST_SIZE; ++i) {
            String file = RawSettings.getUserSetting("recent-file-" + (i + 1));
            if (file == null) {
                break;
            }
            files.add(new File(file));
        }

        // listen for project files to be renamed and update accordingly
        Rename.addRenameListener((Project p, Member newMember, File oldFile, File newFile) -> {
            synchronized (RecentFiles.class) {
                int index = files.indexOf(oldFile);
                if (index >= 0) {
                    // we don't adjust the list on file deletion, so if
                    // the file is replaced it will still work
                    if (newFile != null) {
                        files.set(index, newFile);
                        save();
                    }
                }
            }
        });
    }

    private static void save() {
        int size = files.size();
        for (int i = 0; i < MAXIMUM_LIST_SIZE; ++i) {
            String key = "recent-file-" + (i + 1);
            if (i >= size) {
                RawSettings.removeUserSetting(key);
            } else {
                RawSettings.setUserSetting(key, files.get(i).toString());
            }
        }
    }

    /**
     * Adds a file to the recent file list. If the file is already in the list,
     * it is moved to the start.
     *
     * @param file the file to add to the list
     */
    public static synchronized void add(File file) {
        if (file == null) {
            return;
        }

        files.remove(file);
        files.addFirst(file);
        if (files.size() >= MAXIMUM_LIST_SIZE) {
            files.removeLast();
        }
        save();
    }

    /**
     * Removes a file from the list. Has no effect if the file is not on the
     * list.
     *
     * @param file the file to remove
     */
    public static synchronized void remove(File file) {
        if (files.remove(file)) {
            save();
        }
    }

    /**
     * Returns <code>true</code> if the specified file is a member of the list.
     *
     * @param file the file to check for
     * @return <code>true</code> if the list contains the file
     */
    public static synchronized boolean contains(File file) {
        return files.contains(file);
    }

    /**
     * Returns an iterator over the recent file list.
     *
     * @return an iterator that returns that iterates over the file list in
     * order from most recently used to least recently used
     */
    public static synchronized Iterator<File> iterator() {
        return new LinkedList<>(files).iterator();
    }

    /**
     * Clears the list of recently used files.
     */
    public static synchronized void clear() {
        files.clear();
        save();
    }

    /**
     * Returns the number of files available in the list. Note that this may not
     * be the same number as the number that the user wishes to display.
     *
     * @return the number of available recent files
     * @see #getNumberOfFilesToDisplay()
     */
    public static synchronized int size() {
        return files.size();
    }

    /**
     * Returns the maximum number of files that the user wishes to include in
     * the recent file list. This will be a number between 0 and
     * {@link #MAXIMUM_LIST_SIZE}, inclusive.
     *
     * @return the maximum number of files displayed in the recent file list
     */
    public static int getNumberOfFilesToDisplay() {
        String value = RawSettings.getSetting("recent-file-menu-length");

        int maxFiles = 5;
        if (value != null) {
            try {
                maxFiles = Integer.parseInt(value);
            } catch (NumberFormatException e) {
            }
        }
        if (maxFiles < 0) {
            maxFiles = 0;
        } else if (maxFiles > MAXIMUM_LIST_SIZE) {
            maxFiles = MAXIMUM_LIST_SIZE;
        }
        return maxFiles;
    }

    /**
     * Menu that automatically builds a list of recent file items as its
     * children.
     */
    @SuppressWarnings("serial")
    static class RecentFileMenu extends JMenu {

        RecentFileMenu(JMenu parent) {
            super(string("app-open-recent"));
            clearItem = new JMenuItem(string("app-clear-recent")) {
                @Override
                protected void fireActionPerformed(ActionEvent event) {
                    clear();
                    super.fireActionPerformed(event);
                }
            };
            // Listen for the File menu to activate, and hide/disable the menu
            parent.addMenuListener(new MenuListener() {
                @Override
                public void menuSelected(MenuEvent e) {
                    if (getNumberOfFilesToDisplay() == 0) {
                        setVisible(false);
                        setEnabled(true);
                    } else {
                        setVisible(true);
                        setEnabled(files.size() > 0);
                    }
                }

                @Override
                public void menuDeselected(MenuEvent e) {
                }

                @Override
                public void menuCanceled(MenuEvent e) {
                }
            });
        }

        @Override
        protected void fireMenuSelected() {
            removeAll();
            int maxItems = getNumberOfFilesToDisplay();
            synchronized (RecentFiles.class) {
                // get N recent files, separating into files and projects
                LinkedList<File> projects = new LinkedList<>();
                LinkedList<File> otherFiles = new LinkedList<>();
                int n = 0;
                for (File f : files) {
                    if (n == maxItems) {
                        break;
                    }
                    try {
                        if(isProjectFile(f, false)) {
                            projects.add(f);
                            ++n;
                            continue;
                        }
                    } catch(IOException ioe) {}
                    otherFiles.add(f);
                    ++n;
                }
                for( File f : projects) {
                    add(new RecentFileItem(f));
                }
                if(projects.size() > 0 && otherFiles.size() > 0) {
                    addSeparator();
                }
                for( File f : otherFiles) {
                    add(new RecentFileItem(f));
                }
            }
            if (getMenuComponentCount() == 0) {
                clearItem.setEnabled(false);
                add(clearItem);
            } else {
                add(separator);
                clearItem.setEnabled(true);
                add(clearItem);
            }
            super.fireMenuSelected();
        }

        private JMenuItem clearItem;
        private JPopupMenu.Separator separator = new JPopupMenu.Separator();
    }

    @SuppressWarnings("serial")
    private static class RecentFileItem extends JMenuItem {

        private File file;

        public RecentFileItem(File f) {
            super(f.getName());
            file = f;
            setToolTipText(f.getPath());
            Icon icon = MetadataSource.ICON_BLANK;
            try {
                if (isProjectFile(f, false)) {
                    icon = MetadataSource.ICON_PROJECT;
                }
            } catch (IOException e) {}
            setIcon(icon);
        }

        @Override
        protected void fireActionPerformed(ActionEvent event) {
            StrangeEonsAppWindow w = StrangeEons.getWindow();

            try {
                if (isProjectFile(file, true)) {
                    w.setOpenProject(file);
                    return;
                }
            } catch (IOException e) {
                ErrorDialog.displayError(string("app-err-open", file.getName()), e);
                return;
            }

            w.openFile(file);
            super.fireActionPerformed(event);
        }
    }

    /**
     * Returns <code>true</code> is a file is (likely) a project. This method
     * may be faster than calling {@link Project#isProjectFolder(java.io.File)}
     * and {@link Project#isProjectPackage(java.io.File)}, but it may sometimes
     * produce false positives as well.
     *
     * @param file the file to test
     * @param thorough if <code>false</code>, a quick test is performed; if
     * <code>true</code>, a more thorough test is performed
     * @return <code>true</code> if the file is likely a project
     */
    static boolean isProjectFile(File file, boolean thorough) throws IOException {
        if (file == null || !file.exists()) {
            return false;
        }
        if (thorough) {
            return Project.isProjectFolder(file) || Project.isProjectPackage(file);
        } else {
            return file.isDirectory() || Project.isProjectPackage(file);
        }
    }
}
