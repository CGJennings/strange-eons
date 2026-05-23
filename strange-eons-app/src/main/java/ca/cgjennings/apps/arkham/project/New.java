package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.NewEditorDialog;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.ui.theme.ThemedIcon;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.swing.Icon;
import resources.Language;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * A {@link TaskActionTree} that contains commands for creating new files. Look
 * up and extend this action to add new file types to the <b>New</b>
 * menu.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class New extends TaskActionTree {

    @Override
    public String getLabel() {
        return string("pa-new");
    }

    /**
     * A useful class for adding actions to {@code New}. It is not required: you
     * can use any {@link TaskAction}.
     */
    public static class NewAction extends TaskAction {

        private String name;
        private String resource;
        private String ext;
        private String defaultName;
        private String[] taskTypes;
        private ThemedIcon icon;

        /**
         * Create a new action that you will supply your own file creation logic
         * for. You must override {@link #createFile} in order to create the new
         * project file; otherwise the default behaviour is to create a folder.
         *
         * @param label the name of the action
         * @param defaultFileName the default name used to create a new file
         * @param icon the icon to display as the file type icon
         */
        public NewAction(String label, String defaultFileName, Icon icon) {
            this(label, defaultFileName, "", null, icon);
        }

        /**
         * Create a new action that will create a new empty file by copying a
         * template file from a resource.
         *
         * @param label the name of the action
         * @param defaultFileName the default base name (no extension) used to
         * create a new file
         * @param extension the file name extension to use for the new file
         * @param resourceFile the resource that will be copied to create an
         * empty new file, or {@code null}
         * @param icon the icon to display as the file type icon
         */
        public NewAction(String label, String defaultFileName, String extension, String resourceFile, Icon icon) {
            this(label, defaultFileName, extension, resourceFile, icon, null);
        }

        /**
         * Create a new action that will create a new empty file by copying a
         * template file from a resource. The action will only apply within task
         * folders of the specified type.
         *
         * @param label the name of the action
         * @param defaultFileName the default base name (no extension) used to
         * create a new file
         * @param extension the file name extension to use for the new file
         * @param resourceFile the resource that will be copied to create an
         * empty new file, or {@code null}
         * @param icon the icon to display as the file type icon
         * @param taskType the code for the task type that this action is
         * restricted to, or a comma separated list of such task types;
         * {@code null} to allow the action to apply anywhere
         */
        public NewAction(String label, String defaultFileName, String extension, String resourceFile, Icon icon, String taskType) {
            if (!label.endsWith("...")) {
                label += "...";
            }
            this.name = label;
            this.defaultName = defaultFileName;

            if (extension == null) {
                extension = "";
            }
            this.ext = extension;

            this.resource = resourceFile;

            if (taskType != null) {
                this.taskTypes = taskType.split("\\s*,\\s*");
            }

            if (icon == null) {
                icon = MetadataSource.ICON_BLANK;
            }
            this.icon = ThemedIcon.create(icon).small();
        }

        @Override
        public String getLabel() {
            return name;
        }

        @Override
        public ThemedIcon getIcon() {
            return icon;
        }

        @Override
        public String getActionName() {
            return "newaction:" + name;
        }

        @Override
        public boolean perform(Project project, Task task, Member member) {
            Member parent;
            if (member != null && member.isFolder()) {
                parent = member;
            } else if (task != null) {
                parent = task;
            } else {
                parent = project;
            }

            File f = new File(parent.getFile(), defaultName + (!getFileExtension().isEmpty() ? ("." + getFileExtension()) : ""));
            f = ProjectUtilities.getAvailableFile(f);

            try {
                createFile(f);
                parent.synchronize();
                renameFile(parent, f);
            } catch (IOException e) {
                ErrorDialog.displayError(string("prj-err-create-new"), e);
            }

            return true;
        }

        /**
         * Called after creating the file to give the user a chance to rename
         * it. Override to change rename behaviour. The base class selects the
         * file in the project view, then looks up and applies the "rename" task
         * action.
         *
         * @param parent the project member that is a parent of the new file
         * @param f the newly created file
         * @throws IOException if an error occurs while renaming
         */
        protected void renameFile(Member parent, File f) throws IOException {
            TaskAction rename = Actions.findActionByName("rename");
            Member newFile = parent.findChild(f);
            if (newFile != null) {
                parent.getProject().getView().select(newFile);
                rename.performOnSelection(new Member[]{newFile});
            }
        }

        /**
         * Creates a file of the type represented by this action. The base class
         * copies the specified resource file to the destination. (If the
         * resource file is {@code null}, it creates a folder.)
         *
         * @param file the file to be created
         * @throws IOException if an error occurs while creating the file (if
         * the method throws this, an error message will be displayed)
         */
        public void createFile(File file) throws IOException {
            if (resource == null) {
                file.mkdirs();
                if (!file.isDirectory()) {
                    throw new FileNotFoundException(string("prj-err-folder-misc"));
                }
            } else {
                ProjectUtilities.copyResourceToFile(resource, file);
            }
        }

        /**
         * Returns the file extension of the files created by this action.
         *
         * @return the file extension
         */
        public String getFileExtension() {
            return ext;
        }

        /**
         * Returns {@code true} if the target is any kind of folder (including a
         * project or task) and the task type restrictions (if any) are met.
         *
         * {@inheritDoc}
         */
        @Override
        public boolean appliesTo(Project project, Task task, Member member) {
            if (member != null && !member.isFolder()) {
                return false;
            }
            if (taskTypes != null) {
                String thisType;
                if (task == null) {
                    thisType = Project.PROJECT_TASK_TYPE;
                } else {
                    thisType = task.getSettings().get(Task.KEY_TYPE);
                }
                for (String t : taskTypes) {
                    if (t.equals(thisType)) {
                        return true;
                    }
                }
                return false;
            }
            return true;
        }

        @Override
        public boolean appliesToSelection(Member[] members) {
            return members.length == 1 && super.appliesToSelection(members);
        }
    }

    /**
     * Returns a new instance of {@link New} that is populated with default
     * child actions.
     *
     * @return a prepopulated New action
     */
    static New createDefaultNewAction() {
        New ta = new New();

        ta.add(new AddTask());
        ta.add(new NewAction(string("pa-new-plugin-root"), "eons-plugin", "", "projects/root-template.txt", MetadataSource.ICON_PLUGIN_ROOT, NewTaskType.PLUGIN_TYPE) {
            @Override
            public boolean appliesTo(Project project, Task task, Member member) {
                return member == null && super.appliesTo(project, task, member) && !new File(task.getFile(), "eons-plugin").exists();
            }

            /**
             * {@inheritDoc} Instead of renaming the file, shows the root
             * editor.
             */
            @Override
            protected void renameFile(Member parent, File f) throws IOException {
                Member newFile = parent.findChild(f);
                if (newFile != null) {
                    Project project = parent.getProject();
                    project.getView().select(newFile);
                    RootEditor.showRootEditor(newFile, 0);
                }
            }
        });
        ta.add(new NewAction(string("pa-new-script"), string("pa-new-script-name"), "js", "projects/new-empty.js", MetadataSource.ICON_SCRIPT));
        if (!Settings.getUser().getYesNo("hide-typescript-support")) {
            ta.add(new NewAction(string("pa-new-typescript"), string("pa-new-typescript-name"), "ts", "projects/new-empty.js", MetadataSource.ICON_TYPESCRIPT));
        }
        ta.add(new NewAction(string("pa-new-diy"), string("pa-new-diy-name"), "js", "projects/new-diy.js", MetadataSource.ICON_SCRIPT, NewTaskType.PLUGIN_TYPE));
        ta.add(new NewAction(string("pa-new-plugin-script"), string("pa-new-plugin-script-name"), "js", "projects/new-plugin-script.js", MetadataSource.ICON_SCRIPT, NewTaskType.PLUGIN_TYPE));
        ta.add(null);
        ta.add(new NewGenericGameComponent(true));

        ta.add(null);
        ta.add(new NewAction(string("pa-new-classmap"), string("pa-new-classmap-name"), "classmap", "projects/new-classmap.txt", MetadataSource.ICON_CLASS_MAP, NewTaskType.PLUGIN_TYPE));
        ta.add(new NewAction(string("pa-new-conversionmap"), string("pa-new-conversionmap-name"), "conversionmap", "projects/new-conversionmap.txt", MetadataSource.ICON_CONVERSION_MAP, NewTaskType.PLUGIN_TYPE));
        ta.add(new NewAction(string("pa-new-settings"), string("pa-new-settings-name"), "settings", "projects/new-settings.txt", MetadataSource.ICON_SETTINGS, NewTaskType.PLUGIN_TYPE));
        ta.add(new NewAction(string("pa-new-properties"), string("pa-new-properties-name"), "properties", "projects/new-properties.txt", MetadataSource.ICON_PROPERTIES));
        ta.add(new NewAction(string("pa-new-tiles"), string("pa-new-tiles-name"), "tiles", "projects/new-tiles.txt", MetadataSource.ICON_TILE_SET, NewTaskType.PLUGIN_TYPE));
        ta.add(new NewAction(string("pa-new-sil"), string("pa-new-sil-name"), "silhouettes", "projects/new-silhouettes.txt", MetadataSource.ICON_SILHOUETTES, NewTaskType.PLUGIN_TYPE));
        ta.add(new NewAction(string("cle-file-desc"), string("cle-file-desc"), "cardlayout", "projects/new-layout.cardlayout", MetadataSource.ICON_CARD_LAYOUT, NewTaskType.PLUGIN_TYPE));
        ta.add(new NewGenericGameComponent(false));

        ta.add(null);

        for (TaskAction docType : DocumentTask.getDocumentTypes()) {
            ta.add(docType);
        }

        ta.add(null);

        ta.add(new NewAction(string("pa-new-batchscript"), string("pa-new-batchscript-name"), "ajs", "projects/new-automation.js", MetadataSource.ICON_AUTOMATION_SCRIPT));

        ta.add(null);

        ta.add(new NewAction(string("pa-new-folder"), string("pa-new-folder-name"), MetadataSource.ICON_FOLDER));

        return ta;
    }

    /**
     * Creates any game component, allowing the user to select the desired type
     * in a {@link NewEditorDialog}. This is added twice; one version (added to
     * the top) shows up in all task types except plug-in tasks. The other is
     * used for plug-in tasks and is added near the bottom (since it isn't often
     * one wants to add a component to a plug-in).
     */
    private static class NewGenericGameComponent extends TaskAction {

        private final boolean nonPluginVersion;

        NewGenericGameComponent(boolean nonPluginVersion) {
            this.nonPluginVersion = nonPluginVersion;
        }

        @Override
        public String getLabel() {
            return string("pa-new-comp");
        }

        @Override
        public String getActionName() {
            return nonPluginVersion ? "newgamecomponent" : "newgamecomponent:plugintask";
        }

        @Override
        public boolean perform(Project project, Task task, Member member) {
            if (NewEditorDialog.getSharedInstance().isVisible()) {
                NewEditorDialog.getSharedInstance().setVisible(false);
            }
            NewEditorDialog ned = new NewEditorDialog(true);
            project.getView().moveToLocusOfAttention(ned);
            ned.updateProjectStatus(project);
            ned.setVisible(true);
            return true;
        }

        @Override
        public boolean appliesToSelection(Member[] members) {
            return members.length == 1 && super.appliesToSelection(members);
        }

        @Override
        public boolean appliesTo(Project project, Task task, Member member) {
            if (member != null && !member.isFolder()) {
                return false;
            }
            String taskType = null;
            if (task != null) {
                taskType = task.getSettings().get(Task.KEY_TYPE);
            }

            if (nonPluginVersion) {
                return !NewTaskType.PLUGIN_TYPE.equals(taskType);
            }
            return NewTaskType.PLUGIN_TYPE.equals(taskType);
        }

        @Override
        public Icon getIcon() {
            return MetadataSource.ICON_EON_DEFAULT;
        }
    };

    /**
     * A {@link New} child action that creates a specific game component from a
     * compiled {@link GameComponent} class.
     */
    public static class NewGameComponent extends TaskAction /*implements Info*/ {

        protected String klass, iconKey, docname, label, taskType, taskSubtype;
        protected Icon icon;

        /**
         * Creates a child action that creates a game component from a compiled
         * {@link GameComponent} subclass.
         *
         * @param key a UI text key for the component; if the key has the
         * "app-new-" prefix this may be left off
         * @param className the fully qualified name of the component class; or,
         * if no package is specified, the {@code arkham.component} package is
         * used by default
         * @param taskType the type of task that must be active for the action
         * to apply, or {@code null}
         * @param taskSubtype the type of task subtype that must be active for
         * the action to apply, or {@code null}
         * @throws NullPointerException if the key or class name is {@code null}
         * @throws IllegalArgumentException if the specified class does not
         * exist
         */
        public NewGameComponent(String key, String className, String taskType, String taskSubtype) {
            if (key == null) {
                throw new NullPointerException("key");
            }
            if (className == null) {
                throw new NullPointerException("className");
            }

            Language il = Language.getInterface();
            if (!il.isKeyDefined(key) && il.isKeyDefined("app-new-" + key)) {
                key = "app-new-" + key;
            }
            label = string(key);

            String defaultIcon = "editors/" + key + ".png";
            if (ResourceKit.composeResourceURL(defaultIcon) != null) {
                iconKey = defaultIcon;
            }

            docname = ResourceKit.makeStringFileSafe(label.toLowerCase().replaceAll("\\s+", "")) + ".eon";
            label += "...";
            if (className.indexOf('.') < 0) {
                className = "ca.cgjennings.apps.arkham.component." + className;
            }
            klass = className;
            this.taskType = taskType;
            this.taskSubtype = taskSubtype;
            try {
                Class.forName(klass, false, getClass().getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("no such class: " + klass);
            }
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public boolean perform(Project project, Task task, Member member) {
            try {
                File f = new File(task.getFile(), docname);
                f = ProjectUtilities.getAvailableFile(f);
                GameComponent gc = (GameComponent) Class.forName(klass).getConstructor().newInstance();
                ResourceKit.writeGameComponentToFile(f, gc);
                task.synchronize();
                TaskAction rename = Actions.findActionByName("rename");
                Member newFile = task.findChild(f);
                if (newFile != null) {
                    project.getView().select(newFile);
                    rename.performOnSelection(new Member[]{newFile});
                }
                return true;
            } catch (Exception e) {
                ErrorDialog.displayError(string("prj-err-create-new"), e);
            }
            return false;
        }

        @Override
        public boolean appliesTo(Project project, Task task, Member member) {
            if (task == null || member != null) {
                return false;
            }
            Settings s = task.getSettings();
            if (taskType != null && !taskType.equals(s.get(Task.KEY_TYPE))) {
                return false;
            }
            return !(taskSubtype != null && !taskSubtype.equals(s.get(Task.KEY_SUBTYPE)));
        }

        @Override
        public Icon getIcon() {
            if (icon == null) {
                if (iconKey != null) {
                    BufferedImage bi = ResourceKit.getThemedImage(iconKey);
                    if (bi != null) {
                        icon = ImageUtilities.createIconForSize(bi, MetadataSource.ICON_SIZE);
                    }
                    iconKey = null;
                }
                if (icon == null) {
                    icon = MetadataSource.ICON_BLANK;
                }
            }
            return icon;
        }
    }
}
