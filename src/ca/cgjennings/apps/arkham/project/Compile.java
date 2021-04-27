package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.ui.JLinkLabel;
import java.io.File;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import static resources.Language.string;

/**
 * A task action that compiles selected Java source files. The static method
 * {@link #getCompiler()} has been exposed to provide access to the Java
 * compiler for your own use.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class Compile extends TaskAction {

    @Override
    public String getLabel() {
        return string("pa-compile");
    }

    @Override
    public String getDescription() {
        return string("pa-compile-tt");
    }

    static void showNoJDKMessage() {
        JOptionPane.showMessageDialog(
                StrangeEons.getWindow(),
                new Object[]{
                    string("pa-compile-no-compiler"),
                    new JLinkLabel(string("pa-compile-no-compiler-link"))
                },
                string("pa-compile"), JOptionPane.INFORMATION_MESSAGE
        );
    }

    @Override
    public boolean perform(Project project, Task task, Member member) {
        Member base = task != null ? task : project;
        Compiler c = getCompiler();
        if (c == null) {
            showNoJDKMessage();
            return false;
        }

        boolean ok = c.compile(base.getFile(), member.getFile());
        base.synchronize();
        return ok;
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        return member != null && !member.isFolder() && member.getExtension().equals("java");
    }

    /**
     * An interface that provides basic access to a Java source compiler. Output
     * from the compiler will be displayed in the script output window.
     */
    public static interface Compiler {

        /**
         * Compiles a collection of Java source files by invoking a local source
         * compiler. The compiler will include the main Strange Eons application
         * classes in the class path for the source files to compile against.
         * The files are compiled in place, so that the output for each file
         * will located in the same directory as the source.
         *
         * @param sourceFileBasePath the base directory that contains the source
         * files (i.e., the folder that contains the default package of the
         * source tree)
         * @param sourceFiles one or more source files located in the base path
         * that should be compiled
         * @return <code>true</code> if compilation was successful
         */
        public boolean compile(File sourceFileBasePath, File... sourceFiles);
    }

    /**
     * Returns a {@link Compiler} that can be used to compile Java source files,
     * or <code>null</code> if none is available. If a compiler has been set
     * explicitly using {@link #setCompiler}, then the most recently set
     * compiler is returned. Otherwise, a default compiler implementation is
     * used. The default implementation should be available on any system with a
     * Java 6+ JDK (Java Development Kit) installed.
     *
     * <p>
     * If no compiler has been explicitly set and the default implementation is
     * not available, this method returns <code>null</code>.
     *
     * @return an interface to a Java compiler installed on the user's system,
     * or <code>null</code>
     */
    public static synchronized Compiler getCompiler() {
        if (!loaded) {
            loaded = true;
            try {
                javac = new CompilerImpl();
            } catch (UnsupportedOperationException e) {
                // no compiler is available
                StrangeEons.log.warning("no Java compiler available from ToolProvider");
            } catch (Throwable t) {
                // if anything blows up, treat as no compiler but log it
                StrangeEons.log.log(Level.SEVERE, "unexpected exception", t);
            }
        }
        return javac;
    }
    private static Compiler javac;
    private static boolean loaded;

    /**
     * Explicitly sets the compiler implementation to use. Plug-in developers
     * may call this to replace the default {@link Compiler} with a custom
     * implementation.
     *
     * @param javac the compiler implementation to use when compiling Java
     * source files
     * @throws NullPointerException if the compiler is <code>null</code>
     */
    public static synchronized void setCompiler(Compiler javac) {
        if (javac == null) {
            throw new NullPointerException("javac");
        }
        loaded = true;
        Compile.javac = javac;
    }
}
