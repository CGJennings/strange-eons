package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.TextEncoding;
import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import ca.cgjennings.apps.arkham.plugins.ScriptConsole;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.i18n.IntegerPluralizer;
import java.io.File;
import java.util.LinkedList;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import resources.Language;
import static resources.Language.string;

/**
 * A default implementation of {@link Compiler} that uses the Java 6
 * {@code ToolProvider} API to locate a Java compiler. This class (and the
 * compiler implementation) will be lazily loaded by {@link Compile} when
 * required.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
class CompilerImpl implements Compile.Compiler {

    /**
     * Attempts to locate a Java compiler. If none is available, throws
     * {@code UnsupportedOperationException}.
     */
    public CompilerImpl() {
        javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) {
            throw new UnsupportedOperationException("no Java compiler");
        }

        String pathSep = System.getProperty("path.separator", ";");
        LinkedList<String> temp = new LinkedList<>();

        temp.add("-g");
        temp.add("-deprecation");
        temp.add("-target");
        temp.add("1.8");
        temp.add("-source");
        temp.add("1.8");
        temp.add("-encoding");
        temp.add(TextEncoding.SOURCE_CODE);

        File SEJar = BundleInstaller.getApplicationLibrary();
        temp.add("-classpath");
        CP_INDEX = temp.size();
        temp.add(SEJar == null ? "" : (SEJar.getAbsolutePath() + pathSep));

        temp.add("-sourcepath");
        SP_INDEX = temp.size();
        temp.add("");

        argBase = temp.toArray(String[]::new);
    }

    private int CP_INDEX;
    private int SP_INDEX;

    @Override
    public boolean compile(File baseDir, File... sourceFiles) {
        ScriptConsole con = ScriptMonkey.getSharedConsole();

        IntegerPluralizer pl = Language.getInterface().getPluralizer();
        con.getWriter().printf(pl.pluralize(sourceFiles.length, "pa-compile-message"), sourceFiles.length);
        con.getWriter().println();

        String[] args = new String[argBase.length + sourceFiles.length];
        for (int i = 0; i < argBase.length; ++i) {
            args[i] = argBase[i];
        }
        for (int i = 0; i < sourceFiles.length; ++i) {
            args[argBase.length + i] = sourceFiles[i].getPath();
        }
        args[CP_INDEX] += baseDir;
        args[SP_INDEX] += baseDir;

        int retval = javac.run(
                null, con.getOutputStream(), con.getErrorStream(),
                args
        );
        con.getWriter().printf(string("retval", retval));
        return retval == 0;
    }
    private final JavaCompiler javac;
    private String[] argBase;
}
