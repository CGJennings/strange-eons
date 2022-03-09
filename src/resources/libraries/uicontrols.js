/*
 uicontrols.js - version 17
 Helper functions for UI controls.
 */

function textField(text, columns, listener, spellingChecked) {
    useLibrary.__threadassert();
    text = text ? text : "";
    if (columns < 0 || columns === undefined)
        columns = 0;
    if (spellingChecked === undefined)
        spellingChecked = true;
    var field = spellingChecked ?
            new ca.cgjennings.spelling.ui.JSpellingTextField(text, columns)
            : new swing.JTextField(text, columns);
    if (listener) {
        field.addActionListener(listener);
    }
    field.font = resources.ResourceKit.getEditorFont();
    field.select(0, 0);
    return field;
}

function textArea(text, rows, columns, scroll, spellingChecked) {
    useLibrary.__threadassert();
    text = text ? text : "";
    if (rows === undefined || rows < 0)
        rows = 0;
    if (columns === undefined || columns < 0)
        columns = 0;
    if (spellingChecked === undefined)
        spellingChecked = true;
    var field = spellingChecked ?
            new ca.cgjennings.spelling.ui.JSpellingTextArea(text, rows, columns)
            : new swing.JTextArea(text, rows, columns);
    field.font = resources.ResourceKit.getEditorFont();
    field.select(0, 0);
    field.lineWrap = true;
    field.wrapStyleWord = true;
    field.tabSize = 4;
    if (scroll) {
        field = new swing.JScrollPane(field);
    }
    return field;
}

function codeArea(text, preferredWidth, preferredHeight) {
    useLibrary.__threadassert();
    text = text ? text : "";
    var field = arkham.plugins.UILibraryHelper.createCodeArea();
    field.setInitialText(text);
    if (!preferredWidth || preferredWidth < 1)
        preferredWidth = 400;
    if (!preferredHeight || preferredHeight < 1)
        preferredHeight = 300;
    field.setPreferredSize(new java.awt.Dimension(preferredWidth, preferredHeight));
    return field;
}

function button(label, icon, listener) {
    useLibrary.__threadassert();
    if (label === undefined)
        label = null;
    icon = icon ? icon : null;
    var button;
    if (label) {
        button = new swing.JButton(label, icon);
    } else {
        button = new swing.JButton["(javax.swing.Icon)"](icon);
    }
    if (listener) {
        button.addActionListener(listener);
    }
    return button;
}

function repeaterButton(label, icon, listener) {
    useLibrary.__threadassert();
    if (label === undefined)
        label = null;
    icon = icon ? icon : null;
    var button;
    if (label) {
        button = new ca.cgjennings.ui.JRepeaterButton(label, icon);
    } else {
        button = new ca.cgjennings.ui.JRepeaterButton["(javax.swing.Icon)"](icon);
    }
    if (listener) {
        button.addActionListener(listener);
    }
    return button;
}

function toggleButton(label, icon, selected, listener) {
    useLibrary.__threadassert();
    if (label === undefined)
        label = null;
    icon = icon ? icon : null;
    var button;
    if (label) {
        button = new swing.JToggleButton(label, icon);
    } else {
        button = new swing.JToggleButton(icon);
    }
    button.selected = selected ? true : false;
    if (listener) {
        button.addActionListener(listener);
    }
    return button;
}

function radioButton(label, selected, listener) {
    useLibrary.__threadassert();
    if (label === undefined)
        label = null;
    var button;
    if (label) {
        button = new swing.JRadioButton(label);
    } else {
        button = new swing.JRadioButton();
    }
    button.selected = selected ? true : false;
    if (listener) {
        button.addActionListener(listener);
    }
    return button;
}

function buttonGroup(buttons, settingValues) {
    useLibrary.__threadassert();
    var bg;
    if (settingValues === undefined) {
        bg = new swing.ButtonGroup();
        for (let i = 0; i < buttons.length; ++i) {
            bg.add(buttons[i]);
        }
    } else {
        bg = new arkham.plugins.UILibraryHelper.BindableGroup(buttons, settingValues);
    }
    return bg;
}

function cycleButton(labels, settingValues) {
    useLibrary.__threadassert();
    if (settingValues == null) {
        return new ca.cgjennings.ui.JCycleButton(labels);
    } else {
        return new ca.cgjennings.ui.JCycleButton(labels, settingValues);
    }
}

function checkBox(label, selected, listener) {
    useLibrary.__threadassert();
    if (label === undefined)
        label = null;
    var button = new swing.JCheckBox("" + label);
    if (listener) {
        button.addActionListener(listener);
    }
    button.selected = selected ? true : false;
    return button;
}

function comboBox(items, listener) {
    useLibrary.__threadassert();
    if (!items)
        items = [];
    var model = new swing.DefaultComboBoxModel();
    for (let i = 0; i < items.length; ++i) {
        model.addElement(items[i]);
    }
    var comboBox = new swing.JComboBox(model);
    comboBox.renderer = arkham.diy.ListItemRenderer.shared;
    comboBox.editable = false;
    comboBox.maximumRowCount = 12;
    if (listener) {
        comboBox.addActionListener(listener);
    }
    return comboBox;
}

function autocompletionField(items, sorted) {
    useLibrary.__threadassert();
    if (!items || items["length"] === undefined)
        throw new Error("missing array of autocompletion items");

    var set;
    if (sorted)
        set = new java.util.TreeSet(Language.getInterface().collator);
    else
        set = new java.util.LinkedHashSet();
    java.util.Collections.addAll(set, items);

    var field = new ca.cgjennings.spelling.ui.JSpellingComboBox(set.toArray());
    field.editable = true;
    field.font = resources.ResourceKit.getEditorFont();
    field.maximumRowCount = 12;

    var acdoc = ca.cgjennings.ui.AutocompletionDocument.install(field);
    if (sorted)
        acdoc.collator = Language.getInterface().collator;
    return field;
}

function listControl(items, listener, scroll) {
    useLibrary.__threadassert();
    if (!items)
        items = [];
    var model = new swing.DefaultListModel();
    for (let i = 0; i < items.length; ++i) {
        model.addElement(items[i]);
    }
    var list = new swing.JList(model);
    list.cellRenderer = arkham.diy.ListItemRenderer.shared;
    list.selectionMode = swing.ListSelectionModel.SINGLE_SELECTION;
    if (listener) {
        list.addListSelectionListener(listener);
    }
    if (scroll) {
        list = new swing.JScrollPane(list);
    }
    return list;
}

function label(text, labelFor) {
    var label = new swing.JLabel['(java.lang.String)'](text);
    if (labelFor) {
        label.setLabelFor(labelFor);
    }
    return label;
}

function noteLabel(text) {
    var note = label(text);
    var f = note.font;
    note.font = f.deriveFont(f.size2D - 1);
    return note;
}

function separator(vertical) {
    if (vertical) {
        return new swing.JSeparator(swing.JSeparator.VERTICAL);
    } else {
        return new swing.JSeparator();
    }
}

function hyperlink(text, url) {
    useLibrary.__threadassert();
    if (!text)
        throw new Error("missing URL");
    if (!url)
        url = text;
    var field = new ca.cgjennings.ui.JLinkLabel(text);
    field.setURI(new URI(url));
    return field;
}

function helpButton(url) {
    useLibrary.__threadassert();
    if (!url)
        throw new Error("missing URL");
    var b = new ca.cgjennings.ui.JHelpButton();
    if (url.indexOf(":") >= 0) {
        b.helpPage = url;
    } else {
        b.wikiPage = url;
    }
    return b;
}

function tipButton(text) {
    useLibrary.__threadassert();
    if (!text)
        throw new Error("missing tip text");
    if (text.indexOf("\n") >= 0) {
        text = "<html>" + text.replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;").replaceAll(">", "&gt;")
                .replaceAll("\n", "<br>");
    }
    var b = new ca.cgjennings.ui.JTip(text);
    return b;
}

function spinner(min, max, stepSize, initialValue, listener) {
    useLibrary.__threadassert();
    if (min === undefined)
        min = 1;
    if (max === undefined)
        max = 10;
    if (stepSize == 0 || stepSize == undefined)
        stepSize = 1;
    if (initialValue === undefined)
        initialValue = min;
    var control = new swing.JSpinner(
            new swing.SpinnerNumberModel["(int,int,int,int)"](initialValue, min, max, stepSize)
            );
    if (listener) {
        control.addChangeListener(listener);
    }
    return control;
}

function slider(min, max, initial, valueLabelPairs, listener) {
    useLibrary.__threadassert();
    if (min === undefined)
        min = 1;
    if (max === undefined)
        max = 10;
    if (initial === undefined)
        initial = min;

    min = (min === undefined) ? 1 : min;
    max = (max === undefined) ? 10 : max;
    if (max < min)
        max = min;
    initial = (initial === undefined) ? min : initial;

    var control = new swing.JSlider(min, max, initial);

    if (listener) {
        control.addChangeListener(listener);
    }

    if (valueLabelPairs) {
        var dict = new java.util.Hashtable();
        for (let i = 0; i < valueLabelPairs.length; i += 2) {
            if (!(valueLabelPairs[i + 1] instanceof java.awt.Component)) {
                valueLabelPairs[i + 1] = noteLabel(valueLabelPairs[i + 1].toString());
            }
            dict.put(
                    java.lang.Integer.valueOf(valueLabelPairs[i]),
                    valueLabelPairs[i + 1]
                    );
        }
        control.labelTable = dict;
        control.paintLabels = true;
    }

    return control;
}

function tintPanel() {
    useLibrary.__threadassert();
    var panel = new arkham.HSBPanel();
    panel.presetsVisible = false;
    return panel;
}

function portraitPanel(gc, portraitIndex, title) {
    useLibrary.__threadassert();
    var panel = new arkham.PortraitPanel();
    panel.portrait = gc.getPortrait(portraitIndex);
    if (title !== undefined) {
        panel.panelTitle = title;
    }
    return panel;
}

function noMarkup(c) {
    c.putClientProperty(arkham.MarkupTarget.FORCE_MARKUP_TARGET_PROPERTY, java.lang.Boolean.FALSE);
    c.putClientProperty(arkham.ContextBar.BAR_DISABLE_PROPERTY, java.lang.Boolean.TRUE);
    return c;
}
