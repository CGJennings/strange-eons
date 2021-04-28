package ca.cgjennings.apps.arkham.commands;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.project.Actions;
import ca.cgjennings.apps.arkham.project.Member;
import ca.cgjennings.apps.arkham.project.ProjectView;
import ca.cgjennings.apps.arkham.project.Run;
import java.awt.event.ActionEvent;

/**
 * Implements Run File and Debug File commands.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
class HRunScriptCommand extends DelegatedCommand {

    private final boolean debug;

    HRunScriptCommand(boolean debug) {
        super(debug ? "app-debug-file" : "app-run-file", debug ? null : "toolbar/script-run.png");
        this.debug = debug;
    }

    @Override
    public void performDefaultAction(ActionEvent e) {
        Member[] target = getTargetMembers();
        if (target == null) {
            return;
        }

        final Run action = getTaskAction();
        if (action != null && action.appliesToSelection(target)) {
            action.performOnSelection(target);
        }
    }

    @Override
    public boolean isDefaultActionApplicable() {
        Member[] target = getTargetMembers();
        if (target == null) {
            return false;
        }

        final Run action = getTaskAction();
        return action == null ? false : action.appliesToSelection(target);
    }

    private Member[] getTargetMembers() {
        ProjectView v = StrangeEons.getWindow().getOpenProjectView();
        if (v != null) {
            Member[] sel = v.getSelectedMembers();
            return sel.length == 0 ? null : sel;
        }
        return null;
    }

    private Run getTaskAction() {
        return (Run) Actions.getUnspecializedAction(debug ? "debug" : "run ");
    }
}
