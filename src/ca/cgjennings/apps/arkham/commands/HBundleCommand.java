package ca.cgjennings.apps.arkham.commands;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.project.Actions;
import ca.cgjennings.apps.arkham.project.Member;
import ca.cgjennings.apps.arkham.project.Project;
import ca.cgjennings.apps.arkham.project.ProjectView;
import ca.cgjennings.apps.arkham.project.Task;
import ca.cgjennings.apps.arkham.project.TaskAction;
import java.awt.event.ActionEvent;
import java.io.File;
import static resources.Language.string;

/**
 * A helper class for commands related to created plug-in task bundles.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
class HBundleCommand extends AbstractCommand {

    HBundleCommand(String key, String actionName) {
        super(key);
        nameKey = key;
        action = Actions.findActionByName(actionName);
    }

    @Override
    public Object getValue(String key) {
        if (NAME.equals(key)) {
            String name = string(nameKey);
            int pipe = name.lastIndexOf('|');
            if (pipe >= 0) {
                name = name.substring(0, pipe).trim();
            }
            name = name.replace("&", "");
            Task t = getTask();
            if (t != null) {
                name += " (" + t.getName() + ')';
            }
            return name;
        }
        return super.getValue(key);
    }

    @Override
    public boolean isEnabled() {
        return getTask() != null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Task t = getTask();
        if (t != null) {
            action.perform(t.getProject(), t, null);
        }
    }

    protected Task getTask() {
        // Check if the currently edited file's task is a plug-in...
        Project p = StrangeEons.getOpenProject();
        if (p == null) {
            return null;
        }
        ProjectView v = p.getView();
        if (v == null || !v.isFocusOwner()) {
            StrangeEonsEditor ed = StrangeEons.getWindow().getActiveEditor();
            if (ed != null) {
                File f = ed.getFile();
                if (f != null) {
                    Member m = p.findMember(f);
                    if (m != null) {
                        Task t = m.getTask();
                        if (action.appliesTo(t.getProject(), t, null)) {
                            return t;
                        }
                    }
                }
            }
        }
        // If not, check the selection in the project view
        if (v != null) {
            Member[] sel = v.getSelectedMembers();
            if (sel.length == 0) {
                return null;
            }
            Task t = sel[sel.length - 1].getTask();
            if (action.appliesTo(t.getProject(), t, null)) {
                return t;
            }
        }
        return null;
    }
    private String nameKey;
    private TaskAction action;

}
