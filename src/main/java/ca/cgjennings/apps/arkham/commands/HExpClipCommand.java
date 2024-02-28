package ca.cgjennings.apps.arkham.commands;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.component.GameComponent;
import gamedata.Expansion;
import java.awt.event.ActionEvent;
import resources.Settings;

/**
 * A helper class for creating the expansion symbol copy/paste commands.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
final class HExpClipCommand extends DelegatedCommand {

    private final boolean isCopy;

    public HExpClipCommand(boolean isCopy) {
        super(isCopy ? "app-copy-exp" : "app-paste-exp");
        this.isCopy = isCopy;
    }

    @Override
    public void performDefaultAction(ActionEvent e) {
        StrangeEonsEditor ed = StrangeEons.getActiveEditor();
        if (ed != null && ed.getGameComponent() != null) {
            GameComponent gc = ed.getGameComponent();
            Settings s = gc.getSettings();
            if (isCopy) {
                game = s.get(gamedata.Game.GAME_SETTING_KEY, gamedata.Game.ALL_GAMES_CODE);
                exp = s.getExpansionCode();
                var = s.get(Expansion.VARIANT_SETTING_KEY);
                hasClip = true;
            } else {
                s.set(Expansion.EXPANSION_SETTING_KEY, exp);
                s.set(Expansion.VARIANT_SETTING_KEY, var);
                ((AbstractGameComponentEditor<?>) ed).redrawPreview();
            }
        }
    }

    @Override
    public boolean isDefaultActionApplicable() {
        StrangeEonsEditor ed = StrangeEons.getActiveEditor();
        boolean enable = ed != null && ed.getGameComponent() != null;
        if (!isCopy) {
            enable &= hasClip;
            enable &= game != null && (game.equals(ed.getGameComponent().getSettings().get(gamedata.Game.GAME_SETTING_KEY)) || game.equals(gamedata.Game.ALL_GAMES_CODE));
        }
        return enable;
    }

    private static boolean hasClip;
    private static String game, exp, var;
}
