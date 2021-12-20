package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.apps.arkham.component.AbstractGameComponent;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.deck.DeckDeserializationSupport;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import ca.cgjennings.apps.arkham.sheet.UndecoratedCardBack;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import static resources.Language.string;

/**
 * A page item representing one face of a game component.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class CardFace extends AbstractRenderedItem implements DependentPageItem, EditablePageItem {

    private String path;
    private int sheetIndex;
    private boolean autoMargin = false;
    protected transient Sheet sheet;
    protected transient String name;

    /**
     * Create a {@code CardFace} for use in a deck.
     *
     * @param component the game component to get the faces from
     * @param path the path that the game component can be loaded from in future
     * @param index the index of the face of interest within the component
     */
    public CardFace(GameComponent component, String path, int index) {
        sheetIndex = index;
        this.path = path;
        refresh(component);
        Sheet.DeckSnappingHint snapHint = sheet.getDeckSnappingHint();
        if (snapHint == null) {
            StrangeEons.log.warning("null DeckSnappingHint");
            snapHint = Sheet.DeckSnappingHint.CARD;
        }
        snapHint.apply(this);
    }

    /**
     * Create a {@code CardFace} that can be used on a temporary deck. Temporary
     * decks cannot be saved but can be printed.
     *
     * @param name the name to use for the face
     * @param face the sheet to display
     * @param index the index of the sheet in its source component
     */
    public CardFace(String name, Sheet face, int index) {
        path = "";
        sheetIndex = index;
        this.sheet = face;
        this.name = name == null ? "" : name;
    }

    /**
     * Sets whether the face will generate an automatic nine point bleed margin,
     * if supported by the card face.
     *
     * @param enable whether automatic bleed margin should be enabled, where
     * supported
     * @see #isAutoBleedMarginEnabled()
     */
    public void setAutoBleedMarginEnabled(boolean enable) {
        if (enable != autoMargin) {
            autoMargin = enable;
            clearCachedImages();
        }
    }

    /**
     * Returns {@code true} if the automatic bleed margin feature is enabled.
     *
     * @return {@code true} if an automatic bleed margin is enabled
     * @see #setAutoBleedMarginEnabled(boolean)
     */
    public boolean isAutoBleedMarginEnabled() {
        return autoMargin;
    }

    @Override
    public double[] getFoldMarks() {
        return sheet.getFoldMarks();
    }

    @Override
    public boolean isBleedMarginMarked() {
        return sheet.hasCropMarks();
    }

    @Override
    public double getBleedMargin() {
        return sheet.getRenderedBleedMargin();
    }

    @Override
    public String getName() {
        return name;
    }

    public int getSheetIndex() {
        return sheetIndex;
    }

    public Sheet getSheet() {
        return sheet;
    }

    @Override
    protected double getUprightWidth() {
        return sheet.getPrintDimensions().getWidth();
    }

    @Override
    protected double getUprightHeight() {
        return sheet.getPrintDimensions().getHeight();
    }

    @Override
    protected BufferedImage renderImage(RenderTarget target, double resolution) {
        sheet.setCornerRadius(autoMargin ? 9d : 0d);
        return sheet.paint(target, resolution);
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setPath(String path) {
        this.path = path;

    }

    /**
     * Clears cached representation of the card face. The next time it is drawn,
     * the sheet will be asked to redraw it from the component instance linked
     * with the sheet. This is not suitable if the linked component needs to
     * change (for example, if the file that the component is stored in is
     * overwritten with a new component). In this case, see
     * {@link #refresh(ca.cgjennings.apps.arkham.component.GameComponent)}.
     *
     * @return true if the refresh was successful
     */
    @Override
    public boolean refresh() {
        clearCachedImages();
        itemChanged();
        return true;
    }

    /**
     * Allows for optimized updates when the same component occurs many times in
     * the same deck. The component passed in as a replacement must have a valid
     * set of sheets in place.
     *
     * @param component the component to replace the existing one with
     * @return {@code true} if the existing component was replaced
     */
    public boolean refresh(GameComponent component) {
        if (component == null) {
            return false;
        }
        final Sheet<?>[] sheets = component.getSheets();

        // this is sometimes getting called with components with no sheets;
        // that breaks the contract so we'll dump the stack but create them
        // anyway so the app can continue
        if (sheets == null) {
            throw new AssertionError("component has no sheets");
        }

        if (sheetIndex < sheets.length) {
            clearCachedImages();
            sheet = sheets[sheetIndex];
            updateName(component, sheetIndex, sheets.length);
            return true;
        }
        return false;
    }

    private void updateName(GameComponent component, int index, int count) {
        String label = component.getSheetTitles()[index];
        name = AbstractGameComponent.filterComponentText(component.getFullName());
        if (count > 1) {
            name = string("de-l-card-face-label", name, label);
        }
    }

    @Override
    public void beginEditing() {
        StrangeEons.getWindow().openFile(new File(getPath()));
    }

    @Override
    public void customizePopupMenu(JPopupMenu menu, PageItem[] selection, boolean isSelectionFocus) {
        if (isSelectionFocus) {
            JMenuItem edit = Commands.findCommand(menu, Commands.EDIT_PAGE_ITEM);
            if (edit != null) {
                edit.setText(string("edit-card"));
            }
        }
    }

    private static final int CARD_FACE_VERSION = 2;

    @Override
    protected boolean scaleMipMapUpAtHighZoom(RenderTarget target, double resolution) {
        return (sheet instanceof UndecoratedCardBack) || super.scaleMipMapUpAtHighZoom(target, resolution);
    }

    @Override
    protected void writeImpl(ObjectOutputStream out) throws IOException {
        super.writeImpl(out);

        out.writeInt(CARD_FACE_VERSION);

        out.writeObject(name);
        out.writeObject(path);
        out.writeInt(sheetIndex);
        out.writeBoolean(autoMargin);
    }

    @Override
    protected void readImpl(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readImpl(in);

        int version = in.readInt();

        name = (String) in.readObject();
        path = (String) in.readObject();
        sheetIndex = in.readInt();
        if (version >= 2) {
            autoMargin = in.readBoolean();
        } else {
            autoMargin = false;
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        if (path == null || path.length() == 0) {
            throw new IllegalStateException("cannot write card: has no path");
        }

        writeImpl(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readImpl(in);
        if (!refresh(DeckDeserializationSupport.getShared().findGameComponent(path, name))) {
            StrangeEons.log.warning("refresh() failed when reading game component " + name);
        }
        if (sheet == null) {
            throw new AssertionError("sheet was not set during readObject");
        }
    }
    private static final long serialVersionUID = 3_636_183_606_243_789_529L;
}
