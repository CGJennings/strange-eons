import * as ts from "typescript";

/*
 ** Adapter layer between TSLanguageServices and TypeScript
 ** Declared functions implement TSLanguageServices.ServiceInterface
 */
const arkham = Packages.ca.cgjennings.apps.arkham;
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
importClass(java.util.ArrayList);

/** Name of the library of standard JS type defs. */
const TYPE_LIB_FILENAME = "lib.d.ts";
/** Script snapshot of the library of standard JS type defs. */
const TYPE_LIB_SNAPSHOT = (function () {
    let lib = ProjectUtilities.getResourceText("/ca/cgjennings/apps/arkham/plugins/typescript/" + TYPE_LIB_FILENAME);
    return ts.ScriptSnapshot.fromString(String(lib));
})();

const SHARED_DOCUMENT_REGISTRY = ts.createDocumentRegistry();

const COMPILER_OPTIONS = {
    // allowJs: true,
    checkJs: true,
    sourceMap: true,
    target: ts.ScriptTarget.ES5,
    module: ts.ModuleKind.CommonJS,
    isolatedModules: true,
    forceConsistentCasingInFileNames: true
};

/** Logging functions for TS langauge service to use. */
var log, trace;
var error = (s) => Eons.log.severe("TSLS: " + s);

if (DEBUG) {
    log = (s) => Eons.log.info("TSLS: " + s);
    trace = (s) => Eons.log.fine("TSLS: " + s);
} else {
    log = trace = (s) => undefined;
}


/** **SERVICE** Returns the version of the TypeScript library. */
function getVersion() {
    return ts.version;
}

/** **SERVICE** Returns the raw TS lib object for development/debugging. */
function getServicesLib() {
    return ts;
}

/** **SERVICE** Returns the transpilation of a single source file with no error checking. */
function transpile(fileName: JavaString, text: JavaString) {
    return ts.transpile(String(text), COMPILER_OPTIONS, String(fileName));
}

/** **SERVICE** Creates a script snapshot from the text of a script. */
function createSnapshot(text: JavaString) {
    return ts.ScriptSnapshot.fromString(String(text));
}

/** **SERVICE** Creates a language service from the specified language service host. */
function createLanguageService(host: ts.LanguageServiceHost) {
    return ts.createLanguageService(host, SHARED_DOCUMENT_REGISTRY);
}

/** **SERVICE** Creates a language service host that delegates to a Java `CompilationRoot`. */
function createLanguageServiceHost(compileRoot: CompilationRoot): Partial<ts.LanguageServiceHost> {
    return {
        log: log,
        trace: trace,
        error: error,

        getCompilationSettings() {
            return COMPILER_OPTIONS;
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
            return String(compileRoot.getVersion(fileName));
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

/** **SERVICE** Compiles a source file into a Java CompiledSource. */
function compile(service: ts.LanguageService, fileName: string | JavaString) {
    fileName = String(fileName);
    let emit = service.getEmitOutput(fileName);
    let result = new CompiledSource();
    result.sourceFile = fileName;
    for (let i = 0; i < emit.outputFiles.length; ++i) {
        let out = emit.outputFiles[i];
        if (out.name.endsWith(".js")) {
            // convert file name like source.ts to source.ts.js;
            // otherwise use result.jsFile = out.name;
            result.jsFile = fileName + ".js";
            result.js = out.text;
        } else if (out.name.endsWith(".map")) {
            result.mapFile = out.name;
            result.map = out.text;
        }
    }
    trace(result);
    return result;
}

/** **SERVICE** Returns a list of diagnostic messages for a file. */
function getDiagnostics(service: ts.LanguageService, fileName: string | JavaString, syntactic: boolean, semantic: boolean) {
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

/** Convert and accumulate TS diagnostics into an ArrayList. */
function appendDiagnostics(service: ts.LanguageService, fileName: string | JavaString, list: ArrayList<Diagnostic>, diagnostics: ts.Diagnostic[]) {
    if (diagnostics) {
        if (list == null) {
            list = new ArrayList<Diagnostic>(Math.max(32, diagnostics.length));
        }
        // convert once rather than for each diagnostic
        fileName = JavaString(fileName);
        // convert each diagnostic and append to list
        for (let diag of diagnostics) {
            let javaDiag;
            let code = diag.code ? diag.code : 0;
            let message = ts.flattenDiagnosticMessageText(diag.messageText, "\n");
            // has location info
            if (diag.start) {
                let lineCol = service.toLineColumnOffset(String(fileName), diag.start);
                javaDiag = new Diagnostic(
                    code, message, fileName, lineCol.line,
                    lineCol.character, diag.start, diag.length
                );
            } else {
                javaDiag = new Diagnostic(code, message);
            }
            list.add(javaDiag);
        }
    }
    return list;
}

/** **SERVICE** Returns a list of code completions or null. */
function getCodeCompletions(service: ts.LanguageService, fileName: string | JavaString, position: number) {
    let complInfo: ts.CompletionInfo = service.getCompletionsAtPosition(String(fileName), position, undefined/* options*/);
    if (complInfo == null) {
        return null;
    }

    // if a replacement span is specified, use it as a default for all completions
    let defaultReplacementSpan = null;
    if (complInfo.optionalReplacementSpan) {
        let span = complInfo.optionalReplacementSpan;
        defaultReplacementSpan = new TextSpan(span.start, span.length);
    }

    let javaComplInfo = new CompletionInfo();
    javaComplInfo.isMemberCompletion = !!complInfo.isMemberCompletion;
    javaComplInfo.isNewIdentifierLocation = !!complInfo.isNewIdentifierLocation;
    javaComplInfo.isIncomplete = !!complInfo.isIncomplete;

    let entries = new ArrayList(complInfo.entries.length);
    for (let i = 0; i < complInfo.entries.length; ++i) {
        let entry = complInfo.entries[i];

        let javaEntry = new CompletionInfo.Entry(entry, parseInt(entry.sortText));
        javaEntry.name = entry.name;
        javaEntry.kind = entry.kind;
        javaEntry.kindModifiers = entry.kindModifiers;
        javaEntry.isRecommended = !!entry.isRecommended;
        javaEntry.isSnippet = !!entry.isSnippet;
        javaEntry.insertText = entry.insertText == null ? null : entry.insertText;
        javaEntry.hasAction = !!entry.hasAction;
        javaEntry.sourceDisplay = mergeDocParts(entry.sourceDisplay);

        let span = defaultReplacementSpan;
        if (entry.replacementSpan) {
            span = new TextSpan(entry.replacementSpan.start, entry.replacementSpan.length);
        }
        javaEntry.replacementSpan = span;
        entries.add(javaEntry);
    }
    javaComplInfo.entries = entries;
    return javaComplInfo;
}

function mergeDocParts(parts) {
    let s = "";
    if (parts != null) {
        for (let i = 0; i < parts.length; ++i) {
            s += parts[i].text;
        }
    }
    return s;
}

/** **SERVICE** Returns additional information about a code completion. */
function getCodeCompletionDetails(service: ts.LanguageService, fileName: string | JavaString, position: number, javaEntry: CompletionInfo.Entry) {
    let entry = javaEntry.js;
    let details = service.getCompletionEntryDetails(String(fileName), position, entry.name, undefined, entry.source, undefined, entry.data);
    if (details == null)
        return null;
    let javaDetails = new CompletionInfo.EntryDetails();
    javaDetails.display = mergeDocParts(details.displayParts);
    javaDetails.documentation = mergeDocParts(details.documentation);
    javaDetails.source = mergeDocParts(details.sourceDisplay);

    if (details.codeActions) {
        let actions = new ArrayList(details.codeActions.length);
        for (let i = 0; i < details.codeActions.length; ++i) {
            actions.add(convertCodeAction(service, details.codeActions[i]));
        }
        javaDetails.actions = actions;
    }
    log(javaDetails);
    return javaDetails;
}

function convertCodeAction(service: ts.LanguageService, codeAction: ts.CodeAction) {
    let changes = new ArrayList(codeAction.changes.length);
    for (let i = 0; i < codeAction.changes.length; ++i) {
        let ch = codeAction.changes[i];
        let javaChange = new FileTextChanges(
            ch.fileName, !!ch.isNewFile, convertTextChanges(ch.textChanges)
        );
        changes.add(javaChange);
    }
    let javaAction = new CodeAction(
        codeAction.description, changes, {
        service,
        action: codeAction
    }
    );
    return javaAction;
}

function convertSpan(span) {
    return (span == null || span.start == null || span.length == null)
        ? null
        : new TextSpan(span.start, span.length);
}

function convertTextChanges(changes) {
    let javaChanges = new ArrayList(changes.length);
    for (let i = 0; i < changes.length; ++i) {
        let javaChange = new TextChange(
            changes[i].span.start,
            changes[i].span.length,
            changes[i].newText
        );
        javaChanges.add(javaChange);
    }
    return javaChanges;
}

// /**
//  * Informs the language service that a code action from a code completion,
//  * refactoring, or similar action.
//  * Service interface implementation.
//  * 
//  * @param {CodeAction} javaCodeAction
//  */
// function applyCodeAction(javaCodeAction: CodeAction) {
//     const jai = javaCodeAction.jsActionInfo;
//     jai.service.applyAction(jai.action);
// }

/** **SERVICE** Returns an outline of the file. */
function getNavigationTree(service: ts.LanguageService, fileName: string | JavaString) {
    return convertNavigationTree(service.getNavigationTree(String(fileName)));
}

function convertNavigationTree(root: ts.NavigationTree) {
    let jsRoot = new NavigationTree(root.text, root.kind,
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

/** **SERVICE** Returns an overview ("quick info") for a file position. */
function getOverview(service: ts.LanguageService, fileName: string | JavaString, position: number) {
    let quick = service.getQuickInfoAtPosition(String(fileName), position);
    if (quick == null) return null;

    let overview = new Overview();
    overview.kind = quick.kind;
    overview.kindModifiers = quick.kindModifiers;
    overview.display = mergeDocParts(quick.displayParts);
    overview.documentation = mergeDocParts(quick.documentation);
    if (quick.tags) {
        overview.tags = new ArrayList(quick.tags.length);
        for (let i = 0; i < quick.tags.length; ++i) {
            let tag = quick.tags[i];
            overview.tags.add(convertDocTag(tag));
        }
    }
    return overview;
}

function convertDocTag(tag) {
    let type = tag.name;
    let parts = ["", ""];
    if (tag.text) {
        for (let i = 0, p = 0; i < tag.text.length; ++i) {
            let chunk = tag.text[i];
            if (chunk.kind === "space" || chunk.kind === "text") {
                p = 1;
            }
            parts[p] += chunk.text;
        }
    }
    return new DocTag(type, parts[0], parts[1]);
}





/** Convert JS string to a Java string; equivalent of `String(s)` for Java strings. */
function JavaString(s: string | JavaString): JavaString {
    // @ts-ignore
    if (!(s instanceof java.lang.String)) {
        // @ts-ignore
        s = java.lang.String.valueOf(s);
    }
    // @ts-ignore
    return s;
}