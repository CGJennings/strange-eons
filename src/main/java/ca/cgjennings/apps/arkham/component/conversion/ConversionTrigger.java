package ca.cgjennings.apps.arkham.component.conversion;

import java.util.Objects;

/**
 * A {@code ConversionTrigger} object must be provided when initiating
 * conversion of a {@link ca.cgjennings.apps.arkham.component.GameComponent}.
 *
 * @see ConversionSession#convertGameComponent(ConversionTrigger,
 * ca.cgjennings.apps.arkham.component.GameComponent)
 * @author Henrik Rostedt
 * @since 3.3
 */
public interface ConversionTrigger {

    /**
     * Returns a string identifying why the conversion was triggered. Should
     * generally correspond to the trigger type.
     *
     * @return the conversion cause
     */
    public String getCause();

    /**
     * Returns the identifier for the new component type to convert the
     * {@link ca.cgjennings.apps.arkham.component.GameComponent} to.
     *
     * @return the target component type identifier
     */
    public String getTargetClassName();

    /**
     * Returns the name of required extension for the new component type.
     * Returns {@code null} if the extension is the same as the original
     * component type.
     *
     * @return the name of the required extension
     */
    public String getRequiredExtensionName();

    /**
     * Returns the UUID of required extension for the new component type.
     * Returns {@code null} if the extension is the same as the original
     * component type.
     *
     * @return the UUID of the required extension
     */
    public String getRequiredExtensionId();

    /**
     * Creates a default conversion trigger that is suitable for most conversion
     * tasks.
     *
     * @param cause A non-null string describing the reason for the conversion.
     * @param targetClassName The non-null name of the class map class of the
     * new component.
     * @param extensionName An optional description of the extension required by
     * the target component.
     * @param extensionId An optional UUID or CatalogID string of the extension
     * required by the target component.
     * @return A basic conversion trigger for the specified cause. The returned
     * trigger is guaranteed <em>not</em> to be an instance of
     * {@link AbstractConversionTrigger} (and by extension,
     * {@link UpgradeConversionTrigger}).
     */
    public static ConversionTrigger create(final String cause, final String targetClassName, final String extensionName, final String extensionId) {
        Objects.requireNonNull(cause, "cause");
        Objects.requireNonNull(targetClassName, "targetClassName");

        return new ConversionTrigger() {
            @Override
            public String getCause() {
                return cause;
            }

            @Override
            public String getTargetClassName() {
                return targetClassName;
            }

            @Override
            public String getRequiredExtensionName() {
                return extensionName;
            }

            @Override
            public String getRequiredExtensionId() {
                return extensionId;
            }

            @Override
            public String toString() {
                return "basic trigger, cause " + getCause();
            }
        };
    }
}
