/*

  prefab.js - version 1
  Create diy components that use common layout patterns with a
  minimum of effort.

The SE JavaScript Library Copyright Â© 2008-2013
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

/**
 * Creates a DIY component based on a common layout with a minimum of coding effort.
 * The prefab library includes all the script code needed to create a basic
 * DIY component that includes a title text area, a body text area, and optionally
 * a portrait. All you need to supply is the name of a base setting key, and the
 * library will do the rest, reading everything necessary for the component from
 * settings.
 *
 * Although no coding is required, the prefab library provides hooks for all of the
 * standard DIY functions that allow you to customize behaviour or to extend the libary
 * to create new prefab layouts. For each standard DIY function, you can define a "before"
 * and/or "after" hook which will be called at the start and end of the prefab library's
 * implementation of that function. For example, to customize the basic DIY configuration,
 * which would normally happen in the <tt>create</tt> function, you would define the
 * function <tt>afterCreate( diy )</tt>, which would be called after the prefab library
 * finished its configuration. You can also replace the default implementation completely
 * by assigning a replacement function: <tt>create = function myCreate( diy ) { ... }</tt>.
 */
/**
 * Configuring Prefab Components
 * To create a prefab component, a diy script must use the prefab library,
 * then set the value of the variable <tt>pfBaseKey</tt> to a base name
 * for a family of keys that will be used to configure the component. Note that
 * every key has a default value; it is only necessary to define keys that
 * differ from the default.
 *
 * <table border=0>
 * <tr><th>Key Suffix            <th>Effect or Purpose                                 <th>Default</tr>
 *
 * <tr><th colspan=3>The following keys describe the front face of the component. They are all optional except for <i>-front-template</i>.</tr>
 * <tr><td>-front-template       <td>Background image of front face.                   <td>null, but templates/diy-default.png if default <tt>pfBaseKey</tt> is used</tr>
 * <tr><td>-front-template-hires <td>Optional double resolution version of the background image for print/export. <td>null</tr>
 * <tr><td>-front-ppi            <td>Background image resolution in pixels per inch.   <td>150</tr>
 * <tr><td>-front-expsym-region  <td>Location of expansion symbol(s) on front face.    <td>null</tr>
 * <tr><td>-front-expsym-invert  <td>Index of the expansion symbol logical variant to use. <td>0</tr>
 *
 * <tr><th colspan=3>The following keys describe the back face of the component. If the template key is defined,
 *                   the component will use the <tt>PLAIN_BACK</tt> face style; otherwise it will be <tt>SHARED_FACE</tt>.
 *                   All of the keys are optional.</tr>
 * <tr><td>-back-template        <td>Background image of back face; makes component. <tt>PLAIN_BACK</tt> type.    <td>null</tr>
 * <tr><td>-back-template-hires  <td>Optional double resolution version of the background image for print/export. <td>null</tr>
 * <tr><td>-back-ppi             <td>Background image resolution in pixels per inch.   <td>150</tr>
 * <tr><td>-back-expsym-region   <td>Location of expansion symbol(s) on back face.    <td>null</tr>
 * <tr><td>-back-expsym-invert   <td>Index of the expansion symbol variant to use.     <td>0</tr>
 *
 * <tr><th colspan=3>The following keys control whether, and where, a portrait image is drawn on the component.</tr>
 * <tr><td>-portrait-template    <td>Image to use as the default portrait image; if null, no portrait appears. <td>null</tr>
 * <tr><td>-portrait-clip-region <td>The region that the portrait image is drawn within. <td>null</tr>
 * <tr><td>-portrait-scale       <td>The scale to use for the default portrait.          <td>fit automatically</tr>
 * <tr><td>-portrait-panx        <td>The x-offset to use for the default portrait.       <td>0</tr>
 * <tr><td>-portrait-pany        <td>The y-offset to use for the default portrait.       <td>0</tr>
 * <tr><td>-portrait-overlay     <td>Image to draw over the portrait. If not present,
 *              the portrait is drawn first, then the template. Otherwise, the template
 *              is drawn, followed by the portrait, and finally the overlay image.       <td>null</tr>
 * <tr><td>-portrait-overlay-region<td>Region covered by the overlay image.              <td>cover the entire card</tr>
 * <tr><td>-portrait-on-back     <td>Boolean setting; if true, the portrait appears on the back face. <td>false</tr>
 *
 * <tr><th colspan=3>The following keys control the initial content and labels for the component's text fields. They are all optional.</tr>
 * <tr><td>-name                 <td>The initial name used for new components.         <td>empty string</tr>
 * <tr><td>-content              <td>The initial body content used for new components. <td>empty string</tr>
 * <tr><td>-name-label           <td>The label used for the name text field.           <td>@prefab-l-name</tr>
 * <tr><td>-content-label        <td>The label used for the body content text field.   <td>@prefab-l-content</tr>
 * <tr><td>-tab-label            <td>The label used for the tab added to the editor.   <td>@prefab-l-tab</tr>
 * <tr><td>-panel-title          <td>The title applied to the layout panel containing the controls. <td>null</tr>
 *
 * <tr><th colspan=3>The following keys control the location and style of the title text.</tr>
 * <tr><td>-name-region         <td>Region where the title is drawn; if null, there will be no title field. <td>null</tr>
 * <tr><td>-name-oneliner       <td>Boolean setting; if true, the title is drawn as a single line.    <td>null (false)</tr>
 * <tr><td>-name-alignment      <td>Text alignment of the title text.                 <td>centre, middle</tr>
 * <tr><td>-name-style          <td>Text style of the title text.                     <td>null (uses default style for new markup boxes)</tr>
 *
 * <tr><th colspan=3>The following keys control the location and style of the content text.</tr>
 * <tr><td>-content-region         <td>Region where the content is drawn; if null, there will be no content field. <td>null</tr>
 * <tr><td>-content-alignment      <td>Text alignment of the content text.             <td>centre, top</tr>
 * <tr><td>-content-style          <td>Text style of the content text.                 <td>null (uses default style for new markup boxes)</tr>
 *
 * </table>
 */

useLibrary( 'diy' );
useLibrary( 'ui' );
useLibrary( 'markup' );

var pfBaseKey = 'prefab';

/**
 * pfDIY
 * This variable holds the DIY component.
 */
var pfDIY;

/**
 * pfSettings
 * This variable holds the component's private settings.
 */
var pfSettings;

/**
 * pfTitleBox
 * This variable holds the markup box used to lay out the
 * card title, or <tt>null</tt> if there is no title.
 * It is set during <tt>createFrontPainter</tt>.
 */
var pfTitleBox;
/**
 * pfContentBox
 * This variable holds the markup box used to lay out the
 * content text, or <tt>null</tt> if there is no content text.
 * It is set during <tt>createFrontPainter</tt>.
 */
var pfContentBox;

/*
 * Called to set up global variables once the DIY instance is known.
 */
function pfInit( diy ) {
	pfDIY = diy;
	pfSettings = diy.settings;
}

/*
 * Composes a full key name from the base name
 * (pfBaseKey) and a suffix.
 */
function pfKey( suffix ) {
	return pfBaseKey + suffix;
}

/*
 * Returns a string looked up from the DIY's settings
 * based on the base key (pfFrontKey). If the value
 * starts with '#', '@', or '$', the true value is
 * looked up as if the value were a global variable
 * (i.e., as a game string, interface string, or setting).
 */
function pfString( key, defaultValue ) {
	key = pfKey( key );
	var value = pfSettings.get( key );
	if( value == null ) {
		value = defaultValue === undefined ? '' : defaultValue;
	}
	if( value != null ) {
		if( value.startsWith( '#' ) || value.startsWith( '@' ) || (value.startsWith( '$' ) && !value.startsWith( '$$' )) ) {
			value = global[ value ];
		}
	}
	return value;
}





function create( diy ) {
	if( pfBaseKey == null ) throw new Error( 'pfBaseKey not defined' );

	pfInit( diy );
	if( global['beforeCreate'] !== undefined ) global.beforeCreate( diy );

	// get front and back face keys; define front key if required
	var pfFrontKey = pfKey( '-front' );
	var pfBackKey = pfKey( '-back' );
	var pfFrontTemplate = pfFrontKey + '-template';
	var pfBackTemplate = pfBackKey + '-template';

	if( pfSettings.get( pfFrontTemplate ) == null ) {
		throw new Error( 'front template key not defined: ' + pfFrontKey );
	}

	// card type is determined by whether the back key is defined
	diy.frontTemplateKey = pfFrontKey;
	if( pfSettings.get( pfBackTemplate ) == null ) {
		diy.faceStyle = FaceStyle.SHARED_FACE;
	} else {
		diy.backTemplateKey = pfBackKey;
		if( pfSettings.getYesNo( pfKey( '-portrait-on-back' ) ) && pfSettings.get( pfKey( '-portrait-template' ) ) != null ) {
			diy.faceStyle = FaceStyle.TWO_FACES;
		} else {
			diy.faceStyle = FaceStyle.PLAIN_BACK;
		}
	}

	diy.name = pfString( '-name' );
	$Content = pfString( '-content' );

	if( pfSettings.get( pfKey( '-portrait-template' ) ) != null ) {
		diy.portraitKey = pfBaseKey;
	}

	if( global['afterCreate'] !== undefined ) global.afterCreate( diy );
}





function createInterface( diy, editor ) {
	if( global['beforeCreateInterface'] !== undefined ) global.beforeCreateInterface( diy, editor );

	var bindings = new Bindings( editor, diy );
	var panel = new TypeGrid();


	var hasName = false;
	if( pfSettings.get( pfKey( '-name-region' ) ) != null ) {
		hasName = true;
		var nameField = textField();
		var nameLabel = label( pfString( '-name-label', '@prefab-l-name' ) );
		nameLabel.labelFor = nameField;
		diy.nameField = nameField;
		panel.place( nameLabel, '', nameField, 'hfill' );
	}

	var hasContent = false;
	if( pfSettings.get( pfKey( '-content-region' ) ) != null ) {
		hasContent = true;
		var contentField = textArea( null, 15, 0, true );
		var contentLabel = label( pfString( '-content-label', '@prefab-l-content' ) );
		contentLabel.labelFor = contentField;
		bindings.add( 'Content', contentField, [0] );
		panel.place( contentLabel, hasName ? 'p' : '', contentField, 'br hfill' );
	}

	panel.title = pfString( '-panel-title', null );

	if( global['afterCreateInterface'] !== undefined ) global.afterCreateInterface( diy, editor, panel, bindings );

	bindings.bind();
	if( hasName || hasContent || global['afterCreateInterface'] !== undefined ) {
		panel.addToEditor( editor, pfString( '-tab-label', '@prefab-l-tab' ), null, null, 0 );
	}
}





function createFrontPainter( diy, sheet ) {
	if( global['beforeCreateFrontPainter'] !== undefined ) global.beforeCreateFrontPainter( diy, sheet );

	var makeBox = function( key, isTitle ) {
		var baseKey = pfKey( key );
		// don't create a box if the region isn't defined
		if( pfSettings.get( baseKey + '-region' ) == null ) {
			return null;
		}

		var box = markupBox( sheet );
		if( pfSettings.get( baseKey + '-alignment' ) == null ) {
			box.alignment = LAYOUT_CENTER | (isTitle ? LAYOUT_MIDDLE : LAYOUT_TOP);
		} else {
			pfSettings.getTextAlignment( baseKey, box );
		}
		if( pfSettings.get( baseKey + '-style' ) != null ) {
			pfSettings.getTextStyle( baseKey, box.defaultStyle );
		}
		return box;
	};

	pfTitleBox = makeBox( '-name', true );
	pfContentBox = makeBox( '-content' );

	if( global['afterCreateFrontPainter'] !== undefined ) global.afterCreateFrontPainter( diy, sheet );
}





function createBackPainter( diy, sheet ) {
	if( global['beforeCreateBackPainter'] !== undefined ) global.beforeCreateBackPainter( diy, sheet );
	if( global['afterCreateBackPainter'] !== undefined ) global.afterCreateBackPainter( diy, sheet );
}





function paintFront( g, diy, sheet ) {
	if( global['beforePaintFront'] !== undefined ) global.beforePaintFront( g, diy, sheet );

	g.setPaint( Colour.BLACK );

	var hasOverlay = false;
	if( pfSettings.get( pfKey( '-portrait-template' ) ) != null && !pfSettings.getYesNo( pfKey( '-portrait-on-back' ) ) ) {
		if( pfSettings.get( pfKey( '-portrait-overlay' ) ) == null ) {
			sheet.paintPortrait( g );
		} else {
			hasOverlay = true;
		}
	}
	sheet.paintTemplateImage( g );
	if( hasOverlay ) {
		sheet.paintPortrait( g );
	}

	if( pfTitleBox != null ) {
		pfTitleBox.markupText = diy.name;
		if( pfSettings.getYesNo( pfKey( '-name-oneliner' ) ) ) {
			pfTitleBox.drawAsSingleLine( g, pfSettings.getRegion( pfKey( '-name' ) ) );
		} else {
			pfTitleBox.draw( g, pfSettings.getRegion( pfKey( '-name' ) ) );
		}
	}

	if( pfContentBox != null ) {
		pfContentBox.markupText = $Content;
		pfContentBox.draw( g, pfSettings.getRegion( pfKey( '-content' ) ) );
	}

	if( hasOverlay ) {
		pfPaintOverlay( g, sheet );
	}

	if( global['afterPaintFront'] !== undefined ) global.afterPaintFront( g, diy, sheet );
}





function paintBack( g, diy, sheet ) {
	if( global['beforePaintBack'] !== undefined ) global.beforePaintBack( g, diy, sheet );

	// NOTE WELL: Currently, this function is ONLY called if there is a portrait
	// painted on the back face, so the code assumes that this is the case.
	// If that changes, the code must be updated accordingly.

	var overlayKey = pfKey( '-portrait-overlay' );
	if( pfSettings.get( overlayKey ) == null ) {
		sheet.paintPortrait( g );
		sheet.paintTemplateImage( g );
	} else {
		sheet.paintTemplateImage( g );
		sheet.paintPortrait( g );
		pfPaintOverlay( g, sheet );
	}

	if( global['afterPaintBack'] !== undefined ) global.afterPaintBack( g, diy, sheet );
}


function pfPaintOverlay( g, sheet ) {
	var overlayKey = pfKey( '-portrait-overlay' );
	var regionKey = pfKey( '-portrait-overlay-region' );
	if( pfSettings.get( regionKey ) == null ) {
		sheet.paintImage( g, overlayKey, 0, 0, sheet.templateWidth, sheet.templateHeight );
	} else {
		sheet.paintImage( g, overlayKey, pfKey( '-portrait-overlay-region' ) );
	}
}




function onClear( diy ) {
	if( global['beforeOnClear'] !== undefined ) global.beforeOnClear( diy );
	diy.name = '';
	$Content = '';
	if( global['afterOnClear'] !== undefined ) global.afterOnClear( diy );
}





function onRead( diy, ois ) {
	var prefabVersion = ois.readInt();
	pfInit( diy );
	if( global['beforeOnRead'] !== undefined ) global.beforeOnRead( diy, ois );
	if( global['afterOnRead'] !== undefined ) global.afterOnRead( diy, ois );
}





function onWrite( diy, oos ) {
	oos.writeInt( 1 );
	if( global['beforeOnWrite'] !== undefined ) global.beforeOnWrite( diy, oos );
	if( global['afterOnWrite'] !== undefined ) global.afterOnWrite( diy, ois );
}





if( sourcefile == 'Quickscript' ) {
	// invokeLater gives the library's user a chance to set pfBasekey
	java.awt.EventQueue.invokeLater( testDIYScript );
}