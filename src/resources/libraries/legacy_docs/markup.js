/*

  markup.js - version 7
  Markup boxes, text styles, page shapes.


The SE JavaScript Library Copyright © 2008-2013
Christopher G. Jennings and contributors. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

This software is provided by the author "as is" and any express or implied
warranties, including, but not limited to, the implied warranties of
merchantability and fitness for a particular purpose are disclaimed. In no
event shall the author be liable for any direct, indirect, incidental, special,
exemplary, or consequential damages (including, but not limited to, procurement
of substitute goods or services; loss of use, data, or profits; or business
interruption) however caused and on any theory of liability, whether in
contract, strict liability, or tort (including negligence or otherwise) arising
in any way out of the use of this software, even if advised of the possibility
of such damage.

*/

const MarkupBox = ca.cgjennings.layout.MarkupRenderer;
const GenderMarkupBox = ca.cgjennings.layout.GenderAwareMarkupRenderer;
const TextStyle = ca.cgjennings.layout.TextStyle;
const TextAttribute = java.awt.font.TextAttribute;
const PageShape = ca.cgjennings.layout.PageShape;

/**
 * markupBox( sheet, [genderAware] )
 * Creates a new markup renderer that can lay out text marked up with
 * Strange Eons tags. The renderer will be set up for use with
 * <tt>sheet</tt>, a DIY card face object.
 *
 * sheet : a character sheet object, such as that passed to <tt>DIY.createFrontPainter()</tt>
 * genderAware : if <tt>true</tt> a <tt>GenderMarkupBox</tt> is returned
 */
/**
 * markupBox( ppi, [genderAware] )
 * Creates a new markup renderer. The renderer will be set up for rendering
 * at a resolution of <tt>dpi</tt> pixels per inch.
 *
 * ppi : the rendering resolution in pixels per inch
 * genderAware : if <tt>true</tt> a <tt>GenderMarkupBox</tt> is returned
 */
function markupBox( sheet, genderAware ) {
    var ppi = sheet instanceof arkham.sheet.Sheet
            ? sheet.templateResolution : sheet;
    var renderer;
    if( genderAware )
        renderer = new GenderMarkupBox( ppi );
    else
        renderer = new MarkupBox( ppi );
    sheet.doStandardRendererInitialization( renderer );
    return renderer;
}

/**
 * MarkupBox
 * An object capable of laying out styled text by parsing plain text that is
 * marked up with HTML-like tags.
 * To create an object of this class, it is normally recommended that you use
 * the <tt>markupBox( sheet )</tt> function, which will set the correct
 * resolution and define standard tags.
 *
 * <b>Note:</b> Because it is based on a Java class, you may use the JavaScript engine's
 * getter/setter syntax with a <tt>MarkupBox</tt>. For example, instead of
 * <tt>box.setDefaultStyle( style );</tt> you can write
 * <tt>box.defaultStyle = style;</tt>
 */

/**
 * MarkupBox( ppi ) [ctor]
 * This constructor creates a default markup renderer which draws at the
 * specified resolution.
 * Rather than using a constructor, <tt>MarkupBox</tt>es are usually created
 * indirectly with the <tt>markupBox( sheet )</tt> helper function.
 *
 * ppi : the assumed resolution for drawing, in pixels per inch
 */

/**
 * MarkupBox.draw( g, region )
 * Lays out and draws the current markup text within the specified region.
 * Returns the y-coordinate where the next line of text would start.
 *
 * g : the graphics context to use for drawing
 * region : the area in which to draw text; this may be a <tt>Region</tt> or a <tt>java.awt.geom.Rectangle2D</tt>
 */

/**
 * MarkupBox Constants
 *
 * FIT_NONE : do not try to fit text that is too long to fit in the given region
 * FIT_SCALE_TEXT : fit long text by shrinking it down
 * FIT_TIGHTEN_LINE_SPACING : fit long text by reducing line spacing
 * FIT_BOTH : fit text by shrinking it and reducing the space between lines
 * LAYOUT_LEFT : left-align lines of text
 * LAYOUT_CENTER : center lines of text
 * LAYOUT_RIGHT : right-align lines of text
 * LAYOUT_TOP : vertically align text to the top of the region
 * LAYOUT_MIDDLE : vertically center text within the region
 * LAYOUT_BOTTOM : vertically align text to the bottom of the region
 * LAYOUT_JUSTIFY : lines will be justified to fit the width of the region
 */

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

/**
 * MarkupBox.setStyleForTag( tagName, style )
 * Sets the style associated with a non-parametric tag.
 * If a style was set previously for this tag, it will be replaced by the new
 * style. If a replacement was associated with this tag, the replacement will be removed.
 * Do not include the angle brackets in the tag name.
 *
 * For example:
 * <pre>
 * var style = new TextStyle( WEIGHT, WEIGHT_LIGHT );
 * box.setStyleForTag( "light", style );
 * </pre>
 *
 * tagName : the name of the tag pair (<tt>&lt;<i>tagName</i>&gt; ... <tt>&lt;/<i>tagName</i>&gt;)
 * style : the <tt>TextStyle</tt> to be applied for this pair
 */

/**
 * MarkupBox.getStyleForTag( tagName )
 * Returns the <tt>TextStyle</tt> associated with the tag, or null if the tag does not
 * have a non-parametric style set (including if it has a replacement or
 * parametric style set).
 *
 * tagName : the name of the tag (without angle brackets) to fetch the style of
 *
 * Returns the style for the requested tag, or <tt>null</tt> if it has no,
 * or a different kind, of definition.
 */

/**
 * MarkupBox.setParametricStyleForTag( tagName, factory )
 * Sets a parameter-based style for the given tag. When this tag occurs in the
 * markup text, the factory will be used to create the actual style based on
 * the tag's parameters. This is an advanced feature.
 *
 * The <tt>factory</tt> argument is an object that implements the interface
 * <tt>ca.cgjennings.layout.ParametricStyleFactory</tt>. This interface consists
 * of a single method:
 * <pre>TextStyle createStyle( java.lang.String[] parameters )</pre>
 *
 * tagName : the name of the tag (without angle brackets) to set the factory for
 * factory : a <tt>ParametricStyleFactory</tt> to be used to generate styles for this tag
 */

/**
 * MarkupBox.getParametricStyleForTag( tagName )
 * Returns the <tt>ParametricStyleFactory</tt> associated with the tag,
 * or <tt>null</tt> if the tag does not have a style set (including if it has
 * a replacement or style set).
 *
 * tagName : the name of the tag (without angle brackets) to get the factory for
 *
 * Returns the factory for the requested tag, or <tt>null</tt> if it has no,
 * or a different kind, of definition.
 */

/**
 * MarkupBox.setReplacementForTag( tagName, replacement )
 * Sets a replacement string to be associated with a particular tag.
 * When this exact tag (not including angle brackets) occurs in the markup text,
 * it will be replaced by the replacement string.
 *
 * For example:
 * <pre>
 * box.setReplacementForTag( "copyright", "©" );
 * </pre>
 *
 * tagName : the name of the replacement tag
 * replacement : the text to replace the tag with
 */

/**
 * MarkupBox.getReplacementForTag( tagName )
 * Returns the replacement String associated with the tag, or <tt>null</tt>
 * if the tag does not have a replacement set (including if it has a style
 * or parametric style set).
 *
 * tagName : the name of the tag to return the replacement string for
 *
 * Returns the replacement string for the tag, or <tt>null</tt> if it has no,
 * or a different kind, of definition.
 */

/**
 * MarkupBox.removeTag( tagName )
 * Removes any style or replacement associated with this tag.
 *
 * tagName : the name of the tag to be undefined
 */

/**
 * MarkupBox.removeAllTags()
 * Removes all style and replacement definitions.
 */

/**
 * MarkupBox.setDefaultStyle( style )
 * Sets the base style that is applied to text when no other tags affect it.
 *
 * style : the <tt>TextStyle</tt> that is applied to all text by default
 */

/**
 * MarkupBox.getDefaultStyle()
 * Returns the default style, which is the style that is applied to text when
 * no other tags are in effect. By modifying this style you can control what
 * "normal" text looks like.
 *
 * For example:
 * <pre>
 * box.defaultStyle = new TextStyle(
 *     FAMILY, bodyFamily,
 *     SIZE,   7,
 *     WEIGHT, WEIGHT_BOLD
 * );
 * </pre>
 *
 * You can also modify the default style instead of replacing it. The following
 * changes just the size without affecting other attributes:
 * <pre>
 * box.defaultStyle.add( SIZE, 10 );
 * </pre>
 *
 * style : the <tt>TextStyle</tt> that is applied to all text by default
 */

/**
 * MarkupBox.setAlignment( alignment )
 * Sets the horizontal and vertical alignment of text within a drawn rectangle.
 * This is the logical <i>or</i> of one of <tt>LAYOUT_LEFT</tt>, <tt>LAYOUT_RIGHT</tt>,
 * or <tt>LAYOUT_CENTER</tt> with one of <tt>LAYOUT_TOP</tt>, <tt>LAYOUT_BOTTOM</tt>,
 * or <tt>LAYOUT_MIDDLE</tt>. In addition, if the <tt>LAYOUT_JUSTIFY</tt> bit
 * is set, then paragraphs will be fully justified so that they fill the
 * entire region width.
 *
 * For example:
 * <pre>
 * box.alignment = LAYOUT_CENTER | LAYOUT_TOP | LAYOUT_JUSTIFY;
 * </pre>
 *
 * alignment : a bitmask of alignment options
 */

/**
 * MarkupBox.getAlignment()
 * Returns the alignment bitmask that describes how text is laid out within
 * a drawn rectangle.
 *
 * For example:
 * <pre>
 * if( (box.alignment & LAYOUT_LEFT) != 0 ) println( "left-aligned text" );
 * </pre>
 *
 * Returns the current alignment value.
 */

/**
 * MarkupBox.setHeadlineAlignment( alignment )
 * Sets the horizontal alignment used for text in headline tags (<tt>&lt;h1&gt;</tt>,
 * <tt>&lt;h2&gt;</tt>, and so on). This is similar to <tt>setAlignment()</tt>,
 * but only the horizontal alignment value is used.
 *
 * alignment : a horizontal alignment option
 */

/**
 * MarkupBox.getHeadlineAlignment()
 * Returns the current horizontal alignment setting for heading tags.
 */

/**
 * MarkupBox.setTabWidth( tabWidthInInches )
 * Sets the distance, in inches, between tab stops.
 *
 * tabWidthInInches : the default gap between tab stops
 */

/**
 * MarkupBox.getTabWidth()
 * Returns the default distance, in inches, between tab stops.
 * If multiple stops have been set, this returns the first tab width.
 * It is equivalent to <tt>getTabWidths()[0]</tt>.
 */

/**
 * MarkupBox.setTabWidths( tabWidthsInInches[] )
 * Sets the distance, in inches, between tab stops.
 * This method accepts multiple tab positions as an array, which sets a
 * series of variable-width tab stops instead of a fixed tab size.
 *
 * tabWidthsInInches[] : the default gap between tab stops
 */

/**
 * MarkupBox.getTabWidths()
 * Returns an array of tab widths, in inches. If only one stop has been set,
 * perhaps using <tt>setTabWidth</tt>, then the length of the array is 1.
 */

/**
 * MarkupBox.setTextFitting( fittingStyle )
 * Sets the fitting methods that will be used to shrink text that is too long
 * to fit in the text region. One of <tt>FIT_NONE</tt>,
 * <tt>FIT_TIGHTEN_LINE_SPACING</tt>, <tt>FIT_SCALE_TEXT</tt>, or <tt>FIT_BOTH</tt>.
 */

/**
 * MarkupBox.getTextFitting()
 * Returns the current text fitting method.
 */

/**
 * MarkupBox.setScalingLimit( factor )
 * Sets the limit for shrinking text so that it can be scaled down to no more
 * than <tt>factor</tt> * 100% of original size.
 */

/**
 * MarkupBox.getScalingLimit()
 * Returns the current scaling limit for text shrinking.
 */

/**
 * MarkupBox.setLineTightness( tightness )
 * Sets how tightly lines are grouped together. For normal line spacing, use
 * 1.0; double-spaced lines would be 2.0. Values less than 1 reduce the standard
 * amount of space between lines; at 0 the bottom of one line will normally touch
 * or overlap the top of the next. The exact effect of this setting depends somewhat
 * on the font being used.
 *
 * tightness : the tightness of interline spacing (normally 1.0)
 */

/**
 * MarkupBox.getLineTightness()
 * Returns the current line tightness.
 */

/**
 * MarkupBox.setTightnessLimit( minTightness )
 * Sets the minimum line tightness that can be used when line spacing is reduced
 * to fit long text.
 *
 * minTightness : the smallest amount that line tightness can be reduced to when fitting text
 */

/**
 * MarkupBox.getTightnessLimit()
 * Returns the current line tightness limit.
 */

/**
 * MarkupBox.setPageShape( shape )
 * Sets the <tt>PageShape</tt> that lines of text will conform to.
 * A <tt>PageShape</tt> is used to flow text around other elements of a component.
 * The default shape is <tt>PageShape.RECTANGLE_SHAPE</tt>, which simply uses the
 * sides of the region passed to the <tt>draw</tt> method.
 *
 * shape : the desired <tt>PageShape</tt>
 */

/**
 * MarkupBox.getPageShape()
 * Returns the <tt>PageShape</tt> that will be used to shape lines of text.
 */

/**
 * MarkupBox.setMarkupText( markup )
 * Sets the markup text that will be used when laying out text.
 */

/**
 * MarkupBox.getMarkupText()
 * Returns the current markup text that will be used when laying out text.
 */

/**
 * MarkupBox.parseLibrary( libraryString )
 * Processes a markup string, retaining only any definitions that are created
 * using <tt>&lt;define&gt;</tt> tags. Once some markup text (other than a library)
 * is parsed, all definitions will be cleared again.
 * Therefore, libraries must be parsed again before <i>each</i> call to
 * <tt>setMarkupText</tt>.
 *
 * This method clears the current markup text.
 *
 * libraryString : a string containing markup definitions (like a case book library)
 */

/**
 * MarkupBox.draw( g, region )
 * Lays out and draws the current markup text within the specified region.
 *
 * Returns the y-coordinate where the next line of text would be drawn.
 *
 * g : the graphics context to use for drawing
 * region : the area in which to draw text; this may be a <tt>Region</tt> or a <tt>java.awt.geom.Rectangle2D</tt>
 */

/**
 * MarkupBox.drawAsSingleLine( g, region )
 * Lays out and draws the current markup text within the specified region.
 * The renderer will not insert line breaks into the text at the end of a line,
 * but will instead shrink the text as needed to fit the line within the width
 * of the given region. (If the source text contains hard line breaks then the
 * resulting layout will still consist of multiple lines, but only the width
 * of the last line will be used to determine the scale of the text.)
 *
 * Returns the width of the last line.
 *
 * g : the graphics context to use for drawing
 * region : the area in which to draw text; this may be a <tt>Region</tt> or a <tt>java.awt.geom.Rectangle2D</tt>
 */

/**
 * MarkupBox.measure( g, region )
 * Lays out the current markup text without drawing it, returning the height
 * of the text.
 *
 * g : the graphics context to use for drawing
 * region : the area in which to lay out the ; this may be a <tt>Region</tt> or a <tt>java.awt.geom.Rectangle2D</tt>
 */



/**
 * GenderMarkupBox : MarkupBox
 * A <tt>GenderMarkupBox</tt> is a subclass of <tt>MarkupBox</tt>
 * that includes a boolean "gender" attribute. Tags that include a slash
 * in the middle (&lt;like/this&gt;) will be converted into the text before
 * the slash (if gender is <tt>false</tt>) or into the text after the slash (if gender
 * is <tt>true</tt>). This allows you to use markup that is sensitive to some binary
 * property, like a character's gender: <tt>&lt;His/Her&gt; monkey bit
 * &lt;him/her&gt;</tt>. Since tags are closed with tag names that start with
 * a slash, a dash is used to indicate an empty string (as in
 * <tt>prophet&lt;-/ess&gt;</tt>).
 */

/**
 * GenderMarkupBox.setGender( useRightSideText )
 * Sets the markup box's gender, which controls which text from slash
 * divided gender tags will be used.
 *
 * useRightSideText : if <tt>true</tt>, the text after the slash is used
 */

/**
 * GenderMarkupBox.getGender()
 * Returns the markup box's current gender. If <tt>true</tt>, the text
 * after the slash is used for tags that include a slash.
 */



/**
 * updateNameTags( markupbox, gameComponent )
 * Updates a <tt>MarkupBox</tt>'s <tt>&lt;name&gt;</tt> family of tags using
 * a component's current name. Call this at the start of your painting function
 * before drawing a box's text if you want name tags to work properly. This
 * function sets all of the name tags (<tt>&lt;name&gt;</tt>,
 * <tt>&lt;lastname&gt;</tt>, <tt>&lt;fullname&gt;</tt>) based simply on the
 * component's standard name. For more complete control, use
 * <tt>sheet.setNamesForRenderer( markupbox, firstName, lastName, fullName )</tt>.
 *
 * markupbox : the <tt>MarkupBox</tt> to update
 * gameComponent : the component (such as a <tt>DIY</tt> instance) to extract a name from
 *
 */
function updateNameTags( markupbox, gameComponent ) {
    arkham.sheet.Sheet.setNamesForRenderer( markupbox, gameComponent.name, null, null );
}



/**
 * TextStyle
 * A <tt>TextStyle</tt> consists of one or more style attributes that can be
 * applied to text. Style attributes consist of a style key and an appropriate
 * value. If an attribute is added to a text style and the style already contains
 * an attribute with the same key, the new attribute replaces the old one.
 * (For example, if you add a new font family attribute to a style, it replaces the old
 * family, although other attributes such as size and weight would remain the same.)
 */

/**
 * TextStyle() [ctor]
 * This constructor creates a new, empty style.
 */

/**
 * TextStyle( key1, value1, [key2, value2, ...] ) [ctor]
 * This constructor creates a text style that combines the listed style attributes.
 *
 * key1, value1,&nbsp;... : pairs of text attribute keys and values
 */

/**
 * TextStyle.add( key1, value1, [key2, value2, ...] )
 * Merges the listed style attributes into this text style.
 *
 * key1, value1,&nbsp;... : pairs of text attribute keys and values
 */

/**
 * TextStyle.add( textStyle )
 * Merges the style represented by <tt>textStyle</tt> into this text style.
 *
 * textStyle : a model <tt>TextStyle</tt> style whose settings will be copied ...
 *             into this style
 */

/**
 * TextStyle.get( key )
 * Returns the value for the given style attribute key that will be set by this
 * style, or <tt>null</tt> if it does not modify that attribute.
 */

/**
 * TextStyle.remove( key )
 * Removes the style attribute with this key from this text style.
 */

/**
 * Style Attributes
 * Following are the style attribute keys (and the permitted values) that can
 * be set on a text style.
 */

/**
 * FAMILY
 * The value of this attribute is a string that names the font family to be used.
 * The following "standard family names" are defined for convenience:
 *
 * FAMILY_BODY : the standard Strange Eons body text font, if the core fonts ...
 *     library is installed (and otherwise a stand-in font)
 * FAMILY_SERIF : a default serif family
 * FAMILY_SANS_SERIF : a default sans-serif family
 * FAMILY_MONOSPACED : a default monospace family
 */

const FAMILY = TextAttribute.FAMILY;

// * FAMILY_AH_BODY : the family used for the body of text on Arkham Horror cards
// * FAMILY_AH_TITLE :  the family used for titles on Arkham Horror investigator cards
// * FAMILY_AH_CARD : the family used for the titles on small Arkham Horror cards
// * FAMILY_AH_LARGE_CARD : the family used for titles on large Arkham Horror cards
// * FAMILY_AH_GATE : the family used on Arkham Horror gate cards
//const FAMILY_AH_BODY = resources.ResourceKit.bodyFamily;
//const FAMILY_AH_TITLE = resources.ResourceKit.titleFamily;
//const FAMILY_AH_CARD = resources.ResourceKit.cardFamily;
//const FAMILY_AH_LARGE_CARD = resources.ResourceKit.largeCardFamily;
//const FAMILY_AH_GATE = resources.ResourceKit.gateFamily;

const FAMILY_BODY = ResourceKit.getBodyFamily();
const FAMILY_SERIF = "Serif";
const FAMILY_SANS_SERIF = "SansSerif";
const FAMILY_MONOSPACED = "Monospace";

/**
 * WEIGHT
 * The value of this attribute controls the weight of text. Not all weights
 * may have visible effect. The following standard weights are defined
 * (medium is the "normal" weight):
 * <pre>
 * WEIGHT_EXTRALIGHT
 * WEIGHT_LIGHT
 * WEIGHT_DEMILIGHT
 * WEIGHT_REGULAR
 * WEIGHT_SEMIBOLD
 * WEIGHT_MEDIUM
 * WEIGHT_DEMIBOLD
 * WEIGHT_BOLD
 * WEIGHT_HEAVY
 * WEIGHT_EXTRABOLD
 * WEIGHT_ULTRABOLD
 * </pre>
 *
 * <b>Note:</b> The correctness of this attribute relies
 * on underlying support from the platform and JRE. Some weights may have no
 * effect. (The regular and bold weights should always be available.)
 */

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

/**
 * WIDTH
 * The value of this attribute controls the width of text. The following
 * standard widths are defined (regular is the "normal" width):
 * <pre>
 * WIDTH_CONDENSED
 * WIDTH_SEMICONDENSED
 * WIDTH_REGULAR
 * WIDTH_SEMIEXTENDED
 * WIDTH_EXTENDED
 * </pre>
 */

const WIDTH = TextAttribute.WIDTH;

const WIDTH_CONDENSED = TextAttribute.WIDTH_CONDENSED;
const WIDTH_SEMICONDENSED = TextAttribute.WIDTH_SEMI_CONDENSED;
const WIDTH_SEMI_CONDENSED = TextAttribute.WIDTH_SEMI_CONDENSED;
const WIDTH_REGULAR = TextAttribute.WIDTH_REGULAR;
const WIDTH_SEMIEXTENDED = TextAttribute.WIDTH_SEMI_EXTENDED;
const WIDTH_SEMI_EXTENDED = TextAttribute.WIDTH_SEMI_EXTENDED;
const WIDTH_EXTENDED = TextAttribute.WIDTH_EXTENDED;

/**
 * POSTURE
 * The value of this attribute controls the posture of text.
 * The following standard postures are defined
 * (regular is the "normal" posture, oblique is the standard
 * italic posture):
 * <pre>
 * POSTURE_REGULAR
 * POSTURE_OBLIQUE
 * </pre>
 */

const POSTURE = TextAttribute.POSTURE;

const POSTURE_REGULAR = TextAttribute.POSTURE_REGULAR;
const POSTURE_OBLIQUE = TextAttribute.POSTURE_OBLIQUE;

/**
 * SIZE
 * The value of this attribute is a number that controls the point size of text.
 */

const SIZE = TextAttribute.SIZE;

/**
 * SUPERSCRIPT
 * The absolute value of this attribute is a number that specifies the number of levels
 * of superscripting or subscripting that are in effect (normally 0, positive
 * values specify superscripting, negative values specify subscripting). The
 * following standard values are defined:
 * SUPERSCRIPT_SUPER : one level of superscript
 * SUPERSCRIPT_SUB : one level of subscript
 */


const SUPERSCRIPT = TextAttribute.SUPERSCRIPT;

const SUPERSCRIPT_SUPER = TextAttribute.SUPERSCRIPT_SUPER;
const SUPERSCRIPT_SUB = TextAttribute.SUPERSCRIPT_SUB;

const FONT_OBJECT = TextAttribute.FONT;

/**
 * COLOUR (or COLOR)
 * A <tt>java.awt.Paint</tt> object that controls the text colour.
 */

const COLOUR = TextAttribute.FOREGROUND;
const COLOR = TextAttribute.FOREGROUND;
const PAINT = TextAttribute.FOREGROUND;

/**
 * BGCOLOUR (or BGCOLOR)
 * A <tt>java.awt.Paint</tt> object that controls the background colour
 * of text.
 */

const BGCOLOUR = TextAttribute.BACKGROUND;
const BGCOLOR = TextAttribute.BACKGROUND;
const BGPAINT = TextAttribute.BACKGROUND;

/**
 * UNDERLINE
 * This attribute's value controls text underlining. The standard value
 * <tt>UNDERLINE_ON</tt> is defined.
 */

const UNDERLINE = TextAttribute.UNDERLINE;
const UNDERLINE_ON = TextAttribute.UNDERLINE_ON;

/**
 * STRIKETHROUGH
 * This attribute's value controls text strikethrough. The standard value
 * <tt>STRIKETHROUGH_ON</tt> is defined.
 */
const STRIKETHROUGH = TextAttribute.STRIKETHROUGH;
const STRIKETHROUGH_ON = TextAttribute.STRIKETHROUGH_ON;

/**
 * SWAP_COLOURS (or SWAP_COLORS)
 * This attribute's value controls whether the foreground and background colours
 * are swapped. The standard value
 * <tt>SWAP_COLOURS_ON</tt> is defined.
 */

const SWAP_COLORS = TextAttribute.SWAP_COLORS;
const SWAP_COLORS_ON = TextAttribute.SWAP_COLORS_ON;
const SWAP_COLOURS = TextAttribute.SWAP_COLORS;
const SWAP_COLOURS_ON = TextAttribute.SWAP_COLORS_ON;

/**
 * LIGATURES
 * This attribute's value controls whether automatic ligature replacement is
 * enabled. The standard value <tt>LIGATURES_ON</tt> is defined.
 */

const LIGATURES = TextAttribute.LIGATURES;
const LIGATURES_ON = TextAttribute.LIGATURES_ON;

/**
 * TRACKING
 * The value of this attribute is a number that controls tracking (spacing between
 * letters). A value of 0 provides standard letter spacing. Positive values
 * increase the space between letters, while negative values move them closer
 * together. The following standard values are defined:
 *
 * TRACKING_TIGHT : provide tighter (i.e., closer together) than normal tracking
 * TRACKING_LOOSE : provide looser than normal tracking
 */
const TRACKING = TextAttribute.TRACKING;
const TRACKING_TIGHT = TextAttribute.TRACKING_TIGHT;
const TRACKING_LOOSE = TextAttribute.TRACKING_LOOSE;

/**
 * PageShape [class]
 * Shapes the left and right margins of drawn text.
 * A shape is defined in terms of insets relative to the original margins.
 * A <tt>MarkupBox</tt> will query the <tt>PageShape</tt> with a range of Y-positions
 * covered by a line of text, and the shape must return the narrowest (largest)
 * inset within that range.
 * Positive inset values always reduce the margins; negative values increase
 * them. A zero inset leaves the margin unchanged.
 *
 * To create a new shape, subclass <tt>PageShape</tt> and override
 * <tt>getLeftInset</tt> and <tt>getRightInset</tt>. Optionally, you can
 * also override <tt>debugDraw</tt> to draw lines indicating how the margins
 * have changed. If you do not override this method, then no margin lines
 * (dashed blue lines) will be drawn when the <i>Show Regions</i> plug-in is
 * activated.
 *
 * In addition to creating your own shapes, a number of predefined shapes
 * are also available to handle common cases.
 */

/**
 * PageShape.getLeftInset( y1, y2 )
 * Returns the narrowest (maximum) left edge inset between <tt>y1</tt> and <tt>y2</tt>.
 * When calling this method, the condition <tt>y1 &lt;= y2</tt> must hold.
 *
 * y1 : the top Y-coordinate in the range
 * y2 : the bottom Y-coordinate in the range
 */

/**
 * PageShape.getRightInset( y1, y2 )
 * Returns the narrowest (maximum) right edge inset between <tt>y1</tt> and <tt>y2</tt>.
 * When calling this method, the condition <tt>y1 &lt;= y2</tt> must hold.
 *
 * y1 : the top Y-coordinate in the range
 * y2 : the bottom Y-coordinate in the range
 */

/**
 * PageShape.debugDraw( g, rect )
 * Called when a markup box is drawn and <i>Show Regions</i> is activated.
 * This method should draw lines or curves to indicate the shape visually.
 * The passed-in graphics context will already have been initialized with
 * an appropriate stroke, paint, and transform.
 *
 * g : a <tt>Graphicsa2D</tt> instance that has been initialized with an appropriate scale and style
 * rect : the text drawing region to use to draw the margins for
 */

/**
 * PageShape.RECTANGLE_SHAPE
 * A shared immutable instance of a <tt><a href='#PageShape'>PageShape</a></tt> that defines the default
 * shape for text: insets are always 0, so text conforms to the region passed
 * to <tt>draw</tt>.
 */

/**
 * PageShape.InsetShape [class]
 * A <code>PageShape</code> that modifies that standard margin by a constant
 * amount for all Y-positions. This is often useful when creating a
 * <code>CompoundShape</code>.
 */

/**
 * PageShape.InsetShape( leftInset, rightInset ) [ctor]
 * Creates a new <tt>PageShape.InsetShape</tt> with constant margin insets
 * defined by <tt>leftMargin</tt> and <tt>rightMargin</tt>.
 *
 * leftMargin : the number of units to offset the left edge of the text region
 * rightMargin : the number of units to offset the right edge of the text region
 */

/**
 * PageShape.CupShape [class]
 * A <code>PageShape</code> that is optimized for the most common case:
 * a rectangle that becomes wider or narrower after a certain y-point
 * is reached, e.g.:
 *
 * <pre>
 * i1        i2              i1      i2
 *  +--------+                +------+
 *  |        |                |      |
 *  |        |       or       |      |
 *  +--+  +--+  y          +--+      |  y
 *     |  |                |         |
 *     +--+                +---------+
 *    i3  i4              i3         i4 (same as i2)
 * </pre>
 */

/**
 * PageShape.CupShape( i1, i2, y, i3, i4 ) [ctor]
 * Creates a new <tt>PageShape.CupShape</tt> that switches from insets
 * <tt>i1, i2</tt> to <tt>i3, i4</tt> at Y-position <tt>y</tt>.
 *
 * i1 : the left inset to use in the top part of the shape
 * i2 : the right inset to use in the top part of the shape
 * y : the Y-coordinate at which the insets switch from top to bottom
 * i3 : the left inset to use in the bottom part of the shape
 * i4 : the right inset to use in the bottom part of the shape
 */

/**
 * PageShape.CompoundShape [class]
 * A <tt>PageShape</tt> that switches between two <tt>PageShape</tt>s
 * at a specified Y-position. This may be used to build more complex shapes
 * from simpler ones. Either or both of the shapes may themselves
 * be <tt>CompoundShape</tt>s.
 *
 * The following example uses a compound shape to create a "plus" or
 * "double cup" shape, that is, a cup shape that flows around decorations
 * in up to four corners (as opposed to <tt>PageShape.CupShape</tt>, which
 * handles no more than two:
 * <pre>
 * // Create a "double cup" shape with this form:
 * //
 * //                 i1    i2
 * //                 +------+
 * //                 |      |
 * //             0 +-+      +-+ 0  y1
 * //               |          |
 * //             0 +---+  +---+ 0  y2
 * //                   |  |
 * //                   +--+
 * //                  i3  i4
 *
 * new PageShape.CompoundShape(
 *     new PageShape.CupShape( i1, i2, y1, 0, 0 ),
 *     y2,
 *     new PageShape.InsetShape( i3, i4 )
 * );
 * </pre>
 */

/**
 * PageShape.CompoundShape( topShape, y, bottomShape ) [ctor]
 * Creates a new <tt>PageShape.CompoundShape</tt> that uses <tt>topShape</tt>
 * until the Y-position <tt>y</tt> is reached, then switches to <tt>bottomShape</tt>.
 *
 * topShape : the shape to use until <tt>y</tt> is reached
 * y : the Y-position at which to switch shapes
 * bottomShape : the shape to use after <tt>y</tt> is reached
 */

/**
 * PageShape.MergedShape [class]
 * A shape that merges two source shapes into a single shape.
 * The resulting shape always uses the narrowest margin
 * of the two source shapes at any given Y-position.
 * For example, to ensure that a shape never has a negative
 * inset (which lets text escape from the drawing region),
 * merge it with <tt>PageShape.RECTANGLE_SHAPE</tt>.
 */

/**
 * PageShape.MergedShape( shape1, shape2 ) [ctor]
 * Creates a new <tt>PageShape.MergedShape</tt> that merges
 * <tt>shape1</tt> and <tt>shape2</tt> into a single shape.
 *
 * shape1 : a <tt>PageShape</tt>
 * shape2 : a second <tt>PageShape</tt> to be combined with <tt>shape1</tt>
 */

/**
 * PageShape.GeometricShape( outline ) [ctor]
 * A <tt>PageShape</tt> that redefines margins to match a shape used for
 * drawing (that is, a
 * <tt><a href='javadoc:java/awt/Shape.html'>java.awt.Shape</a></tt>).
 * This allows you to fit margins to an arbitrary polygon, an ellipse, etc. The
 * margins at a given Y point will be adjusted to touch the leftmost and
 * rightmost points on the shape at that Y position. If needed, the shape must
 * be transformed so that it intersects the text drawing region prior to
 * creating the <tt>PageShape</tt>. Creating a <tt>GeometricShape</tt> is a
 * fairly expensive operation, especially compared to other <tt>PageShape</tt>s.
 * It is recommended that you create them ahead of time and reuse them where
 * possible, instead of creating them on the fly in your painting code.
 *
 * outline : a <tt>Shape</tt> that defines an outline to create a <tt>PageShape</tt> from
 */
