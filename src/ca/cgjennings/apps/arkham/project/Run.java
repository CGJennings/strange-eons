package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.plugins.debugging.ScriptDebugging;
import java.io.File;
import java.io.IOException;
import static resources.Language.string;

/**
 * Task action that runs or debugs a selected script. Action to run or debug a
 * script.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class Run extends TaskAction {

    private boolean debug;

    /**
     * Creates a new run or debug action, depending on the specified parameter.
     *
     * @param debugAction if <code>true</code>, creates a debug action rather
     * than a plain run action
     */
    public Run(boolean debugAction) {
        debug = debugAction;
    }

    @Override
    public String getLabel() {
        return debug ? string("pa-debug") : string("pa-run");
    }

    @Override
    public String getDescription() {
        return debug ? string("pa-debug-tt") : string("pa-run-tt");
    }

    @Override
    public String getActionName() {
        return debug ? "debug" : "run";
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        if (debug && !ScriptDebugging.isInstalled()) {
            return false;
        }
        return member != null && ProjectUtilities.matchExtension(member.getFile(), "js");
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        if (member != null) {
            return runFile(member.getFile(), project, task, member);
        }
        return false;
    }

    /**
     * Run a script file as if using this action. The variables
     * <code>project</code>, <code>task</code>, and <code>member</code> are
     * passed to the script but may be <code>null</code> if the script does not
     * rely on them. If the script is currently being edited by the application,
     * any unsaved changes will be saved before running.
     *
     * @param f the script file to run
     * @param project the project instance to pass to the script
     * @param task the task instance to pass to the script
     * @param member the member instance to pass to the script
     * @return <code>true</code> if the script is successfully loaded and run
     * @throws NullPointerException if <code>f</code> is <code>null</code>
     */
    public boolean runFile(File f, Project project, Task task, Member member) {
        if (f == null) {
            throw new NullPointerException();
        }
        try {
            ProjectUtilities.saveIfBeingEdited(f);
            ProjectUtilities.runScript(f, project, task, member, debug);
            return true;
        } catch (IOException e) {
            ErrorDialog.displayError(string("prj-err-open", f.getName()), e);
        }
        return false;
    }
}
