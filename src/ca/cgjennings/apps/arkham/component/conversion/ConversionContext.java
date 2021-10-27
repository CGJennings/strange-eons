package ca.cgjennings.apps.arkham.component.conversion;

import resources.Settings;

public class ConversionContext {

    public enum Trigger {
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

    public ConversionContext(Trigger trigger, String sourceClassName, String sourceExtensionName, String targetClassName, String targetExtensionName) {
        this.trigger = trigger;
        this.sourceClassName = sourceClassName;
        this.sourceExtensionName = sourceExtensionName;
        this.targetClassName = targetClassName;
        this.targetExtensionName = targetExtensionName;
        settings = new Settings();
    }

    public Trigger getTrigger() {
        return trigger;
    }

    public String getSourceClassName() {
        return sourceClassName;
    }

    public String getSourceExtensionName() {
        return sourceExtensionName;
    }

    public String getTargetClassName() {
        return targetClassName;
    }

    public String getTargetExtensionName() {
        return targetExtensionName;
    }

    public Settings getSettings() {
        return settings;
    }

    public boolean shouldCopySettings() {
        return copySettings;
    }

    public void setCopySettings(boolean copySettings) {
        this.copySettings = copySettings;
    }

    public boolean shouldCopyBasics() {
        return copyBasics;
    }

    public void setCopyBasics(boolean copyBasics) {
        this.copyBasics = copyBasics;
    }

    public boolean shouldCopyPortraitImages() {
        return copyPortraitImages;
    }

    public void setCopyPortraitImages(boolean copyPortraitImages) {
        this.copyPortraitImages = copyPortraitImages;
    }

    public boolean shouldCopyPortraitLayouts() {
        return copyPortraitLayouts;
    }

    public void setCopyPortraitLayouts(boolean copyPortraitLayouts) {
        this.copyPortraitLayouts = copyPortraitLayouts;
    }

    public boolean shouldCopyPortraits() {
        return copyPortraitImages || copyPortraitLayouts;
    }

    public void setCopyPortraits(boolean copyPortraits) {
        copyPortraitImages = copyPortraits;
        copyPortraitLayouts = copyPortraits;
    }
}
