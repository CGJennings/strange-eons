/*
 
 prefab.js - version 2
 Create diy components with a minimum of effort.
 
 */

useLibrary('diy');
useLibrary('ui');
useLibrary('markup');

var pfBaseKey = 'prefab';
var pfDIY;
var pfSettings;
var pfTitleBox;
var pfContentBox;

function pfInit(diy) {
    pfDIY = diy;
    pfSettings = diy.settings;
}

function pfKey(suffix) {
    return pfBaseKey + suffix;
}

function pfString(key, defaultValue) {
    key = pfKey(key);
    var value = pfSettings.get(key);
    if (value == null) {
        value = defaultValue === undefined ? '' : defaultValue;
    }
    if (value != null) {
        if (value.startsWith('#') || value.startsWith('@') || (value.startsWith('$') && !value.startsWith('$$'))) {
            value = global[ value ];
        }
    }
    return value;
}

function create(diy) {
    if (pfBaseKey == null)
        throw new Error('pfBaseKey not defined');

    pfInit(diy);
    if (global['beforeCreate'] !== undefined)
        global.beforeCreate(diy);

    // get front and back face keys; define front key if required
    var pfFrontKey = pfKey('-front');
    var pfBackKey = pfKey('-back');
    var pfFrontTemplate = pfFrontKey + '-template';
    var pfBackTemplate = pfBackKey + '-template';

    if (pfSettings.get(pfFrontTemplate) == null) {
        throw new Error('front template key not defined: ' + pfFrontKey);
    }

    // card type is determined by whether the back key is defined
    diy.frontTemplateKey = pfFrontKey;
    if (pfSettings.get(pfBackTemplate) == null) {
        diy.faceStyle = FaceStyle.SHARED_FACE;
    } else {
        diy.backTemplateKey = pfBackKey;
        if (pfSettings.getYesNo(pfKey('-portrait-on-back')) && pfSettings.get(pfKey('-portrait-template')) != null) {
            diy.faceStyle = FaceStyle.TWO_FACES;
        } else {
            diy.faceStyle = FaceStyle.PLAIN_BACK;
        }
    }

    diy.name = pfString('-name');
    $Content = pfString('-content');

    if (pfSettings.get(pfKey('-portrait-template')) != null) {
        diy.portraitKey = pfBaseKey;
    }

    if (global['afterCreate'] !== undefined)
        global.afterCreate(diy);
}

function createInterface(diy, editor) {
    if (global['beforeCreateInterface'] !== undefined)
        global.beforeCreateInterface(diy, editor);

    var bindings = new Bindings(editor, diy);
    var panel = new TypeGrid();

    var hasName = false;
    if (pfSettings.get(pfKey('-name-region')) != null) {
        hasName = true;
        var nameField = textField();
        var nameLabel = label(pfString('-name-label', '@prefab-l-name'));
        nameLabel.labelFor = nameField;
        diy.nameField = nameField;
        panel.place(nameLabel, '', nameField, 'hfill');
    }

    var hasContent = false;
    if (pfSettings.get(pfKey('-content-region')) != null) {
        hasContent = true;
        var contentField = textArea(null, 15, 0, true);
        var contentLabel = label(pfString('-content-label', '@prefab-l-content'));
        contentLabel.labelFor = contentField;
        bindings.add('Content', contentField, [0]);
        panel.place(contentLabel, hasName ? 'p' : '', contentField, 'br hfill');
    }

    panel.title = pfString('-panel-title', null);

    if (global['afterCreateInterface'] !== undefined)
        global.afterCreateInterface(diy, editor, panel, bindings);

    bindings.bind();
    if (hasName || hasContent || global['afterCreateInterface'] !== undefined) {
        panel.addToEditor(editor, pfString('-tab-label', '@prefab-l-tab'), null, null, 0);
    }
}

function createFrontPainter(diy, sheet) {
    if (global['beforeCreateFrontPainter'] !== undefined)
        global.beforeCreateFrontPainter(diy, sheet);

    var makeBox = function (key, isTitle) {
        var baseKey = pfKey(key);
        // don't create a box if the region isn't defined
        if (pfSettings.get(baseKey + '-region') == null) {
            return null;
        }

        var box = markupBox(sheet);
        if (pfSettings.get(baseKey + '-alignment') == null) {
            box.alignment = LAYOUT_CENTER | (isTitle ? LAYOUT_MIDDLE : LAYOUT_TOP);
        } else {
            pfSettings.getTextAlignment(baseKey, box);
        }
        if (pfSettings.get(baseKey + '-style') != null) {
            pfSettings.getTextStyle(baseKey, box.defaultStyle);
        }
        return box;
    };

    pfTitleBox = makeBox('-name', true);
    pfContentBox = makeBox('-content');

    if (global['afterCreateFrontPainter'] !== undefined)
        global.afterCreateFrontPainter(diy, sheet);
}

function createBackPainter(diy, sheet) {
    if (global['beforeCreateBackPainter'] !== undefined)
        global.beforeCreateBackPainter(diy, sheet);
    if (global['afterCreateBackPainter'] !== undefined)
        global.afterCreateBackPainter(diy, sheet);
}

function paintFront(g, diy, sheet) {
    if (global['beforePaintFront'] !== undefined)
        global.beforePaintFront(g, diy, sheet);

    g.setPaint(Colour.BLACK);

    var hasOverlay = false;
    if (pfSettings.get(pfKey('-portrait-template')) != null && !pfSettings.getYesNo(pfKey('-portrait-on-back'))) {
        if (pfSettings.get(pfKey('-portrait-overlay')) == null) {
            sheet.paintPortrait(g);
        } else {
            hasOverlay = true;
        }
    }
    sheet.paintTemplateImage(g);
    if (hasOverlay) {
        sheet.paintPortrait(g);
    }

    if (pfTitleBox != null) {
        pfTitleBox.markupText = diy.name;
        if (pfSettings.getYesNo(pfKey('-name-oneliner'))) {
            pfTitleBox.drawAsSingleLine(g, pfSettings.getRegion(pfKey('-name')));
        } else {
            pfTitleBox.draw(g, pfSettings.getRegion(pfKey('-name')));
        }
    }

    if (pfContentBox != null) {
        pfContentBox.markupText = $Content;
        pfContentBox.draw(g, pfSettings.getRegion(pfKey('-content')));
    }

    if (hasOverlay) {
        pfPaintOverlay(g, sheet);
    }

    if (global['afterPaintFront'] !== undefined)
        global.afterPaintFront(g, diy, sheet);
}

function paintBack(g, diy, sheet) {
    if (global['beforePaintBack'] !== undefined)
        global.beforePaintBack(g, diy, sheet);

    // NOTE WELL: Currently, this function is ONLY called if there is a portrait
    // painted on the back face, so the code assumes that this is the case.
    // If that changes, the code must be updated accordingly.

    var overlayKey = pfKey('-portrait-overlay');
    if (pfSettings.get(overlayKey) == null) {
        sheet.paintPortrait(g);
        sheet.paintTemplateImage(g);
    } else {
        sheet.paintTemplateImage(g);
        sheet.paintPortrait(g);
        pfPaintOverlay(g, sheet);
    }

    if (global['afterPaintBack'] !== undefined)
        global.afterPaintBack(g, diy, sheet);
}

function pfPaintOverlay(g, sheet) {
    var overlayKey = pfKey('-portrait-overlay');
    var regionKey = pfKey('-portrait-overlay-region');
    if (pfSettings.get(regionKey) == null) {
        sheet.paintImage(g, overlayKey, 0, 0, sheet.templateWidth, sheet.templateHeight);
    } else {
        sheet.paintImage(g, overlayKey, pfKey('-portrait-overlay-region'));
    }
}

function onClear(diy) {
    if (global['beforeOnClear'] !== undefined)
        global.beforeOnClear(diy);
    diy.name = '';
    $Content = '';
    if (global['afterOnClear'] !== undefined)
        global.afterOnClear(diy);
}

function onRead(diy, ois) {
    var prefabVersion = ois.readInt();
    pfInit(diy);
    if (global['beforeOnRead'] !== undefined)
        global.beforeOnRead(diy, ois);
    if (global['afterOnRead'] !== undefined)
        global.afterOnRead(diy, ois);
}

function onWrite(diy, oos) {
    oos.writeInt(1);
    if (global['beforeOnWrite'] !== undefined)
        global.beforeOnWrite(diy, oos);
    if (global['afterOnWrite'] !== undefined)
        global.afterOnWrite(diy, ois);
}

if (sourcefile == 'Quickscript') {
    // invokeLater gives the script that is including this library a
    // chance to set the pfBaseKey
    java.awt.EventQueue.invokeLater(testDIYScript);
}