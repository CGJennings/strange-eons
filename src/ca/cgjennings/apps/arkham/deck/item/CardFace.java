package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.apps.arkham.component.AbstractGameComponent;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.deck.Deck;
import ca.cgjennings.apps.arkham.deck.DeckDeserializationSupport;
import ca.cgjennings.apps.arkham.deck.Page;
import ca.cgjennings.apps.arkham.sheet.FinishStyle;
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
public class CardFace extends AbstractRenderedItem implements DependentPageItem, EditablePageItem, BleedMarginStyle {

    private String path;
    private int sheetIndex;
    private FinishStyle finish = FinishStyle.SQUARE;
    private double bleedMargin = 9d;
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

    @Override
    public FinishStyle getFinishStyle() {
        return finish;
    }

    @Override
    public void setFinishStyle(FinishStyle style) {
        transitionAutoMarginOnFirstRender = false;
        if (style == null) {
            Page p = getPage();
            if (p != null) {
                Deck d = p.getDeck();
                style = d.getFinishStyle();
                setBleedMarginWidth(d.getBleedMarginWidth());
            }
        }
        if (style != finish) {
            finish = style;
            clearCachedImages();
            itemChanged();
        }
    }

    @Override
    public double getBleedMarginWidth() {
        return bleedMargin;
    }

    @Override
    public void setBleedMarginWidth(double widthInPoints) {
        transitionAutoMarginOnFirstRender = false;
        if (bleedMargin != widthInPoints) {
            bleedMargin = widthInPoints;
            if (finish != FinishStyle.ROUND && finish != FinishStyle.SQUARE) {
                clearCachedImages();
                itemChanged();
            }
        }
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
        // update margin settings from old deck file:
        // this would either render the exact designed bleed margin, or,
        // if no designed margin, render no margin or a 9pt synthesized option
        // depending on a deck option; the synthesis option is indicated by
        // setting the finishe to MARGIN (otherwise it is SQUARE)
        if (transitionAutoMarginOnFirstRender) {
            transitionAutoMarginOnFirstRender = false;
            double designedMargin = sheet.getBleedMargin();

            // if card has no designed margin and synthesis was requested,
            // set to synthesize a margin
            if (designedMargin == 0d) {
                // set the bleed to a default in case they enable bleed later;
                // if synthesis was requested, the style will be MARGIN and
                // it will be rendered right away; otherwise we set the preferred
                // default in case the user enables bleed later
                bleedMargin = 9d;
            } // otherwise set the margin to the exact designed size
            else {
                finish = FinishStyle.MARGIN;
                bleedMargin = designedMargin;
            }
        }

        double ubm;
        FinishStyle fs = finish;
        if (fs == null) {
            fs = FinishStyle.SQUARE;
        }
        if (fs == FinishStyle.MARGIN) {
            ubm = bleedMargin;
        } else {
            ubm = fs.getSuggestedBleedMargin();
        }
        sheet.setUserBleedMargin(ubm);
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
        sheet.markChanged();
        sheet.freeCachedResources();
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

    private static final int CARD_FACE_VERSION = 3;

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
        out.writeObject(finish == null ? null : finish.name());
        out.writeDouble(bleedMargin);
    }

    @Override
    protected void readImpl(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readImpl(in);

        int version = in.readInt();

        name = (String) in.readObject();
        path = (String) in.readObject();
        sheetIndex = in.readInt();

        if (version >= 3) {
            finish = null;
            String finishName = (String) in.readObject();
            if (finishName != null) {
                try {
                    finish = FinishStyle.valueOf(finishName);
                } catch (IllegalArgumentException iae) {
                    StrangeEons.log.warning("bad finish style");
                }
            }
            bleedMargin = in.readDouble();
            transitionAutoMarginOnFirstRender = false;
        } else /* (version == 1 || version == 2) */ {
            // convert old synthetic bleed option to something reasonable
            boolean autoMargin = false;
            if (version == 2) {
                autoMargin = in.readBoolean();
            }
            bleedMargin = 9d;
            if (autoMargin) {
                finish = FinishStyle.MARGIN;
            } else {
                finish = FinishStyle.SQUARE;
            }
            transitionAutoMarginOnFirstRender = true;
        }
    }
    /**
     * Set during loading if card is from an old version. This indicates that
     * the edge finish needs to be upgraded once the sheet is available.
     * See {@link #renderImage(ca.cgjennings.apps.arkham.sheet.RenderTarget, double)}
     */
    private transient boolean transitionAutoMarginOnFirstRender = false;

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
