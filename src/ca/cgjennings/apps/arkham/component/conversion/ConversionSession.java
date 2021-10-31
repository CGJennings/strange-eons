package ca.cgjennings.apps.arkham.component.conversion;

import ca.cgjennings.apps.arkham.component.AbstractGameComponent;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.component.Portrait;
import ca.cgjennings.apps.arkham.component.PortraitProvider;
import ca.cgjennings.apps.arkham.diy.DIY;
import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import ca.cgjennings.imageio.SimpleImageWriter;
import gamedata.Expansion;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import resources.Settings;

/**
 * A {@code ConversionSession} instance is created by
 * {@link #convertGameComponent(ConversionTrigger, GameComponent)} in order to
 * perform the conversion of a {@link GameComponent} into another component
 * type. The class has several methods to help with the conversion process. Most
 * return the instance itself, so that they can be chained easily.
 *
 * @author Henrik Rostedt
 */
public class ConversionSession {

    private static final String[] EXPANSIONS_SETTINGS_KEYS = new String[]{
        Expansion.EXPANSION_SETTING_KEY,
        Expansion.VARIANT_SETTING_KEY
    };

    private final ConversionTrigger trigger;
    private final GameComponent source;
    private final GameComponent target;
    private final boolean copyPortraitPossible;

    private boolean cancelled = false;
    private String cancelReason;
    private boolean automaticCopyNameEnabled = true;
    private boolean automaticCopyCommentEnabled = true;
    private boolean automaticCopyExpansionsEnabled = true;
    private boolean automaticCopyPortraitsEnabled = true;

    private ConversionSession(ConversionTrigger trigger, GameComponent source, GameComponent target) {
        this.trigger = trigger;
        this.source = source;
        this.target = target;
        copyPortraitPossible = source instanceof PortraitProvider && target instanceof PortraitProvider;
    }

    /**
     * Returns the {@link ConversionTrigger} that initiated the conversion.
     *
     * @return the conversion trigger
     */
    public ConversionTrigger getTrigger() {
        return trigger;
    }

    /**
     * Returns the original component that is being converted. This component
     * should not be modified during the conversion process.
     *
     * @return the source component
     */
    public GameComponent getSource() {
        return source;
    }

    /**
     * Returns the new component that will become the converted component. In
     * most cases it will be easier to use the session methods to copy data,
     * rather than modifying the target directly.
     *
     * @return the target component
     */
    public GameComponent getTarget() {
        return target;
    }

    /**
     * Can be called to cancel the conversion process. This will prevent a
     * converted component from being created.
     *
     * @param reason a description of why the conversion was cancelled
     */
    public void cancel(String reason) {
        cancelled = true;
        cancelReason = reason;
    }

    /**
     * Copies the name of the old component to the new. The new component type
     * must extend {@link AbstractGameComponent}. This action is automatically
     * performed by default.
     *
     * @return the conversion session
     */
    public ConversionSession copyName() {
        automaticCopyNameEnabled = false;
        if (target instanceof AbstractGameComponent) {
            ((AbstractGameComponent) target).setName(source.getName());
        }
        return this;
    }

    /**
     * Copies the comments of the old component to the new. The new component
     * type must extend {@link AbstractGameComponent}. This action is
     * automatically performed by default.
     *
     * @return the conversion session
     */
    public ConversionSession copyComment() {
        automaticCopyCommentEnabled = false;
        if (target instanceof AbstractGameComponent) {
            ((AbstractGameComponent) target).setComment(source.getComment());
        }
        return this;
    }

    /**
     * Copies the settings for the given keys from the old component to the new.
     *
     * @param keys the settings keys to copy
     * @return the conversion session
     */
    public ConversionSession copySettings(String... keys) {
        Settings s = source.getSettings();
        Settings t = target.getSettings();
        for (String key : keys) {
            t.set(key, s.get(key));
        }
        return this;
    }

    /**
     * Copies all settings from the old component to the new. Keys inherited
     * from parent settings are not copied.
     *
     * @return the conversion session
     */
    public ConversionSession copyAllSettings() {
        automaticCopyExpansionsEnabled = false;
        target.getSettings().addSettingsFrom(source.getSettings());
        return this;
    }

    /**
     * Copies all settings from the old component to the new, except for the
     * given keys. Keys inherited from parent settings are not copied.
     *
     * @param excludedKeys the settings keys to exclude
     * @return the conversion session
     */
    public ConversionSession copyAllSettingsExcept(String... excludedKeys) {
        automaticCopyExpansionsEnabled = false;
        Settings s = source.getSettings();
        Settings t = target.getSettings();
        Set<String> keys = s.getKeySet();
        for (String key : excludedKeys) {
            keys.remove(key);
        }
        for (String key : keys) {
            t.set(key, s.get(key));
        }
        return this;
    }

    /**
     * Copies settings from the old component to the new, based on the given key
     * pairs. The setting for the first key in the pair is fetched from the old
     * component and stored using the second key in the new component. If an odd
     * number of keys is given, the last key is ignored.
     *
     * @param pairs the pairs of keys to copy
     * @return the conversion session
     */
    public ConversionSession moveSettings(String... pairs) {
        Settings s = source.getSettings();
        Settings t = target.getSettings();
        int count = pairs.length / 2;
        for (int i = 0; i < count; i++) {
            t.set(pairs[i * 2 + 1], s.get(pairs[i * 2]));
        }
        return this;
    }

    /**
     * Copies the expansions from the old component to the new. This action is
     * automatically performed by default.
     *
     * @return the conversion session
     */
    public ConversionSession copyExpansions() {
        automaticCopyExpansionsEnabled = false;
        return copySettings(EXPANSIONS_SETTINGS_KEYS);
    }

    /**
     * Copies a portrait from the old component to the new. Both component types
     * must extend {@link PortraitProvider}. If the source image file can not be
     * found, a temporary file is used for the conversion. Calling this method
     * will disable automatic portrait copying.
     *
     * @param sourceIndex the index of the portrait in the source component
     * @param targetIndex the index of the portrait in the target component
     * @param copyLayout whether to copy the adjustments of the portrait or not
     * @return the conversion session
     */
    public ConversionSession copyPortrait(int sourceIndex, int targetIndex, boolean copyLayout) {
        disableAutomaticCopyPortraits();
        if (!copyPortraitPossible) {
            return this;
        }
        Portrait sp = ((PortraitProvider) source).getPortrait(sourceIndex);
        Portrait tp = ((PortraitProvider) target).getPortrait(targetIndex);
        if (tp.getFeatures().contains(Portrait.Feature.SOURCE)) {
            String imageSource = sp.getSource();
            if (imageSource != null && new File(imageSource).canRead()) {
                tp.setSource(imageSource);
            } else {
                File tempFile = null;
                try {
                    tempFile = File.createTempFile("conversion-", ".png");
                    new SimpleImageWriter().write(sp.getImage(), tempFile);
                    tp.setSource(tempFile.getAbsolutePath());
                } catch (IOException e) {
                    tp.setSource(null);
                } finally {
                    if (tempFile != null) {
                        tempFile.delete();
                    }
                }
            }
            if (copyLayout) {
                tp.setPanX(sp.getPanX());
                tp.setPanY(sp.getPanY());
                tp.setRotation(sp.getRotation());
                tp.setScale(sp.getScale());
            }
        }
        return this;
    }

    /**
     * Copies a portrait from the old component to the new. Both component types
     * must extend {@link PortraitProvider}. If the source image file can not be
     * found, a temporary file is used for the conversion. Calling this method
     * will disable automatic portrait copying.
     *
     * @param index the index of the portrait
     * @param copyLayout whether to copy the adjustments of the portrait or not
     * @return the conversion session
     */
    public ConversionSession copyPortrait(int index, boolean copyLayout) {
        return copyPortrait(index, index, copyLayout);
    }

    /**
     * Copies the portraits from the old component to the new. Both component
     * types must extend {@link PortraitProvider}. If the source image file for
     * a portrait can not be found, a temporary file is used for the conversion.
     * This action is automatically performed by default.
     *
     * @param copyLayouts whether to copy the individual adjustments of the
     * portraits or not
     * @return the conversion session
     */
    public ConversionSession copyAllPortraits(boolean copyLayouts) {
        if (!copyPortraitPossible) {
            return this;
        }
        int sourceCount = ((PortraitProvider) source).getPortraitCount();
        int targetCount = ((PortraitProvider) target).getPortraitCount();
        int count = Math.min(sourceCount, targetCount);
        for (int i = 0; i < count; i++) {
            copyPortrait(i, copyLayouts);
        }
        return this;
    }

    /**
     * Disables the automatic conversion for component name, comments and
     * expansions. Needs to be called if changing any of these on the target
     * component manually.
     *
     * @return the conversion session
     */
    public ConversionSession disableAutomaticCopyBasics() {
        automaticCopyNameEnabled = false;
        automaticCopyCommentEnabled = false;
        automaticCopyExpansionsEnabled = false;
        return this;
    }

    /**
     * Disables the automatic conversion for component portraits. Needs to be
     * called if modifying portraits on the target component manually.
     *
     * @return the conversion session
     */
    public ConversionSession disableAutomaticCopyPortraits() {
        automaticCopyPortraitsEnabled = false;
        return this;
    }

    /**
     * Disables all automatic conversion steps.
     *
     * @return the conversion session
     */
    public ConversionSession disableAutomaticConversion() {
        automaticCopyNameEnabled = false;
        automaticCopyCommentEnabled = false;
        automaticCopyExpansionsEnabled = false;
        automaticCopyPortraitsEnabled = false;
        return this;
    }

    /**
     * Performs the enabled automatic conversion steps, then disables all
     * conversion steps. This method is automatically called before the
     * conversion is completed. It can be called before manually modifying the
     * target component, so the changes are not overwritten.
     *
     * @return the conversion session
     */
    public ConversionSession performAutomaticConversion() {
        if (automaticCopyNameEnabled) {
            copyName();
        }
        if (automaticCopyCommentEnabled) {
            copyComment();
        }
        if (automaticCopyExpansionsEnabled) {
            copyExpansions();
        }
        if (automaticCopyPortraitsEnabled) {
            copyAllPortraits(true);
        }
        disableAutomaticConversion();
        return this;
    }

    /**
     * Checks if the conversion has been cancelled.
     *
     * @throws ConversionException if the conversion is cancelled
     */
    private void checkForCancellation() throws ConversionException {
        if (cancelled) {
            if (cancelReason == null) {
                throw new ConversionException("conversion cancelled");
            }
            throw new ConversionException("conversion cancelled: " + cancelReason);
        }
    }

    /**
     * Checks if an extension is required, and if so, that it is installed.
     *
     * @param name the name of the extension
     * @param rawId the UUID of the extension
     * @throws ConversionException if the extension can not be found or if it is
     * not installed
     */
    private static void checkExtension(String name, String rawId) throws ConversionException {
        if (rawId == null) {
            return;
        }
        UUID id;
        try {
            id = UUID.fromString(rawId);
        } catch (IllegalArgumentException e) {
            throw new ConversionException("malformed extension UUID: " + rawId);
        }
        if (BundleInstaller.getBundleFileForUUID(id) != null) {
            return;
        }
        if (name != null) {
            throw new ConversionException("required extension not installed: " + name);
        }
        throw new ConversionException("required extension not installed: " + rawId);
    }

    /**
     * Creates a new {@link GameComponent} to be used as the target of the
     * conversion.
     *
     * @param className the identifier of the new component type
     * @return the created target component
     * @throws ConversionException if the component can not be created
     */
    private static GameComponent createTarget(String className) throws ConversionException {
        if (className == null) {
            throw new ConversionException("target component type not specified");
        }
        if (className.startsWith("script:")) {
            throw new ConversionException("can not convert to script components");
        }
        try {
            if (className.startsWith("diy:")) {
                return new DIY(className.substring("diy:".length()), null, false);
            } else {
                return (GameComponent) Class.forName(className).getConstructor().newInstance();
            }
        } catch (Exception e) {
            throw new ConversionException("unable to crate a new instance of " + className, e);
        }
    }

    /**
     * Creates a new {@link GameComponent} which represents the given component
     * converted to another component type. The old component should remain
     * unchanged after the conversion is performed. First a new game component
     * is created based on the provided {@link ConversionTrigger} and a
     * {@link ConversionSession} is created. Then the old and new components
     * each get the opportunity to modify the new component. Lastly, some
     * automatic conversion steps may be performed.
     *
     * @param trigger the trigger for the conversion
     * @param source the component to be converted
     * @return the new converted component
     * @throws ConversionException if the conversion was unsuccessful
     */
    public static GameComponent convertGameComponent(ConversionTrigger trigger, GameComponent source) throws ConversionException {
        checkExtension(trigger.getRequiredExtensionName(), trigger.getRequiredExtensionId());
        GameComponent target = createTarget(trigger.getTargetClassName());
        ConversionSession session = new ConversionSession(trigger, source, target);
        try {
            source.convertFrom(session);
        } catch (Exception e) {
            throw new ConversionException("exception when converting from source", e);
        }
        session.checkForCancellation();
        try {
            target.convertTo(session);
        } catch (Exception e) {
            throw new ConversionException("exception when converting to target", e);
        }
        session.checkForCancellation();
        try {
            session.performAutomaticConversion();
        } catch (Exception e) {
            throw new ConversionException("exception when performing automatic conversion", e);
        }
        return target;
    }
}
