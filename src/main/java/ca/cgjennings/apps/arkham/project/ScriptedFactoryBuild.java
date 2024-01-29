package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.project.NewTaskType.ScriptedFactory;
import ca.cgjennings.ui.ClipPlayer;
import java.io.File;
import java.io.FileNotFoundException;
import static resources.Language.string;
import resources.Settings;

/**
 * Task action that runs the make script associated with a scripted factory.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class ScriptedFactoryBuild extends TaskAction {

    @Override
    public String getLabel() {
        return string("pa-fmake-scripted");
    }

    @Override
    public String getDescription() {
        return string("pa-fmake-scripted-tt");
    }

    @Override
    public String getActionName() {
        return super.getActionName();
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        Settings s = task.getSettings();
        String makeFileRelative = s.get(ScriptedFactory.KEY_MAKE_SCRIPT, "make.js");
        File makeFile = new File(task.getFile(), makeFileRelative.replace('/', File.separatorChar));
        member = project.findMember(makeFile);

        ClipPlayer cp = null;
        try {
            if (member == null) {
                throw new FileNotFoundException(makeFile.getPath());
            }

            if (Settings.getShared().getYesNo("play-factory-audio")) {
                cp = new ClipPlayer(getClass().getResource("factory.wav"), true);
                cp.play();
            }
            ProjectUtilities.runScript(makeFile, project, task, member, false);
            return true;
        } catch (Exception e) {
            ErrorDialog.displayError(string("prj-err-factory", task), e);
            return false;
        } finally {
            if (cp != null) {
                cp.stop();
            }
        }
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        if (task == null || member != null) {
            return false;
        }

        Settings s = task.getSettings();
        String type = s.get(Task.KEY_TYPE);
        if (NewTaskType.FACTORY_TYPE.equals(type)) {
            String subtype = s.get(Task.KEY_SUBTYPE);
            if (NewTaskType.FACTORY_SCRIPTED_SUBTYPE.equals(subtype)) {
                return true;
            }
        }
        return false;
    }
}
