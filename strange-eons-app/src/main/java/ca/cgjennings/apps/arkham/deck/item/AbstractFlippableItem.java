package ca.cgjennings.apps.arkham.deck.item;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * An abstract base class for items that can be turned and flipped.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public abstract class AbstractFlippableItem extends AbstractItem implements FlippablePageItem {

    protected int orientation;
    private static final long serialVersionUID = -9059542117799697527L;

    @Override
    public void setOrientation(int orientation) {
        if (orientation < ORIENT_UPRIGHT || orientation > ORIENT_MIRROR_TURN_RIGHT) {
            throw new IllegalArgumentException("illegal orientation: " + orientation);
        }

        if ((this.orientation & 1) != (orientation & 1)) {
            adjustLocationForReorientation();
        }

        this.orientation = orientation;
    }

    @Override
    public int getOrientation() {
        return orientation;
    }

    @Override
    public final void turnLeft() {
        int mirrorBit = orientation & 4;
        int newOrientation = (orientation + 1) & 3;
        newOrientation |= mirrorBit;
        setOrientation(newOrientation);
    }

    @Override
    public final void turnRight() {
        int mirrorBit = orientation & 4;
        int newOrientation = (orientation - 1) & 3;
        newOrientation |= mirrorBit;
        setOrientation(newOrientation);
    }

    @Override
    public final void flip() {
        setOrientation(orientation ^ 4);
    }

    /**
     * Adjust location of item for its new orientation after it is turned a
     * multiple of 90 degrees. The position is adjusted as if the card were
     * rotated around its center.
     */
    private void adjustLocationForReorientation() {
        Rectangle2D.Double rect = getRectangle();
        setX(rect.x + rect.height / 2d - rect.width / 2d);
        setY(rect.y + rect.width / 2d - rect.height / 2d);
    }

    private static final int ABSTRACT_FLIPPABLE_ITEM_VERSION = 1;

    @Override
    protected void writeImpl(ObjectOutputStream out) throws IOException {
        super.writeImpl(out);

        out.writeInt(ABSTRACT_FLIPPABLE_ITEM_VERSION);
        out.writeInt(orientation);
    }

    @Override
    protected void readImpl(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readImpl(in);

        /* final int version = */ in.readInt();

        orientation = in.readInt();
    }
}
