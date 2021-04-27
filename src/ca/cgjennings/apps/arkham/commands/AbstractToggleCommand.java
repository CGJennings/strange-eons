package ca.cgjennings.apps.arkham.commands;

import java.awt.event.ActionEvent;

/**
 * An abstract base class for commands that toggle a state when selected. Note
 * that whether a toggle command is selected or not is determined by the value
 * of the action's <code>SELECTED_KEY</code> key. It is up to the subclass to
 * set this key to an appropriate initial value. (The {@link #isSelected()}
 * method is simply a convenience that reads the value of this key and casts the
 * result to a boolean value.)
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public abstract class AbstractToggleCommand extends AbstractCommand {

    /**
     * Creates a command with no predefined name or icon.
     */
    public AbstractToggleCommand() {
    }

    /**
     * Defines a command using a name determined by the specified string key.
     *
     * @param nameKey the localized text key for the command name
     * @see #setNameKey
     */
    public AbstractToggleCommand(String nameKey) {
        super(nameKey);
    }

    /**
     * Defines a command using a name determined by the specified string key and
     * an icon loaded from the given icon resource.
     *
     * @param nameKey the localized text key for the command name
     * @param iconResource the resource file to load an icon from
     * @see #setNameKey
     * @see #setIcon
     */
    public AbstractToggleCommand(String nameKey, String iconResource) {
        super(nameKey, iconResource);
    }

    /**
     * Returns <code>true</code> if the command is selected, that is, if the
     * toggle state is currently enabled.
     *
     * @return <code>true</code> if the command is selected
     */
    public final boolean isSelected() {
        return (Boolean) getValue(SELECTED_KEY);
    }

    /**
     * Sets whether the command is selected. This should be used to set the
     * command's initial state. It does not cause the command to be performed.
     *
     * @param selected if <code>true</code>, the command will be selected
     */
    public final void setSelected(boolean selected) {
        putValue(SELECTED_KEY, selected);
    }

    /**
     * If the command is enabled, toggles the selected state of the command by
     * generating a fake action event.
     */
    public final void toggle() {
        update();
        if (!isEnabled()) {
            return;
        }
        setSelected(!isSelected());
        actionPerformed(new ActionEvent(this, 0, "toggle"));
    }
}
