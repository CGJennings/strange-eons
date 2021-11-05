package ca.cgjennings.apps.arkham.component.conversion;

public class ManualConversionTrigger extends AbstractConversionTrigger {

    private final String group;

    public ManualConversionTrigger(String targetClassName, String requiredExtensionName, String requiredExtensionId, String group) {
        super(targetClassName, requiredExtensionName, requiredExtensionId);
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

    @Override
    public String getCause() {
        return "manual";
    }
}
