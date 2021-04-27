package ca.cgjennings.apps.arkham.project;

import static resources.Language.string;

/**
 * Task action that allows a project's packaging to be converted, switching
 * between folder- and package-based projects.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class Packaging extends TaskAction {

    @Override
    public String getLabel() {
        return string("pa-packaging");
    }

    @Override
    public String getDescription() {
        return string("pa-packaging-tt");
    }

    @Override
    public boolean perform(final Project project, Task task, Member member) {
        PackagingDialog d = new PackagingDialog();
        project.getView().moveToLocusOfAttention(d);
        d.setVisible(true);
        return true;
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        return task == null && member == null;
    }

    @Override
    public boolean appliesToSelection(Member[] members) {
        return members.length == 1 && members[0] instanceof Project;
    }
}
