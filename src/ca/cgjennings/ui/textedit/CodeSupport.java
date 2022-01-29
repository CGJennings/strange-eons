package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.editors.CodeEditor;
import ca.cgjennings.apps.arkham.editors.Navigator;

/**
 * Classes that provide additional editing support for a code type.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public interface CodeSupport {

    /**
     * Adds support for a code type to the editor.
     */
    void install(CodeEditorBase editor);

    /**
     * Removes the previously added support.
     */
    void uninstall(CodeEditorBase editor);

    /**
     * Creates a suitable navigator panel for the specified code editor.
     * 
     * @return the new navigator suited to the supported code type, or null if
     * none is supported.
     */
    Navigator createNavigator(CodeEditor editor);
    
    /**
     * Creates a suitable code formatter for the specified code type.
     * 
     * @return a formatter that tidies source code, or null if none is available
     */
    Formatter createFormatter();
}
