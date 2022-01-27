package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.editors.CodeEditor;
import ca.cgjennings.ui.textedit.CodeType;
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

    private final boolean debug;

    /**
     * Creates a new run or debug action, depending on the specified parameter.
     *
     * @param debugAction if {@code true}, creates a debug action rather than a
     * plain run action
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
        if (member == null) {
            return false;
        }
        if (debug && !ScriptDebugging.isInstalled()) {
            return false;
        }
        CodeType ct = CodeType.forFile(member.getFile());
        return ct != null && ct.isRunnable();
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        if (member != null) {
            return runFile(member.getFile(), project, task, member);
        }
        return false;
    }

    /**
     * Run a script file as if using this action. The variables {@code project},
     * {@code task}, and {@code member} are passed to the script but may be
     * {@code null} if the script does not rely on them. If the script is
     * currently being edited by the application, any unsaved changes will be
     * saved before running.
     *
     * @param f the script file to run
     * @param project the project instance to pass to the script
     * @param task the task instance to pass to the script
     * @param member the member instance to pass to the script
     * @return {@code true} if the script is successfully loaded and run
     * @throws NullPointerException if {@code f} is {@code null}
     */
    public boolean runFile(File f, Project project, Task task, Member member) {
        if (f == null) {
            throw new NullPointerException();
        }
        try {
            StrangeEonsEditor[] eds = StrangeEons.getWindow().getEditorsShowingFile(f);
            if (eds.length > 0) {
                for (StrangeEonsEditor ed : eds) {
                    if (ed instanceof CodeEditor) {
                        ((CodeEditor) ed).run(debug);
                        return true;
                    }
                }
            }
            CodeType ct = CodeType.forFile(member.getFile());
            if (ct.getDependentFile(f) != null) {
                f = ct.getDependentFile(f);
            }
            ProjectUtilities.runScript(f, project, task, member, debug);
            return true;
        } catch (IOException e) {
            ErrorDialog.displayError(string("prj-err-open", f.getName()), e);
        }
        return false;
    }
}
