package ca.cgjennings.apps.arkham;

import ca.cgjennings.util.CommandFormatter;
import java.lang.management.ManagementFactory;

/**
 * A default command formatter useful for launching user overridden
 * subprocesses. It predefines the following variables:
 *
 * <dl>
 * <dt>{@code %j}</dt> <dd>path to the Java executable used to start this
 * app</dd>
 * <dt>{@code %c}</dt> <dd>class path for this app</dd>
 * <dt>{@code %v}</dt> <dd>virtual machine arguments for this app</dd>
 * </dl>
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.2
 */
public class DefaultCommandFormatter extends CommandFormatter {

    public DefaultCommandFormatter() {
        super();
        setVariable('c', Subprocess.getClasspath());
        setVariable('v', String.join(" ", ManagementFactory.getRuntimeMXBean().getInputArguments()));
        setVariable('j', Subprocess.getJavaRuntimeExecutable());
    }
}
