package ca.cgjennings.apps.arkham.component.conversion;

public class UpgradeConversionTrigger extends AbstractConversionTrigger {

    public UpgradeConversionTrigger(String targetClassName, String requiredExtensionName, String requiredExtensionId) {
        super(targetClassName, requiredExtensionName, requiredExtensionId);
    }

    @Override
    public String getCause() {
        return "upgrade";
    }
}
