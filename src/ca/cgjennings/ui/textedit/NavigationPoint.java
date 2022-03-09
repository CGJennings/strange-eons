package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.editors.CodeEditor;
import ca.cgjennings.ui.BlankIcon;
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
    private int scope;
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
        int d = offset - rhs.offset;
        if (d == 0) {
            d = scope - rhs.scope;
            if (d == 0) {
                d = description.compareTo(rhs.description);
            }
        }
        return d;
    }

    /**
     * Sort a list of navigation points by their description.
     *
     * @param points the points to sort
     */
    @SuppressWarnings(value = "unchecked")
    public static void sortByName(List<NavigationPoint> points) {
        Collections.sort(points, NAME_ORDER);
    }

    /**
     * Sort a list of navigation points by their offset.
     *
     * @param points the points to sort
     */
    public static void sortByOffset(List<NavigationPoint> points) {
        Collections.sort(points);
    }
    private static final Comparator<NavigationPoint> NAME_ORDER = (NavigationPoint o1, NavigationPoint o2) -> o1.description.compareTo(o2.description);

    public static final Icon ICON_KEYWORD = ResourceKit.getIcon("token-keyword");
    public static final Icon ICON_MODULE = ResourceKit.getIcon("token-module");
    public static final Icon ICON_PACKAGE = ResourceKit.getIcon("token-package");
    public static final Icon ICON_CLASS = ResourceKit.getIcon("token-class");
    public static final Icon ICON_INTERFACE = ResourceKit.getIcon("token-interface");
    public static final Icon ICON_ENUM = ResourceKit.getIcon("token-enum");
    public static final Icon ICON_ENUM_MEMBER = ResourceKit.getIcon("token-enum-item");
    public static final Icon ICON_VAR = ResourceKit.getIcon("token-var");
    public static final Icon ICON_LET = ResourceKit.getIcon("token-let");
    public static final Icon ICON_CONST = ResourceKit.getIcon("token-const");
    public static final Icon ICON_PROPERTY = ResourceKit.getIcon("token-property");
    public static final Icon ICON_FUNCTION = ResourceKit.getIcon("token-function");
    public static final Icon ICON_METHOD = ResourceKit.getIcon("token-method");
    public static final Icon ICON_GETTER = ResourceKit.getIcon("token-getter");
    public static final Icon ICON_SETTER = ResourceKit.getIcon("token-setter");
    public static final Icon ICON_TYPE = ResourceKit.getIcon("token-type");
    public static final Icon ICON_ALIAS = ResourceKit.getIcon("token-alias");
    public static final Icon ICON_PRIMITIVE = ResourceKit.getIcon("token-primitive");
    public static final Icon ICON_CALL = ResourceKit.getIcon("token-call");
    public static final Icon ICON_INDEX = ResourceKit.getIcon("token-index");
    public static final Icon ICON_PARAMETER = ResourceKit.getIcon("token-parameter");
    public static final Icon ICON_TYPE_PARAMETER = ResourceKit.getIcon("token-type-parameter");
    public static final Icon ICON_LABEL = ResourceKit.getIcon("token-label");
    public static final Icon ICON_DIRECTORY = ResourceKit.getIcon("token-directory");
    public static final Icon ICON_GLOBAL = ResourceKit.getIcon("token-global");
    public static final Icon ICON_H1 = ResourceKit.getIcon("token-h1"); 
    public static final Icon ICON_H2 = ResourceKit.getIcon("token-h2"); 
    public static final Icon ICON_H3 = ResourceKit.getIcon("token-h3"); 
    public static final Icon ICON_H4 = ResourceKit.getIcon("token-h4"); 
    public static final Icon ICON_H5 = ResourceKit.getIcon("token-h5"); 
    public static final Icon ICON_H6 = ResourceKit.getIcon("token-h6"); 
    public static final Icon ICON_TITLE = ResourceKit.getIcon("token-title"); 
    public static final Icon ICON_DIV = ResourceKit.getIcon("token-div"); 
    public static final Icon ICON_TABLE = ResourceKit.getIcon("token-table"); 
    public static final Icon ICON_SETTING = ResourceKit.getIcon("token-setting"); 
    

    /**
     * An error symbol icon sometimes used by navigation points. Typical use is
     * to represent compiler errors.
     */
    public static final Icon ICON_ERROR = ResourceKit.getIcon("error").tiny();
    /**
     * A warning symbol icon sometimes used by navigation points. Typical use is
     * to represent compiler warnings or other style hints.
     */
    public static final Icon ICON_WARNING = ResourceKit.getIcon("warning").tiny();
    /**
     * An empty icon the same size as the standard navigation point icons.
     */
    public static final Icon ICON_NONE = new BlankIcon().tiny();
}
