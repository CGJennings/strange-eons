package ca.cgjennings.ui.textedit;

import ca.cgjennings.algo.StaggeredDelay;
import ca.cgjennings.apps.arkham.StrangeEons;
import static ca.cgjennings.ui.textedit.NavigationPoint.*;
import ca.cgjennings.apps.arkham.plugins.typescript.CodeAction;
import ca.cgjennings.apps.arkham.plugins.typescript.CompilationFactory;
import ca.cgjennings.apps.arkham.plugins.typescript.CompilationRoot;
import ca.cgjennings.apps.arkham.plugins.typescript.CompletionInfo;
import ca.cgjennings.apps.arkham.plugins.typescript.Diagnostic;
import ca.cgjennings.apps.arkham.plugins.typescript.EditableSourceUnit;
import ca.cgjennings.apps.arkham.plugins.typescript.FileTextChanges;
import ca.cgjennings.apps.arkham.plugins.typescript.NavigationTree;
import ca.cgjennings.apps.arkham.plugins.typescript.Overview;
import ca.cgjennings.apps.arkham.plugins.typescript.SourceUnit;
import ca.cgjennings.apps.arkham.plugins.typescript.TSLanguageServices;
import ca.cgjennings.ui.theme.Palette;
import ca.cgjennings.text.MarkdownTransformer;
import ca.cgjennings.ui.IconProvider;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Segment;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.CompletionProviderBase;
import org.fife.ui.autocomplete.ParameterizedCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.ToolTipSupplier;

/**
 * Code support for the TypeScript language.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class TypeScriptCodeSupport extends DefaultCodeSupport {

    private String createStyleTag(Font codeFont) {
        String family = "font-family: \"" + codeFont.getFamily().replace("\"", "\\\"") + "\";";
        String size = "font-size: " + codeFont.getSize2D() + "pt;";
        String bold = "font-weight: bold;";
        String blue = "color: #" + Palette.get.foreground.opaque.blue + ';';
        String green = "color: #" + Palette.get.foreground.opaque.green + ';';
        String margin0 = "margin: 0;";
        
        String style =  "<html><style>" +
                "pre, code {" + family + size + '}' +
                ".head {" + margin0 + bold + blue + '}' +
                ".param {" + bold + green + '}' +
                "</style>";
        return style;
    }
    
    @Override
    public void install(CodeEditorBase editor) {
        super.install(editor);
        this.editor = editor;

        // if the TS engine is not already running, start it up now
        // in a background thread since it is likely to be used soon
        TSLanguageServices.getShared();
        
        markdown = new MarkdownTransformer();
        markdown.setDefaultCodeBlockLanguage("ts");
        
        styleTag = createStyleTag(editor.getTextArea().getFont());
        
        ac = new AutoCompletion(new TSCompletionProvider());
        ac.setListCellRenderer(new CompletionRenderer(editor));
        ac.setShowDescWindow(true);
        ac.setAutoCompleteEnabled(true);
        ac.setAutoActivationEnabled(true);
        ac.setAutoActivationDelay(800);
        ac.setAutoCompleteSingleChoices(true);
        ac.install(editor.getTextArea());
        
        editor.getTextArea().setToolTipSupplier(new TSToolTips());
        
        EventQueue.invokeLater(() -> {
            // don't immediately create a root, delay a moment in case a file
            // is set just after; this prevents superfluous creation of
            // a potentially expensive compilation root
            if (root == null) {
                fileChanged(file);
            }
        });
        editor.getTextArea().addParser(new TSParser(editor));
    }

    @Override
    public void uninstall(CodeEditorBase editor) {
        super.uninstall(editor);
        if (ac != null) {
            ac.uninstall();
            ac = null;
        }
        
        editor.getTextArea().setToolTipSupplier(null);
    }

    @Override
    public Formatter createFormatter() {
        return new ScriptedFormatter("beautify-js.js", "js_beautify");
    }

    @Override
    public void fileChanged(File file) {
        this.file = file;
        root = CompilationFactory.forFile(file);
        fileName = root.getSuggestedIdentifier(file);
        if (root != null && root.get(fileName) == null) {
            root.add(new EditableSourceUnit(fileName, file));
        }
    }

    private CodeEditorBase editor;
    private File file;
    private String fileName;
    private CompilationRoot root;
    private AutoCompletion ac;

    private class TSToolTips implements ToolTipSupplier {
        @Override
        public String getToolTipText(RTextArea textArea, MouseEvent mouseEvent) {
            int offset = textArea.viewToModel2D(mouseEvent.getPoint());
            String tip = null;
            if (root != null) {
                Overview ov = root.getOverview(fileName, offset);
                if (ov != null) {
                    tip = ov.toMarkup(markdown, styleTag);
                }
            }
            return tip;
        }
    };
    
    private class TSParser extends AbstractParser {

        private final CodeEditorBase editor;
        private final DefaultParseResult result = new DefaultParseResult(this);
        
        public TSParser(CodeEditorBase editor) {
            this.editor = editor;
        }

        @Override
        public ParseResult parse(RSyntaxDocument rsd, String string) {
            if (root == null || !TSLanguageServices.getShared().isLoaded()) {
                // services not loaded or root not available yet;
                // retry parsing later rather than blocking UI
                StaggeredDelay.then(4000, ()->editor.getTextArea().forceReparsing(this));
                return result;
            }

            // there are results available, update the editor
            try {
                final long start = System.currentTimeMillis();
                result.setError(null);
                result.clearNotices();

                Gutter gutter = editor.getScrollPane().getGutter();
                gutter.removeAllTrackingIcons();

                List<Diagnostic> diagnostics = root.getDiagnostics(fileName, true, true);
                for (Diagnostic d : diagnostics) {
                    if (d.hasLocation()) {
                        DefaultParserNotice notice = new DefaultParserNotice(
                                this, d.message, d.line, d.offset, d.length
                        );
                        notice.setLevel(d.isWarning ? ParserNotice.Level.WARNING : ParserNotice.Level.ERROR);
                        gutter.addLineTrackingIcon(d.line, d.isWarning ? CodeEditorBase.ICON_WARNING : CodeEditorBase.ICON_ERROR, d.message);
                        result.addNotice(notice);
                    }
                }
                result.setParseTime(System.currentTimeMillis() - start);
            } catch (BadLocationException ble) {
                StrangeEons.log.log(Level.WARNING, ble, null);
                result.setError(ble);
            }
            return result;
        }
    }

    private class TSCompletionProvider extends CompletionProviderBase {

        public TSCompletionProvider() {
            setAutoActivationRules(true, ".");
        }

        @Override
        public List<Completion> getCompletions(JTextComponent comp) {
            return getCompletionsImpl(comp);
        }

        @Override
        protected List<Completion> getCompletionsImpl(JTextComponent jtc) {
            if (root == null || fileName == null) {
                return Collections.emptyList();
            }

            String text = jtc.getText();
            SourceUnit source = root.get(fileName);
//            if (source != null) {
//                source.update(text);
//            }
            
            final String prefix = getAlreadyEnteredText(jtc);
            final int caret = jtc.getCaretPosition();
            
            CompletionInfo info = root.getCodeCompletions(fileName, caret);
            if (info == null) {
                return Collections.emptyList();
            }
            List<Completion> results = new ArrayList<>(info.entries.size());
            for (CompletionInfo.Entry entry : info.entries) {
                if (entry.getTextToInsert().startsWith(prefix)) {
                    TSCompletion tc = createTSCompletion(entry, this, fileName, caret, prefix);
                    if (tc != null) {
                        results.add(tc);
                    }
                }
            }
            return results;
        }

        @Override
        public String getAlreadyEnteredText(JTextComponent comp) {
            Document doc = comp.getDocument();

            int dot = comp.getCaretPosition();
            Element root = doc.getDefaultRootElement();
            int index = root.getElementIndex(dot);
            Element elem = root.getElement(index);
            int start = elem.getStartOffset();
            int len = dot - start;
            try {
                doc.getText(start, len, seg);
            } catch (BadLocationException ble) {
                ble.printStackTrace();
                return EMPTY_STRING;
            }

            int segEnd = seg.offset + len;
            start = segEnd - 1;
            while (start >= seg.offset && isValidChar(seg.array[start])) {
                start--;
            }
            start++;

            len = segEnd - start;
            return len == 0 ? EMPTY_STRING : new String(seg.array, start, len);

        }
        private Segment seg = new Segment();

        private boolean isValidChar(char ch) {
            return Character.isJavaIdentifierPart(ch);
        }

        @Override
        public List<Completion> getCompletionsAt(JTextComponent jtc, Point point) {
            return Collections.emptyList();
        }

        @Override
        public List<ParameterizedCompletion> getParameterizedCompletions(JTextComponent jtc) {
            return Collections.emptyList();
        }
    }

    private TSCompletion createTSCompletion(CompletionInfo.Entry source, CompletionProvider provider, String fileName, int position, String prefix) {
        TSCompletion comp = null;

        if (source.insertText == null) {
            if (source.name != null && source.name.startsWith(prefix)) {
                comp = new TSCompletion();
            }
        } else if (source.hasAction) {
            comp = new TSCompletion();
            System.out.println(comp.toString());
        } else {
            System.out.println("other?");
        }

        // fill in common basic values --- avoids need for long constructor
        if (comp != null) {
            comp.provider = provider;
            comp.source = source;
            comp.fileName = fileName;
            comp.position = position;
            comp.prefix = prefix;
        }

        return comp;
    }

    /**
     * Base class for TypeScript completions.
     */
    private class TSCompletion implements Completion, IconProvider, SecondaryTextProvider {

        private CompletionProvider provider;
        private CompletionInfo.Entry source;
        private String fileName;
        private int position;
        private String prefix;

        public TSCompletion() {
        }

        @Override
        public int compareTo(Completion rhs) {
            // completions are already sorted
            return 0;
        }

        @Override
        public String getAlreadyEntered(JTextComponent jtc) {
            return prefix;
        }

        @Override
        public Icon getIcon() {
            return TypeScriptCodeSupport.iconForKind(source.kind, source.kindModifiers);
        }

        @Override
        public String getInputText() {
            return "";
        }

        @Override
        public CompletionProvider getProvider() {
            return provider;
        }

        @Override
        public int getRelevance() {
            return 0;
        }

        @Override
        public String getReplacementText() {
            return source.getTextToInsert();
        }

        @Override
        public String getSummary() {
            if (root != null) {
                CompletionInfo.EntryDetails details = root.getCodeCompletionDetails(fileName, position, source);
                if (details != null) {
                    if (details.actions != null) {
                        for (int i = 0; i < details.actions.size(); ++i) {
                            System.out.println("ACTION #" + (i + 1));
                            CodeAction action = details.actions.get(i);
                            System.out.println(action.description);
                            for (FileTextChanges changes : action.changes) {
                                System.out.println(changes);
                            }
                        }
                    }
                    
                    return details.toMarkup(markdown, styleTag);
                }
            }
            return null;
        }

        @Override
        public String getToolTipText() {
            return null;
        }

        @Override
        public String getText() {
            return source.name;
        }

        @Override
        public String getSecondaryText() {
            return source.kind;
        }

        @Override
        public String toString() {
            String s = source.name + " — ";
            if (source.kindModifiers != null && !source.kindModifiers.isEmpty()) {
                s += source.kindModifiers + ' ' + source.kind;
            } else {
                s += source.kind;
            }
            return s;
        }
    }

    @Override
    public Navigator createNavigator(NavigationHost host) {
        return new Navigator() {
            private final List<NavigationPoint> points = new ArrayList<>(32);
            private NavigationTree latestRequest;
            private int requestNumber = 0;

            @Override
            public List<NavigationPoint> getNavigationPoints(String sourceText) {
                if (!TSLanguageServices.getShared().isLoaded()) {
                    return ASYNC_RETRY;
                }
                
                if (latestRequest == null || root == null) {
                    if (root != null) {
                        final int expectedRequest = ++requestNumber;
                        root.add(fileName, sourceText);
                            root.getNavigationTree(fileName, (tree) -> {
                            if (expectedRequest == requestNumber) {
                                latestRequest = tree;
                                host.refreshNavigator();
                            }
                            });
                    } else {
                        // compilation root does not exist yet, check again later
                        return ASYNC_RETRY;
                    }
                    return points;
                }

                try {
                    points.clear();
                    addPoints(points, latestRequest, -1);
                    Collections.sort(points);
                } finally {
                    latestRequest = null;
                }
                return points;
            }

            private void addPoints(List<NavigationPoint> points, NavigationTree tree, int level) {
                if (level >= 0 && tree.location != null) {
                    String longDesc = tree.kind + ' ' + tree.name;
                    if (tree.kindModifiers != null) {
                        longDesc = tree.kindModifiers.replace(',', ' ') + ' ' + longDesc;
                    }
                    NavigationPoint nav = new NavigationPoint(tree.name, longDesc, tree.location.start, level, iconForKind(tree.kind, tree.kindModifiers));
                    points.add(nav);
                }
                if (tree.children != null) {
                    ++level;
                    final int len = tree.children.size();
                    for (int i = 0; i < len; ++i) {
                        addPoints(points, tree.children.get(i), level);
                    }
                }
            }
        };
    }

    static Icon iconForKind(String kind, String kindModifiers) {
        Icon icon;
        boolean isStatic = kindModifiers != null && kindModifiers.contains("static");
        switch (kind) {
            case "keyword":
                icon = ICON_KEYWORD;
                break;
            case "module":
            case "external module name":
                icon = ICON_MODULE;
                break;
            case "class":
            case "local class":
                icon = ICON_CLASS;
                break;
            case "interface":
                icon = ICON_INTERFACE;
                break;
            case "enum":
                icon = ICON_ENUM;
                break;
            case "enum member":
                icon = ICON_ENUM_MEMBER;
                break;
            case "var":
            case "local var":
                icon = ICON_VAR;
                break;
            case "let":
                icon = ICON_LET;
                break;
            case "const":
                icon = ICON_CONST;
                break;
            case "function":
            case "local function":
                icon = ICON_FUNCTION;
                break;
            case "method":
            case "constructor":
                icon = ICON_METHOD;
                break;
            case "getter":
                icon = ICON_GETTER;
                break;
            case "setter":
                icon = ICON_SETTER;
                break;
            case "property":
                icon = ICON_PROPERTY;
                break;
            case "type":
                icon = ICON_TYPE;
                break;
            case "alias":
                icon = ICON_ALIAS;
                break;
            case "primitive type":
                icon = ICON_PRIMITIVE;
                break;
            case "call":
            case "construct":
                icon = ICON_CALL;
                break;
            case "index":
                icon = ICON_INDEX;
                break;
            case "parameter":
                icon = ICON_PARAMETER;
                break;
            case "type parameter":
                icon = ICON_TYPE_PARAMETER;
                break;
            case "label":
                icon = ICON_LABEL;
                break;
            case "directory":
                icon = ICON_DIRECTORY;
                break;
            // "JSX attribute", "string", "link", "link name", "link text"
            default:
                icon = ICON_NONE;
                break;
        }
        return icon;
    }
    
    private String styleTag;
    private MarkdownTransformer markdown;
    
    
    public static void main(String[] args) {
        EventQueue.invokeLater(()->{
            javax.swing.JFrame f = new javax.swing.JFrame();
            f.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            javax.swing.JEditorPane ed = new javax.swing.JEditorPane("text/html", "");
            javax.swing.JTextArea ta = new javax.swing.JTextArea();
            ta.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    EventQueue.invokeLater(this::update);
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    EventQueue.invokeLater(this::update);
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    EventQueue.invokeLater(this::update);
                }
                
                public void update() {
                    ed.setText(ta.getText());
                }
            });
            f.setLayout(new BoxLayout(f.getContentPane(), BoxLayout.LINE_AXIS));
            f.getContentPane().add(ta);
            f.getContentPane().add(ed);
            f.setSize(800,800);
            f.pack();
            f.setVisible(true);
        });
    }
}
