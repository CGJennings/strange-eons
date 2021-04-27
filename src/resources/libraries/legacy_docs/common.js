/*

  common.js - version 22
  Core functionality included in every script.


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

/***
 * Standard library of commonly used functions.
 */


/***
 * Predefined Global Constants and Variables
 *
 * Global : a reference to the global scope of the script; sometimes needed ...
 *     to specify a global variable when a local variable has the same name, ...
 *     but can also be used to look up global variable programmatically, e.g.: ...
 *     <tt>println( Global['$recent_file_' + 1] );</tt>
 *
 * Eons : a reference to the <a href='javadoc:ca/cgjennings/apps/arkham/StrangeEons'>main application</a>
 * Editor : a reference to the <a href='javadoc:ca/cgjennings/apps/arkham/StrangeEonsEditor'>active editor</a>
 * Component : a reference to the <a href='javadoc:ca/cgjennings/apps/arkham/component/GameComponent'>component</a> ...
 *     currently edited by <tt>Editor</tt>, if any
 * PluginContext : a reference to the plugin context instance for this script
 * sourcefile : the source file of the script being executed
 *
 * ca : convenience for the <tt>ca</tt> root package
 * arkham : convenience for the ...
 *     <tt><a href='javadoc:ca/cgjennings/apps/arkham/'>ca.cgjennings.apps.arkham</a></tt> ...
 *     package
 * resources : convenience for the <tt><a href='javadoc:resources/'>resources</a></tt>
 * swing : convenience for the <tt><a href='javadoc:javax/swing/'>javax.swing</a></tt> package
 *
 * Colour (or Color) : a <a href='javadoc:resources/Settings.Colour'>subclass of ...
 *     <tt>java.awt.Color</tt></a> that is easily written to and read from settings
 * Font : the Font class (<tt>java.awt.Font</tt>)
 * Region : a <a href='javadoc:resources/Settings.Region'>subclass of ...
 *     <tt>java.awt.Rectangle</tt></a> that is easily written to and read from settings
 * Region2D : a <a href='javadoc:resources/Settings.Region2D'>subclass of ...
 *     <tt>java.awt.Rectangle2D</tt></a> that is easily written to and read from settings
 * URL : convenience for <tt>java.net.URL</tt>
 *
 * ResourceKit : convenience for <tt><a href='javadoc:resources/ResourceKit'>resources.ResourceKit</a></tt>
 * Language : convenience for <tt><a href='javadoc:resources/Language'>resources.Language</a></tt>
 * Settings : the class (<tt><a href='javadoc:resources/Settings'>resources.Settings</a></tt>) ...
 *     used to store setting keys and their values
 */

const Global = this;
var global = this; // deprecated

// Commented out packages are listed for completeness; they are already defined
//const java = Packages.java;
//const javax = Packages.javax;
//const org = Packages.org;
const swing = javax.swing;
const ca = Packages.ca;
const gamedata = Packages.gamedata;
const resources = Packages.resources;
const arkham = Packages.ca.cgjennings.apps.arkham;
const Settings = resources.Settings;
const Color = Settings.Colour;
const Colour = Settings.Colour;
const Font = java.awt.Font;
const Region = Settings.Region;
const Region2D = Settings.Region2D;
const URL = java.net.URL;
const ResourceKit = resources.ResourceKit;
const Language = resources.Language;

/***
 * useLibrary( library )
 * Import the objects, functions, and variables defined by a library into the
 * current script. The value of <tt>library</tt> may either be the simple name
 * of a built-in library or the URL of a script file. Libraries may safely
 * be imported multiple times; subsequent calls to <tt>uselibrary</tt> with
 * the same <tt>library</tt> argument will be ignored.
 *
 * <b>Examples:</b>
 * <pre>
 * // import the standard library "markup"
 * // (/resources/libraries/markup.js)
 * useLibrary( 'markup' );
 * // import a custom library from a plug-in
 * // (/resources/myname/myplugin/mylib.js)
 * useLibrary( 'res://myname/myplugin/mylib.js' );
 * </pre>
 *
 * The <tt>common</tt> library is automatically imported into every script.
 *
 * library : the built-in library name or a URL
 */


/***
 * exit()
 * Stops executing the script running in the current thread.
 */
function exit() {
    arkham.plugins.ScriptMonkey.breakScript();
}


/***
 * Error.error( [exception] ) [static]
 * Indicates an error condition by throwing an exception. The argument can be
 * a JavaScript <i>or</i> Java excpeption.
 *
 * exception : a message string, JavaScript <tt>Error</tt>, or Java ...
 *             <tt>Throwable</tt> object to throw
 */
Error.error = function error( exception ) {
    // in the old engine we had to jump through a bunch of hoops to be
    // able to get a stack trace that we could dump to the console;
    // now this function is basically a shorthand way to throw an error
    if( exception == null ) {
		exception = 'unspecified error';
	} else if( exception instanceof java.lang.Throwable ) {
		ca.cgjennings.script.mozilla.javascript.Context.throwAsScriptRuntimeEx( exception );
	} else if( exception instanceof Error ) {
		throw exception;
	}
    throw new Error( exception.toString() );
};

/***
 * Error.warn( [message], [stackFrame] ) [static]
 * If script warnings are enabled, issues a warning with <tt>message</tt>
 * as the warning text. The optional <tt>stackFrame</tt> parameter indicates
 * the relative stack frame to indicate as the location (file and line number)
 * of the warning. The default is -1, which reports the line of the direct caller
 * to <tt>warn</tt>.
 *
 * warning : a warning message string to be printed as a script warning
 * stackFrame : the optional relative stack frame position to report as the source
 */
if( Settings.shared.getBoolean( 'script-warnings' ) ) {
	Error.warn = function warn( message, frame ) {
		if( message === undefined ) message = 'unspecified warning';
		let trace = useLibrary.__engine.getStackTrace();
		if( frame === undefined ) frame = 1;
		if( frame < 0 ) frame = -frame;
		frame = Math.min( trace.length-1, frame );
		if( frame >= 0 ) {
			ca.cgjennings.script.mozilla.javascript.Context.reportWarning(
				message, trace[ frame ].file, trace[ frame ].line, null, -1
			);
		} else {
			ca.cgjennings.script.mozilla.javascript.Context.reportWarning( message );
		}
	};
} else {
    Error.warn = function warn( message, frame ) {};
}

/***
 * Error.deprecated( [message], [stackFrame] )
 * If script warnings are enabled, warns that a deprecated feature has been
 * used. Marking a feature as deprecated warns that it may be removed in a
 * future version. The optional <tt>stackFrame</tt> parameter indicates
 * the relative stack frame to indicate as the location (file and line number)
 * of the warning. The default is -1, which reports the line that called
 * <tt>deprecated</tt>.
 *
 * message : an optional message describing the deprecated feature
 * stackFrame : the optional relative stack frame position to report as the source
 */
if( Settings.shared.getBoolean( 'script-warnings' ) ) {
	Error.deprecated = function deprecated( message, frame ) {
		if( message === undefined ) message = 'use of unspecified deprecated feature';
		message = 'DEPRECATED: ' + message;
		if( frame === undefined ) frame = 1;
		if( frame < 0 ) frame = -frame;
		Error.warn( message, frame+1 );
	};
} else {
	Error.deprecated = function deprecated( message, frame ) {};
}


/***
 * Error.handleUncaught( exception ) [static]
 * Prints a standard error message to the console to describe an exception.
 * This function can be called to handle uncaught script errors in script code
 * that "escapes" the execution of the script that defined it, such as a
 * Java interface implementation.
 *
 * When a script is executed, exceptions that are thrown are caught and displayed
 * in the console as if the entire script were surrounded by a special
 * <tt>try</tt>-<tt>catch</tt> pair. However, when you implement a Java class
 * or interface with script code, the function(s) that implement the Java object
 * "escape" from this try-catch. For example, suppose you create an
 * <tt>ActionListener</tt>, add it to a <tt>JButton</tt>, and then add the
 * button to the application window with <tt>Eons.addCustomComponent</tt>.
 * The script ends, but the listener still exists because it is attached to the
 * button and the button still exists. If the listener function throws an exception,
 * you will not see the error message. To get an error message from this listener,
 * surround the listener code with a <tt>try</tt>/<tt>catch</tt> and call this
 * function with the thrown exception. For example:
 * <pre>
 * let button = new swing.JButton( 'Press Me!' );
 * button.addActionListener( function listener() {
 *     try {
 *         println( 'You pressed me!' );
 *     } catch( ex ) {
 *         Error.handleUncaught( ex );
 *     }
 * });
 * Eons.window.addCustomComponent( button );
 * </pre>
 *
 * <b>Note:</b> Although you could use an anonymous function to create this
 * listener (<tt>function(){...}</tt>), if you give it a name then that that name
 * will appear in the stack trace of any error messages.
 */
Error.handleUncaught = function handleUncaught( exception ) {
    if( exception['javaException'] instanceof arkham.plugins.ScriptMonkey.BreakException ) {
        throw exception;
	}
    let rex = exception['rhinoException'];
    if( rex ) {
        arkham.plugins.ScriptMonkey.scriptError( rex );
    } else {
        Console.err.printObj( '\nUncaught ' );
        Console.err.print( exception );
    }
};



/***
 * prompt( [promptMessage], [initialValue] )
 * Prompts the user for input and returns the result.
 * This function displays a dialog box with a text field in which the user
 * may enter text. If the user presses OK, the entered text is returned.
 * If the user cancels the dialog, <tt>null</tt> is returned.
 *
 * promptMessage : an optional prompt to display
 * initialValue : an optional default value to fill in the text field
 *
 * returns the string entered by the user, or <tt>null</tt> if the dialog
 *     is cancelled
 */
function prompt( promptMessage, initialValue ) {
	useLibrary.__threadassert();
    if( !promptMessage ) promptMessage = '';
    if( !initialValue ) initialValue = '';
    return swing.JOptionPane.showInputDialog( Eons.safeStartupParentWindow, promptMessage, initialValue );
}



/***
 * confirm( promptMessage, [title] )
 * Displays a dialog box containing the promptMessage text along with OK and Cancel
 * buttons. Returns <tt>true</tt> if the user selects OK, or <tt>false</tt> otherwise.
 * The <tt>confirm()</tt> function is commonly supported by Web browsers with
 * JavaScript support. To complement this functionality, two additional
 * variants are available: <tt>confirm.yesno</tt> for Yes/No questions,
 * and <tt>confirm.choose</tt> to choose from a set of custom options.
 * These are both described in detail below.
 *
 * promptMessage : the prompt text to display
 * title : an optional title for the prompt window
 *
 * returns <tt>true</tt> if user selects OK
 */
/***
 * confirm.confirm( promptMessage, [title] ) [static]
 * This is equivalent to <tt>confirm( promptMessage, title )</tt>. It is
 * provided as a parallel to <tt>confirm.yesno</tt> and
 * <tt>confirm.choose</tt>.
 */
/***
 * confirm.yesno( promptMessage, [title] ) [static]
 * Display a dialog box containing the promptMessage text along with Yes and No
 * buttons. Returns <tt>true</tt> if the user selects Yes, or <tt>false</tt> otherwise.
 *
 * promptMessage : the prompt text to display
 * title : an optional title for the prompt window
 *
 * returns <tt>true</tt> if user selects Yes
 */
/***
 * confirm.choose( promptMessage, title, option1, [option2], ... ) [static]
 * Display a dialog box with buttons for each of <tt>option1</tt>, <tt>option2</tt>,
 * and so on.
 * If the user chooses one of the options, the index of the option is returned
 * (0 for <tt>option1</tt>, 1 for <tt>option2</tt>, and so on).
 * If the user closes the dialog without making a selection, <tt>-1</tt>
 * is returned instead.
 *
 * promptMessage : the prompt text to display
 * title : a title for the prompt window (may be <tt>null</tt>)
 * options : a list of one or more options to display
 *
 * returns the selected option's index, or <tt>-1</tt>
 */
let confirm = (function() {
	let confirm = function confirm( promptMessage, title ) {
    	return confirm.confirm( promptMessage, title );
    };

	confirm.confirm = function confirm( promptMessage, title ) {
		useLibrary.__threadassert();
	    if( promptMessage === undefined ) Error.error( 'no promptMessage given' );
	    if( !title ) title = useLibrary.defaultDialogTitle();

	    return swing.JOptionPane.OK_OPTION ===
	        swing.JOptionPane.showConfirmDialog( Eons.safeStartupParentWindow, promptMessage, title,
	        swing.JOptionPane.OK_CANCEL_OPTION );
	};

	confirm.yesno = function yesno( promptMessage, title ) {
		useLibrary.__threadassert();
	    if( promptMessage === undefined ) Error.error( 'no promptMessage given' );
	    if( !title ) title = useLibrary.defaultDialogTitle();

	    return javax.swing.JOptionPane.OK_OPTION ===
	        javax.swing.JOptionPane.showConfirmDialog( Eons.safeStartupParentWindow, promptMessage, title,
	        javax.swing.JOptionPane.YES_NO_OPTION );
	};

	confirm.choose = function choose( promptMessage, title ) {
		useLibrary.__threadassert();
	    if( arguments.length < 3 ) Error.error( 'no options were given' );
	    if( !title ) title = useLibrary.defaultDialogTitle();

	    let options = new Array();
	    for( let i=2; i<arguments.length; ++i ) {
	    	options[i-2] = arguments[i];
	    }

	    let index = swing.JOptionPane.showOptionDialog(
	        Eons.safeStartupParentWindow, promptMessage, title,
	    	swing.JOptionPane.DEFAULT_OPTION,
	    	swing.JOptionPane.QUESTION_MESSAGE,
	    	null, options, options[0]
	    );
	    return index;
	};

    return confirm;
})();



/***
 * alert( message, [isErrorMessage] )
 * Displays a message in a dialog box. The user must press the <i>OK</i> button
 * to continue. This is not generally a user-friendly act and should only
 * be used if the user must acknowledge something before being allowed to
 * continue using the application.
 *
 * The message dialog is normally formatted as a warning message.
 * If the <tt>isErrorMessage</tt> flag is <tt>true</tt>, it will be
 * formatted as an error message instead. (Exactly what this means
 * depends on the platform and selected theme, but typically the
 * resulting dialog box will feature a different
 * icon for warnings than for errors.)
 *
 * message : the message to display
 * isErrorMessage : an optional flag; if <tt>true</tt>, an error dialog is displayed
 */
function alert( message, isErrorMessage ) {
	useLibrary.__threadassert();
    if( !message ) Error.error( 'no message given' );
	
	if( isErrorMessage === undefined ) {
		arkham.dialog.Messenger.displayMessage( null, message );
	} else if( isErrorMessage ) {
		arkham.dialog.Messenger.displayErrorMessage( null, message );
	} else {
		arkham.dialog.Messenger.displayWarningMessage( null, message );
	}
}



/***
 * sleep( [msDelay] )
 * Pauses script execution for a period of time. Returns <tt>true</tt> if you <tt>interrupt()</tt>
 * the thread from another thread while it is sleeping, otherwise returns <tt>false</tt>.
 *
 * delay : the length of time to pause, in milliseconds (default is 1000)
 */
function sleep( msDelay ) {
    if( !msDelay ) msDelay = 1000;
    try {
        java.lang.Thread.currentThread().sleep( msDelay );
    } catch( interrupted ) {
        if( interrupted instanceof java.lang.InterruptedException )
            return true;
        throw interrupted;
    }
    return false;
}


/***
 * Console
 * The console object allows you to interact with the Script Output
 * Console window.
 */
/***
 * Console.print() [static]
 * Prints an object to the script console. This is identical to the global
 * <tt>print</tt> function.
 */
/***
 * Console.println() [static]
 * Prints an object to the script console. This is identical to the global
 * <tt>println</tt> function.
 */
/***
 * Console.printf( formatStirng, [values...] ) [static]
 * Prints a formatted string to the script console. This is identical to the global
 * <tt>printf</tt> function.
 */
/***
 * Console.printImage( image ) [static]
 * Prints an image or icon (subclass of
 * <a href='javadoc:java/awt/Image.html'>java.awt.Image</a>
 * or
 * <a href='javadoc:java/awt/Icon.html'>java.awt.Icon</a>)
 * to <tt>Console.out</tt>.
 *
 * image : an image or icon object to be inserted into the console text
 */
/***
 * Console.printComponent( component ) [static]
 * Prints a user interface component (a subclass of
 * <a href='javadoc:java/awt/Component.html'>java.awt.Component</a>)
 * to <tt>Console.out</tt>.
 *
 * component : an interface component to be inserted into the console text
 */
/***
 * Console.printHTML( html ) [static]
 * Prints a string of HTML markup to the console as formatted text. The level
 * of HTML support is equivalent to that provided by Swing <tt>JLabel</tt>s.
 *
 * html : a string of HTML markup to be parsed, formatted, and inserted
 */
/***
 * Console.clear() [static]
 * Clears the script console.
 */
/***
 * Console.history() [static]
 * Returns the current text of the script console history
 */
/***
 * Console.visible [static]
 * Boolean property that controls whether the console window is visible.
 */
/***
 * Console.queue() [static]
 * Buffers output to <tt>Console.out</tt> until a matching call to <tt>flush()</tt>.
 * This method should be surrounded with a <tt>try</tt>...<tt>finally</tt> block
 * to ensure that the matching call to <tt>flush()</tt> is always performed.
 */
/***
 * Console.flush() [static]
 * Immediately flushes pending writes to <tt>Console.out</tt>. If output is
 * currently being buffered due to a call to <tt>queue()</tt>, buffering ends
 * and the accumulated output is written to the console.
 */
/***
 * Console.out [static] [readonly]
 * A <tt>java.io.PrintWriter</tt> that can be used to write to the
 * console window's output stream.
 */
/***
 * Console.err [static] [readonly]
 * A <tt>java.io.PrintWriter</tt> that can be used to write to the
 * console window's error stream.
 */
const Console = {
    get out() { return arkham.plugins.ScriptMonkey.sharedConsole.getWriter(); },
    get err() { return arkham.plugins.ScriptMonkey.sharedConsole.getErrorWriter(); },
	get visible() { return arkham.plugins.ScriptMonkey.sharedConsole.isVisible(); },
	set visible(b) { arkham.plugins.ScriptMonkey.sharedConsole.setVisible(b); },
    print: print,
    println: println,
    printImage: function printImage( image ) {
        this.out.insertImage( image );
    },
    printComponent: function printComponent( uicomponent ) {
        this.out.insertComponent( image );
    },
	printHTML: function printHTML( html )  {
		this.out.insertHTML( html );
	},
    clear: function clear() {
        arkham.plugins.ScriptMonkey.sharedConsole.clear();
    },
    history: function history() {
        return arkham.plugins.ScriptMonkey.sharedConsole.getHistoryText();
    },
    queue: function queue() {
        arkham.plugins.ScriptMonkey.sharedConsole.queue();
    },
    flush: function flush() {
        arkham.plugins.ScriptMonkey.sharedConsole.flush();
    }
};

/***
 * print( obj )
 * Prints an object to the script console. You may pass multiple arguments
 * to this function; the arguments will be printed in sequence as if they
 * printed by multiple print statements.
 *
 * This is a cover for <tt>Console.print</tt>.
 *
 * obj : an object for which a string representation should be printed
 */
function print( obj ) {
	for( let i=0; i<arguments.length; ++i ) {
		Console.out.printObj( arguments[i] );
	}
}

/***
 * println( obj )
 * Prints an object to the script console, followed by a newline.
 * You may pass multiple arguments to this function; the arguments
 * will be printed in sequence as if by multiple <tt>print</tt> calls,
 * then followed by a newline.
 *
 * This is a cover for <tt>Console.println</tt>.
 *
 * obj : an object for which a string representation should be printed
 */
function println(s) {
	for( let i=0; i<arguments.length; ++i ) {
		Console.out.printObj( arguments[i] );
	}
    Console.out.println();
}

/***
 * string( key, [args...] )
 * Returns the localized user interface string for a string key.
 * If the key is undefined an error message string is returned
 * instead.
 *
 * If the user interface string is a format string, it may be formatted by
 * passing additional arguments. Note that <tt>Number</tt> arguments are converted
 * to <tt>java.lang.Double</tt>s; to fill <tt>%d</tt> format arguments you
 * must explicitly convert the <tt>Number</tt> to a Java <tt>Integer</tt>
 * or <tt>Long</tt> (you can use <tt><i>n</i>.toInt()</tt>).
 *
 * key : a user interface text key
 * args : zero or more arguments used to format the string
 *
 * returns the formatted, localized string mapped to by the key
 */
function string( key ) {
	if( arguments.length === 1 ) {
		return Language.interface.get( key );
	} else {
		return Language.interface.get( key, Array.from( arguments ).slice(1) );
	}
}



/***
 * gstring( key, [args...] )
 * Returns the localized game string for a string key.
 * If the key is undefined an error message string is returned
 * instead.
 *
 * If the user interface string is a format string, it may be formatted by
 * passing additional arguments. Note that <tt>Number</tt> arguments are converted
 * to <tt>java.lang.Double</tt>s; to fill <tt>%d</tt> format arguments you
 * must explicitly convert the <tt>Number</tt> to a Java <tt>Integer</tt>
 * or <tt>Long</tt> (you can use <tt><i>n</i>.toInt()</tt>).
 *
 * key : a user interface text key
 * args : zero or more arguments used to format the string
 *
 * returns the formatted, localized string mapped to by the key
 */
function gstring( key ) {
	if( arguments.length === 1 ) {
		return Language.game.get( key );
	} else {
		return Language.game.get( key, Array.from( arguments ).slice(1) );
	}
}


/***
 * sprintf( formatString, [args...] )
 * Returns a string formatted using C-style printf syntax.
 * See <a href='javadoc:java/util/Formatter.html#syntax'>Formatter Syntax</a>
 * for details. Note that <tt>Number</tt> arguments are converted to <tt>java.lang.Double</tt>.
 *
 * formatString : the string to be formatted using the arguments
 * args : a list of zero or more arguments used to format the string
 *
 * returns a formatted string using the specified format string and arguments.
 */
function sprintf( formatString ) {
    return sprintf._fromArgArray( arguments );
}
sprintf._fromArgArray = function( argv ) {
	let fmt = Array.prototype.shift.call( argv );
    return java.lang.String.format( fmt, Array.from(argv) );
};


/***
 * printf( formatString, [args...] )
 *
 * Print a formatted string to the console using C-style printf syntax.
 * See <a href='javadoc:java/util/Formatter.html#syntax'>Formatter Syntax</a>
 * for details. Note that <tt>Number</tt> arguments are converted to <tt>java.lang.Double</tt>.
 * If you wish to use the <tt>%d</tt> format, convert the number to <tt>java.lang.Integer</tt>
 * using its <tt>toInt()</tt> function.
 *
 * formatString : the string to be formatted using the arguments
 * args : a list of zero or more arguments used to format the string
 */
function printf( formatString ) {
    Console.out.printObj( sprintf._fromArgArray( arguments ) );
}






/***
 * useSettings( source )
 * Sets the <tt>Settings</tt> object that will be used to get and set
 * settings using the <tt>$<i>setting_name</i></tt> syntax. Starting
 * with Strange Eons 2.00.5, script code may read and write settings
 * using global variables that start with a dollar sign ($) followed
 * by the setting name. Setting names that include the hyphen character
 * should use an underscore instead of a hyphen since hyphens are not
 * legal in JavaScript identifiers. For example, to print the value of
 * <tt>stamina-text-region</tt>, one would write:
 * <pre>
 * println( $stamina_text_region );
 * </pre>
 *
 * By default, a script that uses this syntax will read and write the
 * global (shared) settings, except for DIY scripts
 * which use the private settings of their DIY game component object.
 * This function allows you to choose a different <tt>Settings</tt>
 * object to use. The value of <tt>source</tt> can either be a
 * <tt>Settings</tt> object, which will be used directly, or
 * else a game component, in which case the private settings of that
 * component will be used. If <tt>source</tt> is <tt>null</tt> or
 * <tt>undefined</tt>, then shared settings will be used.
 *
 * source : the settings to use for <tt>$</tt> script variables
 */
function useSettings( source ) {
    if( source == null ) {
        source = Settings.shared;
    } else if( source instanceof arkham.component.GameComponent ) {
        source = source.settings;
    } else if( !(source instanceof Settings) ) {
        Error.error( 'source is not a Settings instance or game component' );
    }
    useLibrary.__$notation.globalSettingsProvider = source;
}



/***
 * $( key ) [static]
 * Looks up the value of a key named by the string argument in the current
 * <tt>$</tt>-notation settings. This allows you to describe a key
 * algorithmically using consistent notation.
 * 
 * key : a string whose value is the desired setting key
 */
function $( key ) {
	return useLibrary.__$notation.globalSettingsProvider.get( key );
}



/***
 * useInterfaceLanguage( [language] )
 * Sets the <a href='javadoc:resources/Language'>Language</a> used
 * to look up interface strings with the <tt>@<i>key_name</i></tt>
 * syntax. By default this is <tt>Language.getInterface()</tt>.
 * The <tt>@</tt>-syntax is the same as the <tt>$</tt>-syntax for
 * settings, except that it looks up localized interface strings.
 *
 * language : the localized string source to use
 */
function useInterfaceLanguage( language ) {
	useLibrary.__$notation.globalInterfaceLanguageProvider = language;
}

/***
 * @( key ) [static]
 * Looks up the value of an interface string key in the current <tt>@</tt>-notation
 * language. This allows you to describe a key algorithmically while
 * still using consistent notation.
 * 
 * key : a string whose value is the desired interface language key
 */
// using ['@'], ['#'] prevents error flags in 3rd party IDEs
Global['@'] = function( key ) {
	return useLibrary.__$notation.globalInterfaceLanguageProvider.get( key );
};

/***
 * useGameLanguage( [language] )
 * Sets the <a href='javadoc:resources/Language'>Language</a> used
 * to look up game strings with the <tt>#<i>key_name</i></tt>
 * syntax. By default this is <tt>Language.getGame()</tt>.
 * The <tt>#</tt>-syntax is the same as the <tt>$</tt>-syntax for
 * settings, except that it looks up localized game strings.
 *
 * language : the localized string source to use
 */
function useGameLanguage( language ) {
	useLibrary.__$notation.globalGameLanguageProvider = language;
}

/***
 * #( key ) [static]
 * Looks up the value of a game string key in the current <tt>#</tt>-notation
 * language. This allows you to describe a key algorithmically while
 * still using consistent notation.
 * 
 * key : a string whose value is the desired game language key
 */
Global['#'] = function( key ) {
	return useLibrary.__$notation.globalGameLanguageProvider.get( key );
};



/***
 * Patch
 * The <tt>Patch</tt> object contains static helper methods that modify
 * (or restore) settings.
 * This can be used to fix user issues or to modify program behaviour at
 * runtime, and to create new card types based on existing cards.
 */
 const Patch = {};

/***
 * Patch.apply( [key1, value1], [key2, value2], ... ) [static]
 * Writes zero or more pairs of settings values to user settings and then
 * saves user settings to disk. The new values will override the default
 * values for the application and game language.
 *
 * The arguments must be a sequence of key, value pairs. If the number of
 * arguments is not even, an exception is thrown.
 * For example:
 * <pre>
 * Patch.apply( 'encounter-bright-adjust', '1' )
 * </pre>
 *
 * key1, value1,&nbsp;... : pairs of key and value strings that name the keys to ...
 *                     be modified and their new values
 */
Patch.apply = function apply() {
    if( arguments.length % 2 != 0 ) {
        Error.error( string( 'scriptlib-common-patchargs' ) );
    }
	let s = Settings.user;
    for( let i=0; i<arguments.length; i += 2 ) {
        let oldValue = s.get( arguments[i] );
		s.set( arguments[i], arguments[i+1] );
        println( string( 'scriptlib-common-patch-apply', arguments[i], arguments[i+1], oldValue ) );
    }
	s.flush();
    println( 'OK' );
};



/***
 * Patch.restore( [key1], [key2], ... ) [static]
 * Deletes zero or more user settings and then writes user settings to disk.
 *
 * key1,&nbsp;... : the names of the keys whose values should be restored
 */
Patch.restore = function restore() {
	let s = Settings.user;
    for( let i=0; i<arguments.length; ++i ) {
		s.reset( arguments[i] );
    }
	s.flush();
	s = Settings.shared;
    for( i=0; i<arguments.length; ++i ) {
        let newValue = s.get( arguments[i] );
        println( string( 'scriptlib-common-patch-restore', arguments[i], newValue ) );
    }
    println( 'OK' );
};


/***
 * Patch.temporary( [key1, value1], [key2, value2], ... ) [static]
 * Changes zero or more pairs of settings values until the end of the current
 * session. The settings will return to their previous value the next time
 * the application runs.
 *
 * The arguments must be a sequence of key, value pairs. If the number of
 * arguments is not even, an exception is thrown.
 *
 * Assigning a value to a setting using the <tt>$</tt>-notation is normally
 * equivalent to calling this function unless the script is a DIY component
 * or <tt>usesettings</tt> has been called.
 *
 * key1, value1,&nbsp;... : pairs of key and value strings
 */
Patch.temporary = function temporary() {
    if( arguments.length % 2 !== 0 ) {
        Error.error( string( 'scriptlib-common-patchargs' ) );
    }
    for( let i=0; i<arguments.length; i += 2 ) {
        let oldValue = Settings.shared.get( arguments[i] );
        resources.RawSettings.setGlobalSetting( arguments[i], arguments[i+1] );
        debug( string( 'scriptlib-common-patch-apply', arguments[i], arguments[i+1], oldValue ) );
    }
};



/***
 * Patch.card( component, [key1, value1], [key2, value2], ... ) [static]
 * Modifies the private settings of a component.
 * These settings apply only to <tt>component</tt> and are saved and loaded
 * with it.
 * This can be used by extensions to create custom components based on
 * existing card types by modifying their images, text, regions, and so on.
 *
 * component : the game component to modify
 * key1, value1, ... : pairs of key and value strings
 */
Patch.card = function card() {
    if( arguments.length < 3 || arguments.length % 2 != 1 ) {
        Error.error( 'Patch.card(): invalid number of arguments: ' + arguments.length );
    }
    let privateSettings = arguments[0].settings;
    for( let i=1; i<arguments.length; i += 2 ) {
        privateSettings.set( arguments[i], arguments[i+1] );
    }
};

/***
 * Patch.cardFrom( component, resource ) [static]
 * Modifies the private settings of a component by merging in all of the settings
 * stored in a resource file.
 *
 * component : the game component to modify
 * resource : the path to a resource file from which settings will be read
 */
Patch.cardFrom = function cardFrom( card, resource ) {
    card.settings.addFrom( resource );
};


/***
 * Patch.cardRestore( component, key1, [key2], ... ) [static]
 * Removes one or more private settings from a component, restoring them to
 * their default value.
 *
 * component : the game component to modify
 * key1,&nbsp;... : the names of the keys whose values should be restored
 */
Patch.cardRestore = function cardRestore() {
    if( arguments.length < 2 ) {
        Error.error( 'Patch.cardUnapply(): invalid number of arguments: ' + arguments.length );
    }
    let privateSettings = arguments[0].settings;
    for( let i=1; i<arguments.length; ++i ) {
        privateSettings.reset( arguments[i] );
    }
};



let debug;
if( arkham.plugins.debugging.ScriptDebugging.isInstalled() ) {
	debug = function debug( formatString ) {
		let msg = sprintf._fromArgArray( arguments );
		Eons.log.logp( java.util.logging.Level.INFO, sourcefile, 'debug', msg );
	};	
} else {
	debug = function debug() {};
}



//
// LANGUAGE EXTENSIONS (documented in javascript.doc)
//

Array.from = function from( o ) {
	let a;
	if( o instanceof java.lang.Iterable ) {
		o = o.iterator();
	}
	if( o instanceof java.util.Iterator ) {
		a = [];
		while( o.hasNext() ) {
			a[ a.length ] = o.next();
		}
		return a;
	}
	if( o instanceof java.util.Enumeration ) {
		a = [];
		while( o.hasMoreElements() ) {
			a[ a.length ] = o.nextElement();
		}
		return a;
	}
	if( o['length'] !== undefined ) {
		return Array.prototype.slice.call( o );
	}
	a = [];
	while( o[ a.length ] !== undefined ) {
		a[ a.length ] = o[ a.length ];
	}
	if( a.length == 0 ) {
		throw new TypeError( 'Not an array-like object.' );
	}
	return a;
};

Number.prototype.toInt = function toInt(n) {
	if( !n ) n = this;
	return new java.lang.Integer['(int)']( this );
};

Number.prototype.toLong = function toLong(n) {
	if( !n ) n = this;
	return new java.lang.Long['(long)']( this );
};

Number.prototype.toFloat = function toFloat(n) {
	if( !n ) n = this;
	return new java.lang.Float['(float)']( this );
};

Number.prototype.dontEnum( 'toInt', 'toLong', 'toFloat' );

RegExp.quote = (function() {
	const esc = /([.*+?|(){}[\]\\])/g;
	return function quote( str ) {
		return str.replace( esc, '\\$1' );
	};
})();

RegExp.quoteReplacement = (function() {
	const esc = /\$/g;
	return function quoteReplacement( str ) {
		return str.replace( esc, '$$$$' );
	};
})();

// Note: String.trim is now included in script engine

String.prototype.trimLeft = function trimLeft() {
    return this.replace( /^\s+/g, '' );
};

String.prototype.trimRight = function trimRight() {
    return this.replace( /\s+$/g, '' );
};

String.prototype.replaceAll = function replaceAll( pattern, replacement ) {
	return this.replace(
 		new RegExp( RegExp.quote( pattern ), 'g' ),
			RegExp.quoteReplacement( replacement )
	);
};

String.prototype.startsWith = function startsWith( prefix ) {
	return this.indexOf( prefix ) === 0;
};

String.prototype.endsWith = function endsWith( suffix ) {
	let i = this.length - suffix.length;
	if( i < 0 ) return false;
    return this.lastIndexOf( suffix, i ) == i;
};

String.prototype.dontEnum(
	'trimLeft', 'trimRight',
	'replaceAll', 'startsWith', 'endsWith'
);

Function.abstractMethod = function() {
	Error.warn( 'call to abstract method: this method needs to be overridden in the subclass', -2 );
};

if( useLibrary.compatibilityMode ) {
	useLibrary( 'res://libraries/common.ljs' );
}