package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import static resources.Language.string;

/**
 * A <code>TaskGoup</code> is a task folder that is allowed to contain other
 * {@link Task}s. Moving a task folder into a task that is not a task group will
 * cause the moved task to lose its task status and become a plain file.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see Project
 */
public class TaskGroup extends Task {

    public TaskGroup(TaskGroup parent, File taskToOpen) throws IOException {
        super(parent, taskToOpen);
    }

    /**
     * Create a new task in a project using a {@link NewTaskType} as a template.
     * The new task is returned. If the task folder cannot be created or is only
     * partially created, but cannot be completed to to point of initializing
     * it, then it will be removed. If initialization begins but is not
     * completed, it will be left in place.
     *
     * @param ntt the template used to initialize the task
     * @param folderName the name to use for the new task folder (must not
     * already exist)
     * @return the new task
     * @throws IOException if the task cannot be created
     */
    public Task addNewTask(NewTaskType ntt, String folderName) throws IOException {
        Project proj = getProject();

        File folder = new File(getFile(), folderName);
        if (folder.exists()) {
            throw new IOException(string("prj-err-folder-exists"));
        }

        Task t = null;
        try {
            File result = Task.createTask(getFile(), folderName, ntt.getType());
            if (ntt.getType().equals(NewTaskType.TASK_GROUP_TYPE)) {
                t = new TaskGroup(this, result);
            } else {
                t = new Task(this, result);
            }
        } catch (IOException e) {
            ProjectUtilities.deleteAll(folder);
            throw new IOException(string("prj-err-folder-misc"), e);
        }

        // add the Task object directly to the tree so the same instance
        // is used (instead of relying on a synch, which would generate
        // a new instance
        //
        // NB: We need to do this NOW. If we wait until after the task is
        // initialized, the NewTaskType may create a modal dialog during
        // configuration. If this happens, then synch events will be
        // processed, and the new task folder may be detected and added
        // before we can insert our copy. Then the settings will be the
        // default settings and not include changes by the NTT init.
        // Then we insert ours, and on a subsequent synch it is deleted since
        // it is a duplicate. Then the universe shatters into a thousand
        // pieces and we all die. So do this now, OK? In the event that
        // init fails and the NTT deletes the task, the synch will pick up the
        // delete so no worries.
        insert(t);

        if (ntt.getSubtype() != null) {
            t.getSettings().set("subtype", ntt.getSubtype());
        }

        if (ntt.getIconResource() != null) {
            t.getSettings().set("icon", ntt.getIconResource());
        }

        boolean success = false;
        try {
            success = ntt.initializeNewTask(proj, t);
        } catch (Throwable ex) {
            ErrorDialog.displayError(string("prj-err-inittask"), ex);
        }
        if (!success) {
            ProjectUtilities.deleteAll(t.getFile());
        }

        if (t.getFile().exists()) {
            try {
                t.writeTaskSettings();
            } catch (IOException e) {
                StrangeEons.log.log(Level.WARNING, "failed to write task settings: " + t.getName(), e);
            }
        }

        // we call this in case the NTT deletes the task folder (e.g., the user
        // cancels the task creation).
        synchronize();

        if (t != null && t.getFile().exists()) {
            Member m = proj.findMember(t.getFile());
            if (m != null) {
                m.synchronize();
                ProjectView v = proj.getView();
                if (v != null && m != null) {
                    v.expandFolder(m);
                    v.select(m);
                }
            }
            if (t != m) {
                throw new AssertionError("created task is not the task instance in the project");
            }
            return t;
        }

        return null;
    }
}
