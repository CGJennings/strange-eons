package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import resources.Language;
import static resources.Language.string;
import resources.Settings;

/**
 * A kind of task that can be added to a project.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public abstract class NewTaskType {

    /**
     * The standard task type for decks.
     */
    public static final String DECK_TYPE = "DECK";
    /**
     * The standard task type for case books.
     */
    public static final String CASEBOOK_TYPE = "CASEBOOK";
    /**
     * The standard task type for component factories.
     */
    public static final String FACTORY_TYPE = "FACTORY";
    /**
     * The standard task type for documentation folders.
     */
    public static final String DOCUMENTATION_TYPE = "DOCS";
    /**
     * The standard task type for expansion boards.
     */
    public static final String EXPANSION_BOARD_TYPE = "EXPBOARD";
    /**
     * The standard task type for plug-ins.
     */
    public static final String PLUGIN_TYPE = "PLUGIN";
    /**
     * The standard default task type.
     */
    public static final String GENERIC_TYPE = "TASK";
    /**
     * The task type for task groups.
     */
    public static final String TASK_GROUP_TYPE = "TASKGROUP";

    /**
     * The task subtype for scripted factories.
     */
    public static final String FACTORY_SCRIPTED_SUBTYPE = "SCRIPTED";

    /**
     * Returns the localized, human-readable name of this task type.
     *
     * @return the name displayed for this type in the new task dialog
     */
    public abstract String getLabel();

    /**
     * Returns the localized, human-readable description of this task type as a
     * simple HTML document.
     *
     * @return a description of the task supported by this task type
     */
    public String getDescription() {
        return "";
    }

    /**
     * Returns a string that describes the type of task created by this
     * <code>NewTaskType</code>. The default implementation returns
     * <tt>"TASK"</tt>, the generic task type. Returning <tt>"PROJECT"</tt> is
     * illegal, because that type is reserved to identify project folders. The
     * <tt>type</tt> setting of new tasks of this type will be set to the value
     * returned by this method.
     * <p>
     * {@link TaskAction}s often decide whether or not they can be applied to a
     * given file based on the type and/or subtype of the task folder they
     * belong to.
     * <p>
     * Note that the type identifier returned by this method does not have to be
     * unique.
     *
     * @return a non-<code>null</code> string describing the task type
     */
    public String getType() {
        return GENERIC_TYPE;
    }

    /**
     * Returns a string that describes the subtype of task created by this
     * <code>NewTaskType</code>. This can be <code>null</code> to indicate no
     * subtype. (The default implementation returns <code>null</code>.) The
     * <tt>subtype</tt> setting of new tasks of this type will be set to the
     * value returned by this method if it is non-<code>null</code>.
     *
     * @return a string describing the task subtype, or <code>null</code>
     */
    public String getSubtype() {
        return null;
    }

    /**
     * Returns a string that identifies an image resource to be used to locate
     * the icon image for this task type. If <code>null</code>, a default icon
     * will be used based on the project type. If this value is
     * non-<code>null</code>, then the <tt>icon</tt> setting of new tasks of
     * this type will be set to the returned value. The icon used in the project
     * view can be changed during {@link #initializeNewTask} by changing the
     * value of this setting.
     *
     * @return the path to an image file in the application resources
     */
    public String getIconResource() {
        return null;
    }

    /**
     * This method is called when this task type is added to a project. By the
     * time this is called, the task folder will already exist and the
     * <tt>type</tt>, <tt>subtype</tt>, and <tt>icon</tt> settings will be
     * filled in using the values returned from this object, if any.
     *
     * <p>
     * The project view will be synchronized with the task after this method
     * returns, so any new files added to the task folder will be detected
     * immediately.
     *
     * <p>
     * The default implementation does nothing, which means the user will simply
     * have a new, empty task folder after adding the new task.
     *
     * <p>
     * This method should return <code>true</code> if the initialization is
     * successful. If it returns <code>false</code>, or the method throws an
     * exception, then the task folder will be deleted. If an exception was
     * thrown, then a generic error message will be displayed; if the method
     * returns <code>false</code> then it is assumed that the method displayed
     * its own, more specific message.
     *
     * @param project the project that the task was added to
     * @param task the {@link Member} that represents the new task's folder
     * @return <code>true</code> if initialization succeeded
     * @throws Throwable if an uncaught exception occurs during initialization
     */
    public boolean initializeNewTask(Project project, Task task) throws Throwable {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is overridden to return the task type's label.
     */
    @Override
    public String toString() {
        return getLabel();
    }

    //////////////
    // REGISTRY /////////////////////////////////////////////////////////////
    /////////////
    /**
     * Registers a new task type.
     *
     * @param ntt the type to register
     * @throws NullPointerException if <code>ntt</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>ntt</code> is already
     * registered
     * @throws IllegalArgumentException if <code>ntt</code> uses a reserved type
     * code
     */
    public static void register(NewTaskType ntt) {
        if (ntt == null) {
            throw new NullPointerException("ntt");
        }
        if (types.contains(ntt)) {
            throw new IllegalArgumentException("already registered: " + ntt);
        }
        if (ntt.getType().equals(Project.PROJECT_TASK_TYPE)) {
            throw new IllegalArgumentException("reserved task type: " + Project.PROJECT_TASK_TYPE);
        }

        types.add(ntt);
    }

    /**
     * Unregisters a new task type.
     *
     * @param ntt the type to unregister
     * @throws NullPointerException if <code>ntt</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>ntt</code> is not already
     * registered
     */
    public static void unregister(NewTaskType ntt) {
        if (ntt == null) {
            throw new NullPointerException("ntt");
        }
        if (!types.contains(ntt)) {
            throw new IllegalArgumentException("not registered: " + ntt);
        }

        types.remove(ntt);
    }

    /**
     * Returns <code>true</code> if a new task type instance is currently
     * registered.
     *
     * @param ntt the type to look for
     * @return <code>true</code> if <code>ntt</code> has been registered
     * @throws NullPointerException if <code>ntt</code> is <code>null</code>
     */
    public static boolean isRegistered(NewTaskType ntt) {
        if (ntt == null) {
            throw new NullPointerException("ntt");
        }
        return types.contains(ntt);
    }

    /**
     * Returns an array of the types that are currently registered, sorted by
     * type and then by label.
     *
     * @return an array of the registered new task types
     */
    public static NewTaskType[] getNewTaskTypes() {
        NewTaskType[] ntts = types.toArray(new NewTaskType[types.size()]);
        Arrays.sort(ntts, TYPE_ORDER);
        return ntts;
    }

    private static Set<NewTaskType> types = new HashSet<>();

    static final Comparator<NewTaskType> TYPE_ORDER = new Comparator<NewTaskType>() {
        private Collator coll = Language.getInterface().getCollator();

        /**
         * {@inheritDoc}
         * <p>
         * Sorts by type, then label.
         */
        @Override
        public int compare(NewTaskType o1, NewTaskType o2) {
            // special case: sort task group to top of list
            if (o1.getType().equals(NewTaskType.TASK_GROUP_TYPE)) {
                if (!o2.getType().equals(NewTaskType.TASK_GROUP_TYPE)) {
                    return -1;
                }
            } else if (o2.getType().equals(NewTaskType.TASK_GROUP_TYPE)) {
                return 1;
            }

            int comp = coll.compare(o1.getType(), o2.getType());
            if (comp == 0) {
                return coll.compare(o1.getLabel(), o2.getLabel());
            }
            return comp;
        }
    };

    // register standard types
    static {
        register(new ScriptedFactory());
//		register( new BasicFactory() );
        register(new GenericTask());
        register(new EmptyPluginTask());
        register(new PluginTask());
        register(new PluginImportTask());
        register(new DeckTask());
        register(new DocumentTask());
        register(new ResourceReferenceTask());
        register(new TaskGroupType());
    }

    static class ScriptedFactory extends NewTaskType {

        @Override
        public String getLabel() {
            return string("pt-factory-scripted-name");
        }

        @Override
        public String getDescription() {
            return string("pt-factory-scripted-desc");
        }

        @Override
        public String getType() {
            return FACTORY_TYPE;
        }

        @Override
        public String getSubtype() {
            return FACTORY_SCRIPTED_SUBTYPE;
        }

        @Override
        public boolean initializeNewTask(Project project, Task task) throws Throwable {
            try {
                Settings s = task.getSettings();
                s.set(Clean.KEY_CLEAN_EXTENSIONS, "eon");
                s.set(NewTaskType.ScriptedFactory.KEY_MAKE_SCRIPT, string("pt-factory-scripted-makefile"));
                ProjectUtilities.copyResourceToFile(
                        "projects/factory-script.js",
                        new File(task.getFile(), string("pt-factory-scripted-makefile"))
                );
                return true;
            } catch (IOException e) {
                ErrorDialog.displayError(string("prj-err-inittask"), e);
            }
            return false;
        }

        public static final String KEY_MAKE_SCRIPT = "make-script";

        static {
            Rename.addRenameListener((Project p, Member newMember, File oldFile, File newFile) -> {
                // the make file was deleted; nothing we can do to find the right name so
                // stick to the existing name for now
                if (newFile == null) {
                    return;
                }
                
                Iterator<Task> ti = p.taskIterator();
                while (ti.hasNext()) {
                    Task t = ti.next();
                    if (FACTORY_TYPE.equals(t.getSettings().get(Task.KEY_TYPE))) {
                        String script = t.getSettings().get(KEY_MAKE_SCRIPT);
                        if (script != null) {
                            File absolute = new File(t.getFile(), script.replace('/', File.separatorChar));
                            if (absolute.equals(oldFile)) {
                                String relative = ProjectUtilities.makeFileRelativeTo(absolute, newFile).getPath();
                                t.getSettings().set(KEY_MAKE_SCRIPT, relative);
                                StrangeEons.log.log(Level.INFO, "updated make script path to {0}", relative);
                            }
                        }
                    }
                }
            });
        }
    }

    static class EmptyPluginTask extends NewTaskType {

        @Override
        public String getLabel() {
            return string("pt-empty-plugin-name");
        }

        @Override
        public String getDescription() {
            return string("pt-empty-plugin-desc");
        }

        @Override
        public String getType() {
            return NewTaskType.PLUGIN_TYPE;
        }
    }

    static class GenericTask extends NewTaskType {

        @Override
        public String getLabel() {
            return string("pt-generic-name");
        }

        @Override
        public String getDescription() {
            return string("pt-generic-desc");
        }

        @Override
        public String getType() {
            return NewTaskType.GENERIC_TYPE;
        }

        @Override
        public boolean initializeNewTask(final Project project, final Task task) throws Throwable {
            final TaskAction ta = Actions.findActionByName("changeicon");
            if (ta != null && ta.appliesTo(project, task, null)) {
                EventQueue.invokeLater(() -> {
                    ProjectView pv = project.getView();
                    if (pv != null) {
                        pv.select(task);
                    }
                    ta.perform(project, task, null);
                });
            }
            return true;
        }
    }

    static class TaskGroupType extends NewTaskType {

        @Override
        public String getLabel() {
            return string("pt-task-group-name");
        }

        @Override
        public String getDescription() {
            return string("pt-task-group-desc");
        }

        @Override
        public String getType() {
            return NewTaskType.TASK_GROUP_TYPE;
        }
    }
}
