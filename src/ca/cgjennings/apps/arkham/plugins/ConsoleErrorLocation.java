package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.editors.CodeEditor;
import ca.cgjennings.apps.arkham.project.Member;
import ca.cgjennings.ui.textedit.JSourceCodeEditor;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import javax.swing.UIManager;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * Represents information about an error that has been printed as part of a
 * stack trace in the {@linkplain ScriptConsole script output window}. Contains
 * both an identifier that may be used to locate the original source file, and
 * an optional line number at which the error was located. Instances of this
 * class are not usually created directly, but are instead obtained from the
 * script console.
 *
 * @see ScriptConsole#getErrorAtOffset(int)
 * @see ScriptConsole#getErrorAtPoint(java.awt.Point)
 */
public final class ConsoleErrorLocation {

    private String identifier;
    private int lineNumber;
    private int evalLineNumber;

    /**
     * Creates an error from a stack trace element line. The stack trace line
     * consists of two basic parts: The sequence "\t at ", which signifies that
     * the remaining text (probably) represents stack trace information. The
     * string passed to this constructor should have already removed this
     * prefix, so that it starts just afterward. A stack trace element
     * description, which may have one of the following forms:
     * <ol>
     * <li> A standard script trace element consists of a script identifier, a
     * colon, and a line number. E.g.: <code>Quickscript:2</code>
     * <li> An embedded script trace element consists of the word script, and
     * the script identifier and line number in parentheses. E.g.:
     * <code>script(Quickscript:2)</code>
     * <li> A Java script trace element, which consists of class and method
     * descriptor, and the Java file and line number in parentheses E.g.:
     * <code>java.awt.Component.processEvent(Component.java:6270)</code> The
     * file and line number information may instead read "Unknown Source" if the
     * class was compiled without debugging information, or "Native Method" if
     * executing native (non-Java) code.
     * </ol>
     *
     * <p>
     * <b>Note:</b> If the specified stack trace text does not match one of the
     * above patterns, then the identifier will be <code>null</code>. In this
     * case the <code>ConsoleErrorLocation</code> is not valid and should not be
     * used further.
     *
     * @param stackTraceLine a stack trace line, without the "at" prefix (see
     * description)
     */
    ConsoleErrorLocation(String stackTraceLine) {
        stackTraceLine = stackTraceLine.trim();

        if (stackTraceLine.startsWith("script(")) {
            int close = stackTraceLine.lastIndexOf(')');
            stackTraceLine = stackTraceLine.substring("script(".length(), close);
        }

        int open = stackTraceLine.lastIndexOf('(');
        int hash = stackTraceLine.indexOf('#');
        if (open < 0 || hash > 0) {
            parsePlainScriptElement(stackTraceLine);
        } else {
            parseJavaElement(stackTraceLine, open);
        }
    }

    private void parsePlainScriptElement(String text) {
        int colon = text.lastIndexOf(':');
        if (colon >= 0) {
            try {
                identifier = text.substring(0, colon);
                String lineText = text.substring(colon + 1);
                lineNumber = Integer.parseInt(lineText);
                extractEvalLineNumber();
            } catch (NumberFormatException nfe) {
                // note: lineNumber will be 0 (for unknown)
            }
        }
        if (identifier == null) {
            identifier = text;
            lineNumber = 0;
        }
    }

    private void extractEvalLineNumber() {
        if (identifier.endsWith("(eval)")) {
            evalLineNumber = lineNumber;
            do {
                int hash = identifier.lastIndexOf('#');
                if (hash < 0) {
                    StrangeEons.log.warning("expected #");
                    return;
                }
                String lineText = identifier.substring(hash + 1, identifier.length() - "(eval)".length());
                identifier = identifier.substring(0, hash);
                try {
                    lineNumber = Integer.parseInt(lineText);
                } catch (NumberFormatException nfe) {
                    lineNumber = 0;
                }
            } while (identifier.endsWith("(eval)"));
        }
    }

    private void parseJavaElement(String text, int openParenIndex) {
//        String classInfo = text.substring(0, openParenIndex);
//
//        // try to find the .java file and a line number
//        String file = null;
//        int colon = text.lastIndexOf(':');
//        if (colon > openParenIndex) {
//            file = text.substring(openParenIndex + 1, colon);
//            String lineText = text.substring(colon + 1);
//            int close = lineText.lastIndexOf(')');
//            if (close >= 0) {
//                lineText = lineText.substring(0, close);
//            }
//            try {
//                lineNumber = Integer.parseInt(lineText);
//            } catch (NumberFormatException nfe) {
//                // leave line number set to 0
//            }
//        } else {
//            // there is no line number; we leave file == null and lineNumber == 0
//            // as the parentheses contain either Unknown Source or Native Method
//        }
//
//        // see if we can find this in the API database, which means it is
//        // a standard SE or Java class---in this case, we will set the
//        // identifier to a javadoc: URL that displays the class documentation
//        String[] parts = classInfo.split("\\.");
//        APINode node = APIDatabase.getPackageRoot();
//        int i = 0;
//        for (; i < parts.length; ++i) {
//            APINode child = node.find(parts[i]);
//            if (child == null) {
//                break;
//            }
//            node = child;
//        }
//        // we were able to partially match a class, but not the whole thing;
//        // assume the user has defined a new class in a standard package
//        // (or else the class is package private and not in the docs)
//        if (node instanceof PackageNode) {
//            node = APIDatabase.getPackageRoot();
//        }
//
//        if (node == APIDatabase.getPackageRoot()) {
//            // not in API database, try to find matching user source code
//            // if the .java file is not null
//            if (file != null && parts.length > 0) {
//                StringBuilder b = new StringBuilder(80);
//                b.append("res:");
//                if ("resources".equals(parts[0])) {
//                    b.append("//");
//                } else {
//                    b.append('/').append(parts[0]).append('/');
//                }
//                i = 1;
//                for (; i < parts.length; ++i) {
//                    if (parts[i].isEmpty() || Character.isUpperCase(parts[i].charAt(0))) {
//                        break;
//                    }
//                    b.append(parts[i]).append('/');
//                }
//                b.append(file);
//                parsePlainScriptElement(b.append(':').append(lineNumber).toString());
//                URL existsTest = ResourceKit.composeResourceURL(identifier);
//                if (existsTest == null) {
//                    identifier = null;
//                }
//            }
//        } else {
//            identifier = "javadoc:" + node.getFullyQualifiedName().replace('.', '/');
//        }
    }

    /**
     * Creates a new instance that describes an error with the specified source
     * identifier and line number.
     *
     * @param identifier the identifier that describes the location of the error
     * @param lineNumber the line number, starting from 1, or 0 if the line
     * number is unknown
     */
    public ConsoleErrorLocation(String identifier, int lineNumber) {
        this.identifier = identifier;
        this.lineNumber = Math.max(0, lineNumber);
    }

    /**
     * Returns the full file identifier for the error. For a script run in the
     * Quickscript window, this is "Quickscript". For a script run from a
     * plug-in, this may be a "res:" URL or a "file:" URL. For a script run in
     * the context of an open project, this will be a "project:" URL.
     *
     * @return a string that identifies the file where the error took place
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns a shortened version of the identifier. If the identifier
     * represents a URL or local file, then this returns the last name on the
     * path (the file name). Otherwise, the full identifier is returned.
     *
     * @return the file name portion of the path, or the full identifier
     */
    public String getShortIdentifier() {
        final int slash = identifier.lastIndexOf('/');
        final int os = identifier.lastIndexOf(File.separatorChar);
        final int pos = Math.max(slash, os);
        if (pos < 0) {
            return identifier;
        } else {
            return identifier.substring(pos + 1);
        }
    }

    /**
     * Returns the line number (starting from 1) of the error, or 0 if the line
     * number is unknown.
     *
     * @return the line number of the error
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns the line number (starting from 1) of the error, within an
     * evaluated string or 0 if the line number is unknown or was not within an
     * <code>eval</code> function. When an error occurs during the evaluation of
     * a string, this returns the line number within that string while
     * {@link #getLineNumber()} returns the line number of the
     * <code>eval()</code> call.
     *
     * @return the line number of the error within the evaluated code
     *
     */
    public int getEvalLineNumber() {
        return evalLineNumber;
    }

    /**
     * Attempts to open the error location in an editor. If the location
     * represents a script file, the script is opened in a source editor (if it
     * can be located) and the caret moved to the relevant line. If the error
     * occurred in the Quickscript window, that is made visible (if necessary)
     * and the caret moved to the relevant line. If the identifier refers to a
     * plug-in resource, then the file may be opened in read-only mode unless
     * exactly one equivalent file can be located in an appropriate location in
     * the open project. If the identifier refers to a Java class, then it may
     * be opened in a source editor window if a relevant source file can be
     * located. Otherwise, if the relevant class can be found in the
     * {@linkplain APIDatabase API database}, then its documentation will be
     * shown. If the file cannot be located or it cannot be opened for some
     * other reason, then error feedback (typically a beep) will be provided and
     * no editor will be opened.
     */
    public void open() {
        if (identifier.startsWith("javadoc:")) {
//            APIBrowser b = null;
//            for (StrangeEonsEditor ed : StrangeEons.getWindow().getEditors()) {
//                if (ed instanceof APIBrowser) {
//                    APIBrowser candidate = (APIBrowser) ed;
//                    if (Boolean.TRUE == candidate.getClientProperty("common")) {
//                        b = candidate;
//                        break;
//                    }
//                }
//            }
//            if (b != null) {
//                b.setURL(identifier);
//            } else {
//                b = new APIBrowser(identifier);
//                b.putClientProperty("common", Boolean.TRUE);
//                StrangeEons.addEditor(b);
//            }
//            b.select();
            return;
        }
        JSourceCodeEditor editor = findEditorForIdentifier(identifier);
        if (editor != null) {
            editor.requestFocus();
            showLineInEditor(editor, lineNumber);
            return;
        }
        UIManager.getLookAndFeel().provideErrorFeedback(null);
    }

    // SUPPORT CODE FOR open() ////////////////////////////////////////////////
    private JSourceCodeEditor findEditorForIdentifier(String id) {
        if (id.equals("Quickscript")) {
            return findQuickscript();
        }
        if (id.startsWith("project:")) {
            return findProjectFile(id.substring("project:".length()));
        }
        return findResourceScript(id);
    }

    @SuppressWarnings("empty-statement")
    private JSourceCodeEditor findProjectFile(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }

        Member parent = StrangeEons.getWindow().getOpenProject();
        if (parent == null) {
            return null;
        }

        // skip any initial slashes
        int skip = 0;
        for (; skip < id.length() && id.charAt(skip) == '/'; ++skip);
        if (skip > 0) {
            id = id.substring(skip);
        }

        do {
            String child;
            int slash = id.indexOf('/');
            if (slash >= 0) {
                child = id.substring(0, slash);
                id = id.substring(slash + 1);
            } else {
                child = id;
                id = null;
            }
            int i = 0;
            for (; i < parent.getChildCount(); ++i) {
                if (parent.getChildAt(i).getName().equals(child)) {
                    break;
                }
            }
            if (i == parent.getChildCount()) {
                return null;
            }
            parent = parent.getChildAt(i);
        } while (id != null);

        // sanity check
        if (parent.isFolder()) {
            StrangeEons.log.log(Level.WARNING, "expected .js leaf: {0}", parent);
            return null;
        }
        File f = parent.getFile();

        // already open?
        for (StrangeEonsEditor ed : StrangeEons.getWindow().getEditorsShowingFile(f)) {
            if (ed instanceof CodeEditor) {
                CodeEditor tab = (CodeEditor) ed;
                tab.select();
                return tab.getEditor();
            }
        }

        try {
            CodeEditor ed = new CodeEditor(f, CodeEditor.CodeType.JAVASCRIPT);
            StrangeEons.getWindow().addEditor(ed);
            return ed.getEditor();
        } catch (IOException e) {
            ErrorDialog.displayError(string("prj-err-open", f.getName()), e);
            return null;
        }
    }

    private JSourceCodeEditor findResourceScript(String id) {
        // see if this gets mapped to a project: URL
        if (id.indexOf(':') >= 0) {
            URL url = ResourceKit.composeResourceURL(id);
            if (url != null && "project".equals(url.getProtocol())) {
                return findEditorForIdentifier(url.toExternalForm());
            }
        }

        String script;
        try {
            script = ScriptMonkey.getLibrary(id);
        } catch (Exception e) {
            ErrorDialog.displayError(string("prj-err-open", id), e);
            return null;
        }
        if (script == null) {
            return null;
        }

        // check if this resource file is already being displayed
        StrangeEonsEditor[] editors = StrangeEons.getWindow().getEditors();
        for (int i = 0; i < editors.length; ++i) {
            if (editors[i] instanceof CodeEditor) {
                CodeEditor ed = (CodeEditor) editors[i];
                if (id.equals(ed.getClientProperty("showing"))) {
                    ed.select();
                    return ed.getEditor();
                }
            }
        }

        // create a new read-only editor to display the resource
        CodeEditor ed = new CodeEditor(script, CodeEditor.CodeType.JAVASCRIPT);
        ed.putClientProperty("showing", id);

        String title = id;
        int slash = title.lastIndexOf('/');
        if (slash >= 0) {
            title = title.substring(slash + 1);
        }
        ed.setTitle(title);
        StrangeEons.getWindow().addEditor(ed);

        return ed.getEditor();
    }

    private JSourceCodeEditor findQuickscript() {
        QuickscriptPlugin qs = (QuickscriptPlugin) StrangeEons.getApplication().getLoadedPlugin(QuickscriptPlugin.class.getName());
        if (qs == null) {
            return null;
        }
        if (!qs.isPluginShowing()) {
            qs.showPlugin(PluginContextFactory.createContext(qs, 0), true);
        }
        QuickscriptDialog d = (QuickscriptDialog) qs.getWindow();
        d.toFront();
        return d.getEditor();
    }

    private void showLineInEditor(JSourceCodeEditor ed, int line) {
        if (--line < 0) {
            return; // lines in editor start from 0, but incoming param starts at 1
        }
        int offset = ed.getLineStartOffset(line);
        if (offset < 0) {
            UIManager.getLookAndFeel().provideErrorFeedback(ed);
            return;
        }
        ed.select(offset, offset);
        ed.setFirstDisplayedLine(Math.max(0, line - ed.getDisplayedLineCount() / 2));
        ed.requestFocusInWindow();
    }
}
