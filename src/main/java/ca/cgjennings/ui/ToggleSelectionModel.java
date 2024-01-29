package ca.cgjennings.ui;

import javax.swing.DefaultListSelectionModel;

/**
 * A list selection model that toggles selection states instead of setting or
 * clearing them.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class ToggleSelectionModel extends DefaultListSelectionModel {

    @Override
    public void setValueIsAdjusting(boolean isAdjusting) {
        if (!isAdjusting) {
            adjusting = false;
        }
        super.setValueIsAdjusting(isAdjusting);
    }

    @Override
    public void setSelectionInterval(int index0, int index1) {
        if (!adjusting) {
            if (isSelectedIndex(index1)) {
                super.removeSelectionInterval(index0, index1);
            } else {
                super.addSelectionInterval(index0, index1);
            }
            adjusting = getValueIsAdjusting();
        }
    }

    private boolean adjusting;
}
