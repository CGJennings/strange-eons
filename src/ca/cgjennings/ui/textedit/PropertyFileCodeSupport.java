package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.editors.CodeEditor;
import ca.cgjennings.apps.arkham.editors.Navigator;
import ca.cgjennings.apps.arkham.editors.PropertyNavigator;

/**
 * Code support for the Java language.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class PropertyFileCodeSupport extends DefaultCodeSupport {

    @Override
    public Navigator createNavigator(CodeEditor codeEditor) {
        return new PropertyNavigator();
    }
}
