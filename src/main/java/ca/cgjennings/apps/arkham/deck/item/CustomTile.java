package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.deck.DeckDeserializationSupport;
import ca.cgjennings.apps.arkham.deck.Page;
import ca.cgjennings.apps.arkham.dialog.InsertImageDialog;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.layout.MarkupRenderer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.Timer;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;
import resources.StrangeImage;

/**
 * Resizable, customizable tiles that can be placed in a deck.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class CustomTile extends Tile implements EditablePageItem, SizablePageItem, UserBleedMarginStyle, OutlineStyle, OpacityStyle {

    private float opacity = 1f;
    private boolean addCropMarks = false;
    private double bleedMargin = 0;

    private Color borderColor = Color.BLACK;
    private float borderWidth;
    private DashPattern borderDash = DashPattern.SOLID;
    private int borderJoin = BasicStroke.JOIN_ROUND;
    private LineCap borderCap = LineCap.ROUND;

    private transient Shape borderShape;

    public CustomTile(String identifier, double dpi) {
        super(string("de-l-class-custom"), identifier, dpi);
    }

    @Override
    public Icon getThumbnailIcon() {
        if (sharedIcon == null) {
            sharedIcon = ResourceKit.getIcon("deck/tile.png");
        }
        return sharedIcon;
    }
    private static transient Icon sharedIcon = null;

    @Override
    public void setIdentifier(String identifier) {
        if (!identifier.equals(this.identifier)) {
            super.setIdentifier(identifier);
            lastImageReturnedFromGetImage = null;
            beginMonitoringForImageChanges();
        }
    }

    /**
     * The placeholder identifier that acts as the default for new custom tiles.
     */
    public static final String PLACEHOLDER_IDENTIFIER = "res://board/custom-tile.png";

    /**
     * Fetch this tile's image using its identifier. Locates the image as a file
     * or URL using the same syntax as
     * {@link ca.cgjennings.layout.GraphicStyleFactory}.
     *
     * @return an image for this identifier, possibly a "broken image"
     * placeholder image
     */
    @Override
    protected BufferedImage getImageFromIdentifier() {
        synchronized (this) {
            String path = getIdentifier();

            boolean isDefault = false;
            if (path == null || path.isEmpty()) {
                path = PLACEHOLDER_IDENTIFIER;
                isDefault = true;
            }

            BufferedImage image = StrangeImage.get(path).asBufferedImage();

            if (isDefault) {
                MarkupRenderer r = new MarkupRenderer();
                r.setMarkupText("<middle><center><size 36><colour ffffff><b>" + string("de-custom-tile-content"));
                Graphics2D g = image.createGraphics();
                try {
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                    r.draw(g, new Rectangle2D.Double(4d, 4d, image.getWidth() - 8d, image.getHeight() - 8d));
                } finally {
                    g.dispose();
                }
            }

            if (getOpacity() < 1f) {
                image = ImageUtilities.alphaComposite(image, getOpacity());
            }

            return image;
        }
    }

    @Override
    public void beginEditing() {
        Page p = getPage();
        InsertImageDialog dialog = new InsertImageDialog(StrangeEons.getWindow(), true, true);
        dialog.setLocationRelativeTo(p.getView());
        dialog.setTile(this);
        if (dialog.showDialog()) {
            p.getDeck().markUnsavedChanges();
            p.getView().repaint();
        }
    }

    @Override
    public DragHandle[] getDragHandles() {
        if (dragHandles == null) {
            dragHandles = new DragHandle[]{
                new ResizeHandle(this, ResizeHandle.CORNER_NW, true),
                new ResizeHandle(this, ResizeHandle.CORNER_NE, true),
                new ResizeHandle(this, ResizeHandle.CORNER_SW, true),
                new ResizeHandle(this, ResizeHandle.CORNER_SE, true)
            };
        }
        return dragHandles;
    }

    @Override
    public void setBleedMarginMarked(boolean showCropMarks) {
        if (showCropMarks != addCropMarks) {
            addCropMarks = showCropMarks;
            itemChanged();
        }
    }

    @Override
    public boolean isBleedMarginMarked() {
        return addCropMarks;
    }

    @Override
    public double getBleedMargin() {
        return bleedMargin;
    }

    @Override
    public void setBleedMargin(double bleedMargin) {
        if (bleedMargin != this.bleedMargin) {
            this.bleedMargin = bleedMargin;
            itemChanged();
        }
    }

    @Override
    public void setSize(double width, double height) {
        if (width < 0.01) {
            width = 0.01;
        }
        if (height < 0.01) {
            height = 0.01;
        }
        BufferedImage im = getImageFromIdentifier();
        double dpi = getDPI();
        double w = im.getWidth() / dpi * 72d;
        double h = im.getHeight() / dpi * 72d;
        // guess if the user only changed width or height, and if so make sure
        // we use that dimension for our conversion
        if (Math.abs(w - width) < 0.000001) {
            dpi = im.getHeight() / (height / 72d);
        } else {
            dpi = im.getWidth() / (width / 72d);
        }
        setDPI(dpi);
        clearCachedImages();

        itemChanged();
    }

    /**
     * Returns the opacity (alpha) for the tile.
     *
     * @return the opacity
     */
    @Override
    public float getOpacity() {
        return opacity;
    }

    /**
     * Set the opacity (alpha) of the tile.
     *
     * @param opacity the opacity to set
     */
    @Override
    public void setOpacity(float opacity) {
        if (this.opacity != opacity) {
            this.opacity = opacity;
            clearCachedImages();
            itemChanged();
        }
    }

    @Override
    public Color getOutlineColor() {
        return borderColor;
    }

    @Override
    public void setOutlineColor(Color borderColor) {
        if (borderColor == null) {
            throw new NullPointerException("null borderColor");
        }
        if (!this.borderColor.equals(borderColor)) {
            this.borderColor = borderColor;
            itemChanged();
        }
    }

    @Override
    public float getOutlineWidth() {
        return borderWidth;
    }

    @Override
    public void setOutlineWidth(float borderWidth) {
        if (this.borderWidth != borderWidth) {
            this.borderWidth = borderWidth;
            borderShape = null;
            itemChanged();
        }
    }

    @Override
    public DashPattern getOutlineDashPattern() {
        return borderDash;
    }

    @Override
    public void setOutlineDashPattern(DashPattern pat) {
        if (borderDash != pat) {
            borderDash = pat;
            borderShape = null;
            itemChanged();
        }
    }

    @Override
    public LineJoin getOutlineJoin() {
        return LineJoin.fromInt(borderJoin);
    }

    @Override
    public void setOutlineJoin(LineJoin join) {
        int joinInt = join.toInt();
        if (this.borderJoin != joinInt) {
            this.borderJoin = joinInt;
            borderShape = null;
            itemChanged();
        }
    }

    @Override
    public void setOutlineCap(LineCap cap) {
        if (borderCap != cap) {
            this.borderCap = cap;
            borderShape = null;
            itemChanged();
        }
    }

    @Override
    public LineCap getOutlineCap() {
        return borderCap;
    }

    @Override
    public void setX(double x) {
        super.setX(x);
        borderShape = null;
    }

    @Override
    public void setY(double y) {
        super.setY(y);
        borderShape = null;
    }

    @Override
    public void paint(Graphics2D g, RenderTarget target, double renderResolutionHint) {
        // draw the tile
        if (opacity > 0f) {
            super.paint(g, target, renderResolutionHint);
        }

        if (borderWidth > 0f) {
            Paint p = g.getPaint();
            Stroke s = g.getStroke();
            g.setPaint(borderColor);
            g.setStroke(RenderingAttributeFactory.createOutlineStroke(this));
            g.draw(getRectangle());
            g.setStroke(s);
            g.setPaint(p);
        }
    }

    @Override
    public void clearCachedImages() {
        borderShape = null;
        super.clearCachedImages();
    }

    private boolean checkImageForChanges() {
        BufferedImage im = getImageFromIdentifier();
        boolean changed = (lastImageReturnedFromGetImage != null) && (lastImageReturnedFromGetImage != im);
        if (changed) {
            clearCachedImages();
            itemChanged();
        }
        lastImageReturnedFromGetImage = im;
        return changed;
    }
    private BufferedImage lastImageReturnedFromGetImage;

    private void beginMonitoringForImageChanges() {
        synchronized (changeMonitorLock) {
            if (changeMonitorSet == null) {
                changeMonitorSet = new HashSet<>();

                int period = Settings.getShared().getInt("file-monitoring-period");
                final Timer timer = new Timer(period, (ActionEvent e) -> {
                    synchronized (changeMonitorLock) {
                        SoftReference<CustomTile> killable = null;
                        for (SoftReference<CustomTile> ref : changeMonitorSet) {
                            CustomTile tile = ref.get();
                            if (tile != null) {
                                checkImageForChanges();
                            } else {
                                killable = ref;
                            }
                        }
                        if (killable != null) {
                            changeMonitorSet.remove(killable);
                        }
                        if (changeMonitorSet.isEmpty()) {
                            ((Timer) e.getSource()).stop();
                            changeMonitorSet = null;
                        }
                    }
                });
                timer.start();
            }

            changeMonitorSet.add(new SoftReference<>(this));
        }
    }
    /**
     * The set of active custom tiles that need to be checked to see if their
     * image changes.
     */
    private static Set<SoftReference<CustomTile>> changeMonitorSet;
    private static final Object changeMonitorLock = new Object();
    private static final int CUSTOM_TILE_VERSION = 5;

    @Override
    protected void writeImpl(ObjectOutputStream out) throws IOException {
        super.writeImpl(out);

        out.writeInt(CUSTOM_TILE_VERSION);

        out.writeFloat(getOpacity());
        out.writeBoolean(addCropMarks);
        out.writeDouble(bleedMargin);

        out.writeObject(borderColor);
        out.writeFloat(borderWidth);
        out.writeObject(borderDash);
        out.writeInt(borderJoin);
        out.writeObject(borderCap);
    }

    @Override
    protected void readImpl(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readImpl(in);
        int version = in.readInt();

        if (version >= 2) {
            opacity = in.readFloat();
        } else {
            opacity = 1f;
        }

        if (version >= 3) {
            addCropMarks = in.readBoolean();
            bleedMargin = in.readDouble();
        } else {
            addCropMarks = false;
            bleedMargin = 0d;
        }

        if (version >= 4) {
            borderColor = (Color) in.readObject();
            borderWidth = in.readFloat();
            borderDash = (DashPattern) in.readObject();
            borderJoin = in.readInt();
        } else {
            borderColor = Color.BLACK;
            borderWidth = 0f;
            borderDash = DashPattern.SOLID;
            borderJoin = BasicStroke.JOIN_ROUND;
        }

        if (version >= 5) {
            borderCap = (LineCap) in.readObject();
        } else {
            borderCap = (borderJoin == BasicStroke.JOIN_ROUND ? LineCap.ROUND : LineCap.SQUARE);
        }

        // see if we can read the image in, and if not try to find a replacement
        String path = getIdentifier();
        if (path != null && path.length() > 0 && !StrangeImage.exists(path)) {
            try {
                URL url = StrangeImage.identifierToURL(path);
                File oldFile = new File(url.toURI());
                File newFile = new File(
                        DeckDeserializationSupport.getShared().getDefaultFallbackFolder(),
                        oldFile.getName()
                );
                path = newFile.getAbsolutePath();
                if (StrangeImage.exists(path)) {
                    setIdentifier(path);
                }
            } catch (Exception e) {
                // couldn't create URL, or not a file://
                // we can't continue with the replacement, so do nothing
                // user will get a "broken image" and can fix it manually
            }
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeImpl(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readImpl(in);
    }
    private static final long serialVersionUID = 4720203795348654470L;
}
