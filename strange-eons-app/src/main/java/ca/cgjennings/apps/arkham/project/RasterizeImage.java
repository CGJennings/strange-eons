package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.graphics.shapes.SVGVectorImage;
import ca.cgjennings.graphics.shapes.VectorImage;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * Project action that rasterizes vector images (creating bitmapped versions of
 * the image).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class RasterizeImage extends TaskAction {

    @Override
    public String getLabel() {
        return string("rast-action");
    }

    @Override
    public String getDescription() {
        return string("rast-desc");
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        if (appliesTo(project, task, member)) {
            try {
                ProjectView pv = project.getView();
                VectorImage vi = ResourceKit.getVectorImage(member.getURL().toExternalForm());
//				VectorImage vi = ResourceKit.getVectorImage( member.getFile().toURI().toString() );
                RasterizeDialog d = new RasterizeDialog(vi);
                if (pv != null) {
                    pv.moveToLocusOfAttention(d);
                }
                int[] size = d.showDialog();
                if (size != null) {
                    File out = new File(member.getFile().getParent(), member.getBaseName() + ".png");
                    out = ProjectUtilities.getAvailableFile(out);
                    BufferedImage bi = vi.createRasterImage(size[0], size[1], false);
                    ImageIO.write(bi, "png", out);
                    member.getParent().synchronize();
                    Member rasterized = member.getParent().findChild(out);
                    if (rasterized != null && pv != null) {
                        pv.select(rasterized);
                    }
                }
                return true;
            } catch (Exception ex) {
                ErrorDialog.displayError(string("ae-err-save"), ex);
            }
        }
        return false;
    }

    @Override
    public boolean appliesToSelection(Member[] members) {
        return SVGVectorImage.isSupported() && super.appliesToSelection(members);
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        return member != null && ProjectUtilities.matchExtension(member, "svg", "svgz");
    }
}
