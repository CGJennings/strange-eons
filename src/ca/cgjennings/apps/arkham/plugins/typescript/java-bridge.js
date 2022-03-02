//
// Adapter layer between TSLanguageServices and TypeScript
// Declared functions implement TSLanguageServices.ServiceInterface
//

importClass(Packages.ca.cgjennings.apps.arkham.project.ProjectUtilities);
importClass(java.util.ArrayList);
const TS_PACKAGE = Packages.ca.cgjennings.apps.arkham.plugins.typescript;
const SHARED_DOCUMENT_REGISTRY = ts.createDocumentRegistry();



const TYPE_LIB_FILENAME = "lib.d.ts";

// create snapshot of baseline type defs
const TYPE_LIB_SNAPSHOT = (function() {
    let lib = ProjectUtilities.getResourceText("/ca/cgjennings/apps/arkham/plugins/typescript/" + TYPE_LIB_FILENAME);
    return ts.ScriptSnapshot.fromString(String(lib));
})();







const COMPILER_OPTIONS = {
    allowJs: true,
    checkJs: true,
    sourceMap: true,
//    lib: [ts.ScriptTarget.ES6],
    target: ts.ScriptTarget.ES5,
    module: ts.ModuleKind.CommonJS,
};

function log(s) {
    Eons.log.info(s);
}

function trace(s) {
    Eons.log.fine(s);
}

function error(s) {
    Eons.log.severe(s);
}

function getLib() {
    return ts;
}





function getVersion() {
    return ts.version;
}





function createSnapshot(text) {
    return ts.ScriptSnapshot.fromString(String(text));
}





function transpile(fileName, text) {
    return ts.transpile(String(text), COMPILER_OPTIONS, String(fileName));
}





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
            return "lib";
        },

        getScriptVersion(fileName) {
            if (fileName == TYPE_LIB_FILENAME) {
                return "";
            }            
            
            return compileRoot.getVersion(fileName);
        },

        getScriptSnapshot(fileName) {
            trace("requesting snapshot " + fileName);
            if (fileName == TYPE_LIB_FILENAME) {
                return TYPE_LIB_SNAPSHOT;
            }
            
            return compileRoot.getSnapshot(fileName);
        },

        getScriptFileNames() {
            trace("requesting file list");
            let files = compileRoot.list();
            let out = new Array(files.length);
            for (let i = 0; i < files.length; ++i) {
                out[i] = String(files[i]);
            }
            return out;
        },
        
        getNewLine() {
            return "\n";
        },
    };
}





function createLanguageService(host) {
    return ts.createLanguageService(host, SHARED_DOCUMENT_REGISTRY);
}





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
