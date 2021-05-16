package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.ui.IconProvider;
import java.util.Locale;
import javax.swing.Icon;

/**
 * An action that can be performed on a member of project.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public abstract class TaskAction implements IconProvider {

    public TaskAction() {
    }

    /**
     * Returns the human-readable name of this action.
     *
     * @return the name used to create menu items for this action, localized if
     * possible
     */
    public abstract String getLabel();

    /**
     * Returns a unique internal name for this action. Actions can be looked up
     * by this name. The default implementation returns the class name,
     * converted to lowercase.
     *
     * @return the internal name of this action
     */
    public String getActionName() {
        if (cachedName == null) {
            cachedName = getClass().getSimpleName().toLowerCase(Locale.CANADA);
        }
        return cachedName;
    }
    private String cachedName;

    /**
     * Returns an icon for this action, or {@code null} if the action
     * should not be associated with an icon.
     *
     * @return an icon that represents the action
     */
    @Override
    public Icon getIcon() {
        return null;
    }

    /**
     * Returns a longer description of the action, suitable for use as a tool
     * tip.
     *
     * @return a long description of the action, or {@code null}
     */
    public String getDescription() {
        return null;
    }

    @Override
    public String toString() {
        return getLabel();
    }

    /**
     * Perform this action on a member of a project, a project or a task. If the
     * project itself is the target, {@code task} and {@code member}
     * will be {@code null}. If a task is the target, then
     * {@code member} will be null. If an error occurs while executing the
     * task, then it is the action's responsibility to inform the user. This
     * method can return {@code false} to indicate that if the action is
     * being applied to multiple members, it should stop immediately rather than
     * continue to the next member.
     *
     * @param project the project that is being acted upon
     * @param task the task within the project that is being acted upon;
     * {@code null} if acting on a project
     * @param member the specific member within the task to act upon;
     * {@code null} if this is a project or task
     */
    public abstract boolean perform(Project project, Task task, Member member);

    /**
     * Returns {@code true} if this action can be performed on the
     * specified member of a project, project or a task. If the project itself
     * is the target, {@code task} and {@code member} will be
     * {@code null}. If a task is the target, then {@code member} will
     * be null. If an error occurs while executing the task, then it is the
     * action's responsibility to inform the user. This method can return
     * {@code false} to indicate that if the action is being applied to
     * multiple members, it should stop immediately rather than continue to the
     * next member.
     *
     * @param project the project that is being acted upon
     * @param task the task within the project that is being acted upon;
     * {@code null} if acting on a project
     * @param member the specific member within the task to act upon;
     * {@code null} if this is a project or task
     */
    public abstract boolean appliesTo(Project project, Task task, Member member);

    /**
     * Returns the member being targeted by action regardless of whether it is a
     * project, task, or task member. If your action can be applied to any type
     * of member, you can call this to simplify your handling code. It returns
     * the first of {@code member}, {@code task}, or
     * {@code project} that is non-{@code null}.
     *
     * @param project the project passed in to the action
     * @param task the task passed in to the action
     * @param member the member passed in to the action
     * @return the member that the parameters refer to, regardless of whether it
     * is a project, task, or task member
     */
    protected final Member resolveTarget(Project project, Task task, Member member) {
        if (member != null) {
            return member;
        }
        if (task != null) {
            return task;
        }
        if (project != null) {
            return project;
        }
        throw new AssertionError("null project");
    }

    /**
     * Returns {@code true} if this action is applicable to any of the
     * specified {@link Member}s. By overriding this, you can modify whether an
     * action is listed depending on which other members are selected. For
     * example, you could create a command that can only be applied to a
     * singleton selection by checking the length of {@code members} and
     * returning {@code false} if it is not 1, and otherwise calling the
     * super implementation.
     *
     * @param members a list of zero or more members
     * @return {@code true} is the action can be applied to at least one
     * member
     */
    public boolean appliesToSelection(Member[] members) {
        for (Member m : members) {
            if (appliesTo(m)) {
                return true;
            }
        }
        return false;
    }

    private boolean appliesTo(Member m) {
        Task t = null;
        Project p = null;
        if (!(m instanceof Project)) {
            p = m.getProject();
            if (!(m instanceof Task)) {
                t = m.getTask();
            } else {
                t = (Task) m;
                m = null;
            }
        } else {
            p = (Project) m;
            m = null;
        }
        return appliesTo(p, t, m);
    }

    /**
     * Applies this action to all of the specified project members. By
     * overriding this, you can modify what happens when the user tries to
     * initiate this action. For example, you could add a verification dialog
     * and call the super implementation only if the user verifies the action.
     *
     * @param members
     * @return {@code true} if and only if the action is successfully
     * applied to all of the members
     */
    public boolean performOnSelection(Member[] members) {
        boolean success = true;
        for (Member m : members) {
            success &= perform(m);
        }
        return success;
    }

    private boolean perform(Member m) {
        Task t = null;
        Project p = null;
        if (!(m instanceof Project)) {
            p = m.getProject();
            if (!(m instanceof Task)) {
                t = m.getTask();
            } else {
                t = (Task) m;
                m = null;
            }
        } else {
            p = (Project) m;
            m = null;
        }
        return perform(p, t, m);
    }

    /**
     * Recursively apply a task action to all of the children of a member that
     * that action applies to. The application is depth first---an action that
     * would lead to deleting a tree can succeed because the children would be
     * deleted first.
     *
     * @param project
     * @param task
     * @param parent
     */
    public void applyToChildren(Project project, Task task, Member parent) {
        if (!parent.isFolder()) {
            return;
        }
        for (Member kid : parent.getChildren()) {
            if (kid.isFolder()) {
                applyToChildren(project, task, kid);
            }
            if (this.appliesTo(project, task, kid)) {
                this.perform(project, task, kid);
            }
        }
    }
}
