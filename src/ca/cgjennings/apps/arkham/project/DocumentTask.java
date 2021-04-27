package ca.cgjennings.apps.arkham.project;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.Icon;
import static resources.Language.string;

/**
 * A task for storing project documentation or other text.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class DocumentTask extends NewTaskType {

    @Override
    public String getLabel() {
        return string("pt-doc-name");
    }

    @Override
    public String getDescription() {
        return string("pt-doc-desc");
    }

    @Override
    public String getType() {
        return NewTaskType.DOCUMENTATION_TYPE;
    }

    @Override
    public boolean initializeNewTask(Project project, Task task) {
        NewDocumentDialog ndd = new NewDocumentDialog(getDocumentTypes(), task);
        project.getView().moveToLocusOfAttention(ndd);
        return ndd.showDialog();
    }

    /**
     * Registers support for a new kind of document to document tasks.
     *
     * @param ta a task action that will create a new blank document in the
     * desired format
     * @see DocumentType
     */
    public static void registerDocumentType(TaskAction ta) {
        types.add(ta);
    }

    /**
     * Unregisters support for a previously registered document type.
     *
     * @param ta the previously registered action
     */
    public static void unregisterDocumentType(TaskAction ta) {
        types.remove(ta);
    }

    private static Set<TaskAction> types = new TreeSet<>((TaskAction o1, TaskAction o2) -> o1.getLabel().compareTo(o2.getLabel()));

    /**
     * Returns an array of the registered document types.
     *
     * @return an array of task actions that create supported document types
     */
    public static TaskAction[] getDocumentTypes() {
        return types.toArray(new New.NewAction[types.size()]);
    }

    static {
        registerDocumentType(new DocumentType(string("pa-new-rtf") + "...", "rtf", "projects/doc-template.rtf", MetadataSource.ICON_DOCUMENT));
        registerDocumentType(new DocumentType(string("pa-new-odt") + "...", "odt", "projects/doc-template.odt", MetadataSource.ICON_DOCUMENT));
        registerDocumentType(new New.NewAction(string("pa-new-html") + "...", string("pa-new-doc-name"), "html", "projects/doc-template.html", MetadataSource.ICON_HTML));
        registerDocumentType(new DocumentType(string("pa-new-text"), "txt", "projects/doc-template.txt", MetadataSource.ICON_DOCUMENT));
    }

    /**
     * A task action that creates a new, blank document for the
     * {@link DocumentTask} by copying a template file from a resource.
     */
    public static class DocumentType extends New.NewAction {

        public DocumentType(String documentTypeName, String documentTypeExtension, String templateResource, Icon documentIcon) {
            super(fixLabel(documentTypeName), string("pa-new-doc-name"), documentTypeExtension, templateResource, documentIcon, NewTaskType.DOCUMENTATION_TYPE);
        }

        private static String fixLabel(String label) {
            if (label == null) {
                throw new NullPointerException("documentTypeName");
            }
            if (!label.endsWith("...")) {
                label += "...";
            }
            return label;
        }
    }
}
