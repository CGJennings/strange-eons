package ca.cgjennings.apps.arkham.commands;

import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import java.awt.event.ActionEvent;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import resources.AcceleratorTable;

/**
 * Delegated commands are commands that are normally handled by a
 * {@link Commandable}, such as a {@link StrangeEonsEditor}, instead of being
 * handled by the command itself.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class DelegatedCommand extends AbstractCommand {

    public DelegatedCommand() {
    }

    public DelegatedCommand(String nameKey) {
        super(nameKey);
    }

    public DelegatedCommand(String nameKey, String iconResource) {
        super(nameKey, iconResource);
    }

    public DelegatedCommand(String nameKey, String iconResource, String acceleratorKey) {
        super(nameKey, iconResource);
        if(acceleratorKey != null) {
            final KeyStroke ks = AcceleratorTable.getApplicationTable().get(acceleratorKey);
            if (ks != null) {
                setAccelerator(ks);
            }
        }
    }

    /**
     * Executes the delegated command by finding a command handler to delegate
     * to; if no {@link Commandable} is found, and a default action is
     * applicable, then the default action is performed by calling
     * {@link #performDefaultAction}.
     *
     * @param e the action event that describes the command activation, or
     * {@code null}
     * @see Commands#findCommandable
     * @see #isDefaultActionApplicable()
     * @see #performDefaultAction(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        Commandable c = Commands.findCommandable(this);
        if (c != null) {
            c.performCommand(this);
        } else if (isDefaultActionApplicable()) {
            performDefaultAction(e);
        } else {
            // if the action was activated with a real event, e.g., menu item selection,
            // provide error feedback to indicate that the command was not applicable
            if (e != null && getValue("silent") != Boolean.TRUE) {
                UIManager.getLookAndFeel().provideErrorFeedback(null);
            }
        }
    }

    /**
     * Performs a default action for this delegated command. If no relevant
     * {@link Commandable} wishes to handle a command, and if
     * {@link #isDefaultActionApplicable()} returns {@code true}, then this
     * method is called to handle the command. (The base class implementation
     * does nothing.)
     *
     * @param e the event that caused the command to activate
     * @see Commands#findCommandable
     * @see #actionPerformed(java.awt.event.ActionEvent)
     * @see #isDefaultActionApplicable()
     */
    public void performDefaultAction(ActionEvent e) {
    }

    /**
     * Returns {@code true} if a default action is applicable. (The base
     * class implementation returns {@code false}.)
     *
     * @return {@code true} if there is a default action and it is
     * currently applicable
     */
    public boolean isDefaultActionApplicable() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * For delegated commands, this method updates the command's enabled state
     * based on whether a {@link Commandable} is currently available to handle
     * the command and whether a default action is currently applicable.
     */
    @Override
    public void update() {
        setEnabled(Commands.findCommandable(this) != null || isDefaultActionApplicable());
    }
}
