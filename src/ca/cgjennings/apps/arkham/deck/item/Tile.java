package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.ViewQuality;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EnumSet;
import resources.ResourceKit;

/**
 * The base class for tiles, which are static bitmap graphics that can be placed
 * in a deck.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class Tile extends AbstractRenderedItem {

    protected String identifier;
    private double dpi;
    private String name;
    private boolean allowsOutlineDrawing;

    private transient TileProvider tilePainter;

    private static final SnapClass DEFAULT_SNAPCLASS = SnapClass.SNAP_TILE;
    private static final EnumSet<SnapClass> DEFAULT_SNAPTO = EnumSet.of(
            SnapClass.SNAP_TILE, SnapClass.SNAP_PAGE_GRID
    );

    /**
     * Creates a new tile.
     *
     * @param name the user-friendly tile name
     * @param identifier the identifier used to determine the image to display
     * @param ppi the tile image resolution, in pixels per inch
     */
    public Tile(String name, String identifier, double ppi) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (identifier == null) {
            throw new NullPointerException("identifier");
        }
        setSnapClass(DEFAULT_SNAPCLASS);
        setClassesSnappedTo(DEFAULT_SNAPTO);

        this.name = name;
        this.identifier = identifier;
        this.dpi = ppi;
        allowsOutlineDrawing = false;

        setMipMapCacheEnabled(false);
    }

    /**
     * Sets the user-friendly tile name.
     *
     * @param name the new, non-{@code null} name
     */
    public void setName(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setIdentifier(String identifier) {
        if (identifier == null) {
            throw new NullPointerException("null identifier");
        }
        if (this.identifier == null || !this.identifier.equals(identifier)) {
            this.identifier = identifier;
            cachedTileImage = null;
        }
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setOrientation(int orientation) {
        super.setOrientation(orientation);
        if (tilePainter != null) {
            cachedTileImage = null;
        }
    }

    @Override
    public void clearCachedImages() {
        super.clearCachedImages();
        cachedTileImage = null;
    }

    public void setResolution(double dpi) {
        this.setDPI(dpi);
    }

    public double getResolution() {
        return getDPI();
    }

    @Override
    public void paint(Graphics2D g, RenderTarget target, double renderResolutionHint) {
        if (allowsOutlineDrawing && ViewQuality.get() == ViewQuality.LOW) {
            g.setColor(Color.DARK_GRAY);
            g.setStroke(new BasicStroke(1));
            Rectangle2D.Double r = getRectangle();
            g.draw(r);
            Line2D.Double li = new Line2D.Double(r.x, r.y, r.x + r.width, r.y + r.height);
            g.draw(li);
            li.x1 = li.x2;
            li.x2 = r.x;
            g.draw(li);
        } else {
            BufferedImage i = getOrientedImage(target, getDPI());
            AffineTransform scale = AffineTransform.getScaleInstance(72d / getDPI(), 72d / getDPI());
            scale.preConcatenate(AffineTransform.getTranslateInstance(getX(), getY()));
            g.drawImage(i, scale, null);
        }
    }

    @Override
    protected double getUprightWidth() {
        return renderImage(RenderTarget.PRINT, getDPI()).getWidth() / getDPI() * 72d;
    }

    @Override
    protected double getUprightHeight() {
        return renderImage(RenderTarget.PRINT, getDPI()).getHeight() / getDPI() * 72d;
    }

    public double getDPI() {
        return dpi;
    }

    public void setDPI(double dpi) {
        this.dpi = dpi;
    }

    /**
     * {@inheritDoc}
     *
     * Calls {@link #getImageFromIdentifier}, if required, to generate the tile
     * image. As tiles are fixed in resolution, the other parameters are
     * ignored. The {@link #paint} implementation correctly scales the image
     * when painting.
     */
    @Override
    protected BufferedImage renderImage(RenderTarget target, double resolution) {
        if (cachedTileImage == null) {
            cachedTileImage = getImageFromIdentifier();
        }
        return cachedTileImage;
    }

    @Override
    protected boolean scaleMipMapUpAtHighZoom(RenderTarget target, double resolution) {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Tiles currently disable MIP map caching, but this could change in a
     * future version.
     */
    @Override
    protected boolean isMipMapCacheEnabledByDefault() {
        return false;
    }

    /**
     * Fetch this tile's image using its identifier. The base class uses the
     * identifier as a URL relative to the resources folder. Subclasses may
     * implement other algorithms.
     *
     * @return the image referenced by the tile's identifier
     */
    protected BufferedImage getImageFromIdentifier() {
        if (identifier.startsWith("script:")) {
            if (tilePainter == null) {
                String script = identifier.substring("script:".length());
                ScriptMonkey sm = new ScriptMonkey(script);
                sm.eval(ResourceKit.composeResourceURL(script));
                tilePainter = sm.implement(TileProvider.class);
            }
            return tilePainter.createTileImage(this);
        }
        return ResourceKit.getImageQuietly(identifier);
    }
    private transient BufferedImage cachedTileImage;

    /**
     * Returns {@code true} if the tile can be painted as a simple outline
     * when drawing a low quality preview.
     *
     * @return {@code true} if the tile can be draw as an outline
     */
    public boolean isFastOutlineAllowed() {
        return allowsOutlineDrawing;
    }

    /**
     * Sets whether the tile can be painted as a simple outline when drawing at
     * low quality. Typically, only items in the TILE tile class will set this
     * to {@code true}.
     *
     * @param allowsOutlineDrawing {@code true} to allow drawing as an
     * outline
     */
    public void setFastOutlineAllowed(boolean allowsOutlineDrawing) {
        this.allowsOutlineDrawing = allowsOutlineDrawing;
    }

    private static final int TILE_VERSION = 1;

    @Override
    protected void writeImpl(ObjectOutputStream out) throws IOException {
        super.writeImpl(out);

        out.writeInt(TILE_VERSION);

        out.writeObject(getIdentifier());
        out.writeObject(getName());
        out.writeDouble(getDPI());
        out.writeBoolean(isFastOutlineAllowed());
    }

    @Override
    protected void readImpl(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readImpl(in);

        int version = in.readInt();

        setIdentifier((String) in.readObject());
        setName((String) in.readObject());
        setDPI(in.readDouble());
        setFastOutlineAllowed(in.readBoolean());

        setMipMapCacheEnabled(false);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeImpl(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readImpl(in);
        if (identifier == null) {
            StrangeEons.log.warning("null tile identifier");
        }
    }
    private static final long serialVersionUID = 8_472_020_379_348_654_470L;
}
