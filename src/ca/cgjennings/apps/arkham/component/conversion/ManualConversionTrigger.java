package ca.cgjennings.apps.arkham.component.conversion;

/**
 * A {@link ConversionTrigger} created when the user initiates a manual
 * conversion (e.g. from the Edit menu). {@code ManualConversionTrigger} returns
 * {@code "manual"} from {@link #getCause()}.
 *
 * @author Henrik Rostedt
 */
public class ManualConversionTrigger extends AbstractConversionTrigger {

    private final String group;

    /**
     * Creates a new {@code ManualConversionTrigger} indicating that the user
     * has requested a component conversion.
     *
     * @param targetClassName the identifier of the new component type
     * @param requiredExtensionName the name of the required extension, or null
     * if no extension is required
     * @param requiredExtensionId the UUID of the required extension, or null if
     * no extension is required
     * @param group the conversion group the conversion option belongs to, or
     * null if it is a direct conversion option
     */
    public ManualConversionTrigger(String targetClassName, String requiredExtensionName, String requiredExtensionId, String group) {
        super(targetClassName, requiredExtensionName, requiredExtensionId);
        this.group = group;
    }

    /**
     * Returns the conversion group of the triggering conversion option.
     * {@code null} is returned if it was a direct conversion option.
     *
     * @return the conversion group or null
     */
    public String getGroup() {
        return group;
    }

    @Override
    public String getCause() {
        return "manual";
    }
}
