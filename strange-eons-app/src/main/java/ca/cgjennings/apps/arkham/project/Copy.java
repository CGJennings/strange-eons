package ca.cgjennings.apps.arkham.project;

import static resources.Language.string;

/**
 * Task action that copies the project selection to the clipboard.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class Copy extends TaskAction {

    @Override
    public String getLabel() {
        return string("copy");
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        project.getView().copy();
        return true;
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        return true;
    }
}
