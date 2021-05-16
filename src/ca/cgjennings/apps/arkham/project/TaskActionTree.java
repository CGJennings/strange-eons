package ca.cgjennings.apps.arkham.project;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * An abstract {@link TaskAction} for actions that contain one or more child
 * actions. The action is applicable if any of the children are applicable. A
 * {@code TaskActionTree} is never performed, but it's children can be.
 * Adding a {@code null} value as a child indicates logical separation
 * between two groups of children. This may be manifested, for example, as a
 * separator in a command menu.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public abstract class TaskActionTree extends TaskAction implements Iterable<TaskAction> {

    private final LinkedList<TaskAction> actions = new LinkedList<>();

//	private HashMap<Integer,List<TaskAction>> actionMap = new HashMap<Integer,List<TaskAction>>();
//	private HashMap<TaskAction,Integer> priorityMap = new HashMap<TaskAction,Integer>();
    @Override
    public boolean perform(Project project, Task task, Member member) {
        throw new UnsupportedOperationException("CompoundTaskActions are not performed. They contain other actions.");
    }

    @Override
    public boolean appliesToSelection(Member[] members) {
        final boolean sc = isAppliesToShortCircuited();
        for (TaskAction ta : actions) {
            if (ta != null && ta.appliesToSelection(members)) {
                return true;
            }
            if (sc) {
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        final boolean sc = isAppliesToShortCircuited();
        for (TaskAction ta : actions) {
            if (ta != null && ta.appliesTo(project, task, member)) {
                return true;
            }
            if (sc) {
                return false;
            }
        }
        return false;
    }

    public final void add(TaskAction ta) {
        add(actions.size(), ta);
    }

    public void add(int index, TaskAction ta) {
        actions.add(index, ta);
    }

    /**
     * Returns the index at which to
     * {@link #add(int, ca.cgjennings.apps.arkham.project.TaskAction)} an action
     * in order to place it at the start or end of the numbered section. A new
     * section begins whenever a {@code null} action (separator) appears in
     * the list of actions; the first section (before any separator) is numbered
     * 0. If the specified section number is higher than the actual number of
     * sections, the returned index will append actions to the end of the list.
     *
     * @param section the section number
     * @param placeAtEndOfSection if {@code true}, the index will locate
     * the action at the end of the section rather than the start
     * @return the index at which to add an action to place it at the specified
     * position in the specified section
     * @throws IllegalArgumentException if the section number is negative
     */
    public int getSectionIndex(int section, boolean placeAtEndOfSection) {
        if (section < 0) {
            throw new IllegalArgumentException("section < 0: " + section);
        }
        int inSection = 0;
        int pos = 0;
        while (pos < actions.size() && inSection < section) {
            if (actions.get(pos++) == null) {
                ++inSection;
            }
        }

        if (placeAtEndOfSection) {
            while (pos < actions.size() && actions.get(pos) != null) {
                ++pos;
            }
        }

        return pos;
    }

    public TaskAction remove(int index) {
        return actions.remove(index);
    }

    public TaskAction remove(TaskAction ta) {
        int i = indexOf(ta);
        if (i < 0) {
            return null;
        }
        return remove(i);
    }

    public TaskAction get(int index) {
        return actions.get(index);
    }

    public int size() {
        return actions.size();
    }

    public int indexOf(TaskAction ta) {
        for (int i = 0; i < actions.size(); ++i) {
            if (actions.get(i).equals(ta)) {
                return i;
            }
        }
        return -1;
    }

    public TaskAction findActionByName(String name) {
        for (int i = 0; i < actions.size(); ++i) {
            TaskAction ta = actions.get(i);
            if (ta == null) {
                continue;
            }
            if (ta.getActionName().equals(name)) {
                return ta;
            }
            if (ta instanceof TaskActionTree) {
                ta = ((TaskActionTree) ta).findActionByName(name);
                if (ta != null) {
                    return ta;
                }
            }
        }
        return null;
    }

    @Override
    public Iterator<TaskAction> iterator() {
        return actions.iterator();
    }

    /**
     * If this method returns {@code true}, then the evaluation of
     * {@link #appliesToSelection} and {@link #appliesTo} will assume that every
     * child action performs the same test. Therefore, if the first child action
     * tested does not apply, none of them can apply and the search ends
     * immediately. Otherwise, every child action will be tested to see if it
     * applies until at least one is found that does or they all fail.
     *
     * <p>
     * The base class returns {@code false}.
     *
     * @return {@code true} if all children apply to exactly same set of
     * members
     */
    protected boolean isAppliesToShortCircuited() {
        return false;
    }
}
