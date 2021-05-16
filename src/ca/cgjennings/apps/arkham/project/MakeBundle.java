package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.BusyDialog;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import ca.cgjennings.apps.arkham.plugins.PluginRoot;
import ca.cgjennings.apps.arkham.plugins.catalog.CatalogID;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.zip.ZipOutputStream;
import javax.swing.JOptionPane;
import static resources.Language.string;
import resources.Settings;

/**
 * Task action that creates a plug-in bundle from the contents of a plug-in task
 * folder. The bundle is written to the project folder using the name stored in
 * the task's {@link #KEY_BUNDLE_FILE} ({@code bundle-file}). If this key
 * is not defined, then <tt>bundle.seplugin</tt> is used as a base.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class MakeBundle extends TaskAction {

    private static Rename.RenameListener renameListener;

    public MakeBundle() {
        if (renameListener == null) {
            renameListener = (Project p, Member newMember, File oldFile, File newFile) -> {
                if (newFile == null) {
                    return; // deleted
                }
                if (!ProjectUtilities.matchExtension(oldFile, EXTENSIONS)) {
                    return;
                }
                
                // is a move/delete, not a simple rename
                if (newMember == null) {
                    return;
                }
                
                String name = oldFile.getName();
                
                // find tasks that share this file's parent, and check
                // their bundle extension key to update the old bundle name
                Member parent = newMember.getParent();
                if (parent == null) {
                    return;
                }
                
                for (int i = 0; i < parent.getChildCount(); ++i) {
                    Member child = parent.getChildAt(i);
                    if (!(child instanceof Task)) {
                        return;
                    }
                    Task t = (Task) child;
                    if (name.equals(t.getSettings().get(KEY_BUNDLE_FILE))) {
                        t.getSettings().set(KEY_BUNDLE_FILE, newFile.getName());
                        try {
                            t.writeTaskSettings();
                        } catch (IOException e) {
                            StrangeEons.log.log(Level.WARNING, null, e);
                        }
                    }
                }
            };
            Rename.addRenameListener(renameListener);
        }
    }

    private static final String[] EXTENSIONS = new String[]{
        BundleInstaller.EXTENSION_FILE_EXT.substring(1),
        BundleInstaller.PLUGIN_FILE_EXT.substring(1),
        BundleInstaller.THEME_FILE_EXT.substring(1),
        BundleInstaller.LIBRARY_FILE_EXT.substring(1)
    };

    @Override
    public String getLabel() {
        return string("pa-make-bundle");
    }

    @Override
    public String getDescription() {
        return string("pa-make-bundle-tt");
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        if (task == null || member != null) {
            return false;
        }

        String type = task.getSettings().get(Task.KEY_TYPE);
        return NewTaskType.PLUGIN_TYPE.equals(type) && task.getChildCount() > 0;
    }

    @Override
    public boolean perform(Project project, final Task task, Member member) {
        if (task == null) {
            return false;
        }

        String name = task.getSettings().get(KEY_BUNDLE_FILE);

        Member parentMember;
        if (name != null && name.startsWith(".")) {
            parentMember = task;
        } else {
            parentMember = task.getParent();
        }

        // Compile Java (if any)
        CompileAll compile = (CompileAll) Actions.getUnspecializedAction("compileall");
        if (compile == null) {
            compile = new CompileAll();
        }
        if (compile.appliesTo(project, task, null)) {
            if (!compile.perform(project, task, null)) {
                // compiler error
                Toolkit.getDefaultToolkit().beep();
                return false;
            }
        } else {
            // compileall does this, so we only need to do it
            // if it doesn't get run
            ProjectUtilities.saveAllOpenProjectFiles(false);
        }

        // Analyze the root file if possible, take a best guess at the
        // plug-in type to use as a default when the bundle name is not known,
        // and touch the catalogue ID (if enabled).
        File rootFile = new File(task.getFile(), "eons-plugin");
        PluginRoot root = null;
        if (rootFile.exists()) {
            boolean writeRootUpdate = false;
            try {
                root = new PluginRoot(rootFile);
                if (Settings.getUser().getYesNo(KEY_AUTOTOUCH)) {
                    CatalogID id = root.getCatalogID();
                    if (id == null) {
                        id = new CatalogID(); // generate
                    } else {
                        id = new CatalogID(id); //touch
                    }
                    root.setCatalogID(id);
                    writeRootUpdate = true;
                }
            } catch (IOException e) {
                StrangeEons.log.log(Level.WARNING, "unable to read root file", e);
            }

            if (writeRootUpdate) {
                try {
                    root.writeToFile(rootFile);
                } catch (IOException e) {
                    ErrorDialog.displayError(string("ae-err-save"), e);
                    return false;
                }
            }
        } else {
            // there is no root file!
            // this is only valid for libraries, although not recommended for them
            // if no name has been set, assume it is a new plug-in and warn the user
            if (name == null) {
                int option = JOptionPane.showConfirmDialog(
                        StrangeEons.getWindow(), string("pa-make-err-no-root"),
                        getLabel(), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
                );
                if (option != JOptionPane.YES_OPTION) {
                    return false;
                }
            }
        }

        boolean usingDefaultBundleName = false;
        if (name == null) {
            usingDefaultBundleName = true;
            String bestGuessAtBundleExtension = guessPluginType(task, root);
            String defaultFileName = task.getName().replaceAll("\\s+", "") + bestGuessAtBundleExtension;
            File uniqueDummyName = new File(parentMember.getFile(), defaultFileName);
            name = ProjectUtilities.getAvailableFile(uniqueDummyName).getName();
        }

        File bundleFile = new File(parentMember.getFile(), name);

        ZipOutputStream zip = null;
        try {
            zip = ProjectUtilities.createPluginBundleArchive(bundleFile, true);

            // zip is valid now; put in final var so we can share with inner class
            final ZipOutputStream out = zip;
            packFailure = null;
            new BusyDialog(StrangeEons.getWindow(), string("pa-make-bundle-busy"), () -> {
                try {
                    pack(out, task, "");
                } catch (IOException e) {
                    packFailure = e;
                }
            });
            if (packFailure != null) {
                throw packFailure;
            }

            zip.close();
        } catch (IOException e) {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ie) {
                }
            }
            bundleFile.delete();
            ErrorDialog.displayError(string("prj-err-factory", name), e);
            return false;
        }

        parentMember.synchronize();
        Member bundle = project.findMember(bundleFile);
        if (bundle != null && usingDefaultBundleName) {
            project.getView().select(bundle);
            RenameDialog rd = new RenameDialog(bundle);
            project.getView().moveToLocusOfAttention(rd);
            rd.setVisible(true);
            task.getSettings().set(KEY_BUNDLE_FILE, rd.getNewFile().getName());
            bundle = project.findMember(rd.getNewFile());
        }
        if (bundle != null) {
            project.getView().select(bundle);
        }
        return true;
    }
    private IOException packFailure;

    private void pack(ZipOutputStream zip, Member parent, String base) throws IOException {
        for (Member kid : parent.getChildren()) {
            File f = kid.getFile();
            ProjectUtilities.copyToArchive(zip, f, base, false, true, true);
            if (kid.isFolder()) {
                pack(zip, kid, base + f.getName() + "/");
            }
        }
    }

    /**
     * Task setting that stores the name of the file to use for the bundle.
     */
    public static final String KEY_BUNDLE_FILE = "bundle-file";

    /**
     * User setting that controls whether root file IDs are touched
     * automatically during the make.
     */
    public static final String KEY_AUTOTOUCH = "make-bundle-autotouch";

    private static String guessPluginType(Task task, PluginRoot root) {
        String type = BundleInstaller.PLUGIN_FILE_EXT;
        if (root != null) {
            try {
                String[] plugins = root.getPluginIdentifiers();
                if (plugins.length == 0) {
                    return BundleInstaller.LIBRARY_FILE_EXT;
                }
                // look at the first plug-in bundle and guess the type
                String id = plugins[0];
                if (id.startsWith("script:res://")) {
                    // script id
                    id = id.substring("script:res://".length());
                    if (!id.startsWith("/")) {
                        id = "resources/" + id;
                    }
                    File pluginFile = new File(task.getFile(), id);
                    String data = ProjectUtilities.getFileAsString(pluginFile, ProjectUtilities.ENC_SCRIPT);
                    if (contains(data, "return\\s+(\\p{Alnum}+\\.)*EXTENSION", true) || contains(data, "use[lL]ibrary\\s*\\(\\s*['\"]extension['\"]", true)) {
                        type = BundleInstaller.EXTENSION_FILE_EXT;
                    }
                } else {
                    // class id (could be a theme!)
                    if (contains(id, "theme", false)) {
                        type = BundleInstaller.THEME_FILE_EXT;
                    } else {
                        type = BundleInstaller.EXTENSION_FILE_EXT;
                    }
                }
            } catch (Exception e) {
                StrangeEons.log.log(Level.WARNING, null, e);
            }
        } else {
            type = BundleInstaller.LIBRARY_FILE_EXT;
        }
        return type;
    }

    private static boolean contains(String text, String regex, boolean caseSense) {
        Pattern p;
        if (caseSense) {
            p = Pattern.compile(regex);
        } else {
            p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        }
        return p.matcher(text).find();
    }
}
