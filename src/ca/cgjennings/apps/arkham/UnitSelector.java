package ca.cgjennings.apps.arkham;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import static resources.Language.string;

/**
 * A dialog box that allows the user to select a length unit from amongst cm,
 * inches, and points. The initial unit selection will be equal to
 * {@link Length#getDefaultUnit()}, and the default unit will be updated in
 * response to the user's selection.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.0
 * @see Length
 */
@SuppressWarnings("serial")
public class UnitSelector extends JComboBox {

    public UnitSelector() {
        super();
        String[] units;
        units = new String[]{string("iid-cb-unit0"), string("iid-cb-unit1"), string("iid-cb-unit2")};
        setModel(new DefaultComboBoxModel(units));
        setEditable(false);
        int i = Length.getDefaultUnit();
        setSelectedIndex(i);
    }

    /**
     * Return the currently selected unit, one of {@code Length.CM},
     * {@code Length.IN}, or {@code Length.PT}.
     *
     * @return the currently selected unit
     */
    public int getUnit() {
        return getSelectedIndex();
    }

    /**
     * Sets the currently selected unit. The value of {@code unit} must be
     * one of {@code Length.CM}, {@code Length.IN}, or
     * {@code Length.PT}.
     *
     * @param unit the unit to select
     */
    public void setUnit(int unit) {
        if (unit < 0 || unit >= getModel().getSize()) {
            throw new IllegalArgumentException("invalid unit");
        }
        setSelectedIndex(unit);
    }

    @Override
    protected void fireActionEvent() {
        super.fireActionEvent();
        int sel = getUnit();
        if (sel != -1) {
            Length.setDefaultUnit(sel);
        }
    }
}
