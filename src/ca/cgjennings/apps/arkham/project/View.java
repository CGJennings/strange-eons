package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.dialog.ImageViewer;
import ca.cgjennings.apps.arkham.dialog.InsertCharsDialog;
import ca.cgjennings.apps.arkham.dialog.VectorImageViewer;
import ca.cgjennings.apps.arkham.plugins.InstallationNotesViewer;
import ca.cgjennings.graphics.shapes.SVGVectorImage;
import ca.cgjennings.graphics.shapes.VectorImage;
import ca.cgjennings.imageio.PSDImageReader;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JDialog;
import static resources.Language.string;

/**
 * A task action that displays files in a preview window. This is the default
 * action for files that can be viewed but not edited within Strange Eons, such
 * as image and font files.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class View extends TaskAction {

    @Override
    public String getLabel() {
        return string("pa-view");
    }

    @Override
    public String getDescription() {
        return string("pa-view-tt");
    }

    protected int cascade;

    @Override
    public boolean performOnSelection(Member[] members) {
        cascade = 0;
        return super.performOnSelection(members);
    }

    private void moveWindow(Project p, Window w, int cascade) {
        if (p == null) {
            w.setLocationRelativeTo(StrangeEons.getWindow());
        } else {
            p.getView().moveToLocusOfAttention(w, cascade);
        }
    }

    static BufferedImage getSupportedImage(File f) throws IOException {
        if (!ProjectUtilities.matchExtension(f, imageTypes)) {
            return null;
        }

        BufferedImage image = null;
        if (ProjectUtilities.matchExtension(f, "psd")) {
            PSDImageReader psdr = new PSDImageReader(f);
            image = psdr.createComposite();
        } else if (ProjectUtilities.matchExtension(f, vectorImageSubtypes)) {
            VectorImage vi = new SVGVectorImage(f);
            return vi.createRasterImage(64, 64, true);
        } else {
            image = ImageIO.read(f);
        }
        return image;
    }

    public boolean tryInternalView(Project project, Member member, File f) {
        if (member == null) {
            cascade = 0;
        }

        if (ProjectUtilities.matchExtension(f, imageTypes)) {
            try {
                JDialog viewer;
                if (ProjectUtilities.matchExtension(f, vectorImageSubtypes)) {
                    VectorImage image = new SVGVectorImage(f);
                    VectorImageViewer iv = new VectorImageViewer(StrangeEons.getWindow(), image, f, false);
                    viewer = iv;
                } else {
                    BufferedImage image = getSupportedImage(f);
                    if (image == null) {
                        throw new IOException("Invalid file format");
                    }
                    ImageViewer iv = new ImageViewer(StrangeEons.getWindow(), image, f, false);
                    viewer = iv;
                }
                viewer.setTitle(f.getName() + " (" + ProjectUtilities.formatByteSize(f.length()) + ")");
                moveWindow(project, viewer, cascade++);
                viewer.setVisible(true);
            } catch (IOException e) {
                ErrorDialog.displayError(string("prj-err-open", f.getName()), e);
            }
        } else if (ProjectUtilities.matchExtension(f, fontTypes)) {
            Font font = null;
            String ext = ProjectUtilities.getFileExtension(f);
            int type = ext.equals("ttf") || ext.equals("otf") ? Font.TRUETYPE_FONT : Font.TYPE1_FONT;
            try {
                font = tryToReadFont(type, f);
                if (font == null) {
                    font = tryToReadFont(type == Font.TRUETYPE_FONT ? Font.TYPE1_FONT : Font.TRUETYPE_FONT, f);
                }
                if (font == null) {
                    throw new IOException("Unknown font file format");
                }
                JDialog d = InsertCharsDialog.createFontViewer(font);
                d.setLocationByPlatform(false);
                moveWindow(project, d, cascade++);
                d.setVisible(true);
            } catch (IOException e) {
                ErrorDialog.displayError(string("prj-err-open", f.getName()), e);
            }
        } else if (ProjectUtilities.matchExtension(f, documentTypes)) {
            InstallationNotesViewer v = new InstallationNotesViewer(StrangeEons.getWindow(), member.getURL());
            moveWindow(project, v, cascade++);
            v.setTitle(member.getBaseName());
            v.setVisible(true);
        }
        return true;
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        return tryInternalView(project, member, member.getFile());
    }

    private static Font tryToReadFont(int format, File f) throws IOException {
        try {
            // copy the font to a temporary file that is deleted on exit:
            // this prevents a lock from being set on the original file,
            // so it can still be moved/deleted/renamed/etc. from the project
            // system
            File t = File.createTempFile("se_vf_", '.' + ProjectUtilities.getFileExtension(f));
            t.deleteOnExit();
            ProjectUtilities.copyFile(f, t);
            return Font.createFont(format, t);
        } catch (FontFormatException e) {
            return null;
        }
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        return member != null && ProjectUtilities.matchExtension(member, allTypes);
    }

    static final String[] imageTypes = new String[]{
        "png", "jpg", "jp2", "gif", "psd", "svg", "svgz"
    };
    /**
     * The subset of {@link #imageTypes} that represents vector images.
     */
    static final String[] vectorImageSubtypes = new String[]{
        "svg", "svgz"
    };
    static final String[] fontTypes = new String[]{
        "ttf", "otf", "pfa", "pfb"
    };
    static final String[] documentTypes = new String[] {
        "md"
    };
    static final String[] allTypes;

    static {
        allTypes = new String[imageTypes.length + fontTypes.length + documentTypes.length];
        int j = 0;
        for (int i = 0; i < imageTypes.length;) {
            allTypes[j++] = imageTypes[i++];
        }
        for (int i = 0; i < fontTypes.length;) {
            allTypes[j++] = fontTypes[i++];
        }
        for (int i = 0; i < documentTypes.length;) {
            allTypes[j++] = documentTypes[i++];
        }        
    }
}
