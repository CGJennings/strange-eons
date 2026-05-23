package ca.cgjennings.ui.textedit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

import org.fife.ui.autocomplete.AutoCompletion;

import ca.cgjennings.ui.textedit.PlainTextSupport.WordCompletionProvider;
import resources.ResourceKit;

/**
 * Code support for Markdown.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class MarkdownSupport extends DefaultCodeSupport {
    private AutoCompletion ac;

    @Override
    public void install(CodeEditorBase editor) {
        super.install(editor);
        ac = new AutoCompletion(new WordCompletionProvider());
        ac.install(editor.getTextArea());
    }

    @Override
    public void uninstall(CodeEditorBase editor) {
        super.uninstall(editor);
        ac.uninstall();
        ac = null;
    }


    @Override
    public Navigator createNavigator(NavigationHost host) {
        return new MarkdownNavigator();
    }

    public static class MarkdownNavigator extends RegexNavigatorBase {
        public MarkdownNavigator() {
            super(Pattern.compile("^(##?#?#?) (.*)$", Pattern.MULTILINE));
        }

        @Override
        protected NavigationPoint createNavigationPoint(Matcher m, String sourceText, boolean initialize) {
           final int headingLevel = m.group(1).length() - 1;
           return new NavigationPoint(m.group(2).trim(), null, m.start(), headingLevel, headingIcons[headingLevel]);
        }

        private Icon[] headingIcons = new Icon[] {
            ResourceKit.getIcon("token-h1"),
            ResourceKit.getIcon("token-h2"),
            ResourceKit.getIcon("token-h3"),
            ResourceKit.getIcon("token-h4")
        };
    }
}