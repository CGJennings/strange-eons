package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import resources.Settings;

/**
 * This class tracks and updates the current state of deck-related view options,
 * such as whether drag handles should be shown.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class ViewOptions {

    private ViewOptions() {
    }

    private static final String HANDLE_KEY = "deck-show-handles";
    private static final String MARGIN_KEY = "deck-show-margins";
    private static final String GRID_KEY = "deck-show-grid";

    private static boolean handles, margin, grid;

    static {
        Settings user = Settings.getUser();
        handles = user.getBoolean(HANDLE_KEY);
        margin = user.getBoolean(MARGIN_KEY);
        grid = user.getBoolean(GRID_KEY);
    }

    /**
     * Returns <code>true</code> if the handles for grabbing and manipulating
     * aspects of page items (such as size, rotation, etc.) are painted.
     *
     * @return <code>true</code> if the handles should be painted
     */
    public static boolean isDragHandlePainted() {
        return handles;
    }

    /**
     * Sets whether the handles for grabbing and manipulating aspects of page
     * items (such as size, rotation, etc.) should be painted.
     *
     * @param drawHandles <code>true</code> if the handles should be painted
     */
    public static void setDragHandlePainted(boolean drawHandles) {
        if (handles != drawHandles) {
            handles = drawHandles;
            update(HANDLE_KEY, drawHandles);
        }
    }

    /**
     * Returns <code>true</code> if the page margins are shown.
     *
     * @return <code>true</code> if the margins should be painted
     */
    public static boolean isMarginPainted() {
        return margin;
    }

    /**
     * Sets whether the page margins are shown.
     *
     * @param drawMargin <code>true</code> if the margins should be painted
     */
    public static void setMarginPainted(boolean drawMargin) {
        if (margin != drawMargin) {
            margin = drawMargin;
            update(MARGIN_KEY, drawMargin);
        }
    }

    /**
     * Returns <code>true</code> if the grid is shown.
     *
     * @return <code>true</code> if the grid should be painted
     */
    public static boolean isGridPainted() {
        return grid;
    }

    /**
     * Sets whether the page grid is shown.
     *
     * @param drawGrid <code>true</code> if the grid should be painted
     */
    public static void setGridPainted(boolean drawGrid) {
        if (grid != drawGrid) {
            grid = drawGrid;
            update(MARGIN_KEY, drawGrid);
        }
    }

    private static void update(String key, boolean newValue) {
        Settings.getUser().set(key, newValue ? "yes" : "no");
        StrangeEonsEditor ed = StrangeEons.getWindow().getActiveEditor();
        if (ed instanceof DeckEditor) {
            DeckEditor de = (DeckEditor) ed;
            try {
                de.getDeck().getActivePage().getView().repaint();
            } catch (NullPointerException e) {
                // shouldn't happen, but just in case
                StrangeEons.log.warning("unexpect null pointer while getting page view");
            }
        }
    }
}
