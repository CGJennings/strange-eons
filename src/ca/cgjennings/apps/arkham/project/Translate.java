package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.editors.PropertyBundleEditor;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import javax.swing.UIManager;
import static resources.Language.string;

/**
 * This task action applies to {@code .properties} files that are not in
 * the default language (that is, they have at least one underscore
 * {@code _} in the file name, e.g., {@code X_en.properties}).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class Translate extends TaskAction {

    @Override
    public String getLabel() {
        return string("pa-trans-name");
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        return member != null
                && member.getName().indexOf('_') >= 0
                && ProjectUtilities.matchExtension(member, "properties");
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        File f = member.getFile();
        StrangeEonsEditor[] eds = StrangeEons.getWindow().getEditorsShowingFile(f);
        if (eds.length > 0) {
            eds[0].select();
            UIManager.getLookAndFeel().provideErrorFeedback((Component) eds[0]);
        } else {
            try {
                StrangeEons.getWindow().addEditor(new PropertyBundleEditor(f));
            } catch (IOException e) {
                ErrorDialog.displayError(string("prj-err-open", f.getName()), e);
            }
        }
        return true;
    }
}
