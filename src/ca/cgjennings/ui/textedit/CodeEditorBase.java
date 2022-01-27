package ca.cgjennings.ui.textedit;

import java.awt.BorderLayout;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * This is the base class for all code editing controls used in Strange Eons.
 * 
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class CodeEditorBase extends JPanel {
    private RSyntaxTextArea textArea;
    private RTextScrollPane scroll;
    private CodeType type;
    
    /**
     * Creates a new code editor control.
     */
    public CodeEditorBase() {
        super(new BorderLayout());
        textArea = new RSyntaxTextArea();
        scroll = new RTextScrollPane(textArea);
        textArea.setFractionalFontMetricsEnabled(true);
        textArea.setCodeFoldingEnabled(true);
        add(scroll);
    }
    
    public void setCodeType(CodeType type) {
        if (type == null) {
            type = CodeType.PLAIN;
        }
        this.type = type;
        textArea.setSyntaxEditingStyle(type.getLanguageIdentifier());
    }
    
    /**
     * Sets the contents of the editor.
     * @param text the new text to edit
     */
    public void setText(String text) {
        textArea.setText(text);
    }
    
    /**
     * Returns the contents of the editor.
     * @return the edited text
     */
    public String getText() {
        return textArea.getText();
    }
}
