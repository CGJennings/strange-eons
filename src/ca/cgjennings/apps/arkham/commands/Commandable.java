package ca.cgjennings.apps.arkham.commands;

/**
 * This interface is implemented by classes that can be consulted about the
 * commands they are able to perform. Commands that are aware of this interface
 * can ask a commandable of interest (typically the current editor) if it wants
 * to handle the command. If the commandable does want to handle the command,
 * then the command will delegate command processing to the commandable.
 *
 * @see DelegatedCommand
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface Commandable {

    /**
     * Returns {@code true} if the commandable wishes to handle the given
     * command. This method defines the set of commands that the commandable
     * responds to. The commandable might not be able to act on the command at
     * the current moment. For example, a commandable that responds to "Cut"
     * could return true from {@code handlesCommand}, but false from
     * {@link #isCommandApplicable} if there is currently no selection to cut.
     *
     * @param command the command to be performed
     * @return {@code true} if this commandable wishes to handle the command
     * (even if it cannot execute the command currently)
     */
    boolean canPerformCommand(AbstractCommand command);

    /**
     * Returns {@code true} if the {@code command} can be performed by this
     * commandable in its current state. If {@link #canPerformCommand} would
     * return false for this command, then this must also return false.
     *
     * @param command the command to be performed
     * @return {@code true} if this commandable can currently perform the
     * command
     */
    boolean isCommandApplicable(AbstractCommand command);

    /**
     * Performs the command. If {@link #isCommandApplicable} would return
     * {@code false} for this command, then this should do nothing.
     *
     * @param command the command to perform
     */
    void performCommand(AbstractCommand command);
}
