package ca.cgjennings.apps.arkham.project;

import static resources.Language.string;

/**
 * Task action that allows the user to choose a custom icon to represent a task
 * folder. Plug-ins may register additional icons for the user to pick from
 * using {@link Task#registerCustomIcon(java.lang.String)}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class ChangeIcon extends TaskAction {

    @Override
    public String getLabel() {
        return string("pa-icon-name");
    }

    @Override
    public String getDescription() {
        return string("pa-icon-tt");
    }

    @Override
    public boolean performOnSelection(Member[] members) {
        if (members.length == 0) {
            return false;
        }
        Project project = members[0].getProject();

        ChangeIconDialog d = new ChangeIconDialog();
        project.getView().moveToLocusOfAttention(d);

        String current = null;
        int found = 0;
        for (; found < members.length; ++found) {
            if (members[found] instanceof Task && !(members[found] instanceof Project)) {
                current = ((Task) members[found]).getSettings().get(Task.KEY_ICON);
                break;
            }
        }
        if (found >= members.length) {
            return false;
        }

        if (d.showDialog(current)) {
            String res = d.getSelectedResource();
            for (int i = found; i < members.length; ++i) {
                if (members[i] instanceof Task && !(members[i] instanceof Project)) {
                    Task task = (Task) members[i];
                    if (res == null) {
                        task.getSettings().reset(Task.KEY_ICON);
                    } else {
                        task.getSettings().set(Task.KEY_ICON, res);
                    }
                    task.synchronize();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        if (task == null) {
            return false;
        }
        ChangeIconDialog d = new ChangeIconDialog();
        project.getView().moveToLocusOfAttention(d);
        if (d.showDialog(task.getSettings().get(Task.KEY_ICON))) {
            String res = d.getSelectedResource();
            if (res == null) {
                task.getSettings().reset(Task.KEY_ICON);
            } else {
                task.getSettings().set(Task.KEY_ICON, res);
            }
            task.synchronize();
            return true;
        }
        return false;
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        return member == null && task != null;
    }
}
