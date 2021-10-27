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

public class ConversionKit {

    private static final String[] EXPANSION_KEYS = new String[]{
        Expansion.EXPANSION_SETTING_KEY,
        Expansion.VARIANT_SETTING_KEY
    };

    private ConversionKit() {
    }

    public static void copySettings(GameComponent source, GameComponent target) {
        target.getSettings().addSettingsFrom(source.getSettings());
    }

    public static void copyKeys(GameComponent source, GameComponent target, String... keys) {
        for (String key : keys) {
            target.getSettings().set(key, source.getSettings().get(key));
        }
    }

    public static void copyExpansions(GameComponent source, GameComponent target) {
        copyKeys(source, target, EXPANSION_KEYS);
    }

    public static void copyBasics(GameComponent source, GameComponent target) {
        if (target instanceof AbstractGameComponent) {
            AbstractGameComponent t = (AbstractGameComponent) target;
            t.setName(source.getName());
            t.setComment(source.getComment());
        }
        copyExpansions(source, target);
    }

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
