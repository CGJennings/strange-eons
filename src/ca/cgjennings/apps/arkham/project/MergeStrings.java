package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import javax.swing.Icon;
import resources.Language;
import static resources.Language.string;

/**
 * A task action that loads strings from a string table into the game or UI
 * language. Useful when developing plug-ins that use localized text.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class MergeStrings extends TaskActionTree {

    public MergeStrings() {
        add(new MergeAction(Language.getInterface(), string("pa-merge-strings-ui")));
        add(new MergeAction(Language.getGame(), string("pa-merge-strings-game")));
        add(null);
        add(new MergeAction(null, string("pa-merge-strings-both")));
    }

    @Override
    public String getLabel() {
        return string("pa-merge-strings");
    }

    @Override
    public void add(int index, TaskAction ta) {
        if (ta != null && !(ta instanceof MergeAction)) {
            throw new IllegalArgumentException("no other children can be added to this action");
        }
        super.add(index, ta);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This action tree returns true since all of its children apply only to
     * .properties files.
     *
     * @return <code>true</code>
     */
    @Override
    protected boolean isAppliesToShortCircuited() {
        return true;
    }

    private static class MergeAction extends TaskAction {

        private final Language lang;
        private final String label;

        public MergeAction(Language lang, String label) {
            this.lang = lang;
            this.label = label;
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public boolean appliesTo(Project project, Task task, Member member) {
            if (member != null && !member.isFolder() && ProjectUtilities.matchExtension(member, "properties")) {
                Language.LocalizedFileName lfn = new Language.LocalizedFileName(member.getFile());
                return member.getParent().findChild(lfn.baseName + '.' + lfn.extension) != null;
            }
            return false;
        }

        @Override
        public boolean perform(Project project, Task task, Member member) {
            if (!appliesTo(project, task, member)) {
                return false;
            }

            // member cannot be null since appliesTo passed
            try {
                Language.LocalizedFileName lfn = new Language.LocalizedFileName(member.getFile());
                Member base = member.getParent().findChild(lfn.baseName + '.' + lfn.extension);
                String resource = base.getURL().toExternalForm();
                if (lang == null) {
                    Language.getInterface().addStrings(resource);
                    Language.getGame().addStrings(resource);
                } else {
                    lang.addStrings(resource);
                }
                return true;
            } catch (Exception e) {
                ErrorDialog.displayError(string("prj-err-open", member.getName()), e);
            }
            return false;
        }

        @Override
        public Icon getIcon() {
            if (lang == null) {
                return null;
            }
            return Language.getIconForLocale(lang.getLocale());
        }

        @Override
        public String getActionName() {
            return "MergeStrings<" + (lang == Language.getGame() ? "game" : "interface") + '>';
        }
    }
}
