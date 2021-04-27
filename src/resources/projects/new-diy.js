useLibrary( 'diy' );
useLibrary( 'ui' );
useLibrary( 'markup' );


function create( diy ) {
	// this is where the component's basic settings would be changed,
	// but we are sticking with the defaults---see the diy library
	// documentation

	// set the default name; DIY components have support for a name
	// and comments built in---other attributes are added manually
	diy.name = 'Joe';
	// set the default value of the custom Countdown attribute
	// by writing it into the component's private settings
	// [this is the same as writing Patch.card( diy, 'Countdown', '1' );]
	$Countdown = '0';
}

function createInterface( diy, editor ) {
	var nameField = textField( 'X', 30 );
	var counter = spinner( 0, 9 );

	var panel = new FixedGrid( 2 );
	panel.add( 'Name', nameField );
	panel.add( 'Count', counter );
	panel.setTitle( 'Name' );

	var bindings = new Bindings( editor, diy );
	// Here 'Countdown' is the *name* of the setting key
	bindings.add( 'Countdown', counter, [0] );

	// tell the DIY which control will hold the component's name
	// (the DIY has special support for a component's name and
	// no binding is required)
	diy.setNameField( nameField );
	panel.addToEditor( editor, 'Name' );
}

var textBox;

function createFrontPainter( diy, sheet ) {
	textBox = markupBox( sheet );
}

function createBackPainter( diy, sheet ) {
	// this won't be called because the default face style
	// is a plain (unpainted) card back [FaceStyle.PLAIN_BACK]
	// in fact, we could leave this function out altogether;
	// look out for this when writing your own scripts
	// (a do-nothing function will be created to stand in
	// for any missing DIY functions, so if one of your functions
	// doesn't seem to be getting called, check the spelling
	// carefully)
}


function paintFront( g, diy, sheet ) {
	// paint the image that was specified as the face's
	// template key (we're using the default)
	sheet.paintTemplateImage( g );

	g.setPaint( Color.BLACK );
	var text = 'Hello, ' + diy.name;
	var count = new Number( $Countdown );
	for( var i=count; i>=0; --i ) {
		text += '\n' + i;
	}

	textBox.markupText = text;
	textBox.draw( g, new Region( 20, 20, 200, 340 ) );
}

function paintBack( g, diy, sheet ) {
	// like createBackPainter(), this won't be called because of
	// the type of card we created
}

function onClear() {
	// this us called when the user issues a Clear command;
	// you should reset all of of the card's attributes to
	// a neutral state (the name and comments are cleared for you)
    $Countdown = '1';
}

// These can be used to perform special processing during open/save.
// For example, you can seamlessly upgrade from a previous version
// of the script.
function onRead() {}
function onWrite() {}

// This is part of the diy library; calling it from within a
// script that defines the needed functions for a DIY component
// will create the DIY from the script and add it as a new editor;
// however, saving and loading the new component won't work correctly.
// This means you can test your script directly by running it without
// having to create a plug-in (except to make any required resources
// available).
if( sourcefile == 'Quickscript' ) {
	testDIYScript();
}