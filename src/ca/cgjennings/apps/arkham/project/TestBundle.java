package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.plugins.PluginBundle;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import static resources.Language.string;

/**
 * A task action that starts a separate copy of the application in order to test
 * a plug-in bundle.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class TestBundle extends TaskAction {

    public TestBundle() {
    }

    @Override
    public String getLabel() {
        return string("pa-test-bundle");
    }

    @Override
    public String getDescription() {
        return string("pa-test-bundle-tt");
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        ProjectUtilities.saveAllOpenProjectFiles(false);
        // when this is null, user applied the action to a plug-in folder:
        // try to compile Java code and build the plug-in bundle first
        if (member == null) {
            // Make Bundle
            MakeBundle mb = (MakeBundle) Actions.getUnspecializedAction("makebundle");
            if (mb == null) {
                mb = new MakeBundle();
            }
            if (mb.appliesTo(project, task, member)) {
                if (!mb.perform(project, task, member)) {
                    return false;
                }
            } else {
                Toolkit.getDefaultToolkit().beep();
                throw new AssertionError("applied to plug-in folder, but MakeBundle does not apply");
            }
            Member[] sel = project.getView().getSelectedMembers();
            if (sel.length > 0) {
                member = sel[0];
            }
            if (!ProjectUtilities.matchExtension(member, BUNDLE_EXTENSIONS)) {
                Toolkit.getDefaultToolkit().beep();
                System.err.println("Selected member after bundle construction is not a bundle");
                return false;
            }
        }

        File f = member.getFile();
        try {
            boolean copy = PluginBundle.getBundleFormat(f) == PluginBundle.FORMAT_WRAPPED;
            TestBundleDialog d = new TestBundleDialog(f, copy);
            project.getView().moveToLocusOfAttention(d);
            d.setVisible(true);
            return true;
        } catch (IOException e) {
            ErrorDialog.displayError(string("prj-err-open", f.getName()), e);
        }
        return false;
    }

    @Override
    public boolean appliesToSelection(Member[] members) {
        return members.length == 1 && super.appliesToSelection(members);
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        return (member != null && ProjectUtilities.matchExtension(member, BUNDLE_EXTENSIONS))
                || (member == null && task != null && NewTaskType.PLUGIN_TYPE.equals(task.getSettings().get(Task.KEY_TYPE)) && task.getChildCount() > 0);
    }

    private static final String[] BUNDLE_EXTENSIONS = new String[]{"seplugin", "seext", "setheme", "selibrary"};
}
