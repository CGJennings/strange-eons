/*
  Automation scripts allow you to automate activities that need to be
  performed repeatedly as part of a project. An automation script uses
  the file name extension .ajs instead of .js. When you double-click on
  an automation script, it will execute. To edit an automation script,
  right click on it and choose Open.

  Tips:

   - The variables project, task, and member will be set to the Project object
     for this project, the Task object for the task that this script is stored
     in (or null), and the Member object for this script file.

   - You can get a Java File object for a project member using member.getFile().

   - You can get a project: URL for a project member using member.getURL().

   - The ResourceKit recognizes project URLs (as strings) and can load resources
     from them. For example: ResourceKit.getImage( member.toURL().toString() )

   - The ProjectUtilities class contains a number of useful helper methods for
     copying files and folders and other common tasks.
 */

// this line is just a reminder in case you run the script by accident;
// you can delete it from your own script
println( '[To edit an automation script, right click on it and choose Open]' );



// the rest of this file contains some skeleton code that may be useful
// for writing your own automation scripts; just delete if you don't want it
// (Ctrl+A, Delete)


importClass( java.io.File );
importPackage( arkham.project );


/*
 * forAll( parent, somethingToDo )
 * Use this function to perform some action on each file in a folder.
 * You pass it the member object that is the parent of the files you want to
 * process, and a function that will be called to process each member.
 *
 * Example use:
 *     // print the file path of everything in the task or project where
 *     // this script is located
 *     forAll( task == null ? project : task, printFileName );
 */
function forAll( parent, somethingToDo ) {
	if( !(parent instanceof Member) ) {
		error( 'missing parent, or parent is not a Member' );
	}
	if( !somethingToDo ) {
		error( 'missing somethingToDo' );
	}

	var i;
	var child;
	var children = parent.getChildren();

    for( i=0; i<children.length; ++i ) {
		child = children[i];
		if( child.isFolder() ) {
			forAll( child, somethingToDo );
		}
		somethingToDo( child );
	}
	// this tells the project system that some of the files in
	// parent may have changed without its knowing; it will look
	// for new or deleted files and update the project accordingly
	parent.synchronize();
}

// This sample "somthingToDo" function just prints the name
// of the member being processed:

function printName( member ) {
    println( member.name );
}

// These sample filter functions can be wrapped around a somethingToDo
// function to restrict the members to which the function is applied.
// They are passed a somethingToDo function, and return a new function that
// performs the somethingToDo function, but only if the filter condition is met.
//
// Example use:
//     // print the name of all script files in the project
//     forAll( project, matchesExtension( printName, 'js' ) );

/*
 * matchesExtension( somethingToDo, extension )
 * Only runs somethingToDo on members that are files with a file name
 * extension that matches extension. (For example, 'js' to match script files.)
 */
function matchesExtension( somethingToDo, extension ) {
	// converting the argument to a Java string explicitly
	// saved doing it implicitly for each file
	if( !(extension instanceof java.lang.String) ) {
		extension = new java.lang.String( extension.toString() );
	}
    return function extensionFilter( member ) {
		if( ProjectUtilities.matchExtension( member, extension ) ) {
			somethingToDo( member );
		}
	};
}

/*
 * newerThan( somethingToDo, target )
 * This filter function only runs somethingToDo when the file has been changed
 * more recently than the target. The target can be a project member, a Java
 * File, or a Java Date object.
 */
function newerThan( somethingToDo, target ) {
	var date;
	if( target instanceof Member ) {
		date = target.file.lastModified();
	} else if( target instanceof File ) {
		date = target.lastModified();
	} else {
		date = target.getTime();
	}
	return function newerThanFilter( member ) {
		if( member.file.lastModified() > date ) {
			somethingToDo( member );
		}
	};
}
