package ca.cgjennings.apps.arkham.plugins.debugging;

import ca.cgjennings.apps.CommandLineParser;

/**
 * Arguments accepted by the script debugging client.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class ClientArguments extends CommandLineParser {
    public String host;
    public int port = -1;
    public boolean search;

    @Override
    public String getUsageText() {
        return "Strange Eons Debugger Client\n" +
               "Options:\n" +
               "   --host name  connect to debug server on specified host\n" +
               "   --port n     connect to debug server at specified port\n" +
               "   --search     search for and list debug servers on the local host,\n" +
               "                or on another host if --host is also specified\n\n" +
               "Searches may take several seconds, and even longer on remote hosts.\n";
    }

    @Override
    public void parse(Object target, String... args) {
        super.parse(target, args);
        if(getPlainArguments().length > 0) {
            handleParsingError("Invalid argument");
        }
    }
}
