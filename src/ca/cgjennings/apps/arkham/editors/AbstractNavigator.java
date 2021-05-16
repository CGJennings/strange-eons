package ca.cgjennings.apps.arkham.editors;

import java.util.regex.Matcher;

/**
 * An abstract base class for navigator implementations.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public abstract class AbstractNavigator implements Navigator {

    CodeEditor editor;

    @Override
    public void install(CodeEditor editor) {
        this.editor = editor;
    }

    @Override
    public void uninstall(CodeEditor editor) {
        this.editor = null;
    }

    public CodeEditor getEditor() {
        return editor;
    }

    /**
     * Dumps a helpful description of regular expression exceptions to stderr.
     * This can be useful when debugging a navigator that uses regular
     * expressions.
     *
     * @param t the exception that was thrown
     * @param text the source text being matched against
     * @param m the matcher that threw {@code t}
     */
    public static void dumpRegExpThrowable(Throwable t, String text, Matcher m) {
        System.err.println("Navigation regexp exception:");
        System.err.println("--------");
        if (m != null) {
            int pos = 0;
            try {
                pos = m.start();
            } catch (IllegalStateException e) {
            }
            System.err.print(text.substring(0, pos));
            System.err.print(">>|<<");
            System.err.println(text.substring(pos));
        }
        System.err.println("--------");
        t.printStackTrace();
    }
}
