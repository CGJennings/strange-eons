package ca.cgjennings.apps.arkham.project;

import static resources.Language.string;

/**
 * Task action to add a new locale based on an existing {@code .properties}
 * file.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class AddLocale extends TaskAction {

    @Override
    public String getLabel() {
        return string("pa-at-name");
    }

    @Override
    public String getDescription() {
        return string("pa-at-desc");
    }

    @Override
    public boolean performOnSelection(Member[] members) {
        cascade = 0;
        return super.performOnSelection(members);
    }

    private int cascade;

    @Override
    public boolean perform(Project project, Task task, Member member) {
        AddLocaleDialog d = new AddLocaleDialog(member.getFile());
        project.getView().moveToLocusOfAttention(d, ++cascade);
        d.setVisible(true);
        return true;
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        if (member == null) {
            return false;
        }

        // *.properties
        if (ProjectUtilities.matchExtension(member, "properties")) {
            return true;
        }

        // install.html
        if (!member.getName().startsWith("install")) {
            return false;
        }
        if (!ProjectUtilities.matchExtension(member, "html")) {
            return false;
        }
        return member.getParent() instanceof Task;
    }
}
