
/**
 * A convenience class that launches the Strange Eons script debugger client.
 * <p>
 * To use this from a command line, use a command like the following:<br>
 * <pre>java -cp strange-eons.jar debugger [arguments...]</pre>
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */

public final class debugger {

    private debugger() {
    }

    /**
     * Starts the Strange Eons script debugger client, passing the supplied
     * arguments the client's {@code main} method.
     *
     * @param args the arguments to pass to the debugger client
     */
    public static void main(String[] args) {
        ca.cgjennings.apps.arkham.plugins.debugging.Client.main(args);
    }
}
