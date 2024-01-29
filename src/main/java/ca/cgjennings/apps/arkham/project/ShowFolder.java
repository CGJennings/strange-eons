package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.platform.DesktopIntegration;
import java.io.File;
import static resources.Language.string;

/**
 * Task action that shows the contents of a folder using the desktop shell.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class ShowFolder extends TaskAction {

    public ShowFolder() {
    }

    @Override
    public String getLabel() {
        return string("pa-show-folder");
    }

    @Override
    public String getDescription() {
        return string("pa-show-folder-tt");
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        File f = resolveTarget(project, task, member).getFile();
        return DesktopIntegration.showInShell(f);
    }

    @Override
    public boolean appliesToSelection(Member[] members) {
        return DesktopIntegration.SHOW_IN_SHELL_SUPPORTED && super.appliesToSelection(members);
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        // any folder (member=null -> projects or task -> folder)
        return member == null || member.isFolder();
    }
}
