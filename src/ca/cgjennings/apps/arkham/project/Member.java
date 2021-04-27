package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.AbstractSupportEditor;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.ui.IconProvider;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.swing.Icon;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import resources.Settings;

/**
 * A member of a project. This is the base class for objects that that can be
 * part of a project. Members of a project also correspond to files in the file
 * system; the file for a given member can be obtained by calling
 * {@link #getFile}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class Member implements IconProvider, Iterable<Member>, Comparable<Member> {

    private Member parent;
    private String name;
    private File file;
    private String extension;
    private String basename;

    private Member() {
    }

    Member(Member parent, File file, String name) {
        if (file == null) {
            throw new NullPointerException("file");
        }
        if (name == null) {
            throw new NullPointerException("name");
        }
        this.parent = parent;
        this.file = file;
        updateNameCache(name);
    }

    /**
     * Creates a new project member with the specified parent, representing the
     * specified file. Project members are not normally created this way, but
     * are instead obtained, directly or indirectly from a {@link Project}'s
     * {@link #getChildrenImpl() children}.
     *
     * <p>
     * Note that this constructor can be used to explicitly create a member that
     * is not actually in a project. This is sometimes useful (for example, to
     * obtain a {@link MetadataSource} for an arbitrary file), but it will
     * invalidate some parts of the <code>Member</code> contract (such as
     * {@link #getProject()} always returning a non-<code>null</code> value).
     *
     * @param parent the member that will be the new member's parent
     * @param file the file that the member represents
     */
    public Member(Member parent, File file) {
        this(parent, file, file.getName());
    }

    private void updateNameCache(String n) {
        this.name = n;
        int dot = n.lastIndexOf('.');
        if (dot < 0) {
            extension = "";
            basename = n;
        } else {
            basename = n.substring(0, dot);
            extension = n.substring(dot + 1).toLowerCase(Locale.CANADA);
        }
    }

    /**
     * Returns the member that is this member's parent, or <code>null</code> if
     * this is the member representing the {@link Project} (or if it was
     * explicitly created with a <code>null</code> parent).
     *
     * @return this member's parent, or <code>null</code>
     */
    public final Member getParent() {
        return parent;
    }

    /**
     * Returns the {@link Task} this member belongs to. Returns this member if
     * it is a task or project. If the member is in a project but not in a task
     * folder, the {@link Project} is returned.
     *
     * <p>
     * In the event that the member is not actually in a project, returns
     * <code>null</code> (this is only possible when a member is explicitly
     * created with a <code>null</code> parent).
     *
     * @return the task that owns this member
     */
    public final Task getTask() {
        Member task = this;
        while (task != null && !(task instanceof Task)) {
            task = task.parent;
        }
        return (Task) task;
    }

    /**
     * Returns the {@link Project} this member belongs to.
     *
     * <p>
     * In the event that the member is not actually in a project, returns
     * <code>null</code> (this is only possible when a member is explicitly
     * created with a <code>null</code> parent).
     *
     * @return the project that owns this member
     */
    public final Project getProject() {
        Member project = this;
        while (project.parent != null) {
            project = project.parent;
        }
        if (!(project instanceof Project)) {
            return null;
        }
        return (Project) project;
    }

    /**
     * Returns the file represented by this project member. The result will
     * never be <code>null</code>, although the returned file may not exist if
     * changes have been made to the filesystem that have not yet been detected
     * by the project system.
     *
     * @return the file that this project member refers to
     * @see #synchronize()
     */
    public final File getFile() {
        return file;
    }

    /**
     * Returns a <tt>project:</tt> URL that can be used to access this member
     * whenever this project is the main open project. Returns <code>null</code>
     * if the file has been deleted.
     *
     * @return a project URL for the file.
     */
    public URL getURL() {
        URL result = null;
        File f = getFile();
        if (f.exists()) {
            LinkedList<String> stack = new LinkedList<>();
            Member m = this;
            while (m != null && !(m instanceof Project)) {
                stack.push(m.getName());
                m = m.getParent();
            }
            // if m == null, there is no project parent!
            if (m != null) {
                StringBuilder b = new StringBuilder(f.getPath().length() + 8);
                b.append("project://").append(stack.pop());
                while (!stack.isEmpty()) {
                    b.append('/').append(stack.pop());
                }
                try {
                    result = new URL(b.toString());
                } catch (IOException e) {
                    StrangeEons.log.log(Level.WARNING, null, e);
                }
            }
        }
        return result;
    }

    /**
     * Returns the name of this project member. Typically, this is the same as
     * the name of the corresponding file, including any file name extensions.
     *
     * @return the name of this project member
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the base file name of this project member. The base file name is
     * the same as the file name, less the file name extension and accompanying
     * dot (if any). For example, the base name of "image.png" is "image".
     *
     * @return the base name of the file
     * @see #getName()
     * @see #getExtension()
     */
    public String getBaseName() {
        return basename;
    }

    /**
     * Returns the file name extension of this project member. File name
     * extensions are commonly used to deduce the type of a file. They are the
     * portion of the file's name that falls after a dot character ('.'). For
     * example, if the file name is "image.png", then the returned extension is
     * "png". If the file name has multiple extensions, only the final extension
     * is returned. For example, if the file name is "archive.tar.gz", then this
     * method would return "gz" as the extension. If the file name does not
     * include a dot, then an empty string is returned to indicate that the file
     * name has no extension. Note that this means that the method returns the
     * same extension for files with no extension as it does for files that end
     * in a dot character.
     *
     * @return the file name extension, or an empty string
     * @see #getName()
     * @see #getBaseName()
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Returns an icon that is appropriate for this member. This is essentially
     * a cover method for <code>getMetadataSource().getIcon( this )</code>.
     *
     * @return an icon for this member
     */
    @Override
    public Icon getIcon() {
        return getMetadataSource().getIcon(this);
    }

    /**
     * Recursively closes the project; called via {@link Project#close()}.
     */
    void close() {
        if (isFolder()) {
            for (Member m : getChildrenImpl()) {
                if (m.isFolder()) {
                    m.close();
                }
            }
        }
    }

    /**
     * Returns <code>true</code> if this member is a folder rather than a file.
     * (All {@link Task}s and {@link Project}s are also folders.)
     *
     * @return <code>true</code> if this member represents a folder rather than
     * a regular file
     * @see #hasChildren()
     */
    public boolean isFolder() {
        return file.isDirectory();
    }

    /**
     * Returns <code>true</code> if this member has children.
     *
     * @return <code>true</code> if the member has any children;
     * <code>false</code> otherwise
     */
    public boolean hasChildren() {
        if (children == null) {
            if (file.isDirectory()) {
                return true; // speculate that there are files

//				File[] files = file.listFiles( FILE_FILTER );
//				if( files != null ) {
//					return files.length > 0;
//				}
            }
            return false;
        } else {
            return getChildCount() > 0;
        }
    }

    /**
     * Returns the number of children that this member has (possibly zero).
     *
     * @return the number of direct child members that this member has
     */
    public int getChildCount() {
        return getChildrenImpl().length;
    }

    /**
     * Returns the child member at the specified index. The index must be from 0
     * to <code>getChildCount()</code>-1, inclusive.
     *
     * @param childIndex the index of the child being requested
     * @return the child at the requested index
     * @throws IndexOutOfBoundsException if <code>childIndex</code> &lt; 0 or
     * <code>childIndex</code> &gt;= <code>getChildCount()</code>
     */
    public Member getChildAt(int childIndex) {
        Member[] kids = getChildrenImpl();
        if (childIndex >= kids.length || childIndex < 0) {
            throw new IndexOutOfBoundsException("childIndex: " + childIndex);
        }
        return kids[childIndex];
    }

    /**
     * Returns the children of this member as an array. Excluded and hidden
     * files will not be listed as children of the member (see
     * {@link #setExcludedFilePatterns(java.lang.String[])} for more
     * information).
     *
     * @return an array of this member's children, or an empty array if this is
     * not a folder
     */
    public Member[] getChildren() {
        Member[] kids = getChildrenImpl();
        return kids == NO_CHILDREN ? NO_CHILDREN : kids.clone();
    }

    /**
     * Returns the children of this member as an array. This internal
     * implementation may share the array between invocations. It must not be
     * modified by the caller.
     *
     * @return an array of the member's children
     */
    private Member[] getChildrenImpl() {
        if (file.isDirectory()) {
            if (children == null) {
                File[] files = file.listFiles(FILE_FILTER);
                if (files == null) {
                    // not a dir, or I/O error
                    StrangeEons.log.log(Level.WARNING, "unable to read children of member \"{0}\": bad symlink?", file.getName());
                    return NO_CHILDREN;
                }
                Member[] m = new Member[files.length];
                boolean setJavaHint = false;
                ProjectWatcher watcher = getProject().getWatcher();
                for (int i = 0; i < files.length; ++i) {
                    try {
                        if (this instanceof TaskGroup && Task.isTaskFolder(files[i])) {
                            Task t = new Task((TaskGroup) this, files[i]);
                            Settings s = t.getSettings();
                            // convert subprojects into task groups
                            if (Project.PROJECT_TASK_TYPE.equals(s.get(Task.KEY_TYPE))) {
                                s.set(Task.KEY_TYPE, NewTaskType.TASK_GROUP_TYPE);
                            }
                            // convert tasks into task groups if applicable
                            if (NewTaskType.TASK_GROUP_TYPE.equals(t.getSettings().get(Task.KEY_TYPE))) {
                                t.close();
                                t = new TaskGroup((TaskGroup) this, files[i]);
                            }
                            m[i] = t;
                        } else {
                            m[i] = new Member(this, files[i]);
                            if (!files[i].isDirectory() && m[i].getExtension().equals("java")) {
                                setJavaHint = true;
                            }
                        }
                        // if this is a folder, start watching it for changes
                        if (m[i].isFolder()) {
                            watcher.register(m[i]);
                        }
                    } catch (IOException e) {
                        Toolkit.getDefaultToolkit().beep();
                        StrangeEons.log.log(Level.SEVERE, "error while reading children", e);
                        return NO_CHILDREN;
                    }
                }
                Arrays.sort(m);
                children = m;
                if (setJavaHint) {
                    setJavaSourceHint();
                }
            }
            return children;
        } else {
            return NO_CHILDREN;
        }
    }

    private Member[] children;

    private static final Member[] NO_CHILDREN = new Member[]{};

    private static final FileFilter FILE_FILTER = (File pathname) -> {
        if (pathname.isHidden()) {
            return false;
        }
        String name1 = pathname.getName();
        if (name1.isEmpty() || name1.charAt(0) == '.' || name1.equals(Task.TASK_SETTINGS)) {
            return false;
        }
        return !isFileExcluded(name1);
    };
    private static String[] excludePatterns;
    private static Pattern[] excludePatternsRegex;

    /**
     * Returns an iterator over this member's children. The returned iterator
     * does not support the remove operation.
     *
     * @return an iterator that iterates over all of this member's children
     */
    @Override
    public Iterator<Member> iterator() {
        final Iterator<Member> it;
        if (!hasChildren()) {
            it = Collections.emptyIterator();
        } else {
            it = new Iterator<Member>() {
                int i = 0;
                final Member[] ch = getChildrenImpl();

                @Override
                public boolean hasNext() {
                    return i < ch.length;
                }

                @Override
                public Member next() {
                    if (i >= ch.length) {
                        throw new NoSuchElementException();
                    }
                    return ch[i++];
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
        return it;
    }

    /**
     * Returns the child of this member whose file is equal to the specified
     * file, or <code>null</code> if there is no such child. The file must be an
     * immediate child of this child, not simply a descendant.
     *
     * @param file the child to find
     * @return the child that represents the specified file, or
     * <code>null</code>
     * @see #findChild(java.lang.String)
     */
    public Member findChild(File file) {
        if (file == null) {
            throw new NullPointerException("name ");
        }
        for (Member m : getChildrenImpl()) {
            if (m.getFile().equals(file)) {
                return m;
            }
        }
        return null;
    }

    /**
     * Finds a child of this member by name. Returns <code>null</code> if there
     * is no such child. If the specified name contains one or more slashes
     * ('/'), then the find operation will be repeated recursively for each path
     * segment. For example, <code>findChild( "nested/folder/file.png" )</code>
     * is equivalent to calling
     * <code>findChild( "nested" ).findChild( "folder" ).findChild( "file.png" )</code>,
     * except that if any of the calls returns <code>null</code>, the process
     * stops immediately and the method returns <code>null</code>.
     *
     * @param name the member name to search for
     * @return the child with this name, or <code>null</code>
     */
    public Member findChild(String name) {
        if (name == null) {
            throw new NullPointerException("name ");
        }

        // quickly check if the argument is an immediate child
        for (Member m : getChildrenImpl()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }

        // not an immediate child, check for "/" path before giving up
        Member parent = this;
        if (name.indexOf('/') >= 0) {
            String[] tokens = name.split("\\/");
            for (String token : tokens) {
                Member child = parent.findChild(token);
                if (child == null) {
                    return null;
                }
                parent = child;
            }
            return parent;
        }

        return null;
    }

    /**
     * Returns the index at which the specified member can be found as a child
     * of this member. If the specified member is not a child of this member,
     * returns -1.
     *
     * @param potentialChild the member to search for
     * @return the index of the specified member in the immediate children of
     * this member, or <code>null</code>
     * @see #getChildAt(int)
     * @see #findChild(java.lang.String)
     */
    public int getIndex(Member potentialChild) {
        if (potentialChild == null) {
            throw new NullPointerException("node");
        }
        Member[] ch = getChildrenImpl();
        for (int i = 0; i < ch.length; ++i) {
            if (ch[i] == potentialChild) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Renames the file associated with this member. Calling this method is
     * preferable to calling <code>getFile().renameTo</code> because this method
     * correctly handles additional details and special cases that the
     * <code>File</code> method is unaware of, such as updating any editors
     * displaying the file and informing
     * {@link ca.cgjennings.apps.arkham.project.Rename.RenameListener RenameListener}s.
     *
     * @param newFileName the new name for the member
     * @throws IOException if an I/O error occurs during renaming
     */
    public void renameFile(String newFileName) throws IOException {
        Rename.rename(this, new File(getFile().getParent(), newFileName), null);
    }

    /**
     * Deletes the file associated with this member. <i>If this member is a
     * folder, then any children it may have will also be deleted.</i>
     * Calling this method is preferable to calling
     * <code>getFile().delete()</code> because this method correctly handles
     * additional details and special cases that the <code>File</code> method is
     * unaware of, such as updating any editors displaying the file and
     * informing
     * {@link ca.cgjennings.apps.arkham.project.Rename.RenameListener RenameListener}s.
     *
     * @throws IOException if an I/O error prevents the file from being deleted
     * @see ProjectUtilities#deleteAll(java.io.File)
     */
    public void deleteFile() throws IOException {
        Project p = null;
        File file = getFile();
        boolean folder = isFolder();
        if (folder) {
            // if this is a folder it may have children that are members
            // as well as hidden files that are not; we need to delete everything
            // before we can delete the parent
            for (Member child : getChildrenImpl()) {
                child.deleteFile();
            }
            File[] hiddenChildren = file.listFiles();
            if (hiddenChildren != null) {
                for (File hc : hiddenChildren) {
                    deleteHiddenChild(hc);
                }
            }

            // since this is a folder, stop watching it for changes; this may
            // be required in order to remove an OS lock and delete the file
            p = getProject();
            if (p != null) {
                p.getWatcher().unregister(this);
            }
        }

        IOException failure = null;
        try {
            Files.deleteIfExists(file.toPath());

            // The file was deleted:
            // (1) fire a delete event
            Rename.fireRenameEvent(p == null ? getProject() : p, this, file, null);
            // (2) close any editors showing the file
            for (StrangeEonsEditor ed : StrangeEons.getWindow().getEditorsShowingFile(file)) {
                if (ed instanceof AbstractSupportEditor) {
                    ((AbstractSupportEditor) ed).setUnsavedChanges(false);
                    ed.close();
                } else if (ed.getGameComponent() != null) {
                    ed.getGameComponent().markSaved();
                    ed.close();
                }
            }
        } catch (IOException e) {
            failure = e;
        }

        if (failure != null) {
            // failed to delete, need to re-register the folder
            if (p != null) {
                p.getWatcher().register(this);
            }
            throw failure;
        }
    }

    /**
     * Part of the implementation of deleteFile.
     */
    private void deleteHiddenChild(File f) throws IOException {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File k : files) {
                    deleteHiddenChild(k);
                }
            }
        }
        Files.delete(f.toPath());
    }

    /**
     * Sets a list of file name patterns that will be ignored by the project
     * system. Any file name that matches one of the specified patterns will be
     * ignored. It will not be returned as one of the children of its parent
     * member and it will not appear in project views. Each pattern in the array
     * is a string (empty or <code>null</code> strings in the array will be
     * ignored). The patterns can either specify an exact name to match or they
     * can include wildcards: the * wildcard matches 0 or more characters, while
     * the ? wildcard matches any one character. For example, the pattern
     * "*.gif" would exclude all files ending in ".gif". File patterns are case
     * insensitive: in the previous example, a file named "SAMPLE.GIF" would
     * also be excluded.
     *
     * <p>
     * <b>Excluded versus Hidden:</b><br>
     * When a member's children are requested, files that are <i>excluded</i>
     * or <i>hidden</i> are not listed. The difference is that excluded files
     * (files that match one of the excluded patterns set with this method) are
     * not considered to be part of the project at all, while hidden files are
     * parts of the project that are not shown to the user. For example, task
     * folders include a configuration file that is hidden so that it does not
     * appear in the project view, but the configuration file is still part of
     * the project (it is required for the project to function correctly!).
     * Files that start with a '.' or that are considered hidden by the
     * operating system (on Windows, files have a "hidden" attribute that can be
     * set hide the file) are also considered hidden by projects.
     *
     * <p>
     * Most of the time there is no practical difference between hidden or
     * excluded files for plug-in authors. The main exception is that if you are
     * copying project files from one place to another, you should leave out
     * excluded files but copy hidden files. To do this, you could get a list of
     * files from the parent member using
     * <code>member.getFile().listFiles()</code>, then skip any files for which
     * {@link #isFileExcluded(java.io.File)} returns <code>true</code>.
     *
     * @param patterns an array of pattern strings
     * @see #getExcludedFilePatterns()
     * @see #isFileExcluded(java.io.File)
     */
    public static void setExcludedFilePatterns(String... patterns) {
        if (patterns != null && patterns.length > 0) {
            // ensure entries are unique
            HashSet<String> set = new HashSet<>();
            for (int i = 0; i < patterns.length; ++i) {
                set.add(patterns[i]);
            }
            set.remove("");
            set.remove(null);
            if (set.size() > 0) {
                excludePatterns = set.toArray(new String[set.size()]);
                excludePatternsRegex = new Pattern[excludePatterns.length];
                for (int i = 0; i < excludePatterns.length; ++i) {
                    if (excludePatterns[i].indexOf('*') < 0 && excludePatterns[i].indexOf('.') < 0) {
                        // exact match required, skip regexp for speed
                        excludePatternsRegex[i] = null;
                    } else {
                        // convert * and . glob chars to regular expression
                        String regex = Pattern.quote(excludePatterns[i])
                                .replace("?", "\\E.\\Q")
                                .replace("*", "\\E.*\\Q");
                        excludePatternsRegex[i] = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                    }
                }
                return;
            }
        }
        excludePatterns = null;
        excludePatternsRegex = null;
    }

    /**
     * Returns an array containing the file name patterns that will be ignored
     * by the project system.
     *
     * @return a copy of the patterns that will be ignored by the project system
     * @see #setExcludedFilePatterns
     */
    public static String[] getExcludedFilePatterns() {
        return excludePatterns == null ? new String[0] : excludePatterns.clone();
    }

    /**
     * Returns <code>true</code> if file exclusion patterns have been set and
     * <code>f</code> matches one of the patterns.
     *
     * @param f the file to test
     * @return <code>true</code> if the file matches an active exclusion pattern
     */
    public static boolean isFileExcluded(File f) {
        if (f == null) {
            throw new NullPointerException();
        }
        return isFileExcluded(f.getName());
    }

    /**
     * Returns <code>true</code> if file exclusion patterns have been set and
     * the specified file name matches one of the patterns. Note that the
     * project system may exclude files for other reasons as well. In
     * particular, it will exclude files that start with '.', have the hidden
     * attribute set (on Windows), or use the special reserved name for task
     * folder settings (seproject).
     *
     * @param f the file to test
     * @return <code>true</code> if the file matches an active exclusion pattern
     */
    static boolean isFileExcluded(String name) {
        if (excludePatternsRegex == null) {
            return false;
        }
        for (int i = 0; i < excludePatternsRegex.length; ++i) {
            // pattern has no glob characters, match exactly name
            if (excludePatternsRegex[i] == null) {
                if (name.equalsIgnoreCase(excludePatterns[i])) {
                    return true;
                }
            } else {
                if (excludePatternsRegex[i].matcher(name).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Compares two project members. This method is designed to sort members in
     * the same folder into a nice order for display. It is not intended for
     * general comparison of arbitrary project members.
     *
     * @param rhs the member to compare this member to
     * @return a negative number, zero, or a positive number indicating whether
     * this member should be displayed before, at the same position as, or after
     * the specified member
     */
    @Override
    public int compareTo(Member rhs) {
        if (rhs == null) {
            return -1;
        }
        if (this instanceof Project) {
            if (!(rhs instanceof Project)) {
                return -1;
            }
        } else if (rhs instanceof Project) {
            return 1;
        }
        if (this instanceof Task) {
            if (!(rhs instanceof Task)) {
                return -1;
            }
        } else if (rhs instanceof Task) {
            return 1;
        }
        if (this.isFolder()) {
            if (!rhs.isFolder()) {
                return -1;
            }
        } else if (rhs.isFolder()) {
            return 1;
        }
        return file.getName().compareTo(rhs.file.getName());
    }

    /**
     * Returns <code>true</code> if and only if the specified object is a
     * project member for the same file as this project member.
     *
     * @param obj the object to compare this object to
     * @return if the object is a member for the same underlying file
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this ? true : (obj == null ? false : (obj instanceof Member ? ((Member) obj).file.equals(file) : false));
    }

    /**
     * Returns a hash code for this member.
     *
     * @return a hash code
     */
    @Override
    public int hashCode() {
        return file.hashCode();
    }

    /**
     * Returns the project member's base name.
     *
     * @return the member's base name
     * @see #getBaseName()
     */
    @Override
    public String toString() {
        return basename;
    }

    /**
     * Insert a new file as a child of this member. This is not usually called
     * directly. Instead, call {@link #synchronize()} to update this member's
     * children.
     *
     * @param file the newly added file to insert
     */
    Member insert(File file) {
        if (!isFolder()) {
            throw new UnsupportedOperationException("only folders can have children added");
        }

        Member m = null;
        if (this instanceof TaskGroup && Task.isTaskFolder(file)) {
            try {
                Task t = new Task((TaskGroup) this, file);
                Settings s = t.getSettings();
                // convert subprojects into task groups
                if (Project.PROJECT_TASK_TYPE.equals(s.get(Task.KEY_TYPE))) {
                    s.set(Task.KEY_TYPE, NewTaskType.TASK_GROUP_TYPE);
                }
                // convert tasks into task groups if applicable
                if (NewTaskType.TASK_GROUP_TYPE.equals(t.getSettings().get(Task.KEY_TYPE))) {
                    t.close();
                    t = new TaskGroup((TaskGroup) this, file);
                }
                m = t;
            } catch (IOException e) {
                System.err.println("Failed to create Task when synchronizing folder:");
                e.printStackTrace();
            }
        }
        if (m == null) {
            m = new Member(this, file);
        }
        insert(m);
        return m;
    }

    /**
     * Insert a new member as a child of this member. This is not normally
     * called directly. Instead use {@link #synchronize()} to update this
     * member's children.
     *
     * @param m the member to insert
     */
    void insert(Member m) {
        if (m == null) {
            NullPointerException e = new NullPointerException();
            StrangeEons.log.log(Level.WARNING, "tried to insert null member", e);
            return;
        }

        if (!isFolder()) {
            throw new UnsupportedOperationException("only folders can have children added");
        }

        Member[] children = getChildrenImpl();

        // find the insertion sort point for the new member
        int ip = 0;
        for (; ip < children.length; ++ip) {
            if (children[ip].compareTo(m) > 0) {
                break;
            }
        }

        Member[] kids = new Member[children.length + 1];

        int j = 0;
        for (int i = 0; i <= children.length; ++i) {
            if (i == ip) {
                kids[j++] = m;
            }
            if (i == children.length) {
                break;
            }
            kids[j++] = children[i];
        }

        this.children = kids;

        final DefaultTreeModel model = getJTreeModel();
        if (model != null) {
            model.nodesWereInserted(this.asTreeNode(), new int[]{ip});
        }
        if (!m.file.isDirectory() && m.getExtension().equals("java")) {
            setJavaSourceHint();
        }

        // if this is a folder, start watching it for changes
        if (m.isFolder()) {
            getProject().getWatcher().register(m);
        }
    }

    private void setJavaSourceHint() {
        Task t = getTask();
        if (t == null) {
            t = getProject();
        }
        if (t != null) {
            t.getSettings().set("java-source-hint", "yes");
        }
    }

    void remove() {
        if (parent != null) {
            close();
            int index = parent.getIndex(this);
            if (index >= 0) {
                Member[] kids = parent.getChildrenImpl();
                Member[] kept = new Member[kids.length - 1];
                int dest = 0;
                for (int source = 0; source < kids.length; ++source) {
                    if (source == index) {
                        continue;
                    }
                    kept[dest++] = kids[source];
                }
                parent.children = kept;

                DefaultTreeModel model = getJTreeModel();
                if (model != null) {
                    int[] childIndices = new int[]{index};
                    Object[] removedChildren = new Object[]{this};
                    model.nodesWereRemoved(parent.asTreeNode(), childIndices, removedChildren);
                }
            }
            parent = null;
        }
    }

    /**
     * Hints to the project that this is a good time to update this node's
     * children to reflect changes to the file system. This method is typically
     * called after creating, deleting, or updating a number of this member's
     * children.
     *
     * <p>
     * Projects monitor their members for changes. For example, a folder in a
     * project will detect when a new file has been added to it and update its
     * children accordingly. However, there is typically a short delay before
     * such changes will be reflected in the project view. The exact delay is
     * platform dependent, but it is typically between 100-5000&nbsp;ms. This
     * delay allows changes to the project structure to be made in bulk when a
     * large number of files are being updated. Calling this method is suggests
     * two things to the project system: that the contents of this node have
     * been modified, and that now is a good time to resynchronize the project's
     * member tree with the actual files on disk. Thus, calling this method is
     * entirely optional, but it may make the project view appear more
     * responsive.
     *
     * <p>
     * <b>Note:</b>
     * If you write files to a project folder from a thread other than the event
     * dispatch thread, you can safely update the project from that thread by
     * calling this method. However, it is not safe in general to work with
     * project members from outside of the EDT. Instead, keep a reference to the
     * member that requires updating, and work only with <code>File</code>
     * objects within the other thread (except for calling this method).
     */
    public void synchronize() {
        Project p = getProject();
        if (p != null) {
            p.getWatcher().synchronize(this);
        }
    }

    /**
     * Called by the project watcher when this member folder needs to update its
     * contents.
     */
    void synchronizeImpl() {
        if (!EventQueue.isDispatchThread()) {
            StrangeEons.log.finest("request to synchronize() reposted to event dispatch thread");
            EventQueue.invokeLater(this::synchronize);
            return;
        }

        cachedMDSource = null;
        File file = getFile();

        if (!file.exists() && parent != null) {
            remove();
            return;
        }

        File[] contents = file.listFiles(FILE_FILTER);

        // this member is not (or is no longer) a directory
        if (contents == null) {
            // used to be a directory but isn't anymore
            if (children != null && getChildCount() > 0) {
                children = NO_CHILDREN;
                DefaultTreeModel model = getJTreeModel();
                if (model != null) {
                    model.nodeStructureChanged(this.asTreeNode());
                }
            }
        } // this member is a directory
        else {
            // find the view so we can remember if this was collpased or expanded
            // if we add/remove we'll need to restore this state later
            Project project = getProject();
            ProjectView view = project == null ? null : project.getView();
            boolean expand = false;
            if (view != null) {
                expand = !view.isFolderCollpased(this);
            }
            Member[] ch = getChildrenImpl().clone();
            // we use file names instead of files so that if the case of a name
            // changes on a case insensitive file system, we pick up the change
            HashSet<String> kids = new HashSet<>(contents.length);
            for (File f : contents) {
                kids.add(f.getName());
            }

            for (int i = 0; i < ch.length; ++i) {
                if (ch[i] == null) {
                    StrangeEons.log.log(Level.WARNING, "Warning: ch[{0}] == null", i);
                    continue;
                }
                if (ch[i].getFile() == null) {
                    StrangeEons.log.log(Level.WARNING, "Warning: ch[{0}].getFile() == null", i);
                    continue;
                }
                if (kids.contains(ch[i].getFile().getName())) {
                    kids.remove(ch[i].getFile().getName());
                } else {
                    // file was deleted
                    ch[i].remove();
                }
            }

            // anything left in kids is new
            for (String newKid : kids) {
                insert(new File(file, newKid));
            }

            if (expand) {
                view.expandFolder(this);
            }
        }
    }

    /**
     * Returns <code>true</code> if this is an ancestor of <code>child</code>.
     *
     * @param child the member to test
     * @return <code>true</code> if and only if <code>child</code> is a direct
     * or indirect child of this member
     */
    public boolean isAncestorOf(Member child) {
        if (equals(child)) {
            return false;
        }
        child = child.getParent();
        while (child != null) {
            if (equals(child)) {
                return true;
            }
            child = child.getParent();
        }
        return false;
    }

    /**
     * Get the tree model that is informed of changes to the project. A tree
     * model may be set on the containing {@link Project}. This method is a
     * convenience that queries the project (if any) and returns the current
     * model. It returns <code>null</code> if there is no project or if the
     * project has no model set.
     *
     * @return a tree model that should be updated when the project structure
     * changes
     */
    DefaultTreeModel getJTreeModel() {
        Project p = getProject();
        return p != null ? p.getJTreeModel() : null;
    }

    /**
     * Returns a {@link MetadataSource} that is appropriate for fetching
     * metadata for a file of the type represented by this member.
     *
     * @return the metadata source that is best suited for this member
     */
    public MetadataSource getMetadataSource() {
        if (cachedMDSource == null) {
            for (MetadataSource mds : MD_SOURCES) {
                if (mds.appliesTo(this)) {
                    cachedMDSource = mds.getSpecificInstanceFor(this);
                    break;
                }
            }
        }
        return cachedMDSource;
    }
    private MetadataSource cachedMDSource;

    private static final LinkedList<MetadataSource> MD_SOURCES;

    static {
        MD_SOURCES = new LinkedList<>();
        MD_SOURCES.add(new MetadataSource());
        MD_SOURCES.addFirst(new MetadataSource.DictionaryMetadata());
        MD_SOURCES.addFirst(new MetadataSource.PropertiesMetadata());
        MD_SOURCES.addFirst(new MetadataSource.BundleMetadata());
        MD_SOURCES.addFirst(new MetadataSource.ImageMetadata());
        MD_SOURCES.addFirst(new MetadataSource.GameComponentMetadata());
        MD_SOURCES.addFirst(new MetadataSource.TextMetadata());
        MD_SOURCES.addFirst(new MetadataSource.HTMLMetadata());
        MD_SOURCES.addFirst(new MetadataSource.CopiesMetadata());
    }

    /**
     * Adds a new source of metadata. Sources are always checked in most recent
     * first order. The first source that applies to a given member will be
     * returned as its source. Note, however, that sources can be cached. After
     * adding a new source to a live project, synchronize relevant members to
     * clear any cached sources.
     *
     * @param source the source to add
     */
    public static void registerMetadataSource(MetadataSource source) {
        unregisterMetadataSource(source);
        MD_SOURCES.addFirst(source);
    }

    /**
     * Removes a previously registered source of metadata.
     *
     * @param source the source to remove
     */
    public static void unregisterMetadataSource(MetadataSource source) {
        if (source == MD_SOURCES.getLast()) {
            throw new IllegalArgumentException("cannot remove or reorder default source");
        }
        MD_SOURCES.remove(source);
    }

    /**
     * This class is an adapter that bridges between <code>TreeNode</code>s and
     * <code>Member</code>s, allowing projects to be displayed in a
     * <code>JTree</code> without polluting the <code>Member</code> API with
     * tree node methods.
     */
    static final class MemberTreeNode implements TreeNode {

        private Member m;

        public MemberTreeNode(Member memberToWrap) {
            m = memberToWrap;
        }

        public Member asMember() {
            return m;
        }

        @Override
        public TreeNode getChildAt(int childIndex) {
            return m.getChildAt(childIndex).asTreeNode();
        }

        @Override
        public int getChildCount() {
            return m.getChildCount();
        }

        @Override
        public TreeNode getParent() {
            Member p = m.getParent();
            return p == null ? null : p.asTreeNode();
        }

        @Override
        public int getIndex(TreeNode node) {
            return m.getIndex(((MemberTreeNode) node).m);
        }

        @Override
        public boolean getAllowsChildren() {
            return m.hasChildren();
        }

        @Override
        public boolean isLeaf() {
            return !m.hasChildren();
        }

        @Override
        public Enumeration<TreeNode> children() {
            return new Enumeration<TreeNode>() {
                int i = 0;
                final Member[] ch = m.getChildrenImpl();

                @Override
                public boolean hasMoreElements() {
                    return i < ch.length;
                }

                @Override
                public TreeNode nextElement() {
                    if (i >= ch.length) {
                        throw new NoSuchElementException();
                    }
                    return ch[i++].asTreeNode();
                }
            };
        }

        @Override
        public String toString() {
            return "MemberTreeNode<" + m.getName() + '>';
        }
    }

    /**
     * Returns a view of this member as a {@link MemberTreeNode}.
     *
     * @return a tree node suitable for displaying this member in a tree
     */
    MemberTreeNode asTreeNode() {
        if (node == null) {
            node = new MemberTreeNode(this);
        }
        return node;
    }

    private MemberTreeNode node;
}
