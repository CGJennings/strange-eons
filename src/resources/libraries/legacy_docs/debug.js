/*

  debug.js - version 8
  Debugging aids.


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

/**
 * Simple debugging aids.
 */

/**
 * debug( s, [args] )
 * This function is always defined, but behaves in one of three different
 * ways. Normally, the function does nothing. If the script debugger is
 * enabled, then it will print a
 * <a href='scriptdoc://common#sprintfformatStringargs'>formatted message</a> to the
 * <a href='javadoc:ca/cgjennings/apps/arkham/StrangeEons#log'>application log</a>.
 * If this library has been imported, then the message will be printed
 * to both the application log and the script console.
 */

debug = function debug( formatString ) {
	var msg = sprintf._fromArgArray( arguments );
    Console.err.print( msg );
	Eons.log.logp( java.util.logging.Level.INFO, sourcefile, 'debug', msg );
};

const Debug = {};

Debug.THIS = this;

/**
 * Debug.settings( [settings], [regexFilter] ) [static]
 * Print a list of the current global settings by key and value.
 * An optional <a href="https://developer.mozilla.org/en-US/docs/JavaScript/Guide/Regular_Expressions">Regular Expression</a>
 * allows you to filter the results by printing only those keys that match
 * the expression. If no settings instance is provided, the global shared
 * settings will be used. (A regular expression can still be provided as the
 * first argument.)
 *
 * settings : an optional
 * regexFilter : an optional regular expression that printed keys must match
 */
Debug.settings = function settings( settings, regexFilter ) {
	if( !(settings instanceof resources.Settings) ) {
		if( regexFilter === undefined ) {
			regexFilter = settings;
		}
		settings = resources.Settings.shared;
	}
    Console.err.println( settings );
    Debug._printSettingsImpl( settings, regexFilter );
};

/**
 * Debug.privateSettings( component, [regexFilter] ) [static]
 * Print a list of the private settings attached to a game component.
 * An optional <a href="https://developer.mozilla.org/en-US/docs/JavaScript/Guide/Regular_Expressions">Regular Expression</a>
 * allows you to filter the results by printing only those keys that match
 * the expression.
 *
 * component : a game component to print the private settings of
 * regexFilter : an optional regular expression that printed keys must match
 */
Debug.privateSettings = function privateSettings( component, regexFilter ) {
    Console.err.print( sprintf( "Private settings attached to %s:\n", component.fullName ) );
    Debug._printSettingsImpl( component.settings, regexFilter );
};

Debug._printSettingsImpl = function _printSettingsImpl( s, regexFilter ) {
    var keys = s.keySet.toArray();
    java.util.Arrays.sort( keys, resources.ResourceKit.collator );
    for( var i=0; i<keys.length; ++i ) {
        var key = keys[i];
        if( !regexFilter || regexFilter.test( key ) ) {
            Console.err.print( sprintf('  %s = "%s"\n', key, s.get( key ) ) );
        }
    }
};

/**
 * Debug.printScopeChain( scope, [regexFilter] ) [static]
 * Print a list of variables and their values in the chain of scopes starting
 * with the parent of the scope provided. To create an appropriate scope for
 * the current point in a function, use an empty function, e.g.:
 * <pre>
 * Debug.printScopeChain( function(){} );
 * </pre>
 *
 * The optional <tt>regexFilter</tt> will filter out non-matching variable names
 * from the list.
 */
Debug.printScopeChain = function printScopeChain( scope, regexFilter ) {
	if( !scope ) {
		throw new Error( "Usage: Debug.printScopeChain( function(){} );" );
	}
	scope = scope.__parent__;

	while( scope != null ) {
		for( var i in scope ) {
			if( !regexFilter || regexFilter.test( i ) ) {
				Console.err.print( sprintf("%s = '%s'\n", i, scope[i] ) );
			}
		}
		scope = scope.__parent__;
		if( scope ) {
			Console.err.println( "//////// ENCLOSED BY ////////" );
		}
	}
};

/**
 * Before and After Functions
 * Before and after functions are functions that can be wrapped around an existing
 * function and will be called before or after the function that they wrap.
 * <tt>Debug</tt> supports adding before and/or after functions on both global
 * functions and member functions (<tt>this.f = function f() {}</tt>).
 * To write a before or after function, define a function with the appropriate
 * signature:
 * <div class='indent'>
 */
/**
 * beforeFunction( args, callee, calleeThis, functionName )
 * args : the arguments passed to the function
 * callee : the wrapped function, which will be called when this function returns
 * calleeThis : the <tt>this</tt> object for the wrapped function
 * functionName : the name of the member function
 *
 * returns the arguments to be passed to the <tt>callee</tt> when it is called
 */
/**
 * afterFunction( returnValue, args, callee, calleeThis, functionName )
 * returnValue : the return value from the wrapped function (which was just called)
 * args : the arguments passed to the function
 * callee : the wrapped function
 * calleeThis : the <tt>this</tt> object for the wrapped function
 * functionName : the name of the member function
 *
 * returns the return value for the member function call
 * </div>
 */
/**
 * Modifying Arguments and Return Values
 * A before or after function can change the apparent arguments to the function
 * (before) or the apparent return value from the function (after).
 * An before function is passed a copy of the original arguments and it returns
 * the actual arguments to be passed to the wrapped function (which can simply
 * be <tt>args</tt> if you do not wish to modify them).
 * An after function is passed the return value from the wrapped function and
 * it returns the apparent return value that will be returned to the caller
 * (which can simply be <tt>returnValue</tt> if you do not wish to modify it).
 */

/**
 * Debug.addBefore( [object], functionName, beforeFunction ) [static]
 * Add a before function to a member function, which will be called just
 * before the affected function whenever that function is called.
 *
 * object : the object that contains the function (default is the global object)
 * functionName : the name of the member function
 * beforeFunction : the function to be called before the affected function
 */
Debug.addBefore = function addBefore( object, functionName, beforeFunction ) {
	if( arguments.length == 2 ) {
		Debug.addBefore( null, arguments[0], arguments[1] );
		return;
	}
	if( typeof functionName == "function" )
		throw new Error( "pass the function's name, not the function itself" );
	if( !object ) object = Debug.THIS;
	var innerFunc = object[ functionName ];
	if( innerFunc === undefined )
		throw new Error( "no function with this name in object: " + functionName );
	var outerFunc = function before_function() {
		return innerFunc.apply( this, beforeFunction( arguments, innerFunc, this, functionName ) );
	};
	outerFunc._innerFunction = innerFunc;
	object[ functionName ] = outerFunc;
};

/**
 * Debug.addAfter( [object], functionName, afterFunction ) [static]
 * Add an after function to a member function, which will be called just
 * after the affected function whenever that function is called.
 *
 * object : the object that contains the function (default is the global object)
 * functionName : the name of the member function
 * afterFunction : the function to be called after the affected function
 */
Debug.addAfter = function addAfter( object, functionName, afterFunction ) {
	if( arguments.length == 2 ) {
		Debug.addAfter( null, arguments[0], arguments[1] );
		return;
	}
	if( typeof functionName == "function" )
		throw new Error( "pass the function's name, not the function itself" );
	if( !object ) object = Debug.THIS;
	var innerFunc = object[ functionName ];
	if( innerFunc === undefined )
		throw new Error( "no function with this name in object: " + functionName );
	var outerFunc = function after_function() {
		var retVal = innerFunc.apply( this, arguments );
		return afterFunction( retVal, arguments, innerFunc, this, functionName );
	};
	outerFunc._innerFunction = innerFunc;
	object[ functionName ] = outerFunc;
};

/**
 * Debug.removeAll( object, functionName ) [static]
 * Remove all before and after functions that have been added to a member function.
 *
 * object : the object that contains the function (default is the global object)
 * functionName : the name of the member function
 */
Debug.removeAll = function removeAll( object, functionName ) {
	if( arguments.length == 1 ) {
		Debug.removeBeforeAfter( null, arguments[0] );
		return;
	}
	if( typeof functionName == "function" )
		throw new Error( "pass the function's name, not the function itself" );
	if( !object ) object = Debug.THIS;
	var outerFunc = object[ functionName ];
	if( outerFunc === undefined )
		throw new Error( "no function with this name in object: " + functionName );
	var innerFunc = outerFunc["_innerFunction"];

	// there are no more before or afters set on the function
	if( !innerFunc )
		return;

	object[ functionName ] = innerFunc;

	// recursively check the restored function to remove all levels of before/after
	Debug.removeBeforeAfter( object, functionName, remove );
};


/**
 * Debug.TRACE_ENTRY [static]
 * A function that can be used with <tt>Debug.beforeMember()</tt> to print tracing
 * information whenever a function is called.
 */
Debug.TRACE_ENTRY = function TRACE_ENTRY( args, callee, calleeThis, functionName ) {
	Console.err.print( sprintf("> Entering %s", functionName) );
	if( !(callee.name === undefined) && functionName != callee.name )
		Console.err.print( sprintf( "[%s]", callee.name ) );
	Console.err.print( "(" );
	for( var i=0; i<args.length; ++i ) {
		if( i > 0 ) Console.err.print( ", " );
		Console.err.print( args[i] );
	}
	Console.err.println( ")" );

	return args;
};

/**
 * Debug.TRACE_EXIT [static]
 * A function that can be used with <tt>Debug.afterMember()</tt> to print tracing
 * information whenever a function returns.
 */
Debug.TRACE_EXIT = function TRACE_EXIT( returnValue, args, callee, calleeThis, functionName ) {
	Console.err.print( sprintf( "< Exiting %s (return value '%s')\n", functionName, returnValue ) );
	return returnValue;
};


/**
 * Debug.trace( [object], functionName ) [static]
 * This is a convenience that adds the <tt>TRACE_ENTRY</tt> and <tt>TRACE_EXIT</tt>
 * before and after functions to a member function.
 *
 * object : the object that contains the member function (default is the global object)
 * functionName : the name of the member function
 */
Debug.trace = function trace( object, functionName ) {
	if( arguments.length == 1 ) {
		Debug.trace( null, arguments[0] );
		return;
	}
	Debug.addBefore( object, functionName, Debug.TRACE_ENTRY );
	Debug.addAfter( object, functionName, Debug.TRACE_EXIT );
};

/**
 * Debug.traceAll( [object] ) [static]
 * Adds tracing to every function in an object.
 * object : the object to trace (default is the global object)
 */
Debug.traceAll = function traceAll( object ) {
	if( !object ) object = Debug.THIS;
	for( var p in object ) {
		if( typeof object[p] == "function" ) {
			Debug.trace( object, p );
		}
	}
};
