package ca.cgjennings.apps.arkham.component.conversion;

public interface ConversionTrigger {

    public String getCause();

    public String getTargetClassName();

    public String getRequiredExtensionName();

    public String getRequiredExtensionId();
}
