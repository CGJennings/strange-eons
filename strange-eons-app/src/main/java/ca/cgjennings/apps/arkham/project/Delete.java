package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import resources.Language;
import static resources.Language.string;

/**
 * A task action that deletes the selected files in a project.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
class Delete extends TaskAction {

    @Override
    public String getLabel() {
        return string("delete");
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        Member target = resolveTarget(project, task, member);
        File f = target.getFile();
        boolean deleted = false;

        // if the project is a package, we'll need to delete the package after
        File pkg = null;
        if (target instanceof Project) {
            pkg = project.getPackageFile();
            project.close(); // prevent trying to recreate package after deletion
        }

        if (f.exists()) {
            deleted = delete(project, target);
        }
        if (deleted && StrangeEons.getWindow().getOpenProject() == target) {
            StrangeEons.getWindow().closeProject();
        }

        if (pkg != null) {
            deleted = pkg.delete();
        }

        return deleted;
    }

    private boolean delete(Project p, Member target) {
        Member parent = target.getParent();
        boolean ok = true;
        try {
            target.deleteFile();
        } catch (IOException e) {
            ok = false;
            StrangeEons.log.log(Level.SEVERE, "failed to delete member", e);
        }
        if (parent != null) {
            parent.synchronize();
        }
        return ok;
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        return true;
    }

    @Override
    public boolean performOnSelection(Member[] members) {
        if (members.length == 0) {
            return false;
        }
        members = ProjectUtilities.merge(members);

        String message = null;
        if (members[0] instanceof Project) {
            message = string("pa-delete-ver-proj");
        } else if (members.length == 1) {
            if (members[0].isFolder()) {
                message = string("pa-delete-ver-folder", members[0].getName());
            } else {
                message = string("pa-delete-ver", members[0].getName());
            }
        } else {
            int files = 0;
            int folders = 0;
            for (int i = 0; i < members.length; ++i) {
                if (members[i].isFolder()) {
                    ++folders;
                } else {
                    ++files;
                }
            }
            message = string("pa-delete-mixed");
            if (folders > 0) {
                String pl = Language.getInterface().getPluralizer().pluralize(folders, "pa-delete-mixed-folder");
                message += SPACER + String.format(pl, folders);
            }
            if (files > 0) {
                String pl = Language.getInterface().getPluralizer().pluralize(files, "pa-delete-mixed-file");
                message += SPACER + String.format(pl, files);
            }
        }

        int option = JOptionPane.showConfirmDialog(StrangeEons.getWindow(),
                "<html>" + message, getLabel(), JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (option == JOptionPane.YES_OPTION) {
            boolean success = super.performOnSelection(members);
            if (!success) {
                ErrorDialog.displayError(string("prj-err-inc-delete"), null);
            }
            return success;
        }
        return false;
    }

    private static final String SPACER = "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
}
