package ca.cgjennings.ui.textedit;

import java.io.File;

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
    Navigator createNavigator(NavigationHost host);
    
    /**
     * Creates a suitable code formatter for the specified code type.
     * 
     * @return a formatter that tidies source code, or null if none is available
     */
    Formatter createFormatter();
    
    /**
     * Called when the file associated with the editor changes.
     * 
     * @param file the file now associated with the editor
     */
    void fileChanged(File file);
}
