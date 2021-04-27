/*

  uiutils.js - version 6
  Supports arbitrary manipulation of the SE user interface.


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

/**
 * Low-level utility functions for manipulating the user interface.
 * This library defines an object, <tt>UIUtils</tt> that
 * can be used to modify user interface components in a window
 * such as a component editor.
 */

var UIUtils = {};

/**
 * UIUtils.findComponentByName( parent, name ) [static]
 * Returns the child component of <tt>parent</tt> with the specified
 * <tt>name</tt> (or <tt>parent</tt> itself if it matches). If no
 * child component matches the name, returns <tt>null</tt>.
 *
 * <b>Example:</b><br>
 * <pre>
 * var parent = UIUtils.findComponentByName( Eons.window, "fileMenu" );
 * var menuItem = new swing.JMenuItem( "Now You See Me" );
 * menuItem.toolTipText = "Choose me to remove me from the menu";
 * menuItem.addActionListener( function( event ) {
 *     parent.remove( menuItem );
 * });
 * parent.add( menuItem );
 * </pre>
 *
 * parent : the root of the component tree to search
 * name : the name to search for
 *
 * returns the subcomponent of <tt>parent</tt> with the specified <tt>name</tt>, or <tt>null</tt>
 */
UIUtils.findComponentByName = (function() {
	function _findComponentByName( parent, name ) {
	    if( parent.name == name )
	        return parent;
	    for( var i=0; i<parent.componentCount; ++i ) {
	        var res = _findComponentByName( parent.getComponent( i ), name );
	        if( res != null ) return res;
	    }
		if( parent instanceof swing.JMenu ) {
			for( i=0; i<parent.itemCount; ++i ) {
				var item = parent.getItem(i);
				if( item == null ) continue;
				res = _findComponentByName( item, name );
				if( res != null ) return res;
			}
		}
	    return null;
	}

	return function findComponentByName( parent, name ) {
	    if( !parent )
	        throw new Error( "missing parent" );
	    if( !name )
	        throw new Error( "missing name" );
	    if( !(parent instanceof java.awt.Component) )
	        throw new Error( "parent is not a Component" );
	    return _findComponentByName( parent, name );
	};
})();





/**
 * UIUtils.printTree( parent ) [static]
 * Prints the tree of components with the root <tt>parent</tt> to the console.
 * For each component, the component's name is printed (or &lt;?&gt;
 * if it has no name), followed by a colon and the component's class name.
 *
 * <b>Example:</b><br>
 * <pre>
 * UIUtils.printTree( Editor.contentPane );
 * </pre>
 *
 * parent : the root of the component tree to print
 *
 * prints a component tree
 */
UIUtils.printTree = (function() {
	function _printTree( buff, parent, level ) {
	    var i;
	    for( i=0; i<level; ++i )
	        buff.append( "  " );
	    buff.append( parent.name == null ? "<?>" : parent.name );
	    var klass = parent.getClass().canonicalName;
	    if( klass == null )
	        klass = parent.getClass().name;
	    if( klass.startsWith( "javax.swing." ) )
	        klass = klass.substring(12);
	    buff.append( ": " ).append( klass ).append( "\n" );
	    for( i=0; i<parent.componentCount; ++i ) {
	        _printTree( buff, parent.getComponent(i), level+1 );
	    }
		if( parent instanceof swing.JMenu ) {
			for( i=0; i<parent.itemCount; ++i ) {
				var item = parent.getItem(i);
				if( item == null ) continue;
				_printTree( buff, item, level+1 );
			}
		}
	}

	return function printTree( parent ) {
	    if( !parent )
	        throw new Error( "missing parent" );
	    if( !(parent instanceof java.awt.Component) )
	        throw new Error( "parent is not a Component" );
	    // it is much faster to buffer up long outputs and only call print() once
	    var buff = new java.lang.StringBuilder();
	    _printTree( buff, parent, 0 );
	    print( buff.toString() );
	};
})();





/**
 * UIUtils.findWindow( c ) [static]
 * Given a component, returns the window that the component is contained in.
 * This may be <tt>null</tt> if the component has not been added to a window.
 *
 * c : the component to find the window of
 *
 * returns the window that contains the component
 */
UIUtils.findWindow = function findWindow( c ) {
	if( c != null && !(c instanceof java.awt.Component) ) {
		throw new Error( "not a component" );
	}
	do {
		if( c == null ) {
			return null;
		} else if( c instanceof java.awt.Window ) {
			return c;
		} else {
			c = c.getParent();
		}
	} while( true );
};