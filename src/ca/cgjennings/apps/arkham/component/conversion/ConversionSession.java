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

    public ConversionTrigger getTrigger() {
        return trigger;
    }

    public GameComponent getSource() {
        return source;
    }

    public GameComponent getTarget() {
        return target;
    }

    public void cancel(String reason) {
        cancelled = true;
        cancelReason = reason;
    }

    public ConversionSession copyName() {
        if (target instanceof AbstractGameComponent) {
            ((AbstractGameComponent) target).setName(source.getName());
        }
        return this;
    }

    public ConversionSession copyComment() {
        if (target instanceof AbstractGameComponent) {
            ((AbstractGameComponent) target).setComment(source.getComment());
        }
        return this;
    }

    public ConversionSession copySettings(String... keys) {
        Settings s = source.getSettings();
        Settings t = target.getSettings();
        for (String key : keys) {
            t.set(key, s.get(key));
        }
        return this;
    }

    public ConversionSession copyAllSettings() {
        automaticCopyExpansionsEnabled = false;
        target.getSettings().addSettingsFrom(source.getSettings());
        return this;
    }

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

    public ConversionSession moveSettings(String... pairs) {
        Settings s = source.getSettings();
        Settings t = target.getSettings();
        int count = pairs.length / 2;
        for (int i = 0; i < count; i++) {
            t.set(pairs[i * 2 + 1], s.get(pairs[i * 2]));
        }
        return this;
    }

    public ConversionSession copyExpansions() {
        automaticCopyExpansionsEnabled = false;
        return copySettings(EXPANSIONS_SETTINGS_KEYS);
    }

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

    public ConversionSession copyPortrait(int index, boolean copyLayout) {
        return copyPortrait(index, index, copyLayout);
    }

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

    public ConversionSession disableAutomaticCopyBasics() {
        automaticCopyNameEnabled = false;
        automaticCopyCommentEnabled = false;
        automaticCopyExpansionsEnabled = false;
        return this;
    }

    public ConversionSession disableAutomaticCopyPortraits() {
        automaticCopyPortraitsEnabled = false;
        return this;
    }

    public ConversionSession disableAutomaticConversion() {
        automaticCopyNameEnabled = false;
        automaticCopyCommentEnabled = false;
        automaticCopyExpansionsEnabled = false;
        automaticCopyPortraitsEnabled = false;
        return this;
    }

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

    private void checkForCancellation() throws ConversionException {
        if (cancelled) {
            if (cancelReason == null) {
                throw new ConversionException("conversion cancelled");
            }
            throw new ConversionException("conversion cancelled: " + cancelReason);
        }
    }

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
