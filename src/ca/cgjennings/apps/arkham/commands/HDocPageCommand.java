package ca.cgjennings.apps.arkham.commands;

import java.awt.event.ActionEvent;

/**
 * Command that displays a specific doc page or Web page.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
final class HDocPageCommand extends AbstractCommand {
    private final String page;

    public HDocPageCommand(final String nameKey, final String pageName) {
        super(nameKey);
        page = pageName;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        HHelpCommand.showPage(getName(), page);
    }
}
