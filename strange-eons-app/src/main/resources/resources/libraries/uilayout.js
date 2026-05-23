/*
 uilayout.js - version 13
 Simple panel layout.
 */

function AbstractContainer(controls) {
    this.controls = [];
    this.hints = [];

    // if true, _controlToComponent will set default column and row sizes on
    // JTextFields and JTextAreas if their columns/rows are set to 0; set to
    // true in simple containers like Row and Stack
    this._growTextControls = false;

    if (controls && controls.length > 0) {
        for (let i = 0; i < controls.length; ++i) {
            this.add(controls[i]);
        }
    }

    var realized = false;

    this._checkRealized = function () {
        useLibrary.__threadassert();
        if (realized)
            throw new Error("this container has already been realized");
        realized = true;
    };

    this.__defineGetter__("realized", function () {
        return realized;
    });


    this.title = undefined;
    this.editorTabWrapping = true;
    this.editorTabScrolling = false;


    // Backwards compatibility
    this.isRealized = function isRealized() {
        return realized;
    };
    this.setTitle = function setTitle(text) {
        this.title = text;
    };
    this.getTitle = function getTitle() {
        return this.title;
    };
}

AbstractContainer.prototype._controlsToArray = function _controlsToArray(source) {
    source = source ? source : this.controls;
    var content = java.lang.reflect.Array.newInstance(java.lang.Object, source.length);
    for (let i = 0; i < source.length; ++i) {
        content[i] = this._controlToComponent(source[i]);
    }
    return content;
};

AbstractContainer.prototype._controlToComponent = function _controlToComponent(control) {
    if (control.realize != undefined) {
        control = control.realize();
    } else if (control instanceof java.awt.Component) {
        // do not convert
        if (this._growTextControls) {
            if (control instanceof swing.JTextField || control instanceof swing.JTextArea) {
                control.columns = control.text.length() < 16 ? 16 : control.text.length();
            }
            if (control instanceof swing.JTextArea) {
                control.rows = control.rows == 0 ? 5 : control.rows;
            }
        }
    } else if (control instanceof java.awt.Image) {
        control = new swing.JLabel(new java.awt.ImageIcon(control));
    } else {
        control = new swing.JLabel(control);
    }
    return control;
};

AbstractContainer.prototype._applyTitle = function _applyTitle(panel) {
    var title = this.title;
    if (title !== undefined) {
        if (title !== null)
            title = title.toString();
        var titleBorder = new swing.border.TitledBorder(null, title);
        if (panel.border == null) {
            panel.border = titleBorder;
        } else {
            panel.border = new swing.border.CompoundBorder(titleBorder, panel.border);
        }
    }
    return panel;
};

AbstractContainer.prototype.realize = Function.abstractMethod;

AbstractContainer.prototype.add = function add() {
    for (let i = 0; i < arguments.length; ++i) {
        if (arguments[i] == null)
            throw new Error("missing control at index " + i);
        this.controls[ this.controls.length ] = arguments[i];
        this.hints[ this.hints.length ] = null;
    }
    return this;
};

AbstractContainer.prototype.place = function place() {
    if ((arguments.length % 2) != 0) {
        throw new Error("arguments to place must be pairs: control1, hint1, control2, hint2, ...");
    }
    for (let i = 0; i < arguments.length; i += 2) {
        if (arguments[i] == null)
            throw new Error("missing control at index " + i);
        this.controls[ this.controls.length ] = arguments[i];
        this.hints[ this.hints.length ] = arguments[i + 1];
    }
    return this;
};

AbstractContainer.prototype.getControl = function getControl(index) {
    if (index < 0 || index >= controls.length) {
        throw new Error("invalid control index: " + index);
    }
    return controls[ index ];
};

AbstractContainer.prototype.getHint = function getHint(index) {
    if (index < 0 || index >= controls.length) {
        throw new Error("invalid control index: " + index);
    }
    return hints[ index ];
};

AbstractContainer.prototype.getControlCount = function getControlCount() {
    return controls.length;
};

AbstractContainer.prototype.addToEditor = function addToEditor(editor, title, heartbeatListener, fieldPopulationListener, tabIndex) {
    if (!editor)
        throw new Error("missing editor");
    title = title ? title : string("plug-user");
    var tabPane = AbstractContainer.findEditorTabPane(editor);
    if (tabPane != null) {
        var panel = this.realize();
        if (panel.border == null) {
            panel.border = swing.BorderFactory.createEmptyBorder(8, 8, 8, 8);
        } else {
            panel.border = new swing.border.CompoundBorder(
                    swing.BorderFactory.createEmptyBorder(8, 8, 8, 8),
                    panel.border
                    );
        }

        var panelShell;
        if (this.editorTabWrapping) {
            panelShell = new swing.JPanel(new java.awt.BorderLayout());
            panelShell.add(panel, java.awt.BorderLayout.NORTH);
        } else {
            panelShell = panel;
        }

        if (this.editorTabScrolling) {
            panelShell = new swing.JScrollPane(panelShell);
            panelShell.border = swing.BorderFactory.createEmptyBorder();
            panelShell.horizontalScrollBarPolicy = swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER;
            panelShell.verticalScrollBarPolicy = swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED;
            panelShell.verticalScrollBar.unitIncrement = 16;
        }

        panelShell.putClientProperty("se-custom-tab", title);
        panelShell.setName(title);

        tabIndex = tabIndex != null ? tabIndex : tabPane.tabCount - 1;
        tabPane.add(panelShell, tabIndex);

        // the editor is not showing, reset to the new preferred splitter size
        if (!editor.showing) {
            var splitter = tabPane.parent;
            if (splitter instanceof swing.JSplitPane) {
                splitter["setDividerLocation(int)"](-1);
            }
        }

        if (heartbeatListener) {
            editor.addHeartbeatListener(heartbeatListener);
        }
        if (fieldPopulationListener) {
            editor.addFieldPopulationListener(fieldPopulationListener);
        }

        return panel;
    }
    throw new Error("not a compatible game component type");
};

AbstractContainer.findEditorTabPane = function findEditorTabPane(editor) {
    if (!editor)
        return null;

    var tabPane = null;
    var content = editor.getContentPane();
    var splitter = content.getComponent(0);

    // case book editor
    if (splitter instanceof swing.JTabbedPane) {
        return splitter;
    }

    // most editors, including all card types
    else if (splitter instanceof swing.JSplitPane) {
        tabPane = splitter.leftComponent;

        // card editor with consequences (e.g., investigator and monster)
        if (tabPane instanceof swing.JSplitPane) {
            tabPane = tabPane.topComponent;
        }

        // deck editor
        else if (tabPane instanceof swing.JPanel) {
            tabPane = tabPane.getComponent(0);
        }

        // verify that we found a tab pane
        if (tabPane instanceof swing.JTabbedPane) {
            return tabPane;
        }
    }
    return null;
};

AbstractContainer.prototype.createDialog = function createDialog(title, okText, cancelText, icon, modal) {
    var content = this.realize();

    if (icon === undefined) {
        icon = arkham.plugins.UILibraryDialogTemplate.defaultIcon;
    }

    if (okText === undefined)
        okText = null;
    if (cancelText === undefined)
        cancelText = null;

    if (modal == null || modal === undefined) {
        modal = true;
    }

    if (title == null || title === undefined) {
        title = string("plug-user");
    }

    var d = arkham.plugins.UILibraryDialogTemplate(
            Eons.window, modal, content, icon, okText, cancelText
            );

    d.title = title;

    return d;
};

AbstractContainer.prototype.test = function test() {
    this.createDialog(null, "", null, null).showDialog();
};



function Row() {
    AbstractContainer.call(this, arguments);
    this.gap = 8;
    this._growTextControls = true;

    var alignment = -1;

    this.setAlignment = function setAlignment(align) {
        if (alignment < -1 || alignment > 1)
            throw new Error("invalid alignment: " + alignment);
        alignment = align;
    };

    this.getAlignment = function getAlignment() {
        return alignment;
    };
}

Row.subclass(AbstractContainer);

Row.prototype.realize = function realize() {
    this._checkRealized();

    var content = new swing.JPanel();
    var alignment = java.awt.FlowLayout.CENTER;
    if (this.getAlignment() < 0) {
        alignment = java.awt.FlowLayout.LEADING;
    } else if (this.getAlignment() > 0) {
        alignment = java.awt.FlowLayout.TRAILING;
    }
    var layout = new java.awt.FlowLayout(alignment, 0, 0);
    layout.alignOnBaseline = true;
    content.setLayout(layout);

    for (let i = 0; i < this.controls.length; ++i) {
        var hint = this.hints[i];
        if (hint == null)
            hint = i > 0 ? 1 : 0;
        if (hint > 0) {
            var size = this.gap * hint;
            var spacer = new swing.JLabel();
            spacer.setBorder(swing.BorderFactory.createEmptyBorder(0, size, 0, 0));
            content.add(spacer);
        }
        content.add(this._controlToComponent(this.controls[i]));
    }

    return this._applyTitle(content);
};



function Stack() {
    AbstractContainer.call(this, arguments);
    this._growTextControls = true;
}

Stack.subclass(AbstractContainer);

Stack.prototype.realize = function realize() {
    this._checkRealized();
    var array = this._controlsToArray();
    var panel = arkham.plugins.UILibraryHelper.createStack(array);
    return this._applyTitle(panel);
};






function FixedGrid(columns) {
    AbstractContainer.call(this);
    this._growTextControls = true;

    if (!columns)
        columns = 2;

    this.getColumnCount = function getColumnCount() {
        return columns;
    };

    // backward compatibilty
    this.getColumns = this.getColumnCount();

    if (arguments.length > 1) {
        for (let i = 1; i < arguments.length; ++i) {
            this.add(arguments[i]);
        }
    }
}

FixedGrid.subclass(AbstractContainer);

FixedGrid.prototype.realize = function realize() {
    this._checkRealized();

    var cols = this.getColumnCount();
    var panel = new swing.JPanel(new java.awt.GridBagLayout());
    var cons = new java.awt.GridBagConstraints();

    var insets = new java.awt.Insets(0, 5, 3, 0);
    var endInsets = new java.awt.Insets(0, 5, 3, 5);

    cons.gridx = cons.gridy = 0;
    cons.gridwidth = 1;
    cons.gridheight = 1;
    cons.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
    cons.insets = insets;

    for (let i = 0; i < this.controls.length; ++i) {
        var wrap = this.hints[i] && this.hints[i].equals("wrap");
        var lineEnd = wrap || cons.gridx == cols - 1;

        if (lineEnd) {
            cons.insets = endInsets;
            cons.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        } else {
            cons.insets = insets;
            cons.gridwidth = 1;
        }

        panel.add(this._controlToComponent(this.controls[i]), cons);

        if (lineEnd) {
            cons.gridx = 0;
            ++cons.gridy;
        } else {
            ++cons.gridx;
        }
    }

    var outerPanel = new swing.JPanel(new java.awt.BorderLayout());
    outerPanel.add(panel, java.awt.BorderLayout.LINE_START);

    return this._applyTitle(outerPanel);
};



function Grid(layoutConstraints, columnConstraints, rowConstraints) {
    if (!layoutConstraints)
        layoutConstraints = "";
    if (!columnConstraints)
        columnConstraints = "";
    if (!rowConstraints)
        rowConstraints = "";

    var layout = new Packages.net.miginfocom.swing.MigLayout(
            layoutConstraints, columnConstraints, rowConstraints
            );

    this._getLayout = function () {
        return layout;
    };

    AbstractContainer.call(this);
}

Grid.subclass(AbstractContainer);

Grid.prototype.realize = function realize() {
    this._checkRealized();
    var content = new swing.JPanel(this._getLayout());
    for (let i = 0; i < this.controls.length; ++i) {
        content["add(java.awt.Component,java.lang.Object)"](
                this._controlToComponent(this.controls[i]),
                this.hints[i] ? this.hints[i].toString() : null
                );
    }
    return this._applyTitle(content);
};




function TypeGrid(hgap, vgap) {
    this.hgap = hgap;
    this.vgap = vgap;

    AbstractContainer.call(this);
}

TypeGrid.subclass(AbstractContainer);

TypeGrid.prototype.realize = function realize() {
    this._checkRealized();
    var layout = new Packages.se.datadosen.component.RiverLayout();
    if (!(this.hgap === undefined)) {
        layout.hgap = this.hgap;
    }
    if (!(this.vgap === undefined)) {
        layout.vgap = this.vgap;
    }
    var content = new swing.JPanel(layout);

    for (let i = 0; i < this.controls.length; ++i) {
        content["add(java.awt.Component,java.lang.Object)"](
                this._controlToComponent(this.controls[i]),
                this.hints[i] ? this.hints[i].toString().replaceAll(",", " ") : null
                );
    }
    return this._applyTitle(content);
};


function TabPane() {
    AbstractContainer.call(this, arguments);
    /**
     * TabPane.smallTabs
     * If set to <tt>true</tt>, then the tabbed pane will feature smaller tabs.
     */
    this.smallTabs = false;
}

TabPane.subclass(AbstractContainer);

TabPane.prototype.realize = function realize() {
    this._checkRealized();
    var hint;
    var tabPane = new swing.JTabbedPane();
    for (let i = 0; i < this.controls.length; ++i) {
        hint = this.hints[i];
        if (hint == null)
            hint = 'Tab ' + (i + 1);
        if (hint instanceof swing.JComponent) {
            tabPane.addTab(null, this._controlToComponent(this.controls[i]));
            tabPane.setTabComponentAt(i, hint);
        } else {
            tabPane.addTab(hint.toString(), this._controlToComponent(this.controls[i]));
        }
    }

    if (this.smallTabs) {
        var font = tabPane.font;
        tabPane.font = font['deriveFont(float)'](font.size2D - 2);
    }

    return this._applyTitle(tabPane);
};


function Splitter(verticalSplit, left, right) {
    AbstractContainer.call(this);
    this.verticalSplit = verticalSplit;
    if (left)
        this.add(left);
    if (right)
        this.add(right);
}

Splitter.subclass(AbstractContainer);

Splitter.prototype.realize = function realize() {
    this._checkRealized();

    var len = this.controls.length;
    var left = len < 1 ? "" : this.controls[0];
    var right = len < 2 ? "" : this.controls[1];
    left = this._controlToComponent(left);
    right = this._controlToComponent(right);

    var content = new swing.JSplitPane(
            this.verticalSplit ? swing.JSplitPane.HORIZONTAL_SPLIT : swing.JSplitPane.VERTICAL_SPLIT,
            true, left, right
            );
    content.setOneTouchExpandable(true);
    content.setDividerSize(8);
    content['setDividerLocation(int)'](-1);
    return this._applyTitle(content);
};
