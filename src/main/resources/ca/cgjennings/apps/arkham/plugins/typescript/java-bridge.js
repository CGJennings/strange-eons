"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getOverview = exports.getNavigationTree = exports.getCodeCompletionDetails = exports.getCodeCompletions = exports.getDiagnostics = exports.compile = exports.createLanguageServiceHost = exports.createLanguageService = exports.createSnapshot = exports.transpile = exports.getServicesLib = exports.getVersion = void 0;
var ts = require("typescript");
var arkham = Packages.ca.cgjennings.apps.arkham;
importClass(arkham.project.ProjectUtilities);
importClass(arkham.plugins.typescript.CompiledSource);
importClass(arkham.plugins.typescript.Diagnostic);
importClass(arkham.plugins.typescript.TextSpan);
importClass(arkham.plugins.typescript.TextChange);
importClass(arkham.plugins.typescript.CompletionInfo);
importClass(arkham.plugins.typescript.DocTag);
importClass(arkham.plugins.typescript.DocCommentable);
importClass(arkham.plugins.typescript.CodeAction);
importClass(arkham.plugins.typescript.FileTextChanges);
importClass(arkham.plugins.typescript.NavigationTree);
importClass(arkham.plugins.typescript.Overview);
importClass(arkham.plugins.typescript.JavaTypes);
importClass(java.util.ArrayList);
var COMPILER_OPTIONS = {
    checkJs: true,
    sourceMap: true,
    target: ts.ScriptTarget.ES5,
    module: ts.ModuleKind.CommonJS,
    isolatedModules: true,
    forceConsistentCasingInFileNames: true
};
var SharedDocuments = {
    registry: ts.createDocumentRegistry(),
    map: {},
    add: function (resourcePath) {
        var fileName = resourcePath.substring(resourcePath.lastIndexOf("/") + 1);
        var source = ProjectUtilities.getResourceText(resourcePath);
        if (source == null)
            throw new Error("missing resource: " + resourcePath);
        var snapshot = ts.ScriptSnapshot.fromString(String(source));
        SharedDocuments.map[fileName] = snapshot;
        SharedDocuments.registry.acquireDocument(fileName, COMPILER_OPTIONS, snapshot, ts.version);
        return fileName;
    }
};
var StdLibFileName = SharedDocuments.add("/ca/cgjennings/apps/arkham/plugins/typescript/lib.d.ts");
var CommonLibFileName = SharedDocuments.add("libraries/common.d.ts");
var log, trace;
var error = function (s) { return Eons.log.severe("TSLS: " + s); };
if (DEBUG) {
    log = function (s) { return Eons.log.info("TSLS: " + s); };
    trace = function (s) { return Eons.log.fine("TSLS: " + s); };
}
else {
    log = trace = function (s) { return undefined; };
}
function getVersion() {
    return ts.version;
}
exports.getVersion = getVersion;
function getServicesLib() {
    return ts;
}
exports.getServicesLib = getServicesLib;
function transpile(fileName, text) {
    return ts.transpile(String(text), COMPILER_OPTIONS, String(fileName));
}
exports.transpile = transpile;
function createSnapshot(text) {
    return ts.ScriptSnapshot.fromString(String(text));
}
exports.createSnapshot = createSnapshot;
function createLanguageService(host) {
    return ts.createLanguageService(host, SharedDocuments.registry);
}
exports.createLanguageService = createLanguageService;
function createLanguageServiceHost(compileRoot) {
    return {
        log: log,
        trace: trace,
        error: error,
        getCompilationSettings: function () {
            return COMPILER_OPTIONS;
        },
        getCurrentDirectory: function () {
            return "";
        },
        getDefaultLibFileName: function () {
            return StdLibFileName;
        },
        getScriptVersion: function (fileName) {
            trace("getScriptVersion " + fileName);
            if (SharedDocuments.map[fileName]) {
                return "1";
            }
            return String(compileRoot.getVersion(fileName));
        },
        getScriptSnapshot: function (fileName) {
            trace("getScriptSnapshot " + fileName);
            var snapshot = SharedDocuments.map[fileName];
            if (snapshot == null) {
                snapshot = compileRoot.getSnapshot(fileName);
            }
            return snapshot;
        },
        getScriptFileNames: function () {
            trace("getScriptFileNames");
            var files = compileRoot.list();
            var out = new Array(files.length + 2);
            for (var i = 0; i < files.length; ++i) {
                out[i] = String(files[i]);
            }
            out[out.length - 2] = StdLibFileName;
            out[out.length - 1] = CommonLibFileName;
            return out;
        },
        fileExists: function (fileName) {
            trace("fileExists " + fileName);
            return compileRoot.exists(fileName);
        },
        directoryExists: function (directoryName) {
            trace("directoryExists " + directoryName);
            return compileRoot.directoryExists(directoryName);
        },
        getNewLine: function () {
            return "\n";
        },
    };
}
exports.createLanguageServiceHost = createLanguageServiceHost;
function compile(service, fileName) {
    fileName = String(fileName);
    var emit = service.getEmitOutput(fileName);
    var result = new CompiledSource();
    result.sourceFile = fileName;
    for (var i = 0; i < emit.outputFiles.length; ++i) {
        var out = emit.outputFiles[i];
        if (out.name.endsWith(".js")) {
            result.jsFile = fileName + ".js";
            result.js = out.text;
        }
        else if (out.name.endsWith(".map")) {
            result.mapFile = out.name;
            result.map = out.text;
        }
    }
    trace(result);
    return result;
}
exports.compile = compile;
function getDiagnostics(service, fileName, syntactic, semantic) {
    var list = null;
    fileName = String(fileName);
    if (syntactic) {
        list = appendDiagnostics(service, fileName, list, service.getSyntacticDiagnostics(fileName));
    }
    if (semantic) {
        list = appendDiagnostics(service, fileName, list, service.getSemanticDiagnostics(fileName));
    }
    return list;
}
exports.getDiagnostics = getDiagnostics;
function appendDiagnostics(service, fileName, list, diagnostics) {
    if ((diagnostics === null || diagnostics === void 0 ? void 0 : diagnostics.length) > 0) {
        if (list == null) {
            list = new ArrayList(Math.max(32, diagnostics.length));
        }
        fileName = JavaString(fileName);
        for (var _i = 0, diagnostics_1 = diagnostics; _i < diagnostics_1.length; _i++) {
            var diag = diagnostics_1[_i];
            var javaDiag = void 0;
            var code = diag.code ? diag.code : 0;
            var message = ts.flattenDiagnosticMessageText(diag.messageText, "\n");
            if (diag.start) {
                var lineCol = service.toLineColumnOffset(String(fileName), diag.start);
                javaDiag = new Diagnostic(code, message, fileName, lineCol.line, lineCol.character, diag.start, diag.length);
            }
            else {
                javaDiag = new Diagnostic(code, message);
            }
            list.add(javaDiag);
        }
    }
    return list;
}
function getCodeCompletions(service, fileName, position) {
    var complInfo = service.getCompletionsAtPosition(String(fileName), position, undefined);
    if (complInfo == null) {
        return null;
    }
    var defaultReplacementSpan = null;
    if (complInfo.optionalReplacementSpan) {
        var span = complInfo.optionalReplacementSpan;
        defaultReplacementSpan = new TextSpan(span.start, span.length);
    }
    var javaComplInfo = new CompletionInfo();
    javaComplInfo.isMemberCompletion = !!complInfo.isMemberCompletion;
    javaComplInfo.isNewIdentifierLocation = !!complInfo.isNewIdentifierLocation;
    javaComplInfo.isIncomplete = !!complInfo.isIncomplete;
    var entries = new ArrayList(complInfo.entries.length);
    for (var i = 0; i < complInfo.entries.length; ++i) {
        var entry = complInfo.entries[i];
        var javaEntry = new CompletionInfo.Entry(entry, parseInt(entry.sortText));
        javaEntry.name = entry.name;
        javaEntry.kind = entry.kind;
        javaEntry.kindModifiers = entry.kindModifiers;
        javaEntry.isRecommended = !!entry.isRecommended;
        javaEntry.isSnippet = !!entry.isSnippet;
        javaEntry.insertText = entry.insertText == null ? null : entry.insertText;
        javaEntry.hasAction = !!entry.hasAction;
        javaEntry.sourceDisplay = mergeDocParts(entry.sourceDisplay);
        var span = defaultReplacementSpan;
        if (entry.replacementSpan) {
            span = new TextSpan(entry.replacementSpan.start, entry.replacementSpan.length);
        }
        javaEntry.replacementSpan = span;
        entries.add(javaEntry);
    }
    javaComplInfo.entries = entries;
    return javaComplInfo;
}
exports.getCodeCompletions = getCodeCompletions;
function mergeDocParts(parts) {
    var s = "";
    if (parts != null) {
        for (var i = 0; i < parts.length; ++i) {
            s += parts[i].text;
        }
    }
    return s;
}
function getCodeCompletionDetails(service, fileName, position, javaEntry) {
    var entry = javaEntry.js;
    var details = service.getCompletionEntryDetails(String(fileName), position, entry.name, undefined, entry.source, undefined, entry.data);
    if (details == null)
        return null;
    var javaDetails = new CompletionInfo.EntryDetails();
    javaDetails.display = mergeDocParts(details.displayParts);
    javaDetails.documentation = mergeDocParts(details.documentation);
    javaDetails.source = mergeDocParts(details.sourceDisplay);
    if (details.codeActions) {
        var actions = new ArrayList(details.codeActions.length);
        for (var i = 0; i < details.codeActions.length; ++i) {
            actions.add(convertCodeAction(service, details.codeActions[i]));
        }
        javaDetails.actions = actions;
    }
    log(javaDetails);
    return javaDetails;
}
exports.getCodeCompletionDetails = getCodeCompletionDetails;
function convertCodeAction(service, codeAction) {
    var changes = new ArrayList(codeAction.changes.length);
    for (var i = 0; i < codeAction.changes.length; ++i) {
        var ch = codeAction.changes[i];
        var javaChange = new FileTextChanges(ch.fileName, !!ch.isNewFile, convertTextChanges(ch.textChanges));
        changes.add(javaChange);
    }
    var javaAction = new CodeAction(codeAction.description, changes, {
        service: service,
        action: codeAction
    });
    return javaAction;
}
function convertSpan(span) {
    return (span == null || span.start == null || span.length == null)
        ? null
        : new TextSpan(span.start, span.length);
}
function convertTextChanges(changes) {
    var javaChanges = new ArrayList(changes.length);
    for (var i = 0; i < changes.length; ++i) {
        var javaChange = new TextChange(changes[i].span.start, changes[i].span.length, changes[i].newText);
        javaChanges.add(javaChange);
    }
    return javaChanges;
}
function getNavigationTree(service, fileName) {
    return convertNavigationTree(service.getNavigationTree(String(fileName)));
}
exports.getNavigationTree = getNavigationTree;
function convertNavigationTree(root) {
    var jsRoot = new NavigationTree(root.text, root.kind, root.kindModifiers, convertSpan(root.nameSpan || root.spans[0]));
    if (root.childItems) {
        jsRoot.children = new ArrayList(root.childItems.length);
        for (var i = 0; i < root.childItems.length; ++i) {
            jsRoot.children.add(convertNavigationTree(root.childItems[i]));
        }
    }
    return jsRoot;
}
function getOverview(service, fileName, position) {
    var quick = service.getQuickInfoAtPosition(String(fileName), position);
    if (quick == null)
        return null;
    var overview = new Overview();
    overview.kind = quick.kind;
    overview.kindModifiers = quick.kindModifiers;
    overview.display = mergeDocParts(quick.displayParts);
    overview.documentation = mergeDocParts(quick.documentation);
    if (quick.tags) {
        overview.tags = new ArrayList(quick.tags.length);
        for (var i = 0; i < quick.tags.length; ++i) {
            var tag = quick.tags[i];
            overview.tags.add(convertDocTag(tag));
        }
    }
    return overview;
}
exports.getOverview = getOverview;
function convertDocTag(tag) {
    var type = tag.name;
    var parts = ["", ""];
    if (tag.text) {
        for (var i = 0, p = 0; i < tag.text.length; ++i) {
            var chunk = tag.text[i];
            if (chunk.kind === "space" || chunk.kind === "text") {
                p = 1;
            }
            parts[p] += chunk.text;
        }
    }
    return new DocTag(type, parts[0], parts[1]);
}
function JavaString(s) {
    if (!(s instanceof java.lang.String)) {
        s = java.lang.String.valueOf(s);
    }
    return s;
}
