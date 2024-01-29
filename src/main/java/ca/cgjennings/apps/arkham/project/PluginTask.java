package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import static resources.Language.string;

/**
 * Create a new plug-in using an extendable wizard.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class PluginTask extends NewTaskType {

    @Override
    public String getLabel() {
        return string("pt-new-plugin-name");
    }

    @Override
    public String getDescription() {
        return string("pt-new-plugin-desc");
    }

    @Override
    public String getType() {
        return NewTaskType.PLUGIN_TYPE;
    }

    @Override
    public boolean initializeNewTask(Project project, Task task) {
        PluginWizardDialog pw = new PluginWizardDialog(StrangeEons.getWindow(), task);
        project.getView().moveToLocusOfAttention(pw);
        boolean ok = pw.showDialog();
        task.getSettings().set(Clean.KEY_CLEAN_EXTENSIONS, "class");

        return ok;
    }
}
