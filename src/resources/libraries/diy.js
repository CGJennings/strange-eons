/*
 diy.js - version 12
 Create your own game component editor with script code.
 */

importClass(arkham.diy.DIY);
importClass(arkham.diy.DIYSheet);
const FaceStyle = DIY.FaceStyle;
const DeckSnappingHint = arkham.sheet.Sheet.DeckSnappingHint;
const HighResolutionMode = DIY.HighResolutionMode;

function testDIYScript(gameCode) {
    if (sourcefile != "Quickscript") {
        Error.warn("testDIYScript() left in plug-in bundle");
        return;
    }
    var handler = new arkham.diy.Handler() {
        create: function _create(d) {
            useSettings(d);
            create(d);
        },
        createInterface: function _createInterface(d, e) {
            useSettings(d);
            createInterface(d, e);
        },
        createFrontPainter: function _createFrontPainter(d, s) {
            useSettings(d);
            createFrontPainter(d, s);
        },
        createBackPainter: function _createBackPainter(d, s) {
            useSettings(d);
            createBackPainter(d, s);
        },
        paintFront: function _paintFront(g, d, s) {
            useSettings(d);
            paintFront(g, d, s);
        },
        paintBack: function _paintBack(g, d, s) {
            useSettings(d);
            paintBack(g, d, s);
        },
        onClear: function _onClear(d) {
            useSettings(d);
            onClear(d);
        },
        onRead: function _onRead(d, i) {
            useSettings(d);
            onRead(d, i);
        },
        onWrite: function _onWrite(d, o) {
            useSettings(d);
            onWrite(d, o);
        },
        getPortraitCount: function _getPortraitCount() {
            useSettings(diy);
            return getPortraitCount();
        },
        getPortrait: function _getPortrait(i) {
            useSettings(diy);
            return getPortrait(i);
        }
    };
    var diy = DIY.createTestInstance(handler, gameCode);
    var ed = diy.createDefaultEditor();
    ed.testModeEnabled = true;
    Eons.addEditor(ed);
}

