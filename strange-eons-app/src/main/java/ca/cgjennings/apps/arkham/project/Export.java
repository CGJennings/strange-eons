package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.BusyDialog;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.ZipOutputStream;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * Task action that exports a project to an archive. This action applies to the
 * project folder, and when performed it creates a ZIP archive of the project
 * contents. The chosen export file name is retained in the project's settings.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class Export extends TaskAction {

    @Override
    public String getLabel() {
        return string("pa-export");
    }

    @Override
    public String getDescription() {
        return string("pa-export-tt");
    }

    @Override
    public boolean perform(final Project project, Task task, Member member) {
        Settings s = project.getSettings();
        String name = s.get(KEY_EXPORT_FILE);
        if (name == null) {
            name = project.getName();
        }
        final File f = ResourceKit.showZipFileDialog(StrangeEons.getWindow(), name);
        if (f == null) {
            return false;
        }
        name = f.getName();
        if (name.toLowerCase(Locale.CANADA).endsWith(".zip")) {
            name = name.substring(0, name.length() - ".zip".length());
        }
        s.set(KEY_EXPORT_FILE, name);

        packError = null;
        new BusyDialog(StrangeEons.getWindow(), string("busy-exporting"), () -> {
            StrangeEons.setWaitCursor(true);
            ZipOutputStream zip = null;
            try {
                zip = ProjectUtilities.createPluginBundleArchive(f, false);
                ProjectUtilities.copyToArchive(zip, project.getFile(), "", true, true, true);
            } catch (IOException e) {
                packError = e;
            } finally {
                if (zip != null) {
                    try {
                        zip.close();
                    } catch (IOException e) {
                        packError = e;
                    }
                }
                StrangeEons.setWaitCursor(false);
            }
        });
        if (packError != null) {
            ErrorDialog.displayError(string("rk-err-export"), packError);
            packError = null;
            return false;
        }
        return true;
    }
    private Throwable packError;

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        return task == null && member == null;
    }

    @Override
    public boolean appliesToSelection(Member[] members) {
        return members.length == 1 ? super.appliesToSelection(members) : false;
    }

    public static final String KEY_EXPORT_FILE = "export-file";
}
