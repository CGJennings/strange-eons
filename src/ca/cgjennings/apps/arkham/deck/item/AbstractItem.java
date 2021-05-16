package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.deck.Page;
import ca.cgjennings.apps.arkham.deck.PageView;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EnumSet;
import java.util.HashMap;
import javax.swing.Icon;
import javax.swing.JPopupMenu;

/**
 * A base implementation of a non-resizeable, non-flippable page item.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public abstract class AbstractItem implements PageItem {

    protected double xOff, yOff;
    protected Page parent;
    protected EnumSet<SnapClass> snapToClasses;
    protected SnapTarget snapTarget;
    protected SnapClass snapClass;
    protected boolean selectionLock;
    protected Group group;
    protected transient DragHandle[] dragHandles;

    private HashMap<String, String> clientProperties;

    public AbstractItem() {
        setSnapClass(SnapClass.SNAP_OTHER);
        setClassesSnappedTo(SnapClass.SNAP_SET_NONE);
        setSnapTarget(SnapTarget.TARGET_OUTSIDE);
    }

    @Override
    public abstract void paint(Graphics2D g, RenderTarget target, double renderResolutionHint);

    @Override
    public void prepareToPaint(RenderTarget target, double renderResolutionHint) {
    }

    @Override
    public abstract double getWidth();

    @Override
    public abstract double getHeight();

    @Override
    public void setPage(Page parent) {
        this.parent = parent;
    }

    @Override
    public Page getPage() {
        return parent;
    }

    @Override
    public Rectangle2D.Double getRectangle() {
        return new Rectangle2D.Double(getX(), getY(), getWidth(), getHeight());
    }

    @Override
    public Shape getOutline() {
        return getRectangle();
    }

    @Override
    public boolean hitTest(Point2D p) {
        return getOutline().contains(p);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Concrete subclasses should create an array of handles on demand and cache
     * them in the {@code dragHandles} field in order to ensure that
     * handles are not shared between copies of the item. Example:
     * <pre>
     * public DragHandle[] getDragHandles() {
     *     if( dragHandles == null ) {
     *         dragHandles = new DragHandle[] { ... };
     *     }
     *     return dragHandles;
     * }
     * </pre>
     *
     * @return an array of drag handles that can be used to manipulate the item,
     * or {@code null}
     */
    @Override
    public DragHandle[] getDragHandles() {
        return null;
    }

    @Override
    public SnapClass getSnapClass() {
        return snapClass;
    }

    @Override
    public void setSnapClass(SnapClass snapClass) {
        this.snapClass = snapClass;
    }

    @Override
    public EnumSet<SnapClass> getClassesSnappedTo() {
        return snapToClasses.clone();
    }

    @Override
    public void setClassesSnappedTo(EnumSet<SnapClass> snapClasses) {
        snapToClasses = snapClasses;
    }

    @Override
    public SnapTarget getSnapTarget() {
        return snapTarget;
    }

    @Override
    public void setSnapTarget(SnapTarget target) {
        this.snapTarget = target;
    }

    @Override
    public abstract Icon getThumbnailIcon();

    @Override
    public double getX() {
        return xOff;
    }

    @Override
    public double getY() {
        return yOff;
    }

    @Override
    public void setX(double x) {
        this.xOff = x;
    }

    @Override
    public void setY(double y) {
        this.yOff = y;
    }

    @Override
    public final void setLocation(double x, double y) {
        setX(x);
        setY(y);
    }

    @Override
    public final void setLocation(Point2D loc) {
        setX(loc.getX());
        setY(loc.getY());
    }

    @Override
    public final Point2D getLocation() {
        return new Point2D.Double(getX(), getY());
    }

    @Override
    public boolean isSelectionLocked() {
        return selectionLock;
    }

    @Override
    public void setSelectionLocked(boolean lock) {
        selectionLock = lock;
    }

    @Override
    public double[] getFoldMarks() {
        return null;
    }

    @Override
    public boolean isBleedMarginMarked() {
        return false;
    }

    @Override
    public double getBleedMargin() {
        return 0;
    }

    @Override
    public int getOrientation() {
        return ORIENT_UPRIGHT;
    }

    @Override
    public boolean isHorizontal() {
        int o = getOrientation() & 3;
        return (o == ORIENT_TURN_LEFT) || (o == ORIENT_TURN_RIGHT);
    }

    @Override
    public boolean isVertical() {
        int o = getOrientation() & 3;
        return (o == ORIENT_UPRIGHT) || (o == ORIENT_UPSIDEDOWN);
    }

    @Override
    public boolean isMirrored() {
        return (getOrientation() & 4) != 0;
    }

    @Override
    public boolean isTurned0DegreesFrom(PageItem rhs) {
        return (getOrientation() & 3) == (rhs.getOrientation() & 3);
    }

    @Override
    public boolean isTurned180DegreesFrom(PageItem rhs) {
        int o1 = getOrientation() & 3;
        int o2 = rhs.getOrientation() & 3;
        return (o1 - o2 == 2) || (o2 - o1 == 2);
    }

    @Override
    public boolean isTurned90DegreesFrom(PageItem rhs) {
        int o1 = getOrientation() & 3;
        int o2 = rhs.getOrientation() & 3;
        return (o1 - o2 == 1) || (o2 - o1 == 1);
    }

    @Override
    public Group getGroup() {
        return group;
    }

    @Override
    public void setGroup(Group g) {
        group = g;
    }

    @Override
    public boolean hasExteriorHandles() {
        return false;
    }

    /**
     * Called by subclasses when the item's visual representation, location, or
     * size is about to change. Marks the area currently covered by the item as
     * in need of repainting.
     */
    protected void itemChanging() {
        final Page p = getPage();
        if (p != null) {
            final PageView v = p.getView();
            if (v != null) {
                v.repaint(this);
            }
        }
    }

    /**
     * Called by subclasses when the item's visual representation, location, or
     * size changes. This method will cause the page that the item is on (if
     * any) to be repainted, and marks the deck as having unsaved changes.
     */
    protected void itemChanged() {
        final Page p = getPage();
        if (p != null) {
            p.getDeck().markUnsavedChanges();
            final PageView v = p.getView();
            if (v != null) {
                v.repaint(this);
            }
        }
    }

    @Override
    public void customizePopupMenu(JPopupMenu menu, PageItem[] selection, boolean isSelectionFocus) {
    }

    @Override
    public PageItem clone() {
        try {
            AbstractItem cl = (AbstractItem) super.clone();
            cl.dragHandles = null;
            cl.group = null;
            return cl;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("clone");
        }
    }

    @Override
    public void putClientProperty(String property, String value) {
        if (property == null) {
            throw new NullPointerException("property");
        }
        if (value == null) {
            if (clientProperties != null) {
                clientProperties.remove(property);
                if (clientProperties.isEmpty()) {
                    clientProperties = null;
                }
            }
        } else {
            if (clientProperties == null) {
                clientProperties = new HashMap<>(4);
            }
            clientProperties.put(property, value);
        }
    }

    @Override
    public String getClientProperty(String property) {
        if (property == null) {
            throw new NullPointerException("property");
        }
        if (clientProperties == null) {
            return null;
        } else {
            return clientProperties.get(property);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + xOff + ',' + yOff + ',' + getWidth() + ',' + getHeight() + '}';
    }

    private static final int ABSTRACT_ITEM_VERSION = 3;

    protected void writeImpl(ObjectOutputStream out) throws IOException {
        out.writeInt(ABSTRACT_ITEM_VERSION);

        out.writeObject(getPage());
        out.writeDouble(getX());
        out.writeDouble(getY());
        out.writeObject(getSnapClass());
        out.writeObject(getSnapTarget());
        out.writeObject(getClassesSnappedTo());
        out.writeBoolean(isSelectionLocked());
        out.writeObject(group);
        out.writeObject(clientProperties);
    }

    protected void readImpl(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();

        setPage((Page) in.readObject());
        setLocation(in.readDouble(), in.readDouble());
        setSnapClass((SnapClass) in.readObject());
        setSnapTarget((SnapTarget) in.readObject());
        setClassesSnappedTo((EnumSet<PageItem.SnapClass>) in.readObject());
        setSelectionLocked(in.readBoolean());

        if (version >= 2) {
            group = (Group) in.readObject();
        } else {
            group = null;
        }

        if (version >= 3) {
            clientProperties = (HashMap<String, String>) in.readObject();
        } else {
            clientProperties = null;
        }
    }
    private static final long serialVersionUID = 8_936_744_391_094_878_580L;
}
