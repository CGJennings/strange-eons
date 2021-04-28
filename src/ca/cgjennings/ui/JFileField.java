package ca.cgjennings.ui;

import ca.cgjennings.apps.arkham.MarkupTargetFactory;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ImageResourceBrowser;
import ca.cgjennings.apps.arkham.project.Member;
import ca.cgjennings.apps.arkham.project.Project;
import ca.cgjennings.apps.arkham.project.ProjectFolderDialog;
import ca.cgjennings.ui.dnd.FileDrop;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import resources.ResourceKit;

/**
 * A specialized text field for choosing files and folders.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class JFileField extends JTextField {

    public JFileField() {
        super(FilteredDocument.createFilePathDocument(), null, 0);

        new IconBorder(ResourceKit.getIcon("ui/controls/file-field.png"))
                .installClickable(this, ResourceKit.getIcon("ui/controls/file-field-hi.png"), (ActionEvent e) -> {
                    String sel = showFileDialog(e.getModifiers());
                    if (sel != null) {
                        setText(sel);
                        fireActionPerformed();
                    }
        }, null);

        new FileDrop(this, (File[] files) -> {
            if (files.length > 0 && files[0] != null) {
                String path = null;
                if (type == FileType.PORTRAIT) {
                    Project op = StrangeEons.getOpenProject();
                    if (op != null) {
                        Member m = op.findMember(files[0]);
                        if (m != null) {
                            URL u = m.getURL();
                            if (u != null) {
                                path = u.toString();
                            }
                        }
                    }
                }
                if (path == null) {
                    path = files[0].getAbsolutePath();
                }
                
                setText(path);
                fireActionPerformed();
            }
        });
        MarkupTargetFactory.enableTargeting(this, false);
    }

    public String showFileDialog(int modifiers) {
        String raw = null;
        File sel = null;
        switch (type) {
            case COMPONENT:
                sel = ResourceKit.showOpenDialog(this);
                break;
            case COMPONENT_SAVE:
                File f = getFile();
                sel = ResourceKit.showSaveDialog(this, f == null ? "" : f.getName());
                break;
            case BITMAP_IMAGE:
                sel = ResourceKit.showBitmapImageFileDialog(this);
                break;
            case IMAGE:
                sel = ResourceKit.showImageFileDialog(this);
                break;
            case PORTRAIT:
                if ((modifiers & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK) {
                    ImageResourceBrowser rb = new ImageResourceBrowser();
                    rb.setLocationRelativeTo(this);
                    raw = rb.showDialog();
                } else {
                    sel = ResourceKit.showImageFileDialog(this);
                }
                break;
            case PLUGIN:
                sel = ResourceKit.showPluginFileDialog(this);
                break;
            case SCRIPT:
                sel = ResourceKit.showScriptFileDialog(this);
                break;
            case GENERIC:
                sel = ResourceKit.showGenericOpenDialog(this, getFile(), desc, ext);
                break;
            case GENERIC_SAVE:
                sel = ResourceKit.showGenericSaveDialog(this, getFile(), desc, ext);
                break;
            case PROJECT_CONTAINER:
                ProjectFolderDialog d = new ProjectFolderDialog(SwingUtilities.windowForComponent(this), ProjectFolderDialog.Mode.SELECT_PROJECT_CONTAINER);
                d.setSelectedFolder(getFile());
                sel = d.showDialog();
                break;
            default:
                throw new AssertionError("unknown FileType: " + type);
        }

        if (raw == null && sel != null) {
            raw = sel.getAbsolutePath();
        }

        return raw;
    }

    private String desc;
    private String[] ext;
    private FileType type = FileType.COMPONENT;

    public final void setGenericFileTypeDescription(String description) {
        desc = description;
    }

    public final String getGenericFileTypeDescription() {
        return desc;
    }

    public final void setGenericFileTypeExtensions(String... extension) {
        ext = extension;
    }

    public final String[] getGenericFileTypeExtensions() {
        return ext == null ? null : ext.clone();
    }

    public final void setFileType(FileType type) {
        this.type = type;
    }

    public final FileType getFileType() {
        return type;
    }

    /**
     * Standard Strange Eons file types that this file field can be set to
     * accept. If the type is set to <code>GENERIC</code> or
     * <code>GENERIC_SAVE</code>, the accepted file extensions and description
     * can be set via {@link #setGenericFileTypeExtensions(java.lang.String[])}
     * and {@link #setGenericFileTypeDescription(java.lang.String)},
     * respectively.
     */
    public enum FileType {
        /**
         * Strange Eons game components (cards, markers, etc.)
         */
        COMPONENT,
        /**
         * Only bitmap format images (such as JPEG), not vector image formats
         * (such as SVG).
         */
        BITMAP_IMAGE,
        /**
         * Any supported image file.
         */
        IMAGE,
        /**
         * Any supported image file; shift+click to choose an internal image
         * resource.
         */
        PORTRAIT,
        /**
         * Any plug-in bundle.
         */
        PLUGIN,
        /**
         * A script file.
         */
        SCRIPT,
        /**
         * Use the generic extensions and description.
         */
        GENERIC,
        /**
         * A Save (instead of Open) file dialog for Strange Eons game
         * components.
         */
        COMPONENT_SAVE,
        /**
         * A Save (instead of Open) file dialog using the generic extensions and
         * description.
         */
        GENERIC_SAVE,
        /**
         * A folder for a project to be created within.
         */
        PROJECT_CONTAINER
    }

    /**
     * If the field contains a non-empty string, returns a <code>File</code>
     * from the field content. Otherwise, returns <code>null</code>.
     *
     * @return the field text as a file, if it contains characters other than
     * whitespace
     */
    public File getFile() {
        String text = getText();
        if (text.trim().isEmpty()) {
            return null;
        }
        return new File(text);
    }
}
