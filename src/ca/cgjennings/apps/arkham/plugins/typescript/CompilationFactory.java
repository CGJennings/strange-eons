package ca.cgjennings.apps.arkham.plugins.typescript;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsAppWindow;
import ca.cgjennings.apps.arkham.project.Member;
import ca.cgjennings.apps.arkham.project.NewTaskType;
import ca.cgjennings.apps.arkham.project.Project;
import ca.cgjennings.apps.arkham.project.Task;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates compilation roots for source files. The returned roots may
 * be shared.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class CompilationFactory {
    private CompilationFactory() {
    }
    
    public static CompilationRoot createStandalone() {
        return new CompilationRoot();
    }
    
    /**
     * Returns an array of all compilation roots created for the open project.
     * This returns all of the shared roots that have been initialized so far,
     * not all of the possible roots.
     * 
     * @return an array of the compilation roots 
     */
    public static CompilationRoot[] getProjectRoots() {
        return projectRoots.values().toArray(CompilationRoot[]::new);
    }
    
    /**
     * Returns a shared compilation root for the project member. Typically, each
     * plug-in task folder has its own separate root, and there is one root for
     * the project as a whole.
     * 
     * @param member the member to get a compilation root for
     * @return the shared compilation root
     */
    public static CompilationRoot forMember(Member member) {
        Task t = member.getTask();
        if (!NewTaskType.PLUGIN_TYPE.equals(t.getSettings().get(Task.KEY_TYPE))) {
            t = t.getProject();
        }
        CompilationRoot root = projectRoots.get(t);
        if (root == null) {
            root = new CompilationRoot();
            root.setRootFile(t.getFile());
            // add children
            projectRoots.put(t, root);
        }
        return root;
    }
    
    /**
     * Returns a compilation root for the specified file. If the
     * file is part of the open project, returns the same root that would
     * be returned for the file's project member. Otherwise, returns a
     * new standalone root.
     * 
     * @param file the file to obtain a compilation root for
     * @return the file's compilation root
     */
    public static CompilationRoot forFile(File file) {
        if (file == null) {
            return createStandalone();
        }
        Project project = StrangeEons.getOpenProject();
        if (project != null) {
            Member m = project.findMember(file);
            if (m != null) {
                return forMember(m);
            }
        }
        return createStandalone();
    }
    
    private static Map<Member,CompilationRoot> projectRoots = new HashMap<>();
    
    static {
        StrangeEons.getWindow().addProjectEventListener(new StrangeEonsAppWindow.ProjectEventListener() {
            @Override
            public void projectOpened(Project proj) {
            }

            @Override
            public void projectClosing(Project proj) {
                projectRoots.clear();
            }
        });
    }
}
