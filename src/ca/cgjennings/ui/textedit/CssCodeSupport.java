package ca.cgjennings.ui.textedit;

import org.fife.rsta.ac.css.CssLanguageSupport;

/**
 * Code support for CSS markup.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class CssCodeSupport extends DefaultCodeSupport {

    private CssLanguageSupport ls;

    @Override
    public void install(CodeEditorBase editor) {
        super.install(editor);
        ls = new CssLanguageSupport();
        ls.install(editor.getTextArea());
    }

    @Override
    public void uninstall(CodeEditorBase editor) {
        ls.uninstall(editor.getTextArea());
        super.uninstall(editor);
    }

    @Override
    public Formatter createFormatter() {
        return new ScriptedFormatter("beautify-css.js", "css_beautify");
    }
}
