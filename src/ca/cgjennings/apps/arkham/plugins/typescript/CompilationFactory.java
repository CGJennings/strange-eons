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
                for (CompilationRoot root : projectRoots.values()) {
                    root.dispose();
                }
                projectRoots.clear();
            }
        });
    }
}
