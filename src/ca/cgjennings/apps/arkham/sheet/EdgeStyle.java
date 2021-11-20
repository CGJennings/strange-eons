package ca.cgjennings.apps.arkham.sheet;

import resources.Settings;

/**
 * An enumeration of the different sheet edge styles that can be applied when
 * previewing or printing components.
 *
 * @author Henrik Rostedt
 */
public enum EdgeStyle {
    /**
     * Leaves the sheet unchanged.
     */
    RAW,
    /**
     * Generates a 9pt bleed margin automatically if the sheet does not include
     * a bleed margin.
     */
    BLEED,
    /**
     * Removes any bleed margin included in the sheet.
     */
    NO_BLEED,
    /**
     * Removed any bleed margin included in the sheet, and then rounds the
     * corners of the sheet if a corner radius is specified for the sheet. The
     * new image will include transparency.
     */
    CUT,
    /**
     * Draws the card edge over the raw sheet image, taking bleed margin and
     * corner radius into account. Intended for debugging.
     */
    HIGHLIGHT;

    private static final String KEY_PREVIEW_EDGE_STYLE = "preview-edge-style";

    private static EdgeStyle previewEdgeStyle = null;

    /**
     * Returns the global edge style setting to use when rendering sheets.
     *
     * @return the global preview edge style
     */
    public static EdgeStyle getPreviewEdgeStyle() {
        if (previewEdgeStyle == null) {
            String name = Settings.getShared().get(KEY_PREVIEW_EDGE_STYLE);
            try {
                previewEdgeStyle = valueOf(name);
            } catch (Exception e) {
                previewEdgeStyle = CUT;
            }
        }
        return previewEdgeStyle;
    }

    /**
     * Sets the global preview edge style to use when rendering sheets. This
     * will not update the open previewers. That can be done by calling:
     * {@code StrangeEons.getWindow().redrawPreviews();}.
     *
     * @param previewEdgeStyle the new preview edge style
     */
    public static void setPreviewEdgeStyle(EdgeStyle previewEdgeStyle) {
        EdgeStyle.previewEdgeStyle = previewEdgeStyle;
        Settings.getUser().set(KEY_PREVIEW_EDGE_STYLE, previewEdgeStyle.name());
    }
}
