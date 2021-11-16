package ca.cgjennings.apps.arkham.sheet;

import resources.Settings;

public enum EdgeStyle {
    RAW,
    BLEED,
    NO_BLEED,
    CUT,
    HIGHLIGHT;

    private static final String KEY_PREVIEW_EDGE_STYLE = "preview-edge-style";

    private static EdgeStyle previewEdgeStyle = null;

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

    public static void setPreviewEdgeStyle(EdgeStyle previewEdgeStyle) {
        EdgeStyle.previewEdgeStyle = previewEdgeStyle;
        Settings.getUser().set(KEY_PREVIEW_EDGE_STYLE, previewEdgeStyle.name());
    }
}
