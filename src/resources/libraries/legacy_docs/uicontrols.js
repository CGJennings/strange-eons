/*

  uicontrols.js - version 17
  Helper functions for UI controls.


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
 * Convenience functions that create user interface controls.
 * These functions make it easy to create UI controls to add to a container
 * layout. See the plug-in authoring kit for examples that use this library.
 */

/**
 * textField( [text], [columns], [listener], [spellingChecked] )
 * Returns a new <tt>swing.JTextField</tt>.
 *
 * text : optional initial text for the field (default is an empty string)
 * columns : an optional number of columns for the width of the field (default is 0)
 * listener : an optional function that will be called when enter is pressed in ...
 *     the field (default is no listener)
 * spellingChecked : if <tt>true</tt>, live spelling checking will be performed ...
 *     on the contents of the field (default is <tt>true</tt>)
 *
 * returns the new text field
 */
function textField( text, columns, listener, spellingChecked ) {
    useLibrary.__threadassert();
	text = text ? text : "";
    if( columns < 0 || columns === undefined ) columns = 0;
	if( spellingChecked === undefined ) spellingChecked = true;
	var field = spellingChecked ?
		  new ca.cgjennings.spelling.ui.JSpellingTextField( text, columns )
		: new swing.JTextField( text, columns );
	if( listener ) {
		field.addActionListener( listener );
	}
    field.font = resources.ResourceKit.getEditorFont();
    field.select( 0, 0 );
	return field;
}

/**
 * textArea( [text], [rows], [columns], [scroll], [spellingChecked] )
 * Returns a new <tt>JTextArea</tt>.
 *
 * text : optional initial text for the text area (default is an empty string)
 * rows : an optional number of rows for the width of the text area (default is 0)
 * columns : an optional number of columns for the width of the text area (default is 0)
 * scroll : if <tt>true</tt>, a scroll pane will be wrapped around the text ...
 *     area and returned
 * spellingChecked : if <tt>true</tt>, live spelling checking will be performed ...
 *     on the contents of the field (default is <tt>true</tt>)
 *
 * returns the new text area, or the scroll pane that contains the text area
 *     if <tt>scroll</tt> is <tt>true</tt>
 */
function textArea( text, rows, columns, scroll, spellingChecked ) {
    useLibrary.__threadassert();
	text = text ? text : "";
    if( rows === undefined || rows < 0 ) rows = 0;
    if( columns === undefined || columns < 0 ) columns = 0;
	if( spellingChecked === undefined ) spellingChecked = true;
	var field = spellingChecked ?
		  new ca.cgjennings.spelling.ui.JSpellingTextArea( text, rows, columns )
		: new swing.JTextArea( text, rows, columns );
    field.font = resources.ResourceKit.getEditorFont();
    field.select( 0, 0 );
    field.lineWrap = true;
    field.wrapStyleWord = true;
    field.tabSize = 4;
    if( scroll ) {
        field = new swing.JScrollPane( field );
	}
	return field;
}


/**
 * codeArea( [text], [preferredWidth], [preferredHeight] )
 * Returns a new script editor control. The code area object
 * includes an <tt>execute()</tt> function that will run the script code
 * contained in the editor.
 *
 * text : optional initial text for the code area
 * preferredWidth : an optional preferred width for the control (default is 400)
 * preferredHeight : an optional preferred height for the control (default is 300)
 *
 * returns the new code editor
 */
function codeArea( text, preferredWidth, preferredHeight ) {
    useLibrary.__threadassert();
	text = text ? text : "";
	var field = arkham.plugins.UILibraryHelper.createCodeArea();
	field.setText( text );
	field.select( 0, 0 );
    if( !preferredWidth || preferredWidth < 1 ) preferredWidth = 400;
    if( !preferredHeight || preferredHeight < 1 ) preferredHeight = 300;
    field.setPreferredSize( new java.awt.Dimension( preferredWidth, preferredHeight ) );
    field.setBorder( swing.BorderFactory.createLineBorder( java.awt.Color.BLACK, 1 ) );
	return field;
}

/**
 * button( [label], [icon], [listener] )
 * Returns a new <tt>swing.JButton</tt>.
 *
 * label : an optional text label for the button
 * icon : an optional icon for the button
 * listener : an optional function that will be called when the button is pressed
 *
 * returns the new button
 */
function button( label, icon, listener ) {
    useLibrary.__threadassert();
    if( label === undefined )
        label = null;
    icon = icon ? icon : null;
	var button;
	if( label ) {
		button = new swing.JButton( label, icon );
	} else {
		button = new swing.JButton["(javax.swing.Icon)"]( icon );
	}
	if( listener ) {
		button.addActionListener( listener );
	}
	return button;
}

/**
 * repeaterButton( [label], [icon], [listener] )
 * Returns a new button that continues to send notify the listener at intervals
 * as long as it is pressed down. The rate of listener notifications
 * increases the longer the button is held down. The button tracks the modifier
 * keys that are held down while repeating and passes these through the
 * <tt>ActionEvent</tt>s that it generates. (For example, the portrait panel
 * nudge controls jump in bigger steps if <i>Shift</i> is held down.)
 *
 * label : an optional text label for the button
 * icon : an optional icon for the button
 * listener : an optional function that will be called when the button is pressed
 *
 * returns the new button
 */
function repeaterButton( label, icon, listener ) {
    useLibrary.__threadassert();
    if( label === undefined )
        label = null;
    icon = icon ? icon : null;
	var button;
	if( label ) {
		button = new ca.cgjennings.ui.JRepeaterButton( label, icon );
	} else {
		button = new ca.cgjennings.ui.JRepeaterButton["(javax.swing.Icon)"]( icon );
	}
	if( listener ) {
		button.addActionListener( listener );
	}
	return button;
}



/**
 * toggleButton( [label], [icon], [selected], [listener] )
 * Returns a new <tt>swing.JToggleButton</tt>.
 *
 * label : an optional text label for the button
 * icon : an optional icon for the button
 * selected : if <tt>true</tt>, the box is initially selected
 * listener : an optional function that will be called when the button is pressed
 *
 * returns the new button
 */
function toggleButton( label, icon, selected, listener ) {
    useLibrary.__threadassert();
    if( label === undefined )
        label = null;
    icon = icon ? icon : null;
	var button;
	if( label ) {
		button = new swing.JToggleButton( label, icon );
	} else {
		button = new swing.JToggleButton( icon );
	}
    button.selected = selected ? true : false;
	if( listener ) {
		button.addActionListener( listener );
	}
	return button;
}



/**
 * radioButton( [label], [selected], [listener] )
 * Returns a new <tt>swing.JRadioButton</tt>. Radio buttons are use to represent
 * a group of options of which only one can be selected at a time.
 * To define the members of such a group, first create the buttons and then
 * call <a href='#buttonGroupbuttonsvalues'>buttonGroup( arrayOfButtonsToGroup )</a>.
 *
 * label : an optional text label for the button
 * selected : if <tt>true</tt>, the box is initially selected
 * listener : an optional function that will be called when the button is pressed
 *
 * returns the new radio button
 */
function radioButton( label, selected, listener ) {
    useLibrary.__threadassert();
    if( label === undefined )
        label = null;
	var button;
	if( label ) {
		button = new swing.JRadioButton( label );
	} else {
		button = new swing.JRadioButton();
	}
    button.selected = selected ? true : false;
	if( listener ) {
		button.addActionListener( listener );
	}
	return button;
}



/**
 * buttonGroup( buttons, [settingValues] )
 * Returns a new button group for a group of buttons. Button groups are used
 * to link together a set of related toggle buttons or radio buttons so that
 * only one button in the group can be selected at a time. The optional
 * <tt>settingValues</tt> argument is a list of setting values to associate with
 * the buttons in the group. If it is supplied, then the returned button group
 * can be bound using a <tt>Bindings</tt> instance, and selecting a button
 * in the group will change the bound setting to the corresponding element
 * in <tt>settingValues</tt>. If the <tt>settingValues</tt> argument is not
 * supplied, then a plain button group that does not support binding is returned.
 *
 * buttons : an array of buttons to be grouped together for mutually exclusive selection
 * values : an optional array of setting values (strings) to map the buttons to
 */
function buttonGroup( buttons, settingValues ) {
	useLibrary.__threadassert();
	var bg;
	if( settingValues === undefined ) {
		bg = new swing.ButtonGroup();
		for( let i=0; i<buttons.length; ++i ) {
			bg.add( buttons[i] );
		}
	} else {
		bg = new arkham.plugins.UILibraryHelper.BindableGroup( buttons, settingValues );
	}
	return bg;
}



/**
 * cycleButton( labels, [settingValues] )
 * Returns a new cycle button that will rotate through the specified labels
 * when pressed. Cycle buttons are suitable for use with a very small number
 * of options, preferably 2, when the user can easily guess what the
 * other options in the set are. An example of this is a button to select
 * a gender. If the optional setting values are provided, they will be used
 * to map the selected label to and from a setting value when the control
 * is bound using a <tt><a href='scriptdoc:uibindings'>Bindings</a></tt>
 * instance.
 *
 * buttons : an array of labels for the button to rotate through when pressed
 * values : an optional array of setting values (strings) to map the labels to
 */
function cycleButton( labels, settingValues ) {
	useLibrary.__threadassert();
	if( settingValues == null ) {
		return new ca.cgjennings.ui.JCycleButton( labels );
	} else {
		return new ca.cgjennings.ui.JCycleButton( labels, settingValues );
	}
}



/**
 * checkBox( [label], [selected], [listener] )
 * Returns a new <tt>swing.JCheckBox</tt>.
 *
 * label : an optional text label for the button
 * selected : if <tt>true</tt>, the box is initially checked
 * listener : an optional function that will be called when the box is checked or unchecked
 *
 * returns the new check box
 */
function checkBox( label, selected, listener ) {
    useLibrary.__threadassert();
    if( label === undefined )
        label = null;
	var button = new swing.JCheckBox( ""+label );
	if( listener ) {
		button.addActionListener( listener );
	}
    button.selected = selected ? true : false;
	return button;
}

/**
 * comboBox( [items], [listener] )
 * Returns a new <tt>swing.JComboBox</tt> containing the given list of <tt>list</tt>.
 *
 * items : an array of items for the user to choose from (default is an empty list)
 * listener : an optional listener that is called when the selected item changes
 */
function comboBox( items, listener ) {
	useLibrary.__threadassert();
	if( !items ) items = [];
	var model = new swing.DefaultComboBoxModel();
	for( let i=0; i<items.length; ++i ) {
		model.addElement( items[i] );
	}
	var comboBox = new swing.JComboBox( model );
	comboBox.renderer = arkham.diy.ListItemRenderer.shared;
	comboBox.editable = false;
    comboBox.maximumRowCount = 12;
	if( listener ) {
		comboBox.addActionListener( listener );
	}
	return comboBox;
}


/**
 * autocompletionField( items, [sort] )
 * Create a text field with a drop-down autocompletion list.
 *
 * items : an array of items that the field will use to offer autocompletion choices
 * sorted : an optional flag indicating that the item list should be sorted, ...
 *     which allows faster autocompletion
 *
 * returns a new, editable <tt>swing.JComboBox</tt> with autocompletion support
 */
function autocompletionField( items, sorted ) {
    useLibrary.__threadassert();
	if( !items || items["length"] === undefined )
		throw new Error( "missing array of autocompletion items" );

    var set;
    if( sorted )
        set = new java.util.TreeSet( Language.getInterface().collator );
    else
        set = new java.util.LinkedHashSet();
    java.util.Collections.addAll( set, items );

	var field = new ca.cgjennings.spelling.ui.JSpellingComboBox( set.toArray() );
	field.editable = true;
	field.font = resources.ResourceKit.getEditorFont();
    field.maximumRowCount = 12;

	var acdoc = ca.cgjennings.ui.AutocompletionDocument.install( field );
    if( sorted ) acdoc.collator = Language.getInterface().collator;
	return field;
}


/**
 * listControl( [items], [listener], [scroll] )
 * Returns a new <tt>swing.JList</tt> containing the given list of <tt>items</tt>.
 *
 * items : an array of items for the user to choose from (default is an empty list)
 * listener : an optional listener that is called when the selected item changes
 * scroll : if <tt>true</tt>, a scroll pane will be wrapped around the list ...
 *     control and returned
 *
 * returns the new list control, or the scroll pane that contains the list control
 *     if <tt>scroll</tt> is <tt>true</tt>
 */
function listControl( items, listener, scroll ) {
	useLibrary.__threadassert();
	if( !items ) items = [];
	var model = new swing.DefaultListModel();
	for( let i=0; i<items.length; ++i ) {
		model.addElement( items[i] );
	}
	var list = new swing.JList( model );
	list.cellRenderer = arkham.diy.ListItemRenderer.shared;
	list.selectionMode = swing.ListSelectionModel.SINGLE_SELECTION;
	if( listener ) {
		list.addListSelectionListener( listener );
	}
	if( scroll ) {
		list = new swing.JScrollPane( list );
	}
	return list;
}


/**
 * label( [text], [labelFor] )
 * Returns a new <tt>JLabel</tt>.
 *
 * text : initial label text (default is an empty string)
 * labelFor : the component that this labels; this is used to determine which
 *     control to activate if the label has a mnemonic key set
 *
 * returns a new label
 */
function label( text, labelFor ) {
    var label = new swing.JLabel['(java.lang.String)']( text );
	if( labelFor ) {
		label.setLabelFor( labelFor );
	}
	return label;
}

/**
 * noteLabel( [text] )
 * Returns a new note label, a label with a smaller than usual
 * font size that can be used to add remarks, tips, or other
 * secondary information.
 *
 * text : the text of the note label
 *
 * returns a new label with a smaller font
 */
function noteLabel( text ) {
    var note = label( text );
	var f = note.font;
	note.font = f.deriveFont( f.size2D-1 );
	return note;
}

/**
 * separator( [vertical] )
 * Returns a new separator, a dividing line that can be used to
 * visually separate groups of controls.
 *
 * vertical : if <tt>true</tt> the separator is a vertical line rather than ...
 *     the default horizontal
 *
 * returns a new separator
 */
function separator( vertical ) {
	if( vertical ) {
		return new swing.JSeparator( swing.JSeparator.VERTICAL );
	} else {
		return new swing.JSeparator();
	}
}



/**
 * hyperlink( [text], url )
 * Returns a new hyperlink label.
 * This is a label with underlined blue text that opens
 * a URL in the user's default browser when clicked.
 *
 * text : an optional text label (default is to use <tt>url</tt>)
 * url : the URL to visit when the label is clicked
 *
 * returns the new hyperlink label
 */
function hyperlink( text, url ) {
    useLibrary.__threadassert();
    if( !text ) throw new Error( "missing URL" );
    if( !url ) url = text;
    var field = new ca.cgjennings.ui.JLinkLabel( text );
    field.setURI( new URI( url ) );
    return field;
}



/**
 * helpButton( urlOrWikiPage )
 * Returns a new help button. A help button displays as a small purple
 * help icon. When clicked on, it opens a help page in the user's default
 * browser. If <tt>url</tt> contains a colon (':') then it is assumed to be a
 * complete URL for the page to be opened. Otherwise, it is assumed to be the
 * title of a page in the Strange Eons Wiki.
 * For example, <tt>"User Manual"</tt> would open the contents page
 * of the user manual.
 *
 * urlOrWikiPage : the URL or the title of a Wiki page to open when the button is clicked
 *
 * returns the new help button
 */
function helpButton( url ) {
	useLibrary.__threadassert();
	if( !url ) throw new Error( "missing URL" );
	var b = new ca.cgjennings.ui.JHelpButton();
	if( url.indexOf( ":" ) >= 0 ) {
		b.helpPage = url;
	} else {
		b.wikiPage = url;
	}
	return b;
}



/**
 * tipButton( text )
 * Returns a new tip button. A tip button displays as a small lantern icon.
 * When the pointer is moved over the icon, a small pop-up window displays
 * the tip text.
 *
 * text : the text to display when the pointer is moved over the tip icon
 *
 * returns the new tip button
 */
function tipButton( text ) {
	useLibrary.__threadassert();
	if( !text ) throw new Error( "missing tip text" );
	if( text.indexOf( "\n" ) >= 0 ) {
		text = "<html>" + text.replaceAll( "&", "&amp;" )
			.replaceAll( "<", "&lt;" ).replaceAll( ">", "&gt;" )
			.replaceAll( "\n", "<br>" );
	}
	var b = new ca.cgjennings.ui.JTip( text );
	return b;
}



/**
 * spinner( [min], [max], [stepSize], [initialValue], [listener] )
 * Creates a new spinner control that can be set to one of
 * a range of integer values between <tt>min</tt> and <tt>max</tt>, inclusive.
 * Each click of a spinner arrow will add or subtract <tt>stepSize</tt>
 * from the current value.
 *
 * min : the minimum value that the spinner will allow (default is 1)
 * max : the maximum value that the spinner will allow (default is 10)
 * stepSize : the amount added to the current value by clicking an arrow button (default is 1)
 * initialValue : the initial value stored in the spinner (default is <tt>min</tt>)
 * listener : an optional listener (<tt>swing.event.ChangeListener</tt>) that will be called when the value changes
 */
function spinner( min, max, stepSize, initialValue, listener ) {
    useLibrary.__threadassert();
	if( min === undefined ) min = 1;
	if( max === undefined ) max = 10;
    if( stepSize == 0 || stepSize == undefined ) stepSize = 1;
    if( initialValue === undefined ) initialValue = min;
    var control = new swing.JSpinner(
        new swing.SpinnerNumberModel["(int,int,int,int)"]( initialValue, min, max, stepSize )
    );
    if( listener ) {
        control.addChangeListener( listener );
    }
    return control;
}

/**
 * slider( [min], [max], [initialValue], [valueLabelPairs], [listener] )
 * Creates a new slider control that can be set to one of
 * a range of integer values between <tt>min</tt> and <tt>max</tt>, inclusive.
 * If <tt>valueLabelPairs</tt> is supplied, it must be an array where the
 * even indices are slider positions and the odd indices are labels.
 * The control will display the requested labels at the indicated positions.
 * For example: <tt>[1, "Low", 6, "Medium", 10, "High"]</tt> would display the
 * labels "Low", "Medium", and "High" at positions 1, 6, and 10 on the slider,
 * respectively.
 *
 * min : the minimum value that the slider will allow (default is 1)
 * max : the maximum value that the slider will allow (default is 10)
 * stepSize : the amount added to the current value by clicking an arrow button (default is 1)
 * valueLabelPairs : an array that associates slider positions with labels
 * listener : an optional listener (<tt>swing.event.ChangeListener</tt>) that will be called when the value changes
 */
function slider( min, max, initial, valueLabelPairs, listener ) {
	useLibrary.__threadassert();
	if( min === undefined ) min = 1;
	if( max === undefined ) max = 10;
	if( initial === undefined ) initial = min;

    min = (min === undefined) ? 1 : min;
	max = (max === undefined) ? 10 : max;
	if( max < min ) max = min;
	initial = (initial === undefined) ? min : initial;

	var control = new swing.JSlider( min, max, initial );

    if( listener ) {
        control.addChangeListener( listener );
    }

	if( valueLabelPairs ) {
		var dict = new java.util.Hashtable();
		for( let i=0; i < valueLabelPairs.length; i += 2 ) {
			if( !(valueLabelPairs[i+1] instanceof java.awt.Component) ) {
				valueLabelPairs[i+1] = noteLabel( valueLabelPairs[i+1].toString() );
			}
			dict.put(
			        java.lang.Integer.valueOf( valueLabelPairs[i] ),
					valueLabelPairs[i+1]
			);
		}
		control.labelTable = dict;
		control.paintLabels = true;
	}

	return control;
}

/**
 * tintPanel()
 * Creates a new control panel for adjusting tints.
 */
function tintPanel() {
	useLibrary.__threadassert();
	var panel = new arkham.HSBPanel();
	panel.presetsVisible = false;
    return panel;
}

/**
 * portraitPanel( gc, [portraitIndex], [title] )
 * Creates a portrait panel that allows the user to choose and adjust a portrait
 * image. For DIY components, note that if you use the standard simple method
 * of adding a portrait, the portrait panel will be created and linked up for you.
 * If you use the custom portrait handling option, you'll need to create and
 * add portrait panels yourself.
 *
 * gc : the <tt>PortraitProvider</tt> (usually a game component, such as a DIY) ...
 *     that provides the portrait model
 * portraitIndex : the index of the portrait for a portrait provider ...
 *     with multiple portraits (default is 0)
 * title : an optional title for the panel; if not specified, a localized default
 *     title will be provided
 */
function portraitPanel( gc, portraitIndex, title ) {
	useLibrary.__threadassert();
	var panel = new arkham.PortraitPanel();
	panel.portrait = gc.getPortrait( portraitIndex );
	if( title !== undefined ) {
		panel.panelTitle = title;
	}
	return panel;
}

/**
 * noMarkup( control )
 * Calling this function on a UI control will prevent that control from
 * becoming a markup target. Apply this function to text fields when you
 * do not want the user to be able to insert text into them using the Markup
 * menu. The function returns the control, so it can be used transparently
 * when creating a control:
 * <pre>var field = noMarkup( textField( '42', 4 ) );</pre>
 *
 * control : the UI control to prevent from accepting markup
 */
function noMarkup( c ) {
	c.putClientProperty( arkham.MarkupTarget.FORCE_MARKUP_TARGET_PROPERTY, java.lang.Boolean.FALSE );
	c.putClientProperty( arkham.ContextBar.BAR_DISABLE_PROPERTY, java.lang.Boolean.TRUE );
	return c;
}
