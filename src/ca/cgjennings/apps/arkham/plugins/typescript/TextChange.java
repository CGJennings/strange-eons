package ca.cgjennings.apps.arkham.plugins.typescript;

import ca.cgjennings.ui.textedit.CodeEditorBase;

/**
 * Describes a single contiguous text edit as a range and replacement string.
 * This is used to describe the changes that should be made to a source file.
 * 
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
public class TextChange extends TextSpan {
    /** The text to replace the text span with. */
    public String newText;
    
    public TextChange(int start, int length, String newText) {
        super(start, length);
        this.newText = newText == null ? "" : newText;
    }
    
    /** Applies the change to an open editor. */
    public void apply(CodeEditorBase editor) {
        editor.replaceRange(newText, start, start + length);
    }

    /** Applies the change to a string containing the original text. */
    public String apply(String text) {
        return text.substring(0, start) + newText + text.substring(start + length);
    }

    @Override
    public String toString() {
        String desc;
        if (length == 0) {
            desc = "INSERT (" + start + ", \"" + newText + "\')";
        } else if (newText.isEmpty()) {
            desc = "DELETE (" + start + '-' + (start+length) + ')';
        } else {
            desc = "CHANGE (" + start + '-' + (start+length) + ", \"" + newText + "\')";
        }
        return desc;
    }
}
