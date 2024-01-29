package ca.cgjennings.apps.arkham.project;

import static resources.Language.string;

/**
 * Task action that converts a plug-in bundle in a project into a task folder.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class PluginImportAction extends TaskAction {

    @Override
    public String getLabel() {
        return string("pt-import-plugin-name");
    }

    @Override
    public String getDescription() {
        return string("pt-import-plugin-ta-desc");
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        return member != null && ProjectUtilities.matchExtension(member, ProjectUtilities.BUNDLE_EXTENSIONS);
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        TaskGroup parent = project;
        if (task != null && task instanceof TaskGroup) {
            parent = (TaskGroup) task;
        }
        Task newTask = PluginImportTask.createTaskFromBundle(parent, member.getFile());
        if (newTask != null) {
            Member synchTarget = member.getParent();
            member.getFile().delete();
            if (synchTarget != null) {
                synchTarget.synchronize();
            }
        }
        return true;
    }
}
