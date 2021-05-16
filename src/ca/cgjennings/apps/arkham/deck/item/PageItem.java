package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.deck.*;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.EnumSet;
import javax.swing.Icon;
import javax.swing.JPopupMenu;

/**
 * An item that can be placed in a deck.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface PageItem extends Cloneable, Serializable {

    /**
     * The orientation of flippable cards that are not turned or mirrored.
     */
    public int ORIENT_UPRIGHT = 0;
    /**
     * The orientation of flippable cards that are turned 90 degrees
     * counter-clockwise.
     */
    public int ORIENT_TURN_LEFT = 1;
    /**
     * The orientation of flippable cards that are turned 180 degrees.
     */
    public int ORIENT_UPSIDEDOWN = 2;
    /**
     * The orientation of flippable cards that are turned 90 degrees clockwise.
     */
    public int ORIENT_TURN_RIGHT = 3;
    /**
     * The orientation of flippable cards that are mirror imaged.
     */
    public int ORIENT_MIRROR_UPRIGHT = 4;
    /**
     * The orientation of flippable cards that are turned 90 degrees
     * counter-clockwise and mirror imaged.
     */
    public int ORIENT_MIRROR_TURN_LEFT = 5;
    /**
     * The orientation of flippable cards that are turned 180 and mirror imaged.
     */
    public int ORIENT_MIRROR_UPSIDEDOWN = 6;
    /**
     * The orientation of flippable cards that are turned 90 degrees clockwise
     * and mirror imaged.
     */
    public int ORIENT_MIRROR_TURN_RIGHT = 7;

    /**
     * An enumeration of the general classes of items used to determine how an
     * item will behave when snapped and what it can snap against.
     */
    public enum SnapClass {
        /**
         * Class representing the gridlines on the deck page.
         */
        SNAP_PAGE_GRID,
        /**
         * Class representing card faces.
         */
        SNAP_CARD,
        /**
         * Class representing tiles.
         */
        SNAP_TILE,
        /**
         * Class representing overlays, e.g. misc decorations.
         */
        SNAP_OVERLAY,
        /**
         * Class representing inlays: overlays that are normally
         * {@code TARGET_MIXED}.
         */
        SNAP_INLAY,
        /**
         * Class representing default, other, or unknown objects.
         */
        SNAP_OTHER;

        /**
         * A {@link SnapClass} set for items that do not snap but are always
         * dropped in place. This set is shared; its contents <i>must not</i> be
         * modified.
         *
         * @see #setClassesSnappedTo
         */
        public static final EnumSet<SnapClass> SNAP_SET_NONE = EnumSet.noneOf(SnapClass.class);
        /**
         * A {@link SnapClass} set for items that will snap against anything.
         * This set is shared; its contents <i>must not</i> be modified.
         *
         * @see #setClassesSnappedTo
         */
        public static final EnumSet<SnapClass> SNAP_SET_ANY = EnumSet.allOf(SnapClass.class);
    };

    /**
     * An enumeration of the algorithms used to determine the position of an
     * item when it is snapped against another item.
     */
    public enum SnapTarget {
        /**
         * Items with this target snap to the outside of other items.
         */
        TARGET_OUTSIDE,
        /**
         * Items with this target snap to the inside of other items.
         */
        TARGET_INSIDE,
        /**
         * Items with this target snap to the inside of cards when they are of
         * different classes.
         */
        TARGET_MIXED
    }

    /**
     * Returns a new page item, using this item as a template. The new item
     * should generally be a deep copy, not sharing any objects with the
     * original unless those objects are immutable. The deck editor absolutely
     * relies on this method being implemented correctly! (For example,
     * drag-and-drop and clipboard operations use clones to create copies of
     * existing items.)
     *
     * @return a new copy of this page item
     */
    public PageItem clone();

    /**
     * Return an array of the relative positions and directions of extra fold
     * marks for this item. These fold marks are in addition to any fold marks
     * that are generated automatically based on the juxtaposition of
     * {@link CardFace} items. They are <i>intraitem</i> fold marks that are
     * added along the edges of some objects. For example, foldable tome leaves
     * always have a fold mark along their spine, which runs vertically down the
     * centre of the face.
     *
     * <p>
     * The returned array consists of two points for each mark in
     * (x1,y1),(x2,y2) order. (x1,y1) is a point relative to the width and
     * height of the card, e.g. 0.5, 0 is the center of the top edge. (x2,y2) is
     * a <b>unit vector</b> that indicates the direction of the line that should
     * be drawn from the first point. (This means that the direction of the fold
     * mark line will be the same as that of the line segment from (0,0) to (x2,
     * y2) and that {@code Math.sqrt( x2*x2 + y2*y2 ) = 1}.
     *
     * <p>
     * Returns {@code null} if there are no extra fold marks for this
     * sheet.
     *
     * @return an array of fold mark data in the format described above, or
     * {@code null} if there are no extra fold marks
     */
    public double[] getFoldMarks();

    /**
     * Return the width of this item, in points.
     *
     * @return the item's width
     */
    public double getWidth();

    /**
     * Return the height of this item, in points.
     *
     * @return the item's height
     */
    public double getHeight();

    /**
     * Return the user-friendly short name of this item.
     *
     * @return a name that describes the card or its general kind
     */
    public String getName();

    /**
     * Return an the item's current orientation. If the item does not implement
     * {@link FlippablePageItem}, it should always return
     * {@link FlippablePageItem#ORIENT_UPRIGHT}.
     *
     * @return the item's orientation value
     */
    public int getOrientation();

    /**
     * Return the page that owns this item, or {@code null} if it has no parent.
     *
     * @return the page that this item is on
     */
    public Page getPage();

    /**
     * Return a rectangle of the bounds of this item.
     *
     * @return a bounding rectangle for the item
     */
    public Rectangle2D.Double getRectangle();

    /**
     * Return a {@code Shape} that corresponds to the outline of this item.
     * In the simplest case, this can return the same result as
     * {@link #getRectangle()}. If precise geometry is available for the item,
     * then this should return a more accurate bounding shape.
     *
     * @return the outline of this object
     */
    public Shape getOutline();

    /**
     * Returns an array of the custom {@link DragHandle}s for this item. If the
     * item has no handles, returns {@code null}.
     *
     * @return an array of drag handles for manipulating the object
     */
    public DragHandle[] getDragHandles();

    /**
     * Get the class this item counts as for snapping.
     *
     * @return the snap class of this item
     */
    public SnapClass getSnapClass();

    /**
     * Set the class this item counts as for snapping.
     *
     * @param snapClass the new snap class for this item
     */
    public void setSnapClass(SnapClass snapClass);

    /**
     * Get the set of classes this card snaps against. The item will only snap
     * to the kinds of objects that are included in this class.
     *
     * @return the snap classes that determine the objects that this item will
     * snap against
     */
    public EnumSet<SnapClass> getClassesSnappedTo();

    /**
     * Set the set of classes this card snaps against.
     *
     * @param snapClass the snap classes that determine the objects that this
     * item will snap against
     */
    public void setClassesSnappedTo(EnumSet<SnapClass> snapClass);

    /**
     * Get the snap target rule for this item.
     *
     * @return the rule describing how this object snaps to other objects
     */
    public SnapTarget getSnapTarget();

    /**
     * Set the snap target rule for this item.
     *
     * @param target the rule describing how this object snaps to other objects
     */
    public void setSnapTarget(SnapTarget target);

    /**
     * Returns a small representative icon for this item. The icon should be
     * {@code ICON_SIZE} pixels wide and high.
     */
    public Icon getThumbnailIcon();

    public static final int ICON_SIZE = 48;

    /**
     * Returns the current x-position of this card.
     */
    public double getX();

    /**
     * Returns the current y-position of this card.
     */
    public double getY();

    /**
     * Returns {@code true} if this item should have crop marks added to
     * it. The crop marks will be placed {@link #getBleedMargin()} points from
     * the ends of each edge.
     */
    public boolean isBleedMarginMarked();

    /**
     * Returns the bleed margin used for any automatic crop marks, in points.
     *
     * @return the bleed margin for this item
     */
    public double getBleedMargin();

    /**
     * Returns {@code true} if {@code point} is inside the visible
     * bounds of this object.
     *
     * @param point a point in document coordinates (points from the upper-left
     * corner of the page)
     * @return {@code true} if the point lies inside the bounds of the
     * object
     */
    public boolean hitTest(Point2D point);

    /**
     * Returns {@code true} if this is in a horizontal orientation (turned
     * 90 degrees from normal). If this is not a {@link FlippablePageItem}, it
     * must return {@code false}.
     */
    public boolean isHorizontal();

    /**
     * Returns {@code true} if card is in a vertical orientation. If this
     * is not a {@link FlippablePageItem}, it must return {@code true}.
     */
    public boolean isVertical();

    /**
     * Returns {@code true} if this item is mirror-imaged. If this is not a
     * {@link FlippablePageItem}, it must return {@code false}.
     */
    public boolean isMirrored();

    /**
     * Returns {@code true} if this item has the same orientation rotation
     * as another item.
     */
    public boolean isTurned0DegreesFrom(PageItem rhs);

    /**
     * Returns true if this item's orientation is turned 180 degrees relative to
     * another item.
     */
    public boolean isTurned180DegreesFrom(PageItem rhs);

    /**
     * Returns true if this item's orientation is turned 90 degrees relative to
     * another item.
     */
    public boolean isTurned90DegreesFrom(PageItem rhs);

    /**
     * Paint this item at its current location. The graphics context will be
     * scaled so that 1 unit represents 1 point. The resolution hint is a
     * suggestion as to the resolution the item should be rendered at if it must
     * be converted to a bitmap before being drawn. It may or may not represent
     * the actual resolution of the output target.
     *
     * @param g the graphics context to paint to
     * @param target the type of destination being drawn to
     * @param renderResolutionHint a source resolution hint
     */
    public void paint(Graphics2D g, RenderTarget target, double renderResolutionHint);

    /**
     * Update any cached representations needed to paint this item at the
     * specified settings. It is not required that this be called prior to
     * calling {@link #paint}. Rather, the intent is that if this method returns
     * without throwing any exceptions (such as {@code OutOfMemoryError}),
     * then a call to {@link #paint} with the same settings is also
     * <i>expected</i> to succeed. Thus, this method may be called prior to
     * rendering at high resolution and if it throws an
     * {@code OutOfMemoryError}, the application can attempt to recover by
     * freeing up resources before trying to paint again.
     *
     * <p>
     * If this {@code PageItem} does not require rendering to a buffer,
     * then this method may do nothing.
     *
     * @param target the rendering target
     * @param renderResolutionHint a hint regarding output resolution
     * @see #paint
     */
    public void prepareToPaint(RenderTarget target, double renderResolutionHint);

    /**
     * Set the page which is the parent of this card, or {@code null} to clear
     * its parent. The {@code Page} class will set the parent automatically when
     * a card is added or removed from it.
     */
    public void setPage(Page parent);

    /**
     * Set the x-location of this item.
     *
     * @param x the new x-location
     */
    public void setX(double x);

    /**
     * Set the y-location of this item.
     *
     * @param y the new y-location
     */
    public void setY(double y);

    /**
     * Set the location of this item.
     *
     * @param x the new x-location
     * @param y the new y-location
     */
    public void setLocation(double x, double y);

    /**
     * Set the location of this item.
     *
     * @param loc the new location
     */
    public void setLocation(Point2D loc);

    /**
     * Returns the location of this item as a {@code Point2D}.
     *
     * @return the location of this item on the page
     */
    public Point2D getLocation();

    /**
     * Set the state of the item's selection lock. Locked items cannot be
     * selected or dragged.
     *
     * @param lock if {@code true}, prevent selecting or moving the item
     */
    public void setSelectionLocked(boolean lock);

    /**
     * Returns {@code true} if this item is locked against selection.
     *
     * @return {@code true} if the object is locked
     */
    public boolean isSelectionLocked();

    /**
     * Allows a page item the opportunity to customize the popup menu before it
     * is displayed. When a popup menu is constructed for a page view, the items
     * in the selection will be offered the opportunity to customize the menu.
     * Generally, only the last item (if any) makes any changes.
     *
     * @param menu the menu that will be displayed
     * @param selection the selected items; this should be considered read-only
     * @param isSelectionFocus if {@code true}, this is the last item in
     * the selection
     */
    public void customizePopupMenu(JPopupMenu menu, PageItem[] selection, boolean isSelectionFocus);

    /**
     * Returns the {@link Group} that this item belongs to, or
     * {@code null}.
     *
     * @return the group the object is in, or {@code null} if it isn't in a
     * group
     */
    public Group getGroup();

    /**
     * Sets the {@link Group} for this item. This should not be called directly,
     * but rather the item should be added to a group using
     * {@link Group#add(ca.cgjennings.apps.arkham.deck.item.PageItem)}.
     *
     * @param g the {@code Group} to add this to
     */
    void setGroup(Group g);

    /**
     * Returns {@code true} if this item has one or more drag handles that
     * may lie outside of the item's bounding box. The editor normally only
     * tests handles that lie within the bounding box to see if the user has
     * pointed at them; if this method returns {@code true} then the item's
     * handles are always tested.
     *
     * @return {@code true} if some handles may be outside the bounding box
     */
    public boolean hasExteriorHandles();

    /**
     * Sets a client property on this page item. Client properties can be used
     * by plug-ins to tag page items with plug-in specific data. Client
     * properties are saved as part a deck save file. To delete a client
     * property, use this method to set its value to {@code null}.
     *
     * @param propertyName the property name
     * @param value the value to associate with the property
     * @throws NullPointerException if the property name is {@code null}
     */
    public void putClientProperty(String propertyName, String value);

    /**
     * Returns the value of a client property of this page item, or
     * {@code null} if the property is not defined.
     *
     * @param propertyName the property name
     * @return the value associated with the property, or {@code null}
     * @throws NullPointerException if the property name is {@code null}
     */
    public String getClientProperty(String propertyName);
}
