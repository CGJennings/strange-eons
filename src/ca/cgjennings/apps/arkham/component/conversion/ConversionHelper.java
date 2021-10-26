package ca.cgjennings.apps.arkham.component.conversion;

import ca.cgjennings.apps.arkham.component.AbstractGameComponent;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.component.Portrait;
import ca.cgjennings.apps.arkham.component.PortraitProvider;
import gamedata.Expansion;

public class ConversionHelper {

    private static final String[] EXPANSION_KEYS = new String[]{
        Expansion.EXPANSION_SETTING_KEY,
        Expansion.VARIANT_SETTING_KEY
    };

    private ConversionHelper() {
    }

    public static void copySettings(GameComponent source, GameComponent target) {
        target.getSettings().addSettingsFrom(source.getSettings());
    }

    public static void copyExpansions(GameComponent source, GameComponent target) {
        for (String key : EXPANSION_KEYS) {
            target.getSettings().set(key, source.getSettings().get(key));
        }
    }

    public static void copyBasics(GameComponent source, GameComponent target) {
        if (target instanceof AbstractGameComponent) {
            AbstractGameComponent t = (AbstractGameComponent) target;
            t.setName(source.getName());
            t.setComment(source.getComment());
        }
        copyExpansions(source, target);
    }

    public static void copyPortraits(GameComponent source, GameComponent target) {
        if (!(source instanceof PortraitProvider && target instanceof PortraitProvider)) {
            return;
        }
        PortraitProvider s = (PortraitProvider) source;
        PortraitProvider t = (PortraitProvider) target;
        int count = Math.min(s.getPortraitCount(), t.getPortraitCount());
        for (int i = 0; i < count; i++) {
            Portrait sp = s.getPortrait(i);
            Portrait tp = t.getPortrait(i);
            tp.setImage(sp.getSource(), sp.getImage());
            tp.setPanX(sp.getPanX());
            tp.setPanY(sp.getPanY());
            tp.setRotation(sp.getRotation());
            tp.setScale(sp.getScale());
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
            copyPortraits(source, target);
        }
    }
}
