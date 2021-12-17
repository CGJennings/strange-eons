package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import static resources.Language.string;

/**
 * Task action for renaming project files. Also allows you to register listeners
 * that will be notified when files are renamed (through the project system).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class Rename extends TaskAction {

    @Override
    public String getLabel() {
        return string("pa-rename");
    }

    @Override
    public boolean appliesToSelection(Member[] members) {
        return members.length == 1;
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        return true;
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        Member m = ProjectUtilities.simplify(project, task, member);
        RenameDialog rd = new RenameDialog(m);
        project.getView().moveToLocusOfAttention(rd);
        rd.setVisible(true);
        return true;
    }

    /**
     * Renames a member.
     *
     * @param member the member to rename
     * @param newFileName the new name for the member
     * @return the new file; if the rename failed, this will be the original
     * file
     * @deprecated Replaced by {@link Member#renameFile(java.lang.String)}.
     */
    @Deprecated
    public static File rename(Member member, String newFileName) {
        try {
            return rename(member, new File(member.getFile().getParent(), newFileName), null);
        } catch (IOException ioe) {
            return member.getFile();
        }
    }

    /**
     * Rename a file in the project as if using this action, but without
     * displaying a dialog. This is the recommended way to rename a project
     * member, as it handles a number of special cases.
     * <p>
     * Note that renaming a file should not be used to move a file to a
     * different folder. In other words, the parent of the new file should be
     * the same as the parent of the original file.
     *
     * @param member the member to rename
     * @param newFile the file object that specifies the new file name
     * @param subfolders if member is a folder, and this is non-{@code null},
     * then it is a slash-separated list of subfolders to create
     * @return the new file
     * @throws IllegalArgumentException if the member is not a folder and
     * subfolders is not null
     */
    public static File rename(Member member, File newFile, String subfolders) throws IOException {
        if (member instanceof Project) {
            StrangeEons.getWindow().closeProject();
        }

        File oldFile = member.getFile();
        boolean isDir = oldFile.isDirectory();

        LinkedList<Member> unwatched = null;
        if (isDir) {
            unwatched = new LinkedList<>();
            stopWatchingChildren(member.getProject(), member, unwatched);
        }

        IOException toThrow = null;
        try {
            Path oldPath = member.getFile().toPath();
            Path newPath = oldPath.resolveSibling(newFile.getName());
            Files.move(oldPath, newPath);
            if (!isDir) {
                StrangeEonsEditor[] savedAs = StrangeEons.getWindow().getEditorsShowingFile(oldFile);
                for (int i = 0; i < savedAs.length; ++i) {
                    savedAs[i].setFile(newFile);
                }
            } else {
                for (StrangeEonsEditor editor : StrangeEons.getWindow().getEditors()) {
                    LinkedList<String> elements = new LinkedList<>();
                    File target = editor.getFile();
                    if (target == null) {
                        continue;
                    }
                    if (ProjectUtilities.contains(oldFile, target)) {
                        File f = target;
                        while (f != null && !f.equals(oldFile)) {
                            elements.push(f.getName());
                            f = f.getParentFile();
                        }
                        if (f == null) {
                            throw new AssertionError("renamed folder does not contain its own members");
                        }
                        f = newFile;
                        while (!elements.isEmpty()) {
                            f = new File(f, elements.pop());
                        }
                        editor.setFile(f);
                        elements.clear();
                    }
                }
            }
            if (subfolders != null) {
                if (!isDir) {
                    throw new IllegalArgumentException("can't add subfolders to a file");
                }
                for (String subtree : subfolders.split(",")) {
                    subtree = subtree.trim();
                    if (!subtree.isEmpty()) {
                        subtree = subtree.replace('/', File.separatorChar);
                        new File(newFile, subtree).mkdirs();
                    }
                }
            }

            if (!(member instanceof Project)) {
                member.getParent().synchronize();
            }
        } catch (IOException ioe) {
            // FAILED TO RENAME
            Toolkit.getDefaultToolkit().beep();
            // rename failed, but if it was a project we closed it
            // set the "renamed" file to the original project folder
            // so that the project gets reopened
            newFile = member.getFile();
            // if this was a directory, we stopped watching its children
            // since the watcher may have locks which would prevent renaming;
            // now we need to re-register those children
            if (unwatched != null) {
                ProjectWatcher watcher = member.getProject().getWatcher();
                for (Member m : unwatched) {
                    watcher.register(m);
                }
            }
            toThrow = ioe;
        }

        if (member instanceof Project) {
            StrangeEons.getWindow().setOpenProject(newFile);
        }

        Project p = StrangeEons.getWindow().getOpenProject();
        Member renamedMember = null;
        if (p != null) {
            renamedMember = p.findMember(newFile);
            if (renamedMember != null && p.getView() != null) {
                p.getView().select(renamedMember);
            }
        }

        if (toThrow != null) {
            throw toThrow;
        }

        if (!oldFile.equals(newFile)) {
            fireRenameEvent(p, renamedMember, oldFile, newFile);
        }

        return newFile;
    }

    private static void stopWatchingChildren(Project p, Member parent, List<Member> unwatched) {
        for (Member kid : parent.getChildren()) {
            if (kid.isFolder()) {
                stopWatchingChildren(p, kid, unwatched);
            }
        }
        p.getWatcher().unregister(parent);
        unwatched.add(parent);
    }

    /**
     * A listener for file rename events. Rename events are fired when the
     * rename command is used to rename a project file, or the delete command is
     * used to delete a project file. The event is fired <i>after</i> the rename
     * or delete takes place. Renaming or deleting a file from outside of the
     * project system will not fire an event. (Other parts of the project system
     * may also fire rename events when renaming or moving files.)
     */
    public static interface RenameListener {

        /**
         * Called when a file is renamed or deleted through the project system.
         * The value of {@code oldFile} is always the file that was changed. If
         * the file is being renamed, then {@code newFile} is the new (and
         * current) name of the file. If the file is being deleted, then
         * {@code newFile} will be {@code null}. The value of {@code newMember}
         * may be the project member instance of the {@code newFile}, if one has
         * already been created. Whether or not it is {@code null} depends on
         * the source of the rename action. When a file is renamed with the
         * rename action, it should be available. When the file is being moved
         * by a drag or paste in the project pane, it will be {@code null} since
         * the tree is not updated until the entire operation is completed. If
         * you need to access the new member and the value is {@code null}, you
         * can post an {@code EventQueue.invokeLater} event that calls
         * {@code Project.findMember( newFile )} to locate it.
         *
         * @param p the project in which the command was issued
         * @param newMember the new member associated with the file, if
         * available
         * @param oldFile the original file name
         * @param newFile the new file name, or {@code null} if the file was
         * deleted
         */
        public void fileRenamed(Project p, Member newMember, File oldFile, File newFile);
    }

    private static final LinkedList<RenameListener> listeners = new LinkedList<>();

    /**
     * Adds a new listener for file rename events. All instances of the rename
     * action share a common listener list (there is normally only one
     * instance). This makes it easier to register a listener because the rename
     * action does not need to be looked up.
     *
     * @param listener the listener to be notified when files are renamed or
     * deleted
     */
    public static void addRenameListener(RenameListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a rename event listener. The listener will no longer receive
     * rename events when a project file is renamed.
     *
     * @param listener the previously added listener
     */
    public static void removeRenameListener(RenameListener listener) {
        listeners.remove(listener);
    }

    static void fireRenameEvent(Project p, Member newMember, File oldFile, File newFile) {
        if (oldFile == null) {
            throw new NullPointerException("oldFile");
        }
        if (oldFile.equals(newFile)) {
            return;
        }

        for (RenameListener rl : listeners) {
            rl.fileRenamed(p, newMember, oldFile, newFile);
        }
    }
}
