package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.BusyDialog;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.ui.theme.ThemedIcon;
import java.awt.EventQueue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import javax.swing.tree.DefaultTreeModel;
import static resources.Language.string;

/**
 * Projects simplify the management of multi-file projects.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class Project extends TaskGroup {

    /**
     * Opens an existing project. If the folder is not a project folder, throws
     * an exception.
     */
    public Project(File baseFolder) throws IOException {
        super(null, baseFolder);
        if (!PROJECT_TASK_TYPE.equals(getSettings().get(KEY_TYPE))) {
            throw new IllegalArgumentException("Not a project");
        }

        watcher = new ProjectWatcher(this);
    }

    /**
     * Opens an existing project, either from a project folder or from a project
     * package file.
     *
     * @param f the project folder or package file to open
     * @return the opened project
     * @throws IOException if an error occurs while opening the project
     */
    public static Project open(File f) throws IOException {
        if (f.isDirectory()) {
            return new Project(f);
        }
        return fromPackage(f);
    }

    /**
     * Returns the canonical {@link ProjectView} associated with this project,
     * or {@code null} if no view is associated with it.
     *
     * @return the canonical project view displaying this project, or
     * {@code null}
     */
    public ProjectView getView() {
        ProjectView view = ProjectView.getCurrentView();
        return view.getProject().equals(this) ? view : null;
    }

    @Override
    public URL getURL() {
        try {
            return new URL("project:");
        } catch (MalformedURLException e) {
            StrangeEons.log.log(Level.SEVERE, null, e);
            throw new AssertionError();
        }
    }

    /**
     * Returns the {@link Member} instance pointed to by a URL. The URL protocol
     * must either be {@code file:} or {@code project:}. If the file specified
     * by the URL is not part of the project, {@code null} is returned.
     *
     * @param url the URL to locate a project member for
     * @return the project member described by the URL, or {@code null}
     * @since 3.0
     */
    @SuppressWarnings("empty-statement")
    public Member findMember(URL url) {
        File toFind = null;
        String proto = url.getProtocol();
        switch (proto) {
            case "file":
                try {
                toFind = new File(url.toURI());
            } catch (URISyntaxException ex) {
            }
            break;
            case "project":
                String path = url.getHost() + '/' + url.getPath();
                int skip;
                for (skip = 0; skip < path.length() && path.charAt(skip) == '/'; ++skip);
                path = path.substring(skip);
                toFind = new File(getFile(), path.replace('/', File.separatorChar));
                break;
            default:
                throw new IllegalArgumentException("protocol must be file: or project:");
        }
        return toFind == null ? null : findMember(toFind);
    }

    /**
     * Returns the {@link Member} instance that represents a file in a project.
     * If the file is not part of the project, then {@code null} is returned.
     *
     * @param file the file to locate in the project's member tree
     * @return the project member for {@code file}, or {@code null}
     */
    public Member findMember(File file) {
        if (file == null) {
            throw new NullPointerException("file");
        }
        try {
            file = file.getCanonicalFile();
        } catch (IOException e) {
            StrangeEons.log.log(Level.WARNING, "unable to get canonical file: " + file, e);
            file = file.getAbsoluteFile();
        }
        if (file.equals(getFile())) {
            return this;
        }

        File climber = file;
        LinkedList<String> parents = new LinkedList<>();

        while (climber != null) {
            if (climber.equals(getFile())) {
                break;
            }
            parents.push(climber.getName());
            climber = climber.getParentFile();
        }

        // the file does not descend from the project
        if (climber == null) {
            return null;
        }

        // we now have a stack that is a traversal through the project that
        // leads to this file's member
        Member target = this;
        while (!parents.isEmpty()) {
            String child = parents.pop();

            // Search this member's children for the target; we try this up
            // to two times. If we don't find the target the first time,
            // then we will synchronize the parent and try again.
            Member next = null;
            attemptLoop:
            for (int attempt = 0; attempt < 2; ++attempt) {
                Member[] kids = target.getChildren();
                for (int i = 0; i < kids.length; ++i) {
                    if (kids[i].getFile().getName().equals(child)) {
                        next = kids[i];
                        break attemptLoop;
                    }
                }
                target.synchronize();
            }

            // the target doesn't exist
            if (next == null) {
                return null;
            }

            // look for the next path component
            target = next;
        }

        // safety check: double check that the file exists before returning
        if (target != null && !target.getFile().exists()) {
            target = null;
        }

        return target;
    }

    /**
     * Returns {@code true} if the specified file exists and is a project
     * folder.
     *
     * @param file the file to test
     * @return {@code true} if the file is a regular project folder
     * @throws NullPointerException if the file is {@code null}
     */
    public static final boolean isProjectFolder(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("file");
        }
        if (Task.isTaskFolder(file)) {
            Task t = new Task(null, file);
            return PROJECT_TASK_TYPE.equals(t.getSettings().get(KEY_TYPE));
        }
        return false;
    }

    /**
     * Returns {@code true} if the specified file exists and is a packaged
     * project container.
     *
     * @param file the file to test
     * @return {@code true} if the file is a packaged project
     * @throws NullPointerException if the file is {@code null}
     */
    public static final boolean isProjectPackage(File file) {
        return file.exists() && !file.isDirectory() && file.getName().endsWith(".seproject");
    }

    /**
     * Creates a new project folder with the specified project name. The project
     * will be created as a subfolder of {@code projectFolder}. The returned
     * file can be used to create a {@code Project} instance for the new project
     * using the {@link #Project(java.io.File)} constructor.
     *
     * @param projectFolder the folder to create the project in
     * @param projectName the name of the new project
     * @return the new project folder's file
     * @throws IOException if the project folder does not exist, or if it exists
     * but has a child with the same name as the project name, or if the project
     * folder cannot be created for some reason
     */
    public static File createProject(File projectFolder, String projectName) throws IOException {
        return createTask(projectFolder, projectName, PROJECT_TASK_TYPE);
    }

    public static final String PROJECT_TASK_TYPE = "PROJECT";

    private DefaultTreeModel treeModel;

    /**
     * Get the tree model that is informed of changes to the project.
     *
     * @return the model that is updated in response to project changes
     */
    @Override
    DefaultTreeModel getJTreeModel() {
        return treeModel;
    }

    /**
     * Set a tree model that should be informed of changes to the project. May
     * be {@code null}.
     *
     * @param model the model to update when changes occur
     */
    void setJTreeModel(DefaultTreeModel model) {
        treeModel = model;
    }

    /**
     * Returns an icon that is appropriate for this member, in this case the
     * standard icon for projects.
     *
     * @return an icon for this member
     */
    @Override
    public ThemedIcon getIcon() {
        return (ThemedIcon) MetadataSource.ICON_PROJECT;
    }

    public void synchronizeAll() {
        synchronizeAllImpl(this);
    }

    private void synchronizeAllImpl(Member m) {
        m.synchronize();
        for (Member k : m.getChildren()) {
            if (k.isFolder()) {
                synchronizeAllImpl(k);
            }
        }
    }

    /**
     * Returns the project watcher that watches the project for file changes.
     *
     * @return the watcher for this project
     */
    ProjectWatcher getWatcher() {
        return watcher;
    }
    private ProjectWatcher watcher;

    /**
     * Writes the contents of this project to a package project ("crate") file.
     * A packaged project is essentially a ZIP archive that uses the file
     * extension {@code .seproject}. When a packaged project is opened in the
     * application, it will unpack the file to a temporary folder and open that
     * folder as a project. When the project is closed, it will then copy the
     * modified package back to the original package file. If a project was
     * opened from a package file, then {@link #getPackageFile()} will return a
     * non-{@code null} value.
     *
     * @param pkg the file to write the package archive to
     * @throws IOException if an I/O error occurs while writing the package file
     */
    public void toPackage(final File pkg) throws IOException {
        pkgThrow = null;
        new BusyDialog(StrangeEons.getWindow(), string("prj-write-pkg"), () -> {
            FileOutputStream out = null;
            JarOutputStream jar = null;
            try {
                out = new FileOutputStream(pkg);
                jar = new JarOutputStream(out);
                jar.setLevel(9);
                ProjectUtilities.copyToArchive(jar, getFile(), "", true, true, false);
            } catch (IOException e) {
                pkgThrow = e;
                return;
            } finally {
                if (jar != null) {
                    try {
                        jar.close();
                    } catch (IOException e) {
                        pkgThrow = e;
                    }
                } else if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                    }
                }
            }
        });
        if (pkgThrow != null) {
            IOException e = pkgThrow;
            pkgThrow = null;
            throw e;
        }
    }

    /**
     * Unpack a project package file into a project folder. The project folder
     * will be a subfolder of {@code targetParent}, which must be a folder.
     *
     * @param pkg the package file
     * @param targetParent the intended parent of the project folder
     * @return the unpacked project folder
     * @throws IOException if an error occurs while unpacking the package
     */
    public static File unpackage(final File pkg, final File targetParent) throws IOException {
        if (!EventQueue.isDispatchThread()) {
            throw new UnsupportedOperationException("call from dispatch thread");
        }
        if (targetParent != null && !targetParent.isDirectory()) {
            throw new IllegalArgumentException("targetParent must be a folder");
        }

        pkgThrow = null;
        pkgTemp = null;
        new BusyDialog(StrangeEons.getWindow(), string("prj-read-pkg"), () -> {
            try {
                // determine folder to unpack to; if null use temp folder
                File baseFolder = targetParent;
                if (baseFolder == null) {
                    File arch = File.createTempFile("sepkg", "");
                    arch.deleteOnExit();
                    baseFolder = new File(arch.getAbsolutePath() + "_folder");
                    int attempt = 0;
                    for (attempt = 0; attempt < 100; ++attempt) {
                        baseFolder = ProjectUtilities.getAvailableFile(baseFolder);
                        if (baseFolder.mkdir()) {
                            break;
                        }
                    }
                    if (attempt == 100) {
                        throw new IOException("Unable to create unpacking folder");
                    }
                    baseFolder.deleteOnExit();
                }
                // unpack the package content
                JarFile jar = null;
                try {
                    jar = new JarFile(pkg);
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry e = entries.nextElement();
                        if (e.isDirectory()) {
                            continue;
                        }
                        BusyDialog.statusText(e.getName(), 200);
                        File dest = new File(baseFolder, e.getName().replace('/', File.separatorChar));
                        File parent1 = dest.getParentFile();
                        if (!parent1.exists()) {
                            parent1.mkdirs();
                        }
                        try (InputStream src = jar.getInputStream(e)) {
                            ProjectUtilities.copyStream(src, dest);
                        }
                        if (e.getTime() != -1) {
                            dest.setLastModified(e.getTime());
                        }
                    }
                } finally {
                    if (jar != null) {
                        try {
                            jar.close();
                        } catch (IOException e) {
                            StrangeEons.log.log(Level.WARNING, "unexpected", e);
                        }
                    }
                }
                // there should only be one file in the root of the temp folder,
                // but we cycle through them to be sure
                File projFolder = null;
                for (File f : baseFolder.listFiles()) {
                    if (Project.isProjectFolder(f)) {
                        projFolder = f;
                        break;
                    }
                }
                if (projFolder == null) {
                    throw new IOException("No project folder in package");
                }
                pkgTemp = projFolder;
            } catch (IOException e) {
                pkgThrow = e;
            }
        });

        // recover retvals from busy thread
        if (pkgThrow != null) {
            IOException e = pkgThrow;
            pkgThrow = null;
            pkgTemp = null;
            throw e;
        }
        File projFolder = pkgTemp;
        pkgTemp = null;
        return projFolder;
    }

    // stores any exception during (un)packaging; note that packaging methods
    // must be called only from the EDT or an exception is thrown
    private static IOException pkgThrow;
    private static File pkgTemp;

    static Project fromPackage(File pkg) throws IOException {
        File projFolder = unpackage(pkg, null);

        // create a project for this folder and record the package so that
        // we copy changes back to the package
        Project proj = new Project(projFolder);
        proj.packageFile = pkg;
        return proj;
    }

    private File packageFile;

    /**
     * If this project is stored in a package, returns the package file.
     * Otherwise, returns {@code null}.
     *
     * @return returns the package file for this project
     */
    public File getPackageFile() {
        return packageFile;
    }

    public static final String KEY_RESOURCE_ID = "res-id";

    /**
     * Closes the project. A project must be closed when no longer needed to
     * make sure that any pending changes get written to the file system. If the
     * project is a packaged type, then closing the project will also update the
     * original package archive to reflect the current state of the project.
     */
    @Override
    public void close() {
        super.close();
        if (packageFile != null) {
            try {
                toPackage(packageFile);
                packageFile = null;
                ProjectUtilities.deleteAll(getFile());
            } catch (IOException e) {
                ErrorDialog.displayError(string("prj-err-write-pkg"), e);
            }
        }
        try {
            watcher.close();
        } catch (Throwable t) {
            StrangeEons.log.log(Level.SEVERE, null, t);
        }
    }

    /**
     * Returns an iterator over all task folders in the project, including the
     * project itself.
     *
     * @return an iterator that iterates over all of the tasks in the project
     */
    public Iterator<Task> taskIterator() {
        List<Task> tasks = new LinkedList<>();
        buildIterator(tasks, this);
        return tasks.iterator();
    }

    private static void buildIterator(List<Task> list, Task task) {
        if (task instanceof TaskGroup) {
            for (Member m : task.getChildren()) {
                if (m instanceof Task) {
                    buildIterator(list, (Task) m);
                }
            }
        }
        list.add(task);
    }
}
