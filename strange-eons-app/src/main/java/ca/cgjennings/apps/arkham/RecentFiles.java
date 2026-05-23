package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.project.Member;
import ca.cgjennings.apps.arkham.project.MetadataSource;
import ca.cgjennings.apps.arkham.project.Project;
import ca.cgjennings.apps.arkham.project.Rename;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
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
    public static final int MAXIMUM_LIST_SIZE = 100;

    private static final LinkedList<File> projects = new LinkedList<>();
    private static final List<File> publicProjects = Collections.unmodifiableList(projects);
    private static final LinkedList<File> docs = new LinkedList<>();
    private static final List<File> publicDocs = Collections.unmodifiableList(docs);

    private static final String KEY_PROJECT_LIST = "recent-project-";
    private static final String KEY_DOCUMENT_LIST = "recent-document-";

    /**
     * Reads the file lists from previously stored user settings.
     */
    private static void initializeFileLists() {
        String key = KEY_PROJECT_LIST;
        List<File> list = projects;
        for (int listIt = 0; listIt < 2; ++listIt) {
            for (int i = 1; i <= MAXIMUM_LIST_SIZE; ++i) {
                String path = RawSettings.getUserSetting(key + i);
                if (path == null) {
                    break;
                }
                list.add(new File(path));
            }

            key = KEY_DOCUMENT_LIST;
            list = docs;
        }
    }

    /**
     * Converts an old combined file list into separate lists of documents and
     * projects. This requires checking whether or not an entry is a directory,
     * which means it can take some time if the entry is located on a server.
     * For this reason, the lists are initially left empty and the conversion is
     * performed in a background thread.
     */
    private static void transitionOldFileList() {
        StrangeEons.log.info("updating format of recent file list, menu may be empty for several seconds");
        new Thread(() -> {
            final List<File> uProjects = new LinkedList<>();
            final List<File> uDocs = new LinkedList<>();

            for (int i = 0; i < MAXIMUM_LIST_SIZE; ++i) {
                final String key = "recent-file-" + (i + 1);
                String path = RawSettings.getUserSetting(key);
                if (path == null) {
                    break;
                }
                RawSettings.removeUserSetting(key);
                File file = new File(path);
                if (!file.exists()) {
                    continue;
                }

                List<File> uDest = uDocs;
                if (file.isDirectory() || file.getName().endsWith(".seproject")) {
                    uDest = uProjects;
                }
                if (!uDest.contains(file)) {
                    uDest.add(file);
                }
            }
            EventQueue.invokeLater(() -> {
                // by the time this runs, the list could have already been
                // modified, so we need to check for duplicates
                List<File> source = uProjects;
                List<File> dest = projects;
                for (int listIt = 0; listIt < 2; ++listIt) {
                    for (File f : source) {
                        if (!dest.contains(f)) {
                            dest.add(f);
                        }
                    }
                    source = uDocs;
                    dest = docs;
                }
                StrangeEons.log.info("recent file list updated");
                installExitTask();
            });
        }).start();
    }

    private static void installExitTask() {
        StrangeEons.getApplication().addExitTask(new Runnable() {
            @Override
            public void run() {
                String key = KEY_PROJECT_LIST;
                List<File> list = projects;
                for (int listIt = 0; listIt < 2; ++listIt) {
                    for (int i = 1; i <= MAXIMUM_LIST_SIZE; ++i) {
                        if (i <= list.size()) {
                            RawSettings.setUserSetting(key + i, list.get(i - 1).getAbsolutePath());
                        } else {
                            RawSettings.removeUserSetting(key + i);
                        }
                    }
                    RawSettings.removeUserSetting(key + (MAXIMUM_LIST_SIZE + 1));

                    key = KEY_DOCUMENT_LIST;
                    list = docs;
                }
            }

            @Override
            public String toString() {
                return "storing lists of recent documents";
            }
        });
    }

    private static void handleFileRename(File oldFile, File newFile) {
        // if the file is being deleted, we don't remove it at this point
        // as it might be about to be replaced
        if (newFile == null) {
            return;
        }

        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(() -> handleFileRename(oldFile, newFile));
            return;
        }

        List<File> list = projects;
        for (int listIt = 0; listIt < 2; ++listIt) {
            int i = list.indexOf(oldFile);
            if (i >= 0) {
                list.set(i, newFile);
                StrangeEons.log.log(Level.INFO, "updated recent file entry for {0}", newFile.getName());
            }
            list = docs;
        }
    }

    private static void installRenameListener() {
        Rename.addRenameListener((Project p, Member newMember, File oldFile, File newFile) -> handleFileRename(oldFile, newFile));
    }

    static {
        final boolean hasOldList = RawSettings.getUserSetting("recent-file-1") != null;
        final boolean hasNewList = RawSettings.getUserSetting(KEY_PROJECT_LIST + '1') != null
                || RawSettings.getUserSetting(KEY_DOCUMENT_LIST + '1') != null;
        
        // if there are entries in the new format, ignore entries in the old
        // format; this allows old and new versions to be used on the same
        // device without causing a delay on each startup
        if (hasNewList) {
            initializeFileLists();
        } else if (hasOldList) {
            transitionOldFileList();
        }

        installExitTask();
        installRenameListener();
    }

    /**
     * Returns an immutable list of recently opened document files.
     */
    public static List<File> getRecentDocuments() {
        return publicDocs;
    }

    /**
     * Returns an immutable list of recently opened project files.
     */
    public static List<File> getRecentProjects() {
        return publicProjects;
    }

    /**
     * Adds an entry to the list of recent projects.
     *
     * @param file the non-null project directory or package file to add
     */
    public static void addRecentProject(final File file) {
        addRecent(file, true);
    }

    /**
     * Adds an entry to the list of recent documents.
     *
     * @param file the non-null document file to add
     */
    public static void addRecentDocument(final File file) {
        addRecent(file, false);
    }

    private static void addRecent(final File file, final boolean isProject) {
        Objects.requireNonNull(file);
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(() -> addRecent(file, isProject));
            return;
        }
        LinkedList<File> list = isProject ? projects : docs;
        list.remove(file);
        list.addFirst(file);
        while (list.size() > MAXIMUM_LIST_SIZE) {
            list.removeLast();
        }
    }

    /**
     * Clear all recent documents and projects.
     */
    public static void clear() {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(RecentFiles::clear);
            return;
        }
        projects.clear();
        docs.clear();
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
        return Math.min(MAXIMUM_LIST_SIZE, Math.max(0, maxFiles));
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
                    final int choice = JOptionPane.showConfirmDialog(
                            StrangeEons.getWindow(),
                            string("app-clear-recent-verify"),
                            string("app-clear-recent"),
                            JOptionPane.YES_NO_OPTION
                    );
                    if (choice == JOptionPane.YES_OPTION) {
                        clear();
                    }
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
                        setEnabled(docs.size() + projects.size() > 0);
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
            final int maxItems = getNumberOfFilesToDisplay();
            int numItems = 0;

            // count out the items to include, up the maximum number
            LinkedList<RecentFileItem> projItems = new LinkedList<>();
            LinkedList<RecentFileItem> docItems = new LinkedList<>();
            for (int i = 0; numItems < maxItems; ++i) {
                if (i < projects.size()) {
                    projItems.add(new RecentFileItem(projects.get(i), true));
                    ++numItems;
                } else if (i >= docs.size()) {
                    // no more projects OR documents available,
                    // break to avoid infinite loop
                    break;
                }

                if (numItems == maxItems) {
                    break;
                }

                if (i < docs.size()) {
                    docItems.add(new RecentFileItem(docs.get(i), false));
                    ++numItems;
                }
            }

            removeAll();
            List<RecentFileItem> group = projItems;
            for (int listIt = 0; listIt < 2; ++listIt) {
                if (!group.isEmpty()) {
                    for (RecentFileItem item : group) {
                        add(item);
                    }
                    addSeparator();
                }
                group = docItems;
            }
            add(clearItem);
            clearItem.setEnabled(projItems.size() + docItems.size() > 0);

            super.fireMenuSelected();
        }

        private final JMenuItem clearItem;
    }

    @SuppressWarnings("serial")
    private static class RecentFileItem extends JMenuItem {

        private File file;
        private boolean isProject;

        public RecentFileItem(File f, boolean isProject) {
            super(f.getName());
            file = f;
            this.isProject = isProject;
            setToolTipText(f.getPath());
            setIcon(isProject ? MetadataSource.ICON_PROJECT : MetadataSource.ICON_BLANK);
        }

        @Override
        protected void fireActionPerformed(ActionEvent event) {
            StrangeEonsAppWindow w = StrangeEons.getWindow();
            if (isProject) {
                w.setOpenProject(file);
            } else {
                w.openFile(file);
            }
            super.fireActionPerformed(event);
        }
    }
}
