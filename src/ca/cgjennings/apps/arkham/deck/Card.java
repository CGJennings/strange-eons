package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.deck.item.CardFace;
import ca.cgjennings.apps.arkham.deck.item.PageItem;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * @deprecated This class is retained only for backwards compatibility with very
 * old decks. All objects that can be placed in a deck are now implementations
 * of {@link ca.cgjennings.apps.arkham.deck.item.PageItem}.
 *
 * An obsolete kind of deck object. This class is a skeleton that can is used
 * when loading old decks to convert old objects to an appropriate
 * {@link PageItem}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@Deprecated
@SuppressWarnings("deprecation")
class Card implements Cloneable, Serializable {

    static final long serialVersionUID = 2827360448649571724L;

    protected final void obsolete() {
        throw new UnsupportedOperationException("obsolete class " + getClass().getSimpleName() + " can only be converted to PageItem");
    }

    protected Card() {
        obsolete();
    }

    public Card(String name, Sheet sheet, String sheetPath, int sheetIndex) {
        obsolete();
    }

    @Override
    public String toString() {
        return "obsolete component " + getClass().getName();
    }

    protected String sheetPath;
    protected int sheetIndex;
    protected String name;
    protected Page page;
    protected double x, y;
    protected double snapDx = 0d, snapDy = 0d;

    protected int orientation;

    private void writeObject(ObjectOutputStream out) throws IOException {
        obsolete();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
        StrangeEons.log.info("reading obsolete object");

        name = (String) in.readObject();
        sheetPath = (String) in.readObject();
        sheetIndex = in.readInt();
        x = in.readDouble();
        y = in.readDouble();
        orientation = in.readInt();
        page = (Page) in.readObject();
    }

    public PageItem createCompatiblePageItem() {
        StrangeEons.log.info("converting obsolete object");

        try {
            CardFace item = new CardFace(
                    DeckDeserializationSupport.getShared().findGameComponent(sheetPath, name),
                    sheetPath, sheetIndex
            );
            item.setPage(page);
            item.setLocation(x, y);
            item.setOrientation(orientation);
            return item;
        } catch (IOException e) {
            throw new AssertionError("ioe should not happen here since card already available");
        }
    }
}
