package ca.cgjennings.apps.arkham.commands;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.awt.event.ActionEvent;

/**
 * A helper class for commands that are actually handled by the application
 * window.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
class HProxyCommand extends AbstractCommand {

    public HProxyCommand() {
    }

    public HProxyCommand(String nameKey) {
        super(nameKey);
    }

    public HProxyCommand(String nameKey, String iconResource) {
        super(nameKey, iconResource);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        StrangeEons.getWindow().performCommand(this);
    }
}
