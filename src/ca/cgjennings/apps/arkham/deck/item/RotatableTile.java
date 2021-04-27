package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 *
 * @author Jennings
 */
public class RotatableTile extends Tile {

    static final long serialVersionUID = 8_293_922_190_302_899_303L;
    private double angle = 0;

    public RotatableTile(String name, String identifier, double dpi) {
        super(name, identifier, dpi);
        setSnapClass(PageItem.SnapClass.SNAP_OVERLAY);
        setClassesSnappedTo(SnapClass.SNAP_SET_NONE);
    }

    @Override
    protected double getUprightWidth() {
        return Math.max(super.getUprightWidth(), super.getUprightHeight());
    }

    @Override
    protected double getUprightHeight() {
        return Math.max(super.getUprightWidth(), super.getUprightHeight());
    }

    @Override
    public void paint(Graphics2D g, RenderTarget target, double renderResolutionHint) {
        double width = getUprightWidth();
        BufferedImage i = renderImage(target, getDPI());

        AffineTransform at;
        if (isMirrored()) {
            at = AffineTransform.getScaleInstance(-72d / getDPI(), 72d / getDPI());
            at.preConcatenate(AffineTransform.getTranslateInstance(
                    getX() + (width - super.getUprightWidth()) / 2d + super.getUprightWidth(),
                    getY() + (width - super.getUprightWidth()) / 2d
            ));
        } else {
            at = AffineTransform.getScaleInstance(72d / getDPI(), 72d / getDPI());
            at.preConcatenate(AffineTransform.getTranslateInstance(
                    getX() + (width - super.getUprightWidth()) / 2d,
                    getY() + (width - super.getUprightWidth()) / 2d
            ));
        }

        double theta = getRotation() + (getOrientation() & 3) * Math.PI / 2d;
        at.preConcatenate(AffineTransform.getRotateInstance(theta, getX() + width / 2d, getY() + width / 2d));
        g.drawImage(i, at, null);
    }

    @Override
    public Shape getOutline() {
        Rectangle2D.Double r = getRectangle();
        AffineTransform at = AffineTransform.getRotateInstance(getRotation(), r.x + r.width / 2d, r.y + r.height / 2d);
        return new Path2D.Double(r, at);
    }

    @Override
    public DragHandle[] getDragHandles() {
        if (dragHandles == null) {
            dragHandles = new DragHandle[]{new RotationHandle(this)};
        }
        return dragHandles;
    }

    @Override
    public PageItem clone() {
        RotatableTile rt = (RotatableTile) super.clone();
        rt.dragHandles = null;
        return rt;
    }

    public double getRotation() {
        return angle;
    }

    public void setRotation(double angle) {
        this.angle = angle;
    }

    private static final int ROTATABLE_TILE_VERSION = 1;

    @Override
    protected void writeImpl(ObjectOutputStream out) throws IOException {
        super.writeImpl(out);

        out.writeInt(ROTATABLE_TILE_VERSION);

        out.writeDouble(getRotation());
    }

    @Override
    protected void readImpl(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readImpl(in);

        int version = in.readInt();

        setRotation(in.readDouble());
        dragHandles = null;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeImpl(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readImpl(in);
    }
}
