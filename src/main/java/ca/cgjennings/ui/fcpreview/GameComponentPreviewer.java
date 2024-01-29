package ca.cgjennings.ui.fcpreview;

import ca.cgjennings.apps.arkham.component.ComponentMetadata;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.deck.Deck;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import ca.cgjennings.layout.MarkupRenderer;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import javax.swing.JFileChooser;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * A file chooser accessory that previews selected game components by showing
 * their first sheet image. For components that don't have sheets, a default
 * image will be shown for some known special cases (e.g. decks), and otherwise
 * no preview is provided.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class GameComponentPreviewer extends ImagePreviewer {

    volatile File file = null;

    public GameComponentPreviewer() {
        super();
    }

    public GameComponentPreviewer(JFileChooser fc) {
        super(fc);
    }

    /**
     * Creates the preview image from the target file. Subclasses should
     * override this to create a custom previewer for a different file type.
     * Note that this is typically
     * <b>not</b> run from the event dispatch thread. If constructing the image
     * fails for any reason, this method must return {@code null} and should not
     * display an error message.
     *
     * @param o the file to compose a preview of
     * @return the preview image
     */
    @Override
    protected BufferedImage createPreviewImage(Object o) {
        final File f = (File) o;
        switch (metadataCheck(f)) {
            case TYPE_DECK:
                return ResourceKit.getThemedImage("icons/application/preview/deck.png");
            case TYPE_CASE:
                return ResourceKit.getThemedImage("icons/application/preview/casebook.jpg");
            case TYPE_UNKNOWN_NDL:
                return null;
            case TYPE_LEGACY_FORMAT:
                return getLegacyFormatImage();
            case TYPE_DRAW:
                GameComponent gc = ResourceKit.getGameComponentFromFile(f, false);
                if (Thread.currentThread().isInterrupted()) {
                    return null;
                }
                Sheet[] sheets = null;
                if ((gc == null) || (sheets = gc.createDefaultSheets()) == null || sheets.length == 0) {
                    return null;
                }
                if (Thread.currentThread().isInterrupted()) {
                    return null;
                }
                sheets[0].setPrototypeRenderingModeEnabled(false);
                sheets[0].setUserBleedMargin(-1d);
                return sheets[0].paint(RenderTarget.PREVIEW, 96d);
            default:
                throw new AssertionError("unknown metadata type result");
        }
    }

    /**
     * Returns {@code true} if this file appears to be of a type for which a
     * preview can be created.
     *
     * @param o the file to check
     * @return {@code true} if previewing this file is expected to succeed
     */
    @Override
    public boolean isResourceTypeSupported(Object o) {
        if (!(o instanceof File)) {
            return false;
        }
        final File f = (File) o;
        if (f.isDirectory()) {
            return false;
        }
        return f.getName().endsWith(".eon");
    }

    private int metadataCheck(File f) {
        ComponentMetadata cm = new ComponentMetadata(f);

        if (cm.getMetadataVersion() < 1) {
            return TYPE_LEGACY_FORMAT;
        }

        String klass = cm.getComponentClassName();
        if ("ca.cgjennings.apps.arkham.casebook.Casebook".equals(klass)) {
            return TYPE_CASE;
        }
        if (Deck.class.getName().equals(klass)) {
            return TYPE_DECK;
        }
        if (!cm.isDeckLayoutSupported()) {
            return TYPE_UNKNOWN_NDL;
        }

        return TYPE_DRAW;
    }

    private static final int TYPE_DRAW = 0;
    private static final int TYPE_DECK = 1;
    private static final int TYPE_CASE = 2;
    private static final int TYPE_UNKNOWN_NDL = 3;
    private static final int TYPE_LEGACY_FORMAT = 4;

    private BufferedImage getLegacyFormatImage() {
        MarkupRenderer mr = new MarkupRenderer();
        mr.getDefaultStyle().add(
                TextAttribute.FAMILY, "SansSerif",
                TextAttribute.FOREGROUND, new javax.swing.JLabel().getForeground(),
                //TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD,
                TextAttribute.SIZE, 14
        );
        mr.setAlignment(MarkupRenderer.LAYOUT_MIDDLE | MarkupRenderer.LAYOUT_CENTER);
        mr.setTextFitting(MarkupRenderer.FIT_SCALE_TEXT);
        mr.setMarkupText(string("rk-legacy-preview"));
        BufferedImage legacyImage = new BufferedImage(200, 128, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = legacyImage.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Map<?, ?> hints = (Map<?, ?>) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
            if (hints != null) {
                g.addRenderingHints(hints);
            }
            g.setPaint(getBackground());
            g.fillRect(0, 0, legacyImage.getWidth(), legacyImage.getHeight());
            mr.draw(g, new Rectangle(0, 0, legacyImage.getWidth(), legacyImage.getHeight()));
        } finally {
            g.dispose();
        }
        return legacyImage;
    }
}
