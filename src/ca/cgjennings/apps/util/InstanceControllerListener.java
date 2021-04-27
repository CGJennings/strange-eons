package ca.cgjennings.apps.util;

/**
 * A listener that is called to process command line arguments from an instance.
 * The instance can either be this instance (if the application is being started
 * for the first time), or else another instance that is being blocked from
 * starting by this instance.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface InstanceControllerListener {

    /**
     * This method is called by {@code InstanceController} to pass on arguments
     * from secondary instances for the primary instance to handle. If
     * {@code isInitialInstance} is {@code true}, then the arguments have
     * originated with this instance of the program. Otherwise, the arguments
     * have been sent to this instance's {@code InstanceController} via
     * interprocess communication.
     *
     * <p>
     * This method should return {@code true} if it is able to correctly handle
     * the submitted arguments, otherwise it must return {@code false}.
     */
    public abstract boolean parseInstanceArguments(boolean isInitialInstance, String[] args);
}
