package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.ui.dnd.ScrapBook;
import static resources.Language.string;

/**
 * Paste files on clipboard into the selected folder of a project.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class Paste extends TaskAction {

    @Override
    public String getLabel() {
        if (ScrapBook.isImageAvailable()) {
            return string("paste-image");
        } else {
            return string("paste");
        }
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        project.getView().paste();
        return true;
    }

    @Override
    public boolean performOnSelection(Member[] members) {
        return members.length == 1 && super.performOnSelection(members);
    }

    @Override
    public boolean appliesToSelection(Member[] members) {
        return members.length == 1 && super.appliesToSelection(members);
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        return member == null || member.isFolder();
    }
}
