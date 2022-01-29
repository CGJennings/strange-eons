package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.editors.CodeEditor;
import ca.cgjennings.apps.arkham.editors.Navigator;
import ca.cgjennings.apps.arkham.editors.ResourceFileNavigator;

/**
 * Code support for game resource files (class maps, tiles, and so on).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class ResourceFileCodeSupport extends PropertyFileCodeSupport {

    @Override
    public Navigator createNavigator(CodeEditor codeEditor) {
        return new ResourceFileNavigator();
    }
}
