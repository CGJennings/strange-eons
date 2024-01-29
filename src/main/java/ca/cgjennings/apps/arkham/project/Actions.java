package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

/**
 * A registry of all {@link TaskAction}s that can be performed.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public final class Actions {

    /**
     * The maximum (highest) possible action priority.
     */
    public static final int PRIORITY_MAX = 100;
    /**
     * The default action priority.
     */
    public static final int PRIORITY_DEFAULT = 0;
    /**
     * The minimum (lowest) possible action priority.
     */
    public static final int PRIORITY_MIN = -100;
    /**
     * The priority used by built-in file management actions, such as delete and
     * rename.
     */
    public static final int PRIORITY_FILE_MANAGEMENT = -85;
    /**
     * The priority used by secondary file management actions, such as show
     * folder.
     */
    public static final int PRIORITY_FILE_MANAGEMENT_SECONDARY = -86;
    /**
     * This priority is used by built-in actions that should normally appear at
     * the top of a command list. (Use a higher value to ensure your action has
     * greater priority.) This is usually the default "Open" action, if it
     * applies.
     */
    public static final int PRIORITY_STANDARD_FIRST = 95;

    /**
     * This priority is used by actions that relate to executing, compiling,
     * building or debugging code or running automated processes. The built-in
     * commands to run and debug scripts use this priority.
     */
    public static final int PRIORITY_BUILD = 90;
    /**
     * This priority is used by second tier build commands. For example, Make
     * Deck uses this so that it falls after the factory automation commands.
     */
    public static final int PRIORITY_BUILD_SECONDARY = 85;

    /**
     * This priority is used by clipboard operations (copy, paste).
     */
    public static final int PRIORITY_CLIPBOARD = -5;

    /**
     * This priority is used by tools that import, export, or convert file
     * formats.
     */
    public static final int PRIORITY_IMPORT_EXPORT = -80;

    /**
     * This priority is used by built-in actions that should normally appear at
     * the bottom of a command list.
     */
    public static final int PRIORITY_STANDARD_LAST = -95;

    /**
     * This priority is used by developer tools designed to help debug project
     * extensions.
     */
    public static final int PRIORITY_DEBUGGING_TOOL = -99;

    private static HashMap<Integer, List<TaskAction>> actionMap = new HashMap<>();
    private static HashMap<TaskAction, Integer> priorityMap = new HashMap<>();

    /**
     * Registers a new action with the default priority. Equivalent to
     * {@code register( action, Actions.PRIORITY_DEFAULT )}.
     *
     * @param action the action to register
     */
    public static void register(TaskAction action) {
        register(action, PRIORITY_DEFAULT);
    }

    /**
     * Registers a new action. If the action was already registered, it will be
     * re-registered at the new priority. An action cannot be registered if it
     * has the same name (as returned by {@link TaskAction#getActionName()}) as
     * an already-registered action.
     *
     * @param action the action to be registered
     * @param priority the priority at which to register the action
     * @throws NullPointerException if {@code action} is {@code null}
     * @throws IllegalArgumentException if {@code priority} is not in the range
     * {@code PRIORITY_MIN &lt;= priority &lt;= PRIORITY_MAX}
     * @throws IllegalArgumentException if an action with this action's name is
     * already registered (other than this action)
     */
    public static void register(TaskAction action, int priority) {
        checkPriority(priority);
        if (action == null) {
            throw new NullPointerException("action");
        }
        if (action.getActionName() == null) {
            throw new NullPointerException("action.actionName()");
        }

        //  already registered? unregister it to prevent a name conflict
        if (priorityMap.containsKey(action)) {
            unregister(action);
        }

        // name already used?
        if (findActionByName(action.getActionName()) != null) {
            throw new IllegalArgumentException("an action with this name is already registered: " + action.getActionName());
        }

        priorityMap.put(action, priority);

        List<TaskAction> plist = actionMap.get(priority);
        if (plist == null) {
            plist = new LinkedList<>();
            actionMap.put(priority, plist);
        }
        plist.add(action);
    }

    /**
     * Unregisters a registered task action. If the action is not registered,
     * this method does nothing.
     *
     * @param action the action to be unregistered
     */
    public static void unregister(TaskAction action) {
        Integer p = priorityMap.remove(action);
        if (p == null) {
            return;
        }

        List<TaskAction> plist = actionMap.get(p);
        plist.remove(action);

        if (plist.isEmpty()) {
            actionMap.remove(p);
        }
    }

    /**
     * Returns the registered action with the given name, or {@code null} if no
     * action has this name. Each action must have a unique name, which is
     * returned by {@link TaskAction#getActionName()}.
     *
     * @param name the action name to search for
     * @return the registered action with the given name, or {@code null}
     */
    public static TaskAction findActionByName(String name) {
        for (TaskAction ta : priorityMap.keySet()) {
            if (ta.getActionName().equals(name)) {
                return ta;
            }
            if (ta instanceof TaskActionTree) {
                TaskAction result = ((TaskActionTree) ta).findActionByName(name);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Returns the original, unspecialized instance of an action. If no action
     * with the given name is registered, returns {@code null}. This is useful
     * if you need to call a method on a task action by casting it to its actual
     * type. For example:
     * <pre>
     * ((Open) Actions.getUnspecializedAction( "open" )).registerOpener( myOpener );
     * </pre>
     *
     * @param name the name of the action to find the unspecialized instance of
     * @return the originally registered action with the given name, or
     * {@code null}
     */
    public static TaskAction getUnspecializedAction(String name) {
        TaskAction a = Actions.findActionByName(name);
        if (a != null && a instanceof SpecializedAction) {
            a = ((SpecializedAction) a).getRootAction();
        }
        return a;
    }

    /**
     * Returns the current priority of a registered action.
     *
     * @param ta the action to look up the priority of
     * @return the priority at which {@code ta} was registered
     * @throws IllegalArgumentException if {@code ta} is not a registered action
     */
    public static int getPriorityForAction(TaskAction ta) {
        Integer p = priorityMap.get(ta);
        if (p == null) {
            throw new IllegalArgumentException("action is not registered: " + ta);
        }
        return p;
    }

    /**
     * Replaces an action with a specialized version. Unlike unregistering and
     * registering an action, this will preserve the original action's order
     * amongst commands with the same priority. This is not called directly;
     * {@link SpecializedAction#SpecializedAction} will call it on your behalf.
     *
     * @param ta the original action
     * @param sa the action the specializes the original action
     */
    static void specializeAction(TaskAction ta, SpecializedAction sa) {
        int pri = Actions.getPriorityForAction(ta);

        List<TaskAction> actions = actionMap.get(pri);
        int i = actions.indexOf(ta);
        if (i < 0) {
            throw new AssertionError("action missing; concurrent modification?");
        }

        // replace original action at its position in the list
        actions.set(i, sa);
        priorityMap.remove(ta);
        priorityMap.put(sa, pri);
    }

    /**
     * Replaces a specialized action with its immediate child action. This is
     * not called directly. {@link SpecializedAction#unintall} will call this if
     * the action being uninstalled has not itself been specialized. That is,
     * when you uninstall the most recent specialization, it replaces it with
     * the action it specializes. When you uninstall a specialization in the
     * middle of a chain of specializations, it is simply unlinked from the
     * chain.
     *
     * @param ta the action being uninstalled
     */
    static void unspecializeAction(SpecializedAction ta) {
        int pri = Actions.getPriorityForAction(ta);

        List<TaskAction> actions = actionMap.get(pri);
        int i = actions.indexOf(ta);
        if (i < 0) {
            throw new AssertionError("action missing; not the first specialization?");
        }

        // replace original action at its position in the list
        TaskAction old = ta.getSuperAction();
        actions.set(i, old);
        priorityMap.remove(ta);
        priorityMap.put(old, pri);
    }

    /**
     * Returns a list of the actions registered at a given priority, or an empty
     * list if there are no registered actions at that priority. Returned lists
     * are immutable.
     *
     * @param priority the desired priority of the listed actions
     * @return all registered actions that are registered at the requested
     * priority
     * @throws IllegalArgumentException if {@code priority} is not a legal
     * priority value
     */
    static List<TaskAction> getActionsForPriority(int priority) {
        checkPriority(priority);
        List<TaskAction> plist = actionMap.get(priority);
        return plist == null ? Collections.emptyList() : Collections.unmodifiableList(plist);
    }

    /**
     * Return an array of all actions registered at the root level (that is, the
     * root of action trees and the most specialized of specialized actions).
     *
     * @return an array of the basic actions available for project users
     */
    public static TaskAction[] getRootActions() {
        LinkedList<TaskAction> list = new LinkedList<>();
        for (int p = PRIORITY_MAX; p >= PRIORITY_MIN; --p) {
            list.addAll(getActionsForPriority(p));
        }
        return list.toArray(TaskAction[]::new);
    }

    /**
     * Creates a popup menu that displays suitable actions for the specified
     * members.
     *
     * @param targets the members to create a menu for
     * @return a menu of applicable actions
     */
    static JPopupMenu buildMenu(Member[] targets) {
        boolean anyItemHasIcon = false;
        JPopupMenu menu = new JPopupMenu();
        for (int p = PRIORITY_MAX; p >= PRIORITY_MIN; --p) {
            List<TaskAction> actions = actionMap.get(p);
            if (actions != null && !actions.isEmpty()) {
                boolean addSeparator = menu.getComponentCount() > 0;
                for (TaskAction action : actions) {
                    if (!action.appliesToSelection(targets)) {
                        continue;
                    }
                    if (addSeparator) {
                        menu.addSeparator();
                        addSeparator = false;
                    }
                    JMenuItem item = buildMenuItem(action, targets);
                    anyItemHasIcon |= item.getIcon() != null;
                    menu.add(item);
                }
            }
        }

        // if any item has an icon set, supply a blank icon for any unset items
        // this works around some L&Fs that don't align the text labels otherwise
        if (anyItemHasIcon) {
            for (int i = 0; i < menu.getComponentCount(); ++i) {
                Component c = menu.getComponent(i);
                if (c != null && c instanceof JMenuItem) {
                    JMenuItem item = (JMenuItem) c;
                    if (item.getIcon() == null) {
                        item.setIcon(MetadataSource.ICON_BLANK);
                    }
                }
            }
        }

        return menu;
    }

    private static JMenuItem buildMenuItem(TaskAction action, Member[] targets) {
        if (action instanceof TaskActionTree) {
            boolean anyItemHasIcon = false;
            JMenu menu = new JMenu(action.getLabel());
            boolean lastWasSeparator = true;
            for (TaskAction child : (TaskActionTree) action) {
                if (child != null && !child.appliesToSelection(targets)) {
                    continue;
                }
                if (child == null) {
                    if (!lastWasSeparator) {
                        menu.addSeparator();
                        lastWasSeparator = true;
                    }
                } else {
                    JMenuItem item = buildMenuItem(child, targets);
                    anyItemHasIcon |= item.getIcon() != null;
                    menu.add(item);
                    lastWasSeparator = false;
                }
            }

            // if any item has an icon set, supply a blank icon for any unset items
            // this works around some L&Fs that don't align the text labels otherwise
            if (anyItemHasIcon) {
                for (int i = 0; i < menu.getComponentCount(); ++i) {
                    Component c = menu.getComponent(i);
                    if (c != null && c instanceof JMenuItem) {
                        JMenuItem item = (JMenuItem) c;
                        if (item.getIcon() == null) {
                            item.setIcon(MetadataSource.ICON_BLANK);
                        }
                    }
                }
            }

            menu.setIcon(action.getIcon());
            menu.setToolTipText(action.getDescription());

            return menu;
        } else {
            JMenuItem item = new JMenuItem(action.getLabel());
            item.addActionListener(createActionListener(action, targets));
            item.setIcon(action.getIcon());
            item.setToolTipText(createToolTip(action));
//			KeyStroke hint = acceleratorHints.get( action );
//			if( hint != null ) {
//				item.setAccelerator( hint );
//			}
            return item;
        }
    }

    @SuppressWarnings("deprecation")
    private static String createToolTip(TaskAction action) {
        KeyStroke hint = acceleratorHints.get(action);
        String desc = action.getDescription();
        if (hint == null && desc == null) {
            return null;
        }
        StringBuilder b = new StringBuilder("<html><b>");
        b.append(action.getLabel()).append("</b>");

        if (hint != null) {
            b.append("&nbsp;&nbsp;&nbsp;&nbsp;");

            int mod = hint.getModifiers();
            if (mod > 0) {
                b.append(KeyEvent.getKeyModifiersText(mod));
                b.append('+');
            }
            b.append(KeyEvent.getKeyText(hint.getKeyCode()));
        }
        if (desc != null) {
            b.append("<br><span style='font-size:90%'>")
                    .append(desc)
                    .append("</span>");

        }
        return b.toString();
    }

    static void setAcceleratorHint(TaskAction ta, KeyStroke key) {
        acceleratorHints.put(ta, key);
    }
    private static HashMap<TaskAction, KeyStroke> acceleratorHints = new HashMap<>();

    /**
     * Helper method that creates an action listener for one of the popup menu
     * items created in {@link #buildMenu}. When it receives an action event, it
     * executes the task action on the provided targets (normally the members
     * that were selected when the menu was built).
     *
     * @param action the action to apply
     * @param targets the targets to apply the action to
     * @return an action listener for a menu item
     */
    private static ActionListener createActionListener(final TaskAction action, final Member[] targets) {
        return (ActionEvent e) -> {
            // add activity to event queue so menu can close immediately
            EventQueue.invokeLater(() -> {
                StrangeEons.setWaitCursor(true);
                try {
                    action.performOnSelection(targets);
                } finally {
                    StrangeEons.setWaitCursor(false);
                }
            });
        };
    }

    /**
     * Helper method that checks that a priority value is legal and throws an
     * IAE if it is not.
     *
     * @param p the priority level to check
     * @throws IllegalArgumentException if {@code p} is invalid
     */
    private static void checkPriority(int p) {
        if (p < PRIORITY_MIN || p > PRIORITY_MAX) {
            throw new IllegalArgumentException("invalid priority: " + p);
        }
    }

    /**
     * This class cannot be instantiated.
     */
    private Actions() {
    }

    // register the built-in actions
    static {
        register(New.createDefaultNewAction(), PRIORITY_STANDARD_FIRST);
        // special version of the run command for .ajs files to make it the default
        register(new Run(false) {
            @Override
            public boolean appliesTo(Project project, Task task, Member member) {
                return member != null && ProjectUtilities.matchExtension(member, "ajs");
            }

            @Override
            public String getActionName() {
                return "run-automation";
            }
        }, PRIORITY_STANDARD_FIRST);
        register(new Translate(), PRIORITY_STANDARD_FIRST);
        register(new View(), PRIORITY_STANDARD_FIRST);
        register(new Open(), PRIORITY_STANDARD_FIRST);
        register(new Browse(), PRIORITY_STANDARD_FIRST);
        register(new CompareFiles(), PRIORITY_STANDARD_FIRST);

        register(new Run(false), PRIORITY_BUILD);
        register(new Run(true), PRIORITY_BUILD);
        register(new MakeBundle(), PRIORITY_BUILD);
        register(new TestBundle(), PRIORITY_BUILD);
        register(new Compile(), PRIORITY_BUILD);
        register(new CompileAll(), PRIORITY_BUILD);
//		register( new CSVFactoryBuildAll(), PRIORITY_BUILD );
//		register( new CSVFactoryBuild(), PRIORITY_BUILD );
        register(new ScriptedFactoryBuild(), PRIORITY_BUILD);

        register(new Clean(), PRIORITY_BUILD);
        register(new AddLocale(), PRIORITY_BUILD);
        register(new DrawRegion(), PRIORITY_BUILD);
        register(new MergeSettings(), PRIORITY_BUILD);
        register(new MergeStrings(), PRIORITY_BUILD);

        register(new VirtualDeck(), PRIORITY_BUILD_SECONDARY);
        register(new MakeDeck(), PRIORITY_BUILD_SECONDARY);

        register(new Rename(), PRIORITY_FILE_MANAGEMENT);
        register(new Delete(), PRIORITY_FILE_MANAGEMENT);

        register(new ShowFolder(), PRIORITY_FILE_MANAGEMENT_SECONDARY);

        register(new Cut(), PRIORITY_CLIPBOARD);
        register(new Copy(), PRIORITY_CLIPBOARD);
        register(new Paste(), PRIORITY_CLIPBOARD);

        register(new ConvertImage(), PRIORITY_IMPORT_EXPORT);
        register(new RasterizeImage(), PRIORITY_IMPORT_EXPORT);
        register(new ConvertSpellingDictionary(), PRIORITY_IMPORT_EXPORT);
        register(new PublishBundle(), PRIORITY_IMPORT_EXPORT);

        register(new PluginImportAction(), PRIORITY_STANDARD_LAST);
        register(new ChangeIcon(), PRIORITY_STANDARD_LAST);
        register(new Export(), PRIORITY_STANDARD_LAST);
        register(new Packaging(), PRIORITY_STANDARD_LAST);

    }
}
