package gamedata;

//import ca.cgjennings.apps.arkham.deck.DeckEditor;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.deck.DeckEditor;
import ca.cgjennings.apps.arkham.deck.item.DashPattern;
import ca.cgjennings.apps.arkham.deck.item.LineCap;
import ca.cgjennings.apps.arkham.deck.item.LineJoin;
import ca.cgjennings.apps.arkham.deck.item.OutlinedTile;
import ca.cgjennings.apps.arkham.deck.item.PageItem;
import ca.cgjennings.apps.arkham.deck.item.RotatableTile;
import ca.cgjennings.apps.arkham.deck.item.Tile;
import ca.cgjennings.io.EscapedLineReader;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import resources.ResourceKit;
import resources.Settings;

/**
 * A database of available tile set resources.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class TileSet {

    private TileSet() {
    }

    private static LinkedHashSet<String> sets = new LinkedHashSet<>();
    private static LinkedHashSet<Entry> entries = new LinkedHashSet<>();

    /**
     * Adds a tile set resource to the list of tile set files. If the tile set
     * has not been added previously, its entries will be parsed and added to
     * the available tiles.
     *
     * @param resource the location of the tile set resource
     */
    public static void add(String resource) {
        if (resource == null) {
            throw new NullPointerException("resource");
        }
        Lock.test();
        if (sets.add(resource)) {
            try {
                StrangeEons.log.log(Level.INFO, "Parsing tile set {0}", resource);
                Parser p = new Parser(resource, true);
                Entry en = p.next();
                while (en != null) {
                    entries.add(en);
                    en = p.next();
                }
            } catch (IOException e) {
                StrangeEons.log.log(Level.SEVERE, "failed to load tile set " + resource, e);
            }
        }
    }

    /**
     * Returns the names of all of the resource files that have been added by
     * calling {@link #add}.
     *
     * @return all added tile set resources
     */
    public static String[] getTileSets() {
        return sets.toArray(new String[sets.size()]);
    }

    /**
     * Returns all entries from all added tile sets as an immutable set.
     *
     * @return the set of added tile set entries
     */
    public static Set<Entry> getTileSetEntries() {
        return Collections.unmodifiableSet(entries);
    }

    /**
     * An enumeration of the possible class values for a tile's tile set entry.
     */
    public static enum TileClass {
        /**
         * A tile with the TILE snap class.
         */
        TILE,
        /**
         * A tile with the INLAY snap class.
         */
        INLAY,
        /**
         * A tile with the OVERLAY snap class.
         */
        OVERLAY,
        /**
         * A tile with the OVERLAY snap class that will allow free rotation.
         */
        SPINNABLE
    }

    /**
     * An enumeration of the general categories that a {@link PageItem} can fall
     * into.
     */
    public static enum Category {
        /**
         * This special category is reserved for particular {@link PageItem}
         * subclasses that add objects like text boxes and geometric shapes to a
         * deck.
         */
        TOOLS,
        /**
         * This special category is reserved for the faces of game components
         * that have been added to a deck.
         */
        FACES,
        /**
         * This category is generally populated with large rectangular image
         * tiles designed to be placed side-by-side to quickly build up
         * backdrops, maps, and so forth.
         */
        TILES,
        /**
         * This category is generally populated with smaller, often
         * non-rectangular image tiles that are designed to placed overtop
         * {@link #TILES} to customize the look of a board.
         */
        DECORATIONS,
        /**
         * This category is generally populated with tiles that represent small
         * to medium-sized objects with either a game-related function or a
         * usability-related function. Where tiles and decorations serve
         * primarily an aesthetic function, the positioning of board bits either
         * affects gameplay or else helps to improve gameplay, for example, by
         * organizing parts of the board into logical groups.
         */
        BOARD_BITS,
        /**
         * This is a catch-all category provided for plug-in authors to use when
         * no other category applies.
         */
        OTHER
    }

    /**
     * An entry in a tile set; that is, a description of a single tile from a
     * tile set file. Tile set entries are used by the {@link DeckEditor} to
     * create the prototype {@link Tile} objects that the user drags onto deck
     * pages.
     */
    public static final class Entry {

        private String tileName;
        private String name;
        private String resource;
        private String credit;
        private double resolution = 150d;
        private Color outlineColor = Color.BLACK;
        private float outlineWidth = 0f;
        private DashPattern outlineDash = DashPattern.SOLID;
        private LineJoin outlineJoin = LineJoin.MITER;
        private LineCap outlineCap = LineCap.SQUARE;
        private Category cat = Category.OTHER;
        private TileClass tileClass = TileClass.TILE;
        private String game = Game.ALL_GAMES_CODE;
        private int line;
        private Map<String, String> clientProperties;
        private PageItem proto;

        private Entry() {
        }

        /**
         * Returns the line in the tile set file at which this entry begins.
         *
         * @return the line number of the tile set entry
         */
        public int getLine() {
            return line;
        }

        /**
         * Returns the original tile name for the tile. If the tile name is
         * localized, this is the key that was used to localized the tile name.
         * Otherwise, this will be the same as {@link #getName()}.
         *
         * @return the original tile name, as listed on the first line of the
         * tile set entry
         */
        public String getTileName() {
            return tileName;
        }

        /**
         * Returns the name of the tile. If the tile name starts with
         * {@code @} in the tile set file, this name will already be
         * converted into its localized form.
         *
         * @return the (possibly localized) name of the tile
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the image resource that contains the tile graphics.
         *
         * @return an image resource identifier
         * @see ResourceKit#getImage(java.lang.String)
         */
        public String getImageResource() {
            return resource;
        }

        /**
         * Returns the color used to draw the outline.
         *
         * @return the outline color
         */
        public Color getOutlineColor() {
            return outlineColor;
        }

        /**
         * Returns the width, in points, of the outline.
         *
         * @return the outline line width
         */
        public float getOutlineWidth() {
            return outlineWidth;
        }

        /**
         * Returns the dash pattern used to draw the outline.
         *
         * @return the dash pattern for the outline
         */
        public DashPattern getOutlineDashPattern() {
            return outlineDash;
        }

        /**
         * Returns the line cap style used on outline ends.
         *
         * @return the line cap style
         */
        public LineCap getOutlineCap() {
            return outlineCap;
        }

        /**
         * Returns the method used to join the line segments that make up the
         * outline.
         *
         * @return the line joining method
         */
        public LineJoin getOutlineJoin() {
            return outlineJoin;
        }

        /**
         * Returns the credits for the tile, or {@code null} if the tile
         * doesn't specify any credits.
         *
         * @return the credits for the tile design
         */
        public String getCredit() {
            return credit;
        }

        /**
         * Returns the resolution of the tile, in pixels per inch.
         *
         * @return the tile image resolution; this, combined with the dimensions
         * of the tile image, will determine the physical size of the tile
         */
        public double getResolution() {
            return resolution;
        }

        /**
         * Returns the tile class of the tile. This determines the tile's
         * default snap class, along with other properties.
         *
         * @return the tile class, as determined by the entry's
         * {@code class} key
         */
        public TileClass getTileClass() {
            return tileClass;
        }

        /**
         * Returns the snap class of the tile; this describes the default
         * behaviour of the tile when it is snapped against other deck objects.
         *
         * @return the tile's default snapping behaviour
         */
        public PageItem.SnapClass getSnapClass() {
            switch (tileClass) {
                case TILE:
                    return PageItem.SnapClass.SNAP_TILE;
                case INLAY:
                    return PageItem.SnapClass.SNAP_INLAY;
                case OVERLAY:
                case SPINNABLE:
                    return PageItem.SnapClass.SNAP_OVERLAY;
                default:
                    throw new AssertionError();
            }
        }

        /**
         * Returns the basic category to which the tile belongs. This determines
         * in which list the tile will appear in the deck editor.
         *
         * @return the tile category
         */
        public Category getCategory() {
            return cat;
        }

        /**
         * Returns the code of the game that this tile belongs to, or
         * {@link Game#ALL_GAMES_CODE} if the graphic is not tied to a
         * particular game.
         *
         * @return the code for the game this tile is for
         */
        public String getGameCode() {
            return game;
        }

        /**
         * Returns an immutable copy of the client properties as a map.
         *
         * @return the client properties that will be set on the prototype item
         */
        public Map<String, String> getClientProperties() {
            if (clientProperties == null) {
                return Collections.emptyMap();
            } else {
                return Collections.unmodifiableMap(clientProperties);
            }
        }

        /**
         * Returns the prototype page item for this tile set entry. This is the
         * master item that appears in the deck editor's list of components.
         *
         * @return the prototype item for display in the deck editor
         */
        @SuppressWarnings("fallthrough")
        public synchronized PageItem getPrototypeItem() {
            if (proto == null) {
                Tile t;
                if (tileClass == TileClass.SPINNABLE) {
                    t = new RotatableTile(name, resource, resolution);
                } else {
                    if (outlineWidth > 0f) {
                        OutlinedTile ot = new OutlinedTile(name, resource, resolution);
                        ot.setOutlineColor(outlineColor);
                        ot.setOutlineWidth(outlineWidth);
                        ot.setOutlineCap(outlineCap);
                        ot.setOutlineJoin(outlineJoin);
                        ot.setOutlineDashPattern(outlineDash);
                        t = ot;
                    } else {
                        t = new Tile(name, resource, resolution);
                    }
                }

                switch (tileClass) {
                    case INLAY:
                        t.setSnapTarget(PageItem.SnapTarget.TARGET_MIXED);
                        t.setSnapClass(PageItem.SnapClass.SNAP_INLAY);
                        t.setClassesSnappedTo(EnumSet.of(PageItem.SnapClass.SNAP_TILE, PageItem.SnapClass.SNAP_INLAY));
                    // fallthrough
                    case TILE:
                        t.setFastOutlineAllowed(true);
                        break;
                    case OVERLAY:
                    case SPINNABLE:
                        t.setClassesSnappedTo(PageItem.SnapClass.SNAP_SET_NONE);
                        t.setSnapClass(PageItem.SnapClass.SNAP_OVERLAY);
                        break;
                    default:
                        throw new AssertionError();
                }

                if (clientProperties != null) {
                    for (Map.Entry<String, String> en : clientProperties.entrySet()) {
                        t.putClientProperty(en.getKey(), en.getValue());
                    }
                }

                proto = t;
            }
            return proto;
        }

        @Override
        public String toString() {
            return "TileSetEntry{tileName=" + tileName + ", name=" + name + ", resource=" + resource + ", credit=" + credit + ", resolution=" + resolution + ", cat=" + cat + ", tileClass=" + tileClass + ", game=" + game + '}';
        }
    }

    /**
     * A parser for tile set files.
     */
    public static final class Parser extends ResourceParser<Entry> {

        public Parser(String resource, boolean gentle) throws IOException {
            super(resource, gentle);
        }

        public Parser(InputStream in, boolean gentle) throws IOException {
            super(in, gentle);
        }

        @Override
        public Entry next() throws IOException {
            String li = readNonemptyLine();
            if (li == null) {
                return null;
            }

            Entry en = new Entry();
            if (li.indexOf('=') >= 0) {
                warning("tile entry appears to be missing name and resource");
            }
            en.tileName = li;
            en.name = localizeTileName(li);
            en.line = getLineNumber();

            li = readLine();
            if (li == null || li.isEmpty()) {
                error("tile entry is missing image resource");
                return null;
            }
            if (li.indexOf('=') >= 0) {
                warning("tile entry appears to be missing image resource");
            }
            en.resource = li;

            // any number of properties may follow
            for (;;) {
                String[] prop = readProperty(false);
                // check for EOF or empty line
                if (prop == null) {
                    break;
                }
                if (prop[0].isEmpty() && prop[1].isEmpty()) {
                    break;
                }

                if (prop[1].isEmpty() && !prop[0].equals("credit")) {
                    error("key has no value");
                }
                switch (prop[0]) {
                    case "ppi":
                    case "dpi":
                        try {
                            en.resolution = Double.valueOf(prop[1]);
                        } catch (NumberFormatException e) {
                            error("invalid resolution: " + prop[1]);
                        }
                        break;
                    case "class":
                        try {
                            en.tileClass = TileClass.valueOf(prop[1].toUpperCase(Locale.CANADA));
                        } catch (IllegalArgumentException e) {
                            error("invalid class: " + prop[1]);
                        }
                        break;
                    case "credit":
                        en.credit = prop[1];
                        if (en.credit.isEmpty()) {
                            en.credit = null;
                        }
                        break;
                    case "set":
                        if (prop[1].equals("bits")) {
                            prop[1] = Category.BOARD_BITS.name();
                        }
                        try {
                            Category cat = Category.valueOf(prop[1].toUpperCase(Locale.CANADA));
                            if (cat == Category.TOOLS || cat == Category.FACES) {
                                error("cannot assign tile to reserved set " + prop[1]);
                            } else {
                                en.cat = cat;
                            }
                        } catch (IllegalArgumentException e) {
                            error("invalid class: " + prop[1]);
                        }
                        break;
                    case "outline":
                        try {
                            String[] outline = prop[1].trim().split("\\s*;\\s*");
                            en.outlineWidth = Float.valueOf(outline[0]);
                            if (outline.length > 1) {
                                en.outlineColor = Settings.colour(outline[1]);
                            }
                            if (outline.length > 2) {
                                en.outlineCap = LineCap.valueOf(outline[2].toUpperCase(Locale.CANADA));
                            }
                            if (outline.length > 3) {
                                en.outlineJoin = LineJoin.valueOf(outline[3].toUpperCase(Locale.CANADA));
                            }
                            if (outline.length > 4) {
                                en.outlineDash = DashPattern.valueOf(outline[4].toUpperCase(Locale.CANADA));
                            }
                        } catch (Exception e) {
                            error("invalid outline: " + prop[1]);
                        }
                        break;
                    case "game":
                        Game g = Game.get(prop[1]);
                        if (g == null) {
                            warning("not a registered game: " + prop[1]);
                        }
                        en.game = prop[1];
                        break;
                    default:
                        if (prop[0].startsWith("client-")) {
                            if (!prop[1].isEmpty()) {
                                if (en.clientProperties == null) {
                                    en.clientProperties = new HashMap<>();
                                }
                                en.clientProperties.put(prop[0].substring("client-".length()), prop[1]);
                            }
                        } else {
                            error("not a known tile property: " + prop[0]);
                        }
                        break;
                }
            }

            return en;
        }

        /**
         * Localize tile name using the parser language. If the name doesn't
         * start with @, it is returned unchanged. Otherwise it is looked up and
         * possibly formatted as described in the sample tile set file.
         *
         * @param name the tile name value
         * @return a localized tile name
         */
        private String localizeTileName(String name) {
            if (name.startsWith("@")) {
                name = name.substring(1).replace('_', '-');
                int index = 0;
                int lastSlash = name.lastIndexOf('/');
                if (lastSlash >= 0) {
                    try {
                        index = Integer.parseInt(name.substring(lastSlash + 1));
                        name = name.substring(0, lastSlash);
                    } catch (NumberFormatException e) {
                    }
                }
                name = getLanguage().get(name, index);
            }
            return name;
        }
    }
}
