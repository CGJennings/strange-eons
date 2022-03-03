package ca.cgjennings.apps.arkham.plugins.typescript;

import java.util.List;

/**
 * Encapsulates a description and set of changes to perform that will
 * complete a complex editing operation.
 */
public class CodeAction {
    public String description;
    public List<FileTextChanges> changes;
    private Object data;
    
    /**
     * Creates a code action with the specified description and changes, as well
     * as the original TS language service CodeAction object.
     * If the changes are applied, the language service should be notified
     * by calling {@link #applyAction()}.
     * 
     * @param description the text description
     * @param changes the changes to apply
     * @param data a token set by {@link TSLanguageServices} to support
     *    {@link #applyAction}
     */
    public CodeAction(String description, List<FileTextChanges> changes, Object data) {
        this.description = description;
        this.changes = changes;
        this.data = data;
    }
}
