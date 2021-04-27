/*

  uibindings.js - version 14
  Create and manage bindings between controls and data.


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
 * Create bindings between user interface (UI) controls and private settings on
 * components.
 * Bindings automate the details of synchronizing the state of the user interface
 * controls with the state of an edited game component.
 * When the user manipulates a bound control, the new state is converted into a
 * setting string value and written to the component. When the component
 * is loaded into an editor that uses bound controls, the bound controls will
 * be set to match the setting value in the component.
 */

importClass( arkham.diy.SettingBackedControl );

/**
 * Binding( name, uiControl, gameComponent, [sheetsToUpdate] ) [ctor]
 * A <tt>Binding</tt> is an association between a UI component
 * and part of the game component's state, usually one of the game
 * component's private settings. When the UI component is activated
 * by the user, calling <tt>Binding.update()</tt> will convert the state
 * of the UI component into a game component setting and use this to
 * update the game component so that it matches the state of the UI component.
 * When the game component is loaded from a file, calling <tt>Binding.initComponent()</tt>
 * will read the game component's state and update the state of the
 * UI component to match it.
 *
 * When you create a binding, you provide a name, a UI control, a
 * game component, and an array of numbers. The array of numbers is a
 * list of card faces (0 for front, 1 for back, and so on)
 * that need to be updated when the bound state changes. For
 * example, if the bound state represented a monster's toughness, then
 * you would use <tt>[1]</tt>, because this information only appears on the
 * back of a monster token.
 *
 * <b>The Binding Process</b><br>
 * When the <tt>update()</tt> method is called,
 * it will first call <tt>controlToSetting()</tt> to convert the
 * state of the UI component into a setting value (a string).
 * It will then look in the private settings of the game component for
 * a setting with the provided name. If the setting does not exist or
 * is different from the the setting returned from <tt>controlToSetting()</tt>,
 * then it will copy the new value into the game component's private settings
 * and mark the sheets listed in the list of card faces as being out of date.
 * <tt>update()</tt> returns <tt>true</tt> if it updated the component.
 *
 * When the <tt>initComponent()</tt> method is called,
 * it will first fetch the named setting from the game component's
 * private settings and then call <tt>settingToControl()</tt> to
 * modify the state of the control to reflect the setting value.
 *
 * <b>Handling of <tt>SettingBackedControl</tt>s</b><br>
 * The <tt>arkham.diy.SettingBackedControl</tt> can be used to customize the
 * mapping between the component state and setting values. When the bound control
 * implements this interface, then <tt>update</tt> and <tt>initComponent</tt>
 * will call the <tt>fromSetting</tt> and <tt>toSetting</tt> methods provided
 * by the control instead of the default <tt>controlToSetting</tt> and
 * <tt>settingToControl</tt> methods provided by the binding class.
 * As an example of when this would be useful, consider the case where you
 * have a combo box of different options, and you want those options to be
 * localized into the game language. The default binding class will use the string
 * value of the selected option, so if a component is created in one language,
 * saved, and opened in another language, the value saved with the component
 * won't match any of the available options (since it is written in the original
 * language). Using <tt>SettingBackedControl</tt>, you could map each label to
 * a number or other neutral identifer instead. (Although, if the list of
 * options is fixed, you could use <tt>IndexedComboBoxBinding</tt> for this
 * purpose.)
 *
 * <b>Writing Binding Classes</b><br>
 * The <tt>Binding</tt> base class will copy the text that a user writes
 * in a text component to a private setting (for <tt>update()</tt>) and
 * will set the text in the component to the value of the private setting
 * (for <tt>initComponent()</tt>. For other kinds of components, you need to create
 * an appropriate subclass that knows about the specific kind of control
 * it is binding the game component state to. To create a subclass you
 * only need to override the <tt>controlToSetting()</tt> and
 * <tt>settingToControl()</tt> methods to handle the new type of control.
 * For example, the following binding will bind a checkbox to a yes/no
 * setting value in the game component:
 * <pre>
 * function CheckboxBinding( name, uiControl, gameComponent, sheetsToUpdate ) {
 *     // call superclass constructor
 *     Binding.call( this, name, uiControl, gameComponent, sheetsToUpdate );
 * }
 *
 * CheckboxBinding.subclass( Binding );
 *
 * CheckboxBinding.prototype.toSetting = function controlToSetting( control ) {
 *     if( control.selected )
 *         return 'yes';
 *     else
 *         return 'no';
 * }
 *
 * CheckboxBinding.prototype.settingToControl = function settingToControl( control, value ) {
 *     control.selected = value != null && value.equals( 'yes' );
 * }
 * </pre>
 *
 * If you wish to create more advanced binding behaviours, such as calling methods
 * on the game component instead of setting custom settings, you can override
 * <tt>update()</tt> and <tt>initComponent</tt>. It is critical that the <tt>update()</tt>
 * method returns <tt>true</tt> <i>if and only if</i> the setting is updated with a different
 * value.
 *
 * name : the name of the binding; this is used as the setting key to bind the control to; ...
 *     if the name starts with $, the actual key name will be determined by removing the ...
 *     $ and replacing all underscore characters with dashes
 * uiControl : the UI component that takes part in the binding
 * gameComponent : the game component to bind with the UI component
 * sheetsToUpdate : an aray of sheet indices that depend on the bound setting
 */
function Binding( name, uiControl, gameComponent, sheetsToUpdate ) {
	if( !name ) throw new Error( 'missing bind name' );
	if( !uiControl ) throw new Error( 'missing uiControl' );
	if( !gameComponent ) throw new Error( 'missing gameComponent' );

	if( !(uiControl instanceof java.awt.Component) && !(uiControl instanceof swing.ButtonGroup) )
		throw new Error( 'uiControl must be a Component or ButtonGroup' );

	if( !(gameComponent instanceof arkham.component.GameComponent) )
		throw new Error( 'gameComponent must be a GameComponent' );

	if( name.startsWith('$') ) {
		name = name.substring(1).replace( '_', '-' );
	}

	if( sheetsToUpdate === undefined ) {
		this.sheetBits = -1;
	} else {
		this.sheetBits = 0;
		for( let i=0; i<sheetsToUpdate.length; ++i ) {
			if( sheetsToUpdate[i] > 31 )
				throw new Error( 'illegal sheet number: ' + sheetsToUpdate[i] );
			if( gameComponent.sheets != null && sheetsToUpdate[i] >= gameComponent.sheets.length )
				throw new Error( 'sheet does not exist: ' + sheetsToUpdate[i] );
			this.sheetBits |= (1 << sheetsToUpdate[i]);
		}
	}

    this.isActive = false;
    this.name = name;
    this.settings = gameComponent.settings;
    this.component = gameComponent;
    this.control = uiControl;

	if( this.settings.get( name ) != null ) {
		this.initComponent();
	}
}

/**
 * Binding.prototype.update()
 * Updates the UI component setting using the current value of the
 * control.
 *
 * returns <tt>true</tt> if the setting has changed
 */
Binding.prototype.update = function update() {
    var oldValue = this.settings.get( this.name );
	var newValue;
	if( this.control instanceof SettingBackedControl ) {
		newValue = this.control.toSetting();
	} else {
		newValue = this.controlToSetting( this.control );
	}
    if( !newValue.equals( oldValue ) ) {

        this.settings.set( this.name, newValue );
        var mask = this.sheetBits;
        var sheets = this.component.sheets;

		if( sheets ) {
			for( let i=0; i<sheets.length; ++i, mask >>= 1 ) {
				if( (mask & 1) == 1 ) {
					this.component.markChanged( i );
				}
			}
		}
        return true;
    }
    return false;
};

/**
 * Binding.prototype.initComponent()
 * Updates the UI component using the current setting value.
 */
Binding.prototype.initComponent = function initComponent() {
	var value = this.settings.get( this.name );
	if( this.control instanceof SettingBackedControl ) {
		value = this.control.fromSetting( value );
	} else {
		this.settingToControl( value, this.control );
	}
};

Binding.prototype.toString = function toString() {
    var s = 'Binding "' + this.name + '": [' + this.control.getClass().getSimpleName()
         + ' -> ' + this.component.getClass().getSimpleName() + '], sheets: [';
    var set = 0;
    var mask = this.sheetBits;
    var MAX_SHEETS = 31;

    for( let i=0; i<MAX_SHEETS; ++i, mask >>= 1 ) {
        if( (mask & 1) == 1 ) {
            if( set++ > 0 ) s += ', ';
            s += i;
        }
    }

    return s + ']';
};


/**
 * Binding.prototype.controlToSetting( control )
 * Convert the state of the <tt>control</tt> to a setting string.
 * Subclasses override this to customize how the control's state is
 * represented in the game component's private settings.
 *
 * control : the control whose state must be converted to a string
 *
 * returns a string representing the state of the control
 */
Binding.prototype.controlToSetting = function controlToSetting( control ) {
    if( control['text'] === undefined )
        throw new Error( 'you must provide a binding class for this control: ' + this.name );
    return control.text;
};

/**
 * Binding.prototype.settingToControl( value, control )
 * Change the state of the <tt>control</tt> to reflect the provided
 * setting value.
 * Subclasses override this to customize how the control's state is
 * represented in the game component's private settings.
 *
 * value : the string value to be represented by the control
 * control : the UI component to modify
 */
Binding.prototype.settingToControl = function settingToControl( value, control ) {
    if( !(control['text'] === undefined) ) {
        control.text = value;
        control.select( 0, 0 );
    } else {
        throw new Error( 'you must provide a binding class for this control: ' + this.name );
    }
};



/**
 * ActiveBinding( name, uiControl, gameComponent, sheetsToUpdate ) : Binding [ctor]
 * An <tt>ActiveBinding</tt> is a type of binding that can determine for itself
 * when it needs to update the game component because of a change to the
 * state of the UI control. For example, the binding might install
 * a listener on the control that will be notified when the control is updated.
 * The listener would then call <tt>update()</tt> when it is notified of a
 * change by the control.
 *
 * To create a new type of active binding, you must subclass <tt>ActiveBinding</tt> and
 * override the <tt>installActivationHandler()</tt> method to make the binding
 * active. (Typically, this means adding a listener to <tt>this.control</tt>.)
 * This base class adds an <tt>ActionListener</tt> to <tt>this.control</tt>,
 * which is sufficient for many kinds of component.
 *
 * name : the name of the binding; this is used as the setting key to bind the control to
 * uiControl : the UI component that takes part in the binding
 * gameComponent : the game component to bind with the UI component
 * sheetsToUpdate : an array of sheet indices that depend on the bound setting
 */
function ActiveBinding( name, uiControl, gameComponent, sheetsToUpdate ) {
    Binding.call( this, name, uiControl, gameComponent, sheetsToUpdate );
    this.isActive = true;
    this.installActivationHandler();
}

ActiveBinding.subclass( Binding );

ActiveBinding.prototype.installActivationHandler = function installActivationHandler() {
    try {
        var thiz = this;
        this.control.addActionListener( function actionListener() {
                thiz.update();
        });
    } catch( ex ) {
        throw new Error(
			'this control does not support an ActionListener: you must create ' +
			'a custom ActiveBinding subclass or use a Binding instead'
		);
    }
};





/**
 * Bindings( editor, [gameComponent] ) [ctor]
 * A collection of bindings for a group of custom controls in
 * <tt>editor</tt> that are bound to settings in <tt>gameComponent</tt>.
 * If <tt>gameComponent</tt> is not specified, the game component currently
 * installed in the editor will be used.
 *
 * editor : the editor that will contain the bound controls
 * gameComponent : the game component that that will be edited using the controls
 */
function Bindings( editor, gameComponent ) {
	if( !gameComponent ) gameComponent = editor.gameComponent;

	this.bindings = [];
    this.activeBindings = [];
    this.editor = editor;
    this.component = gameComponent;
    this.alreadyBound = false;
}

/**
 * Bindings.prototype.add( name, control, [sheets], [bindClass] )
 * Create a new <tt>Binding</tt> of type <tt>bindClass</tt> and add it to
 * this set of bindings. The binding is created as if by calling
 * <tt>new bindClass( name, control, gameComponent, sheets )</tt>.
 * (If <tt>control</tt> is a scroll pane, then the component contained by
 * the scroll pane will be bound rather than the scroll pane itself.)
 *
 * If <tt>bindClass</tt> is not specified, then a default class will be
 * searched for <tt>Bindings.getBindingClass</tt>.
 *
 * name : the setting key to use for the binding
 * control : the UI control that will be used to edit the setting
 * sheetsToUpdate : an array of sheet indices that depend on the bound setting
 * bindClass : the optional <tt>Binding</tt> contructor to be used to create the binding
 */
Bindings.prototype.add = function add( name, control, sheets, bindClass ) {
	if( control instanceof swing.JScrollPane ) {
		control = control.viewport.view;
	}
    if( !bindClass ) {
        bindClass = Bindings.getBindingClass( control.getClass() );
        if( bindClass == null ) {
            throw new Error( 'no binding class is registered for ' + control.getClass() );
        }
    }

    var binding = new bindClass( name, control, this.component, sheets );
    if( binding.isActive ) {
        this.activeBindings[ this.activeBindings.length ] = binding;
    } else {
        this.bindings[ this.bindings.length ] = binding;
    }
};

/**
 * Bindings.prototype.addAll( bindingArray )
 * Adds multiple bindings to this set of bindings. The arguments to
 * this method are arrays of binding arguments as they would appear
 * when calling <tt>Bindings.add()</tt>. For example:
 * <pre>
 * bindings.bindAll(
 *     [ 'determination', detmCtrl, [0] ],
 *     [ 'special-effect', effectField, [1] ],
 *     [ 'hint', hintField, [0,1] ]
 * );
 * </pre>
 *
 * bindingArray : an array of arrays of binding arguments
 */
Bindings.prototype.addAll = function addAll() {
    for( let i=0; i<arguments.length; ++i ) {
        this.add.apply( arguments[i] );
    }
};

/**
 * Bindings.prototype.createUpdateFunction()
 * Returns a function that will call <tt>update()</tt> for all of the
 * bindings in this set.
 *
 * returns an update function for these bindings
 */
Bindings.prototype.createUpdateFunction = function createUpdateFunction() {
    var bindings = this.bindings;
    return function updateFunction() {
        try {
            var changed = false;
            for( let i=0; i<bindings.length; ++i ) {
                changed = changed || bindings[i].update();
            }
            return changed;
        } catch( ex ) {
            return Error.handleUncaught( ex );
        }
    };
};

/**
 * Bindings.prototype.createPopulateFunction()
 * Returns a function that will call <tt>initComponent()</tt> for all of the
 * bindings in this set.
 *
 * returns a field populator function for these bindings
 */
Bindings.prototype.createPopulateFunction = function createPopulateFunction() {
    var bindings = this.bindings;
    var activeBindings = this.activeBindings;
    return function populateFunction() {
        try {
            for( let i=0; i<bindings.length; ++i ) {
                bindings[i].initComponent();
            }
            for( let i=0; i<activeBindings.length; ++i ) {
                activeBindings[i].initComponent();
            }
        } catch( ex ) {
            Error.handleUncaught( ex );
        }
    };
};

/**
 * Bindings.prototype.bind()
 *
 * Creates and install listeners on the editor associated with this
 * <tt>Bindings</tt> instance that will bind the editor controls
 * with the component settings.
 */
Bindings.prototype.bind = function bind() {
    if( this.alreadyBound ) {
        throw new Error( 'Bindings instance has already been bound' );
    }
    this.alreadyBound = true;
    this.editor.addFieldPopulationListener( this.createPopulateFunction() );
    this.editor.addHeartbeatListener( this.createUpdateFunction() );
};

/**
 * Bindings.prototype.toString()
 *
 * Returns a string representation of all of the bindings associated with this
 * <tt>Bindings</tt> instance.
 */
Bindings.prototype.toString = function toString() {
    var s = '';
    var bindings = this.bindings;
    for( let j=0; j<2; ++j ) {
        for( let i=0; i<bindings.length; ++i ) {
            var b = bindings[i];
            if( b === undefined || b == null )
                s += sprintf( '%03.0f  [null]\n', i );
            else
                s += sprintf( '%03.0f  %s\n', i, b.toString() );
        }
        bindings = this.activeBindings;
    }
    return s;
};



/**
 * Bindings.getBindingClass( componentClass ) [static]
 * Returns the default binding class for a component of type
 * <tt>componentClass</tt>. If there is no default binding class for
 * the component class, this method returns <tt>null</tt>.
 *
 * If <tt>componentClass</tt> itself does not have a registered
 * binding class, then this method will search up the chain of
 * superclasses of the component's class and return the first
 * registered binding class it finds.
 */
Bindings.getBindingClass = function getBindingClass( componentClass ) {
	if( componentClass == null ) return null;

	var type = Bindings._bindMap[ componentClass ];

	if( !type )
		return Bindings.getBindingClass( componentClass.getSuperclass() );
	return type;
};

/**
 * Bindings.registerBindingClass( componentClass, bindingClass ) [static]
 * Registers a subclass of <tt>Binding</tt> as the default binding class for
 * components of type <tt>componentClass</tt>, which may either be a Java
 * <tt>Class</tt> object or a string that names a Java class.
 *
 * componentClass : a type of component that will be bound using <tt>bindingClass</tt>
 * bindingClass : a constructor for a binding class that can convert between setting strings and component state
 */
Bindings.registerBindingClass = function registerBindingClass( componentClass, bindingClass ) {
	if( !(componentClass instanceof java.lang.Class) )
		componentClass = java.lang.Class.forName( componentClass );
	Bindings._bindMap[ componentClass ] = bindingClass;
};

Bindings._bindMap = [];










/**
 * CheckBoxBinding( name, uiControl, gameComponent, sheetsToUpdate ) : ActiveBinding [ctor]
 * Binds <tt>swing.JCheckBox</tt> to a yes/no setting that can be fetched with
 * <tt>gameComponent.settings.getYesNo( name )</tt>.
 */
function CheckBoxBinding( name, uiControl, gameComponent, sheetsToUpdate ) {
	Binding.call( this, name, uiControl, gameComponent, sheetsToUpdate );
}

CheckBoxBinding.subclass( ActiveBinding );

CheckBoxBinding.prototype.controlToSetting = function controlToSetting( control ) {
	if( control.selected )
		return '1';
	else
		return '0';
};

CheckBoxBinding.prototype.settingToControl = function settingToControl( value, control ) {
	control.selected = Settings.yesNo( value );
};




/**
 * ListBinding( name, uiControl, gameComponent, sheetsToUpdate ) : ActiveBinding [ctor]
 * Binds <tt>swing.JList</tt>s.
 */
function ListBinding( name, uiControl, gameComponent, sheetsToUpdate ) {
	ActiveBinding.call( this, name, uiControl, gameComponent, sheetsToUpdate );
}

ListBinding.subclass( ActiveBinding );

ListBinding.prototype.installActivationHandler = function installActivationHandler() {
    var thiz = this;
    this.control.addListSelectionListener( function listSelectionListener() {
        thiz.update();
    });
};

ListBinding.prototype.controlToSetting = function controlToSetting( control ) {
	var sel = control.selectedValue;
	// ignore if nothing is selected
	if( sel == null ) return null;
	return sel.toString();
};

ListBinding.prototype.settingToControl = function settingToControl( value, control ) {
	var model = control.model;
	var len = model.size();
	for( let i=0; i<len; ++i ) {
		var itemValue = model.getElementAt(i).toString();

		if( itemValue.equals( value ) ) {
			control.selectedIndex = i;
			return;
		}
	}
};





/**
 * ComboBoxBinding( name, uiControl, gameComponent, sheetsToUpdate ) : ActiveBinding [ctor]
 * Binds <tt>swing.JComboBox</tt>es using the <tt>toString()</tt> value of the
 * options in the combo box.
 */
function ComboBoxBinding( name, uiControl, gameComponent, sheetsToUpdate ) {
	ActiveBinding.call( this, name, uiControl, gameComponent, sheetsToUpdate );
}

ComboBoxBinding.subclass( ActiveBinding );


ComboBoxBinding.prototype.controlToSetting = function controlToSetting( control ) {
	var sel = control.selectedItem;
	// ignore if nothing is selected; this can happen while initializing
	// the content of the control
	if( sel == null ) return null;
	return sel.toString();
};

ComboBoxBinding.prototype.settingToControl = function settingToControl( value, control ) {
    if( control.isEditable() ) {
        control.setSelectedItem( value );
    } else {
        var model = control.model;
        var len = model.size;
        for( let i=0; i<len; ++i ) {
            var item = model.getElementAt(i);
            var itemValue = item.toString();
            if( itemValue.equals( value ) ) {
                control.selectedIndex = i;
                return;
            }
        }
    }
};

/**
 * IndexedComboBoxBinding( name, uiControl, gameComponent, sheetsToUpdate ) : ActiveBinding [ctor]
 * Binds <tt>swing.JComboBox</tt>es using the index of the selected item.
 * This type of binding must be explicitly requested by passing this as the
 * optional binding class to <tt>Bindings.add</tt>.
 *
 * <b>Note:</b> This cannot be used with editable combo boxes.
 */
function IndexedComboBoxBinding( name, uiControl, gameComponent, sheetsToUpdate ) {
	ActiveBinding.call( this, name, uiControl, gameComponent, sheetsToUpdate );
}

IndexedComboBoxBinding.subclass( ActiveBinding );

IndexedComboBoxBinding.prototype.controlToSetting = function controlToSetting( control ) {
	var sel = control.selectedIndex;
	if( sel < 0 ) return null;
	return java.lang.Integer.toString( sel );
};

IndexedComboBoxBinding.prototype.settingToControl = function settingToControl( value, control ) {
    if( control.isEditable() ) {
        throw new Error( 'IndexedComboBoxBinding cannot be used with editable combo boxes' );
    } else {
		control.selectedIndex = java.lang.Integer.valueOf( value );
    }
};


/**
 * SpinnerBinding( name, uiControl, gameComponent, sheetsToUpdate ) : ActiveBinding [ctor]
 * Binds <tt>swing.JSpinner</tt>s that use integer models (including those made
 * with the <tt>spinner()</tt> function in the <tt>uicontrols</tt> library)
 * to an integer digit string that can be fetched with
 * <tt>gameComponent.settings.getInt( name )</tt>.
 */
function SpinnerBinding( name, uiControl, gameComponent, sheetsToUpdate ) {
	ActiveBinding.call( this, name, uiControl, gameComponent, sheetsToUpdate );
}

SpinnerBinding.subclass( ActiveBinding );

SpinnerBinding.prototype.installActivationHandler = function installActivationHandler() {
    var thiz = this;
    this.control.addChangeListener( function changeListener() {
        thiz.update();
    });
};

SpinnerBinding.prototype.controlToSetting = function controlToSetting( control ) {
    return java.lang.Integer.toString( control.value.intValue() );
};

SpinnerBinding.prototype.settingToControl = function settingToControl( value, control ) {
    control.value = java.lang.Integer.valueOf( value );
};



/**
 * SliderBinding( name, uiControl, gameComponent, sheetsToUpdate ) : SpinnerBinding [ctor]
 * Binds <tt>swing.JSlider</tt>s
 * to an integer digit string that can be fetched with
 * <tt>gameComponent.settings.getInt( name )</tt>.
 */
function SliderBinding( name, uiControl, gameComponent, sheetsToUpdate ) {
	ActiveBinding.call( this, name, uiControl, gameComponent, sheetsToUpdate );
}

SliderBinding.subclass( SpinnerBinding );

SliderBinding.prototype.controlToSetting = function controlToSetting( control ) {
    return java.lang.Integer.toString( control.value );
};



/**
 * HSBPanelBinding( name, uiControl, gameComponent, sheetsToUpdate ) : ActiveBinding [ctor]
 * Binds <tt>arkham.HSBPanel</tt>s to a comma-separated list of hue, saturation, and
 * brightness values. Hue is represented as a relative angle (in degrees); saturation
 * and brightness and represented as numbers between 0 and 1 (inclusive).
 * Tints can be fetched as an array of three <tt>float</tt> values by calling
 * <tt>Settings.tint( gameComponent.settings.get( name ) )</tt>.
 * The returned array represents the tint in the same format as that used by
 * a tint filters, which expresses the hue as an angle between 0 and 1.
 */
function HSBPanelBinding( name, uiControl, gameComponent, sheetsToUpdate ) {
	ActiveBinding.call( this, name, uiControl, gameComponent, sheetsToUpdate );
	var thiz = this;
	this.helper = arkham.plugins.UILibraryHelper.TintableBinding(
		function callAfterWrite() {
			try {
				thiz.update();
			} catch( ex ) {
				Error.handleUncaught( ex );
			}
		}
	);
	this.settingToControl( gameComponent.settings.get( name ), uiControl );
	uiControl.setTintable( this.helper, false );
}

HSBPanelBinding.subclass( ActiveBinding );

HSBPanelBinding.prototype.installActivationHandler = function installActivationHandler() {
};

HSBPanelBinding.prototype.controlToSetting = function controlToSetting( control ) {
	return control.toString();
};

HSBPanelBinding.prototype.settingToControl = function settingToControl( value, control ) {
	control.setHSB( Settings.tint( value ) );
};


/**
 * ButtonGroupBinding( name, uiControl, gameComponent, sheetsToUpdate ) : ActiveBinding [ctor]
 * Binds a button group to a set of user-defined values. The button group
 * must be a special subclass of <tt>swing.ButtonGroup</tt>, which can be
 * created using the <tt>buttonGroup</tt> function in the <tt>uicontrols</tt> library.
 * When the button group is created, each button in the group is associated with
 * a specific setting value. This binding maps between those values and the
 * selection state of buttons in the group.
 */
function ButtonGroupBinding( name, uiControl, gameComponent, sheetsToUpdate ) {
	ActiveBinding.call( this, name, uiControl, gameComponent, sheetsToUpdate );
}

ButtonGroupBinding.subclass( ActiveBinding );

ButtonGroupBinding.prototype.installActivationHandler = function installActivationHandler() {
    var thiz = this;
    this.control.addActionListener( function actionListener() {
        thiz.update();
    });
};

ButtonGroupBinding.prototype.controlToSetting = function controlToSetting( control ) {
	return control.toSetting();
};

ButtonGroupBinding.prototype.settingToControl = function settingToControl( value, control ) {
	control.fromSetting( value );
};


// set up the default bindings for standard Swing/Strange Eons controls
Bindings.registerBindingClass( 'javax.swing.text.JTextComponent', Binding );
Bindings.registerBindingClass( 'ca.cgjennings.apps.arkham.plugins.UILibraryHelper$CodeArea', Binding );
Bindings.registerBindingClass( 'javax.swing.JCheckBox', CheckBoxBinding );
Bindings.registerBindingClass( 'javax.swing.JToggleButton', CheckBoxBinding );
// since the cycle button is setting-backed, we just need a binding that will
// attach an ActionListener to the button; the SettingBackedControl interface
// will be given preference over the binding class for mapping settings <-> values
Bindings.registerBindingClass( 'ca.cgjennings.ui.JCycleButton', CheckBoxBinding );
Bindings.registerBindingClass( 'javax.swing.JList', ListBinding );
Bindings.registerBindingClass( 'javax.swing.JComboBox', ComboBoxBinding );
Bindings.registerBindingClass( 'javax.swing.JSpinner', SpinnerBinding );
Bindings.registerBindingClass( 'javax.swing.JSlider', SliderBinding );
Bindings.registerBindingClass( 'ca.cgjennings.apps.arkham.HSBPanel', HSBPanelBinding );
Bindings.registerBindingClass( 'ca.cgjennings.apps.arkham.plugins.UILibraryHelper$BindableGroup', ButtonGroupBinding );
