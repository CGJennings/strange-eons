/*
  uibindings.js - version 15
  Create and manage bindings between controls and data.
*/

importClass( arkham.diy.SettingBackedControl );

function Binding( name, uiControl, gameComponent, sheetsToUpdate ) {
    if (!name) throw new Error('missing bind name');
    if (!uiControl) throw new Error('missing uiControl');
    if (!gameComponent) throw new Error('missing gameComponent');

    if (!(uiControl instanceof java.awt.Component) && !(uiControl instanceof swing.ButtonGroup))
        throw new Error('uiControl must be a Component or ButtonGroup');

    if (!(gameComponent instanceof arkham.component.GameComponent))
        throw new Error('gameComponent must be a GameComponent');

    if (name.startsWith('$')) {
        name = name.substring(1).replace('_', '-');
    }

    if (sheetsToUpdate === undefined) {
        this.sheetBits = -1;
    } else {
        this.sheetBits = 0;
        for (let i = 0; i < sheetsToUpdate.length; ++i) {
            if (sheetsToUpdate[i] > 31)
                throw new Error('illegal sheet number: ' + sheetsToUpdate[i]);
            if (gameComponent.sheets != null && sheetsToUpdate[i] >= gameComponent.sheets.length)
                throw new Error('sheet does not exist: ' + sheetsToUpdate[i]);
            this.sheetBits |= (1 << sheetsToUpdate[i]);
        }
    }

    this.isActive = false;
    this.name = name;
    this.settings = gameComponent.settings;
    this.component = gameComponent;
    this.control = uiControl;

    if (this.settings.get(name) != null) {
        this.initComponent();
    }
}

Binding.prototype.update = function update() {
    var oldValue = this.settings.get(this.name);
    var newValue;
    if (this.control instanceof SettingBackedControl) {
        newValue = this.control.toSetting();
    } else {
        newValue = this.controlToSetting(this.control);
    }
    if (!newValue.equals(oldValue)) {

        this.settings.set(this.name, newValue);
        var mask = this.sheetBits;
        var sheets = this.component.sheets;

        if (sheets) {
            for (let i = 0; i < sheets.length; ++i, mask >>= 1) {
                if ((mask & 1) === 1) {
                    this.component.markChanged(i);
                }
            }
        }
        return true;
    }
    return false;
};

Binding.prototype.initComponent = function initComponent() {
    var value = this.settings.get(this.name);
    if (this.control instanceof SettingBackedControl) {
        value = this.control.fromSetting(value);
    } else {
        this.settingToControl(value, this.control);
    }
};

Binding.prototype.controlToSetting = function controlToSetting( control ) {
    if( control['text'] === undefined )
        throw new Error( 'you must provide a binding class for this control: ' + this.name );
    return control.text;
};

Binding.prototype.settingToControl = function settingToControl( value, uiControl ) {
    if( !(uiControl['text'] === undefined) ) {
        uiControl.text = value;
        uiControl.select( 0, 0 );
    } else {
        throw new Error( 'you must provide a binding class for this control: ' + this.name );
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

function ActiveBinding( name, uiControl, gameComponent, sheetsToUpdate ) {
    Binding.call( this, name, uiControl, gameComponent, sheetsToUpdate );
    this.isActive = true;
    this.installActivationHandler();
}

ActiveBinding.subclass( Binding );

ActiveBinding.prototype.installActivationHandler = function installActivationHandler() {
    try {
        var thiz = this;
        this.control.addActionListener(function actionListener() {
            thiz.update();
        });
    } catch (ex) {
        throw new Error(
            'this control does not support an ActionListener: you must create ' +
            'a custom ActiveBinding subclass or use a Binding instead'
        );
    }
};




function Bindings( editor, gameComponent ) {
    if( !gameComponent ) gameComponent = editor.gameComponent;
    this.bindings = [];
    this.activeBindings = [];
    this.editor = editor;
    this.component = gameComponent;
    this.alreadyBound = false;
}

Bindings.prototype.add = function add( name, control, sheets, bindClass ) {
    if (control instanceof swing.JScrollPane) {
        control = control.viewport.view;
    }
    if (bindClass == null) {
        bindClass = Bindings.getBindingClass(control.getClass());
        if (bindClass == null) {
            throw new Error('no binding class is registered for ' + control.getClass());
        }
    }

    var binding = new bindClass(name, control, this.component, sheets);
    if (binding.isActive) {
        this.activeBindings[ this.activeBindings.length ] = binding;
    } else {
        this.bindings[ this.bindings.length ] = binding;
    }
};

Bindings.prototype.addAll = function addAll() {
    for( let i=0; i<arguments.length; ++i ) {
        this.add.apply( arguments[i] );
    }
};

Bindings.prototype.createUpdateFunction = function createUpdateFunction() {
    var bindings = this.bindings;
    return function updateFunction() {
        let changed = false;
        for( let i=0; i<bindings.length; ++i ) {
            try {
                changed = changed || bindings[i].update();
            } catch(ex) {
                Error.handleUncaught(ex);
            }
        }
        return changed;
    };
};

Bindings.prototype.createPopulateFunction = function createPopulateFunction() {
    var bindings = this.bindings;
    var activeBindings = this.activeBindings;
    return function populateFunction() {
        try {
            for (let i = 0; i < bindings.length; ++i) {
                bindings[i].initComponent();
            }
            for (let i = 0; i < activeBindings.length; ++i) {
                activeBindings[i].initComponent();
            }
        } catch (ex) {
            Error.handleUncaught(ex);
        }
    };
};

Bindings.prototype.bind = function bind() {
    if (this.alreadyBound) {
        throw new Error("already bound: " + this);
    }
    this.alreadyBound = true;
    this.editor.addFieldPopulationListener(this.createPopulateFunction());
    this.editor.addHeartbeatListener(this.createUpdateFunction());
};

Bindings.prototype.toString = function toString() {
    var s = '';
    var bindings = this.bindings;
    for (let j = 0; j < 2; ++j) {
        for (let i = 0; i < bindings.length; ++i) {
            var b = bindings[i];
            if (b === undefined || b == null)
                s += sprintf('%03.0f  [null]\n', i);
            else
                s += sprintf('%03.0f  %s\n', i, b.toString());
        }
        bindings = this.activeBindings;
    }
    return s;
};

Bindings.getBindingClass = function getBindingClass( componentClass ) {
    if (componentClass == null) return null;
    let type = Bindings._bindMap[ componentClass ];
    if (!type) return Bindings.getBindingClass(componentClass.getSuperclass());
    return type;
};

Bindings.registerBindingClass = function registerBindingClass( componentClass, bindingClass ) {
    if (!(componentClass instanceof java.lang.Class)) {
        componentClass = java.lang.Class.forName(String(componentClass));
    }
    Bindings._bindMap[ componentClass ] = bindingClass;
};

Bindings._bindMap = [];










function CheckBoxBinding(name, uiControl, gameComponent, sheetsToUpdate) {
    Binding.call(this, name, uiControl, gameComponent, sheetsToUpdate);
}

CheckBoxBinding.subclass(ActiveBinding);

CheckBoxBinding.prototype.controlToSetting = function controlToSetting(control) {
    if (control.selected)
        return '1';
    else
        return '0';
};

CheckBoxBinding.prototype.settingToControl = function settingToControl(value, control) {
    control.selected = Settings.yesNo(value);
};


function ListBinding(name, uiControl, gameComponent, sheetsToUpdate) {
    ActiveBinding.call(this, name, uiControl, gameComponent, sheetsToUpdate);
}

ListBinding.subclass(ActiveBinding);

ListBinding.prototype.installActivationHandler = function installActivationHandler() {
    var thiz = this;
    this.control.addListSelectionListener(function listSelectionListener() {
        thiz.update();
    });
};

ListBinding.prototype.controlToSetting = function controlToSetting(control) {
    var sel = control.selectedValue;
    // ignore if nothing is selected
    if (sel == null)
        return null;
    return sel.toString();
};

ListBinding.prototype.settingToControl = function settingToControl(value, control) {
    var model = control.model;
    var len = model.size();
    for (let i = 0; i < len; ++i) {
        var itemValue = model.getElementAt(i).toString();

        if (itemValue.equals(value)) {
            control.selectedIndex = i;
            return;
        }
    }
};


function ComboBoxBinding(name, uiControl, gameComponent, sheetsToUpdate) {
    ActiveBinding.call(this, name, uiControl, gameComponent, sheetsToUpdate);
}

ComboBoxBinding.subclass(ActiveBinding);


ComboBoxBinding.prototype.controlToSetting = function controlToSetting(control) {
    var sel = control.selectedItem;
    // ignore if nothing is selected; this can happen while initializing
    // the content of the control
    if (sel == null)
        return null;
    return sel.toString();
};

ComboBoxBinding.prototype.settingToControl = function settingToControl(value, control) {
    if (control.isEditable()) {
        control.setSelectedItem(value);
    } else {
        var model = control.model;
        var len = model.size;
        for (let i = 0; i < len; ++i) {
            var item = model.getElementAt(i);
            var itemValue = item.toString();
            if (itemValue.equals(value)) {
                control.selectedIndex = i;
                return;
            }
        }
    }
};

function IndexedComboBoxBinding(name, uiControl, gameComponent, sheetsToUpdate) {
    ActiveBinding.call(this, name, uiControl, gameComponent, sheetsToUpdate);
}

IndexedComboBoxBinding.subclass(ActiveBinding);

IndexedComboBoxBinding.prototype.controlToSetting = function controlToSetting(control) {
    var sel = control.selectedIndex;
    if (sel < 0) return null;
    return java.lang.Integer.toString(sel);
};

IndexedComboBoxBinding.prototype.settingToControl = function settingToControl(value, control) {
    if (control.isEditable()) {
        throw new Error('IndexedComboBoxBinding cannot be used with editable combo boxes');
    } else {
        control.selectedIndex = java.lang.Integer.valueOf(value);
    }
};


function SpinnerBinding(name, uiControl, gameComponent, sheetsToUpdate) {
    ActiveBinding.call(this, name, uiControl, gameComponent, sheetsToUpdate);
}

SpinnerBinding.subclass(ActiveBinding);

SpinnerBinding.prototype.installActivationHandler = function installActivationHandler() {
    var thiz = this;
    this.control.addChangeListener(function changeListener() {
        thiz.update();
    });
};

SpinnerBinding.prototype.controlToSetting = function controlToSetting(control) {
    return java.lang.Integer.toString(control.value.intValue());
};

SpinnerBinding.prototype.settingToControl = function settingToControl(value, control) {
    control.value = java.lang.Integer.valueOf(value);
};


function SliderBinding(name, uiControl, gameComponent, sheetsToUpdate) {
    ActiveBinding.call(this, name, uiControl, gameComponent, sheetsToUpdate);
}

SliderBinding.subclass(SpinnerBinding);

SliderBinding.prototype.controlToSetting = function controlToSetting(control) {
    return java.lang.Integer.toString(control.value);
};


function HSBPanelBinding(name, uiControl, gameComponent, sheetsToUpdate) {
    ActiveBinding.call(this, name, uiControl, gameComponent, sheetsToUpdate);
    var thiz = this;
    this.helper = arkham.plugins.UILibraryHelper.TintableBinding(
            function callAfterWrite() {
                try {
                    thiz.update();
                } catch (ex) {
                    Error.handleUncaught(ex);
                }
            }
    );
    this.settingToControl(gameComponent.settings.get(name), uiControl);
    uiControl.setTintable(this.helper, false);
}

HSBPanelBinding.subclass(ActiveBinding);

HSBPanelBinding.prototype.installActivationHandler = function installActivationHandler() {
};

HSBPanelBinding.prototype.controlToSetting = function controlToSetting(control) {
    return control.toString();
};

HSBPanelBinding.prototype.settingToControl = function settingToControl(value, control) {
    control.setHSB(Settings.tint(value));
};


function ButtonGroupBinding(name, uiControl, gameComponent, sheetsToUpdate) {
    ActiveBinding.call(this, name, uiControl, gameComponent, sheetsToUpdate);
}

ButtonGroupBinding.subclass(ActiveBinding);

ButtonGroupBinding.prototype.installActivationHandler = function installActivationHandler() {
    var thiz = this;
    this.control.addActionListener(function actionListener() {
        thiz.update();
    });
};

ButtonGroupBinding.prototype.controlToSetting = function controlToSetting(control) {
    return control.toSetting();
};

ButtonGroupBinding.prototype.settingToControl = function settingToControl(value, control) {
    control.fromSetting(value);
};


// set up the default bindings for standard Swing/Strange Eons controls
Bindings.registerBindingClass('javax.swing.text.JTextComponent', Binding);
Bindings.registerBindingClass('ca.cgjennings.apps.arkham.plugins.UILibraryHelper$CodeArea', Binding);
Bindings.registerBindingClass('javax.swing.JCheckBox', CheckBoxBinding);
Bindings.registerBindingClass('javax.swing.JToggleButton', CheckBoxBinding);
// since the cycle button is setting-backed, we just need a binding that will
// attach an ActionListener to the button; the SettingBackedControl interface
// will be given preference over the binding class for mapping settings <-> values
Bindings.registerBindingClass('ca.cgjennings.ui.JCycleButton', CheckBoxBinding);
Bindings.registerBindingClass('javax.swing.JList', ListBinding);
Bindings.registerBindingClass('javax.swing.JComboBox', ComboBoxBinding);
Bindings.registerBindingClass('javax.swing.JSpinner', SpinnerBinding);
Bindings.registerBindingClass('javax.swing.JSlider', SliderBinding);
Bindings.registerBindingClass('ca.cgjennings.apps.arkham.HSBPanel', HSBPanelBinding);
Bindings.registerBindingClass('ca.cgjennings.apps.arkham.plugins.UILibraryHelper$BindableGroup', ButtonGroupBinding);