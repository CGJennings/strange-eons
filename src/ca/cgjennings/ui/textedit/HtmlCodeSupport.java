package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.editors.CodeEditor;
import ca.cgjennings.apps.arkham.editors.HtmlNavigator;
import ca.cgjennings.apps.arkham.editors.Navigator;
import org.fife.rsta.ac.html.HtmlLanguageSupport;

/**
 * Code support for HTML markup.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class HtmlCodeSupport extends DefaultCodeSupport {

    private HtmlLanguageSupport ls;

    @Override
    public void install(CodeEditorBase editor) {
        super.install(editor);
        ls = new HtmlLanguageSupport();
        ls.install(editor.getTextArea());
    }

    @Override
    public void uninstall(CodeEditorBase editor) {
        ls.uninstall(editor.getTextArea());
        super.uninstall(editor);
    }

    @Override
    public Navigator createNavigator(CodeEditor codeEditor) {
        return new HtmlNavigator();
    }
    
    @Override
    public Formatter createFormatter() {
        return new ScriptedFormatter("beautify-html.js", "html_beautify");
    }
}
