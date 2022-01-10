package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.BusyDialog;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.deck.Deck;
import ca.cgjennings.apps.arkham.deck.DeckEditor;
import ca.cgjennings.apps.arkham.deck.Page;
import ca.cgjennings.apps.arkham.deck.PaperProperties;
import ca.cgjennings.apps.arkham.deck.PaperSets;
import ca.cgjennings.apps.arkham.deck.item.CardFace;
import ca.cgjennings.apps.arkham.deck.item.PageItem;
import ca.cgjennings.apps.arkham.dialog.ImageViewer;
import ca.cgjennings.apps.arkham.diy.DIY;
import ca.cgjennings.apps.arkham.sheet.FinishStyle;
import ca.cgjennings.apps.arkham.sheet.MarkerStyle;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * A deck packer lays out a deck of game components automatically. The layout
 * algorithm is a modified bin-packing algorithm that balances producing a
 * minimal page count against grouping the objects in the deck sensibly.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class DeckPacker {

    private boolean doubleSided;
    private final List<Card> cardList = new LinkedList<>();
    private PaperProperties paper;
    private PaperProperties[] paperSizes;
    private boolean groupCardPairs = true;
    private boolean addBleedMargin = true;

    /**
     * Creates a new deck packer. The layout initially contains no game
     * components and will target PA4-sized paper (assuming that paper size is
     * installed in the application settings, which it is unless explicitly
     * removed). PA4 layouts can be printed on both North American letter and
     * ISO A4 paper.
     */
    public DeckPacker() {
        // find closest match to PA4 paper
        HashMap<Object, Object> attr = new HashMap<>();
        attr.put(PaperSets.KEY_CONCRETENESS, PaperSets.VALUE_CONCRETENESS_PHYSICAL);
        Set<PaperProperties> papers = PaperSets.getMatchingPapers(attr);
        paper = PaperSets.findBestPaper(8.27 * 72, 11 * 72, papers).deriveOrientation(false);
    }

    /**
     * Adds one copy of a game component stored in a file to the resulting
     * layout. This is a convenience for {@code add( gcFile, 1 )}.
     *
     * @param gcFile the file containing the game component
     * @throws NullPointerException if the game component file is
     * {@code null}
     * @see #add(java.io.File, int)
     */
    public boolean add(File gcFile) {
        return add(gcFile, 1);
    }

    /**
     * Reads a game component from a file and adds it to the resulting layout.
     * Some kinds of components, such as other decks, cannot be added to a deck.
     * Adding components of this type will have no effect on the resulting
     * layout, and this method will return {@code false} to signal that the
     * component was skipped. If an error occurs while loading the file, an
     * error message will be displayed, the file will be skipped, and this
     * method will return {@code false}.
     *
     * @param gcFile the file containing the game component
     * @param copies the number of copies of the game component to include
     * @return {@code true} if the file has been successfully added and the
     * layout will change as a result
     * @throws NullPointerException if the game component file is
     * {@code null}
     * @throws IllegalArgumentException if the number of copies is negative
     */
    public boolean add(File gcFile, int copies) {
        if (gcFile == null) {
            throw new NullPointerException("gcFile");
        }
        if (isCancelled()) {
            throw new CancellationException();
        }
        if (copies < 0) {
            throw new IllegalArgumentException("copies: " + copies);
        }
        if (copies == 0) {
            return false;
        }

        if (!Deck.isDeckLayoutSupported(gcFile)) {
            return false;
        }

        GameComponent gc = ResourceKit.getGameComponentFromFile(gcFile, true);
        if (gc == null) {
            return false;
        }
        for (int i = 0; i < copies; ++i) {
            addImpl(gcFile, gc);
        }
        return true;
    }

    /**
     * Called by {@link #add} to add a loaded game component to the layout. This
     * method takes incoming game components and adds {@link Card} objects to
     * the layout based on the game component's sheets.
     *
     * @param f the file associated with the game component
     * @param gc the game component instance to add
     * @see #addCard
     */
    protected void addImpl(File f, GameComponent gc) {
        String path = f.getAbsolutePath();
        Sheet[] sheets = gc.getSheets() == null ? gc.createDefaultSheets() : gc.getSheets();
        String klass = gc.getClass().getName();
        if (klass == null) {
            klass = "";
        }
        if (gc.getClass() == DIY.class) {
            klass += ':' + ((DIY) gc).getHandlerScript();
        }
        
        for (int i = 0; i < sheets.length; ++i) {
            int index = i;
            CardFace front, back = null;

            front = new CardFace(gc, path, i);

            // if this sheet is an embedded marker, the back might be
            // autogenerated
            MarkerStyle style = sheets[i].getMarkerStyle();
            if (style == MarkerStyle.COPIED || style == MarkerStyle.MIRRORED) {
                back = new CardFace(gc, path, i);
                if (style == MarkerStyle.MIRRORED) {
                    back.flip();
                }
            } // if this is not an embedded marker, and there is at least
            // one more sheet, always assume that the next sheet represents
            // the back of this sheet
            else if (style == null) {
                if (i < sheets.length - 1) {
                    back = new CardFace(gc, path, ++i);
                }
            }

            addCard(new Card(f, index, klass, front, back));
        }
    }

    /**
     * Adds a card to the internal list of cards to include in the layout. Can
     * be used by subclasses that override
     * {@link #addImpl(java.io.File, ca.cgjennings.apps.arkham.component.GameComponent)}
     * to post the {@link Card} groupings that they create. Cards added with
     * this method will later be returned from
     * {@link #getSortedCards(java.util.Comparator)}.
     *
     * @param c the card instance to be added
     */
    protected final void addCard(Card c) {
        cardList.add(c);
    }

    /**
     * Returns {@code true} if the layout will be designed for double-sided
     * printing.
     *
     * @return {@code true} if double-sided layouts are enabled
     * @see #setLayoutDoubleSided
     */
    public boolean isLayoutDoubleSided() {
        return doubleSided;
    }

    /**
     * Sets whether the layout will be designed for double-sided printing.
     * Double-sided layouts place the front face of a component on an
     * odd-numbered page, and the matching back face on the following page. The
     * layout order of the following page is flipped horizontally so that they
     * will line up correctly when printed.
     *
     * @param doubleSided if {@code true}, a double-sided layout is created
     */
    public void setLayoutDoubleSided(boolean doubleSided) {
        this.doubleSided = doubleSided;
    }

    /**
     * Returns the quality setting for the layout algorithm.
     *
     * @return the layout quality value
     */
    public int getLayoutQuality() {
        return quality;
    }

    /**
     * Sets the layout algorithm quality setting. The quality setting is a value
     * between 0 and 9 inclusive, where lower values favour speed over quality,
     * and higher values favour quality over speed. Higher quality layouts try
     * harder to pack components more neatly and/or into a smaller number of
     * pages.
     *
     * @param quality the quality setting, from {@link #QUALITY_SPEED} to
     * {@link #QUALITY_FIT}
     */
    public void setLayoutQuality(int quality) {
        if (quality < 0 || quality > 9) {
            throw new IllegalArgumentException("quality must be between 0 and 9 inclusive");
        }
        this.quality = quality;
    }

    private int quality = 7;

    /**
     * The layout quality value that maximizes layout speed.
     */
    public static final int QUALITY_SPEED = 0;
    /**
     * The layout quality value that maximizes compactness.
     */
    public static final int QUALITY_FIT = 9;

    /**
     * Returns the paper type used to create the layout.
     *
     * @return the paper type that determines the size of pages and margin in
     * the layout
     */
    public PaperProperties getPaper() {
        return paper;
    }

    /**
     * Sets the paper format to use for laying out cards. The paper's size and
     * margin determine the area available to lay out components on each page.
     *
     * @param paper the paper type that determines the size of pages and margin
     * in the layout
     * @throws NullPointerException if the paper is {@code null}
     */
    public void setPaper(PaperProperties paper) {
        if (paper == null) {
            throw new NullPointerException("paper");
        }
        this.paper = paper;
    }

    /**
     * Create a deck layout using the current cards and layout options. This
     * method creates and returns a new deck.
     *
     * @return a new deck object containing the laid out cards
     */
    public Deck createLayout() {
        cancelled = false;
        Deck deck = new Deck();
        if (addBleedMargin) {
            final FinishStyle finishStyle = addBleedMargin ? FinishStyle.MARGIN : FinishStyle.SQUARE;
            final double bleedMargin = addBleedMargin ? 9d : 0d;
            deck.setFinishStyle(finishStyle);
            deck.setBleedMarginWidth(bleedMargin);
        }
        deck.setPaperProperties(paper);
        layout(deck);
        return deck;
    }

    /**
     * Create a deck layout using the current cards and layout options. This
     * method uses the deck of the supplied editor. The deck is expected to be
     * new and empty.
     *
     * @param deckEditor the deck that will contain the laid out cards
     */
    public void createLayout(DeckEditor deckEditor) {
        cancelled = false;
        usingEditor = deckEditor;
        Deck deck = deckEditor.getDeck();
        deck.setPaperProperties(paper);
        layout(deck);
    }
    private DeckEditor usingEditor;

    /**
     * Lays out a prepared deck and set of cards. Subclasses may override this
     * to use a different bin packing algorithm. The passed-in deck is the
     * destination for the layout, and cannot be {@code null}. The paper
     * size for the layout can be obtained by calling
     * {@code deck.getPaperProperties()}.
     *
     * @param deck the deck that should contain the details of the layout
     * @throws CancellationException if the cancellation flag was set while
     * computing the card layout
     */
    protected void layout(Deck deck) {
        // reset state in case this is called a second time
        areas = null;

        PaperProperties paper = deck.getPaperProperties();

        Card[] cards = getSortedCards(null);
        double w = paper.getPageWidth() - 2 * paper.getMargin();
        double h = paper.getPageHeight() - 2 * paper.getMargin();

        // only cards with an area less than this value
        // will be considered for the more aggressive fitting
        // algorithm
        final double aggressionArea = 0.66 * 0.66 * w * h;

        BusyDialog bd = BusyDialog.getCurrentDialog();
        if (bd != null) {
            bd.setProgressMaximum(cards.length);
        }

        for (int cardNum = 0; cardNum < cards.length; ++cardNum) {
            Card c = cards[cardNum];
            if (bd != null) {
                bd.setStatusText(string("pa-makedeck-busy-place", c.file.getName()));
                bd.setProgressCurrent(cardNum);
            }

            boolean placed = false;
            for (int i = 0; i < deck.getPageCount(); ++i) {
                if (tryToPlaceOnPage(deck, i, c, w, h)) {
                    placed = true;
                    break;
                }
                if (isCancelled()) {
                    throw new CancellationException();
                }
            }

            // if it didn't fit, before adding a new page we will try
            // a more aggressive algorithm that looks for holes in
            // the current layout---however, we only do this if the
            // area of the card being placed is relatively small,
            // as any holes that exist are almost certainly small
            if (!placed /*&& quality > 2*/ && c.area < aggressionArea) {
                for (int i = 0; i < deck.getPageCount(); ++i) {
                    if (tryToPlaceOnPageAggressively(deck, i, c, w, h)) {
                        placed = true;
                        break;
                    }
                    if (isCancelled()) {
                        throw new CancellationException();
                    }
                }
            }

            if (!placed) {
                int blankPage = deck.getPageCount() - 1;
                if (blankPage < 0) {
                    blankPage = 0;
                }
                if (!getArea(blankPage).isEmpty()) {
                    ++blankPage;
                }
                place(deck, blankPage, c, 0, 0);
            }
        }

        for (int i = 0; i < deck.getPageCount(); ++i) {
            deck.getPage(i).centerContent();
        }
    }

    /**
     * This is the fast fit algorithm. The algorithm looks at a sliding window
     * that moves down the page. The window is always as high as the height of
     * the card(s) being fitted. At each window position, it computes the
     * bounding rectangle of all of the cards that overlap the window. It then
     * checks the left and right edges of this bounding box and looks to see if
     * there is enough space to place the card. When there is, it computes the
     * bounding box of a second window. It covers the same horizontal area as
     * the placement of the card, but extends from the top of the page to the
     * top of the window. (This snaps the card to the top of any cards above
     * it.)
     *
     * @param deck the deck to place the card in
     * @param index the page index of the page to try
     * @param c the card to place
     * @param w the required width
     * @param h the required height
     * @return {@code true} if the card was placed successfully
     */
    private boolean tryToPlaceOnPage(Deck deck, int index, Card c, double w, double h) {
        Page p = deck.getPage(index);
        Area a = getArea(index);
        double granularity = Math.min(FAST_QUALITIES[quality], c.height);

        // no cards on this page, place a first card in the upper-left corner
        if (a.isEmpty()) {
            place(deck, index, c, 0, 0);
            return true;
        }

        // go to the last y pos that still allows card to fit, but add an extra
        // amount of space equal to the granularity; this adds one extra pass
        // so that cards that fit but are put over the bottom initially due to
        // granularity can still be placed
        double limit = h - c.height + granularity;

        for (double y = 0; y < limit; y += granularity) {
            Area slice = new Area(new Rectangle2D.Double(0d, y, w, c.height));
            slice.intersect(a);
            Rectangle2D bounds = slice.getBounds2D();

            // try to fit the card on the LHS
            if (bounds.getMinX() >= c.width) {
                // the card fits but there may be empty space over it
                slice = new Area(new Rectangle2D.Double(bounds.getMinX() - c.width, 0, c.width, y));
                slice.intersect(a);
                Rectangle2D overhead = slice.getBounds2D();
                if (overhead.getMaxY() + c.height < h) {
                    place(deck, index, c, bounds.getMinX() - c.width, overhead.getMaxY());
                    return true;
                }
            }

            // try to fit the card on the RHS
            if ((bounds.getMaxX() + c.width) <= w) {
                // the card fits but there may be empty space over it
                slice = new Area(new Rectangle2D.Double(bounds.getMaxX(), 0, c.width, y));
                slice.intersect(a);
                Rectangle2D overhead = slice.getBounds2D();
                if (overhead.getMaxY() + c.height < h) {
                    place(deck, index, c, bounds.getMaxX(), overhead.getMaxY());
                    return true;
                }
            }
        }
        return false;
    }

    private static final double[] FAST_QUALITIES = new double[]{
        128d, 64d, 32d, 16d, 8d, 4d, 2d, 1d, 0.5d, 0.25d
    };
    private static final double[] AGGRESSIVE_QUALITIES = new double[]{
        32d, 24d, 16d, 12d, 8d, 6d, 4d, 2d, 1d, 0.5d
    };

    private boolean tryToPlaceOnPageAggressively(Deck deck, int index, Card c, double w, double h) {
        Page p = deck.getPage(index);
        Area a = getArea(index);
        double granularity = Math.min(AGGRESSIVE_QUALITIES[quality], c.height);

        // no cards on this page, place a first card in the upper-left corner
        if (a.isEmpty()) {
            place(deck, index, c, 0, 0);
            return true;
        }

        // go to the last pos that still allows card to fit, but add an extra
        // amount of space equal to the granularity; this adds one extra pass
        // so that cards that fit but are put over the bottom initially due to
        // granularity can still be placed
        double yLimit = h - c.height + granularity;
        double xLimit = w - c.width + granularity;

        // debugging only
//		final int SCALE = 4;
//		BufferedImage window = new BufferedImage(
//				(int) Math.ceil( c.width * SCALE ), (int) Math.ceil( c.height * SCALE ),
//				BufferedImage.TYPE_INT_RGB
//		);
        for (double y = 0; y < yLimit; y += granularity) {
            if (isCancelled()) {
                throw new CancellationException();
            }
            for (double x = 0; x < xLimit; x += granularity) {
                Area slice = new Area(new Rectangle2D.Double(x, y, c.width, c.height));
                slice.intersect(a);

                // debugging only: is the window empty?
//				Graphics2D g = window.createGraphics();
//				try {
//					g.setColor( Color.WHITE );
//					g.fillRect( 0, 0, window.getWidth(), window.getHeight() );
//					g.scale( SCALE, SCALE );
//					g.setColor( Color.BLACK );
//					g.translate( -x, -y );
//					g.fill( slice );
//				} finally {
//					g.dispose();
//				}
//
//				boolean empty = true;
//				for( int py=0; empty && py<window.getHeight(); ++py ) {
//					for( int px=0; px<window.getWidth(); ++px ) {
//						if( window.getRGB( px, py ) != 0xffffffff  ) {
//							empty = false;
//							break;
//						}
//					}
//				}
//				if( empty ) System.err.println( "Bitmap says empty: " + c.file );
//				if( slice.isEmpty() ) System.err.println( "Slice says empty: " + c.file );
//				if( !empty ) continue;
                if (!slice.isEmpty()) {
                    continue;
                }

                // fit to top edge
                slice = new Area(new Rectangle2D.Double(x, 0, c.width, y));
                slice.intersect(a);
                double yPos = slice.isEmpty() ? 0 : slice.getBounds2D().getMaxY();

                if (yPos + c.height > h) {
                    continue;
                }

                // fit to left edge
                slice = new Area(new Rectangle2D.Double(0, yPos, x, c.height));
                slice.intersect(a);
                double xPos = slice.isEmpty() ? 0 : slice.getBounds2D().getMaxX();

                if (xPos + c.width > w) {
                    continue;
                }

                place(deck, index, c, xPos, yPos);
                return true;
            }
        }

        return false;
    }

    private void addPage(Deck deck) {
        if (usingEditor == null) {
            deck.addNewPage();
        } else {
            usingEditor.addPage();
        }
    }

    /**
     * Places a card into a deck on a given page and position.
     *
     * @param deck the deck to place the component in
     * @param pageIndex the index of the page within the deck; if the page does
     * not exist it is created
     * @param c the card to add
     * @param x the x-coordinate to place the card at
     * @param y the y-coordinate to place the card at
     */
    protected void place(Deck deck, int pageIndex, Card c, double x, double y) {
        while (pageIndex > deck.getPageCount() - 1) {
            addPage(deck);
        }
        if (isLayoutDoubleSided() && c.back != null && pageIndex == deck.getPageCount() - 1) {
            addPage(deck);
        }
        Page p = deck.getPage(pageIndex);
        p.addCard(c.front);
        c.front.setLocation(x, y);
        updateArea(pageIndex, c.front);
        if (c.back != null) {
            if (isLayoutDoubleSided()) {
                p = deck.getPage(pageIndex + 1);
                double edge = deck.getPaperProperties().getPageWidth() - deck.getPaperProperties().getMargin() * 2;
                p.addCard(c.back);
                c.back.setLocation(edge - x - c.back.getWidth(), y);
                updateArea(pageIndex + 1, c.back);
            } else {
                p.addCard(c.back);
                c.back.setLocation(x + c.front.getWidth(), y);
                updateArea(pageIndex, c.back);

                if (isGroupingEnabled()) {
                    deck.setSelection(c.front);
                    deck.addToSelection(c.back);
                    deck.groupSelection();
                }
            }
        }
    }

    private Area getArea(int pageIndex) {
        if (areas == null) {
            areas = new ArrayList<>();
        }
        while (pageIndex > areas.size() - 1) {
            areas.add(new Area());
        }
        return areas.get(pageIndex);
    }

    private void updateArea(int pageIndex, PageItem item) {
        Area a = getArea(pageIndex);
        a.add(new Area(item.getRectangle()));
        areas.set(pageIndex, a);

        if (layoutDebug) {
            BufferedImage image = paper.createCompatibleImage(72);
            Graphics2D g = image.createGraphics();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, image.getWidth(), image.getHeight());
                g.setColor(new Color(0x77_aaff));
                g.fill(a);
                g.setColor(Color.PINK);
                g.fill(item.getRectangle());
                ImageViewer iv = new ImageViewer(StrangeEons.getWindow(), image, true);
                iv.setTitle(item.getName() + " placed on Page: " + (pageIndex + 1));
                iv.setVisible(true);
            } finally {
                g.dispose();
            }
        }
    }

    private ArrayList<Area> areas;

    /**
     * Subclasses may set this to {@code true} during testing. If the
     * subclass uses {@link #place} to add cards, then a sequence of images will
     * be displayed showing how each card is placed.
     */
    protected boolean layoutDebug = false;

    /**
     * This method prepares cards for layout once all of the layout requirements
     * are known. The base class fills in the width, height, and area for a
     * card; if not making a double-sided deck, then the two faces of a
     * double-sided card are treated as a single unit and the sizes are set
     * appropriately.
     *
     * @param c the card to prepare
     */
    protected void prepareCard(Card c) {
        if (!isLayoutDoubleSided() && c.back != null) {
            c.width = c.front.getWidth() + c.back.getWidth();
        } else {
            c.width = c.front.getWidth();
        }
        c.height = c.front.getHeight();
        c.area = c.width * c.height;
    }

    /**
     * Returns an array of the cards that have been added for layout. If the
     * comparator {@code comp} is non-{@code null} then it will be
     * used to determine the sort order. Otherwise, a default sort order is used
     * based on {@code Card}'s {@code Comparable} implementation. The
     * default order is suited to the base implementation's bin packing
     * algorithm. Subclasses may substitute a different comparison function
     * suited for other algorithms.
     */
    protected Card[] getSortedCards(Comparator<Card> comp) {
        // make sure sizes are up to date before starting
        for (Card c : cardList) {
            prepareCard(c);
        }

        // a copy of the card list that lets us do additional splitting up
        // now that we know the layout details
        List<Card> separatedCards;

        // if not side by side, but keeping the cards together makes them
        // not possible to fit on a page, then split them into separate cards
        if (!isLayoutDoubleSided()) {
            double w = paper.getPageWidth() - paper.getMargin() * 2;
            double h = paper.getPageHeight() - paper.getMargin() * 2;
            separatedCards = new LinkedList<>();
            for (Card c : cardList) {
                if (c.back != null && c.width > w && c.front.getWidth() < w) {
                    Card c2 = new Card(c.file, c.sheetIndex + 1, c.klass, c.back, null);
                    c.back = null;
                    separatedCards.add(c);
                    separatedCards.add(c2);
                    prepareCard(c);
                    prepareCard(c2);
                } else {
                    separatedCards.add(c);
                }
            }
        } else {
            separatedCards = cardList;
        }

        Card[] cards = separatedCards.toArray(new Card[cardList.size()]);
        java.util.Arrays.sort(cards, comp);
        return cards;
    }

    /**
     * Returns {@code true} if front and back faces should be grouped
     * together. This has no effect if double-sided layout is enabled.
     *
     * @return {@code true} if grouping is enabled
     */
    public boolean isGroupingEnabled() {
        return groupCardPairs;
    }

    /**
     * Sets whether front and back faces should be grouped together. This has no
     * effect if double-sided layout is enabled.
     *
     * @param groupCardPairs if {@code true}, front and back faces placed
     * side-by-side will be placed into a group
     */
    public void setGroupingEnabled(boolean groupCardPairs) {
        this.groupCardPairs = groupCardPairs;
    }
    
    /**
     * Returns {@code true} if bleed margins should be added to faces.
     * 
     * @return {@code true} if bleed margins are enabled
     */
    public boolean isBleedMarginEnabled() {
        return addBleedMargin;
    }
    
    /**
     * Sets whether bleed margins should be added to card faces.
     * 
     * @param bleedMargin 
     */
    public void setBleedMarginEnabled(boolean bleedMargin) {
        this.addBleedMargin = bleedMargin;
    }

    /**
     * This class represents one or more card faces in a form suitable for
     * planning layouts.
     */
    protected static class Card implements Comparable<Card> {

        public Card(File f, int index, String klass, PageItem front, PageItem back) {
            this.file = f;
            this.sheetIndex = index;
            this.klass = klass;
            this.front = front;
            this.back = back;
        }

        /**
         * The class ID of the component.
         */
        String klass;
        /**
         * The front face of the card; always valid.
         */
        PageItem front;
        /**
         * The back face of the card; may be {@code null}. If present, must
         * be same size as {@code front}.
         */
        PageItem back;
        /**
         * The width of the card; if not a double sided layout and there is a
         * back, the width is the total for the front and back.
         */
        double width;
        /**
         * The height of the card.
         */
        double height;
        /**
         * The precomputed area (width times height).
         */
        double area;
        /**
         * The file that the card came from.
         */
        File file;
        /**
         * The index of the front face's sheet in the card.
         */
        int sheetIndex;

        @Override
        public int compareTo(Card o) {
            // put all cards with only one face at the end, to minimize the
            // number of holes when doing a double-sided layout
            if (back == null && o.back != null) {
                return 1;
            }
            if (o.back == null && back != null) {
                return -1;
            }

            // sort by height, longest is "less than"
            int c = cmp(o.height, height);
            if (c != 0) {
                return c;
            }

            // then by area, largest is "less than"
            c = cmp(o.area, area);
            if (c != 0) {
                return c;
            }

            // then by class name
            c = klass.compareTo(o.klass);
            if (c != 0) {
                return c;
            }

            // then by file name
            if (file == null && o.file != null) {
                return -1;
            }
            c = file.compareTo(file);
            if (c != 0) {
                return c;
            }

            // finally by the index of the sheet
            return sheetIndex - o.sheetIndex;
        }

        /**
         * Compare two point measures, with an epsilon of a 1/100th of a point.
         * Measures within the epsion are considered equal. Otherwise, a
         * standard comparison applies.
         *
         * @param lhs
         * @param rhs
         * @return -1 is lhs &lt; rhs; 0 if lhs == rhs; 1 if lhs &gt; rhs
         */
        private int cmp(double lhs, double rhs) {
            double d = lhs - rhs;
            return Math.abs(d) < EPSILON ? 0 : (d < 0d ? -1 : 1);
        }
        private static final double EPSILON = 1d / 100d;
    }

    /**
     * Sets the cancellation flag. If a deck is being laid out, this will cause
     * the layout algorithm to throw an exception. It can be called in response
     * to user requests to cancel the layout operation.
     */
    public final void cancel() {
        cancelled = true;
    }

    /**
     * Returns {@code true} if the cancellation flag has been set.
     *
     * @return {@code true} if the layout operation has been cancelled
     * @see #cancel()
     */
    public final boolean isCancelled() {
        return cancelled;
    }

    private volatile boolean cancelled = false;
}
