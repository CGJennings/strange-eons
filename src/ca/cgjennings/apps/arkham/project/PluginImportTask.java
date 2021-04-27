package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.BusyDialog;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.plugins.PluginBundle;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * Unpack an existing plug-in into a task folder.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class PluginImportTask extends NewTaskType {

    public PluginImportTask() {
    }

    private PluginImportTask(File bundleToImport) {
        defaultBundle = bundleToImport;
    }

    private File defaultBundle;

    @Override
    public String getLabel() {
        return string("pt-import-plugin-name");
    }

    @Override
    public String getDescription() {
        return string("pt-import-plugin-desc");
    }

    @Override
    public String getType() {
        return NewTaskType.PLUGIN_TYPE;
    }

    protected static void importZipFile(ZipFile zip, File parent) throws IOException {
        BusyDialog feedback = BusyDialog.getCurrentDialog();
        if (feedback != null) {
            feedback.setProgressMaximum(zip.size());
        }

        int progress = 0;
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            String name = entry.getName().replace("/", System.getProperty("file.separator"));
            if (entry.isDirectory()) {
                name = name.substring(0, name.length() - 1);
            }

            File dest = new File(parent, name);

            if (entry.isDirectory()) {
                dest.mkdirs();
            } else {
                if (feedback != null) {
                    feedback.setStatusText(entry.getName());
                }
                dest.getParentFile().mkdirs();
                InputStream in = zip.getInputStream(entry);
                ProjectUtilities.copyStream(in, dest);
            }
            if (feedback != null) {
                feedback.setProgressCurrent(++progress);
            }
        }
    }

    @Override
    public boolean initializeNewTask(Project project, Task task) {
        File bundle = defaultBundle;
        if (bundle == null) {
            bundle = ResourceKit.showPluginFileDialog(StrangeEons.getWindow());
        }
        initializeNewTaskImpl(task, bundle);
        return true;
    }

    private static void initializeNewTaskImpl(Task task, final File bundle) {
        // check for user cancellation
        if (bundle == null) {
            ProjectUtilities.deleteAll(task.getFile());
            return;
        }

        final File taskFile = task.getFile();
        task.getSettings().set(MakeBundle.KEY_BUNDLE_FILE, bundle.getName());
        task.getSettings().set(Clean.KEY_CLEAN_EXTENSIONS, "class");

        new BusyDialog(StrangeEons.getWindow(), string("prj-l-importing"), () -> {
            ZipFile zip = null;
            try {
                PluginBundle pb = new PluginBundle(bundle);
                zip = new ZipFile(pb.getPlainFile());
                importZipFile(zip, taskFile);
            } catch (IOException e) {
                ProjectUtilities.deleteAll(taskFile);
                ErrorDialog.displayError(string("prj-err-task"), e);
            } finally {
                if (zip != null) {
                    try {
                        zip.close();
                    } catch (IOException ie) {
                        ie.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Converts a bundle into a task folder by unpacking the contents of the
     * bundle archive.
     *
     * @param parent the parent that holds the new task
     * @param bundleFile the bundle file to convert into a task
     * @return the task if conversion was a success, or <code>null</code> if it
     * failed
     */
    public static Task createTaskFromBundle(TaskGroup parent, File bundleFile) {
        if (bundleFile == null) {
            throw new NullPointerException("bundleFile");
        }

        String name = bundleFile.getName();
        int dot = name.indexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }

        File target = new File(parent.getFile(), name);
        target = ProjectUtilities.getAvailableFile(target);

        try {
            Task task = parent.addNewTask(new PluginImportTask(bundleFile), target.getName());
            if (task != null && task.getFile().exists()) {
                return task;
            }
        } catch (IOException e) {
            ErrorDialog.displayError(string("prj-err-task"), e);
        }
        return null;
    }
}
