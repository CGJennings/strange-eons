package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.BusyDialog;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import ca.cgjennings.apps.arkham.plugins.catalog.Catalog.VersioningState;
import ca.cgjennings.apps.arkham.plugins.catalog.CatalogID;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import resources.CoreComponents;
import static resources.Language.string;

/**
 * A task type that extracts application and core resources into a task folder.
 * This task type replaces the resource tool included with Strange Eons 1 and 2.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
class ResourceReferenceTask extends NewTaskType {

    @Override
    public String getLabel() {
        return string("pt-resref-name");
    }

    @Override
    public String getDescription() {
        return string("pt-resref-desc");
    }

    @Override
    public String getIconResource() {
        return "icons/project/mt95.png";
    }

    @Override
    public boolean initializeNewTask(final Project project, final Task task) {
        for (final CoreComponents cc : CoreComponents.values()) {
            try {
                cc.validate();
            } catch (CoreComponents.MissingCoreComponentException mcc) {
                return false;
            }
        }

        new BusyDialog(StrangeEons.getWindow(), string("pt-resref-extract"), () -> {
            try {
                File appFile = BundleInstaller.getApplicationLibrary();
                if (appFile.isDirectory() && StrangeEons.getBuildNumber() == 99_999) {
                    appFile = new File(System.getenv("programfiles") + "/StrangeEons/SE3/strange-eons.jar");
                }
                if (!appFile.isDirectory() && appFile.exists()) {
                    unpack(task, appFile);
                }
                
                for (final CoreComponents cc : CoreComponents.values()) {
                    if (cc.getInstallationState() == VersioningState.NOT_INSTALLED) {
                        continue;
                    }
                    
                    CatalogID id = cc.getCatalogID();
                    if (id == null) {
                        continue;
                    }
                    
                    File coreFile = BundleInstaller.getBundleFileForUUID(id.getUUID());
                    if (coreFile == null) {
                        continue;
                    }
                    
                    unpack(task, coreFile);
                }
            } catch (IOException e) {
                ErrorDialog.displayError("prj-err-task", e);
                ProjectUtilities.deleteAll(task.getFile());
            }
        });
        return true;
    }

    private void unpack(Task task, File jarFile) throws IOException {
        File parent = task.getFile();

        try (JarFile libraryBundle = new JarFile(jarFile)) {
            Enumeration<? extends ZipEntry> entries = libraryBundle.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                // filter entries
                if (name.endsWith(".class")) {
                    continue;
                }
                if (name.equals("eons-plugin")) {
                    continue;
                }
                if (name.startsWith("META-INF/")) {
                    continue;
                }
                if (name.equals("ca/cgjennings/apps/arkham/rev")) {
                    continue;
                }
                if (name.startsWith("ca/cgjennings/script/")) {
                    continue;
                }
                // skip install_xx.html files
                if (name.startsWith("install") && name.endsWith((".html"))) {
                    continue;
                }

                String displayName = name;
                if (displayName.length() > 40) {
                    displayName = "..." + displayName.substring(displayName.length() - 40);
                }
                BusyDialog.statusText(displayName, 50);

                File dest = new File(parent, localizePath(name));

                if (!name.endsWith("/")) {
                    if (!dest.getParentFile().exists()) {
                        dest.getParentFile().mkdirs();
                    }
                    try (InputStream in = libraryBundle.getInputStream(entry)) {
                        ProjectUtilities.copyStream(in, dest);
                    }
                }
            }
        }
    }

    private String localizePath(String path) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path.replace("/", File.separator);
    }
}
