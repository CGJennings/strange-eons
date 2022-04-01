package ca.cgjennings.apps.arkham.plugins.typescript;

import ca.cgjennings.apps.arkham.EditorAdapter;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.editors.CodeEditor;
import java.io.File;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * A source unit that is stored on disk but may also be open in an editor. If
 * the file is being edited, then the file content will come from the editor.
 * Otherwise it will be updated from the file.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class EditableSourceUnit extends FileSourceUnit {

    private CodeEditor cachedOpenEditor;

    public EditableSourceUnit(String identifier, File loadFrom) {
        super(identifier, loadFrom);
    }

    private CodeEditor getOpenCodeEditorForFile() {
        if (cachedOpenEditor != null) {
            return cachedOpenEditor;
        }

        for (StrangeEonsEditor ed : StrangeEons.getWindow().getEditorsShowingFile(getFile())) {
            if (ed instanceof CodeEditor) {
                cachedOpenEditor = (CodeEditor) ed;
                cachedOpenEditor.addEditorListener(new EditorAdapter() {
                    @Override
                    public void editorClosing(StrangeEonsEditor editor) {
                        cachedOpenEditor = null;
                    }
                });
                return cachedOpenEditor;
            }
        }

        return null;
    }

    @Override
    protected void updateFromSource(String possiblyStaleVersion) {
        // update from the open code editor if it exists and
        // has been modified
        CodeEditor openEditor = getOpenCodeEditorForFile();
        if (openEditor != null) {
            Document doc = openEditor.getEditor().getDocument();
            try {
                update(doc.getText(0, doc.getLength()));
            } catch (BadLocationException ble) {
                // can't happen
            }
        } else {
            // update from the source file instead
            super.updateFromSource(possiblyStaleVersion);
        }
    }
}
