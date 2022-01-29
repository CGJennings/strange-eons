package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.editors.CodeEditor;
import ca.cgjennings.apps.arkham.editors.Navigator;

/**
 * Code support for the TypeScript language.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class TypeScriptCodeSupport extends ScriptCodeSupport {

    @Override
    public Navigator createNavigator(CodeEditor codeEditor) {
        return null;
    }
}
