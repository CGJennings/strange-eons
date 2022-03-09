/*
 ** Adapter layer between TSLanguageServices and TypeScript
 ** Declared functions implement TSLanguageServices.ServiceInterface
 */

importClass(Packages.ca.cgjennings.apps.arkham.project.ProjectUtilities);
importClass(java.util.ArrayList);
const TS_PACKAGE = Packages.ca.cgjennings.apps.arkham.plugins.typescript;


/** Name of the library of standard JS type defs. */
const TYPE_LIB_FILENAME = "lib.d.ts";
/** Script snapshot of the library of standard JS type defs. */
const TYPE_LIB_SNAPSHOT = (function () {
    let lib = ProjectUtilities.getResourceText("/ca/cgjennings/apps/arkham/plugins/typescript/" + TYPE_LIB_FILENAME);
    return ts.ScriptSnapshot.fromString(String(lib));
})();

const SHARED_DOCUMENT_REGISTRY = ts.createDocumentRegistry();

const COMPILER_OPTIONS = {
    allowJs: true,
    checkJs: true,
    sourceMap: true,
    target: ts.ScriptTarget.ES5,
    module: ts.ModuleKind.CommonJS,
};

/* Logging functions used by TS LS host. */
var log, trace;
var error = (s) => Eons.log.severe("TSLS: " + s);
;
if (DEBUG) {
    log = (s) => Eons.log.info("TSLS: " + s);
    trace = (s) => Eons.log.fine("TSLS: " + s);
} else {
    log = trace = (s) => undefined;
}


/**
 * Returns the version of the TypeScript library.
 * Service interface implementation.
 * @returns {string} version string
 */
function getVersion() {
    return ts.version;
}

/**
 * Returns the raw TS lib object for development/debugging.
 * Service interface implementation.
 * @returns root TS library object
 */
function getServicesLib() {
    return ts;
}

/**
 * Returns the transpilation of a single source file with no error checking.
 * Service interface implementation.
 * @param {string} fileName a name for the script
 * @param {string} the TS source text
 * @returns {string} the transpiled JS
 */
function transpile(fileName, text) {
    return ts.transpile(String(text), COMPILER_OPTIONS, String(fileName));
}

/**
 * Creates a script snapshot from the text of a script.
 * Service interface implementation.
 * @param {string} the TS source text
 * @returns {ts.ScriptSnapshot} the script snapshot
 */
function createSnapshot(text) {
    return ts.ScriptSnapshot.fromString(String(text));
}

/**
 * Creates a language service from the specified language service host.
 * Service interface implementation.
 * @param {ts.LanguageServiceHost} host
 * @returns {ts.LanguageService} the language service
 */
function createLanguageService(host) {
    return ts.createLanguageService(host, SHARED_DOCUMENT_REGISTRY);
}

/**
 * Creates a language service host that delegates to a CompilationRoot.
 * Service interface implementation.
 * @param {CompilationRoot} compileRoot
 * @returns {ts.LanguageService} the language service
 */
function createLanguageServiceHost(compileRoot) {
    return {
        log: log,
        trace: trace,
        error: error,

        getCompilationSettings() {
            return COMPILER_OPTIONS;
        },

        getScriptIsOpen(fileName) {
            return true;
        },

        getCurrentDirectory() {
            return "";
        },

        getDefaultLibFileName() {
            return "lib.d.ts";
        },

        getScriptVersion(fileName) {
            if (fileName == TYPE_LIB_FILENAME) {
                return "";
            }

            return compileRoot.getVersion(fileName);
        },

        getScriptSnapshot(fileName) {
            if (fileName == TYPE_LIB_FILENAME) {
                return TYPE_LIB_SNAPSHOT;
            }

            trace("getScriptSnapshot " + fileName);
            return compileRoot.getSnapshot(fileName);
        },

        getScriptFileNames() {
            trace("getScriptFileNames");
            let files = compileRoot.list();
            let out = new Array(files.length + 1);
            for (let i = 0; i < files.length; ++i) {
                out[i] = String(files[i]);
            }
            out[out.length - 1] = TYPE_LIB_FILENAME;
            return out;
        },

        fileExists(fileName) {
            trace("fileExists" + fileName);
            return compileRoot.exists(fileName);
        },

        getNewLine() {
            return "\n";
        },
    };
}

/**
 * Compiles a source file.
 * Service interface implementation.
 * @param {ts.LanguageService} service
 * @param {string} fileName
 * @returns {CompiledSource} the transpiled code and source map
 */
function compile(service, fileName) {
    fileName = String(fileName);
    let emit = service.getEmitOutput(fileName);
    let result = new TS_PACKAGE.CompiledSource();
    result.sourceFile = fileName;
    for (let i = 0; i < emit.outputFiles.length; ++i) {
        let out = emit.outputFiles[i];
        if (out.name.endsWith(".js")) {
            result.jsFile = out.name;
            result.js = out.text;
        } else if (out.name.endsWith(".map")) {
            result.mapFile = out.name;
            result.map = out.text;
        }
    }
    trace(result);
    return result;
}

/**
 * Returns a list of diagnostic messages for a file.
 * Service interface implementation.
 * @param {ts.LanguageService} service
 * @param {string} fileName
 * @param {boolean} syntactic if true, include syntactic messages
 * @param {boolean} semantic if true, include semantic messages
 * @returns {ArrayList<Diagnostic>} list of diagnostics
 */
function getDiagnostics(service, fileName, syntactic, semantic) {
    let list = null;
    fileName = String(fileName);
    if (syntactic) {
        list = appendDiagnostics(service, fileName, list, service.getSyntacticDiagnostics(fileName));
    }
    if (semantic) {
        list = appendDiagnostics(service, fileName, list, service.getSemanticDiagnostics(fileName));
    }
    return list;
}

function appendDiagnostics(service, fileName, list, diagnostics) {
    if (diagnostics) {
        if (list == null) {
            list = new ArrayList(diagnostics.length);
        }
        if (!fileName instanceof java.lang.String) {
            // convert once rather than for each diagnostic
            fileName = java.lang.String.valueOf(fileName);
        }
        for (let diag of diagnostics) {
            list.add(convertDiagnostic(service, fileName, diag));
        }
    }
    return list;
}

function convertDiagnostic(service, fileName, diag) {
    let javaDiag;
    let code = diag.code ? diag.code : 0;
    let message = ts.flattenDiagnosticMessageText(diag, "\n");
    // has location info
    if (diag.start) {
        let lineCol = service.toLineColumnOffset(String(fileName), diag.start);
        javaDiag = new TS_PACKAGE.Diagnostic(
                code, message, fileName, lineCol.line,
                lineCol.character, diag.start, diag.length
                );
    } else {
        javaDiag = new TS_PACKAGE.Diagnostic(code, message);
    }
    return javaDiag;
}

/**
 * Returns a collection of code completions or null.
 * Service interface implementation.
 * @param {ts.LanguageService} service
 * @param {string} fileName
 * @param {number} position file offset
 * @returns {CompletionInfo} collected completions and context information
 */
function getCodeCompletions(service, fileName, position) {
    let complInfo = service.getCompletionsAtPosition(fileName, position/*, options*/);
    if (complInfo == null) {
        return null;
    }

    // if a replacement span is specified, use it as a default for all completions
    let defaultReplacementSpan = null;
    if (complInfo.optionalReplacementSpan) {
        let span = complInfo.optionalReplacementSpan;
        defaultReplacementSpan = new TS_PACKAGE.TextSpan(span.start, span.length);
    }

    let javaComplInfo = new TS_PACKAGE.CompletionInfo();
    javaComplInfo.isMemberCompletion = !!complInfo.isMemberCompletion;
    javaComplInfo.isNewIdentifierLocation = !!complInfo.isNewIdentifierLocation;
    javaComplInfo.isIncomplete = !!complInfo.isIncomplete;

    let entries = new ArrayList(complInfo.entries.length);
    for (let i = 0; i < complInfo.entries.length; ++i) {
        let entry = complInfo.entries[i];

        let javaEntry = new TS_PACKAGE.CompletionInfo.Entry(entry, parseInt(entry.sortText));
        javaEntry.name = entry.name;
        javaEntry.kind = entry.kind;
        javaEntry.kindModifiers = entry.kindModifiers;
        javaEntry.isRecommended = !!entry.isRecommended;
        javaEntry.isSnippet = !!entry.isSnippet;
        javaEntry.insertText = entry.insertText == null ? null : entry.insertText;
        javaEntry.hasAction = !!entry.hasAction;
        javaEntry.sourceDisplay = mergeSymbolDisplayParts(entry.sourceDisplay);

        let span = defaultReplacementSpan;
        if (entry.replacementSpan) {
            span = new TS_PACKAGE.TextSpan(entry.replacementSpan.start, entry.replacementSpan.length);
        }
        javaEntry.replacementSpan = span;
        entries.add(javaEntry);
    }
    javaComplInfo.entries = entries;
    return javaComplInfo;
}

function mergeSymbolDisplayParts(parts) {
    let s = "";
    if (parts != null) {
        for (let i = 0; i < parts.length; ++i) {
            s += parts[i].text;
        }
    }
    return s;
}

/**
 * Returns additional information about a code completion.
 * Service interface implementation.
 * @param {ts.LanguageService} service
 * @param {string} fileName
 * @param {number} position file offset
 * @param {CompletionInfo.Entry} javaEntry the completion entry to get details for
 * @returns {CompletionInfo.EntryDetails} further details about the completion
 */
function getCodeCompletionDetails(service, fileName, position, javaEntry) {
    let entry = javaEntry.js;
    let details = service.getCompletionEntryDetails(fileName, position, entry.name, undefined, entry.source, undefined, entry.data);
    if (details == null)
        return null;
    let javaDetails = new TS_PACKAGE.CompletionInfo.EntryDetails();
    javaDetails.display = mergeSymbolDisplayParts(details.displayParts);
    javaDetails.documentation = mergeSymbolDisplayParts(details.documentation);
    javaDetails.source = mergeSymbolDisplayParts(details.sourceDisplay);

    if (details.codeActions) {
        let actions = new ArrayList(details.codeActions.length);
        for (let i = 0; i < actions.length; ++i) {
            actions.add(convertCodeAction(service, details.codeActions[i]));
        }
        javaDetails.codeActions = actions;
    }
    log(javaDetails);
    return javaDetails;
}

function convertCodeAction(service, codeAction) {
    let changes = new ArrayList(codeAction.changes.length);
    for (let i = 0; i < codeAction.changes.length; ++i) {
        let ch = codeAction.changes[i];
        let javaChange = new TS_PACKAGE.FileTextChanges(
                ch.fileName, !!ch.isNewFile, covertTextChanges(ch.textChanges)
                );
        changes.add(javaChange);
    }
    let javaAction = new TS_PACKAGE.CodeAction(
            codeAction.description, changes, {
                service: service,
                action: codeAction
            }
    );
    return javaAction;
}

function convertSpan(span) {
    return (span == null || span.start == null || span.length == null)
            ? null
            : new TS_PACKAGE.TextSpan(span.start, span.length);
}

function convertTextChanges(changes) {
    let javaChanges = new ArrayList(changes.length);
    for (let i = 0; i < changes.length; ++i) {
        let javaChange = new TS_PACKAGE.TextChange(
                changes[i].span.start,
                changes[i].span.length,
                changes[i].newText
                );
        javaChanges.add(javaChange);
    }
    return javaChanges;
}

/**
 * Informs the language service that a code action from a code completion,
 * refactoring, or similar action.
 * Service interface implementation.
 * 
 * @param {CodeAction} javaCodeAction
 */
function applyCodeAction(javaCodeAction) {
    let data = javaCodeAction.data;
    data.service.applyAction(data.action);
}

/**
 * Returns an outline of the file.
 * Service interface implementation.
 * @param {ts.LanguageService} service
 * @param {string} fileName
 * @returns {NavigationTree} a tree of node descriptions
 */
function getNavigationTree(service, fileName) {
    return convertNavigationTree(service.getNavigationTree(String(fileName)));
}

function convertNavigationTree(root) {
    let jsRoot = new TS_PACKAGE.NavigationTree(root.text, root.kind,
            root.kindModifiers, convertSpan(root.nameSpan || root.spans[0])
            );
    if (root.childItems) {
        jsRoot.children = new ArrayList(root.childItems.length);
        for (let i = 0; i < root.childItems.length; ++i) {
            jsRoot.children.add(convertNavigationTree(root.childItems[i]));
        }
    }
    return jsRoot;
}
