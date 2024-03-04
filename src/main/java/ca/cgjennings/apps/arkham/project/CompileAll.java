package ca.cgjennings.apps.arkham.project;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import static resources.Language.string;
import resources.Settings;

/**
 * A task action that compiles <i>all</i> Java source files in a plug-in task.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @see Compile
 * @since 2.1
 */
public class CompileAll extends TaskAction {

    @Override
    public String getLabel() {
        return string("pa-compile-all");
    }

    @Override
    public String getDescription() {
        return string("pa-compile-all-tt");
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        if (member != null) {
            return false;
        }
        if (task == null) {
            return false;
        }
        final Settings taskSettings = task.getSettings();
        return NewTaskType.PLUGIN_TYPE.equals(taskSettings.get(Task.KEY_TYPE))
                && (taskSettings.getBoolean("java-source-hint") || project.getSettings().getBoolean("java-source-hint"));
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        LinkedList<File> sources = new LinkedList<>();
        ProjectUtilities.saveAllOpenProjectFiles(false);
        addSources(sources, task.getFile());
        if (sources.isEmpty()) {
            return true;
        }

        Compile.Compiler jc = Compile.getCompiler();
        if (jc == null) {
            Compile.showNoJDKMessage();
            return false;
        }
        return jc.compile(task.getFile(), sources.toArray(File[]::new));
    }

    private void addSources(List<File> sources, File parent) {
        for (File kid : parent.listFiles()) {
            if (kid.isDirectory() && !kid.isHidden()) {
                addSources(sources, kid);
            } else if (kid.getName().endsWith(".java") && !kid.isHidden()) {
                sources.add(kid);
            }
        }
    }
}
