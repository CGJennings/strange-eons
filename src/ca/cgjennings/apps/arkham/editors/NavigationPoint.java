package ca.cgjennings.apps.arkham.editors;

import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.ui.textedit.CodeEditorBase;
import ca.cgjennings.ui.textedit.JSourceCodeEditor;
import ca.cgjennings.ui.theme.ThemedIcon;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.Icon;
import resources.ResourceKit;

/**
 * A navigation point describes a semantically significant location within a
 * text file.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see CodeEditor
 * @see NavigationPoint
 */
public class NavigationPoint implements Comparable<NavigationPoint> {

    private String description;
    private String longDescription;
    private int offset;
    int scope;
    private Icon icon;

    /**
     * Creates a new navigation point.
     *
     * @param description a brief description or summary of the content of the
     * location
     * @param offset the location of the point in the text, as a character
     * offset from the start of the file
     */
    public NavigationPoint(String description, int offset) {
        this(description, null, offset, 0, null);
    }

    /**
     * Creates a new navigation point.
     *
     * @param description a brief description or summary of the content of the
     * location
     * @param offset the location of the point in the text, as a character
     * offset from the start of the file
     * @param scope a value that indicates a nesting depth relative to other
     * navigation points in a collection of navigation points; default is 0
     */
    public NavigationPoint(String description, int offset, int scope) {
        this(description, null, offset, scope, null);
    }

    /**
     * Creates a new navigation point.
     *
     * @param description a brief description or summary of the content of the
     * location
     * @param longDescription a longer, more detailed description (may be
     * {@code null})
     * @param offset the location of the point in the text, as a character
     * offset from the start of the file
     * @param scope a value that indicates a nesting depth relative to other
     * navigation points in a collection of navigation points; default is 0
     * @param icon an icon for the navigation point type (may be {@code null})
     */
    public NavigationPoint(String description, String longDescription, int offset, int scope, Icon icon) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset < 0");
        }
        if (scope < 0) {
            throw new IllegalArgumentException("scope < 0");
        }
        this.description = description;
        this.offset = offset;
        this.scope = scope;
        this.icon = icon;
        this.longDescription = longDescription == null ? description : longDescription;
    }

    /**
     * Returns the character offset into the file that the navigation point
     * represents.
     *
     * @return the offset that this point jumps to
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Returns the icon for this point, or {@code null}.
     *
     * @return this point's icon, if any
     */
    public Icon getIcon() {
        return icon;
    }

    /**
     * Returns the long description of this navigation point. If the point has
     * no long description, returns the short description.
     *
     * @return the long description of the point
     */
    public String getLongDescription() {
        return longDescription;
    }

    /**
     * Returns the scope depth of this navigation point.
     *
     * @return the non-negative scope depth
     */
    public int getScope() {
        return scope;
    }

    /**
     * Returns the description of the navigation point.
     *
     * @return the short description of the point
     */
    @Override
    public String toString() {
        return description;
    }

    /**
     * Visits the point in a code editor. The editor's caret will be moved to
     * the offset specified by this navigation point.
     *
     * @param editor the editor to display the navigation point within
     */
    public void visit(CodeEditor editor) {
        CodeEditorBase ed = editor.getEditor();
        NavigationPoint target = this;
        Navigator navigator = editor.getNavigator();
        if (navigator != null) {
            List<NavigationPoint> points = navigator.getNavigationPoints(ed.getText());
            if (points != null && !points.isEmpty()) {
                target = getClosestPoint(points);
            }
        }
        int offset = target.offset;
        if (offset > ed.getLength()) {
            offset = ed.getLength();
        }
        int line = ed.getLineOfOffset(offset);
        offset = ed.getLineStartOffset(line);
        ed.select(offset, offset);
        ed.scrollToLine(line);
        ed.requestFocus();
    }

    /**
     * Returns {@code true} if two navigation points are considered equal, based
     * on their string description. Line numbers are NOT be considered when
     * determining equality. This is intentional.
     *
     * @param rhs the object to compare to
     * @return true if the specified object is a navigation point with the same
     * description
     */
    @Override
    public boolean equals(Object rhs) {
        if (rhs == null || !(rhs instanceof NavigationPoint)) {
            return false;
        }
        return description.equals(((NavigationPoint) rhs).description);
    }

    /**
     * Returns a hash code for this navigation point.
     *
     * @return a hash code consistent with equals
     */
    @Override
    public int hashCode() {
        return description.hashCode();
    }

    /**
     * Returns the navigation point in a list of points that is the best match
     * for this point. This method attempts to detect the point that best
     * reflects this point even if the points in the list were generated from a
     * version of the same text file that has been edited since the original
     * point was generated by a {@link Navigator}.
     *
     * @param newPoints the points to consider
     * @return the best matching point in the list for this point, or
     * {@code null} if the list is empty
     */
    public NavigationPoint getClosestPoint(List<NavigationPoint> newPoints) {
        if (newPoints == null || newPoints.isEmpty()) {
            return null;
        }
        NavigationPoint closestNav = null;
        boolean match = true;
        for (int i = 0; i < 2 && closestNav == null; ++i) {
            int dist = Integer.MAX_VALUE;
            for (NavigationPoint p : newPoints) {
                if (match && !equals(p)) {
                    continue;
                }
                int targetDist = Math.abs(p.offset - offset);
                if (targetDist < dist) {
                    closestNav = p;
                    dist = targetDist;
                }
            }
            match = false;
        }
        return closestNav;
    }

    /**
     * Compares two navigation points by their description.
     *
     * @param rhs the navigation point to compare this point to
     * @return a negative, zero, or positive integer as this point is less than,
     * equal to, or greater than the target of the comparison
     */
    @Override
    public int compareTo(NavigationPoint rhs) {
        return description.compareTo(rhs.description);
    }

    /**
     * Sort a list of navigation points by their description.
     *
     * @param points the points to sort
     */
    @SuppressWarnings(value = "unchecked")
    public static void sortByName(List<NavigationPoint> points) {
        Collections.sort(points);
    }

    /**
     * Sort a list of navigation points by their offset.
     *
     * @param points the points to sort
     */
    public static void sortByOffset(List<NavigationPoint> points) {
        Collections.sort(points, OFFSET_ORDER);
    }
    private static final Comparator<NavigationPoint> OFFSET_ORDER = (NavigationPoint o1, NavigationPoint o2) -> o1.offset - o2.offset;

    /**
     * An object cluster icon sometimes used by navigation points. Typical use
     * is to represent a class or object.
     */
    public static final Icon ICON_CLUSTER = new ThemedIcon("icons/ui/dev/class.png", true);
    /**
     * An object cluster icon with a bar through it sometimes used by navigation
     * points. Typical use is to represent a class or object assigned to a
     * constant.
     */
    public static final Icon ICON_CLUSTER_BAR = new ThemedIcon("icons/ui/dev/class-bar.png", true);
    /**
     * An object package icon sometimes used by navigation points. Typical use
     * is to represent a package of classes.
     */
    public static final Icon ICON_PACKAGE = new ThemedIcon("icons/ui/dev/package.png", true);
    /**
     * A hexagon icon sometimes used by navigation points. Typical use is to
     * represent a script library.
     */
    public static final Icon ICON_HEXAGON = new ThemedIcon("icons/ui/dev/hexagon.png", true);
    /**
     * A circle icon sometimes used by navigation points. Typical use is to
     * represent a field (member variable).
     */
    public static final Icon ICON_CIRCLE = new ThemedIcon("icons/ui/dev/circle.png", true);
    /**
     * A circle icon with a bar through it sometimes used by navigation points.
     * Typical use is to represent a static or constant field (member variable).
     */
    public static final Icon ICON_CIRCLE_BAR = new ThemedIcon("icons/ui/dev/circle-bar.png", true);
    /**
     * A small circle icon sometimes used by navigation points. Typical use is
     * to represent a keyword.
     */
    public static final Icon ICON_CIRCLE_SMALL = new ThemedIcon("icons/ui/dev/circle-small.png", true);
    /**
     * A diamond icon sometimes used by navigation points. Typical use is to
     * represent a function or method.
     */
    public static final Icon ICON_DIAMOND = new ThemedIcon("icons/ui/dev/diamond.png", true);
    /**
     * A diamond icon with a bar through it sometimes used by navigation points.
     * Typical use is to represent a constant function or static method.
     */
    public static final Icon ICON_DIAMOND_BAR = new ThemedIcon("icons/ui/dev/diamond-bar.png", true);
    /**
     * A leftward diamond icon sometimes used by navigation points. Typical use
     * is to represent a getter function.
     */
    public static final Icon ICON_DIAMOND_LEFT = new ThemedIcon("icons/ui/dev/getter.png", true);
    /**
     * A rightward diamond icon sometimes used by navigation points. Typical use
     * is to represent a setter function.
     */
    public static final Icon ICON_DIAMOND_RIGHT = new ThemedIcon("icons/ui/dev/setter.png", true);
    /**
     * A square icon sometimes used by navigation points. Typical use is to
     * represent a variable.
     */
    public static final Icon ICON_SQUARE = new ThemedIcon("icons/ui/dev/square.png", true);
    /**
     * A square icon with a bar through it sometimes used by navigation points.
     * Typical use is to represent a static variable or constant.
     */
    public static final Icon ICON_SQUARE_BAR = new ThemedIcon("icons/ui/dev/square-bar.png", true);
    /**
     * A square icon in an alternative colour that is sometimes used by
     * navigation points. Typical use is to represent a function or method
     * parameter.
     */
    public static final Icon ICON_SQUARE_ALTERNATIVE = new ThemedIcon("icons/ui/dev/square-alt.png", true);
    /**
     * A square icon with a bar through it in an alternative colour that is
     * sometimes used by navigation points. Typical use is to represent an
     * enumeration value.
     */
    public static final Icon ICON_SQUARE_ALTERNATIVE_BAR = new ThemedIcon("icons/ui/dev/square-alt-bar.png", true);
    /**
     * A triangle icon sometimes used by navigation points. Typical use is to
     * represent a property key name.
     */
    public static final Icon ICON_TRIANGLE = new ThemedIcon("icons/ui/dev/triangle.png", true);
    /**
     * A Greek cross icon sometimes used by navigation points. Typical use is to
     * represent a generator function.
     */
    public static final Icon ICON_CROSS = new ThemedIcon("icons/ui/dev/cross.png", true);
    /**
     * A Greek cross icon with a bar through it sometimes used by navigation
     * points. Typical use is to represent a constant generator function.
     */
    public static final Icon ICON_CROSS_BAR = new ThemedIcon("icons/ui/dev/cross-bar.png", true);
    /**
     * An error symbol icon sometimes used by navigation points. Typical use is
     * to represent compiler errors.
     */
    public static final Icon ICON_ERROR = ImageUtilities.createIconForSize(ResourceKit.getThemedImage("icons/ui/error.png"), 12);
    /**
     * A warning symbol icon sometimes used by navigation points. Typical use is
     * to represent compiler warnings or other style hints.
     */
    public static final Icon ICON_WARNING = ImageUtilities.createIconForSize(ResourceKit.getThemedImage("icons/ui/warning.png"), 12);
}
