/*

  diy.js - version 11
  Create your own game component editor with script code.


The SE JavaScript Library Copyright © 2008-2012
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

importClass( arkham.diy.DIY );
importClass( arkham.diy.DIYSheet );
const FaceStyle = DIY.FaceStyle;
const DeckSnappingHint = arkham.sheet.Sheet.DeckSnappingHint;
const HighResolutionMode = DIY.HighResolutionMode;

/**
 * A component type that is controlled by script code.
 * The DIY library defines a number of useful constants and the
 * <tt>testDIYScript()</tt> function, but most of the functionality is
 * provided by the <tt>DIY</tt> and <tt>DIYSheet</tt> classes.
 * This document provides an introduction to the <tt>DIY</tt> class.
 * For more complete information please refer to the pages for these classes
 * in the Strange Eons API section of the API browser.
 */

/**
 * DIY( scriptFileResource, [gameCode], [debug] ) [ctor]
 * Create a new <tt>DIY</tt> component using the script stored in <tt>scriptFileResource</tt>.
 * The script file must define the functions <tt>create</tt>, <tt>createInterface</tt>,
 * <tt>createFrontPainter</tt>, <tt>createBackPainter</tt>, <tt>paintFront</tt>,
 * <tt>paintBack</tt>, <tt>onRead</tt>, and <tt>onWrite</tt>, as described below.
 *
 * A DIY component is not usually created directly, but is created by the
 * New Component Dialog from DIY class map entries. (Or, during testing, created
 * by the <tt>testDIYScript()</tt> function.)
 * 
 * scriptFileResource : the URL of the script file that defines the component
 * gameCode : the short code identifying the game the component is for
 * debug : if true, a breakpoint will be set at the start of the script
 */

/**
 * create( diy )
 * This function is called when the component is first created
 * (before the <tt>DIY</tt> constructor returns).
 * The function serves two basic purposes. First, it must set the DIY's
 * basic configuration properties (number of sheets,
 * the base image to use for the card, and so on).
 * Second, it must set default values for all of
 * the properties that the user will be able to edit later
 * (like Name, Gender, Speed&mdash;whatever is appropriate for the card).
 *
 * The following are the basic configuration properties for
 * a <tt>DIY</tt> component. These properties can only be set inside of the
 * DIY's <tt>create()</tt> function (or inside of its <tt>onRead()</tt>):
 *
 * <div class='indent'>
 * <b>DIY.verison</b> (default <tt>1</tt>)<br>
 * Sets the version of this component type that is created by this script.
 * See <tt>onRead</tt> for a description of how a new version of an extension
 * can be made backwards compatible. Note that this is property is an integer
 * value. If you assign a value like "1.2" it will be silently rounded down.
 *
 * <b>DIY.extensionName</b> (default <tt>null</tt>)<br>
 * If set, this will be displayed to the user as part of the error message
 * if the script file cannot be found. If a user opens a component for which
 * the script file is not present, this allows Strange Eons to direct them
 * to the extension they need to install in order to open the file.
 *
 * <b>DIY.faceStyle</b> (default: <tt>FaceStyle.PLAIN_BACK</tt>)<br>
 * This controls the number and type of faces (sides) that the component provides.
 * The following values are possible:
 *
 * FaceStyle.ONE_FACE : The component has only one face.
 * FaceStyle.PLAIN_BACK : The component has a front face and a plain back face ...
 *     (a fixed image that is drawn automatically).
 * FaceStyle.SHARED_FACE : The component has two faces, but they are identical.
 * FaceStyle.TWO_FACES : The component has two different faces.
 * FaceStyle.CARD_AND_MARKER : The component has three faces (four in practice); ...
 *     the first two faces are typically the front and back of a card, while the ...
 *     third is one side of a marker (with identical obverse side). If the component ...
 *     has a portrait key set (see below), then the card and marker will share a ...
 *     potrait. This is most commonly used for character cards with matching ...
 *     character markers.
 * FaceStyle.FOUR_FACES : The component has four faces; for example, two closely ...
 *     related cards or a folding panel.
 *
 * If the style is set to <tt>ONE_FACE</tt> or <tt>SHARED_FACE</tt>, then any
 * properties that pertain to the card's back face (such as <tt>backTemplateKey</tt>)
 * will be ignored and <tt>paintBack()</tt> will never be called. If the style is
 * set to <tt>PLAIN_BACK</tt>, then the <tt>backTemplateKey</tt> should point to
 * an image resource for the back of the card. In this case again, <tt>paintBack()</tt>
 * will not be called (the card back will be created automatically from the image
 * resource). If the style is set to anything other than <tt>CARD_AND_MARKER</tt>,
 * then attempting to change or read any property associated with the market
 * will typically throw an <tt>IllegalStateException</tt>.
 *
 * <b>DIY.frontTemplateKey</b> (default: <tt>diy-front-sheet</tt>)<br>
 * This is the base name for a group of settings keys that describe the template
 * image for the front face of the component. The template image determines the
 * size of the component and is normally painted on the face to provide the
 * component's background. The following keys can be defined, but only the
 * first is required (where <tt><i>xxx</i></tt> is the base name provided by
 * <tt>frontTemplateKey</tt>):
 *
 * <i>xxx</i>-template : The resource file that contains the image.
 * <i>xxx</i>-ppi : If present, the resolution of the template image in ...
 *     pixels per inch (2.54cm) (default is <tt>150</tt>).
 * <i>xxx</i>-expsym-region : If present, the region where the expansion ...
 *     symbol is drawn (default is no expansion symbol). (It is also possible ...
 *     to add additional keys to position specific expansions.)
 * <i>xxx</i>-expsym-invert : The index of the expansion symbol variant to use ...
 *     by default (the default is index 0).
 * <i>xxx</i>-upsample : A factor to multiply the resolution by for previews ...
 *     (default is <tt>1.0</tt>; usually only changed when a component ...
 *     has very small text, about 6 points or less)
 *
 * The template image for a face can be drawn in its respective paint function
 * (<tt>paintFront</tt> or <tt>paintBack</tt>) by calling
 * <tt>sheet.paintTemplateImage( g )</tt>.
 *
 * <b>DIY.backTemplateKey</b> (default: <tt>diy-front-sheet</tt>)<br>
 * This property works the same as <tt>frontTemplateKey</tt>, but it controls
 * the template image for the back face rather than the front. The property
 * is ignored if the face style is <tt>FaceStyle.ONE_FACE</tt> or
 * <tt>FaceStyle.SHARED_FACE</tt>.
 *
 * <b>Note</b><br>
 * For faces after the first two, you must set the key using the
 * <tt>setTemplateKey( index, templateKey )</tt> function, which allows you to
 * set any face's template by its index. The properties <tt>frontTemplateKey</tt>
 * and <tt>backTemplateKey</tt> are just shorthand for setting the faces with
 * indices 0 and 1, respectively. For example, to set the template key for
 * the marker in a <tt>CARD_AND_MARKER</tt> component, you would use an index
 * of 2.
 *
 * <b>DIY.portraitKey</b> (default: <tt>null</tt>)<br>
 * If this is not <tt>null</tt>, then the card has a portrait image and the
 * editor will provide a tab for editing the portrait settings. The value is
 * the base name of a setting key. To use a portrait, the following settings
 * must also be defined (where <tt><i>xxx</i></tt> is the base name provided by
 * <tt>portraitKey</tt>):
 *
 * <i>xxx</i>-portrait-template : The resource file that contains the ...
 *     default portrait image.
 * <i>xxx</i>-portrait-clip-region : The region where the portrait ...
 *     will be drawn on the card.
 *
 * In addition, the following settings are optional. If they are not present, then
 * the portrait will default to a pan of (0,0) and the scale will be determined
 * automatically using the size of the portrait image and the clip region.
 *
 * <i>xxx</i>-portrait-scale : The scale setting for the default portrait ...
 *     image (where 1 = 100%).
 * <i>xxx</i>-portrait-panx : The number of units by which the default ...
 *     image is shifted horizontally (usually 0).
 * <i>xxx</i>-portrait-pany : The number of units by which the default ...
 *     image is shifted vertically (usually 0).
 *
 * <b>Note</b><br>
 * If the face style is <tt>FaceStyle.CARD_AND_MARKER</tt>, then you must
 * also define a clip region for the portrait as it is drawn on the marker,
 * and you may define scale and pan values for the default portrait image.
 * These keys use the same names as above, but replacing <i>portrait</i>
 * with <i>marker</i>. (The marker will use the same portrait image
 * as the card, but it will have its own scale and position.)
 *
 * <b>DIY.portraitBackgroundFilled</b> (default: <tt>true</tt>)</b><br>
 * If <tt>true</tt>, the portrait clip region will be filled in with solid
 * white before painting the portrait. This is usually set when the user is
 * expected to use portraits that have transparency, for example, if there is
 * a standard graphic that should appear behind the portrait image.
 *
 * <b>DIY.portraitScaleUsesMinimum</b> (default: <tt>false</tt>)</b><br>
 * This flag changes how the defualt scale is selected when the user chooses
 * a new portrait image. Normally, a portrait is scaled so that the
 * card's portrait clip region is just barely completely covered, so that no
 * background shows through. When this flag is set to <tt>true</tt>, the portrait
 * is scaled so that the entire image is just contained within the clip region.
 * This is useful when there is one standard background shared by all components
 * and the portraits themselves are a transparent "cut-out" that will be drawn over
 * this standard background.
 *
 * <b>DIY.portraitClipping</b> (default: <tt>true</tt>)</b><br>
 * If set to <tt>true</tt> (the default), then the portrait will be clipped to
 * the portrait clipping region: any part of the portrait that falls outside
 * of the clip region will not be drawn. When set to <tt>false</tt>, the
 * portrait clip region will only be used to determine the initial scale of a
 * portrait.
 *
 * <b>Note</b><br>
 * If the face style is <tt>FaceStyle.CARD_AND_MARKER</tt>, then you can also
 * control the background fill, default scaling method, and clipping for the
 * portrait when drawn on the marker using the same properties as above
 * but substituting <i>marker</i> for <i>portrait</i> in the property name.
 *
 * <b>DIY.customPortraitHandling</b> (default: <tt>false</tt>)<br>
 * If this is <tt>true</tt>, then the simplified portrait handling described above
 * will not be used. Instead, the DIY script must provide its own implementation
 * of the <tt>PortraitProvider</tt> interface (by implementing
 * the functions <tt>getPortraitCount</tt> and <tt>getPortrait( index )</tt>).
 * This allows you to create components with more complex portrait needs, such as
 * portraits that allow rotation, multiple portraits, or linked portraits
 * (a portrait that shares an image with another portrait, but has its own
 * pan and scale values, like the marker for a <tt>CARD_AND_MARKER</tt> component).
 *
 * If you wish to use custom portrait handling, then the methods for controlling
 * clipping, filling, and default scale will have no effect. However, you can
 * use the class <tt>DefaultPortriat</tt> to implement your custom portraits, which
 * provides analogous functionality.
 *
 * <b>Note:</b> When this is <tt>true</tt>, reading the <tt>portraitKey</tt>
 * will return <tt>null</tt> and attempting to change it to anything other than
 * <tt>null</tt> will result in an <tt>IllegalStateException</tt> being thrown.
 *
 * <b>DIY.transparentFaces</b> (default: <tt>false</tt>)</b><br>
 * This flag indicates that the face(s) of this card may contain transparent
 * and/or translucent pixels. This can be used to create faces with shaped
 * edges or holes. This flag should only be set when needed as transparent
 * faces require more processor and memory resources than opaque cards.
 *
 * <b>DIY.variableSizedFaces</b> (default: <tt>false</tt>)</b><br>
 * This flag indicates that the component may vary in size depending on its
 * content. The template image must be the maximum size that the card can
 * grow to. The final size of the face will be determined after it is painted by
 * cropping off any edges that are completely transparent (i.e., have an alpha value
 * of 0). Setting this flag to <tt>true</tt> automatically sets
 * <tt>DIY.transparentFaces</tt> to <tt>true</tt>.
 *
 * <b>DIY.deckSnappingHint</b> (default: <tt>DeckSnappingHint.CARD</tt>)</b><br>
 * This setting controls how this component will initially snap to other components
 * when it is in a deck. The possible values are:
 *
 * DeckSnappingHint.CARD : This is a card; it will have crop marks and snap ...
 *     to other cards and the page grid.
 * DeckSnappingHint.TILE : This is a tile (a basic building block for an ...
 *     expansion board). It will not have crop marks and it will snap to ...
 *     other tiles and the page grid.
 * DeckSnappingHint.OVERLAY : This component is overlaid over other ...
 *     components (usually tiles). Overlays do not snap to other components ...
 *     or the page grid and do not have crop marks.
 * DeckSnappingHint.INLAY : This is an inlay. Inlays snap to other inlays, ...
 *     tiles and the page grid, but when they snap to objects other than ...
 *     inlays they snap to the <i>inside</i> rather than the outside of the ...
 *     snapping edge.
 * DeckSnappingHint.OTHER : Objects with this hint snap only to other ...
 *     objects with this hint. They are not given crop marks.
 *
 * <b>DIY.bleedMargin</b> (default: 0)<br>
 * This indicates the size of an optional margin around the edges of a component
 * that is to be used as a bleed margin. A bleed margin is an area where the
 * graphics of a card continue outside of the normal boundary of the card.
 * This allows for imprecision when the card is cut by machine. If the machine
 * is misaligned by a small amount, the card content will be slightly off center but
 * it won't have a blank area along the edge(s) that would make the misalignment
 * obvious. When a margin value is set, the crop marks produced by the deck editor
 * are automatically adjusted accordingly.
 *
 * If a bleed margin is set, the same value is used on all four sides.
 * The size of the margin is measured in points (pt) and cannot be negative.
 * Typical bleed margin sizes in the publishing industry are 3 mm (8.5 pt)
 * or 1/8" (9 pt).
 *
 * Setting the margin value does not change the underlying graphics. You must
 * design your template image with a bleed margin in mind, adding pixels equal to
 * twice the margin size to both the width and height, and centering the "real"
 * card template in the larger space.
 * For example, if your template image is 150 DPI and you wish to include a
 * 9 pt margin, you must add:<br>
 * 150 pixels/in / 72 pt/in * 9 pt * 2 sides = 38 pixels (after rounding up)
 *
 * <b>DIY.highResolutionSubstitutionMode</b> (default: enabled)<br>
 * This controls whether high resolution image substitution is enabled.
 * When enabled, methods defined in <tt>DIYSheet</tt> that paint images
 * will automatically check for a copy of the image key that ends with
 * <tt>-hires</tt>. If that key exists, then that image will be substituted
 * when the card is being printed or exported at a resolution higher than
 * that of the card's template image. For example, suppose we provide the
 * following keys for a card's front template image:
 * <pre>
 * my-card-front-template = mycard/front-150.jp2
 * my-card-front-dpi = 150
 * my-card-front-template-hires = mycard/front-300.jp2
 * </pre>
 * Then when the card is printed or exported at more than 150 DPI, the
 * alternate template image will be substituted by
 * <tt>DIYSheet.paintTemplateImage</tt>.
 *
 * Changing this property allows you to force substitution on or off.
 * It is normally only changed for debugging purposes.
 * Unlike other properties listed here, this can be changed at any time.
 * The possible values are:
 *
 * HighResolutionMode.DISABLE : do not allow substitution
 * HighResolutionMode.ENABLE : allow substitution using the normal rules
 * HighResolutionMode.FORCE : always substitute if possible
 *
 * <b>DIY.setCustomFoldMarks( faceIndex, foldMarkVectors )</b> (default: none)<br>
 * Defines custom fold marks for a card face. These will be drawn with the face
 * when it is added to a deck.
 * The <tt>faceIndex</tt> defines the card face (0=front, 1=back) to add marks
 * for, although usually such a card would only have a front side.
 * The array <tt>foldMarkVectors</tt> consists of one or more pairs of points
 * (four numbers in sequence) in the order (px,py),(dx,dy). The point (px,py)
 * indicates the location of the start of the fold mark
 * (assuming that the distance from the card to the fold mark is 0).
 * The point is measured realtive to the width and height of the card.
 * For example, (0.5, 0.0) is located at the middle of the top edge of the card.
 * The point (dx,dy) is a unit vector that indicates the direction of the
 * mark. For example, the unit vector (0,-1) points straight up.
 * </div>
 */


/**
 * createInterface( diy, editor )
 * This function is called when the component is installed in an editor.
 * It allows you to build an interface to edit your component with, and
 * to install that interface on the editor. In most cases, you will use
 * a <tt>Bindings</tt> instance to link UI controls to private settings
 * on the card or other variables so that the component will be automatically
 * updated and redrawn as the user edits the component.
 * One exception to be aware of is that the editor for a
 * DIY component includes the special method <tt>setNameField( JTextComponent )</tt>.
 * If you set this to one of the text fields in your user interface, then it
 * will update the <tt>name</tt> field of the component for you automatically,
 * and users will be able to correctly use <tt>&lt;name&gt;</tt> tags in the
 * component's markup.
 *
 * diy : the custom component
 * editor : the editor that the UI should be added to
 */

/**
 * createFrontPainter( diy, sheet )
 * This function is called before the front sheet is painted for the
 * first time, and is called again if a new set of sheets is created for
 * the component. It allows you to initialize painting resources that
 * depend on the sheet being painted.
 *
 * If the component has more than two faces, this method will be called once
 * for each even-numbered face (0, 2, and so on). You can determine the
 * index of the face being initialized with <tt>sheet.sheetIndex</tt>.
 *
 * diy : the component being painted
 * sheet : the sheet instance that is responsible for drawing this face of the component
 */

/**
 * createBackPainter( diy, sheet )
 * This function is called before the back sheet is painted for the
 * first time, and is called again if a new set of sheets is created for
 * the component. It allows you to initialize painting resources that
 * depend on the sheet being painted. If the face style of the component
 * is other than <tt>FaceStyle.TWO_FACES</tt>, this method is never called.
 *
 * If the component has more than two faces, this method will be called once
 * for each odd-numbered face (1, 3, and so on). You can determine the
 * index of the face being initialized with <tt>sheet.sheetIndex</tt>.
 *
 * diy : the component being painted
 * sheet : the sheet instance that is responsible for drawing this face of ...
 *     the component
 */

/**
 * paintFront( g, diy, sheet )
 * This function is called when the front face of your component needs to be
 * painted.
 *
 * The graphics context <tt>g</tt> will be scaled to the resolution of
 * the card's template image (usually 150 pixels per inch/2.54 cm). This means
 * that 1 unit in the graphics context is equivalent to 1 pixel on your template
 * image. (If for some reason you need to know the true resolution that the card
 * is being drawn at, you can read it from <tt>sheet.effectiveSheetDotsPerInch</tt>.)
 *
 * If the component has more than two faces, this method will be called once
 * for each even-numbered face (0, 2, and so on). You can determine the
 * index of the face being initialized with <tt>sheet.sheetIndex</tt>.
 *
 * g : a <tt>Graphics2D</tt> context that can be used to paint the card face
 * diy : the component being painted
 * sheet : the sheet instance that is responsible for drawing this face of ...
 *     the component; it offers useful helper methods that can simplify painting
 */

/**
 * paintBack( g, diy, sheet )
 * This function is called when the back face of your component needs to be
 * painted.  If the face style of the component is other than
 * <tt>FaceStyle.TWO_FACES</tt>, this method is never called.
 *
 * If the component has more than two faces, this method will be called once
 * for each odd-numbered face (1, 3, and so on). You can determine the
 * index of the face being initialized with <tt>sheet.sheetIndex</tt>.
 *
 * g : a <tt>Graphics2D</tt> context that can be used to paint the card face
 * diy : the component being painted
 * sheet : the sheet instance that is responsible for drawing this face of ...
 *     the component; it offers useful helper methods that can simplify painting
 */

/**
 * onClear( diy )
 * This function is called when the user issues the <b>Clear</b> command to reset
 * the component to an empty state. It should be defined to set all of the
 * component's properties to a valid but "empty" value. The exact meaning of
 * empty is left up to the component designer. The card's name field
 * will be set to an empty string automatically, although you can change this
 * to something else in your <tt>onClear</tt> function. Any expansion symbol
 * set on the card will also be removed.
 *
 * diy : the component being cleared
 */

/**
 * onRead( diy, objectInputStream )
 * This function is called when your component is read from a stream disk,
 * such as when the user opens an <tt>.eon</tt> file containing your card or
 * adds it to a deck.
 *
 * One use of this function is to upgrade old cards if you introduce a
 * new version of the component. Here is the basic process for upgrading a card:
 * The <tt>DIY</tt> object has an integer <tt>version</tt> field which contains the
 * value 1 by default. When you introduce a new version of your component,
 * set this field to the next higher number in your <tt>create()</tt> function.
 * When the card is read in, check the version number and upgrade it if it is
 * from an older version.
 * For example, the second version of your component would include code like
 * this:
 * <pre>
 * function create( diy ) {
 *     diy.version = 2;
 * }
 * // ...
 * function onRead( diy, objectInputStream ) {
 *    if( diy.version < 2 ) {
 *        // set settings that are new in v2 to a default value
 *        // ...
 *        diy.version = 2; // this card is now the latest version
 *    }
 * }
 * </pre>
 *  diy : the component being read from disk
 *  objectInputStream : the stream from which the card is being loaded; for advanced use only
 */

/**
 * onWrite( diy, objectOutputStream )
 * This function is called when your component is written to file, for example,
 * when the user saves the component from the editor.
 * It is provided for advanced use only.
 */

/**
 * testDIYScript( [gameCode] )
 * Create and display a test instance <tt>DIY</tt> using functions in the
 * current script. This function is useful when developing a new <tt>DIY</tt>
 * custom component. For example, you can write the script functions for the
 * component in the Quickscript window, add <tt>testDIYScript()</tt> to the
 * end of the script, run the file, and an editor will be created and added
 * based on your script functions. (Be sure to remove the test function before
 * placing the script in your plug-in.)
 *
 * If this function is left in a plug-in bundle, it will display a warning
 * (if script warnings are enabled), but otherwise it has no effect.
 * It will only create a test component if run directly from a script editor.
 *
 * gameCode : an optional code for the game this component is from; ...
 *            this is needed if you wish to test expansion symbol painting ...
 *            or inherit from the game's master settings
 */
function testDIYScript( gameCode ) {
    if( sourcefile != "Quickscript" ) {
        Error.warn( "testDIYScript() left in plug-in bundle" );
		return;
	}
    var handler = new arkham.diy.Handler() {
		create: function _create(d) { useSettings(d); create(d); },
		createInterface: function _createInterface(d,e) { useSettings(d); createInterface(d,e); },
		createFrontPainter: function _createFrontPainter(d,s) { useSettings(d); createFrontPainter(d,s); },
		createBackPainter: function _createBackPainter(d,s) { useSettings(d); createBackPainter(d,s); },
		paintFront: function _paintFront(g,d,s) { useSettings(d); paintFront(g,d,s); },
		paintBack: function _paintBack(g,d,s) { useSettings(d); paintBack(g,d,s); },
        onClear: function _onClear(d) { useSettings(d); onClear(d); },
		onRead: function _onRead(d,i){ useSettings(d); onRead(d,i); },
		onWrite: function _onWrite(d,o){ useSettings(d); onWrite(d,o); },
		getPortraitCount: function _getPortraitCount(){ useSettings(diy); return getPortraitCount(); },
		getPortrait: function _getPortrait(i){ useSettings(diy); return getPortrait(i); }
	};
	var diy = DIY.createTestInstance( handler, gameCode );
	var ed = diy.createDefaultEditor();
	ed.testModeEnabled = true;
	Eons.addEditor( ed );
}

/**
 * DIY.createTestInstance( handler ) [static]
 * Create a <tt>DIY</tt> instance for testing purposes. Note that if this
 * instance is saved to a file, you will not be able to reopen the file.
 * This method lets you create a <tt>DIY</tt> without reference to an external
 * script file. To create the test instance, you must provide an implementation
 * of the <tt>arkham.diy.Handler</tt> interface, which contains the same functions
 * as those that would be present in an external script file. For example:
 * <pre>
 * var diy = DIY.createTestInstance( new arkham.diy.Handler() {
 *     create: function() { println( "create" ); },
 *     createInterface: function() { println( "createInterface" ); },
 *     createFrontPainter: function() { println( "createFrontPainter" ); },
 *     createBackPainter: function() { println( "createBackPainter" ); },
 *     paintFront: function() { println( "paintFront" ); },
 *     paintBack: function() { println( "paintBack" ); },
 *     onClear: function() { println( "onClear" ); },
 *     onRead: function() { println( "onRead" ); },
 *     onWrite: function() { println( "onWrite" ); },
 * });
 * var ed = diy.createDefaultEditor();
 * Eons.addEditor( ed );
 * </pre>
 * See <tt>testDIYScript()</tt>
 */

/**
 * DIYSheet [class]
 * This is the class of all objects that are passed to a DIY card using the
 * <tt>sheet</tt> argument. You do not create <tt>DIYSheets</tt> yourself;
 * this is handled automatically. For a complete description, see the
 * <a href='javadoc:ca/cgjennings/apps/arkham/diy/DIYSheet'>DIYSheet class documentation</a>.
 * The following list is a summary of commonly used properties and methods
 * available through sheet objects:
 */

/**
 * DIYSheet.applyQualityHints( g )
 * This method can be called from a painting function to initialize a
 * graphics context in a standardized way based on the current rendering target
 * and quality. For example, it may enable high quality image interpolation
 * when exporting a card.
 * You do not need to call this for the graphics context that is passed to
 * your painting function, as it will already be initialized unless the
 * advanced feature <code>DIY.OPT_NO_QUALITY_INIT</code> is set.
 * However, it is useful if you need to create other graphics contents,
 * for example, if you create a draw on a temporary image.
 *
 * g : the graphics context to initialize
 */

/**
 * DIYSheet.templateResolution
 * The base resolution of the face. If no resolution key was set for the template
 * image, this will be 150 pixels per inch (approximately 59 pixels per cm).
 * The physical size (in inches) of the face is equal to the width and height of the
 * template image (in pixels) divided by the resolution. (If the face is
 * transparent, the size is determined <i>after</i> trimming transparent edges.)
 */

/**
 * DIYSheet.paintingResolution
 * The resolution that the sheet is set to render at, as opposed to the
 * "natural" resolution of the template image that was set when the DIY
 * was created. For example, if the user is exporting the card at 300 ppi,
 * then this will be 300.
 */

/**
 * DIYSheet.highResolutionRendering
 * This property is <tt>true</tt> if high resolution substitution is active.
 * The value is only guaranteed to be accurate when called from
 * <tt>paintFront</tt> or <tt>paintBack</tt>.
 */

/**
 * DIYSheet.paintImage( g, imageKey, x, y )
 * Paints an image at its normal size at the specified location.
 * This method will perform automatic high resolution image substitution
 * if there is a key with same name as <code>key</code> but with
 * <code>"-hires"</code> appended.
 *
 * g : the graphics context to use for painting
 * imageKey : the settings key of the image
 * x : the horizontal offset from the left side of the template image
 * y : the vertical offset from the top edge of the template image
 */

/**
 * DIYSheet.paintImage( g, imageKey, x, y, width, height )
 * Paints an image at the specified location and size.
 * This method will perform automatic high resolution image substitution
 * if there is a key with same name as <code>key</code> but with
 * <code>"-hires"</code> appended.
 *
 * g : the graphics context to use for painting
 * imageKey : the settings key of the image
 * x : the horizontal offset from the left side of the template image
 * y : the vertical offset from the top edge of the template image
 * width : the width of the painted image; the image is resized to fit if required
 * height : the height of the painted image; the image is resized to fit if required
 */

/**
 * DIYSheet.paintImage( g, sharedKey )
 * Paints an image at a location and size that are taken from a region
 * setting. The image resource is determined from the value of
 * <tt>sharedKey</tt>, while the region is obtained by appending
 * <tt>"-region"</tt> to the shared key name.
 * This method will perform automatic high resolution image
 * substitution if there is a key with same name as <code>sharedKey</code> but with
 * <code>"-hires"</code> appended.
 *
 * g : the graphics context to use for painting
 * sharedKey : the settings key of the image, and base name of the region key
 */

/**
 * DIYSheet.paintImage( g, imageKey, regionKey )
 * Paints an image at a location and size that are taken from a region
 * setting. This method will perform automatic high resolution image
 * substitution if there is a key with same name as <tt>key</tt> but with
 * <tt>"-hires"</tt> appended.
 *
 * g : the graphics context to use for painting
 * imageKey : the settings key of the image
 * regionKey : the settings key of the region where the image should be ...
 *     drawn, without the <tt>"-region"</tt> suffix
 */

/**
 * DIYSheet.paintTemplateImage( g )
 * Paints the card template image that was set when the card was created.
 * This method will perform automatic high resolution image substitution
 * if there is a key with same name as the template key but with
 * <code>"-hires"</code> appended.
 *
 * g : the graphics context to use for painting
 */

/**
 * DIYSheet.paintPortrait( g )
 * Paints the standard portrait using the settings keys specified during
 * DIY creation and the current portrait settings.
 */

/**
 * DIYSheet.paintMarkerPortrait( g )
 * Paints the standard portrait using the portrait settings for the marker.
 * If the face style is not <tt>CARD_AND_MARKER</tt>, this will throw an
 * <tt>IllegalStateException</tt>.
 */

/**
 * DIYSheet.drawTitle( g, text, region, font, size, alignment )
 * Draws a string within the specified region. This can be used to draw
 * fixed titles. (Unlike a markup box, it does not process any tags.)
 * The text will be centered vertically and will be aligned
 * horizontally according to the value of <tt>alignment</tt>.
 * This is one of: <tt>DIYSheet.ALIGN_LEFT</tt>, <tt>DIYSheet.ALIGN_CENTER</tt>,
 * <tt>DIYSheet.ALIGN_RIGHT</tt>, <tt>DIYSheet.ALIGN_LEADING</tt>, or
 * <tt>DIYSheet.ALIGN_TRAILING</tt>.
 *
 * g : the graphics context to use for painting
 * text : the string to draw
 * region : the region to draw the text in
 * font : the font to use
 * size : the point size of the text
 * alignment : the horizontal alignment of the text within <tt>region</tt>
 */

/**
 * DIYSheet.fitTitle( g, text, region, font, maxSize, alignment )
 * Draws a string within the specified region. This is equivalent to
 * <tt>drawTitle</tt>, except that if the text is too large for
 * the rectangle at the requested size, it will be shrunk to fit.
 * This is one of: <tt>DIYSheet.ALIGN_LEFT</tt>, <tt>DIYSheet.ALIGN_CENTER</tt>,
 * <tt>DIYSheet.ALIGN_RIGHT</tt>, <tt>DIYSheet.ALIGN_LEADING</tt>, or
 * <tt>DIYSheet.ALIGN_TRAILING</tt>.
 *
 * g : the graphics context to use for painting
 * text : the string to draw
 * region : the region to draw the text in
 * font : the font to use
 * maxSize : the maximum point size of the text
 * alignment : the horizontal alignment of the text within <tt>region</tt>
 */

/**
 * DIYSheet.drawOutlinedTitle( g, text, region, font, maxSize, outlineSize, textColor, outlineColor, alignment, outlineUnderneath )
 * Draws a string within the specified region. This is similar to
 * <tt>fitTitle</tt>, but each glyph (character) will be outlined in another colour.
 * The text will be centered vertically and will be aligned
 * horizontally according to the value of <tt>alignment</tt>.
 * This is one of: <tt>DIYSheet.ALIGN_LEFT</tt>, <tt>DIYSheet.ALIGN_CENTER</tt>,
 * <tt>DIYSheet.ALIGN_RIGHT</tt>, <tt>DIYSheet.ALIGN_LEADING</tt>, or
 * <tt>DIYSheet.ALIGN_TRAILING</tt>.
 *
 * g : the graphics context to use for painting
 * text : the string to draw
 * region : the region to draw the text in
 * font : the font to use
 * maxSize : the point size of the text
 * outlineSize : the size of the outline
 * textColor : the colour of the glyph interiors
 * outlineColor : the colour of the glyph outlines
 * alignment : the horizontal alignment of the text within <tt>region</tt>
 * outlineUnderneath: if <tt>true</tt>, the outline is drawn under the text, ...
 *     otherwise over it
 */

/**
 * DIYSheet.drawRotatedTitle( g, text, region, font, maxSize, alignment, turns )
 * Draws a string within the specified region but the text is rotated the
 * specified number of 90 degree anticlockwise turns. The constants
 * <tt>DIYSheet.ROTATE_NONE</tt>, <tt>DIYSheet.ROTATE_LEFT</tt>,
 * <tt>DIYSheet.ROTATE_RIGHT</tt>, and <tt>DIYSheet.ROTATE_UPSIDE_DOWN</tt>
 * may also be used to specify an orientation.
 * The text is drawn as a single line and it does not interpret markup tags.
 * The interpretation of alignment is rotated along with the text.
 * For example, if the text is rotated left, then a left alignment will
 * align the text to the bottom of the region.
 *
 * g : the graphics context to use for painting
 * text : the string to draw
 * region : the region to draw the text in
 * font : the font to use
 * maxSize : the maximum point size of the text
 * alignment : the horizontal alignment of the text within <tt>region</tt>
 */

/**
 * DIYSheet.drawRegionBox( g, region )
 * Draws a highlight box around a region. Sometimes useful for debugging.
 * g : the graphics context to use for painting
 * text : the string to draw
 * region : the region to highlight
 */
