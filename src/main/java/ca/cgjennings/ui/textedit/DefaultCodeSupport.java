package ca.cgjennings.ui.textedit;

import java.io.File;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;

/**
 * Default code support; adds basic syntax highlighting based on the editor's
 * code type.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class DefaultCodeSupport implements CodeSupport {
    private static final String SYNTAX_SE_JAVASCRIPT = "text/sejavascript";
    private static final String SYNTAX_RESOURCE_FILE = "text/resourcefile";
    static {
        // register our custom tokenizers
        AbstractTokenMakerFactory factory = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        factory.putMapping(SYNTAX_SE_JAVASCRIPT, SEJavaScriptTokenMaker.class.getName());
        factory.putMapping(SYNTAX_RESOURCE_FILE, ResourceFileTokenMaker.class.getName());
    }

    @Override
    public void install(CodeEditorBase editor) {
        final SyntaxTextArea ta = editor.getTextArea();

        String id = languageIdFor(editor);
        if (id == null) {
            id = SyntaxConstants.SYNTAX_STYLE_NONE;
        }

        ta.clearParsers();
        ta.setSyntaxEditingStyle(id);
    }

    @Override
    public void uninstall(CodeEditorBase editor) {
        final SyntaxTextArea ta = editor.getTextArea();
        ta.clearParsers();
        ta.setSyntaxEditingStyle(null);
    }

    @Override
    public Navigator createNavigator(NavigationHost host) {
        return null;
    }

    @Override
    public Formatter createFormatter() {
        return null;
    }
    
    @Override
    public void fileChanged(File file) {
    }

    /**
     * Returns a syntax area language ID for the specified editor. The default
     * implementation returns a value based on
     * {@linkplain CodeEditorBase#getCodeType() the editor's code type}.
     *
     * @param editor the editor to determine a language ID for
     * @return a language ID for the editor; may return null for plain text
     */
    protected String languageIdFor(CodeEditorBase editor) {
        return editor == null ? null : languageIdFor(editor.getCodeType());
    }

    /**
     * Returns the default syntax area language ID for a code type.
     * 
     * @param type the code type to return a syntax ID for
     * @return returns an ID for the type; if the type is null or unknown,
     * returns an ID for plain text
     */
    static String languageIdFor(CodeType type) {
        String id;
        type = type == null ? CodeType.PLAIN : type;
        switch (type.normalize()) {
            case JAVA:
                id = SyntaxConstants.SYNTAX_STYLE_JAVA;
                break;
            case JAVASCRIPT:
                id = SYNTAX_SE_JAVASCRIPT;
                break;
            case TYPESCRIPT:
                id = SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT;
                break;
            case HTML:
                id = SyntaxConstants.SYNTAX_STYLE_HTML;
                break;
            case CSS:
                id = SyntaxConstants.SYNTAX_STYLE_CSS;
                break;
            case SETTINGS:
            case PROPERTIES:
                id = SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE;
                break;
            case CLASS_MAP:
            case CONVERSION_MAP:
            case SILHOUETTES:
            case TILES:
                id = SYNTAX_RESOURCE_FILE;
                break;
            case MARKDOWN:
                id = SyntaxConstants.SYNTAX_STYLE_MARKDOWN;
                break;
            default:
                id = SyntaxConstants.SYNTAX_STYLE_NONE;
                break;
        }
        return id;
    }
}
