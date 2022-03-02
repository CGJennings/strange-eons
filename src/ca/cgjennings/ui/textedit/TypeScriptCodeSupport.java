package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.plugins.typescript.CompilationFactory;
import ca.cgjennings.apps.arkham.plugins.typescript.CompilationRoot;
import ca.cgjennings.apps.arkham.plugins.typescript.Diagnostic;
import ca.cgjennings.apps.arkham.plugins.typescript.TSLanguageServices;
import java.awt.EventQueue;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import javax.swing.text.BadLocationException;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;
import org.fife.ui.rtextarea.Gutter;

/**
 * Code support for the TypeScript language.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class TypeScriptCodeSupport extends DefaultCodeSupport {

    @Override
    public void install(CodeEditorBase editor) {
        super.install(editor);
        
        // if the TS engine is not already running, start it up now
        // in a background thread since it is likely to be used soon
        TSLanguageServices.getShared();
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
    public Formatter createFormatter() {
        return new ScriptedFormatter("beautify-js.js", "js_beautify");
    }
    
    @Override
    public void fileChanged(File file) {
        this.file = file;
        root = CompilationFactory.forFile(file);
        // TODO set identifier for root
        identifier = "index.ts";
    }
    
    private File file;
    private String identifier;
    private CompilationRoot root;
    
    private class TSParser extends AbstractParser {
        private final CodeEditorBase editor;
        private final DefaultParseResult result = new DefaultParseResult(this);
        
        public TSParser(CodeEditorBase editor) {
            this.editor = editor;
        }
        
        @Override
        public ParseResult parse(RSyntaxDocument rsd, String string) {
            try {
                final long start = System.currentTimeMillis();
                result.setError(null);
                result.clearNotices();

                Gutter gutter = editor.getScrollPane().getGutter();
                gutter.removeAllTrackingIcons();
                
                if (root != null) {
                    root.add(identifier, rsd.getText(0, rsd.getLength()));
                    List<Diagnostic> diagnostics = root.getDiagnostics(identifier, true, true);
                    for(Diagnostic d : diagnostics) {
                        if (d.hasLocation()) {
                            DefaultParserNotice notice = new DefaultParserNotice(
                                    this, d.message, d.line, d.offset, d.length
                            );
                            notice.setLevel(d.isWarning ? ParserNotice.Level.WARNING : ParserNotice.Level.ERROR);
                            gutter.addLineTrackingIcon(d.line, d.isWarning ? CodeEditorBase.ICON_WARNING : CodeEditorBase.ICON_ERROR, d.message);
                            result.addNotice(notice);                            
                        }
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
}
