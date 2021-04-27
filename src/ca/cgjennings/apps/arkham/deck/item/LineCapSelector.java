package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.ui.JIconSelector;

/**
 * A control for selecting a line cap value.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class LineCapSelector extends JIconSelector<LineCap> {

    public LineCapSelector() {
        for (LineCap c : LineCap.values()) {
            addItem(c);
        }
    }
}
