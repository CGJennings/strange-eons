package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.io.IOException;
import java.util.logging.Level;
import java.util.regex.Pattern;
import static resources.Language.string;
import resources.Settings;

/**
 * Task action that deletes build products. This task action can be used with
 * custom task types. It is applicable to any task folder that defines the key
 * with the name {@link #KEY_CLEAN_EXTENSIONS} ("clean-ext"). The value of this
 * key should be set to a comma-separated list of the file extensions of all
 * file types that should be deleted by the action. For example, the value
 * "class,jar" would delete all compiled Java class files and JAR archives.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class Clean extends TaskAction {

    @Override
    public String getLabel() {
        return string("pa-clean");
    }

    @Override
    public String getDescription() {
        return string("pa-clean-tt");
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        if (task == null || member != null) {
            return false;
        }

        Settings s = task.getSettings();
        String extlist = s.get(KEY_CLEAN_EXTENSIONS);

        if (extlist == null) {
            return false;
        }

        String[] exts = SPLITTER.split(extlist);

        clean(task, exts, false);

        return true;
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        if (task == null || member != null) {
            return false;
        }

        Settings s = task.getSettings();
        String exts = s.get(KEY_CLEAN_EXTENSIONS);
        return exts != null;
    }

    /**
     * Returns {@code true} if the clean command should delete a particular
     * file or folder. The base class implementation returns {@code true}
     * when the member is not a folder and its extension matches one of those
     * that is passed in. Subclasses can override this to create customized
     * clean actions for a specific task. (If the new action uses
     * {@link #KEY_CLEAN_EXTENSIONS}, then you should extend the original
     * command as a {@link SpecializedAction} to avoid having both clean
     * commands match a task.)
     *
     * @param m the candidate for deletion
     * @param extensions an array of file extensions that may be used to assist
     * in decision-making
     * @return {@code true} if {@code m} should be deleted by a clean
     * operation
     */
    protected boolean willDelete(Member m, String[] extensions) {
        return ProjectUtilities.matchExtension(m.getFile(), extensions);
    }

    private void clean(Member parent, String[] extensions, boolean force) {
        boolean deletedSomething = false;
        for (Member kid : parent.getChildren()) {
            if (kid.isFolder()) {
                // if the folder should be deleted, recursively force deletion
                // of all children first; otherwise just scan for cleanable files
                if (force || willDelete(kid, extensions)) {
                    clean(kid, extensions, true);
                } else {
                    clean(kid, extensions, force);
                }
            }
            if (force || willDelete(kid, extensions)) {
                try {
                    kid.deleteFile();
                    deletedSomething |= true;
                } catch (IOException e) {
                    StrangeEons.log.log(Level.WARNING, "failed to clean " + kid.getName(), e);
                }
            }
        }
        if (deletedSomething) {
            parent.synchronize();
        }
    }

    private static final Pattern SPLITTER = Pattern.compile(",");

    /**
     * Set the key with this name on a task's settings to a comma-separated list
     * of file extensions and the clean command will automatically apply to that
     * task and delete the files with those extensions.
     *
     * @see Task#getSettings()
     */
    public static final String KEY_CLEAN_EXTENSIONS = "clean-ext";
}
