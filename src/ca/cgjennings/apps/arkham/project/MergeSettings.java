package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import gamedata.Game;
import gamedata.Lock;
import java.util.Iterator;
import javax.swing.Icon;
import static resources.Language.string;
import resources.Settings;

/**
 * A task action that loads settings from a settings file into the shared or a
 * game's master settings. Useful when developing plug-ins that require certain
 * settings to be available, such as DIY component scripts.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class MergeSettings extends TaskActionTree {

    public MergeSettings() {
    }

    @Override
    public String getLabel() {
        return string("pa-merge-settings");
    }

    @Override
    public void add(int index, TaskAction ta) {
        if (ta != null && !(ta instanceof MergeAction)) {
            throw new IllegalArgumentException("no other children can be added to " + getClass().getSimpleName());
        }
        super.add(index, ta);
    }

    @Override
    public TaskAction get(int index) {
        loadChildren();
        return super.get(index);
    }

    @Override
    public Iterator<TaskAction> iterator() {
        loadChildren();
        return super.iterator();
    }

    @Override
    public int size() {
        loadChildren();
        return super.size();
    }

    @Override
    public boolean appliesToSelection(Member[] members) {
        loadChildren();
        return super.appliesToSelection(members);
    }

    /*
	 * The children of this action are not added until the first
	 * time someone tries to access them after the game database has been locked.
	 * This ensures that all games are installed before the children are created,
	 * but it means that in the event that someone tries to access the children
	 * before the database is locked, it will appear to have zero children.
     */
    private void loadChildren() {
        if (loadedChildren || !Lock.hasBeenLocked()) {
            return;
        }

        loadedChildren = true;
        add(new MergeAction(null));
        add(null);
        for (Game g : Game.getGames(true)) {
            add(new MergeAction(g));
        }
    }
    private boolean loadedChildren = false;

    /**
     * {@inheritDoc}
     *
     * <p>
     * This action tree returns true since all of its children represent
     * settings instances with which a settings file could be merged.
     *
     * @return <code>true</code>
     */
    @Override
    protected boolean isAppliesToShortCircuited() {
        return true;
    }

    private static class MergeAction extends TaskAction {

        private final Game game;

        public MergeAction(Game g) {
            game = g;
        }

        @Override
        public String getLabel() {
            if (game == null) {
                return string("pa-merge-shared");
            } else {
                return game.getUIName();
            }
        }

        @Override
        public boolean appliesTo(Project project, Task task, Member member) {
            return member != null && !member.isFolder() && ProjectUtilities.matchExtension(member, "settings");
        }

        @Override
        public boolean perform(Project project, Task task, Member member) {
            if (!appliesTo(project, task, member)) {
                return false;
            }

            ProjectUtilities.saveIfBeingEdited(member.getFile());

            // member cannot be null since appliesTo passed
            Settings s = (game == null) ? Settings.getShared() : game.getMasterSettings();
            try {
                s.addSettingsFrom(member.getURL().toExternalForm());
                return true;
            } catch (Exception e) {
                ErrorDialog.displayError(string("prj-err-open", member.getName()), e);
            }
            return false;
        }

        @Override
        public Icon getIcon() {
            return game == null ? null : game.getIcon();
        }

        @Override
        public String getActionName() {
            return "MergeSettings<" + (game != null ? game.getCode() : "") + '>';
        }
    }
}
