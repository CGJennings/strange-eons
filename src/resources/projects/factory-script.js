useLibrary( 'imageutils' );
useLibrary( 'threads' );

importClass( java.io.File );
importPackage( arkham );
importPackage( arkham.project );
importPackage( arkham.component );
importPackage( arkham.diy );
importPackage( gamedata );

const CONFIRM_MESSAGE = string( 'pt-factory-scripted-make-confirm', task.name );


/**
 * make( progress )
 *   Creates a set of components and saves them in the factory task
 *   folder.
 *
 *   progress : a progress window
 */
function make( progress ) {
	for( let i=0; i<10; ++i ) {
		var fileName = 'example-' + (i+1) + '.eon';

		// update the user with the name of what we're working on
		progress.status = fileName;

		// create an actual component any way you wish
		var gc = makeComponent( i );

		// save the new component
		try {
			var outputFile = new File( task.file, fileName );
			ResourceKit.writeGameComponentToFile( outputFile, gc );
		} catch( ex ) {
			// print information about the error without stopping the make
			Console.err.println( string( 'pt-factory-scripted-err', fileName ) );
			Error.handleUncaught( ex );
		}

		// check if the user pressed Cancel
		if( progress.cancelled ) break;
	}
	// update the contents of the task folder immediately
	task.synchronize();
}

/**
 * makeComponent( index )
 *   This is called from main() to make an example component.
 *   You can create any kind of component you like here, using
 *   any method you like. This is just a very basic example.
 *
 *   index : the number of the component being created (starting from 0)
 */
function makeComponent( index ) {
	// create a new marker and fiddle with some of its properties
	var gc = new Marker();
	// this will make $-notation refer to the new component;
	// useful for DIY components
	useSettings( gc );
	$TheMagicWord = 'xyzzy';

	// we will name the components A, B, C, and so on.
	var name = String.fromCharCode( 65 + index );
	gc.name = name;

	// put a letter on the front, a number on the back...
	gc.frontText = '<family "Monospaced"><b><size 32>' + name;
	gc.backText = '<family "Monospaced"><b><size 32>' + (index+1);

	// pick a random shape for the marker
	var sils = Silhouette.getSilhouettes();
	gc.silhouette = sils[ Math.floor( Math.random() * sils.length ) ];

	return gc;
}




if( confirm.yesno( CONFIRM_MESSAGE, project.name ) ) {
	Thread.busyWindow( make, string('pt-factory-scripted-progress'), true );
}