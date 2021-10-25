package ca.cgjennings.apps.arkham.component.conversion;

public class ConversionContext {

    private final String targetClassName;
    private final String targetExtensionName;

    public ConversionContext(String targetClassName, String targetExtensionName) {
        this.targetClassName = targetClassName;
        this.targetExtensionName = targetExtensionName;
    }

    public String getTargetClassName() {
        return targetClassName;
    }

    public String getTargetExtensionName() {
        return targetExtensionName;
    }
}
