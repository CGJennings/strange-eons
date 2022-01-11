
import java.util.Arrays;
import java.util.Locale;

/**
 * A convenience class that launches Strange Eons. The primary purpose of this
 * class is to make it easy to launch Strange Eons from the command line. In
 * most cases, it simply forwards the supplied arguments on to the
 * {@code main()} method of {@link ca.cgjennings.apps.arkham.StrangeEons}.
 * This allows starting the application using a command such as:
 * <pre>java -cp strange-eons.jar strangeeons [arguments...]</pre>
 *
 * <p>
 * A secondary purpose of this class is to launch tools included with Strange
 * Eons that are otherwise difficult to start on some platforms. For example, on
 * OS X it is difficult to run other classes that are part of Strange Eons since
 * it is packaged as an {@code .app}. This secondary purpose is activated
 * by passing {@code --tool} as the first parameter. In this case, the
 * launcher looks for a class with the specified name in the default package and
 * tries to invoke its static main method to start it. All arguments after the
 * tool name are passed to the tool. Applicable tools include
 * {@link catalogid}, {@link compress}, {@link debugger}, and {@link register}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class strangeeons {

    private strangeeons() {
    }

    /**
     * Starts Strange Eons, passing any passed-in arguments on unchanged.
     *
     * @param args command-line arguments to forward to the application
     */
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("--tool")) {
            if (args.length == 1) {
                System.err.println("Missing name of tool to run: catalogid; debugger; register");
                System.exit(20);
            }
            Class<?> tool = null;
            args[1] = args[1].trim().toLowerCase(Locale.CANADA);
            try {
                // only allow invoking classes in the default package
                if (args[1].indexOf('.') >= 0) {
                    throw new ClassNotFoundException();
                }
                tool = Class.forName(args[1]);
            } catch (ClassNotFoundException cnf) {
                System.err.println("Unknown tool name: " + args[1]);
                System.exit(20);
            }
            args = Arrays.copyOfRange(args, 2, args.length);
            try {
                tool.getMethod("main", String[].class).invoke(null, (Object) args);
            } catch (Throwable t) {
                System.err.println("Uncaught exception from tool.main:");
                t.printStackTrace(System.err);
                System.exit(20);
            }
        } else {
            ca.cgjennings.apps.arkham.StrangeEons.main(args);
        }
    }
}
