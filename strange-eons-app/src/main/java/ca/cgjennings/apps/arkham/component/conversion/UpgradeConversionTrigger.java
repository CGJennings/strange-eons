package ca.cgjennings.apps.arkham.component.conversion;

/**
 * A {@link ConversionTrigger} created by
 * {@link ca.cgjennings.apps.arkham.component.GameComponent}s after being read
 * from file to indicate that the component need to be converted to a new
 * component type. {@code UpgradeConversionTrigger} returns {@code "upgrade"}
 * from {@link #getCause()}.
 *
 * @author Henrik Rostedt
 * @since 3.3
 */
public class UpgradeConversionTrigger extends AbstractConversionTrigger {

    /**
     * Creates a new {@code UpgradeConversionTrigger} indicating a component
     * needs to be upgraded to another component type. Use
     * {@link #UpgradeConversionTrigger(String, String, String)} if the
     * component type belongs to another extension.
     *
     * @param targetClassName the identifier of the new component type
     */
    public UpgradeConversionTrigger(String targetClassName) {
        super(targetClassName, null, null);
    }

    /**
     * Creates a new {@code UpgradeConversionTrigger} indicating a component
     * needs to be upgraded to another component type. Use
     * {@link #UpgradeConversionTrigger(String)} if the component type belongs
     * to the same extension.
     *
     * @param targetClassName the identifier of the new component type
     * @param requiredExtensionName the name of the required extension
     * @param requiredExtensionId the UUID of the required extension
     */
    public UpgradeConversionTrigger(String targetClassName, String requiredExtensionName, String requiredExtensionId) {
        super(targetClassName, requiredExtensionName, requiredExtensionId);
    }

    @Override
    public String getCause() {
        return "upgrade";
    }
}
