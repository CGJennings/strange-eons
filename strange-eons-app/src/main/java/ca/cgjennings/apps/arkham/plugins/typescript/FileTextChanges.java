package ca.cgjennings.apps.arkham.plugins.typescript;

import ca.cgjennings.ui.textedit.CodeEditorBase;
import java.util.List;

/**
 * Describes a series of atomic changes to be made to a file.
 * The file name may be null if the target is implied.
 */
public class FileTextChanges {
    public String fileName;
    public List<TextChange> changes;
    public boolean isNewFile;

    public FileTextChanges(String fileName, boolean isNewFile, List<TextChange> changes) {
        this.fileName = fileName;
        this.isNewFile = isNewFile;
        this.changes = changes;
    }

    public void apply(CodeEditorBase editor) {
        editor.beginCompoundEdit();
        try {
            for (TextChange tc : changes) {
                tc.apply(editor);
            }
        } finally {
            editor.endCompoundEdit();
        }
    }
    
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(fileName);
        if (isNewFile) b.append('*');
        for (TextChange tc : changes) {
            b.append('\n').append(tc);
        }
        return b.toString();
    }
}
