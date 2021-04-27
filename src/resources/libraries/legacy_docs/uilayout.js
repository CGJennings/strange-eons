/*

  uilayout.js - version 13
  Simple panel layout.


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
 * Classes that assist with laying out a user interface.
 * User interfaces created using these
 * classes can be displayed as dialogs, added as tabs to a game component
 * editor, or realized into generic containers. Containers can be
 * nested to create complex interface layouts.
 *
 * See the plug-in authoring kit for examples that use this library.
 */

/**
 * AbstractContainer( [controls...] )
 * An <tt>AbstractContainer</tt> is a base class for other classes in this
 * library. An <tt>AbstractContainer</tt> manages a list of objects and
 * can <i>realize</i> these objects into a grouping of user interface controls.
 *
 * When a container is realized, the control objects that have been added to
 * it will be converted into user interface widgets and laid out in a panel
 * or other user interface object.
 * A given container can only be realized once; attempting to realize the
 * container again will throw an exception.
 *
 * Any kind of object may be added to an <tt>AbstractContainer</tt>.
 * Interface components are added unchanged, and other objects are converted
 * into an interface component when the container is realized.
 * The standard conversions are as follows:
 * <ul>
 * <li> <tt>AbstractContainer</tt>s are converted into a control or group of
 *      controls by calling the container's <tt>realize</tt> function
 *      (in fact, this works for any object with a <tt>realize</tt> function)
 * <li> icons and images are converted into icon labels
 * <li> other kinds of objects are first converted to a string by calling their
 *      <tt>toString</tt> function, and then that string is added as a label control
 * </ul>
 *
 * Some kinds of containers can have controls added with "hints" that affect
 * how the control is laid out. Controls are added with hints using the
 * <tt>place</tt> method. Adding controls using the <tt>add</tt> method will
 * add it with a default hint (<tt>null</tt>).
 *
 * controls : an optional list of controls that will be added to the container
 */
function AbstractContainer( objects ) {
	this.controls = [];
    this.hints = [];

    // if true, _controlToComponent will set default column and row sizes on
    // JTextFields and JTextAreas if their columns/rows are set to 0; set to
    // true in simple containers like Row and Stack
    this._growTextControls = false;

	if( objects && objects.length > 0 ) {
		for( let i=0; i<objects.length; ++i ) {
			this.add( objects[i] );
		}
	}

    var realized = false;

    this._checkRealized = function() {
        useLibrary.__threadassert();
        if( realized ) throw new Error( "this container has already been realized" );
        realized = true;
    };


	/**
	 * AbstractContainer.realized [readonly]
	 * This read-only property is <tt>true</tt> if this container was previously
	 * realized, which means that realizing it again will throw an exception.
     *
     * <tt>true</tt> is this container has been realized
     */
	this.__defineGetter__( "realized", function() {return realized;} );

    /**
     * AbstractContainer.title
	 * This property defines a title for the container. If a title is set,
	 * the realized UI panel will be placed in a border titled using this text.
	 * Some types of container may ignore this setting.
     *
     * a title string to use for the container
     */
	this.title = undefined;

	/**
	 * AbstractContainer.editorTabWrapping
	 * This boolean property affects how a container will be added to a
	 * component editor when calling <tt>addToEditor</tt>. If <tt>true</tt>
	 * (the default), then the container will be nested inside of a
	 * panel. The panel will be laid out with <tt>BorderLayout</tt> and the
	 * container will be placed in the <tt>NORTH</tt> position. This produces
	 * a more consistent layout by preventing the container from expanding to
	 * cover the entire extent of the tab.
	 */
	this.editorTabWrapping = true;

	/**
	 * AbstractContainer.editorTabScrolling
	 * This boolean property affects how a container will be added to a
	 * component editor when calling <tt>addToEditor</tt>. If <tt>true</tt>,
	 * then the container will be wrapped in a scroll pane that enables vertical
	 * scrolling. If the editor is not tall enough to display all of the
	 * controls in a tab, a scroll bar will automatically appear to allow the
	 * user to scroll to unreachable controls. If <tt>false</tt> (the default),
	 * then the controls will be clipped to the bottom of their tab.
	 */
	this.editorTabScrolling = false;


	// Backwards compatibility
	this.isRealized = function isRealized() {
		return realized;
	};
    this.setTitle = function setTitle( text ) {
        this.title = text;
    };
    this.getTitle = function getTitle() {
        return this.title;
    };
}

/*
 * AbstractContainer.prototype._controlsToArray( [source] )
 * Convert an array of controls to a Java <tt>Object[]</tt>.
 * This is used sometimes used internally as a stage in realizing a container.
 * If <tt>source</tt> is <tt>undefined</tt>, it uses the controls that have
 * been added to this container.
 */
AbstractContainer.prototype._controlsToArray = function _controlsToArray( source ) {
    source = source ? source : this.controls;
	var content = java.lang.reflect.Array.newInstance( java.lang.Object, source.length );
	for( let i=0; i<source.length; ++i ) {
		content[i] = this._controlToComponent( source[i] );
	}
	return content;
};

/*
 * AbstractContainer.prototype._controlToComponent( control )
 * Convert a control object into a corresponding UI component.
 * If the control is already a UI component, it is returned
 * unchanged. If it is an icon, it is wrapped in a label.
 * If it is an image, the image is converted into an icon
 * and treated as above. Other objects are converted into
 * strings and then returned as labels.
 */
AbstractContainer.prototype._controlToComponent = function _controlToComponent( control ) {
    if( control.realize != undefined ) {
        control = control.realize();
    } else if( control instanceof java.awt.Component ) {
        // do not convert
        if( this._growTextControls ) {
            if( control instanceof swing.JTextField || control instanceof swing.JTextArea ) {
                control.columns = control.text.length() < 16 ? 16 : control.text.length();
            }
            if( control instanceof swing.JTextArea ) {
                control.rows = control.rows == 0 ? 5 : control.rows;
            }
        }
    } else if( control instanceof java.awt.Image ) {
        control = new swing.JLabel( new java.awt.ImageIcon( control ) );
    } else {
        control = new swing.JLabel( control );
    }
    return control;
};

/*
 * AbstractContainer.prototype._applyTitle( panel )
 * Wrap <tt>panel</tt> in a titled border (or not), based on the
 * <tt>title</tt> property of this container. This is a convenience
 * method that concrete subclasses can use when implementing
 * <tt>realize()</tt>.
 *
 * returns <tt>panel</tt>
 */
AbstractContainer.prototype._applyTitle = function _applyTitle( panel ) {
	var title = this.title;
	if( title !== undefined ) {
		if( title !== null ) title = title.toString();
        var titleBorder = new swing.border.TitledBorder( null, title );
        if( panel.border == null ) {
            panel.border = titleBorder;
        } else {
            panel.border = new swing.border.CompoundBorder( titleBorder, panel.border );
        }
	}
    return panel;
};


/**
 * AbstractContainer.prototype.realize() [abstract]
 * Convert this container into a user interface component that groups together
 * the controls that have been added to it. (Typically the returned object
 * is a <tt>swing.JPanel</tt>, but this is not required.)
 *
 * This method is not defined in <tt>AbstractContainer</tt> and will
 * throw an exception if called. Sublasses must override this method.
 */
AbstractContainer.prototype.realize = Function.abstractMethod;

/**
 * AbstractContainer.prototype.add( [control...] )
 * Adds zero or more control objects to this container. The added objects
 * will all be given a default hint value that allows this container
 * to lay the control out as it sees fit.
 *
 * control&nbsp;... : controls to be added with the component's default ...
 *                    placement rules
 *
 * returns this container
 */
AbstractContainer.prototype.add = function add() {
	for( let i=0; i<arguments.length; ++i ) {
        if( arguments[i] == null ) throw new Error( "missing control at index " + i );
		this.controls[ this.controls.length ] = arguments[i];
        this.hints[ this.hints.length ] = null;
	}
    return this;
};

/**
 * AbstractContainer.prototype.place( [control, hint...] )
 * Adds zero or more control objects to this container, using hints to
 * control the layout. Not all <tt>AbstractContainer</tt>s use hints.
 *
 * control, hint&nbsp;... : pairs of controls to be added and their associated ...
 *                          placement hints
 *
 * returns this container
 */
AbstractContainer.prototype.place = function place() {
    if( (arguments.length % 2) != 0 ) {
        throw new Error( "arguments to place must be pairs: control1, hint1, control2, hint2, ..." );
    }
	for( let i=0; i<arguments.length; i += 2 ) {
        if( arguments[i] == null ) throw new Error( "missing control at index " + i );
		this.controls[ this.controls.length ] = arguments[i];
        this.hints[ this.hints.length ] = arguments[i+1];
	}
    return this;
};


/**
 * AbstractContainer.prototype.getControl( index )
 * Returns the control at position <tt>index</tt> in the list of controls
 * maintained by this container.
 *
 * index : the index of the control
 *
 * returns the requested control or throws an error if <tt>index</tt> is invalid
 */
AbstractContainer.prototype.getControl = function getControl( index ) {
	if( index < 0 || index >= controls.length ) {
		throw new Error( "invalid control index: " + index );
	}
	return controls[ index ];
};

/**
 * AbstractContainer.prototype.getHint( index )
 * Returns the hint for the control at position <tt>index</tt> in the list of controls
 * maintained by this container.
 * index : the index of the control
 *
 * returns the requested control hint or throws an error if <tt>index</tt> is invalid
 */
AbstractContainer.prototype.getHint = function getHint( index ) {
	if( index < 0 || index >= controls.length ) {
		throw new Error( "invalid control index: " + index );
	}
	return hints[ index ];
};

/**
 * AbstractContainer.prototype.getControlCount()
 * Returns the number of controls in this container.
 *
 * returns the number of added controls
 */
AbstractContainer.prototype.getControlCount = function getControlCount() {
	return controls.length;
};


/**
 * AbstractContainer.prototype.addToEditor( editor, [title], [heartbeatListener], [fieldPopulationListener], [tabIndex] )
 * Realizes this container as a user interface control and adds it
 * to a component editor as a new tab.
 *
 * editor : the <tt>StrangeEonsEditor</tt> to add this to
 * title : an optional title for the tab that will be added
 * heartbeatListener :  an optional function that is called every update heartbeat
 * fieldPopulationListener : an optional function that is called when the controls should be populated with data from the component
 * tabIndex : an optional index at which to insert the stack in the list of tabs
 *
 * returns the user interface element that contains the converted stack controls
 */
AbstractContainer.prototype.addToEditor = function addToEditor( editor, title, heartbeatListener, fieldPopulationListener, tabIndex ) {
    if( !editor ) throw new Error( "missing editor" );
	title = title ? title : string( "plug-user" );
    var tabPane = AbstractContainer.findEditorTabPane( editor );
	if( tabPane != null ) {
    	var panel = this.realize();
    	if( panel.border == null ) {
    		panel.border = swing.BorderFactory.createEmptyBorder( 8, 8, 8, 8 );
    	} else {
			panel.border = new swing.border.CompoundBorder(
				swing.BorderFactory.createEmptyBorder( 8, 8, 8, 8 ),
				panel.border
			);
    	}

		var panelShell;
		if( this.editorTabWrapping ) {
			panelShell = new swing.JPanel( new java.awt.BorderLayout() );
			panelShell.add( panel, java.awt.BorderLayout.NORTH );
		} else {
			panelShell = panel;
		}

		if( this.editorTabScrolling ) {
			panelShell = new swing.JScrollPane( panelShell );
			panelShell.border = swing.BorderFactory.createEmptyBorder();
			panelShell.horizontalScrollBarPolicy = swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER;
			panelShell.verticalScrollBarPolicy = swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED;
			panelShell.verticalScrollBar.unitIncrement = 16;
		}

    	panelShell.putClientProperty( "se-custom-tab", title );
    	panelShell.setName( title );

    	tabIndex = tabIndex != null ? tabIndex : tabPane.tabCount-1;
    	tabPane.add( panelShell, tabIndex );

		// the editor is not showing, reset to the new preferred splitter size
		if( !editor.showing ) {
			var splitter = tabPane.parent;
			if( splitter instanceof swing.JSplitPane ) {
				splitter["setDividerLocation(int)"]( -1 );
			}
		}

        if( heartbeatListener ) {
            editor.addHeartbeatListener( heartbeatListener );
        }
        if( fieldPopulationListener ) {
            editor.addFieldPopulationListener( fieldPopulationListener );
        }

    	return panel;
    }
    throw new Error( "not a compatible game component type" );
};


/**
 * AbstractContainer.findEditorTabPane( editor ) [static]
 * Returns the <tt>JTabbedPane</tt> instance that houses the primary editing
 * controls for <tt>editor</tt>. Returns <tt>null</tt> if an appropriate
 * tab pane cannot be found.
 *
 * editor : the <tt>StrangeEonsEditor</tt> to search for a tab pane
 */
AbstractContainer.findEditorTabPane = function findEditorTabPane( editor ) {
	if( !editor ) return null;

	var tabPane = null;
    var content = editor.getContentPane();
    var splitter = content.getComponent(0);

	// case book editor
	if( splitter instanceof swing.JTabbedPane ) {
    	return splitter;
    }

    // most editors, including all card types
    else if( splitter instanceof swing.JSplitPane ) {
    	tabPane = splitter.leftComponent;

    	// card editor with consequences (e.g., investigator and monster)
    	if( tabPane instanceof swing.JSplitPane ) {
    		tabPane = tabPane.topComponent;
    	}

		// deck editor
    	else if( tabPane instanceof swing.JPanel ) {
    		tabPane = tabPane.getComponent(0);
    	}

    	// verify that we found a tab pane
        if( tabPane instanceof swing.JTabbedPane ) {
            return tabPane;
        }
    }
    return null;
};

/**
 * AbstractContainer.prototype.createDialog( [title], [okText], [cancelText], [icon], [modal] ) {
 * Realizes this container and places the resulting component in a dialog box.
 * The returned dialog box can be displayed by calling its <tt>showDialog()</tt>
 * method. In addition to the full suite of methods provided by <tt>swing.JDialog</tt>,
 * the returned dialog box offers the following methods:
 *
 * <div class='indent'>
 * <b>int getCloseButton()</b><br>
 * Returns the button pressed by the user to close the dialog:<br>
 *      -1 : dialog still open (possible if modeless), or user closed window
 *       &nbsp;0 : user pressed Cancel button
 *       &nbsp;1 : user pressed OK button
 *
 * <b>JButton getOKButton()</b><br>
 * Returns the dialog box's OK button (or <tt>null</tt> if it doesn't have one).
 *
 * <b>JButton getCancelButton()</b><br>
 * Returns the dialog box's Cancel button (or <tt>null</tt> if it doesn't have one).
 *
 * <b>int showDialog()</b><br>
 * Displays the dialog. If the dialog is modeless, the method returns
 * immediately. If it is modal, the script stops until the user closes
 * the dialog, and the returned value indicates the button used to close
 * the dialog (see the description of <tt>getCloseButton()</tt>.
 *
 * <b>Component getContent()</b><br>
 * Return the component that was realized by the container when the dialog
 * was created.
 * </div>
 *
 * title : an optional title for the dialog window
 * okText : optional text to use for the OK button; <tt>null</tt> for default text, "" to hide button
 * cancelText : optional text to use for the Cancel button; <tt>null</tt> for default text, "" to hide button
 * icon : an optional icon that will be placed next to the content (<tt>null</tt> for no icon)
 * modal : optional flag; if <tt>true</tt>, the script stops running until the dialog is closed
 *
 * returns the new dialog box
 */
AbstractContainer.prototype.createDialog = function createDialog( title, okText, cancelText, icon, modal ) {
    var content = this.realize();

    if( icon === undefined ) {
        icon = arkham.plugins.UILibraryDialogTemplate.defaultIcon;
    }

	if( okText === undefined ) okText = null;
	if( cancelText === undefined ) cancelText = null;

    if( modal == null || modal === undefined ) {
        modal = true;
    }

    if( title == null || title === undefined ) {
        title = string( "plug-user" );
    }

    var d = arkham.plugins.UILibraryDialogTemplate(
        Eons.window, modal, content, icon, okText, cancelText
    );

    d.title = title;

    return d;
};

/**
 * AbstractContainer.prototype.test()
 * Tests the layout of an <tt>AbstractContainer</tt> by realizing it and
 * displaying the result in a simple dialog.
 */
AbstractContainer.prototype.test = function test() {
    this.createDialog( null, "", null, null ).showDialog();
};





/**
 * Row( [control...] ) : AbstractContainer
 *
 * Creates a new <tt>Row</tt> container, which organizes controls into a
 * horizontal row. Controls in this container will accept a non-negative
 * numeric hint that adjusts the size of the gap between the hinted control
 * and the previous control. The default hints will provide a gap of 0 for the
 * first control and 1 for subsequent controls. Each unit of gap inserts
 * a small indentation.
 *
 * control : a list of zero or more controls to be added to the new <tt>Row</tt>
 */
function Row() {
	AbstractContainer.call( this, arguments );
	this.gap = 8;
    this._growTextControls = true;

    var alignment = -1;

    /**
     * Row.setAlignment( alignment )
     * Set the alignment of the row within the parent container;
     * left-justified if -1, centered if 0, right-justified if 1.
     *
     * alignment : the alignment value to set
     */
    this.setAlignment = function setAlignment( align ) {
        if( alignment < -1 || alignment > 1 ) throw new Error( "invalid alignment: " + alignment );
        alignment = align;
    };

    /**
     * Row.getAlignment()
     * Returns the alignment setting of the row.
     */
    this.getAlignment = function getAlignment() {
        return alignment;
    };
}

Row.subclass( AbstractContainer );

Row.prototype.realize = function realize() {
    this._checkRealized();

	var content = new swing.JPanel();
    var alignment = java.awt.FlowLayout.CENTER;
    if( this.getAlignment() < 0 ) {
        alignment = java.awt.FlowLayout.LEADING;
    } else if( this.getAlignment() > 0 ) {
        alignment = java.awt.FlowLayout.TRAILING;
    }
	var layout = new java.awt.FlowLayout( alignment, 0, 0 );
	layout.alignOnBaseline = true;
	content.setLayout( layout );

	for( let i=0; i<this.controls.length; ++i ) {
        var hint = this.hints[i];
        if( hint == null ) hint = i > 0 ? 1 : 0;
        if( hint > 0 ) {
            var size = this.gap * hint;
            var spacer = new swing.JLabel();
            spacer.setBorder( swing.BorderFactory.createEmptyBorder( 0, size, 0, 0 ) );
            content.add( spacer );
        }
        content.add( this._controlToComponent( this.controls[i] ) );
	}

	return this._applyTitle( content );
};










/**
 * Stack( [control...] ) : AbstractContainer
 * Creates a new <tt>Stack</tt> object, which organizes controls into a vertical
 * stack. This container does not use hints.
 *
 * control : a list of zero or more controls to be added to the new <tt>Stack</tt>
 */
function Stack() {
	AbstractContainer.call( this, arguments );
	this._growTextControls = true;
}

Stack.subclass( AbstractContainer );

Stack.prototype.realize = function realize() {
    this._checkRealized();
	var array = this._controlsToArray();
	var panel = arkham.plugins.UILibraryHelper.createStack( array );
	return this._applyTitle( panel );
};









/**
 * FixedGrid( columns, [controls...] ) : AbstractContainer
 * Creates a new <tt>FixedGrid</tt> object, which organizes controls into a simple
 * grid. All cells in a grid column have the same width, and all cells in a grid
 * row have the same height. This container recognizes a single hint, the string
 * <tt>"wrap"</tt>. A component with this hint will end the current row of controls
 * and start a new row with the next control added.
 *
 * columns : the number of column grids
 * control : a list of zero or more controls to be added to the new <tt>Stack</tt>
 */
function FixedGrid( columns ) {
	AbstractContainer.call( this );
	this._growTextControls = true;

	if( !columns ) columns = 2;

	/**
	 * FixedGrid.getColumns()
	 * Returns the number of columns in this grid.
	 */
	this.getColumns = function getColumns() {
		return columns;
	};

	/**
	 * FixedGrid.setColumns()
	 * Sets the number of columns to use when realizing the container.
	 */
	this.getColumns = function getColumns() {
		return columns;
	};

	if( arguments.length > 1 ) {
		for( let i=1; i<arguments.length; ++i ) {
			this.add( arguments[i] );
		}
	}
}

FixedGrid.subclass( AbstractContainer );

FixedGrid.prototype.realize = function realize() {
    this._checkRealized();

	var cols = this.getColumns();
	var panel = new swing.JPanel( new java.awt.GridBagLayout() );
	var cons = new java.awt.GridBagConstraints();

	var insets = new java.awt.Insets( 0, 5, 3, 0 );
	var endInsets = new java.awt.Insets( 0, 5, 3, 5 );

	cons.gridx = cons.gridy = 0;
	cons.gridwidth = 1;
	cons.gridheight = 1;
	cons.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
	cons.insets = insets;

	for( let i=0; i<this.controls.length; ++i ) {
		var wrap = this.hints[i] && this.hints[i].equals("wrap");
		var lineEnd = wrap || cons.gridx == cols-1;

		if( lineEnd ) {
			cons.insets = endInsets;
			cons.gridwidth = java.awt.GridBagConstraints.REMAINDER;
		} else {
			cons.insets = insets;
			cons.gridwidth = 1;
		}

		panel.add( this._controlToComponent( this.controls[i] ), cons );

		if( lineEnd ) {
			cons.gridx = 0;
			++cons.gridy;
		} else {
			++cons.gridx;
		}
	}

    var outerPanel = new swing.JPanel( new java.awt.BorderLayout() );
    outerPanel.add( panel, java.awt.BorderLayout.LINE_START );

	return this._applyTitle( outerPanel );
};










/**
 * Grid( [constraints], [colConstraints], [rowConstraints] ) : AbstractContainer
 * Creates a new <tt>Grid</tt> object, which organizes controls using a highly
 * flexible grid. The layout is controlled using strings that describe the
 * desired layout options.
 * <a href="http://www.migcalendar.com/miglayout/cheatsheet.html">See this
 * summary of the the hint string syntax for details.</a>
 *
 * <b>Example:</b>
 * <pre>
 * var grid = new Grid();
 * grid.place(
 *     "Name", "",
 *     textField(), "wrap, span, grow",
 *     "Ability", "",
 *     textField( "", 15 ), "grow",
 *     "Modifier", "",
 *     textField( "", 10 ), "grow"
 * );
 * grid.createDialog().showDialog();
 * </pre>
 *
 *  layoutConstraints : optional global constraints on the layout
 *  columnConstraints : optional constraints on the grid columns
 *  rowConstraints : optional constraints on the grid rows
 *
 *  returns a new <tt>Grid</tt> containter
 */
function Grid( layoutConstraints, columnConstraints, rowConstraints ) {
	if( !layoutConstraints )
		layoutConstraints = "";
	if( !columnConstraints )
		columnConstraints = "";
	if( !rowConstraints )
		rowConstraints = "";

	var layout = new Packages.net.miginfocom.swing.MigLayout(
		layoutConstraints, columnConstraints, rowConstraints
	);

	this._getLayout = function() {
		return layout;
	};

	AbstractContainer.call( this );
}

Grid.subclass( AbstractContainer );

Grid.prototype.realize = function realize() {
    this._checkRealized();
	var content = new swing.JPanel( this._getLayout() );
	for( let i=0; i<this.controls.length; ++i ) {
		content["add(java.awt.Component,java.lang.Object)"](
			this._controlToComponent( this.controls[i] ),
			this.hints[i] ? this.hints[i].toString() : null
		);
	}
	return this._applyTitle( content );
};









/**
 * TypeGrid( [hgap], [vgap] ) : AbstractContainer
 * Creates a new <tt>TypeGrid</tt> object, which organizes controls similarly
 * to a typewritten page. A <tt>TypeGrid</tt> is easier to use, but less
 * flexible than, a <tt>Grid</tt> container.
 * The layout is controlled using strings that describe the
 * desired layout options. The following constraint strings are available:
 *
 * <table border=0>
 * <tr><td>br</td><td>Insert a line break before this control</td></tr>
 * <tr><td>p</td><td>Insert a paragraph break before this control</td></tr>
 * <tr><td>tab</td><td>Align control to a tab stop</td></tr>
 * <tr><td>hfill</td><td>Cause control to fill the available horizontal space</td></tr>
 * <tr><td>vfill</td><td>Cause control to fill the available vertical space (may be used once per container)</td></tr>
 * <tr><td>left</td><td>Align subsequent controls to the left edge of the container (this is the default)</td></tr>
 * <tr><td>right</td><td>Align subsequent controls to the right edge of the container</td></tr>
 * <tr><td>center</td><td>Center subsequent controls horizontally</td></tr>
 * <tr><td>vtop</td><td>Align controls to the top of the container (this is the default)</td></tr>
 * <tr><td>vcenter</td><td>Align controls to the vertical center of the container</td></tr>
 * </table>
 *
 * <b>Example:</b>
 * <pre>
 * var typeGrid = new TypeGrid();
 * typeGrid.place(
 *     "<html><b><u>Registration", "center",
 *     "Name", "p left",
 *     textField( "", 30 ), "tab hfill",
 *     "Age", "br",
 *     textField( "", 3 ), "tab",
 *     "Gender", "br",
 *     comboBox( ["Male","Female","Other"] ), "tab"
 * );
 * typeGrid.createDialog( "Demo", "Register", null, null ).showDialog();
 * </pre>
 *
 *  layoutConstraints : optional global constraints on the layout
 *  columnConstraints : optional constraints on the grid columns
 *  rowConstraints : optional constraints on the grid rows
 *
 *  returns a new <tt>TypeGrid</tt> containter
 */
function TypeGrid( hgap, vgap ) {
	this.hgap = hgap;
	this.vgap = vgap;

	AbstractContainer.call( this );
}

TypeGrid.subclass( AbstractContainer );

TypeGrid.prototype.realize = function realize() {
    this._checkRealized();
	var layout = new Packages.se.datadosen.component.RiverLayout();
	if( !(this.hgap === undefined) ) {
		layout.hgap = this.hgap;
	}
	if( !(this.vgap === undefined) ) {
		layout.vgap = this.vgap;
	}
	var content = new swing.JPanel( layout );

	for( let i=0; i<this.controls.length; ++i ) {
		content["add(java.awt.Component,java.lang.Object)"](
			this._controlToComponent( this.controls[i] ),
			this.hints[i] ? this.hints[i].toString().replaceAll( ",", " " ) : null
		);
	}
	return this._applyTitle( content );
};










/**
 * TabPane( [control...] ) : AbstractContainer
 * Creates a new <tt>TabPane</tt> object, which organizes controls into a set of
 * tabs with mutually exclusive visibility. Each control added will be shown on a
 * separate tab. For this reason, the added controls are usually other containers.
 * The hint supplied for each added control will be used as the label for the tab.
 * If no hint is given, a series of dummy names ("Tab 1", "Tab 2", etc.), will be used.
 *
 * control : a list of zero or more controls to be added to the new <tt>TabPane</tt>
 */
function TabPane() {
	AbstractContainer.call( this, arguments );
	/**
	 * TabPane.smallTabs
	 * If set to <tt>true</tt>, then the tabbed pane will feature smaller tabs.
	 */
	this.smallTabs = false;
}

TabPane.subclass( AbstractContainer );

TabPane.prototype.realize = function realize() {
    this._checkRealized();
    var hint;
    var tabPane = new swing.JTabbedPane();
    for( let i=0; i<this.controls.length; ++i ) {
    	hint = this.hints[i];
    	if( hint == null ) hint = 'Tab ' + (i+1);
    	if( hint instanceof swing.JComponent ) {
    		tabPane.addTab( null, this._controlToComponent( this.controls[i] ) );
    		tabPane.setTabComponentAt( i, hint );
    	} else {
    		tabPane.addTab( hint.toString(), this._controlToComponent( this.controls[i] ) );
    	}
    }

    if( this.smallTabs ) {
    	var font = tabPane.font;
    	tabPane.font = font['deriveFont(float)']( font.size2D-2 );
    }

    return this._applyTitle( tabPane );
};









/**
 * Splitter( [verticalSplit], [left], [right] ) : AbstractContainer
 * Creates a new <tt>Splitter</tt> object, which creates a panel that separates
 * two controls by a splitter bar that can be dragged to change the relative
 * space that they are allotted.
 *
 * verticalSplit : if <tt>true</tt>, the splitter divides the space into two ...
 *    columns; otherwise it divides the space into two rows
 * left : the first component
 * right : the second component
 *
 *  returns a new <tt>Splitter</tt> container
 */
function Splitter( verticalSplit, left, right ) {
	AbstractContainer.call( this );
	this.verticalSplit = verticalSplit;
	if( left ) this.add( left );
	if( right ) this.add( right );
}

Splitter.subclass( AbstractContainer );

Splitter.prototype.realize = function realize() {
    this._checkRealized();

	var len = this.controls.length;
	var left = len < 1 ? "" : this.controls[0];
	var right = len < 2 ? "" : this.controls[1];
	left = this._controlToComponent( left );
	right = this._controlToComponent( right );

	var content = new swing.JSplitPane(
		this.verticalSplit ? swing.JSplitPane.HORIZONTAL_SPLIT : swing.JSplitPane.VERTICAL_SPLIT,
		true, left, right
	);
	content.setOneTouchExpandable( true );
	content.setDividerSize(8);
	content['setDividerLocation(int)'](-1);
	return this._applyTitle( content );
};
