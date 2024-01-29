package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.platform.DesktopIntegration;
import java.io.IOException;
import static resources.Language.string;

/**
 * Task action that shows the selected HTML document(s) in the system web
 * browser.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class Browse extends TaskAction {

    public Browse() {
    }

    @Override
    public String getLabel() {
        return string("pa-browse-name");
    }

    @Override
    public String getDescription() {
        return string("pa-browse-tt");
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        if (member != null) {
            try {
                DesktopIntegration.browse(member.getFile().toURI(), StrangeEons.getWindow().getOpenProjectView());
                return true;
            } catch (IOException e) {
                ErrorDialog.displayError(string("prj-err-open", member.getName()), e);
            }
        }
        return false;
    }

    @Override
    public boolean appliesToSelection(Member[] members) {
        return DesktopIntegration.BROWSE_SUPPORTED && super.appliesToSelection(members);
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        return member != null && ProjectUtilities.matchExtension(member, HTML_EXTENSIONS);
    }

    private static final String[] HTML_EXTENSIONS = new String[]{"html", "htm"};
}
