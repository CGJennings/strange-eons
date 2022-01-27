package ca.cgjennings.ui.textedit;
import java.awt.*;
import javax.swing.*;
import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;

/**
 * A simple example showing how to use RSyntaxTextArea to add Java syntax
 * highlighting to a Swing application.<p>
 *
 * This example uses RSyntaxTextArea 3.1.4.
 */
public class Test extends JFrame {

    public Test() {

        CodeEditorBase cp = new CodeEditorBase();
        
        

//        RSyntaxTextArea textArea = new RSyntaxTextArea(20, 60);
//        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
//        textArea.setCodeFoldingEnabled(true);
//        RTextScrollPane sp = new RTextScrollPane(textArea);
//        cp.add(sp);

        cp.setPreferredSize(new Dimension(800,600));
        cp.setCodeType(CodeType.JAVASCRIPT);
        setContentPane(cp);
        setTitle("Text Editor Demo");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        pack();
        setLocationByPlatform(true);

    }

    public static void main(String[] args) {
        // Start all Swing applications on the EDT.
        SwingUtilities.invokeLater(() -> new Test().setVisible(true));
    }

}
