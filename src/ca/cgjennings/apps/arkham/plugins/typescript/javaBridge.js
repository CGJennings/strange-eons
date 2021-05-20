// The bridge object defines methods that implement TypeScriptServices
var COMPILER_OPTIONS = {
    allowJs: true,
    checkJs: true,
    lib: [ts.ScriptTarget.ES6],
    target: ts.ScriptTarget.ES5,
    module: ts.ModuleKind.CommonJS,
};

var bridge = {
    getVersion() {
        return ts.version;
    },
    transpile(sourceString) {
        return ts.transpile(sourceString, COMPILER_OPTIONS, "Quickscript");
    }
};