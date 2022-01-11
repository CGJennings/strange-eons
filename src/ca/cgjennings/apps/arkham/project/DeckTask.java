package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.editors.CodeEditor;
import ca.cgjennings.ui.dnd.FileDrop;
import ca.cgjennings.ui.textedit.JSourceCodeEditor;
import java.io.File;
import java.util.ArrayList;
import static resources.Language.string;

/**
 * An empty deck task. The main feature is support for automatically creating a
 * deck from the cards in the task.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class DeckTask extends NewTaskType {

    private static Rename.RenameListener renameListener;

    /**
     * The setting key that stores the name of the {@link CopiesList} file for
     * the task.
     */
    public static final String KEY_COPIES_LIST_FILE = "copies-list";

    private static final String DEFAULT_COPIES_FILE_NAME = "copies";

    /**
     * Returns {@code true} if the specified task is a kind that supports copies
     * list files.
     *
     * @param t the task to check
     * @return {@code true} if it can contain copies files
     */
    public static boolean taskCanHaveCopiesList(Task t) {
        if (t != null) {
            String type = t.getSettings().get(Task.KEY_TYPE);
            if (NewTaskType.DECK_TYPE.equals(type) || NewTaskType.FACTORY_TYPE.equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the name of the file that contains the copies list for the task,
     * or {@code null} if this is not a deck or factory task. If the specified
     * task is {@code null}, the default copies file name is returned.
     *
     * @param t the task to check
     * @return the name of the copies list file for the task, or {@code null}
     */
    public static String getCopiesListFileName(Task t) {
        if (t == null) {
            return DEFAULT_COPIES_FILE_NAME;
        }
        if (!taskCanHaveCopiesList(t)) {
            return null;
        }
        return t.getSettings().get(KEY_COPIES_LIST_FILE, DEFAULT_COPIES_FILE_NAME);
    }

    /**
     * Changes the name of the copies list file for this task. This does not
     * change the name of the actual file, but changes the name of the file that
     * project actions will look for to locate the copies list.
     *
     * @param t the task to set the copies list name for
     * @param fileName the new name
     * @throws IllegalArgumentException if the new name has an extension or the
     * task type does not use copies lists
     */
    public static void setCopiesListFileName(Task t, String fileName) {
        if (!taskCanHaveCopiesList(t)) {
            throw new IllegalArgumentException("this task type does not use a copies list");
        }
        if (fileName == null) {
            fileName = DEFAULT_COPIES_FILE_NAME;
        }
        if (fileName.indexOf('.') >= 0) {
            throw new IllegalArgumentException("copies file list cannot have an extension");
        }
        t.getSettings().set(KEY_COPIES_LIST_FILE, fileName);
    }

    /**
     * Returns {@code true} if the given member is a copies list in a task.
     *
     * @param m the member to test
     * @return {@code true} if the member is the copies list file for its task
     */
    public static boolean isCopiesList(Member m) {
        if (m == null) {
            return false;
        }
        if (!m.getExtension().isEmpty()) {
            return false;
        }
        Task t = m.getTask();
        if (t == null) {
            return false;
        }
        // copies files must be in the task root
        if (m.getParent() != t) {
            return false;
        }
        // copies files only used in some task types
        if (!DeckTask.taskCanHaveCopiesList(t)) {
            return false;
        }
        // copies files can be renamed, check task for current name
        return DeckTask.getCopiesListFileName(t).equals(m.getName());
    }

    public DeckTask() {
        if (renameListener == null) {
            renameListener = (Project p, Member newMember, File oldFile, File newFile) -> {
                if (newFile == null) {
                    return; // deleted
                }
                if (newMember != null) {
                    if (newMember instanceof Task) {
                        return;
                    }

                    Task t = newMember.getTask();
                    if (t == null) {
                        return;
                    }

                    // only applies to decks and factories
                    if (!taskCanHaveCopiesList(t)) {
                        return;
                    }

                    // if the old name doesn't actually match the
                    // current copies file, then stop now
                    if (!oldFile.getName().equals(getCopiesListFileName(t))) {
                        return;
                    }

                    // copies files do not have an extension; adding
                    // an extension actually changes the file type
                    String newName = newFile.getName();
                    if (newName.indexOf('.') >= 0) {
                        return;
                    }

                    // copies files must be in the task root; moving the
                    // file elsewhere changes the file type
                    if (newMember.getParent() != t) {
                        return;
                    }

                    // OK, update the file name
                    setCopiesListFileName(t, newName);

                    newMember.synchronize();
                }
            };
            Rename.addRenameListener(renameListener);
        }
    }

    @Override
    public String getLabel() {
        return string("pt-deck-name");
    }

    @Override
    public String getDescription() {
        return string("pt-deck-desc");
    }

    @Override
    public String getType() {
        return NewTaskType.DECK_TYPE;
    }

    @Override
    public boolean initializeNewTask(Project project, Task task) throws Throwable {
        ProjectUtilities.copyResourceToFile("projects/copies", new File(task.getFile(), string("pt-deck-copies-file")));
        return true;
    }

    static class CopiesFileDropListener implements FileDrop.Listener {

        private final JSourceCodeEditor ed;

        public CopiesFileDropListener(CodeEditor editor) {
            ed = editor.getEditor();
        }

        @Override
        public void filesDropped(File[] files) {
            String text = ed.getText();
            ArrayList<String> lines = new ArrayList<>();
            String[] rawLines = text.split("\n");
            for (int i = 0; i < rawLines.length; ++i) {
                lines.add(rawLines[i]);
            }

            int lineOfLastInsert = -1;
            int colOfLastInsert = 0;
            ed.getDocument().beginCompoundEdit();
            try {
                for (int i = 0; i < files.length; ++i) {
                    String name = files[i].getName();
                    name = name.replace("\\", "\\\\").replace(" ", "\\ ").replace(":", "\\:").replace("=", "\\=");
                    String shortName = name;
                    if (name.endsWith(".eon")) {
                        shortName = name.substring(0, name.length() - 4);
                    } else {
                        shortName = name;
                        name += ".eon";
                    }
                    int j;
                    for (j = 0; j < lines.size(); ++j) {
                        String li = lines.get(j);
                        if (li.isEmpty() || li.charAt(0) == '#') {
                            continue;
                        }
                        li = li.trim();
                        if (li.isEmpty() || li.charAt(0) == '#') {
                            continue;
                        }
                        String[] tokens = li.split("\\s*=\\s*");
                        if (tokens.length < 2) {
                            continue;
                        }
                        if (tokens[0].equals(name) || tokens[0].equals(shortName)) {
                            int v = 1;
                            try {
                                v = Integer.parseInt(tokens[1].trim());
                            } catch (NumberFormatException e) {
                            }
                            ++v;
                            lines.set(j, shortName + " = " + v);
                            lineOfLastInsert = j;
                            colOfLastInsert = shortName.length() + 3;
                            break;
                        }
                    }
                    if (j == lines.size()) {
                        lineOfLastInsert = lines.size();
                        colOfLastInsert = shortName.length() + 3;
                        lines.add(shortName + " = 2");
                    }
                }
                StringBuilder b = new StringBuilder(2_048);
                for (int i = 0; i < lines.size(); ++i) {
                    if (i > 0) {
                        b.append('\n');
                    }
                    b.append(lines.get(i));
                }
                if (lineOfLastInsert >= 0) {
                    ed.setText(b.toString());
                    final int start = ed.getLineStartOffset(lineOfLastInsert) + colOfLastInsert;
                    final int end = ed.getLineEndOffset(lineOfLastInsert) + (lineOfLastInsert == ed.getLineCount() - 1 ? 0 : -1);
                    ed.select(start, end);
                    ed.requestFocusInWindow();
                }
            } finally {
                ed.getDocument().endCompoundEdit();
            }
        }
    }
}
