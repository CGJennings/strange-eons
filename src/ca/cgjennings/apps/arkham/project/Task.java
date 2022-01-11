package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.io.EscapedLineReader;
import ca.cgjennings.io.EscapedLineWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import resources.ResourceKit;
import resources.Settings;

/**
 * A task is kind of subproject within a project; the type of a task determines
 * the kinds of actions that may be performed within it.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class Task extends Member {

    private boolean settingsAreDirty = false;

    /**
     * Creates a task that represents a task already stored on disk.
     *
     * @param taskToOpen
     */
    public Task(TaskGroup parent, File taskToOpen) throws IOException {
        super(parent, taskToOpen);
        if (!isTaskFolder(taskToOpen)) {
            throw new IllegalArgumentException("Not a task folder");
        }
        readTaskSettings();
    }

    public static boolean isTaskFolder(File f) {
        if (f.isDirectory()) {
            return new File(f, TASK_SETTINGS).exists();
        }
        return false;
    }

    public Settings getSettings() {
        return settings;
    }

    /**
     * Recursively closes the project; called via {@link Project#close()}. After
     * recursively closing any children, the task will write out its task
     * settings to the special task setting file.
     */
    @Override
    void close() {
        super.close();
        try {
            if (settingsAreDirty && getFile().exists()) {
                writeTaskSettings();
            }
        } catch (IOException ex) {
            StrangeEons.log.log(Level.WARNING, "failed to write task settings for " + getFile(), ex);
        }
    }

    /**
     * Creates a new, empty task. If this succeeds, then the task can be opened
     * using the returned file. This is a low-level method that creates the
     * basic file structure for a task. To add a new task to a project, see
     * {@link Project#addNewTask(ca.cgjennings.apps.arkham.project.NewTaskType, java.lang.String)}.
     *
     * @param parentFolder the root project folder or a {@link TaskGroup} folder
     * @param taskName a name for the task
     * @param taskType a type descriptor for the project type
     * @return the root folder of the task
     * @throws IOException if the parent folder does not exist, or if it exists
     * but has a child with the same name as the new task name, or if the task
     * folder cannot be created for some reason
     */
    public static File createTask(File parentFolder, String taskName, String taskType) throws IOException {
        if (!parentFolder.exists()) {
            throw new FileNotFoundException("The parent folder for this task does not exist");
        }

        taskName = ResourceKit.makeStringFileSafe(taskName);

        File folder = new File(parentFolder, taskName);
        if (folder.exists()) {
            throw new IOException("The task folder already exists " + folder);
        }

        if (!folder.mkdir()) {
            throw new IOException("Unable to create task folder " + folder);
        }

        Settings taskSettings = new Settings();
        taskSettings.set(KEY_TYPE, taskType);

        try {
            writeTaskSettings(folder, taskSettings);
        } catch (IOException e) {
            ProjectUtilities.deleteAll(folder);
            throw e;
        }

        return folder;
    }

    public void writeTaskSettings() throws IOException {
        writeTaskSettings(getFile(), settings);
        settingsAreDirty = false;
    }

    private static void writeTaskSettings(File f, Settings s) throws IOException {
        try (EscapedLineWriter lineWriter = new EscapedLineWriter(new File(f, TASK_SETTINGS))) {
            lineWriter.writeComment("WARNING: If this file is deleted, Strange Eons will no longer\nsee this as a project/task folder!");
            for (String key : s) {
                lineWriter.writeProperty(key, s.get(key));
            }
        }
    }

    public void readTaskSettings() throws IOException {
        settings = new Settings();
        try (EscapedLineReader lineReader = new EscapedLineReader(new File(getFile(), TASK_SETTINGS))) {
            String[] pair;
            while ((pair = lineReader.readProperty(true)) != null) {
                settings.set(pair[0], pair[1]);
            }
        }
        settingsAreDirty = false;
        settings.addPropertyChangeListener((ev) -> settingsAreDirty = true);
    }

    private Settings settings;

    protected static final String TASK_SETTINGS = "seproject";

    public static final String KEY_TYPE = "type";
    public static final String KEY_SUBTYPE = "subtype";
    public static final String KEY_ICON = "icon";

    /**
     * Add the resource path to a new custom task icon that can be selected by
     * the user using {@link ChangeIcon}.
     *
     * @since 2.1a7
     * @param resource
     */
    public static void registerCustomIcon(String resource) {
        if (resource == null) {
            throw new NullPointerException();
        }
        if (ResourceKit.composeResourceURL(resource) == null) {
            throw new AssertionError("missing icon resource: " + resource);
        }
        customIcons.add(resource);
    }

    /**
     * Remove the resource path to a new custom task icon that can be selected
     * by the user using {@link ChangeIcon}.
     *
     * @since 2.1a7
     * @param resource
     */
    public static void unregisterCustomIcon(String resource) {
        customIcons.remove(resource);
    }

    /**
     * Returns an array of all registered custom icons.
     *
     * @since 2.1a7
     * @return an array of registered custom icon resource paths
     */
    public static String[] getCustomIcons() {
        return customIcons.toArray(new String[0]);
    }

    private static final LinkedHashSet<String> customIcons = new LinkedHashSet<>();

    private static final int NUM_CUSTOM_ICONS = 128;

    static {
        for (int i = 0; i < NUM_CUSTOM_ICONS; ++i) {
            registerCustomIcon("icons/project/mt" + (i < 10 ? ("0" + i) : ("" + i)) + ".png");
        }
    }
}
