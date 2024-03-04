package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.ui.AutocompletionDocument;
import ca.cgjennings.ui.BlankIcon;
import ca.cgjennings.ui.IconBorder;
import ca.cgjennings.ui.IconProvider;
import ca.cgjennings.ui.JIconComboBox;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import resources.ResourceKit;

/**
 * A combo box that selects an image from the image resources in a project.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ImageResourceCombo extends JIconComboBox<Object> {

    private File base;

    {
        setEditable(true);
        AutocompletionDocument.install(this, true);
        IconBorder.applyLabelBorder(this, "res://", ResourceKit.getIcon("ui/controls/image-field.png"), null, null, null);
    }

    public ImageResourceCombo() {
    }

    public ImageResourceCombo(Member taskFolder) {
        this(taskFolder.getFile());
    }

    public ImageResourceCombo(File taskFolder) {
        setResourceBase(taskFolder);
    }

    public void setRepresentativeImageMode(boolean pngAndJp2Only) {
        activeExtensions = pngAndJp2Only ? rimExtensions : extensions;
    }

    public boolean isRepresentativeImageMode() {
        return activeExtensions == rimExtensions;
    }

    public void setResourceBase(File taskFolder) {
        DefaultComboBoxModel<Object> m = new DefaultComboBoxModel<>();
        m.addElement(new Resource(null, ""));

        if (taskFolder == null) {
            setModel(m);
            return;
        }

        if (taskFolder.isDirectory()) {
            File resFolder = new File(taskFolder, "resources");
            if (resFolder.isDirectory()) {
                scanFolder(resFolder, m, true, false, "");
            }
            scanFolder(taskFolder, m, true, true, "/");
        }

        base = taskFolder;
        setModel(m);
    }

    public File getResourceBase() {
        return base;
    }

    private static final int ICON_SIZE = 16;

    private static class DeferredIcon implements Icon {

        File f;
        BufferedImage im;

        public DeferredIcon(File f) {
            this.f = f;
        }

        @Override
        public int getIconHeight() {
            return ICON_SIZE;
        }

        @Override
        public int getIconWidth() {
            return ICON_SIZE;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (im == null) {
                try {
                    im = ImageIO.read(f);
                } catch (IOException e) {
                }
                float s = ImageUtilities.idealCoveringScaleForImage(ICON_SIZE, ICON_SIZE, im.getWidth(), im.getHeight());
                im = ImageUtilities.resample(im, s, true, RenderingHints.VALUE_INTERPOLATION_BILINEAR, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                im = ImageUtilities.center(im, ICON_SIZE, ICON_SIZE);
            }
            g.drawImage(im, x, y, null);
        }
    }

    private static class Resource implements IconProvider {
        String path;
        Icon i;

        public Resource(File imgFile, String respath) {
            if (imgFile == null) {
                i = new BlankIcon(ICON_SIZE);
            } else {
                i = new DeferredIcon(imgFile);
            }
            path = respath;
        }

        @Override
        public Icon getIcon() {
            return i;
        }

        @Override
        public String toString() {
            return path;
        }

        @Override
        public boolean equals(Object rhs) {
            return rhs instanceof Resource && Objects.equals(path, ((Resource) rhs).path);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(path);
        }
    }

    private void scanFolder(File parent, DefaultComboBoxModel<Object> m, boolean recurse, boolean rootMode, String path) {
        for (File kid : parent.listFiles()) {
            if (kid.isDirectory()) {
                if (!recurse) {
                    continue;
                }
                if (rootMode && kid.getName().equals("resources")) {
                    continue;
                }
                scanFolder(kid, m, true, false, path + kid.getName() + '/');
            } else {
                if (accept(kid)) {
                    m.addElement(new Resource(kid, path + kid.getName()));
                }
            }
        }
    }

    protected boolean accept(File f) {
        return ProjectUtilities.matchExtension(f, activeExtensions);
    }
    private final String[] extensions = new String[]{"png", "jpg", "jp2"};
    private final String[] rimExtensions = new String[]{"png", "jp2"};
    private String[] activeExtensions = rimExtensions;
}
