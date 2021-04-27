/*
 
 markup.js - version 8
 Markup boxes, text styles, page shapes.
 
 */

const MarkupBox = ca.cgjennings.layout.MarkupRenderer;
const GenderMarkupBox = ca.cgjennings.layout.GenderAwareMarkupRenderer;
const TextStyle = ca.cgjennings.layout.TextStyle;
const TextAttribute = java.awt.font.TextAttribute;
const PageShape = ca.cgjennings.layout.PageShape;

function markupBox(sheet, genderAware) {
    var ppi = sheet instanceof arkham.sheet.Sheet
            ? sheet.templateResolution : sheet;
    var renderer;
    if (genderAware) {
        renderer = new GenderMarkupBox(ppi);
    } else {
        renderer = new MarkupBox(ppi);
    }
    sheet.doStandardRendererInitialization(renderer);
    return renderer;
}

const FIT_NONE = MarkupBox.FIT_NONE;
const FIT_SCALE_TEXT = MarkupBox.FIT_SCALE_TEXT;
const FIT_TIGHTEN_LINE_SPACING = MarkupBox.FIT_TIGHTEN_LINE_SPACING;
const FIT_BOTH = MarkupBox.FIT_BOTH;
const LAYOUT_LEFT = MarkupBox.LAYOUT_LEFT;
const LAYOUT_CENTER = MarkupBox.LAYOUT_CENTER;
const LAYOUT_RIGHT = MarkupBox.LAYOUT_RIGHT;
const LAYOUT_TOP = MarkupBox.LAYOUT_TOP;
const LAYOUT_MIDDLE = MarkupBox.LAYOUT_MIDDLE;
const LAYOUT_BOTTOM = MarkupBox.LAYOUT_BOTTOM;
const LAYOUT_JUSTIFY = MarkupBox.LAYOUT_JUSTIFY;

function updateNameTags(markupbox, gameComponent) {
    arkham.sheet.Sheet.setNamesForRenderer(markupbox, gameComponent.name, null, null);
}

const FAMILY = TextAttribute.FAMILY;

const FAMILY_BODY = ResourceKit.getBodyFamily();
const FAMILY_SERIF = "Serif";
const FAMILY_SANS_SERIF = "SansSerif";
const FAMILY_MONOSPACED = "Monospace";

const WEIGHT = TextAttribute.WEIGHT;

const WEIGHT_EXTRALIGHT = TextAttribute.WEIGHT_EXTRA_LIGHT;
const WEIGHT_LIGHT = TextAttribute.WEIGHT_LIGHT;
const WEIGHT_DEMILIGHT = TextAttribute.WEIGHT_DEMILIGHT;
const WEIGHT_REGULAR = TextAttribute.WEIGHT_REGULAR;
const WEIGHT_SEMIBOLD = TextAttribute.WEIGHT_SEMIBOLD;
const WEIGHT_MEDIUM = TextAttribute.WEIGHT_MEDIUM;
const WEIGHT_DEMIBOLD = TextAttribute.WEIGHT_DEMIBOLD;
const WEIGHT_BOLD = TextAttribute.WEIGHT_BOLD;
const WEIGHT_HEAVY = TextAttribute.WEIGHT_HEAVY;
const WEIGHT_EXTRABOLD = TextAttribute.WEIGHT_EXTRABOLD;
const WEIGHT_ULTRABOLD = TextAttribute.WEIGHT_ULTRABOLD;

const WIDTH = TextAttribute.WIDTH;

const WIDTH_CONDENSED = TextAttribute.WIDTH_CONDENSED;
const WIDTH_SEMICONDENSED = TextAttribute.WIDTH_SEMI_CONDENSED;
const WIDTH_SEMI_CONDENSED = TextAttribute.WIDTH_SEMI_CONDENSED;
const WIDTH_REGULAR = TextAttribute.WIDTH_REGULAR;
const WIDTH_SEMIEXTENDED = TextAttribute.WIDTH_SEMI_EXTENDED;
const WIDTH_SEMI_EXTENDED = TextAttribute.WIDTH_SEMI_EXTENDED;
const WIDTH_EXTENDED = TextAttribute.WIDTH_EXTENDED;

const POSTURE = TextAttribute.POSTURE;

const POSTURE_REGULAR = TextAttribute.POSTURE_REGULAR;
const POSTURE_OBLIQUE = TextAttribute.POSTURE_OBLIQUE;

const SIZE = TextAttribute.SIZE;

const SUPERSCRIPT = TextAttribute.SUPERSCRIPT;

const SUPERSCRIPT_SUPER = TextAttribute.SUPERSCRIPT_SUPER;
const SUPERSCRIPT_SUB = TextAttribute.SUPERSCRIPT_SUB;

const FONT_OBJECT = TextAttribute.FONT;

const COLOUR = TextAttribute.FOREGROUND;
const COLOR = TextAttribute.FOREGROUND;
const PAINT = TextAttribute.FOREGROUND;

const BGCOLOUR = TextAttribute.BACKGROUND;
const BGCOLOR = TextAttribute.BACKGROUND;
const BGPAINT = TextAttribute.BACKGROUND;

const UNDERLINE = TextAttribute.UNDERLINE;
const UNDERLINE_ON = TextAttribute.UNDERLINE_ON;

const STRIKETHROUGH = TextAttribute.STRIKETHROUGH;
const STRIKETHROUGH_ON = TextAttribute.STRIKETHROUGH_ON;

const SWAP_COLORS = TextAttribute.SWAP_COLORS;
const SWAP_COLORS_ON = TextAttribute.SWAP_COLORS_ON;
const SWAP_COLOURS = TextAttribute.SWAP_COLORS;
const SWAP_COLOURS_ON = TextAttribute.SWAP_COLORS_ON;

const LIGATURES = TextAttribute.LIGATURES;
const LIGATURES_ON = TextAttribute.LIGATURES_ON;

const TRACKING = TextAttribute.TRACKING;
const TRACKING_TIGHT = TextAttribute.TRACKING_TIGHT;
const TRACKING_LOOSE = TextAttribute.TRACKING_LOOSE;
