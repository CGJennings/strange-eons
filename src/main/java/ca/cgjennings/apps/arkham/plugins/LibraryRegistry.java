package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.plugins.debugging.ScriptDebugging;
import java.io.IOException;
import java.util.Arrays;
import java.util.TreeSet;

/**
 * A registry of available scripting libraries. Plug-ins can register script
 * libraries for use by other plug-ins. Although it is always possible to access
 * scripts from other loaded plug-ins whether they are registered or not,
 * registering a library has some additional benefits. First, it clearly
 * identifies that the code is intended for use by other parties. Second, it
 * makes the library documentation available in the built-in API browser. And
 * finally, registered libraries are pre-loaded into the debugger so that break
 * points can be set in them even if they haven't been used by any scripts yet.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @see <a href="scriptdoc:libutils">libutils script library</a>
 */
public class LibraryRegistry {

    private LibraryRegistry() {
    }

    /**
     * Registers a script library for use by plug-ins.
     *
     * @param libraryName the library name (as passed to {@code useLibrary})
     * @throws IOException if an I/O occurs while reading the library
     */
    public static void register(String libraryName) throws IOException {
        if (libraryName == null) {
            throw new NullPointerException("libraryName");
        }
        ScriptMonkey.getLibrary(libraryName);
        // still here? then the library exists and can be read
        init();
        if (!libs.add(libraryName)) {
            ScriptDebugging.preprocessScript(libraryName);
        }
    }

    /**
     * Registers multiple libraries. The libraries are registered one at a time
     * as if {@link #register(java.lang.String)} was called for each in turn.
     *
     * @param libraryNames the libraries to register
     * @throws IOException if an I/O occurs while reading a library
     */
    public static void register(String... libraryNames) throws IOException {
        if (libraryNames == null) {
            throw new NullPointerException("libraryNames");
        }
        for (int i = 0; i < libraryNames.length; ++i) {
            if (libraryNames[i] == null) {
                throw new NullPointerException("libraryNames[" + i + ']');
            }
            register(libraryNames[i]);
        }
    }

    /**
     * Returns {@code true} if the specified library has been registered, If the
     * library has not been registered, but it can be loaded successfully, then
     * it will be registered immediately, returning {@code true}. Otherwise,
     * returns {@code false}.
     *
     * @param libraryName the name of the library to check
     * @return {@code true} if the library is registered
     */
    public static boolean isRegistered(String libraryName) {
        init();

        if (libs.contains(libraryName)) {
            return true;
        }

        try {
            register(libraryName);
            return true;
        } catch (IOException e) {
        }

        return false;
    }

    /**
     * Returns an array of all registered libraries.
     *
     * @return a possibly empty array of library names
     */
    public static String[] getLibraries() {
        init();
        return libs.toArray(String[]::new);
    }

    /**
     * Returns an array of all of the standard, built-in libraries.
     *
     * @return a possibly empty array of library names
     */
    public static String[] getStandardLibraries() {
        return STANDARD_LIBS.clone();
    }

    private static TreeSet<String> libs;

    private static void init() {
        if (libs == null) {
            libs = new TreeSet<>();
            libs.addAll(Arrays.asList(STANDARD_LIBS));
        }
    }

    private static final String[] STANDARD_LIBS = new String[]{
        "cards", "common", "diy", "extension",
        "fontutils", "imageutils",
        "markup", "modifiers", "prefab", "project", "random",
        "threads", "tints",
        "ui", "uibindings", "uicontrols", "uilayout"
    };
}
