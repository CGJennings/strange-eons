package ca.cgjennings.apps.arkham.commands;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.AbbreviationEditor;
import java.awt.event.ActionEvent;

/**
 * Command that displays an abbreviation table editor.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
final class HAbbrevTable extends AbstractCommand {

    private final boolean markup;

    public HAbbrevTable(boolean markup) {
        super("app-markup-abbreviations");
        this.markup = markup;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new AbbreviationEditor(StrangeEons.getWindow(), markup).setVisible(true);
    }
}
