package ca.cgjennings.apps.arkham.project;

import static resources.Language.string;

/**
 * Task action that adds a new subtask to a project or task group.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class AddTask extends TaskAction {

    @Override
    public String getLabel() {
        return string("pa-new-task");
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        TaskGroup parent = task == null ? project : (TaskGroup) task;
        NewTaskDialog ntd = new NewTaskDialog(parent);
        project.getView().moveToLocusOfAttention(ntd);
        ntd.setVisible(true);
        return true;
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        // only applies to projects and task groups
        return member == null && (task == null || task instanceof TaskGroup);
    }

}
