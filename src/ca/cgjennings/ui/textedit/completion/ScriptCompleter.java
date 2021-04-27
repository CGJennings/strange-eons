package ca.cgjennings.ui.textedit.completion;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.editors.NavigationPoint;
import ca.cgjennings.apps.arkham.plugins.SEScriptEngineFactory;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.ui.textedit.JSourceCodeEditor;
import static ca.cgjennings.ui.textedit.completion.CompletionUtilities.*;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Icon;
import resources.ResourceKit;

/**
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class ScriptCompleter implements CodeCompleter {

    private static final String COMPLETION_SCRIPT = "res://libraries/completion/main.js";
    private final ProxyBinding proxy;

    private static final int I_PROPOSAL = 0;
    private static final int I_DESCRIPTION = 1;
    private static final int I_ESC_POSITION = 2;
    private static final int I_STYLE = 3;

    /**
     * Creates a new script completer.
     */
    public ScriptCompleter() {
        SEScriptEngineFactory.setWarningReportingEnabled(false);
        SEScriptEngineFactory.setOptimizationLevel(9);
        ScriptMonkey monkey = new ScriptMonkey(COMPLETION_SCRIPT);
        monkey.eval(ResourceKit.composeResourceURL(COMPLETION_SCRIPT));
        proxy = monkey.implement(ProxyBinding.class);
    }

    /**
     * Returns a (possibly empty) set of code completion options for a source
     * file.
     *
     * @param editor the editor in which code completion is being performed
     * @return a set of possible alternatives
     */
    @Override
    public Set<CodeAlternative> getCodeAlternatives(JSourceCodeEditor editor) {
        StrangeEons.getWindow().setWaitCursor();
        try {
            return getCodeAlternativesImpl(editor);
        } finally {
            StrangeEons.getWindow().setDefaultCursor();
        }
    }

    private Set<CodeAlternative> getCodeAlternativesImpl(JSourceCodeEditor editor) {
        String text = editor.getText();
        int offset = editor.getCaretPosition();
        int selStart = editor.getSelectionStart();
        int selEnd = editor.getSelectionEnd();
        final String prefix = prefixOf(text, offset);
        final int prefixLen = prefix.length();

//		System.out.printf("<%s>\n",prefix);
        String composedText = composeFullText(text);
        final int caretDelta = composedText.length() - text.length();
        offset += caretDelta;
        selStart += caretDelta;
        selEnd += caretDelta;
        text = composedText;

        Object[][] results = proxy.getProposals(text, prefix, offset, selStart, selEnd);

        LinkedHashSet<CodeAlternative> set = new LinkedHashSet<>();
        for (int i = 0, max = Math.min(100, results.length); i < max; ++i) {
            if ("".equals(results[i][I_PROPOSAL])) {
                max = Math.min(max + 1, results.length);
                continue;
            }
            set.add(createAlternative(editor, prefixLen, results[i]));
        }

//		System.out.println( results.length + " / " + set.size() );
        return set;
    }

    private static final int MAX_DESC_LEN = 40;
    private static final int MAX_TYPE_LEN = 20;

    /**
     * Convert a raw result from the JS side to a {@link CodeAlternative}.
     */
    private CodeAlternative createAlternative(JSourceCodeEditor editor, int prefixLength, Object[] result) {
        String toInsert = (String) result[I_PROPOSAL];
        toInsert = toInsert.replaceAll("<|>", "").replaceAll("\\(([^)])", "( $1").replaceAll("([^(])\\)", "$1 )");

        String description = (String) result[I_DESCRIPTION];
        String style = (String) result[I_STYLE];
        String type = "";

        Icon icon = null;

        if (description.indexOf('(') >= 0) {
            icon = NavigationPoint.ICON_DIAMOND;
        }

        int typeSplit = description.lastIndexOf(" : ");
        if (typeSplit >= 0) {
            type = description.substring(typeSplit + 3).trim();
            description = description.substring(0, typeSplit).trim();
        }

        if ("undefined".equals(type)) {
            type = "";
        } else if (type.length() > MAX_TYPE_LEN) {
            if (type.startsWith("function(")) {
                type = type.substring(0, MAX_TYPE_LEN - 2) + "…)";
            } else if (type.startsWith("{")) {
                type = type.substring(0, MAX_TYPE_LEN - 2) + "…}";
            } else {
                type = type.substring(0, MAX_TYPE_LEN - 1) + '…';
            }
        }

        if (description.length() > MAX_DESC_LEN) {
            description = description.substring(0, MAX_DESC_LEN - 1) + '…';
        }

        if (icon == null) {
            switch (type) {
                case "Number":
                    icon = NavigationPoint.ICON_CIRCLE;
                    break;
                case "String":
                    icon = NavigationPoint.ICON_TRIANGLE;
                    break;
                case "Boolean":
                    icon = NavigationPoint.ICON_SQUARE;
                    break;
                default:
                    icon = NavigationPoint.ICON_CLUSTER;
                    break;
            }
        }

        final boolean emphasize = "emphasis".equals(style);

        String details = null;
        int parenDepth = 0;
        StringBuilder b = new StringBuilder(description.length() * 4);
        b.append("<html>");
        if (emphasize) {
            b.append("<b>");
        }
        for (int i = 0, len = description.length(); i < len; ++i) {
            char ch = description.charAt(i);
            switch (ch) {
                case '(':
                    b.append('(');
                    if (parenDepth++ == 0) {
                        if (emphasize) {
                            b.append("</b>");
                        }
                        b.append("<font color='#777777'>");
                    }
                    break;
                case ')':
                    if (--parenDepth == 0) {
                        b.append("</font>");
                        if (emphasize) {
                            b.append("<b>");
                        }
                    }
                    b.append(')');
                    break;
                case '<': // <...> = optional parameter
                    b.append("<i>");
                    break;
                case '>':
                    b.append("</i>");
                    break;
                case '\n':
                    details = description.substring(i + 1);
                    i = len;
                    break;
                default:
                    b.append(ch);
                    break;
            }
        }

        return new DefaultCodeAlternative(editor, toInsert, b.toString(), type, icon, 0, false, prefixLength);
    }

    /**
     * Helper interface to let us call into the JS-based completion code.
     */
    static interface ProxyBinding {

        Object[][] getProposals(String fileText, String prefix, int offset, int selStart, int selEnd);
    }

    /**
     * Finds the partially typed prefix at the caret location. For example,
     * given:
     * <pre>object.mem|ber</pre> with the caret at the point indicated by "|",
     * returns "mem".
     *
     * @param text the text of the file containing the caret
     * @param offset the offset of the caret
     */
    private static String prefixOf(String text, int offset) {
        if (offset == 0) {
            return "";
        }

        int start = offset - 1;
        for (; start > 0; --start) {
            char ch = text.charAt(start);

            if (!(isScriptIdentifierPart(ch) || isScriptIdentifierStart(ch))) {
                ++start;
                break;
            }
        }
        // If we never hit a non-identifier char, then we never did ++start
        // and now start == -1; hence the max(0,start)
        return text.substring(Math.max(0, start), offset);
    }

    /**
     * This is a temporary? hack since I haven't gotten assistant.computeSummary
     * to work as expected. It guesses what libraries are used by the script and
     * prepends them so that their content can be parsed.
     *
     * @param text the script text
     * @return the text of all used libraries followed by the original text
     */
    private static String composeFullText(String text) {
        StringBuilder b = new StringBuilder(text.length() + 24 * 1024);
        Set<String> visitedLibs = new HashSet<>();
        composeFullTextImpl(b, visitedLibs, text, "");
        return b.toString();
    }

    private static void composeFullTextImpl(StringBuilder b, Set<String> visitedLibs, String libText, String libName) {
        visitedLibs.add(libName);

        Matcher m = USE_LIBRARY_PATTERN.matcher(libText);
        while (m.find()) {
            String includedLibName = extractLibraryName(m.group(1));
            if (includedLibName.endsWith(".ljs") || visitedLibs.contains(includedLibName)) {
                continue;
            }
            try {
                String includedLibText = ScriptMonkey.getLibrary(includedLibName);
                composeFullTextImpl(b, visitedLibs, includedLibText, includedLibName);
            } catch (Exception e) {
            }
        }

        b.append(libText);
    }

    private static final Pattern USE_LIBRARY_PATTERN = Pattern.compile("^\\s*useLibrary\\(\\s*(.*)$", Pattern.MULTILINE);

    private static String extractLibraryName(String library) {
        if (library.isEmpty()) {
            return library;
        }
        StringBuilder b = new StringBuilder(library.length());
        final char quote = library.charAt(0);
        if (quote != '\'' && quote != '\"') {
            return "";
        }

        boolean escaped = false;
        for (int c = 1; c < library.length(); ++c) {
            char ch = library.charAt(c);
            if (escaped) {
                b.append(ch);
                escaped = false;
                continue;
            }
            switch (ch) {
                case '\\':
                    escaped = true;
                    break;
                case '\'':
                    if (quote == '\'') {
                        c = library.length();
                    } else {
                        b.append(ch);
                    }
                    break;
                case '"':
                    if (quote == '"') {
                        c = library.length();
                    } else {
                        b.append(ch);
                    }
                    break;
                default:
                    b.append(ch);
                    break;
            }
        }
        return b.toString();
    }
}
