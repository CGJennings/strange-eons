package ca.cgjennings.apps.arkham.component.conversion;

/**
 * Provides default implementations for the {@link ConversionTrigger} interface.
 *
 * @author Henrik Rostedt
 * @since 3.3
 */
public abstract class AbstractConversionTrigger implements ConversionTrigger {

    private final String targetClassName;
    private final String requiredExtensionName;
    private final String requiredExtensionId;

    public AbstractConversionTrigger(String targetClassName, String requiredExtensionName, String requiredExtensionId) {
        this.targetClassName = targetClassName;
        this.requiredExtensionName = requiredExtensionName;
        this.requiredExtensionId = requiredExtensionId;
    }

    @Override
    public String getTargetClassName() {
        return targetClassName;
    }

    @Override
    public String getRequiredExtensionName() {
        return requiredExtensionName;
    }

    @Override
    public String getRequiredExtensionId() {
        return requiredExtensionId;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ", cause " + getCause();
    }
}
