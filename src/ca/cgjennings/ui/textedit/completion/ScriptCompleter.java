package ca.cgjennings.ui.textedit.completion;

import ca.cgjennings.ui.textedit.JSourceCodeEditor;
import java.util.Collections;
import java.util.Set;

/**
 * Completion for script code.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class ScriptCompleter implements CodeCompleter {

    public ScriptCompleter() {
        // Note: previous implementation was removed;
        // this is now a skeletong that always returns no results
    }

    /**
     * Returns a (possibly empty) set of code completion options for a source
     * file.
     *
     * @param editor the editor in which code completion is being performed
     * @return a set of possible alternatives
     */
    @Override
    public Set<CodeAlternative> getCodeAlternatives(JSourceCodeEditor editor) {
        return Collections.emptySet();

        /*
        StrangeEons.getWindow().setWaitCursor();
        try {
            return getCodeAlternativesImpl(editor);
        } finally {
            StrangeEons.getWindow().setDefaultCursor();
        }
         */
    }
}
