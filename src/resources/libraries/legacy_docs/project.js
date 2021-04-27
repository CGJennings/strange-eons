/*

  project.js - version 4
  Support for extending the project system.


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

importClass( java.io.File );
importPackage( arkham.project );
importClass( arkham.project.Member );
importClass( arkham.project.Task );
importClass( arkham.project.Project );
importClass( arkham.project.Actions );
importClass( arkham.project.Open );
importClass( arkham.project.ProjectUtilities );
const ProjectUtils = ProjectUtilities;

/**
 * Support Library for Extending the Project System
 * This library provides a set of functions that make it easier to add new
 * features to the project system. Everything that you register using these
 * methods will be tracked, so that it can be unregistered in a single step.
 * For example code, see the Project Examples folder of the plug-in authoring
 * kit.
 */

 /**
  * Predfined Global Variables
  *
  * The <tt><a href='javadoc:ca/cgjennings/apps/arkham/project/package-summary'
  * >arkham.project</a></tt> package is imported into the name space, as is
  * the class <tt><a href='javadoc:java/io/File'>java.io.File</a></tt>.
  * The constant <tt>ProjectUtils</tt> is also defined to be the class
  * <tt><a href='javadoc:ca/cgjennings/apps/arkham/project/ProjectUtilities'
  * >ProjectUtilities</a></tt>, to match the naming conventions used in other
  * script libraries.
  */

/**
 * TrackedRegistry( regFn, unregFn ) [constructor]
 * A tracked registry registers and unregisters objects in some kind of registry,
 * and also tracks the registered objects so that they can all be unregistered
 * at once. Registry trackers are used by the rest of the project library to track
 * various project entities.
 * regFn : the function to call to perform registration
 * unregFn : the function to call to perform unregistration
 */
 function TrackedRegistry( regFn, unregFn ) {
 	if( regFn == null ) throw new Error('null regFn');
 	if( unregFn == null ) throw new Error('null unregFn');

 	var reg = []; // tracks objects registered through this tracker

 	/**
 	 * TrackedRegistry.register( entity )
 	 * Registers an entity with this tracker by calling the <tt>regFn</tt>
 	 * used to create the tracker. Any arguments passed to the function
 	 * will be passed to <tt>regFn</tt>, including <tt>entity</tt>.
 	 */
 	this.register = function register( entity ) {
 		regFn.apply( null, arguments );
 		reg.push( entity );
 	};

 	/**
 	 * TrackedRegistry.unregister()
 	 * Unregisters an entity with this tracker by calling the <tt>unregFn</tt>
 	 * used to create the tracker.
 	 */
	this.unregister = function unregister( entity ) {
		unregFn.apply( null, arguments );
		for( var i=0; i<reg.length; ++i ) {
			if( reg[i] == entity ) {
				reg[i].splice( i, 1 );
				break;
			}
		}
	};

	/**
	 * TrackedRegistry.unregisterAll()
	 * Unregisters all tracked entities.
	 */
	 this.unregisterAll = function unregisterAll() {
		for( var i=0; i<reg.length; ++i ) {
			unregFn.call( null, reg[i] );
		}
		reg = [];
	 };

	 // add this new registry to the list of all registries
	 var trackers = TrackedRegistry.prototype.trackers;
	 trackers[ trackers.length ] = this;
}
TrackedRegistry.prototype.trackers = [];

/**
 * ActionRegistry
 * A tracked registry of project actions. Project actions are commands
 * that can be applied to the selected project file(s). The register
 * function takes a task action and (optionally), a priority value. The
 * priority determines the order of the action in the project view's
 * context menu. The default is <tt>Actions.PRIORITY_DEFAULT</tt>.
 */
var ActionRegistry = new TrackedRegistry(
	function register( action, priority ) {
		if( priority === undefined ) {
			priority = Actions.PRIORITY_DEFAULT;
		}
		Actions.register( action, priority );
	},
	function unregister( action ) {
		Actions.unregister( action );
	}
);


/**
 * NewActionRegistry
 * A tracked registry of child actions of the New action.
 * These are actions that will appear as suboptions of the <b>New</b> menu
 * in a project. The register function takes a task action and (optionally)
 * an action (or the name of an action) that this action should be inserted after.
 */
var NewActionRegistry = new TrackedRegistry(
	function register( action, after ) {
		var newAction = Actions.getUnspecializedAction( "new" );
		var index = -1;
		if( after != null ) {
			if( !(after instanceof TaskAction) ) {
				after = newAction.findActionByName( after.toString() );
			}
			if( after != null ) {
				index = newAction.indexOf( after );
			}
		}
		if( index < 0 ) index = newAction.size()-1;
		newAction.add( index, action );
	},
	function unregister( action ) {
		var newAction = Actions.getUnspecializedAction( "new" );
		newAction.remove( action );
	}
);

/**
 * OpenerRegistry
 * A tracked registry of internal openers. Internal openers are used by the
 * Open action to open files inside of the application. A new internal
 * opener can be used to support opening new kinds of file.
 */
var OpenerRegistry = new TrackedRegistry(
	function register( opener ) {
		Actions.getUnspecializedAction( 'open' ).registerOpener( opener );
	},
	function unregister( opener ) {
		Actions.getUnspecializedAction( 'open' ).unregisterOpener( opener );
	}
);

/**
 * MetadataRegistry
 * A tracked registry of project metadata sources. A metatdata source
 * provides metadata for a particular file type. This includes the
 * icon used for the file type in the project view, and the information
 * listed on the Properties tab when files of that type are selected.
 */
var MetadataRegistry = new TrackedRegistry(
	function register( source ) {
		Member.registerMetadataSource( source );
	},
	function unregister( source ) {
		Member.unregisterMetadataSource( source );
	}
);

/**
 * NewTaskTypeRegistry
 * A tracked registry of <b>NewTaskType</b>s. New task types are used to add
 * support for new kinds of task folders.
 */
var NewTaskTypeRegistry = new TrackedRegistry(
	function register( ntt ) {
		NewTaskType.register( ntt );
	},
	function unregister( ntt ) {
		NewTaskType.unregister( ntt );
	}
);



/**
 * unregisterAll()
 * This function unregisters all entities that have been registered
 * with <i>any</i> <tt>TrackedRegistry</tt>.
 * It is typically called in a plug-in's <tt>unload()</tt> function.
 */
function unregisterAll() {
	 var trackers = TrackedRegistry.prototype.trackers;
	for( let i=0; i<trackers.length; ++i ) {
		try {
			trackers[i].unregisterAll();
		} catch( ex ) {
			Error.handleUncaught( ex );
		}
	}
}





/**
 * testProjectScript()
 * When run directly from a script editor, this function calls the script's
 * run() function, then creates an Unregister button that will call the script's
 * unload() function (which should in turn call <tt>unregisterAll()</tt>).
 * When developing a project plug-in, it is important to unregister changes
 * you make to the project system before re-running your script, because many
 * elements in the project system can only be registered once. For example, only
 * one task action with a given name can be registered at a given time.
 */
 function testProjectScript() {
	if( sourcefile == 'Quickscript' ) {
		var b = new swing.JButton( 'Unregister' );
		b.addActionListener(  function() {
			try {
				unload();
			} catch( error ) {
				Error.handleUncaught( error );
			} finally {
				Eons.window.removeCustomComponent( b );
			}
		} );
		Eons.window.addCustomComponent( b );
		run();
	}
 }

