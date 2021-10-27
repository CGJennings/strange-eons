package ca.cgjennings.apps.arkham.component;

import resources.Settings;

/**
 * Is used to share context information during {@link GameComponet} conversion.
 * The initiator creates an instance of the class with the basic information
 * required to start the conversion process. Then the old component gets a
 * chance update and add to the context (or make changes to the new component
 * directly). After this the new component gets a change to update the context
 * (or update itself directly). When both components have had their turns, any
 * automatic conversion steps are performed according to which copy flags are
 * set in the context.
 *
 * @author Henrik Rostedt
 */
public class ConversionContext {

    /**
     * The different reasons a conversion may be triggered. Can be checked by
     * components if they need to handle the situations differently.
     */
    public enum Trigger {
        /**
         * Conversion was triggered by a component when upgrading an old file.
         */
        UPGRADE
    }

    private final Trigger trigger;
    private final String sourceClassName;
    private final String sourceExtensionName;
    private final String targetClassName;
    private final String targetExtensionName;
    private final Settings settings;

    private boolean copySettings = false;
    private boolean copyBasics = true;
    private boolean copyPortraitImages = true;
    private boolean copyPortraitLayouts = true;

    /**
     * Creates a new {@code ConversionContext} as a base for triggering a
     * {@link GameComponet} conversion. This is usually only done manually if
     * needing to convert a compiled component during upgrade. All parameters
     * except {@code targetClassName} are just included to provide context. The
     * default copy flags indicate that component name, comments, expansions and
     * portraits should be copied.
     *
     * @param trigger the reason to start the conversion
     * @param sourceClassName the identifier for the class or DIY script to
     * convert the component from
     * @param sourceExtensionName the name of the extension holding the source
     * class or script
     * @param targetClassName the identifier for the class or DIY script to
     * convert the component to
     * @param targetExtensionName the name of the extension holding the target
     * class or script
     */
    public ConversionContext(Trigger trigger, String sourceClassName, String sourceExtensionName, String targetClassName, String targetExtensionName) {
        this.trigger = trigger;
        this.sourceClassName = sourceClassName;
        this.sourceExtensionName = sourceExtensionName;
        this.targetClassName = targetClassName;
        this.targetExtensionName = targetExtensionName;
        settings = new Settings();
    }

    /**
     * Returns the {@link Trigger} of the conversion.
     *
     * @return the reason the conversion was triggered
     */
    public Trigger getTrigger() {
        return trigger;
    }

    /**
     * Returns the class or script identifier of the source component type.
     *
     * @return the identifier for the source component type
     */
    public String getSourceClassName() {
        return sourceClassName;
    }

    /**
     * Returns the name of the extension providing the source component type.
     *
     * @return the name of the source extension
     */
    public String getSourceExtensionName() {
        return sourceExtensionName;
    }

    /**
     * Returns the class or script identifier of the target component type.
     *
     * @return the identifier for the target component type
     */
    public String getTargetClassName() {
        return targetClassName;
    }

    /**
     * Returns the name of the extension providing the target component type.
     *
     * @return the name of the target extension
     */
    public String getTargetExtensionName() {
        return targetExtensionName;
    }

    /**
     * Returns a {@link Settings} object that can be used to store additional
     * conversion metadata.
     *
     * @return a settings object for the context
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     * Indicates whether the settings for the component should be copied or not.
     * Default value is {@code false}.
     *
     * @return whether the settings should be copied
     */
    public boolean shouldCopySettings() {
        return copySettings;
    }

    /**
     * Sets whether the settings for the component should be copied or not. It
     * might be better to copy the relevant settings explicitly.
     *
     * @param copySettings whether to copy settings or not
     */
    public void setCopySettings(boolean copySettings) {
        this.copySettings = copySettings;
    }

    /**
     * Indicates whether the name, comments and expansions for the component
     * should be copied or not. Default value is {@code true}.
     *
     * @return whether the basic information should be copied
     */
    public boolean shouldCopyBasics() {
        return copyBasics;
    }

    /**
     * Sets whether the name, comments and expansions for the component should
     * be copied or not. The name and comments can only be copied if the target
     * component type extends {@link AbstractGameComponent}.
     *
     * @param copyBasics whether to copy basic information or not
     */
    public void setCopyBasics(boolean copyBasics) {
        this.copyBasics = copyBasics;
    }

    /**
     * Indicates whether the portrait images for the component should be copied
     * or not. Default value is {@code true}.
     *
     * @return whether the portrait images should be copied
     */
    public boolean shouldCopyPortraitImages() {
        return copyPortraitImages;
    }

    /**
     * Sets whether the portrait images for the component should be copied or
     * not. A one to one relationship between source and target portraits is
     * assumed, any extra portraits are ignored. If the source image file can
     * not be identified, a temporary image file is created for the conversion.
     *
     * @param copyPortraitImages whether to copy portrait images or not
     */
    public void setCopyPortraitImages(boolean copyPortraitImages) {
        this.copyPortraitImages = copyPortraitImages;
    }

    /**
     * Indicates whether the portrait adjustments for the component should be
     * copied or not. Default value is {@code true}.
     *
     * @return whether the portrait adjustments should be copied
     */
    public boolean shouldCopyPortraitLayouts() {
        return copyPortraitLayouts;
    }

    /**
     * Sets whether the portrait adjustments for the component should be copied
     * or not. A one to one relationship between source and target portraits is
     * assumed, any extra portraits are ignored.
     *
     * @param copyPortraitLayouts whether to copy portrait adjustments or not
     */
    public void setCopyPortraitLayouts(boolean copyPortraitLayouts) {
        this.copyPortraitLayouts = copyPortraitLayouts;
    }

    /**
     * Indicates whether the portraits for the component should be copied or
     * not. Returns {@code true} if either {@link #shouldCopyPortraitImages()}
     * or {@link #shouldCopyPortraitLayouts()} returns {@code true}.
     *
     * @return whether the portraits should be copied
     */
    public boolean shouldCopyPortraits() {
        return copyPortraitImages || copyPortraitLayouts;
    }

    /**
     * Sets whether the portraits for the component should be copied or not.
     * Just a shorthand for setting both {@link #setCopyPortraitImages()} and
     * {@link #setCopyPortraitLayouts()}. See those methods for details.
     *
     * @param copyPortraits whether to copy portraits or not
     */
    public void setCopyPortraits(boolean copyPortraits) {
        copyPortraitImages = copyPortraits;
        copyPortraitLayouts = copyPortraits;
    }
}
