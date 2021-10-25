package ca.cgjennings.apps.arkham.component.conversion;

public class ConversionContext {

    private final String targetClassName;

    public ConversionContext(String targetClassName) {
        this.targetClassName = targetClassName;
    }

    public String getTargetClassName() {
        return targetClassName;
    }
}
