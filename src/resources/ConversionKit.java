package resources;

import ca.cgjennings.apps.arkham.component.AbstractGameComponent;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.component.Portrait;
import ca.cgjennings.apps.arkham.component.Portrait.Feature;
import ca.cgjennings.apps.arkham.component.PortraitProvider;
import ca.cgjennings.apps.arkham.component.ConversionContext;
import ca.cgjennings.imageio.SimpleImageWriter;
import gamedata.Expansion;
import java.io.File;
import java.io.IOException;

/**
 * Utility methods for converting components. Is used for the automatic
 * conversion steps. But it can also be used directly by components during
 * {@link GameComponent#convertFrom(GameComponent, ConversionContext)} and
 * {@link GameComponent#convertTo(GameComponent, ConversionContext)}, or by DIY
 * scripts during {@code onConvertFrom} and {@code onConvertTo}.
 *
 * @author Henrik Rostedt
 */
public class ConversionKit {

    private static final String[] EXPANSION_KEYS = new String[]{
        Expansion.EXPANSION_SETTING_KEY,
        Expansion.VARIANT_SETTING_KEY
    };

    /**
     * This class cannot be instantiated.
     */
    private ConversionKit() {
    }

    /**
     * Copies the settings from one component to another. Keys inherited from
     * parent settings are not copied.
     *
     * @param source the source component
     * @param target the target component
     */
    public static void copySettings(GameComponent source, GameComponent target) {
        target.getSettings().addSettingsFrom(source.getSettings());
    }

    /**
     * Copies the specified settings from one component to another.
     *
     * @param source the source component
     * @param target the target component
     * @param keys the keys of the settings to copy
     */
    public static void copyKeys(GameComponent source, GameComponent target, String... keys) {
        for (String key : keys) {
            target.getSettings().set(key, source.getSettings().get(key));
        }
    }

    /**
     * Copies the expansion related settings from one component to another.
     *
     * @param source the source component
     * @param target the target component
     */
    public static void copyExpansions(GameComponent source, GameComponent target) {
        copyKeys(source, target, EXPANSION_KEYS);
    }

    /**
     * Copies the name, comments and expansion related settings from one
     * component to another. The target component must extend
     * {@link AbstractGameComponent} for name and comments to be copied.
     *
     * @param source the source component
     * @param target the target component
     */
    public static void copyBasics(GameComponent source, GameComponent target) {
        if (target instanceof AbstractGameComponent) {
            AbstractGameComponent t = (AbstractGameComponent) target;
            t.setName(source.getName());
            t.setComment(source.getComment());
        }
        copyExpansions(source, target);
    }

    /**
     * Copies the portraits from one component to another. Both components must
     * implement {@link PortraitProvider}. The portraits are assumed to be in
     * the same order. If the portrait counts differ, the excess is ignored. If
     * an image file can not be found, a temporary file is created for the
     * conversion. It is possible to adjust what portrait data should be copied.
     *
     * @param source the source component
     * @param target the target component
     * @param copyImages whether to copy the portrait images or not
     * @param copyLayouts whether to copy the portrait adjustments or not
     */
    public static void copyPortraits(GameComponent source, GameComponent target, boolean copyImages, boolean copyLayouts) {
        if (!(source instanceof PortraitProvider && target instanceof PortraitProvider)) {
            return;
        }
        PortraitProvider s = (PortraitProvider) source;
        PortraitProvider t = (PortraitProvider) target;
        int count = Math.min(s.getPortraitCount(), t.getPortraitCount());
        for (int i = 0; i < count; i++) {
            Portrait sp = s.getPortrait(i);
            Portrait tp = t.getPortrait(i);
            if (copyImages && tp.getFeatures().contains(Feature.SOURCE)) {
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
            }
            if (copyLayouts) {
                tp.setPanX(sp.getPanX());
                tp.setPanY(sp.getPanY());
                tp.setRotation(sp.getRotation());
                tp.setScale(sp.getScale());
            }
        }
    }

    /**
     * Copies the requested data from one component to another based on the
     * provided {@link ConversionContext}.
     *
     * @param source the source component
     * @param target the target component
     * @param context the conversion context
     */
    public static void copyRequestedData(GameComponent source, GameComponent target, ConversionContext context) {
        if (context.shouldCopySettings()) {
            copySettings(source, target);
        }
        if (context.shouldCopyBasics()) {
            copyBasics(source, target);
        }
        if (context.shouldCopyPortraits()) {
            copyPortraits(source, target, context.shouldCopyPortraitImages(), context.shouldCopyPortraitLayouts());
        }
    }
}
