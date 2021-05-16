package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.ContextBar;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.ViewQuality;
import ca.cgjennings.apps.arkham.commands.AbstractCommand;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.apps.arkham.component.AbstractGameComponent;
import ca.cgjennings.apps.arkham.deck.item.CustomTile;
import ca.cgjennings.apps.arkham.deck.item.DragHandle;
import ca.cgjennings.apps.arkham.deck.item.EditablePageItem;
import ca.cgjennings.apps.arkham.deck.item.Group;
import ca.cgjennings.apps.arkham.deck.item.Line;
import ca.cgjennings.apps.arkham.deck.item.OutlineStyle;
import ca.cgjennings.apps.arkham.deck.item.PageItem;
import ca.cgjennings.apps.arkham.deck.item.ShadowStyle;
import ca.cgjennings.apps.arkham.deck.item.Style;
import ca.cgjennings.apps.arkham.deck.item.TextBox;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import ca.cgjennings.math.Interpolation;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.StepSelector;
import ca.cgjennings.ui.anim.Animation;
import ca.cgjennings.ui.anim.TimeShiftedComposer;
import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashSet;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * A view of a page of cards that forms part of a deck.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public final class PageView extends JComponent {

    /**
     * If {@code true}, feedback about view performance will be printed at
     * the top of the view. This is initially {@code true} only if the
     * release type is {@code DEVELOPMENT}, but it can be changed at any
     * time.
     *
     * @see StrangeEons#getReleaseType()
     */
    public static boolean DEBUG_DRAW = StrangeEons.getReleaseType() == StrangeEons.ReleaseType.DEVELOPMENT;

    /**
     * Common deck commands use this property key to determine which key they
     * should be bound to in the page view. They are commonly bound to simple
     * keys that are unsuitable for use as global accelerators, like F for Move
     * to Front.
     */
    public static final String PAGE_VIEW_ACTION_KEY = "se#pageviewkey";

    private final CropMarkManager cropMarks;
    private boolean editable = true;
    // turning off anti-aliasing will slow things down if using quartz renderer
    private final boolean alwaysAntiAliasView = Settings.getShared().getYesNo("always-anti-alias-page-views") || PlatformSupport.PLATFORM_IS_MAC;

    private static final String[] DROPPABLE_IMAGE_TYPES = new String[]{"jpg", "jpeg", "png", "jp2"};

    /**
     *     */
    public PageView() {
        setAutoscrolls(true);
        setBackground(BORDER);
        setDoubleBuffered(true);
//		setTransferHandler( itemDropHandler );

        putClientProperty(ContextBar.BAR_LEADING_SIDE_PROPERTY, true);
        putClientProperty(ContextBar.BAR_CUSTOM_LOCATION_PROPERTY, BAR_LOCATOR);

        addMouseMotionListener(listenerAdapter);
        addMouseListener(listenerAdapter);
        addMouseWheelListener(listenerAdapter);
        addMouseMotionListener(listenerAdapter);
        addKeyListener(listenerAdapter);

        // possible OS X workaround
        enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);

        setFocusable(true);
        setSize(2_000, 2_000);
        cropMarks = new CropMarkManager(2f, CROP);
        updateCropMarkManager();

        localizeForOSX();
        changeView();
    }

    private final ListenerAdapter listenerAdapter = new ListenerAdapter();

    private class ListenerAdapter extends KeyAdapter implements MouseWheelListener, MouseListener, MouseMotionListener, FocusListener {

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            int rotation = e.getWheelRotation();
            if (!Settings.getShared().getYesNo("invert-wheel-zoom")) {
                rotation = -rotation;
            }
            adjustZoomBySteps(rotation);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            PageView.this.mouseClicked(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            PageView.this.mousePressed(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            PageView.this.mouseReleased(e);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            getPage().getDeck().setActivePage(getPage());
            updateCursor(e.getPoint());
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            PageView.this.mouseDragged(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            updateCursor(e.getPoint());
        }

        @Override
        public void focusGained(FocusEvent e) {
            putClientProperty(ContextBar.BAR_DISABLE_PROPERTY, null);
            getPage().getDeck().setActivePage(getPage());
        }

        @Override
        public void focusLost(FocusEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
            PageView.this.keyPressed(e);
        }
    };

    void updateCropMarkManager() {
        Page p = getPage();
        Deck d = p == null ? null : p.getDeck();
        if (d == null) {
            return;
        }

        cropMarks.setEnabled(d.getPublishersMarksEnabled());
        cropMarks.setMarkWidth(d.getPublishersMarkWidth());
        cropMarks.setMarkSize(d.getPublishersMarkDistance(), d.getPublishersMarkLength());
        if (isShowing()) {
            repaint();
        }
    }

    private void localizeForOSX() {
        putClientProperty(
                "Quaqua.TabbedPaneChild.contentBackground", BORDER
        );
    }

    protected void updateCursor(Point mousepos) {
        Cursor c = Cursor.getDefaultCursor();
        if (editable) {
            if (dragHandle != null) {
                c = dragHandle.getCursor();
            } else {
                DragHandle h = getDragHandleUnderPosition(mousepos);
                if (h != null) {
                    c = h.getCursor();
                }
            }
        }

        setCursor(c);
    }

    /**
     * If the card the pointer is over has drag handles, and the pointer is over
     * one of the handles, return the handle. Otherwise, return
     * {@code null}.
     */
    private DragHandle getDragHandleUnderPosition(Point pos) {
        if (pos == null) {
            pos = getMousePosition();
        }
        if (pos == null || !ViewOptions.isDragHandlePainted()) {
            return null;
        }
        Point2D loc = viewToDocument(pos.x, pos.y);
        PageItem c = page.getCardAtLocation(loc);
        DragHandle[] handles = null;
        if (c != null && !c.isSelectionLocked() && (handles = c.getDragHandles()) != null) {
            for (DragHandle h : handles) {
                if (h.hitTest(loc)) {
                    return h;
                }
            }
        }

        // nothing found in card under cursor: search through all items with
        // exterior handles
        int cards = page.getCardCount();
        for (int i = cards - 1; i >= 0; --i) {
            PageItem pi = page.getCard(i);
            if (pi.hasExteriorHandles()) {
                if (pi != null && !pi.isSelectionLocked()) {
                    for (DragHandle h : pi.getDragHandles()) {
                        if (h.hitTest(loc)) {
                            return h;
                        }
                    }
                }
            }
        }
        return null;
    }
    public static final double HIGH_DPI = 200d;
    public static final double MEDIUM_DPI = 150;
    public static final double LOW_DPI = 96;

    void forceRerender() {
        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dummy.createGraphics();
        try {
            for (int i = 0; i < page.getCardCount(); ++i) {
                page.getCard(i).paint(g, RenderTarget.PREVIEW, 1d);
            }
        } finally {
            g.dispose();
        }
        repaint();
    }

    /**
     * Repaints the part of the view covered by an item.
     *
     * @param item the item that needs to be updated
     */
    public void repaint(PageItem item) {
        if (item == null) {
            throw new NullPointerException("item");
        }
        // if the page item has exterior drag handles, we'll have to repaint
        // the entire view since we don't know where the handle is
        if (item.hasExteriorHandles()) {
            repaint();
            return;
        }

        Rectangle2D.Double bounds = item.getRectangle();

        // width of the extent that outlines, shadows, etc. can
        // protrude from outside of the bounding box
        double protrusion = 0d;
        if (item instanceof OutlineStyle) {
            protrusion = ((OutlineStyle) item).getOutlineWidth();
        }

        // this will need to be updated when shadows have more parameters
        if (item instanceof ShadowStyle) {
//			final ShadowStyle shadowed = (ShadowStyle) item;
            final Line shadowed = (Line) item;
            if (shadowed.isShadowed()) {
                // only lines have shadows and we need the width
                protrusion += shadowed.getLineWidth();
            }
        }

        // adjust the bounds rectangle
        if (protrusion > 0d) {
            final double extentsAdjust = protrusion * 2d;
            bounds.x -= protrusion;
            bounds.y -= protrusion;
            bounds.width += extentsAdjust;
            bounds.height += extentsAdjust;
        }

//		repaint( bounds );
        repaint();
    }

    /**
     * Repaints the area of the view that is covered by a rectangle in the
     * document coordinate system. The area will be converted to the the view
     * coordinate system, and if part of the rectangle is currently visible in
     * the view then that part of the view will be repainted.
     *
     * @param rectangleInDocumentSpace the area of the document that needs
     * repainting
     */
    public void repaint(Rectangle2D.Double rectangleInDocumentSpace) {
        Rectangle rectToPaint = documentToView(rectangleInDocumentSpace);
        Rectangle visiblePart = rectToPaint.intersection(new Rectangle(0, 0, getWidth(), getHeight()));
        if (!visiblePart.isEmpty()) {
            repaint(visiblePart);
        }
    }

    private long frameTime;

    private static final Composite alphaComposite40 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.40f);
    private static final Composite alphaComposite67 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.67f);

    private final Rectangle2D.Double viewClip = new Rectangle2D.Double();

    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = ((Graphics2D) g1);

        frameTime = System.currentTimeMillis();
        AffineTransform oldAT = g.getTransform();

        ViewQuality viewQuality = editable ? ViewQuality.get() : ViewQuality.ULTRAHIGH;
        RenderTarget quality = RenderTarget.PREVIEW;
        if (viewQuality.ordinal() < ViewQuality.HIGH.ordinal()) {
            quality = RenderTarget.FAST_PREVIEW;
        }
        viewQuality.applyPreviewWindowHints(g);

        Composite composite = g.getComposite();

        // update the clip rectangle based on the component frame
        // this prevents us from redrawing rendered items outside of the view
        {
            Point2D cp = viewToDocument(0, 0);
            viewClip.x = cp.getX();
            viewClip.y = cp.getY();
            cp = viewToDocument(getWidth(), getHeight());
            viewClip.width = cp.getX() - viewClip.x;
            viewClip.height = cp.getY() - viewClip.y;

            // adjusts the clip rect so that we still draw
            // publisher's marks that are onscreen if the card is offscreen
            Deck deck = getPage().getDeck();
            if (deck != null) {
                double markAdj = deck.getPublishersMarkDistance() + deck.getPublishersMarkLength()
                        + deck.getPublishersMarkWidth();

                // for selection lines, and border lines on text boxes and such
                markAdj = Math.max(markAdj, 26);

                viewClip.x -= markAdj;
                viewClip.y -= markAdj;
                viewClip.width += markAdj * 2d;
                viewClip.height += markAdj * 2d;
            }
        }

        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        g.transform(worldToView);

        Rectangle2D rect = new Rectangle2D.Double();

        // draw the page outline, margin, and interior
        rect.setRect(0, 0, pageWidth(), pageHeight());

        final boolean drawMargin = ViewOptions.isMarginPainted();
        g.setColor(drawMargin ? MARGIN : PAGE);
        g.fill(rect);

        g.setColor(GRID_OUTLINE);
        g.setStroke(GRID_OUTLINE_STROKE);
        g.draw(rect);

        if (drawMargin) {
            // if margins are disabled, we already painted the page background;
            // otherwise we painted the margins and now we need to paint the interior
            g.setColor(PAGE);
            rect.setRect(margin(), margin(), pageWidth() - margin() * 2d, pageHeight() - margin() * 2d);
            g.fill(rect);
        }

        if (ViewOptions.isGridPainted()) {
            boolean drawMinor = scale >= 0.5d;
            g.setColor(GRID);
            Line2D line = new Line2D.Double();
            double gridSeparation = gridSeparation();
            double minor = gridSeparation / 2d;

            if (gridSeparation > 0) {
                for (double x = gridSeparation; x < pageWidth(); x += gridSeparation) {
                    if (x + minor < viewClip.x) {
                        continue;
                    }
                    if (x > viewClip.x + viewClip.width) {
                        break;
                    }

                    g.setStroke(GRID_MAJOR_STROKE);
                    line.setLine(x, 0d, x, pageHeight());
                    g.draw(line);
                    if (drawMinor && x + minor < pageWidth()) {
                        g.setStroke(GRID_MINOR_STROKE);
                        line.setLine(x + minor, 0d, x + minor, pageHeight());
                        g.draw(line);
                    }
                }
                for (double y = gridSeparation; y < pageHeight(); y += gridSeparation) {
                    if (y + minor < viewClip.y) {
                        continue;
                    }
                    if (y > viewClip.y + viewClip.height) {
                        break;
                    }

                    g.setStroke(GRID_MAJOR_STROKE);
                    line.setLine(0d, y, pageWidth(), y);
                    g.draw(line);
                    if (drawMinor && y + minor < pageHeight()) {
                        g.setStroke(GRID_MINOR_STROKE);
                        line.setLine(0d, y + minor, pageWidth(), y + minor);
                        g.draw(line);
                    }
                }
                if (drawMinor) {
                    line.setLine(minor, 0d, minor, pageHeight());
                    g.draw(line);
                    line.setLine(0d, minor, pageWidth(), minor);
                    g.draw(line);
                }
            }
        }

        ////////////////////////////////////////////////////////
        if (alwaysAntiAliasView || (!isDragging && viewQuality != ViewQuality.LOW)) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }

        viewQuality.applyPreviewWindowHints(g);

        Page page = getPage();
        PageItem[] selection = (page.getDeck().getSelectionPage() == page) ? page.getDeck().getSelection() : null;

        cropMarks.update(page);
        cropMarks.paint(g);

        // draw cards and handles
        g.setColor(INACTIVE_HANDLE);
        g.setStroke(HANDLE_STROKE);

        for (int i = 0; i < page.getCardCount(); ++i) {
            PageItem c = page.getCard(i);
            paintPageItem(g, c, scale * 72d/*viewDPI*/, quality);

            DragHandle[] handles = c.getDragHandles();
            if (editable && !c.isSelectionLocked() && handles != null && ViewOptions.isDragHandlePainted()) {
                for (DragHandle h : handles) {
                    h.paintHandle(g);
                }
            }
        }

        // if the user is dragging a selection around,
        //   draw a ghost where the selection will snap to
        if (isDragging && selection != null) {
            if (selection.length > 0) {
                PageItem snapCard = selection[selection.length - 1];
                PageItem overlapped = page.getCardToSnapTo(snapCard, snapCard.getClassesSnappedTo());
                if (overlapped != null) {
                    Point2D snapPos = getPage().getSnapPosition(selection);
                    g.setComposite(alphaComposite40);
                    Rectangle2D r = new Rectangle2D.Double(snapPos.getX(), snapPos.getY(), snapCard.getWidth(), snapCard.getHeight());
                    Area union = new Area(overlapped.getRectangle());
                    union.add(new Area(r));

                    g.setColor(SILHOUETTE);
                    g.fill(union);
                    g.setColor(SILHOUETTE_OUTLINE);
                    g.fill(r);
                    g.setComposite(composite);
                    g.setStroke(SILHOUETTE_STROKE);
                    g.draw(union);
                }
            }
        }

        // draw selection over all cards
        if (selection != null) {
            g.setPaint(SELECTION);
            g.setStroke(SELECTION_STROKE);
            g.setComposite(alphaComposite67);

            deferredPaintGroup.clear();
            for (int i = 0; i < selection.length; ++i) {
                PageItem c = selection[i];
                if (c.getGroup() != null) {
                    deferredPaintGroup.add(c.getGroup());
                } else {
                    g.draw(selection[i].getOutline());
                }
                if (i == selection.length - 2) {
                    g.setPaint(SELECTION_TAIL);
                }
            }
            if (!deferredPaintGroup.isEmpty()) {
                Group finalGroup = selection[selection.length - 1].getGroup();
                for (Group group : deferredPaintGroup) {
                    g.setPaint(finalGroup == group ? SELECTION_TAIL : SELECTION);
                    g.draw(group.getRectangle());
                }
            }
            g.setComposite(composite);
        }

        // draw handle drag effects
        if (dragHandle != null) {
            g.setColor(ACTIVE_HANDLE);
            g.setStroke(HANDLE_STROKE);
            dragHandle.paintDragState(g);
        }

        // if dragging a selection box, draw it
        if (selectDrag) {
            g.setColor(SELECTION);
            g.setStroke(HANDLE_STROKE);
            g.draw(selectDragRect);
        }

        paintDragAndDropCards(g, scale * 72d, quality, alphaComposite40, composite);

        // draw view clip for debugging (use negative markAdj to make visible)
//		g.setPaint( Color.RED );
//		g.draw( viewClip );
        frameTime = System.currentTimeMillis() - frameTime;
        if (DEBUG_DRAW) {
            double time = frameTime;
            if (time == 0d) {
                time = 1d;
            }
            String debug = String.format("X: %.1f   Y: %.1f  Scale: %.2f%%  Res: %.0f ppi  FPS: %.1f",
                    tx, ty, scale * 100f, scale * 72d, 1000d / frameTime
            );
            g.setTransform(oldAT);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
            g.setColor(Color.BLACK);
            g.drawString(debug, 14, 14);
            g.drawString(debug, 14, 16);
            g.drawString(debug, 16, 14);
            g.drawString(debug, 16, 16);
            g.setColor(Color.WHITE);
            g.drawString(debug, 15, 15);
        }
    }

    private final LinkedHashSet<Group> deferredPaintGroup = new LinkedHashSet<>();

    private void paintDragAndDropCards(Graphics2D g, double viewDPI, RenderTarget quality, Composite alphaComposite, Composite composite) {
        // if drag-dropping a card(s) over the page,
        //  draw the dragged card and its snap shadow
        if (droppablePageItem != null) {
            Point p = getMousePosition();
            if (p != null) {
                PageItem c = droppablePageItem;
                Point2D pos = viewToDocument(p.x, p.y);
                c.setX(pos.getX());
                c.setY(pos.getY());
                Rectangle2D.Double r = new Rectangle2D.Double(pos.getX(), pos.getY(), c.getWidth(), c.getHeight());

                PageItem snapCard = page.getCardToSnapTo(c, c.getClassesSnappedTo());
                if (snapCard != null) {
                    Point2D snapPos = page.getSnapPosition(new PageItem[]{c});
                    r.x = snapPos.getX();
                    r.y = snapPos.getY();

                    Area union = new Area(snapCard.getOutline());
                    union.add(new Area(r));
                    g.setComposite(alphaComposite);
                    g.setColor(SILHOUETTE);
                    g.fill(union);
                    g.setColor(SILHOUETTE_OUTLINE);
                    g.fill(r);
                    g.setComposite(composite);
                    g.setStroke(SILHOUETTE_STROKE);
                    g.draw(union);
                }
                c.setX(pos.getX());
                c.setY(pos.getY());
                paintPageItem(g, c, viewDPI, quality);
                g.setColor(SELECTION);
                g.setStroke(SELECTION_STROKE);
                g.draw(c.getOutline());
//				r.setFrame(pos.getX(), pos.getY(), c.getWidth(), c.getHeight());
//				g.draw(r);
            }
        }
    }

    private void paintPageItem(Graphics2D g, PageItem c, double viewDPI, RenderTarget quality) {
        if (viewClip.intersects(c.getRectangle())) {
            c.paint(g, quality, viewDPI);
        }
    }

    private void clearSelection() {
        getPage().getDeck().clearSelection();
    }

    public void addToSelection(PageItem card) {
        if (!card.isSelectionLocked()) {
            getPage().getDeck().addToSelection(card);
        }
    }

    public void removeFromSelection(PageItem card) {
        getPage().getDeck().removeFromSelection(card);
    }

    public void setSelection(PageItem card) {
        if (!card.isSelectionLocked()) {
            getPage().getDeck().clearSelection();
            getPage().getDeck().addToSelection(card);
        }
    }

    public boolean isSelected(PageItem card) {
        return getPage().getDeck().isSelected(card);
    }
    public final static Color PAGE = Color.WHITE;
    public final static Color BORDER = Color.GRAY;
    public final static Color MARGIN = new Color(0xee_f7f7);
    public final static Color GRID = new Color(0x88_9cbc);
    public final static Color CROP = new Color(0x44_4466);
    public final static Color INACTIVE_HANDLE = new Color(0x33_4a8a);
    public final static Color ACTIVE_HANDLE = new Color(0x88_9cbc);
    public final static Color GRID_OUTLINE = Color.DARK_GRAY;
    public final static Color SELECTION = new Color(0xe5_7a00);
    public final static Color SELECTION_TAIL = SELECTION.brighter();
    public final static Color SILHOUETTE = Color.BLACK;
    public final static Color SILHOUETTE_OUTLINE = new Color(0x00_44ae);
    private final static Stroke GRID_OUTLINE_STROKE = new BasicStroke(2f);
    private final static Stroke GRID_MINOR_STROKE = new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0f, new float[]{1f, 3f}, 0f);
    private final static Stroke GRID_MAJOR_STROKE = new BasicStroke(1f);
    private final static Stroke SILHOUETTE_STROKE = new BasicStroke(4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
    private final static Stroke SELECTION_STROKE = new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0f);
    private final static Stroke HANDLE_STROKE = GRID_OUTLINE_STROKE;
    private Page page;
    private double tx = 0d, ty = 0d, scale = 1d, cx, cy;

    /**
     * Adjusts the zoom level of the view by a number of "ticks", as if the user
     * had rotated a mouse wheel the specified number of steps.
     *
     * @param ticks the number of ticks to adjust the zoom level by
     */
    public void adjustZoomBySteps(int ticks) {
        changeScale(scaleSelector.select(scale, ticks), true);
    }

    /**
     * Returns the zoom factor of the view; this is a positive value where 1
     * represents 100% zoom.
     *
     * @return the scaling factor
     */
    public double getZoom() {
        return scale;
    }

    /**
     * Changes the zoom factor to the specified level. If the zoom level is out
     * of range, it will be clamped to the nearest acceptable value.
     *
     * @param newScale the new scale factor
     */
    public void setScale(double newScale) {
        if (newScale < STANDARD_ZOOM_LEVELS[0]) {
            newScale = STANDARD_ZOOM_LEVELS[0];
        } else if (newScale > STANDARD_ZOOM_LEVELS[STANDARD_ZOOM_LEVELS.length - 1]) {
            newScale = STANDARD_ZOOM_LEVELS[STANDARD_ZOOM_LEVELS.length - 1];
        }
        changeScale(newScale, false);
    }

    private static final double[] STANDARD_ZOOM_LEVELS = new double[]{
        4.6875d / 72d, 9.375d / 72d, 18.75d / 72d, 37.5d / 72d,
        1d, // should be 75/72 ~= 1.042, but uses 1 so that 1 pixel = 1 pt at 100%
        150d / 72d, 300d / 72d, 450d / 72d, 600d / 72d, 750d / 72d, 900d / 72d, 1050d / 72d, 1200d / 72d
    };
    // these zoom steps have been selected to optimize performance with
    // components that have template images that are a multiple of 150
    private static final StepSelector scaleSelector = new StepSelector(STANDARD_ZOOM_LEVELS);
    private static final StepSelector gestureScaleSelector;

    static {
        double[] levels = new double[(STANDARD_ZOOM_LEVELS.length - 1) * 3 + 1];
        levels[0] = STANDARD_ZOOM_LEVELS[0];
        for (int i = 1, j = 1; i < STANDARD_ZOOM_LEVELS.length; ++i) {
            levels[j++] = Interpolation.lerp(0.333d, STANDARD_ZOOM_LEVELS[i - 1], STANDARD_ZOOM_LEVELS[i]);
            levels[j++] = Interpolation.lerp(0.667d, STANDARD_ZOOM_LEVELS[i - 1], STANDARD_ZOOM_LEVELS[i]);
            levels[j++] = STANDARD_ZOOM_LEVELS[i];
        }
        gestureScaleSelector = new StepSelector(levels);
    }

    private void changeScale(final double newScale, boolean animate) {
        if (scale == newScale) {
            return;
        }

        // if view is showing, animate the scale change
        if (animate && isShowing()) {
            final double oldScale = scale;
            Animation zoomAnim = new Animation(0.25f) {
                @Override
                public void composeFrame(float position) {
                    if (position == 0) {
                        return;
                    }
                    scale = Interpolation.lerp(position, oldScale, newScale);
                    Point2D cb = viewToDocument(getWidth() / 2, getHeight() / 2);
                    changeView();
                    Point2D ca = viewToDocument(getWidth() / 2, getHeight() / 2);
                    tx += ca.getX() - cb.getX();
                    ty += ca.getY() - cb.getY();
                    changeView();
                }
            };
            new TimeShiftedComposer(zoomAnim);
            zoomAnim.play(this);
        } else {
            scale = newScale;
            Point2D cb = viewToDocument(getWidth() / 2, getHeight() / 2);
            changeView();
            Point2D ca = viewToDocument(getWidth() / 2, getHeight() / 2);
            tx += ca.getX() - cb.getX();
            ty += ca.getY() - cb.getY();
            changeView();
        }
    }

    public void centerOver(double x, double y) {
        Point2D center = viewToDocument(getWidth() / 2, getHeight() / 2);
        tx += center.getX() - x;
        ty += center.getY() - y;
        changeView();
    }

    public void setViewLocation(double x, double y) {
        tx = x;
        ty = y;
        changeView();
    }

    public Point2D getViewLocation() {
        return new Point2D.Double(tx, ty);
    }

    public double getViewX() {
        return tx;
    }

    public double getViewY() {
        return ty;
    }

    public void panView(double dx, double dy) {
        tx += dx;
        ty += dy;
        changeView();
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
        PaperProperties pp = page.getDeck().getPaperProperties();
        tx = 8;
        ty = 8;

        changeView();
        if (isVisible()) {
            repaint();
        }
        updateCropMarkManager();
    }

    private double pageWidth() {
        return page.getDeck().getPaperProperties().getPageWidth();
    }

    private double pageHeight() {
        return page.getDeck().getPaperProperties().getPageHeight();
    }

    private double margin() {
        return page.getDeck().getPaperProperties().getMargin();
    }

    private double gridSeparation() {
        return page.getDeck().getPaperProperties().getGridSeparation();
    }

    private void changeView() {
        changeView(false);
    }

    private void changeView(boolean alreadyPulled) {
        worldToView = AffineTransform.getScaleInstance(scale, scale);
        worldToView.concatenate(AffineTransform.getTranslateInstance(tx, ty));

        viewToWorld = AffineTransform.getTranslateInstance(-tx, -ty);
        viewToWorld.concatenate(AffineTransform.getScaleInstance(1d / scale, 1d / scale));

        if (page == null || page.getDeck() == null) {
            repaint();
            return;
        }

        // check if the new position needs to be pulled back into frame
        boolean pulled = false;
        double pullMargin = Math.max(margin(), 24);
        if (!alreadyPulled) {
            if (tx < -(pageWidth() - pullMargin)) {
                pulled = true;
                tx = -(pageWidth() - pullMargin);
            }
            if (ty < -(pageHeight() - pullMargin)) {
                pulled = true;
                ty = -(pageHeight() - pullMargin);
            }

            Point2D p = viewToDocument(getWidth(), getHeight());
            double sx = p.getX() - pullMargin, sy = p.getY() - pullMargin;
            if (sx < 0) {
                tx += sx;
                pulled = true;
            }
            if (sy < 0) {
                ty += sy;
                pulled = true;
            }
        }

        if (pulled) {
            changeView(true);
        } else {
            repaint();
        }
    }
    private AffineTransform viewToWorld, worldToView;

    /**
     * Scrolls the view, if necessary, to make the specified rectangle visible.
     * There is a limit to how much the view can be scrolled, as the view
     * ensures that some edge of the page is always visible. Therefore, it is
     * possible that the object will still not be visible after this is called.
     *
     * @param rectInDocumentSpace the rectangle to make visible, in document
     * space (points from the upper-left corner of the page)
     */
    public void ensureVisible(Rectangle2D rectInDocumentSpace) {
        if (rectInDocumentSpace == null) {
            return;
        }

        final double rx1 = rectInDocumentSpace.getX();
        final double rx2 = rectInDocumentSpace.getX() + rectInDocumentSpace.getWidth();
        final double ry1 = rectInDocumentSpace.getY();
        final double ry2 = rectInDocumentSpace.getY() + rectInDocumentSpace.getHeight();
        // determine area covered by view in document space
        final Rectangle2D.Double view = viewToDocument(new Rectangle(0, 0, getWidth(), getHeight()));
        final double vx1 = view.x;
        final double vx2 = view.x + view.width;
        final double vy1 = view.y;
        final double vy2 = view.y + view.height;

        double otx = tx, oty = ty;
        if (rx2 > vx2) {
            tx += (vx2 - rx2);
        }
        if (rx1 < vx1) {
            tx += (vx1 - rx1);
        }
        if (ry2 > vy2) {
            ty += (vy2 - ry2);
        }
        if (ry1 < vy1) {
            ty += (vy1 - ry1);
        }
        if (otx != tx || oty != ty) {
            changeView();
        }
    }

    private void mouseDragged(MouseEvent e) {
        updateCursor(e.getPoint());

        if ((e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) != 0) {
            return;
        }

        final int x = e.getX(), y = e.getY();
        final int dx = x - dragX;
        final int dy = y - dragY;

        // check if pointer is out of window, and if so scroll the view
        boolean pageHasNoSelection = getPage().getDeck().getSelectionPage() != getPage();
        double scrollScale = 1d / scale;
        double xPan = 0d, yPan = 0d;

        // if user is dragging the paper, don't scroll here or we will "double scroll"
        boolean draggingPaper = dragHandle == null && !selectDrag && (pageHasNoSelection || e.isAltDown() || e.isAltGraphDown());
        if (draggingPaper) {
            scrollScale = -scrollScale;
        }

        if (x > getWidth()) {
            xPan = (getWidth() - x) * scrollScale;
            double temp = tx;
            panView(xPan, 0d);
            xPan = tx - temp; // the *actual* amount scrolled (may be less than requested)
        } else if (x < 0) {
            xPan = (-x) * scrollScale;
            double temp = tx;
            panView(xPan, 0d);
            xPan = tx - temp;
        }
        if (y > getHeight()) {
            yPan = (getHeight() - y) * scrollScale;
            double temp = ty;
            panView(0d, yPan);
            yPan = ty - temp;
        } else if (y < 0) {
            yPan = (-y) * scrollScale;
            double temp = ty;
            panView(0d, yPan);
            yPan = ty - temp;
        }
        // do the actual dragging

        if (dragHandle != null) {
            dragHandle.drag(viewToDocument(x, y), e);
            repaint();
        } else if (selectDrag) {
            selectDragRect.x = selectDragStart.getX();
            selectDragRect.y = selectDragStart.getY();
            selectDragRect.width = 0d;
            selectDragRect.height = 0d;
            selectDragRect.add(viewToDocument(x, y));
            repaint();
        } else if (draggingPaper) {
            tx += dx / scale;
            ty += dy / scale;
            changeView();
        } else {
            if (!isDragging) {
                setOptionText(string("de-l-opt-dragging"));
            }
            PageItem[] selection = getPage().getDeck().getSelection();
            if (dragSelectionOriginalPositions == null) {
                dragSelectionOriginalPositions = new Point2D[selection.length];
                for (int i = 0; i < selection.length; ++i) {
                    dragSelectionOriginalPositions[i] = new Point2D.Double(selection[i].getX(), selection[i].getY());
                }
            }

            // xPan and yPan are the amount we scrolled the view by because the
            // user dragged the pointer past the window edge; we need to add this
            // in to the amount we translate the objects by or else the objects
            // will not stay "attached" to the pointer --- basically, when we
            // move the pointer to the window edge, the objects stop moving and
            // we move the paper relative to the objects instead
            for (PageItem c : selection) {
                c.setX(c.getX() + dx / scale - xPan);
                c.setY(c.getY() + dy / scale - yPan);
            }
            repaint();
        }

        isDragging = true;
        dragX = x;
        dragY = y;
        getPage().getDeck().fireSelectionChanged();
    }
    private boolean isDragging;
    private int dragX, dragY;

    private void mouseReleased(MouseEvent e) {
        boolean command = e.isControlDown() || e.isMetaDown();
        boolean alt = e.isAltDown() || e.isAltGraphDown();
        boolean shift = e.isShiftDown();

        // if dragging a group of cards, we can now "forget" their starting points
        // as it is too late for the user to cancel now
        dragSelectionOriginalPositions = null;

        if (dragHandle != null) {
            if (dragHandle.handleMovedDuringDrag()) {
                dragHandle.endDrag();
                getPage().getDeck().markUnsavedChanges();
            } else {
                dragHandle.cancelDrag();
            }
            dragHandle = null;
            // set this even if the handle didn't actually move so
            // that the state is reset and the view is repainted
            isDragging = true;
        }

        if (isDragging) {
            if (selectDrag) {
                for (int i = 0; i < page.getCardCount(); ++i) {
                    PageItem pi = page.getCard(i);
                    if (selectDragRect.contains(pi.getRectangle())) {
                        addToSelection(pi);
                    }
                }
            } else if (getPage().getDeck().getSelectionPage() == getPage()) {
                if (!command) {
                    page.snapCard(getPage().getDeck().getSelection());
                }
                getPage().getDeck().markUnsavedChanges();
            }
            isDragging = false;
            selectDrag = false;
            setDefaultOptionText();
            repaint(); // redraw with anti-aliasing
            getPage().getDeck().fireSelectionChanged();
        }

        if (e.isPopupTrigger()) {
            showPopupMenu(e, page.getCardAtLocation(viewToDocument(e.getX(), e.getY())));
        }
    }
    private Point2D selectDragStart;
    private final Rectangle2D.Double selectDragRect = new Rectangle2D.Double();

    private void mouseClicked(MouseEvent e) {
        boolean command = e.isControlDown() || e.isMetaDown();
        boolean alt = e.isAltDown() || e.isAltGraphDown();
        boolean shift = e.isShiftDown();

        if (e.getButton() == MouseEvent.BUTTON1 && editable) {
            Point2D loc = viewToDocument(e.getX(), e.getY());
            PageItem c = page.getCardAtLocation(loc);
            if ((c != null) && !shift && !command && (getDragHandleUnderPosition(e.getPoint()) == null)) {
                setSelection(c);
            }
        }
    }

    private void mousePressed(MouseEvent e) {
        boolean command = e.isControlDown() || e.isMetaDown();
        boolean alt = e.isAltDown() || e.isAltGraphDown();
        boolean shift = e.isShiftDown();

        TextBox.cancelActiveEditor();
        requestFocusInWindow();
        getPage().getDeck().setActivePage(getPage());

        dragX = e.getX();
        dragY = e.getY();

        Point2D loc = viewToDocument(e.getX(), e.getY());
        PageItem c = page.getCardAtLocation(loc);
        if (!editable) {
            c = null;
        }

        // convert middle button to drag select
        if (e.getButton() == MouseEvent.BUTTON2) {
            e = new MouseEvent(this, e.getID(), e.getWhen(),
                    MouseEvent.SHIFT_DOWN_MASK,
                    e.getX(), e.getY(), 1, false, MouseEvent.BUTTON1);
            c = null;
            shift = true;
        }

        if (e.getButton() == MouseEvent.BUTTON1) {
            DragHandle handle = getDragHandleUnderPosition(e.getPoint());
            if (handle != null && isEditable()) {
                dragHandle = handle;
                dragHandle.beginDrag(loc, e);
                setSelection(dragHandle.getOwner());
                updateCursor(e.getPoint());
            } else if (c == null || c.isSelectionLocked()) {
                if (!shift && !command) {
                    clearSelection();
                    clearOptionText();
                } else {
                    if (isEditable()) {
                        selectDrag = true;
                        selectDragStart = loc;
                        selectDragRect.x = selectDragStart.getX();
                        selectDragRect.y = selectDragStart.getY();
                        selectDragRect.width = 0d;
                        selectDragRect.height = 0d;
                    }
                }
            } else {
                if (e.getClickCount() == 2) {
                    if (c instanceof EditablePageItem) {
                        ((EditablePageItem) c).beginEditing();
                    } else if (c instanceof Style && Commands.EDIT_STYLE.isDefaultActionApplicable()) {
                        Commands.EDIT_STYLE.performDefaultAction(new ActionEvent(this, 0, null));
                    }
                } else if (shift) {
                    addToSelection(c);
                } else if (command) {
                    if (getPage().getDeck().isSelected(c)) {
                        removeFromSelection(c);
                    } else {
                        addToSelection(c);
                    }
                } else if (!getPage().getDeck().isSelected(c)) {
                    // make sure card selected in case we are about to drag it
                    setSelection(c);
                }
            }
        }
        repaint();

        if (e.isPopupTrigger() && !isDragging) {
            showPopupMenu(e, c);
        }
    }

    private void showPopupMenu(MouseEvent event, PageItem cardUnderMouse) {
        if (!editable) {
            return;
        }
        Deck deck = getPage().getDeck();
        if (cardUnderMouse != null && getPage().getDeck().getSelectionPage() != getPage()) {
            deck.setSelection(cardUnderMouse);
        }

        createMergedPopupMenu(deck.getSelection(), event.getPoint()).show(this, event.getX(), event.getY());
    }

    private JPopupMenu createMergedPopupMenu(PageItem[] selection, Point point) {
        // Popup menu when no selection and not over a card
        if (selection.length == 0) {
            JPopupMenu menu = new JPopupMenu();
            menu.add(Commands.CENTER_CONTENT);
            menu.add(Commands.SELECT_ALL);
            return menu;
        }

        JPopupMenu menu = new JPopupMenu();

        menu.add(Commands.EDIT_PAGE_ITEM);
        menu.add(Commands.EDIT_STYLE);
        menu.addSeparator();

        menu.add(Commands.TO_FRONT);
        menu.add(Commands.TO_BACK);
        menu.addSeparator();

        menu.add(Commands.GROUP);
        menu.add(Commands.UNGROUP);
        menu.addSeparator();

        menu.add(Commands.LOCK);
        menu.add(Commands.UNLOCK);
        menu.addSeparator();

        // Add menu item/submenu for selecting objects under the cursor
        PageItem[] selectables = getPage().getAllCardsAtLocation(viewToDocument(point.x, point.y));
        if (selectables.length > 1) {
            JMenu selectMenu = new JMenu(string("select"));
            for (PageItem i : selectables) {
                selectMenu.add(createSelectionItem(i));
            }
            menu.add(selectMenu);
        } else if (selectables.length == 1 && !getPage().getDeck().isSelected(selectables[0])) {
            JMenuItem selectMenu = createSelectionItem(selectables[0]);
            selectMenu.setText(string("select-single", selectables[0].getName()));
            menu.add(selectMenu);
        }
        menu.add(Commands.SELECT_ALL);
        menu.add(Commands.SELECT_INVERSE);
        menu.add(Commands.SELECT_RESTORE);
        menu.addSeparator();

        menu.add(Commands.CUT);
        menu.add(Commands.COPY);
        menu.add(Commands.PASTE);
//		menu.addSeparator();

        // enable/disable the commands
        int items = menu.getComponentCount();
        for (int i = 0; i < items; ++i) {
            Component c = menu.getComponent(i);
            if (c instanceof JMenuItem) {
                JMenuItem mi = (JMenuItem) c;
                if (mi.getAction() != null && mi.getAction() instanceof AbstractCommand) {
                    AbstractCommand cmd = (AbstractCommand) mi.getAction();
                    cmd.update();
                    if (cmd.getAccelerator() == null) {
                        KeyStroke viewKey = (KeyStroke) cmd.getValue(PAGE_VIEW_ACTION_KEY);
                        if (viewKey != null) {
                            mi.setAccelerator(viewKey);
                        }
                    }
                    ((AbstractCommand) mi.getAction()).update();
                }
            }
        }

        // give the selection a chance to customize the menus
        for (int i = 0; i < selection.length; ++i) {
            selection[i].customizePopupMenu(menu, selection, i == selection.length - 1);
        }

        return menu;
    }

    /**
     * Creates a menu item that will select a deck item if selected; if Shift is
     * down it will add to the selection; if Ctrl is down it will toggle
     * selection; if neither is down it will replace the selection.
     *
     * @param selectee the object that will be selected
     * @return a menu item for selecting the object
     */
    private JMenuItem createSelectionItem(final PageItem selectee) {
        JMenuItem item = new JMenuItem(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Deck d = getPage().getDeck();
                int mod = e.getModifiers();
                if ((mod & ActionEvent.SHIFT_MASK) != 0) {
                    d.addToSelection(selectee);
                } else if ((mod & ActionEvent.CTRL_MASK) != 0) {
                    if (d.isSelected(selectee)) {
                        d.removeFromSelection(selectee);
                    } else {
                        d.addToSelection(selectee);
                    }
                } else if ((mod & ActionEvent.ALT_MASK) != 0) {
                    d.selectAll(getPage());
                    d.removeFromSelection(selectee);
                } else {
                    d.setSelection(selectee);
                }
            }
        });
        String name = selectee.getName();
        if (selectee instanceof CustomTile) {
            String file = ((CustomTile) selectee).getIdentifier();
            int fileStart = Math.max(file.lastIndexOf('/'), file.lastIndexOf(File.separatorChar));
            name = name + " (" + file.substring(fileStart + 1) + ')';
        } else if (selectee instanceof TextBox) {
            String text = AbstractGameComponent.filterComponentText(((TextBox) selectee).getText());
            int chars = Math.min(text.length(), 20);
            name = name + " (" + text.substring(0, chars) + (chars == text.length() ? ")" : "...)");
        }
        item.setText(name);
        if (selectee.isSelectionLocked()) {
            if (selectionItemLock == null) {
                selectionItemLock = ResourceKit.getIcon("ui/locked.png");
            }
            item.setIcon(selectionItemLock);
        }
        return item;
    }
    private static Icon selectionItemLock;

    // stores the new width and height a card will have if the user stops resizing it now
    private double resizeWidth, resizeHeight;
    // stores the handle currently being dragged
    private DragHandle dragHandle = null;
    private boolean selectDrag = false;
    /**
     * Copy of the selection when a drag of cards begins.
     */
    private Point2D[] dragSelectionOriginalPositions;

    /**
     * Returns the position within the page for a given point in the view
     * component.
     *
     * @param x the x-offset from the left edge of the component
     * @param y the y-offset from the top of the component
     * @return the location of the point {@code (x,y)} in the document
     * space
     */
    public Point2D.Double viewToDocument(int x, int y) {
        Point2D.Double dst = new Point2D.Double();
        viewToWorld.transform(
                new Point2D.Double(x, y),
                dst);
        return dst;
    }

    /**
     * Returns a rectangle in document coordinates that covers the same area of
     * the document space as a rectangle in view coordinates covers in the view.
     *
     * @param viewRectangle a rectangle in view coordinates
     * @return the rectangle of document space covered by the view rectangle
     */
    public Rectangle2D.Double viewToDocument(Rectangle viewRectangle) {
        Point2D.Double xy = viewToDocument(viewRectangle.x, viewRectangle.y);
        // convert width, height to a point before transformation, then back to w, h after (for rounding)
        Point2D.Double wh = viewToDocument(viewRectangle.x + viewRectangle.width, viewRectangle.y + viewRectangle.height);
        return new Rectangle2D.Double(xy.x, xy.y, wh.x - xy.x, wh.y - xy.y);
    }

    /**
     * Returns the position within the view component for a given point in the
     * page coordinates.
     *
     * @param x the x-offset from the left edge of the page
     * @param y the y-offset from the top of the page
     * @return the location of the point {@code (x,y)} in the view
     */
    public Point documentToView(double x, double y) {
        Point2D dst = new Point2D.Double();
        worldToView.transform(new Point2D.Double(x, y), dst);
        return new Point(
                (int) Math.round(dst.getX()),
                (int) Math.round(dst.getY()));
    }

    /**
     * Returns a rectangle in view coordinates that covers a rectangle in
     * document coordinates. Note that the returned rectangle is not restricted
     * the visible part of the view.
     *
     * @param documentRectangle a rectangle in document space
     * @return the rectangle of view space covered by the document rectangle
     */
    public Rectangle documentToView(Rectangle2D documentRectangle) {
        Point xy = documentToView(documentRectangle.getX(), documentRectangle.getY());
        // convert width, height to a point before transformation, then back to w, h after (for rounding)
        Point wh = documentToView(documentRectangle.getX() + documentRectangle.getWidth(), documentRectangle.getY() + documentRectangle.getHeight());
        return new Rectangle(xy.x, xy.y, wh.x - xy.x, wh.y - xy.y);
    }

    public void pageChangeEvent() {
        setDefaultOptionText();
        repaint();
    }

    private void keyPressed(KeyEvent e) {
        KeyStroke pressed = KeyStroke.getKeyStrokeForEvent(e);
        for (int i = 0; i < keyableDeckCommands.length; ++i) {
            final AbstractCommand c = keyableDeckCommands[i];
            if (pressed.equals(c.getValue(PAGE_VIEW_ACTION_KEY))) {
                e.consume();
                c.update();
                if (c.isEnabled()) {
                    c.actionPerformed(new ActionEvent(this, 0, (String) c.getValue(AbstractCommand.ACTION_COMMAND_KEY)));
                } else {
                    UIManager.getLookAndFeel().provideErrorFeedback(this);
                }
                return;
            }
        }

        DeckEditor ed;
        Deck deck = getPage().getDeck();
        int vk = e.getKeyCode();

        boolean command = e.isControlDown() || e.isMetaDown();
        boolean alt = e.isAltDown() || e.isAltGraphDown();
        boolean isPanTrigger = alt || !isEditable();
        boolean shift = e.isShiftDown();

        double nudgeSize = 1d;
        if (command || shift) {
            nudgeSize = deck.getPaperProperties().getGridSeparation();
            if (command) {
                nudgeSize /= 2;
            }
        }

        switch (vk) {
            case KeyEvent.VK_ESCAPE:
                if (dragHandle != null) {
                    dragHandle.cancelDrag();
                    dragHandle = null;
                    isDragging = false;
                    repaint();
                    e.consume();
                } else if (isDragging) {
                    selectDrag = false;
                    isDragging = false;
                    repaint();
                    e.consume();
                } else if (dragSelectionOriginalPositions != null) {
                    PageItem[] selection = deck.getSelection();
                    for (int i = 0; i < selection.length; ++i) {
                        selection[i].setLocation(dragSelectionOriginalPositions[i]);
                    }
                    repaint();
                    e.consume();
                }
                break;
            case KeyEvent.VK_DELETE:
            case KeyEvent.VK_BACK_SPACE:
                if (command) {
                    getEditor().removeCurrentPage();
                } else {
                    deck.deleteSelection();
                }
                e.consume();
                break;

            case KeyEvent.VK_UP:
            case KeyEvent.VK_KP_UP:
                if (isPanTrigger) {
                    panView(0d, nudgeSize);
                } else {
                    deck.nudgeSelection(0d, -nudgeSize);
                    ensureVisible(deck.getSelectionRectangle());
                }
                e.consume();
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_KP_DOWN:
                if (isPanTrigger) {
                    panView(0d, -nudgeSize);
                } else {
                    deck.nudgeSelection(0d, nudgeSize);
                    ensureVisible(deck.getSelectionRectangle());
                }
                e.consume();
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_KP_LEFT:
                if (isPanTrigger) {
                    panView(nudgeSize, 0d);
                } else {
                    deck.nudgeSelection(-nudgeSize, 0d);
                    ensureVisible(deck.getSelectionRectangle());
                }
                e.consume();
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_KP_RIGHT:
                if (isPanTrigger) {
                    panView(-nudgeSize, 0d);
                } else {
                    deck.nudgeSelection(nudgeSize, 0d);
                    ensureVisible(deck.getSelectionRectangle());
                }
                e.consume();
                break;

//			case KeyEvent.VK_T:
//				if( command ) {
//					ed = getEditor();
//					if( ed != null ) {
//						ed.addPage();
//						e.consume();
//					}
//				}
//				break;
            case KeyEvent.VK_PAGE_UP:
                ed = getEditor();
                if (ed != null) {
                    int p = getEditor().getSelectedPageIndex() - 1;
                    if (p < 0) {
                        p = deck.getPageCount() - 2;
                    }
                    ed.setCurrentPage(p);
                    ed.addPage();
                    e.consume();
                }
                break;
            case KeyEvent.VK_PAGE_DOWN:
                ed = getEditor();
                if (ed != null) {
                    int p = getEditor().getSelectedPageIndex() + 1;
                    if (p > deck.getPageCount() - 2) {
                        p = 0;
                    }
                    ed.setCurrentPage(p);
                    ed.addPage();
                    e.consume();
                }
                break;
            case KeyEvent.VK_SPACE:
                Point pos = getMousePosition();
                if (pos != null) {
                    Point2D doc = viewToDocument(pos.x, pos.y);
                    centerOver(doc.getX(), doc.getY());
                    changeScale(150d / 72d, true);
                } else {
                    getToolkit().beep();
                }
                e.consume();
                break;
            case KeyEvent.VK_MINUS:
            case KeyEvent.VK_UNDERSCORE:
                adjustZoomBySteps(-1);
                e.consume();
                break;
            case KeyEvent.VK_PLUS:
            case KeyEvent.VK_EQUALS:
                adjustZoomBySteps(1);
                e.consume();
                break;
            case KeyEvent.VK_0:
            case KeyEvent.VK_1:
            case KeyEvent.VK_2:
            case KeyEvent.VK_3:
            case KeyEvent.VK_4:
            case KeyEvent.VK_5:
            case KeyEvent.VK_6:
            case KeyEvent.VK_7:
            case KeyEvent.VK_8:
            case KeyEvent.VK_9:
                int group = vk - KeyEvent.VK_0;
                if (command) {
                    deck.storeSelectionInNumberedGroup(group);
                } else {
                    deck.selectNumberedGroup(group);
                }
                e.consume();
                break;

            case KeyEvent.VK_H:
                Commands.VIEW_DECK_HANDLES.toggle();
                e.consume();
                break;
        }
    }

    private DeckEditor getEditor() {
        StrangeEonsEditor ed = StrangeEons.getWindow().getActiveEditor();
        if (ed != null && ed instanceof DeckEditor) {
            DeckEditor deckEd = (DeckEditor) ed;
            if (deckEd.getDeck() == getPage().getDeck()) {
                return deckEd;
            }
        }
        return null;
    }

    protected void setOptionText(String s) {
        if (optionLabel != null) {
            optionLabel.setText(s);
        }
    }

    protected void clearOptionText() {
        setOptionText(" ");
    }

    protected void setDefaultOptionText() {
        if (getPage().getDeck().getSelectionPage() == null) {
            clearOptionText();
        } else {
            setOptionText(string("de-l-opt-selecting"));
        }
    }

    public void setOptionLabel(JLabel label) {
        this.optionLabel = label;
    }

    public JLabel getOptionLabel() {
        return optionLabel;
    }
    private JLabel optionLabel;

//	private void dropFiles( List<File> files ) {
//		Point mouse = getMousePosition();
//		Point2D dropPoint = null;
//		if( mouse != null ) {
//			dropPoint = viewToDocument( mouse.x, mouse.y );
//		}
//		for( File file : files  ) {
//			if( ProjectUtilities.matchExtension( file, DROPPABLE_IMAGE_TYPES ) ) {
//				CustomTile ct = new CustomTile( file.getAbsolutePath(), 150 );
//				getPage().addCard( ct );
//				if( dropPoint != null ) {
//					ct.setLocation( dropPoint.getX() - ct.getWidth()/2d, dropPoint.getY() - ct.getHeight()/2d );
//					dropPoint.setLocation( dropPoint.getX() + 12, dropPoint.getY() + 12 );
//				}
//			} else {
//				StrangeEons.getWindow().openFile( file );
//			}
//		}
//	}
//	private TransferHandler itemDropHandler = new TransferHandler() {
//		@Override
//		public boolean importData( TransferHandler.TransferSupport support ) {
//			try {
//				Transferable t = support.getTransferable();
//				if( support.isDataFlavorSupported( DataFlavor.javaFileListFlavor ) ) {
//					dropFiles( (List<File>) t.getTransferData( DataFlavor.javaFileListFlavor ) );
//				} else if( t.isDataFlavorSupported( PageItemTransferable.DATA_FLAVOR ) ) {
//					droppablePageItem = null;
//
//					PageItem[] itemsToDrop = (PageItem[]) t.getTransferData( PageItemTransferable.DATA_FLAVOR );
//					Point dropLoc = support.getDropLocation().getDropPoint();
//					Point2D p = viewToDocument( dropLoc.x, dropLoc.y );
//
//					double cx = p.getX(), cy = p.getY();
//					boolean firstCard = true;
//					getPage().getDeck().clearSelection();
//
//					for( PageItem c : itemsToDrop  ) {
//						c.setX( cx );
//						c.setY( cy );
//						getPage().addCard( c, false );
//						if( firstCard ) {
//							getPage().snapCard( c );
//						}
//						cx = c.getX() + c.getWidth();
//						cy = c.getY();
//						getPage().getDeck().addToSelection( c );
//						firstCard = false;
//					}
//					requestFocusInWindow();
//					getPage().getDeck().markUnsavedChanges();
//				} else {
//					return false;
//				}
//			} catch( Exception e ) {
//				StrangeEons.log.log( Level.SEVERE, null, e );
//			}
////			putClientProperty( ContextBar.BAR_DISABLE_PROPERTY, null );
//			requestFocusInWindow();
//			return true;
//		}
//
//		@Override
//		public boolean canImport( TransferHandler.TransferSupport support ) {
//			boolean accept = false;
//			if( support.isDataFlavorSupported( DataFlavor.javaFileListFlavor ) ) {
//				accept = true;
//			}
//			if( support.isDataFlavorSupported( PageItemTransferable.DATA_FLAVOR ) ) {
//				accept = true;
//			}
//			// since canImport is called continuously during the drag, this will
//			// update the visual representation of the drop as the mouse moves
//			if( droppablePageItem != null ) {
//				repaint();
//			}
//			return accept;
//		}
//
//	};
//	@SuppressWarnings( "unchecked" )
//	private void drop( DropTargetDropEvent dtde ) {
//		try {
//			Transferable t = dtde.getTransferable();
//			if( dtde.isDataFlavorSupported( DataFlavor.javaFileListFlavor ) ) {
//				dtde.acceptDrop( dtde.getDropAction() );
//				dropFiles( (List<File>) t.getTransferData( DataFlavor.javaFileListFlavor ) );
//				dtde.dropComplete( true );
//				requestFocusInWindow();
//				return;
//			}
//
//			if( t.isDataFlavorSupported( PageItemTransferable.DATA_FLAVOR ) ) {
//				dtde.acceptDrop( dtde.getDropAction() );
//				droppablePageItem = null;
//
//				PageItem[] itemsToDrop = (PageItem[]) t.getTransferData( PageItemTransferable.DATA_FLAVOR );
//				Point loc = dtde.getLocation();
//				Point2D p = viewToDocument( loc.x, loc.y );
//
//				double cx = p.getX(), cy = p.getY();
//				boolean firstCard = true;
//				getPage().getDeck().clearSelection();
//
//				for( PageItem c : itemsToDrop  ) {
//					c.setX( cx );
//					c.setY( cy );
//					getPage().addCard( c, false );
//					if( firstCard ) {
//						getPage().snapCard( c );
//					}
//					cx = c.getX() + c.getWidth();
//					cy = c.getY();
//					getPage().getDeck().addToSelection( c );
//					firstCard = false;
//				}
//				dtde.dropComplete( true );
//				requestFocusInWindow();
//				getPage().getDeck().markUnsavedChanges();
//			}
//		} catch( Exception e ) {
//			StrangeEons.log.log( Level.SEVERE, null, e );
//		}
//		putClientProperty( ContextBar.BAR_DISABLE_PROPERTY, null );
//	}
    /**
     * Sets the item to display during a drop, or updates its position.
     *
     * @param dropItem
     */
    void setDropItem(PageItem dropItem) {
        droppablePageItem = dropItem;
        repaint();
    }

    /**
     * Drops the active drop item.
     */
    void drop(Point dropLoc) {
        Page page = getPage();
        Deck deck = page.getDeck();
        Point2D p = viewToDocument(dropLoc.x, dropLoc.y);

        double cx = p.getX(), cy = p.getY();
        getPage().getDeck().clearSelection();

        PageItem c = droppablePageItem.clone();
        c.setLocation(p);
        page.addCard(c, false);
        page.snapCard(c);
        deck.setSelection(c);
        deck.markUnsavedChanges();
        requestFocusInWindow();
    }

    /**
     * This is set during drag operations so that the item to be dropped can be
     * drawn over the view.
     */
    private static PageItem droppablePageItem;

//	private void dragOver( DropTargetDragEvent dtde ) {
//		putClientProperty( ContextBar.BAR_DISABLE_PROPERTY, null );
//
//		try {
//			if( dtde.isDataFlavorSupported( DataFlavor.javaFileListFlavor ) ) {
//				dtde.acceptDrag( DnDConstants.ACTION_COPY );
//				return;
//			}
//			updateDragVisual( dtde );
//		} catch( Exception e ) {
//			StrangeEons.log.log( Level.SEVERE, null, e );
//		}
//	}
//	private void dragEnter( DropTargetDragEvent dtde ) {
//		putClientProperty( ContextBar.BAR_DISABLE_PROPERTY, null );
//		try {
//			if( dtde.isDataFlavorSupported( DataFlavor.javaFileListFlavor ) ) {
//				dtde.acceptDrag( DnDConstants.ACTION_COPY );
//				return;
//			}
//			if( dtde.isDataFlavorSupported( PageItemTransferable.DATA_FLAVOR ) ) {
//				updateDragVisual( dtde );
//			} else {
//				dtde.rejectDrag();
//			}
//		} catch( Exception e ) {
//			StrangeEons.log.log( Level.SEVERE, null, e );
//		}
//	}
//	private void updateDragVisual( DropTargetDragEvent dtde ) {
////		try {
////			if( dtde.isDataFlavorSupported( PageItemTransferable.DATA_FLAVOR ) ) {
////				dtde.acceptDrag( DnDConstants.ACTION_COPY );
////				droppablePageItem = (Object[]) dtde.getTransferable().getTransferData( PageItemTransferable.DATA_FLAVOR );
////				repaint();
////			}
////		} catch( Exception e ) {
////			StrangeEons.log.log( Level.SEVERE, null, e );
////		}
//		repaint();
//		putClientProperty( ContextBar.BAR_DISABLE_PROPERTY, Boolean.TRUE );
//	}
//	private void dragExit( DropTargetEvent dte ) {
//		try {
//			clearOptionText();
//			droppablePageItem = null;
//			repaint();
//		} catch( Exception e ) {
//			StrangeEons.log.log( Level.SEVERE, null, e );
//		}
//	}
    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    private static final ContextBar.Locator BAR_LOCATOR = (ContextBar bar, int barWidth, int barHeight, JComponent target) -> {
        if (!(target instanceof PageView)) {
            return null;
        }
        
        PageView pv = (PageView) target;
        Deck d = pv.getPage().getDeck();
        
        if (!d.getSettings().getYesNo("show-context-bar-over-deck-selection")) {
            return null;
        }
        
        Rectangle selRect = null;
        // if there is a selection, float the bar around the selection
        if (d.getSelectionSize() > 0) {
//				selRect = pv.documentToView( d.getSelection()[ d.getSelectionSize()-1 ].getRectangle() );
selRect = pv.documentToView(d.getSelectionRectangle());

// clip the selection rectangle to the visible part of the page
if (selRect.x + barWidth > pv.getWidth()) {
    selRect.x = pv.getWidth() - barWidth;
}
if (selRect.y + barHeight > pv.getHeight()) {
    selRect.y = pv.getHeight(); /* - barHeight; */ // done when moving window "above" the rectangle
}
if (selRect.x < 0) {
    selRect.x = 0;
}
if (selRect.y < 0) {
    selRect.y = 0;
}

// convert to screen coordinates
Point sp = target.getLocationOnScreen();
selRect.x += sp.x;
selRect.y += sp.y;
        }
        return selRect;
    };

    private static final AbstractCommand[] keyableDeckCommands = new AbstractCommand[]{
        Commands.TO_FRONT, Commands.TO_BACK, Commands.GROUP, Commands.UNGROUP,
        Commands.ALIGN_LEFT, Commands.ALIGN_CENTER, Commands.ALIGN_RIGHT, Commands.ALIGN_TOP, Commands.ALIGN_MIDDLE, Commands.ALIGN_BOTTOM,
        Commands.TURN_LEFT, Commands.TURN_RIGHT, Commands.TURN_180,
        Commands.FLIP_VERT, Commands.FLIP_HORZ,
        Commands.LOCK, Commands.UNLOCK, Commands.EDIT_PAGE_ITEM, Commands.EDIT_STYLE,
        Commands.COPY_STYLE, Commands.PASTE_STYLE, Commands.SELECT_INVERSE, Commands.SELECT_RESTORE
    };

}
