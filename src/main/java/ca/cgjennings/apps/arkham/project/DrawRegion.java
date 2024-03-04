package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.RegionPicker;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import java.awt.image.BufferedImage;
import java.io.IOException;
import static resources.Language.string;

/**
 * Task action that allows the user to define regions interactively by drawing
 * them on the selected image.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class DrawRegion extends TaskAction {

    @Override
    public String getLabel() {
        return string("pa-draw-region");
    }

    @Override
    public String getDescription() {
        return string("pa-draw-region-tt");
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        if (member == null) {
            return false;
        }
        return ProjectUtilities.matchExtension(member, View.imageTypes);
    }

    @Override
    public boolean performOnSelection(Member[] members) {
        cascade = 0;
        return super.performOnSelection(members);
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        BufferedImage bi;
        try {
            bi = View.getSupportedImage(member.getFile());
        } catch (IOException e) {
            ErrorDialog.displayError(string("prj-err-open", member.getFile().getName()), e);
            return false;
        }

        RegionPicker rpd = new RegionPicker(StrangeEons.getWindow(), false);
        rpd.setImage(bi);
        rpd.setCloseMode(true);
        project.getView().moveToLocusOfAttention(rpd, cascade++);
        rpd.setVisible(true);
        return true;
    }

    private int cascade;
}
