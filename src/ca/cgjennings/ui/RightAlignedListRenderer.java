package ca.cgjennings.ui;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;

/**
 * A renderer for lists that produces right-aligned (actually, opposite of
 * reading order) text.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class RightAlignedListRenderer extends DefaultListCellRenderer {

    public RightAlignedListRenderer() {
        setHorizontalAlignment(JLabel.TRAILING);
    }
}
