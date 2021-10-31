package ca.cgjennings.apps.arkham.component.conversion;

/**
 * A {@code ConversionTrigger} object must be provided when initiating
 * conversion of a {@link ca.cgjennings.apps.arkham.component.GameComponent}.
 *
 * @see ConversionSession#convertGameComponent(ConversionTrigger,
 * ca.cgjennings.apps.arkham.component.GameComponent)
 * @author Henrik Rostedt
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
}
