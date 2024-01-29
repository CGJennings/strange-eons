package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * A tile with a static outline. This class is used to instantiate tile set
 * entries for tiles with outlines. It provides methods similar to
 * {@link OutlineStyle}, but it does not actually implement that interface, so
 * the outline cannot be configured through the {@link StyleEditor}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class OutlinedTile extends Tile {

    private Color borderColor = Color.BLACK;
    private float borderWidth;
    private DashPattern borderDash = DashPattern.SOLID;
    private LineJoin borderJoin = LineJoin.MITER;
    private LineCap borderCap = LineCap.SQUARE;

    /**
     * Creates a new outlined tile.
     *
     * @param name the user-friendly tile name
     * @param identifier the identifier used to determine the image to display
     * @param ppi the tile image resolution, in pixels per inch
     */
    public OutlinedTile(String name, String identifier, double ppi) {
        super(name, identifier, ppi);
    }

    /**
     * Returns the color used to draw the outline.
     *
     * @return the outline color
     */
    public Color getOutlineColor() {
        return borderColor;
    }

    /**
     * Sets the color used to draw the outline.
     *
     * @param borderColor the outline color
     */
    public void setOutlineColor(Color borderColor) {
        if (borderColor == null) {
            throw new NullPointerException("null borderColor");
        }
        if (!this.borderColor.equals(borderColor)) {
            this.borderColor = borderColor;
            itemChanged();
        }
    }

    /**
     * Returns the width, in points, of the outline.
     *
     * @return the outline line width
     */
    public float getOutlineWidth() {
        return borderWidth;
    }

    /**
     * Sets the color used to draw the outline.
     *
     */
    public void setOutlineWidth(float borderWidth) {
        if (this.borderWidth != borderWidth) {
            this.borderWidth = borderWidth;
            itemChanged();
        }
    }

    /**
     * Returns the dash pattern used to draw the outline.
     *
     * @return the dash pattern for the outline
     */
    public DashPattern getOutlineDashPattern() {
        return borderDash;
    }

    /**
     * Sets the dash pattern used to draw the outline.
     *
     * @param pat the dash pattern type
     */
    public void setOutlineDashPattern(DashPattern pat) {
        if (borderDash != pat) {
            borderDash = pat;
            itemChanged();
        }
    }

    /**
     * Returns the method used to join the line segments that make up the
     * outline.
     *
     * @return the line joining method
     */
    public LineJoin getOutlineJoin() {
        return borderJoin;
    }

    /**
     * Sets the method used to join the line segments that make up the outline.
     *
     */
    public void setOutlineJoin(LineJoin borderJoin) {
        if (borderJoin == null) {
            throw new NullPointerException("join");
        }
        if (this.borderJoin != borderJoin) {
            this.borderJoin = borderJoin;
            itemChanged();
        }
    }

    /**
     * Sets the line cap style used on outline ends.
     *
     * @param cap the line cap type
     */
    public void setOutlineCap(LineCap cap) {
        if (borderCap == null) {
            throw new NullPointerException("cap");
        }
        if (borderCap != cap) {
            this.borderCap = cap;
            itemChanged();
        }
    }

    /**
     * Returns the line cap style used on outline ends.
     *
     * @return the line cap style
     */
    public LineCap getOutlineCap() {
        return borderCap;
    }

    @Override
    public void paint(Graphics2D g, RenderTarget target, double renderResolutionHint) {
        // draw the tile
//		if( opacity > 0f ) {
        super.paint(g, target, renderResolutionHint);
//		}

        if (borderWidth > 0f) {
            Paint p = g.getPaint();
            Stroke s = g.getStroke();
            g.setPaint(borderColor);
            g.setStroke(RenderingAttributeFactory.createStroke(borderWidth, borderCap, borderJoin, borderDash));
            g.draw(getRectangle());
            g.setStroke(s);
            g.setPaint(p);
        }
    }

    private static final int OUTLINED_TILE_VERSION = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(OUTLINED_TILE_VERSION);

        out.writeObject(borderColor);
        out.writeFloat(borderWidth);
        out.writeObject(borderDash);
        out.writeObject(borderJoin);
        out.writeObject(borderCap);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();

        borderColor = (Color) in.readObject();
        borderWidth = in.readFloat();
        borderDash = (DashPattern) in.readObject();
        borderJoin = (LineJoin) in.readObject();
        borderCap = (LineCap) in.readObject();
    }

    private static final long serialVersionUID = 8720203321123654132L;
}
