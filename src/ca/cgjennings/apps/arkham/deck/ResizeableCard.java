package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.deck.item.PageItem;
import ca.cgjennings.apps.arkham.deck.item.TextBox;
import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * An obsolete kind of deck object. This class is a skeleton that can is used
 * when loading old decks to convert old objects to an appropriate
 * {@link PageItem}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @deprecated This class is retained only for backwards compatibility with very
 * old decks. All objects that can be placed in a deck are now implementations
 * of {@link ca.cgjennings.apps.arkham.deck.item.PageItem}.
 */
@Deprecated
@SuppressWarnings("deprecation")
class ResizeableCard extends Card implements Serializable, Cloneable {

    static final long serialVersionUID = 2827360448649571724L;
    private String text;
    private Color background;
    private double margin = 4;

    public ResizeableCard() {
        obsolete();
    }

    private double width, height; // in points

    private void writeObject(ObjectOutputStream out) throws IOException {
        obsolete();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();

        StrangeEons.log.info("reading obsolete object");

        name = (String) in.readObject();
        text = (String) in.readObject();
        x = in.readDouble();
        y = in.readDouble();
        width = in.readDouble();
        height = in.readDouble();
        orientation = in.readInt();
        page = (Page) in.readObject();

        snapDx = 0d;
        snapDy = 0d;

        if (version >= 2) {
            background = (Color) in.readObject();
        } else {
            background = Color.WHITE;
        }

        margin = 4;
    }

    @Override
    public PageItem createCompatiblePageItem() {

        StrangeEons.log.info("converting obsolete object");

        TextBox item = new TextBox();
        item.setPage(page);
        item.setLocation(x, y);
        item.setSize(width, height);
        item.setFillColor(background);
        item.setOrientation(orientation);
        item.setText(text);
        return item;
    }
}
