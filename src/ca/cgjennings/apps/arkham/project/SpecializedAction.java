package ca.cgjennings.apps.arkham.project;

/**
 * A superclass for actions that specialize one of the built-in actions by
 * modifying their behaviour. The same action can be specialized multiple times.
 * As long as each instance is well-behaved and passes all calls that they don't
 * care about on to {@code superAction}, multiple specializations can only
 * interfere with each other if there is overlap between the cases they
 * specialize. When two or more actions specialize the same case (such as files
 * with a certain extension), only the last one registered takes effect.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public abstract class SpecializedAction extends TaskAction {

    protected TaskAction superAction;
    private SpecializedAction parent;

    /**
     * Creates a specialized action that is initially uninstalled. Specialized
     * actions must be installed onto an existing action. The result of trying
     * to use this action before it is installed are undefined.
     */
    public SpecializedAction() {
    }

    /**
     * Use this instance to specialize an already registered action.
     *
     * @param action the action that this action specializes
     * @throws IllegalArgumentException if {@code action} is not registered
     */
    public SpecializedAction(TaskAction action) {
        install(action);
    }

    /**
     * Use this instance to specialize an already registered action by name.
     *
     * @param name the name of the action that this action specializes
     * @throws IllegalArgumentException if there is no action registered with
     * the given name
     */
    public SpecializedAction(String name) {
        install(name);
    }

    /**
     * Installs this specialized action as a specialized version of the action
     * with the given name.
     *
     * @param name the name of the action to specialize
     * @throws IllegalArgumentException if no task is registered with the given
     * name
     * @see #install(ca.cgjennings.apps.arkham.project.TaskAction)
     */
    public void install(String name) {
        TaskAction child = Actions.findActionByName(name);

        if (child == null) {
            throw new IllegalArgumentException("no task is registered with name: " + name);
        }

        install(child);
    }

    /**
     * Installs this specialized action as a specialization of the specified
     * action. Note that this action can only be installed once: to reinstall a
     * specialized action after uninstallation, create a new instance of the
     * specialized action.
     *
     * @param action the action that this action is a more specialized version
     * of
     */
    public void install(TaskAction action) {
        if (action == null) {
            throw new NullPointerException("action");
        }
        if (superAction != null) {
            throw new IllegalStateException("already installed");
        }

        superAction = action;
        if (action instanceof SpecializedAction) {
            ((SpecializedAction) action).parent = this;
        }
        Actions.specializeAction(superAction, this);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation returns the same name as that of the original action.
     */
    @Override
    public final String getActionName() {
        return superAction.getActionName();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation returns the result of calling this method on the
     * original action.
     */
    @Override
    public String getLabel() {
        return superAction.getLabel();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation returns the result of calling this method on the
     * original action.
     */
    @Override
    public boolean performOnSelection(Member[] members) {
        return superAction.performOnSelection(members);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation returns the result of calling this method on the
     * original action.
     */
    @Override
    public boolean perform(Project project, Task task, Member member) {
        return superAction.perform(project, task, member);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation returns the result of calling this method on the
     * original action.
     */
    @Override
    public boolean appliesToSelection(Member[] members) {
        return superAction.appliesToSelection(members);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation returns the result of calling this method on the
     * original action.
     */
    @Override
    public boolean appliesTo(Project project, Task task, Member member) {
        return superAction.appliesTo(project, task, member);
    }

    /**
     * Returns the action that this action specializes.
     *
     * @return the parent action that this action specialized
     */
    public final TaskAction getSuperAction() {
        return superAction;
    }

    /**
     * Returns the topmost super action in the chain of specialized actions.
     * This is similar to {@link #getSuperAction()}, except that if an action
     * has been specialized multiple times, this method will return the
     * original, unspecialized action at the top of the specialization chain.
     *
     * @return the original, unspecialized action
     */
    public final TaskAction getRootAction() {
        TaskAction parent = superAction;
        while (parent != null && parent instanceof SpecializedAction) {
            parent = ((SpecializedAction) parent).superAction;
        }
        return parent;
    }

    /**
     * Removes this action from the specialization chain of the root action.
     */
    public void uninstall() {
        // this is currently the head of the specialization chain, so we
        // need to replace this action with our superaction
        if (parent == null) {
            Actions.unspecializeAction(this);
        } else {
            // we have been specialized: we need to point our parent's
            // superAction to our superAction to remove ourselves from the chain
            parent.superAction = superAction;
        }

        // if our child is a specialized action, we need to update its parent
        // to point to our parent (this may be null if we were the head of the chain)
        if (superAction instanceof SpecializedAction) {
            ((SpecializedAction) superAction).parent = parent;
        }
    }
}
