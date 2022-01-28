package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.plugins.engine.SyntaxChecker;
import java.util.logging.Level;
import javax.swing.text.BadLocationException;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * A document parser that highlights syntax errors in Strange Rhino scripts.
 * This adapts the existing {@link SyntaxChecker} class to the code editor's
 * API.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
final class StrangeRhinoParser extends AbstractParser {
    private final CodeEditorBase editor;
    private final SyntaxChecker checker;
    private final DefaultParseResult result;
    
    public StrangeRhinoParser(CodeEditorBase editor) {
        this.editor = editor;
        checker = new SyntaxChecker();
        result = new DefaultParseResult(this);
    }
    
    @Override
    public ParseResult parse(RSyntaxDocument rsd, String string) {
        try {
            final long start = System.currentTimeMillis();
            result.setError(null);
            result.clearNotices();
            
            Gutter gutter = editor.getScrollPane().getGutter();
            gutter.removeAllTrackingIcons();
            
            checker.parse(rsd.getText(0, rsd.getLength()));
            for (SyntaxChecker.SyntaxError err : checker.getErrors()) {
                final int offset = err.offset();                
                final int line = editor.getLineOfOffset(err.offset());
                final boolean warn = err.isWarning();
                DefaultParserNotice notice = new DefaultParserNotice(
                        this,
                        err.message(),
                        line,
                        err.offset(),
                        err.length()
                );
                notice.setLevel(warn ? ParserNotice.Level.WARNING : ParserNotice.Level.ERROR);
                gutter.addLineTrackingIcon(line, warn ? CodeEditorBase.ICON_WARNING : CodeEditorBase.ICON_ERROR, err.message());
                result.addNotice(notice);
            }
            result.setParseTime(System.currentTimeMillis() - start);
        } catch (BadLocationException ble) {
            StrangeEons.log.log(Level.WARNING, ble, null);
            result.setError(ble);
        }
        return result;
    }
}