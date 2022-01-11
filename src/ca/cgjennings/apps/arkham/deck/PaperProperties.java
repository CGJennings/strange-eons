package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.sheet.PrintDimensions;
import ca.cgjennings.graphics.filters.ClearFilter;
import ca.cgjennings.ui.IconProvider;
import gamedata.Game;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import javax.print.attribute.standard.MediaSize;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import resources.Language;
import static resources.Language.string;

/**
 * {@code PaperProperties} are immutable objects that describe the properties of
 * the paper used in a deck.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class PaperProperties implements Comparable<PaperProperties>, IconProvider, Serializable {

    static final long serialVersionUID = 5_085_508_097_117_626_622L;

    private static final double DEF_GRID = 72d * 0.393701d;
    private static final double DEF_HEIGHT = 11d * 72d;
    private static final double DEF_MARGIN = DEF_GRID * 2d;
    private static final double DEF_WIDTH = 8.5d * 72d;

    private String name = "Letter";
    private String trueName = name;
    private double pageWidth = DEF_WIDTH;
    private double pageHeight = DEF_HEIGHT;
    private double gridSize = DEF_GRID; // 0.5cm
    private double margin = DEF_MARGIN; // 2cm
    private boolean isPortrait = true;
    private boolean isPhysical = true;
    private String gameCode = Game.ALL_GAMES_CODE;

    private transient PrintDimensions printDimen;

    /**
     * A constant that indicates the portrait orientation (in which the width is
     * the shortest dimension).
     */
    public static final boolean PORTRAIT = true;
    /**
     * A constant that indicates the landscape orientation (in which the width
     * is the longest dimension).
     */
    public static final boolean LANDSCAPE = false;
    /**
     * The maximum size for a paper dimension, in points. This is the same value
     * as {@link Deck#MAX_PAPER_SIZE}.
     */
    public static final double MAX_PAPER_SIZE = Deck.MAX_PAPER_SIZE;

    /**
     * Create a new paper properties object using default dimensions that match
     * the "North American Letter" paper size.
     */
    public PaperProperties() {
    }

    /**
     * Create a new paper property description. Note that the width and height
     * can be given in any order. The actual width and height dimensions will be
     * determined automatically from the orientation. The paper name is
     * localized automatically if the name begins with '@'. The localized name
     * is determined as if by {@code Language.string( name.substring(1) )}.
     *
     * @param name the name to use for the paper type
     * @param width paper width in points
     * @param height paper height in points
     * @param orientation one of {@link #LANDSCAPE} or {@link #PORTRAIT}
     * @throws NullPointerException if the name is {@code null}
     */
    public PaperProperties(String name, double width, double height, boolean orientation) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        name = name.trim();
        trueName = name;
        if (!name.isEmpty() && name.charAt(0) == '@') {
            name = string(name.substring(1));
        }
        this.name = name;
        if (width > height) {
            double temp = width;
            width = height;
            height = temp;
        }
        pageWidth = width;
        pageHeight = height;
        isPortrait = orientation;
    }

    /**
     * Create a new paper property description. Note that the width and height
     * can be given in any order. The actual width and height dimensions will be
     * determined automatically from the orientation.
     *
     * @param name the name to use for the paper type
     * @param width paper width in points
     * @param height paper height in points
     * @param orientation one of {@link #LANDSCAPE} or {@link #PORTRAIT}
     * @param margin the size of the print edge margin, in points
     * @param gridSeparation the size of the snapping grid, in points
     */
    public PaperProperties(String name, double width, double height, boolean orientation, double margin, double gridSeparation) {
        this(name, width, height, orientation);
        this.margin = margin;
        this.gridSize = gridSeparation;
    }

    /**
     * Create a new paper property description. Note that the width and height
     * can be given in any order. The actual width and height dimensions will be
     * determined automatically from the orientation.
     *
     * @param name the name to use for the paper type
     * @param width paper width in points
     * @param height paper height in points
     * @param orientation one of {@link #LANDSCAPE} or {@link #PORTRAIT}
     * @param margin the size of the print edge margin, in points
     * @param gridSeparation the size of the snapping grid, in points
     * @param isPhysical {@code true} if this paper represents physical media,
     * or {@code false} if it represents a virtual paper size such as the size
     * of an expansion board
     * @param game the game that this paper applies to, or {@code null} for all
     * games
     */
    public PaperProperties(String name, double width, double height, boolean orientation, double margin, double gridSeparation, boolean isPhysical, Game game) {
        this(name, width, height, orientation, margin, gridSeparation);
        this.isPhysical = isPhysical;
        gameCode = game == null ? Game.ALL_GAMES_CODE : game.getCode();
    }

    /**
     * Creates a paper properties object from a print media description.
     *
     * @param media the media description to
     * @param orientation one of {@link #LANDSCAPE} or {@link #PORTRAIT}
     */
    public PaperProperties(MediaSize media, boolean orientation) {
        this(
                media.getMediaSizeName() == null ? "<Anonymous>" : media.getMediaSizeName().toString(),
                72d * media.getX(MediaSize.INCH),
                72d * media.getY(MediaSize.INCH),
                orientation
        );
    }

    /**
     * Creates a paper properties object from a print media description.
     *
     * @param media the media description to
     * @param orientation one of {@link #LANDSCAPE} or {@link #PORTRAIT}
     * @param margin the size of the print edge margin, in points
     * @param gridSeparation the size of the snapping grid, in points
     */
    public PaperProperties(MediaSize media, boolean orientation, double margin, double gridSeparation) {
        this(
                media.getMediaSizeName() == null ? "<Anonymous>" : media.getMediaSizeName().toString(),
                72d * media.getX(MediaSize.INCH),
                72d * media.getY(MediaSize.INCH),
                orientation,
                margin, gridSeparation
        );
    }

    /**
     * Returns the name for this paper type.
     *
     * @return the paper's base name
     * @see #toString()
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the internal name for this paper type. The internal name is the
     * name that the paper was created with. This will be the same value as
     * {@link #getName()} unless the paper was created with a localizable
     * {@code @}-name.
     *
     * @return the name that the paper was created with
     * @since 3.0
     */
    public String getInternalName() {
        return trueName;
    }

    /**
     * Returns the page width in points (1/72 inch).
     *
     * @return the page width, corrected for orientation
     */
    public double getPageWidth() {
        if (isPortraitOrientation()) {
            return pageWidth;
        } else {
            return pageHeight;
        }
    }

    /**
     * Returns the page height in points (1/72 inch).
     *
     * @return the page height, corrected for orientation
     */
    public double getPageHeight() {
        if (isPortraitOrientation()) {
            return pageHeight;
        } else {
            return pageWidth;
        }
    }

    /**
     * Returns the page dimensions as an immutable {@link PrintDimensions}
     * instance. The dimensions will reflect the page's orientation.
     *
     * @return the page dimensions, corrected for orientation
     */
    public PrintDimensions getPrintDimensions() {
        synchronized (this) {
            if (printDimen == null) {
                printDimen = new PrintDimensions(getPageWidth(), getPageHeight());
            }
        }
        return printDimen;
    }

    /**
     * Returns the distance between grid lines, in points (1/72 inch).
     *
     * @return the snapping grid size
     */
    public double getGridSeparation() {
        return gridSize;
    }

    /**
     * Returns the margin around page edges, in points (1/72 inch).
     *
     * @return the edge margin
     */
    public double getMargin() {
        return margin;
    }

    /**
     * Returns {@code true} if this page uses portrait orientation. (The
     * definition of {@link #PORTRAIT} is such that the value returned from this
     * method and the value returned from {@link #getOrientation} are
     * identical.)
     *
     * @return {@code true} if orientation is {@link #PORTRAIT}, otherwise
     * {@code false}
     * @see #getOrientation()
     */
    public boolean isPortraitOrientation() {
        return isPortrait;
    }

    /**
     * Returns {@link #PORTRAIT} or {@link #LANDSCAPE} depending on the
     * orientation. (This returns exactly the same value as
     * {@link #isPortraitOrientation()}).
     *
     * @return the orientation of the paper
     * @see #isPortraitOrientation()
     */
    public boolean getOrientation() {
        return isPortrait;
    }

    /**
     * Returns {@code true} if this describes a physical paper type; that is, if
     * it matches real-world paper sizes that might fit in a particular printer
     * model.
     *
     * @return {@code true} if this is a physical paper type
     */
    public boolean isPhysical() {
        return isPhysical;
    }

    /**
     * Returns the game code for the {@link Game} that this paper type is
     * associated with. If the paper type is not associated with a game, this
     * will be {@link Game#ALL_GAMES_CODE}.
     *
     * @return the non-{@code null} game code for the game this paper type is
     * associated with
     */
    public String getGameCode() {
        return gameCode == null ? Game.ALL_GAMES_CODE : gameCode;
    }

    /**
     * Returns a {@code PaperProperties} with the same dimensions as this paper
     * but with the requested orientation. If the orientation matches this
     * paper's orientation, this instance is returned. Otherwise, a new instance
     * is returned with the requested orientation.
     *
     * @param orientation one of {@link #LANDSCAPE} or {@link #PORTRAIT}
     * @return a paper properties with these dimensions and the requested
     * orientation
     */
    public PaperProperties deriveOrientation(boolean orientation) {
        if (isPortraitOrientation() == orientation) {
            return this;
        }
        PaperProperties turned = new PaperProperties(
                this.trueName, this.pageWidth, this.pageHeight, orientation, this.margin, this.gridSize
        );
        turned.isPhysical = isPhysical;
        turned.gameCode = gameCode;
        return turned;
    }

    /**
     * Create a {@code PageFormat} object that is compatible with this
     * {@code PaperProperties}. {@code PageFormat}s are used by the printing
     * system to describe physical pages. If {@code applyMargins} is
     * {@code true}, then the imageable area of the resulting {@code PageFormat}
     * will be reduced to account for the margin. Otherwise, the imageable area
     * will be set to the entire page.
     *
     * @param applyMargins if {@code true}, use the margin to set the imageable
     * area
     * @return a page format that represents the same physical paper size and
     * orientation as this instance
     */
    public PageFormat createCompatiblePageFormat(boolean applyMargins) {
        PageFormat pf = new PageFormat();
        Paper p = new Paper();

        p.setSize(pageWidth, pageHeight);
        if (applyMargins) {
            p.setImageableArea(getMargin(), getMargin(), pageWidth - getMargin() * 2d, pageHeight - getMargin() * 2d);
        } else {
            p.setImageableArea(0d, 0d, pageWidth, pageHeight);
        }

        pf.setPaper(p);
        pf.setOrientation(isPortraitOrientation() ? PageFormat.PORTRAIT : PageFormat.LANDSCAPE);
        return pf;
    }

    /**
     * Create an image compatible with the dimensions of this
     * {@code PaperProperties}. The size of the image will be determined by the
     * page size and the resolution, specified in pixels per inch. The new image
     * will be filled with solid white.
     *
     * @param ppi the resolution of resulting image, in pixels per inch
     * @return an image the same size as this paper size at the given resolution
     * @throws IllegalArgumentException if {@code ppi} is not a positive number
     */
    public BufferedImage createCompatibleImage(double ppi) {
        if (ppi <= 0d) {
            throw new IllegalArgumentException("ppi <= 0: " + ppi);
        }
        int width = (int) Math.round(getPageWidth() / 72d * ppi);
        int height = (int) Math.round(getPageHeight() / 72d * ppi);
        BufferedImage i = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        new ClearFilter(0xffff_ffff).filter(i, i);
        return i;
    }

    /**
     * Returns a localized description of the paper type.
     *
     * @return a human-friendly description of the paper type
     */
    @Override
    public String toString() {
        return string("de-l-paper-format", name, isPortrait ? string("de-l-paper-portrait") : string("de-l-paper-landscape"));
    }

    /**
     * Returns a string that describes this paper properties at a low level for
     * debugging purposes.
     *
     * @return debug string
     */
    String toDebugString() {
        return "PaperProperties{ " + trueName + ", " + pageWidth + ", "
                + pageHeight + ", grid=" + gridSize + ", margin=" + margin
                + ", portrait=" + isPortrait + ", physical=" + isPhysical
                + ", game=" + gameCode + '}';
    }

    /**
     * Returns {@code true} if another properties instance has the same
     * dimensions, orientation, grid size, margin, and name as this instance.
     *
     * @param that the paper properties to compare this with
     * @return {@code true} if the paper properties are equal to this
     */
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that != null && that instanceof PaperProperties) {
            PaperProperties rhs = (PaperProperties) that;
            return pageWidth == rhs.pageWidth && pageHeight == rhs.pageHeight
                    && gridSize == rhs.gridSize && margin == rhs.margin
                    && isPortrait == rhs.isPortrait && isPhysical == rhs.isPhysical
                    && (name.equals(rhs.name) || trueName.equals(rhs.trueName));
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            hash = 7;
            hash = 61 * hash + trueName.hashCode();
            hash = 61 * hash + (int) (Double.doubleToLongBits(this.pageWidth) ^ (Double.doubleToLongBits(this.pageWidth) >>> 32));
            hash = 61 * hash + (int) (Double.doubleToLongBits(this.pageHeight) ^ (Double.doubleToLongBits(this.pageHeight) >>> 32));
            hash = 61 * hash + (int) (Double.doubleToLongBits(this.gridSize) ^ (Double.doubleToLongBits(this.gridSize) >>> 32));
            hash = 61 * hash + (int) (Double.doubleToLongBits(this.margin) ^ (Double.doubleToLongBits(this.margin) >>> 32));
            hash = 61 * hash + (this.isPortrait ? 1 : 0);
        }
        return hash;
    }
    private int hash;

    /**
     * The natural ordering of paper properties instances sorts them by their
     * string representation using the interface language's collator.
     * <p>
     * <b>Note:</b> This ordering is incompatible with the definition of
     * {@link #equals}.
     *
     * @param that the properties to compare this to
     * @return for the {@link #toString()} value of this and that object:<br>
     * the value 0 if the values are equal;<br>
     * a value less than 0 if this is "less" than that;<br>
     * and a value greater than 0 if this is "greater" than that
     */
    @Override
    public int compareTo(PaperProperties that) {
        if (that == null || !(that instanceof PaperProperties)) {
            throw new IllegalArgumentException("that");
        }

        return Language.getInterface().getCollator().compare(toString(), that.toString());
    }

    @Override
    public synchronized Icon getIcon() {
        if (icon == null) {
            double dw = getPageWidth() / 72d + 0.5d;
            double dh = getPageHeight() / 72d + 0.5d;
            if (dw > 17d || dh > 17d) {
                final double hscale = 17d / dw;
                final double vscale = 17d / dh;
                final double scale = Math.min(hscale, vscale);
                dw *= scale;
                dh *= scale;
            }
            int w = (int) (dw + 0.5d);
            int h = (int) (dh + 0.5d);

            BufferedImage bi = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bi.createGraphics();
            try {
                String code = getGameCode();
                if (!code.equals(Game.ALL_GAMES_CODE)) {
                    Game game = Game.get(code);
                    if (game != null) {
                        game.getIcon().paintIcon(null, g, 0, 0);
                    }
                }
                int xo = (18 - w) / 2;
                int yo = (18 - h) / 2;
                g.setPaint(new Color(0x88DD_DDDD, true));
                g.fillRect(xo, yo, w, h);
                g.setPaint(Color.GRAY);
                g.drawRect(xo, yo, w, h);
            } finally {
                g.dispose();
            }
            icon = new ImageIcon(bi);
        }
        return icon;
    }
    private transient Icon icon;

    private static void checkPaperDimension(double d) {
        if (d != d || d <= 0 || d > ca.cgjennings.apps.arkham.deck.Deck.MAX_PAPER_SIZE) {
            throw new IllegalArgumentException("invalid paper measure: " + d);
        }
    }

    private static final int CURRENT_VERSION = 4;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(CURRENT_VERSION);

        out.writeObject(trueName);
        out.writeObject(name);
        out.writeDouble(pageWidth);
        out.writeDouble(pageHeight);
        out.writeBoolean(isPortrait);
        out.writeDouble(gridSize);
        out.writeDouble(margin);
        out.writeBoolean(isPhysical);
        out.writeObject(gameCode);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();

        trueName = (String) in.readObject();
        if (version >= 4) {
            name = (String) in.readObject();
            if (!trueName.isEmpty() && trueName.charAt(0) == '@') {
                String key = trueName.substring(1);
                if (Language.getInterface().isKeyDefined(key)) {
                    name = string(key);
                } // else keep name we just read in
            }
        } else {
            trueName = trueName.trim(); // names are trimmed in constructor now
            name = trueName;
        }
        name = name.trim();
        pageWidth = in.readDouble();
        pageHeight = in.readDouble();
        isPortrait = in.readBoolean();
        gridSize = in.readDouble();
        margin = in.readDouble();

        if (version >= 2) {
            isPhysical = in.readBoolean();
        } else {
            isPhysical = true;
        }

        if (version >= 3) {
            gameCode = (String) in.readObject();
        } else {
            gameCode = Game.ALL_GAMES_CODE;
        }
    }
}
