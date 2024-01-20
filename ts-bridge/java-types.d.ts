import * as ts from "typescript";

declare global {
    var DEBUG: boolean;

    /**
     * The TS lib is picky about being passed Java strings
     * instead of JavaScript strings. By declaring incoming
     * Java strings as `JavaString`, compile-time errors
     * will ensure we convert them with `String(javaString)`.
     */
    interface JavaString {
        equals(other: JavaString): boolean;
    }

    //
    // Minimal typing to describe the bits of Strange Eons API
    // that we need to implement the library.
    //

    type JavaPackage = {
        [key: string]: JavaPackage;
    };
    var Packages: JavaPackage;
    var java: JavaPackage;
    function importClass(javaPackage: JavaPackage): void;


    interface List<T> {
        add(T): boolean;
        size(): number;
    }
    class ArrayList<T> implements List<T> {
        constructor();
        constructor(initialCapacity: number);
        add(item: T): boolean;
        size(): number;
    }


    interface SELogger {
        fine(message: string): void;
        info(message: string): void;
        severe(message: string): void;
    }
    var Eons: {
        log: SELogger;
    }


    var ProjectUtilities: {
        getResourceText(path: string | JavaString): JavaString
    }


    interface CompilationRoot {
        getVersion(fileName: string | JavaString): JavaString;
        getSnapshot(fileName: string | JavaString): ts.IScriptSnapshot;
        list(): JavaString[];
        exists(fileName: string | JavaString): boolean;
    }

    class CompiledSource {
        compilationRoot: CompilationRoot;
        sourceFile: string;
        jsFile: string;
        js: string;
        mapFile: string;
        map: string;
    }

    class Diagnostic {
        constructor(code: number, message: string | JavaString);
        constructor(code: number, message: string | JavaString, file: string | JavaString, line: number, col: number, offset: number, length: number);
    }

    class TextSpan {
        start: number;
        length: number;
        constructor(start: number, length: number);
    }

    class TextChange extends TextSpan {
        newText: string|JavaString;
        constructor(start: number, length: number, newText: string|JavaString);
    }

    namespace CompletionInfo {
        class Entry {
            constructor(jsObject, sortKey: number);

            name: string;
            kind: string;
            kindModifiers: string;
            isSnippet: boolean;
            insertText: string;
            replacementSpan: TextSpan;
            isRecommended: boolean;
            hasAction: boolean;
            sourceDisplay: string;
            js: ts.CompletionEntry;
        }

        class EntryDetails extends DocCommentable {
            constructor();
            actions: List<CodeAction>;
        }
    }
    class CompletionInfo {
        constructor();
        isMemberCompletion: boolean;
        isNewIdentifierLocation: boolean;
        isIncomplete: boolean;
        entries: List<CompletionInfo.Entry>;
    }

    class DocTag {
        constructor(tag: string, name: string, text: string);
        tag: string;
        name: string;
        text: string;
    }

    class DocCommentable {
        constructor();
        kind: string;
        kindModifiers: string;
        name: string;
        display: string;
        documentation: string;
        source: string;
        tags: List<DocTag>;
    }

    class CodeAction {
        constructor(description: string, changes: List<FileTextChanges>, data: object);
        description: string;
        changes: List<FileTextChanges>;
        jsActionInfo: { service: ts.LanguageService, action: ts.CodeAction };
    }

    class FileTextChanges {
        constructor(fileName: string | JavaString, isNewFile: boolean, textChanges: List<TextChange>);
        fileName: string;
        isNewFile: boolean;
        textChanges: List<TextChange>;
    }

    class NavigationTree {
        constructor(name: string, kind: string, kindModifiers: string, location: TextSpan);
        name: string;
        kind: string;
        kindModifiers: string;
        location: TextSpan;
        children: List<NavigationTree>;
    }

    class Overview extends DocCommentable {
        constructor();
    }
}