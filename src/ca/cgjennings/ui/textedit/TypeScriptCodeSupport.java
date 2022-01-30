package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.plugins.typescript.TypeScript;

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
        TypeScript.warmUp();
    }
    
    @Override
    public Formatter createFormatter() {
        return new ScriptedFormatter("beautify-js.js", "js_beautify");
    }
}
