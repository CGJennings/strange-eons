package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.ui.JIconSelector;

/**
 * A control for selecting a line join value.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class LineJoinSelector extends JIconSelector<LineJoin> {

    public LineJoinSelector() {
        for (LineJoin c : LineJoin.values()) {
            addItem(c);
        }
    }
}
